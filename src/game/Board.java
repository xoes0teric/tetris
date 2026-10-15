package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

/**
    The board is a 10x20 grid of cells
    Each cell contains a color if it's occupied and null if it's free

    Bosses can add unbreakable rows at the bottom. They are only a count and are
    never stored in cells, so nothing that edits cells (line clears, bombs) can
    remove them. Cells in those rows always stay null.
*/
public class Board {
    public static final int COLUMNS = 10;
    public static final int ROWS = 20;
    public static final Color GARBAGE_COLOR = Color.gray;

    private final Color[][] cells = new Color[ROWS][COLUMNS];
    private int unbreakableRows = 0;

    public Color getColor(int col, int row) {
        return cells[row][col];
    }

    // Rows that pieces can use, counted from the top
    public int getPlayableRows() {
        return ROWS - unbreakableRows;
    }

    public int getUnbreakableRows() {
        return unbreakableRows;
    }

    public boolean isUnbreakable(int row) {
        return row >= getPlayableRows() && row < ROWS;
    }

    public boolean isFreeCell(int col, int row) {
        if (col < 0 || col >= COLUMNS || row >= getPlayableRows()) {
            return false;
        }
        return row < 0 || cells[row][col] == null;
    }

    public void lock(Tetromino piece) {
        for (int[] block : piece.getBlocks()) {
            if (block[1] >= 0 && block[1] < getPlayableRows()) {
                cells[block[1]][block[0]] = piece.getShape().getColor();
            }
        }
    }

    public int countBlocks(int row) {
        int count = 0;
        for (Color cell : cells[row]) {
            if (cell != null) {
                ++count;
            }
        }
        return count;
    }

    private boolean isFullRow(int row) {
        for (Color cell : cells[row]) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }

    // Takes the row out and moves every row above it down by one
    private void removeRow(int row) {
        for (int above = row; above > 0; --above) {
            cells[above] = cells[above - 1];
        }
        cells[0] = new Color[COLUMNS];
    }

    // Clears and moves rows and also returns which rows were cleared
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

    // Removes a (not full) row like a line clear would, returns how many blocks it had
    public int destroyRow(int row) {
        if (row < 0 || row >= getPlayableRows()) {
            return 0;
        }
        int blocks = countBlocks(row);
        removeRow(row);
        return blocks;
    }

    // Adds unbreakable rows at the bottom and pushes the stack up to make room.
    // Returns false if blocks were pushed out of the top (the stack topped out)
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

    // Pushes the stack up and fills the lowest playable rows with garbage,
    // leaving the cell in gapCol free. Returns false if blocks were pushed out of the top
    public boolean addGarbageRows(int count, int gapCol) {
        boolean fits = true;
        for (int row = 0; row < count; ++row) {
            if (countBlocks(row) > 0) {
                fits = false;
            }
        }
        int playable = getPlayableRows();
        for (int row = 0; row < playable - count; ++row) {
            cells[row] = cells[row + count];
        }
        for (int row = playable - count; row < playable; ++row) {
            cells[row] = new Color[COLUMNS];
            for (int col = 0; col < COLUMNS; ++col) {
                if (col != gapCol) {
                    cells[row][col] = GARBAGE_COLOR;
                }
            }
        }
        return fits;
    }

    // Takes away all unbreakable rows, the stack moves back down
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
