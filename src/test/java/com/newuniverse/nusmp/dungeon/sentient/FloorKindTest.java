package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The five floor kinds, their names, and which tier or Nexus zone is which floor (the contract of the Sentient Dungeon). */
class FloorKindTest {
    @Test
    void theLabelsAreTheOwnersNames() {
        assertEquals("The Upper Catacombs", FloorKind.CATACOMBS.label());
        assertEquals("The Deep Waterways", FloorKind.WATERWAYS.label());
        assertEquals("The Underworld Gate", FloorKind.UNDERWORLD.label());
        assertEquals("The Monarch's Vault", FloorKind.MONARCH.label());
        assertEquals("The Dungeon", FloorKind.LEGACY.label());
    }

    @Test
    void theOrderIsTheContractsSoOrdinalsSaveSafely() {
        assertArrayEquals(new FloorKind[]{FloorKind.CATACOMBS, FloorKind.WATERWAYS, FloorKind.UNDERWORLD, FloorKind.MONARCH, FloorKind.LEGACY}, FloorKind.values());
    }

    @Test
    void onlyThreeFloorsBirthFromTheirWalls() {
        assertTrue(FloorKind.CATACOMBS.hasRoomBirths());
        assertTrue(FloorKind.WATERWAYS.hasRoomBirths());
        assertTrue(FloorKind.UNDERWORLD.hasRoomBirths());
        assertFalse(FloorKind.MONARCH.hasRoomBirths());
        assertFalse(FloorKind.LEGACY.hasRoomBirths());
    }

    @Test
    void theTiersMapToTheFloors() {
        assertEquals(FloorKind.CATACOMBS, FloorKind.ofTier(DungeonTier.UPPER_CATACOMBS.name()));
        assertEquals(FloorKind.CATACOMBS, FloorKind.ofTier(DungeonTier.VOLCANIC_VENTS.name()), "the Vents have no type of their own in the design");
        assertEquals(FloorKind.WATERWAYS, FloorKind.ofTier(DungeonTier.DEEP_WATERWAYS.name()));
        assertEquals(FloorKind.UNDERWORLD, FloorKind.ofTier(DungeonTier.GOLDEN_CITY.name()), "the plain Variable Core caves");
        for (DungeonTier t : DungeonTier.values()) assertNotNull(FloorKind.ofTier(t.name()));
    }

    @Test
    void theNexusZonesMapToTheFloors() {
        assertEquals(FloorKind.MONARCH, FloorKind.ofZone(NexusZone.MONARCH_VAULT.name()));
        assertEquals(FloorKind.UNDERWORLD, FloorKind.ofZone(NexusZone.UNDERWORLD_GATE.name()));
        assertEquals(FloorKind.LEGACY, FloorKind.ofZone(NexusZone.TEMPEST_MIRAGE.name()));
        assertEquals(FloorKind.LEGACY, FloorKind.ofZone(NexusZone.GOLDEN_CITY.name()));
        assertEquals(FloorKind.LEGACY, FloorKind.ofZone("A_ZONE_OF_A_LATER_RELEASE"));
        for (NexusZone z : NexusZone.values()) assertNotNull(FloorKind.ofZone(z.name()));
    }
}
