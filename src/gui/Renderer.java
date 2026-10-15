package gui;

import boss.BedrockBoss;
import boss.BossOutcome;
import game.Board;
import game.GameState;
import game.PlayManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import tetromino.Shape;
import tetromino.Tetromino;
import upgrades.Hold;
import upgrades.UpgradeSelection;

// Draws the game. This is the only class that turns cells into pixels
public class Renderer {
    private static final int BLOCK_SIZE = 30; // width and height of one cell, in pixels
    private static final int BLOCK_MARGIN = 2; // gap around each block, in pixels
    private static final int SMALL_BLOCK_SIZE = 18; // blocks in the hold box
    private static final int TINY_BLOCK_SIZE = 13; // blocks in the later pieces frame

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

    // Frame for the pieces after the next one (See the Future), in pixels
    private static final int LATER_X = RIGHT_X + 320;
    private static final int LATER_WIDTH = 140;
    private static final int LATER_LABEL_HEIGHT = 25;

    // Hold box on the left, in pixels
    private static final int HOLD_X = 190;
    private static final int HOLD_BOX_SIZE = 80;

    private static final Color UNBREAKABLE_COLOR = new Color(90, 90, 90);
    private static final Color UNBREAKABLE_LINE_COLOR = new Color(140, 140, 140);

    // Row clear effect
    private boolean effectCounterOn;
    private int effectCounter;
    private final List<Integer> effectRows = new ArrayList<>();

    private final UpgradeMenu upgradeMenu = new UpgradeMenu();
    private final SpinView spinView;
    private final DropImpact dropImpact = new DropImpact();
    private final BossPanel bossPanel = new BossPanel();

    public Renderer(SoundMixer mixer) {
        spinView = new SpinView(mixer);
    }

    public void draw(Graphics2D g2, PlayManager pm, boolean paused) {
        GameState state = pm.getGameState();

        Background.draw(g2);

        // Everything up to the menus shakes after a hard drop, the background doesn't
        int[] shake = dropImpact.shakeOffset();
        g2.translate(shake[0], shake[1]);

        // Draw Play Area, black so the blocks are easy to see on any background
        g2.setColor(Color.black);
        g2.fillRect(LEFT_X, TOP_Y, WIDTH, HEIGHT);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(LEFT_X - 4, TOP_Y - 4, WIDTH + 8, HEIGHT + 8);

        dropImpact.drawTrails(g2);

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

        // Draw the next piece in the frame, with its first block at NEXT_PIECE_X, NEXT_PIECE_Y
        List<Shape> nextShapes = pm.getNextShapes();
        drawShapeFromFirstBlock(g2, nextShapes.get(0), NEXT_PIECE_X, NEXT_PIECE_Y);
        drawLaterPieces(g2, nextShapes.subList(1, nextShapes.size()));

        // Draw the landed blocks and the unbreakable rows
        Board board = pm.getBoard();
        for (int row = 0; row < Board.ROWS; ++row) {
            for (int col = 0; col < Board.COLUMNS; ++col) {
                if (board.isUnbreakable(row)) {
                    drawUnbreakableBlock(g2, cellX(col), cellY(row));
                    continue;
                }
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

        dropImpact.drawEffects(g2);

        drawHold(g2, pm.getUpgrader().get(Hold.class));

        BedrockBoss boss = pm.getBoss();
        if (boss != null && boss.getOutcome() != BossOutcome.WAITING) {
            bossPanel.drawStatus(g2, boss);
        }

        // Draw the Game Title
        x = 35;
        y = TOP_Y + 320;
        g2.setColor(Color.white);
        g2.setFont(new Font("Times New Roman", Font.ITALIC, 60));
        g2.drawString("++Tetris", x + 20, y);

        g2.translate(-shake[0], -shake[1]);

        // Draw the screens on top of the board
        UpgradeSelection selection = pm.getUpgradeSelection();
        if (state == GameState.SELECT_UPGRADE && selection != null) {
            upgradeMenu.draw(g2, selection, pm.isBossFightRunning());
        } else if (state == GameState.GAMBLE && pm.getSpin() != null) {
            spinView.draw(g2, pm.getSpin(), paused);
        } else if (state == GameState.BOSS_INTRO && boss != null) {
            bossPanel.drawIntro(g2, boss);
        } else if (state == GameState.BOSS_RESULT && boss != null) {
            bossPanel.drawResult(g2, boss);
        }

        // Draw Pause or Game Over
        g2.setColor(Color.yellow);
        g2.setFont(new Font("Arial", Font.PLAIN, 50));
        if (pm.isGameOver()) {
            x = LEFT_X + 25;
            y = TOP_Y + 320;
            g2.drawString("GAME OVER", x, y);
        } else if (paused) {
            x = LEFT_X + 70;
            y = TOP_Y + 320;
            g2.drawString("PAUSED", x, y);
        }
    }

    // Starts the hard drop effects. fromBlocks are the piece's cells before it dropped
    public void hardDropped(Tetromino piece, int[][] fromBlocks) {
        dropImpact.start(toPixels(fromBlocks), toPixels(piece.getBlocks()), BLOCK_SIZE,
                piece.getShape().getColor());
    }

    // Top left corner of each cell, in pixels
    private List<int[]> toPixels(int[][] cells) {
        List<int[]> pixels = new ArrayList<>();
        for (int[] cell : cells) {
            pixels.add(new int[]{cellX(cell[0]), cellY(cell[1])});
        }
        return pixels;
    }

    // Pieces after the next one, only shown with See the Future
    private void drawLaterPieces(Graphics2D g2, List<Shape> later) {
        if (later.isEmpty()) {
            return;
        }
        int top = BOTTOM_Y - 200;
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(LATER_X, top, LATER_WIDTH, 200);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        drawCentered(g2, "THEN", LATER_X + LATER_WIDTH / 2, top + 20);

        int slotHeight = (200 - LATER_LABEL_HEIGHT) / later.size();
        for (int i = 0; i < later.size(); ++i) {
            int centerY = top + LATER_LABEL_HEIGHT + slotHeight * i + slotHeight / 2;
            drawShapeCentered(g2, later.get(i), LATER_X + LATER_WIDTH / 2, centerY,
                    TINY_BLOCK_SIZE);
        }
    }

    private void drawHold(Graphics2D g2, Hold hold) {
        if (!hold.isUnlocked()) {
            return;
        }
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.drawString("HOLD", HOLD_X, TOP_Y + 25);

        int boxY = TOP_Y + 40;
        // a gray frame shows that hold was already used for this piece
        g2.setColor(hold.isUsedThisPiece() ? Color.gray : Color.white);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(HOLD_X, boxY, HOLD_BOX_SIZE, HOLD_BOX_SIZE);
        Shape shape = hold.getHeld();
        if (shape != null) {
            drawShapeCentered(g2, shape, HOLD_X + HOLD_BOX_SIZE / 2,
                    boxY + HOLD_BOX_SIZE / 2, SMALL_BLOCK_SIZE);
        }
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        drawCentered(g2, "[C]", HOLD_X + HOLD_BOX_SIZE / 2, boxY + HOLD_BOX_SIZE + 22);
    }

    // Draws the shape so that its first block is at (x, y), like the next piece always was
    private void drawShapeFromFirstBlock(Graphics2D g2, Shape shape, int x, int y) {
        int[][] cells = shape.getCells();
        for (int[] cell : cells) {
            drawBlock(g2, shape.getColor(),
                    x + (cell[0] - cells[0][0]) * BLOCK_SIZE,
                    y + (cell[1] - cells[0][1]) * BLOCK_SIZE);
        }
    }

    // Draws the shape with the middle of its blocks at (centerX, centerY)
    private void drawShapeCentered(Graphics2D g2, Shape shape, int centerX, int centerY,
            int size) {
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (int[] cell : shape.getCells()) {
            minCol = Math.min(minCol, cell[0]);
            maxCol = Math.max(maxCol, cell[0]);
            minRow = Math.min(minRow, cell[1]);
            maxRow = Math.max(maxRow, cell[1]);
        }
        int left = centerX - (maxCol - minCol + 1) * size / 2;
        int top = centerY - (maxRow - minRow + 1) * size / 2;
        for (int[] cell : shape.getCells()) {
            drawBlock(g2, shape.getColor(), left + (cell[0] - minCol) * size,
                    top + (cell[1] - minRow) * size, size);
        }
    }

    // Draws text with its middle at centerX
    static void drawCentered(Graphics2D g2, String text, int centerX, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
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
        drawBlock(g2, color, x, y, BLOCK_SIZE);
    }

    private void drawBlock(Graphics2D g2, Color color, int x, int y, int size) {
        g2.setColor(color);
        int margin = BLOCK_MARGIN * size / BLOCK_SIZE;
        g2.fillRect(x, y, size - (margin * 2), size - (margin * 2));
    }

    // Gray with a cross, so it doesn't look like a normal block
    private void drawUnbreakableBlock(Graphics2D g2, int x, int y) {
        int size = BLOCK_SIZE - (BLOCK_MARGIN * 2);
        g2.setColor(UNBREAKABLE_COLOR);
        g2.fillRect(x, y, size, size);
        g2.setColor(UNBREAKABLE_LINE_COLOR);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x, y, x + size, y + size);
        g2.drawLine(x + size, y, x, y + size);
    }

    // Same size as drawBlock, but only the outline
    private void drawGhostBlock(Graphics2D g2, Color color, int x, int y) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x + 1, y + 1, BLOCK_SIZE - (BLOCK_MARGIN * 2) - 2,
                BLOCK_SIZE - (BLOCK_MARGIN * 2) - 2);
    }
}
