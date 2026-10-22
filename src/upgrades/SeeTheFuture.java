package upgrades;

/**
 * See the Future shows more of the upcoming pieces. Every level shows one more.
 */
public class SeeTheFuture {
    public static final int MAX_LEVEL = 3;
    public static final int BASE_PREVIEW = 1; // the game always shows the next piece

    private final Upgrade upgrade = new Upgrade("See the Future", MAX_LEVEL, false,
            new String[]{
                "Shows one more upcoming piece (2 in total).",
                "Shows one more upcoming piece (3 in total).",
                "Shows one more upcoming piece (4 in total).",
            });

    /**
     * Returns the upgrade information for the selection screen.
     *
     * @return the upgrade
     */
    public Upgrade getUpgrade() {
        return upgrade;
    }

    /**
     * Returns how many upcoming pieces are shown.
     *
     * @return the number of pieces to preview, at least 1
     */
    public int getPreviewCount() {
        return BASE_PREVIEW + upgrade.getLevel();
    }
}
