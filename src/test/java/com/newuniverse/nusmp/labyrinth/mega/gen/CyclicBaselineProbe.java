package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.TreeMap;

class CyclicBaselineProbe {
    @Test
    void probe() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        int n = 0;
        double de = 0, cs = 0, cyc = 0, rooms = 0, deg1 = 0;
        double[] cat5 = new double[5];
        TreeMap<String, Integer> causes = new TreeMap<>();
        TreeMap<String, Integer> byKind = new TreeMap<>();
        TreeMap<Integer, Integer> doors = new TreeMap<>();
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 12; k++) {
                for (int s = 0; s < 6; s++) {
                    long seed = Rng.mix(s + 1, 4711 + k);
                    Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), cat.themes(), null);
                    SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                    if (!sv.ok()) continue;
                    Result res = sv.result();
                    RoomGraph g = CyclicGraphs.of(res);
                    RoomGraph.Report r = g.report(Roles.TERMINAL);
                    n++;
                    de += r.deadEndRatio();
                    cs += r.cycleShare();
                    cyc += r.cyclomatic();
                    rooms += r.rooms();
                    deg1 += g.deadEnds(0).size() / (double) r.rooms();
                    double[] sh = CyclicGraphs.shares(g.categoryCounts());
                    for (int i = 0; i < 5; i++) cat5[i] += sh[i];
                    int[] ids = res.roomIds();
                    for (int dead : g.deadEnds(Roles.TERMINAL)) {
                        Placed pd = null;
                        for (Placed p : res.pieces) if (ids[p.index] == dead) pd = p;
                        byKind.merge(pd.shape.kind.name(), 1, Integer::sum);
                        int open = 0;
                        for (int si = 0; si < pd.shape.sockets.size(); si++) if (!pd.isUsed(si) && !pd.shape.sockets.get(si).sealed()) open++;
                        int total = 0;
                        for (Socket so : pd.shape.sockets) if (!so.sealed()) total++;
                        doors.merge(total, 1, Integer::sum);
                        for (int si = 0; si < pd.shape.sockets.size(); si++) {
                            if (pd.isUsed(si) || pd.shape.sockets.get(si).sealed()) continue;
                            Vec f = pd.socketFront(si);
                            String c;
                            Box fb = new Box(f.x(), f.y(), f.z(), f.x(), f.y(), f.z());
                            Placed hit = null;
                            for (Placed p : res.pieces) if (p.box.overlaps(fb) && !p.shape.isCap()) hit = p;
                            Placed cap = null;
                            for (Placed p : res.pieces) if (p.box.overlaps(fb) && p.shape.isCap()) cap = p;
                            if (hit != null) c = "front is a room wall (" + hit.shape.kind + ")";
                            else if (!fb.inside(q.region, 0)) c = "front outside region";
                            else c = "front is free space" + (cap != null ? " (capped)" : "");
                            causes.merge(c, 1, Integer::sum);
                        }
                        if (open == 0) causes.merge("no open door at all", 1, Integer::sum);
                    }
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "BASELINE floors=%d rooms=%.1f deadEnd(non-terminal)=%.1f%% deadEnd(all deg1)=%.1f%% cycleShare=%.1f%% loops=%.2f", n, rooms / n, de / n * 100, deg1 / n * 100, cs / n * 100, cyc / n));
        StringBuilder b = new StringBuilder("BASELINE categories:");
        for (RoomCategory c : RoomCategory.values()) b.append(String.format(Locale.ROOT, " %s=%.1f%%", c, cat5[c.ordinal()] / n * 100));
        System.out.println(b);
        System.out.println("dead ends by kind " + byKind);
        System.out.println("dead ends by total doors " + doors);
        System.out.println("open doors of dead ends: " + causes);
    }
}
