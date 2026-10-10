package gui;

import game.Board;
import game.PlayManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import tetromino.Tetromino;

// Draws the game. This is the only class that turns cells into pixels
public class Renderer {
    private static final int BLOCK_SIZE = 30; // width and height of one cell, in pixels
    private static final int BLOCK_MARGIN = 2; // gap around each block, in pixels

    // Main Play Area, in pixels
    private static final int WIDTH = Board.COLUMNS * BLOCK_SIZE; // 300
    private static final int HEIGHT = Board.ROWS * BLOCK_SIZE; // 600
    private static final int LEFT_X = (GamePanel.WIDTH / 2) - (WIDTH / 2); // 1280/2 - 300/2 = 490
    private static final int RIGHT_X = LEFT_X + WIDTH;
    private static final int TOP_Y = 50;
    private static final int BOTTOM_Y = TOP_Y + HEIGHT;

    // Where the first block of the next piece is drawn, in pixels
    private static final int NEXT_PIECE_X = RIGHT_X + 175;
    private static final int NEXT_PIECE_Y = TOP_Y + 500;

    // Row clear effect
    private boolean effectCounterOn;
    private int effectCounter;
    private final List<Integer> effectRows = new ArrayList<>();

    public void draw(Graphics2D g2, PlayManager pm, boolean paused) {
        // Draw Play Area
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(LEFT_X - 4, TOP_Y - 4, WIDTH + 8, HEIGHT + 8);

        // Draw Next Piece Frame
        int x = RIGHT_X + 100;
        int y = BOTTOM_Y - 200;
        g2.drawRect(x, y, 200, 200);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString("NEXT", x + 60, y + 60);

        // Draw Score Frame
        g2.drawRect(x, TOP_Y, 250, 300);
        x += 40;
        y = TOP_Y + 90;
        g2.drawString("LEVEL: " + pm.getLevel(), x, y);
        y += 70;
        g2.drawString("LINES: " + pm.getLines(), x, y);
        y += 70;
        g2.drawString("SCORE: " + pm.getScore(), x, y);

        // Draw the ghost piece (where the piece lands after a hard drop)
        Tetromino current = pm.getCurrentPiece();
        int dropDistance = current.getDropDistance();
        for (int[] block : current.getBlocks()) {
            drawGhostBlock(g2, current.getShape().getColor(),
                    cellX(block[0]), cellY(block[1] + dropDistance));
        }

        // Draw the currentPiece on the board
        for (int[] block : current.getBlocks()) {
            drawBlock(g2, current.getShape().getColor(), cellX(block[0]), cellY(block[1]));
        }

        // Draw the nextPiece in the frame, with its first block at NEXT_PIECE_X, NEXT_PIECE_Y
        Tetromino next = pm.getNextPiece();
        int[][] nextBlocks = next.getBlocks();
        for (int[] block : nextBlocks) {
            drawBlock(g2, next.getShape().getColor(),
                    NEXT_PIECE_X + (block[0] - nextBlocks[0][0]) * BLOCK_SIZE,
                    NEXT_PIECE_Y + (block[1] - nextBlocks[0][1]) * BLOCK_SIZE);
        }

        // Draw the landed blocks
        Board board = pm.getBoard();
        for (int row = 0; row < Board.ROWS; ++row) {
            for (int col = 0; col < Board.COLUMNS; ++col) {
                Color color = board.getColor(col, row);
                if (color != null) {
                    drawBlock(g2, color, cellX(col), cellY(row));
                }
            }
        }

        // Draw Effect
        List<Integer> cleared = pm.takeClearedRows();
        if (!cleared.isEmpty()) {
            effectCounterOn = true;
            effectRows.addAll(cleared);
        }
        if (effectCounterOn) {
            ++effectCounter;

            g2.setColor(Color.red);
            for (int row : effectRows) {
                g2.fillRect(LEFT_X, cellY(row), WIDTH, BLOCK_SIZE);
            }

            if (effectCounter == 10) {
                effectCounterOn = false;
                effectCounter = 0;
                effectRows.clear();
            }
        }

        // Draw Pause or Game Over
        g2.setColor(Color.yellow);
        g2.setFont(g2.getFont().deriveFont(50f));
        if (pm.isGameOver()) {
            x = LEFT_X + 25;
            y = TOP_Y + 320;
            g2.drawString("GAME OVER", x, y);
        } else if (paused) {
            x = LEFT_X + 70;
            y = TOP_Y + 320;
            g2.drawString("PAUSED", x, y);
        }

        // Draw the Game Title
        x = 35;
        y = TOP_Y + 320;
        g2.setColor(Color.white);
        g2.setFont(new Font("Times New Roman", Font.ITALIC, 60));
        g2.drawString("++Tetris", x + 20, y);
    }

    // Left edge of a board column, in pixels
    private int cellX(int col) {
        return LEFT_X + col * BLOCK_SIZE;
    }

    // Top edge of a board row, in pixels
    private int cellY(int row) {
        return TOP_Y + row * BLOCK_SIZE;
    }

    private void drawBlock(Graphics2D g2, Color color, int x, int y) {
        g2.setColor(color);
        g2.fillRect(x, y, BLOCK_SIZE - (BLOCK_MARGIN * 2), BLOCK_SIZE - (BLOCK_MARGIN * 2));
    }

    // Same size as drawBlock, but only the outline
    private void drawGhostBlock(Graphics2D g2, Color color, int x, int y) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x + 1, y + 1, BLOCK_SIZE - (BLOCK_MARGIN * 2) - 2,
                BLOCK_SIZE - (BLOCK_MARGIN * 2) - 2);
    }
}
