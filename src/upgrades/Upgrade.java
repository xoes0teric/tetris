package upgrades;

import game.PlayManager;

/**
    Base class of every upgrade

    HOW TO ADD AN UPGRADE
    1. Make a class in this package that extends Upgrade
    2. Call super(name, rarity, maxLevel) in its constructor
    3. Override onPick if it should do something the moment it is picked,
       or add methods the game can ask (like Hold.canHold) if it changes a rule
    4. Add it to the list in Upgrader

    Every pick raises the level by one. Upgrades are offered until they reach maxLevel.
*/
public abstract class Upgrade {
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final String name;
    private final Rarity rarity;
    private final int maxLevel;
    private int level = 0;

    protected Upgrade(String name, Rarity rarity, int maxLevel) {
        this.name = name;
        this.rarity = rarity;
        this.maxLevel = maxLevel;
    }

    // Runs every time the upgrade is picked, after the level went up
    protected void onPick(PlayManager game) {
    }

    public final void pick(PlayManager game) {
        ++level;
        onPick(game);
    }

    public String getName() {
        return name;
    }

    public Rarity getRarity() {
        return rarity;
    }

    // How often the upgrade was picked, 0 if never
    public int getLevel() {
        return level;
    }

    public boolean isAvailable() {
        return level < maxLevel;
    }
}
