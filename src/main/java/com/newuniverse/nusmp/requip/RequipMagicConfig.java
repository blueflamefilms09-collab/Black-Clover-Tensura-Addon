package com.newuniverse.nusmp.requip;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the few numbers of the Equip Magic that a server owner may want to change: the Requip Reflex (on or off, and the pause between two reflexes) and the share of a boss one volley may take. The
 * file is {@code nusmp-requip-server.toml}. A read before the config has loaded (or on a failure) gives the default.
 */
public final class RequipMagicConfig {
    private RequipMagicConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue REFLEX_ENABLED;
    private static final ModConfigSpec.IntValue REFLEX_SECONDS;
    private static final ModConfigSpec.DoubleValue BOSS_VOLLEY_SHARE;
    private static final ModConfigSpec.BooleanValue FLOURISH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("requip_magic");
        REFLEX_ENABLED = b.comment("Requip Reflex: a hit taken while a loadout is marked as the reflex loadout swaps to it by itself (or puts on the Adamantine Armor).").define("reflexEnabled", true);
        REFLEX_SECONDS = b.comment("Seconds between two reflexes (default 20).").defineInRange("reflexSeconds", RequipMagicRules.REFLEX_SECONDS, 0, 600);
        BOSS_VOLLEY_SHARE = b.comment("The share of a boss's max health that one Barrage, Strike or finisher may take in total (default 0.10).").defineInRange("bossVolleyShare", BarrageMath.BOSS_VOLLEY_SHARE, 0.01, 1.0);
        FLOURISH = b.comment("A loadout swap forges a few pieces of light armor round the user and shatters them (the Djinn equip animation), when no Erza armor is on.").define("loadoutFlourish", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean reflexEnabled() {
        try { return REFLEX_ENABLED.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static int reflexTicks() {
        try { return REFLEX_SECONDS.get() * 20; } catch (RuntimeException notLoaded) { return RequipMagicRules.REFLEX_SECONDS * 20; }
    }

    public static boolean flourish() {
        try { return FLOURISH.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double bossVolleyShare() {
        try { return BOSS_VOLLEY_SHARE.get(); } catch (RuntimeException notLoaded) { return BarrageMath.BOSS_VOLLEY_SHARE; }
    }
}
