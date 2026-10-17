package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the unit tests of {@link HubModel} look at the finished world; these look one level down, at the two things every landmark is built on, so that when the
 * world test fails the reason is already narrowed: the walkway (it hugs the wall exactly, climbs or descends half a block at a time, holds its landings at the
 * heights the galleries and the cave floors were aligned to, and ends at the gate's standing level) and the Wave Function Collapse results (each gallery and the
 * whole chasm grid is a consistent assembly judged by a checker that does not trust the solver, pre-collapsed cells are where the landings are, and no free cell
 * may hold a doorway tile). They run on several seeds because the solver's choices are the only thing that changes with the seed.
 */
public class LandmarkModelsTest {
    @Test
    public void theSpireStairHugsTheInnerWallAndHoldsItsLandingsAtTheGalleryHeights() {
        for (long seed : new long[] {1, 77, 31337}) {
            SpireModel sp = HubModel.of(seed).spire();
            Spiral st = sp.stair();
            HelixPath p = st.path;
            for (int i = 0; i < p.steps(); i += 5) {
                int row = st.surfaceRow(i);
                assertEquals(SpireModel.rin(row), p.radius(i) + SpireModel.WIDTH / 2.0, 1e-9, "the walkway's outer edge is the inner face of the wall at step " + i);
                assertEquals(HubLayout.SURFACE_Y + p.q(i) / 2.0, st.topHalf(i) / 2.0, 1e-9, "half a block of height per half step");
            }
            assertEquals(2 * (HubLayout.HOLY_GATE_Y - 2 - HubLayout.SURFACE_Y), p.q(p.steps() - 1), "the stair ends at the standing level of the Holy gate");
            for (int g = 0; g < sp.galleryCount(); g++) {
                int[] ab = st.landingSteps(2 * SpireModel.GALLERY_STEP * g);
                assertTrue(ab != null && ab[1] - ab[0] + 1 >= 10, "landing " + g + " is at least 10 blocks of arc");
                assertEquals(HubLayout.SURFACE_Y + SpireModel.GALLERY_STEP * g - 1, st.surfaceRow(ab[0]), "the landing floor of gallery " + g + " is the floor row of the gallery's cells");
                assertEquals(sp.gallery(g).standY(), HubLayout.SURFACE_Y + SpireModel.GALLERY_STEP * g);
            }
            assertEquals(Geo.wrapPi(SpireModel.DOOR_ANGLE), Geo.wrapPi(st.landingMid(0)), 0.02, "the first landing is centred on the front door");
            assertTrue(p.isOpen(p.steps() - 1) && !p.isOpen(0), "only the last stop is railing free");
        }
    }

    /** The largest difference in half blocks between the surface heights of two 4-neighbouring walkway columns of a spiral (1 = every column can be walked onto). */
    private static int worstStep(Spiral sp, int cx, int cz, double reach) {
        int n = (int) reach + 2, worst = 0;
        Spiral.Hits ha = new Spiral.Hits(), hb = new Spiral.Hits();
        int[][] d = {{1, 0}, {0, 1}};
        for (int x = cx - n; x <= cx + n; x++)
            for (int z = cz - n; z <= cz + n; z++) {
                double ax = x - cx, az = z - cz, ra = Math.sqrt(ax * ax + az * az);
                sp.find(Geo.angle(ax, az), ra, ha);
                for (int i = 0; i < ha.n; i++) {
                    int sa = ha.step[i];
                    if (sp.part(sa, ra, sp.surfaceRow(sa)) != Spiral.WALK) continue;
                    for (int[] dd : d) {
                        double bx = x + dd[0] - cx, bz = z + dd[1] - cz, rb = Math.sqrt(bx * bx + bz * bz);
                        sp.find(Geo.angle(bx, bz), rb, hb);
                        int best = Integer.MAX_VALUE;
                        for (int j = 0; j < hb.n; j++) {
                            int sb = hb.step[j];
                            if (sp.part(sb, rb, sp.surfaceRow(sb)) != Spiral.WALK) continue;
                            best = Math.min(best, Math.abs(sp.topHalf(sa) - sp.topHalf(sb)));
                        }
                        if (best != Integer.MAX_VALUE && best < 30) worst = Math.max(worst, best);
                    }
                }
            }
        return worst;
    }

    @Test
    public void everyColumnOfTheWalkwayCanBeWalkedOntoFromItsNeighboursWithoutAJump() {
        for (long seed : new long[] {1, 4242}) {
            HubModel m = HubModel.of(seed);
            assertTrue(worstStep(m.spire().stair(), SpireModel.CX, SpireModel.CZ, SpireModel.REACH) <= 1, "Spire: no step on the stair is more than half a block");
            assertTrue(worstStep(m.chasm().ramp(), ChasmModel.CX, ChasmModel.CZ, ChasmModel.R0 + 2) <= 1, "Chasm: no step on the ramp is more than half a block");
        }
    }

    @Test
    public void theSpireGalleriesAreConsistentAssembliesWithTheDoorwayOnTheLanding() {
        for (long seed : new long[] {3, 1234, 99999}) {
            SpireModel sp = HubModel.of(seed).spire();
            int prevSectors = 1000;
            for (int g = 0; g < sp.galleryCount(); g++) {
                SpireModel.Gallery gal = sp.gallery(g);
                Wfc3D.Problem p = SpireTiles.problem(gal.sectors(), g == 0);
                assertFalse(gal.wfc().fallbackUsed, "gallery " + g + " needed no fallback");
                assertEquals(0, Wfc3DTest.violations(SpireTiles.TILESET, p, gal.wfc()), "gallery " + g + " is a consistent assembly (seed " + seed + ")");
                assertEquals(g == 0 ? SpireTiles.PASSAGE : SpireTiles.HALL_DOOR, gal.wfc().tileAt(0, 0, 0), "the cell on the landing is pre-collapsed");
                for (int s = 0; s < gal.sectors(); s++) for (int t = 0; t < 2; t++) {
                    int tile = gal.wfc().tileAt(s, t, 0);
                    if (s == 0 && t == 0) continue;
                    assertTrue(((1L << tile) & SpireTiles.FIXED_ONLY) == 0, "no doorway tile at " + s + "," + t + " of gallery " + g);
                }
                assertTrue(gal.sectors() <= prevSectors, "the ring gets smaller going up (the tower tapers)");
                prevSectors = gal.sectors();
            }
        }
    }

    @Test
    public void theChasmRampHugsTheWallDescendsHalfABlockAtATimeAndLandsOnTheCaveFloors() {
        for (long seed : new long[] {1, 77, 31337}) {
            ChasmModel ch = HubModel.of(seed).chasm();
            Spiral ramp = ch.ramp();
            HelixPath p = ramp.path;
            for (int i = 0; i < p.steps(); i += 5) {
                int row = ramp.surfaceRow(i);
                assertEquals(ChasmModel.rwall(row), p.radius(i) + ChasmModel.WIDTH / 2.0, 1e-9, "the ramp's outer edge is the wall at step " + i);
                assertEquals(HubLayout.SURFACE_Y - p.q(i) / 2.0, ramp.topHalf(i) / 2.0, 1e-9, "half a block of height per half step, descending");
            }
            assertEquals(2 * (HubLayout.SURFACE_Y - ChasmModel.FLOOR_STAND), p.q(p.steps() - 1), "the ramp ends at the floor of the pit");
            assertEquals(0, p.q(0), "it starts at the rim");
            int[] lv = ch.landingLevels();
            assertTrue(lv.length >= 5, "about every 18 blocks: " + lv.length + " landings");
            int cell = ch.params().cell();
            for (int i = 0; i < lv.length; i++) {
                int stand = ChasmModel.CAVE_TOP - cell * (lv[i] + 1) + 2;
                int[] ab = ramp.landingSteps(2 * (HubLayout.SURFACE_Y - stand));
                assertTrue(ab != null, "landing at level " + lv[i]);
                assertEquals(stand - 1, ramp.surfaceRow(ab[0]), "the landing floor is the floor row of cave level " + lv[i]);
                if (i > 0) assertTrue(lv[i] > lv[i - 1], "landing levels descend");
            }
            assertEquals(Geo.wrapPi(ChasmModel.DOOR_ANGLE), Geo.wrapPi(ramp.landingMid(0)), 0.02, "the first landing is centred on the rim door");
        }
    }

    @Test
    public void theChasmGridIsAConsistentAssemblyWithAMouthBehindEveryLanding() {
        for (long seed : new long[] {3, 1234, 99999}) {
            ChasmModel ch = HubModel.of(seed).chasm();
            Wfc3D.Problem p = ChasmTiles.problem(ch.sectors(), ch.levels());
            Wfc3D.Result r = ch.wfc();
            assertFalse(r.fallbackUsed);
            // the pre-collapsed cells of the landings must be re-created for the independent checker: it needs the problem the model solved
            Spiral ramp = ch.ramp();
            int[] lv = ch.landingLevels();
            int cell = ch.params().cell();
            int fixed = 0;
            for (int i = 0; i < lv.length; i++) {
                int[] ab = ramp.landingSteps(2 * (HubLayout.SURFACE_Y - (ChasmModel.CAVE_TOP - cell * (lv[i] + 1) + 2)));
                int j = ch.levels() - 1 - lv[i];
                for (int step = ab[0]; step <= ab[1]; step++) {
                    double ang = ramp.path.angleOf(step) + ramp.path.dir * 0.5 / Math.max(1.0, ramp.path.radius(step));
                    int s = Geo.sector(ang, ChasmModel.DOOR_ANGLE, ch.sectors());
                    p.setFixed(s, j, 0, ChasmTiles.MOUTH);
                    assertEquals(ChasmTiles.MOUTH, r.tileAt(s, j, 0), "the mouth behind landing " + i + " at sector " + s);
                    fixed++;
                }
            }
            assertTrue(fixed >= 10, "several mouths are pre-collapsed: " + fixed);
            assertEquals(0, Wfc3DTest.violations(ChasmTiles.TILESET, p, r), "the whole chasm grid is a consistent assembly (seed " + seed + ")");
            int open = 0, total = 0;
            for (int s = 0; s < ch.sectors(); s++) for (int y = 0; y < ch.levels(); y++) for (int z = 0; z < ChasmModel.RINGS; z++) {
                total++;
                if (r.tileAt(s, y, z) != ChasmTiles.ROCK) open++;
            }
            double share = (double) open / total;
            assertTrue(share > 0.25 && share < 0.75, "a believable share of the rock is cavern: " + share);
        }
    }

    @Test
    public void everyGalleryAndTheChasmAreSolvedWithoutFallbackOnAHundredSeeds() {
        int solves = 0;
        long worst = 0;
        for (int i = 0; i < 100; i++) {
            HubModel m = HubModel.of(5000L + 104729L * i);
            for (int g = 0; g < m.spire().galleryCount(); g++) {
                Wfc3D.Result r = m.spire().gallery(g).wfc();
                assertFalse(r.fallbackUsed, "Spire gallery " + g + ", seed index " + i);
                worst = Math.max(worst, r.restarts);
                solves++;
            }
            assertFalse(m.chasm().wfc().fallbackUsed, "Chasm, seed index " + i);
            worst = Math.max(worst, m.chasm().wfc().restarts);
            solves++;
        }
        assertEquals(700, solves);
        assertTrue(worst <= 1, "restarts are rare to never: the worst needed " + worst);
    }

    @Test
    public void theLargestRealGridSolvesInWellUnderTwoSeconds() {
        long t0 = System.nanoTime();
        Wfc3D.Problem p = ChasmTiles.problem(42, 19);
        Wfc3D.Result r = Wfc3D.solve(ChasmTiles.TILESET, p, 4711L, 12, ChasmTiles.ROCK);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertFalse(r.fallbackUsed);
        assertEquals(0, Wfc3DTest.violations(ChasmTiles.TILESET, p, r));
        assertTrue(ms < 2000, "42 x 19 x 3 cells solved in " + ms + " ms");
    }
}
