package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: since 1.55.0 the Mega-Dungeon is two roads in one dimension, the Holy chain at z 0 and the Abyssal chain {@code MegaGrid.LANE_Z} blocks away, with sector ids
 * that are global and therefore INTERLEAVE the two chains (Holy 0, Abyssal 1, Holy 2 ...). What can go wrong is quiet: a sector that takes the other road's
 * sectors for obstacles plans differently depending on how far the other road has grown; a replay after a crash that counts a later sector of the other road
 * changes the plan; a road that drifts to the other's lane. These tests grow both roads together in the simulator, through the same planner, tunnel builder and
 * assembler the game uses, and check the lanes, the chains, the floors and, above all, that every sector replays to the identical plan from the data a saved sector has.
 */
class RoadsTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    private static List<WorldObstacles.Known> known(SimWorld w) {
        List<WorldObstacles.Known> out = new ArrayList<>();
        for (SimWorld.Entry e : w.sectors) out.add(new WorldObstacles.Known(e.id(), e.plan().region(), e.prevSeal(), e.prevFacing(), e.plan().entry(), e.road()));
        return out;
    }

    @Test
    void bothRoadsGrowOnTheirOwnLanesWithInterleavedIds() {
        for (long seed = 0; seed < 4; seed++) {
            SimWorld w = new SimWorld(CAT, 9100 + seed, new Recipe(), 450, 40, 24);
            boolean all = w.growRoads(6);
            if (!all) assertTrue(w.stopped.startsWith("dead end"), "a world may only stop at a dead end: " + w.stopped);
            assertTrue(w.chain(Polarity.HOLY).size() >= 3 && w.chain(Polarity.ABYSSAL).size() >= 3, "both roads grew: " + w.chain(Polarity.HOLY).size() + " and " + w.chain(Polarity.ABYSSAL).size());
            assertEquals(Polarity.HOLY, w.sectors.get(0).road());
            assertEquals(Polarity.ABYSSAL, w.sectors.get(1).road(), "the ids interleave");
            for (int i = 0; i < w.sectors.size(); i++) assertEquals(i, w.sectors.get(i).id(), "ids are global and in order");
            for (SimWorld.Entry e : w.sectors) {
                int lane = MegaGrid.laneZ(e.road());
                Box r = e.plan().region();
                assertTrue(Math.abs((r.minZ() + r.maxZ()) / 2 - lane) < MegaGrid.LANE_Z / 2, "sector " + e.id() + " stays near its own lane");
                for (Placed p : e.result().pieces) assertTrue(Math.abs(p.box.centerZ() - lane) < MegaGrid.LANE_Z / 2, "a piece of sector " + e.id() + " stays near its lane");
            }
            assertEquals(0, w.chain(Polarity.HOLY).get(0).plan().entry().z());
            assertEquals(MegaGrid.LANE_Z, w.chain(Polarity.ABYSSAL).get(0).plan().entry().z());
            for (Polarity road : Polarity.values()) {
                List<SimWorld.Entry> chain = w.chain(road);
                for (int i = 0; i < chain.size(); i++) assertEquals(i, chain.get(i).chainIndex(), "the chain index counts the road's own sectors");
            }
        }
    }

    @Test
    void noRegionOfOneRoadComesNearARegionOfTheOther() {
        SimWorld w = new SimWorld(CAT, 9200, new Recipe(), 450, 40, 24);
        w.growRoads(5);
        for (SimWorld.Entry a : w.chain(Polarity.HOLY)) {
            for (SimWorld.Entry b : w.chain(Polarity.ABYSSAL)) {
                assertTrue(a.plan().region().gapXZ(b.plan().region()) > 100_000, "sector " + a.id() + " and sector " + b.id());
            }
        }
        for (Polarity road : Polarity.values()) {
            List<SimWorld.Entry> chain = w.chain(road);
            for (int i = 0; i < chain.size(); i++) {
                for (int j = i + 1; j < chain.size(); j++) {
                    assertTrue(chain.get(i).plan().region().gapXZ(chain.get(j).plan().region()) >= MegaGrid.REGION_GAP, "the " + road.id() + " road's sectors " + i + " and " + j);
                }
            }
        }
    }

    @Test
    void aRoadPlansTheSameWhateverTheOtherRoadHasBuilt() {
        // the Holy road grown alone and the Holy road grown beside the Abyssal one must be the same sectors, piece by piece
        SimWorld alone = new SimWorld(CAT, 9300, new Recipe(), 450, 40, 24);
        alone.grow(5);
        SimWorld both = new SimWorld(CAT, 9300, new Recipe(), 450, 40, 24);
        both.growRoads(5);
        List<SimWorld.Entry> a = alone.chain(Polarity.HOLY), b = both.chain(Polarity.HOLY);
        int n = Math.min(a.size(), b.size());
        assertTrue(n >= 3, "enough sectors to compare: " + n);
        for (int i = 0; i < n; i++) {
            assertEquals(TestPlans.fingerprint(a.get(i).result()), TestPlans.fingerprint(b.get(i).result()), "the Holy road's sector " + i + " does not depend on the Abyssal road");
            assertEquals(a.get(i).plan().region(), b.get(i).plan().region());
        }
    }

    @Test
    void theAbyssalRoadIsTheHolyRoadMovedToItsLaneWhenTheSeedKeysMatch() {
        // the same plan seed on both lanes gives the same sector, only 300 000 blocks to the side: nothing in the geometry depends on where the lane is
        SectorPlanner.Plan holy = SectorPlanner.first(77, 0, Polarity.HOLY), abyss = SectorPlanner.first(77, 1, Polarity.ABYSSAL);
        SectorRequests.Prepared ph = SectorRequests.prepare(new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, 4242, holy.entry(), holy.forward(), holy.region(), null, null, null, List.of(), 3));
        SectorRequests.Prepared pa = SectorRequests.prepare(new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, 4242, abyss.entry(), abyss.forward(), abyss.region(), null, null, null, List.of(), 3));
        Result rh = MegaAssembler.assemble(ph.request()), ra = MegaAssembler.assemble(pa.request());
        assertTrue(rh.assembled() && ra.assembled());
        assertEquals(rh.pieces.size(), ra.pieces.size());
        for (int i = 0; i < rh.pieces.size(); i++) {
            Placed x = rh.pieces.get(i), y = ra.pieces.get(i);
            assertEquals(x.shape.id, y.shape.id);
            assertEquals(x.rot, y.rot);
            assertEquals(x.origin.add(0, 0, MegaGrid.LANE_Z), y.origin);
        }
    }

    @Test
    void everySectorOfTwoInterleavedRoadsReplaysToTheIdenticalPlan() {
        int replayed = 0;
        for (long seed = 0; seed < 3; seed++) {
            SimWorld w = new SimWorld(CAT, 9400 + seed, new Recipe(), 450, 40, 24);
            w.growRoads(5);
            List<WorldObstacles.Known> known = known(w);
            for (SimWorld.Entry e : w.sectors) {
                // what a saved sector knows: the sectors of its own road with a lower id, the seal it hangs from, its seed and its relic count
                SimWorld.Entry prev = e.prevId() < 0 ? null : w.sectors.get(e.prevId());
                SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(CAT, w.recipe, w.maxPieces, w.maxLinkModules, e.plan().seed(), e.plan().entry(), e.plan().forward(),
                        e.plan().region(), e.prevSeal(), e.prevFacing(), prev == null ? null : prev.plan().region(), WorldObstacles.of(known, e.id(), -1, e.road()), e.relics()));
                assertTrue(p.ok(), p.failure());
                SectorSolver.Solved again = SectorSolver.solve(p.request(), w.tries);
                assertTrue(again.ok(), "sector " + e.id() + " replays");
                assertEquals(TestPlans.fingerprint(e.result()), TestPlans.fingerprint(again.result()), "seed " + seed + " sector " + e.id() + " of the " + e.road().id() + " road");
                replayed++;
            }
        }
        assertTrue(replayed >= 20, "enough sectors were replayed: " + replayed);
    }

    @Test
    void theRoadFilterKeepsOnlyTheRoadsOwnEarlierSectorsAndTunnels() {
        SimWorld w = new SimWorld(CAT, 9500, new Recipe(), 450, 40, 24);
        assertTrue(w.growRoads(3), w.stopped);
        assertEquals(6, w.sectors.size());
        List<WorldObstacles.Known> known = known(w);
        assertEquals(0, WorldObstacles.of(known, 0, -1, Polarity.HOLY).size(), "nothing exists before sector 0");
        assertEquals(0, WorldObstacles.of(known, 1, -1, Polarity.ABYSSAL).size(), "the Abyssal road has nothing before its first sector (id 1): the Holy sector 0 is not its business");
        assertEquals(1, WorldObstacles.of(known, 1, -1, Polarity.HOLY).size(), "the Holy road's sector 0 region");
        assertEquals(1, WorldObstacles.of(known, 2, -1, Polarity.ABYSSAL).size(), "an Abyssal sector with id 2 would see the Abyssal sector 1's region only, not the Holy sector 0");
        int holyAll = WorldObstacles.of(known, Integer.MAX_VALUE, -1, Polarity.HOLY).size(), abyssAll = WorldObstacles.of(known, Integer.MAX_VALUE, -1, Polarity.ABYSSAL).size();
        assertEquals(2 * 3 - 1, holyAll, "three regions and two tunnels");
        assertEquals(2 * 3 - 1, abyssAll);
        assertEquals(holyAll + abyssAll, WorldObstacles.of(known, Integer.MAX_VALUE, -1).size(), "no road named: every road, as 1.54.0 asked");
        assertEquals(holyAll + abyssAll, WorldObstacles.of(known, Integer.MAX_VALUE, -1, null).size());
    }

    @Test
    void floorsContinueFromOneSectorToTheNextOnEachRoad() {
        SimWorld w = new SimWorld(CAT, 9600, new Recipe(), 450, 40, 24);
        w.growRoads(4);
        for (Polarity road : Polarity.values()) {
            List<SimWorld.Entry> chain = w.chain(road);
            for (int i = 0; i < chain.size(); i++) {
                int first = FloorMap.firstFloor(road, chain.get(i).chainIndex(), FloorMap.DEFAULT_PER_REGION), last = FloorMap.lastFloor(road, chain.get(i).chainIndex(), FloorMap.DEFAULT_PER_REGION);
                assertEquals(road.sign() * (10 * i + 1), first);
                assertEquals(road.sign() * (10 * i + 10), last);
                if (i > 0) assertEquals(FloorMap.lastFloor(road, i - 1, 10) + road.sign(), first, "the next sector starts one floor beyond the last");
            }
        }
    }
}
