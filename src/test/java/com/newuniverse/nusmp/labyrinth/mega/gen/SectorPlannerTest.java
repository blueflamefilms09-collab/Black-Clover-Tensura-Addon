package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: where a sector goes is the one decision that cannot be repaired later (two sectors that overlap overwrite each other for ever), so it is checked
 * with numbers worked out by hand for each direction, with the edge cases (a seal at the region's edge, a seal deep inside it, an obstacle that needs a
 * longer tunnel, an obstacle the tunnel would cross), and then with ten thousand random expansions in which no two regions may ever come closer than the
 * gap. The Way Onward policy is checked for what it is for: an impossible seal costs -1, a seal whose next region is walled in costs more than an open one.
 */
class SectorPlannerTest {

    /** Clear cells between two boxes in the horizontal plane (0 when they touch or overlap). */
    private static int gap(Box a, Box b) {
        int dx = Math.max(0, Math.max(a.minX() - b.maxX(), b.minX() - a.maxX()) - 1);
        int dz = Math.max(0, Math.max(a.minZ() - b.maxZ(), b.minZ() - a.maxZ()) - 1);
        return Math.max(dx, dz);
    }

    @Test
    void regionsAreSquaresCentredOnTheDoorAndStartAtItsPlane() {
        Vec e = new Vec(0, SectorPlanner.DOOR_Y, 0);
        assertEquals(65, SectorPlanner.DOOR_Y);
        assertEquals(new Box(0, 56, -85, 170, 103, 85), SectorPlanner.regionFor(e, Facing.EAST));
        assertEquals(new Box(-170, 56, -85, 0, 103, 85), SectorPlanner.regionFor(e, Facing.WEST));
        assertEquals(new Box(-85, 56, 0, 85, 103, 170), SectorPlanner.regionFor(e, Facing.SOUTH));
        assertEquals(new Box(-85, 56, -170, 85, 103, 0), SectorPlanner.regionFor(e, Facing.NORTH));
        for (Facing f : Facing.values()) {
            Box b = SectorPlanner.regionFor(new Vec(37, 65, -1000), f);
            assertEquals(171, b.sizeX());
            assertEquals(171, b.sizeZ());
            assertEquals(48, b.sizeY());
        }
        assertEquals(171, MegaGrid.blocks(MegaGrid.REGION_MODULES));
    }

    @Test
    void theFirstSectorStartsAtTheFixedOriginAndGrowsEast() {
        SectorPlanner.Plan p = SectorPlanner.first(12345);
        assertEquals(0, p.id());
        assertEquals(new Vec(0, 65, 0), p.entry());
        assertEquals(new Vec(0, 64, 0), p.entryFloor(), "Sector.entry is the floor cell below the door");
        assertEquals(Facing.EAST, p.forward());
        assertEquals(0, p.linkModules());
        assertEquals(SectorPlanner.regionFor(p.entry(), Facing.EAST), p.region());
        assertEquals(p.seed(), SectorPlanner.first(12345).seed());
        assertNotEquals(p.seed(), SectorPlanner.first(12346).seed());
    }

    @Test
    void seedsAreDistinctPerSectorAndPerTry() {
        Set<Long> seen = new HashSet<>();
        for (int id = 0; id < 50; id++) {
            long s = SectorPlanner.sectorSeed(777, id);
            for (int t = 0; t < 30; t++) assertTrue(seen.add(SectorPlanner.trySeed(s, t)), "seed collision at sector " + id + " try " + t);
        }
        assertEquals(SectorPlanner.sectorSeed(777, 3), SectorPlanner.sectorSeed(777, 3));
    }

    @Test
    void theGapIsAtLeastTheContractsAndTwiceTheCapReach() {
        assertTrue(SectorPlanner.GAP >= MegaGrid.REGION_GAP);
        assertTrue(SectorPlanner.GAP >= 2 * MegaGrid.CAP_MARGIN, "the caps of two neighbours cannot meet");
        assertEquals(16, SectorPlanner.GAP);
    }

    @Test
    void tunnelBoxesAreOneModuleWideAndStartInFrontOfTheSeal() {
        Box t = SectorPlanner.tunnelBox(new Vec(170, 65, 10), Facing.EAST, 2);
        assertEquals(new Box(171, 56, 6, 188, 103, 14), t);
        assertEquals(new Box(-188, 56, 6, -171, 103, 14), SectorPlanner.tunnelBox(new Vec(-170, 65, 10), Facing.WEST, 2));
        assertEquals(new Box(-9, 56, 86, -1, 103, 103), SectorPlanner.tunnelBox(new Vec(-5, 65, 85), Facing.SOUTH, 2));
        assertEquals(new Box(1, 56, -100, 9, 103, -83), SectorPlanner.tunnelBox(new Vec(5, 65, -82), Facing.NORTH, 2));
    }

    @Test
    void planNextAtTheEdgeNeedsTheShortestBridge() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);          // x 0..170, z -85..85
        SectorPlanner.Next n = SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(), 40);
        assertNotNull(n);
        assertEquals(2, n.linkModules(), "16 free cells need two 9-block segments");
        assertEquals(new Vec(189, 65, 10), n.entry());
        assertEquals(Facing.EAST, n.forward());
        assertEquals(new Box(189, 56, -75, 359, 103, 95), n.region());
        assertEquals(18, gap(r0, n.region()));
        assertEquals(SectorPlanner.tunnelBox(new Vec(170, 65, 10), Facing.EAST, 2), n.tunnel());
        assertEquals(2, SectorPlanner.linkModulesBetween(new Vec(170, 65, 10), Facing.EAST, n.entry()));
    }

    @Test
    void planNextFromDeepInsideTheRegionBridgesTheRestOfIt() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        SectorPlanner.Next n = SectorPlanner.planNext(new Vec(100, 65, 0), Facing.EAST, r0, List.of(), 40);
        assertEquals(10, n.linkModules(), "70 cells of region and 16 of gap: ceil(86 / 9)");
        assertEquals(new Vec(191, 65, 0), n.entry());
        assertEquals(20, gap(r0, n.region()));
    }

    @Test
    void planNextWorksInEveryDirection() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        SectorPlanner.Next south = SectorPlanner.planNext(new Vec(76, 65, 85), Facing.SOUTH, r0, List.of(), 40);
        assertEquals(2, south.linkModules());
        assertEquals(new Box(-9, 56, 104, 161, 103, 274), south.region());
        SectorPlanner.Next north = SectorPlanner.planNext(new Vec(76, 65, -85), Facing.NORTH, r0, List.of(), 40);
        assertEquals(2, north.linkModules());
        assertEquals(new Box(-9, 56, -274, 161, 103, -104), north.region());
        SectorPlanner.Next west = SectorPlanner.planNext(new Vec(0, 65, -10), Facing.WEST, r0, List.of(), 40);
        assertEquals(2, west.linkModules());
        assertEquals(new Box(-189, 56, -95, -19, 103, 75), west.region());
        for (SectorPlanner.Next n : List.of(south, north, west)) assertEquals(18, gap(r0, n.region()));
    }

    @Test
    void anObstacleBesideTheRunMakesTheTunnelLongerAndTooShortALimitGivesUp() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        SectorPlanner.Obstacle beside = SectorPlanner.Obstacle.region(new Box(200, 56, 60, 260, 103, 120));      // grown by 16: x 184..276, z 44..136
        SectorPlanner.Next n = SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(beside), 40);
        assertNotNull(n);
        assertEquals(12, n.linkModules(), "the region must start beyond x 276: 171 + 9 * 12 = 279");
        assertTrue(gap(n.region(), beside.box()) >= SectorPlanner.GAP);
        assertNull(SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(beside), 10), "a tunnel limited to 10 segments cannot get round it");
    }

    @Test
    void anObstacleOnTheRunsPathMeansNoRoomAtAll() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        SectorPlanner.Obstacle ahead = SectorPlanner.Obstacle.region(new Box(230, 56, -10, 290, 103, 30));
        assertNull(SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(ahead), 60), "the straight tunnel would run through it");
        SectorPlanner.Obstacle tunnelAcross = SectorPlanner.Obstacle.tunnel(new Box(190, 56, -50, 198, 103, 50));
        assertNull(SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(tunnelAcross), 60), "nor may it cross another tunnel");
    }

    @Test
    void linkModulesBetweenIsTheInverseOfPlacingTheLandingBehindTheTunnel() {
        for (Facing f : Facing.values()) {
            for (int m = 1; m <= 12; m++) {
                Vec seal = new Vec(-33, 65, 41);
                Vec entry = seal.step(f).step(f, 9 * m);
                assertEquals(m, SectorPlanner.linkModulesBetween(seal, f, entry));
            }
        }
        assertEquals(0, SectorPlanner.linkModulesBetween(new Vec(0, 65, 0), Facing.EAST, new Vec(1, 65, 0)));
    }

    @Test
    void regionsNeverComeCloserThanTheGapOverTenThousandRandomExpansions() {
        Rng rng = new Rng(2024);
        int expansions = 0;
        long minRegionGap = Long.MAX_VALUE, minTunnelGap = Long.MAX_VALUE;
        for (int walk = 0; walk < 300; walk++) {
            List<Box> regions = new ArrayList<>();
            List<Box> tunnels = new ArrayList<>();
            List<SectorPlanner.Obstacle> obstacles = new ArrayList<>();
            Vec entry = new Vec(rng.nextInt(5000) - 2500, 65, rng.nextInt(5000) - 2500);
            Facing forward = Facing.byIndex(rng.nextInt(4));
            Box region = SectorPlanner.regionFor(entry, forward);
            regions.add(region);
            for (int step = 0; step < 40; step++) {
                Facing f = forward.turn(new int[] {0, 1, 3}[rng.nextInt(3)]);
                Vec seal = randomSeal(rng, region, f);
                List<SectorPlanner.Obstacle> others = new ArrayList<>(obstacles);
                SectorPlanner.Next n = SectorPlanner.planNext(seal, f, region, others, 40);
                if (n == null) {
                    boolean any = false;
                    for (int tries = 0; tries < 6 && !any; tries++) {
                        f = forward.turn(new int[] {0, 1, 3}[rng.nextInt(3)]);
                        seal = randomSeal(rng, region, f);
                        n = SectorPlanner.planNext(seal, f, region, others, 40);
                        any = n != null;
                    }
                    if (!any) break;
                }
                expansions++;
                assertEquals(n.linkModules(), SectorPlanner.linkModulesBetween(seal, f, n.entry()));
                assertEquals(SectorPlanner.regionFor(n.entry(), n.forward()), n.region());
                obstacles.add(SectorPlanner.Obstacle.region(region));
                obstacles.add(SectorPlanner.Obstacle.tunnel(n.tunnel()));
                for (Box old : regions) {
                    int g = gap(old, n.region());
                    minRegionGap = Math.min(minRegionGap, g);
                    assertTrue(g >= MegaGrid.REGION_GAP, "walk " + walk + " step " + step + ": regions " + old + " and " + n.region() + " are only " + g + " apart");
                }
                for (Box t : tunnels) {
                    int g = gap(t, n.region());
                    minTunnelGap = Math.min(minTunnelGap, g);
                    assertTrue(g >= MegaGrid.CAP_MARGIN, "a region " + g + " from an older tunnel");
                    assertTrue(gap(t, n.tunnel()) >= 1 || !t.overlaps(n.tunnel()), "tunnels never overlap");
                }
                for (Box old : regions) {
                    if (old.equals(region)) continue;                                  // the tunnel leaves its own region by design
                    assertTrue(gap(old, n.tunnel()) >= MegaGrid.CAP_MARGIN, "a tunnel " + gap(old, n.tunnel()) + " from an older region");
                }
                regions.add(n.region());
                tunnels.add(n.tunnel());
                region = n.region();
                forward = n.forward();
            }
        }
        assertTrue(expansions > 8000, "enough expansions were exercised: " + expansions);
        assertTrue(minRegionGap >= MegaGrid.REGION_GAP);
    }

    private static Vec randomSeal(Rng rng, Box region, Facing f) {
        // a seal somewhere in the part of the region the boss chain can reach: on the 9-grid, at least one module in from every edge, pointing out through f
        int mods = MegaGrid.REGION_MODULES;
        int a = 1 + rng.nextInt(mods - 2), b = 1 + rng.nextInt(mods - 2);
        int x = region.minX() + a * MegaGrid.MODULE + MegaGrid.CENTER, z = region.minZ() + b * MegaGrid.MODULE + MegaGrid.CENTER;
        return new Vec(x, SectorPlanner.DOOR_Y, z);
    }

    @Test
    void theVariantOrderIsAPermutationOfTheThreeAndSeeded() {
        Set<String> expected = Set.of(SectorPlanner.POOL_STRAIGHT, SectorPlanner.POOL_LEFT, SectorPlanner.POOL_RIGHT);
        Set<List<String>> orders = new HashSet<>();
        for (long s = 0; s < 100; s++) {
            List<String> o = SectorPlanner.variantOrder(s);
            assertEquals(expected, new HashSet<>(o));
            assertEquals(3, o.size());
            assertEquals(o, SectorPlanner.variantOrder(s));
            orders.add(o);
        }
        assertEquals(6, orders.size(), "all six orders occur");
    }

    @Test
    void thePolicyRejectsAWalledInSealAndPrefersOpenGround() {
        Box r0 = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        OnwardPolicy open = SectorPlanner.policy(1, r0, List.of(), 40);
        assertEquals(3, open.variants().size());
        int cost = open.cost(new Vec(170, 65, 10), Facing.EAST);
        assertEquals(2, cost, "an open world: the shortest tunnel, three exits, leading outward");
        // walls on three sides of the region that a seal facing east would lead to
        SectorPlanner.Next n = SectorPlanner.planNext(new Vec(170, 65, 10), Facing.EAST, r0, List.of(), 40);
        List<SectorPlanner.Obstacle> ring = new ArrayList<>();
        Box r1 = n.region();
        ring.add(SectorPlanner.Obstacle.region(new Box(r1.maxX() + 20, 56, r1.minZ(), r1.maxX() + 190, 103, r1.maxZ())));        // east of it
        ring.add(SectorPlanner.Obstacle.region(new Box(r1.minX(), 56, r1.minZ() - 190, r1.maxX(), 103, r1.minZ() - 20)));        // north
        ring.add(SectorPlanner.Obstacle.region(new Box(r1.minX(), 56, r1.maxZ() + 20, r1.maxX(), 103, r1.maxZ() + 190)));        // south
        OnwardPolicy walled = SectorPlanner.policy(1, r0, ring, 40);
        int walledCost = walled.cost(new Vec(170, 65, 10), Facing.EAST);
        assertTrue(walledCost >= SectorPlanner.EXIT_PENALTY * 2, "a region with no way on costs a lot: " + walledCost);
        assertTrue(walledCost > cost);
        // a seal that leads straight into an older sector is impossible
        OnwardPolicy blocked = SectorPlanner.policy(1, r0, List.of(SectorPlanner.Obstacle.region(new Box(230, 56, -10, 290, 103, 30))), 40);
        assertEquals(-1, blocked.cost(new Vec(170, 65, 10), Facing.EAST));
    }

    @Test
    void exclusionsForKeepsNearObstaclesGrownByTheCapReachAndDropsFarOnes() {
        Box region = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        SectorPlanner.Obstacle near = SectorPlanner.Obstacle.region(new Box(190, 56, -85, 360, 103, 85));
        SectorPlanner.Obstacle far = SectorPlanner.Obstacle.region(new Box(2000, 56, 2000, 2170, 103, 2170));
        List<Box> ex = SectorPlanner.exclusionsFor(region, List.of(near, far));
        assertEquals(1, ex.size());
        assertEquals(near.box().inflateXZ(MegaGrid.CAP_MARGIN), ex.get(0));
        assertFalse(ex.get(0).overlaps(region), "a neighbour at the planned gap never reaches into the region");
    }
}
