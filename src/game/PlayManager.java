package game;

import boss.BedrockBoss;
import boss.BossOutcome;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;
import upgrades.Flush;
import upgrades.Hammer;
import upgrades.Hold;
import upgrades.Patch;
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
    private boolean hardDropped = false; // whether the current piece was hard dropped
    private GameState gameState = GameState.PLAY;

    private final Timer timer = new Timer();
    private final PiecePicker picker;
    private final ScoreTracker tracker = new ScoreTracker(timer);
    private final ClearHistory history = new ClearHistory();
    private final Upgrader upgrader;
    // The upcoming pieces shown in the GUI. Only changed on the game thread so the
    // renderer never touches the picker
    private List<Shape> nextShapes;
    // Rows cleared for GUI effects
    private final List<Integer> clearedRows = new ArrayList<>();

    private UpgradeSelection upgradeSelection; // open while SELECT_UPGRADE

    private final Queue<BedrockBoss> upcomingBosses = new ArrayDeque<>();
    private BedrockBoss boss; // the boss being introduced, fought or just finished
    private boolean bossResultPending = false;

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
        nextShapes = List.copyOf(picker.peek(upgrader.getSeeTheFuture().getPreviewCount()));
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
            currentPiece.tryMove(-1, 0);
        }
    }

    public void moveRight() {
        if (canControl()) {
            currentPiece.tryMove(1, 0);
        }
    }

    public void moveDown() {
        // When moved down, reset the autoDropCounter
        if (canControl() && currentPiece.tryMove(0, 1)) {
            timer.resetAutoDropCounter();
        }
    }

    public void hardDrop() {
        if (canControl()) {
            while (currentPiece.tryMove(0, 1)) {
            }
            active = false;
            hardDropped = true;
        }
    }

    // Swaps the falling piece with a hold slot (needs the Hold upgrade)
    public void hold(int slot) {
        Hold hold = upgrader.getHold();
        if (!canControl() || !hold.canHold(slot)) {
            return;
        }
        Shape held = hold.swap(slot, currentPiece.getShape());
        currentPiece = held == null ? pickPiece() : new Tetromino(held, board);
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        hardDropped = false;
        timer.resetCounters();
        // the swapped in piece has no room to spawn
        if (!currentPiece.fitsHere()) {
            gameState = GameState.GAME_OVER;
        }
    }

    public void update(long elapsedNanos) {
        updateBoss(elapsedNanos);
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
        // blocks above the board can't be kept, so locking there means the stack
        // has reached the top (game over)
        boolean lockedAboveBoard = false;
        for (int[] block : currentPiece.getBlocks()) {
            if (block[1] < 0) {
                lockedAboveBoard = true;
            }
        }

        board.lock(currentPiece);
        history.countPiece();
        upgrader.getHold().onPieceLocked();
        Shape lockedShape = currentPiece.getShape();
        boolean wasHardDropped = hardDropped;

        // when a piece locks, check if line(s) can be deleted
        List<Integer> cleared = board.clearFullRows();
        if (!cleared.isEmpty()) {
            int points = tracker.countClearedLines(cleared);
            recordClear(ClearSource.LINE_CLEAR, cleared.size(), cleared,
                    cleared.size() * Board.COLUMNS, points, lockedShape, wasHardDropped);
        }

        // replace the currentPiece with the next piece, after the lines are gone
        currentPiece = pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        active = true;
        hardDropped = false;
        timer.resetCounters();

        // if the new piece overlaps the stack where it spawns, the stack has
        // reached the top (game over)
        if (lockedAboveBoard || !currentPiece.fitsHere()) {
            gameState = GameState.GAME_OVER;
            return;
        }
        afterBoardChange();
    }

    // Saves the clear for later and gives its points to the boss
    private void recordClear(ClearSource source, int lines, List<Integer> rows, int blocks,
            int points, Shape piece, boolean wasHardDropped) {
        boolean duringBoss = isBossFightRunning();
        history.record(new ClearEvent(source, lines, rows, blocks, points,
                tracker.getLevel(), tracker.getLinesCleared(), piece,
                history.getPiecesSinceLastEvent(), wasHardDropped, board.isEmpty(), duringBoss));
        clearedRows.addAll(rows);
        // Combo is not implemented yet, a combo tracker would read the new event here

        if (duringBoss) {
            boss.addScore(points);
            if (boss.getOutcome() != BossOutcome.RUNNING) {
                finishBoss();
            }
        }
    }

    // Called after anything changes the board
    private void afterBoardChange() {
        // rows moved, make sure the falling piece doesn't overlap blocks
        currentPiece.moveUpUntilFits();
        resolveNextState();
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
        upgradeSelection.confirm();
        upgradeSelection = null;
        refreshNextShapes(); // See the Future may show more now
        resolveNextState();
    }

    // ---- Hammer, patch and flush ----

    public void useHammer() {
        Hammer hammer = upgrader.getHammer();
        if (canControl() && hammer.hasCharge()) {
            hammer.startAiming(board);
            gameState = GameState.AIM_HAMMER;
        }
    }

    public void usePatch() {
        Patch patch = upgrader.getPatch();
        if (canControl() && patch.hasCharge()) {
            patch.startAiming(board, currentPiece);
            gameState = GameState.AIM_PATCH;
        }
    }

    public boolean isAiming() {
        return gameState == GameState.AIM_HAMMER || gameState == GameState.AIM_PATCH;
    }

    public void moveCursor(int dCol, int dRow) {
        if (gameState == GameState.AIM_HAMMER) {
            upgrader.getHammer().moveCursor(dRow);
        } else if (gameState == GameState.AIM_PATCH) {
            upgrader.getPatch().moveCursor(dCol, dRow);
        }
    }

    public void markTarget() {
        if (gameState == GameState.AIM_PATCH) {
            upgrader.getPatch().mark();
        }
    }

    public void cancelTarget() {
        if (isAiming()) {
            gameState = GameState.PLAY;
        }
    }

    public void confirmTarget() {
        if (gameState == GameState.AIM_HAMMER) {
            confirmHammer();
        } else if (gameState == GameState.AIM_PATCH) {
            confirmPatch();
        }
    }

    private void confirmHammer() {
        Hammer hammer = upgrader.getHammer();
        if (!hammer.canConfirm()) {
            return;
        }
        int row = hammer.getAimRow();
        int blocks = hammer.confirm();
        int points = hammer.getPoints(blocks, tracker.getLevel());
        tracker.addPoints(points);
        // whole rows only move down, so destroying a row can't complete another one
        recordClear(ClearSource.HAMMER, 0, List.of(row), blocks, points, null, false);
        afterBoardChange();
    }

    private void confirmPatch() {
        Patch patch = upgrader.getPatch();
        if (!patch.canConfirm()) {
            return;
        }
        patch.confirm();
        // lines completed by the patch are normal line clears
        List<Integer> cleared = board.clearFullRows();
        if (!cleared.isEmpty()) {
            int points = tracker.countClearedLines(cleared);
            recordClear(ClearSource.PATCH, cleared.size(), cleared,
                    cleared.size() * Board.COLUMNS, points, null, false);
        }
        afterBoardChange();
    }

    public void useFlush() {
        Flush flush = upgrader.getFlush();
        if (!canControl() || !flush.canUse(board)) {
            return;
        }
        List<Integer> rows = flush.bottomRows(board);
        int blocks = flush.use(board);
        recordClear(ClearSource.FLUSH, 0, rows, blocks, 0, null, false);
        afterBoardChange();
    }

    // ---- Boss ----

    public boolean isBossFightRunning() {
        return boss != null && boss.getOutcome() == BossOutcome.RUNNING;
    }

    // The boss clock keeps running in menus so they can't be used to get more time.
    // Only a real pause (the game doesn't call update) stops it
    private boolean bossClockRuns() {
        return gameState == GameState.PLAY || gameState == GameState.SELECT_UPGRADE
                || isAiming();
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
        }
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

    public UpgradeSelection getUpgradeSelection() {
        return upgradeSelection;
    }

    public BedrockBoss getBoss() {
        return boss;
    }

    public ClearHistory getHistory() {
        return history;
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
