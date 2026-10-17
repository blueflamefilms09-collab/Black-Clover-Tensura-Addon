package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the pool weights decide how dense and how loopy a sector is, and they can only be tuned by looking at hundreds of sectors, which is impossible in
 * the game and trivial here. This runs the real solver over a catalog and prints, for every seed, how the sector came out (pieces, rooms, door caps, loops,
 * fill, deepest door, how many tries and whether the fallback layout was needed) and, at the end, the failure rate and the minimum, average and maximum of each
 * number, plus the reasons tries failed for, most frequent first. The plan files of the first five seeds are written for
 * {@code python3 tools/labyrinth/mega_preview.py --plan plan.json map.png}. With {@code --chain N} it also grows N sectors per seed through the real planner and
 * tunnel builder and reports the closest two regions ever came and whether the dungeon could keep growing.
 *
 * <pre>
 *   java -cp OUT com.newuniverse.nusmp.labyrinth.mega.gen.MegaSim &lt;catalog.json | --synthetic&gt; &lt;seeds&gt; [planOutDir] [--chain N]
 * </pre>
 */
public final class MegaSim {
    private MegaSim() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: MegaSim <catalog.json | --synthetic> <seeds> [planOutDir] [--chain N]");
            return;
        }
        PoolSource src;
        if (args[0].equals("--synthetic")) src = SyntheticCatalog.standard();
        else {
            CatalogSource c = CatalogSource.load(Path.of(args[0]));
            if (c.isEmpty()) {
                System.out.println("The catalog " + args[0] + " has no templates yet: nothing to simulate.");
                return;
            }
            src = c;
        }
        int seeds = Integer.parseInt(args[1]);
        Path planDir = null;
        int chain = 0;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--chain") && i + 1 < args.length) chain = Integer.parseInt(args[++i]);
            else planDir = Path.of(args[i]);
        }
        if (planDir != null) Files.createDirectories(planDir);

        Recipe recipe = new Recipe();
        int ok = 0, fb = 0, failed = 0, deadEnds = 0;
        long tPieces = 0, tRooms = 0, tCaps = 0, tLoops = 0;
        int minRooms = Integer.MAX_VALUE, maxRooms = 0, minLoops = Integer.MAX_VALUE, maxLoops = 0, minPieces = Integer.MAX_VALUE, maxPieces = 0;
        double fillSum = 0, fillMin = Double.MAX_VALUE, fillMax = 0;
        long tTries = 0, nanos = 0;
        java.util.Map<String, Integer> reasons = new java.util.LinkedHashMap<>();
        System.out.println("seed  tries fb  pieces rooms caps link loops safe depth  fill%   ms  note");
        for (int s = 0; s < seeds; s++) {
            long dungeonSeed = Rng.mix(s, 1234567L);
            SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed);
            int relics = RegionTheme.GRAVITY.relicCount(dungeonSeed, 0);
            SectorRequests.Inputs in = new SectorRequests.Inputs(src, recipe, 450, 40, plan.seed(), plan.entry(), plan.forward(), plan.region(), null, null, null, List.of(), relics);
            SectorRequests.Prepared prep = SectorRequests.prepare(in);
            long t0 = System.nanoTime();
            SectorSolver.Solved sv = SectorSolver.solve(prep.request(), 24);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            nanos += System.nanoTime() - t0;
            tTries += sv.tries();
            for (java.util.Map.Entry<String, Integer> e : sv.failures().entrySet()) reasons.merge(e.getKey(), e.getValue(), Integer::sum);
            if (!sv.ok()) {
                failed++;
                System.out.printf(Locale.ROOT, "%-5d %-5d FAILED %s%n", s, sv.tries(), SectorSolver.summary(sv.failures(), 2));
                continue;
            }
            Result r = sv.result();
            int rooms = 0, caps = 0, links = 0, depth = 0;
            double area = 0;
            for (Placed p : r.pieces) {
                if (p.shape.isCap()) caps++;
                else if (p.link) links++;
                else {
                    rooms++;
                    area += (double) p.box.sizeX() * p.box.sizeZ();
                }
                depth = Math.max(depth, p.depth);
            }
            double fill = 100.0 * area / ((double) r.region.sizeX() * r.region.sizeZ());
            int safe = r.count(RoomKind.SAFE);
            if (sv.fallback()) fb++;
            else ok++;
            if (sv.deadEnd()) deadEnds++;
            tPieces += r.pieces.size();
            tRooms += rooms;
            tCaps += caps;
            tLoops += r.loops.size();
            minRooms = Math.min(minRooms, rooms);
            maxRooms = Math.max(maxRooms, rooms);
            minLoops = Math.min(minLoops, r.loops.size());
            maxLoops = Math.max(maxLoops, r.loops.size());
            minPieces = Math.min(minPieces, r.pieces.size());
            maxPieces = Math.max(maxPieces, r.pieces.size());
            fillSum += fill;
            fillMin = Math.min(fillMin, fill);
            fillMax = Math.max(fillMax, fill);
            System.out.printf(Locale.ROOT, "%-5d %-5d %-3s %-6d %-5d %-4d %-4d %-5d %-4d %-5d %5.1f %4d  %s%n", s, sv.tries(), sv.fallback() ? "FB" : "", r.pieces.size(), rooms, caps, links,
                    r.loops.size(), safe, depth, fill, ms, r.warnings.isEmpty() ? "" : r.warnings.get(0));
            if (planDir != null && s < 5) {
                String json = PlanJson.write(r, plan.entryFloor(), plan.forward(), sv.audit(), sv.tries(), sv.fallback(), "sim seed " + s);
                Files.writeString(planDir.resolve("plan_" + s + ".json"), json);
            }
        }
        int built = ok + fb;
        System.out.println();
        System.out.println("seeds " + seeds + ": " + ok + " passed the audit, " + fb + " needed the fallback layout, " + failed + " could not be built, " + deadEnds + " ended in a dead-end seal");
        if (built > 0) {
            System.out.printf(Locale.ROOT, "pieces %d / %.1f / %d (min / avg / max), rooms %d / %.1f / %d, loops %d / %.1f / %d, caps avg %.1f, fill %.1f%% / %.1f%% / %.1f%%, tries avg %.2f, %.1f ms per seed%n",
                    minPieces, tPieces / (double) built, maxPieces, minRooms, tRooms / (double) built, maxRooms, minLoops, tLoops / (double) built, maxLoops, tCaps / (double) built,
                    fillMin, fillSum / built, fillMax, tTries / (double) seeds, nanos / 1e6 / seeds);
        }
        if (!reasons.isEmpty()) System.out.println("failed tries by reason: " + SectorSolver.summary(reasons, 8));
        if (chain > 1) {
            int grown = 0, stuck = 0;
            long minGap = Long.MAX_VALUE;
            java.util.Map<String, Integer> why = new java.util.LinkedHashMap<>();
            for (int s = 0; s < seeds; s++) {
                SimWorld w = new SimWorld(src, Rng.mix(s, 1234567L), recipe, 450, 40, 24);
                boolean done = w.grow(chain);
                if (done) grown++;
                else {
                    stuck++;
                    why.merge(SectorSolver.stripNumbers(w.stopped == null ? "?" : w.stopped), 1, Integer::sum);
                }
                for (int i = 0; i < w.sectors.size(); i++) {
                    for (int j = i + 1; j < w.sectors.size(); j++) minGap = Math.min(minGap, gap(w.sectors.get(i).plan().region(), w.sectors.get(j).plan().region()));
                }
            }
            System.out.println("chain of " + chain + " sectors: " + grown + " of " + seeds + " worlds grew fully, " + stuck + " stopped early; closest two regions: " + (minGap == Long.MAX_VALUE ? "n/a" : minGap + " blocks"));
            if (!why.isEmpty()) System.out.println("stopped because: " + SectorSolver.summary(why, 5));
        }
    }

    /** The clear distance between two boxes in the horizontal plane (0 when they touch or overlap). */
    static long gap(Box a, Box b) {
        long dx = Math.max(0, Math.max(a.minX() - b.maxX(), b.minX() - a.maxX()) - 1);
        long dz = Math.max(0, Math.max(a.minZ() - b.maxZ(), b.minZ() - a.maxZ()) - 1);
        return Math.max(dx, dz);
    }
}
