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
