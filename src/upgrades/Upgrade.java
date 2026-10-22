package upgrades;

/**
 * The information about one upgrade that the selection screen needs:
 * its name, description and level.
 *
 * <p>Stackable upgrades go up one level each time they are picked.
 * Single use upgrades get one charge when picked (level 1), which is spent when
 * used (back to level 0). While the charge is unused the upgrade is maxed, so it
 * isn't offered again.
 *
 * <p>The upgrade classes (Hold, Hammer, ...) each have one of these and read
 * its level to know what they are allowed to do.
 */
public class Upgrade {
    private final String name;
    // descriptions[level] says what picking the upgrade at that level does
    private final String[] descriptions;
    private final int maxLevel;
    private final boolean singleUse;
    private int level = 0;

    /**
     * Creates an upgrade at level 0 (not picked yet).
     *
     * @param name         the name shown on the selection screen
     * @param maxLevel     the highest level it can reach
     * @param singleUse    whether it is spent when used
     * @param descriptions descriptions[level] says what picking it at that level does
     */
    public Upgrade(String name, int maxLevel, boolean singleUse, String[] descriptions) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.singleUse = singleUse;
        this.descriptions = descriptions;
    }

    /**
     * Returns the name shown on the selection screen.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns what picking this upgrade (again) does.
     *
     * @return the description for the current level
     */
    public String getDescription() {
        return descriptions[Math.min(level, descriptions.length - 1)];
    }

    /**
     * Returns the current level. For single use upgrades 1 means it has a charge.
     *
     * @return the level, 0 if not picked
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the highest level the upgrade can reach.
     *
     * @return the max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Returns whether the upgrade is spent when used.
     *
     * @return true for single use upgrades
     */
    public boolean isSingleUse() {
        return singleUse;
    }

    /**
     * Checks whether the upgrade can still be offered.
     *
     * @return true if it is not at its max level
     */
    public boolean isAvailable() {
        return level < maxLevel;
    }

    /**
     * Raises the level by one, or gives the charge for single use upgrades.
     */
    public void apply() {
        if (level < maxLevel) {
            ++level;
        }
    }

    /**
     * Uses up the charge (back to level 0). Single use upgrades call this when
     * they are used.
     */
    public void spendCharge() {
        level = 0;
    }
}
