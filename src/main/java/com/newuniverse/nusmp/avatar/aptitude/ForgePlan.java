package com.newuniverse.nusmp.avatar.aptitude;

import java.util.List;

/**
 * Why: what the server has agreed to do for a Finalize request, computed from its own data by {@link ForgeValidator}: which aptitudes the Avatar gets (hand
 * picks first, then the rolled one), which skills, and exactly what it costs (tax, magicule paid now, Genesis Cores). The ritual keeps the plan while the
 * birth spectacle plays, and the creation code applies it at the moment the Avatar is born; nothing in it came from a client number.
 *
 * @param aptitudes     the final aptitude ids the Avatar gets (hand picks, then the rolled one), at most the configured maximum
 * @param handPicked    the ids among them that were picked by hand (these are the taxed ones)
 * @param rolledAptitude the free rolled id, "" when none
 * @param unique        the Unique skill id, "" for none
 * @param ultimate      the Ultimate skill id, "" for none
 * @param paidAwakening the creator pays for a guaranteed Instant Skill Awakening
 * @param quote         the bill
 */
public record ForgePlan(List<String> aptitudes, List<String> handPicked, String rolledAptitude, String unique, String ultimate, boolean paidAwakening,
                        ForgeCosts.Quote quote) {
    public ForgePlan {
        aptitudes = List.copyOf(aptitudes);
        handPicked = List.copyOf(handPicked);
    }

    /** A plan that adds nothing (the classic flow, the admin command): no aptitudes, no skills, no cost beyond the Genesis Core. */
    public static ForgePlan empty() {
        return new ForgePlan(List.of(), List.of(), "", "", "", false, new ForgeCosts.Quote(0, 0, 0, 0, 0, 1));
    }

    /** True when the creator bound at least one Unique or Ultimate skill. */
    public boolean hasChosenSkill() { return !unique.isEmpty() || !ultimate.isEmpty(); }

    public double permanentTax() { return quote.permanentTax(); }

    public double upfrontMagicule() { return quote.upfrontMagicule(); }

    public int coresNeeded() { return quote.coresNeeded(); }
}
