package gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import upgrades.Spin;
import upgrades.Upgrade;

/**
    Draws the Gamble spin like a case opening: a strip of cards slides right to left
    under a marker, slows down, stops, and the winning card is revealed.

    Everything is calculated from Spin.getPosition() every frame, this class only
    remembers what it needs for the sounds (which card last ticked, if the reveal
    sound was played).
*/
class SpinView {
    // The window the strip is seen through, in pixels
    private static final int PANEL_WIDTH = 1040;
    private static final int PANEL_HEIGHT = 210;
    private static final int PANEL_X = (GamePanel.WIDTH - PANEL_WIDTH) / 2;
    private static final int PANEL_Y = 255;
    private static final int CENTER_X = GamePanel.WIDTH / 2;

    // One card of the strip, in pixels
    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 160;
    private static final int CARD_GAP = 10;
    private static final int STEP = CARD_WIDTH + CARD_GAP; // pixels per item of position
    private static final int CARD_Y = PANEL_Y + (PANEL_HEIGHT - CARD_HEIGHT) / 2;
    private static final int RARITY_BAR_HEIGHT = 7;
    private static final int CARD_PADDING = 10;

    private static final int FADE_WIDTH = 190; // dark edges of the window
    private static final int MARKER_TRIANGLE = 14;
    private static final int RAY_COUNT = 14;
    private static final double WINNER_GROWTH = 0.10; // the winning card grows by 10%
    private static final double BLUR_START_SPEED = 3; // items per second
    private static final double BLUR_FULL_SPEED = 25;

    private static final Color DIM_COLOR = new Color(0, 0, 0, 215);
    private static final Color PANEL_COLOR = new Color(10, 10, 14);
    private static final Color PANEL_BORDER_COLOR = new Color(90, 90, 100);
    private static final Color CARD_COLOR = new Color(30, 30, 38);
    private static final Color MARKER_COLOR = new Color(255, 215, 0);
    private static final Color TEXT_COLOR = Color.white;

    private final SpinSound sound;
    private Spin lastSpin; // to notice when a new spin starts
    private int lastTickedItem;
    private boolean revealSoundPlayed;

    SpinView(SoundMixer mixer) {
        sound = new SpinSound(mixer);
    }

    void draw(Graphics2D g2, Spin spin, boolean paused) {
        if (spin != lastSpin) {
            lastSpin = spin;
            lastTickedItem = itemUnderMarker(spin.getPosition());
            revealSoundPlayed = false;
            sound.startSpin();
        }
        sound.setPaused(paused);
        playSounds(spin);

        Object oldAntialias = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(DIM_COLOR);
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
        drawHeader(g2, spin);

        g2.setColor(PANEL_COLOR);
        g2.fillRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);

        // everything inside the window is clipped to it
        Shape oldClip = g2.getClip();
        g2.clipRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        if (spin.hasLanded()) {
            drawWinnerGlow(g2, spin);
        }
        drawStrip(g2, spin);
        drawEdgeFades(g2);
        g2.setClip(oldClip);

        g2.setColor(PANEL_BORDER_COLOR);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        drawMarker(g2, spin);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
    }

    // ---- Sounds ----

    // A tick every time a new card reaches the marker, a chime when the result is revealed
    private void playSounds(Spin spin) {
        int item = itemUnderMarker(spin.getPosition());
        if (item != lastTickedItem && !spin.hasLanded()) {
            lastTickedItem = item;
            sound.tick();
        }
        if (spin.hasLanded() && !revealSoundPlayed) {
            revealSoundPlayed = true;
            sound.reveal(spin.getResult().getRarity());
        }
    }

    // Cards are centered on whole positions, so the card under the marker is the nearest one
    private static int itemUnderMarker(double position) {
        return (int) Math.floor(position + 0.5);
    }

    // ---- Header ----

    // "GAMBLE" while spinning, the result after it stops
    private void drawHeader(Graphics2D g2, Spin spin) {
        int y = PANEL_Y - 60;
        if (!spin.hasLanded()) {
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            Renderer.drawCentered(g2, "GAMBLE", CENTER_X, y);
            return;
        }
        Upgrade result = spin.getResult();
        float fadeIn = (float) Math.min(1.0, spin.getRevealProgress() * 4);
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeIn));

        g2.setColor(result.getRarity().getColor());
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        Renderer.drawCentered(g2, result.getRarity().name(), CENTER_X, y - 40);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        Renderer.drawCentered(g2, result.getName(), CENTER_X, y);

        g2.setComposite(oldComposite);
    }

    // ---- Strip ----

    private void drawStrip(Graphics2D g2, Spin spin) {
        double position = spin.getPosition();
        List<Upgrade> items = spin.getItems();
        float blur = blurAmount(spin.getSpeed());
        double reveal = easeOut(spin.getRevealProgress() * 3); // quick, then stays

        // only the cards that can be seen, plus one on each side
        int visibleHalf = PANEL_WIDTH / STEP / 2 + 2;
        int first = Math.max(0, itemUnderMarker(position) - visibleHalf);
        int last = Math.min(items.size() - 1, itemUnderMarker(position) + visibleHalf);

        // the winner is drawn last so it can grow over its neighbours
        for (int i = first; i <= last; ++i) {
            if (spin.hasLanded() && i == spin.getResultIndex()) {
                continue;
            }
            int x = cardX(i, position);
            drawCard(g2, items.get(i), x, CARD_Y, blur);
            if (spin.hasLanded()) {
                // the losers fade into the dark
                g2.setColor(new Color(0, 0, 0, (int) (170 * reveal)));
                g2.fillRect(x, CARD_Y, CARD_WIDTH, CARD_HEIGHT);
            }
        }
        if (spin.hasLanded()) {
            drawWinner(g2, spin, reveal);
        }
    }

    // Left edge of item i's card when the strip is at position, in pixels
    private static int cardX(int item, double position) {
        return (int) Math.round(CENTER_X + (item - position) * STEP - CARD_WIDTH / 2.0);
    }

    // 0 when the strip is slow, up to 1 when it is very fast
    private static float blurAmount(double speed) {
        double amount = (speed - BLUR_START_SPEED) / (BLUR_FULL_SPEED - BLUR_START_SPEED);
        return (float) Math.max(0, Math.min(1, amount));
    }

    // A card: dark top, glowing into its rarity color at the bottom, with the name
    private void drawCard(Graphics2D g2, Upgrade upgrade, int x, int y, float blur) {
        Color rarityColor = upgrade.getRarity().getColor();

        g2.setPaint(new GradientPaint(x, y, CARD_COLOR, x, y + CARD_HEIGHT,
                mix(CARD_COLOR, rarityColor, 0.35)));
        g2.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
        g2.setColor(rarityColor);
        g2.fillRect(x, y + CARD_HEIGHT - RARITY_BAR_HEIGHT, CARD_WIDTH, RARITY_BAR_HEIGHT);
        g2.setColor(mix(CARD_COLOR, Color.white, 0.12));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT);

        // fast cards are hard to read, like a real spin
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - 0.75f * blur));

        g2.setColor(TEXT_COLOR);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        List<String> lines = wrap(g2, upgrade.getName(), CARD_WIDTH - 2 * CARD_PADDING);
        int lineHeight = 23;
        int textY = y + CARD_HEIGHT / 2 - (lines.size() * lineHeight) / 2 + 12;
        for (String line : lines) {
            Renderer.drawCentered(g2, line, x + CARD_WIDTH / 2, textY);
            textY += lineHeight;
        }
        g2.setColor(rarityColor);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        Renderer.drawCentered(g2, upgrade.getRarity().name(), x + CARD_WIDTH / 2,
                y + CARD_HEIGHT - RARITY_BAR_HEIGHT - 10);

        g2.setComposite(oldComposite);
    }

    // The winning card grows a little and gets a pulsing frame in its rarity color
    private void drawWinner(Graphics2D g2, Spin spin, double reveal) {
        int x = cardX(spin.getResultIndex(), spin.getPosition());
        Color rarityColor = spin.getResult().getRarity().getColor();
        double scale = 1 + WINNER_GROWTH * reveal;
        double centerX = x + CARD_WIDTH / 2.0;
        double centerY = CARD_Y + CARD_HEIGHT / 2.0;

        AffineTransform oldTransform = g2.getTransform();
        g2.translate(centerX, centerY);
        g2.scale(scale, scale);
        g2.translate(-centerX, -centerY);

        drawCard(g2, spin.getResult(), x, CARD_Y, 0f);
        double pulse = 0.5 + 0.5 * Math.sin(spin.getRevealProgress() * Math.PI * 6);
        g2.setColor(withAlpha(rarityColor, (int) (140 + 115 * pulse)));
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(x - 2, CARD_Y - 2, CARD_WIDTH + 4, CARD_HEIGHT + 4);

        g2.setTransform(oldTransform);
    }

    // Light rays turning slowly behind the winner, and a soft glow
    private void drawWinnerGlow(Graphics2D g2, Spin spin) {
        Color rarityColor = spin.getResult().getRarity().getColor();
        double reveal = easeOut(spin.getRevealProgress() * 2);
        int centerX = cardX(spin.getResultIndex(), spin.getPosition()) + CARD_WIDTH / 2;
        int centerY = CARD_Y + CARD_HEIGHT / 2;

        float radius = PANEL_HEIGHT * 1.4f;
        g2.setPaint(new RadialGradientPaint(centerX, centerY, radius, new float[]{0f, 1f},
                new Color[]{withAlpha(rarityColor, (int) (110 * reveal)),
                    withAlpha(rarityColor, 0)}));
        g2.fillRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);

        double turn = spin.getRevealProgress() * Math.PI / 3;
        double rayLength = PANEL_WIDTH / 2.0;
        double rayWidth = Math.PI / RAY_COUNT / 2; // half of each slice is a ray
        g2.setColor(withAlpha(rarityColor, (int) (45 * reveal)));
        for (int i = 0; i < RAY_COUNT; ++i) {
            double angle = turn + i * 2 * Math.PI / RAY_COUNT;
            Polygon ray = new Polygon();
            ray.addPoint(centerX, centerY);
            ray.addPoint((int) (centerX + Math.cos(angle - rayWidth) * rayLength),
                    (int) (centerY + Math.sin(angle - rayWidth) * rayLength));
            ray.addPoint((int) (centerX + Math.cos(angle + rayWidth) * rayLength),
                    (int) (centerY + Math.sin(angle + rayWidth) * rayLength));
            g2.fillPolygon(ray);
        }
    }

    // The window's edges fade to black, so cards slide in and out of the dark
    private void drawEdgeFades(Graphics2D g2) {
        Color clear = withAlpha(PANEL_COLOR, 0);
        g2.setPaint(new GradientPaint(PANEL_X, 0, PANEL_COLOR, PANEL_X + FADE_WIDTH, 0, clear));
        g2.fillRect(PANEL_X, PANEL_Y, FADE_WIDTH, PANEL_HEIGHT);
        int right = PANEL_X + PANEL_WIDTH;
        g2.setPaint(new GradientPaint(right - FADE_WIDTH, 0, clear, right, 0, PANEL_COLOR));
        g2.fillRect(right - FADE_WIDTH, PANEL_Y, FADE_WIDTH, PANEL_HEIGHT);
    }

    // The line in the middle that picks the winner, with a triangle above and below
    private void drawMarker(Graphics2D g2, Spin spin) {
        Color color = spin.hasLanded() ? spin.getResult().getRarity().getColor() : MARKER_COLOR;
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(CENTER_X, PANEL_Y + 4, CENTER_X, PANEL_Y + PANEL_HEIGHT - 4);

        int top = PANEL_Y - 2;
        int bottom = PANEL_Y + PANEL_HEIGHT + 2;
        g2.fillPolygon(new int[]{CENTER_X - MARKER_TRIANGLE, CENTER_X + MARKER_TRIANGLE,
            CENTER_X}, new int[]{top - MARKER_TRIANGLE, top - MARKER_TRIANGLE, top + 4}, 3);
        g2.fillPolygon(new int[]{CENTER_X - MARKER_TRIANGLE, CENTER_X + MARKER_TRIANGLE,
            CENTER_X}, new int[]{bottom + MARKER_TRIANGLE, bottom + MARKER_TRIANGLE, bottom - 4},
            3);
    }

    // ---- Helpers ----

    // 0 to 1, fast at first and slow at the end. Values above 1 count as 1
    private static double easeOut(double t) {
        double clamped = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - clamped, 3);
    }

    // amount 0 gives a, 1 gives b
    private static Color mix(Color a, Color b, double amount) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    private static Color withAlpha(Color color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }

    // Splits the text into lines that fit in width pixels
    private static List<String> wrap(Graphics2D g2, String text, int width) {
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
