package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import upgrades.Upgrade;
import upgrades.UpgradeSelection;

/**
 * Draws the upgrade selection screen over the game. It only shows the options,
 * choosing is done by PlayManager.
 */
class UpgradeMenu {
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 110;
    private static final int CARD_GAP = 30;
    private static final int CARD_TOP = 210;
    private static final int CARD_PADDING = 20;
    private static final Color DIM_COLOR = new Color(0, 0, 0, 200);
    private static final Color CARD_COLOR = new Color(30, 30, 40);
    private static final Color SELECTED_COLOR = Color.yellow;

    /**
     * Dims the game and draws one card per option, with the highlighted one in yellow.
     *
     * @param g2          the graphics to draw with
     * @param selection   the options and which one is highlighted
     * @param bossRunning whether to warn that the boss timer is still running
     */
    void draw(Graphics2D g2, UpgradeSelection selection, boolean bossRunning) {
        g2.setColor(DIM_COLOR);
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 40));
        Renderer.drawCentered(g2, "Choose an upgrade", GamePanel.WIDTH / 2, 150);

        List<Upgrade> options = selection.getOptions();
        int totalWidth = options.size() * CARD_WIDTH + (options.size() - 1) * CARD_GAP;
        int x = (GamePanel.WIDTH - totalWidth) / 2;
        for (int i = 0; i < options.size(); ++i) {
            drawCard(g2, options.get(i), i, x, i == selection.getSelected());
            x += CARD_WIDTH + CARD_GAP;
        }

        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        String numbers = options.size() == 1 ? "1" : "1-" + options.size();
        Renderer.drawCentered(g2, "LEFT/RIGHT: choose   ENTER: take it   or press " + numbers,
                GamePanel.WIDTH / 2, CARD_TOP + CARD_HEIGHT + 60);
        if (bossRunning) {
            g2.setColor(Color.red);
            Renderer.drawCentered(g2, "The boss timer is still running!",
                    GamePanel.WIDTH / 2, CARD_TOP + CARD_HEIGHT + 95);
        }
    }

    /**
     * Draws one option card with its number, name and level.
     *
     * @param g2       the graphics to draw with
     * @param upgrade  the upgrade on the card
     * @param index    the position of the card, starting at 0
     * @param x        the left edge of the card, in pixels
     * @param selected whether the card is highlighted
     */
    private void drawCard(Graphics2D g2, Upgrade upgrade, int index, int x, boolean selected) {
        g2.setColor(CARD_COLOR);
        g2.fillRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);
        g2.setColor(selected ? SELECTED_COLOR : Color.gray);
        g2.setStroke(new BasicStroke(selected ? 5f : 2f));
        g2.drawRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);

        int textX = x + CARD_PADDING;
        int y = CARD_TOP + 45;
        g2.setColor(selected ? SELECTED_COLOR : Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        g2.drawString((index + 1) + ". " + upgrade.getName(), textX, y);

        y += 35;
        g2.setColor(Color.lightGray);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString(levelText(upgrade), textX, y);
    }

    /**
     * Returns the level line of a card, for example "Level 1 -> 2   (Max 3)".
     *
     * @param upgrade the upgrade on the card
     * @return the text to show under the name
     */
    private String levelText(Upgrade upgrade) {
        if (upgrade.isSingleUse()) {
            return "Single Use";
        }
        return "Level " + upgrade.getLevel() + " -> " + (upgrade.getLevel() + 1)
                + "   (Max " + upgrade.getMaxLevel() + ")";
    }
}
