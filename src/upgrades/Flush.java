package upgrades;

import game.Board;
import java.util.ArrayList;
import java.util.List;

/**
    Flush removes the bottom 3 playable rows and the stack moves down 3 rows

    It is an emergency button: it gives no points and isn't a line clear.
    Unbreakable rows are below the playable rows, so flush never touches them.
*/
public class Flush {
    public static final int ROWS = 3;

    private final Upgrade upgrade = new Upgrade("Flush", 1, true, new String[]{
        "Press F to wash away the bottom " + ROWS + " rows of your stack."
                + " No points. Single use.",
    });

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public boolean hasCharge() {
        return upgrade.getLevel() > 0;
    }

    // Only usable when it would remove something, so the charge isn't wasted
    public boolean canUse(Board board) {
        if (!hasCharge()) {
            return false;
        }
        for (int row : bottomRows(board)) {
            if (board.countBlocks(row) > 0) {
                return true;
            }
        }
        return false;
    }

    // The rows flush removes, from the bottom up
    public List<Integer> bottomRows(Board board) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 1; i <= ROWS && board.getPlayableRows() - i >= 0; ++i) {
            rows.add(board.getPlayableRows() - i);
        }
        return rows;
    }

    // Removes the rows, spends the charge and returns how many blocks were removed
    public int use(Board board) {
        upgrade.spendCharge();
        return board.removeBottomRows(ROWS);
    }
}
