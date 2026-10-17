package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: the assembler is random, and a random plan can quietly break a promise the rest of the mod relies on: a sector with two Guardians' halls, a
 * vault with the wrong number of pedestals, a Way Onward whose seal faces a wall, a room nobody can walk to, a doorway that opens onto the void. The
 * audit checks every one of those invariants on a finished plan and returns each failure as a plain sentence, so the solver can throw the plan away and
 * try the next seed, {@code /multiverse mega audit} can print what is wrong with a sector, and the tests can break one invariant at a time and see exactly
 * that sentence appear. It is run by the generator on every plan before anything is placed, so a sector that reaches the world has passed it (or is the
 * documented fallback layout, which skips the pacing checks but never the structural ones). The sentences contain no semicolon: the command joins them with one.
 *
 * <p>1.58.0, the floor model: a plan whose request carries a {@link Request#floor} is judged as a FLOOR. The first room is the arrival stairwell, not a Landing;
 * an ordinary floor has no boss chain at all (a Guardian or a vault on a floor that does not end a decade would block nothing and give a relic for free), a boss
 * floor has the whole chain with a stairwell behind its Way Onward; there are exactly two stairwells, each with an arrival cell and a stair exit; and in the full
 * audit the forward stairs are at least {@link Recipe#stairMinWalk} doorways from the arrival (a floor whose stairs are next to its door is no level to cross).
 */
public final class SectorAudit {
    private SectorAudit() {}

    /**
     * Every failed invariant of {@code r}. {@code strict} is the full audit; {@code false} (the fallback layout, which is only the Landing and the boss
     * chain) skips the pacing checks (extra safe room, minimum rooms and loops, trials, treasure closets, open doorways) but keeps all structural ones.
     */
    public static List<String> audit(Result r, Request q, boolean strict) {
        List<String> out = new ArrayList<>();
        if (!r.assembled()) {
            out.add("no layout: " + (r.failure == null ? "no Landing" : r.failure));
            return out;
        }
        Recipe rc = q.recipe == null ? new Recipe() : q.recipe;
        List<Placed> ps = r.pieces;
        Placed landing = r.landingPiece();
        final Request.Floor fl = q.floor;
        final boolean bossChain = fl == null || fl.boss();
        RoomKind startKind = fl == null ? RoomKind.LANDING : RoomKind.STAIRWELL;
        if (landing.shape.kind != startKind) out.add("the first room is a " + landing.shape.kind.folder() + ", not a " + (fl == null ? "Landing" : "stairwell"));

        // ---- structure: the boss chain
        List<Placed> arenas = of(ps, RoomKind.ARENA);
        Placed ante = null, vault = null, onward = null;
        if (!bossChain) {
            for (RoomKind k : new RoomKind[] {RoomKind.ARENA, RoomKind.ANTECHAMBER, RoomKind.VAULT, RoomKind.ONWARD}) {
                if (r.count(k) > 0) out.add("an ordinary floor has " + r.count(k) + " " + k.folder() + " room(s): only a boss floor has the boss chain");
            }
        } else if (arenas.size() != 1) out.add("expected exactly one arena, found " + arenas.size());
        else {
            Placed arena = arenas.get(0);
            ante = childOf(ps, arena, RoomKind.ANTECHAMBER);
            if (ante == null) out.add("the arena does not lead to an antechamber");
            else {
                vault = childOf(ps, ante, RoomKind.VAULT);
                onward = childOf(ps, ante, RoomKind.ONWARD);
                if (vault == null) out.add("the antechamber has no vault");
                if (onward == null) out.add("the antechamber has no Way Onward");
            }
        }
        if (vault != null) {
            int peds = vault.shape.countMarkers(MarkerKind.PEDESTAL);
            if (peds != q.relicCount) out.add("the vault has " + peds + " pedestal markers but the sector needs " + q.relicCount);
        }
        if (onward != null && fl != null) {
            Result.Expansion e = r.expansion;
            if (e == null || e.piece() != onward.index) out.add("the Way Onward has no readable seal");
            else if (childOf(ps, onward, RoomKind.STAIRWELL) == null) out.add("the Way Onward leads nowhere: no stairwell stands behind its seal");
        } else if (onward != null) {
            Result.Expansion e = r.expansion;
            if (e == null || e.piece() != onward.index) out.add("the Way Onward has no readable seal");
            else {
                Box inner = e.inner();
                for (Placed p : ps) {
                    if (p.index == onward.index) continue;
                    if (p.box.overlaps(p.shape.isCap() ? inner : e.strip())) {
                        out.add("the strip in front of the seal is blocked by " + p.shape.id);
                        break;
                    }
                }
            }
        }

        // ---- structure: landmarks the rest of the mod reads
        if (rc.requireMarkers) {
            if (fl == null && countIn(ps, RoomKind.LANDING, MarkerKind.ARRIVAL) < 1) out.add("no ARRIVAL marker in the Landing");
            if (!arenas.isEmpty() && countIn(ps, RoomKind.ARENA, MarkerKind.GUARDIAN_SPAWN) != 1) out.add("the arena needs exactly one GUARDIAN_SPAWN marker");
            if (bossChain && countAll(ps, MarkerKind.GATEKEEPER) < 1) out.add("no GATEKEEPER marker (the door of the boss chain)");
            if (vault != null && countIn(ps, RoomKind.VAULT, MarkerKind.VAULT_CENTER) < 1) out.add("no VAULT_CENTER marker in the vault");
            if (onward != null && !onward.shape.expansionMarked) out.add("the Way Onward has no EXPANSION_SOCKET marker");
        }

        // ---- structure: the stairs of the floor model
        if (fl != null) stairs(r, rc, ps, landing, strict, out);

        // ---- structure: space
        BoxIndex seen = new BoxIndex();
        for (Placed p : ps) {
            if (!p.link && !p.box.inside(q.region, p.shape.isCap() ? q.capMargin : 0)) out.add("piece " + p.shape.id + " at " + p.origin + " leaves the region");
            for (Box ex : q.exclusions) {
                if (!p.link && ex.overlaps(p.box)) {                 // the tunnel to the Landing starts inside the old sector's margin by design
                    out.add("piece " + p.shape.id + " at " + p.origin + " is inside another sector's territory");
                    break;
                }
            }
            int hit = seen.firstOverlap(p.box);
            if (hit >= 0) out.add("pieces " + ps.get(hit).shape.id + " and " + p.shape.id + " overlap at " + p.origin);
            seen.add(p.box);
        }
        for (RoomKind k : RoomKind.values()) {
            if (k != RoomKind.CAP && r.count(k) > rc.max(k)) out.add("more than " + rc.max(k) + " " + k.folder() + " room(s): " + r.count(k));
        }
        int unreachable = unreachable(r);
        if (unreachable > 0) out.add(unreachable + " room(s) cannot be reached from the Landing");

        if (!strict) return out;

        // ---- pacing
        int rooms = 0;
        for (Placed p : ps) if (Result.isRoom(p) && !p.link) rooms++;
        int wantRooms = fl == null ? rc.minRooms : Math.max(rc.minRooms, fl.minRooms());
        if (rooms < wantRooms) out.add("only " + rooms + " rooms (the " + (fl == null ? "recipe" : "floor") + " wants " + wantRooms + ")");
        if (r.loops.size() < rc.minLoops) out.add("only " + r.loops.size() + " loops (the recipe wants " + rc.minLoops + ")");
        int safe = 0;
        Placed first = landing;
        for (Placed p : ps) {
            if (p.shape.kind != RoomKind.SAFE) continue;
            double d = Math.hypot(p.box.centerX() - first.box.centerX(), p.box.centerZ() - first.box.centerZ()) / MegaGrid.MODULE;
            if (d >= rc.safeMinDistance && d <= rc.safeMaxDistance) safe++;
        }
        if (safe < rc.extraSafe) out.add("only " + safe + " extra safe room(s) " + rc.safeMinDistance + ".." + rc.safeMaxDistance + " modules from the Landing (the recipe wants " + rc.extraSafe + ")");
        for (RoomKind k : RoomKind.values()) {
            if (k == RoomKind.CAP || rc.min(k) <= 0) continue;
            if (r.count(k) < rc.min(k)) out.add("only " + r.count(k) + " " + k.folder() + " room(s) (the recipe wants " + rc.min(k) + ")");
        }
        int open = 0;
        String firstOpen = null;
        for (Placed p : ps) {
            if (p.shape.isCap()) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                if (p.isUsed(si) || p.shape.sockets.get(si).sealed()) continue;
                Vec f = p.socketFront(si);
                if (!seen.overlapsAny(new Box(f.x(), f.y(), f.z(), f.x(), f.y(), f.z()))) {
                    open++;
                    if (firstOpen == null) firstOpen = p.shape.id + " at " + p.socketCell(si);
                }
            }
        }
        if (open > 0) out.add(open + " doorway(s) open onto the void (first: " + firstOpen + ")");
        return out;
    }

    /**
     * The stairs of a floor: exactly two stairwells (the arrival one is the first room), each with an ARRIVAL cell (where a party stands) and a STAIR_EXIT (where the
     * stairs end), and in the full audit the forward one at least {@code stairMinWalk} doorways from the arrival.
     */
    private static void stairs(Result r, Recipe rc, List<Placed> ps, Placed landing, boolean strict, List<String> out) {
        List<Placed> stairs = of(ps, RoomKind.STAIRWELL);
        if (stairs.size() != 2) {
            out.add("expected exactly two stairwells (arrival and forward), found " + stairs.size());
            return;
        }
        Placed forward = stairs.get(0).index == landing.index ? stairs.get(1) : stairs.get(0);
        if (rc.requireMarkers) {
            for (Placed st : stairs) {
                String which = st.index == landing.index ? "arrival" : "forward";
                if (st.shape.countMarkers(MarkerKind.ARRIVAL) != 1) out.add("the " + which + " stairwell needs exactly one ARRIVAL marker");
                if (st.shape.countMarkers(MarkerKind.STAIR_EXIT) != 1) out.add("the " + which + " stairwell needs exactly one STAIR_EXIT marker");
            }
        }
        if (strict) {
            int[] dist = WalkGraph.distances(ps.size(), pieceEdges(r), landing.index);
            if (dist[forward.index] < 0) out.add("the forward stairwell cannot be walked to from the arrival stairwell");
            else if (dist[forward.index] < rc.stairMinWalk) out.add("the forward stairwell is only " + dist[forward.index] + " doorways from the arrival (the recipe wants " + rc.stairMinWalk + ")");
        }
    }

    /** The doorways between the pieces that are rooms, as pairs of PIECE indices: every join and every loop. */
    private static List<int[]> pieceEdges(Result r) {
        List<int[]> edges = new ArrayList<>();
        for (Placed p : r.pieces) {
            if (p.parent < 0 || p.shape.isCap()) continue;
            if (r.pieces.get(p.parent).shape.isCap()) continue;
            edges.add(new int[] {p.parent, p.index});
        }
        for (Result.Loop l : r.loops) edges.add(new int[] {l.pieceA(), l.pieceB()});
        return edges;
    }

    private static List<Placed> of(List<Placed> ps, RoomKind k) {
        List<Placed> out = new ArrayList<>();
        for (Placed p : ps) if (p.shape.kind == k) out.add(p);
        return out;
    }

    private static Placed childOf(List<Placed> ps, Placed parent, RoomKind k) {
        for (Placed p : ps) if (p.parent == parent.index && p.shape.kind == k) return p;
        return null;
    }

    private static int countIn(List<Placed> ps, RoomKind k, MarkerKind m) {
        int n = 0;
        for (Placed p : ps) if (p.shape.kind == k) n += p.shape.countMarkers(m);
        return n;
    }

    private static int countAll(List<Placed> ps, MarkerKind m) {
        int n = 0;
        for (Placed p : ps) n += p.shape.countMarkers(m);
        return n;
    }

    /** How many rooms the Landing cannot reach through the doorways of the room graph (a breadth-first walk over {@link Result#edges}). */
    private static int unreachable(Result r) {
        int[] ids = r.roomIds();
        int rooms = 0;
        for (int id : ids) if (id >= 0) rooms++;
        if (rooms == 0 || r.landing < 0 || ids[r.landing] < 0) return rooms;
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < rooms; i++) adj.add(new ArrayList<>());
        for (int[] e : r.edges()) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
        }
        boolean[] seen = new boolean[rooms];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        // the tunnel to the Landing is part of the graph: start from the first room, which is the first tunnel piece or the Landing itself
        q.add(0);
        seen[0] = true;
        int reached = 1;
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adj.get(u)) {
                if (!seen[v]) {
                    seen[v] = true;
                    reached++;
                    q.add(v);
                }
            }
        }
        return rooms - reached;
    }
}
