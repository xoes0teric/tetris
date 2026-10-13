package game;

/**
    Timer keeps track of when to drop or deactivate pieces
*/
public class Timer {
    private static final int LOCK_DELAY = 45; // frames a landed piece waits before locking
    // in frames
    private int autoDropCounter = 0;
    private int deactivateCounter = 0;
    private int dropInterval = 60;

    public boolean shouldDrop() {
        ++autoDropCounter;
        if (autoDropCounter < dropInterval) {
            return false;
        }
        autoDropCounter = 0;
        return true;
    }

    public boolean shouldLock() {
        ++deactivateCounter;
        return deactivateCounter >= LOCK_DELAY;
    }

    public void speedUp() {
        if (dropInterval > 10) {
            dropInterval -= 10;
        } else if (dropInterval > 1) {
            dropInterval -= 1;
        }
    }

    public void resetAutoDropCounter() {
        autoDropCounter = 0;
    }

    public void resetDeactivateCounter() {
        deactivateCounter = 0;
    }

    public void resetCounters() {
        autoDropCounter = 0;
        deactivateCounter = 0;
    }
}
