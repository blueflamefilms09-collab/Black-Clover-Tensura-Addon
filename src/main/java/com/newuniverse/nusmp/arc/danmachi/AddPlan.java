package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: three of the monsters call help with a roar (the Spartoi of Udaeus, the monsters of the Water City, the monsters the Black Goliath's roar brings), and
 * a boss that can call without limit turns a fight into a crowd. The limits are numbers (how many may stand at once at each power, how many one roar brings,
 * how long they live, how strong they are) and live here, pure and tested, so the game class that spawns them has nothing to decide.
 */
public final class AddPlan {
    private AddPlan() {}

    /** The hard ceiling on called monsters alive at once, whatever the config says. */
    public static final int HARD_CAP = 24;

    /** The most called monsters alive at once: 2 at the opening, 3, 4, 5, and 6 at the Gates; never above {@code configMax} nor {@link #HARD_CAP}. */
    public static int cap(int power, int configMax) {
        int byPower = 2 + PowerCurve.stage(power);
        return Math.max(0, Math.min(Math.min(byPower, configMax), HARD_CAP));
    }

    /** How many one roar brings: one more than the volley size of the power, as many as there is room for. */
    public static int toSummon(int cap, int alive, int volleySlots) {
        return Math.max(0, Math.min(cap - Math.max(0, alive), 1 + Math.max(1, volleySlots)));
    }

    /** Ticks a called monster lives: a minute at the opening, ten seconds more for every power step. */
    public static int lifetime(int power) { return 1200 + 200 * PowerCurve.stage(power); }

    /** The health of a called monster: the kit's base, 60% of it at the opening and 15% more for every power step. */
    public static double health(double base, int power) { return base * (0.6 + 0.15 * PowerCurve.stage(power)); }

    /** The attack of a called monster: the kit's base, 70% at the opening and 10% more for every step. */
    public static double attack(double base, int power) { return base * (0.7 + 0.1 * PowerCurve.stage(power)); }
}
