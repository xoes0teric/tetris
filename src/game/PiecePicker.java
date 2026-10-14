package game;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import tetromino.Shape;
import tetromino.Tetromino;

/**
    PiecePicker manages the piece queue

    It uses a system where 7 pieces are put into a "bag" and then drawn
    one at a time out of the bag. This way pieces don't repeat too much
    and the longest you can go without a specific piece is 12 draws.
*/
public class PiecePicker {
    private final Board board;
    private final Queue<Shape> bag = new ArrayDeque<>();

    public PiecePicker(Board board) {
        this.board = board;
    }

    private void fillBag() {
        // it's not possible to shuffle a queue directly so we need to use an arrayList
        List<Shape> temp = new ArrayList<>(List.of(Shape.values()));
        Collections.shuffle(temp);
        bag.addAll(temp);
    }

    public Tetromino pickPiece() {
        if (bag.isEmpty()) {
            fillBag();
        }
        return new Tetromino(bag.poll(), board);
    }
}
