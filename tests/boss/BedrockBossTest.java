package boss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import game.Board;

/**
 * Tests for the Bedrock boss target score. Run them with ./test.sh
 */
class BedrockBossTest {

    @Test
    void targetIsBaseAtLevelOne() {
        BedrockBoss boss = new BedrockBoss();
        assertEquals(BedrockBoss.TARGET_SCORE, boss.getTargetScore());
    }

    @Test
    void targetScalesWithLevel() {
        BedrockBoss boss = new BedrockBoss();
        boss.setLevel(3);
        assertEquals(BedrockBoss.TARGET_SCORE * 3, boss.getTargetScore());
    }

    @Test
    void baseScoreDoesNotWinAtHigherLevel() {
        BedrockBoss boss = new BedrockBoss();
        boss.setLevel(3);
        boss.start(new Board());
        boss.addScore(BedrockBoss.TARGET_SCORE);
        assertEquals(BossOutcome.RUNNING, boss.getOutcome());
    }

    @Test
    void scaledScoreWins() {
        BedrockBoss boss = new BedrockBoss();
        boss.setLevel(3);
        boss.start(new Board());
        boss.addScore(BedrockBoss.TARGET_SCORE * 3);
        assertEquals(BossOutcome.WON, boss.getOutcome());
    }
}
