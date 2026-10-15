package gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
    The picture behind the game (assets/background.png)

    It is scaled once to fill the window, cutting off the sides or top if the
    picture has a different shape, so drawing it every frame is cheap.
    If the file is missing, nothing is drawn and the background stays black.
*/
class Background {
    // relative to the folder the game is started from (run.sh starts it in the repo)
    private static final String FILE = "assets/background.png";

    private static Image image;
    private static boolean loaded = false;

    static void draw(Graphics2D g2) {
        if (!loaded) {
            image = load();
            loaded = true;
        }
        if (image != null) {
            g2.drawImage(image, 0, 0, null);
        }
    }

    // The picture scaled to cover the whole window, null if it can't be read
    private static Image load() {
        try {
            BufferedImage original = ImageIO.read(new File(FILE));
            if (original == null) {
                return null;
            }
            double scale = Math.max((double) GamePanel.WIDTH / original.getWidth(),
                    (double) GamePanel.HEIGHT / original.getHeight());
            int width = (int) Math.ceil(original.getWidth() * scale);
            int height = (int) Math.ceil(original.getHeight() * scale);

            BufferedImage scaled = new BufferedImage(GamePanel.WIDTH, GamePanel.HEIGHT,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // centered, so the same amount is cut off on both sides
            g2.drawImage(original, (GamePanel.WIDTH - width) / 2,
                    (GamePanel.HEIGHT - height) / 2, width, height, null);
            g2.dispose();
            return scaled;
        } catch (Exception e) {
            return null;
        }
    }
}
