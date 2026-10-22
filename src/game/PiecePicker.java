package game;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import tetromino.Shape;
import tetromino.Tetromino;

/**
 * PiecePicker manages the piece queue.
 *
 * <p>It uses a system where 7 pieces are put into a "bag" and then drawn
 * one at a time out of the bag. This way pieces don't repeat too much
 * and the longest you can go without a specific piece is 12 draws.
 */
public class PiecePicker {
    private final Board board;
    private final Random random;
    private final Queue<Shape> bag = new ArrayDeque<>();

    /**
     * Creates a picker with a random order.
     *
     * @param board the board the picked pieces move on
     */
    public PiecePicker(Board board) {
        this(board, new Random());
    }

    /**
     * Creates a picker that shuffles with the given random (useful for tests).
     *
     * @param board  the board the picked pieces move on
     * @param random the random used to shuffle the bags
     */
    public PiecePicker(Board board, Random random) {
        this.board = board;
        this.random = random;
    }

    /**
     * Adds one shuffled bag of all 7 shapes to the end of the queue.
     */
    private void fillBag() {
        // it's not possible to shuffle a queue directly so we need to use an arrayList
        List<Shape> temp = new ArrayList<>(List.of(Shape.values()));
        Collections.shuffle(temp, random);
        bag.addAll(temp);
    }

    /**
     * Takes the next piece out of the bag, filling a new bag when it is empty.
     *
     * @return a new piece of the next shape
     */
    public Tetromino pickPiece() {
        if (bag.isEmpty()) {
            fillBag();
        }
        return new Tetromino(bag.poll(), board);
    }

    /**
     * Returns the next shapes pickPiece will return, in order.
     * Fills extra bags when needed, which doesn't change what gets picked.
     *
     * @param count the number of shapes to look at
     * @return the next count shapes
     */
    public List<Shape> peek(int count) {
        while (bag.size() < count) {
            fillBag();
        }
        return new ArrayList<>(bag).subList(0, count);
    }
}
