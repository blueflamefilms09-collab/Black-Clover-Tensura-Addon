package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.guard.SectorDoorRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the sector Gatekeeper must open to the Mana Core of its own sector and to nothing else (not a blank core, not another sector's core, not a door that
 * belongs to no sector, not a sector that is still being built), because that one decision is what makes the dungeon grow; this test walks the outcomes of
 * {@link SectorDoorRules} with no game running.
 */
class SectorDoorRulesTest {
    @Test
    void theCoreOfThisSectorOpensItsDoor() {
        assertEquals(Verdict.OPEN, SectorDoorRules.check(3, 3, true));
        assertEquals(Verdict.OPEN, SectorDoorRules.check(0, 0, true));
    }

    @Test
    void aCoreOfAnotherSectorOpensNothing() {
        assertEquals(Verdict.WRONG_SECTOR, SectorDoorRules.check(2, 3, true));
        assertEquals(Verdict.WRONG_SECTOR, SectorDoorRules.check(4, 3, true));
        assertEquals(Verdict.WRONG_SECTOR, SectorDoorRules.check(0, 1, true), "sector 0's core is not sector 1's key");
    }

    @Test
    void aBlankCoreIsDormant() {
        assertEquals(Verdict.DORMANT, SectorDoorRules.check(-1, 3, true));
        assertEquals(Verdict.DORMANT, SectorDoorRules.check(-1, -1, true), "dormant is said first");
        assertEquals(Verdict.DORMANT, SectorDoorRules.check(-5, 0, false));
    }

    @Test
    void aDoorOutsideEverySectorHasNothingToUnseal() {
        assertEquals(Verdict.NOT_HERE, SectorDoorRules.check(3, -1, true));
        assertEquals(Verdict.NOT_HERE, SectorDoorRules.check(0, -1, true));
    }

    @Test
    void aSectorThatIsStillBeingBuiltCannotBeOpened() {
        assertEquals(Verdict.NOT_READY, SectorDoorRules.check(3, 3, false));
        assertEquals(Verdict.WRONG_SECTOR, SectorDoorRules.check(2, 3, false), "the wrong core is refused before the state matters");
    }
}
