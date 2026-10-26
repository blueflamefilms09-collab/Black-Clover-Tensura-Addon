package com.newuniverse.nusmp.skilltree.line;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why (3.26.0, the Evolution skills): the whole Evolution feature (the requirement counters, the locked Ultimate Evolution art, the evolved passives, the Debt) changes how the game plays, so it is a SERVER
 * setting, {@code nusmp-evolution-server.toml}, and the owner of a server can switch it off. {@code evolutionSkills} true (the default) is the feature; false restores the old behaviour exactly: the counters
 * stop, the Ultimate art is an ordinary finisher with no evolution or full-mastery gate, the passives never use their evolved numbers, and no Debt is stored (a Debt art is then a plain blow). A plain {@code define}
 * (a list-validated value throws while the server is still loading), read through {@link #enabled} which answers the default while the spec is not loaded yet (a self-test or an event can run before the
 * config does, and a plain read of an unloaded value throws).
 */
public final class EvolutionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue EVOLUTION_SKILLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("evolution");
        EVOLUTION_SKILLS = b.comment("The Evolution skills: requirement counters, the Ultimate Evolution art that stays locked until the requirement is met and the skill is mastered, evolved passives and the Debt mechanic.",
                        "false = counters stop, the Ultimate art needs no evolution (it is an ordinary finisher), passives keep their base numbers, no Debt is stored.")
                .define("evolutionSkills", true);
        b.pop();
        SPEC = b.build();
    }

    private EvolutionConfig() {}

    /** True when the Evolution feature is on (the default while the config is not loaded yet). */
    public static boolean enabled() {
        try {
            return EVOLUTION_SKILLS.get();
        } catch (IllegalStateException notLoaded) {
            return EVOLUTION_SKILLS.getDefault();
        }
    }
}
