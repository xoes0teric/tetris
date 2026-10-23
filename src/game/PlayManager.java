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
 * PlayManager manages the movement of pieces.
 * Everything is in cells, drawing in pixels is done by gui.Renderer.
 *
 * <p>It also decides which screen comes next (upgrade selection, boss) after
 * anything changes the board, see resolveNextState().
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

    /**
     * Creates a new game with a random piece order and the default boss.
     */
    public PlayManager() {
        this(new Random(), List.of(new BedrockBoss()));
    }

    /**
     * Creates a new game with a chosen random and bosses (useful for tests).
     *
     * @param random the random used for pieces and upgrade options
     * @param bosses the bosses to fight, in order
     */
    public PlayManager(Random random, List<BedrockBoss> bosses) {
        picker = new PiecePicker(board, random);
        upgrader = new Upgrader(random);
        upcomingBosses.addAll(bosses);

        // Set the starting piece
        currentPiece = pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
    }

    /**
     * Takes the next piece from the picker and updates the shown queue.
     *
     * @return the next piece, not placed yet
     */
    private Tetromino pickPiece() {
        Tetromino piece = picker.pickPiece();
        refreshNextShapes();
        return piece;
    }

    /**
     * Updates the upcoming pieces shown in the GUI, as many as See the Future allows.
     */
    private void refreshNextShapes() {
        nextShapes = List.copyOf(picker.peek(upgrader.getSeeTheFuture().getPreviewCount()));
    }

    /**
     * Checks whether the falling piece can be moved. This is only during normal play.
     *
     * @return true if the player controls the piece
     */
    private boolean canControl() {
        return active && gameState == GameState.PLAY;
    }

    /**
     * Rotates the falling piece clockwise.
     */
    public void rotate() {
        if (canControl()) {
            currentPiece.rotate();
        }
    }

    /**
     * Moves the falling piece one column to the left.
     */
    public void moveLeft() {
        if (canControl()) {
            currentPiece.tryMove(-1, 0);
        }
    }

    /**
     * Moves the falling piece one column to the right.
     */
    public void moveRight() {
        if (canControl()) {
            currentPiece.tryMove(1, 0);
        }
    }

    /**
     * Moves the falling piece one row down (soft drop).
     */
    public void moveDown() {
        // When moved down, reset the autoDropCounter
        if (canControl() && currentPiece.tryMove(0, 1)) {
            timer.resetAutoDropCounter();
        }
    }

    /**
     * Drops the falling piece as far as it goes. It locks on the next update.
     */
    public void hardDrop() {
        if (canControl()) {
            while (currentPiece.tryMove(0, 1)) {
            }
            active = false;
            hardDropped = true;
        }
    }

    /**
     * Swaps the falling piece with a hold slot (needs the Hold upgrade).
     * If the slot was empty, the next piece comes in.
     *
     * @param slot the hold slot to use, starting at 0
     */
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

    /**
     * Runs one frame of the game: the boss clock, gravity and locking.
     *
     * @param elapsedNanos the real time since the last update, in nanoseconds
     */
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

    /**
     * Locks the falling piece, clears full lines, spawns the next piece and
     * checks for game over.
     */
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

    /**
     * Saves the clear in the history, remembers its rows for the GUI effect
     * and gives its points to the boss.
     *
     * @param source         what removed the blocks
     * @param lines          full lines cleared by the normal line clear rules
     * @param rows           rows that were removed
     * @param blocks         number of blocks taken off the board
     * @param points         points earned
     * @param piece          the piece that locked, null if an upgrade caused it
     * @param wasHardDropped whether the piece was hard dropped
     */
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

    /**
     * Called after anything changes the board. Fixes the falling piece and
     * picks the next screen.
     */
    private void afterBoardChange() {
        // rows moved, make sure the falling piece doesn't overlap blocks
        currentPiece.moveUpUntilFits();
        resolveNextState();
    }

    /**
     * Picks the next screen: boss result, then upgrade selection, then boss intro,
     * then play.
     */
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
            boss.setLevel(getLevel());
            gameState = GameState.BOSS_INTRO;
            return;
        }
        gameState = GameState.PLAY;
    }

    // ---- Upgrade selection ----

    /**
     * Moves the highlight on the upgrade selection screen.
     *
     * @param direction -1 for left, 1 for right
     */
    public void moveUpgradeSelection(int direction) {
        if (gameState == GameState.SELECT_UPGRADE) {
            upgradeSelection.move(direction);
        }
    }

    /**
     * Highlights an upgrade option directly.
     *
     * @param index the option to highlight, starting at 0
     */
    public void selectUpgrade(int index) {
        if (gameState == GameState.SELECT_UPGRADE) {
            upgradeSelection.select(index);
        }
    }

    /**
     * Takes the highlighted upgrade and closes the selection screen.
     */
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

    /**
     * Starts aiming the hammer, if the player has one.
     */
    public void useHammer() {
        Hammer hammer = upgrader.getHammer();
        if (canControl() && hammer.hasCharge()) {
            hammer.startAiming(board);
            gameState = GameState.AIM_HAMMER;
        }
    }

    /**
     * Starts placing the patch blocks, if the player has a patch.
     */
    public void usePatch() {
        Patch patch = upgrader.getPatch();
        if (canControl() && patch.hasCharge()) {
            patch.startAiming(board, currentPiece);
            gameState = GameState.AIM_PATCH;
        }
    }

    /**
     * Checks whether the player is aiming the hammer or the patch.
     *
     * @return true while aiming
     */
    public boolean isAiming() {
        return gameState == GameState.AIM_HAMMER || gameState == GameState.AIM_PATCH;
    }

    /**
     * Moves the hammer or patch cursor. The hammer only uses the row.
     *
     * @param dCol the number of columns to move
     * @param dRow the number of rows to move
     */
    public void moveCursor(int dCol, int dRow) {
        if (gameState == GameState.AIM_HAMMER) {
            upgrader.getHammer().moveCursor(dRow);
        } else if (gameState == GameState.AIM_PATCH) {
            upgrader.getPatch().moveCursor(dCol, dRow);
        }
    }

    /**
     * Marks or unmarks the cell under the patch cursor.
     */
    public void markTarget() {
        if (gameState == GameState.AIM_PATCH) {
            upgrader.getPatch().mark();
        }
    }

    /**
     * Stops aiming without using the upgrade.
     */
    public void cancelTarget() {
        if (isAiming()) {
            gameState = GameState.PLAY;
        }
    }

    /**
     * Uses the hammer or patch on the aimed target.
     */
    public void confirmTarget() {
        if (gameState == GameState.AIM_HAMMER) {
            confirmHammer();
        } else if (gameState == GameState.AIM_PATCH) {
            confirmPatch();
        }
    }

    /**
     * Smashes the aimed row and gives points for its blocks.
     */
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

    /**
     * Places the patch blocks and clears the lines they complete.
     */
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

    /**
     * Removes the bottom rows with the flush, if the player has one.
     */
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

    /**
     * Checks whether a boss fight is going on.
     *
     * @return true while a boss fight is running
     */
    public boolean isBossFightRunning() {
        return boss != null && boss.getOutcome() == BossOutcome.RUNNING;
    }

    /**
     * Checks whether the boss clock should run. It keeps running in menus so they
     * can't be used to get more time. Only a real pause (the game doesn't call
     * update) stops it.
     *
     * @return true if the boss clock should count down
     */
    private boolean bossClockRuns() {
        return gameState == GameState.PLAY || gameState == GameState.SELECT_UPGRADE
                || isAiming();
    }

    /**
     * Counts down the boss clock and ends the fight when the time runs out.
     *
     * @param elapsedNanos the real time since the last update, in nanoseconds
     */
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

    /**
     * Ends the boss fight. If the boss was beaten its rows are removed and it
     * comes back after another RESPAWN_LINES lines. The result screen is shown next.
     */
    private void finishBoss() {
        if (boss.getOutcome() == BossOutcome.WON) {
            boss.end(board);
            currentPiece.moveUpUntilFits();
            upcomingBosses.add(boss.respawn(getLines()));
        }
        bossResultPending = true;
    }

    /**
     * Starts the fight from the intro screen, or closes the result screen.
     */
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

    /**
     * Returns the rows cleared since the last call and forgets them.
     *
     * @return the cleared rows, for the GUI effect
     */
    public List<Integer> takeClearedRows() {
        List<Integer> rows = new ArrayList<>(clearedRows);
        clearedRows.clear();
        return rows;
    }

    /**
     * Returns the board.
     *
     * @return the board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the falling piece.
     *
     * @return the current piece
     */
    public Tetromino getCurrentPiece() {
        return currentPiece;
    }

    /**
     * Returns the upcoming pieces, as many as See the Future shows.
     *
     * @return the next shapes, the very next one first
     */
    public List<Shape> getNextShapes() {
        return nextShapes;
    }

    /**
     * Returns the screen the game is on.
     *
     * @return the current game state
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Checks whether the game has ended.
     *
     * @return true if the game is over
     */
    public boolean isGameOver() {
        return gameState == GameState.GAME_OVER;
    }

    /**
     * Returns the object that owns all upgrades.
     *
     * @return the upgrader
     */
    public Upgrader getUpgrader() {
        return upgrader;
    }

    /**
     * Returns the open upgrade selection.
     *
     * @return the selection, or null if none is open
     */
    public UpgradeSelection getUpgradeSelection() {
        return upgradeSelection;
    }

    /**
     * Returns the boss being introduced, fought or just finished.
     *
     * @return the boss, or null if there is none right now
     */
    public BedrockBoss getBoss() {
        return boss;
    }

    /**
     * Returns the history of all clears in the game.
     *
     * @return the clear history
     */
    public ClearHistory getHistory() {
        return history;
    }

    /**
     * Returns the current level.
     *
     * @return the level
     */
    public int getLevel() {
        return tracker.getLevel();
    }

    /**
     * Returns the lines cleared in the whole game.
     *
     * @return the total number of cleared lines
     */
    public int getLines() {
        return tracker.getLinesCleared();
    }

    /**
     * Returns the total score.
     *
     * @return the score
     */
    public int getScore() {
        return tracker.getScore();
    }
}
