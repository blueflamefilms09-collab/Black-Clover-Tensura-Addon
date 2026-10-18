package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;


/**
 * Why: the cyclic layer ({@code mega.cyclic}) judges a plain {@link RoomGraph}; the generator holds a {@link Result} of placed pieces. This is the one place where
 * the second becomes the first, so the loop pass, the tests and the in-game {@code gen.cyclic} self-test read a sector the same way: rooms are the pieces that
 * are not door caps (the tunnel pieces of the old model included, as {@link Result#edges()} counts them), the roles come from the room kinds
 * ({@link Roles#of}), the category from kind and template ({@link RoomCategory#of}), and the doorways are the joins and loops of the plan.
 */
public final class CyclicGraphs {
    private CyclicGraphs() {}

    /** The room graph of a finished plan; node i is room id i of {@link Result#roomIds()}. */
    public static RoomGraph of(Result r) {
        RoomGraph g = new RoomGraph();
        for (Placed p : r.pieces) {
            if (!Result.isRoom(p)) continue;
            g.addNode(RoomCategory.of(p.shape.kind, p.shape.id), roles(p));
        }
        for (int[] e : r.edges()) g.addEdge(e[0], e[1]);
        return g;
    }

    /** The roles of a placed room: its kind's own, plus ENTRANCE for the first room of a floor. */
    static int roles(Placed p) { return Roles.of(p.shape.kind); }

    /** The category counts of a plan, in {@link RoomCategory} order (door caps are not rooms). */
    public static int[] categoryCounts(Result r) { return of(r).categoryCounts(); }

    /** The numbers of {@link RoomGraph.Report} for a plan, with the standard terminal mask. */
    public static RoomGraph.Report report(Result r) { return of(r).report(Roles.TERMINAL); }

    /** Shares (0..1) of the five categories among the rooms, in {@link RoomCategory} order. */
    public static double[] shares(int[] counts) {
        double total = 0;
        for (int c : counts) total += c;
        double[] s = new double[counts.length];
        if (total > 0) for (int i = 0; i < s.length; i++) s[i] = counts[i] / total;
        return s;
    }

    /** Is this piece a one-module corridor or link with two doorways, both joined to a room (a hallway cell, or a corridor that happens to be one)? */
    public static boolean throughHallway(Result r, Placed p) {
        if (p.shape.isCap() || p.link || (p.shape.kind != RoomKind.CORRIDOR && p.shape.kind != RoomKind.LINK)) return false;
        if (p.shape.sx != com.newuniverse.nusmp.labyrinth.mega.MegaGrid.MODULE || p.shape.sz != com.newuniverse.nusmp.labyrinth.mega.MegaGrid.MODULE || p.shape.sockets.size() != 2) return false;
        int joined = 0;
        for (int si = 0; si < 2; si++) {
            if (si == p.ownSocket && p.parent >= 0 && !r.pieces.get(p.parent).shape.isCap()) joined++;
            else if (si != p.ownSocket && partnerRoom(r, p, si)) joined++;
        }
        return joined == 2;
    }

    /** Does socket {@code si} of {@code p} lead to a room (a child that is not a cap, or a loop partner)? */
    private static boolean partnerRoom(Result r, Placed p, int si) {
        for (Placed c : r.pieces) if (c.parent == p.index && c.parentSocket == si && !c.shape.isCap()) return true;
        for (Result.Loop l : r.loops) if ((l.pieceA() == p.index && l.socketA() == si) || (l.pieceB() == p.index && l.socketB() == si)) return true;
        return false;
    }
}
