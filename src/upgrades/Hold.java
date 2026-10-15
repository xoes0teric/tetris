package upgrades;

import tetromino.Shape;

/**
    Hold puts the falling piece aside to use later (key C or SHIFT)

    Only one hold is allowed per piece, so you can't keep swapping forever.
*/
public class Hold extends Upgrade {
    private Shape held; // null when empty
    private boolean usedThisPiece = false;

    public Hold() {
        super("Hold", Rarity.RARE, 1);
    }

    public boolean isUnlocked() {
        return getLevel() > 0;
    }

    // The held shape, null if there is none
    public Shape getHeld() {
        return held;
    }

    public boolean isUsedThisPiece() {
        return usedThisPiece;
    }

    public boolean canHold() {
        return isUnlocked() && !usedThisPiece;
    }

    // Holds the shape and returns the one held before (null if there was none)
    public Shape swap(Shape shape) {
        Shape old = held;
        held = shape;
        usedThisPiece = true;
        return old;
    }

    public void onPieceLocked() {
        usedThisPiece = false;
    }
}
