package com.newuniverse.nusmp.elite.world.rules;

import java.util.List;

/**
 * Why: the phase rules of a calamity boss as pure functions. A boss has {@code thresholds.size() + 1} phases (index 0 is the first); the thresholds are health fractions in descending order
 * (0.66, 0.33: a new phase at two thirds and at one third). Two rules keep the fight honest: a single blow can never skip a phase (the damage is cut so the health stops at the next
 * threshold, the boss changes phase, and only then can it fall further), and each phase tightens the boss (shorter cooldowns, harder hits, more helpers). No phase heals the boss:
 * the boss rules of the mod forbid regeneration in combat.
 */
public final class CalamityPhases {
    private CalamityPhases() {}

    /**
     * A health this close to a line (a fraction of the maximum) counts as ON it: floats do not land exactly on 0.66 * 2400, and a boss that stopped a hair above its line must still change
     * phase, or it would be stuck there (nothing can hurt it, and the phase never comes).
     */
    public static final double EPS = 1e-4;

    /** The phase index for a health fraction (1.0 = full). */
    public static int phaseFor(double healthFraction, List<Double> thresholds) {
        int p = 0;
        for (double t : thresholds) if (healthFraction <= t + EPS) p++;
        return p;
    }

    /** The last phase index. */
    public static int last(List<Double> thresholds) { return thresholds.size(); }

    /** The fraction at which the NEXT phase begins after {@code phase}, or -1 when this is the last phase. */
    public static double nextThreshold(int phase, List<Double> thresholds) { return phase >= 0 && phase < thresholds.size() ? thresholds.get(phase) : -1.0; }

    /**
     * The damage a blow may do from {@code health} of {@code max} while the boss is in {@code phase}: it is cut so that the health does not go under the next threshold (it may land exactly
     * on it). In the last phase it is not cut. A non-positive amount stays 0.
     */
    public static float clampDamage(float health, float max, int phase, List<Double> thresholds, float amount) {
        if (!(amount > 0f) || max <= 0f) return 0f;
        double next = nextThreshold(phase, thresholds);
        if (next < 0.0) return amount;
        float floor = (float) (next * max);
        float room = health - floor;
        return room <= EPS * max ? 0f : Math.min(amount, room);
    }

    /** What a phase changes about the boss's pace. */
    public record Tuning(double cooldownMult, double damageMult, double speedMult, int addCap) {}

    /** The tuning of phase {@code p} (0-based) of a boss that has {@code phaseCount} phases: gradual, never under 55% of the cooldown or over x1.6 damage. */
    public static Tuning tuning(int p, int phaseCount) {
        int last = Math.max(1, phaseCount - 1);
        double k = Math.max(0.0, Math.min(1.0, p / (double) last));
        return new Tuning(1.0 - 0.45 * k, 1.0 + 0.6 * k, 1.0 + 0.12 * k, 2 + (int) Math.round(6 * k));
    }
}
