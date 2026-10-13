package com.newuniverse.nusmp.dungeon.nexus;

import java.util.List;
import java.util.Locale;

/**
 * Which zone a cell that holds a Nexus gets, as a pure rule: a build that has started fixes the zone for good; before that an admin's
 * forced zone wins; before that the stable hash of the cell ({@link NexusZone#of}). Also the words the admin command takes.
 */
public final class ZoneChoice {
    private ZoneChoice() {}

    /** The words of {@code /multiverse dungeon nexus zone <word>}, in the order of {@link NexusZone} (the two arenas were appended). */
    public static final List<String> WORDS = List.of("monarch", "underworld", "mirage", "golden", "goliath", "charybdis");

    /**
     * @param built    the zone a started build fixed, or null
     * @param override the zone an admin forced for this cell, or null
     */
    public static NexusZone pick(int cellX, int cellZ, NexusZone built, NexusZone override) {
        if (built != null) return built;
        if (override != null) return override;
        return NexusZone.of(cellX, cellZ);
    }

    /** A zone can only be forced for a cell nothing was ever built in. */
    public static boolean canForce(NexusState state) { return state == null || state == NexusState.UNBUILT; }

    /** The command word of a zone. */
    public static String word(NexusZone zone) { return WORDS.get(zone.ordinal()); }

    /** The zone a command word (or a few natural synonyms) names; null if none. */
    public static NexusZone parse(String word) {
        if (word == null) return null;
        switch (word.toLowerCase(Locale.ROOT)) {
            case "monarch", "vault", "monarch_vault" -> { return NexusZone.MONARCH_VAULT; }
            case "underworld", "gate", "underworld_gate" -> { return NexusZone.UNDERWORLD_GATE; }
            case "mirage", "tempest", "tempest_mirage" -> { return NexusZone.TEMPEST_MIRAGE; }
            case "golden", "city", "golden_city" -> { return NexusZone.GOLDEN_CITY; }
            case "goliath", "pit", "goliath_pit" -> { return NexusZone.GOLIATH_PIT; }
            case "charybdis", "basin", "charybdis_basin" -> { return NexusZone.CHARYBDIS_BASIN; }
            default -> { return null; }
        }
    }
}
