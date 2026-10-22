package tetromino;

import game.Board;

/**
 * A piece that is falling on the board.
 *
 * <p>It combines a {@link Shape} with its current rotation and position. Everything
 * is in cells: (col, row) is where the shape's box is on the board. The piece only
 * moves or rotates when the new position is free on the board.
 */
public class Tetromino {
    private final Shape shape;
    private final Board board;
    private int[][] cells;
    private int col;
    private int row;

    /**
     * Creates a piece of the given shape in its starting rotation.
     *
     * @param shape the shape of the piece
     * @param board the board the piece moves on, used to check for collisions
     */
    public Tetromino(Shape shape, Board board) {
        this.shape = shape;
        this.board = board;
        this.cells = shape.getCells();
    }

    /**
     * Puts the piece so that its first block is at (firstCol, firstRow) on the board.
     *
     * @param firstCol the board column of the first block
     * @param firstRow the board row of the first block
     */
    public void place(int firstCol, int firstRow) {
        col = firstCol - cells[0][0];
        row = firstRow - cells[0][1];
    }

    /**
     * Returns the shape of this piece.
     *
     * @return the shape
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Returns the board position of each block, the first block first.
     *
     * @return the {column, row} of each block on the board
     */
    public int[][] getBlocks() {
        int[][] blocks = new int[cells.length][];
        for (int i = 0; i < cells.length; ++i) {
            blocks[i] = new int[]{col + cells[i][0], row + cells[i][1]};
        }
        return blocks;
    }

    /**
     * Checks if the cells, with the box at (boxCol, boxRow), are all free on the board.
     *
     * @param newCells the {column, row} of each block inside the box
     * @param boxCol   the board column of the box
     * @param boxRow   the board row of the box
     * @return true if every block lands on a free cell
     */
    private boolean fits(int[][] newCells, int boxCol, int boxRow) {
        for (int[] cell : newCells) {
            if (!board.isFreeCell(boxCol + cell[0], boxRow + cell[1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves the piece to the new cells and box position if they fit.
     *
     * @param newCells the {column, row} of each block inside the box
     * @param boxCol   the new board column of the box
     * @param boxRow   the new board row of the box
     * @return true if the piece moved, false if it didn't fit
     */
    private boolean tryPlace(int[][] newCells, int boxCol, int boxRow) {
        if (!fits(newCells, boxCol, boxRow)) {
            return false;
        }
        cells = newCells;
        col = boxCol;
        row = boxRow;
        return true;
    }

    /**
     * Checks whether the piece doesn't overlap any blocks where it is now.
     *
     * @return true if the piece fits at its current position
     */
    public boolean fitsHere() {
        return fits(cells, col, row);
    }

    /**
     * Checks whether the piece can fall one more row.
     *
     * @return true if the row below the piece is free
     */
    public boolean canMoveDown() {
        return fits(cells, col, row + 1);
    }

    /**
     * Returns how many rows the piece can fall before it lands (used for the ghost piece).
     *
     * @return the number of rows the piece can still fall
     */
    public int getDropDistance() {
        int distance = 0;
        while (fits(cells, col, row + distance + 1)) {
            ++distance;
        }
        return distance;
    }

    /**
     * Checks whether one of the piece's blocks is on the given board cell.
     *
     * @param boardCol the board column
     * @param boardRow the board row
     * @return true if the piece covers that cell
     */
    public boolean occupies(int boardCol, int boardRow) {
        for (int[] block : getBlocks()) {
            if (block[0] == boardCol && block[1] == boardRow) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the board changed under the piece (rows moved), moves it up until it
     * doesn't overlap any blocks. Rows above the board are always free.
     */
    public void moveUpUntilFits() {
        for (int i = 0; i <= Board.ROWS && !fits(cells, col, row); ++i) {
            --row;
        }
    }

    /**
     * Moves the piece by (dx, dy) cells if there is room.
     *
     * @param dx the number of columns to move, negative is left
     * @param dy the number of rows to move, positive is down
     * @return true if the piece moved
     */
    public boolean tryMove(int dx, int dy) {
        return tryPlace(cells, col + dx, row + dy);
    }

    /**
     * Rotates the piece 90 degrees clockwise inside its box.
     *
     * <p>If it doesn't fit, tries shifting the piece left, right, then up (wall kick).
     * The 2 cell kicks are for the I piece, which can stick out 2 cells past a wall.
     * If no position fits, the piece doesn't rotate.
     * THIS IS A SIMPLIFIED WALL KICK, REAL TETRIS USES KICK TABLES.
     */
    public void rotate() {
        int last = shape.getBoxSize() - 1;
        int[][] rotated = new int[cells.length][];
        for (int i = 0; i < cells.length; ++i) {
            rotated[i] = new int[]{last - cells[i][1], cells[i][0]};
        }
        int[][] kicks = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {-2, 0}, {2, 0}, {0, -2}}; // {dx, dy}
        for (int[] kick : kicks) {
            if (tryPlace(rotated, col + kick[0], row + kick[1])) {
                return;
            }
        }
    }
}
