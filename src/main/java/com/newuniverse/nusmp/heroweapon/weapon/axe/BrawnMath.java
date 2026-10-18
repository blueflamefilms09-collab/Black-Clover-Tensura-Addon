package com.newuniverse.nusmp.heroweapon.weapon.axe;

/**
 * Why: the canon power-up method of the Vassal Axe is muscle enhancement, a physical strengthening of the body (from a search summary of the fandom wiki; the wiki itself says the
 * details are unknown). Here it is BRAWN: a stack of up to eight that the wielder earns by fighting with the axe (a full swing, a technique that hits, a kill) and that makes the
 * body stronger while it lasts: more melee damage, more weight behind the techniques, harder to shove, a little faster. It is held for six seconds after the last gain, then
 * fades one stack every two seconds (every four with the Shine Axe's Radiance). It is the ammunition of Shine Axe Burst, which needs five and spends them all. The stack is two
 * numbers on the wielder (how many, and the tick of the last gain) and is read LAZILY: the fading is worked out from the clock when someone asks, so no timer is needed. Pure.
 */
public final class BrawnMath {
    private BrawnMath() {}

    public static final int MAX = 8, SHINE_MIN = 5;
    /** Ticks a stack is held after the last gain, then the ticks between two stacks fading (normally / with Radiance). */
    public static final int HOLD = 120, STEP = 40, STEP_SLOW = 80;
    /** What each stack gives: a share more melee damage, a share more raw on techniques, knockback resistance, a share more speed. */
    public static final double MELEE_PER_STACK = 0.03, SKILL_PER_STACK = 0.02, KNOCKBACK_PER_STACK = 0.04, SPEED_PER_STACK = 0.01;
    /** Shine Axe Burst: the raw damage with no Brawn beyond the minimum, and for each stack. */
    public static final double SHINE_BASE = 14.0, SHINE_PER_STACK = 2.2;

    /** The stacks standing at {@code now}, given the stored count and the tick of the last gain. */
    public static int current(int stored, long stamp, long now, boolean slowFade) {
        int held = Math.max(0, Math.min(stored, MAX));
        if (held == 0) return 0;
        long idle = Math.max(0L, now - stamp);
        if (idle <= HOLD) return held;
        long lost = (idle - HOLD) / (slowFade ? STEP_SLOW : STEP);
        return (int) Math.max(0L, held - lost);
    }

    /** The stacks after gaining {@code add} at {@code now} (the caller stores the result with the stamp {@code now}). */
    public static int gain(int stored, long stamp, long now, int add, boolean slowFade) { return Math.min(MAX, current(stored, stamp, now, slowFade) + Math.max(0, add)); }

    public static double meleeBonus(int stacks) { return MELEE_PER_STACK * clamp(stacks); }

    /** The factor on a technique's raw damage. */
    public static double skillMult(int stacks) { return 1.0 + SKILL_PER_STACK * clamp(stacks); }

    public static double knockbackResistance(int stacks) { return KNOCKBACK_PER_STACK * clamp(stacks); }

    public static double speedBonus(int stacks) { return SPEED_PER_STACK * clamp(stacks); }

    public static boolean canShine(int stacks) { return stacks >= SHINE_MIN; }

    /** The raw damage of Shine Axe Burst for the stacks it spends: 14 plus 2.2 each, so 25 at five and 31.6 at eight. */
    public static float shineRaw(int stacks) { return (float) (SHINE_BASE + SHINE_PER_STACK * clamp(stacks)); }

    private static int clamp(int stacks) { return Math.max(0, Math.min(MAX, stacks)); }
}
