package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.devil.DevilTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the contract rules of phase 2 (docs/devil_contract_phase2.md) are decisions of the server that must be testable without one, so every formula is a pure function here (no Minecraft type, unit tested by
 * {@code Phase2RulesTest}): the summon chance, the betrayal roll, the EP requirement of a rank and of the Nth strong devil, the three contract types and what each needs, the stacking verdict, the shared share
 * of power, the cap of contracts, the sever-by-kill rule, the sabotage debuff and the friendly-fire counter. The numbers that are data (EP per rank, chance per rank, which devil fights) are in
 * {@link Phase2Table}; the numbers that are server settings come in as arguments (and are clamped again here).
 */
public final class Phase2Rules {
    private Phase2Rules() {}

    /** The three ways to sign. Stored BY NAME (never by ordinal); new kinds are appended only. {@code share} is the part of the devil's power the player gets, {@code corruption} the factor on the creeping corruption. */
    public enum Kind {
        SUBMISSION(0.25, 0.5), EVEN(0.5, 1.0), DOMINANCE(1.0, 1.0);

        public final double share, corruption;

        Kind(double share, double corruption) { this.share = share; this.corruption = corruption; }

        public String key() { return name().toLowerCase(Locale.ROOT); }

        /** The kind with this name, or null (a damaged save never throws). The command words submit, even and dominate are accepted too. */
        public static Kind byName(String s) {
            if (s == null) return null;
            String w = s.trim().toLowerCase(Locale.ROOT);
            return switch (w) {
                case "submission", "submit" -> SUBMISSION;
                case "even", "equal" -> EVEN;
                case "dominance", "dominate" -> DOMINANCE;
                default -> null;
            };
        }
    }

    /** What the trial fight ended in: nothing yet, the devil is impressed (its health fell under the impress share), or it was beaten. */
    public static final int TRIAL_NONE = 0, TRIAL_IMPRESSED = 1, TRIAL_WON = 2;

    /** Hard bounds: a hand-edited config can not go past them. */
    public static final int MAX_CONTRACTS_CAP = 16, MAX_WEAK_CAP = 16, MAX_STRONG_CAP = 8;
    public static final double MAX_EP = 1.0E15;

    static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ================================================================ the summon
    /** The chance that a finished ritual brings the devil: the rank's base chance, plus {@code relicBonus} when the caller carries something of that devil, never under {@code min} and never over 1. */
    public static double chance(double base, boolean relic, double relicBonus, double min) {
        return clamp(clamp(base, 0, 1) + (relic ? clamp(relicBonus, 0, 1) : 0), clamp(min, 0, 1), 1.0);
    }

    /** One roll: {@code r} is a number in [0, 1). A chance of 1 always succeeds, 0 never. */
    public static boolean roll(double chance, double r) { return chance >= 1.0 || r < chance; }

    // ================================================================ the ego (EP) requirement
    /** The existence points a rank asks for: the devil's own number when it has one, else the rank's, times the server scale. */
    public static double requiredEp(double devilOwn, double tierEp, double scale) {
        double base = devilOwn > 0 ? devilOwn : tierEp;
        return clamp(base * clamp(scale, 0, 1000), 0, MAX_EP);
    }

    /** Does the player's EP meet the need? A need of 0 or less is always met; an unknown (NaN) EP never is. */
    public static boolean epOk(double ep, double need) { return need <= 0 || (!Double.isNaN(ep) && ep >= need); }

    /** What a dominance contract asks: the single requirement times the factor (at least 1). */
    public static double dominanceEp(double need, double factor) { return clamp(need * clamp(factor, 1, 1000), 0, MAX_EP); }

    // ================================================================ stacking
    /** Strong devils (Supreme) are stacked under the strict rule; every weaker one stacks freely up to a bound. */
    public static boolean strong(DevilTier t) { return t == DevilTier.SUPREME; }

    /** The factor on the single requirement for the next strong devil when {@code strongHeld} are held: 1 for the first, {@code first} for the second, then rising to {@code max} (5, 7.5, 10 by default). */
    public static double strongFactor(int strongHeld, double first, double max) {
        if (strongHeld <= 0) return 1.0;
        double f = clamp(first, 1, 1000), m = Math.max(f, clamp(max, 1, 1000)), step = (m - f) / 2.0;
        return Math.min(m, f + (strongHeld - 1) * step);
    }

    /** The answer of the stacking rule: {@code ok}, why not ({@code why} is a lang key tail), and the EP it needs. */
    public record Stack(boolean ok, String why, double needEp) {}

    /**
     * May this player take one more devil? {@code enabled} false = the old rule: one contract only. A weaker devil needs only its own requirement and a free place (at most {@code weakMax}); a strong one needs
     * {@code strongFactor} times its requirement (the second at {@code first}, rising to {@code max}) and a free place under {@code strongMax} (the Supreme cap).
     */
    public static Stack stack(boolean enabled, boolean holdsAny, int weakHeld, int strongHeld, boolean candidateStrong, double baseEp, double ep, int weakMax, int strongMax, double first, double max) {
        if (!enabled) return holdsAny ? new Stack(false, "one_only", 0) : new Stack(epOk(ep, baseEp), "ep", baseEp);
        if (candidateStrong) {
            if (strongHeld >= clamp(strongMax, 0, MAX_STRONG_CAP)) return new Stack(false, "strong_cap", 0);
            double need = clamp(baseEp * strongFactor(strongHeld, first, max), 0, MAX_EP);
            return epOk(ep, need) ? new Stack(true, "", need) : new Stack(false, strongHeld > 0 ? "ep_stack" : "ep", need);
        }
        if (weakHeld >= clamp(weakMax, 0, MAX_WEAK_CAP)) return new Stack(false, "weak_cap", 0);
        return epOk(ep, baseEp) ? new Stack(true, "", baseEp) : new Stack(false, "ep", baseEp);
    }

    // ================================================================ one of one and the shared share
    /** A devil is one-of-one when sharing is off (the phase 1 rule: every devil) or its tier is Supreme; every weaker devil is shared. An unranked tier counts as one-of-one (the safe reading). */
    public static boolean oneOfOne(boolean sharedOn, DevilTier tier) { return !sharedOn || tier == null || tier == DevilTier.SUPREME || tier == DevilTier.NONE; }

    public static int contractCap(int setting) { return clamp(setting, 1, MAX_CONTRACTS_CAP); }

    /** May one more player sign with a shared devil that has {@code count} contracts (cap {@code cap})? */
    public static boolean canJoin(int count, int cap) { return count < contractCap(cap); }

    /** The even split: {@code holders} contracts share the devil, each gets 1/holders. */
    public static double split(int holders) { return 1.0 / Math.max(1, holders); }

    /** The strength of one holder: the kind's share of the devil, times the split, times the sabotage factor; always in [0, 1]. */
    public static double power(Kind kind, int holders, double sabotageFactor) {
        return clamp(kind.share * split(holders) * clamp(sabotageFactor, 0, 1), 0, 1);
    }

    /** The share in whole percent, for the screen. */
    public static int percent(double power) { return (int) Math.round(clamp(power, 0, 1) * 100.0); }

    // ================================================================ the types
    /** What the trial gave: for a devil that fights, the player must have impressed it (even, submission) or beaten it (dominance); a devil that does not fight asks nothing, and gives no dominance. */
    public static boolean kindAllowed(Kind k, boolean epOk, boolean dominanceEpOk, boolean devilFights, boolean trialOn, int trial) {
        if (!epOk) return false;
        boolean fought = !devilFights || !trialOn || trial >= TRIAL_IMPRESSED;
        return switch (k) {
            case SUBMISSION, EVEN -> fought;
            case DOMINANCE -> dominanceEpOk && devilFights && trialOn && trial >= TRIAL_WON;
        };
    }

    /** The first reason a kind is not offered ("" when it is): a lang key tail. */
    public static String kindWhy(Kind k, boolean epOk, boolean dominanceEpOk, boolean devilFights, boolean trialOn, int trial) {
        if (kindAllowed(k, epOk, dominanceEpOk, devilFights, trialOn, trial)) return "";
        if (!epOk) return "ep";
        if (k == Kind.DOMINANCE) return !devilFights || !trialOn ? "no_trial" : trial < TRIAL_WON ? "not_won" : "ep_dominance";
        return "no_trial_yet";
    }

    /** The result of a blow in a trial: killed is a win; health at or under {@code impressShare} impresses; else nothing. Never goes back down once reached ({@code before}). */
    public static int trialResult(int before, double healthShare, boolean killed, double impressShare) {
        int now = killed ? TRIAL_WON : healthShare <= clamp(impressShare, 0.05, 0.95) ? TRIAL_IMPRESSED : TRIAL_NONE;
        return Math.max(before, now);
    }

    // ================================================================ sever by killing
    /** The devils this killer and this victim both hold that are shared (not one-of-one): the contracts the kill severs. Empty when the rule is off or the two are the same man. */
    public static List<String> severed(boolean on, boolean sameMan, List<String> killerHolds, List<String> victimHolds, java.util.function.Predicate<String> shared) {
        List<String> out = new ArrayList<>();
        if (!on || sameMan || killerHolds == null || victimHolds == null) return out;
        for (String d : victimHolds) if (killerHolds.contains(d) && shared.test(d) && !out.contains(d)) out.add(d);
        return out;
    }

    // ================================================================ sabotage
    public static long debuffUntil(long now, int seconds) { return now + clamp(seconds, 1, 3600) * 20L; }

    public static boolean debuffActive(long until, long now) { return until > now; }

    /** The factor on the devil's power while the debuff lasts. */
    public static double sabotageFactor(boolean active, double debuffPower) { return active ? clamp(debuffPower, 0, 1) : 1.0; }

    /** The friendly-fire counter: {@code hits} blows on a fellow holder since {@code start}; a window that ran out starts again. {@code trip} true = the threshold was reached (the counter then resets). */
    public record Fire(int hits, long start, boolean trip) {}

    public static Fire friendlyFire(int hits, long start, long now, int windowSeconds, int threshold) {
        if (threshold <= 0) return new Fire(0, 0, false);
        long window = clamp(windowSeconds, 1, 600) * 20L;
        boolean fresh = start <= 0 || now - start > window;
        int h = fresh ? 1 : hits + 1;
        long s = fresh ? now : start;
        return h >= clamp(threshold, 1, 100) ? new Fire(0, 0, true) : new Fire(h, s, false);
    }
}
