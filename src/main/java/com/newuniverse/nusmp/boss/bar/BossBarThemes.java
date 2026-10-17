package com.newuniverse.nusmp.boss.bar;

/**
 * The colour pair of each mod boss's animated bar and how it moves with the phase (pure; the server fills the sync with it).
 * Lucius is gold and turns deep purple as the Gates of the Tree of Qliphoth open; the floor bosses have their own palettes; every
 * other boss falls back to the palette it is given (its devil's colours for the Zogratis family; from 1.60.0 the boss's own {@code styleColor()} and
 * {@code styleAccent()}, which is how the characters of the arcs get a bar in their own colours). Phase is {@code PowerCurve.stage(power)}: 0 at 20%, 1 at 40, 2 at 60, 3 at 80, 4 once the Gates are open.
 */
public final class BossBarThemes {
    private BossBarThemes() {}

    public static final int MAX_PHASE = 4;

    public static final int LUCIUS_GOLD = 0xE8B923, LUCIUS_GOLD_LIGHT = 0xFFF0A0, LUCIUS_PURPLE = 0x3B0F6B, LUCIUS_VIOLET = 0xB07CFF;
    public static final int GOLIATH_STEEL = 0x8D96A3, GOLIATH_EMBER = 0xFFC266, GOLIATH_HOT = 0xC0553A, GOLIATH_HOT_LIGHT = 0xFFD27F;
    public static final int TIDE_BLUE = 0x1E7FC0, TIDE_FOAM = 0x7FF0FF, TIDE_DEEP = 0x0D4F8F, TIDE_FOAM_LIGHT = 0xC8FFFF;
    public static final int ORDER_GOLD = 0xE3C46A, ORDER_WHITE = 0xFFFFFF, ORDER_OPEN = 0xF2F2F2, ORDER_OPEN_LIGHT = 0xFFE08A;

    /** How far Lucius's gold has drifted toward purple at each phase: a little at 60% and 80%, all of it with the Gates. */
    private static final float[] LUCIUS_DRIFT = {0f, 0f, 0.15f, 0.30f, 1f};
    /** Share of the secondary colour mixed into every other boss's primary once its Gates are open. */
    private static final float GATES_SHIFT = 0.35f;

    /** The two colours of a bar. */
    public record Tint(int primary, int secondary) {}

    /** The pair for boss 'bossId' (its entity type path) at 'phase'; 'devilRgb' / 'accentRgb' are the fallback colours of its devil. */
    public static Tint tint(String bossId, int devilRgb, int accentRgb, int phase) {
        int p = Math.max(0, Math.min(MAX_PHASE, phase));
        String id = bossId == null ? "" : bossId;
        switch (id) {
            case "lucius_zogratis": {
                float k = LUCIUS_DRIFT[p];
                return new Tint(BossBarColors.lerp(LUCIUS_GOLD, LUCIUS_PURPLE, k), BossBarColors.lerp(LUCIUS_GOLD_LIGHT, LUCIUS_VIOLET, k));
            }
            case "goliath":
                return p >= MAX_PHASE ? new Tint(GOLIATH_HOT, GOLIATH_HOT_LIGHT) : new Tint(GOLIATH_STEEL, GOLIATH_EMBER);
            case "charybdis_spawn":
                return p >= MAX_PHASE ? new Tint(TIDE_DEEP, TIDE_FOAM_LIGHT) : new Tint(TIDE_BLUE, TIDE_FOAM);
            case "god_of_order":
                return p >= MAX_PHASE ? new Tint(ORDER_OPEN, ORDER_OPEN_LIGHT) : new Tint(ORDER_GOLD, ORDER_WHITE);
            default:
                int primary = devilRgb & 0xFFFFFF, secondary = accentRgb & 0xFFFFFF;
                return new Tint(p >= MAX_PHASE ? BossBarColors.lerp(primary, secondary, GATES_SHIFT) : primary, secondary);
        }
    }

    /**
     * The rage flag: the boss burns harder than its phase says. True once the Gates are open, or for a Triad rage power (+10 steps
     * such as 50 or 70, which sit between the 20% grid of the normal phases).
     */
    public static boolean rage(int power) {
        return power >= 100 || power % 20 != 0;
    }
}
