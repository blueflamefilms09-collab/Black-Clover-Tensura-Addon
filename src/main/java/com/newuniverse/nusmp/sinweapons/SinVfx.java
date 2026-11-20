package com.newuniverse.nusmp.sinweapons;

/**
 * Why: the pure half of the Berserk weapon effects (docs/sin_weapons_abilities.md): what the ONE effect shape {@code VfxShape.SIN_WEAPON} carries and how the client reads it back. The server sends one packet per use
 * ({@link SinFx}); its power is {@code weapon * STYLES + style + 1} (a payload power of 0 would read as 1, so the +1), the layer ({@code vfx.client.layer.SinWeaponLayer}) decodes it here. No game type, so a test and the
 * headless preview read it without Minecraft. Also the client switch {@code classicVfx} (file {@code nusmp-sinweapons-vfx-client.toml}, mirrored by {@code vfx.client.SinWeaponVfxConfig}): true = the layer draws nothing.
 */
public final class SinVfx {
    private SinVfx() {}

    /** The picture styles of one weapon. TECH is the right-click technique, ABILITY_A / ABILITY_B the extra techniques, HIT a plain hit, PASSIVE the passive's proc, STEAL (Envy only) Covet's thread. */
    public static final int TECH = 0, ABILITY_A = 1, ABILITY_B = 2, HIT = 3, PASSIVE = 4, STEAL = 5;
    /** Style slots per weapon in the power (room for more styles without moving the old ones). */
    public static final int STYLES = 8;

    /** The power of the effect of {@code style} of this weapon. */
    public static float power(SinRules.Kind k, int style) { return k.ordinal() * STYLES + Math.max(0, Math.min(style, STYLES - 1)) + 1; }

    /** The weapon a power stands for (0..4), anything out of range reads as the first. */
    public static int kindOf(float power) {
        int v = Math.round(power) - 1;
        int k = v / STYLES;
        return v >= 0 && k < SinRules.Kind.values().length ? k : 0;
    }

    /** The style a power stands for (0..STYLES-1). */
    public static int styleOf(float power) {
        int v = Math.round(power) - 1;
        return v >= 0 ? v % STYLES : 0;
    }

    /** The style of an extra technique: slot 1 = ABILITY_A, slot 2 = ABILITY_B. */
    public static int styleOfSlot(int slot) { return slot == 2 ? ABILITY_B : ABILITY_A; }

    /** How long the picture lives, in ticks (the server sends it as the duration). */
    public static int duration(int style) {
        return switch (style) {
            case TECH, ABILITY_A, ABILITY_B -> 22;
            case STEAL -> 14;
            default -> 10;
        };
    }

    /** The radius of the picture of an area technique, or the length of a line one, from the rules (the layer needs no number of its own). */
    public static double reach(SinRules.Kind k, int style) {
        if (style == ABILITY_A || style == ABILITY_B) {
            SinRules.Ability a = SinRules.ability(k, style == ABILITY_A ? 1 : 2);
            if (a != null) return a.reach;
        }
        return k.reach;
    }

    /** true: the layer draws nothing for SIN_WEAPON (looks only, a client setting). */
    private static volatile boolean classic = false;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }
}
