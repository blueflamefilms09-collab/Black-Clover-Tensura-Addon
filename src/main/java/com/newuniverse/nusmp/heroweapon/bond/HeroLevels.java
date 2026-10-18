package com.newuniverse.nusmp.heroweapon.bond;

/**
 * Why: a hero weapon grows with its wielder (the series' level of the hero and of the weapon). The pure arithmetic of that growth lives apart from the game so it
 * is tested without a server and every system (the skills' power, the cooldown, the HUD, the Weapon Book) reads the same numbers. Experience is the weapon's own
 * (gained by blows, kills and mastery; refused while two Holy wielders stand close, see the rules package), not vanilla experience. Level 0 to {@link #MAX}; the
 * experience that reaches level L is {@code 40 * L^1.7}, so level 10 needs about 2 000 and level 100 about 100 000 points.
 */
public final class HeroLevels {
    private HeroLevels() {}

    public static final int MAX = 100;

    /** Total experience needed to be at {@code level} (0 for level 0). */
    public static long expFor(int level) {
        int l = Math.max(0, Math.min(MAX, level));
        return l == 0 ? 0 : Math.round(40.0 * Math.pow(l, 1.7));
    }

    /** The level an experience total has reached, 0 to {@link #MAX}. */
    public static int level(long exp) {
        if (exp <= 0) return 0;
        int lo = 0, hi = MAX;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (expFor(mid) <= exp) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /** The damage and strength factor of a level: 1.0 at level 0 up to 2.2 at level 100. */
    public static double power(int level) { return 1.0 + 0.012 * Math.max(0, Math.min(MAX, level)); }

    /** The cooldown factor of a level: 1.0 at level 0 down to 0.75 at level 100. */
    public static double cooldownScale(int level) { return 1.0 - Math.min(0.25, 0.0025 * Math.max(0, level)); }

    /** Share (0..1) of the way from this level to the next, for the HUD bar; 1.0 at the maximum. */
    public static double progress(long exp) {
        int l = level(exp);
        if (l >= MAX) return 1.0;
        long a = expFor(l), b = expFor(l + 1);
        return b <= a ? 1.0 : Math.max(0.0, Math.min(1.0, (exp - a) / (double) (b - a)));
    }
}
