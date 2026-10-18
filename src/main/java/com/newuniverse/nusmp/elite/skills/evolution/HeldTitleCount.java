package com.newuniverse.nusmp.elite.skills.evolution;

/** Why: a collector's gate: the player must hold at least {@code count} titles ({@code EliteHooks.heldTitleCount}). */
public final class HeldTitleCount implements EvolutionTrigger {
    @Override public String id() { return "held_title_count"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) { return ctx.heldTitles() >= p.integer("count", 1); }

    @Override
    public String describe(Params p, String skillId) { return "hold " + p.integer("count", 1) + " titles"; }
}
