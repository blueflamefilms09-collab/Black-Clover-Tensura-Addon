package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.dungeon.nexus.nav.ClusterFinder.Cluster;
import com.newuniverse.nusmp.dungeon.nexus.nav.ClusterFinder.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Anti-Bird Compass's search for the densest group of chunks. */
class ClusterFinderTest {
    private static List<Sample> flat(int radius, float biomass) {
        List<Sample> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) out.add(new Sample(x, z, biomass));
        return out;
    }

    private static void set(List<Sample> s, int cx, int cz, float biomass) {
        s.removeIf(a -> a.cx() == cx && a.cz() == cz);
        s.add(new Sample(cx, cz, biomass));
    }

    @Test
    void nothingLoadedFindsNothing() {
        assertNull(ClusterFinder.find(0, 0, List.of()));
    }

    @Test
    void chunksThatNeverSimulatedAreNoCluster() {
        assertNull(ClusterFinder.find(0, 0, flat(6, 20f)), "20 is the default of a chunk nobody simulated");
        assertNull(ClusterFinder.find(0, 0, flat(6, 0f)), "an empty dungeon");
        assertNull(ClusterFinder.find(0, 0, flat(6, Float.NaN)), "garbage is nothing");
    }

    @Test
    void aSingleThickChunkIsTheCluster() {
        List<Sample> s = flat(6, 20f);
        set(s, 3, -2, 80f);
        Cluster c = ClusterFinder.find(0, 0, s);
        assertNotNull(c);
        assertEquals(3, c.peakCx());
        assertEquals(-2, c.peakCz());
        assertEquals(3 * 16 + 8, c.x(), 1e-9, "the middle of its chunk");
        assertEquals(-2 * 16 + 8, c.z(), 1e-9);
        assertEquals(60f, c.density(), 1e-4);
    }

    @Test
    void aThinChunkBelowTheLeastDensityIsNotACluster() {
        List<Sample> s = flat(6, 20f);
        set(s, 1, 1, 20f + ClusterFinder.MIN_DENSITY - 1f);
        assertNull(ClusterFinder.find(0, 0, s));
        set(s, 1, 1, 20f + ClusterFinder.MIN_DENSITY);
        assertNotNull(ClusterFinder.find(0, 0, s));
    }

    @Test
    void theCentreLeansTowardTheHeavierNeighbour() {
        List<Sample> s = flat(6, 20f);
        set(s, 0, 0, 100f);      // weight 80
        set(s, 1, 0, 60f);       // weight 40
        Cluster c = ClusterFinder.find(5, 5, s);
        assertNotNull(c);
        assertEquals(0, c.peakCx());
        assertEquals(0, c.peakCz());
        double a = 80 * 1.0, b = 40 * 0.7;
        double expect = (a * 8 + b * 24) / (a + b);
        assertEquals(expect, c.x(), 1e-6);
        assertEquals(8.0, c.z(), 1e-6, "no pull in z");
        assertTrue(c.x() > 8 && c.x() < 24, "between the two chunks, nearer the heavy one");
    }

    @Test
    void aGroupOfChunksIsOneClusterAtItsMiddle() {
        List<Sample> s = flat(6, 20f);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(s, 2 + dx, 2 + dz, 50f);
        Cluster c = ClusterFinder.find(-3, -3, s);
        assertNotNull(c);
        assertEquals(2, c.peakCx(), "the middle of the group is its peak");
        assertEquals(2, c.peakCz());
        assertEquals(2 * 16 + 8, c.x(), 1e-6);
        assertEquals(2 * 16 + 8, c.z(), 1e-6);
    }

    @Test
    void theNearestStrongClusterWins() {
        List<Sample> s = flat(6, 20f);
        set(s, 5, 0, 100f);      // weight 80, far
        set(s, -2, 0, 90f);      // weight 70, near and at least half as dense
        Cluster c = ClusterFinder.find(0, 0, s);
        assertNotNull(c);
        assertEquals(-2, c.peakCx());
    }

    @Test
    void aWeakStrayNearbyIsIgnoredForTheRealCluster() {
        List<Sample> s = flat(6, 20f);
        set(s, 1, 0, 55f);       // weight 35: a stray
        set(s, -5, 4, 120f);     // weight 100: the real thing
        Cluster c = ClusterFinder.find(0, 0, s);
        assertNotNull(c);
        assertEquals(-5, c.peakCx());
        assertEquals(4, c.peakCz());
        set(s, 1, 0, 75f);       // weight 55: now more than half as dense, and nearer
        assertEquals(1, ClusterFinder.find(0, 0, s).peakCx());
    }

    @Test
    void equalClustersAreBrokenByDistanceThenByPlace() {
        List<Sample> s = flat(6, 20f);
        set(s, 4, 0, 90f);
        set(s, -4, 0, 90f);
        Cluster c = ClusterFinder.find(0, 0, s);
        assertEquals(-4, c.peakCx(), "equally near and dense: the lowest x");
        assertEquals(4, ClusterFinder.find(6, 0, s).peakCx(), "nearer to this holder");
    }

    @Test
    void twoEqualNeighboursDoNotBothBecomeThePeak() {
        List<Sample> s = flat(6, 20f);
        set(s, 0, 0, 80f);
        set(s, 1, 0, 80f);
        Cluster c = ClusterFinder.find(0, 3, s);
        assertNotNull(c);
        assertEquals((60 * 8 + 42 * 24) / 102.0, c.x(), 1e-6, "the peak at full weight, its twin at 0.7");
        assertEquals(0, c.peakCx(), "a tie goes to the lowest chunk");
    }

    @Test
    void theOrderOfTheSamplesDoesNotMatter() {
        Random rnd = new Random(7);
        for (int run = 0; run < 40; run++) {
            List<Sample> s = flat(6, 20f);
            for (int i = 0; i < 6; i++) set(s, rnd.nextInt(13) - 6, rnd.nextInt(13) - 6, 20f + rnd.nextInt(90));
            Cluster a = ClusterFinder.find(0, 0, s);
            List<Sample> shuffled = new ArrayList<>(s);
            Collections.shuffle(shuffled, rnd);
            Cluster b = ClusterFinder.find(0, 0, shuffled);
            assertEquals(a, b);
        }
    }

    @Test
    void theResultIsAlwaysInsideTheSampledChunks() {
        Random rnd = new Random(11);
        for (int run = 0; run < 60; run++) {
            List<Sample> s = flat(6, 20f);
            for (int i = 0; i < 8; i++) set(s, rnd.nextInt(13) - 6, rnd.nextInt(13) - 6, rnd.nextInt(200));
            Cluster c = ClusterFinder.find(rnd.nextInt(5) - 2, rnd.nextInt(5) - 2, s);
            if (c == null) continue;
            assertTrue(c.x() >= -7 * 16 && c.x() <= 7 * 16 + 16, "x " + c.x());
            assertTrue(c.z() >= -7 * 16 && c.z() <= 7 * 16 + 16, "z " + c.z());
            assertTrue(c.density() >= ClusterFinder.MIN_DENSITY);
        }
    }

    @Test
    void aLoneSampleOnTheEdgeOfWhatIsLoadedStillWorks() {
        Cluster c = ClusterFinder.find(0, 0, List.of(new Sample(6, 6, 90f)));
        assertNotNull(c);
        assertEquals(6 * 16 + 8, c.x(), 1e-9);
    }

    @Test
    void duplicateSamplesOfOneChunkCountOnce() {
        List<Sample> s = new ArrayList<>();
        s.add(new Sample(2, 2, 90f));
        s.add(new Sample(2, 2, 90f));
        Cluster c = ClusterFinder.find(0, 0, s);
        assertEquals(70f, c.density(), 1e-4);
    }
}
