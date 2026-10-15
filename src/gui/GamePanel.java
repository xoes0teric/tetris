package gui;

import game.GameState;
import game.PlayManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import tetromino.Tetromino;

public class GamePanel extends JPanel implements Runnable {
    // Window size, in pixels
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    static final int FPS = 60;
    Thread gameThread;
    PlayManager pm;
    Renderer renderer;
    private long lastUpdateTime;

    // One mixer for all sounds, kept when the game is reset
    private final SoundMixer sound = new SoundMixer();
    private final float[] dropSound = SoundMixer.loadWav("assets/drop.wav");
    private static final float DROP_VOLUME = 0.8f;

    public GamePanel() {
        // Panel Settings
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setLayout(null);

        this.addKeyListener(new KeyHandler());
        this.setFocusable(true);

        pm = new PlayManager();
        renderer = new Renderer(sound);
    }

    public void launchGame() {
        lastUpdateTime = System.nanoTime();
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
        // Real time since the last update, the boss timer uses it.
        // Paused frames are skipped so the pause doesn't count
        long now = System.nanoTime();
        long elapsed = now - lastUpdateTime;
        lastUpdateTime = now;

        if (KeyHandler.resetPressed) {
            resetGame();
        }

        if (KeyHandler.pausePressed || pm.isGameOver()) {
            // keys pressed now must not do anything after unpausing
            KeyHandler.clearActions();
            return;
        }

        // Pass the pressed keys to the game, each screen only reads its own keys
        GameState stateBefore = pm.getGameState();
        switch (stateBefore) {
            case PLAY -> handlePlayKeys();
            case SELECT_UPGRADE -> handleUpgradeKeys();
            case BOSS_INTRO, BOSS_RESULT -> handleBossScreenKeys();
            default -> { }
        }
        Tetromino piece = pm.getCurrentPiece(); // the boss may force it down
        pm.update(elapsed);
        int[][] forcedFrom = pm.takeForcedDropStart();
        if (forcedFrom != null) {
            sound.play(dropSound, DROP_VOLUME);
            renderer.hardDropped(piece, forcedFrom);
        }

        // keys from one screen must not carry over to the next one
        if (pm.getGameState() != stateBefore) {
            KeyHandler.clearActions();
        }
    }

    private void handlePlayKeys() {
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
            Tetromino piece = pm.getCurrentPiece();
            int[][] before = piece.getBlocks();
            if (pm.hardDrop()) {
                sound.play(dropSound, DROP_VOLUME);
                renderer.hardDropped(piece, before);
            }
            KeyHandler.spacePressed = false;
        }
        if (KeyHandler.holdPressed) {
            pm.hold();
            KeyHandler.holdPressed = false;
        }
    }

    private void handleUpgradeKeys() {
        if (KeyHandler.leftPressed) {
            pm.moveUpgradeSelection(-1);
            KeyHandler.leftPressed = false;
        }
        if (KeyHandler.rightPressed) {
            pm.moveUpgradeSelection(1);
            KeyHandler.rightPressed = false;
        }
        if (KeyHandler.numberPressed > 0) {
            pm.selectUpgrade(KeyHandler.numberPressed - 1);
            KeyHandler.numberPressed = 0;
            pm.confirmUpgrade();
        } else if (KeyHandler.enterPressed || KeyHandler.spacePressed) {
            pm.confirmUpgrade();
        }
        KeyHandler.enterPressed = false;
        KeyHandler.spacePressed = false;
    }

    private void handleBossScreenKeys() {
        if (KeyHandler.enterPressed || KeyHandler.spacePressed) {
            pm.confirmBossScreen();
        }
        KeyHandler.enterPressed = false;
        KeyHandler.spacePressed = false;
    }

    private void resetGame() {
        sound.stopAll();
        pm = new PlayManager();
        renderer = new Renderer(sound);
        KeyHandler.clearActions();
        KeyHandler.pausePressed = false;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        renderer.draw(g2, pm, KeyHandler.pausePressed);
    }
}
