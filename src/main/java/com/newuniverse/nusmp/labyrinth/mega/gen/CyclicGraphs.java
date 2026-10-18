package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;

import java.util.List;

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

    /** Is this kind one of the circulation kinds a hallway room is drawn from? */
    static boolean hallwayKind(RoomKind k) { return k == RoomKind.CORRIDOR || k == RoomKind.LINK; }

    /** For tests: the room graph's edges as a plain list of pairs. */
    public static List<int[]> edges(Result r) { return r.edges(); }
}
