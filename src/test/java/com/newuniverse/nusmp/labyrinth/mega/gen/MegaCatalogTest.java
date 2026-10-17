package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the assembler is only worth anything if it builds sectors out of the templates that SHIP, so this test assembles real sectors from
 * {@code src/test/resources/mega/catalog.json}, the dump of the real template and pool files that {@code tools/labyrinth/mega_catalog.py} writes, and
 * demands that the full audit passes for 200 seeds. While the other builders are still drawing templates the catalog is empty; then there is nothing to
 * assemble and the test says so and passes (a build must not fail because content has not arrived yet). The moment the catalog has any template it runs
 * for real, and a pool that is missing, a boss chain that cannot be built or a Way Onward without a seal marker fails it with the solver's own
 * explanation of what went wrong. The numbers it saw are printed so the pool weights can be tuned from a build log.
 */
class MegaCatalogTest {

    @Test
    void realTemplatesAssembleIntoSectorsThatPassTheAudit() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        if (cat.isEmpty()) {
            System.out.println("MegaCatalogTest: " + CatalogSource.DEFAULT + " holds no templates yet, so there is nothing to assemble (this passes until the templates are merged).");
            return;
        }
        int seeds = 200, ok = 0, fallback = 0, tries = 0;
        long pieces = 0, rooms = 0, loops = 0;
        Map<String, Integer> failures = new java.util.LinkedHashMap<>();
        java.util.Set<String> warnings = new java.util.LinkedHashSet<>();
        long t0 = System.nanoTime();
        for (int s = 0; s < seeds; s++) {
            long dungeonSeed = Rng.mix(s, 4711);
            SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed);
            int relics = RegionTheme.GRAVITY.relicCount(dungeonSeed, 0);
            SectorRequests.Prepared prep = SectorRequests.prepare(new SectorRequests.Inputs(cat, new Recipe(), 450, 40, plan.seed(), plan.entry(), plan.forward(), plan.region(),
                    null, null, null, List.of(), relics));
            assertTrue(prep.ok(), prep.failure());
            SectorSolver.Solved sv = SectorSolver.solve(prep.request(), 24);
            tries += sv.tries();
            sv.failures().forEach((k, v) -> failures.merge(k, v, Integer::sum));
            assertTrue(sv.ok(), "seed " + s + " could not be built from the real templates after " + sv.tries() + " tries: " + SectorSolver.summary(sv.failures(), 4));
            Result r = sv.result();
            warnings.addAll(r.warnings);
            Request q = prep.request().withSeed(r.seed, sv.fallback());
            assertEquals(List.of(), TestPlans.brute(r, q), "seed " + s + " is not structurally sound");
            assertEquals(List.of(), SectorAudit.audit(r, q, !sv.fallback()), "seed " + s);
            if (sv.fallback()) fallback++;
            else ok++;
            pieces += r.pieces.size();
            for (Placed p : r.pieces) if (Result.isRoom(p) && !p.link) rooms++;
            loops += r.loops.size();
        }
        double ms = (System.nanoTime() - t0) / 1e6 / seeds;
        System.out.println(String.format(Locale.ROOT, "MegaCatalogTest: %d templates, %d pools; %d seeds: %d passed the audit, %d used the fallback layout; avg pieces %.1f, rooms %.1f, loops %.1f, tries %.2f, %.1f ms per seed",
                cat.shapeCount(), cat.poolCount(), seeds, ok, fallback, pieces / (double) seeds, rooms / (double) seeds, loops / (double) seeds, tries / (double) seeds, ms));
        if (!failures.isEmpty()) System.out.println("MegaCatalogTest: tries failed for: " + SectorSolver.summary(failures, 5));
        for (String w : warnings) System.out.println("MegaCatalogTest warning: " + w);
        assertTrue(fallback <= seeds / 4, fallback + " of " + seeds + " seeds needed the fallback layout (more than a quarter): the pool weights or the templates need work; tries failed for: " + SectorSolver.summary(failures, 4));
    }

    @Test
    void realTemplatesGrowIntoALongerDungeon() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        if (cat.isEmpty()) {
            System.out.println("MegaCatalogTest: no templates yet, the growth check is skipped.");
            return;
        }
        int worlds = 6, sectors = 0;
        for (int s = 0; s < worlds; s++) {
            SimWorld w = new SimWorld(cat, Rng.mix(s, 815), new Recipe(), 450, 40, 24);
            w.grow(6);
            sectors += w.sectors.size();
            assertTrue(w.sectors.size() >= 4 || (w.stopped != null && w.stopped.startsWith("dead end")), "world " + s + " stopped after " + w.sectors.size() + " sectors: " + w.stopped);
            for (SimWorld.Entry e : w.sectors) if (e.solved().ok()) assertEquals(List.of(), TestPlans.brute(e.result(), e.prepared().request().withSeed(e.result().seed, e.solved().fallback())));
        }
        System.out.println("MegaCatalogTest: " + worlds + " worlds grew " + sectors + " sectors in total through the real planner and tunnels.");
    }
}
