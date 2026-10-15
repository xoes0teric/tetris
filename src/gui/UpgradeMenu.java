package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import upgrades.Upgrade;
import upgrades.UpgradeSelection;

// Draws the upgrade selection screen over the game. It only shows the options,
// choosing is done by PlayManager
class UpgradeMenu {
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 120;
    private static final int CARD_GAP = 30;
    private static final int CARD_TOP = 300;

    void draw(Graphics2D g2, UpgradeSelection selection, boolean bossRunning) {
        List<Upgrade> options = selection.getOptions();
        int totalWidth = options.size() * CARD_WIDTH + (options.size() - 1) * CARD_GAP;
        int x = (GamePanel.WIDTH - totalWidth) / 2;
        for (int i = 0; i < options.size(); ++i) {
            // a thicker frame shows the selected one
            drawCard(g2, options.get(i), x, i == selection.getSelected());
            x += CARD_WIDTH + CARD_GAP;
        }
    }

    // A box with the upgrade's name in the color of its rarity
    private void drawCard(Graphics2D g2, Upgrade upgrade, int x, boolean thick) {
        g2.setColor(Color.black);
        g2.fillRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(thick ? 5f : 1f));
        g2.drawRect(x, CARD_TOP, CARD_WIDTH, CARD_HEIGHT);
        g2.setColor(upgrade.getRarity().getColor());
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        Renderer.drawCentered(g2, upgrade.getName(), x + CARD_WIDTH / 2,
                CARD_TOP + CARD_HEIGHT / 2 + 8);
    }
}
