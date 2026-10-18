package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

class CyclicCompareProbe {
    static void measure(String label, CatalogSource cat, LoopConfig cfg, int seeds, int floors) {
        int n = 0, fail = 0, fallback = 0, tries = 0;
        double de = 0, cs = 0, cyc = 0, rooms = 0, links = 0, halls = 0, pruned = 0, loops = 0;
        double[] cat5 = new double[5];
        Map<String, Integer> failures = new TreeMap<>();
        java.util.List<Double> ratios = new java.util.ArrayList<>();
        Map<String, Integer> leftKinds = new TreeMap<>();
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < floors; k++) {
                for (int s = 0; s < seeds; s++) {
                    long seed = Rng.mix(s + 1, 4711 + k);
                    Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), cat.themes(), null);
                    q.cyclic = cfg;
                    SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                    if (!sv.ok()) {
                        fail++;
                        continue;
                    }
                    tries += sv.tries();
                    sv.failures().forEach((x, y) -> failures.merge(x, y, Integer::sum));
                    if (sv.fallback()) fallback++;
                    Result res = sv.result();
                    RoomGraph g = CyclicGraphs.of(res);
                    RoomGraph.Report r = g.report(Roles.TERMINAL);
                    n++;
                    ratios.add(r.deadEndRatio());
                    { int[] ids = res.roomIds(); for (int dead : g.deadEnds(Roles.TERMINAL)) for (Placed p : res.pieces) if (ids[p.index] == dead) leftKinds.merge(p.shape.kind.name(), 1, Integer::sum); }
                    de += r.deadEndRatio();
                    cs += r.cycleShare();
                    cyc += r.cyclomatic();
                    rooms += r.rooms();
                    loops += res.loops.size();
                    links += res.stats.getOrDefault("cyclic.links", 0);
                    halls += res.stats.getOrDefault("cyclic.hallways", 0);
                    pruned += res.stats.getOrDefault("cyclic.pruned", 0);
                    double[] sh = CyclicGraphs.shares(g.categoryCounts());
                    for (int i = 0; i < 5; i++) cat5[i] += sh[i];
                }
            }
        }
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "%-28s floors=%d fail=%d fallback=%d tries=%.2f rooms=%.1f deadEnd=%.1f%% onCycle=%.1f%% loops=%.2f links=%.2f hallwayRooms=%.2f pruned=%.2f |",
                label, n, fail, fallback, tries / (double) Math.max(1, n), rooms / n, de / n * 100, cs / n * 100, cyc / n, links / n, halls / n, pruned / n));
        for (RoomCategory c : RoomCategory.values()) b.append(String.format(Locale.ROOT, " %s=%.1f%%", c.name().substring(0, 3), cat5[c.ordinal()] / n * 100));
        java.util.Collections.sort(ratios);
        int under = 0; for (double x : ratios) if (x <= 0.08 + 1e-9) under++;
        b.append(String.format(Locale.ROOT, "%n   dead-end ratio: %d of %d floors <= 8%%, p95=%.1f%%, max=%.1f%%; remaining dead ends by kind %s", under, n, ratios.get((int) (ratios.size() * 0.95) - 1) * 100, ratios.get(ratios.size() - 1) * 100, leftKinds));
        System.out.println(b);
        if (!failures.isEmpty()) System.out.println("   tries failed for: " + SectorSolver.summary(failures, 4));
    }

    @Test
    void probe() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        int seeds = 4, floors = 8;
        LoopConfig d = LoopConfig.defaults();
        long t0 = System.nanoTime();
        measure("old (cyclic off)", cat, null, 6, 22);
        System.out.println("   ms: " + (System.nanoTime() - t0) / 1_000_000);
        measure("default (la 12, bal 2)", cat, d, 6, 22);
    }
}
