package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;

// Everything is in cells, drawing is done by gui.Renderer
public class PlayManager {
    private static final int LOCK_DELAY = 45; // frames a landed piece waits before locking
    // Where the first block of a new piece appears, in cells
    private static final int SPAWN_COL = 4;
    private static final int SPAWN_ROW = 1;

    private final Board board = new Board();

    private Tetromino currentPiece;
    private Tetromino nextPiece;
    private boolean active = true; // false once the current piece is ready to lock
    private int autoDropCounter = 0;
    private int deactivateCounter = 0;
    private boolean deactivating = false;

    private int dropInterval = 60; // the piece drops one row every 60 frames
    private boolean gameOver;
    // Rows cleared for GUI effects
    private final List<Integer> clearedRows = new ArrayList<>();

    // Score
    private int level = 1;
    private int lines;
    private int score;

    public PlayManager() {
        // Set the starting piece
        currentPiece = pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        nextPiece = pickPiece();
    }

    private Tetromino pickPiece() {
        Shape[] shapes = Shape.values();
        return new Tetromino(shapes[new Random().nextInt(shapes.length)], board);
    }

    public void rotate() {
        if (active) {
            currentPiece.rotate();
        }
    }

    public void moveLeft() {
        if (active) {
            currentPiece.tryMove(-1, 0);
        }
    }

    public void moveRight() {
        if (active) {
            currentPiece.tryMove(1, 0);
        }
    }

    public void moveDown() {
        // When moved down, reset the autoDropCounter
        if (active && currentPiece.tryMove(0, 1)) {
            autoDropCounter = 0;
        }
    }

    public void update() {
        if (!active) {
            lockPiece();
            return;
        }

        if (deactivating) {
            deactivating();
        }

        if (!currentPiece.canMoveDown()) {
            deactivating = true;
        } else {
            ++autoDropCounter; // the counter increases in every frame
            if (autoDropCounter == dropInterval) {
                currentPiece.tryMove(0, 1);
                autoDropCounter = 0;
            }
        }
    }

    private void deactivating() {
        ++deactivateCounter;
        // Wait LOCK_DELAY frames until deactivate
        if (deactivateCounter == LOCK_DELAY) {
            deactivateCounter = 0;
            // if the bottom is still hitting after the delay, lock the piece
            if (!currentPiece.canMoveDown()) {
                active = false;
            }
        }
    }

    private void lockPiece() {
        board.lock(currentPiece);

        // if the first block is still on the spawn cell, the piece couldn't move at all,
        // so the stack has reached the top (game over)
        int[] first = currentPiece.getBlocks()[0];
        if (first[0] == SPAWN_COL && first[1] == SPAWN_ROW) {
            gameOver = true;
        }

        // replace the currentPiece with the nextPiece
        currentPiece = nextPiece;
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        nextPiece = pickPiece();
        active = true;
        autoDropCounter = 0;
        deactivateCounter = 0;
        deactivating = false;

        // when a piece locks, check if line(s) can be deleted
        List<Integer> cleared = board.clearFullRows();
        clearedRows.addAll(cleared);
        for (int i = 0; i < cleared.size(); ++i) {
            ++lines;
            // Drop Speed
            // if the line score hits a certain number, increase the drop speed
            // 1 is the fastest
            if (lines % 10 == 0 && dropInterval > 1) {
                ++level;
                if (dropInterval > 10) {
                    dropInterval -= 10;
                } else {
                    dropInterval -= 1;
                }
            }
        }

        // Add Score
        if (!cleared.isEmpty()) {
            int singleLineScore = 10 * level;
            score += singleLineScore * cleared.size();
        }
    }

    // Returns the rows cleared since the last call and forgets them
    public List<Integer> takeClearedRows() {
        List<Integer> rows = new ArrayList<>(clearedRows);
        clearedRows.clear();
        return rows;
    }

    public Board getBoard() {
        return board;
    }

    public Tetromino getCurrentPiece() {
        return currentPiece;
    }

    public Tetromino getNextPiece() {
        return nextPiece;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getLevel() {
        return level;
    }

    public int getLines() {
        return lines;
    }

    public int getScore() {
        return score;
    }
}
