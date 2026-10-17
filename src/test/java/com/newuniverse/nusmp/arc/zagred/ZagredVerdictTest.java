package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the verdict is the multi-target mechanic, so how many players are named, how long they have and which words are dealt is pinned here. */
class ZagredVerdictTest {
    @Test
    void theNumberMarkedGrowsWithTheActAndNeverPassesTheFighters() {
        assertEquals(0, ZagredVerdict.targets(0, 3));
        assertEquals(1, ZagredVerdict.targets(1, 2));
        assertEquals(1, ZagredVerdict.targets(1, 4));
        assertEquals(2, ZagredVerdict.targets(2, 2));
        assertEquals(2, ZagredVerdict.targets(5, 2));
        assertEquals(3, ZagredVerdict.targets(5, 3));
        assertEquals(3, ZagredVerdict.targets(9, 4));
        assertEquals(2, ZagredVerdict.targets(2, 4), "two fighters: both can be marked");
    }

    @Test
    void theWindowShrinksButStaysFairlyLong() {
        assertEquals(70, ZagredVerdict.windowTicks(2));
        assertEquals(60, ZagredVerdict.windowTicks(3));
        assertEquals(50, ZagredVerdict.windowTicks(4));
        for (int s = 2; s <= 4; s++) assertTrue(ZagredVerdict.windowTicks(s) >= 40, "never under two seconds");
    }

    @Test
    void wordsAreDistinctAndTheSameSeedDealsTheSameWords() {
        for (long seed = 0; seed < 50; seed++) {
            List<ZagredWord> a = ZagredVerdict.assign(3, seed);
            assertEquals(3, a.size());
            Set<ZagredWord> set = new HashSet<>(a);
            assertEquals(3, set.size(), "no two players are told the same word");
            assertTrue(ZagredVerdict.POOL.containsAll(a));
            assertEquals(a, ZagredVerdict.assign(3, seed));
        }
    }

    @Test
    void differentSeedsDealDifferentHands() {
        Set<List<ZagredWord>> hands = new HashSet<>();
        for (long seed = 0; seed < 60; seed++) hands.add(ZagredVerdict.assign(2, seed));
        assertTrue(hands.size() >= 6, "the draw really varies: " + hands.size());
    }

    @Test
    void theCountIsClamped() {
        assertEquals(0, ZagredVerdict.assign(-3, 1).size());
        assertEquals(ZagredVerdict.POOL.size(), ZagredVerdict.assign(99, 1).size());
        assertEquals(5, ZagredVerdict.POOL.size());
    }
}
