package gui;

import boss.BedrockBoss;
import boss.BossOutcome;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Draws the boss status panel, its introduction and its result screen.
 * It only reads the boss, it never changes it.
 */
class BossPanel {
    private static final int PANEL_X = 130;
    private static final int PANEL_Y = 420;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 230;
    private static final int BAR_HEIGHT = 16;
    private static final long LOW_TIME_NANOS = 10_000_000_000L; // timer turns red
    private static final Color BOSS_COLOR = new Color(255, 120, 60);
    private static final Color BAR_BACK_COLOR = new Color(60, 60, 60);
    private static final Color DIM_COLOR = new Color(0, 0, 0, 200);

    // Screen box for the intro and result, over the board
    private static final int BOX_WIDTH = 560;
    private static final int BOX_HEIGHT = 300;
    private static final int BOX_X = (GamePanel.WIDTH - BOX_WIDTH) / 2;
    private static final int BOX_Y = 200;

    /**
     * Draws the panel next to the board with the boss name, the time left and
     * the score, each with a progress bar.
     *
     * @param g2   the graphics to draw with
     * @param boss the boss to show
     */
    void drawStatus(Graphics2D g2, BedrockBoss boss) {
        g2.setColor(BOSS_COLOR);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);

        int x = PANEL_X + 20;
        int barWidth = PANEL_WIDTH - 40;
        int y = PANEL_Y + 40;
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.drawString("BOSS: " + boss.getName(), x, y);

        long remaining = boss.getRemainingNanos();
        y += 45;
        g2.setColor(remaining <= LOW_TIME_NANOS ? Color.red : Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        g2.drawString("TIME  " + formatTime(remaining), x, y);
        y += 12;
        drawBar(g2, x, y, barWidth, (double) remaining / boss.getTimeLimitNanos(),
                remaining <= LOW_TIME_NANOS ? Color.red : Color.white);

        y += BAR_HEIGHT + 45;
        g2.setColor(Color.white);
        g2.drawString("SCORE  " + boss.getScore() + " / " + boss.getTargetScore(), x, y);
        y += 12;
        drawBar(g2, x, y, barWidth, (double) boss.getScore() / boss.getTargetScore(),
                Color.green);
    }

    /**
     * Draws the introduction screen that explains the boss before the fight.
     *
     * @param g2   the graphics to draw with
     * @param boss the boss to introduce
     */
    void drawIntro(Graphics2D g2, BedrockBoss boss) {
        drawBox(g2);
        int centerX = GamePanel.WIDTH / 2;
        int y = BOX_Y + 60;
        g2.setColor(BOSS_COLOR);
        g2.setFont(new Font("Arial", Font.BOLD, 44));
        Renderer.drawCentered(g2, "BOSS: " + boss.getName(), centerX, y);

        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        y += 55;
        // the description has two sentences, show one per line
        for (String sentence : boss.getDescription().split("(?<=[.!]) ")) {
            Renderer.drawCentered(g2, sentence, centerX, y);
            y += 30;
        }
        y += 15;
        g2.setFont(new Font("Arial", Font.PLAIN, 26));
        Renderer.drawCentered(g2, "TARGET " + boss.getTargetScore() + " POINTS   TIME "
                + formatTime(boss.getTimeLimitNanos()), centerX, y);

        g2.setColor(Color.yellow);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        Renderer.drawCentered(g2, "Press ENTER to begin", centerX, BOX_Y + BOX_HEIGHT - 30);
    }

    /**
     * Draws the screen that shows whether the boss was beaten.
     *
     * @param g2   the graphics to draw with
     * @param boss the boss that was fought
     */
    void drawResult(Graphics2D g2, BedrockBoss boss) {
        drawBox(g2);
        int centerX = GamePanel.WIDTH / 2;
        boolean won = boss.getOutcome() == BossOutcome.WON;
        g2.setColor(won ? Color.green : Color.red);
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        Renderer.drawCentered(g2, won ? "BOSS DEFEATED!" : "DEFEAT", centerX, BOX_Y + 90);

        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        Renderer.drawCentered(g2, "Boss score: " + boss.getScore() + " / "
                + boss.getTargetScore(), centerX, BOX_Y + 150);
        Renderer.drawCentered(g2, won ? "The bedrock crumbles away." : "Time ran out.",
                centerX, BOX_Y + 190);

        g2.setColor(Color.yellow);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        Renderer.drawCentered(g2, won ? "Press ENTER to continue" : "Press ENTER",
                centerX, BOX_Y + BOX_HEIGHT - 30);
    }

    /**
     * Dims the whole window and draws the empty box for the intro or result screen.
     *
     * @param g2 the graphics to draw with
     */
    private void drawBox(Graphics2D g2) {
        g2.setColor(DIM_COLOR);
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
        g2.setColor(Color.black);
        g2.fillRect(BOX_X, BOX_Y, BOX_WIDTH, BOX_HEIGHT);
        g2.setColor(BOSS_COLOR);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(BOX_X, BOX_Y, BOX_WIDTH, BOX_HEIGHT);
    }

    /**
     * Draws a horizontal progress bar.
     *
     * @param g2       the graphics to draw with
     * @param x        the left edge, in pixels
     * @param y        the top edge, in pixels
     * @param width    the full width of the bar, in pixels
     * @param fraction how full the bar is, from 0 to 1 (clamped)
     * @param color    the color of the filled part
     */
    private void drawBar(Graphics2D g2, int x, int y, int width, double fraction, Color color) {
        double clamped = Math.max(0, Math.min(1, fraction));
        g2.setColor(BAR_BACK_COLOR);
        g2.fillRect(x, y, width, BAR_HEIGHT);
        g2.setColor(color);
        g2.fillRect(x, y, (int) (width * clamped), BAR_HEIGHT);
    }

    /**
     * Formats a time as m:ss.t (minutes, seconds and tenths).
     *
     * @param nanos the time, in nanoseconds
     * @return the formatted time
     */
    private static String formatTime(long nanos) {
        long tenths = nanos / 100_000_000L;
        long minutes = tenths / 600;
        long seconds = (tenths / 10) % 60;
        return String.format("%d:%02d.%d", minutes, seconds, tenths % 10);
    }
}
