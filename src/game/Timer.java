package game;

/**
 * Timer keeps track of when to drop or deactivate pieces.
 * It counts frames, so each method is called once per frame.
 */
public class Timer {
    private static final int LOCK_DELAY = 45; // frames a landed piece waits before locking
    // in frames
    private int autoDropCounter = 0;
    private int deactivateCounter = 0;
    private int dropInterval = 60;

    /**
     * Counts one frame and checks whether the piece should fall one row.
     *
     * @return true once every drop interval
     */
    public boolean shouldDrop() {
        ++autoDropCounter;
        if (autoDropCounter < dropInterval) {
            return false;
        }
        autoDropCounter = 0;
        return true;
    }

    /**
     * Counts one frame of a landed piece and checks whether it should lock.
     *
     * @return true when the piece has waited the lock delay
     */
    public boolean shouldLock() {
        ++deactivateCounter;
        return deactivateCounter >= LOCK_DELAY;
    }

    /**
     * Makes pieces fall faster: 10 frames faster per level, then 1 frame,
     * down to one row per frame.
     */
    public void speedUp() {
        if (dropInterval > 10) {
            dropInterval -= 10;
        } else if (dropInterval > 1) {
            dropInterval -= 1;
        }
    }

    /**
     * Restarts the wait for the next automatic drop.
     */
    public void resetAutoDropCounter() {
        autoDropCounter = 0;
    }

    /**
     * Restarts the lock delay.
     */
    public void resetDeactivateCounter() {
        deactivateCounter = 0;
    }

    /**
     * Restarts both the automatic drop and the lock delay, used for a new piece.
     */
    public void resetCounters() {
        autoDropCounter = 0;
        deactivateCounter = 0;
    }
}
