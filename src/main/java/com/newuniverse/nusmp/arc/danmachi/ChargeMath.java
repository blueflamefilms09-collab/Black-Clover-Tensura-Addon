package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: the Minotaur and the Goliath hurl themselves along a lane, and the rules of that run are numbers that the telegraph and the strike must agree on: how
 * many ticks a run of a given length takes, how long a bull that rams a wall is stunned (it learns: the stun shortens as its power rises), how many dashes a
 * Rampage chains, how long each dash is shown before it happens. They are pure so the tests can pin them down, and so that "the answer to the charge is a
 * wall at your back" stays true however the power tuning is changed later.
 */
public final class ChargeMath {
    private ChargeMath() {}

    /** The ticks of warning every dash of a Rampage gets before it runs, whatever the power. */
    public static final int DASH_TELL = 12;
    /** The shortest charge: a run is never over before it began. */
    public static final int MIN_TICKS = 4;
    /** What a boss that has rammed a wall takes: damage and nothing else (it cannot act). */
    public static final float CRASH_BONUS = 1.30f;
    /** Height in blocks of a wall that stops a charge (a step of one block is climbed, as the boss's step height allows). */
    public static final double WALL_HEIGHT = 1.0;

    /** Ticks a run of {@code length} blocks takes at {@code speed} blocks a tick. */
    public static int ticks(double length, double speed) {
        if (!(speed > 0.01)) return MIN_TICKS;
        return Math.max(MIN_TICKS, (int) Math.ceil(Math.max(0.0, length) / speed));
    }

    /** How long a bull stuck in a wall is helpless: 60 ticks at the opening, 6 less for every power step, never under 30. */
    public static int crashStun(int power) { return Math.max(30, 60 - 6 * PowerCurve.stage(power)); }

    /** Dashes in one Rampage: 3 at 60%, 4 at 80%, 5 at the Gates; under 60% it cannot rampage at all. */
    public static int rampageDashes(int power) {
        if (power >= PowerCurve.FULL) return 5;
        if (power >= 80) return 4;
        return power >= 60 ? 3 : 0;
    }

    /** Ticks between the end of one dash and the telegraph of the next: 14 at 60%, quicker later, never under 8. */
    public static int rampageBreath(int power) { return Math.max(8, 20 - 3 * PowerCurve.stage(power)); }

    /** The most blocks a single dash of a Rampage covers: the move's reach, shortened so the whole Rampage stays inside the arena. */
    public static double dashLength(double reach, int dashes) { return Math.max(4.0, Math.min(reach, 44.0 / Math.max(1, dashes))); }

    /** The point after {@code step} blocks on a lane that starts at (ox, oz) and runs along the unit vector (fx, fz). */
    public static double[] pointOn(double ox, double oz, double fx, double fz, double step) { return new double[]{ox + fx * step, oz + fz * step}; }
}
