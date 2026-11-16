package com.newuniverse.nusmp.skill.rules;

/**
 * Why: the master guide (§5.2) wants the Lord / King / God classes of trnightmare as real code that balance can read, plus the separate Dragon (Black Clover: Devil) track. Each class caps the
 * EP damage scaling, prices a cast as a share of max magicule, sets how much mastery the skill takes and the EP-ratio gates a class may use. The numbers here are only the DEFAULTS: every column is a
 * SERVER setting (nusmp-server.toml, [ultimateClasses], read through NUConfig) and is clamped by the methods below. Pure (no game class), so the math is unit-tested. Append new classes at the end.
 */
public enum UltimateClass {
    //      epCap costPct costRef maxMastery killBelow shredBelow
    LORD  ( 8.0, 2.0, 2_000, 10_000, 0.0,  0.0 ),   // strong numbers + one rule-bend, no instakill
    KING  (10.0, 3.0, 3_000, 25_000, 0.0,  0.70),   // EP-ratio shredding, time / space control
    GOD   (12.0, 5.0, 4_000, 50_000, 0.60, 0.80),   // bypass / degrade, own resource, the strongest gates
    DRAGON(12.0, 4.0, 3_000, 15_000, 0.0,  0.0 );   // separate track: body / summon / domain power (Black Clover: Devil)

    /** Most a class may cap the EP scaling at: the old {@code BalanceLaw.epScale} ceiling. */
    public static final double EP_CAP_MAX = 12.0;
    /** The 75 % milestone: cost x0.8, cooldown x0.75. The 25 % milestone: range / radius x1.2. */
    public static final double MILESTONE_COST = 0.8, MILESTONE_COOLDOWN = 0.75, MILESTONE_RANGE = 1.2;

    /** Damage ceiling of the EP scaling (x1 .. x12). */
    public final double epCap;
    /** Late-game price: a cast whose flat price is {@link #costRef} costs this % of the caster's max magicule. */
    public final double costPct;
    /** The flat price that maps to {@link #costPct}; every cast keeps its own flat price as the floor. */
    public final double costRef;
    /** Mastery points to master a skill of this class (Tensura's Ultimate default is 10,000). */
    public final int maxMastery;
    /** EP-ratio gates (fractions of the caster's EP): instakill a non-boss below killBelow, shred below shredBelow; 0 = none. Read by the class's skills, never by the balance law. */
    public final double killBelow, shredBelow;

    UltimateClass(double epCap, double costPct, double costRef, int maxMastery, double killBelow, double shredBelow) {
        this.epCap = epCap;
        this.costPct = costPct;
        this.costRef = costRef;
        this.maxMastery = maxMastery;
        this.killBelow = killBelow;
        this.shredBelow = shredBelow;
    }

    /** The lower-case name used for the config sub-section and the docs. */
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    // ------------------------------------------------------------------ clamps (the config values pass through these)
    private static double clamp(double v, double lo, double hi, double fallback) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : fallback; }

    public static double clampEpCap(double v) { return clamp(v, 1.0, EP_CAP_MAX, EP_CAP_MAX); }
    public static double clampCostPct(double v) { return clamp(v, 0.0, 50.0, 0.0); }
    public static double clampCostRef(double v) { return clamp(v, 1.0, 1.0E9, 1.0E9); }
    public static int clampMaxMastery(int v) { return Math.max(100, Math.min(1_000_000, v)); }
    public static double clampRatio(double v) { return clamp(v, 0.0, 0.95, 0.0); }
    /** The late-game share of the combined cap: the class's total damage scaling is at most epCap x this. */
    public static double clampLateShare(double v) { return clamp(v, 1.0, 6.0, 1.0); }

    // ------------------------------------------------------------------ damage scaling
    /** The old EP curve (x1 at 1,000 EP, +2.5 per tenfold), capped at {@code cap}; bad input = x1. */
    public static double epScale(double ep, double cap) {
        double c = clampEpCap(cap);
        if (!(ep > 1000.0)) return 1.0;
        double s = 1.0 + (Math.log10(ep) - 3.0) * 2.5;
        return Double.isFinite(s) ? Math.min(c, s) : c;
    }

    /**
     * The total damage scaling of a classified hit: the class-capped EP scaling times the late-game factor, never above {@code cap x lateShare}. With the defaults (lateShare 3) that is x24 Lord,
     * x30 King, x36 God / Dragon, against x72 (12 x 6) for an unclassified hit at the very top.
     */
    public static double totalScale(double ep, double cap, double lateFactor, double lateShare) {
        double late = Double.isFinite(lateFactor) && lateFactor > 1.0 ? lateFactor : 1.0;
        return Math.min(epScale(ep, cap) * late, clampEpCap(cap) * clampLateShare(lateShare));
    }

    // ------------------------------------------------------------------ cost
    /**
     * The price of a cast whose flat price is {@code flat}: never below the flat price; once the caster's pool is large it grows with it, so a cast of {@code ref} costs {@code pct} % of max
     * magicule and every other cast costs in the same proportion (a 600 dash stays cheaper than a 4,000 meteor). Early game nothing changes: the flat price wins.
     */
    public static double price(double flat, double maxMagicule, double pct, double ref) {
        if (!(flat > 0)) return 0.0;
        double p = clampCostPct(pct), r = clampCostRef(ref);
        double mm = Double.isFinite(maxMagicule) && maxMagicule > 0 ? maxMagicule : 0.0;
        double scaled = mm * p / 100.0 * flat / r;
        return Double.isFinite(scaled) ? Math.max(flat, scaled) : flat;
    }

    // ------------------------------------------------------------------ mastery milestones (§5.2)
    /** 0 below 25 %, 1 from 25 %, 2 from 50 %, 3 from 75 %, 4 = mastered. */
    public static int milestone(double masteryFrac) {
        if (!(masteryFrac > 0)) return 0;
        return (int) (Math.min(1.0, masteryFrac) * 4);
    }

    public static double milestoneRange(int milestone) { return milestone >= 1 ? MILESTONE_RANGE : 1.0; }
    public static boolean milestoneSecondEffect(int milestone) { return milestone >= 2; }
    public static double milestoneCost(int milestone) { return milestone >= 3 ? MILESTONE_COST : 1.0; }
    public static double milestoneCooldown(int milestone) { return milestone >= 3 ? MILESTONE_COOLDOWN : 1.0; }

    /**
     * Mastery earned against {@code oldMax}, moved to {@code newMax} at the same fraction, so a class that raises the mastery a skill takes never takes a mastered skill away (and lowering it
     * never over-masters one). Negative mastery (still learning) is left alone.
     */
    public static double rescaleMastery(double mastery, int oldMax, int newMax) {
        if (!(mastery > 0) || oldMax <= 0 || newMax <= 0 || oldMax == newMax) return mastery;
        if (mastery >= oldMax) return newMax;
        return Math.min(newMax, mastery * (double) newMax / oldMax);
    }
}
