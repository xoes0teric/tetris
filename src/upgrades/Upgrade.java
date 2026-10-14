package upgrades;

/**
    The information about one upgrade that the selection screen needs:
    its name, description and level

    Stackable upgrades go up one level each time they are picked.
    Single use upgrades get one charge when picked (level 1), which is spent when
    used (back to level 0). While the charge is unused the upgrade is maxed, so it
    isn't offered again.

    The upgrade classes (Hold, Hammer, ...) each have one of these and read
    its level to know what they are allowed to do.
*/
public class Upgrade {
    private final String name;
    // descriptions[level] says what picking the upgrade at that level does
    private final String[] descriptions;
    private final int maxLevel;
    private final boolean singleUse;
    private int level = 0;

    public Upgrade(String name, int maxLevel, boolean singleUse, String[] descriptions) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.singleUse = singleUse;
        this.descriptions = descriptions;
    }

    public String getName() {
        return name;
    }

    // What picking this upgrade (again) does
    public String getDescription() {
        return descriptions[Math.min(level, descriptions.length - 1)];
    }

    public int getLevel() {
        return level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isSingleUse() {
        return singleUse;
    }

    public boolean isAvailable() {
        return level < maxLevel;
    }

    // Raises the level by one, or gives the charge for single use upgrades
    public void apply() {
        if (level < maxLevel) {
            ++level;
        }
    }

    // Single use upgrades call this when they are used
    public void spendCharge() {
        level = 0;
    }
}
