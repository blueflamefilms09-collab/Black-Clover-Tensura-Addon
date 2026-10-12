package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The cube of Spatial Mana Domination: who is inside, how far the walls are, which way out, where the edges run. */
class DomainCubeTest {
    static final double E = 1e-9;

    @Test
    void containsTheInsideAndTheWallsButNotTheOutside() {
        DomainCube c = new DomainCube(10, 64, -5, 8);
        assertTrue(c.contains(10, 64, -5));
        assertTrue(c.contains(2, 56, -13), "a corner is on the wall, which counts as inside");
        assertTrue(c.contains(18, 72, 3));
        assertFalse(c.contains(18.001, 64, -5));
        assertFalse(c.contains(1.99, 64, -5));
        assertFalse(c.contains(10, 72.01, -5));
        assertFalse(c.contains(10, 55.9, -5));
        assertFalse(c.contains(10, 64, 3.01));
        assertFalse(c.contains(10, 64, -13.01));
    }

    @Test
    void paddingGrowsAndShrinksTheCube() {
        DomainCube c = new DomainCube(0, 0, 0, 5);
        assertFalse(c.contains(5.5, 0, 0));
        assertTrue(c.contains(5.5, 0, 0, 1));
        assertTrue(c.contains(4.5, 0, 0, -0.4));
        assertFalse(c.contains(4.7, 0, 0, -0.4));
        assertTrue(c.contains(0, 0, 0, 0));
    }

    @Test
    void sizeIsClamped() {
        assertEquals(DomainCube.MIN_HALF, new DomainCube(0, 0, 0, 0.1).half(), E);
        assertEquals(DomainCube.MIN_HALF, new DomainCube(0, 0, 0, -9).half(), E);
        assertEquals(DomainCube.MAX_HALF, new DomainCube(0, 0, 0, 500).half(), E);
        assertEquals(DomainCube.MIN_HALF, new DomainCube(0, 0, 0, Double.NaN).half(), E);
        assertEquals(DomainCube.MAX_HALF, DomainCube.around(0, 0, 0, 99).half(), E);
        assertEquals(11, new DomainCube(0, 0, 0, 11).half(), E);
    }

    @Test
    void aroundAGroundPointPutsTheFloorBelowIt() {
        DomainCube c = DomainCube.around(100.5, 64, 200.5, 9.5);
        assertEquals(64 - DomainCube.BELOW_FLOOR, c.minY(), E);
        assertEquals(64 - DomainCube.BELOW_FLOOR + 19, c.maxY(), E);
        assertEquals(100.5, c.cx(), E);
        assertEquals(200.5, c.cz(), E);
        assertTrue(c.contains(100.5, 64, 200.5), "the ground point is inside");
        assertTrue(c.contains(100.5, 64 + 1.8, 200.5), "the head of someone standing there is inside");
        assertTrue(c.contains(100.5, 64 + 12, 200.5), "a jump or a leap stays inside");
        assertFalse(c.contains(100.5, 64 + 17, 200.5), "but not far over the roof");
        assertFalse(c.contains(100.5, 64 - 3.5, 200.5));
    }

    @Test
    void wallDistanceIsPositiveInsideZeroOnTheWallNegativeOutside() {
        DomainCube c = new DomainCube(0, 0, 0, 10);
        assertEquals(10, c.wallDistance(0, 0, 0), E);
        assertEquals(4, c.wallDistance(6, 0, 0), E);
        assertEquals(4, c.wallDistance(0, -6, 3), E);
        assertEquals(0, c.wallDistance(0, 0, 10), E);
        assertEquals(-2, c.wallDistance(12, 0, 0), E);
        assertEquals(-5, c.wallDistance(3, 15, 3), E);
        // the nearest wall decides
        assertEquals(1, c.wallDistance(9, 2, -3), E);
    }

    @Test
    void insideMeansNonNegativeWallDistance() {
        DomainCube c = new DomainCube(3, -4, 7, 6.5);
        for (double x = -6; x <= 12; x += 1.7)
            for (double y = -12; y <= 4; y += 1.9)
                for (double z = 0; z <= 14; z += 2.3)
                    assertEquals(c.contains(x, y, z), c.wallDistance(x, y, z) >= 0, x + "," + y + "," + z);
    }

    @Test
    void nearestFaceAndTheSideWayOut() {
        DomainCube c = new DomainCube(0, 0, 0, 10);
        assertEquals(DomainCube.EAST, c.nearestFace(8, 0, 0));
        assertEquals(DomainCube.WEST, c.nearestFace(-7, 1, 2));
        assertEquals(DomainCube.UP, c.nearestFace(1, 9, 0));
        assertEquals(DomainCube.DOWN, c.nearestFace(1, -9, 2));
        assertEquals(DomainCube.NORTH, c.nearestFace(0, 0, -8));
        assertEquals(DomainCube.SOUTH, c.nearestFace(0, 0, 9));
        assertEquals(DomainCube.WEST, c.nearestFace(0, 0, 0), "a dead-centre tie goes to the first face");
        // near the roof the walker's way out is still a side wall
        assertEquals(DomainCube.EAST, c.nearestSideFace(7, 1));
        assertEquals(DomainCube.NORTH, c.nearestSideFace(1, -8));
        assertEquals(DomainCube.UP, c.nearestFace(7, 9.5, 1));
    }

    @Test
    void faceDistancesAddUpToTheSide() {
        DomainCube c = new DomainCube(5, 5, 5, 4);
        double[] d = c.faceDistances(6.5, 3, 7);
        assertEquals(c.half() * 2, d[DomainCube.WEST] + d[DomainCube.EAST], E);
        assertEquals(c.half() * 2, d[DomainCube.DOWN] + d[DomainCube.UP], E);
        assertEquals(c.half() * 2, d[DomainCube.NORTH] + d[DomainCube.SOUTH], E);
    }

    @Test
    void exitHintNamesTheWallAndTheDistance() {
        DomainCube c = new DomainCube(0, 0, 0, 10);
        assertEquals("east wall, 3 blocks", c.exitHint(7, 0, 0));
        assertEquals("north wall, 6 blocks", c.exitHint(0, 0, -4));
        assertEquals("west wall, 1 block", c.exitHint(-9.5, 0, 0));
        assertEquals("south wall, 1 block", c.exitHint(0, 0, 10));
        assertEquals("west", DomainCube.faceName(DomainCube.WEST));
        assertEquals("up", DomainCube.faceName(DomainCube.UP));
        assertEquals("away", DomainCube.faceName(99));
    }

    @Test
    void faceDirectionPointsOutOfTheCube() {
        assertArrayEquals(new double[]{-1, 0}, DomainCube.faceDirection(DomainCube.WEST), E);
        assertArrayEquals(new double[]{1, 0}, DomainCube.faceDirection(DomainCube.EAST), E);
        assertArrayEquals(new double[]{0, -1}, DomainCube.faceDirection(DomainCube.NORTH), E);
        assertArrayEquals(new double[]{0, 1}, DomainCube.faceDirection(DomainCube.SOUTH), E);
        assertArrayEquals(new double[]{0, 0}, DomainCube.faceDirection(DomainCube.UP), E);
    }

    @Test
    void twelveEdgesEachAxisAlignedAndTheSideLong() {
        DomainCube c = new DomainCube(1, 2, 3, 5);
        double[][] edges = c.edges();
        assertEquals(DomainCube.EDGES, edges.length);
        int[] along = new int[3];
        for (double[] e : edges) {
            int changed = 0, axis = -1;
            for (int a = 0; a < 3; a++) if (Math.abs(e[a] - e[a + 3]) > E) { changed++; axis = a; }
            assertEquals(1, changed, "an edge runs along exactly one axis");
            assertEquals(10, Math.abs(e[axis] - e[axis + 3]), E);
            along[axis]++;
            for (int p = 0; p < 2; p++) {
                // both ends are corners of the cube
                for (int a = 0; a < 3; a++) {
                    double v = e[p * 3 + a], lo = (a == 0 ? c.minX() : a == 1 ? c.minY() : c.minZ()), hi = (a == 0 ? c.maxX() : a == 1 ? c.maxY() : c.maxZ());
                    assertTrue(Math.abs(v - lo) < E || Math.abs(v - hi) < E, "a corner coordinate");
                }
            }
        }
        assertArrayEquals(new int[]{4, 4, 4}, along);
    }

    @Test
    void edgesAreAllDistinctAndMeetTwoPerCorner() {
        DomainCube c = new DomainCube(0, 0, 0, 3);
        double[][] edges = c.edges();
        java.util.Map<String, Integer> corner = new java.util.HashMap<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (double[] e : edges) {
            assertTrue(seen.add(java.util.Arrays.toString(e)), "no repeated edge");
            corner.merge(e[0] + "," + e[1] + "," + e[2], 1, Integer::sum);
            corner.merge(e[3] + "," + e[4] + "," + e[5], 1, Integer::sum);
        }
        assertEquals(8, corner.size(), "eight corners");
        for (int n : corner.values()) assertEquals(3, n, "three edges meet at every corner");
    }

    @Test
    void edgeSamplesWalkTheEdgeWithTheSpacingAskedFor() {
        DomainCube c = new DomainCube(0, 0, 0, 5);
        for (int e = 0; e < DomainCube.EDGES; e++) {
            List<double[]> s = c.edgeSamples(e, 2);
            assertEquals(6, s.size(), "10 blocks at spacing 2: five gaps, six points");
            double[] edge = c.edges()[e];
            assertArrayEquals(new double[]{edge[0], edge[1], edge[2]}, s.get(0), E);
            assertArrayEquals(new double[]{edge[3], edge[4], edge[5]}, s.get(s.size() - 1), E);
            for (int i = 1; i < s.size(); i++) {
                double[] a = s.get(i - 1), b = s.get(i);
                assertEquals(2, Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2)), 1e-6);
            }
        }
        assertTrue(c.edgeSamples(-1, 2).isEmpty());
        assertTrue(c.edgeSamples(12, 2).isEmpty());
        assertTrue(c.edgeSamples(0, 0).size() >= 2, "a zero spacing still terminates");
        assertEquals(41, c.edgeSamples(0, 0.25).size());
    }

    @Test
    void wallPointLiesOnThePlaneAndInsideTheFace() {
        DomainCube c = new DomainCube(0, 0, 0, 10);
        assertArrayEquals(new double[]{10, 2, -3}, c.wallPoint(DomainCube.EAST, 7, 2, -3), E);
        assertArrayEquals(new double[]{-10, 2, -3}, c.wallPoint(DomainCube.WEST, 7, 2, -3), E);
        assertArrayEquals(new double[]{7, 10, -3}, c.wallPoint(DomainCube.UP, 7, 2, -3), E);
        assertArrayEquals(new double[]{7, -10, -3}, c.wallPoint(DomainCube.DOWN, 7, 2, -3), E);
        assertArrayEquals(new double[]{7, 2, -10}, c.wallPoint(DomainCube.NORTH, 7, 2, -3), E);
        assertArrayEquals(new double[]{7, 2, 10}, c.wallPoint(DomainCube.SOUTH, 7, 2, -3), E);
        double[] clamped = c.wallPoint(DomainCube.EAST, 7, 40, -40);
        assertArrayEquals(new double[]{10, 10, -10}, clamped, E, "clamped to the face's rectangle");
        assertTrue(c.contains(clamped[0], clamped[1], clamped[2]));
    }

    @Test
    void aWalkerFromTheMiddleLeavesInTimeAtEveryPower() {
        for (int p = 60; p <= 100; p += 5) {
            double half = DomainMath.half(p);
            DomainCube c = DomainCube.around(0, 0, 0, half);
            double walked = 0;
            int ticks = 0;
            // walk due east at walking speed until outside
            while (c.contains(walked, 0, 0)) { walked += DomainMath.WALK; ticks++; }
            assertTrue(ticks <= DomainMath.ticksToLeave(half) + 1, "power " + p);
            int windUp = Math.round(ZenonTable.byId(ZenonTable.DOMINATION).windUpTicks() * (float) PowerCurve.tuning(p).windUpMultiplier());
            assertTrue(ticks <= windUp + DomainClock.FORM_TICKS, "power " + p + " walks out in " + ticks + " ticks, has " + (windUp + DomainClock.FORM_TICKS));
        }
    }
}
