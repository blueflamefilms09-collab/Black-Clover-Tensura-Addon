package com.newuniverse.nusmp.rebirth;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the rebirth rites erase or add grimoires, so every switch and number is a SERVER setting (file {@code nusmp-rebirth-server.toml}, registered by {@link RebirthBoot}). Every value is clamped
 * again by {@link RebirthRules} when it is read.
 */
public final class RebirthConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, TWO_BOOKS_ON_FUSION;
    public static final ModConfigSpec.IntValue COOLDOWN_DAYS, CONFIRM_SECONDS, XP_LEVEL_COST, SECOND_MIN_LEAVES, SECOND_MIN_RESOLVE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("rebirth");
        ENABLED = b.comment("Master switch of the Sigil of Rebirth and the Sigil of the Second Book. false = neither rite can be used (the items stay, they do nothing). The admin command /anime_multiverse grimoire reset is not affected.")
                .define("rebirth", true);
        COOLDOWN_DAYS = b.comment("Game days (24000 ticks) a player must wait between two rites of either kind (0 = no wait).")
                .defineInRange("cooldownDays", RebirthRules.DEFAULT_COOLDOWN_DAYS, 0, RebirthRules.MAX_COOLDOWN_DAYS);
        CONFIRM_SECONDS = b.comment("Seconds after the warning in which the sneaking second click confirms a rite.")
                .defineInRange("confirmSeconds", RebirthRules.DEFAULT_CONFIRM_SECONDS, RebirthRules.MIN_CONFIRM_SECONDS, RebirthRules.MAX_CONFIRM_SECONDS);
        XP_LEVEL_COST = b.comment("Experience levels a rite costs besides the sigil (0 = none).")
                .defineInRange("xpLevelCost", 0, 0, RebirthRules.MAX_XP_COST);
        SECOND_MIN_LEAVES = b.comment("Leaf tier the first grimoire must have for the Sigil of the Second Book (4 = a four-leaf or better, like Yuno).")
                .defineInRange("secondBookMinLeaves", RebirthRules.DEFAULT_SECOND_MIN_LEAVES, 3, 5);
        SECOND_MIN_RESOLVE = b.comment("Resolve score (written by the questionnaire as nusmp_resolve) needed for the Second Book. A player with no stored score passes. 0 = the check is off.")
                .defineInRange("secondBookMinResolve", RebirthRules.DEFAULT_SECOND_MIN_RESOLVE, 0, 10);
        TWO_BOOKS_ON_FUSION = b.comment("In the Tower Wizard's questionnaire, a high-resolve mage (four leaves or more, no devil cover) whose top two natures are both strong gets TWO real grimoires instead of one fused grimoire. false (default) = nothing changes: the pair fuses.")
                .define("twoBooksOnFusion", false);
        b.pop();
        SPEC = b.build();
    }

    private RebirthConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }
    public static int cooldownDays() { return RebirthRules.cooldownDays(get(COOLDOWN_DAYS)); }
    public static int windowTicks() { return RebirthRules.windowTicks(get(CONFIRM_SECONDS)); }
    public static int xpCost() { return RebirthRules.xpCost(get(XP_LEVEL_COST)); }
    public static int secondMinLeaves() { return RebirthRules.minLeaves(get(SECOND_MIN_LEAVES)); }
    public static int secondMinResolve() { return Math.max(0, Math.min(10, get(SECOND_MIN_RESOLVE))); }
    public static boolean twoBooksOnFusion() { return get(TWO_BOOKS_ON_FUSION); }
}
