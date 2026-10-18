package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: in Tensura the greatest skills open only after an awakening (a Demon Lord's, a True Hero's). Whether and how the player awakened is not exposed by one stable API, so the engine
 * keeps its own flag per kind in the player's data ({@code AwakeningEvolution.KEY_PREFIX + kind}, set by {@code EliteSkills.awaken} or the awakening system when it exists); this
 * trigger just asks the context. Params: {@code kind} ("demon_lord", "hero", or "any", the default).
 */
public final class AwakeningEvolution implements EvolutionTrigger {
    /** The persistent-data key prefix of the flags (the context reads {@code KEY_PREFIX + kind}). */
    public static final String KEY_PREFIX = "nusmp_elite_awakened_";

    @Override public String id() { return "awakening_evolution"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        return ctx.awakened(p.str("kind", "any"));
    }

    @Override
    public String describe(Params p, String skillId) {
        String kind = p.str("kind", "any");
        return "awaken" + ("any".equals(kind) ? "" : " as " + kind.replace('_', ' '));
    }
}
