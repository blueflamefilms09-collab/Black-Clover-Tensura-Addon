package com.newuniverse.nusmp.heroweapon.weapon.hammer;

/**
 * Why: the Vassal Hammer is the weapon of impact (the series: a shield is weak against impact; the hammer's wielder breaks what the shield hero cannot). Every blow of it builds
 * POISE damage on the foe, an unseen gauge that is as big as the foe is tough; when the gauge is full the foe BREAKS (staggered, slowed, easier to hurt, for a moment) and cannot
 * be broken again for a while, so the wielder gets a window to strike and the foe a window to recover, instead of a stun that could be chained forever. The gauge lives on the
 * foe itself (four numbers in its saved data) and is read LAZILY: it recovers by the clock, so no timer is needed and nothing is left behind when the foe unloads or dies. This
 * class is that arithmetic, pure, so the gauge can be tested without a server. Times are game ticks.
 */
public final class PoiseMath {
    private PoiseMath() {}

    /** Ticks after the last blow before the gauge starts to recover, and the ticks a full gauge takes to empty. */
    public static final int DECAY_DELAY = 60, DECAY_SPAN = 100;
    /** How long a break lasts (an ordinary foe / a boss) and how long the foe then stays unbreakable. */
    public static final int BREAK_TICKS = 50, BREAK_TICKS_BOSS = 20, LOCKOUT = 200, LOCKOUT_BOSS = 400;
    /** A stored lock further ahead than any lock can be is a clock that went backwards (a copied entity): it counts as over. */
    public static final long MAX_LOCK = 700;

    /** The gauge of one foe: points, the tick of the last blow, the tick the break ends, the tick the foe can be broken again. */
    public record State(float value, long stamp, long brokenUntil, long lockUntil) {
        public static final State EMPTY = new State(0f, 0L, 0L, 0L);
    }

    /** The new state after a blow, and whether it broke the foe. */
    public record Result(State state, boolean broke) {}

    /** The size of a foe's gauge: 60 percent of its health from 12 to 80 points; a boss 25 percent of its health from 100 to 300. */
    public static float capacity(float maxHealth, boolean boss) {
        return boss ? clamp(maxHealth * 0.25f, 100f, 300f) : clamp(maxHealth * 0.6f, 12f, 80f);
    }

    /** The points in the gauge at {@code now}: held for {@link #DECAY_DELAY} ticks after the last blow, then falling a {@code capacity / DECAY_SPAN} a tick. */
    public static float current(State s, long now, float capacity) {
        long idle = Math.max(0L, now - s.stamp());
        float held = Math.min(Math.max(s.value(), 0f), capacity);
        if (idle <= DECAY_DELAY) return held;
        double fall = (idle - DECAY_DELAY) * (double) capacity / DECAY_SPAN;
        return (float) Math.max(0.0, held - fall);
    }

    /** True while the foe is broken. */
    public static boolean broken(State s, long now) { return now < s.brokenUntil() && s.brokenUntil() - now <= MAX_LOCK; }

    /** True while the foe cannot take poise damage (broken, or recovering from a break). */
    public static boolean locked(State s, long now) { return now < s.lockUntil() && s.lockUntil() - now <= MAX_LOCK; }

    /** Adds {@code amount} points. A foe that is broken or recovering takes none. When the gauge fills, the foe breaks and the gauge starts again empty. */
    public static Result add(State s, float amount, long now, float capacity, boolean boss) {
        if (amount <= 0f || locked(s, now)) return new Result(s, false);
        float v = current(s, now, capacity) + amount;
        if (v >= capacity) {
            long end = now + (boss ? BREAK_TICKS_BOSS : BREAK_TICKS);
            return new Result(new State(0f, now, end, end + (boss ? LOCKOUT_BOSS : LOCKOUT)), true);
        }
        return new Result(new State(v, now, s.brokenUntil(), s.lockUntil()), false);
    }

    /** The gauge as a share of its size, 0 to 1 (for the feedback of a blow). */
    public static float fill(State s, long now, float capacity) { return capacity <= 0f ? 0f : clamp(current(s, now, capacity) / capacity, 0f, 1f); }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
