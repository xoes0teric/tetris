package tests;

import boss.BedrockBoss;
import game.GameState;
import game.PlayManager;
import gui.GamePanel;
import gui.Renderer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

// Draws every screen into an image to catch exceptions in the drawing code
final class RendererSmokeTest {
    private static final long SECOND = 1_000_000_000L;

    private RendererSmokeTest() {
    }

    private static void draw(PlayManager pm, boolean paused) {
        BufferedImage image = new BufferedImage(GamePanel.WIDTH, GamePanel.HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        new Renderer().draw(g2, pm, paused);
        g2.dispose();
    }

    static void testEveryScreenDraws() {
        BedrockBoss boss = new BedrockBoss(1, 4, 10_000, 100 * SECOND);
        PlayManager pm = new PlayManager(new Random(12), List.of(boss));
        draw(pm, false);
        draw(pm, true);

        for (int i = 0; i < 3; ++i) {
            pm.getUpgrader().getHold().getUpgrade().apply();
            pm.getUpgrader().getSeeTheFuture().getUpgrade().apply();
        }
        pm.getUpgrader().getHammer().getUpgrade().apply();
        pm.getUpgrader().getFlush().getUpgrade().apply();
        pm.hold(0);
        pm.hardDrop();
        pm.update(PlayManagerTest.FRAME);
        draw(pm, false);

        PlayManagerTest.fillRowsWithGap(pm.getBoard(), 19, 19);
        PlayManagerTest.patchColumn(pm, 0, 19, 1);
        Check.equal(GameState.BOSS_INTRO, pm.getGameState(), "intro");
        draw(pm, false);

        pm.confirmBossScreen();
        draw(pm, false);

        pm.useHammer();
        draw(pm, false);
        pm.cancelTarget();
        pm.getUpgrader().getPatch().getUpgrade().apply();
        pm.usePatch();
        pm.markTarget();
        draw(pm, false);
        pm.cancelTarget();

        PlayManagerTest.fillRowsWithGap(pm.getBoard(), 7, 15);
        for (int count : new int[]{3, 3, 3}) {
            PlayManagerTest.patchColumn(pm, 0, 15, count);
        }
        Check.equal(GameState.SELECT_UPGRADE, pm.getGameState(), "selection");
        draw(pm, false);
        draw(pm, true);
        pm.confirmUpgrade();

        pm.update(200 * SECOND);
        Check.equal(GameState.BOSS_RESULT, pm.getGameState(), "result");
        draw(pm, false);
        pm.confirmBossScreen();
        Check.equal(GameState.GAME_OVER, pm.getGameState(), "game over");
        draw(pm, false);
    }
}
