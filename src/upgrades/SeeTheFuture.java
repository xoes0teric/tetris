package upgrades;

/**
    See the Future shows more of the upcoming pieces. Every level shows one more
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

    public Upgrade getUpgrade() {
        return upgrade;
    }

    // How many upcoming pieces are shown
    public int getPreviewCount() {
        return BASE_PREVIEW + upgrade.getLevel();
    }
}
