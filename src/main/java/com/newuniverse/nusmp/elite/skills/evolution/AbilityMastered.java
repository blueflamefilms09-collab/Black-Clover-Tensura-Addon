package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: the classic gate, the same one the original {@code SkillEvolution} uses: the skill (by default the one that is evolving) must be owned AND mastered. Params: {@code skill}
 * (optional id of another skill that must be mastered).
 */
public final class AbilityMastered implements EvolutionTrigger {
    @Override public String id() { return "ability_mastered"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        return ctx.ownsMastered(p.str("skill", skillId));
    }

    @Override
    public String describe(Params p, String skillId) { return "master " + p.str("skill", skillId); }
}
