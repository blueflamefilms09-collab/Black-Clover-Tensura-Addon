package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: a skill that has been through a cycle of resets (Ouroboros' Cycle, or any skill / class reset the player did) has been refined by them. The count is owned by this subsystem
 * ({@code ResetLedger}, which also sets {@code EliteHooks.resetCount} so other systems can read it). Params: {@code count}.
 */
public final class ResetCount implements EvolutionTrigger {
    @Override public String id() { return "reset_count"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) { return ctx.resets() >= p.integer("count", 1); }

    @Override
    public String describe(Params p, String skillId) { return "reset a skill or class " + p.integer("count", 1) + " times"; }
}
