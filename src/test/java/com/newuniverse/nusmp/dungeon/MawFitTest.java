package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawFit.Cell;
import com.newuniverse.nusmp.dungeon.MawFit.Fit;
import com.newuniverse.nusmp.dungeon.MawFit.Place;
import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import com.newuniverse.nusmp.dungeon.MawShapes.Mat;
import com.newuniverse.nusmp.dungeon.MawShapes.Plan;
import com.newuniverse.nusmp.dungeon.MawShapes.Put;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Does a Maw shape fit the ground: refused on water, cliffs, trunks, empty chunks, creatures, built ground, bottomless caves; footed on slopes. */
class MawFitTest {
    private static final Kind[] PLANNED = {Kind.TOWER, Kind.RUINS, Kind.GATE};
    private static final int CX = 100, CZ = -200, TOP = 64;

    /** A world that is ground below a height function and air above it, with cells and creatures put on top. */
    private static final class Fake implements MawFit.Terrain {
        IntBinaryOperator height = (x, z) -> TOP;
        BiPredicate<Integer, Integer> loaded = (x, z) -> true;
        final Map<Long, Cell> over = new HashMap<>();
        final List<int[]> beings = new ArrayList<>();
        int minY = -64, maxY = 320, heightCalls;

        private static long key(int x, int y, int z) { return ((long) x << 40) ^ ((long) (y + 1000) << 20) ^ (z + 100000); }

        void set(int x, int y, int z, Cell c) { over.put(key(x, y, z), c); }

        @Override public boolean loaded(int x, int z) { return loaded.test(x, z); }

        @Override public int height(int x, int z) { heightCalls++; return height.applyAsInt(x, z); }

        @Override public Cell cell(int x, int y, int z) {
            Cell o = over.get(key(x, y, z));
            return o != null ? o : y < height.applyAsInt(x, z) ? Cell.GROUND : Cell.AIR;
        }

        @Override public boolean occupied(int x1, int y1, int z1, int x2, int y2, int z2) {
            for (int[] b : beings) if (b[0] >= x1 && b[0] <= x2 && b[1] >= y1 && b[1] <= y2 && b[2] >= z1 && b[2] <= z2) return true;
            return false;
        }

        @Override public int minY() { return minY; }

        @Override public int maxY() { return maxY; }
    }

    private static Plan plan(Kind k, int rot, long seed) { return MawShapes.plan(k, rot, seed, MawShapes.PLAIN); }

    // ---------------------------------------------------------------- flat ground
    @Test
    void aFlatPatchTakesEveryShapeAsItIs() {
        for (Kind k : PLANNED) {
            for (int rot = 0; rot < 4; rot++) {
                for (long seed = 0; seed < 12; seed++) {
                    Plan p = plan(k, rot, seed);
                    Fit f = MawFit.scan(p, new Fake(), CX, CZ);
                    assertNotNull(f, k + " rot " + rot);
                    assertEquals(TOP, f.base());
                    long expected = p.puts().stream().filter(q -> !(q.mat() == Mat.AIR && q.y() >= 0)).count();     // clearing air that is already air writes nothing
                    assertEquals(expected, f.places().size(), k + ": no footings on flat ground, nothing clears the air");
                    Place last = f.places().get(f.places().size() - 1);
                    assertEquals(Mat.MAW, last.mat(), "the mouth is written last");
                    assertEquals(CX + p.maw().x(), last.x());
                    assertEquals(TOP + p.maw().y(), last.y());
                    assertEquals(CZ + p.maw().z(), last.z());
                    for (Place q : f.places()) assertTrue(Math.abs(q.x() - CX) <= p.reach() && Math.abs(q.z() - CZ) <= p.reach(), "inside the reach: " + q);
                }
            }
        }
    }

    @Test
    void theRootsAreExactlyTheRootsOfThePlan() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 1, 4);
            Fit f = MawFit.scan(p, new Fake(), CX, CZ);
            assertEquals(p.roots(), f.places().stream().filter(q -> q.mat() == Mat.ROOT).count());
            assertEquals(1, f.places().stream().filter(q -> q.mat() == Mat.MAW).count());
        }
    }

    @Test
    void theSameGroundGivesTheSameWrites() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 2, 9);
            assertEquals(MawFit.scan(p, new Fake(), CX, CZ), MawFit.scan(p, new Fake(), CX, CZ));
        }
    }

    // ---------------------------------------------------------------- slopes, cliffs
    @Test
    void aLowColumnIsFootedUpToTheFloor() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 3);
            Fake w = new Fake();
            w.height = (x, z) -> x > CX ? TOP - 2 : TOP;                                   // the east half is two blocks lower
            Fit f = MawFit.scan(p, w, CX, CZ);
            assertNotNull(f, k + " takes a slope of two");
            assertEquals(TOP, f.base(), "the floor is at the highest column");
            int[] feet = p.footings();
            int expected = 0;
            for (int i = 0; i < feet.length; i += 3) {
                int x = CX + feet[i], z = CZ + feet[i + 1], lowest = feet[i + 2];
                if (x > CX) {                                                               // two below: the cells from the floor down to the ground top are air
                    int air = 0;
                    for (int y = TOP + lowest - 1; w.cell(x, y, z) == Cell.AIR; y--) air++;
                    expected += air;
                    for (int y = TOP + lowest - 1, n = 0; n < air; y--, n++) {
                        final int fy = y;
                        assertTrue(f.places().stream().anyMatch(q -> q.x() == x && q.y() == fy && q.z() == z && (q.mat() == Mat.COBBLE || q.mat() == Mat.MOSSY_COBBLE)), k + " footing at " + x + "," + fy + "," + z);
                    }
                }
            }
            assertTrue(expected > 0, k + " has columns on the low side");
            long cobble = f.places().stream().filter(q -> q.mat() == Mat.COBBLE || q.mat() == Mat.MOSSY_COBBLE).count();
            long fromPlan = p.puts().stream().filter(q -> q.mat() == Mat.COBBLE || q.mat() == Mat.MOSSY_COBBLE).count();
            assertEquals(expected + fromPlan, cobble, k + " writes the footings and nothing else of stone cobble");
        }
    }

    @Test
    void aSlopeOfThreeIsTooSteep() {
        for (Kind k : PLANNED) {
            Fake w = new Fake();
            w.height = (x, z) -> x > CX ? TOP - 3 : TOP;
            assertNull(MawFit.scan(plan(k, 0, 3), w, CX, CZ), k.name());
        }
    }

    @Test
    void aDoorwayMayLieOneBlockAboveTheGroundButNotTwo() {
        for (int rot = 0; rot < 4; rot++) {
            Plan p = plan(Kind.TOWER, rot, 5);
            int[] door = p.approach();
            for (int drop = 0; drop <= 2; drop++) {
                final int d = drop;
                Fake w = new Fake();
                w.height = (x, z) -> x == CX + door[0] && z == CZ + door[1] ? TOP - d : TOP;
                if (drop <= MawShapes.MAX_STEP) assertNotNull(MawFit.scan(p, w, CX, CZ), "rot " + rot + " drop " + drop);
                else assertNull(MawFit.scan(p, w, CX, CZ), "rot " + rot + ": the doorway hangs over a cliff");
            }
        }
    }

    @Test
    void aHigherDoorstepRaisesTheFloor() {
        Plan p = plan(Kind.TOWER, 0, 5);
        int[] door = p.approach();
        Fake w = new Fake();
        w.height = (x, z) -> x == CX + door[0] && z == CZ + door[1] ? TOP + 1 : TOP;
        Fit f = MawFit.scan(p, w, CX, CZ);
        assertNotNull(f);
        assertEquals(TOP + 1, f.base(), "the highest column sets the floor");
    }

    // ---------------------------------------------------------------- things in the way
    @Test
    void waterIsRefusedWhereverItIs() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 3);
            for (Put q : p.puts()) {
                if (q.mat() == Mat.AIR && q.y() >= 4) continue;                              // sample: every kind of cell near the floor
                Fake w = new Fake();
                w.set(CX + q.x(), TOP + q.y(), CZ + q.z(), Cell.FLUID);
                assertNull(MawFit.scan(p, w, CX, CZ), k + " water at " + q);
            }
            Fake lake = new Fake();
            lake.set(CX + p.maw().x(), TOP - 1, CZ + p.maw().z(), Cell.FLUID);
            assertNull(MawFit.scan(p, lake, CX, CZ), "water under the mouth");
        }
    }

    @Test
    void waterUnderAFootingIsRefused() {
        Plan p = plan(Kind.TOWER, 0, 3);
        Fake w = new Fake();
        w.height = (x, z) -> x == CX + 2 && z == CZ + 2 ? TOP - 2 : TOP;                   // a low corner
        assertNotNull(MawFit.scan(p, w, CX, CZ));
        w.set(CX + 2, TOP - 2, CZ + 2, Cell.FLUID);
        assertNull(MawFit.scan(p, w, CX, CZ), "the footing would stand in water");
    }

    @Test
    void aTrunkOrAnyBuiltBlockInTheVolumeIsRefusedAndGrassIsNot() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 6);
            Fake trunk = new Fake();
            trunk.set(CX, TOP + 2, CZ, Cell.OTHER);
            assertNull(MawFit.scan(p, trunk, CX, CZ), k + " does not stand in a trunk");
            Fake bush = new Fake();
            bush.set(CX, TOP + 2, CZ, Cell.SOFT);
            assertNotNull(MawFit.scan(p, bush, CX, CZ), k + " builds over leaves and grass");
        }
    }

    @Test
    void softCellsAreWrittenOverEvenAsAir() {
        Plan p = plan(Kind.TOWER, 0, 6);
        Fake w = new Fake();
        w.set(CX, TOP + 3, CZ + 1, Cell.SOFT);                                            // leaves inside the tower, in the room between the door and the roots: they are cleared
        Fit f = MawFit.scan(p, w, CX, CZ);
        assertNotNull(f);
        assertTrue(f.places().stream().anyMatch(q -> q.x() == CX && q.y() == TOP + 3 && q.z() == CZ + 1 && q.mat() == Mat.AIR));
    }

    @Test
    void builtGroundUnderTheFloorIsRefusedButNaturalGroundIsNot() {
        Plan tower = plan(Kind.TOWER, 0, 6);
        Fake planks = new Fake();
        planks.set(CX, TOP - 1, CZ, Cell.OTHER);                                          // a wooden floor where the tower's floor goes
        assertNull(MawFit.scan(tower, planks, CX, CZ));
        Fake dirt = new Fake();
        dirt.set(CX, TOP - 1, CZ, Cell.GROUND);
        assertNotNull(MawFit.scan(tower, dirt, CX, CZ));
    }

    @Test
    void theSunkenChamberIsOnlyCutIntoNaturalGround() {
        Plan ruins = plan(Kind.RUINS, 0, 6);
        Fake ore = new Fake();
        ore.set(CX, TOP - 2, CZ, Cell.OTHER);                                             // an ore (or a chest) where the mouth goes
        assertNull(MawFit.scan(ruins, ore, CX, CZ));
        Fake wall = new Fake();
        wall.set(CX + 2, TOP - 2, CZ, Cell.OTHER);                                        // and where a wall goes
        assertNull(MawFit.scan(ruins, wall, CX, CZ));
        assertNotNull(MawFit.scan(ruins, new Fake(), CX, CZ));
    }

    @Test
    void anythingAliveInThePatchIsRefusedButNotNextToIt() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 2);
            Fake w = new Fake();
            w.beings.add(new int[]{CX + 1, TOP, CZ});
            assertNull(MawFit.scan(p, w, CX, CZ), k + " does not wall anyone in");
            Fake far = new Fake();
            far.beings.add(new int[]{CX + 30, TOP, CZ});
            far.beings.add(new int[]{CX, TOP + 40, CZ});
            assertNotNull(MawFit.scan(p, far, CX, CZ), k + " ignores what is far away");
        }
    }

    // ---------------------------------------------------------------- chunks, limits, drops
    @Test
    void aShapeNeverReadsAChunkThatIsNotLoaded() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 2);
            int r = p.reach();
            int[][] corners = {{-r, -r}, {r, -r}, {-r, r}, {r, r}};
            for (int[] c : corners) {
                Fake w = new Fake();
                w.loaded = (x, z) -> !(x == CX + c[0] && z == CZ + c[1]);
                assertNull(MawFit.scan(p, w, CX, CZ), k + " corner " + c[0] + "," + c[1]);
            }
            Fake none = new Fake();
            none.loaded = (x, z) -> false;
            assertNull(MawFit.scan(p, none, CX, CZ));
            assertEquals(0, none.heightCalls, "an unloaded patch is not even measured");
        }
    }

    @Test
    void theWorldsHeightLimitsAreKept() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 2);
            Fake roof = new Fake();
            roof.maxY = TOP + p.maxY() + 2;                                                 // no room for the top
            assertNull(MawFit.scan(p, roof, CX, CZ));
            roof.maxY = TOP + p.maxY() + 3;
            assertNotNull(MawFit.scan(p, roof, CX, CZ));
            Fake floor = new Fake();
            floor.minY = TOP + p.minY() - MawShapes.FOOT_MAX - 1;                           // no room for the footings
            assertNull(MawFit.scan(p, floor, CX, CZ));
            floor.minY = TOP + p.minY() - MawShapes.FOOT_MAX - 2;
            assertNotNull(MawFit.scan(p, floor, CX, CZ));
        }
    }

    @Test
    void aCaveUnderAColumnIsFilledIfShallowAndRefusedIfDeep() {
        Plan p = plan(Kind.TOWER, 0, 4);
        for (int depth = 1; depth <= 6; depth++) {
            Fake w = new Fake();
            for (int d = 0; d < depth; d++) w.set(CX + 2, TOP - 2 - d, CZ + 2, Cell.AIR);   // air under the floor of the south-east corner
            Fit f = MawFit.scan(p, w, CX, CZ);
            if (depth <= MawShapes.FOOT_MAX) {
                assertNotNull(f, "a cave " + depth + " deep is filled");
                long footing = f.places().stream().filter(q -> q.x() == CX + 2 && q.z() == CZ + 2 && q.y() < TOP - 1).count();
                assertEquals(depth, footing, "one block of footing for every block of cave");
            } else {
                assertNull(f, "a cave " + depth + " deep is bottomless to a footing");
            }
        }
    }

    @Test
    void theWritesAreCappedEvenOverTheDeepestCaves() {
        for (Kind k : PLANNED) {
            for (int rot = 0; rot < 4; rot++) {
                for (long seed = 0; seed < 30; seed++) {
                    Plan p = plan(k, rot, seed);
                    Fake w = new Fake();
                    w.height = (x, z) -> TOP - 2 + Math.floorMod(x + z, 3) / 2;              // a bumpy surface, never more than two apart
                    Fit f = MawFit.scan(p, w, CX, CZ);
                    if (f != null) assertTrue(f.places().size() <= MawFit.MAX_WRITES, k + " writes " + f.places().size());
                }
            }
        }
        assertTrue(MawFit.MAX_WRITES >= MawShapes.MAX_PUTS);
    }

    @Test
    void footingsAreWrittenFirstAndOnlyUnderStone() {
        Plan p = plan(Kind.RUINS, 0, 8);
        Fake w = new Fake();
        w.height = (x, z) -> x > CX + 1 ? TOP - 2 : TOP;
        Fit f = MawFit.scan(p, w, CX, CZ);
        assertNotNull(f);
        Map<String, Integer> lowest = new HashMap<>();
        for (Put q : p.puts()) if (q.mat().stone()) lowest.merge(q.x() + "," + q.z(), q.y(), Math::min);
        int firstOfPlan = -1;
        List<Place> places = f.places();
        for (int i = 0; i < places.size(); i++) {
            Place q = places.get(i);
            boolean fromPlan = p.puts().stream().anyMatch(u -> CX + u.x() == q.x() && TOP + u.y() == q.y() && CZ + u.z() == q.z());
            if (fromPlan && firstOfPlan < 0) firstOfPlan = i;
            if (firstOfPlan < 0) {
                Integer low = lowest.get((q.x() - CX) + "," + (q.z() - CZ));
                assertNotNull(low, "a footing is under a column of stone: " + q);
                assertTrue(q.y() < TOP + low, "and under its lowest block: " + q);
            } else {
                assertTrue(fromPlan, "after the first block of the plan only the plan follows: " + q);
            }
        }
        assertTrue(firstOfPlan > 0, "there were footings, and they come first");
    }

    // ---------------------------------------------------------------- one after another
    @Test
    void aManifestationIsNotBuiltOverAnother() {
        for (Kind k : PLANNED) {
            Plan p = plan(k, 0, 5);
            Fake w = new Fake();
            Fit first = MawFit.scan(p, w, CX, CZ);
            assertNotNull(first);
            for (Place q : first.places()) w.set(q.x(), q.y(), q.z(), q.mat() == Mat.AIR ? Cell.AIR : Cell.OTHER);   // what was written is now someone's block
            assertNull(MawFit.scan(plan(k, 0, 6), w, CX, CZ), k + " on top of itself");
            assertNull(MawFit.scan(plan(Kind.TOWER, 1, 6), w, CX + 2, CZ + 1), "and not on its edge either");
            assertNotNull(MawFit.scan(plan(Kind.TOWER, 1, 6), w, CX + 30, CZ), "but a block away is another place");
        }
    }
}
