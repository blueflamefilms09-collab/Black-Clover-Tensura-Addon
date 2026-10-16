package com.newuniverse.nusmp.avatar.outcome;

/**
 * Why: the owner's outcome engine is a weighted roll whose weights move with the creator's stored luck (luck is subtracted from the 30% Betrayal chance
 * and added to the 25% Soul Awakening chance). The weights and the decision are pure arithmetic, so they are here without Minecraft and unit tested; the
 * entity only feeds in a random number.
 */
public final class OutcomeMath {
    private OutcomeMath() {}

    /** The three chances; they always sum to 1. */
    public record Weights(double betrayal, double soul, double ordinary) {}

    /** The spec numbers: shift = clamp(luck * 0.01, 0, 0.30); betrayal 0.30 - shift, soul 0.25 + shift, ordinary 0.45. */
    public static Weights weights(double luck) { return weights(luck, 0.30, 0.25, 0.01, 0.30); }

    /**
     * Weights with configurable bases. The shift can never take more than the betrayal chance there is, and never more than {@code shiftMax}; ordinary is
     * what is left over (so a tuned config can never make the sum drift away from 1).
     */
    public static Weights weights(double luck, double betrayalBase, double soulBase, double shiftPerPoint, double shiftMax) {
        double b = clamp01(betrayalBase), s = clamp01(soulBase);
        double shift = clamp(luck * shiftPerPoint, 0.0, Math.min(shiftMax, b));
        double betrayal = b - shift;
        double soul = Math.min(1.0 - betrayal, s + shift);
        double ordinary = Math.max(0.0, 1.0 - betrayal - soul);
        return new Weights(betrayal, soul, ordinary);
    }

    /** Picks an outcome for a uniform random number {@code r} in [0, 1). */
    public static Outcome pick(Weights w, double r) {
        if (r < w.betrayal()) return Outcome.BETRAYAL;
        if (r < w.betrayal() + w.soul()) return Outcome.SOUL_AWAKENING;
        return Outcome.ORDINARY;
    }

    /**
     * What happens when the timer reaches zero. A named Avatar and an Avatar with a Unique / Ultimate skill never roll (Soul-Bond Lock and independence);
     * naming wins when both apply.
     */
    public static Outcome decide(boolean named, boolean hasUniqueOrUltimate, double luck, double r) { return decide(named, hasUniqueOrUltimate, weights(luck), r); }

    public static Outcome decide(boolean named, boolean hasUniqueOrUltimate, Weights w, double r) {
        if (named) return Outcome.SKIPPED_NAMED;
        if (hasUniqueOrUltimate) return Outcome.SKIPPED_UNIQUE;
        return pick(w, r);
    }

    private static double clamp01(double v) { return clamp(v, 0.0, 1.0); }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
