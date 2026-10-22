package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

/**
 * The board is a 10x20 grid of cells.
 * Each cell contains a color if it's occupied and null if it's free.
 *
 * <p>Bosses can add unbreakable rows at the bottom. They are only a count and are
 * never stored in cells, so nothing that edits cells (line clears, hammer, patch,
 * flush) can remove them. Cells in those rows always stay null.
 */
public class Board {
    public static final int COLUMNS = 10;
    public static final int ROWS = 20;

    private final Color[][] cells = new Color[ROWS][COLUMNS];
    private int unbreakableRows = 0;

    /**
     * Returns the color of the block in a cell.
     *
     * @param col the column of the cell
     * @param row the row of the cell
     * @return the color of the block, or null if the cell is free
     */
    public Color getColor(int col, int row) {
        return cells[row][col];
    }

    /**
     * Returns the number of rows that pieces can use, counted from the top.
     *
     * @return the number of playable rows
     */
    public int getPlayableRows() {
        return ROWS - unbreakableRows;
    }

    /**
     * Returns the number of unbreakable rows at the bottom of the board.
     *
     * @return the number of unbreakable rows
     */
    public int getUnbreakableRows() {
        return unbreakableRows;
    }

    /**
     * Checks whether a row is one of the unbreakable rows.
     *
     * @param row the row to check
     * @return true if the row is unbreakable
     */
    public boolean isUnbreakable(int row) {
        return row >= getPlayableRows() && row < ROWS;
    }

    /**
     * Checks whether a block can be in a cell. Cells above the board are free,
     * cells next to or below the playable rows are not.
     *
     * @param col the column of the cell
     * @param row the row of the cell, negative for above the board
     * @return true if the cell is free
     */
    public boolean isFreeCell(int col, int row) {
        if (col < 0 || col >= COLUMNS || row >= getPlayableRows()) {
            return false;
        }
        return row < 0 || cells[row][col] == null;
    }

    /**
     * Stores the blocks of a piece on the board. Blocks above the board are lost.
     *
     * @param piece the piece to lock
     */
    public void lock(Tetromino piece) {
        for (int[] block : piece.getBlocks()) {
            if (block[1] >= 0 && block[1] < getPlayableRows()) {
                cells[block[1]][block[0]] = piece.getShape().getColor();
            }
        }
    }

    /**
     * Puts a single block on a free cell.
     *
     * @param col   the column of the cell
     * @param row   the row of the cell
     * @param color the color of the block
     * @return true if the block was placed
     */
    public boolean fillCell(int col, int row, Color color) {
        if (row < 0 || !isFreeCell(col, row)) {
            return false;
        }
        cells[row][col] = color;
        return true;
    }

    /**
     * Counts the blocks in a row.
     *
     * @param row the row to count
     * @return the number of occupied cells in the row
     */
    public int countBlocks(int row) {
        int count = 0;
        for (Color cell : cells[row]) {
            if (cell != null) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Checks whether there are no blocks in the playable rows (a perfect clear).
     *
     * @return true if the board is empty
     */
    public boolean isEmpty() {
        for (int row = 0; row < getPlayableRows(); ++row) {
            if (countBlocks(row) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether every cell in a row is occupied.
     *
     * @param row the row to check
     * @return true if the row is full
     */
    private boolean isFullRow(int row) {
        for (Color cell : cells[row]) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes the row out and moves every row above it down by one.
     *
     * @param row the row to remove
     */
    private void removeRow(int row) {
        for (int above = row; above > 0; --above) {
            cells[above] = cells[above - 1];
        }
        cells[0] = new Color[COLUMNS];
    }

    /**
     * Clears all full rows and moves the rows above them down.
     *
     * @return the rows that were cleared
     */
    public List<Integer> clearFullRows() {
        List<Integer> cleared = new ArrayList<>();
        for (int row = 0; row < getPlayableRows(); ++row) {
            if (isFullRow(row)) {
                cleared.add(row);
                removeRow(row);
            }
        }
        return cleared;
    }

    /**
     * Removes a (not necessarily full) row like a line clear would.
     *
     * @param row the row to destroy, only playable rows can be destroyed
     * @return the number of blocks the row had
     */
    public int destroyRow(int row) {
        if (row < 0 || row >= getPlayableRows()) {
            return 0;
        }
        int blocks = countBlocks(row);
        removeRow(row);
        return blocks;
    }

    /**
     * Removes the lowest playable rows, everything above moves down.
     *
     * @param count the number of rows to remove
     * @return the number of blocks that were removed
     */
    public int removeBottomRows(int count) {
        int blocks = 0;
        int rows = Math.min(count, getPlayableRows());
        for (int i = 0; i < rows; ++i) {
            int bottom = getPlayableRows() - 1;
            blocks += countBlocks(bottom);
            removeRow(bottom);
        }
        return blocks;
    }

    /**
     * Adds unbreakable rows at the bottom and pushes the stack up to make room.
     *
     * @param count the number of unbreakable rows to add
     * @return false if blocks were pushed out of the top (the stack topped out)
     * @throws IllegalArgumentException if count is negative or leaves no playable rows
     */
    public boolean addUnbreakableRows(int count) {
        if (count < 0 || count >= getPlayableRows()) {
            throw new IllegalArgumentException("Can't add " + count + " unbreakable rows");
        }
        boolean fits = true;
        for (int row = 0; row < count; ++row) {
            if (countBlocks(row) > 0) {
                fits = false;
            }
        }
        int oldPlayable = getPlayableRows();
        for (int row = 0; row < oldPlayable - count; ++row) {
            cells[row] = cells[row + count];
        }
        for (int row = oldPlayable - count; row < ROWS; ++row) {
            cells[row] = new Color[COLUMNS];
        }
        unbreakableRows += count;
        return fits;
    }

    /**
     * Takes away all unbreakable rows, the stack moves back down.
     */
    public void removeUnbreakableRows() {
        int count = unbreakableRows;
        unbreakableRows = 0;
        for (int row = ROWS - 1; row >= count; --row) {
            cells[row] = cells[row - count];
        }
        for (int row = 0; row < count; ++row) {
            cells[row] = new Color[COLUMNS];
        }
    }

}
