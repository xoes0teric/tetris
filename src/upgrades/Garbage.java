package upgrades;

import game.PlayManager;

/**
    Garbage pushes rows with one gap up from the bottom. It is never offered,
    it is what you get when Gamble goes wrong
*/
public class Garbage extends Upgrade {
    private static final int ROWS = 2;

    public Garbage() {
        super("Garbage", Rarity.CURSED, UNLIMITED);
    }

    @Override
    protected void onPick(PlayManager game) {
        game.addGarbageRows(ROWS);
    }
}
