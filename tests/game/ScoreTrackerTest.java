package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for scoring in ScoreTracker. Run them with ./test.sh
 */
class ScoreTrackerTest {
    private ScoreTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ScoreTracker(new Timer());
    }

    @Test
    void multiplierStaysBeforeTenLines() {
        clearLines(9);
        assertEquals(1, tracker.getScoreMultiplier());
    }

    @Test
    void multiplierGoesUpAtTenLines() {
        clearLines(10);
        assertEquals(2, tracker.getScoreMultiplier());
    }

    @Test
    void multiplierGoesUpEveryTenLines() {
        clearLines(20);
        assertEquals(3, tracker.getScoreMultiplier());
    }

    @Test
    void pointsUseTheMultiplier() {
        clearLines(10); // multiplier is now 2
        int before = tracker.getScore();
        int earned = tracker.countClearedLines(rows(1));
        assertEquals(200, earned);
        assertEquals(200, tracker.getScore() - before);
    }

    @Test
    void levelUpClearIsScoredAtOldMultiplier() {
        clearLines(8);
        int earned = tracker.countClearedLines(rows(4)); // line goes from 8 to 12
        assertEquals(1000, earned);
        assertEquals(2, tracker.getScoreMultiplier());
    }

    @Test
    void emptyClearThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.countClearedLines(List.of()));
    }

    @Test
    void tooManyLinesThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.countClearedLines(rows(5)));
    }

    // ---- Helpers ----

    /** Clears the given number of lines one single at a time. */
    private void clearLines(int lines) {
        for (int i = 0; i < lines; ++i) {
            tracker.countClearedLines(rows(1));
        }
    }

    /** Makes a list of row numbers, only its size matters to ScoreTracker. */
    private static List<Integer> rows(int count) {
        Integer[] rows = new Integer[count];
        for (int i = 0; i < count; ++i) {
            rows[i] = Board.ROWS - 1 - i;
        }
        return List.of(rows);
    }
}
