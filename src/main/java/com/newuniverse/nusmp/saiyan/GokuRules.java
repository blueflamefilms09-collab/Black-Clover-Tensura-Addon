package com.newuniverse.nusmp.saiyan;

/**
 * Why: the numbers of Son Goku's two first techniques (3.65.0, Stage 5) with no Minecraft type, so they are unit tested. KAMEHAMEHA: hold to charge, release to fire; the longer the charge, the more
 * damage, range, width, knockback and the more Ki it costs; mastery shortens the charge; the beam is a raycast, never an entity. ROCK, SCISSORS 'N' PAPER: a three-part combo (a direct punch, a fast
 * precision strike, an open-palm shockwave) that only chains in order with timed inputs; chaining all three adds a bonus to the last. Every number is a starting value and a server setting.
 */
public final class GokuRules {
    private GokuRules() {}

    // ------------------------------------------------------------------ Kamehameha
    /** The seconds a full charge takes: the base time, cut by up to 40% at full mastery, never under half a second. */
    public static double maxChargeSeconds(double base, double mastery) { return Math.max(0.5, base * (1.0 - 0.4 * clamp01(mastery))); }

    /** 0..1: how charged the beam is after this many ticks of holding. */
    public static double charge(long heldTicks, double maxSeconds) { return clamp01(Math.max(0, heldTicks) / 20.0 / Math.max(0.5, maxSeconds)); }

    /** A release before this many ticks fizzles instead of firing. */
    public static boolean canFire(long heldTicks, int minTicks) { return heldTicks >= Math.max(0, minTicks); }

    /** The damage of the beam before the balance law: a share of the full damage that grows from 35% (a quick shot) to 100%. */
    public static double damage(double fullDamage, double charge) { return Math.max(0.0, fullDamage) * (0.35 + 0.65 * clamp01(charge)); }

    /** The range: 40% of the full range for a quick shot, all of it for a full charge. */
    public static double range(double fullRange, double charge) { return Math.max(1.0, fullRange) * (0.4 + 0.6 * clamp01(charge)); }

    /** The half-width of the beam in blocks. */
    public static double width(double charge) { return 0.6 + 1.4 * clamp01(charge); }

    /** How many foes the beam passes through: one for a quick shot, up to eight. */
    public static int pierce(double charge) { return 1 + (int) Math.floor(clamp01(charge) * 7.0); }

    public static double knockback(double charge) { return 0.3 + 1.2 * clamp01(charge); }

    /** The Ki of the release in percent of the maximum: 40% of the full price for a quick shot, all of it for a full charge. */
    public static double kiPercent(double fullPercent, double charge) { return Math.max(0.0, fullPercent) * (0.4 + 0.6 * clamp01(charge)); }

    // ------------------------------------------------------------------ Rock, Scissors 'N' Paper
    public static final int ROCK = 1, SCISSORS = 2, PAPER = 3;

    /**
     * The stage a press makes. 0 = ignored (too soon after the last one: the inputs are timed); 1 = a new combo (nothing before, or the window ran out); 2 or 3 = the next part. After Paper the combo is
     * over and the next press starts again with Rock.
     */
    public static int nextStage(int last, long now, long lastAt, int minGap, int window) {
        if (last <= 0 || last >= PAPER || now < lastAt || now - lastAt > window) return ROCK;
        if (now - lastAt < minGap) return 0;
        return last + 1;
    }

    /** True when this stage continues a combo (so it is part of a chain). */
    public static boolean chained(int stage, int last) { return stage > ROCK && stage == last + 1; }

    /** The damage of a stage as a share of the base blow: Rock 1.0, Scissors 0.7, Paper 0.8, and Paper after a full chain 1.3 times that. */
    public static double stageDamage(int stage, boolean chained) {
        return switch (stage) {
            case SCISSORS -> 0.7;
            case PAPER -> chained ? 0.8 * 1.3 : 0.8;
            default -> 1.0;
        };
    }

    /** How far the stage reaches in blocks. */
    public static double stageReach(int stage) { return stage == ROCK ? 4.5 : 5.0; }

    /** The knockback of the stage: Rock shoves, Scissors barely does, Paper's shockwave throws. */
    public static double stageKnockback(int stage) { return stage == SCISSORS ? 0.2 : stage == PAPER ? 1.2 : 0.8; }

    /** Paper hits everything in front of the palm; Rock and Scissors hit one foe. */
    public static int stageTargets(int stage, int paperMax) { return stage == PAPER ? Math.max(1, paperMax) : 1; }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }
}
