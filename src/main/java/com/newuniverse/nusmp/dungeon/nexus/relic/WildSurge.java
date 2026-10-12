package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.function.IntUnaryOperator;

/**
 * The Gambler's Grimoire Page and its curse, Wild Magic Surge (docs/living_dungeon.md section 7): every use rolls a hidden d20, and a 1
 * backfires. The page cannot be spammed: it rests for a minute after every use. Pure; randomness comes in as a function from a bound to a
 * number below it, so the tests can fix every roll.
 */
public final class WildSurge {
    private WildSurge() {}

    public static final int SIDES = 20, BACKFIRE_ON = 1;
    /** A minute between two uses, a second of wind-up, and the day (in ticks) a backfire costs a tenth of the mana pool. */
    public static final int COOLDOWN_TICKS = 1200, WINDUP_TICKS = 20, PENALTY_TICKS = 24000;
    public static final double PENALTY = 0.10;

    /** A d20: 1 to 20. */
    public static int roll(IntUnaryOperator below) { return below.applyAsInt(SIDES) + 1; }

    public static boolean backfires(int roll) { return roll == BACKFIRE_ON; }

    /** The chance of a backfire on one roll. */
    public static double backfireChance() { return 1.0 / SIDES; }

    public static long readyAt(long now) { return now + COOLDOWN_TICKS; }

    /** Is the page rested? A stamp from a clock that ran later (another world, a reset) counts as rested. */
    public static boolean ready(long now, long readyAt) { return readyAt <= now || readyAt - now > COOLDOWN_TICKS; }

    public static int secondsLeft(long now, long readyAt) { return ready(now, readyAt) ? 0 : (int) ((readyAt - now + 19) / 20); }

    /** Which of {@code kinds} spells the page casts. */
    public static int pick(IntUnaryOperator below, int kinds) { return kinds <= 1 ? 0 : below.applyAsInt(kinds); }
}
