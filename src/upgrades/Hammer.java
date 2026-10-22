package upgrades;

import game.Board;
import java.util.ArrayList;
import java.util.List;

/**
 * Hammer destroys one chosen row. The rows above move down like after a line clear.
 *
 * <p>Destroying a row is not a line clear: it doesn't count towards lines or levels.
 * It gives points for every destroyed block instead.
 * It can never reach unbreakable rows because it can only aim at playable rows.
 * Cells are {column, row}.
 */
public class Hammer {
    private static final int POINTS_PER_BLOCK = 1; // times the level

    private final Upgrade upgrade = new Upgrade("Hammer", 1, true, new String[]{
        "Press H, then pick a row to smash. Its blocks are destroyed and"
                + " the rows above fall down. Single use.",
    });
    private Board board;
    private int aimRow;

    /**
     * Returns the upgrade information for the selection screen.
     *
     * @return the upgrade
     */
    public Upgrade getUpgrade() {
        return upgrade;
    }

    /**
     * Checks whether the player has a hammer to use.
     *
     * @return true if the hammer has a charge
     */
    public boolean hasCharge() {
        return upgrade.getLevel() > 0;
    }

    /**
     * Starts aiming, with the cursor on the bottom playable row.
     *
     * @param board the board to aim at
     */
    public void startAiming(Board board) {
        this.board = board;
        aimRow = board.getPlayableRows() - 1;
    }

    /**
     * Moves the aimed row up or down, staying inside the playable rows.
     *
     * @param dRow the number of rows to move, positive is down
     */
    public void moveCursor(int dRow) {
        aimRow = Math.max(0, Math.min(board.getPlayableRows() - 1, aimRow + dRow));
    }

    /**
     * Returns the row that is aimed at.
     *
     * @return the aimed row
     */
    public int getAimRow() {
        return aimRow;
    }

    /**
     * Checks whether the aimed row can be smashed: there is a charge and the row has blocks.
     *
     * @return true if confirm() can be called
     */
    public boolean canConfirm() {
        return hasCharge() && aimRow < board.getPlayableRows() && board.countBlocks(aimRow) > 0;
    }

    /**
     * Smashes the row and spends the charge.
     * Only call this when canConfirm() is true.
     *
     * @return the number of blocks that were destroyed
     */
    public int confirm() {
        upgrade.spendCharge();
        return board.destroyRow(aimRow);
    }

    /**
     * Calculates the points for smashing a row.
     *
     * @param blocksDestroyed the number of blocks destroyed
     * @param level           the current level
     * @return the points earned
     */
    public int getPoints(int blocksDestroyed, int level) {
        return blocksDestroyed * POINTS_PER_BLOCK * level;
    }

    /**
     * Returns the cells of the cursor, which is the whole aimed row.
     *
     * @return the {column, row} of every cell in the aimed row
     */
    public List<int[]> getCursorCells() {
        List<int[]> cells = new ArrayList<>();
        for (int col = 0; col < Board.COLUMNS; ++col) {
            cells.add(new int[]{col, aimRow});
        }
        return cells;
    }

    /**
     * Checks whether the cursor is on a row that can be smashed.
     *
     * @return true if the aim is valid
     */
    public boolean isCursorValid() {
        return canConfirm();
    }

    /**
     * Returns the blocks that will be destroyed.
     *
     * @return the {column, row} of every block in the aimed row
     */
    public List<int[]> getTargetCells() {
        List<int[]> cells = new ArrayList<>();
        for (int col = 0; col < Board.COLUMNS; ++col) {
            if (board.getColor(col, aimRow) != null) {
                cells.add(new int[]{col, aimRow});
            }
        }
        return cells;
    }

    /**
     * Returns the controls shown while aiming.
     *
     * @return the help text
     */
    public String getHelpText() {
        return "HAMMER   UP/DOWN: pick row   ENTER: smash   ESC: cancel";
    }
}
