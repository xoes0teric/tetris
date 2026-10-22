package game;

import java.util.List;
import tetromino.Shape;

/**
 * Raw information about one action that removed blocks from the board.
 *
 * <p>This is only the data, nothing here decides how impressive a clear was.
 * That can be calculated from the history later (for example to pick better upgrades).
 * There is no combo field because the game has no combo yet, but a combo can be
 * worked out from piecesSinceLastClear and ClearSource.isComboAction().
 */
public class ClearEvent {
    private final ClearSource source; // what removed the blocks
    private final int linesCleared; // full lines cleared by the normal line clear rules
    private final List<Integer> rows; // rows that were removed
    private final int blocksRemoved; // number of blocks taken off the board
    private final int scoreEarned; // points earned by this action
    private final int level; // the level used for the score
    private final int totalLines; // lines cleared in the whole game after this action
    private final Shape piece; // the piece that locked, null if an upgrade caused it
    private final int piecesSinceLastClear; // pieces locked since the previous event
    private final boolean hardDrop; // whether the piece was hard dropped
    private final boolean boardEmptyAfter; // whether the board was empty after (perfect clear)
    private final boolean duringBoss; // whether a boss fight was running

    /**
     * Creates an event. The list of rows is copied.
     *
     * @param source               what removed the blocks
     * @param linesCleared         full lines cleared by the normal line clear rules
     * @param rows                 rows that were removed
     * @param blocksRemoved        number of blocks taken off the board
     * @param scoreEarned          points earned by this action
     * @param level                the level used for the score
     * @param totalLines           lines cleared in the whole game after this action
     * @param piece                the piece that locked, null if an upgrade caused it
     * @param piecesSinceLastClear pieces locked since the previous event
     * @param hardDrop             whether the piece was hard dropped
     * @param boardEmptyAfter      whether the board was empty after (perfect clear)
     * @param duringBoss           whether a boss fight was running
     */
    public ClearEvent(ClearSource source, int linesCleared, List<Integer> rows,
            int blocksRemoved, int scoreEarned, int level, int totalLines, Shape piece,
            int piecesSinceLastClear, boolean hardDrop, boolean boardEmptyAfter,
            boolean duringBoss) {
        this.source = source;
        this.linesCleared = linesCleared;
        this.rows = List.copyOf(rows);
        this.blocksRemoved = blocksRemoved;
        this.scoreEarned = scoreEarned;
        this.level = level;
        this.totalLines = totalLines;
        this.piece = piece;
        this.piecesSinceLastClear = piecesSinceLastClear;
        this.hardDrop = hardDrop;
        this.boardEmptyAfter = boardEmptyAfter;
        this.duringBoss = duringBoss;
    }

    /**
     * Returns what removed the blocks.
     *
     * @return the source of the clear
     */
    public ClearSource getSource() {
        return source;
    }

    /**
     * Returns the full lines cleared by the normal line clear rules.
     *
     * @return the number of lines cleared
     */
    public int getLinesCleared() {
        return linesCleared;
    }

    /**
     * Returns the rows that were removed.
     *
     * @return an unmodifiable list of the removed rows
     */
    public List<Integer> getRows() {
        return rows;
    }

    /**
     * Returns the number of blocks taken off the board.
     *
     * @return the number of removed blocks
     */
    public int getBlocksRemoved() {
        return blocksRemoved;
    }

    /**
     * Returns the points earned by this action.
     *
     * @return the points earned
     */
    public int getScoreEarned() {
        return scoreEarned;
    }

    /**
     * Returns the level used to calculate the score.
     *
     * @return the level at the time of the clear
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the lines cleared in the whole game after this action.
     *
     * @return the total number of lines cleared
     */
    public int getTotalLines() {
        return totalLines;
    }

    /**
     * Returns the shape of the piece that locked.
     *
     * @return the shape, or null if an upgrade caused the clear
     */
    public Shape getPiece() {
        return piece;
    }

    /**
     * Returns the number of pieces locked since the previous event.
     *
     * @return the pieces locked since the last clear
     */
    public int getPiecesSinceLastClear() {
        return piecesSinceLastClear;
    }

    /**
     * Returns whether the piece was hard dropped.
     *
     * @return true if the piece was hard dropped
     */
    public boolean isHardDrop() {
        return hardDrop;
    }

    /**
     * Returns whether the board was empty after the clear (a perfect clear).
     *
     * @return true if the board was empty afterwards
     */
    public boolean isBoardEmptyAfter() {
        return boardEmptyAfter;
    }

    /**
     * Returns whether a boss fight was running during the clear.
     *
     * @return true if it happened during a boss fight
     */
    public boolean isDuringBoss() {
        return duringBoss;
    }
}
