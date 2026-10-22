package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "keep the current size and the connectivity graph" is proved on real plans, not only on single files. This plans floors of both roads (ordinary and boss, the cyclic loop planner
 * and dead-end pruner as the generator runs them on the real catalog) and, for every piece of every plan, resolves the rebuilt room that stands in for its shipped template
 * ({@link RoomVariants#assign}) and checks on the FILES that the plan is unchanged by it: the same piece count, and for every piece the rebuilt file has the same size and the same
 * sockets as the shipped shape the plan used (so the boxes, the joins, the loops and the walking graph computed from the plan are exactly those of the shipped rooms), the room graph
 * of the plan is connected from the arrival, and the choice is the same when asked twice. Also that rebuilt rooms really appear in plans (a plan is mostly rebuilt pieces) and that
 * the first copies of one template in a floor are all different rooms.
 */
class RoomsRebuiltPlanTest {
    @Test
    void thePlansAreUnchangedAndEveryPieceHasTheSameSizeAndSocketsInItsRebuiltRoom() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        assertTrue(!cat.isEmpty(), "the catalog of the shipped templates is needed");
        RoomVariants.Table table = RoomVariants.table();
        TemplateThemes themes = cat.themes();
        Map<String, Map<String, String>> socketCache = new HashMap<>();
        Map<String, int[]> sizeCache = new HashMap<>();
        int plans = 0, pieces = 0, rebuilt = 0, copies = 0, distinctCopies = 0, roomsOnFloors = 0;
        for (Polarity road : Polarity.values()) {
            RegionTheme previous = null;
            for (int k = 0; k < 12; k++) {
                for (int s = 0; s < 4; s++) {
                    long seed = Rng.mix(s + 1, 9001 + k);
                    SectorDraft draft = SectorDraft.of(seed, 0, road, k, 1, previous, null);
                    Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), themes, previous);
                    q.themeId = draft.theme().id();
                    SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                    assertTrue(sv.ok(), road + " floor " + (k + 1) + " seed " + s + ": " + SectorSolver.summary(sv.failures(), 3));
                    Result r = sv.result();
                    List<String> ids = RoomVariants.templatesOf(r.pieces);
                    String[] a = RoomVariants.assign(ids, r.seed, table), b = RoomVariants.assign(ids, r.seed, table);
                    assertEquals(List.of(a), List.of(b), "the same plan gives the same rooms");
                    assertEquals(r.pieces.size(), a.length, "one template per piece");
                    // the walking graph of the plan is connected (it is the plan's own: variants cannot change it)
                    int[] dist = FloorAssemblerTest.walk(r);
                    int reached = 0;
                    for (int d : dist) if (d >= 0) reached++;
                    assertTrue(reached >= r.pieces.stream().filter(p -> !p.shape.isCap()).count() - 1, "every room of the plan can be reached from the arrival");
                    Map<String, Integer> nth = new HashMap<>();
                    Map<String, Set<String>> used = new HashMap<>();
                    for (Placed p : r.pieces) {
                        String id = a[p.index];
                        pieces++;
                        if (id.equals(p.shape.id)) continue;
                        rebuilt++;
                        TemplateFile shipped = TemplateFile.load(p.shape.id), v = TemplateFile.load(id);
                        assertNotNull(v, id);
                        Map<String, String> sockets = socketCache.computeIfAbsent(p.shape.id, x -> {
                            try { return shipped.jigsaws(); } catch (RuntimeException e) { throw new IllegalStateException(e); }
                        });
                        int[] size = sizeCache.computeIfAbsent(id, x -> new int[] {v.sx, v.sy, v.sz});
                        assertEquals(p.shape.sx, size[0], id + " width");
                        assertEquals(p.shape.sy, size[1], id + " height");
                        assertEquals(p.shape.sz, size[2], id + " depth");
                        assertEquals(p.shape.sockets.size(), sockets.size(), id + " socket count");
                        assertEquals(sockets, v.jigsaws(), id + " sockets");
                        nth.merge(p.shape.id, 1, Integer::sum);
                        used.computeIfAbsent(p.shape.id, x -> new HashSet<>()).add(id);
                    }
                    for (Map.Entry<String, Integer> e : nth.entrySet()) {
                        int n = Math.min(e.getValue(), table.count(e.getKey()));
                        copies += n;
                        distinctCopies += Math.min(n, used.get(e.getKey()).size());
                    }
                    plans++;
                    roomsOnFloors += (int) r.pieces.stream().filter(p -> !p.shape.isCap()).count();
                }
            }
        }
        System.out.println("ROOMS REBUILT plans: " + plans + " floors, " + pieces + " pieces (" + roomsOnFloors + " rooms), " + rebuilt + " built from a rebuilt room");
        assertTrue(plans >= 90, "plans: " + plans);
        assertTrue(rebuilt > pieces / 4, "a good share of the pieces of a floor is a rebuilt room: " + rebuilt + " of " + pieces);
        assertEquals(copies, distinctCopies, "the first copies of one template in a floor are different rooms");
    }
}
