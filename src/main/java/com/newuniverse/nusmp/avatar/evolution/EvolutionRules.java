package com.newuniverse.nusmp.avatar.evolution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Why: "as they gain EP, they undergo Harvest Festivals or racial evolutions dynamically" needs a few numbers decided in one place and tested without a game:
 * how much EP an Avatar needs before a given race is open to it (a share of the target race's own base EP, so a goblin does not become a hobgoblin by
 * accident but a hobgoblin-sized goblin does), when the check is due (not twice in a row, not during a festival), how the chain of evolutions of a race is walked
 * without ever looping, and how much EP a kill adds to an Avatar so that its EP can grow at all. The Minecraft side ({@link AvatarEvolution}) only reads the
 * Avatar's EP and the races and applies the result.
 */
public final class EvolutionRules {
    private EvolutionRules() {}

    /** The share of the target race's base EP an Avatar must reach before it evolves into it. */
    public static final double DEFAULT_FACTOR = 0.6;

    /** EP needed to evolve into a race whose base EP is {@code targetBaseEp}: a share of it, the share kept between 5% and 500%. */
    public static double threshold(double targetBaseEp, double factor) {
        double f = Double.isNaN(factor) ? DEFAULT_FACTOR : Math.max(0.05, Math.min(5.0, factor));
        return Math.max(0.0, targetBaseEp) * f;
    }

    /** Is the evolution due: enough EP, no festival running, and the cooldown since the last evolution over? */
    public static boolean due(double ep, double threshold, boolean busy, long now, long lastEvolvedAt, long cooldownTicks) {
        if (busy || threshold <= 0.0 || ep < threshold) return false;
        return lastEvolvedAt <= 0 || now - lastEvolvedAt >= Math.max(0, cooldownTicks);
    }

    /** The races an Avatar passes through from {@code start} (not including it), stopping at the end of the chain, at a repeat, or after {@code max} steps. */
    public static List<String> chain(Function<String, String> next, String start, int max) {
        List<String> out = new ArrayList<>();
        String cur = start;
        for (int i = 0; i < Math.max(0, max); i++) {
            String n = cur == null ? null : next.apply(cur);
            if (n == null || n.isEmpty() || n.equals(start) || out.contains(n)) break;
            out.add(n);
            cur = n;
        }
        return out;
    }

    /** After a change of race the EP must not be lower than before it: the factor to scale it by (before / after when it fell, else 1.0; unknown numbers give 1.0). */
    public static double keepFactor(double before, double after) {
        if (!(before > 0.0) || !(after > 0.0) || before <= after) return 1.0;
        return before / after;
    }

    // ---------------------------------------------------------------- growth
    /** A victim weaker than this share of the Avatar's EP is not worth anything (no farming rabbits). */
    public static final double WORTHY_SHARE = 0.10;
    /** One kill never adds more than this share of the Avatar's own EP. */
    public static final double MAX_GAIN_SHARE = 0.02;

    /** EP a kill adds: {@code share} of the victim's EP, nothing for a weak victim, never more than 2% of the Avatar's own EP at once. */
    public static double killGain(double victimEp, double avatarEp, double share) {
        if (victimEp <= 0.0 || avatarEp <= 0.0 || share <= 0.0) return 0.0;
        if (victimEp < avatarEp * WORTHY_SHARE) return 0.0;
        return Math.min(victimEp * share, avatarEp * MAX_GAIN_SHARE);
    }

    // ---------------------------------------------------------------- the festival
    /** Ticks left of a festival after one more tick (never below zero) and whether it has just ended. */
    public record Step(int left, boolean ended) {}

    public static Step festivalTick(int left) {
        if (left <= 0) return new Step(0, false);
        int n = left - 1;
        return new Step(n, n == 0);
    }
}
