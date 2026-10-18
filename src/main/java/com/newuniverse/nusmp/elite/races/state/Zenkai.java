package com.newuniverse.nusmp.elite.races.state;

/**
 * Why: the Saiyan trait the lore is known for: surviving a near-death fight makes the body stronger. Pure rule: a hit that leaves the player alive at or under
 * {@link #THRESHOLD} of their health adds one stack (at most once per {@link #COOLDOWN_TICKS}, never beyond the cap of the tier). Each stack is a small permanent bonus, applied by the
 * runtime as a transient modifier from the saved count: +{@link #ATTACK_PER_STACK} of the base attack and +{@link #HEALTH_PER_STACK} maximum health. The numbers are designed here
 * (the original addon's were not available) and kept small on purpose; the cap rises with the tier so the line keeps something to grow into.
 */
public final class Zenkai {
    private Zenkai() {}

    public static final double THRESHOLD = 0.25;
    public static final long COOLDOWN_TICKS = 6000L;       // 5 minutes
    public static final double ATTACK_PER_STACK = 0.03;
    public static final double HEALTH_PER_STACK = 2.0;
    public static final int CAP_PER_TIER = 3;

    /** The most stacks a race tier can hold (3 per tier). */
    public static int cap(int tier) { return Math.max(0, tier) * CAP_PER_TIER; }

    /** The record of one player: stacks and the game tick of the last stack. */
    public record State(int stacks, long lastAt) {
        public static final State NONE = new State(0, Long.MIN_VALUE / 2);
    }

    /**
     * A hit left the player at {@code hpFractionAfter} of their health (alive). Returns the new state (the same object when nothing changes).
     */
    public static State onSurvivedHit(State s, double hpFractionAfter, long now, int tier) {
        if (hpFractionAfter <= 0.0 || hpFractionAfter > THRESHOLD) return s;
        if (now - s.lastAt() < COOLDOWN_TICKS) return s;
        if (s.stacks() >= cap(tier)) return s;
        return new State(s.stacks() + 1, now);
    }

    /** Stacks above the cap of the tier (the player fell to a lower race) are dropped to the cap. */
    public static State clampTo(State s, int tier) {
        int cap = cap(tier);
        return s.stacks() <= cap ? s : new State(cap, s.lastAt());
    }

    public static double attackBonus(int stacks) { return Math.max(0, stacks) * ATTACK_PER_STACK; }

    public static double healthBonus(int stacks) { return Math.max(0, stacks) * HEALTH_PER_STACK; }
}
