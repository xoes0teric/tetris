package gui;

import game.PlayManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    // Window size, in pixels
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    static final int FPS = 60;
    Thread gameThread;
    PlayManager pm;
    Renderer renderer;

    public GamePanel() {
        // Panel Settings
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setLayout(null);

        this.addKeyListener(new KeyHandler());
        this.setFocusable(true);

        pm = new PlayManager();
        renderer = new Renderer();
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        // Game Loop
        double drawInterval = (double) 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

            // Wait a moment instead of checking the time nonstop
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void update() {
        if (KeyHandler.resetPressed) {
            resetGame();
        }

        if (!KeyHandler.pausePressed && !pm.isGameOver()) {
            // Pass the pressed keys to the game
            if (KeyHandler.upPressed) {
                pm.rotate();
                KeyHandler.upPressed = false;
            }
            if (KeyHandler.downPressed) {
                pm.moveDown();
                KeyHandler.downPressed = false;
            }
            if (KeyHandler.leftPressed) {
                pm.moveLeft();
                KeyHandler.leftPressed = false;
            }
            if (KeyHandler.rightPressed) {
                pm.moveRight();
                KeyHandler.rightPressed = false;
            }
            if (KeyHandler.spacePressed) {
                pm.hardDrop();
                KeyHandler.spacePressed = false;
            }
            pm.update();
        }
    }

    private void resetGame() {
        pm = new PlayManager();
        renderer = new Renderer();
        KeyHandler.resetPressed = false;
        KeyHandler.pausePressed = false;
        KeyHandler.upPressed = false;
        KeyHandler.downPressed = false;
        KeyHandler.leftPressed = false;
        KeyHandler.rightPressed = false;
        KeyHandler.spacePressed = false;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        renderer.draw(g2, pm, KeyHandler.pausePressed);
    }
}
