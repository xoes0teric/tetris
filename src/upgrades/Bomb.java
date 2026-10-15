package upgrades;

import game.PlayManager;
import tetromino.Shape;

/**
    Bomb makes the next piece a single bomb block. Where it lands, its whole row
    is cleared, even if it isn't full. PlayManager handles the explosion
*/
public class Bomb extends Upgrade {
    public Bomb() {
        super("Bomb", Rarity.COMMON, UNLIMITED);
    }

    @Override
    protected void onPick(PlayManager game) {
        game.getPicker().putNext(Shape.BOMB);
    }
}
