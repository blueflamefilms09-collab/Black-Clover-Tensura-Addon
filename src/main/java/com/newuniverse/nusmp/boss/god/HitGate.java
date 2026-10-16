package com.newuniverse.nusmp.boss.god;

/**
 * Why: custom 3D weapons and burst builds can take a boss apart in one swing, and the owner wants a hard rule instead: no single hit
 * may take more than a small share of his maximum health (2% by default), and when the cap bites he answers, alternating an evasive
 * teleport and a counter-attack, never more often than every 3 seconds and never while he is busy (a channel, a transformation, a
 * stage change). The cap is a pure function and the reaction is a tiny state machine here, so the rules are unit tested; the entity
 * applies the amount at the very start of the damage path, before the per-second budget, and starts the reaction.
 * <p>
 * Exempt on purpose, because none of them is a "hit": the gem shatter (a debt paid over seconds), Time Reversal's restore, the stage
 * refill and every heal. They never come through the damage path this class guards.
 */
public final class HitGate {
    /** Ticks between two reactions to a clamped hit. */
    public static final int REACT_COOLDOWN = 60;

    /** What he does when a hit was clamped. */
    public enum Reaction { TELEPORT, COUNTER }

    /** The amount that goes on, and whether the cap bit. */
    public record Result(float amount, boolean clamped) {}

    private long nextReact;
    private int flip;

    /** The most one hit may take: 'pct' percent of the maximum (never negative). */
    public static float cap(float maxHealth, double pct) {
        if (!(maxHealth > 0f) || !(pct > 0.0)) return 0f;
        return (float) (maxHealth * pct / 100.0);
    }

    /** Clamps one hit. Non-positive and NaN amounts pass unchanged (nothing to clamp); a pct of 0 or less switches the gate off. */
    public static Result clamp(float amount, float maxHealth, double pct) {
        if (!(amount > 0f) || !(pct > 0.0) || !(maxHealth > 0f)) return new Result(amount, false);
        float cap = cap(maxHealth, pct);
        return amount > cap ? new Result(cap, true) : new Result(amount, false);
    }

    /**
     * The reaction to a clamped hit now, or null when it is on cooldown or he is busy. Alternates: the first is a teleport, then a
     * counter, then a teleport again. A refused call changes nothing, so a busy moment does not eat the alternation.
     */
    public Reaction react(long now, boolean busy) {
        if (busy || now < nextReact) return null;
        nextReact = now + REACT_COOLDOWN;
        return (flip++ & 1) == 0 ? Reaction.TELEPORT : Reaction.COUNTER;
    }

    public long nextReactAt() { return nextReact; }

    public void reset() {
        nextReact = 0;
        flip = 0;
    }

    /** {nextReact, flip} for the save data. */
    public long[] save() { return new long[]{nextReact, flip}; }

    public void restore(long[] s) {
        reset();
        if (s == null || s.length < 2) return;
        nextReact = s[0];
        flip = (int) (s[1] & 1);
    }
}
