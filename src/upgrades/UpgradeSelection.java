package upgrades;

import java.util.List;

/**
 * The options of one upgrade selection screen and which one is highlighted.
 */
public class UpgradeSelection {
    private final List<Upgrade> options;
    private int selected = 0;

    /**
     * Creates a selection with the first option highlighted. The list is copied.
     *
     * @param options the upgrades to choose from
     */
    public UpgradeSelection(List<Upgrade> options) {
        this.options = List.copyOf(options);
    }

    /**
     * Returns the upgrades to choose from.
     *
     * @return an unmodifiable list of the options
     */
    public List<Upgrade> getOptions() {
        return options;
    }

    /**
     * Returns which option is highlighted.
     *
     * @return the index of the highlighted option
     */
    public int getSelected() {
        return selected;
    }

    /**
     * Moves the highlight left (-1) or right (+1), wrapping around.
     *
     * @param direction -1 for left, 1 for right
     */
    public void move(int direction) {
        if (!options.isEmpty()) {
            selected = Math.floorMod(selected + direction, options.size());
        }
    }

    /**
     * Highlights an option directly. Invalid indexes are ignored.
     *
     * @param index the option to highlight, starting at 0
     */
    public void select(int index) {
        if (index >= 0 && index < options.size()) {
            selected = index;
        }
    }

    /**
     * Applies the highlighted upgrade.
     *
     * @return the upgrade that was applied
     */
    public Upgrade confirm() {
        Upgrade chosen = options.get(selected);
        chosen.apply();
        return chosen;
    }
}
