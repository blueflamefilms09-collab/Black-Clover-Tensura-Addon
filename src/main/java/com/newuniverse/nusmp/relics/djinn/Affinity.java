package com.newuniverse.nusmp.relics.djinn;

/**
 * The eight kinds of magic a Djinn can hold (the Magi guide: Heat, Lightning, Water, Wind, Life, Strength, Sound, Light). Pure data: the glue maps
 * each one to a Tensura element boost, a particle and a damage flavour.
 */
public enum Affinity {
    HEAT("Heat", 0xFF7A2A),
    LIGHTNING("Lightning", 0x6CC4FF),
    WATER("Water", 0x3AA8E8),
    WIND("Wind", 0x7CE0B0),
    LIFE("Life", 0x62CC62),
    STRENGTH("Strength", 0xE05A4A),
    SOUND("Sound", 0xB98AFF),
    LIGHT("Light", 0xFFE88A);

    public final String label;
    /** A colour that reads as this magic (0xRRGGBB). */
    public final int tint;

    Affinity(String label, int tint) {
        this.label = label;
        this.tint = tint;
    }

    public static Affinity byName(String name) {
        for (Affinity a : values()) if (a.name().equalsIgnoreCase(name)) return a;
        return null;
    }
}
