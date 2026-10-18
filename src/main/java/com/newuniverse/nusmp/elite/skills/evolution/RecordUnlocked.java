package com.newuniverse.nusmp.elite.skills.evolution;

/** Why: an evolution can wait for a deed that the records subsystem tracks ("defeated a calamity", "forged a mythical item"): {@code EliteHooks.recordUnlocked(player, record)}. Params: {@code record}. */
public final class RecordUnlocked implements EvolutionTrigger {
    @Override public String id() { return "record_unlocked"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        String r = p.str("record", "");
        return !r.isEmpty() && ctx.record(r);
    }

    @Override
    public String describe(Params p, String skillId) { return "unlock the record " + p.str("record", "?"); }
}
