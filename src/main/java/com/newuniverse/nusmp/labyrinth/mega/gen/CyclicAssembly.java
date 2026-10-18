package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Cell;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.DeadEndPruner;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Dir;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopPlanner;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.SocketRules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the seam between the pure cyclic layer ({@code mega.cyclic}) and the real assembler. The cyclic layer decides WHICH loose doorways to join and
 * WHERE a hallway runs, on integer module cells; this class turns those decisions into real pieces, with the assembler's own placement test ({@code tryOne}:
 * region, exclusions, blockers, overlaps, the recipe's limits, the theme rules), so a hallway that is built is as legal as any other piece and one that does not
 * fit is simply not built. It does three things, all gated by {@link Request#cyclic} (null = none of this runs):
 * <ol>
 * <li>{@link #best}: the WFC-style placement step. When a doorway has several candidates that fit, the one whose own doorways line up with open doorways already
 *     in the plan (each is a free loop), that does not wall its doorways into blank walls and is not a leaf, wins. The first fitting candidate wins when none is
 *     better, so the weighted-random order of the pools still decides ties.</li>
 * <li>{@link #pass}: after the second fill and before the door caps (so every loose doorway is still a doorway, not a niche with a cap): read the plan into a
 *     {@link RoomGraph}, let {@link LoopPlanner} join loose doorways through hallways made of the corridor templates that ship (a straight piece or a bend per
 *     module cell, each hung from the previous one so its doorway is real and the last one lines up with the far doorway), then let {@link DeadEndPruner} take away
 *     the dangling rooms no loop could cure.</li>
 * <li>keeps the books: the plan's stats carry the dead ends, loops and hallway rooms before and after, which the tests and the in-game self-test read.</li>
 * </ol>
 * The pass only ADDS doorways and rooms (hallways) and then removes leaf rooms that are neither terminal nor required, so the walk from the arrival to the stairs,
 * the boss chain and the safe rooms is never cut. Nothing here uses a Minecraft type.
 */
final class CyclicAssembly {
    private CyclicAssembly() {}

    private static final int MODULE = MegaGrid.MODULE;
    private static final String P_SIDE = MegaGrid.P_SIDE, P_LINK = "nusmp:mega/link";

    // ================================================================================================================ the placement step

    private record Open(Placed piece, int socket, Vec front, Facing facing) {}

    /**
     * The best of several candidates for one doorway, by {@link SocketRules#placementScore}. {@code fits} are in the order the weighted draw produced them, and
     * the first of equal scores wins, so with no preference the old behaviour (the first fitting candidate) is what happens.
     */
    static Placed best(MegaAssembler a, List<Placed> fits) {
        if (fits.size() == 1) return fits.get(0);
        LoopConfig cfg = a.req.cyclic;
        Map<Vec, Open> byCell = new HashMap<>();
        List<Open> opens = new ArrayList<>();
        int[] counts = new int[RoomCategory.values().length];
        for (Placed p : a.placed) {
            if (p.shape.isCap()) continue;
            if (!p.link) counts[RoomCategory.of(p.shape.kind, p.shape.id).ordinal()]++;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                Socket s = p.shape.sockets.get(si);
                if (p.isUsed(si) || s.sealed() || !s.isDoor()) continue;
                Open o = new Open(p, si, p.socketFront(si), p.socketFacing(si));
                opens.add(o);
                byCell.put(p.socketCell(si), o);
            }
        }
        Placed best = fits.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Placed c : fits) {
            int aligned = 0, blocked = 0, killed = 0, free = 0, other = 0;
            Set<Open> matched = new HashSet<>();
            for (int j = 0; j < c.shape.sockets.size(); j++) {
                Socket s = c.shape.sockets.get(j);
                if (j == c.ownSocket || s.sealed() || !s.isDoor()) continue;
                other++;
                Vec f = c.socketFront(j);
                Open o = byCell.get(f);
                if (o != null && o.facing() == c.socketFacing(j).opposite() && o.front().equals(c.socketCell(j))
                        && Socket.canAttach(s.target(), c.socketFacing(j), o.piece().shape.sockets.get(o.socket()).name(), o.facing())) {
                    aligned++;
                    matched.add(o);
                    continue;
                }
                Box pt = new Box(f.x(), f.y(), f.z(), f.x(), f.y(), f.z());
                if (!pt.inside(a.region, 0) || a.hitsExclusion(pt) || a.index.overlapsAny(pt) || c.box.contains(f)) blocked++;
                else free++;
            }
            for (Open o : opens) if (!matched.contains(o) && c.box.contains(o.front())) killed++;
            Shape sh = c.shape;
            boolean leafOk = (Roles.of(sh.kind) & Roles.TERMINAL) != 0;
            double score = SocketRules.placementScore(new SocketRules.PlacementFacts(aligned, blocked, killed, free, other, RoomCategory.of(sh.kind, sh.id), leafOk), cfg, counts);
            if (score > bestScore + 1e-9) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    // ================================================================================================================ the pass

    /** The corridor templates a hallway cell can be: two doorways, one module, straight or bent. */
    private record Kit(List<PoolEntry> straight, List<PoolEntry> bend) {}

    /** Runs the loop pass and the pruner on the plan so far. Never throws for a plan it cannot read: it records why in the stats and leaves the plan alone. */
    static void pass(MegaAssembler a) {
        boolean limit = a.limitRooms;
        a.limitRooms = false;                                    // hallways are extra rooms: the room budget was for the ordinary filler
        try {
            run(a, a.req.cyclic);
        } finally {
            a.limitRooms = limit;
        }
    }

    private static boolean ongrid(MegaAssembler a, int ox, int oz) {
        for (Placed p : a.placed) {
            if (p.shape.isCap() || p.link) continue;
            if (Math.floorMod(p.box.minX() - ox, MODULE) != 0 || Math.floorMod(p.box.minZ() - oz, MODULE) != 0 || p.box.sizeX() % MODULE != 0 || p.box.sizeZ() % MODULE != 0) return false;
        }
        return true;
    }

    private static Kit kit(MegaAssembler a) {
        List<PoolEntry> straight = new ArrayList<>(), bend = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String poolId : new String[] {P_SIDE, P_LINK}) {
            PoolDef pool = a.src.pool(poolId);
            if (pool == null) continue;
            for (PoolEntry e : pool.entries()) {
                if (!seen.add(e.shape())) continue;
                Shape sh = a.shapeOf(e, poolId);
                if (sh == null || (sh.kind != RoomKind.CORRIDOR && sh.kind != RoomKind.LINK) || sh.id.contains("waystation")) continue;
                if (sh.sx != MODULE || sh.sz != MODULE || sh.sockets.size() != 2) continue;
                Socket s0 = sh.sockets.get(0), s1 = sh.sockets.get(1);
                if (!s0.isDoor() || !s1.isDoor() || s0.sealed() || s1.sealed()) continue;
                if (s0.facing() == s1.facing().opposite()) straight.add(e);
                else if (s0.facing() != s1.facing()) bend.add(e);
            }
        }
        return new Kit(straight, bend);
    }

    private static int roles(MegaAssembler a, Placed p, int[] requiredLeft) {
        int r = Roles.of(p.shape.kind);
        if (p.index == a.landing) r |= Roles.ENTRANCE;
        if (p.link) r |= Roles.ENTRANCE | Roles.REQUIRED;
        if (!p.shape.isCap() && requiredLeft[p.shape.kind.ordinal()] > 0) {
            requiredLeft[p.shape.kind.ordinal()]--;
            r |= Roles.REQUIRED;
        }
        return r;
    }

    /** The room graph of the pieces placed so far: node per piece that is not a cap, tree joins and the loops that already line up. {@code nodeOf} maps piece index to node (-1 for caps). */
    private static RoomGraph graph(MegaAssembler a, int[] nodeOf) {
        RoomGraph g = new RoomGraph();
        int[] requiredLeft = new int[RoomKind.values().length];
        for (RoomKind k : RoomKind.values()) requiredLeft[k.ordinal()] = a.rc.min(k);
        for (Placed p : a.placed) {
            if (p.shape.isCap()) {
                nodeOf[p.index] = -1;
                continue;
            }
            nodeOf[p.index] = g.addNode(RoomCategory.of(p.shape.kind, p.shape.id), roles(a, p, requiredLeft));
        }
        for (Placed p : a.placed) if (p.parent >= 0 && nodeOf[p.index] >= 0 && nodeOf[p.parent] >= 0) g.addEdge(nodeOf[p.parent], nodeOf[p.index]);
        for (Result.Loop l : a.loopsNow()) if (nodeOf[l.pieceA()] >= 0 && nodeOf[l.pieceB()] >= 0) g.addEdge(nodeOf[l.pieceA()], nodeOf[l.pieceB()]);
        return g;
    }

    private static void run(MegaAssembler a, LoopConfig cfg) {
        if (a.landing < 0) return;
        Placed land = a.placed.get(a.landing);
        final int ox = land.box.minX(), oz = land.box.minZ();
        if (!ongrid(a, ox, oz)) {
            a.bump("cyclic.offgrid", 1);
            return;
        }
        int[] nodeOf = new int[a.placed.size()];
        RoomGraph g = graph(a, nodeOf);
        RoomGraph.Report r0 = g.report(cfg.terminalMask());
        a.bump("cyclic.rooms0", r0.rooms());
        a.bump("cyclic.deadEnds0", r0.deadEnds());
        a.bump("cyclic.onCycle0", r0.onCycle());
        a.bump("cyclic.loops0", r0.cyclomatic());

        // ---- the open doorways (those that line up already are loops, not loose)
        Set<Long> paired = new HashSet<>();
        for (Result.Loop l : a.loopsNow()) {
            paired.add(((long) l.pieceA() << 20) | l.socketA());
            paired.add(((long) l.pieceB() << 20) | l.socketB());
        }
        List<LoopPlanner.Door> doors = new ArrayList<>();
        final Map<LoopPlanner.Door, int[]> sockets = new HashMap<>();
        for (Placed p : a.placed) {
            if (p.shape.isCap() || p.link) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                Socket s = p.shape.sockets.get(si);
                if (p.isUsed(si) || s.sealed() || !s.isDoor() || paired.contains(((long) p.index << 20) | si)) continue;
                Vec w = p.socketCell(si);
                LoopPlanner.Door d = new LoopPlanner.Door(nodeOf[p.index], new Cell(Math.floorDiv(w.x() - ox, MODULE), Math.floorDiv(w.z() - oz, MODULE)), Dir.byIndex(p.socketFacing(si).ordinal()));
                doors.add(d);
                sockets.put(d, new int[] {p.index, si});
            }
        }
        Kit kit = kit(a);
        LoopConfig use = kit.straight().isEmpty() || kit.bend().isEmpty() ? cfg.withMaxHallway(0) : cfg;
        List<int[]> keep = new ArrayList<>();
        int arrival = nodeOf[a.landing];
        for (Placed p : a.placed) {
            if (p.shape.kind == RoomKind.STAIRWELL && p.index != a.landing && a.rc.stairMinWalk > 0) keep.add(new int[] {arrival, nodeOf[p.index], a.rc.stairMinWalk});
        }
        LoopPlanner.Terrain terrain = c -> {
            int x = ox + c.x() * MODULE, z = oz + c.z() * MODULE;
            Box b = new Box(x, a.region.minY(), z, x + MODULE - 1, a.region.maxY(), z + MODULE - 1);
            if (!b.inside(a.region, 0) || a.hitsExclusion(b)) return false;
            for (Box rb : a.roomBlockers) if (rb.overlaps(b)) return false;
            return !a.index.overlapsAny(b);
        };
        LoopPlanner.Host host = link -> build(a, link, sockets, kit, ox, oz);
        LoopPlanner.Outcome out = LoopPlanner.run(g, doors, terrain, use, a.req.seed, keep, host);
        a.bump("cyclic.links", out.links().size());
        a.bump("cyclic.hallways", out.hallwayRooms());
        a.bump("cyclic.refused", out.rejected());

        // ---- trim what no loop could cure
        int[] nodeOf2 = new int[a.placed.size()];
        RoomGraph g2 = graph(a, nodeOf2);
        RoomGraph.Report r1 = g2.report(cfg.terminalMask());
        a.bump("cyclic.deadEnds1", r1.deadEnds());
        a.bump("cyclic.onCycle1", r1.onCycle());
        int minKeep = Math.max(a.rc.minRooms, a.req.floor == null ? 0 : a.req.floor.minRooms()) + 2;
        List<Integer> gone = DeadEndPruner.plan(g2, cfg, minKeep, nodeOf2[a.landing]);
        if (gone.isEmpty()) return;
        int[] pieceOf = new int[g2.size()];
        for (Placed p : a.placed) if (nodeOf2[p.index] >= 0) pieceOf[nodeOf2[p.index]] = p.index;
        BitSet drop = new BitSet();
        for (int node : gone) drop.set(pieceOf[node]);
        for (int i = drop.nextSetBit(0); i >= 0; i = drop.nextSetBit(i + 1)) {
            Placed p = a.placed.get(i);
            if (p.link || i == a.landing || (a.expansion != null && a.expansion.piece() == i)) return;           // never expected: the roles forbid it; leave the plan whole rather than guess
        }
        for (Placed p : a.placed) if (p.parent >= 0 && drop.get(p.parent) && !drop.get(p.index)) return;          // something hangs from a room we would remove: leave the plan whole
        a.removePieces(drop);
        a.bump("cyclic.pruned", gone.size());
    }

    // ================================================================================================================ building a hallway

    /**
     * Builds the hallway of an accepted link: one piece per cell, each hung from the doorway of the one before (the first from door A), the corridor straight or
     * bent as the cell asks. Every piece goes through the assembler's own placement test, so it is inside the region, clear of the exclusions and of every other
     * piece. The LAST piece's free doorway must stand in the module in front of door B and face it, so the two doorways line up and the loop finder joins them;
     * if anything fails the hallway is taken back off and the link is refused (the plan is unchanged). Direct links (doorways that already line up) need nothing
     * built: {@code loopsNow} finds them.
     */
    private static boolean build(MegaAssembler a, LoopPlanner.Link link, Map<LoopPlanner.Door, int[]> sockets, Kit kit, int ox, int oz) {
        if (link.direct()) return true;
        int[] pa = sockets.get(link.a()), pb = sockets.get(link.b());
        if (pa == null || pb == null) return false;
        Placed target = a.placed.get(pb[0]);
        int targetSocket = pb[1];
        if (a.placed.get(pa[0]).isUsed(pa[1]) || target.isUsed(targetSocket)) return false;
        int mark = a.placed.size();
        Placed cur = a.placed.get(pa[0]);
        int curSocket = pa[1];
        for (LoopPlanner.Hop hop : link.hops()) {
            List<PoolEntry> choices = new ArrayList<>(hop.straight() ? kit.straight() : kit.bend());
            a.rng.shuffle(choices);
            Placed next = null;
            int nextOut = -1;
            Facing want = Facing.byIndex(hop.out().ordinal());
            outer:
            for (PoolEntry e : choices) {
                Shape sh = a.shapeOf(e, P_SIDE);
                if (sh == null) continue;
                for (int k : a.order(sh)) {
                    Placed c = a.tryOne(cur, curSocket, sh, e, k, a.req.maxPieces, null);
                    if (c == null) continue;
                    int out = 1 - k;                                            // a hallway shape has exactly two doorways
                    if (c.socketFacing(out) != want) continue;
                    if (c.box.minX() != ox + hop.cell().x() * MODULE || c.box.minZ() != oz + hop.cell().z() * MODULE) continue;
                    next = c;
                    nextOut = out;
                    break outer;
                }
            }
            if (next == null) {
                a.rollbackTo(mark);
                return false;
            }
            a.push(next);
            cur = next;
            curSocket = nextOut;
        }
        Socket from = cur.shape.sockets.get(curSocket), to = target.shape.sockets.get(targetSocket);
        boolean lines = cur.socketFront(curSocket).equals(target.socketCell(targetSocket)) && target.socketFront(targetSocket).equals(cur.socketCell(curSocket))
                && Socket.canAttach(from.target(), cur.socketFacing(curSocket), to.name(), target.socketFacing(targetSocket));
        if (!lines) {
            a.rollbackTo(mark);
            return false;
        }
        return true;
    }
}
