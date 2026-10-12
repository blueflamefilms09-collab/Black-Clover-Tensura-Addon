package com.newuniverse.nusmp.dungeon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a Dungeon Maw looks like when it tears open (docs/living_dungeon.md section 2: "plain stone towers, ruins, or isolated gates").
 * Pure: a {@link Plan} is an ordered list of block placements round the Maw on a FLAT patch, in a frame whose origin is the first free
 * cell above the ground (y=-1 is the ground's top block, x is east, z is south). {@link MawFit} checks that the real ground fits and
 * fills the footings of the low columns, {@link MawManifest} writes it. Every shape keeps the root mechanic of the 1.17 mouth: the Maw's
 * six neighbours are roots or stone, so nothing walks into it until the roots are cut, and there are far more roots than the way in
 * needs, so the Maw stays open after the first gap is chopped.
 */
public final class MawShapes {
    private MawShapes() {}

    /** The ways a Maw shows itself: a plain stone tower, a ruin, an isolated gate, or the 1.17 mouth in the ground (built by DungeonMaw itself). */
    public enum Kind {
        TOWER("Tower"), RUINS("Ruin"), GATE("Gate"), HOLE("Maw");

        /** The word the broadcast uses: "A new Dungeon Tower has breached ...". */
        public final String word;

        Kind(String word) { this.word = word; }

        /** Does this kind come from a plan here? The hole is the 1.17 mouth, which DungeonMaw builds itself. */
        public boolean planned() { return this != HOLE; }
    }

    /** What a cell is made of. AIR clears (grass, leaves, or the ground of a sunken chamber); ROOT is the Maw's root; MAW is the mouth itself. */
    public enum Mat {
        AIR, BRICK, MOSSY, CRACKED, CHISELED, COBBLE, MOSSY_COBBLE, STAIR, SLAB, LADDER, ROOT, MAW;

        /** Stone that holds weight and needs a footing under it. */
        public boolean stone() {
            return switch (this) {
                case BRICK, MOSSY, CRACKED, CHISELED, COBBLE, MOSSY_COBBLE, STAIR, SLAB -> true;
                default -> false;
            };
        }

        /** The order a plan is written in: clear, then stone (and the ladder), then roots, the Maw last. */
        int phase() { return this == AIR ? 0 : this == ROOT ? 2 : this == MAW ? 3 : 1; }
    }

    /** Facings of stairs and ladders (clockwise from north, as in the city's plans) and the flip bit (upside-down stair, top slab). */
    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, FLIP = 4;

    /** The most blocks one manifestation may write (footings come on top, see {@link #FOOT_MAX}); every plan is far below it. */
    public static final int MAX_PUTS = 900;
    /** No plan reaches further than this from its centre column in x or z, so a patch of this reach is at most 9 blocks across. */
    public static final int MAX_REACH = 4;
    /** The deepest a footing may fill under a low column; the steepest slope a plan accepts is never more than this. */
    public static final int FOOT_MAX = 4;
    /** The most the ground outside a doorway may lie below its sill: one block is a step, more is a cliff. */
    public static final int MAX_STEP = 1;

    /** How worn the stone is, in percent of the bricks: mossy, cracked (the rest are plain). */
    public record Style(int mossy, int cracked) {
        public Style {
            if (mossy < 0 || cracked < 0 || mossy + cracked > 100) throw new IllegalArgumentException("worn stone is " + mossy + "% mossy and " + cracked + "% cracked");
        }
    }

    public static final Style PLAIN = new Style(15, 20);

    /** One block of a plan, relative to the origin; {@code variant}: bits 0-1 facing (N E S W), bit 2 flip. */
    public record Put(int x, int y, int z, Mat mat, int variant) {
        public int facing() { return variant & 3; }

        public boolean flip() { return (variant & FLIP) != 0; }
    }

    /** The order the shapes are tried in on a patch: the one the hash chose first, then the others, always the same. The hole has none. */
    public static Kind[] order(Kind first) {
        return switch (first) {
            case TOWER -> new Kind[]{Kind.TOWER, Kind.RUINS, Kind.GATE};
            case RUINS -> new Kind[]{Kind.RUINS, Kind.GATE, Kind.TOWER};
            case GATE -> new Kind[]{Kind.GATE, Kind.TOWER, Kind.RUINS};
            case HOLE -> new Kind[0];
        };
    }

    // ---------------------------------------------------------------- the plan
    /** A finished shape: its blocks in writing order and what the glue needs to know about the ground it stands on. */
    public static final class Plan {
        public final Kind kind;
        public final int rot;
        private final List<Put> puts;
        private final Put maw;
        private final int reach, minY, maxY, roots;
        private final int[] columns, footings, approach;

        private Plan(Kind kind, int rot, List<Put> puts, Put maw, int roots, int[] approach) {
            this.kind = kind;
            this.rot = rot;
            this.puts = List.copyOf(puts);
            this.maw = maw;
            this.roots = roots;
            this.approach = approach;
            int r = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            Set<Long> cols = new LinkedHashSet<>();
            Map<Long, Integer> low = new LinkedHashMap<>();
            for (Put p : puts) {
                r = Math.max(r, Math.max(Math.abs(p.x()), Math.abs(p.z())));
                lo = Math.min(lo, p.y());
                hi = Math.max(hi, p.y());
                long col = column(p.x(), p.z());
                cols.add(col);
                if (p.mat().stone()) low.merge(col, p.y(), Math::min);
            }
            this.reach = r;
            this.minY = lo;
            this.maxY = hi;
            this.columns = new int[cols.size() * 2];
            int i = 0;
            for (long col : cols) { columns[i++] = columnX(col); columns[i++] = columnZ(col); }
            this.footings = new int[low.size() * 3];
            i = 0;
            for (Map.Entry<Long, Integer> e : low.entrySet()) { footings[i++] = columnX(e.getKey()); footings[i++] = columnZ(e.getKey()); footings[i++] = e.getValue(); }
        }

        /** Every block, in the order to write them: the clearing, the stone from the bottom up, the roots, the Maw last. */
        public List<Put> puts() { return puts; }

        /** The mouth. */
        public Put maw() { return maw; }

        /** How many roots wrap it (all of them are lost with the Maw when it closes; the stone stays). */
        public int roots() { return roots; }

        /** The largest |x| or |z| of any block. */
        public int reach() { return reach; }

        public int minY() { return minY; }

        public int maxY() { return maxY; }

        /** How much lower than the highest column of the patch a column may be (the footings fill the difference). */
        public int maxSlope() { return 2; }

        /** The column just outside the doorway (x, z), or null if the shape has no door. The ground there may be at most one block below the sill. */
        public int[] approach() { return approach == null ? null : approach.clone(); }

        /** The distinct columns the plan touches, as x, z pairs. */
        public int[] columns() { return columns.clone(); }

        /** The columns that hold stone, as x, z, lowest-y triples: the glue fills a footing under each down to the ground. */
        public int[] footings() { return footings.clone(); }
    }

    private static long column(int x, int z) { return ((long) (x + 64) << 16) | (z + 64); }

    private static int columnX(long col) { return (int) (col >> 16) - 64; }

    private static int columnZ(long col) { return (int) (col & 0xFFFF) - 64; }

    /**
     * The shape of this kind, turned {@code rot} quarter turns, with its stone worn as {@code style} says and every free choice taken
     * from {@code seed}: the same arguments always give the same plan.
     */
    public static Plan plan(Kind kind, int rot, long seed, Style style) {
        if (!kind.planned()) throw new IllegalArgumentException(kind + " has no plan: DungeonMaw builds it");
        Canvas c = new Canvas(rot);
        Rng r = new Rng(seed ^ ((kind.ordinal() + 1) * 0x9E3779B97F4A7C15L));
        switch (kind) {
            case TOWER -> tower(c, r, style);
            case RUINS -> ruins(c, r, style);
            default -> gate(c, r, style);
        }
        return c.finish(kind);
    }

    // ---------------------------------------------------------------- the tower
    /**
     * A plain stone tower, 5x5, eight to eleven blocks high, with a doorway in one wall and a ladder up the inside to a battlemented deck.
     * The Maw sits on the floor at the back, in a cocoon of roots; the doorway is plugged with roots too, and roots hang under the deck.
     */
    private static void tower(Canvas c, Rng r, Style st) {
        int h = 7 + r.below(4);                                       // wall height 7..10; with the battlements the tower stands 8..11
        c.box(Mat.AIR, 0, -1, 0, -1, 1, h + 1, 1);                     // the inside, cleared of leaves and grass
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) c.put(floor(r, st), 0, x, -1, z);
        for (int y = 0; y < h; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;
                    if (x == 0 && z == 2 && y <= 1) continue;          // the doorway
                    boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                    c.put(corner && (y == 0 || y == h - 1) ? Mat.CHISELED : brick(r, st), 0, x, y, z);
                }
            }
        }
        for (int[] s : new int[][]{{-2, 3, 0}, {0, 3, -2}, {0, h - 2, 2}, {-2, h - 2, 0}, {0, h - 2, -2}}) c.put(Mat.AIR, 0, s[0], s[1], s[2]);   // arrow slits
        for (int i = 2 + r.below(3); i > 0; i--) {                    // a few bricks long gone from the upper walls (never the ladder's wall)
            int side = r.below(4), t = r.range(-1, 1), y = r.range(4, h - 3);
            int x = side == 0 ? t : side == 1 ? 2 : side == 2 ? t : -2, z = side == 0 ? -2 : side == 1 ? t : side == 2 ? 2 : t;
            if (x == 2 && z == 0) continue;
            c.put(Mat.AIR, 0, x, y, z);
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (!(x == 1 && z == 0)) c.put(brick(r, st), 0, x, h - 1, z);    // the deck, with a hatch for the ladder
        for (int x = -2; x <= 2; x++) {                                // the battlements: every second block of the rim
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;
                if (Math.floorMod(x + z, 2) != 0) continue;
                if ((x != 2 || z != 0) && r.chance(18)) continue;     // a broken merlon (the ladder's own is kept)
                c.put(brick(r, st), 0, x, h, z);
            }
        }
        for (int y = 0; y <= h; y++) c.put(Mat.LADDER, WEST, 1, y, 0);   // up the east wall, through the hatch, one above the deck
        // the roots: the doorway plug, the cocoon round the Maw (three columns and a cap) and some loose growth
        c.put(Mat.ROOT, 0, 0, 0, 2);
        c.put(Mat.ROOT, 0, 0, 1, 2);
        c.put(Mat.AIR, 0, 0, 0, 3);                                    // the doorstep, cleared
        c.put(Mat.AIR, 0, 0, 1, 3);
        c.approach(0, 3);
        column(c, -1, -1, r.range(2, 3));
        column(c, 1, -1, r.range(2, 3));
        column(c, 0, 0, r.range(2, 3));
        c.put(Mat.ROOT, 0, 0, 1, -1);
        column(c, -1, 0, r.range(1, 2));
        if (r.chance(50)) column(c, -1, 1, 1);
        for (int i = 0; i < 3; i++) {                                  // roots hanging from under the deck
            int x = r.range(-1, 1), z = r.range(-1, 1);
            if (x == 1 && z == 0) continue;
            for (int k = r.range(1, 2); k > 0; k--) c.putIfAbsent(Mat.ROOT, 0, x, h - 1 - k, z);
        }
        c.put(Mat.MAW, 0, 0, 0, -1);
    }

    // ---------------------------------------------------------------- the gate
    /**
     * An isolated gate: two pillars and a lintel on a low plinth, the Maw at the foot of the frame, roots filling the frame and matting its
     * two faces. Walk through along z; the pillars stand east and west of the middle.
     */
    private static void gate(Canvas c, Rng r, Style st) {
        c.box(Mat.AIR, 0, -3, 0, -1, 3, 7, 1);
        for (int x = -3; x <= 3; x++) for (int z = -1; z <= 1; z++) c.put(floor(r, st), 0, x, -1, z);
        for (int s : new int[]{-2, 2}) {
            for (int y = 0; y <= 4; y++) c.put(y == 0 || y == 4 ? Mat.CHISELED : brick(r, st), 0, s, y, 0);
            c.put(Mat.STAIR, NORTH, s, 0, 1);                          // a step hugging each pillar's foot, front and back
            c.put(Mat.STAIR, SOUTH, s, 0, -1);
        }
        for (int x = -2; x <= 2; x++) c.put(x == 0 ? Mat.CHISELED : brick(r, st), 0, x, 5, 0);   // the lintel, a keystone in the middle
        for (int x = -3; x <= 3; x++) c.put(Mat.SLAB, 0, x, 6, 0);                                  // the cap, overhanging
        for (int x = -1; x <= 1; x++) {                                // the frame, filled with roots (the cocoon cells are never skipped)
            for (int y = 0; y <= 4; y++) {
                if (x == 0 && y == 0) continue;
                boolean must = (x != 0 && y == 0) || (x == 0 && y == 1);
                if (must || r.chance(80)) c.put(Mat.ROOT, 0, x, y, 0);
            }
        }
        for (int z : new int[]{-1, 1}) {                               // and a mat of roots on each face, over the mouth
            c.put(Mat.ROOT, 0, 0, 0, z);
            c.put(Mat.ROOT, 0, 0, 1, z);
            for (int x : new int[]{-1, 1}) if (r.chance(60)) c.putIfAbsent(Mat.ROOT, 0, x, 0, z);
            if (r.chance(50)) c.putIfAbsent(Mat.ROOT, 0, 0, 2, z);
        }
        rubble(c, r, st, -3, 3, -1, 1, 22);
        c.put(Mat.MAW, 0, 0, 0, 0);
    }

    // ---------------------------------------------------------------- the ruins
    /**
     * Ruins: a half-sunken chamber (a 3x3 shaft two blocks into the ground, walled in stone, broken down on one side) with the Maw on its
     * floor under a cocoon of roots, roots lying over the breach; round it the stumps of a courtyard wall, fallen columns and rubble.
     */
    private static void ruins(Canvas c, Rng r, Style st) {
        c.box(Mat.AIR, 0, -1, -2, -1, 1, 3, 1);                        // the shaft: ground below the surface, grass and leaves above
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) c.put(floor(r, st), 0, x, -3, z);
        int breach = r.below(4);                                       // the side the wall has fallen on
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != 2) continue;
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                int top = switch (r.below(10)) { case 0, 1, 2 -> 0; case 3, 4, 5 -> 1; case 6, 7 -> 2; default -> -1; };
                if (corner) top = Math.max(top, 1);
                if (!corner && sideOf(x, z) == breach) top = -1;
                for (int y = -2; y <= top; y++) c.put(brick(r, st), 0, x, y, z);
                if (!corner && sideOf(x, z) == breach) c.put(Mat.ROOT, 0, x, 0, z);   // roots lie over the gap
            }
        }
        for (int x = -1; x <= 1; x++) {                                // the cocoon: the four neighbours of the Maw stand at least two high, the rest vary
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                int hgt = (x == 0 || z == 0) ? r.range(2, 3) : r.range(1, 3);
                for (int k = 0; k < hgt; k++) c.put(Mat.ROOT, 0, x, -2 + k, z);
            }
        }
        c.put(Mat.ROOT, 0, 0, -1, 0);                                  // the cap over the Maw
        for (int side = 0; side < 4; side++) {                         // stumps of the courtyard wall
            if (!r.chance(65)) continue;
            int len = 2 + r.below(3), t0 = r.range(-3, 4 - len);
            for (int i = 0; i < len; i++) {
                int hgt = Math.max(1, Math.min(3, 1 + r.below(3) - (i == 0 || i == len - 1 ? 1 : 0)));
                for (int y = 0; y < hgt; y++) c.putIfAbsent(brick(r, st), 0, edgeX(side, t0 + i, 4), y, edgeZ(side, t0 + i, 4));
            }
        }
        for (int sx : new int[]{-4, 4}) {
            for (int sz : new int[]{-4, 4}) {
                if (!r.chance(70)) continue;
                for (int y = 0, hgt = 1 + r.below(3); y < hgt; y++) c.putIfAbsent(y == 0 ? Mat.CHISELED : brick(r, st), 0, sx, y, sz);
            }
        }
        int fallen = -1;
        for (int i = 0; i < 2; i++) {                                  // two fallen columns on different sides: drums of stone lying on the ground, capped at the ends
            int side = fallen < 0 ? r.below(4) : (fallen + 1 + r.below(3)) & 3, len = 3 + r.below(2), t0 = r.range(-3, 4 - len);
            fallen = side;
            for (int k = 0; k < len; k++) c.putIfAbsent(k == 0 || k == len - 1 ? Mat.CHISELED : brick(r, st), 0, edgeX(side, t0 + k, 3), 0, edgeZ(side, t0 + k, 3));
        }
        rubble(c, r, st, -4, 4, -4, 4, 9);
        c.put(Mat.MAW, 0, 0, -2, 0);
    }

    // ---------------------------------------------------------------- shared parts
    /** A column of roots from the floor up. */
    private static void column(Canvas c, int x, int z, int height) {
        for (int y = 0; y < height; y++) c.put(Mat.ROOT, 0, x, y, z);
    }

    /** Loose stone on the ground inside a rectangle, wherever nothing stands: cobble, mossy cobble, cracked bricks, half slabs. */
    private static void rubble(Canvas c, Rng r, Style st, int x1, int x2, int z1, int z2, int percent) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) < 3 || !r.chance(percent)) continue;
                switch (r.below(4)) {
                    case 0 -> c.putIfAbsent(Mat.COBBLE, 0, x, 0, z);
                    case 1 -> c.putIfAbsent(Mat.MOSSY_COBBLE, 0, x, 0, z);
                    case 2 -> c.putIfAbsent(Mat.CRACKED, 0, x, 0, z);
                    default -> c.putIfAbsent(Mat.SLAB, 0, x, 0, z);
                }
            }
        }
    }

    /** Which side of the 5x5 ring a cell is on (0 north, 1 east, 2 south, 3 west); a corner belongs to none that matters. */
    private static int sideOf(int x, int z) {
        if (z == -2 && Math.abs(x) < 2) return NORTH;
        if (x == 2 && Math.abs(z) < 2) return EAST;
        if (z == 2 && Math.abs(x) < 2) return SOUTH;
        return WEST;
    }

    /** The point t along a side at this distance from the middle: x of it. */
    private static int edgeX(int side, int t, int dist) { return side == NORTH || side == SOUTH ? t : side == EAST ? dist : -dist; }

    private static int edgeZ(int side, int t, int dist) { return side == EAST || side == WEST ? t : side == NORTH ? -dist : dist; }

    /** Worn bricks: mossy or cracked as the style says, plain otherwise. */
    private static Mat brick(Rng r, Style st) {
        int v = r.below(100);
        return v < st.mossy() ? Mat.MOSSY : v < st.mossy() + st.cracked() ? Mat.CRACKED : Mat.BRICK;
    }

    /** A floor: bricks, or cobble where the paving is gone. */
    private static Mat floor(Rng r, Style st) {
        return r.chance(30) ? (r.chance(Math.max(20, st.mossy())) ? Mat.MOSSY_COBBLE : Mat.COBBLE) : brick(r, st);
    }

    // ---------------------------------------------------------------- the canvas
    /** The blocks of a shape under construction, turned by the plan's quarter turns; a later put on a cell replaces an earlier one. */
    private static final class Canvas {
        private final int rot;
        private final Map<Long, Put> cells = new LinkedHashMap<>();
        private int[] approach;

        Canvas(int rot) { this.rot = rot & 3; }

        /** Marks the column in front of the doorway (local x, z). */
        void approach(int x, int z) { approach = new int[]{rx(x, z), rz(x, z)}; }

        private int rx(int x, int z) { return switch (rot) { case 0 -> x; case 1 -> -z; case 2 -> -x; default -> z; }; }

        private int rz(int x, int z) { return switch (rot) { case 0 -> z; case 1 -> x; case 2 -> -z; default -> -x; }; }

        private static long key(int x, int y, int z) { return ((long) (x + 64) << 32) | ((long) (y + 64) << 16) | (z + 64); }

        void put(Mat m, int variant, int x, int y, int z) {
            int tx = rx(x, z), tz = rz(x, z);
            int v = (variant & ~3) | (((variant & 3) + rot) & 3);       // a stair or ladder turns with the shape
            cells.put(key(tx, y, tz), new Put(tx, y, tz, m, v));
        }

        /** Puts only where nothing stands yet (a cell that was only cleared counts as free). */
        void putIfAbsent(Mat m, int variant, int x, int y, int z) {
            Put there = cells.get(key(rx(x, z), y, rz(x, z)));
            if (there == null || there.mat() == Mat.AIR) put(m, variant, x, y, z);
        }

        void box(Mat m, int variant, int x1, int y1, int z1, int x2, int y2, int z2) {
            for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) put(m, variant, x, y, z);
        }

        Plan finish(Kind kind) {
            List<Put> ordered = new ArrayList<>(cells.values());
            ordered.sort(Comparator.<Put>comparingInt(p -> p.mat().phase()).thenComparingInt(p -> p.mat().phase() == 1 ? p.y() : 0));
            Put maw = null;
            int roots = 0;
            for (Put p : ordered) {
                if (p.mat() == Mat.MAW) { if (maw != null) throw new IllegalStateException("two mouths"); maw = p; }
                if (p.mat() == Mat.ROOT) roots++;
            }
            if (maw == null) throw new IllegalStateException("no mouth");
            return new Plan(kind, rot, ordered, maw, roots, approach);
        }
    }

    /** SplitMix64: the same seed gives the same shape on every machine. */
    private static final class Rng {
        private long state;

        Rng(long seed) { state = seed; }

        private long next() {
            long z = (state += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        /** 0 .. n-1 */
        int below(int n) { return (int) ((next() >>> 33) % n); }

        /** lo .. hi, both included */
        int range(int lo, int hi) { return lo + below(hi - lo + 1); }

        boolean chance(int percent) { return below(100) < percent; }
    }
}
