package com.newuniverse.nusmp.entity.zogratis;

/**
 * The numbers of Lucius's Soul Magic (pure): how many Soul Paladins and clones he may hold at a power, how strong they are made,
 * what the Soul Drain and the Soul Purge give back to him, and how many souls he can raise in a fight. Everything is a function of the
 * power so that the party can read the fight from the boss bar: more power, more followers, never unbounded.
 */
public final class SoulMath {
    private SoulMath() {}

    // ---------------------------------------------------------------- Soul Paladins
    /** Most Paladins at once: 1 at 20% power, 2 at 40, 3 at 60, 4 from 80. */
    public static int paladinCap(int power) { return Math.min(4, 1 + PowerCurve.stage(power)); }

    /** Ticks a struck-down Paladin kneels before it rises: 12 seconds at the opening, 8 at the Gates. */
    public static int reviveTicks(int power) {
        double f = Math.max(0, Math.min(1, (power - PowerCurve.OPENING) / (double) (PowerCurve.FULL - PowerCurve.OPENING)));
        return (int) Math.round(240 - 80 * f);
    }

    /** The health a Paladin is raised to: 1.6 x its own plus 0.4 x its stage, at least 120, at most 600; never less than it had. */
    public static double paladinHealth(double baseMax, int power) {
        double want = Math.max(120, Math.min(600, baseMax * (1.6 + 0.4 * PowerCurve.stage(power))));
        return Math.max(baseMax, want);
    }

    /** The attack a Paladin is raised to: its own plus 4 plus 1.5 per stage when it hits weakly, plus 2 when it already hits hard. */
    public static double paladinAttack(double baseAttack, int power) {
        return baseAttack + (baseAttack < 14 ? 4 + 1.5 * PowerCurve.stage(power) : 2);
    }

    /** A mob is strong enough to be made a Paladin: 30 health or more (a boss is never one, see {@link #isBossScale}). */
    public static boolean worthy(double maxHealth) { return maxHealth >= 30; }

    /** Bodies of this size are bosses of their own and cannot be taken. */
    public static boolean isBossScale(double maxHealth) { return maxHealth >= 300; }

    // ---------------------------------------------------------------- clones
    /** Most clones alive at once: 2 at the opening, +1 per stage, 6 at the Gates. */
    public static int cloneCap(int power) { return Math.min(6, 2 + PowerCurve.stage(power)); }

    /** Health of a clone: low, so that they shatter under real pressure. */
    public static double cloneHealth(int power) { return 60 + 20.0 * PowerCurve.stage(power); }

    /** A clone fades after this many ticks: 30 seconds, 40 at the Gates. */
    public static int cloneLife(int power) { return PowerCurve.gatesOpen(power) ? 800 : 600; }

    /** Ticks between a clone's weak spells: 100 at the opening, 60 at the Gates. */
    public static int cloneSpellGap(int power) { return (int) Math.round(100 - 40 * Math.max(0, Math.min(1, (power - 20) / 80.0))); }

    /** Fraction of Lucius's own spell damage that a clone's spell does. */
    public static final double CLONE_DAMAGE = 0.35;

    // ---------------------------------------------------------------- draining and purifying
    /** Soul Drain: Lucius heals half of the damage a beam tick does, at most 0.6% of his maximum health per tick. */
    public static float drainHeal(float dealt, float maxHealth) { return Math.max(0f, Math.min(dealt * 0.5f, maxHealth * 0.006f)); }

    /** The most one Soul Drain can give back: 3% of his maximum health. */
    public static float drainCap(float maxHealth) { return maxHealth * 0.03f; }

    /** Soul Purge: 1.5% of his maximum health for every devil power he strips, at most 6%. */
    public static float purgeHeal(int stripped, float maxHealth) { return Math.min(maxHealth * 0.06f, Math.max(0, stripped) * maxHealth * 0.015f); }

    /** The Purge hits a foe still in Devil Union harder: x1.5. */
    public static float purgeFactor(boolean devilUnion) { return devilUnion ? 1.5f : 1.0f; }

    /** Seconds the purified Zogratis siblings stay cut off from their devil's power: 8 at 40%, 12 from 80%. */
    public static int siblingCutTicks(int power) { return (power >= 80 ? 12 : 8) * 20; }

    // ---------------------------------------------------------------- resurrection
    /** Souls he can raise in one fight: 3, +1 per stage. */
    public static int resurrectBudget(int power) { return 3 + PowerCurve.stage(power); }

    /** Most souls one Resurrection raises: 2, 3 at the Gates. */
    public static int resurrectPerCast(int power) { return PowerCurve.gatesOpen(power) ? 3 : 2; }

    /** The fights's resurrection budget: spends are counted, and refused when it is used up. */
    public static final class Budget {
        private int used;

        public int used() { return used; }
        public void reset() { used = 0; }
        public void restore(int n) { used = Math.max(0, n); }

        /** Takes up to 'want' souls from the budget; returns how many were granted. */
        public int take(int want, int power) {
            int room = Math.max(0, resurrectBudget(power) - used);
            int n = Math.max(0, Math.min(want, room));
            used += n;
            return n;
        }
    }
}
