package com.newuniverse.nusmp.skill.rules;

/**
 * Why: the toggle and passive skills (Hell Walker / Hell Conqueror, Hawk Eye, Spirit Card, Hati, Six Eyes) all trade a steady magicule
 * drain or a damage hook for an effect. The drain per pulse, the damage curve of "stronger near death", the healing of the Spirit Card
 * and Hati's absorb-to-Strength conversion are pure numbers, tested here so the curves cannot silently flatten.
 */
public final class AuraRules {
    private AuraRules() {}

    // ---- Hell Walker / Hell Conqueror ----
    public static final double HELL_DRAIN = 60;
    public static final int HELL_INTERVAL = 20;

    /** How steeply the damage rises with missing health. */
    public static double hellScale(Tier t) { return t.pick(1.1, 1.6, 2.5); }

    /** Damage multiplier: 1 at full health, 1 + scale at death's door. */
    public static double hellMultiplier(double health, double max, Tier t) {
        return 1.0 + SkillMath.missing(health, max) * hellScale(t);
    }

    /** Hell Conqueror heals for this share of the damage it deals, once below half health. */
    public static double hellLifesteal(double health, double max, Tier t) {
        return t == Tier.ULTIMATE && SkillMath.missing(health, max) > 0.5 ? 0.2 : 0.0;
    }

    /** Hell Conqueror's Hellfire: below this share of health the aura burns foes around you. */
    public static double hellfireBelow() { return 0.5; }
    public static double hellfireRadius() { return 4.5; }
    public static double hellfireDamage() { return 4.0; }
    public static boolean hellfire(double health, double max, Tier t) {
        return t == Tier.ULTIMATE && max > 0 && health / max < hellfireBelow();
    }
    /** Below a third of health the Conqueror also gains speed and strength. */
    public static boolean berserk(double health, double max, Tier t) {
        return t == Tier.ULTIMATE && max > 0 && health / max < 0.34;
    }

    // ---- Hawk Eye ----
    public static final double HAWK_DRAIN = 30;
    public static double hawkRange(Tier t) { return t == Tier.BASE ? 32 : 48; }
    public static int hawkMaxMarked() { return 48; }

    // ---- Spirit Card ----
    public static final double SPIRIT_DRAIN = 80;
    public static int spiritInterval() { return 40; }
    public static double spiritRadius(Tier t) { return t == Tier.BASE ? 8 : 12; }
    public static double spiritHeal(Tier t) { return t == Tier.BASE ? 2.0 : 4.0; }
    public static boolean spiritCleanses(Tier t) { return t != Tier.BASE; }

    // ---- Six Eyes ----
    public static final double SIX_DRAIN = 40;
    public static double sixRange(Tier t) { return t == Tier.BASE ? 40 : 64; }
    /** A mastered Six Eyes wastes less magicule. */
    public static double sixDrain(Tier t) { return t == Tier.BASE ? SIX_DRAIN : SIX_DRAIN / 2.0; }

    // ---- Hati ----
    /** Share of fire / lightning / magic damage that still lands. */
    public static double hatiPassThrough(Tier t) { return t == Tier.BASE ? 0.6 : 0.4; }
    /** Highest Strength amplifier the absorbed damage can stack to (amplifier 0 = Strength I). */
    public static int hatiMaxAmp(Tier t) { return t == Tier.BASE ? 1 : 3; }
    public static int hatiTicks(Tier t) { return t == Tier.BASE ? 200 : 300; }

    /** Next Strength amplifier given the current one ({@code -1} when there is none). */
    public static int hatiNextAmp(int current, Tier t) { return SkillMath.clamp(current + 1, 0, hatiMaxAmp(t)); }

    /** Health a mastered Hati recovers from the damage it absorbed. */
    public static double hatiHeal(double absorbed, Tier t) { return t == Tier.BASE ? 0.0 : Math.max(0.0, absorbed) * 0.25; }

    /** Absorbed damage amount: what was prevented. */
    public static double hatiAbsorbed(double incoming, Tier t) { return Math.max(0.0, incoming) * (1.0 - hatiPassThrough(t)); }
}
