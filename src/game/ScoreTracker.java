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
     * Counts the cleared lines, raises the level when needed and adds the points.
     *
     * @param cleared the rows that were cleared
     * @return the points earned for the cleared lines
     */
    public int countClearedLines(List<Integer> cleared) {
        for (int i = 0; i < cleared.size(); ++i) {
            ++linesCleared;
            if (linesCleared % 10 == 0) {
                ++level;
                timer.speedUp();
            }
        }
        int earned = 0;
        if (!cleared.isEmpty()) {
            int singleLineScore = 10 * level;
            earned = singleLineScore * cleared.size();
            score += earned;
        }
        return earned;
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
