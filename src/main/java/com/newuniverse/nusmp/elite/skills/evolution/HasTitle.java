package com.newuniverse.nusmp.elite.skills.evolution;

/** Why: some evolutions only need the title to have been EARNED, not worn: {@code EliteHooks.hasTitle(player, title)}. Params: {@code title}. A missing title id never passes. */
public final class HasTitle implements EvolutionTrigger {
    @Override public String id() { return "has_title"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        String t = p.str("title", "");
        return !t.isEmpty() && ctx.hasTitle(t);
    }

    @Override
    public String describe(Params p, String skillId) { return "earn the title " + p.str("title", "?"); }
}
