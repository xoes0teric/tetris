package gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Remembers which keys were pressed so the game loop can read them.
 *
 * <p>Pressing a key only sets a flag. The game loop (GamePanel) reads the flag
 * on its next frame and sets it back. Pause is a toggle and stays on until
 * P is pressed again.
 */
public class KeyHandler implements KeyListener {
    // TODO: All of these should be private
    public static boolean upPressed;
    public static boolean downPressed;
    public static boolean leftPressed;
    public static boolean rightPressed;
    public static boolean pausePressed;
    public static boolean spacePressed;
    public static boolean resetPressed;
    public static boolean enterPressed;
    public static boolean escapePressed;
    public static boolean hammerPressed;
    public static boolean patchPressed;
    public static boolean flushPressed;
    public static int holdSlotPressed = -1; // -1 when no hold key was pressed
    public static int numberPressed = 0; // 1 to 3, 0 when none was pressed

    /**
     * Not used.
     *
     * @param e the key event
     */
    @Override
    public void keyTyped(KeyEvent e) {}

    /**
     * Sets the flag of the pressed key. P toggles pause.
     *
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (code == KeyEvent.VK_SPACE) {
            spacePressed = true;
        }
        if (code == KeyEvent.VK_R) {
            resetPressed = true;
        }
        if (code == KeyEvent.VK_P) {
            if (pausePressed) {
                pausePressed = false;
            } else {
                pausePressed = true;
            }
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escapePressed = true;
        }
        if (code == KeyEvent.VK_H) {
            hammerPressed = true;
        }
        if (code == KeyEvent.VK_G) {
            patchPressed = true;
        }
        if (code == KeyEvent.VK_F) {
            flushPressed = true;
        }
        if (code == KeyEvent.VK_C || code == KeyEvent.VK_SHIFT) {
            holdSlotPressed = 0;
        }
        if (code == KeyEvent.VK_V) {
            holdSlotPressed = 1;
        }
        if (code == KeyEvent.VK_B) {
            holdSlotPressed = 2;
        }
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
            numberPressed = code - KeyEvent.VK_1 + 1;
        }
    }

    /**
     * Not used.
     *
     * @param e the key event
     */
    @Override
    public void keyReleased(KeyEvent e) {}

    /**
     * Forgets every pressed key except pause, so keys don't carry over between screens.
     */
    public static void clearActions() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        spacePressed = false;
        resetPressed = false;
        enterPressed = false;
        escapePressed = false;
        hammerPressed = false;
        patchPressed = false;
        flushPressed = false;
        holdSlotPressed = -1;
        numberPressed = 0;
    }
}
