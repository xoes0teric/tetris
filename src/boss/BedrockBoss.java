package boss;

import game.Board;

/**
    Bedrock, the first boss

    It pushes unbreakable rows under the stack, so there is less room to play.
    The player has to score TARGET_SCORE points before the time runs out.
    Winning removes the rows again, losing ends the run.

    The game asks the boss what to do, the boss doesn't control the game.
    Its values can be changed with the second constructor to balance it.
*/
public class BedrockBoss {
    public static final int TRIGGER_LINES = 10;
    public static final int UNBREAKABLE_ROWS = 4;
    public static final int TARGET_SCORE = 300;
    public static final long TIME_LIMIT_NANOS = 120_000_000_000L; // 2 minutes

    private final int triggerLines;
    private final int unbreakableRows;
    private final int targetScore;
    private final long timeLimitNanos;

    private BossOutcome outcome = BossOutcome.WAITING;
    private long remainingNanos;
    private int score = 0;

    public BedrockBoss() {
        this(TRIGGER_LINES, UNBREAKABLE_ROWS, TARGET_SCORE, TIME_LIMIT_NANOS);
    }

    public BedrockBoss(int triggerLines, int unbreakableRows, int targetScore,
            long timeLimitNanos) {
        this.triggerLines = triggerLines;
        this.unbreakableRows = unbreakableRows;
        this.targetScore = targetScore;
        this.timeLimitNanos = timeLimitNanos;
        this.remainingNanos = timeLimitNanos;
    }

    public String getName() {
        return "BEDROCK";
    }

    public String getDescription() {
        return unbreakableRows + " rows of unbreakable stone rise from below."
                + " Score " + targetScore + " points before time runs out!";
    }

    // Cleared lines needed before the boss appears
    public int getTriggerLines() {
        return triggerLines;
    }

    public int getUnbreakableRows() {
        return unbreakableRows;
    }

    // Starts the fight and adds the unbreakable rows.
    // Returns false if the stack was pushed out of the top (game over)
    public boolean start(Board board) {
        if (outcome != BossOutcome.WAITING) {
            return true;
        }
        outcome = BossOutcome.RUNNING;
        remainingNanos = timeLimitNanos;
        return board.addUnbreakableRows(unbreakableRows);
    }

    // Removes the unbreakable rows after the boss is beaten
    public void end(Board board) {
        board.removeUnbreakableRows();
    }

    // Counts down the time. Only called while the boss clock should run
    public void tick(long elapsedNanos) {
        if (outcome != BossOutcome.RUNNING) {
            return;
        }
        remainingNanos = Math.max(0, remainingNanos - elapsedNanos);
        if (remainingNanos == 0) {
            outcome = BossOutcome.LOST;
        }
    }

    // Points the player earned during the fight
    public void addScore(int points) {
        if (outcome != BossOutcome.RUNNING) {
            return;
        }
        score += points;
        if (score >= targetScore) {
            outcome = BossOutcome.WON;
        }
    }

    public BossOutcome getOutcome() {
        return outcome;
    }

    public int getScore() {
        return score;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public long getRemainingNanos() {
        return remainingNanos;
    }

    public long getTimeLimitNanos() {
        return timeLimitNanos;
    }
}
