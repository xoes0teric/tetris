package boss;

// Abilities a boss can use during its fight
public enum BossAbility {
    INVERT_CONTROLS("INVERT CONTROLS"), // left and right are swapped for a while
    FORCE_DROP("FORCE DROP"); // the falling piece is hard dropped

    private final String name;

    BossAbility(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
