package com.newuniverse.nusmp.lords;

/**
 * Why: the one-number language between the server that sends a Lord effect ({@link LordFx}) and the client layer that draws it ({@code vfx.client.layer.LordLayer}): {@code power = sin * 16 + style + 1 + radius / 40}
 * (the whole part says WHAT, the fraction says how big, so one float carries both and no new packet exists). Pure, no game type, so the server, the layer and the headless preview all read the same table, and
 * a test pins it. A style is one picture: the three arts (one per form), the Lord's decree, the strained state (an aura that follows the wielder), a mastery level-up, a small hit flash and, for Greed, the
 * Half-Starved awakening. The sin is {@link LordRules.Sin#ordinal()}.
 */
public final class LordVfxCodes {
    private LordVfxCodes() {}

    private static volatile boolean classic;

    /** The client setting {@code lords.classicVfx}: true = the layer draws nothing for this player (the server's small puff stays). Looks only. */
    public static boolean classic() { return classic; }

    public static void setClassic(boolean v) { classic = v; }

    public static final int ART0 = 0, ART1 = 1, ART2 = 2, DECREE = 3, STATE = 4, LEVEL = 5, HIT = 6, HALF = 7, STYLES = 16;
    /** Largest radius the fraction can carry, in blocks. */
    public static final float MAX_RADIUS = 39.0f;

    public static float power(LordRules.Sin sin, int style, float radius) {
        float r = Float.isNaN(radius) ? 0.0f : Math.max(0.0f, Math.min(MAX_RADIUS, radius));
        return sin.ordinal() * STYLES + Math.max(0, Math.min(STYLES - 1, style)) + 1 + r / 40.0f;
    }

    /** The sin ordinal a power stands for (0 when it is out of range). */
    public static int sinOf(float power) {
        int code = (int) Math.floor(power) - 1;
        int s = code < 0 ? 0 : code / STYLES;
        return s >= LordRules.Sin.values().length ? 0 : s;
    }

    public static int styleOf(float power) {
        int code = (int) Math.floor(power) - 1;
        return code < 0 ? 0 : code % STYLES;
    }

    public static float radiusOf(float power) { return (power - (float) Math.floor(power)) * 40.0f; }

    /** How long the picture lives, in ticks (the server sends it as the duration). */
    public static int duration(int style) {
        return switch (style) {
            case ART0, ART1, ART2 -> 26;
            case DECREE -> 60;
            case STATE -> 70;
            case LEVEL -> 50;
            case HALF -> 46;
            default -> 12;
        };
    }
}
