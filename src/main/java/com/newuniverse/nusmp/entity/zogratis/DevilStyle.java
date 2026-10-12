package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;

/**
 * Colours and names that go with each devil of the Zogratis family (pure; {@link Devil} has no Minecraft types). Colours are
 * 0xRRGGBB. Used by the boss bar, the telegraph dust, the Dark Disciples' glow and the client renderers.
 */
public final class DevilStyle {
    private DevilStyle() {}

    public static final int GRAVITY = 0x5B2A8C, CURSE = 0x9E1B22, SPATIAL = 0x3A52D6, TIME = 0xE8B923, NEUTRAL = 0x8A8A9A;

    /** Main glow colour: Lucifero violet, Megicula blood red, Beelzebub indigo, Astaroth gold. */
    public static int color(Devil d) {
        if (d == null) return NEUTRAL;
        return switch (d) {
            case LUCIFERO -> GRAVITY;
            case MEGICULA -> CURSE;
            case BEELZEBUB -> SPATIAL;
            case ASTAROTH -> TIME;
            default -> NEUTRAL;
        };
    }

    /** A lighter accent for rims and sparks. */
    public static int accent(Devil d) {
        if (d == null) return 0xC8C8D8;
        return switch (d) {
            case LUCIFERO -> 0xB58CFF;
            case MEGICULA -> 0xFF5A4A;
            case BEELZEBUB -> 0x8FB0FF;
            case ASTAROTH -> 0xFFF0A0;
            default -> 0xC8C8D8;
        };
    }

    /** The magic a devil lends its contractor. */
    public static String magic(Devil d) {
        if (d == null) return "Devil";
        return switch (d) {
            case LUCIFERO -> "Gravity";
            case MEGICULA -> "Curse-Warding";
            case BEELZEBUB -> "Spatial";
            case ASTAROTH -> "Time";
            default -> d.displayName;
        };
    }

    public static float red(int rgb) { return ((rgb >> 16) & 255) / 255f; }
    public static float green(int rgb) { return ((rgb >> 8) & 255) / 255f; }
    public static float blue(int rgb) { return (rgb & 255) / 255f; }
}
