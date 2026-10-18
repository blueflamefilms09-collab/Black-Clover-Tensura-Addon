package com.newuniverse.nusmp.elite.skills.evolution;

/** Why: the soul must be big enough to carry the new skill. The player's EP (existence points) must reach {@code min} (default 0, so a missing parameter never blocks). */
public final class EPGate implements EvolutionTrigger {
    @Override public String id() { return "ep_gate"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) { return ctx.ep() >= p.dbl("min", 0.0); }

    @Override
    public String describe(Params p, String skillId) { return "reach " + (long) p.dbl("min", 0.0) + " EP"; }
}
