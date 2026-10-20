package upgrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
    Upgrader owns all upgrades and decides when and which upgrades are offered

    Every LINES_PER_UPGRADE cleared lines is a milestone. Each milestone opens
    exactly one selection. To add an upgrade, make its class and add its Upgrade to all.
*/
public class Upgrader {
    public static final int LINES_PER_UPGRADE = 5;
    public static final int OPTION_COUNT = 3;

    private final Hold hold = new Hold();
    private final SeeTheFuture seeTheFuture = new SeeTheFuture();
    private final Hammer hammer = new Hammer();
    private final Patch patch = new Patch();
    private final Flush flush = new Flush();
    private final List<Upgrade> all = List.of(hold.getUpgrade(), seeTheFuture.getUpgrade(),
            hammer.getUpgrade(), patch.getUpgrade(), flush.getUpgrade());

    private final Random random;
    private int milestonesUsed = 0;

    public Upgrader(Random random) {
        this.random = random;
    }

    public boolean isSelectionDue(int linesCleared) {
        return linesCleared / LINES_PER_UPGRADE > milestonesUsed;
    }

    // Uses up the next milestone and offers up to OPTION_COUNT different upgrades
    // that aren't maxed. Options are random for now, later the ClearHistory
    // could be used here to offer better upgrades for impressive clears
    public UpgradeSelection openSelection() {
        ++milestonesUsed;
        List<Upgrade> available = new ArrayList<>();
        for (Upgrade upgrade : all) {
            if (upgrade.isAvailable()) {
                available.add(upgrade);
            }
        }
        Collections.shuffle(available, random);
        return new UpgradeSelection(available.subList(0, Math.min(OPTION_COUNT, available.size())));
    }

    public List<Upgrade> getAll() {
        return all;
    }

    public Hold getHold() {
        return hold;
    }

    public SeeTheFuture getSeeTheFuture() {
        return seeTheFuture;
    }

    public Hammer getHammer() {
        return hammer;
    }

    public Patch getPatch() {
        return patch;
    }

    public Flush getFlush() {
        return flush;
    }
}
