package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the curse machine is pure and tested without a game, so it cannot read the config file; it is handed every number it needs in one plain object. The config
 * ({@link CurseConfig}) builds one of these from the server's toml each time the system looks at the players, and the tests build {@link #defaults()} and change
 * a field. The defaults are the single source of truth: the config declares each key with the matching {@code DEFAULT_} constant, so a number can never differ
 * between the file, the tests and the documentation. Fields are public and mutable on purpose (it is a bag of numbers, built, read, and thrown away); nothing keeps
 * one between ticks. Time values are ticks unless the name says seconds.
 */
public final class CurseTuning {
    // ------------------------------------------------------------------ defaults (the config's default values)
    public static final int DEFAULT_OFFER_THRESHOLD = 600, DEFAULT_OFFER_SECONDS = 90, DEFAULT_REOFFER_SECONDS = 180;
    public static final int DEFAULT_DECLINE_DESPAIR = 300, DEFAULT_EMBRACE_DESPAIR = 150, DEFAULT_DESPAIR_DECAY = 2, DEFAULT_CALM_SECONDS = 20;
    public static final int DEFAULT_PAUSE_ALLY = 100, DEFAULT_PAUSE_BETRAYAL = 200, DEFAULT_PAUSE_NEAR_DEATH = 1200;
    public static final double DEFAULT_ALLY_RADIUS = 32.0, DEFAULT_NEAR_DEATH_SHARE = 0.15;
    public static final int DEFAULT_PER_TECHNIQUE = 6, DEFAULT_ULTIMATE_FACTOR = 3, DEFAULT_MIN_PER_SECOND = 2, DEFAULT_CORRUPTION_DECAY = 2;
    public static final double DEFAULT_CORRUPTION_SCALE = 1.0;
    public static final int DEFAULT_OVERRUN_MIN_SECONDS = 10, DEFAULT_OVERRUN_MAX_SECONDS = 20, DEFAULT_OVERRUN_END_CORRUPTION = 400, DEFAULT_WEAK_SECONDS = 60;
    public static final double DEFAULT_OVERRUN_REACH = 4.5, DEFAULT_OVERRUN_RAW = 4.0, DEFAULT_OVERRUN_PLAYER_SHARE = 0.10;
    public static final boolean DEFAULT_OVERRUN_HITS_PLAYERS = true, DEFAULT_OVERRUN_BLOCKS_TECHNIQUES = false;
    public static final int DEFAULT_TAX_FIRST = 4, DEFAULT_TAX_NEXT = 2, DEFAULT_MIN_HEARTS = 4;
    public static final int DEFAULT_CHARM_REFUND_HEARTS = 2, DEFAULT_CHARM_REFUND_USES = 1;
    public static final double DEFAULT_REST_CLEAR_SHARE = 0.5;
    public static final double DEFAULT_INTENSITY = 1.0;

    public static final double DEFAULT_WRATH_DRAIN_SHARE = 0.02, DEFAULT_WRATH_DRAIN_MIN = 0.5, DEFAULT_WRATH_FLOOR_SHARE = 0.30, DEFAULT_WRATH_HEAL_CUT = 0.30;
    public static final double DEFAULT_PRIDE_HELP_CUT = 0.60, DEFAULT_PRIDE_SELF_HEAL_MAX = 1.0;
    public static final double DEFAULT_LUST_RANGE = 14.0, DEFAULT_LUST_MIN_DISTANCE = 3.0, DEFAULT_LUST_PULL = 0.14;
    public static final int DEFAULT_ENVY_INTERVAL_SECONDS = 12, DEFAULT_ENVY_LOCK_SECONDS = 6;
    public static final int DEFAULT_GREED_BURDEN_FULL = 128;
    public static final double DEFAULT_GREED_MAX_SLOW = 0.25, DEFAULT_GREED_SURCHARGE = 0.5;
    public static final double DEFAULT_GLUTTONY_EXHAUST = 0.4, DEFAULT_GLUTTONY_KILL_HEAL = 0.04;
    public static final int DEFAULT_GLUTTONY_FOOD_FLOOR = 3, DEFAULT_GLUTTONY_KILL_FOOD = 2;
    public static final boolean DEFAULT_GLUTTONY_BLOCKS_XP = true;
    public static final double DEFAULT_SLOTH_SLOW = 0.20, DEFAULT_SLOTH_ATTACK_SLOW = 0.15;
    public static final int DEFAULT_SLOTH_LOCK_TICKS = 30;

    /** The despair a curse may reach and the corruption at which an Overrun starts. */
    public static final int MAX = 1000;

    // ------------------------------------------------------------------ the numbers
    public int offerThreshold = DEFAULT_OFFER_THRESHOLD, offerSeconds = DEFAULT_OFFER_SECONDS, reofferSeconds = DEFAULT_REOFFER_SECONDS;
    public int declineDespair = DEFAULT_DECLINE_DESPAIR, embraceDespair = DEFAULT_EMBRACE_DESPAIR, despairDecayPerSecond = DEFAULT_DESPAIR_DECAY, calmSeconds = DEFAULT_CALM_SECONDS;
    public int pauseAlly = DEFAULT_PAUSE_ALLY, pauseBetrayal = DEFAULT_PAUSE_BETRAYAL, pauseNearDeath = DEFAULT_PAUSE_NEAR_DEATH;
    public double allyRadius = DEFAULT_ALLY_RADIUS, nearDeathShare = DEFAULT_NEAR_DEATH_SHARE;

    public int corruptionPerTechnique = DEFAULT_PER_TECHNIQUE, ultimateFactor = DEFAULT_ULTIMATE_FACTOR, minCorruptionPerSecond = DEFAULT_MIN_PER_SECOND;
    public int corruptionDecayPerSecond = DEFAULT_CORRUPTION_DECAY;
    public double corruptionScale = DEFAULT_CORRUPTION_SCALE;

    public int overrunMinSeconds = DEFAULT_OVERRUN_MIN_SECONDS, overrunMaxSeconds = DEFAULT_OVERRUN_MAX_SECONDS, overrunEndCorruption = DEFAULT_OVERRUN_END_CORRUPTION;
    public int weakSeconds = DEFAULT_WEAK_SECONDS;
    public double overrunReach = DEFAULT_OVERRUN_REACH, overrunRaw = DEFAULT_OVERRUN_RAW, overrunPlayerShare = DEFAULT_OVERRUN_PLAYER_SHARE;
    public boolean overrunHitsPlayers = DEFAULT_OVERRUN_HITS_PLAYERS, overrunBlocksTechniques = DEFAULT_OVERRUN_BLOCKS_TECHNIQUES;

    public int taxFirstHearts = DEFAULT_TAX_FIRST, taxNextHearts = DEFAULT_TAX_NEXT, minHearts = DEFAULT_MIN_HEARTS;
    public int charmRefundHearts = DEFAULT_CHARM_REFUND_HEARTS, charmRefundUses = DEFAULT_CHARM_REFUND_USES;
    public double restClearShare = DEFAULT_REST_CLEAR_SHARE;

    public double wrathDrainShare = DEFAULT_WRATH_DRAIN_SHARE, wrathDrainMin = DEFAULT_WRATH_DRAIN_MIN, wrathFloorShare = DEFAULT_WRATH_FLOOR_SHARE, wrathHealCut = DEFAULT_WRATH_HEAL_CUT;
    public double prideHelpCut = DEFAULT_PRIDE_HELP_CUT, prideSelfHealMax = DEFAULT_PRIDE_SELF_HEAL_MAX;
    public double lustRange = DEFAULT_LUST_RANGE, lustMinDistance = DEFAULT_LUST_MIN_DISTANCE, lustPull = DEFAULT_LUST_PULL;
    public int envyIntervalSeconds = DEFAULT_ENVY_INTERVAL_SECONDS, envyLockSeconds = DEFAULT_ENVY_LOCK_SECONDS;
    public int greedBurdenFull = DEFAULT_GREED_BURDEN_FULL;
    public double greedMaxSlow = DEFAULT_GREED_MAX_SLOW, greedSurcharge = DEFAULT_GREED_SURCHARGE;
    public double gluttonyExhaust = DEFAULT_GLUTTONY_EXHAUST, gluttonyKillHeal = DEFAULT_GLUTTONY_KILL_HEAL;
    public int gluttonyFoodFloor = DEFAULT_GLUTTONY_FOOD_FLOOR, gluttonyKillFood = DEFAULT_GLUTTONY_KILL_FOOD;
    public boolean gluttonyBlocksXp = DEFAULT_GLUTTONY_BLOCKS_XP;
    public double slothSlow = DEFAULT_SLOTH_SLOW, slothAttackSlow = DEFAULT_SLOTH_ATTACK_SLOW;
    public int slothLockTicks = DEFAULT_SLOTH_LOCK_TICKS;

    private final Map<Cause, Integer> amounts = new EnumMap<>(Cause.class);
    private final Map<Sin, Double> intensity = new EnumMap<>(Sin.class);

    private CurseTuning() {
        for (Cause c : Cause.values()) amounts.put(c, c.defaultAmount());
        for (Sin s : Sin.values()) intensity.put(s, DEFAULT_INTENSITY);
    }

    /** A fresh set of the default numbers. */
    public static CurseTuning defaults() { return new CurseTuning(); }

    // ------------------------------------------------------------------ per cause and per sin
    /** Despair points this cause adds. */
    public int amount(Cause c) { return amounts.get(c); }

    public void setAmount(Cause c, int value) { amounts.put(c, Math.max(0, Math.min(MAX, value))); }

    /** Game ticks the cause waits between two accepted reports on the same wielder. */
    public int pause(Cause c) {
        return switch (c.pause()) {
            case ALLY -> pauseAlly;
            case BETRAYAL -> pauseBetrayal;
            case NEAR_DEATH -> pauseNearDeath;
            case NONE -> 0;
        };
    }

    /** The strength multiplier of a sin's downside, 0 (off) to 3. */
    public double intensity(Sin s) { return intensity.get(s); }

    public void setIntensity(Sin s, double value) { intensity.put(s, Math.max(0.0, Math.min(3.0, value))); }

    // ------------------------------------------------------------------ derived
    public long offerTicks() { return offerSeconds * 20L; }

    public long reofferTicks() { return reofferSeconds * 20L; }

    public long calmTicks() { return calmSeconds * 20L; }

    public long weakTicks() { return weakSeconds * 20L; }

    /** A corruption amount after the global scale, rounded, never negative. */
    public int scaled(int corruption) { return (int) Math.max(0L, Math.round(corruption * corruptionScale)); }
}
