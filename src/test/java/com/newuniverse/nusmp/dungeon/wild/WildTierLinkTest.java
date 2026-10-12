package com.newuniverse.nusmp.dungeon.wild;

import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure wildlife rules must keep agreeing with the classes they stand in for. */
class WildTierLinkTest {
    @Test
    void rosterPlacesAreTheOrdinalsOfTheDungeonTiers() {
        assertEquals(DungeonTier.UPPER_CATACOMBS.ordinal(), Roster.UPPER);
        assertEquals(DungeonTier.DEEP_WATERWAYS.ordinal(), Roster.DEEP);
        assertEquals(DungeonTier.VOLCANIC_VENTS.ordinal(), Roster.VENTS);
        assertEquals(DungeonTier.GOLDEN_CITY.ordinal(), Roster.CITY);
    }

    @Test
    void hungerScaleIsThatOfTheOldCreatures() {
        assertEquals(DungeonCreature.HUNGER_MAX, WildRules.HUNGER_MAX);
        assertEquals(DungeonCreature.HUNGRY, WildRules.HUNGRY);
        assertEquals(DungeonCreature.FULL, WildRules.FULL);
    }
}
