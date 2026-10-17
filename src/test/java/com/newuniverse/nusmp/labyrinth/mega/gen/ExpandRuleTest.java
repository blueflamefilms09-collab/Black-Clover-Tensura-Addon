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
}
