package com.newuniverse.nusmp.multiverse;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KnightSystemRulesTest {
    @Test
    void theNineSquadsHaveUniqueIdsAndAreFoundById() {
        assertEquals(9, SquadStyle.values().length);
        Set<String> ids = new HashSet<>();
        for (SquadStyle s : SquadStyle.values()) {
            assertTrue(ids.add(s.id()));
            assertSame(s, SquadStyle.byId(s.id()));
            assertSame(s, SquadStyle.byId(s.display));
            assertFalse(s.captain.isEmpty());
        }
        assertNull(SquadStyle.byId("nope"));
        assertNull(SquadStyle.byId(null));
        assertEquals("William Vangeance", SquadStyle.GOLDEN_DAWN.captain);
    }

    @Test
    void offersAreThreeDistinctKindsThatChangeWithTheDay() {
        Set<MissionBoardRules.Kind> days = new HashSet<>();
        for (int d = 0; d < 40; d++) {
            var k = MissionBoardRules.offerKinds(MissionBoardRules.seed(1234L, 5678L, d));
            assertEquals(3, k.length);
            assertEquals(3, new HashSet<>(java.util.Arrays.asList(k)).size());
            days.add(k[0]);
        }
        assertTrue(days.size() > 1);
        assertEquals(MissionBoardRules.seed(1L, 2L, 3), MissionBoardRules.seed(1L, 2L, 3));
        assertEquals(0, MissionBoardRules.day(-5L));
        assertEquals(2, MissionBoardRules.day(48000L));
    }

    @Test
    void needAndRewardGrowWithRankAndStayBounded() {
        for (var k : MissionBoardRules.Kind.values()) {
            assertTrue(MissionBoardRules.need(k, 14) > MissionBoardRules.need(k, 0));
            assertTrue(MissionBoardRules.need(k, 999) <= MissionBoardRules.MAX_NEED);
            int n = MissionBoardRules.need(k, 5);
            int r = MissionBoardRules.reward(k, n, 100);
            assertTrue(r >= 1 && r <= MissionBoardRules.MAX_REWARD);
            assertTrue(MissionBoardRules.reward(k, 100000, 1000) == MissionBoardRules.MAX_REWARD);
            assertEquals(1, MissionBoardRules.reward(k, 1, 0));
        }
        assertTrue(MissionBoardRules.open(5, 6) && !MissionBoardRules.open(6, 6));
    }

    @Test
    void starAwardsComeEveryNDaysAndPayTheTopThree() {
        assertEquals(0, StarAwardRules.period(0, 28));
        assertEquals(1, StarAwardRules.period(24000L * 28, 28));
        assertEquals(StarAwardRules.period(24000L * 3650, 99999), StarAwardRules.period(24000L * 3650, StarAwardRules.MAX_DAYS));
        assertEquals(5, StarAwardRules.reward(1, 5, 3, 1));
        assertEquals(3, StarAwardRules.reward(2, 5, 3, 1));
        assertEquals(1, StarAwardRules.reward(3, 5, 3, 1));
        assertEquals(0, StarAwardRules.reward(4, 5, 3, 1));
        assertEquals(0, StarAwardRules.reward(1, -5, 3, 1));
        assertFalse(StarAwardRules.due(-1, 5));
        assertTrue(StarAwardRules.due(2, 3));
        assertFalse(StarAwardRules.due(3, 3));
    }
}
