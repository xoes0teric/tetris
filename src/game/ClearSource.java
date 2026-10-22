package game;

/**
 * What removed blocks from the board in a ClearEvent.
 */
public enum ClearSource {
    LINE_CLEAR(true), // a locked piece completed lines (the normal rules)
    HAMMER(true), // the hammer destroyed a row (not a line clear)
    PATCH(true), // patched blocks completed lines (counts as line clears)
    FLUSH(false); // the bottom rows were flushed away (no points, not a line clear)

    // Combo is not implemented yet. This says whether this kind of clear
    // should keep a combo going once it is
    private final boolean comboAction;

    /**
     * Creates a clear source.
     *
     * @param comboAction whether this kind of clear should keep a combo going
     */
    ClearSource(boolean comboAction) {
        this.comboAction = comboAction;
    }

    /**
     * Returns whether this kind of clear should keep a combo going.
     * Combo is not implemented yet.
     *
     * @return true if it counts towards a combo
     */
    public boolean isComboAction() {
        return comboAction;
    }
}
