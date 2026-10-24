package com.newuniverse.nusmp.mana.method;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the Mana Method: {@code serverconfig/nusmp-mana-method-server.toml}. Every setting changes gameplay, so it is a SERVER setting; the caps can only be moved
 * inside the ranges of {@link RuneRules}. With {@code enabled} off the five Mana Method pages are not usable (the older True Magic page and everything else is as it was).
 */
public final class ManaMethodConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, TRUE_BLOCKS, TRUE_FIRE;
    public static final ModConfigSpec.IntValue PER_CASTER, GLOBAL, BLOCK_SECONDS;
    /** 3.5.0: the anime fixes. Every one defaults ON; OFF is exactly the 3.4.0 behaviour of that part. */
    public static final ModConfigSpec.BooleanValue HOLD_TO_CHARGE, COMMAND_FILTER, MOBILITY, PHYSICAL_DAMAGE, ANTI_MAGIC_STILL_CANCELS, PLANT_CAGE, LIGHTNING_BOLT, LIGHTNING_REAL_BOLT;
    public static final ModConfigSpec.IntValue MIN_HOLD, MAX_HOLD, AIR_STEP_SECONDS;
    public static final ModConfigSpec.DoubleValue DASH_STRENGTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("manaMethod");
        ENABLED = b.comment("The Mana Method pages (Flame, Water, Wind, Earth and True Magic Rune) of every grimoire. Off = they cannot be used; the older True Magic page is not affected.")
                .define("enabled", true);
        PER_CASTER = b.comment("Rune casts one caster may have in the air at once (charging or flying).").defineInRange("maxPerCaster", RuneRules.DEFAULT_PER_CASTER, 1, RuneRules.MAX_PER_CASTER);
        GLOBAL = b.comment("Rune casts the whole server may have in the air at once.").defineInRange("maxGlobal", RuneRules.DEFAULT_GLOBAL, RuneRules.MIN_GLOBAL, RuneRules.MAX_GLOBAL);
        HOLD_TO_CHARGE = b.comment("Hold the cast key to charge the rune: a tap fires a small basic spell (one ring), a full hold a massive one (all rings, both runes when sneaking, stronger Growth). Off = the 3.4.0 fixed 18 tick charge.")
                .define("holdToCharge", true);
        MIN_HOLD = b.comment("Ticks of holding below which the spell is the basic one (one ring tier, no second rune).").defineInRange("minHoldTicks", RuneCharge.DEFAULT_MIN_HOLD, RuneCharge.MIN_HOLD_LOW, RuneCharge.MIN_HOLD_HIGH);
        MAX_HOLD = b.comment("Ticks of holding for the full spell (all ring tiers). Always kept at least 8 ticks above minHoldTicks.").defineInRange("maxHoldTicks", RuneCharge.DEFAULT_MAX_HOLD, RuneCharge.MAX_HOLD_LOW, RuneCharge.MAX_HOLD_HIGH);
        COMMAND_FILTER = b.comment("The rune injects its commands (Pursue = homing, Grow = scaling) and the charge into the bolt at the launch. Off = the 3.4.0 bolt numbers whatever the charge.")
                .define("commandFilter", true);
        MOBILITY = b.comment("The two mobility pages: Air Step Rune (invisible barrier blocks under your feet for a few seconds) and Dash Rune (a velocity push along your look). Off = they cannot be used.")
                .define("mobility", true);
        AIR_STEP_SECONDS = b.comment("Seconds the Air Step Rune keeps a platform under your feet.").defineInRange("airStepSeconds", MobilityRules.DEFAULT_SECONDS, MobilityRules.MIN_SECONDS, MobilityRules.MAX_SECONDS);
        DASH_STRENGTH = b.comment("Blocks per tick the Dash Rune pushes at a normal charge (x0.55 for a tap, x1.5 for a full hold; never above 2.0).").defineInRange("dashStrength", MobilityRules.DEFAULT_STRENGTH, MobilityRules.MIN_STRENGTH, MobilityRules.MAX_STRENGTH);
        b.pop();
        b.push("trueMagic");
        TRUE_BLOCKS = b.comment("True Magic places real blocks on impact (water sources, stone, moss, fire). Off = it only deals its damage.").define("placeBlocks", true);
        TRUE_FIRE = b.comment("True Flame sets real fire (it still needs the doFireTick game rule).").define("placeFire", true);
        BLOCK_SECONDS = b.comment("Seconds before a block True Magic placed is taken back.").defineInRange("blockSeconds", TrueBlockRules.DEFAULT_SECONDS, TrueBlockRules.MIN_SECONDS, TrueBlockRules.MAX_SECONDS);
        PHYSICAL_DAMAGE = b.comment("True Magic hurts through a vanilla physical / environmental damage type (attributed to the caster) instead of magic damage, so Tensura magic resistance and magic-blocking skills do not apply. Off = the 3.4.0 magic damage with the resistance cut undone.")
                .define("physicalDamage", true);
        ANTI_MAGIC_STILL_CANCELS = b.comment("With physicalDamage on: an Anti-Magic user still cancels True Magic (the old magic route is taken against them). Off = True Magic slips past Anti-Magic.")
                .define("antiMagicStillCancels", true);
        PLANT_CAGE = b.comment("True Plant grows a small cage of logs and leaves round the creature it strikes (taken back after blockSeconds, at most 30). Off = the 3.4.0 moss carpet.")
                .define("plantCage", true);
        LIGHTNING_BOLT = b.comment("True Lightning summons a real vanilla lightning bolt entity at the impact (at most one per cast, three per second on the server). By default the visual-only kind: flash and thunder, no vanilla damage, no fire.")
                .define("lightningBolt", true);
        LIGHTNING_REAL_BOLT = b.comment("The bolt is the full vanilla one (damage, fire, mob changes) when nobody but foes is within 4.5 blocks, doFireTick is on and placeFire is on. Off = always the visual-only bolt.")
                .define("lightningRealBolt", false);
        b.pop();
        SPEC = b.build();
    }

    private ManaMethodConfig() {}

    public static boolean enabled() {
        try { return ENABLED.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static int perCaster() {
        try { return RuneRules.clampPerCaster(PER_CASTER.get()); } catch (RuntimeException notLoaded) { return RuneRules.DEFAULT_PER_CASTER; }
    }

    public static int global() {
        try { return RuneRules.clampGlobal(GLOBAL.get()); } catch (RuntimeException notLoaded) { return RuneRules.DEFAULT_GLOBAL; }
    }

    public static boolean trueBlocks() {
        try { return TRUE_BLOCKS.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean trueFire() {
        try { return TRUE_FIRE.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static int blockSeconds() {
        try { return TrueBlockRules.seconds(BLOCK_SECONDS.get()); } catch (RuntimeException notLoaded) { return TrueBlockRules.DEFAULT_SECONDS; }
    }

    // ---------------------------------------------------------------- 3.5.0
    private static boolean flag(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (RuntimeException notLoaded) { return fallback; }
    }

    public static boolean holdToCharge() { return flag(HOLD_TO_CHARGE, true); }

    public static int minHold() {
        try { return RuneCharge.minHold(MIN_HOLD.get()); } catch (RuntimeException notLoaded) { return RuneCharge.DEFAULT_MIN_HOLD; }
    }

    public static int maxHold() {
        try { return RuneCharge.maxHold(MIN_HOLD.get(), MAX_HOLD.get()); } catch (RuntimeException notLoaded) { return RuneCharge.DEFAULT_MAX_HOLD; }
    }

    public static boolean commandFilter() { return flag(COMMAND_FILTER, true); }

    public static boolean mobility() { return flag(MOBILITY, true); }

    public static int airStepSeconds() {
        try { return MobilityRules.seconds(AIR_STEP_SECONDS.get()); } catch (RuntimeException notLoaded) { return MobilityRules.DEFAULT_SECONDS; }
    }

    public static double dashStrength() {
        try { return MobilityRules.strength(DASH_STRENGTH.get()); } catch (RuntimeException notLoaded) { return MobilityRules.DEFAULT_STRENGTH; }
    }

    public static boolean physicalDamage() { return flag(PHYSICAL_DAMAGE, true); }

    public static boolean antiMagicStillCancels() { return flag(ANTI_MAGIC_STILL_CANCELS, true); }

    public static boolean plantCage() { return flag(PLANT_CAGE, true); }

    public static boolean lightningBolt() { return flag(LIGHTNING_BOLT, true); }

    public static boolean lightningRealBolt() { return flag(LIGHTNING_REAL_BOLT, false); }
}
