package upgrades;

import game.PlayManager;

/**
    Slowdown makes the pieces fall slower, like going back one level of speed
*/
public class Slowdown extends Upgrade {
    public Slowdown() {
        super("Slowdown", Rarity.COMMON, UNLIMITED);
    }

    @Override
    protected void onPick(PlayManager game) {
        game.getTimer().slowDown();
    }
}
