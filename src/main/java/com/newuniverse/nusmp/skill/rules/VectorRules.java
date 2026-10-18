package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Vector Manipulation (Accelerator) reflects projectiles and part of every melee hit; Black Wings, its Ultimate, reflects all of it
 * and lets you hover and glide. Reflection share, drain and the hover rules are pure numbers kept here and tested, including that the
 * reflected hit can never exceed what the attacker actually dealt.
 */
public final class VectorRules {
    private VectorRules() {}

    public static double drain(Tier t) { return t == Tier.ULTIMATE ? 80 : 50; }
    public static int interval() { return 20; }

    /** Share of a melee hit turned back on the attacker. */
    public static double reflectShare(Tier t) { return t.pick(0.5, 0.75, 1.0); }

    /** Damage sent back and damage you still take, given the hit. The two always sum to the hit. */
    public static double reflected(double hit, Tier t) { return Math.max(0, hit) * reflectShare(t); }
    public static double taken(double hit, Tier t) { return Math.max(0, hit) - reflected(hit, t); }

    /** Speed multiplier a reflected projectile leaves with. */
    public static double projectileBoost(Tier t) { return t == Tier.ULTIMATE ? 1.6 : 1.0; }

    // ---- Black Wings: hover and glide ----
    /** Longest time the wings keep you aloft before a landing is needed (ticks of continuous hover). */
    public static int hoverTicks() { return 100; }
    /** Vertical speed while gliding (negative = still sinking a little). */
    public static double glideFall() { return -0.04; }
    public static double glideForward() { return 1.04; }
    public static boolean canGlide(Tier t, boolean sneaking, boolean onGround, boolean inWater) {
        return t == Tier.ULTIMATE && sneaking && !onGround && !inWater;
    }
    public static boolean fallImmune(Tier t) { return t == Tier.ULTIMATE; }
}
