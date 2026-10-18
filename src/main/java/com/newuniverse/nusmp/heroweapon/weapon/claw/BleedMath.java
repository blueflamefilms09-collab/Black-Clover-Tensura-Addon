package com.newuniverse.nusmp.heroweapon.weapon.claw;

/**
 * Why: the Claw's blows open wounds that go on hurting after the claw is gone, and its Hemorrhage cashes them in. A wound count (stacks) and the tick it closes are the whole
 * state; they live on the foe itself (see {@code ClawBleed}), so nothing is left in a static table when the foe unloads or dies. This class is the arithmetic, pure, so the
 * stacking, the refresh, the pulse size and the burst can be tested without a server: stacks add up to a limit, every new wound renews the time, a closed wound counts as none,
 * a pulse is a little damage a stack, and the burst of a Hemorrhage is a base plus a share a stack up to a cap. Times are game ticks.
 */
public final class BleedMath {
    private BleedMath() {}

    /** Ordinary and Dragon-Claw limits of wounds, and how long a wound stays open (ticks). */
    public static final int MAX_STACKS = 5, MAX_STACKS_FANGS = 7, TICKS = 120, TICKS_FANGS = 160;
    /** Ticks between two pulses of the bleed, and the longest a chain of pulses may run on one foe (a repeating task never outlives 20 seconds). */
    public static final int INTERVAL = 20, CHAIN_MAX = 380;
    /** The raw damage of one pulse for each wound. */
    public static final float PULSE_PER_STACK = 0.9f;
    /** The burst of a Hemorrhage: this plus this a wound, never more than the cap. */
    public static final float BURST_BASE = 2.0f, BURST_PER_STACK = 2.6f, BURST_MAX = 15.0f;
    /** A stored time further ahead than any wound can last is a clock that went backwards (a copied entity): it counts as closed. */
    public static final long MAX_AHEAD = 400;

    /** The wounds of one foe: how many, and the tick they close. */
    public record State(int stacks, long until) {
        public static final State EMPTY = new State(0, 0L);
    }

    public static boolean active(State s, long now) { return s.stacks() > 0 && now < s.until() && s.until() - now <= MAX_AHEAD; }

    /** The wounds open at {@code now}. */
    public static int stacks(State s, long now) { return active(s, now) ? s.stacks() : 0; }

    /** Opens {@code add} more wounds (up to {@code max}) and renews the time to {@code duration} from now. */
    public static State add(State s, int add, int max, long now, int duration) {
        if (add <= 0) return s;
        return new State(Math.min(Math.max(1, max), stacks(s, now) + add), now + Math.max(1, duration));
    }

    /** The raw damage of one pulse of {@code stacks} wounds. */
    public static float pulse(int stacks) { return Math.max(0, stacks) * PULSE_PER_STACK; }

    /** The raw damage of a Hemorrhage on {@code stacks} wounds. */
    public static float burst(int stacks) { return stacks <= 0 ? 0f : Math.min(BURST_MAX, BURST_BASE + BURST_PER_STACK * stacks); }

    /** True when a pulse chain started at {@code start} has run its course at {@code now}. */
    public static boolean chainOver(long start, long now) { return now - start >= CHAIN_MAX; }
}
