package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: {@code /dungeon debug wfc} has to show an admin, inside the running game, whether the floor they stand on was laid out the way the owner's anti-dead-end rule says (every
 * room on a loop, no room that only leads to nowhere). The judge is not new: {@link RoomGraph} (rooms are nodes, doorways are edges) already knows bridges, cycles, dead ends and
 * terminals, and it is what the cyclic generator and its CI self-test use, so this class asks the SAME graph about the SAVED floor (its rooms and its doorway pairs) and turns the
 * answer into pictures. The colour code is the one the owner asked for and it is stated per edge and per room: GREEN = connected on a loop (removing the doorway leaves the floor
 * connected), YELLOW = single link (a bridge: the only way between its two sides, a room that has no loop through it), RED = dead end (a non-terminal room with one doorway or none,
 * and the doorway that leads into it); the arrival stairs, forward stairs, boss chain and Safe Zone are terminals and never red, as everywhere else in the cyclic layer.
 *
 * <p>The picture is built as three layers of points, all in world coordinates, with no Minecraft type so the geometry and the throttle are unit tested: the OUTLINE of every room's
 * bounding box, the GRAPH LINES (room centre to doorway to room centre) and the SOCKET NODES (one at every doorway). A burst takes a small share from each layer, from where the
 * last burst stopped, and only points within range of the player, so a floor of thirty rooms is drawn a few hundred particles at a time, over a second or two, never in one packet
 * storm. The constants below are the whole throttle: one burst every {@value #BURST_EVERY} ticks, at most {@value #MAX_PER_BURST} points per burst, at most {@value #MAX_PER_TICK}
 * across all admins in one tick, off after five minutes. Pure.
 */
public final class WfcDebugModel {
    private WfcDebugModel() {}

    // ---------------------------------------------------------------- the throttle
    /** Ticks between two bursts to one admin. */
    public static final int BURST_EVERY = 10;
    /** Most points one burst sends. */
    public static final int MAX_PER_BURST = 160;
    /** Most points all admins together get in one tick (bursts are staggered by player, this is the hard stop). */
    public static final int MAX_PER_TICK = 200;
    /** The debug view switches itself off after this many ticks (five minutes). */
    public static final long TTL_TICKS = 20L * 60L * 5L;
    /** Points farther than this from the admin are not sent (blocks). */
    public static final double RANGE = 72.0;
    /** Spacing of the points along a box outline and along a graph line (blocks). */
    public static final double BOX_STEP = 3.0, LINE_STEP = 2.0;

    /** Is this the tick of the admin's burst? {@code offset} staggers admins (any int, e.g. a hash of the player id). */
    public static boolean burstNow(long tick, int offset) { return Math.floorMod(tick + (long) offset, (long) BURST_EVERY) == 0L; }

    /** Has the view run its five minutes? */
    public static boolean expired(long startTick, long nowTick) { return nowTick - startTick >= TTL_TICKS; }

    // ---------------------------------------------------------------- the colour code and the layers
    public enum Hue { GREEN, YELLOW, RED }

    /** Which layer a point belongs to (the renderer draws nodes larger than lines). */
    public enum Kind { NODE, EDGE, BOX }

    /** One room as the model needs it: its id, the roles of {@link Roles} (terminals), its category (may be null) and its bounding box in blocks, both ends inclusive. */
    public record RoomIn(int id, int roles, RoomCategory category, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cx() { return (minX + maxX + 1) / 2.0; }

        double cz() { return (minZ + maxZ + 1) / 2.0; }
    }

    /** The numbers of the summary line. {@code skippedEdges} are doorways that named a room the floor does not have. */
    public record Summary(int rooms, int edges, int skippedEdges, int deadEnds, int loops, int onLoop, int singleLink, int bridges, int components) {
        /** The owner's rule: no dead ends, and the floor is one connected piece. */
        public boolean clean() { return rooms > 0 && deadEnds == 0 && components <= 1 && skippedEdges == 0; }
    }

    /** A growable list of coloured points. */
    public static final class Layer {
        private double[] xyz = new double[96];
        private byte[] hue = new byte[32];
        private int n;

        void add(double x, double y, double z, Hue h) {
            if (n == hue.length) {
                xyz = java.util.Arrays.copyOf(xyz, xyz.length * 2);
                hue = java.util.Arrays.copyOf(hue, hue.length * 2);
            }
            xyz[3 * n] = x;
            xyz[3 * n + 1] = y;
            xyz[3 * n + 2] = z;
            hue[n++] = (byte) h.ordinal();
        }

        public int count() { return n; }

        public double x(int i) { return xyz[3 * i]; }

        public double y(int i) { return xyz[3 * i + 1]; }

        public double z(int i) { return xyz[3 * i + 2]; }

        public Hue hue(int i) { return Hue.values()[hue[i]]; }
    }

    /** The finished picture of one floor. */
    public static final class Model {
        public final Summary summary;
        public final Layer nodes = new Layer(), edges = new Layer(), boxes = new Layer();
        /** The colour of each room, in the order the rooms were given. */
        public final Hue[] roomHue;
        /** The colour of each doorway that was drawn, in graph order (smaller room first). */
        public final List<Hue> edgeHue = new ArrayList<>();

        Model(Summary summary, Hue[] roomHue) {
            this.summary = summary;
            this.roomHue = roomHue;
        }

        public int points() { return nodes.count() + edges.count() + boxes.count(); }
    }

    // ---------------------------------------------------------------- building the model
    /**
     * Judges a floor and draws it. {@code rooms} are the saved rooms, {@code doorways} the saved pairs of room ids that are joined (an unknown id is skipped and counted), {@code floorY}
     * the world row of the floor (the lines run a block and a half above it, where an admin's eyes are).
     */
    public static Model build(List<RoomIn> rooms, List<int[]> doorways, int floorY) {
        RoomGraph g = new RoomGraph();
        Map<Integer, Integer> nodeOf = new HashMap<>();
        for (RoomIn r : rooms) nodeOf.put(r.id(), g.addNode(r.category(), r.roles()));
        int skipped = 0;
        for (int[] d : doorways) {
            Integer a = d.length < 2 ? null : nodeOf.get(d[0]), b = d.length < 2 ? null : nodeOf.get(d[1]);
            if (a == null || b == null || a.equals(b)) {
                skipped++;
                continue;
            }
            g.addEdge(a, b);
        }
        int size = g.size();
        BitSet bridges = g.bridges();
        BitSet onCycle = g.onCycle();
        boolean[] dead = new boolean[size];
        for (int d : g.deadEnds(Roles.TERMINAL)) dead[d] = true;
        Hue[] roomHue = new Hue[size];
        int dead0 = 0, single = 0;
        for (int i = 0; i < size; i++) {
            roomHue[i] = dead[i] ? Hue.RED : onCycle.get(i) ? Hue.GREEN : Hue.YELLOW;
            if (dead[i]) dead0++;
            else if (!onCycle.get(i)) single++;
        }
        Summary summary = new Summary(size, g.edgeCount(), skipped, dead0, g.cyclomatic(), onCycle.cardinality(), single, bridges.cardinality(), g.components());
        Model m = new Model(summary, roomHue);

        // the outline of every room
        for (int i = 0; i < size; i++) outline(m.boxes, rooms.get(i), roomHue[i]);
        // the doorways and the lines through them
        double lineY = floorY + 1.5;
        for (int[] e : g.edgeList()) {
            int a = e[0], b = e[1];
            Hue h = dead[a] || dead[b] ? Hue.RED : bridges.get(a * size + b) ? Hue.YELLOW : Hue.GREEN;
            m.edgeHue.add(h);
            RoomIn ra = rooms.get(a), rb = rooms.get(b);
            double[] door = doorway(ra, rb);
            m.nodes.add(door[0], lineY, door[1], h);
            line(m.edges, ra.cx(), lineY, ra.cz(), door[0], lineY, door[1], h);
            line(m.edges, door[0], lineY, door[1], rb.cx(), lineY, rb.cz(), h);
        }
        return m;
    }

    /**
     * The doorway between two rooms, as {x, z} in block-edge coordinates: the middle of the stretch their boxes share on each axis. Rooms that touch (the usual case: a doorway is
     * where two boxes meet) share a line on the axis they meet on; rooms that do not touch (a piece between them is not stored) get the middle between their centres on that axis.
     */
    public static double[] doorway(RoomIn a, RoomIn b) {
        return new double[] {middle(a.minX(), a.maxX() + 1, b.minX(), b.maxX() + 1), middle(a.minZ(), a.maxZ() + 1, b.minZ(), b.maxZ() + 1)};
    }

    private static double middle(int aLo, int aHi, int bLo, int bHi) {
        int lo = Math.max(aLo, bLo), hi = Math.min(aHi, bHi);
        return lo <= hi ? (lo + hi) / 2.0 : ((aLo + aHi) / 2.0 + (bLo + bHi) / 2.0) / 2.0;
    }

    private static void outline(Layer out, RoomIn r, Hue h) {
        double x0 = r.minX(), x1 = r.maxX() + 1.0, y0 = r.minY(), y1 = r.maxY() + 1.0, z0 = r.minZ(), z1 = r.maxZ() + 1.0;
        for (double y : new double[] {y0, y1}) {
            for (double z : new double[] {z0, z1}) line(out, x0, y, z, x1, y, z, h, BOX_STEP);
            for (double x : new double[] {x0, x1}) line(out, x, y, z0, x, y, z1, h, BOX_STEP);
        }
        for (double x : new double[] {x0, x1}) for (double z : new double[] {z0, z1}) line(out, x, y0, z, x, y1, z, h, BOX_STEP);
    }

    private static void line(Layer out, double x0, double y0, double z0, double x1, double y1, double z1, Hue h) { line(out, x0, y0, z0, x1, y1, z1, h, LINE_STEP); }

    /** Points along a segment, the first included and the last included, about {@code step} apart. */
    private static void line(Layer out, double x0, double y0, double z0, double x1, double y1, double z1, Hue h, double step) {
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int parts = Math.max(1, (int) Math.ceil(len / step));
        for (int i = 0; i <= parts; i++) {
            double t = (double) i / parts;
            out.add(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, h);
        }
    }

    // ---------------------------------------------------------------- one burst
    /** Where the last burst stopped, per layer: the next burst starts there, so everything in range is drawn in turn. */
    public static final class Cursor {
        public int nodes, edges, boxes;
    }

    /** The points of one burst (parallel arrays: x, y, z triples, the {@link Hue} ordinal and the {@link Kind} ordinal of each point). */
    public record Burst(double[] xyz, byte[] hue, byte[] kind) {
        public int count() { return hue.length; }
    }

    /**
     * The next burst for an admin at (px, py, pz): at most {@code budget} points, shared 40 % to the socket nodes, 35 % to the graph lines and the rest to the room outlines, each
     * layer from its own cursor and only points within {@code range} blocks. A layer with fewer points in range than its share simply sends fewer.
     */
    public static Burst next(Model m, Cursor c, double px, double py, double pz, double range, int budget) {
        int total = Math.max(0, budget);
        int qNodes = total * 2 / 5, qEdges = total * 7 / 20, qBoxes = total - qNodes - qEdges;
        double r2 = range * range;
        List<double[]> pts = new ArrayList<>();
        List<Byte> hues = new ArrayList<>(), kinds = new ArrayList<>();
        c.nodes = take(m.nodes, c.nodes, qNodes, px, py, pz, r2, Kind.NODE, pts, hues, kinds);
        c.edges = take(m.edges, c.edges, qEdges, px, py, pz, r2, Kind.EDGE, pts, hues, kinds);
        c.boxes = take(m.boxes, c.boxes, qBoxes, px, py, pz, r2, Kind.BOX, pts, hues, kinds);
        double[] xyz = new double[pts.size() * 3];
        byte[] hue = new byte[pts.size()], kind = new byte[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            xyz[3 * i] = pts.get(i)[0];
            xyz[3 * i + 1] = pts.get(i)[1];
            xyz[3 * i + 2] = pts.get(i)[2];
            hue[i] = hues.get(i);
            kind[i] = kinds.get(i);
        }
        return new Burst(xyz, hue, kind);
    }

    private static int take(Layer l, int cursor, int quota, double px, double py, double pz, double r2, Kind kind, List<double[]> pts, List<Byte> hues, List<Byte> kinds) {
        int n = l.count();
        if (n == 0 || quota <= 0) return n == 0 ? 0 : Math.floorMod(cursor, n);
        int pos = Math.floorMod(cursor, n);
        int taken = 0;
        for (int scanned = 0; scanned < n && taken < quota; scanned++) {
            double dx = l.x(pos) - px, dy = l.y(pos) - py, dz = l.z(pos) - pz;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                pts.add(new double[] {l.x(pos), l.y(pos), l.z(pos)});
                hues.add((byte) l.hue(pos).ordinal());
                kinds.add((byte) kind.ordinal());
                taken++;
            }
            pos = (pos + 1) % n;
        }
        return pos;
    }
}
