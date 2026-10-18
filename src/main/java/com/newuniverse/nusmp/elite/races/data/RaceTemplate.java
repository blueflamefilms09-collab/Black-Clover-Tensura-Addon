package com.newuniverse.nusmp.elite.races.data;

import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: one race of the elite lines as plain, immutable data: its place (line and tier), the stats of Tensura's default-race block, the extra attribute modifiers, the intrinsic
 * and resistance skills, the next race and the rules to get there, the rules to enter it from outside, its traits, and its transformation states. The numbers are DESIGNED in
 * this mod (the original addon's source was not available); they live in {@code data/nusmp/elite/races/*.json} and are read by {@link RaceTemplateParser}.
 *
 * @param stats         Tensura's default-race block, as additive amounts on top of the vanilla base (the same reading the working Tensura add-ons use)
 * @param attributes    extra modifiers the race always carries (never one the stats already set)
 * @param intrinsic     skills the race grants (registry ids)
 * @param resistances   resistance skills the race grants (registry ids)
 * @param natural       the race's signature skill, the one an evolution asks you to master (also in {@code intrinsic})
 * @param evolvesTo     the template id of the next race, or empty at the top of a line
 * @param evolution     the rules to evolve into {@code evolvesTo}
 * @param entry         the rules to become this race from outside the line (the Primordial rite); empty = not enterable that way
 * @param traits        small named passives ({@code zenkai})
 * @param transformations the states this race can enter
 */
public record RaceTemplate(String id, String line, int tier, String difficulty, String alignment, Stats stats, List<Extra> attributes, List<String> intrinsic,
                           List<String> resistances, String natural, String evolvesTo, RaceRuleSet evolution, RaceRuleSet entry, Set<String> traits,
                           Map<TransformState, TransformSpec> transformations) {
    public RaceTemplate {
        attributes = List.copyOf(attributes);
        intrinsic = List.copyOf(intrinsic);
        resistances = List.copyOf(resistances);
        traits = Set.copyOf(traits);
        transformations = Map.copyOf(transformations);
    }

    /** The default-race block: ranges for aura (the ki pool) and magicule, then the additive combat stats. */
    public record Stats(double auraMin, double auraMax, double magiculeMin, double magiculeMax, double size, double maxHealth, double spiritualHealth, double attack,
                        double attackSpeed, double knockbackResistance, double movementSpeed, double swimSpeed) {}

    /** An extra attribute modifier a race always carries. */
    public record Extra(String attribute, double amount, StateModifier.Op op) {}

    public boolean hasTrait(String trait) { return traits.contains(trait); }

    public TransformSpec spec(TransformState s) { return transformations.get(s); }
}
