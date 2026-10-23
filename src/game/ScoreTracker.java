package game;

import java.util.List;

/**
 * ScoreTracker keeps track of the score and increases levels.
 * Every 10 cleared lines the level goes up and the pieces fall faster.
 */
public class ScoreTracker {
    private final Timer timer;
    private int level = 1;
    private int linesCleared = 0;
    private int score = 0;
    private int scoreMultiplier = 1;

    /**
     * Creates a tracker at level 1 with no score.
     *
     * @param timer the timer to speed up when the level goes up
     */
    public ScoreTracker(Timer timer) {
        this.timer = timer;
    }

    /**
     * Returns the current level.
     *
     * @return the level, starting at 1
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the lines cleared in the whole game.
     *
     * @return the total number of cleared lines
     */
    public int getLinesCleared() {
        return linesCleared;
    }

    /**
     * Returns the total score.
     *
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * Returns the multiplier applied to line clear points.
     *
     * @return the score multiplier
     */
    public int getScoreMultiplier() {
        return scoreMultiplier;
    }

    /**
     * Counts the cleared lines, raises the level when needed and adds the points.
     *
     * @param cleared the rows that were cleared, 1 to 4
     * @return the points earned for the cleared lines
     */
    public int countClearedLines(List<Integer> cleared) {
        int earnedPoints;
        switch (cleared.size()) {
            case 1:
                earnedPoints = 100;
                break;
            case 2:
                earnedPoints = 300;
                break;
            case 3:
                earnedPoints = 500;
                break;
            case 4:
                earnedPoints = 1000;
                break;
            default:
                throw new IllegalArgumentException("can't score " + cleared.size() + " lines");
        }
        earnedPoints *= scoreMultiplier;
        score += earnedPoints;

        // level up check after counting the points
        for (int i = 0; i < cleared.size(); ++i) {
            ++linesCleared;
            if (linesCleared % 10 == 0) {
                levelUp();
            }
        }
        return earnedPoints;
    }

    /**
     * Raises the level and the score multiplier by one and makes the pieces
     * fall faster.
     */
    private void levelUp() {
        level++;
        scoreMultiplier++;
        timer.speedUp();
    }

    /**
     * Adds points that don't come from line clears (like the hammer).
     *
     * @param points the points to add
     */
    public void addPoints(int points) {
        score += points;
    }
}
