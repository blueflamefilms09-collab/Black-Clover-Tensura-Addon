package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.MagicType;

/** The 14 mana affinities the id generator draws its aura glow from (render layer 3, tintindex 3). APPEND ONLY. */
public enum AuraAffinity {
    ANTI_MAGIC("Anti-Magic", 0x2A1A33),
    FIRE("Fire", 0xFF4A1F),
    WATER("Water", 0x3F8CFF),
    WIND("Wind", 0x4DFF9A),
    LIGHTNING("Lightning", 0x5AE6FF),
    DARK("Dark", 0xA04CFF),
    EARTH("Earth", 0xB07A3C),
    LIGHT("Light", 0xFFE680),
    TIME("Time", 0xE8C050),
    SPATIAL("Spatial", 0xB070FF),
    ICE("Ice", 0x9FE8FF),
    PLANT("Plant", 0x7CE04A),
    BLOOD("Blood", 0xE0182C),
    CREATION("Creation", 0xFFFFFF);

    public final String displayName;
    public final int color;

    AuraAffinity(String displayName, int color) { this.displayName = displayName; this.color = color; }

    /** The grimoire magic a book with this aura wields when the generator hands out a playable one. */
    public MagicType magic() {
        return switch (this) {
            case ANTI_MAGIC -> MagicType.ANTI_MAGIC; case FIRE -> MagicType.FLAME; case WATER -> MagicType.WATER; case WIND -> MagicType.WIND;
            case LIGHTNING -> MagicType.LIGHTNING; case DARK -> MagicType.DARK; case EARTH -> MagicType.EARTH; case LIGHT -> MagicType.LIGHT;
            case TIME -> MagicType.TIME; case SPATIAL -> MagicType.SPATIAL; case ICE -> MagicType.ICE; case PLANT -> MagicType.PLANT;
            case BLOOD -> MagicType.BLOOD; case CREATION -> MagicType.CREATION;
        };
    }

    private static final AuraAffinity[] VALUES = values();
    public static AuraAffinity byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }
}
