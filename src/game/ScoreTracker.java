package game;

import java.util.List;

/**
    ScoreTracker keeps track of the score and increases levels
*/
public class ScoreTracker {
    private final Timer timer;
    private int level = 1;
    private int linesCleared = 0;
    private int score = 0;

    public ScoreTracker(Timer timer) {
        this.timer = timer;
    }

    public int getLevel() {
        return level;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public int getScore() {
        return score;
    }

    // Returns the points earned for the cleared lines
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

    // Points that don't come from line clears (like the hammer)
    public void addPoints(int points) {
        score += points;
    }
}
