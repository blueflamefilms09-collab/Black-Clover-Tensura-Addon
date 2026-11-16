package com.newuniverse.nusmp.saiyan.armor;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Saiyan alloy set bonus ({@code serverconfig/nusmp-saiyan-armor-server.toml}). The bonus changes gameplay, so it is a server setting (rule 9), clamped again when it is
 * read. {@code setBonus = false} removes the bonus and leaves the armour plain armour.
 */
public final class SaiyanArmorConfig {
    private SaiyanArmorConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SET_BONUS;
    private static final ModConfigSpec.DoubleValue KI_REGEN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("saiyanArmor");
        SET_BONUS = b.comment("true: a Saiyan (with the Ki awakened) who wears all four pieces of the Saiyan alloy set regains extra Ki. false: the set is plain armour.").define("setBonus", true);
        KI_REGEN = b.comment("The extra Ki regained per second with the full set, in percent of the maximum Ki (on top of kiRegenPercentPerSecond of the Saiyan settings).")
                .defineInRange("setKiRegenPercent", SaiyanArmorRules.DEFAULT_KI_REGEN, 0.0, SaiyanArmorRules.MAX_KI_REGEN);
        b.pop();
        SPEC = b.build();
    }

    public static boolean setBonus() {
        try { return SET_BONUS.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double kiRegenPercent() {
        try { return Math.max(0.0, Math.min(SaiyanArmorRules.MAX_KI_REGEN, KI_REGEN.get())); } catch (RuntimeException notLoaded) { return SaiyanArmorRules.DEFAULT_KI_REGEN; }
    }
}
