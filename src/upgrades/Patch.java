package upgrades;

import game.Board;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

/**
 * Patch adds up to 3 connected blocks to free cells of the board.
 *
 * <p>The player moves a cursor and marks cells, each new cell has to touch one that
 * is already marked. A cell can only be marked if it is free, inside the playable
 * rows and not covered by the falling piece. Lines completed by the patch are
 * normal line clears, that is handled by the game, not here.
 * Cells are {column, row}.
 */
public class Patch {
    public static final int MAX_BLOCKS = 3;
    public static final Color COLOR = new Color(230, 230, 230);

    private final Upgrade upgrade = new Upgrade("Patch", 1, true, new String[]{
        "Press G, then place up to " + MAX_BLOCKS + " connected blocks into gaps of"
                + " your stack. Single use.",
    });
    private Board board;
    private Tetromino piece;
    private int cursorCol;
    private int cursorRow;
    private final List<int[]> marked = new ArrayList<>();

    /**
     * Returns the upgrade information for the selection screen.
     *
     * @return the upgrade
     */
    public Upgrade getUpgrade() {
        return upgrade;
    }

    /**
     * Checks whether the player has a patch to use.
     *
     * @return true if the patch has a charge
     */
    public boolean hasCharge() {
        return upgrade.getLevel() > 0;
    }

    /**
     * Starts aiming with no marked cells and the cursor at the bottom middle.
     *
     * @param board the board to patch
     * @param piece the falling piece, its cells can't be marked
     */
    public void startAiming(Board board, Tetromino piece) {
        this.board = board;
        this.piece = piece;
        cursorCol = Board.COLUMNS / 2;
        cursorRow = board.getPlayableRows() - 1;
        marked.clear();
    }

    /**
     * Moves the cursor, staying inside the playable part of the board.
     *
     * @param dCol the number of columns to move
     * @param dRow the number of rows to move
     */
    public void moveCursor(int dCol, int dRow) {
        cursorCol = Math.max(0, Math.min(Board.COLUMNS - 1, cursorCol + dCol));
        cursorRow = Math.max(0, Math.min(board.getPlayableRows() - 1, cursorRow + dRow));
    }

    /**
     * Checks whether a block can be placed in a cell: it is on the board, free and
     * not covered by the falling piece.
     *
     * @param col the column of the cell
     * @param row the row of the cell
     * @return true if the cell is open
     */
    private boolean isOpen(int col, int row) {
        return row >= 0 && board.isFreeCell(col, row) && !piece.occupies(col, row);
    }

    /**
     * Checks whether two cells are next to each other (not diagonal).
     *
     * @param a the first {column, row}
     * @param b the second {column, row}
     * @return true if the cells share a side
     */
    private static boolean touches(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1;
    }

    /**
     * Finds a cell in the marked list.
     *
     * @param col the column of the cell
     * @param row the row of the cell
     * @return the index in the marked list, or -1 if it isn't marked
     */
    private int indexOfMarked(int col, int row) {
        for (int i = 0; i < marked.size(); ++i) {
            if (marked.get(i)[0] == col && marked.get(i)[1] == row) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks whether a cell can be marked: there is room for another block, the cell
     * is open and it touches a marked cell (unless it is the first one).
     *
     * @param col the column of the cell
     * @param row the row of the cell
     * @return true if the cell can be added
     */
    private boolean canAdd(int col, int row) {
        if (marked.size() >= MAX_BLOCKS || !isOpen(col, row)) {
            return false;
        }
        if (marked.isEmpty()) {
            return true;
        }
        int[] cell = {col, row};
        for (int[] other : marked) {
            if (touches(cell, other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the marked cells stay connected after removing the one at index.
     *
     * @param index the index in the marked list
     * @return true if the cell can be unmarked
     */
    private boolean canRemove(int index) {
        List<int[]> rest = new ArrayList<>(marked);
        rest.remove(index);
        // with at most 3 cells, 2 left cells are connected only if they touch
        return rest.size() < 2 || touches(rest.get(0), rest.get(1));
    }

    /**
     * Returns the marked cells.
     *
     * @return a copy of the {column, row} of every marked cell
     */
    public List<int[]> getMarked() {
        return new ArrayList<>(marked);
    }

    /**
     * Marks the cell under the cursor, or unmarks it if it was already marked.
     * Does nothing if that would break the rules.
     */
    public void mark() {
        int index = indexOfMarked(cursorCol, cursorRow);
        if (index >= 0) {
            if (canRemove(index)) {
                marked.remove(index);
            }
        } else if (canAdd(cursorCol, cursorRow)) {
            marked.add(new int[]{cursorCol, cursorRow});
        }
    }

    /**
     * Checks whether the patch can be placed: there is a charge, at least one cell
     * is marked and all marked cells are still open.
     *
     * @return true if confirm() can be called
     */
    public boolean canConfirm() {
        if (!hasCharge() || marked.isEmpty()) {
            return false;
        }
        for (int[] cell : marked) {
            if (!isOpen(cell[0], cell[1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Places the marked blocks and spends the charge.
     * Only call this when canConfirm() is true.
     */
    public void confirm() {
        upgrade.spendCharge();
        for (int[] cell : marked) {
            board.fillCell(cell[0], cell[1], COLOR);
        }
        marked.clear();
    }

    /**
     * Returns the cell under the cursor.
     *
     * @return a list with the {column, row} of the cursor
     */
    public List<int[]> getCursorCells() {
        return List.of(new int[]{cursorCol, cursorRow});
    }

    /**
     * Checks whether pressing mark on the cursor would do something.
     *
     * @return true if the cursor cell can be marked or unmarked
     */
    public boolean isCursorValid() {
        return indexOfMarked(cursorCol, cursorRow) >= 0 || canAdd(cursorCol, cursorRow);
    }

    /**
     * Returns the blocks that will be placed.
     *
     * @return the {column, row} of every marked cell
     */
    public List<int[]> getTargetCells() {
        return getMarked();
    }

    /**
     * Returns the controls shown while aiming.
     *
     * @return the help text
     */
    public String getHelpText() {
        return "PATCH   ARROWS: move   SPACE: add/remove block   ENTER: place   ESC: cancel";
    }
}
