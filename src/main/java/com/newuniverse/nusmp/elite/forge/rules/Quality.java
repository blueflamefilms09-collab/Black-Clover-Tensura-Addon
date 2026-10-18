package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the six qualities the forge can roll, Common to Mythical (the owner's inventory: "quality rolling (Common to Mythical)"). The numbers are this mod's own
 * design (the original addon's source was not available): {@code minPoints} is the roll total at which a quality starts (see {@link QualityRoller}); {@code statBonus}
 * is the fraction added to the attack damage or the armour of the item; {@code durabilityBonus} the fraction added to its maximum durability. The component on a
 * stack stores {@link #level()} (1..6), the same scale as {@code EliteHooks.forgeBestQuality} (0 means none). Pure Java.
 */
public enum Quality {
    COMMON(1, "common", 0xAAAAAA, 0, 0.00, 0.00),
    UNCOMMON(2, "uncommon", 0x55FF55, 30, 0.04, 0.10),
    RARE(3, "rare", 0x5599FF, 45, 0.08, 0.20),
    EPIC(4, "epic", 0xB050FF, 60, 0.13, 0.35),
    LEGENDARY(5, "legendary", 0xFFAA00, 75, 0.20, 0.55),
    MYTHICAL(6, "mythical", 0xFF4466, 92, 0.30, 0.85);

    private final int level;
    private final String id;
    private final int rgb;
    private final int minPoints;
    private final double statBonus;
    private final double durabilityBonus;

    Quality(int level, String id, int rgb, int minPoints, double statBonus, double durabilityBonus) {
        this.level = level;
        this.id = id;
        this.rgb = rgb;
        this.minPoints = minPoints;
        this.statBonus = statBonus;
        this.durabilityBonus = durabilityBonus;
    }

    public int level() { return level; }

    public String id() { return id; }

    public int rgb() { return rgb; }

    public int minPoints() { return minPoints; }

    public double statBonus() { return statBonus; }

    public double durabilityBonus() { return durabilityBonus; }

    /** The quality of a level 1..6, or null for anything else (0 = none). */
    public static Quality ofLevel(int level) {
        for (Quality q : values()) if (q.level == level) return q;
        return null;
    }

    /** The quality a roll total falls in (below the first threshold is Common). */
    public static Quality byPoints(double points) {
        Quality best = COMMON;
        for (Quality q : values()) if (points >= q.minPoints) best = q;
        return best;
    }

    /** The quality of an id such as {@code "rare"}, or null. */
    public static Quality byId(String id) {
        if (id == null) return null;
        for (Quality q : values()) if (q.id.equalsIgnoreCase(id)) return q;
        return null;
    }

    /** The lower of two qualities. */
    public static Quality min(Quality a, Quality b) { return a.level <= b.level ? a : b; }
}
