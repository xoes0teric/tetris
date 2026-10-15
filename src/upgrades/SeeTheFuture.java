package upgrades;

/**
    See the Future shows more of the upcoming pieces. Every level shows one more
*/
public class SeeTheFuture extends Upgrade {
    private static final int BASE_PREVIEW = 1; // the game always shows the next piece

    public SeeTheFuture() {
        super("See the Future", Rarity.COMMON, 3);
    }

    // How many upcoming pieces are shown
    public int getPreviewCount() {
        return BASE_PREVIEW + getLevel();
    }
}
