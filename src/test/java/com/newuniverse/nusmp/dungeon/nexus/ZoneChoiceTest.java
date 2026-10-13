package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Which zone a core cell gets: a started build, then a forced zone, then the hash. */
class ZoneChoiceTest {
    @Test
    void theHashDecidesWhenNothingElseDoes() {
        for (int cx = -20; cx <= 20; cx++) for (int cz = -20; cz <= 20; cz++) assertSame(NexusZone.of(cx, cz), ZoneChoice.pick(cx, cz, null, null));
    }

    @Test
    void aForcedZoneBeatsTheHash() {
        for (NexusZone forced : NexusZone.values()) {
            for (int cx = -5; cx <= 5; cx++) assertSame(forced, ZoneChoice.pick(cx, 3, null, forced));
        }
    }

    @Test
    void aStartedBuildBeatsBoth() {
        for (NexusZone built : NexusZone.values()) {
            for (NexusZone forced : NexusZone.values()) assertSame(built, ZoneChoice.pick(7, -7, built, forced));
            assertSame(built, ZoneChoice.pick(7, -7, built, null));
        }
    }

    @Test
    void aZoneCanOnlyBeForcedBeforeAnythingIsBuilt() {
        assertTrue(ZoneChoice.canForce(null));
        assertTrue(ZoneChoice.canForce(NexusState.UNBUILT));
        for (NexusState s : NexusState.values()) if (s != NexusState.UNBUILT) assertFalse(ZoneChoice.canForce(s), s.name());
    }

    @Test
    void theCommandWordsNameTheZonesInOrder() {
        assertEquals(NexusZone.values().length, ZoneChoice.WORDS.size());
        for (NexusZone z : NexusZone.values()) {
            assertSame(z, ZoneChoice.parse(ZoneChoice.word(z)), z.name());
            assertSame(z, ZoneChoice.parse(ZoneChoice.WORDS.get(z.ordinal())));
        }
        assertSame(NexusZone.MONARCH_VAULT, ZoneChoice.parse("MONARCH"));
        assertSame(NexusZone.UNDERWORLD_GATE, ZoneChoice.parse("Underworld_Gate"));
        assertSame(NexusZone.TEMPEST_MIRAGE, ZoneChoice.parse("tempest"));
        assertSame(NexusZone.GOLDEN_CITY, ZoneChoice.parse("city"));
        assertNull(ZoneChoice.parse("nonsense"));
        assertNull(ZoneChoice.parse(null));
    }

    @Test
    void theTwoArenasWereAppendedToTheWordsAndNothingBeforeThemMoved() {
        assertEquals(java.util.List.of("monarch", "underworld", "mirage", "golden", "goliath", "charybdis"), ZoneChoice.WORDS);
        assertEquals("goliath", ZoneChoice.word(NexusZone.GOLIATH_PIT));
        assertEquals("charybdis", ZoneChoice.word(NexusZone.CHARYBDIS_BASIN));
        assertSame(NexusZone.GOLIATH_PIT, ZoneChoice.parse("Goliath"));
        assertSame(NexusZone.GOLIATH_PIT, ZoneChoice.parse("pit"));
        assertSame(NexusZone.GOLIATH_PIT, ZoneChoice.parse("goliath_pit"));
        assertSame(NexusZone.CHARYBDIS_BASIN, ZoneChoice.parse("CHARYBDIS"));
        assertSame(NexusZone.CHARYBDIS_BASIN, ZoneChoice.parse("basin"));
        assertSame(NexusZone.CHARYBDIS_BASIN, ZoneChoice.parse("charybdis_basin"));
        for (String old : new String[]{"monarch", "vault", "monarch_vault", "underworld", "gate", "underworld_gate", "mirage", "tempest", "tempest_mirage", "golden", "city", "golden_city"}) {
            assertTrue(ZoneChoice.parse(old).isCore(), old + " still names a core zone");
        }
    }

    @Test
    void aForcedArenaOnACoreCellOrACoreZoneOnAnArenaCellHolds() {
        assertSame(NexusZone.GOLIATH_PIT, ZoneChoice.pick(0, 0, null, NexusZone.GOLIATH_PIT));
        assertSame(NexusZone.MONARCH_VAULT, ZoneChoice.pick(7, 7, null, NexusZone.MONARCH_VAULT));
        assertSame(NexusZone.CHARYBDIS_BASIN, ZoneChoice.pick(7, 7, NexusZone.CHARYBDIS_BASIN, NexusZone.GOLIATH_PIT), "a started build beats a forced zone");
    }

    @Test
    void theStatesKeepTheirSavedNumbers() {
        assertEquals(0, NexusState.UNBUILT.id);
        assertEquals(1, NexusState.PLAN.id);
        assertEquals(2, NexusState.BUILDING.id);
        assertEquals(3, NexusState.DONE.id);
        assertEquals(4, NexusState.COLLAPSED.id);
        for (NexusState s : NexusState.values()) assertSame(s, NexusState.of(s.id));
        assertSame(NexusState.UNBUILT, NexusState.of(77), "an unknown number reads as unbuilt");
        assertFalse(NexusState.UNBUILT.started());
        assertTrue(NexusState.PLAN.started());
        assertTrue(NexusState.DONE.built() && NexusState.COLLAPSED.built());
        assertFalse(NexusState.BUILDING.built());
    }
}
