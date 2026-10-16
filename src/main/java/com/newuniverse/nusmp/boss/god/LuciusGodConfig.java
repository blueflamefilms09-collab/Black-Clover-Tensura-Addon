package com.newuniverse.nusmp.boss.god;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the god-tier Lucius (1.45.0) is a stack of a dozen counter-mechanics, and the owner must be able to switch any one of them off
 * on a server without touching the others, or all of them with one master switch to get exactly the 1.42.0 fight back. Own spec file
 * ({@code nusmp-lucius-god-server.toml}, SERVER) so the numbers can be tuned without touching the other boss configs; the lead
 * registers it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, LuciusGodConfig.SPEC, "nusmp-lucius-god-server.toml")}.
 * Plain {@code define}/{@code defineInRange} only. Reads fall back to the defaults before the config is loaded, so the rules hold from
 * the first tick. Every mechanic needs the master switch ({@code luciusGodTier}) <i>and</i> the final-arc switch
 * ({@code luciusAnimeArc} in {@link BossRulesConfig}) <i>and</i> its own switch.
 */
public final class LuciusGodConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GOD_TIER, THREE_STAGES, VOICE_LINES, HIT_CAP, TRUE_DAMAGE, TIME_DECAY, RELIC_EMP, SEVERANCE,
            STARVATION, ANTI_FLIGHT, SPATIAL_SWAP, ULTIMATE_REVERSAL, CHECKMATE_COUNTER, STASIS_TRAPS, BLOCK_ESCAPE_TELEPORTS, AFFECT_CREATIVE;
    public static final ModConfigSpec.DoubleValue HIT_CAP_PCT, AURA_RADIUS, EMP_SECONDS, SEVERANCE_SECONDS, STARVATION_PCT_PER_SEC,
            STARVATION_PULSE_PCT, MIRROR_PCT, TRUE_DAMAGE_SCALE, MAGICULE_DRAIN_FACTOR, TRAP_SECONDS, DIFFICULTY;
    public static final ModConfigSpec.IntValue STAGE_REFILL_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("luciusGod");
        GOD_TIER = b.comment("Master switch of the god-tier Lucius (1.45.0): three stages with full health resets and every counter-mechanic below.",
                        "Needs luciusAnimeArc (nusmp-boss-rules-server.toml). false = exactly the 1.42.0 final-arc fight.")
                .define("luciusGodTier", true);
        THREE_STAGES = b.comment("Three stages: a fatal blow in stage 1 or 2 refills his health bar (the owner's third exception to 'no second wind', after the Gates and Time Reversal) and the next act begins.",
                        "false = the acts follow the 65% and 30% health lines of 1.42.0 and the single bar is the whole fight.")
                .define("luciusThreeStages", true);
        VOICE_LINES = b.comment("Theatrical lines in chat (and a title on a stage change) to everyone in the arena.").define("luciusVoiceLines", true);
        HIT_CAP = b.comment("No single hit may take more than luciusHitCapPct percent of his maximum health; a clamped hit makes him teleport or counter.").define("luciusHitCap", true);
        HIT_CAP_PCT = b.comment("Percent of his maximum health one hit may take at most.").defineInRange("luciusHitCapPct", 2.0, 0.1, 100.0);
        TRUE_DAMAGE = b.comment("His spells deal a percent of the TARGET's maximum health as true damage (1.5% to 6% by move tier since 1.46.0, was 3% to 12%), and drain the same share of its magicule pool.",
                        "Still under the shared 18%-per-second pressure cap, and one hit never takes more than 25% of a player's maximum.")
                .define("luciusTrueDamage", true);
        TRUE_DAMAGE_SCALE = b.comment("Multiplier on the true-damage percents (1.0 = 1.5% to 6%).").defineInRange("luciusTrueDamageScale", 1.0, 0.1, 3.0);
        MAGICULE_DRAIN_FACTOR = b.comment("Share of the true-damage percent that is also drained from the victim's magicule (0 = no drain).").defineInRange("luciusMagiculeDrainFactor", 1.0, 0.0, 2.0);
        TIME_DECAY = b.comment("Time Decay: no passive healing for players and their pets within luciusAuraRadius of him while the fight is on (off during a stage change and for 10 s after it).",
                        "Healing potions and food heal normally. His own whitelisted heals are not affected.")
                .define("luciusTimeDecay", true);
        AURA_RADIUS = b.comment("Blocks (14 since 1.46.0, was 20).").defineInRange("luciusAuraRadius", 14.0, 4.0, 64.0);
        RELIC_EMP = b.comment("A spatial pulse that de-attunes every relic of players in range for luciusEmpSeconds. Nothing is unequipped, dropped or damaged.").define("luciusRelicEmp", true);
        EMP_SECONDS = b.comment("Seconds.").defineInRange("luciusEmpSeconds", 10.0, 1.0, 60.0);
        SEVERANCE = b.comment("A targeted binding that severs a player's Djinn and Black Clover spirits for luciusSeveranceSeconds. Nothing is destroyed.").define("luciusSeverance", true);
        SEVERANCE_SECONDS = b.comment("Seconds.").defineInRange("luciusSeveranceSeconds", 12.0, 1.0, 60.0);
        STARVATION = b.comment("Magicule Starvation: the arena drains luciusStarvationPctPerSec percent of the magicule pool per second, with pulses from the starvation field. Never touches health, never below 10% of the pool (one basic spell stays castable).").define("luciusStarvation", true);
        STARVATION_PCT_PER_SEC = b.comment("Percent of the maximum per second inside the arena (0.5 since 1.46.0, was 1.0).").defineInRange("luciusStarvationPctPerSec", 0.5, 0.0, 10.0);
        STARVATION_PULSE_PCT = b.comment("Percent of the maximum drained by one pulse (3 since 1.46.0, was 5).").defineInRange("luciusStarvationPulsePct", 3.0, 0.0, 25.0);
        ANTI_FLIGHT = b.comment("Lucifero's gravity: no flight in the arena while the fight is on (skills, wings, relics, elytra); a firm pull toward the ground.").define("luciusAntiFlight", true);
        AFFECT_CREATIVE = b.comment("Also take flight from creative-mode players (restored when they leave). Default off: ops and testers keep creative flight.").define("luciusAffectCreative", false);
        SPATIAL_SWAP = b.comment("Spatial Disorientation: when a player releases a massive attack he swaps places with them and the blow returns half its damage to its owner (20 s cooldown, was 12).").define("luciusSpatialSwap", true);
        ULTIMATE_REVERSAL = b.comment("An Ultimate-grade skill activated in the arena is cancelled and luciusMirrorPct percent of the caster's maximum health is dealt back as true damage (20 s cooldown, was 10).").define("luciusUltimateReversal", true);
        MIRROR_PCT = b.comment("Percent of the caster's maximum health dealt back by a reversal (never more than 25%; 3 since 1.46.0, was 6).").defineInRange("luciusMirrorPct", 3.0, 0.0, 25.0);
        CHECKMATE_COUNTER = b.comment("The Checkmate counter: a massive channel makes him step behind the caster and strike (telegraphed, unblockable, 14 s cooldown per target, was 8).").define("luciusCheckmateCounter", true);
        STASIS_TRAPS = b.comment("Time Stasis Traps under players who dodge three times in six seconds (at most 3 at once, was 6; 12 s each; armed after a 24-tick warning, was 20); whoever steps in is frozen at most 3 s and his next hit on them is a critical.").define("luciusStasisTraps", true);
        TRAP_SECONDS = b.comment("Seconds a trap lasts.").defineInRange("luciusTrapSeconds", 12.0, 3.0, 24.0);
        BLOCK_ESCAPE_TELEPORTS = b.comment("Ender pearls and chorus fruit cannot be used to leave the arena while the fight is on.").define("luciusBlockEscapeTeleports", true);
        STAGE_REFILL_TICKS = b.comment("Ticks of the invulnerable stage change (the bar refills over them).").defineInRange("luciusStageRefillTicks", 80, 20, 200);
        DIFFICULTY = b.comment("1.46.0 master difficulty of the Lucius fight: multiplies his damage numbers (true damage, the mirror, the old base damage) and divides his cooldowns,",
                        "so the owner can tune the fight without code. 0.5 easy, 1.0 normal, 1.5 hard. The cooldown floors (EMP 25 s, severance 30 s, starvation pulse 12 s, reversal and swap 20 s, checkmate counter 14 s) hold at any value.")
                .defineInRange("luciusDifficulty", 0.8, 0.5, 1.5);
        b.pop();
        SPEC = b.build();
    }

    private LuciusGodConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The master switch: the god tier is on only with the final arc on too. */
    public static boolean godTier() { return get(GOD_TIER) && BossRulesConfig.luciusAnimeArc(); }

    public static boolean threeStages() { return godTier() && get(THREE_STAGES); }
    public static boolean voiceLines() { return godTier() && get(VOICE_LINES); }
    public static boolean hitCap() { return godTier() && get(HIT_CAP); }
    public static double hitCapPct() { return get(HIT_CAP_PCT); }
    public static boolean trueDamage() { return godTier() && get(TRUE_DAMAGE); }
    public static double trueDamageScale() { return get(TRUE_DAMAGE_SCALE); }
    public static double magiculeDrainFactor() { return get(MAGICULE_DRAIN_FACTOR); }
    public static boolean timeDecay() { return godTier() && get(TIME_DECAY); }
    public static double auraRadius() { return get(AURA_RADIUS); }
    public static boolean relicEmp() { return godTier() && get(RELIC_EMP); }
    public static int empTicks() { return (int) Math.round(get(EMP_SECONDS) * 20.0); }
    public static boolean severance() { return godTier() && get(SEVERANCE); }
    public static int severanceTicks() { return (int) Math.round(get(SEVERANCE_SECONDS) * 20.0); }
    public static boolean starvation() { return godTier() && get(STARVATION); }
    public static double starvationPctPerSec() { return get(STARVATION_PCT_PER_SEC); }
    public static double starvationPulsePct() { return get(STARVATION_PULSE_PCT); }
    public static boolean antiFlight() { return godTier() && get(ANTI_FLIGHT); }
    public static boolean affectCreative() { return get(AFFECT_CREATIVE); }
    public static boolean spatialSwap() { return godTier() && get(SPATIAL_SWAP); }
    public static boolean ultimateReversal() { return godTier() && get(ULTIMATE_REVERSAL); }
    public static double mirrorPct() { return get(MIRROR_PCT); }
    public static boolean checkmateCounter() { return godTier() && get(CHECKMATE_COUNTER); }
    public static boolean stasisTraps() { return godTier() && get(STASIS_TRAPS); }
    public static int trapTicks() { return (int) Math.round(get(TRAP_SECONDS) * 20.0); }
    public static boolean blockEscapeTeleports() { return godTier() && get(BLOCK_ESCAPE_TELEPORTS); }
    public static int stageRefillTicks() { return get(STAGE_REFILL_TICKS); }

    /** The difficulty master (0.5 to 1.5, default 0.8), clamped again so a bad value can never reach the arithmetic. */
    public static double difficulty() { return LuciusBalance.clampDifficulty(get(DIFFICULTY)); }
}
