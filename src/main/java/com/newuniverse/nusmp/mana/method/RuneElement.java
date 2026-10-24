package com.newuniverse.nusmp.mana.method;

import java.util.Locale;

/**
 * The elements a Mana Method rune can be written in. Pure data (no game classes). The first four are the basic elements every Mana Method page offers; LIGHTNING and PLANT exist
 * for the True Magic page only (they follow the natural attributes of {@link com.newuniverse.nusmp.mana.zone.TrueMagicRules#NATURAL}). The ordinal travels in the packet and in the
 * spell's NBT, so entries are only ever APPENDED.
 *
 * <p>{@code glyph} is the id of the custom rune particle of {@code particle_catalog.json} (family rune) the symbols of the rune are drawn with, {@code tint} the colour of the
 * array (RGB).
 */
public enum RuneElement {
    FIRE("Flame", "rune_flame", 0xFF7A1A),
    WATER("Water", "rune_wave", 0x3FA9FF),
    WIND("Wind", "rune_luna", 0x9FE8C8),
    EARTH("Earth", "rune_key", 0xB59A6A),
    LIGHTNING("Lightning", "rune_bolt", 0xFFE45C),
    PLANT("Plant", "rune_leaf", 0x6CD65C);

    /** The particle that traces the rings of every rune (tinted). */
    public static final String RING_PARTICLE = "mana_mote";

    public final String title, glyph;
    public final int tint;

    RuneElement(String title, String glyph, int tint) {
        this.title = title;
        this.glyph = glyph;
        this.tint = tint;
    }

    /** The four basic elements (the pages Flame, Water, Wind and Earth Rune). */
    public static final RuneElement[] BASIC = {FIRE, WATER, WIND, EARTH};

    /** The ordinal from a packet or a tag; a bad number gives FIRE (never an exception). */
    public static RuneElement byOrdinal(int o) {
        RuneElement[] v = values();
        return o >= 0 && o < v.length ? v[o] : FIRE;
    }

    /** The element of a natural attribute (a MagicType name: FLAME, WATER, WIND, EARTH, LIGHTNING, PLANT); null for any other. */
    public static RuneElement ofMagic(String magicName) {
        if (magicName == null) return null;
        return switch (magicName.toUpperCase(Locale.ROOT)) {
            case "FLAME" -> FIRE;
            case "WATER" -> WATER;
            case "WIND" -> WIND;
            case "EARTH" -> EARTH;
            case "LIGHTNING" -> LIGHTNING;
            case "PLANT" -> PLANT;
            default -> null;
        };
    }

    /** The default modifier of the element's page: water and wind PURSUE, the others GROW. Sneaking while casting writes both runes. */
    public int defaultMods() {
        return this == WATER || this == WIND ? RuneMods.HOMING : RuneMods.GROWTH;
    }
}
