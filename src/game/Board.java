package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

// The board is a 10x20 grid of cells
public class Board {
    public static final int COLUMNS = 10;
    public static final int ROWS = 20;

    // Each cell contains a color if it's occupied and null if it's free
    private final Color[][] cells = new Color[ROWS][COLUMNS];

    public Color getColor(int col, int row) {
        return cells[row][col];
    }

    public boolean isFreeCell(int col, int row) {
        if (col < 0 || col >= COLUMNS || row >= ROWS) {
            return false;
        }
        return row < 0 || cells[row][col] == null;
    }

    public void lock(Tetromino piece) {
        for (int[] block : piece.getBlocks()) {
            if (block[1] >= 0) {
                cells[block[1]][block[0]] = piece.getShape().getColor();
            }
        }
    }

    private boolean isFullRow(int row) {
        for (Color cell : cells[row]) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }

    // Clears and moves rows and also returns which rows were cleared
    public List<Integer> clearFullRows() {
        List<Integer> cleared = new ArrayList<>();
        for (int row = 0; row < ROWS; ++row) {
            if (isFullRow(row)) {
                cleared.add(row);
                for (int above = row; above > 0; --above) {
                    cells[above] = cells[above - 1];
                }
                cells[0] = new Color[COLUMNS];
            }
        }
        return cleared;
    }

}
