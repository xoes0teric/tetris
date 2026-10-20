package upgrades;

import game.Board;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

/**
    Patch adds up to 3 connected blocks to free cells of the board

    The player moves a cursor and marks cells, each new cell has to touch one that
    is already marked. A cell can only be marked if it is free, inside the playable
    rows and not covered by the falling piece. Lines completed by the patch are
    normal line clears, that is handled by the game, not here.
    Cells are {column, row}
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

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public boolean hasCharge() {
        return upgrade.getLevel() > 0;
    }

    public void startAiming(Board board, Tetromino piece) {
        this.board = board;
        this.piece = piece;
        cursorCol = Board.COLUMNS / 2;
        cursorRow = board.getPlayableRows() - 1;
        marked.clear();
    }

    public void moveCursor(int dCol, int dRow) {
        cursorCol = Math.max(0, Math.min(Board.COLUMNS - 1, cursorCol + dCol));
        cursorRow = Math.max(0, Math.min(board.getPlayableRows() - 1, cursorRow + dRow));
    }

    private boolean isOpen(int col, int row) {
        return row >= 0 && board.isFreeCell(col, row) && !piece.occupies(col, row);
    }

    private static boolean touches(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1;
    }

    private int indexOfMarked(int col, int row) {
        for (int i = 0; i < marked.size(); ++i) {
            if (marked.get(i)[0] == col && marked.get(i)[1] == row) {
                return i;
            }
        }
        return -1;
    }

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

    // Whether the marked cells stay connected after removing the one at index
    private boolean canRemove(int index) {
        List<int[]> rest = new ArrayList<>(marked);
        rest.remove(index);
        // with at most 3 cells, 2 left cells are connected only if they touch
        return rest.size() < 2 || touches(rest.get(0), rest.get(1));
    }

    public List<int[]> getMarked() {
        return new ArrayList<>(marked);
    }

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

    // Places the marked blocks and spends the charge.
    // Only call this when canConfirm() is true
    public void confirm() {
        upgrade.spendCharge();
        for (int[] cell : marked) {
            board.fillCell(cell[0], cell[1], COLOR);
        }
        marked.clear();
    }

    public List<int[]> getCursorCells() {
        return List.of(new int[]{cursorCol, cursorRow});
    }

    public boolean isCursorValid() {
        return indexOfMarked(cursorCol, cursorRow) >= 0 || canAdd(cursorCol, cursorRow);
    }

    // The blocks that will be placed
    public List<int[]> getTargetCells() {
        return getMarked();
    }

    public String getHelpText() {
        return "PATCH   ARROWS: move   SPACE: add/remove block   ENTER: place   ESC: cancel";
    }
}
