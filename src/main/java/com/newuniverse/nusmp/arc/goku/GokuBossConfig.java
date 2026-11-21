package com.newuniverse.nusmp.arc.goku;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Goku boss, {@code nusmp-goku-boss-server.toml}, section {@code [goku_boss]}. All of them change gameplay, so they are SERVER settings (rule 9). The strength
 * of the fight comes from the difficulty preset and from the Tensura EP of the players near him ({@link GokuBossMath}); every number is documented in docs/goku_boss.md. Every value is a
 * plain {@code define} / {@code defineInRange} (never a list-validated one: it throws while the server is still loading) and every reader goes through {@link #get}, which answers the
 * default while the spec is not loaded yet.
 */
public final class GokuBossConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BLOCK_DAMAGE, EP_SCALING, FAREWELL;
    /** Goku Omega (docs/goku_omega.md), section {@code [goku_boss.omega]}. */
    public static final ModConfigSpec.BooleanValue OMEGA_ENABLED, OMEGA_EP_SKILLS, OMEGA_HACKS, OMEGA_SECOND_WIND, OMEGA_TRAINING;
    public static final ModConfigSpec.DoubleValue OMEGA_HEALTH, OMEGA_DAMAGE, OMEGA_MAX_HIT, OMEGA_DODGE_CAP;
    public static final ModConfigSpec.IntValue OMEGA_LEVEL_CAP, OMEGA_XP_PER_HOUR, OMEGA_TRAINING_DAILY, OMEGA_FIGHT_DAILY;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER, DAMAGE_MULTIPLIER, EP_WEIGHT, EP_CAP, PER_PLAYER_BONUS, MAX_HIT_PERCENT, UI_DODGE_CAP;
    public static final ModConfigSpec.IntValue PLAYER_CAP, GLOBAL_CAP, ENRAGE_MINUTES;

    /** The defaults, also read by the tests. */
    public static final double DEFAULT_EP_WEIGHT = 0.6, DEFAULT_EP_CAP = 8.0, DEFAULT_PER_PLAYER_BONUS = 0.35, DEFAULT_MAX_HIT = 0.30, DEFAULT_DODGE_CAP = 0.35;
    public static final int DEFAULT_GLOBAL_CAP = 2, DEFAULT_ENRAGE_MINUTES = 12;
    public static final double DEFAULT_OMEGA_HEALTH = 2.5, DEFAULT_OMEGA_DAMAGE = 1.5, DEFAULT_OMEGA_MAX_HIT = 0.5, DEFAULT_OMEGA_DODGE = 0.85;
    public static final int DEFAULT_XP_PER_HOUR = 60, DEFAULT_TRAINING_DAILY = 1500;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("goku_boss");
        ENABLED = b.comment("The Goku boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        DIFFICULTY = b.comment("The preset: normal (half health and damage, 60% of the form bonuses), overpowered (the default) or anime (double health, 1.6x damage, 130% of the form bonuses). Unknown words mean overpowered.")
                .define("difficulty", "overpowered");
        HEALTH_MULTIPLIER = b.comment("Multiplies his health on top of the preset and the EP scaling.")
                .defineInRange("healthMultiplier", 1.0, 0.1, 50.0);
        DAMAGE_MULTIPLIER = b.comment("Multiplies his damage on top of the preset and the EP scaling (one hit on a player still ends at maxHitPercent of their health).")
                .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
        EP_SCALING = b.comment("His health, damage and form pace follow the Tensura Existence Points of the strongest players near him (read at spawn and then every 100 ticks). False: Battle Power, then the preset's fixed value.")
                .define("epScaling", true);
        FAREWELL = b.comment("4.90.0: when Goku is beaten he is not killed: he is knocked down, says goodbye (\"I'll see you next time, when I'm stronger!\"), flashes away and drops his loot. The fight counts as won. False = he dies as before.")
                .define("farewell", true);
        EP_WEIGHT = b.comment("How strongly EP raises him: health x (1 + epWeight x (EP curve - 1)); damage takes half of it. 0 = EP changes nothing.")
                .defineInRange("epWeight", DEFAULT_EP_WEIGHT, 0.0, 5.0);
        EP_CAP = b.comment("The highest value of the EP curve that counts (the curve is 1 at 1,000 EP and +2.5 per decade of EP: 5 is reached at about 40,000 EP and 8 at about 630,000 EP). Lower = he stops growing earlier.")
                .defineInRange("epCap", DEFAULT_EP_CAP, 1.0, 12.0);
        PER_PLAYER_BONUS = b.comment("Extra health per player beyond the first near him (damage gets 40% of it).")
                .defineInRange("perPlayerBonus", DEFAULT_PER_PLAYER_BONUS, 0.0, 2.0);
        PLAYER_CAP = b.comment("How many players count toward that bonus (at most 10).")
                .defineInRange("playerCap", GokuBossMath.MAX_PLAYERS, 1, GokuBossMath.MAX_PLAYERS);
        MAX_HIT_PERCENT = b.comment("One hit of his takes at most this share of a player's maximum health (0.30 = 30%): he cannot one-shot through the mod's own balance law.")
                .defineInRange("maxHitPercent", DEFAULT_MAX_HIT, 0.05, 0.9);
        UI_DODGE_CAP = b.comment("The most chance his automatic Ultra Instinct dodge may ever have (it also has a cooldown and a limit per window).")
                .defineInRange("uiDodgeCap", DEFAULT_DODGE_CAP, 0.0, 0.6);
        ENRAGE_MINUTES = b.comment("The soft enrage timer: after this many minutes (shorter with high EP) he is in True Ultra Instinct whatever his health is.")
                .defineInRange("enrageMinutes", DEFAULT_ENRAGE_MINUTES, 3, 60);
        BLOCK_DAMAGE = b.comment("The Kamehameha and the Spirit Bomb may break blocks (a few, through the boss framework's block ledger, and only when the griefBlocks rule is on too). Off by default.")
                .define("blockDamage", false);
        GLOBAL_CAP = b.comment("How many Goku bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        b.push("omega");
        OMEGA_ENABLED = b.comment("Goku Omega: the strongest boss of the mod. He is guaranteed above every other boss, levels up (fights and autonomous training), has Energy Drain, Skill Seal, adaptation and a list of hacks, and gains Mastered Ultra Instinct. False = exactly the Goku of before (everything below is ignored).")
                .define("enabled", true);
        OMEGA_EP_SKILLS = b.comment("Omega: Goku uses Tensura-style abilities on players (Energy Drain of magicules and aura, Skill Seal of one skill). Never hunger. Each has a cooldown, a cap per fight and a cap per player.")
                .define("epSkills", true);
        OMEGA_HACKS = b.comment("Omega: the other hacks (Ki Sense, Counter, Mirror, adaptation, Time Skip, Ki Barrier). Each unlocks at a level and has a cooldown and a cap per fight.")
                .define("hacks", true);
        OMEGA_SECOND_WIND = b.comment("Omega: a small capped heal at every form change (3% of his health, at most 15% per fight). It is a deliberate exception to the boss rule 'no heal in combat' (like the Gates refill); false = he never heals in a fight.")
                .define("secondWind", true);
        OMEGA_HEALTH = b.comment("Omega: his health factor is multiplied by this (on top of the level), then lifted to the strongest-boss floor.")
                .defineInRange("healthBonus", DEFAULT_OMEGA_HEALTH, 1.0, 10.0);
        OMEGA_DAMAGE = b.comment("Omega: the same for his damage.")
                .defineInRange("damageBonus", DEFAULT_OMEGA_DAMAGE, 1.0, 10.0);
        OMEGA_MAX_HIT = b.comment("Omega: one hit of his takes at most this share of a player's maximum health (0.5 = half; replaces maxHitPercent while Omega is on, never lower than it; at most 0.95, so no hit takes a whole bar).")
                .defineInRange("maxHitPercent", DEFAULT_OMEGA_MAX_HIT, 0.05, 0.95);
        OMEGA_DODGE_CAP = b.comment("Omega: the most chance his Ultra Instinct dodge may have (replaces uiDodgeCap while Omega is on; Mastered Ultra Instinct is near-perfect at 0.85+). It still has a cooldown and a limit per window.")
                .defineInRange("uiDodgeCap", DEFAULT_OMEGA_DODGE, 0.0, 0.95);
        OMEGA_LEVEL_CAP = b.comment("Omega: the highest level he can reach (hard maximum 200).")
                .defineInRange("levelCap", GokuOmegaMath.DEFAULT_LEVEL_CAP, 1, GokuOmegaMath.HARD_MAX_LEVEL);
        OMEGA_TRAINING = b.comment("Omega: he trains by himself while nobody fights him (pure arithmetic once an in-game hour and at the start of a fight; no entity, no chunk).")
                .define("training", true);
        OMEGA_XP_PER_HOUR = b.comment("Omega: experience he trains per in-game hour (1,000 ticks).")
                .defineInRange("trainingXpPerHour", DEFAULT_XP_PER_HOUR, 0, 1_000_000);
        OMEGA_TRAINING_DAILY = b.comment("Omega: the most experience training gives per in-game day (24,000 ticks).")
                .defineInRange("trainingDailyCap", DEFAULT_TRAINING_DAILY, 0, 10_000_000);
        OMEGA_FIGHT_DAILY = b.comment("Omega: the most experience fights give per in-game day (so summoning him again and again does not farm levels).")
                .defineInRange("fightDailyCap", (int) GokuOmegaMath.DEFAULT_FIGHT_DAILY_CAP, 0, 10_000_000);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private GokuBossConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean blockDamage() { return get(BLOCK_DAMAGE); }

    public static GokuBossMath.Preset preset() { return GokuBossMath.Preset.parse(get(DIFFICULTY)); }

    public static double maxHitPercent() { return get(MAX_HIT_PERCENT); }

    public static double uiDodgeCap() { return get(UI_DODGE_CAP); }

    public static int globalCap() { return get(GLOBAL_CAP); }

    public static int enrageMinutes() { return get(ENRAGE_MINUTES); }

    /** All the scaling inputs at once. */
    public static GokuBossMath.Params params() {
        return new GokuBossMath.Params(preset(), get(HEALTH_MULTIPLIER), get(DAMAGE_MULTIPLIER), get(EP_SCALING), get(EP_WEIGHT), get(EP_CAP), get(PER_PLAYER_BONUS), get(PLAYER_CAP));
    }

    public static boolean epScaling() { return get(EP_SCALING); }

    public static boolean farewell() { return get(FAREWELL); }

    // ------------------------------------------------------------------ Goku Omega
    public static boolean omega() { return get(OMEGA_ENABLED); }

    public static boolean omegaEpSkills() { return omega() && get(OMEGA_EP_SKILLS); }

    public static boolean omegaHacks() { return omega() && get(OMEGA_HACKS); }

    public static boolean omegaSecondWind() { return omega() && get(OMEGA_SECOND_WIND); }

    public static boolean omegaTraining() { return omega() && get(OMEGA_TRAINING); }

    public static int omegaLevelCap() { return get(OMEGA_LEVEL_CAP); }

    /** The share of a player's health one hit may take: the classic cap, or the Omega one while Omega is on. */
    public static double hitShare() { return GokuOmegaMath.hitShare(omega(), maxHitPercent(), get(OMEGA_MAX_HIT)); }

    /** The dodge chance cap: the classic one, or the Omega one (never lower than the classic) while Omega is on. */
    public static double dodgeCap() { return omega() ? Math.max(uiDodgeCap(), get(OMEGA_DODGE_CAP)) : uiDodgeCap(); }
}
