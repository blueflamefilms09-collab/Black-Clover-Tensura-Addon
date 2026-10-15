package com.newuniverse.nusmp.boss.rules;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner's boss rules (no regeneration, no Unique/Ultimate skills, Lucius's combo AI, the one refill at the Gates) must each be
 * a switch, because the brief overrides an older design and the old code paths are kept reachable behind them. Own spec file
 * ({@code nusmp-boss-rules-server.toml}, SERVER) so the rules can be tuned without touching the other boss configs; the lead registers
 * it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, BossRulesConfig.SPEC, "nusmp-boss-rules-server.toml")}. Plain
 * {@code define}/{@code defineInRange} only (the forbidden words are one comma separated string, parsed by the pure policy). Reads fall
 * back to the defaults before the config is loaded, so the rules hold from the very first tick.
 */
public final class BossRulesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STRIP_REGENERATION, BAN_UNIQUE_ULTIMATE, LUCIUS_COMBO_AI, REFILL_AT_GATES;
    public static final ModConfigSpec.ConfigValue<String> FORBIDDEN_WORDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossRules");
        STRIP_REGENERATION = b.comment("Bosses get no regeneration: no Regeneration / Instant Regeneration / Self Regeneration skills or effects, no heal while in combat.",
                        "The scripted refill at the Gates (refillAtGates) is the only restoration left.")
                .define("stripRegeneration", true);
        BAN_UNIQUE_ULTIMATE = b.comment("Bosses never own or use Unique or Ultimate skills (Tensura's skill types). Standard skills, innate traits and spellcasting stay.")
                .define("banUniqueUltimate", true);
        FORBIDDEN_WORDS = b.comment("Extra words that mark a skill or effect id as part of the regeneration family, comma separated (the built-in words always apply).")
                .define("forbiddenWords", BossSkillPolicy.DEFAULT_WORDS);
        LUCIUS_COMBO_AI = b.comment("Lucius chains distinct spells (spatial, offensive, defensive or control, then one heavy magic cast) under cooldowns. false = his old move chooser.")
                .define("luciusComboAi", true);
        REFILL_AT_GATES = b.comment("The Gates (100% mode) refill a Zogratis boss's health bar once. The one deliberate exception to 'no second wind'; false removes it.")
                .define("refillAtGates", true);
        b.pop();
        SPEC = b.build();
    }

    private BossRulesConfig() {}

    public static boolean stripRegeneration() { return get(STRIP_REGENERATION); }

    public static boolean banUniqueUltimate() { return get(BAN_UNIQUE_ULTIMATE); }

    public static String forbiddenWords() { return get(FORBIDDEN_WORDS); }

    public static boolean luciusComboAi() { return get(LUCIUS_COMBO_AI); }

    public static boolean refillAtGates() { return get(REFILL_AT_GATES); }

    /** The switches and words as the pure policy wants them. */
    public static BossSkillPolicy.Settings settings() {
        return new BossSkillPolicy.Settings(stripRegeneration(), banUniqueUltimate(), forbiddenWords());
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
