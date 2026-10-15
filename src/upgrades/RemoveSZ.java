package upgrades;

import game.PlayManager;
import tetromino.Shape;

/**
    Remove S/Z takes the S and Z pieces out of the game for the rest of the run
*/
public class RemoveSZ extends Upgrade {
    public RemoveSZ() {
        super("Remove S/Z", Rarity.LEGENDARY, 1);
    }

    @Override
    protected void onPick(PlayManager game) {
        game.getPicker().removeShapes(Shape.S, Shape.Z);
    }
}
