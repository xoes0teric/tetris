package game;

/**
 * The screen the game is on. It decides which keys do something and what is drawn.
 */
public enum GameState {
    PLAY, // normal play, the piece falls
    PAUSE, // not used yet, pausing is handled by KeyHandler and GamePanel
    SELECT_UPGRADE, // choosing an upgrade, the board is frozen
    AIM_HAMMER, // choosing the row for the hammer, the board is frozen
    AIM_PATCH, // placing the patch blocks, the board is frozen
    BOSS_INTRO, // boss introduction, its timer hasn't started yet
    BOSS_RESULT, // boss won or lost screen
    GAME_OVER // the stack reached the top or a boss was lost
}
