package upgrades;

import game.Board;
import java.util.ArrayList;
import java.util.List;

/**
    Hammer destroys one chosen row. The rows above move down like after a line clear.

    Destroying a row is not a line clear: it doesn't count towards lines or levels.
    It gives points for every destroyed block instead.
    It can never reach unbreakable rows because it can only aim at playable rows.
    Cells are {column, row}
*/
public class Hammer {
    private static final int POINTS_PER_BLOCK = 1; // times the level

    private final Upgrade upgrade = new Upgrade("Hammer", 1, true, new String[]{
        "Press H, then pick a row to smash. Its blocks are destroyed and"
                + " the rows above fall down. Single use.",
    });
    private Board board;
    private int aimRow;

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public boolean hasCharge() {
        return upgrade.getLevel() > 0;
    }

    public void startAiming(Board board) {
        this.board = board;
        aimRow = board.getPlayableRows() - 1;
    }

    public void moveCursor(int dRow) {
        aimRow = Math.max(0, Math.min(board.getPlayableRows() - 1, aimRow + dRow));
    }

    public int getAimRow() {
        return aimRow;
    }

    public boolean canConfirm() {
        return hasCharge() && aimRow < board.getPlayableRows() && board.countBlocks(aimRow) > 0;
    }

    // Smashes the row, spends the charge and returns how many blocks were destroyed.
    // Only call this when canConfirm() is true
    public int confirm() {
        upgrade.spendCharge();
        return board.destroyRow(aimRow);
    }

    public int getPoints(int blocksDestroyed, int level) {
        return blocksDestroyed * POINTS_PER_BLOCK * level;
    }

    // The whole aimed row
    public List<int[]> getCursorCells() {
        List<int[]> cells = new ArrayList<>();
        for (int col = 0; col < Board.COLUMNS; ++col) {
            cells.add(new int[]{col, aimRow});
        }
        return cells;
    }

    public boolean isCursorValid() {
        return canConfirm();
    }

    // The blocks that will be destroyed
    public List<int[]> getTargetCells() {
        List<int[]> cells = new ArrayList<>();
        for (int col = 0; col < Board.COLUMNS; ++col) {
            if (board.getColor(col, aimRow) != null) {
                cells.add(new int[]{col, aimRow});
            }
        }
        return cells;
    }

    public String getHelpText() {
        return "HAMMER   UP/DOWN: pick row   ENTER: smash   ESC: cancel";
    }
}
