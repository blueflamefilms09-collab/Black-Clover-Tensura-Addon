package com.newuniverse.nusmp.labyrinth.mega.foundry;

import java.util.List;

/**
 * Why: a roster row says "archetype: caster" and nothing else about how the mob fights; this record is what that word means, read from {@code _archetypes.json} (data, so the
 * owner can retune a whole family without a rebuild). {@code health, damage, speed, armor} multiply the tier's base numbers ({@link FoundryScaling}); {@code style} picks the
 * movement the AI uses ({@link Style}); {@code near, far, keep} are the ranged band and the distance a shooter backs away from (blocks); {@code abilityEvery} is the ticks between
 * two ability rolls. Pure, immutable.
 */
public record FoundryArchetype(String id, double health, double damage, double speed, double armor, Style style, double near, double far, double keep, int abilityEvery) {

    /** The ways a mob moves in a fight; each has one goal class ({@link FoundryGoals}). */
    public enum Style {
        MELEE, HITRUN, RANGED, SUPPORT, CHARGE, AIR;

        public static Style of(String name) {
            if (name == null) return null;
            for (Style s : values()) if (s.name().equalsIgnoreCase(name)) return s;
            return null;
        }
    }

    /** The ids of the ten archetypes of the contract (the roster guard and the tests insist on all of them). */
    public static final List<String> CONTRACT = List.of("brute", "skirmisher", "archer", "caster", "assassin", "tank", "support", "summoner", "flier", "charger");

    /** A neutral archetype for an id the data does not know (a broken data pack must not stop a mob from existing). */
    public static final FoundryArchetype FALLBACK = new FoundryArchetype("brute", 1.0, 1.0, 1.0, 1.0, Style.MELEE, 0, 0, 0, 30);

    public boolean ranged() { return style == Style.RANGED || style == Style.SUPPORT; }
}
