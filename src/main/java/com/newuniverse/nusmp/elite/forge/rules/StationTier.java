package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the four forge stations (Basic, Magisteel, Mythril, Dragonforge) differ in exactly two numbers, so they are data of one enum and the rest of the system asks
 * the enum. {@code qualityBonus} is the flat number of points the station adds to every roll; {@code ceiling} is the best quality it can produce at all (so a Basic
 * station can never make a Mythical item however well the player plays, and each upgrade opens the next quality); and a recipe of tier N needs a station of tier N
 * or better. The values are this mod's own design. Pure Java.
 */
public enum StationTier {
    BASIC(0, "basic", 0, Quality.RARE),
    MAGISTEEL(1, "magisteel", 4, Quality.EPIC),
    MYTHRIL(2, "mythril", 8, Quality.LEGENDARY),
    DRAGONFORGE(3, "dragonforge", 13, Quality.MYTHICAL);

    private final int index;
    private final String id;
    private final int qualityBonus;
    private final Quality ceiling;

    StationTier(int index, String id, int qualityBonus, Quality ceiling) {
        this.index = index;
        this.id = id;
        this.qualityBonus = qualityBonus;
        this.ceiling = ceiling;
    }

    public int index() { return index; }

    public String id() { return id; }

    public int qualityBonus() { return qualityBonus; }

    public Quality ceiling() { return ceiling; }

    /** The next tier up, or null for the last. */
    public StationTier next() { return index + 1 < values().length ? values()[index + 1] : null; }

    /** True when this station can forge a recipe of the given tier index. */
    public boolean canForge(int recipeTier) { return recipeTier <= index; }

    public static StationTier byIndex(int i) { return i >= 0 && i < values().length ? values()[i] : null; }

    public static StationTier byId(String id) {
        if (id == null) return null;
        for (StationTier t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
