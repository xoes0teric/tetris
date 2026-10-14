package tests;

import boss.BedrockBoss;
import boss.BossOutcome;
import game.Board;
import game.GameState;
import game.PlayManager;
import java.awt.Color;
import java.util.List;
import java.util.Random;

final class BossTest {
    private static final long SECOND = 1_000_000_000L;

    private BossTest() {
    }

    // A game whose boss shows up after the first cleared line
    static PlayManager gameWithBoss(BedrockBoss boss) {
        PlayManager pm = new PlayManager(new Random(11), List.of(boss));
        PlayManagerTest.fillRowsWithGap(pm.getBoard(), 19, 19);
        PlayManagerTest.patchColumn(pm, 0, 19, 1);
        Check.equal(GameState.BOSS_INTRO, pm.getGameState(), "boss intro");
        return pm;
    }

    static void testBossWinsWhenTargetReachedInTime() {
        BedrockBoss boss = new BedrockBoss();
        boss.start(new Board());
        boss.tick(SECOND);
        boss.addScore(BedrockBoss.TARGET_SCORE);
        Check.equal(BossOutcome.WON, boss.getOutcome(), "won");
        boss.tick(BedrockBoss.TIME_LIMIT_NANOS);
        Check.equal(BossOutcome.WON, boss.getOutcome(), "a won fight can't be lost");
    }

    static void testBossLosesWhenTimeRunsOut() {
        BedrockBoss boss = new BedrockBoss(1, 4, 100, 10 * SECOND);
        boss.start(new Board());
        boss.addScore(50);
        for (int i = 0; i < 9; ++i) {
            boss.tick(SECOND);
        }
        Check.equal(BossOutcome.RUNNING, boss.getOutcome(), "time left");
        boss.tick(2 * SECOND);
        Check.equal(BossOutcome.LOST, boss.getOutcome(), "lost");
        Check.equal(0L, boss.getRemainingNanos(), "no time left");
        boss.addScore(1000);
        Check.equal(BossOutcome.LOST, boss.getOutcome(), "a lost fight can't be won");
    }

    static void testBossFightVictoryInGame() {
        BedrockBoss boss = new BedrockBoss(1, 4, 20, 10 * SECOND);
        PlayManager pm = gameWithBoss(boss);
        Board board = pm.getBoard();
        board.fillCell(3, 19, Color.red);

        // the intro doesn't use up time
        pm.update(5 * SECOND);
        Check.equal(10 * SECOND, boss.getRemainingNanos(), "timer waits for the intro");

        pm.confirmBossScreen();
        Check.equal(GameState.PLAY, pm.getGameState(), "fight started");
        Check.equal(4, board.getUnbreakableRows(), "unbreakable rows");
        Check.equal(Color.red, board.getColor(3, 15), "stack pushed up");
        Check.boardValid(board);

        pm.update(SECOND);
        Check.equal(9 * SECOND, boss.getRemainingNanos(), "timer counts elapsed time");

        // 2 lines at level 1 are 20 points
        PlayManagerTest.fillRowsWithGap(board, 14, 15);
        PlayManagerTest.patchColumn(pm, 0, 15, 2);
        Check.equal(20, boss.getScore(), "boss score");
        Check.equal(BossOutcome.WON, boss.getOutcome(), "won");
        Check.equal(GameState.BOSS_RESULT, pm.getGameState(), "result screen");
        Check.equal(0, board.getUnbreakableRows(), "bedrock removed after winning");
        Check.boardValid(board);

        pm.update(60 * SECOND);
        Check.equal(BossOutcome.WON, boss.getOutcome(), "still won");
        pm.confirmBossScreen();
        Check.equal(GameState.PLAY, pm.getGameState(), "back to normal play");
        Check.equal(null, pm.getBoss(), "boss gone");
        pm.update(PlayManagerTest.FRAME);
        Check.equal(GameState.PLAY, pm.getGameState(), "boss doesn't come back");
    }

    static void testBossFightDefeatInGame() {
        BedrockBoss boss = new BedrockBoss(1, 4, 1000, 5 * SECOND);
        PlayManager pm = gameWithBoss(boss);
        pm.confirmBossScreen();
        pm.update(2 * SECOND);

        // aiming the hammer doesn't stop the clock
        pm.getUpgrader().getHammer().getUpgrade().apply();
        pm.useHammer();
        pm.update(2 * SECOND);
        Check.equal(SECOND, boss.getRemainingNanos(), "timer ran while aiming");
        pm.cancelTarget();

        pm.update(2 * SECOND);
        Check.equal(BossOutcome.LOST, boss.getOutcome(), "lost");
        Check.equal(GameState.BOSS_RESULT, pm.getGameState(), "result screen");
        Check.equal(4, pm.getBoard().getUnbreakableRows(), "rows stay after losing");
        pm.update(2 * SECOND);
        Check.equal(GameState.BOSS_RESULT, pm.getGameState(), "defeat only once");
        pm.confirmBossScreen();
        Check.equal(GameState.GAME_OVER, pm.getGameState(), "run is over");
    }

    static void testUpgradeMenuDuringBossDoesNotStopTimer() {
        BedrockBoss boss = new BedrockBoss(1, 4, 10_000, 100 * SECOND);
        PlayManager pm = gameWithBoss(boss);
        pm.confirmBossScreen();
        Board board = pm.getBoard();
        // 9 more lines reach the first milestone during the fight
        PlayManagerTest.fillRowsWithGap(board, 7, 15);
        for (int count : new int[]{3, 3, 3}) {
            PlayManagerTest.patchColumn(pm, 0, 15, count);
        }
        Check.equal(10, pm.getLines(), "lines");
        Check.equal(GameState.SELECT_UPGRADE, pm.getGameState(), "upgrade during boss");
        Check.equal(4, board.getUnbreakableRows(), "line clears can't remove bedrock");
        Check.boardValid(board);

        long before = boss.getRemainingNanos();
        pm.update(3 * SECOND);
        Check.equal(before - 3 * SECOND, boss.getRemainingNanos(), "timer ran in the menu");

        pm.confirmUpgrade();
        Check.equal(GameState.PLAY, pm.getGameState(), "back to the fight");
        pm.update(PlayManagerTest.FRAME);
        Check.equal(GameState.PLAY, pm.getGameState(), "no second selection");
    }

    static void testBossToolsCantTouchBedrock() {
        BedrockBoss boss = new BedrockBoss(1, 4, 10_000, 100 * SECOND);
        PlayManager pm = gameWithBoss(boss);
        pm.confirmBossScreen();
        Board board = pm.getBoard();

        board.fillCell(2, 15, Color.red);
        pm.getUpgrader().getHammer().getUpgrade().apply();
        pm.useHammer();
        pm.moveCursor(0, 10);
        pm.confirmTarget();
        Check.equal(0, board.countBlocks(15), "hammer hit the lowest playable row");

        PlayManagerTest.fillRowsWithGap(board, 13, 15);
        pm.getUpgrader().getFlush().getUpgrade().apply();
        pm.useFlush();
        Check.equal(0, PlayManagerTest.countAll(board), "flush removed the playable rows");

        pm.getUpgrader().getPatch().getUpgrade().apply();
        pm.usePatch();
        pm.moveCursor(0, 10);
        pm.markTarget();
        pm.confirmTarget();
        Check.equal(15, lowestFilledRow(board), "patch stays above bedrock");

        Check.equal(4, board.getUnbreakableRows(), "bedrock intact");
        Check.boardValid(board);
    }

    private static int lowestFilledRow(Board board) {
        for (int row = Board.ROWS - 1; row >= 0; --row) {
            if (board.countBlocks(row) > 0) {
                return row;
            }
        }
        return -1;
    }

    static void testBossStartTopOutEndsGame() {
        PlayManager pm = gameWithBoss(new BedrockBoss(1, 4, 100, 100 * SECOND));
        pm.getBoard().fillCell(9, 2, Color.red);
        pm.confirmBossScreen();
        Check.equal(GameState.GAME_OVER, pm.getGameState(), "stack pushed out of the top");
    }
}
