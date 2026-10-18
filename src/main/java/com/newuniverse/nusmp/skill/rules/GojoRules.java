package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Limitless (Gojo Satoru's Ultimate) has four modes (Infinity, Blue, Red, Hollow Purple) with very different costs, radii and
 * damage, and Infinity's upkeep must be cheaper once mastered. Mode order is append-only and matches the saved tag "Infinity". All the
 * numbers are here so the four techniques are tested against each other (Purple strongest, Infinity cheapest).
 */
public final class GojoRules {
    private GojoRules() {}

    public static final int INFINITY = 0, BLUE = 1, RED = 2, PURPLE = 3, MODES = 4;
    /** The tag key of the Infinity switch (unchanged: old worlds keep their toggle). */
    public static final String INFINITY_KEY = "Infinity";

    // ---- Infinity ----
    /** Magicule per second the barrier costs. */
    public static double infinityDrain(boolean mastered) { return mastered ? 60 : 120; }
    /** Share of an incoming hit the barrier stops (1.0 = all of it). */
    public static double infinityBlock() { return 1.0; }

    // ---- Blue ----
    public static double blueCost() { return 1500; }
    public static int blueCooldown() { return 200; }
    public static double blueRange() { return 32; }
    public static double blueRadius(boolean m) { return m ? 12 : 9; }
    public static double blueDamage(boolean m) { return m ? 16 : 10; }
    public static double bluePull() { return 1.5; }

    // ---- Red ----
    public static double redCost() { return 1800; }
    public static int redCooldown() { return 240; }
    public static double redRadius(boolean m) { return m ? 10 : 7; }
    public static double redDamage(boolean m) { return m ? 24 : 16; }
    public static double redPush() { return 3.0; }

    // ---- Hollow Purple ----
    public static double purpleCost() { return 6000; }
    public static int purpleCooldown() { return 1800; }
    public static double purpleLength() { return 64; }
    public static double purpleRadius() { return 2.5; }
    public static double purpleDamage(boolean m) { return m ? 80 : 50; }

    /** The cheapest technique to start is Infinity (per second), the most expensive is Purple. */
    public static double cost(int mode, boolean m) {
        return switch (mode) {
            case INFINITY -> infinityDrain(m);
            case BLUE -> blueCost();
            case RED -> redCost();
            default -> purpleCost();
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case BLUE -> blueCooldown();
            case RED -> redCooldown();
            case PURPLE -> purpleCooldown();
            default -> 0;
        };
    }
}
