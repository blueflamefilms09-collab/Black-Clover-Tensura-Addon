package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the "loose socket" pass, the heart of the owner's requirement. A dungeon grown as a tree has open doorways everywhere: at the frontier where
 * growth stopped, and beside neighbours that happen not to line up. Each open doorway is a chance to turn a dead end into a loop. The pass, in order:
 * <ol>
 * <li>lists the OPEN doorways of rooms that may take one (never the stairs, the entrance or the boss chain: {@link Roles#LOCKED});</li>
 * <li>for each pair of open doorways of two DIFFERENT rooms asks whether they can be joined: directly (they already line up), or through a <b>transitional
 *     hallway</b> of at most {@code maxHallway} module cells over free ground, each cell a straight piece or a bend, the first cell fed by doorway A and the
 *     last cell feeding doorway B ({@link #hallway});</li>
 * <li>keeps only joins that close a real loop (at least {@code minCycleFix} / {@code minCycle} doorways round the circuit), that keep the hallway share under its
 *     cap, and that leave every {@code keepApart} pair (e.g. the arrival and the forward stairs) at least as far apart as the plan wants;</li>
 * <li>picks the best by a score: a dead end cured is worth 100, a room newly on a cycle 6, a longer detour up to 8 steps 1.5 per step, every hallway cell
 *     costs 4, plus a seeded jitter under 1 so ties break the same way on every machine;</li>
 * <li>asks the {@link Host} to build it; on success the graph grows (hallway rooms are Transitions) and the loop repeats; on failure the pair is never offered again.</li>
 * </ol>
 * It first cures dead ends (until the ratio is at or under the target), then adds plain loops until the cycle share reaches its target or the link budget
 * is spent. It only ever ADDS doorways and rooms, so every path that existed still exists: the way to the stairs, the boss and the safe rooms cannot be cut
 * by this pass. Deterministic: the same graph, doors, terrain, config and seed give the same links. Pure.
 */
public final class LoopPlanner {
    private LoopPlanner() {}

    /** Ground the pass may build a hallway on. */
    public interface Terrain {
        boolean free(Cell c);
    }

    /** The builder of a link: returns true when the hallway / doorway really exists afterwards (false = leave it out, the plan is unchanged). */
    public interface Host {
        boolean realize(Link link);
    }

    /** An open doorway: the room it belongs to, the module cell INSIDE that room it stands in, and the way it points out of the room. */
    public record Door(int room, Cell cell, Dir dir) {
        public Cell front() { return cell.step(dir); }
    }

    /** One cell of a hallway: entered while walking {@code in}, left while walking {@code out}. {@code in == out} is a straight piece, otherwise a bend. */
    public record Hop(Cell cell, Dir in, Dir out) {
        public boolean straight() { return in == out; }
    }

    /**
     * A join of two open doorways. {@code hops} is empty for a direct doorway (the two already line up); otherwise the hallway cells from A to B.
     * {@code cycleLength} is the number of doorways round the loop it closes (a large number when it joins two separate parts).
     */
    public record Link(Door a, Door b, List<Hop> hops, int fixes, int newOnCycle, int cycleLength, double score) {
        public Link { hops = List.copyOf(hops); }

        public boolean direct() { return hops.isEmpty(); }
    }

    /** What the pass did. {@code links} are the accepted ones in order, {@code rejected} how many the host refused. */
    public record Outcome(List<Link> links, int rejected, RoomGraph.Report before, RoomGraph.Report after, int hallwayRooms) {}

    private static final double W_FIX = 100.0, W_CYCLE = 6.0, W_GAIN = 1.5, W_CELL = 4.0;
    private static final int GAIN_CAP = 8, EXACT = 8;

    // ------------------------------------------------------------------------------------------------------------------------------ the hallway

    /**
     * The shortest hallway (fewest cells, at most {@code maxCells}) that runs from the front of {@code a} to the front of {@code b} over free cells, or null.
     * Cells are visited without repeating, a hallway never turns back on itself (a piece has two different doors), straight is tried before the two turns
     * (so a straight corridor wins over a bent one of the same length), and the order is fixed, so the answer is deterministic. If the two fronts are the same
     * cell the hallway is that one cell (a bend, or a straight when the doorways face each other across it).
     */
    public static List<Hop> hallway(Door a, Door b, int maxCells, java.util.function.Predicate<Cell> free) {
        if (maxCells < 1) return null;
        Cell start = a.front(), goalCell = b.front();
        Dir goalOut = b.dir().opposite();
        if (!free.test(start) || !free.test(goalCell)) return null;
        for (int k = Math.max(1, start.manhattan(goalCell) + 1); k <= maxCells; k++) {
            List<Hop> acc = new ArrayList<>(k);
            if (dfs(start, a.dir(), k, goalCell, goalOut, free, acc)) return acc;
        }
        return null;
    }

    private static boolean dfs(Cell c, Dir in, int left, Cell goalCell, Dir goalOut, java.util.function.Predicate<Cell> free, List<Hop> acc) {
        Dir[] outs = {in, in.cw(), in.ccw()};
        for (Dir out : outs) {
            if (left == 1) {
                if (c.equals(goalCell) && out == goalOut) {
                    acc.add(new Hop(c, in, out));
                    return true;
                }
                continue;
            }
            Cell n = c.step(out);
            if (n.manhattan(goalCell) > left - 1) continue;
            if (!free.test(n)) continue;
            boolean seen = false;
            for (Hop h : acc) if (h.cell().equals(n)) { seen = true; break; }
            if (seen || n.equals(c)) continue;
            acc.add(new Hop(c, in, out));
            if (dfs(n, out, left - 1, goalCell, goalOut, free, acc)) return true;
            acc.remove(acc.size() - 1);
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the pass

    /**
     * Runs the pass on {@code g}, which is changed in place (hallway rooms and the new doorways are added). {@code keepApart} is a list of {int a, int b, int minWalk}:
     * after every accepted link the walking distance between rooms a and b must still be at least minWalk (a pair that is already closer than that is ignored).
     */
    public static Outcome run(RoomGraph g, List<Door> doors, Terrain terrain, LoopConfig cfg, long seed, List<int[]> keepApart, Host host) {
        Rng rng = new Rng(seed ^ 0x5DEECE66DL);
        RoomGraph.Report before = g.report(cfg.terminalMask());
        List<Door> open = new ArrayList<>();
        for (Door d : doors) if (d.room() >= 0 && d.room() < g.size() && !g.hasRole(d.room(), cfg.lockedMask())) open.add(d);
        Set<Cell> taken = new HashSet<>();
        Set<Long> banned = new HashSet<>();
        List<Link> accepted = new ArrayList<>();
        int rejected = 0, hallwayRooms = 0;
        for (int iter = 0; iter < cfg.maxLinks() + 4 * cfg.maxLinks() && accepted.size() < cfg.maxLinks(); iter++) {
            RoomGraph.Report rep = g.report(cfg.terminalMask());
            boolean needFix = rep.deadEndRatio() > cfg.deadEndTarget();
            boolean needCycle = rep.cycleShare() < cfg.cycleTarget();
            if (!needFix && !needCycle) break;
            Link best = null;
            if (needFix) best = best(g, open, terrain, taken, banned, cfg, rng, keepApart, true, hallwayRooms);
            if (best == null && needCycle) best = best(g, open, terrain, taken, banned, cfg, rng, keepApart, false, hallwayRooms);
            if (best == null) break;
            if (host.realize(best)) {
                commit(g, best);
                accepted.add(best);
                hallwayRooms += best.hops().size();
                open.remove(best.a());
                open.remove(best.b());
                for (Hop h : best.hops()) taken.add(h.cell());
            } else {
                banned.add(pairKey(best.a(), best.b()));
                rejected++;
            }
        }
        return new Outcome(accepted, rejected, before, g.report(cfg.terminalMask()), hallwayRooms);
    }

    /** Adds the hallway rooms and the doorways of an accepted link to the graph. */
    private static void commit(RoomGraph g, Link l) {
        int prev = l.a().room();
        for (int i = 0; i < l.hops().size(); i++) {
            int n = g.addNode(RoomCategory.TRANSITION, Roles.HALLWAY);
            g.addEdge(prev, n);
            prev = n;
        }
        g.addEdge(prev, l.b().room());
    }

    private static long pairKey(Door a, Door b) {
        long x = key(a), y = key(b);
        return x < y ? x * 1_000_003L + y : y * 1_000_003L + x;
    }

    private static long key(Door d) { return ((long) d.room() * 4 + d.dir().ordinal()) * 100_003L + d.cell().x() * 997L + d.cell().z(); }

    private static Link best(RoomGraph g, List<Door> open, Terrain terrain, Set<Cell> taken, Set<Long> banned, LoopConfig cfg, Rng rng, List<int[]> keepApart, boolean fixPhase,
                             int hallwayRooms) {
        Set<Integer> dead = new HashSet<>(g.deadEnds(cfg.terminalMask()));
        Map<Integer, int[]> dist = new HashMap<>();
        java.util.function.Predicate<Cell> free = c -> terrain.free(c) && !taken.contains(c);
        List<Link> cands = new ArrayList<>();
        int n = g.size();
        for (int i = 0; i < open.size(); i++) {
            Door a = open.get(i);
            for (int j = i + 1; j < open.size(); j++) {
                Door b = open.get(j);
                if (a.room() == b.room() || g.hasEdge(a.room(), b.room())) continue;
                if (banned.contains(pairKey(a, b))) continue;
                int fixes = (dead.contains(a.room()) ? 1 : 0) + (dead.contains(b.room()) ? 1 : 0);
                if (fixPhase && fixes == 0) continue;
                boolean direct = SocketRules.aligned(a, b);
                List<Hop> hops = List.of();
                if (!direct) {
                    if (cfg.maxHallway() < 1 || a.front().manhattan(b.front()) + 1 > cfg.maxHallway()) continue;
                    if (hallwayRooms + 1 > cfg.hallwayShare() * (n + 1) + 1e-9) continue;
                    hops = hallway(a, b, cfg.maxHallway(), free);
                    if (hops == null) continue;
                    if (hallwayRooms + hops.size() > cfg.hallwayShare() * (n + hops.size()) + 1e-9) continue;
                }
                int[] da = dist.computeIfAbsent(a.room(), g::distances);
                int d = da[b.room()];
                int cycleLen = d < 0 ? 1_000 : d + hops.size() + 1;
                if (d >= 0 && cycleLen < (fixPhase ? cfg.minCycleFix() : cfg.minCycle())) continue;
                double cheap = W_FIX * fixes + W_GAIN * Math.min(d < 0 ? GAIN_CAP : d, GAIN_CAP) - W_CELL * hops.size() + rng.nextDouble();
                cands.add(new Link(a, b, hops, fixes, 0, cycleLen, cheap));
            }
        }
        if (cands.isEmpty()) return null;
        cands.sort(Comparator.comparingDouble(Link::score).reversed());
        BitSet onCycle = g.onCycle();
        int baseCycle = onCycle.cardinality();
        Link winner = null;
        double winScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < cands.size() && i < EXACT; i++) {
            Link c = cands.get(i);
            RoomGraph t = g.copy();
            commit(t, c);
            int gain = t.onCycle().cardinality() - baseCycle - c.hops().size();       // hallway rooms on the new loop do not count as "rooms newly on a cycle"
            if (!fixPhase && gain < 1) continue;
            if (!keepsApart(t, g, keepApart)) continue;
            double exact = c.score() + W_CYCLE * Math.max(0, gain);
            if (exact > winScore) {
                winScore = exact;
                winner = new Link(c.a(), c.b(), c.hops(), c.fixes(), Math.max(0, gain), c.cycleLength(), exact);
            }
        }
        return winner;
    }

    private static boolean keepsApart(RoomGraph after, RoomGraph before, List<int[]> keepApart) {
        if (keepApart == null) return true;
        for (int[] k : keepApart) {
            int was = before.distance(k[0], k[1]);
            if (was >= 0 && was < k[2]) continue;                  // already closer than the plan wants: not this pass's doing
            int now = after.distance(k[0], k[1]);
            if (now >= 0 && now < k[2]) return false;
        }
        return true;
    }
}
