package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Devil's Footprints (Shinra, Fire Force) and its Ultimate Adolla Burst are movement-and-kick skills. The Ultimate replaces the
 * ignition dash with a light-speed strike and adds a third mode (the Adolla Burst blast), so the mode list, the costs, the distances and the
 * damage are pure data that the tests check: the Ultimate must out-class the base in every number.
 */
public final class FireForceRules {
    private FireForceRules() {}

    /** Mode indexes. DASH is Ignition Dash in the Unique and Light Speed in the Ultimate; BURST exists only in the Ultimate. */
    public static final int DASH = 0, KICK = 1, BURST = 2;

    public static int modes(Tier t) { return t == Tier.ULTIMATE ? 3 : 2; }

    // ---- dash / light speed ----
    public static double dashCost(Tier t) { return t == Tier.ULTIMATE ? 600 : 150; }
    public static int dashCooldown(Tier t) { return t == Tier.ULTIMATE ? 60 : 30; }
    public static double dashPower(Tier t) { return t == Tier.BASE ? 1.7 : 2.4; }
    /** Light Speed's reach in blocks (the safe-landing search of the relics' blink is capped at 32). */
    public static double lightSpeedRange(Tier t) { return t == Tier.MASTERED ? 28 : 32; }
    public static double lightSpeedDamage(Tier t) { return 22.0; }
    public static double lightSpeedWidth() { return 1.8; }

    // ---- kick ----
    public static double kickCost(Tier t) { return t == Tier.ULTIMATE ? 800 : 300; }
    public static int kickCooldown(Tier t) { return t == Tier.ULTIMATE ? 60 : 100; }
    public static double kickRange(Tier t) { return t == Tier.ULTIMATE ? 8 : 5; }
    public static double kickDamage(Tier t) { return t.pick(14.0, 21.0, 30.0); }
    public static int kickBurnSeconds(Tier t) { return t == Tier.ULTIMATE ? 9 : 6; }

    // ---- Adolla Burst blast (Ultimate) ----
    public static double burstCost() { return 1600; }
    public static int burstCooldown() { return 200; }
    public static double burstRadius() { return 9.0; }
    public static double burstDamage() { return 36.0; }
    public static int burstBurnSeconds() { return 10; }

    /** Fire damage is cancelled for the wearer: Shinra's own flames cannot burn him. */
    public static boolean fireImmune(boolean hasSkill) { return hasSkill; }
}
