package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import upgrades.Upgrade;
import upgrades.UpgradeSelection;

// Draws the upgrade selection screen over the game. It only shows the options,
// choosing is done by PlayManager
class UpgradeMenu {
    private static final int CARD_WIDTH = 320;
    private static final int CARD_HEIGHT = 260;
    private static final int CARD_GAP = 30;
    private static final int CARD_TOP = 210;
    private static final int CARD_PADDING = 20;
    private static final Color DIM_COLOR = new Color(0, 0, 0, 200);
    private static final Color CARD_COLOR = new Color(30, 30, 40);
    private static final Color SELECTED_COLOR = Color.yellow;

    void draw(Graphics2D g2, UpgradeSelection selection, boolean bossRunning) {
        g2.setColor(DIM_COLOR);
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        Renderer.drawCentered(g2, "CHOOSE AN UPGRADE", GamePanel.WIDTH / 2, 150);

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

    private void drawCard(Graphics2D g2, Upgrade upgrade, int index, int x, boolean selected) {
        g2.setColor(CARD_COLOR);
        g2.fillRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);
        g2.setColor(selected ? SELECTED_COLOR : Color.gray);
        g2.setStroke(new BasicStroke(selected ? 5f : 2f));
        g2.drawRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);

        int textX = x + CARD_PADDING;
        int y = CARD_TOP + 45;
        g2.setColor(selected ? SELECTED_COLOR : Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.drawString((index + 1) + ". " + upgrade.getName(), textX, y);

        y += 35;
        g2.setColor(Color.lightGray);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString(levelText(upgrade), textX, y);

        y += 40;
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        for (String line : wrap(g2, upgrade.getDescription(), CARD_WIDTH - 2 * CARD_PADDING)) {
            g2.drawString(line, textX, y);
            y += 26;
        }
    }

    private String levelText(Upgrade upgrade) {
        if (upgrade.isSingleUse()) {
            return "SINGLE USE";
        }
        return "LEVEL " + upgrade.getLevel() + " -> " + (upgrade.getLevel() + 1)
                + "   (MAX " + upgrade.getMaxLevel() + ")";
    }

    // Splits the text into lines that fit in width pixels
    private List<String> wrap(Graphics2D g2, String text, int width) {
        FontMetrics metrics = g2.getFontMetrics();
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String longer = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(longer) > width && !line.isEmpty()) {
                lines.add(line);
                line = word;
            } else {
                line = longer;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }
}
