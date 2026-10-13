package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The ground a Nexus covers: inside tests, margins, and the chunks it touches. */
class FootprintTest {
    @Test
    void anOvalReachesItsHalfWidthsAndNotItsCorners() {
        Footprint f = Footprint.oval(100, -200, 40, 24);
        assertTrue(f.contains(100, -200));
        assertTrue(f.contains(140, -200) && f.contains(60, -200), "the ends of the long axis");
        assertFalse(f.contains(141, -200) || f.contains(59, -200));
        assertTrue(f.contains(100, -176) && f.contains(100, -224), "the ends of the short axis");
        assertFalse(f.contains(100, -175) || f.contains(100, -225));
        assertFalse(f.contains(140, -176), "a corner of the bounding box is outside the oval");
        assertEquals(60, f.minX());
        assertEquals(140, f.maxX());
        assertEquals(-224, f.minZ());
        assertEquals(-176, f.maxZ());
    }

    @Test
    void aRectangleHasItsCorners() {
        Footprint f = Footprint.rect(0, 0, 10, 6);
        assertTrue(f.contains(10, 6) && f.contains(-10, -6));
        assertFalse(f.contains(11, 0) || f.contains(0, 7));
    }

    @Test
    void aMarginGrowsAndShrinksTheEdge() {
        for (Footprint f : new Footprint[]{Footprint.oval(5, 5, 30, 20), Footprint.rect(5, 5, 30, 20)}) {
            assertFalse(f.contains(5 + 31, 5));
            assertTrue(f.contains(5 + 31, 5, 1));
            assertTrue(f.contains(5 + 30, 5));
            assertFalse(f.contains(5 + 30, 5, -1));
            assertFalse(f.contains(5, 5, -21), "shrunk past nothing");
        }
    }

    @Test
    void extentIsTheLargestColumnOfTheRow() {
        for (int[] ab : new int[][]{{36, 48}, {48, 36}, {20, 20}, {7, 3}}) {
            int a = ab[0], b = ab[1];
            for (int v = -b - 2; v <= b + 2; v++) {
                int best = -1;
                for (int u = 0; u <= a + 2; u++) if (Footprint.inside(u, v, a, b)) best = u;
                assertEquals(best, Footprint.extent(v, a, b), "a=" + a + " b=" + b + " v=" + v);
            }
        }
    }

    @Test
    void chunksAreExactlyTheChunksWithAColumnInside() {
        for (Footprint f : new Footprint[]{Footprint.oval(-37, 91, 45, 33), Footprint.rect(200, -5, 30, 50), Footprint.oval(8, 8, 3, 3)}) {
            Set<Long> listed = new HashSet<>();
            for (int[] c : f.chunks()) listed.add(((long) c[0] << 32) | (c[1] & 0xFFFFFFFFL));
            assertEquals(f.chunks().size(), listed.size(), "no chunk twice");
            Set<Long> brute = new HashSet<>();
            for (int x = f.minX() - 20; x <= f.maxX() + 20; x++) {
                for (int z = f.minZ() - 20; z <= f.maxZ() + 20; z++) {
                    if (f.contains(x, z)) brute.add(((long) Math.floorDiv(x, 16) << 32) | (Math.floorDiv(z, 16) & 0xFFFFFFFFL));
                }
            }
            assertEquals(brute, listed, f.toString());
            for (int[] c : f.chunks()) assertTrue(f.touchesChunk(c[0], c[1]));
            assertFalse(f.touchesChunk(Math.floorDiv(f.maxX(), 16) + 2, Math.floorDiv(f.centreZ(), 16)));
        }
    }

    @Test
    void aFootprintNeedsAShapeAndRoom() {
        assertThrows(IllegalArgumentException.class, () -> Footprint.oval(0, 0, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Footprint(null, 0, 0, 5, 5));
    }

    @Test
    void aSmallOvalStillHasItsMiddleChunk() {
        List<int[]> chunks = Footprint.oval(8, 8, 3, 3).chunks();
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0)[0]);
        assertEquals(0, chunks.get(0)[1]);
    }
}
