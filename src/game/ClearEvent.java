package game;

import java.util.List;
import tetromino.Shape;

/**
    Raw information about one action that removed blocks from the board

    This is only the data, nothing here decides how impressive a clear was.
    That can be calculated from the history later (for example to pick better upgrades).
    There is no combo field because the game has no combo yet, but a combo can be
    worked out from piecesSinceLastClear and ClearSource.isComboAction().
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

    public ClearSource getSource() {
        return source;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public List<Integer> getRows() {
        return rows;
    }

    public int getBlocksRemoved() {
        return blocksRemoved;
    }

    public int getScoreEarned() {
        return scoreEarned;
    }

    public int getLevel() {
        return level;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public Shape getPiece() {
        return piece;
    }

    public int getPiecesSinceLastClear() {
        return piecesSinceLastClear;
    }

    public boolean isHardDrop() {
        return hardDrop;
    }

    public boolean isBoardEmptyAfter() {
        return boardEmptyAfter;
    }

    public boolean isDuringBoss() {
        return duringBoss;
    }
}
