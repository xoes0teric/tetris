package game;

import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

/**
    PlayManager manages the movement of pieces
    Everything is in cells, drawing in pixels is done by gui.Renderer
*/
public class PlayManager {
    // Where the first block of a new piece appears, in cells
    private static final int SPAWN_COL = 4;
    private static final int SPAWN_ROW = 1;

    private final Board board = new Board();

    private Tetromino currentPiece;
    private Tetromino nextPiece;
    private boolean active = true; // false once the current piece is ready to lock
    private GameState gameState = GameState.PLAY;

    private final Timer timer = new Timer();
    private final PiecePicker picker = new PiecePicker(board);
    private final ScoreTracker tracker = new ScoreTracker(timer);
    // Rows cleared for GUI effects
    private final List<Integer> clearedRows = new ArrayList<>();

    public PlayManager() {
        // Set the starting piece
        currentPiece = picker.pickPiece();
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        nextPiece = picker.pickPiece();
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
            timer.resetAutoDropCounter();
        }
    }

    public void hardDrop() {
        if (active) {
            while (currentPiece.tryMove(0, 1)) {
            }
            active = false;
        }
    }

    // TODO: Add Hold mechanic

    public void update() {
        if (!active) {
            lockPiece();
            return;
        }

        if (currentPiece.canMoveDown()) {
            timer.resetDeactivateCounter();
            if (timer.shouldDrop()) {
                currentPiece.tryMove(0, 1);
            }
        } else if (timer.shouldLock()) {
            active = false;
        }
    }

    private void lockPiece() {
        board.lock(currentPiece);

        // if the first block is still on the spawn cell, the piece couldn't move at all,
        // so the stack has reached the top (game over)
        int[] first = currentPiece.getBlocks()[0];
        if (first[0] == SPAWN_COL && first[1] == SPAWN_ROW) {
            gameState = GameState.GAME_OVER;
        }

        // replace the currentPiece with the nextPiece
        currentPiece = nextPiece;
        currentPiece.place(SPAWN_COL, SPAWN_ROW);
        nextPiece = picker.pickPiece();
        active = true;
        timer.resetCounters();

        // when a piece locks, check if line(s) can be deleted
        List<Integer> cleared = board.clearFullRows();
        clearedRows.addAll(cleared);
        tracker.countClearedLines(cleared);
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
        return gameState == GameState.GAME_OVER;
    }

    public int getLevel() {
        return tracker.getLevel();
    }

    public int getLines() {
        return tracker.getLinesCleared();
    }

    public int getScore() {
        return tracker.getScore();
    }
}
