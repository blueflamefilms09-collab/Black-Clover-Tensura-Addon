package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: what one run of the assembler produced, as plain data: every placed piece (rooms, the tunnel to the Landing, door caps) in placement order,
 * the loops, the Way Onward's seal, the warnings the run collected (a pool that names a missing template, a shape with no readable seal) and, when the
 * run could not build a sector, the reason. The audit, the plan writer, the simulator and the game-side generator all read this one shape, so
 * what the tests prove about a plan is what the game places. Nothing here is Minecraft-shaped.
 */
public final class Result {
    /** A doorway that two rooms share without the assembler joining them: both sockets are unused and face each other across the wall between the rooms. */
    public record Loop(int pieceA, int socketA, int pieceB, int socketB, Vec cell, Facing facing) {}

    /**
     * The end of the Way Onward: which piece holds it, the jigsaw cell of its sealed socket, the way it points, the seal's size, the keep-clear strip
     * in front of it (the corridor the tunnel to the next sector will run through) and the policy's cost for it ({@code DEAD_END} when nothing can follow).
     */
    public record Expansion(int piece, Vec seal, Facing facing, int w, int h, int d, Box strip, int cost) {
        /** The part of the strip door caps must stay out of (see {@code MegaAssembler.inner}). */
        public Box inner() { return MegaAssembler.inner(strip, facing); }

        /** Does the seal face a wall of older sectors, so that no sector can ever be attached behind it? */
        public boolean deadEnd() { return cost >= MegaAssembler.DEAD_END; }
    }

    public final Box region;
    public final List<Placed> pieces;
    public final List<Loop> loops;
    public final Expansion expansion;
    public final String failure;
    public final List<String> warnings;
    public final long seed;
    public final boolean fallback;
    /** The index of the Landing in {@link #pieces}, or -1. */
    public final int landing;
    /** Counters of what the search tried (doorways, fits, rejections), in the order they were first bumped: the answer to "why did this seed fail". */
    public final Map<String, Integer> stats;

    public Result(Box region, List<Placed> pieces, List<Loop> loops, Expansion expansion, String failure, List<String> warnings, long seed, boolean fallback, int landing,
                  Map<String, Integer> stats) {
        this.region = region;
        this.pieces = List.copyOf(pieces);
        this.loops = List.copyOf(loops);
        this.expansion = expansion;
        this.failure = failure;
        this.warnings = List.copyOf(warnings);
        this.seed = seed;
        this.fallback = fallback;
        this.landing = landing;
        this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
    }

    /** Did the run build a layout (it may still fail the audit)? */
    public boolean assembled() { return failure == null && landing >= 0; }

    public Placed landingPiece() { return landing < 0 ? null : pieces.get(landing); }

    /** Is this piece a room (not a door cap)? Tunnel pieces count as rooms: they are places a player stands. */
    public static boolean isRoom(Placed p) { return p.shape.kind != RoomKind.CAP; }

    /** The pieces that are rooms, in placement order. */
    public List<Placed> rooms() {
        List<Placed> out = new ArrayList<>();
        for (Placed p : pieces) if (isRoom(p)) out.add(p);
        return out;
    }

    /** For every piece index its room id (0.. in placement order among rooms), or -1 for a door cap. */
    public int[] roomIds() {
        int[] ids = new int[pieces.size()];
        int next = 0;
        for (int i = 0; i < ids.length; i++) ids[i] = isRoom(pieces.get(i)) ? next++ : -1;
        return ids;
    }

    /** The doorways of the room graph as pairs of room ids: every parent-child join between two rooms, then every loop. */
    public List<int[]> edges() {
        int[] ids = roomIds();
        List<int[]> out = new ArrayList<>();
        for (Placed p : pieces) {
            if (p.parent >= 0 && ids[p.index] >= 0 && ids[p.parent] >= 0) out.add(new int[] {ids[p.parent], ids[p.index]});
        }
        for (Loop l : loops) {
            if (ids[l.pieceA()] >= 0 && ids[l.pieceB()] >= 0) out.add(new int[] {ids[l.pieceA()], ids[l.pieceB()]});
        }
        return out;
    }

    /** How many pieces of this kind the plan holds. */
    public int count(RoomKind kind) {
        int n = 0;
        for (Placed p : pieces) if (p.shape.kind == kind) n++;
        return n;
    }

    /** The union of all piece boxes (door caps included), or null for an empty plan. */
    public Box bounds() {
        Box b = null;
        for (Placed p : pieces) b = b == null ? p.box : b.union(p.box);
        return b;
    }

    /** A failed run: no pieces, the reason, and whatever warnings were collected. */
    public static Result failed(Box region, String why, List<String> warnings, long seed, boolean fallback, Map<String, Integer> stats) {
        return new Result(region, List.of(), List.of(), null, why, warnings, seed, fallback, -1, stats);
    }
}
