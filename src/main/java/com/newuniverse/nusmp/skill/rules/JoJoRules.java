package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Gold Experience (Giorno) gives life; its Ultimate Requiem returns harm to zero. The heal sizes, the chance a hit is returned to
 * zero, the length of the Return to Zero window and the new Life Burst are pure numbers kept here and tested, so Requiem is provably
 * stronger than the base.
 */
public final class JoJoRules {
    private JoJoRules() {}

    // ---- Gold Experience ----
    public static final int GIVE_LIFE = 0, OVERLOAD = 1;
    public static double lifeCost() { return 800; }
    public static int lifeCooldown() { return 300; }
    public static double lifeHeal(Tier t) { return t == Tier.BASE ? 10 : 16; }
    public static int lifeRegenTicks() { return 160; }
    public static double lifeRange() { return 8; }
    public static double overloadCost() { return 600; }
    public static int overloadCooldown() { return 160; }
    public static double overloadDamage(Tier t) { return t == Tier.BASE ? 6 : 10; }
    public static int overloadTicks(Tier t) { return t == Tier.BASE ? 100 : 160; }
    /** Ticks of Slowness a plain melee hit leaves. */
    public static int hitSlowTicks() { return 60; }
    public static double overloadRange() { return 16; }
    public static int modes() { return 2; }

    // ---- Gold Experience Requiem ----
    public static final int ZERO = 0, LIFE_BURST = 1;
    public static double zeroCost() { return 5000; }
    public static int zeroCooldown() { return 2400; }
    public static int zeroTicks(boolean mastered) { return mastered ? 200 : 120; }
    /** Chance (0..1) that any attack is returned to zero even outside the window. */
    public static double returnChance(boolean mastered) { return mastered ? 0.35 : 0.2; }
    public static boolean returns(boolean windowOpen, double roll, boolean mastered) { return windowOpen || roll < returnChance(mastered); }
    public static int punishTicks() { return 100; }
    public static double burstCost() { return 3000; }
    public static int burstCooldown() { return 900; }
    public static double burstRadius(boolean mastered) { return mastered ? 20 : 14; }
    public static double burstHealShare() { return 0.5; }
    public static int burstRegenTicks() { return 300; }
    public static int burstAbsorbAmp() { return 3; }
    public static int requiemModes() { return 2; }
}
