package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the solver is what stands between a random plan and the world, so its three exits are tested separately: a plan that passes the audit is returned at
 * once with the try that produced it; a catalog whose full audit can never pass (a recipe that asks for the impossible) falls back to the smallest sector
 * that is still a sector and says so; a catalog that cannot make even that fails with the reasons the tries gave. A dead-end seal is the fourth case: the
 * sector is valid but nothing can follow it, which the solver reports without losing the sector. The solve is deterministic, because a restart re-solves.
 */
class SectorSolverTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    @Test
    void aPlanThatPassesTheAuditIsReturnedWithItsTryNumber() {
        SectorSolver.Solved s = SectorSolver.solve(TestPlans.first(CAT, 3), 24);
        assertTrue(s.ok());
        assertFalse(s.fallback());
        assertFalse(s.deadEnd());
        assertTrue(s.tries() >= 1 && s.tries() <= 24);
        assertEquals(List.of(), s.audit());
        assertEquals(List.of(), SectorAudit.audit(s.result(), TestPlans.first(CAT, 3).withSeed(s.result().seed, false), true));
    }

    @Test
    void theSolveIsDeterministic() {
        for (long seed = 0; seed < 6; seed++) {
            SectorSolver.Solved a = SectorSolver.solve(TestPlans.first(CAT, seed), 24), b = SectorSolver.solve(TestPlans.first(CAT, seed), 24);
            assertEquals(a.tries(), b.tries());
            assertEquals(TestPlans.fingerprint(a.result()), TestPlans.fingerprint(b.result()));
        }
    }

    @Test
    void anImpossibleRecipeFallsBackToTheChainOnlyLayout() {
        Recipe rc = new Recipe();
        rc.minRooms = 100000;                                 // the full audit can never pass
        Request q = TestPlans.first(CAT, 4, rc, 450);
        SectorSolver.Solved s = SectorSolver.solve(q, 3);
        assertTrue(s.ok(), s.failures().toString());
        assertTrue(s.fallback());
        assertEquals(4, s.tries(), "three strict tries and the fallback");
        assertFalse(s.audit().isEmpty(), "what the strict tries failed on is kept for the log");
        Result r = s.result();
        assertTrue(r.fallback);
        int rooms = 0;
        for (Placed p : r.pieces) if (!p.shape.isCap() && !p.link) rooms++;
        assertTrue(rooms <= 8, "the fallback layout is the Landing and the boss chain: " + rooms);
        Request fq = q.withSeed(r.seed, true);
        assertEquals(List.of(), SectorAudit.audit(r, fq, false));
        assertEquals(List.of(), TestPlans.brute(r, fq));
    }

    @Test
    void aCatalogThatCannotMakeASectorFailsWithTheReasons() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pools().remove(MegaGrid.P_ARENA);
        SectorSolver.Solved s = SectorSolver.solve(TestPlans.first(c, 4), 5);
        assertFalse(s.ok());
        assertNull(s.result());
        assertTrue(s.fallback());
        assertEquals(6, s.tries());
        assertFalse(s.failures().isEmpty());
        String summary = SectorSolver.summary(s.failures(), 3);
        assertTrue(summary.contains("no boss chain"), summary);
        assertTrue(summary.startsWith("5x ") || summary.startsWith("6x ") || summary.contains("x no layout"), summary);
    }

    @Test
    void aSectorWhoseSealFacesOlderSectorsIsKeptAsADeadEnd() {
        Request q = TestPlans.first(CAT, 2);
        q.onward = new OnwardPolicy() {
            @Override
            public List<String> variants() { return List.of(SectorPlanner.POOL_STRAIGHT, SectorPlanner.POOL_LEFT, SectorPlanner.POOL_RIGHT); }

            @Override
            public int cost(Vec sealCell, Facing facing) { return -1; }
        };
        SectorSolver.Solved s = SectorSolver.solve(q, 4);
        assertTrue(s.ok(), "the sector is still built: " + s.failures());
        assertTrue(s.deadEnd());
        assertFalse(s.fallback());
        assertEquals(1, s.tries(), "the first valid plan is remembered; no better one exists");
        assertTrue(s.result().expansion.deadEnd());
        assertEquals(MegaAssembler.DEAD_END, s.result().expansion.cost());
    }

    private static Result asDeadEnd(Result r) {
        Result.Expansion e = r.expansion;
        Result.Expansion dead = new Result.Expansion(e.piece(), e.seal(), e.facing(), e.w(), e.h(), e.d(), e.strip(), MegaAssembler.DEAD_END);
        return new Result(r.region, r.pieces, r.loops, dead, null, r.warnings, r.seed, r.fallback, r.landing, r.stats);
    }

    @Test
    void aLiveContinuationBeatsADeadEndOnALaterTry() {
        Request q = TestPlans.first(CAT, 6);
        Result live = SectorSolver.solve(q, 24).result();
        Result dead = asDeadEnd(live);
        int[] calls = {0};
        SectorSolver.Solved s = SectorSolver.solve(q, 6, r -> ++calls[0] < 3 ? dead : live);
        assertTrue(s.ok());
        assertFalse(s.deadEnd());
        assertEquals(3, s.tries(), "the first two tries gave valid plans that nothing can follow; the third has a way on");
        assertSame(live, s.result());
        assertEquals(3, calls[0], "and the search stops there");
    }

    @Test
    void ifEveryTryIsADeadEndTheFirstOneIsKept() {
        Request q = TestPlans.first(CAT, 6);
        Result dead = asDeadEnd(SectorSolver.solve(q, 24).result());
        int[] calls = {0};
        SectorSolver.Solved s = SectorSolver.solve(q, 4, r -> {
            calls[0]++;
            return dead;
        });
        assertTrue(s.ok());
        assertTrue(s.deadEnd());
        assertFalse(s.fallback());
        assertEquals(1, s.tries(), "reported as the try that produced it");
        assertEquals(4, calls[0], "all the strict tries were spent looking for a live one");
        assertSame(dead, s.result());
    }

    @Test
    void aFailedTryFollowedByASuccessReturnsTheSuccess() {
        Request q = TestPlans.first(CAT, 6);
        Result live = SectorSolver.solve(q, 24).result();
        int[] calls = {0};
        SectorSolver.Solved s = SectorSolver.solve(q, 5, r -> ++calls[0] < 3 ? Result.failed(q.region, "no boss chain: doors=0", List.of(), r.seed, false, java.util.Map.of()) : live);
        assertTrue(s.ok());
        assertEquals(3, s.tries());
        assertEquals(2, (int) s.failures().values().iterator().next(), "the two failed tries are tallied together");
        assertFalse(s.deadEnd());
    }

    @Test
    void numbersInFailureTextsAreFoldedSoTheSummaryTalliesThem() {
        assertEquals("only # rooms (the recipe wants #)", SectorSolver.stripNumbers("only 12 rooms (the recipe wants 40)"));
        assertEquals("", SectorSolver.summary(java.util.Map.of(), 3));
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 7);
        m.put("c", 3);
        assertEquals("7x b | 3x c", SectorSolver.summary(m, 2));
    }
}
