package com.newuniverse.nusmp.avatar.aptitude;

import java.util.List;

/**
 * Why: the part of the Finalize packet that is specific to the Soul Forge, already cut and cleaned (ids lower-cased, lists copied), and nothing else: the
 * creator's CHOICES. There is deliberately no price, no tax and no aptitude "result" in it; the free roll is referred to by a token only, because the
 * server holds the rolled aptitude itself. Pure, so the validator and its tests never see a network class.
 *
 * @param aptitudePicks   aptitude ids picked by hand (taxed), in the order the creator chose them
 * @param rollToken       0 = the free roll is not used; otherwise the token of the roll the server made (it must match the server's record)
 * @param unique          the chosen Unique skill id, "" for none
 * @param ultimate        the chosen Ultimate skill id, "" for none
 * @param paidAwakening   the creator pays an extra Genesis Core and a permanent tax for a guaranteed Instant Skill Awakening
 */
public record ForgeChoices(List<String> aptitudePicks, long rollToken, String unique, String ultimate, boolean paidAwakening) {
    public static final ForgeChoices NONE = new ForgeChoices(List.of(), 0L, "", "", false);

    public ForgeChoices {
        List<String> clean = new java.util.ArrayList<>();
        if (aptitudePicks != null) for (String id : aptitudePicks) clean.add(id == null ? "" : id);      // a null entry becomes an id nothing matches
        aptitudePicks = List.copyOf(clean);
        unique = unique == null ? "" : unique;
        ultimate = ultimate == null ? "" : ultimate;
    }
}
