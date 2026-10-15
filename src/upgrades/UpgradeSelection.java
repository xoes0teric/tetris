package upgrades;

import java.util.List;

/**
    The options of one upgrade selection screen and which one is highlighted
*/
public class UpgradeSelection {
    private final List<Upgrade> options;
    private int selected = 0;

    public UpgradeSelection(List<Upgrade> options) {
        this.options = List.copyOf(options);
    }

    public List<Upgrade> getOptions() {
        return options;
    }

    public int getSelected() {
        return selected;
    }

    // Moves the highlight left (-1) or right (+1), wrapping around
    public void move(int direction) {
        if (!options.isEmpty()) {
            selected = Math.floorMod(selected + direction, options.size());
        }
    }

    public void select(int index) {
        if (index >= 0 && index < options.size()) {
            selected = index;
        }
    }

    public Upgrade getSelectedUpgrade() {
        return options.get(selected);
    }
}
