package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: one random plan is not enough. The assembler can run out of room for the boss chain, or build a sector that is legal but thin (too few rooms, no
 * loops), and the right answer is to try another seed, not to give up on the sector or to ship the thin one. This runs the assembler with the seeds
 * {@code trySeed(seed, 0..n-1)}, audits each plan with the full {@link SectorAudit}, and returns the first that passes; when every try fails it falls back
 * to the smallest sector that is still a sector (the Landing and the boss chain only), which skips the pacing checks but still passes the structural ones,
 * and when even that cannot be built it says so, with the reasons the tries failed for, so the log tells the owner what to fix. The seeds are derived from
 * the request's seed only, so the same sector always resolves to the same try and the same plan: a sector regenerated after a crash is the one that was placed.
 */
public final class SectorSolver {
    private SectorSolver() {}

    /**
     * The outcome. {@code result} is the plan that will be placed (null when nothing could be built); {@code tries} is how many assembler runs it took
     * (the fallback counts as one more); {@code fallback} is true when the minimal layout was used; {@code failures} are the distinct reasons the failed
     * tries gave, with how often each came up; {@code audit} is empty for a plan that passed, else what the best rejected plan failed on; {@code deadEnd}
     * is true when the plan is valid but its Way Onward faces a wall of older sectors (no try found a seal with room behind it), so the dungeon ends there.
     */
    public record Solved(Result result, int tries, boolean fallback, boolean deadEnd, Map<String, Integer> failures, List<String> audit) {
        public boolean ok() { return result != null && result.assembled(); }
    }

    public static Solved solve(Request base, int maxTries) { return solve(base, maxTries, MegaAssembler::assemble); }

    /** {@link #solve(Request, int)} with the assembler injected, so a test can script what each try returns. */
    static Solved solve(Request base, int maxTries, java.util.function.Function<Request, Result> assembler) {
        int tries = Math.max(1, maxTries);
        Map<String, Integer> failures = new LinkedHashMap<>();
        Result deadEndPlan = null;
        int deadEndTries = 0;
        List<String> bestAudit = null;
        for (int i = 0; i < tries; i++) {
            Request q = base.withSeed(SectorPlanner.trySeed(base.seed, i), false);
            Result r = assembler.apply(q);
            List<String> audit = SectorAudit.audit(r, q, true);
            if (audit.isEmpty()) {
                if (r.expansion != null && r.expansion.deadEnd()) {          // valid, but nothing can follow: keep looking for a live continuation, remember this one
                    failures.merge("the Way Onward faces a wall of older sectors", 1, Integer::sum);
                    if (deadEndPlan == null) {
                        deadEndPlan = r;
                        deadEndTries = i + 1;
                    }
                    continue;
                }
                return new Solved(r, i + 1, false, false, failures, List.of());
            }
            for (String a : audit) failures.merge(stripNumbers(a), 1, Integer::sum);
            if (r.assembled() && (bestAudit == null || audit.size() < bestAudit.size())) bestAudit = audit;
        }
        if (deadEndPlan != null) return new Solved(deadEndPlan, deadEndTries, false, true, failures, List.of());
        Request fq = base.withSeed(SectorPlanner.trySeed(base.seed, tries), true);
        Result fr = assembler.apply(fq);
        List<String> faudit = SectorAudit.audit(fr, fq, false);
        if (faudit.isEmpty()) return new Solved(fr, tries + 1, true, fr.expansion != null && fr.expansion.deadEnd(), failures, bestAudit == null ? List.of() : bestAudit);
        for (String a : faudit) failures.merge("fallback: " + stripNumbers(a), 1, Integer::sum);
        return new Solved(null, tries + 1, true, false, failures, faudit);
    }

    /** A short readable summary of the failures, most frequent first: {@code 12x no boss chain ...; 3x only # rooms ...}. */
    public static String summary(Map<String, Integer> failures, int max) {
        List<Map.Entry<String, Integer>> es = new ArrayList<>(failures.entrySet());
        es.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < es.size() && i < max; i++) {
            if (i > 0) b.append(" | ");
            b.append(es.get(i).getValue()).append("x ").append(es.get(i).getKey());
        }
        return b.toString();
    }

    /** Replaces runs of digits with {@code #} so that failures that differ only in a count are tallied together. */
    static String stripNumbers(String s) { return s.replaceAll("[0-9]+", "#"); }
}
