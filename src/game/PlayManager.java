package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Random;
import tetromino.Block;
import tetromino.Mino;
import tetromino.MinoI;
import tetromino.MinoJ;
import tetromino.MinoL;
import tetromino.MinoO;
import tetromino.MinoS;
import tetromino.MinoT;
import tetromino.MinoZ;

public class PlayManager {
    // Main Play Area
    static final int WIDTH = 360;
    static final int HEIGHT = 600;
    public static int left_x;
    public static int right_x;
    public static int top_y;
    public static int bottom_y;

    // Mino
    Mino currentMino;
    final int minoStartX;
    final int minoStartY;
    Mino nextMino;
    final int nextMinoX;
    final int nextMinoY;
    public static ArrayList<Block> staticBlocks = new ArrayList<>();


    // Others
    public static int dropInterval = 60; // Mino drops in every 60 frames
    boolean gameOver;

    // Effect
    boolean effectCounterOn;
    int effectCounter;
    ArrayList<Integer> effectY = new ArrayList<>();

    // Score
    int level = 1;
    int lines;
    int score;


    public PlayManager() {

        // Main Play Area Frame
        left_x = (GamePanel.WIDTH / 2) - (WIDTH / 2); // 1280/2 - 360/2 = 460
        right_x = left_x + WIDTH;
        top_y = 50;
        bottom_y = top_y + HEIGHT;

        minoStartX = left_x + (WIDTH / 2) - Block.SIZE;
        minoStartY = top_y + Block.SIZE;

        nextMinoX = right_x + 175;
        nextMinoY = top_y + 500;

        // Set the starting Mino
        currentMino = pickMino();
        currentMino.setXY(minoStartX, minoStartY);
        nextMino = pickMino();
        nextMino.setXY(nextMinoX, nextMinoY);
    }

    private Mino pickMino() {
        Mino mino = null;
        int i = new Random().nextInt(7);

        switch (i) {
            case 0:
                mino = new MinoL();
                break;
            case 1:
                mino = new MinoJ();
                break;
            case 2:
                mino = new MinoO();
                break;
            case 3:
                mino = new MinoI();
                break;
            case 4:
                mino = new MinoT();
                break;
            case 5:
                mino = new MinoZ();
                break;
            case 6:
                mino = new MinoS();
                break;
            default:
                break;
        }
        return mino;
    }

    public void update() {
        // Check if currentMino is active
        if (!currentMino.active) {
            // if the mino is not active, put it into the staticBlocks
            staticBlocks.add(currentMino.b[0]);
            staticBlocks.add(currentMino.b[1]);
            staticBlocks.add(currentMino.b[2]);
            staticBlocks.add(currentMino.b[3]);

            // check if the game is over
            if (currentMino.b[0].x == minoStartX && currentMino.b[0].y == minoStartY) {
                // this means the currentMino immediately collided a block and couldn't move at all
                // so it's xy are the same with the nextMino's
                gameOver = true;
            }

            currentMino.deactivating = false;

            // replace the currentMino with the nextMino
            currentMino = nextMino;
            currentMino.setXY(minoStartX, minoStartY);
            nextMino = pickMino();
            nextMino.setXY(nextMinoX, nextMinoY);

            // when a mino becomes inactive, check if lines(s) can be deleted
            checkDelete();
        } else {
            currentMino.update();
        }
    }

    private void checkDelete() {
        int x = left_x;
        int y = top_y;
        int blockCount = 0;
        int lineCount = 0;


        while (x < right_x && y < bottom_y) {

            for (int i = 0; i < staticBlocks.size(); ++i) {
                if (staticBlocks.get(i).x == x && staticBlocks.get(i).y == y) {
                    // increase count if there is a static block
                    ++blockCount;
                }
            }

            x += Block.SIZE;

            if (x == right_x) {

                // if block count hits 12, that means the current y line is all filled with blocks
                // so we can delete them
                if (blockCount == 12) {

                    effectCounterOn = true;
                    effectY.add(y);

                    for (int i = staticBlocks.size() - 1; i > -1; --i) {
                        // remove all the blocks in the current y line
                        if (staticBlocks.get(i).y == y) {
                            staticBlocks.remove(i);
                        }
                    }

                    ++lineCount;
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

                    // a line has been deleted so need to slide down blocks that are above it
                    for (int i = 0; i < staticBlocks.size(); ++i) {
                        // if block is above the current y, move it down by the block size
                        if (staticBlocks.get(i).y < y) {
                            staticBlocks.get(i).y += Block.SIZE;
                        }
                    }
                }

                blockCount = 0;
                x = left_x;
                y += Block.SIZE;
            }
        }

        // Add Score
        if (lineCount > 0) {
            int singleLineScore = 10 * level;
            score += singleLineScore * lineCount;

        }
    }

    public void draw(Graphics2D g2) {
        // Draw Play Area
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(left_x - 4, top_y - 4, WIDTH + 8, HEIGHT + 8);

        // Draw Mino Frame
        int x = right_x + 100;
        int y = bottom_y - 200;
        g2.drawRect(x, y, 200, 200);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString("NEXT", x + 60, y + 60);

        // Draw Score Frame
        g2.drawRect(x, top_y, 250, 300);
        x += 40;
        y = top_y + 90;
        g2.drawString("LEVEL: " + level, x, y);
        y += 70;
        g2.drawString("LINES: " + lines, x, y);
        y += 70;
        g2.drawString("SCORE: " + score, x, y);

        // Draw the currentMino
        if (currentMino != null) {
            currentMino.draw(g2);
        }

        // Draw the nextMino
        nextMino.draw(g2);

        // Draw staticBlocks
        for (int i = 0; i < staticBlocks.size(); ++i) {
            staticBlocks.get(i).draw(g2);
        }

        // Draw Effect
        if (effectCounterOn) {
            ++effectCounter;

            g2.setColor(Color.red);
            for (int i = 0; i < effectY.size(); ++i) {
                g2.fillRect(left_x, effectY.get(i), WIDTH, Block.SIZE);
            }

            if (effectCounter == 10) {
                effectCounterOn = false;
                effectCounter = 0;
                effectY.clear();
            }
        }

        // Draw Pause or Game Over
        g2.setColor(Color.yellow);
        g2.setFont(g2.getFont().deriveFont(50f));
        if (gameOver) {
            x = left_x + 25;
            y = top_y + 320;
            g2.drawString("GAME OVER", x, y);
        } else if (KeyHandler.pausePressed) {
            x = left_x + 70;
            y = top_y + 320;
            g2.drawString("PAUSED", x, y);
        }

        // Draw the Game Title
        x = 35;
        y = top_y + 320;
        g2.setColor(Color.white);
        g2.setFont(new Font("Times New Roman", Font.ITALIC, 60));
        g2.drawString("++Tetris", x + 20, y);
    }
}
