package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The order the columns come down in: far side first, the corridors of the exits last. */
class CollapseSweepTest {
    private static long d2(CollapseSweep s, int i, int[] ex, int[] ez) {
        long best = Long.MAX_VALUE;
        for (int k = 0; k < ex.length; k++) {
            long dx = (long) s.x(i) - ex[k], dz = (long) s.z(i) - ez[k];
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }

    @Test
    void everyColumnOfTheFootprintComesOnceAndNothingElse() {
        Footprint fp = Footprint.oval(100, -40, 30, 20);
        CollapseSweep s = CollapseSweep.build(fp, 1, new int[]{128, 70}, new int[]{-40, -40}, 7);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < s.size(); i++) {
            assertTrue(fp.contains(s.x(i), s.z(i), 1), "inside the footprint with its edge");
            assertTrue(seen.add(((long) s.x(i) << 32) | (s.z(i) & 0xFFFFFFFFL)), "once");
        }
        int expected = 0;
        for (int x = 60; x <= 140; x++) for (int z = -70; z <= -10; z++) if (fp.contains(x, z, 1)) expected++;
        assertEquals(expected, s.size());
        assertTrue(s.open() > 0 && s.open() < s.size());
    }

    @Test
    void theFarSideFallsFirstAndTheNearestToAnExitLast() {
        Footprint fp = Footprint.oval(0, 0, 44, 44);
        int[] ex = {40, -40}, ez = {0, 0};
        CollapseSweep s = CollapseSweep.build(fp, 1, ex, ez, 7);
        long last = Long.MAX_VALUE;
        for (int i = 0; i < s.open(); i++) {
            long d = d2(s, i, ex, ez);
            assertTrue(d <= last, "distance to the nearest exit never grows along the order, at " + i);
            assertTrue(d > 49, "swept columns are outside every corridor");
            last = d;
        }
        long lastSpared = Long.MAX_VALUE;
        for (int i = s.open(); i < s.size(); i++) {
            long d = d2(s, i, ex, ez);
            assertTrue(d <= 49, "spared columns are inside a corridor");
            assertTrue(d <= lastSpared);
            assertTrue(s.spared(i));
            lastSpared = d;
        }
        assertFalse(s.spared(0));
        assertTrue(d2(s, 0, ex, ez) >= d2(s, s.open() - 1, ex, ez));
    }

    @Test
    void theSameInputAlwaysGivesTheSameOrder() {
        Footprint fp = Footprint.rect(5, 9, 25, 18);
        CollapseSweep a = CollapseSweep.build(fp, 1, new int[]{30, 5}, new int[]{9, 27}, 7);
        CollapseSweep b = CollapseSweep.build(fp, 1, new int[]{30, 5}, new int[]{9, 27}, 7);
        assertEquals(a.size(), b.size());
        assertEquals(a.open(), b.open());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.x(i), b.x(i));
            assertEquals(a.z(i), b.z(i));
        }
    }

    @Test
    void withoutExitsTheMiddleStandsInAndNothingIsSpared() {
        Footprint fp = Footprint.oval(10, 10, 12, 12);
        CollapseSweep s = CollapseSweep.build(fp, 1, new int[0], new int[0], 7);
        assertEquals(s.size(), s.open());
        assertEquals(s.size(), CollapseSweep.build(fp, 1, null, null, 7).open());
        long d0 = (long) (s.x(0) - 10) * (s.x(0) - 10) + (long) (s.z(0) - 10) * (s.z(0) - 10);
        long dn = (long) (s.x(s.size() - 1) - 10) * (s.x(s.size() - 1) - 10) + (long) (s.z(s.size() - 1) - 10) * (s.z(s.size() - 1) - 10);
        assertTrue(d0 >= dn, "from the rim in to the middle");
        assertEquals(0, dn, "the middle is the last column");
    }

    @Test
    void exitListsOfDifferentLengthsUseTheShorter() {
        Footprint fp = Footprint.oval(0, 0, 10, 10);
        CollapseSweep s = CollapseSweep.build(fp, 0, new int[]{10, -10, 0}, new int[]{0}, 3);
        assertTrue(s.open() < s.size());
        assertTrue(s.size() > 0);
    }

    @Test
    void theIndexLookupMatchesTheOrder() {
        Footprint fp = Footprint.oval(-30, 17, 14, 9);
        CollapseSweep s = CollapseSweep.build(fp, 1, new int[]{-16}, new int[]{17}, 7);
        for (int i = 0; i < s.size(); i++) assertEquals(i, s.indexOf(s.x(i), s.z(i)));
        assertEquals(-1, s.indexOf(0, 0));
        assertEquals(-1, s.indexOf(-30 - 16, 17));
        assertEquals(-1, s.indexOf(Integer.MIN_VALUE, Integer.MAX_VALUE));
        int none = 0;
        for (int x = -50; x <= -10; x++) for (int z = 0; z <= 35; z++) if (s.indexOf(x, z) < 0) none++;
        assertEquals(41 * 36 - s.size(), none, "everything not in the order is not found");
    }

    @Test
    void aBigRectangleIsStillCheap() {
        Footprint fp = Footprint.rect(0, 0, 56, 56);
        long t0 = System.nanoTime();
        CollapseSweep s = CollapseSweep.build(fp, 1, new int[]{56, -56, 0, 0}, new int[]{0, 0, 56, -56}, 7);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(115 * 115, s.size());
        assertTrue(ms < 2000, "built in " + ms + " ms");
    }
}
