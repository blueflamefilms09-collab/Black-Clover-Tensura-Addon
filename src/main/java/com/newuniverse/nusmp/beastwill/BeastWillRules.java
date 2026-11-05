package com.newuniverse.nusmp.beastwill;

import java.util.Map;

/**
 * Why: every number and rule of the Beast Will system that does not need Minecraft (3.45.0), in one pure class so the unit tests, the data validator ({@link BeastWillSchema}) and the CI self-test read
 * the same table the game uses. It holds the bounds a will file may not leave (a data pack cannot make a will stronger than these), the ranks, the strain tiers, the progress and strain arithmetic, and the
 * anti-farming budgets. Nothing here touches a player or a world.
 */
public final class BeastWillRules {
    private BeastWillRules() {}

    // ------------------------------------------------------------------ phases and strain
    public static final int OFF = 0, ACQUIRE = 1, INTEGRATE = 2;
    public static final float STRAIN_MAX = 100f;
    public static final long DAY_TICKS = 24000L;
    /** Strain at which each tier begins: strained 30, exhausted 60, overload 85, rejection 100. */
    public static final float STRAINED_AT = 30f, EXHAUSTED_AT = 60f, OVERLOAD_AT = 85f;

    public enum Tier {
        CALM(1.0), STRAINED(0.85), EXHAUSTED(0.6), OVERLOAD(0.35);
        /** How much of its bonuses a will keeps at this tier. */
        public final double effectiveness;
        Tier(double e) { effectiveness = e; }
    }

    public static Tier tier(float strain) {
        if (strain >= OVERLOAD_AT) return Tier.OVERLOAD;
        if (strain >= EXHAUSTED_AT) return Tier.EXHAUSTED;
        if (strain >= STRAINED_AT) return Tier.STRAINED;
        return Tier.CALM;
    }

    public static float clampStrain(double s) { return (float) Math.max(0, Math.min(STRAIN_MAX, s)); }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    // ------------------------------------------------------------------ ranks
    /** The EP at which a beast counts as C, B, A, S and SS (the server can change them). */
    public static final double[] DEFAULT_RANK_EP = {1_000, 5_000, 20_000, 100_000, 500_000};

    /** The highest rank whose threshold the EP reaches, or null below the lowest. */
    public static BeastRank rankOf(double ep, double[] thresholds) {
        double[] t = thresholds != null && thresholds.length == BeastRank.values().length ? thresholds : DEFAULT_RANK_EP;
        BeastRank out = null;
        for (BeastRank r : BeastRank.values()) if (ep >= t[r.ordinal()]) out = r;
        return out;
    }

    /** A creature Tensura does not give EP to is judged by its health instead (a hundred EP for every point), so a listed dragon of another mod can still count. */
    public static double effectiveEp(double ep, double maxHealth, boolean tensura) {
        return tensura ? Math.max(0, ep) : Math.max(Math.max(0, ep), Math.max(0, maxHealth) * 100.0);
    }

    // ------------------------------------------------------------------ host and compatibility
    /** 1.0 for a listed host race, otherwise the will's own figure for other races (never below 0.2: a poor fit is slow and strained, never impossible). */
    public static double compat(boolean listedRace, double otherHostCompat) { return listedRace ? 1.0 : clamp(otherHostCompat, 0.2, 1.0); }

    // ------------------------------------------------------------------ strain arithmetic
    /** Strain per second while a phase is held: poor compatibility makes it up to double, holding it before you are used to it up to triple ({@link #prematureMult}). */
    public static double secondStrain(double base, double compat, double prematureMult, double rateScale) {
        return Math.max(0, base) * (2.0 - clamp(compat, 0, 1)) * clamp(prematureMult, 1, 3) * clamp(rateScale, 0, 4);
    }

    /** Holding a phase before you are used to it costs more: Acquire is comfortable from 40 % assimilation, Integrate from 70 % mastery; below that the strain climbs to triple at none. */
    public static double prematureMult(int phase, double assimilation, double mastery) {
        double have = phase == INTEGRATE ? mastery : assimilation, comfort = phase == INTEGRATE ? 0.7 : 0.4;
        return have >= comfort ? 1.0 : 1.0 + 2.0 * (comfort - clamp(have, 0, comfort)) / comfort;
    }

    public static double abilityStrain(double base, double compat, double rateScale) {
        return Math.max(0, base) * (2.0 - clamp(compat, 0, 1)) * clamp(rateScale, 0, 4);
    }

    /** Strain shed per second while the will is off: resting, and three times as fast while meditating. */
    public static double restDecay(double perSecond, boolean meditating) { return Math.max(0, perSecond) * (meditating ? 3.0 : 1.0); }

    // ------------------------------------------------------------------ progress
    /** The share of its bonuses a will gives: a third at the first spark of assimilation, everything at full assimilation and mastery, times the strain tier and the server's scale. */
    public static double statScale(double assimilation, double mastery, Tier tier, double globalScale) {
        double base = 0.35 + 0.35 * clamp(assimilation, 0, 1) + 0.30 * clamp(mastery, 0, 1);
        return clamp(base * tier.effectiveness * clamp(globalScale, 0, 2), 0, 2);
    }

    /** What may still be granted today: a request is cut to what is left of the day's allowance. */
    public static double budgeted(double requested, double usedToday, double dayCap) {
        return Math.max(0, Math.min(requested, dayCap - usedToday));
    }

    /** Is the assimilation far enough for the phase? */
    public static boolean phaseUnlocked(int phase, double assimilation, double mastery, double needAssimilation, double needMastery) {
        if (phase == ACQUIRE) return assimilation >= needAssimilation;
        if (phase == INTEGRATE) return assimilation >= needAssimilation && mastery >= needMastery;
        return true;
    }

    public static long dayOf(long gameTime) { return Math.max(0, gameTime) / DAY_TICKS; }

    /** The ticks an ability stays cooling down, scaled by the caster's cooldown figure and kept between half and double the written value. */
    public static int cooldown(int written, double mult) { return (int) Math.round(written * clamp(mult, 0.5, 2.0)); }

    // ------------------------------------------------------------------ the data bounds
    /** The attributes a will may touch and the most it may add to each (flat) or multiply (a share of the base). A stat outside this table is rejected by the validator. */
    public record AttrCap(double maxAdd, double maxMult) {}

    public static final Map<String, AttrCap> ATTR_CAPS = Map.of(
            "minecraft:generic.movement_speed", new AttrCap(0.02, 0.40),
            "minecraft:generic.attack_damage", new AttrCap(8.0, 0.50),
            "minecraft:generic.max_health", new AttrCap(12.0, 0.30),
            "minecraft:generic.armor", new AttrCap(10.0, 0.0),
            "minecraft:generic.armor_toughness", new AttrCap(5.0, 0.0),
            "minecraft:generic.knockback_resistance", new AttrCap(0.9, 0.0),
            "minecraft:generic.attack_speed", new AttrCap(0.0, 0.30),
            "minecraft:generic.step_height", new AttrCap(0.6, 0.0),
            "minecraft:generic.jump_strength", new AttrCap(0.0, 0.40),
            "minecraft:generic.safe_fall_distance", new AttrCap(6.0, 0.0));

    /** The bounds one kind of ability keeps no matter what the file says. */
    public record KindLimit(int maxDuration, double maxRadius, int maxTargets, int minCooldown) {}

    public static final Map<String, KindLimit> KINDS = Map.ofEntries(
            Map.entry("dragon_sight", new KindLimit(200, 24, 8, 200)),
            Map.entry("draconic_pulse", new KindLimit(100, 8, 6, 60)),
            Map.entry("time_exclusion", new KindLimit(100, 10, 6, 600)),
            Map.entry("shadow_veil", new KindLimit(200, 0, 1, 200)),
            Map.entry("shadow_dash", new KindLimit(0, 0, 1, 60)),
            Map.entry("ambush", new KindLimit(0, 0, 1, 200)),
            Map.entry("vine_lash", new KindLimit(60, 10, 3, 40)),
            Map.entry("verdant_mend", new KindLimit(200, 0, 1, 400)),
            Map.entry("restraining_vines", new KindLimit(100, 8, 6, 400)),
            Map.entry("bark_ward", new KindLimit(200, 0, 1, 400)),
            Map.entry("mane_surge", new KindLimit(200, 0, 1, 300)),
            Map.entry("world_howl", new KindLimit(60, 12, 10, 400)),
            // the three kinds of the 18 later wills (docs/beastwill_new.md): a poison cone, a pull-in field and a dread that makes hunters lose their aim
            Map.entry("venom_spit", new KindLimit(120, 10, 6, 100)),
            Map.entry("maelstrom", new KindLimit(100, 10, 6, 240)),
            Map.entry("dread_gaze", new KindLimit(160, 16, 6, 300)));

    public static final double MAX_COST_PCT = 25, MAX_STRAIN = 40, MAX_DRAIN_PCT = 3, MAX_STRAIN_PER_SECOND = 2.5, MAX_RANGE = 24, MAX_POWER = 8, MAX_REGEN = 1.0;
    public static final int MAX_COOLDOWN = 12000, MAX_ABILITIES_PER_PHASE = 2;
}
