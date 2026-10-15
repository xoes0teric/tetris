package upgrades;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
    Upgrader owns all upgrades and decides when and which upgrades are offered

    Every LINES_PER_UPGRADE cleared lines is a milestone. Each milestone opens
    exactly one selection.
*/
public class Upgrader {
    public static final int LINES_PER_UPGRADE = 5;
    public static final int OPTION_COUNT = 3;

    // Every upgrade in the game. To add one, add it here
    private final List<Upgrade> all = List.of(
        new Hold(),
        new SeeTheFuture(),
        new ScoreMultiplier(),
        new RemoveSZ(),
        new Bomb(),
        new Slowdown(),
        new Gamble()
    );

    private final Random random;
    private int milestonesUsed = 0;

    public Upgrader(Random random) {
        this.random = random;
    }

    // The upgrade of that class, for example get(Hold.class)
    public <T extends Upgrade> T get(Class<T> type) {
        for (Upgrade upgrade : all) {
            if (type.isInstance(upgrade)) {
                return type.cast(upgrade);
            }
        }
        throw new IllegalArgumentException(type.getSimpleName() + " is not in Upgrader.all");
    }

    public List<Upgrade> getAll() {
        return all;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isSelectionDue(int linesCleared) {
        return linesCleared / LINES_PER_UPGRADE > milestonesUsed;
    }

    // Uses up the next milestone and offers up to OPTION_COUNT different upgrades
    // that aren't maxed. Rarer upgrades are less likely to be offered
    public UpgradeSelection openSelection() {
        ++milestonesUsed;
        List<Upgrade> available = new ArrayList<>();
        for (Upgrade upgrade : all) {
            if (upgrade.isAvailable()) {
                available.add(upgrade);
            }
        }
        List<Upgrade> options = new ArrayList<>();
        while (options.size() < OPTION_COUNT && !available.isEmpty()) {
            options.add(available.remove(pickWeightedIndex(available)));
        }
        return new UpgradeSelection(options);
    }

    // A random one of the upgrades, each one's chance is its rarity weight
    public Upgrade pickWeighted(List<Upgrade> upgrades) {
        return upgrades.get(pickWeightedIndex(upgrades));
    }

    private int pickWeightedIndex(List<Upgrade> upgrades) {
        int total = 0;
        for (Upgrade upgrade : upgrades) {
            total += upgrade.getRarity().getWeight();
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < upgrades.size(); ++i) {
            roll -= upgrades.get(i).getRarity().getWeight();
            if (roll < 0) {
                return i;
            }
        }
        return upgrades.size() - 1;
    }
}
