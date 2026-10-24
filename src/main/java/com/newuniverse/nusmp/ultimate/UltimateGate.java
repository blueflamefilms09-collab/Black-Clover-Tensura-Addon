package com.newuniverse.nusmp.ultimate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: an Ultimate is the LATE-GAME end of a grimoire, so its page opens only at the top of the ladder, exactly like the Mana Zone gate (mana.zone.ManaZoneGate) but smaller: three requirements, pure and tested.
 * <ol>
 *   <li><b>Mastery</b>: the whole mastery bar of the grimoire (100 percent);</li>
 *   <li><b>Rank</b>: Magic Knight step 14 (Senior Magic Knight, 1st Class); Grand Magic Knight and the Wizard King count too;</li>
 *   <li><b>EP</b>: Tensura existence points, 1,000,000 (the same order of magnitude as the Mana Zone's floor).</li>
 * </ol>
 * The Wizard King is open on rank and EP by right of the crown but still needs the mastery (an Ultimate is the book's own last page). The strictness multiplier of the server config makes the EP and rank
 * stricter or a little easier but never under the floors.
 */
public final class UltimateGate {
    private UltimateGate() {}

    public static final double MASTERY_NEED = 1.0 - 1e-9;
    public static final int DEFAULT_STEP = 14, FLOOR_STEP = 12;
    public static final double DEFAULT_EP = 1_000_000.0, FLOOR_EP = 500_000.0;
    public static final double MIN_STRICTNESS = 0.5, MAX_STRICTNESS = 4.0;

    public enum Req { MASTERY, RANK, EP }

    public record Progress(double mastery, int knightStep, double ep, boolean wizardKing) {
        public static final Progress NONE = new Progress(0, 0, 0, false);
    }

    public record Check(Req req, boolean met, String have, String need) {}

    public record Verdict(boolean open, List<Check> checks) {
        public List<Check> unmet() {
            List<Check> out = new ArrayList<>();
            for (Check c : checks) if (!c.met()) out.add(c);
            return out;
        }

        /** "mastery 80% of 100%; rank Senior 4th Class of Senior 1st Class". */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (Check c : unmet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(label(c.req())).append(' ').append(c.have()).append(" of ").append(c.need());
            }
            return sb.toString();
        }
    }

    public static String label(Req r) {
        return switch (r) { case MASTERY -> "mastery"; case RANK -> "rank"; case EP -> "EP"; };
    }

    public static double clampStrictness(double s) { return Double.isNaN(s) ? 1.0 : Math.max(MIN_STRICTNESS, Math.min(MAX_STRICTNESS, s)); }

    public static double needEp(double strictness) { return Math.max(FLOOR_EP, DEFAULT_EP * clampStrictness(strictness)); }

    /** The rank is a ladder, not a quantity: the multiplier does not move it. */
    public static int needStep() { return Math.max(FLOOR_STEP, DEFAULT_STEP); }

    public static Verdict evaluate(double strictness, Progress p) {
        List<Check> out = new ArrayList<>(3);
        out.add(new Check(Req.MASTERY, p.mastery() >= MASTERY_NEED, percent(p.mastery()), "100%"));
        boolean rank = p.wizardKing() || p.knightStep() >= needStep();
        out.add(new Check(Req.RANK, rank, rank(p.knightStep()), rank(needStep())));
        double ep = Math.max(0, p.ep());
        out.add(new Check(Req.EP, p.wizardKing() || ep >= needEp(strictness), number(ep), number(needEp(strictness))));
        boolean all = true;
        for (Check c : out) all &= c.met();
        return new Verdict(all, List.copyOf(out));
    }

    public static boolean passes(double strictness, Progress p) { return evaluate(strictness, p).open(); }

    static String number(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    static String percent(double v) { return Math.round(Math.max(0, Math.min(1, v)) * 100) + "%"; }

    /** Plain name of a Magic Knight step (0 = Junior 5th Class ... 14 = Senior 1st Class, 15 Grand Magic Knight, 16 Wizard King). */
    public static String rank(int step) {
        if (step >= 16) return "Wizard King";
        if (step >= 15) return "Grand Magic Knight";
        String[] ranks = {"Junior", "Intermediate", "Senior"};
        int s = Math.max(0, step), cls = 5 - (s % 5);
        return ranks[Math.min(2, s / 5)] + " " + cls + (cls == 1 ? "st" : cls == 2 ? "nd" : cls == 3 ? "rd" : "th") + " Class";
    }
}
