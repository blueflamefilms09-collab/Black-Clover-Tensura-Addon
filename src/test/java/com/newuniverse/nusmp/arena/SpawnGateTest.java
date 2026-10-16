package com.newuniverse.nusmp.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The table of the spawn gate: dimension, tag, config, disk. */
class SpawnGateTest {
    private static SpawnGate.Verdict v(boolean family, boolean only, boolean disk, boolean arena, boolean tag) {
        return SpawnGate.decide(family, only, disk, arena, tag);
    }

    @Test
    void luciusOutsideTheArenaWithTheSwitchOnIsDenied() {
        assertEquals(SpawnGate.Verdict.DENY, v(true, true, false, false, false));
    }

    @Test
    void insideTheArenaHeIsAllowedWhoeverMadeHim() {
        assertEquals(SpawnGate.Verdict.ALLOW, v(true, true, false, true, false), "an operator's summon in the arena");
        assertEquals(SpawnGate.Verdict.ALLOW, v(true, true, false, true, true));
    }

    @Test
    void theDirectorsTagLetsHimJoin() {
        assertEquals(SpawnGate.Verdict.ALLOW, v(true, true, false, false, true));
    }

    @Test
    void withTheSwitchOffEverythingIsAllowedAsBefore() {
        for (boolean arena : new boolean[]{false, true})
            for (boolean tag : new boolean[]{false, true})
                for (boolean disk : new boolean[]{false, true})
                    assertEquals(SpawnGate.Verdict.ALLOW, v(true, false, disk, arena, tag));
    }

    @Test
    void aLuciusAlreadyInTheWorldIsNotDeletedByTheUpdate() {
        assertEquals(SpawnGate.Verdict.ALLOW, v(true, true, true, false, false));
    }

    @Test
    void otherEntitiesAreNotTouched() {
        for (boolean only : new boolean[]{false, true})
            assertEquals(SpawnGate.Verdict.ALLOW, v(false, only, false, false, false));
    }

    @Test
    void theFullTableHasExactlyOneDenyingRow() {
        int deny = 0;
        for (int bits = 0; bits < 32; bits++) {
            boolean family = (bits & 1) != 0, only = (bits & 2) != 0, disk = (bits & 4) != 0, arena = (bits & 8) != 0, tag = (bits & 16) != 0;
            if (v(family, only, disk, arena, tag) == SpawnGate.Verdict.DENY) {
                deny++;
                assertTrue(family && only && !disk && !arena && !tag);
            }
        }
        assertEquals(1, deny);
    }

    @Test
    void heCanLeaveOnlyForTheArena() {
        assertFalse(SpawnGate.mayLeave(true, true, false));
        assertTrue(SpawnGate.mayLeave(true, true, true));
        assertTrue(SpawnGate.mayLeave(true, false, false), "switch off: as before");
        assertTrue(SpawnGate.mayLeave(false, true, false), "not him");
    }

    @Test
    void naturalSpawnsAreRefusedInTheArenaOnly() {
        assertTrue(SpawnGate.refusesNatural(true, true));
        assertFalse(SpawnGate.refusesNatural(true, false));
        assertFalse(SpawnGate.refusesNatural(false, true));
    }
}
