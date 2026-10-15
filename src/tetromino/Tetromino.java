package tetromino;

import game.Board;

// Everything is in cells: (col, row) is where the shape's box is on the board
public class Tetromino {
    private final Shape shape;
    private final Board board;
    private int[][] cells;
    private int col;
    private int row;

    public Tetromino(Shape shape, Board board) {
        this.shape = shape;
        this.board = board;
        this.cells = shape.getCells();
    }

    // Puts the piece so that its first block is at (firstCol, firstRow) on the board
    public void place(int firstCol, int firstRow) {
        col = firstCol - cells[0][0];
        row = firstRow - cells[0][1];
    }

    public Shape getShape() {
        return shape;
    }

    // Board position {column, row} of each block, the first block first
    public int[][] getBlocks() {
        int[][] blocks = new int[cells.length][];
        for (int i = 0; i < cells.length; ++i) {
            blocks[i] = new int[]{col + cells[i][0], row + cells[i][1]};
        }
        return blocks;
    }

    // Checks if the cells, with the box at (boxCol, boxRow), are all free on the board
    private boolean fits(int[][] newCells, int boxCol, int boxRow) {
        for (int[] cell : newCells) {
            if (!board.isFreeCell(boxCol + cell[0], boxRow + cell[1])) {
                return false;
            }
        }
        return true;
    }

    // Moves the piece to the new cells and box position if they fit, returns whether it moved
    private boolean tryPlace(int[][] newCells, int boxCol, int boxRow) {
        if (!fits(newCells, boxCol, boxRow)) {
            return false;
        }
        cells = newCells;
        col = boxCol;
        row = boxRow;
        return true;
    }

    public boolean canMoveDown() {
        return fits(cells, col, row + 1);
    }

    // How many rows the piece can fall before it lands (used for the ghost piece)
    public int getDropDistance() {
        int distance = 0;
        while (fits(cells, col, row + distance + 1)) {
            ++distance;
        }
        return distance;
    }

    // If the board changed under the piece (rows moved), moves it up until it
    // doesn't overlap any blocks. Rows above the board are always free
    public void moveUpUntilFits() {
        for (int i = 0; i <= Board.ROWS && !fits(cells, col, row); ++i) {
            --row;
        }
    }

    // Moves the piece by (dx, dy) cells if there is room
    public boolean tryMove(int dx, int dy) {
        return tryPlace(cells, col + dx, row + dy);
    }

    // Rotates 90 degrees clockwise inside the box. If it doesn't fit, tries
    // shifting the piece left, right, then up (wall kick). The 2 cell kicks
    // are for the I piece, which can stick out 2 cells past a wall
    // THIS IS A SIMPLIFIED WALL KICK, REAL TETRIS USES KICK TABLES
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
