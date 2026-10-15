package upgrades;

import game.PlayManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
    Gamble opens a case: a strip of upgrades spins and lands on either a free
    upgrade or Garbage, 50/50. The free upgrade is picked like a normal offer,
    so rarity counts. The spin itself is in Spin and gui.SpinView
*/
public class Gamble extends Upgrade {
    private static final int STRIP_LENGTH = 60;
    private static final int RESULT_INDEX = 52; // a few items stay visible after it
    private static final double GARBAGE_FILL_CHANCE = 0.3; // how much of the strip is red
    private static final double NEAR_MISS_CHANCE = 0.5; // a legendary right next to the result

    private final Garbage garbage = new Garbage();

    public Gamble() {
        super("Gamble", Rarity.COMMON, UNLIMITED);
    }

    @Override
    protected void onPick(PlayManager game) {
        Upgrader upgrader = game.getUpgrader();
        Random random = upgrader.getRandom();

        List<Upgrade> prizes = new ArrayList<>();
        for (Upgrade upgrade : upgrader.getAll()) {
            if (upgrade != this && upgrade.isAvailable()) {
                prizes.add(upgrade);
            }
        }
        Upgrade result = garbage;
        if (!prizes.isEmpty() && random.nextBoolean()) {
            result = upgrader.pickWeighted(prizes);
        }

        // the strip looks like a real case: mostly commons, a few rare ones
        List<Upgrade> strip = new ArrayList<>();
        for (int i = 0; i < STRIP_LENGTH; ++i) {
            if (prizes.isEmpty() || random.nextDouble() < GARBAGE_FILL_CHANCE) {
                strip.add(garbage);
            } else {
                strip.add(upgrader.pickWeighted(prizes));
            }
        }
        strip.set(RESULT_INDEX, result);
        addNearMiss(strip, prizes, random);

        double landingOffset = (random.nextDouble() - 0.5) * 0.8; // never on the edge
        game.startSpin(new Spin(strip, RESULT_INDEX, landingOffset));
    }

    // Sometimes puts a legendary right after the result, so it just barely misses
    private void addNearMiss(List<Upgrade> strip, List<Upgrade> prizes, Random random) {
        if (random.nextDouble() >= NEAR_MISS_CHANCE) {
            return;
        }
        for (Upgrade prize : prizes) {
            if (prize.getRarity() == Rarity.LEGENDARY && prize != strip.get(RESULT_INDEX)) {
                strip.set(RESULT_INDEX + 1, prize);
                return;
            }
        }
    }
}
