package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the pure heart of the data-driven evolution: given a path and a context, are ALL requirements met, and if not which are missing; and, once the player (or a test) commits, pay
 * the requirements that cost something (item offerings) exactly once. Checking and paying are separate on purpose: a path with an offering and a kill count must never take the offering
 * while the kills are still short. A path with no requirements is never satisfied (an empty list in a data file is almost certainly a mistake, and "free evolution" is not a feature).
 */
public final class EvolutionEngine {
    private EvolutionEngine() {}

    public record Outcome(boolean ok, List<String> unmet) {}

    /** Tests every requirement. {@code unmet} lists a line per requirement that failed. */
    public static Outcome check(EvolutionTable.Path path, EvolutionContext ctx) {
        if (path == null || ctx == null || path.reqs().isEmpty()) return new Outcome(false, List.of("no requirements are defined"));
        List<String> unmet = new ArrayList<>();
        for (EvolutionTable.Req r : path.reqs()) {
            boolean ok;
            try {
                ok = r.trigger().test(ctx, r.params(), path.from());
            } catch (RuntimeException e) {
                ok = false;                                                         // a trigger that throws is a failed trigger, never a crash or a free pass
            }
            if (!ok) unmet.add(r.trigger().describe(r.params(), path.from()));
        }
        return new Outcome(unmet.isEmpty(), List.copyOf(unmet));
    }

    /**
     * Re-checks and then pays every requirement. Returns true when the path is complete and everything was paid; false when something was missing (nothing is paid then)
     * or an offering could not be taken after all.
     */
    public static boolean commit(EvolutionTable.Path path, EvolutionContext ctx) {
        if (!check(path, ctx).ok()) return false;
        boolean paid = true;
        for (EvolutionTable.Req r : path.reqs()) {
            try {
                paid &= r.trigger().consume(ctx, r.params(), path.from());
            } catch (RuntimeException e) {
                paid = false;
            }
        }
        return paid;
    }
}
