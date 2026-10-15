package com.newuniverse.nusmp.labyrinth.seal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Peak Altar's weighted draw. */
class BossRosterTest {
    private static final List<BossRoster.Entry> ROSTER = List.of(
            new BossRoster.Entry("nusmp:dante_zogratis", 3), new BossRoster.Entry("nusmp:vanica_zogratis", 3),
            new BossRoster.Entry("nusmp:zenon_zogratis", 3), new BossRoster.Entry("nusmp:floor_warden", 2));

    @Test
    void theSameSeedDrawsTheSameBoss() {
        for (long seed : new long[]{0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertEquals(BossRoster.pick(ROSTER, seed, id -> true), BossRoster.pick(ROSTER, seed, id -> true));
        }
    }

    @Test
    void drawsFollowTheWeights() {
        Map<String, Integer> counts = new HashMap<>();
        int n = 20000;
        for (int i = 0; i < n; i++) counts.merge(BossRoster.pick(ROSTER, BossRoster.seed(i * 7919L, i), id -> true), 1, Integer::sum);
        assertEquals(4, counts.size(), "every entry is drawn");
        for (BossRoster.Entry e : ROSTER) {
            double expected = n * e.weight() / 11.0, got = counts.get(e.id());
            assertTrue(Math.abs(got - expected) < expected * 0.12, e.id() + ": expected ~" + expected + " got " + got);
        }
    }

    @Test
    void unknownIdsAndZeroWeightsAreSkipped() {
        List<BossRoster.Entry> roster = List.of(new BossRoster.Entry("othermod:dragon", 100), new BossRoster.Entry("nusmp:floor_warden", 1),
                new BossRoster.Entry("nusmp:dante_zogratis", 0), new BossRoster.Entry(null, 5));
        for (int i = 0; i < 200; i++) {
            assertEquals("nusmp:floor_warden", BossRoster.pick(roster, BossRoster.seed(i, 99), id -> id.startsWith("nusmp:")));
        }
    }

    @Test
    void nothingDrawableFallsBackToTheWarden() {
        assertNull(BossRoster.pick(List.of(), 5L, id -> true));
        assertNull(BossRoster.pick(List.of(new BossRoster.Entry("othermod:dragon", 9)), 5L, id -> false));
        assertNull(BossRoster.pick(List.of(new BossRoster.Entry("nusmp:floor_warden", 0)), 5L, id -> true));
        assertEquals(BossRoster.FALLBACK, BossRoster.pickOrFallback(List.of(), 5L, id -> true));
        assertEquals("nusmp:floor_warden", BossRoster.FALLBACK);
    }

    @Test
    void positionAndTimeBothChangeTheSeed() {
        long a = BossRoster.seed(100L, 10L), b = BossRoster.seed(101L, 10L), c = BossRoster.seed(100L, 11L);
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(b, c);
        assertEquals(a, BossRoster.seed(100L, 10L));
    }
}
