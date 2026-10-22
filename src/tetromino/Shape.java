package tetromino;

import java.awt.Color;

/**
 * The seven Tetris shapes (I, O, T, S, Z, L and J).
 *
 * <p>Each shape sits in a square box and rotates around the box's center. The
 * cells are {column, row} positions inside that box. The first cell is the block
 * that is used to position the piece when it spawns.
 */
public enum Shape {

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

    /**
     * Creates a shape.
     *
     * @param color   the color its blocks are drawn in
     * @param boxSize the width and height of the box it rotates in, in blocks
     * @param cells   the {column, row} of each block inside the box, spawn block first
     */
    Shape(Color color, int boxSize, int[][] cells) {
        this.color = color;
        this.boxSize = boxSize;
        this.cells = cells;
    }

    /**
     * Returns the color the blocks of this shape are drawn in.
     *
     * @return the color of this shape
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the width and height of the square box the shape rotates in.
     *
     * @return the box size, in blocks
     */
    public int getBoxSize() {
        return boxSize;
    }

    /**
     * Returns the position of each block inside the box, in the starting rotation.
     *
     * @return the {column, row} of each block, the spawn block first
     */
    public int[][] getCells() {
        return cells;
    }
}
