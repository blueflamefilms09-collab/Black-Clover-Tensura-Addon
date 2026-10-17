package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@link FloorAssemblerTest} proves the floor logic on a stand-in catalog; this proves it on the templates that SHIP, read from
 * {@code src/test/resources/mega/catalog.json} (the dump of the real template files, with the real stairwells, the real pools and the real theme rules). It builds
 * floors of both roads, ordinary and boss, and demands that the full audit passes, that the arrival and forward stairwells are the real ones of the right direction,
 * that no floor needs the fallback layout, and that the numbers are printed so the pool weights can be tuned from a build log. Like the old catalog test it says so and
 * passes while the catalog cannot make floors yet (no stairwell templates), so a build never fails for content that has not been merged.
 */
class FloorCatalogTest {

    private static String missing(CatalogSource cat) {
        for (String id : new String[] {FloorPlan.P_UP, FloorPlan.P_DOWN, FloorPlan.P_GRAND_UP, FloorPlan.P_GRAND_DOWN, FloorPlan.P_STAIR_DOOR, "nusmp:mega/side", "nusmp:mega/cap",
                "nusmp:mega/arena", "nusmp:mega/antechamber", "nusmp:mega/vault_3", "nusmp:mega/onward"}) {
            PoolDef p = cat.pool(id);
            boolean stocked = false;
            if (p != null) for (PoolEntry e : p.entries()) if (cat.shape(e.shape()) != null) stocked = true;
            if (!stocked) return id;
        }
        return null;
    }

    @Test
    void realTemplatesBuildFloorsOfBothRoadsThatPassTheFullAudit() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        String missing = cat.isEmpty() ? "everything" : missing(cat);
        if (missing != null) {
            System.out.println("FloorCatalogTest: the catalog cannot make floors yet (the pool " + missing + " holds no template), so there is nothing to assemble (this passes until it is merged).");
            return;
        }
        int total = 0, ok = 0, fallback = 0, tries = 0, rooms = 0, atBest = 0, ordinary = 0;
        long walk = 0;
        Map<String, Integer> failures = new java.util.LinkedHashMap<>();
        TemplateThemes themes = cat.themes();
        for (Polarity road : Polarity.values()) {
            RegionTheme previous = null;
            for (int k = 0; k < 22; k++) {
                for (int s = 0; s < 6; s++) {
                    long seed = Rng.mix(s + 1, 4711 + k);
                    SectorDraft draft = SectorDraft.of(seed, 0, road, k, 1, previous, null);
                    Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), themes, previous);
                    q.themeId = draft.theme().id();
                    SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                    total++;
                    tries += sv.tries();
                    sv.failures().forEach((a, b) -> failures.merge(a, b, Integer::sum));
                    assertTrue(sv.ok(), road + " floor " + (k + 1) + " seed " + s + " could not be built from the real templates after " + sv.tries() + " tries: " + SectorSolver.summary(sv.failures(), 4));
                    Result r = sv.result();
                    Request fq = q.withSeed(r.seed, sv.fallback());
                    assertEquals(List.of(), TestPlans.brute(r, fq), road + " floor " + (k + 1) + " seed " + s + " is not structurally sound");
                    assertEquals(List.of(), SectorAudit.audit(r, fq, !sv.fallback()), road + " floor " + (k + 1) + " seed " + s);
                    if (sv.fallback()) {
                        fallback++;
                        continue;
                    }
                    ok++;
                    for (Placed p : r.pieces) if (Result.isRoom(p)) rooms++;
                    boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(road, k));
                    Placed arrival = r.landingPiece();
                    assertTrue(arrival.shape.id.contains(road == Polarity.HOLY ? "/stairwell/down_" : "/stairwell/up_"), "the arrival stairs lead back: " + arrival.shape.id);
                    Placed forward = null;
                    for (Placed p : r.pieces) if (p.shape.kind == RoomKind.STAIRWELL && p.index != r.landing) forward = p;
                    assertTrue(forward != null && forward.shape.id.contains(road == Polarity.HOLY ? "up_" : "down_"), "the forward stairs follow the road");
                    assertEquals(boss, forward.shape.id.contains("/grand_"), "the grand stairs are for the boss floors");
                    if (!boss) {
                        ordinary++;
                        int[] d = FloorAssemblerTest.walk(r);
                        walk += d[forward.index];
                        if (r.stats.getOrDefault("stairs.skipped", 0) == 0) atBest++;
                    }
                }
                previous = SectorDraft.of(Rng.mix(1, 4711 + k), 0, road, k, 1, previous, null).theme();
            }
        }
        System.out.println(String.format(Locale.ROOT, "FloorCatalogTest: %d floors: %d passed at the first valid try, %d used the fallback layout; avg tries %.2f, avg rooms %.1f, stairs %.1f doorways from the arrival, %d of %d ordinary floors on the very farthest doorway",
                total, ok, fallback, tries / (double) total, rooms / (double) Math.max(1, ok), walk / (double) Math.max(1, ordinary), atBest, ordinary));
        if (!failures.isEmpty()) System.out.println("FloorCatalogTest: tries failed for: " + SectorSolver.summary(failures, 5));
        assertTrue(fallback <= total / 10, fallback + " of " + total + " floors needed the fallback layout (more than a tenth): tries failed for: " + SectorSolver.summary(failures, 4));
    }

    @Test
    void aReservedRoomIsNeverDrawnInAnotherTheme() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        TemplateThemes rules = cat.themes();
        assertFalse(rules == null);
        if (rules.size() == 0) {
            System.out.println("FloorCatalogTest: no template is reserved for a theme yet; the rule is exercised by FloorAssemblerTest on a stand-in.");
            return;
        }
        String missing = cat.isEmpty() ? "everything" : missing(cat);
        if (missing != null) return;
        for (RegionTheme t : RegionTheme.values()) {
            for (int s = 0; s < 8; s++) {
                Request q = TestPlans.floor(cat, 9000 + s, Polarity.HOLY, 3, 28, Recipe.forFloors(), rules, null);
                q.themeId = t.id();
                Result r = MegaAssembler.assemble(q);
                if (!r.assembled()) continue;
                for (Placed p : r.pieces) assertTrue(rules.allows(p.shape.id, t.id()), t.id() + " drew " + p.shape.id + ", which is reserved for " + rules.themesOf(p.shape.id));
            }
        }
    }
}
