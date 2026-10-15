package upgrades;

import java.awt.Color;

/**
    How often an upgrade is offered. The weight is the chance compared to the others,
    so a COMMON upgrade shows up 6 times as often as a LEGENDARY one.
    CURSED upgrades are never offered, only Gamble can give them
*/
public enum Rarity {
    COMMON(60, Color.white),
    RARE(30, new Color(80, 160, 255)),
    LEGENDARY(10, new Color(255, 180, 0)),
    CURSED(0, new Color(230, 50, 50));

    private final int weight;
    private final Color color;

    Rarity(int weight, Color color) {
        this.weight = weight;
        this.color = color;
    }

    public int getWeight() {
        return weight;
    }

    public Color getColor() {
        return color;
    }
}
