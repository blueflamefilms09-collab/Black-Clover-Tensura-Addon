package com.newuniverse.nusmp.avatar.aptitude;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: this is the anti-cheat core of the Soul Forge. The client sends CHOICES (aptitude ids, a roll token, two skill ids, a yes/no for the paid awakening);
 * this pure function turns them into a {@link ForgePlan} only if every id exists, the slots and kinds are right, the free roll really is the one the server
 * made, nothing is picked twice, and the creator can truly pay: the magicule it costs now, the permanent tax it adds on top of what they already carry,
 * the EP gate of each skill and the Genesis Cores. Every number is read from the server's catalogs, rules and fresh {@link ForgeContext}, so a hacked
 * client has nothing to lie with: too many aptitudes, an unknown id, a rare aptitude claimed as "free", a token from an old roll, a skill in the wrong
 * slot, a pool it cannot afford: each ends in a {@link Refusal} with a plain sentence for the player and a stable {@link Code} for the tests.
 * Checks run in a fixed order (ids, duplicates, roll, count, skills, EP, tax cap, magicule, cores) so the first reason is always the same.
 */
public final class ForgeValidator {
    private ForgeValidator() {}

    public enum Code {
        TOO_MANY_APTITUDES, UNKNOWN_APTITUDE, DUPLICATE_APTITUDE, ROLL_MISMATCH, UNKNOWN_SKILL, WRONG_SKILL_KIND, NOT_ENOUGH_EP, TAX_CAP,
        NOT_ENOUGH_MAGICULE, NEED_CORES
    }

    public record Refusal(Code code, String text) {}

    /** Exactly one of {@code plan} and {@code refusal} is set. */
    public record Verdict(ForgePlan plan, Refusal refusal) {
        public boolean ok() { return plan != null; }

        static Verdict yes(ForgePlan p) { return new Verdict(p, null); }

        static Verdict no(Code c, String text) { return new Verdict(null, new Refusal(c, text)); }
    }

    public static Verdict validate(ForgeChoices in, ForgeContext ctx, ForgeRules rules, Function<String, Aptitude> aptitudes, Function<String, ForgeSkill> skills) {
        // ---- the hand picks: well-formed, known, once each
        List<Aptitude> picked = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : in.aptitudePicks()) {
            String id = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
            Aptitude a = AptitudeLook.validId(id) ? aptitudes.apply(id) : null;
            if (a == null) return Verdict.no(Code.UNKNOWN_APTITUDE, "The Soul Forge does not know the aptitude '" + clip(raw) + "'.");
            if (!seen.add(a.id())) return Verdict.no(Code.DUPLICATE_APTITUDE, a.name() + " can only be chosen once.");
            picked.add(a);
        }

        // ---- the free roll: it must be the one the server made, and it is not also a hand pick
        String rolledId = "";
        if (in.rollToken() != 0L) {
            if (ctx.rolledToken() == 0L || ctx.rolledToken() != in.rollToken() || ctx.rolledAptitude().isEmpty()) {
                return Verdict.no(Code.ROLL_MISMATCH, "That aptitude roll is out of date: roll again.");
            }
            Aptitude r = aptitudes.apply(ctx.rolledAptitude());
            if (r == null) return Verdict.no(Code.UNKNOWN_APTITUDE, "The rolled aptitude no longer exists: roll again.");
            if (!seen.add(r.id())) return Verdict.no(Code.DUPLICATE_APTITUDE, r.name() + " can only be chosen once.");
            rolledId = r.id();
        }

        int count = picked.size() + (rolledId.isEmpty() ? 0 : 1);
        if (count > rules.maxAptitudes()) {
            return Verdict.no(Code.TOO_MANY_APTITUDES, "An Avatar can carry at most " + rules.maxAptitudes() + " aptitude" + (rules.maxAptitudes() == 1 ? "" : "s") + ".");
        }

        // ---- the skills: each in its own slot
        ForgeSkill unique = null, ultimate = null;
        if (!in.unique().isEmpty()) {
            unique = skills.apply(in.unique().toLowerCase(Locale.ROOT));
            if (unique == null) return Verdict.no(Code.UNKNOWN_SKILL, "The Soul Forge does not offer the skill '" + clip(in.unique()) + "'.");
            if (unique.kind() != ForgeSkill.Kind.UNIQUE) return Verdict.no(Code.WRONG_SKILL_KIND, unique.name() + " does not fit the Unique slot.");
        }
        if (!in.ultimate().isEmpty()) {
            ultimate = skills.apply(in.ultimate().toLowerCase(Locale.ROOT));
            if (ultimate == null) return Verdict.no(Code.UNKNOWN_SKILL, "The Soul Forge does not offer the skill '" + clip(in.ultimate()) + "'.");
            if (ultimate.kind() != ForgeSkill.Kind.ULTIMATE) return Verdict.no(Code.WRONG_SKILL_KIND, ultimate.name() + " does not fit the Ultimate slot.");
        }
        if (!ctx.creative()) {
            for (ForgeSkill s : new ForgeSkill[]{unique, ultimate}) {
                if (s != null && ctx.maxEp() + 1.0e-9 < s.minEp()) {
                    return Verdict.no(Code.NOT_ENOUGH_EP, s.name() + " needs at least " + Math.round(s.minEp()) + " EP of you; you have " + Math.round(ctx.maxEp()) + ".");
                }
            }
        }

        // ---- the bill
        ForgeCosts.Quote q = ForgeCosts.quote(picked, unique, ultimate, in.paidAwakening(), rules, ctx.maxMagicule());
        if (q.taxed() && ctx.existingTax() + q.permanentTax() > rules.maxTotalTax() + 1.0e-9) {
            return Verdict.no(Code.TAX_CAP, "That would take " + pct(ctx.existingTax() + q.permanentTax()) + " of your maximum magicule for good; the limit is " + pct(rules.maxTotalTax()) + ".");
        }
        if (!ctx.creative() && ctx.magicule() + 1.0e-6 < ctx.raceMagiculeCost() + q.upfrontMagicule()) {
            return Verdict.no(Code.NOT_ENOUGH_MAGICULE, "You need " + Math.round(ctx.raceMagiculeCost() + q.upfrontMagicule()) + " magicule to bind this; you hold " + Math.round(ctx.magicule()) + ".");
        }
        if (!ctx.creative() && ctx.cores() < q.coresNeeded()) {
            return Verdict.no(Code.NEED_CORES, "You need " + q.coresNeeded() + " Genesis Core" + (q.coresNeeded() == 1 ? "" : "s") + "; you hold " + ctx.cores() + ".");
        }

        List<String> all = new ArrayList<>();
        List<String> hand = new ArrayList<>();
        for (Aptitude a : picked) { all.add(a.id()); hand.add(a.id()); }
        if (!rolledId.isEmpty()) all.add(rolledId);
        return Verdict.yes(new ForgePlan(all, hand, rolledId, unique == null ? "" : unique.id(), ultimate == null ? "" : ultimate.id(), in.paidAwakening(), q));
    }

    private static String pct(double fraction) { return Math.round(fraction * 1000.0) / 10.0 + "%"; }

    private static String clip(String s) {
        if (s == null) return "";
        String t = s.length() > 32 ? s.substring(0, 32) : s;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.length(); i++) { char c = t.charAt(i); if (c >= 32 && c != 127 && c != '§') b.append(c); }
        return b.toString();
    }
}
