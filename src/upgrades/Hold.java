package upgrades;

import tetromino.Shape;

/**
 * Hold puts the falling piece aside to use later.
 *
 * <p>Every level adds one hold slot, so more pieces can be kept for later
 * (for example an I piece for a tetris and a T piece for a tight gap).
 * Only one hold is allowed per piece, so you can't keep swapping forever.
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

    /**
     * Returns the upgrade information for the selection screen.
     *
     * @return the upgrade
     */
    public Upgrade getUpgrade() {
        return upgrade;
    }

    /**
     * Returns how many hold slots the player has, one per level.
     *
     * @return the number of slots
     */
    public int getSlotCount() {
        return upgrade.getLevel();
    }

    /**
     * Returns the key that uses a hold slot.
     *
     * @param slot the slot, starting at 0
     * @return the name of the key
     */
    public static String getSlotKey(int slot) {
        return SLOT_KEYS[slot];
    }

    /**
     * Returns the shape in a slot.
     *
     * @param slot the slot, starting at 0
     * @return the shape, or null if the slot is empty
     */
    public Shape getSlot(int slot) {
        return slots[slot];
    }

    /**
     * Returns whether hold was already used for the falling piece.
     *
     * @return true if the player can't hold again until the piece locks
     */
    public boolean isUsedThisPiece() {
        return usedThisPiece;
    }

    /**
     * Checks whether the slot exists and hold wasn't used for this piece yet.
     *
     * @param slot the slot, starting at 0
     * @return true if the player can hold into that slot
     */
    public boolean canHold(int slot) {
        return slot >= 0 && slot < getSlotCount() && !usedThisPiece;
    }

    /**
     * Puts the shape in the slot and returns what was there before.
     *
     * @param slot  the slot, starting at 0
     * @param shape the shape to put aside
     * @return the shape that was in the slot, or null if it was empty
     */
    public Shape swap(int slot, Shape shape) {
        Shape old = slots[slot];
        slots[slot] = shape;
        usedThisPiece = true;
        return old;
    }

    /**
     * Allows holding again. Called every time a piece locks.
     */
    public void onPieceLocked() {
        usedThisPiece = false;
    }
}
