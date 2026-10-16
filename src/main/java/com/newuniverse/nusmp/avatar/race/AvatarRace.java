package com.newuniverse.nusmp.avatar.race;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.dnd.Stat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a race is data, not code (owner section 7): a datapack file under {@code data/nusmp/avatar_races/} (the owner wrote {@code multiverse_mod}, the real
 * mod id is {@code nusmp}) describes how big the Avatar is, what its scores are multiplied by, what the creator must have to pick it, and what it evolves into
 * when named. This record is the parsed, immutable form; {@link AvatarRaceParser} builds it and {@link AvatarRaces} holds the loaded set. Pure (no Minecraft).
 *
 * @param id                    the file name without extension ("demon_host")
 * @param baseAttributes        extra attribute values added on top of the score-derived ones, by attribute id ("minecraft:generic.max_health" -> 4.0)
 * @param statMultipliers       per D&D score multiplier (missing = 1.0)
 * @param baseMagiculeCost      magicules the creator pays when the Avatar is created (0 = free)
 * @param baseEp                the EP the race starts from before the scores and the naming surge
 * @param evolvesTo             id of the race naming turns the Avatar into, "" for none
 * @param requiresDndRoll       true: the six scores come from the creator's server-side roll; false: the race has fixed scores (12 each)
 * @param selectable            false: the race can only be reached by evolution (it is not offered in the creation menu)
 * @param connectionConditions  tags the creator must satisfy (see {@link RaceConnectionChecker}); all of them
 * @param allowedPersonalities  empty = every personality is allowed
 * @param appearanceHints       default look values by AppearanceData key ("skin_tone" -> 0xB04040, "horn_id" -> 2)
 */
public record AvatarRace(String id, String displayName, String description, float defaultScale, Map<String, Double> baseAttributes,
                         Map<String, Double> statMultipliers, double baseMagiculeCost, double baseEp, String evolvesTo, boolean requiresDndRoll,
                         boolean selectable, List<String> connectionConditions, List<AvatarPersonality> allowedPersonalities,
                         Map<String, Integer> appearanceHints) {

    /** The multipliers keyed by {@link Stat} (the datapack keys are the lower-case short names: "str", "dex", ...). */
    public Map<Stat, Double> multipliers() {
        Map<Stat, Double> m = new EnumMap<>(Stat.class);
        for (Map.Entry<String, Double> e : statMultipliers.entrySet()) {
            Stat s = Stat.byName(e.getKey());
            if (s != null) m.put(s, e.getValue());
        }
        return m;
    }

    /** Alias used by the GUI payload code. */
    public String evolvesToRaceId() { return evolvesTo == null ? "" : evolvesTo; }

    public boolean allows(AvatarPersonality p) { return p == null || allowedPersonalities.isEmpty() || allowedPersonalities.contains(p); }

    public boolean evolves() { return evolvesTo != null && !evolvesTo.isEmpty(); }
}
