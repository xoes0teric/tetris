package upgrades;

import tetromino.Shape;

/**
    Hold puts the falling piece aside to use later

    Every level adds one hold slot, so more pieces can be kept for later
    (for example an I piece for a tetris and a T piece for a tight gap).
    Only one hold is allowed per piece, so you can't keep swapping forever.
*/
public class Hold {
    public static final int MAX_LEVEL = 3;
    private static final String[] SLOT_KEYS = {"C", "V", "B"};

    private final Upgrade upgrade = new Upgrade("Hold", MAX_LEVEL, false, new String[]{
        "Press C to put the falling piece aside and swap it back later. Once per piece.",
        "Adds hold slot 2 (key V) to keep one more piece aside.",
        "Adds hold slot 3 (key B) to keep one more piece aside.",
    });
    private final Shape[] slots = new Shape[MAX_LEVEL];
    private boolean usedThisPiece = false;

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public int getSlotCount() {
        return upgrade.getLevel();
    }

    public static String getSlotKey(int slot) {
        return SLOT_KEYS[slot];
    }

    // The shape in the slot, null if it is empty
    public Shape getSlot(int slot) {
        return slots[slot];
    }

    public boolean isUsedThisPiece() {
        return usedThisPiece;
    }

    public boolean canHold(int slot) {
        return slot >= 0 && slot < getSlotCount() && !usedThisPiece;
    }

    // Puts the shape in the slot and returns what was there before (null if it was empty)
    public Shape swap(int slot, Shape shape) {
        Shape old = slots[slot];
        slots[slot] = shape;
        usedThisPiece = true;
        return old;
    }

    public void onPieceLocked() {
        usedThisPiece = false;
    }
}
