package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code /multiverse mega regen} and the repair after a crash re-plan a sector from saved data and place the result over the old one, which only works if
 * the re-plan is the SAME plan. The risky case is an old sector that already has neighbours: when it was first planned only the earlier sectors existed, but
 * when it is re-planned the later ones (above all the next sector, whose tunnel starts at the old sector's own seal) are in the saved data too. These tests
 * grow whole simulated dungeons, then replay every sector from the data a saved sector would have (region, entry door, the seal it hangs from, its seed)
 * with the obstacle rule the game uses ({@link WorldObstacles}) and demand the identical plan, piece by piece; and they show that the naive rule (everything
 * else is an obstacle) really does change the plan, which is why the rule exists.
 */
class RegenReplayTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    private static List<WorldObstacles.Known> known(SimWorld w) {
        List<WorldObstacles.Known> out = new ArrayList<>();
        for (SimWorld.Entry e : w.sectors) out.add(new WorldObstacles.Known(e.id(), e.plan().region(), e.prevSeal(), e.prevFacing(), e.plan().entry()));
        return out;
    }

    private static SectorSolver.Solved replay(SimWorld w, int j, List<SectorPlanner.Obstacle> others) {
        SimWorld.Entry e = w.sectors.get(j);
        Box prevRegion = j == 0 ? null : w.sectors.get(j - 1).plan().region();
        SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(CAT, w.recipe, w.maxPieces, w.maxLinkModules, e.plan().seed(), e.plan().entry(),
                e.plan().forward(), e.plan().region(), e.prevSeal(), e.prevFacing(), prevRegion, others, e.relics()));
        assertTrue(p.ok(), "sector " + j + ": " + p.failure());
        return SectorSolver.solve(p.request(), w.tries);
    }

    @Test
    void everySectorOfAGrownDungeonReplaysToTheIdenticalPlan() {
        int replayed = 0;
        for (long seed = 0; seed < 5; seed++) {
            SimWorld w = new SimWorld(CAT, 7000 + seed, new Recipe(), 450, 40, 24);
            w.grow(8);
            List<WorldObstacles.Known> known = known(w);
            for (int j = 0; j < w.sectors.size(); j++) {
                SectorSolver.Solved again = replay(w, j, WorldObstacles.of(known, j, -1));
                assertTrue(again.ok(), "seed " + seed + " sector " + j + " replays");
                assertEquals(TestPlans.fingerprint(w.sectors.get(j).result()), TestPlans.fingerprint(again.result()), "seed " + seed + " sector " + j);
                assertEquals(w.sectors.get(j).solved().tries(), again.tries(), "the same try wins");
                replayed++;
            }
        }
        assertTrue(replayed >= 30, "enough sectors were replayed: " + replayed);
    }

    @Test
    void lettingTheLaterSectorsInChangesThePlanOfAnOlderOneWhichIsWhyTheyAreKeptOut() {
        int changed = 0, tested = 0;
        for (long seed = 0; seed < 5; seed++) {
            SimWorld w = new SimWorld(CAT, 7000 + seed, new Recipe(), 450, 40, 24);
            w.grow(8);
            List<WorldObstacles.Known> known = known(w);
            for (int j = 0; j + 1 < w.sectors.size(); j++) {
                SectorSolver.Solved naive = replay(w, j, WorldObstacles.of(known, Integer.MAX_VALUE, j));
                tested++;
                if (!naive.ok() || !TestPlans.fingerprint(w.sectors.get(j).result()).equals(TestPlans.fingerprint(naive.result()))) changed++;
            }
        }
        assertTrue(tested >= 30, "enough sectors were tested: " + tested);
        System.out.println("RegenReplayTest: with every other sector as an obstacle " + changed + " of " + tested + " replays came out different from the original.");
        assertTrue(changed > 0, "if this never changed anything the obstacle rule would not matter (tested " + tested + ")");
    }

    @Test
    void theObstacleRuleKeepsOnlyEarlierSectorsAndTheirTunnels() {
        SimWorld w = new SimWorld(CAT, 7100, new Recipe(), 450, 40, 24);
        w.grow(5);
        List<WorldObstacles.Known> known = known(w);
        assertEquals(0, WorldObstacles.of(known, 0, -1).size(), "nothing exists before sector 0");
        assertEquals(1, WorldObstacles.of(known, 1, -1).size(), "sector 1 sees sector 0's region only (sector 0 has no tunnel into it)");
        assertEquals(2 * 1 + 1, WorldObstacles.of(known, 2, -1).size(), "sector 2 sees two regions and the tunnel into sector 1");
        assertEquals(2 * 5 - 1, WorldObstacles.of(known, Integer.MAX_VALUE, -1).size(), "everything: five regions and four tunnels");
        assertEquals(2 * 5 - 2, WorldObstacles.of(known, Integer.MAX_VALUE, 3).size(), "one region skipped");
    }
}
