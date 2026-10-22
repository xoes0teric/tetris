package boss;

/**
 * The stage a boss fight is in.
 */
public enum BossOutcome {
    WAITING, // not started yet
    RUNNING, // the fight is going on
    WON, // the player reached the target score in time
    LOST // the time ran out
}
