package tetromino;

import java.awt.Color;

public enum Shape {

    // Each shape sits in a square box and rotates around the box's center.
    // cells are {column, row} inside the box. The first block is where the piece spawns.
    I(Color.cyan,       4, new int[][]{{1, 2}, {0, 2}, {2, 2}, {3, 2}}),
    // O turns around the center of its 2x2 box, so rotating it does nothing
    O(Color.yellow,     2, new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}),
    T(Color.magenta,    3, new int[][]{{1, 1}, {1, 0}, {0, 1}, {2, 1}}),
    S(Color.green,      3, new int[][]{{1, 1}, {1, 0}, {2, 1}, {2, 2}}),
    Z(Color.red,        3, new int[][]{{1, 1}, {1, 0}, {0, 1}, {0, 2}}),
    L(Color.orange,     3, new int[][]{{1, 1}, {1, 0}, {1, 2}, {2, 2}}),
    J(Color.blue,       3, new int[][]{{1, 1}, {1, 0}, {1, 2}, {0, 2}});

    private final Color color;
    private final int boxSize; // width and height of the box, in blocks
    private final int[][] cells; // position of each block inside the box, in blocks

    Shape(Color color, int boxSize, int[][] cells) {
        this.color = color;
        this.boxSize = boxSize;
        this.cells = cells;
    }

    public Color getColor() {
        return color;
    }

    public int getBoxSize() {
        return boxSize;
    }

    public int[][] getCells() {
        return cells;
    }
}
