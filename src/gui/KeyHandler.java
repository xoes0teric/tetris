package gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
    public static boolean holdPressed;
    public static int numberPressed = 0; // 1 to 3, 0 when none was pressed

    @Override
    public void keyTyped(KeyEvent e) {}

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
        if (code == KeyEvent.VK_C || code == KeyEvent.VK_SHIFT) {
            holdPressed = true;
        }
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
            numberPressed = code - KeyEvent.VK_1 + 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    // Forgets every pressed key except pause, so keys don't carry over between screens
    public static void clearActions() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        spacePressed = false;
        resetPressed = false;
        enterPressed = false;
        holdPressed = false;
        numberPressed = 0;
    }
}
