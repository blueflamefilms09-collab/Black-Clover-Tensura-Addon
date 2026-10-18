package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: a skill grows by being used to kill. Params: {@code count}; {@code filter} ("any", "boss", "monster", "undead"); {@code withSkill} (default true): count only the kills made while the evolving
 * skill was owned (the runtime keeps a counter per skill for every skill that has an evolution), or all kills of the player when false.
 */
public final class KillEvolution implements EvolutionTrigger {
    @Override public String id() { return "kill_evolution"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        String filter = p.str("filter", "any");
        int have = p.bool("withSkill", true) ? ctx.killsWith(skillId, filter) : ctx.killsTotal(filter);
        return have >= p.integer("count", 1);
    }

    @Override
    public String describe(Params p, String skillId) {
        String filter = p.str("filter", "any");
        return "defeat " + p.integer("count", 1) + ("any".equals(filter) ? "" : " " + filter) + " enemies" + (p.bool("withSkill", true) ? " with this skill" : "");
    }
}
