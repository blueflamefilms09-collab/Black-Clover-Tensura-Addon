package com.newuniverse.nusmp.avatar;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: every number of the Avatar system (outcome timer, outcome weights, naming surge, tax, drop chance, frenzy cost) is a balance decision the owner
 * wants to tune without a rebuild, and the whole system needs one off switch. Own spec file (nusmp-avatar-server.toml, SERVER); the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, AvatarConfig.SPEC, "nusmp-avatar-server.toml")}. Plain define/defineInRange only. The static
 * accessors fall back to the defaults while the config is not loaded yet (unit tests, early access), so other builders can call them anywhere.
 */
public final class AvatarConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue OUTCOME_TICKS, LUCK_THRESHOLD, MAX_AVATARS, REROLL_COST_MAGICULE, NAMING_SLEEP_SECONDS, REROLLS_MAX;
    private static final ModConfigSpec.DoubleValue BETRAYAL, SOUL, ORDINARY, NAMING_MIN, NAMING_MAX, TAX_PERCENT, FRENZY_BURN, DROP_CHANCE,
            BETRAYAL_HIT_CAP, LUCK_SHIFT_PER_POINT, LUCK_SHIFT_MAX;
    private static final ModConfigSpec.IntValue BETRAYAL_GRACE_TICKS, FOLLOW_TELEPORT_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("avatar");
        ENABLED = b.comment("Master switch of the whole Avatar system (Genesis Core use, spawning, outcomes).").define("enabled", true);
        MAX_AVATARS = b.comment("How many living Avatars one player can have at once.").defineInRange("maxAvatarsPerPlayer", 1, 1, 16);
        OUTCOME_TICKS = b.comment("Ticks from creation until the outcome roll (72000 = three in-game days).").defineInRange("outcomeTicks", 72000, 20, 24 * 72000);
        BETRAYAL = b.comment("Base chance of Betrayal at the outcome roll, before the luck shift.").defineInRange("betrayalBase", 0.30, 0.0, 1.0);
        SOUL = b.comment("Base chance of Soul Awakening, before the luck shift.").defineInRange("soulBase", 0.25, 0.0, 1.0);
        ORDINARY = b.comment("Base chance of Ordinary Existence (informational: it is always the remainder of the other two).").defineInRange("ordinaryBase", 0.45, 0.0, 1.0);
        LUCK_SHIFT_PER_POINT = b.comment("Each point of the creator's stored luck moves this much chance from Betrayal to Soul Awakening.").defineInRange("luckShiftPerPoint", 0.01, 0.0, 1.0);
        LUCK_SHIFT_MAX = b.comment("The most chance the luck can move (never more than the betrayal base).").defineInRange("luckShiftMax", 0.30, 0.0, 1.0);
        LUCK_THRESHOLD = b.comment("Total luck above this makes the stat roll 5d6 drop the two lowest instead of 4d6 drop the lowest.").defineInRange("luckBonusThreshold", 10, 0, 1000);
        NAMING_MIN = b.comment("Naming: the least EP surge (0.5 = +50%).").defineInRange("namingMinEpBoost", 0.5, 0.0, 10.0);
        NAMING_MAX = b.comment("Naming: the most EP surge (2.0 = +200%).").defineInRange("namingMaxEpBoost", 2.0, 0.0, 10.0);
        TAX_PERCENT = b.comment("Percent of the creator's maximum magicule an Avatar takes for as long as it lives, once it is named.").defineInRange("permanentTaxPercent", 10.0, 0.0, 90.0);
        NAMING_SLEEP_SECONDS = b.comment("Seconds of the Naming Sleep exhaustion the creator suffers when the Avatar is named.").defineInRange("namingSleepSeconds", 180, 0, 3600);
        FRENZY_BURN = b.comment("Yandere frenzy: health the Avatar burns on every spell it casts.").defineInRange("frenzyHealthBurnPerCast", 2.0, 0.0, 100.0);
        DROP_CHANCE = b.comment("Chance of a Genesis Core from a dungeon boss kill after the guaranteed first one of each player.").defineInRange("genesisCoreDropChance", 0.15, 0.0, 1.0);
        REROLL_COST_MAGICULE = b.comment("Magicule a stat reroll costs the creator (0 = free).").defineInRange("rerollCostMagicule", 0, 0, 1_000_000);
        REROLLS_MAX = b.comment("How many rerolls a single creation session allows (the first roll is free).").defineInRange("maxRerolls", 5, 0, 100);
        BETRAYAL_HIT_CAP = b.comment("A betrayed Avatar can never hurt in one blow more than this fraction of the victim's maximum health.").defineInRange("betrayalHitCap", 0.25, 0.01, 1.0);
        BETRAYAL_GRACE_TICKS = b.comment("Ticks of grace the creator gets after each time a betrayed Avatar teleports to them.").defineInRange("betrayalGraceTicks", 200, 0, 1200);
        FOLLOW_TELEPORT_DISTANCE = b.comment("A following Avatar teleports to its creator beyond this many blocks.").defineInRange("followTeleportDistance", 24, 8, 128);
        b.pop();
        SPEC = b.build();
    }

    private AvatarConfig() {}

    private static boolean ready() { return SPEC.isLoaded(); }

    public static boolean enabled() { return !ready() || ENABLED.get(); }
    public static int maxAvatarsPerPlayer() { return ready() ? MAX_AVATARS.get() : 1; }
    public static int outcomeTicks() { return ready() ? OUTCOME_TICKS.get() : 72000; }
    public static double betrayalBase() { return ready() ? BETRAYAL.get() : 0.30; }
    public static double soulBase() { return ready() ? SOUL.get() : 0.25; }
    public static double ordinaryBase() { return ready() ? ORDINARY.get() : 0.45; }
    public static double luckShiftPerPoint() { return ready() ? LUCK_SHIFT_PER_POINT.get() : 0.01; }
    public static double luckShiftMax() { return ready() ? LUCK_SHIFT_MAX.get() : 0.30; }
    public static int luckBonusThreshold() { return ready() ? LUCK_THRESHOLD.get() : 10; }
    public static double namingMinEpBoost() { return ready() ? NAMING_MIN.get() : 0.5; }
    public static double namingMaxEpBoost() { return ready() ? NAMING_MAX.get() : 2.0; }
    public static double permanentTaxPercent() { return ready() ? TAX_PERCENT.get() : 10.0; }
    public static int namingSleepSeconds() { return ready() ? NAMING_SLEEP_SECONDS.get() : 180; }
    public static double frenzyHealthBurnPerCast() { return ready() ? FRENZY_BURN.get() : 2.0; }
    public static double genesisCoreDropChance() { return ready() ? DROP_CHANCE.get() : 0.15; }
    public static int rerollCostMagicule() { return ready() ? REROLL_COST_MAGICULE.get() : 0; }
    public static int maxRerolls() { return ready() ? REROLLS_MAX.get() : 5; }
    public static double betrayalHitCap() { return ready() ? BETRAYAL_HIT_CAP.get() : 0.25; }
    public static int betrayalGraceTicks() { return ready() ? BETRAYAL_GRACE_TICKS.get() : 200; }
    public static int followTeleportDistance() { return ready() ? FOLLOW_TELEPORT_DISTANCE.get() : 24; }
}
