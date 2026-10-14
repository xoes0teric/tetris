package tests;

import game.Board;
import java.util.Objects;

// Tiny assertion helpers so the tests don't need a library
final class Check {
    private Check() {
    }

    static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    // Unbreakable rows must never contain cells
    static void boardValid(Board board) {
        for (int row = board.getPlayableRows(); row < Board.ROWS; ++row) {
            for (int col = 0; col < Board.COLUMNS; ++col) {
                isTrue(board.getColor(col, row) == null,
                        "unbreakable cell " + col + "," + row + " holds a block");
                isTrue(!board.isFreeCell(col, row), "unbreakable cell " + col + "," + row
                        + " is free");
            }
        }
    }
}
