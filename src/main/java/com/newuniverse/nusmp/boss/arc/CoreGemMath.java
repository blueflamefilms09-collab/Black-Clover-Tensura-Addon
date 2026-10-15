package com.newuniverse.nusmp.boss.arc;

/**
 * Why: in the last act Lucius shows a gem on his chest and the clones take the aggro. The owner's rules are: body hits do little, a
 * critical hit or an Anti-Magic weapon on the gem breaks his resonance. The decisions in that are small pure facts (when is the gem
 * out, what is a critical hit, what does a hit on the gem do, how much does the shatter cost him) and are easy to get subtly wrong,
 * so they live here, without Minecraft types, with a test for each; the part entity and the boss only gather the inputs.
 * <ul>
 *   <li><b>Body</b>: in act 3 every blow on his body does {@value #BODY_PERCENT}% damage.</li>
 *   <li><b>Gem window</b>: {@value #OPEN_TICKS} ticks (8 s) of every {@value #CYCLE_TICKS} (14 s), the first {@value #CLOSED_FIRST}
 *       ticks of the act closed so the clones come first; also while he channels anything (the caller passes that in).</li>
 *   <li><b>Critical hit</b>: vanilla's conditions (falling, off the ground, full swing, not sprinting ...) worked out by hand because a
 *       part is not a living entity and vanilla does not crit against it.</li>
 *   <li><b>Shatter</b>: {@value #SHATTER_PERCENT}% of his maximum health as true damage (paid through the damage budget, never
 *       below 1 health), all clones gone, a {@value #SHATTER_STAGGER}-tick stagger.</li>
 * </ul>
 */
public final class CoreGemMath {
    private CoreGemMath() {}

    public static final int BODY_PERCENT = 15, SHATTER_PERCENT = 22;
    public static final int OPEN_TICKS = 160, CYCLE_TICKS = 280, CLOSED_FIRST = 120, SHATTER_STAGGER = 200, WARN_TICKS = 20;
    /** The swing must be at least this charged to count as full strength. */
    public static final float FULL_SWING = 0.9f;

    public enum Hit { HIDDEN, NORMAL, SHATTER }

    /** Is the gem out, 'ticksIntoAct' ticks after act 3 began? 'channeling' keeps it out whatever the cycle says. */
    public static boolean exposed(long ticksIntoAct, boolean channeling) {
        if (channeling) return true;
        if (ticksIntoAct < 0) return false;
        return ticksIntoAct % CYCLE_TICKS >= CLOSED_FIRST && ticksIntoAct % CYCLE_TICKS < CLOSED_FIRST + OPEN_TICKS;
    }

    /** Ticks until the gem comes out (0 if it is out), for the warning ring. */
    public static int opensIn(long ticksIntoAct) {
        if (ticksIntoAct < 0) return CLOSED_FIRST;
        long m = ticksIntoAct % CYCLE_TICKS;
        if (m >= CLOSED_FIRST && m < CLOSED_FIRST + OPEN_TICKS) return 0;
        return (int) (m < CLOSED_FIRST ? CLOSED_FIRST - m : CYCLE_TICKS - m + CLOSED_FIRST);
    }

    /** Vanilla's melee critical conditions (Player.attack), minus the entity-type test that a part fails. */
    public static boolean meleeCrit(float swingStrength, float fallDistance, boolean onGround, boolean climbing, boolean inWater,
                                    boolean blind, boolean passenger, boolean sprinting) {
        return swingStrength > FULL_SWING && fallDistance > 0f && !onGround && !climbing && !inWater && !blind && !passenger && !sprinting;
    }

    /** What a blow on the gem does. */
    public static Hit classify(boolean exposed, boolean antiMagic, boolean crit) {
        if (!exposed) return Hit.HIDDEN;
        return antiMagic || crit ? Hit.SHATTER : Hit.NORMAL;
    }

    /** The factor on a blow on his body or on a non-critical blow on the gem. */
    public static float bodyFactor(int phase) { return phase == LuciusArc.CLONES ? BODY_PERCENT / 100f : 1f; }

    /** The true damage of a shatter: the share of his maximum, never more than leaves him 1 health. */
    public static float shatterDamage(float maxHealth, float current) {
        float want = maxHealth * SHATTER_PERCENT / 100f;
        return Math.max(0f, Math.min(want, current - 1f));
    }

    /** True damage that is paid out over several ticks because the damage budget lets only a share through each second. */
    public static final class Burn {
        private float pending;

        public void add(float amount) { if (amount > 0f) pending += amount; }

        public float pending() { return pending; }

        public boolean active() { return pending > 0.01f; }

        /** The amount to try to take this tick: all that is pending. */
        public float want() { return pending; }

        /** The health that actually fell this tick: that much of the pending debt is paid. */
        public void paid(float dropped) { pending = Math.max(0f, pending - Math.max(0f, dropped)); }

        public void clear() { pending = 0f; }
    }
}
