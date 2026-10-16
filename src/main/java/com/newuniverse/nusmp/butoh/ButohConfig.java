package com.newuniverse.nusmp.butoh;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the dance state is a strong defensive tool (a chance to slip hits, a smaller hitbox, deflected missiles), so every number that
 * decides how strong must be a switch the owner can tune without a rebuild. Own spec file ({@code nusmp-butoh-server.toml}, SERVER); the
 * lead registers it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, ButohConfig.SPEC, "nusmp-butoh-server.toml")}. Plain
 * {@code define}/{@code defineInRange} only. Reads fall back to the defaults before the config is loaded.
 */
public final class ButohConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DANCE_DRAIN, EVADE_MAX, DANCE_CAST_BONUS;
    public static final ModConfigSpec.BooleanValue DANCE_SHRINK, DANCE_SIDESTEP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("butoh");
        DANCE_DRAIN = b.comment("Mana upkeep of the Butoh dance, as a fraction of the dancer's maximum magicule per second (0.01 = 1% per second).",
                "The dance ends when the mana runs out.").defineInRange("danceDrain", 0.01, 0.0, 0.5);
        EVADE_MAX = b.comment("The highest chance to slip a hit while dancing (0.65 = 65%). Each move has its own base chance between 35% and 65%; bosses take 20 points off.",
                "It can never be a guarantee: values above 0.95 are held at 0.95.").defineInRange("evadeMax", EvasionTable.DEFAULT_CAP, 0.0, 0.95);
        DANCE_CAST_BONUS = b.comment("Damage bonus of Butoh pages cast while dancing (0.15 = +15%).").defineInRange("danceCastBonus", 0.15, 0.0, 1.0);
        DANCE_SHRINK = b.comment("The dancer's scale shrinks by 15 to 25 percent while dancing (a smaller hitbox). false = the hitbox stays the normal size.")
                .define("danceShrink", true);
        DANCE_SIDESTEP = b.comment("A dodged hit also slips the dancer one step sideways (with a safe-position check). false = the hit is only cancelled.")
                .define("danceSidestep", true);
        b.pop();
        SPEC = b.build();
    }

    private ButohConfig() {}

    private static double get(ModConfigSpec.DoubleValue v, double fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }

    private static boolean get(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }

    public static double danceDrain() { return get(DANCE_DRAIN, 0.01); }

    public static double evadeMax() { return get(EVADE_MAX, EvasionTable.DEFAULT_CAP); }

    public static double danceCastBonus() { return get(DANCE_CAST_BONUS, 0.15); }

    public static boolean danceShrink() { return get(DANCE_SHRINK, true); }

    public static boolean danceSidestep() { return get(DANCE_SIDESTEP, true); }
}
