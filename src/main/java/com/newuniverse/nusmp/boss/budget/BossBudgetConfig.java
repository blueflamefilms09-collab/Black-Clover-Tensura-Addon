package com.newuniverse.nusmp.boss.budget;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config of the boss damage budget: world/serverconfig/nusmp-boss-budget-server.toml. */
public final class BossBudgetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, IGNORE_CREATIVE, USE_BASE_HEALTH;
    public static final ModConfigSpec.DoubleValue FRACTION, MINIMUM, FINAL_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bosses");
        ENABLED = b.comment("The per-second damage budget of this mod's bosses. false = no boss is capped.")
                .define("dpsCapEnabled", true);
        FRACTION = b.comment("Share of a boss's max health it can lose in one second (0.06 = 6%): hits, true damage and /kill all draw from the same budget.")
                .defineInRange("dpsCapFraction", 0.06, 0.001, 1.0);
        MINIMUM = b.comment("Lowest cap in health per second, so a boss with little health (a test boss) can still be killed.")
                .defineInRange("dpsCapMinimum", 20.0, 0.0, 1.0E6);
        FINAL_SCALE = b.comment("Multiplier of the cap in the boss's final or enraged phase (half of it one phase before). 1 = the same cap in every phase.")
                .defineInRange("dpsCapFinalPhaseScale", 1.5, 1.0, 10.0);
        USE_BASE_HEALTH = b.comment("Leon study fix 1: the cap is taken from the boss's BASE max health (max health divided by the Black Clover x3 modifier), so the x3 lengthens the shortest possible fight",
                        "(about 17 s becomes about 50 s) instead of tripling the cap along with the bar. false = the cap follows the full max health (the old behaviour).")
                .define("dpsCapUsesBaseHealth", true);
        IGNORE_CREATIVE = b.comment("A creative-mode player's blows are not capped (to test and to clear a boss at once).")
                .define("dpsCapIgnoreCreative", true);
        b.pop();
        SPEC = b.build();
    }

    private BossBudgetConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static double fraction() { return get(FRACTION); }

    public static double minimum() { return get(MINIMUM); }

    public static double finalScale() { return get(FINAL_SCALE); }

    public static boolean baseHealth() { return get(USE_BASE_HEALTH); }

    public static boolean ignoreCreative() { return get(IGNORE_CREATIVE); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
