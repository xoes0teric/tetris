package upgrades;

/**
    Score Multiplier gives more points for line clears. Every level adds x0.5
*/
public class ScoreMultiplier extends Upgrade {
    private static final int PERCENT_PER_LEVEL = 50;

    public ScoreMultiplier() {
        super("Score Multiplier", Rarity.RARE, 4);
    }

    // 100 is x1, 150 is x1.5, ...
    public int getPercent() {
        return 100 + PERCENT_PER_LEVEL * getLevel();
    }
}
