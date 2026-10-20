package com.newuniverse.nusmp.mana.zone;

/**
 * The accent of a caster's Mana Zone, from the soul type of their magic (pure). The numbers are ManaZoneGeometry.STYLE_*: the client tints the motes
 * of the zone towards the element, so a flame mage's zone throws embers of gold and a water mage's runs blue.
 */
public final class ManaZoneStyle {
    private ManaZoneStyle() {}

    public static final int ARCANE = 0, FLAME = 1, WATER = 2, WIND = 3, EARTH = 4, LIGHT = 5, DARK = 6, SPACE = 7, TIME = 8;

    /** MagicType.soulType() (FLAME, WATER, WIND, EARTH, LIGHT, DARKNESS, SPACE, TIME, BATTLE, FANTASY, EMPTY) to a style. */
    public static int of(String soulType) {
        if (soulType == null) return ARCANE;
        return switch (soulType.toUpperCase(java.util.Locale.ROOT)) {
            case "FLAME" -> FLAME;
            case "WATER" -> WATER;
            case "WIND" -> WIND;
            case "EARTH" -> EARTH;
            case "LIGHT" -> LIGHT;
            case "DARKNESS" -> DARK;
            case "SPACE" -> SPACE;
            case "TIME" -> TIME;
            default -> ARCANE;
        };
    }
}
