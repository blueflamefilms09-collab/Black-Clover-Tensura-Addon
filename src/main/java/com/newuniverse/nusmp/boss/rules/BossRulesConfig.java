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
    public static final ModConfigSpec.BooleanValue STRIP_REGENERATION, BAN_UNIQUE_ULTIMATE, LUCIUS_COMBO_AI, REFILL_AT_GATES,
            LUCIUS_ANIME_ARC, LUCIUS_TIME_REVERSAL;
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
        LUCIUS_ANIME_ARC = b.comment("Lucius fights as in the final arc: three acts (levitating Messiah, grounded Soul Corruptor, the clones and the chest gem), spell chains,",
                        "Soul Touch, a precognition that only simultaneous or very fast strikes, anti-magic or synchronized relics beat. In this mode the Gates do not refill his health bar.",
                        "false = the old 1.20 to 1.37 Lucius (Julius windows, charges of precognition, the Gates refill) untouched.")
                .define("luciusAnimeArc", true);
        LUCIUS_TIME_REVERSAL = b.comment("The owner's second exception to 'no second wind' (like refillAtGates): in act 2, after a burst of damage Lucius channels Time Reversal for 3 seconds",
                        "and returns to the health he had when the burst began, never above it. Anti-magic, a heavy hit or a lock interrupts it. false = he never rewinds wounds.",
                        "Needs luciusAnimeArc. BossHealGuard stays the whitelist: the restore runs through BossHealGuard.allow and the budget bypass.")
                .define("luciusTimeReversal", true);
        b.pop();
        SPEC = b.build();
    }

    private BossRulesConfig() {}

    public static boolean stripRegeneration() { return get(STRIP_REGENERATION); }

    public static boolean banUniqueUltimate() { return get(BAN_UNIQUE_ULTIMATE); }

    public static String forbiddenWords() { return get(FORBIDDEN_WORDS); }

    public static boolean luciusComboAi() { return get(LUCIUS_COMBO_AI); }

    public static boolean refillAtGates() { return get(REFILL_AT_GATES); }

    /** The final-arc Lucius (1.42.0): three acts, chains, Soul Touch, the new precognition, clones and the chest gem. */
    public static boolean luciusAnimeArc() { return get(LUCIUS_ANIME_ARC); }

    /** The owner's exception: Lucius's channelled Time Reversal (act 2) may restore the health a burst took. */
    public static boolean luciusTimeReversal() { return get(LUCIUS_TIME_REVERSAL); }

    /** The switches and words as the pure policy wants them. */
    public static BossSkillPolicy.Settings settings() {
        return new BossSkillPolicy.Settings(stripRegeneration(), banUniqueUltimate(), forbiddenWords());
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
