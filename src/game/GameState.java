package game;

public enum GameState {
    PLAY,
    PAUSE,
    SELECT_UPGRADE, // choosing an upgrade, the board is frozen
    GAMBLE, // the Gamble spin is running, the board is frozen
    BOSS_INTRO, // boss introduction, its timer hasn't started yet
    BOSS_RESULT, // boss won or lost screen
    GAME_OVER
}
