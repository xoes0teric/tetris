package upgrades;

import java.util.List;

/**
    The Gamble spin, like opening a case: a long strip of upgrades slides past a
    marker, slows down and stops on the result. This class only does the timing
    and the position, gui.SpinView draws it.

    The position is measured in items: position 7.0 means the middle of item 7 is
    under the marker, 7.4 means the strip moved 0.4 of an item further.
    The strip stops a bit off the middle of the result (landingOffset) so it doesn't
    look like it was decided beforehand, even though it was.
*/
public class Spin {
    // Same as gui.SpinSound.MUSIC_DROP_NANOS, so the reel stops on the music's drop
    public static final long SPIN_NANOS = 5_930_000_000L;
    public static final long REVEAL_NANOS = 2_000_000_000L; // result stays on screen
    private static final double START_POSITION = 3.0;

    private final List<Upgrade> items;
    private final int resultIndex;
    private final double endPosition;
    private long elapsedNanos = 0;

    // landingOffset should be between -0.5 and 0.5 so the marker stays on the result
    public Spin(List<Upgrade> items, int resultIndex, double landingOffset) {
        this.items = List.copyOf(items);
        this.resultIndex = resultIndex;
        this.endPosition = resultIndex + landingOffset;
    }

    public void update(long elapsed) {
        elapsedNanos += elapsed;
    }

    // How far the spin is, 0 at the start and 1 when it stops
    private double progress() {
        return Math.min(1.0, (double) elapsedNanos / SPIN_NANOS);
    }

    // Which item is under the marker, see the class comment
    public double getPosition() {
        // ease out: very fast at first, then a long slow crawl at the end
        double eased = 1 - Math.pow(1 - progress(), 4);
        return START_POSITION + (endPosition - START_POSITION) * eased;
    }

    // Items per second, the GUI uses it to blur fast moving cards
    public double getSpeed() {
        double speed = 4 * Math.pow(1 - progress(), 3);
        return (endPosition - START_POSITION) * speed * 1_000_000_000.0 / SPIN_NANOS;
    }

    public boolean hasLanded() {
        return elapsedNanos >= SPIN_NANOS;
    }

    // 0 right when the strip stops, 1 when the reveal is over
    public double getRevealProgress() {
        if (!hasLanded()) {
            return 0;
        }
        return Math.min(1.0, (double) (elapsedNanos - SPIN_NANOS) / REVEAL_NANOS);
    }

    public boolean isFinished() {
        return elapsedNanos >= SPIN_NANOS + REVEAL_NANOS;
    }

    public List<Upgrade> getItems() {
        return items;
    }

    public int getResultIndex() {
        return resultIndex;
    }

    public Upgrade getResult() {
        return items.get(resultIndex);
    }
}
