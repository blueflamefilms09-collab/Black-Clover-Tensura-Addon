package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the admin command and the CI script both call {@code expand} repeatedly, in every state the dungeon can be in, and the answer must be right each time:
 * create sector 0 when there is none, grow from the newest sector that has a free Way Onward, and otherwise say clearly why not.
 */
class ExpandRuleTest {
    private static ExpandRule.Row row(int id, boolean seal, int next, boolean generating, boolean failed) { return new ExpandRule.Row(id, seal, next, generating, failed); }

    @Test
    void anEmptyDungeonCreatesSectorZero() {
        ExpandRule.Decision d = ExpandRule.decide(List.of());
        assertTrue(d.first());
        assertNull(d.refusal());
    }

    @Test
    void theNewestSectorWithAFreeWayOnwardGrows() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, 1, false, false), row(1, true, 2, false, false), row(2, true, -1, false, false)));
        assertFalse(d.first());
        assertNull(d.refusal());
        assertEquals(2, d.from());
    }

    @Test
    void aSectorThatAlreadyLeadsSomewhereIsSkippedForAnOlderFreeOne() {
        // sector 1 was never followed (it kept its seal) although sector 2 hangs off sector 0 by hand: the newest FREE seal wins
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, 2, false, false), row(1, true, -1, false, false), row(2, true, 3, false, false)));
        assertEquals(1, d.from());
    }

    @Test
    void aSectorStillGeneratingIsReportedNotSkipped() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, false, -1, true, false)));
        assertEquals("sector 0 is still generating", d.refusal());
    }

    @Test
    void aFailedSectorTellsTheAdminHowToRetry() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, 1, false, false), row(1, false, -1, false, true)));
        assertEquals("sector 1 failed to generate, retry it with regen 1", d.refusal());
    }

    @Test
    void aDungeonWhereNoSectorHasAFreeSealIsRefused() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, false, -1, false, false)));
        assertEquals("no sector has a free Way Onward", d.refusal());
    }

    @Test
    void aGeneratingSectorBeatsAFailedOneInTheMessage() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, false, -1, false, true), row(1, false, -1, true, false)));
        assertEquals("sector 1 is still generating", d.refusal());
    }

    @Test
    void asksMadeWhileTheNewestSectorIsBeingBuiltAreRefusedNotQueuedTwice() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, 1, false, false), row(1, true, 2, false, false), row(2, false, -1, true, false)));
        assertEquals("sector 2 is still generating", d.refusal());
    }

    @Test
    void whenSeveralSectorsHaveAFreeSealTheNewestOneGrows() {
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, -1, false, false), row(1, true, -1, false, false)));
        assertEquals(1, d.from());
    }

    @Test
    void aSectorBeingRegeneratedKeepsItsSealDataButIsNotGrownFromUntilItIsFinished() {
        // regen sets the state back to GENERATING while expansionPos is still saved
        ExpandRule.Decision d = ExpandRule.decide(List.of(row(0, true, 1, false, false), row(1, true, -1, true, false)));
        assertEquals("sector 1 is still generating", d.refusal());
    }

    // ---------------------------------------------------------------- 1.55.0: when may the next sector be requested

    @Test
    void aNextSectorMayBeRequestedWhateverTheProgressOfTheBossOfTheSectorIs() {
        // ACTIVE (an admin pushing), GUARDIAN_DEAD (the core exists, the door is sealed), UNSEALED (the door is open, the vault waits) and COMPLETE (the vault is done)
        // are all the same to the rule: a finished sector with a Way Onward and no successor can have one. The facade holds the request back until the vault is
        // resolved (mega.expandAfterVault); the generator must accept it at any of those times.
        ExpandRule.Row finished = row(3, true, -1, false, false);
        assertNull(ExpandRule.refusalForNext(finished, 4, 500, false));
        assertNull(ExpandRule.refusalForNext(finished, 499, 500, false));
    }

    @Test
    void aSecondRequestForTheSameSectorIsRefusedNotBuiltTwice() {
        assertEquals("sector 3 already leads to sector 4", ExpandRule.refusalForNext(row(3, true, 4, false, false), 5, 500, false));
        assertEquals("sector 0 already leads to sector 7", ExpandRule.refusalForNext(row(0, true, 7, false, false), 8, 500, true), "the successor wins over every other reason");
    }

    @Test
    void aSectorThatCannotBeGrownFromSaysWhy() {
        assertEquals("sector 2 is still generating", ExpandRule.refusalForNext(row(2, true, -1, true, false), 3, 500, false));
        assertEquals("sector 2 failed to generate (retry it with /multiverse mega regen 2)", ExpandRule.refusalForNext(row(2, true, -1, false, true), 3, 500, false));
        assertEquals("sector 2 has no Way Onward", ExpandRule.refusalForNext(row(2, false, -1, false, false), 3, 500, false));
        assertEquals("the dungeon reached maxSectors (500)", ExpandRule.refusalForNext(row(2, true, -1, false, false), 500, 500, false));
        assertEquals("the dungeon reached maxSectors (3)", ExpandRule.refusalForNext(row(2, true, -1, false, false), 4, 3, false));
        assertEquals("no sector", ExpandRule.refusalForNext(null, 0, 500, false));
    }

    @Test
    void aRoadThatAlreadyHasASectorInTheNextSlotRefusesAThirdOne() {
        String why = ExpandRule.refusalForNext(row(5, true, -1, false, false), 9, 500, true);
        assertNotNull(why);
        assertTrue(why.contains("already has a sector after it"), why);
    }

    @Test
    void theDecisionForOneRoadOnlyLooksAtThatRoadsRows() {
        // a world whose Holy road has two sectors and whose Abyssal road is not opened: asking about the Abyssal road bootstraps it, asking about the Holy road grows it
        assertTrue(ExpandRule.decide(List.of()).first(), "an unopened road creates its first sector");
        ExpandRule.Decision holy = ExpandRule.decide(List.of(row(0, true, 2, false, false), row(2, true, -1, false, false)));
        assertFalse(holy.first());
        assertEquals(2, holy.from());
    }
}
