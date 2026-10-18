package com.newuniverse.nusmp.elite.races.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: an evolution is a list of rules that must ALL hold, shown as one progress bar. This class does the three things the callers need and nothing else: the bar
 * ({@link #percent}: each rule's progress times its weight, over the sum of the weights), the yes/no ({@link #allMet}, with the unmet rules listed for the message) and
 * the commit ({@link #tryCommit}: only when every rule is met, each rule's {@link RaceEvolutionRule#commit} runs once). An empty set is met at 100 %, so a race with no
 * rules evolves freely (an administrator's choice in the race file).
 */
public final class RaceRuleSet {
    public static final RaceRuleSet EMPTY = new RaceRuleSet(List.of());

    private final List<RaceEvolutionRule> rules;

    public RaceRuleSet(List<RaceEvolutionRule> rules) { this.rules = List.copyOf(rules); }

    public List<RaceEvolutionRule> rules() { return rules; }

    public boolean isEmpty() { return rules.isEmpty(); }

    private double totalWeight() {
        double t = 0;
        for (RaceEvolutionRule r : rules) t += Math.max(0.0, r.weight());
        return t;
    }

    /** The share of one rule in the bar, 0..100 (weights normalised to sum 100). */
    public double sharePercent(RaceEvolutionRule rule) {
        double t = totalWeight();
        if (t <= 0) return rules.isEmpty() ? 0.0 : 100.0 / rules.size();
        return Math.max(0.0, rule.weight()) / t * 100.0;
    }

    /** The whole bar, 0..100. */
    public double percent(RaceCtx ctx) {
        if (rules.isEmpty()) return 100.0;
        double sum = 0;
        for (RaceEvolutionRule r : rules) sum += sharePercent(r) * Math.max(0.0, Math.min(1.0, r.progress(ctx)));
        return Math.max(0.0, Math.min(100.0, sum));
    }

    public boolean allMet(RaceCtx ctx) {
        for (RaceEvolutionRule r : rules) if (!r.met(ctx)) return false;
        return true;
    }

    public List<RaceEvolutionRule> unmet(RaceCtx ctx) {
        List<RaceEvolutionRule> out = new ArrayList<>();
        for (RaceEvolutionRule r : rules) if (!r.met(ctx)) out.add(r);
        return out;
    }

    /** The outcome of an attempt: {@code ok}, or the descriptions of what is still missing. */
    public record Outcome(boolean ok, List<String> missing) {}

    /** Commits the set if (and only if) every rule is met. */
    public Outcome tryCommit(RaceCtx ctx) {
        List<RaceEvolutionRule> unmet = unmet(ctx);
        if (!unmet.isEmpty()) {
            List<String> words = new ArrayList<>();
            for (RaceEvolutionRule r : unmet) words.add(r.describe());
            return new Outcome(false, words);
        }
        for (RaceEvolutionRule r : rules) r.commit(ctx);
        return new Outcome(true, List.of());
    }

    /** Takes what the rules take, without checking (used when an administrator forces an evolution that Tensura's own screen already validated). */
    public void commitOnly(RaceCtx ctx) {
        for (RaceEvolutionRule r : rules) r.commit(ctx);
    }
}
