package com.newuniverse.nusmp.elite.skills.evolution;

/** Why: a smith's skill evolves with a smith's work. The number of items the player has forged comes from the forge subsystem ({@code EliteHooks.forgeCraftedCount}); params: {@code count}. */
public final class ForgeCraftedCount implements EvolutionTrigger {
    @Override public String id() { return "forge_crafted_count"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) { return ctx.forgeCrafted() >= p.integer("count", 1); }

    @Override
    public String describe(Params p, String skillId) { return "forge " + p.integer("count", 1) + " items"; }
}
