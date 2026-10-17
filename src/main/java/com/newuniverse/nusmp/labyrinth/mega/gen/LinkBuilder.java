package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the bridge between two sectors is not a room to search for, it is a straight run of identical-looking tunnel segments from the seal of the old
 * sector to the door of the new Landing, so it is built directly (turn each segment so its two doors line up with the run, hang it on the previous one)
 * instead of by the random search. Every segment is exactly one module long (9 blocks) so the run lands on the Landing's module grid; every eighth segment
 * is a waystation (a wider piece with a bench, a lantern and a map niche) so a long run stays a place with something in it. Waystations are optional
 * decoration: one that would not fit beside an obstacle is replaced by a plain segment. The run is described as {@link Placed} pieces with the final indices
 * 0..n-1, flagged as tunnel pieces, so the assembler installs them first and hangs the Landing from the last one, and the placer, the plan file and the
 * audit treat them like any other piece. Pure and deterministic like the assembler.
 */
public final class LinkBuilder {
    private LinkBuilder() {}

    /** Every how-many-th segment is a waystation. */
    public static final int STATION_EVERY = 8;

    /** The segments, the cell where the next sector's Landing door goes, and what went wrong. */
    public record Tunnel(List<Placed> pieces, Vec landingFront, int lastOutSocket, String failure) {
        public boolean ok() { return failure == null; }
    }

    /** The pair of doors of a shape that a straight run passes through: the one it is entered by and the opposite one it is left by. */
    private record Through(int in, int out) {}

    /**
     * Builds {@code modules} segments in front of {@code seal} (the jigsaw cell of the old sector's sealed socket) running towards {@code facing}.
     * {@code free} says whether a box is clear of obstacles (used only for waystations, which may be wider than the run).
     */
    public static Tunnel build(PoolSource src, Vec seal, Facing facing, int modules, long seed, Predicate<Box> free) {
        if (modules <= 0) return new Tunnel(List.of(), seal.step(facing), -1, null);
        PoolDef pool = src.pool(MegaGrid.P_LINK);
        if (pool == null || pool.isEmpty()) return fail("the link pool " + MegaGrid.P_LINK + " is missing or empty");
        List<PoolEntry> straights = new ArrayList<>(), stations = new ArrayList<>();
        for (PoolEntry e : pool.entries()) {
            Shape sh = src.shape(e.shape());
            if (sh == null || through(sh) == null) continue;
            (isStation(e.shape()) ? stations : straights).add(e);
        }
        if (straights.isEmpty() && stations.isEmpty()) return fail("no template of " + MegaGrid.P_LINK + " has two opposite doors one module apart");
        Rng rng = new Rng(Rng.mix(seed, 0x4C494E4BL));
        List<Placed> out = new ArrayList<>();
        Vec inFront = seal.step(facing);
        int prevOut = -1;
        for (int n = 0; n < modules; n++) {
            boolean wantStation = !stations.isEmpty() && (n + 1) % STATION_EVERY == 0;
            Placed p = null;
            if (wantStation) p = segment(src, pick(stations, rng), facing, inFront, n, prevOut, rng, free);
            if (p == null && !straights.isEmpty()) p = segment(src, pick(straights, rng), facing, inFront, n, prevOut, rng, null);
            if (p == null) p = segment(src, pick(stations.isEmpty() ? straights : stations, rng), facing, inFront, n, prevOut, rng, null);
            if (p == null) return fail("a tunnel segment does not line up with the run");
            if (n > 0) out.get(n - 1).markUsed(prevOut, true);
            out.add(p);
            prevOut = outOf(p);
            inFront = p.socketFront(prevOut);
        }
        return new Tunnel(out, inFront, prevOut, null);
    }

    private static Tunnel fail(String why) { return new Tunnel(List.of(), null, -1, why); }

    private static Placed segment(PoolSource src, PoolEntry e, Facing facing, Vec inFront, int n, int prevOut, Rng rng, Predicate<Box> free) {
        Shape sh = src.shape(e.shape());
        Through t = through(sh);
        if (t == null) return null;
        boolean swap = rng.nextInt(2) == 0;                         // a piece with two equal ends can be entered by either: variety
        int in = swap ? t.out() : t.in();
        int out = swap ? t.in() : t.out();
        Socket ks = sh.sockets.get(in);
        int r = Rot.turnsFor(ks.facing(), facing.opposite());
        Vec o = inFront.sub(Rot.cell(r, ks.cell()));
        Placed p = new Placed(n, sh, e.processors(), r, o, -1, n - 1, prevOut, in, true);
        if (p.socketFacing(out) != facing || !p.socketFront(out).equals(inFront.step(facing, MegaGrid.MODULE))) return null;
        if (free != null && !free.test(p.box)) return null;
        return p;
    }

    /** The door a segment is left by: the other door of its through pair. */
    private static int outOf(Placed p) {
        Through t = through(p.shape);
        return p.ownSocket == t.in() ? t.out() : t.in();
    }

    private static PoolEntry pick(List<PoolEntry> list, Rng rng) {
        int total = 0;
        for (PoolEntry e : list) total += e.weight();
        int r = rng.nextInt(total);
        for (PoolEntry e : list) {
            if (r < e.weight()) return e;
            r -= e.weight();
        }
        return list.get(list.size() - 1);
    }

    private static boolean isStation(String templateId) {
        int slash = templateId.lastIndexOf('/');
        return templateId.substring(slash + 1).startsWith("waystation");
    }

    /** The two doors a run passes through: both ordinary doors, on opposite faces, on the same line and exactly one module (8 cells between them) apart. */
    private static Through through(Shape sh) {
        List<Socket> s = sh.sockets;
        for (int i = 0; i < s.size(); i++) {
            for (int j = i + 1; j < s.size(); j++) {
                Socket a = s.get(i), b = s.get(j);
                if (!a.isDoor() || !b.isDoor() || a.facing() != b.facing().opposite()) continue;
                int along = Math.abs(b.cell().along(a.facing()) - a.cell().along(a.facing()));
                int lateral = a.facing().alongX() ? b.cell().z() - a.cell().z() : b.cell().x() - a.cell().x();
                if (lateral == 0 && along == MegaGrid.MODULE - 1) return new Through(i, j);
            }
        }
        return null;
    }
}
