package com.newuniverse.nusmp.avatar.cast;

/**
 * Why: an Avatar's spells must cost the AVATAR magicules, not the creator and not nothing. The mod's own cost formula reads "max magicule" of the
 * caster, so the pool is described by pure numbers here: a maximum (the Avatar's Tensura magicule maximum, or a floor that grows with its
 * intelligence when Tensura gives it none), a current value and a regeneration rate. Where the current value is stored (the Avatar's persistent
 * data) is the caller's business; this class is plain arithmetic and is unit-tested.
 */
public final class AvatarManaPool {
    private AvatarManaPool() {}

    /** Least pool of any Avatar, so a fresh Avatar can cast a starter page (starter pages cost max(150, 8%)). */
    public static final double FLOOR = 600.0;
    /** Extra floor per point of intelligence above 10. */
    public static final double PER_INT = 40.0;

    /** The pool maximum: the larger of Tensura's own maximum and the floor. */
    public static double max(double tensuraMax, int intelligence) {
        return Math.max(tensuraMax, FLOOR + PER_INT * Math.max(0, intelligence - 10));
    }

    /** Magicules regained per tick: the whole pool in 120 s. */
    public static double regenPerTick(double max) { return max / 2400.0; }

    /** Current value after {@code ticks} of regeneration, never above the maximum. */
    public static double regen(double current, double max, int ticks) { return Math.min(max, Math.max(0, current) + regenPerTick(max) * ticks); }

    public static boolean canPay(double current, double cost) { return cost <= 0 || current >= cost; }

    /** Value left after paying (never negative); a frenzy passes cost 0. */
    public static double pay(double current, double cost) { return Math.max(0, current - Math.max(0, cost)); }
}
