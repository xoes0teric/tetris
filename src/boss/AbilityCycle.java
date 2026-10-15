package boss;

import java.util.List;

/**
    Uses a boss's abilities one after another, with a warning before each one
    so the player can get ready.

    It only keeps the time, the game does what the used ability means
    (see takeUsed()). Inverted controls are tracked here because they last a while.
*/
public class AbilityCycle {
    public static final long INTERVAL_NANOS = 15_000_000_000L; // between two abilities
    public static final long WARNING_NANOS = 3_000_000_000L; // warning before an ability
    public static final long INVERT_NANOS = 8_000_000_000L; // how long controls stay inverted

    private final List<BossAbility> abilities;
    private int nextIndex = 0;
    private long untilNextNanos = INTERVAL_NANOS;
    private long invertedNanos = 0;
    private BossAbility used; // used since the last takeUsed(), null when none

    public AbilityCycle(List<BossAbility> abilities) {
        this.abilities = List.copyOf(abilities);
    }

    public void tick(long elapsedNanos) {
        invertedNanos = Math.max(0, invertedNanos - elapsedNanos);
        if (abilities.isEmpty()) {
            return;
        }
        untilNextNanos -= elapsedNanos;
        if (untilNextNanos > 0) {
            return;
        }
        used = abilities.get(nextIndex);
        nextIndex = (nextIndex + 1) % abilities.size();
        untilNextNanos = INTERVAL_NANOS;
        if (used == BossAbility.INVERT_CONTROLS) {
            invertedNanos = INVERT_NANOS;
        }
    }

    // Returns the ability used since the last call and forgets it, null if none
    public BossAbility takeUsed() {
        BossAbility ability = used;
        used = null;
        return ability;
    }

    // The ability coming next, null when it's too early to warn
    public BossAbility getWarning() {
        if (abilities.isEmpty() || untilNextNanos > WARNING_NANOS) {
            return null;
        }
        return abilities.get(nextIndex);
    }

    public long getUntilNextNanos() {
        return untilNextNanos;
    }

    public boolean isControlsInverted() {
        return invertedNanos > 0;
    }

    public long getInvertedNanos() {
        return invertedNanos;
    }

    public List<BossAbility> getAbilities() {
        return abilities;
    }
}
