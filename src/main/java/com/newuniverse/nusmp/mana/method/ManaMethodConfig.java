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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("manaMethod");
        ENABLED = b.comment("The Mana Method pages (Flame, Water, Wind, Earth and True Magic Rune) of every grimoire. Off = they cannot be used; the older True Magic page is not affected.")
                .define("enabled", true);
        PER_CASTER = b.comment("Rune casts one caster may have in the air at once (charging or flying).").defineInRange("maxPerCaster", RuneRules.DEFAULT_PER_CASTER, 1, RuneRules.MAX_PER_CASTER);
        GLOBAL = b.comment("Rune casts the whole server may have in the air at once.").defineInRange("maxGlobal", RuneRules.DEFAULT_GLOBAL, RuneRules.MIN_GLOBAL, RuneRules.MAX_GLOBAL);
        b.pop();
        b.push("trueMagic");
        TRUE_BLOCKS = b.comment("True Magic places real blocks on impact (water sources, stone, moss, fire). Off = it only deals its damage.").define("placeBlocks", true);
        TRUE_FIRE = b.comment("True Flame sets real fire (it still needs the doFireTick game rule).").define("placeFire", true);
        BLOCK_SECONDS = b.comment("Seconds before a block True Magic placed is taken back.").defineInRange("blockSeconds", TrueBlockRules.DEFAULT_SECONDS, TrueBlockRules.MIN_SECONDS, TrueBlockRules.MAX_SECONDS);
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
}
