package com.newuniverse.nusmp.boss.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure engage rule (docs/boss_ai_engage.md): who a boss fights, who it ignores, when it looks again. */
class BossEngageTest {
    private static BossEngage.Candidate player(int id, double dist) { return new BossEngage.Candidate(id, dist * dist, true, false, false, false); }

    @Test
    void takesTheNearestPlayerWithoutLineOfSightWhenClose() {
        List<BossEngage.Candidate> cs = List.of(player(1, 20), player(2, 9), player(3, 5.5));
        assertEquals(3, BossEngage.choose(cs, 32, true, id -> false, id -> false));
    }

    @Test
    void ignoresCreativeSpectatorAndDeadPlayers() {
        List<BossEngage.Candidate> cs = List.of(
                new BossEngage.Candidate(1, 4, true, false, true, true),
                new BossEngage.Candidate(2, 9, true, true, false, true),
                new BossEngage.Candidate(3, 16, false, false, false, true));
        assertEquals(-1, BossEngage.choose(cs, 32, true, id -> true, id -> true));
        assertFalse(BossEngage.fair(cs.get(0), 32));
        assertFalse(BossEngage.fair(cs.get(1), 32));
        assertFalse(BossEngage.fair(cs.get(2), 32));
    }

    @Test
    void rangeIsTheLimit() {
        List<BossEngage.Candidate> cs = List.of(player(1, 33));
        assertEquals(-1, BossEngage.choose(cs, 32, true, id -> true, id -> true));
        assertEquals(1, BossEngage.choose(cs, 40, true, id -> true, id -> true));
        assertEquals(8, BossEngage.clampRange(1));
        assertEquals(96, BossEngage.clampRange(500));
        assertEquals(32, BossEngage.clampRange(BossEngage.DEFAULT_RANGE));
    }

    @Test
    void farPlayerNeedsSightOrAPath() {
        List<BossEngage.Candidate> cs = List.of(player(7, 20));
        assertEquals(-1, BossEngage.choose(cs, 32, true, id -> false, id -> false));
        assertEquals(7, BossEngage.choose(cs, 32, true, id -> true, id -> false));
        assertEquals(7, BossEngage.choose(cs, 32, true, id -> false, id -> true));
        assertEquals(-1, BossEngage.choose(cs, 32, false, id -> false, id -> true), "no path question while it is on its retry delay");
    }

    @Test
    void reachCountsEvenAtTheEdgeOfTheSeeFreeRange() {
        BossEngage.Candidate inReach = new BossEngage.Candidate(9, 13 * 13, true, false, false, true);
        assertEquals(9, BossEngage.choose(List.of(inReach), 32, true, id -> false, id -> false));
        assertEquals(5, BossEngage.choose(List.of(player(5, BossEngage.SEE_FREE_RANGE)), 32, true, id -> false, id -> false));
    }

    @Test
    void theNearestVisibleBeatsANearerHiddenOne() {
        List<BossEngage.Candidate> cs = List.of(player(1, 15), player(2, 25));
        assertEquals(2, BossEngage.choose(cs, 32, false, id -> id == 2, id -> false));
    }

    @Test
    void askedAtMostOnceAboutAPathAndAtMostTenPlayersAreRead() {
        List<BossEngage.Candidate> cs = new ArrayList<>();
        for (int i = 0; i < 40; i++) cs.add(player(i, 20 + i * 0.1));
        int[] sees = {0}, paths = {0};
        assertEquals(-1, BossEngage.choose(cs, 90, true, id -> { sees[0]++; return false; }, id -> { paths[0]++; return false; }));
        assertEquals(BossEngage.MAX_CANDIDATES, sees[0]);
        assertEquals(1, paths[0]);
    }

    @Test
    void scansAreSpacedAndRushStartsAfterQuiet() {
        assertFalse(BossEngage.due(5, 10));
        assertTrue(BossEngage.due(10, 10));
        assertEquals(10, BossEngage.SCAN_INTERVAL);
        assertFalse(BossEngage.rush(100, 40));
        assertTrue(BossEngage.rush(120, 40));
    }
}
