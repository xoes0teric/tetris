package game;

import boss.BedrockBoss;
import boss.BossAbility;
import boss.BossOutcome;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;
import upgrades.Hold;
import upgrades.ScoreMultiplier;
import upgrades.SeeTheFuture;
import upgrades.Spin;
import upgrades.Upgrade;
import upgrades.UpgradeSelection;
import upgrades.Upgrader;

/**
    PlayManager manages the movement of pieces
    Everything is in cells, drawing in pixels is done by gui.Renderer

    It also decides which screen comes next (upgrade selection, boss) after
    anything changes the board, see resolveNextState()
*/
public class PlayManager {
    // Where the first block of a new piece appears, in cells
    private static final int SPAWN_COL = 4;
    private static final int SPAWN_ROW = 1;

    private final Board board = new Board();

    private Tetromino currentPiece;
    private boolean active = true; // false once the current piece is ready to lock
    private GameState gameState = GameState.PLAY;

    private final Timer timer = new Timer();
    private final PiecePicker picker;
    private final ScoreTracker tracker = new ScoreTracker(timer);
    private final Upgrader upgrader;
    // The upcoming pieces shown in the GUI. Only changed on the game thread so the
    // renderer never touches the picker
    private List<Shape> nextShapes;
    // Rows cleared for GUI effects
    private final List<Integer> clearedRows = new ArrayList<>();

    private UpgradeSelection upgradeSelection; // open while SELECT_UPGRADE
    private Spin spin; // running while GAMBLE

    private final Queue<BedrockBoss> upcomingBosses = new ArrayDeque<>();
    private BedrockBoss boss; // the boss being introduced, fought or just finished
    private boolean bossResultPending = false;
    private int[][] forcedDropStart; // blocks before the boss forced a drop, for GUI effects

    public PlayManager() {
        this(new Random(), List.of(new BedrockBoss()));
    }

    public PlayManager(Random random, List<BedrockBoss> bosses) {
        picker = new PiecePicker(board, random);
        upgrader = new Upgrader(random);
        upcomingBosses.addAll(bosses);

        // Set the starting piece
        currentPiece = pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
    }

    // Takes the next piece from the picker and updates the shown queue
    private Tetromino pickPiece() {
        Tetromino piece = picker.pickPiece();
        refreshNextShapes();
        return piece;
    }

    private void refreshNextShapes() {
        nextShapes = List.copyOf(picker.peek(upgrader.get(SeeTheFuture.class).getPreviewCount()));
    }

    // The falling piece can only be moved during normal play
    private boolean canControl() {
        return active && gameState == GameState.PLAY;
    }

    public void rotate() {
        if (canControl()) {
            currentPiece.rotate();
        }
    }

    public void moveLeft() {
        if (canControl()) {
            currentPiece.tryMove(sideways(-1), 0);
        }
    }

    public void moveRight() {
        if (canControl()) {
            currentPiece.tryMove(sideways(1), 0);
        }
    }

    // The boss can swap left and right
    private int sideways(int direction) {
        return isControlsInverted() ? -direction : direction;
    }

    public void moveDown() {
        // When moved down, reset the autoDropCounter
        if (canControl() && currentPiece.tryMove(0, 1)) {
            timer.resetAutoDropCounter();
        }
    }

    // Returns whether the piece was dropped
    public boolean hardDrop() {
        if (!canControl()) {
            return false;
        }
        while (currentPiece.tryMove(0, 1)) {
        }
        active = false;
        return true;
    }

    // Swaps the falling piece with the held one (needs the Hold upgrade)
    public void hold() {
        Hold hold = upgrader.get(Hold.class);
        if (!canControl() || !hold.canHold()) {
            return;
        }
        Shape held = hold.swap(currentPiece.getShape());
        currentPiece = held == null ? pickPiece() : new Tetromino(held, board);
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        timer.resetCounters();
    }

    public void update(long elapsedNanos) {
        updateBoss(elapsedNanos);
        if (gameState == GameState.GAMBLE) {
            updateSpin(elapsedNanos);
            return;
        }
        if (gameState != GameState.PLAY) {
            return;
        }

        if (!active) {
            lockPiece();
            return;
        }

        if (currentPiece.canMoveDown()) {
            timer.resetDeactivateCounter();
            if (timer.shouldDrop()) {
                currentPiece.tryMove(0, 1);
            }
        } else if (timer.shouldLock()) {
            active = false;
        }
    }

    private void lockPiece() {
        board.lock(currentPiece);
        upgrader.get(Hold.class).onPieceLocked();
        Tetromino locked = currentPiece;

        // if the first block is still on the spawn cell, the piece couldn't move at all,
        // so the stack has reached the top (game over)
        int[] first = currentPiece.getBlocks()[0];
        if (first[0] == SPAWN_COL && first[1] == SPAWN_ROW) {
            gameState = GameState.GAME_OVER;
        }

        // replace the currentPiece with the next piece
        currentPiece = pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        active = true;
        timer.resetCounters();

        // a bomb clears its whole row, full or not
        List<Integer> cleared = new ArrayList<>();
        if (locked.getShape() == Shape.BOMB) {
            int row = locked.getBlocks()[0][1];
            board.destroyRow(row);
            cleared.add(row);
        }
        // when a piece locks, check if line(s) can be deleted
        cleared.addAll(board.clearFullRows());
        if (!cleared.isEmpty()) {
            int points = tracker.countClearedLines(cleared,
                    upgrader.get(ScoreMultiplier.class).getPercent());
            clearedRows.addAll(cleared);
            addBossScore(points);
        }

        // rows moved, make sure the falling piece doesn't overlap blocks
        currentPiece.moveUpUntilFits();
        resolveNextState();
    }

    // Gives the points to the boss while a fight is running
    private void addBossScore(int points) {
        if (isBossFightRunning()) {
            boss.addScore(points);
            if (boss.getOutcome() != BossOutcome.RUNNING) {
                finishBoss();
            }
        }
    }

    // Picks the next screen: boss result, then upgrade selection, then boss intro, then play
    private void resolveNextState() {
        if (gameState == GameState.GAME_OVER) {
            return;
        }
        if (bossResultPending) {
            gameState = GameState.BOSS_RESULT;
            return;
        }
        if (spin != null) {
            gameState = GameState.GAMBLE;
            return;
        }
        while (upgrader.isSelectionDue(getLines())) {
            UpgradeSelection selection = upgrader.openSelection();
            if (!selection.getOptions().isEmpty()) {
                upgradeSelection = selection;
                gameState = GameState.SELECT_UPGRADE;
                return;
            }
        }
        if (boss == null && !upcomingBosses.isEmpty()
                && getLines() >= upcomingBosses.peek().getTriggerLines()) {
            boss = upcomingBosses.poll();
            gameState = GameState.BOSS_INTRO;
            return;
        }
        gameState = GameState.PLAY;
    }

    // ---- Upgrade selection ----

    public void moveUpgradeSelection(int direction) {
        if (gameState == GameState.SELECT_UPGRADE) {
            upgradeSelection.move(direction);
        }
    }

    public void selectUpgrade(int index) {
        if (gameState == GameState.SELECT_UPGRADE) {
            upgradeSelection.select(index);
        }
    }

    public void confirmUpgrade() {
        if (gameState != GameState.SELECT_UPGRADE) {
            return;
        }
        upgradeSelection.getSelectedUpgrade().pick(this);
        upgradeSelection = null;
        refreshNextShapes(); // See the Future may show more now
        resolveNextState();
    }

    // ---- Gamble ----

    // Called by Gamble when it is picked, the result is given when the spin ends
    public void startSpin(Spin spin) {
        this.spin = spin;
    }

    private void updateSpin(long elapsedNanos) {
        spin.update(elapsedNanos);
        if (spin.isFinished()) {
            Upgrade result = spin.getResult();
            spin = null;
            result.pick(this);
            refreshNextShapes();
            resolveNextState();
        }
    }

    // Pushes garbage rows up from the bottom, each with a gap in the same random column
    public void addGarbageRows(int count) {
        int gapCol = upgrader.getRandom().nextInt(Board.COLUMNS);
        if (!board.addGarbageRows(count, gapCol)) {
            gameState = GameState.GAME_OVER;
        }
        currentPiece.moveUpUntilFits();
    }

    // ---- Boss ----

    public boolean isBossFightRunning() {
        return boss != null && boss.getOutcome() == BossOutcome.RUNNING;
    }

    // The boss clock keeps running in menus so they can't be used to get more time.
    // Only a real pause (the game doesn't call update) stops it
    private boolean bossClockRuns() {
        return gameState == GameState.PLAY || gameState == GameState.SELECT_UPGRADE
                || gameState == GameState.GAMBLE;
    }

    private void updateBoss(long elapsedNanos) {
        if (!isBossFightRunning() || !bossClockRuns()) {
            return;
        }
        boss.tick(elapsedNanos);
        if (boss.getOutcome() != BossOutcome.RUNNING) {
            upgradeSelection = null;
            finishBoss();
            resolveNextState();
            return;
        }
        // abilities wait while a menu is open, the clock doesn't
        if (gameState == GameState.PLAY) {
            boss.tickAbilities(elapsedNanos);
            useBossAbility(boss.getAbilities().takeUsed());
        }
    }

    // Inverted controls are handled by the boss itself, see isControlsInverted()
    private void useBossAbility(BossAbility ability) {
        if (ability == BossAbility.FORCE_DROP) {
            int[][] before = currentPiece.getBlocks();
            if (hardDrop()) {
                forcedDropStart = before;
            }
        }
    }

    public boolean isControlsInverted() {
        return isBossFightRunning() && boss.getAbilities().isControlsInverted();
    }

    // Returns where the piece was before the boss forced it down and forgets it,
    // null if the boss didn't
    public int[][] takeForcedDropStart() {
        int[][] before = forcedDropStart;
        forcedDropStart = null;
        return before;
    }

    private void finishBoss() {
        if (boss.getOutcome() == BossOutcome.WON) {
            boss.end(board);
            currentPiece.moveUpUntilFits();
        }
        bossResultPending = true;
    }

    // Starts the fight from the intro screen, or closes the result screen
    public void confirmBossScreen() {
        if (gameState == GameState.BOSS_INTRO) {
            if (!boss.start(board)) {
                gameState = GameState.GAME_OVER;
                return;
            }
            currentPiece.moveUpUntilFits();
            gameState = GameState.PLAY;
        } else if (gameState == GameState.BOSS_RESULT) {
            bossResultPending = false;
            if (boss.getOutcome() == BossOutcome.LOST) {
                gameState = GameState.GAME_OVER;
                return;
            }
            boss = null;
            resolveNextState();
        }
    }

    // Returns the rows cleared since the last call and forgets them
    public List<Integer> takeClearedRows() {
        List<Integer> rows = new ArrayList<>(clearedRows);
        clearedRows.clear();
        return rows;
    }

    public Board getBoard() {
        return board;
    }

    public Tetromino getCurrentPiece() {
        return currentPiece;
    }

    // The upcoming pieces, as many as See the Future shows
    public List<Shape> getNextShapes() {
        return nextShapes;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isGameOver() {
        return gameState == GameState.GAME_OVER;
    }

    public Upgrader getUpgrader() {
        return upgrader;
    }

    // Upgrades use these to change the game when they are picked
    public PiecePicker getPicker() {
        return picker;
    }

    public Timer getTimer() {
        return timer;
    }

    public UpgradeSelection getUpgradeSelection() {
        return upgradeSelection;
    }

    public Spin getSpin() {
        return spin;
    }

    public BedrockBoss getBoss() {
        return boss;
    }

    public int getLevel() {
        return tracker.getLevel();
    }

    public int getLines() {
        return tracker.getLinesCleared();
    }

    public int getScore() {
        return tracker.getScore();
    }
}
