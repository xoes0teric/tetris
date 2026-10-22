package game;

import gui.GamePanel;
import javax.swing.JFrame;

/**
 * The starting point of the program. It opens the window and starts the game.
 */
public class Main {
    /**
     * Creates the game window, adds the game panel and starts the game loop.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        JFrame window = new JFrame("Tetris");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Add GamePanel to the window
        GamePanel gp = new GamePanel();
        window.add(gp);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gp.launchGame();

    }
}
