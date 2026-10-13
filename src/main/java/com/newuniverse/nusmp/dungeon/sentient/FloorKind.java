package com.newuniverse.nusmp.dungeon.sentient;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The kinds of floor of the sentient Dungeon (docs: the owner's design, section 2): the four floors that have a spawn list and a boss, and
 * everything else. Which floor a place is follows the Dungeon's tiers and, inside a Nexus of the Variable Core, the zone that stands there.
 */
public enum FloorKind {
    /** Type A, the Upper Catacombs (also the Volcanic Vents tier, which the design has no type for). */
    CATACOMBS("The Upper Catacombs"),
    /** Type B, the Deep Waterways. */
    WATERWAYS("The Deep Waterways"),
    /** Type C, the Underworld Gate (the zone, and the plain Variable Core caves between the zones). */
    UNDERWORLD("The Underworld Gate"),
    /** Type D, the Monarch's Vault: no standard mob births at all. */
    MONARCH("The Monarch's Vault"),
    /** The Tempest Mirage, the Golden City and any other Nexus volume: they run their own populations, no room births. */
    LEGACY("The Dungeon");

    private final String label;

    FloorKind(String label) { this.label = label; }

    public String label() { return label; }

    /** Do rooms of this floor birth ordinary mobs from their walls? */
    public boolean hasRoomBirths() { return this == CATACOMBS || this == WATERWAYS || this == UNDERWORLD; }

    /** The floor kind at a position of the Dungeon: inside a Nexus volume by its zone, anywhere else by the tier of the depth. */
    public static FloorKind at(ServerLevel level, BlockPos pos) { return FloorLookup.kindAt(level, pos); }

    /** The floor of a tier of the Dungeon, by the name of its DungeonTier: the Variable Core's caves between the zones are the Underworld, the Vents the Catacombs. */
    public static FloorKind ofTier(String tierName) {
        if ("DEEP_WATERWAYS".equals(tierName)) return WATERWAYS;
        if ("GOLDEN_CITY".equals(tierName)) return UNDERWORLD;
        return CATACOMBS;
    }

    /** The floor of a Nexus zone, by the name of its NexusZone: the Monarch's Vault and the Underworld Gate are floors of their own, every other zone runs its own population. */
    public static FloorKind ofZone(String zoneName) {
        if ("MONARCH_VAULT".equals(zoneName)) return MONARCH;
        if ("UNDERWORLD_GATE".equals(zoneName)) return UNDERWORLD;
        return LEGACY;
    }
}
