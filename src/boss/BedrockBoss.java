package boss;

import game.Board;

/**
 * Bedrock, the first boss.
 *
 * <p>It pushes unbreakable rows under the stack, so there is less room to play.
 * The player has to score TARGET_SCORE points before the time runs out.
 * Winning removes the rows again, losing ends the run.
 *
 * <p>The game asks the boss what to do, the boss doesn't control the game.
 * Its values can be changed with the second constructor to balance it.
 */
public class BedrockBoss {
    public static final int TRIGGER_LINES = 30;
    public static final int RESPAWN_LINES = 30; // lines after a win before it returns
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

    /**
     * Creates the boss with the default values.
     */
    public BedrockBoss() {
        this(TRIGGER_LINES, UNBREAKABLE_ROWS, TARGET_SCORE, TIME_LIMIT_NANOS);
    }

    /**
     * Creates the boss with custom values, for example to balance or test it.
     *
     * @param triggerLines    the cleared lines needed before the boss appears
     * @param unbreakableRows the number of unbreakable rows it adds
     * @param targetScore     the points needed to beat it
     * @param timeLimitNanos  the time the player has, in nanoseconds
     */
    public BedrockBoss(int triggerLines, int unbreakableRows, int targetScore,
            long timeLimitNanos) {
        this.triggerLines = triggerLines;
        this.unbreakableRows = unbreakableRows;
        this.targetScore = targetScore;
        this.timeLimitNanos = timeLimitNanos;
        this.remainingNanos = timeLimitNanos;
    }

    /**
     * Returns the name shown on screen.
     *
     * @return the boss name
     */
    public String getName() {
        return "BEDROCK";
    }

    /**
     * Returns the text shown on the introduction screen.
     *
     * @return a description of what the boss does and how to beat it
     */
    public String getDescription() {
        return unbreakableRows + " rows of unbreakable stone rise from below."
                + " Score " + targetScore + " points before time runs out!";
    }

    /**
     * Returns the cleared lines needed before the boss appears.
     *
     * @return the number of lines that triggers the boss
     */
    public int getTriggerLines() {
        return triggerLines;
    }

    /**
     * Creates a fresh copy of this boss that appears RESPAWN_LINES lines after
     * the given line count, used to bring the boss back after it is beaten.
     *
     * @param currentLines the lines cleared when this boss was beaten
     * @return a new boss with the same values and a later trigger
     */
    public BedrockBoss respawn(int currentLines) {
        return new BedrockBoss(currentLines + RESPAWN_LINES, unbreakableRows, targetScore,
                timeLimitNanos);
    }

    /**
     * Returns how many unbreakable rows the boss adds.
     *
     * @return the number of unbreakable rows
     */
    public int getUnbreakableRows() {
        return unbreakableRows;
    }

    /**
     * Starts the fight and adds the unbreakable rows. Does nothing if the fight
     * was already started.
     *
     * @param board the board to add the rows to
     * @return false if the stack was pushed out of the top (game over)
     */
    public boolean start(Board board) {
        if (outcome != BossOutcome.WAITING) {
            return true;
        }
        outcome = BossOutcome.RUNNING;
        remainingNanos = timeLimitNanos;
        return board.addUnbreakableRows(unbreakableRows);
    }

    /**
     * Removes the unbreakable rows after the boss is beaten.
     *
     * @param board the board to remove the rows from
     */
    public void end(Board board) {
        board.removeUnbreakableRows();
    }

    /**
     * Counts down the time and sets the outcome to LOST when it runs out.
     * Only called while the boss clock should run.
     *
     * @param elapsedNanos the time since the last tick, in nanoseconds
     */
    public void tick(long elapsedNanos) {
        if (outcome != BossOutcome.RUNNING) {
            return;
        }
        remainingNanos = Math.max(0, remainingNanos - elapsedNanos);
        if (remainingNanos == 0) {
            outcome = BossOutcome.LOST;
        }
    }

    /**
     * Adds points the player earned during the fight and sets the outcome to WON
     * once the target is reached.
     *
     * @param points the points earned
     */
    public void addScore(int points) {
        if (outcome != BossOutcome.RUNNING) {
            return;
        }
        score += points;
        if (score >= targetScore) {
            outcome = BossOutcome.WON;
        }
    }

    /**
     * Returns the stage the fight is in.
     *
     * @return the current outcome
     */
    public BossOutcome getOutcome() {
        return outcome;
    }

    /**
     * Returns the points scored during the fight.
     *
     * @return the boss fight score
     */
    public int getScore() {
        return score;
    }

    /**
     * Returns the points needed to beat the boss.
     *
     * @return the target score
     */
    public int getTargetScore() {
        return targetScore;
    }

    /**
     * Returns the time left in the fight.
     *
     * @return the remaining time, in nanoseconds
     */
    public long getRemainingNanos() {
        return remainingNanos;
    }

    /**
     * Returns the total time the player has to beat the boss.
     *
     * @return the time limit, in nanoseconds
     */
    public long getTimeLimitNanos() {
        return timeLimitNanos;
    }
}
