package game;

import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;

/**
    PiecePicker manages the piece queue and randomness
*/
public class PiecePicker {
    private final Board board;

    // TODO: random logic here

    public PiecePicker(Board board) {
        this.board = board;
    }

    public Tetromino pickPiece() {
        Shape[] shapes = Shape.values();
        return new Tetromino(shapes[new Random().nextInt(shapes.length)], board);
    }
}
