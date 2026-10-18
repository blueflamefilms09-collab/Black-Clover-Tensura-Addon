package com.newuniverse.nusmp.elite.races.rule;

import java.util.List;
import java.util.Locale;

/**
 * Why: the five kinds of requirement the owner named, as small records, in one file because each is a few lines. All are pure and clamp their progress to 0..1.
 * {@link AnyOf} is the one composite: "this OR that" (for instance five boss kills or the Saiyan Legend record), which is how a rule that needs the records system
 * can still be satisfied while that system is absent.
 */
public final class EvolutionRules {
    private EvolutionRules() {}

    private static double ratio(double have, double need) {
        if (need <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, have / need));
    }

    private static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.format(Locale.ROOT, "%,d", (long) v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** At least {@code min} maximum EP (times the server's gate scale). */
    public record EpGate(double min, double weight) implements RaceEvolutionRule {
        @Override public String type() { return "ep"; }

        @Override public double progress(RaceCtx c) { return ratio(c.ep(), min * Math.max(0.0, c.epScale())); }

        @Override public String describe() { return "Reach " + num(min) + " EP"; }
    }

    /** At least {@code count} kills of a category ({@code any}, {@code hostile}, {@code boss}, {@code player}). */
    public record Kills(String key, int count, double weight) implements RaceEvolutionRule {
        @Override public String type() { return "kills"; }

        @Override public double progress(RaceCtx c) { return ratio(c.kills(key), count); }

        @Override public String describe() {
            return "Defeat " + count + ("any".equals(key) ? " creatures" : " " + key + " creatures");
        }
    }

    /** At least {@code fraction} (0..1) mastery of a skill. */
    public record Mastery(String skill, double fraction, double weight) implements RaceEvolutionRule {
        @Override public String type() { return "mastery"; }

        @Override public double progress(RaceCtx c) { return ratio(c.mastery(skill), fraction); }

        @Override public String describe() { return "Master " + skill + " to " + Math.round(fraction * 100) + "%"; }
    }

    /** Carry {@code count} of an item; with {@code consume} the evolution takes it. */
    public record Item(String item, int count, boolean consume, double weight) implements RaceEvolutionRule {
        @Override public String type() { return "item"; }

        @Override public double progress(RaceCtx c) { return ratio(c.itemCount(item), count); }

        @Override public String describe() { return (consume ? "Offer " : "Carry ") + count + " x " + item; }

        @Override public void commit(RaceCtx c) { if (consume) c.consume(item, count); }
    }

    /** Have a record unlocked (read through the records system's hook). */
    public record RecordUnlocked(String id, double weight) implements RaceEvolutionRule {
        @Override public String type() { return "record"; }

        @Override public double progress(RaceCtx c) { return c.record(id) ? 1.0 : 0.0; }

        @Override public String describe() { return "Unlock the record " + id; }
    }

    /** Satisfied when any one child is; progress is the best child's. Only the first met child commits. */
    public record AnyOf(List<RaceEvolutionRule> rules, double weight) implements RaceEvolutionRule {
        public AnyOf { rules = List.copyOf(rules); }

        @Override public String type() { return "any_of"; }

        @Override public double progress(RaceCtx c) {
            double best = 0.0;
            for (RaceEvolutionRule r : rules) best = Math.max(best, r.progress(c));
            return best;
        }

        @Override public String describe() {
            StringBuilder sb = new StringBuilder("Either: ");
            for (int i = 0; i < rules.size(); i++) sb.append(i == 0 ? "" : " or ").append(rules.get(i).describe());
            return sb.toString();
        }

        @Override public void commit(RaceCtx c) {
            for (RaceEvolutionRule r : rules) {
                if (r.met(c)) {
                    r.commit(c);
                    return;
                }
            }
        }
    }
}
