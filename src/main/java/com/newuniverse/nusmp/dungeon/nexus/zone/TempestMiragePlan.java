package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Tempest Mirage, planned (docs/living_dungeon.md section 5, zone 3). A pure, deterministic layout with no Minecraft imports: the core
 * cell goes in, a {@link Layout} comes out, the same on every machine.
 *
 * <p>A twisted, overgrown illusion of a peaceful monster village: one oval cavern, 100 by 92 blocks across the floor, walled in a stone
 * shell three blocks thick all round so the flooded caves of the Dungeon cannot pour in, with a dome of moss, hanging roots and glow berries
 * for a sky. In the middle a paved plaza with a standard (the hive's rally point), a town hall of spruce and mud brick, and behind the hall
 * the Treasury, whose doorway is left open for the mana gate. Round it two rings of mud, timber and thatch houses, wells, market stalls,
 * laundry lines, lamp posts and giant mushrooms, all grown over with vines and moss. Four village gates in the shell, on the four sides,
 * are sealed with a plug; {@link TempestWays} finds the way from each to a cave and the glue cuts it. A few of the houses are fakes
 * ({@link Fake}): the glue lets them dissolve into an ambush when someone walks in.
 *
 * <p>Ops are ordered in phases: carve; shell, floor and roads; structures (hall, treasury, gates, houses, wells, stalls); details
 * (lamps, laundry, mushrooms, floor growth, ceiling growth). Everything is written in the cell's own offsets from the centre.
 */
public final class TempestMiragePlan {
    private TempestMiragePlan() {}

    // ---------------------------------------------------------------- the size of the cavern
    /** Half-widths of the air at the floor (x, z), the thickness of the shell, and the half-widths of the whole (the plan's footprint). */
    public static final int A = 50, B = 46, WALL = 3, OUTER_X = A + WALL, OUTER_Z = B + WALL;
    /** The air above the floor at the middle, how high the wall stands straight, the roof's thickness and the foundation's. */
    public static final int HEIGHT = 30, WALL_H = 12, ROOF = 3, FOUNDATION = 3;
    /** The cavern's centre may sit this far from the middle of its core cell. */
    public static final int JITTER = 8;
    /** The plaza (half-widths), the lane that rings the village, and where the town hall stands (in its own frame: front is +z). */
    public static final int PLAZA_X = 24, PLAZA_Z = 22, RING_X = 38, RING_Z = 35, RING_W = 2;
    public static final int HALL_X = 12, HALL_BACK = -6, HALL_FRONT = 12, TREAS_X = 8, TREAS_BACK = -18;
    /** Where the standard stands in the hall's frame, and the rally point in front of it. */
    public static final int STANDARD_Z = 17, RALLY_Z = 14;
    /** The cells of a gate's passage, counted from the interior edge d outward: the first cell the glue cuts, and the cell of the open door. */
    public static final int GATE_FIRST = 1, GATE_DOOR = WALL;

    // ---------------------------------------------------------------- blocks
    private static final String STONE_BRICKS = "minecraft:stone_bricks", MOSSY_BRICKS = "minecraft:mossy_stone_bricks", MOSSY_COBBLE = "minecraft:mossy_cobblestone",
            COBBLE = "minecraft:cobblestone", SMOOTH = "minecraft:smooth_stone", MOSS = "minecraft:moss_block", GRASS = "minecraft:grass_block",
            PODZOL = "minecraft:podzol", ROOTED = "minecraft:rooted_dirt", MYCELIUM = "minecraft:mycelium", COARSE = "minecraft:coarse_dirt",
            PATH = "minecraft:dirt_path", GRAVEL = "minecraft:gravel", MUD = "minecraft:mud_bricks", PACKED = "minecraft:packed_mud",
            SPRUCE = "minecraft:spruce_planks", SPRUCE_LOG = "minecraft:spruce_log", STRIPPED = "minecraft:stripped_spruce_log",
            SPRUCE_STAIRS = "minecraft:spruce_stairs", SPRUCE_SLAB = "minecraft:spruce_slab", SPRUCE_FENCE = "minecraft:spruce_fence",
            DARK = "minecraft:dark_oak_planks", DARK_STAIRS = "minecraft:dark_oak_stairs", DARK_SLAB = "minecraft:dark_oak_slab",
            DARK_LOG = "minecraft:dark_oak_log", BAMBOO = "minecraft:bamboo_planks", BAMBOO_BLOCK = "minecraft:bamboo_block",
            MUD_STAIRS = "minecraft:mud_brick_stairs",
            MUD_SLAB = "minecraft:mud_brick_slab", HAY = "minecraft:hay_block", GLASS_PANE = "minecraft:glass_pane", LANTERN = "minecraft:lantern",
            CHAIN = "minecraft:chain", SHROOMLIGHT = "minecraft:shroomlight", GOLD = "minecraft:gold_block", WATER = "minecraft:water",
            STEM = "minecraft:mushroom_stem", RED_CAP = "minecraft:red_mushroom_block", BROWN_CAP = "minecraft:brown_mushroom_block",
            DEEPSLATE_TILES = "minecraft:deepslate_tiles", CHISELED = "minecraft:chiseled_stone_bricks", IRON_BARS = "minecraft:iron_bars",
            CARPET = "minecraft:red_carpet";

    // ---------------------------------------------------------------- the dome
    private static final int[] AX = new int[HEIGHT + 1], BZ = new int[HEIGHT + 1];

    static {
        for (int h = 0; h <= HEIGHT; h++) {
            double k = h <= WALL_H ? 1.0 : Math.sqrt(1.0 - Math.pow((h - WALL_H) / (double) (HEIGHT - WALL_H + 1), 2));
            AX[h] = (int) Math.round(A * k);
            BZ[h] = (int) Math.round(B * k);
        }
    }

    /** The half-widths of the air at the layer h above the floor (0 is the floor itself). */
    public static int airX(int h) { return AX[Math.max(0, Math.min(h, HEIGHT))]; }

    public static int airZ(int h) { return BZ[Math.max(0, Math.min(h, HEIGHT))]; }

    /** The height above the floor of the topmost air in the column at offset (u, v) from the centre; 0 if the column is not inside the cavern. */
    public static int columnTop(int u, int v) {
        for (int h = HEIGHT; h >= 1; h--) if (Footprint.inside(u, v, AX[h], BZ[h])) return h;
        return 0;
    }

    /** Is the column at offset (u, v) inside the cavern floor, the margin grown (or shrunk, if negative)? */
    public static boolean insideFloor(int u, int v, int margin) { return Footprint.inside(u, v, A + margin, B + margin); }

    // ---------------------------------------------------------------- the four sides
    /**
     * A side of the village seen as a local frame in which "outward" is +z: the same mapping NexusOps applies for frame(0, 0, swap, su, sv).
     * 0 north, 1 east, 2 south, 3 west. Houses and the hall are described once and drawn facing any side.
     */
    record Side(int index, boolean swap, int su, int sv) {
        int wx(int lx, int lz) { return swap ? lz * sv : lx * su; }

        int wz(int lx, int lz) { return swap ? lx * su : lz * sv; }

        /** The unit step toward +z of the local frame, in the world. */
        int dx() { return wx(0, 1); }

        int dz() { return wz(0, 1); }
    }

    static final Side[] SIDES = {new Side(0, false, -1, -1), new Side(1, true, 1, 1), new Side(2, false, 1, 1), new Side(3, true, 1, -1)};

    /** How far the air reaches from the centre toward a side, along its axis. */
    static int reach(int side) { return (side & 1) == 0 ? B : A; }

    private static void at(NexusOps o, Side s, int cu, int cv) { o.frame(cu, cv, s.swap(), s.su(), s.sv()); }

    // ---------------------------------------------------------------- the few numbers that place the cavern
    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0xB5297A4D3F84D5B5L ^ cellZ * 0x8CB92BA72F3D8DD7L ^ 0x2545F4914F6CDD1DL;
        h ^= h >>> 31;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return h;
    }

    public static int centreX(int cellX, int cellZ) { return NexusZone.centre(cellX) + new TempestRng(mix(cellX, cellZ)).range(-JITTER, JITTER); }

    public static int centreZ(int cellX, int cellZ) {
        TempestRng r = new TempestRng(mix(cellX, cellZ));
        r.next();
        return NexusZone.centre(cellZ) + r.range(-JITTER, JITTER);
    }

    /** The y of the topmost floor block: -57 to -55, so a bit of rock is left under the foundation and the whole village lies in the deepest tier. */
    public static int floorY(int cellX, int cellZ) {
        TempestRng r = new TempestRng(mix(cellX, cellZ));
        r.next();
        r.next();
        return -57 + r.below(3);
    }

    /** The side the town hall faces (0 north, 1 east, 2 south, 3 west); the Treasury lies behind it, toward the opposite side. */
    public static int hallSide(int cellX, int cellZ) {
        TempestRng r = new TempestRng(mix(cellX, cellZ));
        for (int i = 0; i < 3; i++) r.next();
        return r.below(4);
    }

    /** Which of {@code n} relics is the central one. */
    public static int centralRelicIndex(int cellX, int cellZ, int n) {
        TempestRng r = new TempestRng(mix(cellX, cellZ));
        for (int i = 0; i < 4; i++) r.next();
        return n <= 0 ? 0 : r.below(n);
    }

    // ---------------------------------------------------------------- what the rest of the mod reads
    /** A way into the village: a sealed gate in the shell, to be opened to a cave. The passage runs from (firstX, firstZ) outward (dx, dz) to the cell of the door. */
    public record Gateway(int side, int firstX, int firstZ, int doorX, int doorZ, int dx, int dz) {}

    /**
     * A house that is a fake: it stands like the others until someone comes close, then it dissolves into an ambush. {@code ops} rebuilds it
     * (every block of the house above the floor), the bounds hold it, (markX, markY, markZ) is a block of its roof that is there only while
     * it stands, (doorX, doorZ) is the cell in front of its door, and {@code ambush} are the floor cells inside where the creatures appear.
     */
    public record Fake(int id, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int doorX, int doorZ, int markX, int markY, int markZ,
                       List<NexusOp> ops, List<ZonePlan.Spot> ambush) {
        public boolean near(double x, double z, double radius) {
            double nx = Math.max(minX, Math.min(x, maxX + 1.0)), nz = Math.max(minZ, Math.min(z, maxZ + 1.0));
            return (x - nx) * (x - nx) + (z - nz) * (z - nz) <= radius * radius;
        }
    }

    /** The plan and everything the glue needs to know about it. */
    public record Layout(ZonePlan plan, List<Gateway> gateways, List<Fake> fakes, int hallSide, int[] treasury, ZonePlan.Spot rally, ZonePlan.Spot standard) {}

    private static final Map<Long, Layout> CACHE = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Layout> eldest) { return size() > 4; }
    };

    /** The layout of the village in core cell (cellX, cellZ); the last few are kept. */
    public static Layout layout(int cellX, int cellZ) {
        long key = (((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL);
        synchronized (CACHE) {
            Layout l = CACHE.get(key);
            if (l == null) {
                l = build(cellX, cellZ);
                CACHE.put(key, l);
            }
            return l;
        }
    }

    public static ZonePlan of(int cellX, int cellZ) { return layout(cellX, cellZ).plan(); }

    /** The air of the Treasury: {minX, minY, minZ, maxX, maxY, maxZ}, inclusive, in world coordinates. */
    public static int[] treasuryBounds(int cellX, int cellZ) { return layout(cellX, cellZ).treasury(); }

    // ---------------------------------------------------------------- keeping things apart
    /** The rectangles taken so far, in offsets from the centre, and the questions the placing of things asks. */
    private static final class Taken {
        final List<int[]> rects = new ArrayList<>();

        void take(int x1, int z1, int x2, int z2) { rects.add(new int[]{Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2)}); }

        /** Is the rectangle clear of everything taken, with `margin` blocks to spare? */
        boolean free(int x1, int z1, int x2, int z2, int margin) {
            int a = Math.min(x1, x2) - margin, b = Math.min(z1, z2) - margin, c = Math.max(x1, x2) + margin, d = Math.max(z1, z2) + margin;
            for (int[] r : rects) if (a <= r[2] && c >= r[0] && b <= r[3] && d >= r[1]) return false;
            return true;
        }

        boolean at(int x, int z) { return !free(x, z, x, z, 0); }
    }

    static boolean inPlaza(int x, int z) { return Footprint.inside(x, z, PLAZA_X, PLAZA_Z); }

    static boolean inRing(int x, int z) { return Footprint.inside(x, z, RING_X, RING_Z) && !Footprint.inside(x, z, RING_X - RING_W, RING_Z - RING_W); }

    /** On one of the four roads that run from the plaza to the gates (three wide)? */
    static boolean inRoad(int x, int z) { return (Math.abs(x) <= 1 && Math.abs(z) >= PLAZA_Z - 1) || (Math.abs(z) <= 1 && Math.abs(x) >= PLAZA_X - 1); }

    /** Is the rectangle clear of the plaza (an ellipse), and, if asked, of the lane that rings the village? */
    private static boolean clearOfGround(int x1, int z1, int x2, int z2, int margin, boolean lane) {
        int nx = Math.max(x1, Math.min(0, x2)), nz = Math.max(z1, Math.min(0, z2));
        if (Footprint.inside(nx, nz, PLAZA_X + margin, PLAZA_Z + margin)) return false;
        if (lane) for (int x = x1 - margin; x <= x2 + margin; x++) for (int z = z1 - margin; z <= z2 + margin; z++) if (inRing(x, z)) return false;
        return true;
    }

    private static boolean insideAir(int x1, int z1, int x2, int z2, int margin) {
        return insideFloor(x1, z1, -margin) && insideFloor(x1, z2, -margin) && insideFloor(x2, z1, -margin) && insideFloor(x2, z2, -margin);
    }

    // ---------------------------------------------------------------- the houses
    private record Pal(String wall, String post, String roofFill, String roofStairs, String roofCap, String slab) {}

    private static final Pal[] PALS = {
            new Pal(MUD, SPRUCE_LOG, DARK, DARK_STAIRS, DARK, DARK_SLAB),
            new Pal(PACKED, STRIPPED, SPRUCE, SPRUCE_STAIRS, SPRUCE, SPRUCE_SLAB),
            new Pal(BAMBOO, BAMBOO_BLOCK, MUD, MUD_STAIRS, MUD, MUD_SLAB)};

    private static final int MUD_HUT = 0, TIMBER = 1, LONGHOUSE = 2;

    /** A house: its kind, where it stands (offsets from the centre), which way its door faces (0 north .. 3 west, in the world), its palette, half-width and half-depth. */
    private record Hut(int kind, int x, int z, int door, int pal, int hw, int hd, long seed) {
        Side side() { return SIDES[door]; }

        /** The local bounds the house stands on (the porch included, the roof's one-block overhang not): x1, z1, x2, z2. */
        int[] local() { return kind == MUD_HUT ? new int[]{-3, -3, 3, 3} : new int[]{-hw, -hd, hw, hd + 2}; }

        /** The world rectangle (offsets from the centre): x1, z1, x2, z2. */
        int[] rect() {
            int[] l = local();
            Side s = side();
            int a = x + s.wx(l[0], l[1]), b = x + s.wx(l[2], l[3]), c = z + s.wz(l[0], l[1]), d = z + s.wz(l[2], l[3]);
            return new int[]{Math.min(a, b), Math.min(c, d), Math.max(a, b), Math.max(c, d)};
        }

        /** The local z of the door. */
        int doorLocal() { return kind == MUD_HUT ? 3 : hd; }

        int topTier() { return kind == MUD_HUT ? 0 : hipTop(-hw - 1, -hd - 1, hw + 1, hd + 1); }

        int markY(int f) { return kind == MUD_HUT ? f + 8 : f + 5 + topTier(); }

        void build(NexusOps o, int f) {
            at(o, side(), x, z);
            if (kind == MUD_HUT) mudHut(o, f);
            else timber(o, f, hw, hd, PALS[pal]);
            o.resetFrame();
            if (kind != MUD_HUT) {                                                            // vines up the walls, in the world's own directions
                int[] wr = rectOf(side(), -hw, -hd, hw, hd);
                vines(o, f, seed, x + wr[0], z + wr[1], x + wr[2], z + wr[3], 4, kind * 31 + x, door, -1);
            }
        }

        /** The floor patch inside (phase 1), so the house stands on boards and not on moss. */
        void floor(NexusOps o, int f) {
            at(o, side(), x, z);
            if (kind == MUD_HUT) o.disc(SPRUCE, "", 3, 3, f, f);
            else o.box(SPRUCE, -hw, f, -hd, hw, f, hd);
            o.resetFrame();
        }
    }

    /** The number of tiers a hip roof over (x1, z1)-(x2, z2) has, less one: the last tier's offset from the first. */
    private static int hipTop(int x1, int z1, int x2, int z2) {
        int t = 0;
        while (x1 + t + 1 <= x2 - t - 1 && z1 + t + 1 <= z2 - t - 1) t++;
        return t;
    }

    /**
     * A hip roof over the rectangle: each tier a block smaller on every side, filled, the rim of stairs facing uphill (inward), the last tier
     * of full blocks. Returns the y of the top.
     */
    private static int hipRoof(NexusOps o, int x1, int z1, int x2, int z2, int y, String fill, String stairs, String cap) {
        int t = 0;
        while (true) {
            int a = x1 + t, b = z1 + t, c = x2 - t, d = z2 - t;
            if (!(a + 1 <= c - 1 && b + 1 <= d - 1)) {
                o.box(cap, a, y + t, b, c, y + t, d);
                return y + t;
            }
            o.box(fill, a, y + t, b, c, y + t, d);
            o.box(stairs, "facing=east", a, y + t, b, a, y + t, d);
            o.box(stairs, "facing=west", c, y + t, b, c, y + t, d);
            o.box(stairs, "facing=south", a, y + t, b, c, y + t, b);
            o.box(stairs, "facing=north", a, y + t, d, c, y + t, d);
            t++;
        }
    }

    /** A round mud hut with a conical thatch: an octagon of mud brick, three high, with its door on the +z side. */
    private static void mudHut(NexusOps o, int f) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (!octagon(x, z)) continue;
                boolean rim = !(octagon(x + 1, z) && octagon(x - 1, z) && octagon(x, z + 1) && octagon(x, z - 1));
                if (!rim) continue;
                if (Math.abs(x) == 2 && Math.abs(z) == 2) o.box(SPRUCE_LOG, "axis=y", x, f + 1, z, x, f + 3, z);
                else o.box(MUD, x, f + 1, z, x, f + 3, z);
            }
        }
        o.air(0, f + 1, 3, 0, f + 2, 3);
        o.put(GLASS_PANE, -3, f + 2, 0);
        o.put(GLASS_PANE, 3, f + 2, 0);
        o.put("minecraft:spruce_trapdoor", "facing=south,half=top,open=false", 0, f + 3, 3);
        o.disc(HAY, "", 4, 4, f + 4, f + 4);
        o.disc(HAY, "", 3, 3, f + 5, f + 5);
        o.disc(HAY, "", 2, 2, f + 6, f + 6);
        o.disc(HAY, "", 1, 1, f + 7, f + 7);
        o.put(HAY, 0, f + 8, 0);
        o.put(LANTERN, "hanging=false", 0, f + 9, 0);
        o.put("minecraft:crafting_table", -2, f + 1, 0);
        o.put("minecraft:red_bed", "part=foot,facing=north", 1, f + 1, -1);
        o.put("minecraft:red_bed", "part=head,facing=north", 1, f + 1, -2);
        o.put("minecraft:potted_fern", 2, f + 1, 1);
        o.put(LANTERN, "hanging=true", 0, f + 3, 0);
    }

    private static boolean octagon(int x, int z) { return Math.abs(x) <= 3 && Math.abs(z) <= 3 && Math.abs(x) + Math.abs(z) <= 4; }

    /** A house of walls, posts and a hip roof with a small porch on its +z side. */
    private static void timber(NexusOps o, int f, int hw, int hd, Pal p) {
        o.ring(p.wall(), "", -hw, -hd, hw, hd, f + 1, f + 4);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) o.box(p.post(), "axis=y", sx * hw, f + 1, sz * hd, sx * hw, f + 4, sz * hd);
        o.air(0, f + 1, hd, 0, f + 2, hd);
        o.put(p.post(), "axis=x", 0, f + 3, hd);
        o.put(GLASS_PANE, -hw, f + 2, 0);
        o.put(GLASS_PANE, hw, f + 2, 0);
        o.put(GLASS_PANE, 0, f + 2, -hd);
        hipRoof(o, -hw - 1, -hd - 1, hw + 1, hd + 1, f + 5, p.roofFill(), p.roofStairs(), p.roofCap());
        o.box(p.post(), "axis=y", -2, f + 1, hd + 2, -2, f + 3, hd + 2);
        o.box(p.post(), "axis=y", 2, f + 1, hd + 2, 2, f + 3, hd + 2);
        o.box(p.slab(), "type=bottom", -3, f + 4, hd + 1, 3, f + 4, hd + 2);
        o.put(LANTERN, "hanging=true", 0, f + 3, hd + 2);
        o.put("minecraft:red_bed", "part=foot,facing=north", -hw + 1, f + 1, 0);
        o.put("minecraft:red_bed", "part=head,facing=north", -hw + 1, f + 1, -1);
        o.put(SPRUCE_FENCE, hw - 1, f + 1, -1);
        o.put("minecraft:spruce_pressure_plate", hw - 1, f + 2, -1);
        o.put("minecraft:crafting_table", hw - 1, f + 1, 1);
        o.put("minecraft:potted_fern", -hw + 1, f + 1, hd - 1);
        o.put(LANTERN, "hanging=true", 0, f + 4, 0);
    }

    // ---------------------------------------------------------------- the town hall and the Treasury (frame of the hall: front is +z)
    private static void hallFloor(NexusOps o, int f) {
        o.box(SPRUCE, -HALL_X + 1, f, HALL_BACK + 1, HALL_X - 1, f, HALL_FRONT - 1);
        o.box(DEEPSLATE_TILES, -TREAS_X + 1, f, TREAS_BACK + 1, TREAS_X - 1, f, HALL_BACK - 1);
        o.box(GOLD, -1, f, TREAS_BACK + 1, 1, f, HALL_BACK - 1);
        o.box(STONE_BRICKS, -HALL_X - 1, f, HALL_FRONT + 1, HALL_X + 1, f, HALL_FRONT + 2);
    }

    private static void hall(NexusOps o, int f) {
        o.ring(MUD, "", -HALL_X, HALL_BACK, HALL_X, HALL_FRONT, f + 1, f + 4);
        o.ring(SPRUCE, "", -HALL_X, HALL_BACK, HALL_X, HALL_FRONT, f + 5, f + 8);
        int[] sideZ = {HALL_BACK, 0, 6, HALL_FRONT}, frontX = {-HALL_X, -6, 6, HALL_X};
        for (int sx = -1; sx <= 1; sx += 2) for (int z : sideZ) o.box(SPRUCE_LOG, "axis=y", sx * HALL_X, f + 1, z, sx * HALL_X, f + 8, z);
        for (int x : frontX) o.box(SPRUCE_LOG, "axis=y", x, f + 1, HALL_FRONT, x, f + 8, HALL_FRONT);
        for (int x : new int[]{-6, 6}) o.box(SPRUCE_LOG, "axis=y", x, f + 1, HALL_BACK, x, f + 8, HALL_BACK);
        // the front door, three wide and four high, framed in logs with a beam over it
        o.air(-1, f + 1, HALL_FRONT, 1, f + 4, HALL_FRONT);
        o.box(SPRUCE_LOG, "axis=y", -2, f + 1, HALL_FRONT, -2, f + 5, HALL_FRONT);
        o.box(SPRUCE_LOG, "axis=y", 2, f + 1, HALL_FRONT, 2, f + 5, HALL_FRONT);
        o.box(DARK_LOG, "axis=x", -2, f + 5, HALL_FRONT, 2, f + 5, HALL_FRONT);
        // windows
        for (int z : new int[]{-2, 3, 8}) for (int sx = -1; sx <= 1; sx += 2) o.box(GLASS_PANE, sx * HALL_X, f + 5, z, sx * HALL_X, f + 6, z);
        for (int x : new int[]{-9, -3, 3, 9}) o.box(GLASS_PANE, x, f + 5, HALL_FRONT, x, f + 6, HALL_FRONT);
        // the doorway of the Treasury in the back wall, left open for the mana gate
        o.air(0, f + 1, HALL_BACK, 0, f + 2, HALL_BACK);
        o.box(GOLD, -1, f + 1, HALL_BACK, -1, f + 3, HALL_BACK);
        o.box(GOLD, 1, f + 1, HALL_BACK, 1, f + 3, HALL_BACK);
        o.box(CHISELED, 0, f + 3, HALL_BACK, 0, f + 3, HALL_BACK);
        o.box(CHISELED, -2, f + 4, HALL_BACK, 2, f + 4, HALL_BACK);
        // the roof: a hip of dark oak, and a lantern lit cupola on top
        int top = hipRoof(o, -HALL_X - 1, HALL_BACK - 1, HALL_X + 1, HALL_FRONT + 1, f + 9, DARK, DARK_STAIRS, DARK);
        o.box(SPRUCE, -1, top + 1, 2, 1, top + 3, 4);
        o.put(SHROOMLIGHT, 0, top + 2, 3);
        for (int[] w : new int[][]{{0, 2}, {0, 4}, {-1, 3}, {1, 3}}) o.put(GLASS_PANE, w[0], top + 2, w[1]);
        o.box(DARK_SLAB, "type=bottom", -2, top + 4, 1, 2, top + 4, 5);
        // inside: a red carpet from the door to the Treasury, pillars, long tables, banners, chandeliers
        o.box(CARPET, -1, f + 1, HALL_BACK + 1, 1, f + 1, HALL_FRONT - 1);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int z : new int[]{-3, 3, 9}) o.box(SPRUCE_LOG, "axis=y", sx * 5, f + 1, z, sx * 5, f + 8, z);
            o.box(SPRUCE_SLAB, "type=top", sx * 8, f + 1, -3, sx * 9, f + 1, 9);
            o.box(SPRUCE_STAIRS, "facing=" + (sx > 0 ? "west" : "east"), sx * 7, f + 1, -3, sx * 7, f + 1, 9);
            o.box(SPRUCE_STAIRS, "facing=" + (sx > 0 ? "east" : "west"), sx * 10, f + 1, -3, sx * 10, f + 1, 9);
            o.put("minecraft:red_wall_banner", "facing=south", sx * 4, f + 5, HALL_BACK + 1);
            o.put("minecraft:red_wall_banner", "facing=north", sx * 6, f + 5, HALL_FRONT - 1);
        }
        for (int z : new int[]{-2, 4, 10}) {
            o.box(CHAIN, "axis=y", 0, f + 7, z, 0, f + 8, z);
            o.put(LANTERN, "hanging=true", 0, f + 6, z);
        }
        // lantern posts either side of the door
        for (int sx = -1; sx <= 1; sx += 2) {
            o.box(SPRUCE_FENCE, sx * 4, f + 1, HALL_FRONT + 2, sx * 4, f + 3, HALL_FRONT + 2);
            o.put(LANTERN, "hanging=false", sx * 4, f + 4, HALL_FRONT + 2);
        }
    }

    /** The Treasury behind the hall: a closed room of stone brick and gold, the pedestal plinths, light. */
    private static void treasury(NexusOps o, int f) {
        o.ring(STONE_BRICKS, "", -TREAS_X, TREAS_BACK, TREAS_X, HALL_BACK, f + 1, f + 6);
        o.box(DEEPSLATE_TILES, -TREAS_X, f + 7, TREAS_BACK, TREAS_X, f + 7, HALL_BACK);
        o.box(GOLD, -TREAS_X, f + 4, TREAS_BACK, TREAS_X, f + 4, TREAS_BACK);
        for (int sx = -1; sx <= 1; sx += 2) {
            o.box(GOLD, sx * TREAS_X, f + 4, TREAS_BACK + 1, sx * TREAS_X, f + 4, HALL_BACK - 1);
            o.box(GOLD, sx * (TREAS_X - 1), f + 1, TREAS_BACK + 1, sx * (TREAS_X - 1), f + 2, TREAS_BACK + 3);
            o.box("minecraft:raw_gold_block", sx * (TREAS_X - 1), f + 3, TREAS_BACK + 1, sx * (TREAS_X - 1), f + 3, TREAS_BACK + 2);
        }
        o.box(GOLD, -2, f + 1, -16, 2, f + 1, -12);
        o.box(CHISELED, -1, f + 1, -15, 1, f + 1, -13);
        for (int[] m : MINORS) o.box(GOLD, m[0], f + 1, m[1], m[0], f + 1, m[1]);
        for (int[] l : new int[][]{{-4, -9}, {4, -9}, {-4, -15}, {4, -15}}) o.put(LANTERN, "hanging=true", l[0], f + 6, l[1]);
        // the mana gate is set into the doorway (0, f + 1..f + 2, HALL_BACK) when the village is populated
    }

    /** The pedestals of the minor relics, and the central one, in the hall's frame. */
    private static final int[][] MINORS = {{-5, -9}, {5, -9}, {-5, -16}, {5, -16}};
    private static final int[] CENTRAL = {0, -14};

    // ---------------------------------------------------------------- the gates (frame of their side: outward is +z, d the reach of the air)
    private static void gate(NexusOps o, int f, int d) {
        o.box(MUD, -4, f + 1, d - 5, 4, f + 7, d - 2);
        for (int sx = -1; sx <= 1; sx += 2) for (int z : new int[]{d - 5, d - 2}) o.box(SPRUCE_LOG, "axis=y", sx * 4, f + 1, z, sx * 4, f + 7, z);
        o.air(-1, f + 1, d - 5, 1, f + 5, d - 2);
        o.box(DARK_LOG, "axis=x", -2, f + 6, d - 5, 2, f + 6, d - 5);
        o.put(LANTERN, "hanging=true", 0, f + 5, d - 3);
        int top = hipRoof(o, -5, d - 6, 5, d - 1, f + 8, DARK, DARK_STAIRS, DARK);
        o.put(LANTERN, "hanging=false", 0, top + 1, d - 4);
        // the plug: a gate of planks across the end of the archway, set into the shell
        o.box(SPRUCE, -1, f + 1, d + 1, 1, f + 4, d + 1);
        o.box(IRON_BARS, -1, f + 3, d + 1, 1, f + 3, d + 1);
        o.box(DARK_LOG, "axis=y", -2, f + 1, d + 1, -2, f + 5, d + 1);
        o.box(DARK_LOG, "axis=y", 2, f + 1, d + 1, 2, f + 5, d + 1);
        o.box(DARK_LOG, "axis=x", -2, f + 5, d + 1, 2, f + 5, d + 1);
    }

    // ---------------------------------------------------------------- a well, a stall, the standard, a lamp, a line of washing, a mushroom
    private static void well(NexusOps o, int f) {
        o.box(MOSSY_COBBLE, -2, f, -2, 2, f + 1, 2);
        o.box(WATER, "", -1, f, -1, 1, f, 1);
        o.air(-1, f + 1, -1, 1, f + 1, 1);
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -2; sz <= 2; sz += 4) o.box(SPRUCE_FENCE, sx, f + 2, sz, sx, f + 4, sz);
        hipRoof(o, -3, -3, 3, 3, f + 5, SPRUCE, SPRUCE_STAIRS, SPRUCE);
        o.box(CHAIN, "axis=y", 0, f + 3, 0, 0, f + 4, 0);
        o.put(LANTERN, "hanging=true", 0, f + 2, 0);
    }

    private static final String[] WOOLS = {"minecraft:red_wool", "minecraft:white_wool", "minecraft:yellow_wool", "minecraft:white_wool", "minecraft:red_wool"};

    private static void stall(NexusOps o, int f, String ware, String ware2) {
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -1; sz <= 1; sz += 2) o.box(SPRUCE_FENCE, sx, f + 1, sz, sx, f + 3, sz);
        for (int x = -2; x <= 2; x++) o.box(WOOLS[x + 2], x, f + 4, -2, x, f + 4, 2);
        o.box(SPRUCE, -2, f + 1, 1, 2, f + 1, 1);
        o.put(ware, -1, f + 2, 1);
        o.put(ware2, 0, f + 2, 1);
        o.put(HAY, 1, f + 2, 1);
        o.put("minecraft:composter", 0, f + 1, -1);
        o.put(LANTERN, "hanging=true", -1, f + 3, 0);
        o.put(LANTERN, "hanging=true", 1, f + 3, 0);
    }

    private static void standard(NexusOps o, int f) {
        o.box(MUD, -1, f + 1, -1, 1, f + 1, 1);
        o.box(STRIPPED, "axis=y", 0, f + 2, 0, 0, f + 9, 0);
        o.box(STRIPPED, "axis=x", -2, f + 8, 0, 2, f + 8, 0);
        o.put(LANTERN, "hanging=true", -2, f + 7, 0);
        o.put(LANTERN, "hanging=true", 2, f + 7, 0);
        o.put(SHROOMLIGHT, 0, f + 10, 0);
        o.put("minecraft:red_banner", "rotation=0", 0, f + 11, 0);
    }

    private static void lampPost(NexusOps o, int f, int x, int z) {
        o.box(SPRUCE_FENCE, x, f + 1, z, x, f + 3, z);
        o.put(LANTERN, "hanging=false", x, f + 4, z);
    }

    private static final String[] PANES = {"minecraft:white_stained_glass_pane", "minecraft:red_stained_glass_pane", "minecraft:yellow_stained_glass_pane",
            "minecraft:light_blue_stained_glass_pane", "minecraft:lime_stained_glass_pane", "minecraft:pink_stained_glass_pane"};

    /** A line of washing along x: a post at each end, a rail, and sheets hanging two high. */
    private static void washing(NexusOps o, int f, int half, int colour) {
        o.box(SPRUCE_FENCE, -half, f + 1, 0, -half, f + 3, 0);
        o.box(SPRUCE_FENCE, half, f + 1, 0, half, f + 3, 0);
        o.box(STRIPPED, "axis=x", -half + 1, f + 3, 0, half - 1, f + 3, 0);
        for (int x = -half + 1, k = 0; x <= half - 1; x += 2, k++) o.box(PANES[(colour + k) % PANES.length], x, f + 1, 0, x, f + 2, 0);
    }

    private static void mushroom(NexusOps o, int f, int x, int z, int height, int radius, boolean red) {
        o.box(STEM, x, f + 1, z, x, f + height, z);
        String cap = red ? RED_CAP : BROWN_CAP;
        o.frame(x, z, false, 1, 1);
        o.disc(cap, "", radius, radius, f + height + 1, f + height + 1);
        if (radius > 1) o.disc(cap, "", radius - 1, radius - 1, f + height + 2, f + height + 2);
        o.resetFrame();
    }

    // ---------------------------------------------------------------- the floor
    private static boolean spoke(int x, int z) { return inRoad(x, z); }

    private static String floorBlock(long seed, int x, int z) {
        if (inPlaza(x, z)) {
            double d = Math.sqrt((double) x * x / (PLAZA_X * PLAZA_X) + (double) z * z / (PLAZA_Z * PLAZA_Z));
            int h = TempestRng.hash(seed, Math.floorDiv(x, 2), Math.floorDiv(z, 2), 3) % 100;
            if (d > 0.9) return MOSSY_COBBLE;
            if (d > 0.74) return h < 60 ? MUD : PACKED;
            if (d > 0.3) return h < 45 ? STONE_BRICKS : h < 75 ? COBBLE : MOSSY_BRICKS;
            if (d > 0.18) return MUD;
            return SMOOTH;
        }
        if (spoke(x, z) || inRing(x, z)) return TempestRng.hash(seed, x, z, 5) % 100 < 72 ? PATH : GRAVEL;
        int h = TempestRng.hash(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 1) % 100;
        int j = TempestRng.hash(seed, x, z, 2) % 100;
        if (h < 34) return j < 82 ? MOSS : GRASS;
        if (h < 62) return j < 85 ? GRASS : MOSS;
        if (h < 74) return PODZOL;
        if (h < 84) return ROOTED;
        if (h < 92) return j < 60 ? MYCELIUM : PODZOL;
        return COARSE;
    }

    // ---------------------------------------------------------------- the plan
    private static Layout build(int cellX, int cellZ) {
        long seed = mix(cellX, cellZ);
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), f = floorY(cellX, cellZ), hs = hallSide(cellX, cellZ);
        Side hall = SIDES[hs];
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.TEMPEST_MIRAGE, cellX, cellZ, cx, cz);
        b.floorY(f).topY(f + HEIGHT + ROOF).footprint(Footprint.Shape.OVAL, OUTER_X, OUTER_Z);
        NexusOps o = b.ops();
        Taken taken = new Taken();

        // ---- what stands where (decided before anything is written)
        int[] hallRect = rectOf(hall, -HALL_X - 2, TREAS_BACK - 2, HALL_X + 2, HALL_FRONT + 3);
        taken.take(hallRect[0], hallRect[1], hallRect[2], hallRect[3]);
        int[] standardRect = rectOf(hall, -2, STANDARD_Z - 2, 2, STANDARD_Z + 2);
        taken.take(standardRect[0], standardRect[1], standardRect[2], standardRect[3]);
        taken.take(-3, -B, 3, B);
        taken.take(-A, -3, A, 3);
        for (Side s : SIDES) {
            int[] g = rectOf(s, -6, reach(s.index()) - 7, 6, reach(s.index()) + WALL);
            taken.take(g[0], g[1], g[2], g[3]);
        }
        List<int[]> wells = new ArrayList<>(), stalls = new ArrayList<>();
        TempestRng r = new TempestRng(seed ^ 0x5DEECE66DL);
        for (int[] p : new int[][]{{-19, 9}, {19, 9}, {-19, -9}, {19, -9}}) {            // wells, on the plaza beside the hall
            if (wells.size() >= 2) break;
            int wx = hall.wx(p[0], p[1]), wz = hall.wz(p[0], p[1]);
            if (taken.free(wx - 3, wz - 3, wx + 3, wz + 3, 1) && inPlaza(wx, wz)) {
                wells.add(new int[]{wx, wz});
                taken.take(wx - 3, wz - 3, wx + 3, wz + 3);
            }
        }
        for (int[] p : new int[][]{{-18, 17}, {18, 17}, {-20, 1}, {20, 1}, {-8, 20}, {8, 20}, {-19, -9}, {19, -9}}) {            // market stalls, round the plaza's edge
            if (stalls.size() >= 4) break;
            int sx = hall.wx(p[0], p[1]), sz = hall.wz(p[0], p[1]);
            int[] sr = rectOf(hall, p[0] - 2, p[1] - 2, p[0] + 2, p[1] + 2);
            if (taken.free(sr[0], sr[1], sr[2], sr[3], 1) && inPlaza(sx, sz)) {
                stalls.add(new int[]{sx, sz, hs});
                taken.take(sr[0], sr[1], sr[2], sr[3]);
            }
        }
        List<Hut> huts = placeHuts(r, taken, seed);
        List<int[]> lines = new ArrayList<>();                                                // lines of washing: {x, z, half}
        for (int attempt = 0; attempt < 400 && lines.size() < 4; attempt++) {
            double a = r.unit() * Math.PI * 2, q = 0.6 + r.unit() * 0.3;
            int x = (int) Math.round(Math.cos(a) * A * q), z = (int) Math.round(Math.sin(a) * B * q), half = r.range(3, 4);
            if (!insideAir(x - half - 1, z - 1, x + half + 1, z + 1, 4) || !clearOfGround(x - half - 1, z - 1, x + half + 1, z + 1, 2, true)) continue;
            if (!taken.free(x - half - 1, z - 1, x + half + 1, z + 1, 2)) continue;
            lines.add(new int[]{x, z, half, r.below(PANES.length)});
            taken.take(x - half - 1, z - 1, x + half + 1, z + 1);
        }

        // ---- phase 0: carve the cavern
        carve(o, f);
        o.phase();

        // ---- phase 1: foundation, shell, roof, floor, roads, house floors
        shell(o, f);
        floor(o, f, seed);
        hallFloorAt(o, f, hall);
        for (Hut h : huts) h.floor(o, f);
        o.phase();

        // ---- phase 2: structures
        at(o, hall, 0, 0);
        treasury(o, f);
        hall(o, f);
        at(o, hall, 0, STANDARD_Z);
        standard(o, f);
        o.resetFrame();
        for (Side s : SIDES) {
            at(o, s, 0, 0);
            gate(o, f, reach(s.index()));
        }
        o.resetFrame();
        for (int[] w : wells) {
            o.frame(w[0], w[1], false, 1, 1);
            well(o, f);
        }
        o.resetFrame();
        String[][] wares = {{"minecraft:melon", "minecraft:pumpkin"}, {"minecraft:pumpkin", "minecraft:melon"}, {"minecraft:melon", "minecraft:hay_block"}, {"minecraft:pumpkin", "minecraft:hay_block"}};
        for (int i = 0; i < stalls.size(); i++) {
            int[] s = stalls.get(i);
            at(o, hall, s[0], s[1]);
            stall(o, f, wares[i % wares.length][0], wares[i % wares.length][1]);
        }
        o.resetFrame();
        for (Hut h : huts) h.build(o, f);
        o.phase();

        // ---- phase 3: details
        List<int[]> lamps = lampSites();
        for (int[] l : lamps) lampPost(o, f, l[0], l[1]);
        for (int[] w : lines) {
            o.frame(w[0], w[1], false, 1, 1);
            washing(o, f, w[2], w[3]);
        }
        o.resetFrame();
        for (int i = 0; i < 40; i++) {                                                          // giant mushrooms in whatever room is left
            for (int attempt = 0; attempt < 80; attempt++) {
                double a = r.unit() * Math.PI * 2, q = 0.35 + r.unit() * 0.62;
                int x = (int) Math.round(Math.cos(a) * A * q), z = (int) Math.round(Math.sin(a) * B * q), rad = r.range(2, 3), hgt = r.range(3, 8);
                if (!insideAir(x - rad, z - rad, x + rad, z + rad, 2) || !clearOfGround(x - rad, z - rad, x + rad, z + rad, 1, false)) continue;
                if (!taken.free(x - rad, z - rad, x + rad, z + rad, 0)) continue;
                if (columnTop(x, z) < hgt + 4 || columnTop(x + rad, z + rad) < hgt + 4 || columnTop(x - rad, z - rad) < hgt + 4) continue;
                mushroom(o, f, x, z, hgt, rad, r.below(100) < 55);
                taken.take(x - rad, z - rad, x + rad, z + rad);
                break;
            }
        }
        growth(o, f, seed, taken, hall);
        ceiling(o, f, seed, lamps);

        // ---- where things are
        ZonePlan.Spot rally = new ZonePlan.Spot(cx + hall.wx(0, RALLY_Z), f + 1, cz + hall.wz(0, RALLY_Z));
        ZonePlan.Spot standardAt = new ZonePlan.Spot(cx + hall.wx(0, STANDARD_Z), f + 1, cz + hall.wz(0, STANDARD_Z));
        b.centralRelic(cx + hall.wx(CENTRAL[0], CENTRAL[1]), f + 2, cz + hall.wz(CENTRAL[0], CENTRAL[1]));
        for (int[] m : MINORS) b.minorRelic(cx + hall.wx(m[0], m[1]), f + 2, cz + hall.wz(m[0], m[1]));
        b.gate(cx + hall.wx(0, HALL_BACK), f + 1, cz + hall.wz(0, HALL_BACK));
        List<Gateway> gateways = new ArrayList<>();
        for (Side s : SIDES) {
            int d = reach(s.index());
            b.entrance(cx + s.wx(0, d - 1), f + 1, cz + s.wz(0, d - 1));
            gateways.add(new Gateway(s.index(), cx + s.wx(0, d + GATE_FIRST), cz + s.wz(0, d + GATE_FIRST), cx + s.wx(0, d + GATE_DOOR), cz + s.wz(0, d + GATE_DOOR), s.dx(), s.dz()));
        }
        b.spawn("rally", rally.x(), rally.y(), rally.z());
        b.spawn(ZonePlan.ROLE_ARRIVAL, cx + hall.wx(-5, RALLY_Z), f + 1, cz + hall.wz(-5, RALLY_Z));
        for (int[] g : new int[][]{{-3, -3}, {3, -3}, {-3, 2}, {3, 2}}) b.spawn(ZonePlan.ROLE_GUARD, cx + hall.wx(g[0], g[1]), f + 1, cz + hall.wz(g[0], g[1]));
        for (int[] e : new int[][]{{-9, 17}, {9, 17}, {-14, 8}, {14, 8}}) b.spawn(ZonePlan.ROLE_ELITE, cx + hall.wx(e[0], e[1]), f + 1, cz + hall.wz(e[0], e[1]));
        for (int[] s : swarmSpots(r, taken)) b.spawn(ZonePlan.ROLE_SWARM, cx + s[0], f + 1, cz + s[1]);

        // ---- the fakes
        List<Fake> fakes = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < huts.size(); i++) if (huts.get(i).kind() != LONGHOUSE) order.add(i);
        order.sort((p, q) -> Integer.compare(TempestRng.hash(seed, p, 0, 77), TempestRng.hash(seed, q, 0, 77)));
        for (int k = 0; k < Math.min(3, order.size()); k++) fakes.add(fake(k, huts.get(order.get(k)), cx, cz, f));

        int[] t = new int[]{cx + Math.min(hall.wx(-TREAS_X + 1, TREAS_BACK + 1), hall.wx(TREAS_X - 1, HALL_BACK - 1)), f + 1,
                cz + Math.min(hall.wz(-TREAS_X + 1, TREAS_BACK + 1), hall.wz(TREAS_X - 1, HALL_BACK - 1)),
                cx + Math.max(hall.wx(-TREAS_X + 1, TREAS_BACK + 1), hall.wx(TREAS_X - 1, HALL_BACK - 1)), f + 6,
                cz + Math.max(hall.wz(-TREAS_X + 1, TREAS_BACK + 1), hall.wz(TREAS_X - 1, HALL_BACK - 1))};
        return new Layout(b.build(), Collections.unmodifiableList(gateways), Collections.unmodifiableList(fakes), hs, t, rally, standardAt);
    }

    /** The world rectangle {x1, z1, x2, z2} (offsets from the centre) of a local rectangle of a side's frame. */
    private static int[] rectOf(Side s, int lx1, int lz1, int lx2, int lz2) {
        int a = s.wx(lx1, lz1), b = s.wx(lx2, lz2), c = s.wz(lx1, lz1), d = s.wz(lx2, lz2);
        return new int[]{Math.min(a, b), Math.min(c, d), Math.max(a, b), Math.max(c, d)};
    }

    private static void hallFloorAt(NexusOps o, int f, Side hall) {
        at(o, hall, 0, 0);
        hallFloor(o, f);
        o.resetFrame();
    }

    private static Fake fake(int id, Hut h, int cx, int cz, int f) {
        NexusOps scratch = new NexusOps(cx, cz);
        h.build(scratch, f);
        int[] rc = h.rect();
        rc = new int[]{rc[0] - 1, rc[1] - 1, rc[2] + 1, rc[3] + 1};                          // the roof overhangs the walls by a block
        Side s = h.side();
        List<ZonePlan.Spot> ambush = new ArrayList<>();
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {0, 0}}) ambush.add(new ZonePlan.Spot(cx + h.x() + s.wx(c[0], c[1]), f + 1, cz + h.z() + s.wz(c[0], c[1])));
        int maxY = f;
        for (NexusOp op : scratch.list()) maxY = Math.max(maxY, op.y2());
        int dl = h.doorLocal() + (h.kind() == MUD_HUT ? 1 : 3);
        return new Fake(id, cx + rc[0], f + 1, cz + rc[1], cx + rc[2], maxY, cz + rc[3], cx + h.x() + s.wx(0, dl), cz + h.z() + s.wz(0, dl),
                cx + h.x(), h.markY(f), cz + h.z(), Collections.unmodifiableList(new ArrayList<>(scratch.list())), Collections.unmodifiableList(ambush));
    }

    /** Where the houses stand: two rings round the plaza, each turned to face the middle, none touching anything taken. */
    private static List<Hut> placeHuts(TempestRng r, Taken taken, long seed) {
        List<Hut> out = new ArrayList<>();
        for (int attempt = 0; attempt < 900 && out.size() < 18; attempt++) {
            double a = r.unit() * Math.PI * 2, q = 0.55 + r.unit() * 0.4;
            int x = (int) Math.round(Math.cos(a) * A * q), z = (int) Math.round(Math.sin(a) * B * q);
            int roll = r.below(100), kind = roll < 34 ? MUD_HUT : roll < 72 ? TIMBER : LONGHOUSE, pal = r.below(PALS.length);
            int door = Math.abs(x) * B > Math.abs(z) * A ? (x > 0 ? 3 : 1) : (z > 0 ? 0 : 2);
            Hut h = new Hut(kind, x, z, door, pal, kind == LONGHOUSE ? 4 : 3, kind == LONGHOUSE ? 2 : 3, seed);
            int[] rc = h.rect();
            if (!insideAir(rc[0] - 1, rc[1] - 1, rc[2] + 1, rc[3] + 1, 3)) continue;
            if (!clearOfGround(rc[0], rc[1], rc[2], rc[3], 1, false)) continue;
            if (!taken.free(rc[0], rc[1], rc[2], rc[3], 2)) continue;
            int low = Math.min(columnTop(rc[0], rc[1]), Math.min(columnTop(rc[2], rc[1]), Math.min(columnTop(rc[0], rc[3]), columnTop(rc[2], rc[3]))));
            if (low < 12) continue;                                                              // under the dome there is room for the roof
            out.add(h);
            taken.take(rc[0], rc[1], rc[2], rc[3]);
        }
        return out;
    }

    /** Where the lamp posts stand: both sides of the four roads, every six blocks from the plaza to the gate. */
    private static List<int[]> lampSites() {
        List<int[]> out = new ArrayList<>();
        for (Side s : SIDES) {
            int d = reach(s.index());
            for (int t = (s.swap() ? PLAZA_X : PLAZA_Z) + 2; t < d - 8; t += 6) for (int sx = -2; sx <= 2; sx += 4) out.add(new int[]{s.wx(sx, t), s.wz(sx, t)});
        }
        return out;
    }

    /** Spots for the swarm: free floor cells in the village, spread out, in a fixed order. */
    private static List<int[]> swarmSpots(TempestRng r, Taken taken) {
        List<int[]> out = new ArrayList<>();
        for (int attempt = 0; attempt < 4000 && out.size() < 30; attempt++) {
            double a = r.unit() * Math.PI * 2, q = Math.sqrt(r.unit()) * 0.92;
            int x = (int) Math.round(Math.cos(a) * A * q), z = (int) Math.round(Math.sin(a) * B * q);
            if (!insideAir(x - 1, z - 1, x + 1, z + 1, 3) || taken.at(x, z) || !taken.free(x, z, x, z, 1)) continue;
            boolean apart = true;
            for (int[] s : out) if ((s[0] - x) * (s[0] - x) + (s[1] - z) * (s[1] - z) < 36) apart = false;
            if (apart) out.add(new int[]{x, z});
        }
        return out;
    }

    // ---------------------------------------------------------------- phase 0 and 1: the cavern
    /** Clears the air of the cavern: one disc per run of layers of the same size. */
    private static void carve(NexusOps o, int f) {
        int h = 1;
        while (h <= HEIGHT) {
            int e = h;
            while (e < HEIGHT && AX[e + 1] == AX[h] && BZ[e + 1] == BZ[h]) e++;
            o.carveDisc(AX[h], BZ[h], f + h, f + e);
            h = e + 1;
        }
    }

    /** The cells between two ellipses round the origin: inside (aOut, bOut) and outside (aIn, bIn), from y1 to y2. */
    private static void band(NexusOps o, String block, String props, int aOut, int bOut, int aIn, int bIn, int y1, int y2) {
        for (int v = -bOut; v <= bOut; v++) {
            int e = Footprint.extent(v, aOut, bOut);
            if (e < 0) continue;
            int in = Math.abs(v) <= bIn ? Footprint.extent(v, aIn, bIn) : -1;
            if (in < 0) {
                o.box(block, props, -e, y1, v, e, y2, v);
            } else if (e > in) {
                o.box(block, props, -e, y1, v, -in - 1, y2, v);
                o.box(block, props, in + 1, y1, v, e, y2, v);
            }
        }
    }

    /**
     * The foundation, the wall and the dome-shaped roof: every cell round the air that a leak could come through. The shell of a layer reaches
     * three blocks past the air of the layer below it, so what lies above the air is shell too; the wall's face is a skin of mossy cobblestone.
     */
    private static void shell(NexusOps o, int f) {
        o.disc(MOSSY_BRICKS, "", OUTER_X, OUTER_Z, f - FOUNDATION, f - 1);
        band(o, MOSSY_BRICKS, "", OUTER_X, OUTER_Z, A, B, f, f);
        int h = 1;
        while (h <= HEIGHT) {
            int e = h;
            while (e < HEIGHT && AX[e + 1] == AX[h] && BZ[e + 1] == BZ[h] && AX[e] == AX[h - 1] && BZ[e] == BZ[h - 1]) e++;
            band(o, STONE_BRICKS, "", AX[h - 1] + WALL, BZ[h - 1] + WALL, AX[h], BZ[h], f + h, f + e);
            h = e + 1;
        }
        o.disc(STONE_BRICKS, "", AX[HEIGHT] + WALL, BZ[HEIGHT] + WALL, f + HEIGHT + 1, f + HEIGHT + ROOF);
        band(o, MOSSY_COBBLE, "", A + 1, B + 1, A, B, f + 1, f + WALL_H);
    }

    /** The floor of the village, row by row, in runs of one block. */
    private static void floor(NexusOps o, int f, long seed) {
        for (int z = -B; z <= B; z++) {
            int e = Footprint.extent(z, A, B);
            int start = -e;
            String cur = null;
            for (int x = -e; x <= e + 1; x++) {
                String blk = x <= e ? floorBlock(seed, x, z) : null;
                if (blk == null ? cur != null : !blk.equals(cur)) {
                    if (cur != null) o.box(cur, start, f, z, x - 1, f, z);
                    cur = blk;
                    start = x;
                }
            }
        }
    }

    // ---------------------------------------------------------------- phase 3: growth
    private static final String[] FLOWERS = {"minecraft:poppy", "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:cornflower", "minecraft:oxeye_daisy"};

    /** Grass, ferns, moss carpet, flowers, small mushrooms and petals on the open ground; vines up the walls of the houses and the hall. */
    private static void growth(NexusOps o, int f, long seed, Taken taken, Side hall) {
        for (int z = -B; z <= B; z++) {
            int e = Footprint.extent(z, A, B);
            for (int x = -e + 1; x <= e - 1; x++) {
                if (inPlaza(x, z) || inRoad(x, z) || inRing(x, z) || taken.at(x, z)) continue;
                int h = TempestRng.hash(seed, x, z, 11) % 1000;
                if (h < 70) o.fillAir("minecraft:short_grass", "", x, f + 1, z, x, f + 1, z);
                else if (h < 100) o.fillAir("minecraft:fern", "", x, f + 1, z, x, f + 1, z);
                else if (h < 150) o.fillAir("minecraft:moss_carpet", "", x, f + 1, z, x, f + 1, z);
                else if (h < 170) o.fillAir(FLOWERS[TempestRng.hash(seed, x, z, 12) % FLOWERS.length], "", x, f + 1, z, x, f + 1, z);
                else if (h < 182) o.fillAir(h % 2 == 0 ? "minecraft:red_mushroom" : "minecraft:brown_mushroom", "", x, f + 1, z, x, f + 1, z);
                else if (h < 190) o.fillAir("minecraft:pink_petals", "flower_amount=" + (1 + h % 4) + ",facing=" + facingName(h), x, f + 1, z, x, f + 1, z);
            }
        }
        int[] hr = rectOf(hall, -HALL_X, HALL_BACK, HALL_X, HALL_FRONT);
        vines(o, f, seed, hr[0], hr[1], hr[2], hr[3], 8, 991, hall.index(), (hall.index() + 2) & 3);
    }

    private static String facingName(int h) { return new String[]{"north", "east", "south", "west"}[Math.floorMod(h, 4)]; }

    /**
     * Vines on the outside of the walls of a rectangular building (x1, z1)-(x2, z2) whose walls stand to `height`: a vine in the cell outside a
     * wall cell, on its face. The side the door is on (0 north .. 3 west) is left bare, and one more side if asked (-1: none): the hall's
     * back, where the Treasury is joined to it.
     */
    private static void vines(NexusOps o, int f, long seed, int x1, int z1, int x2, int z2, int height, int salt, int doorSide, int bareSide) {
        for (int y = f + 1; y <= f + height; y++) {
            for (int x = x1; x <= x2; x++) {
                if (doorSide != 0 && bareSide != 0 && TempestRng.hash(seed, x, y, salt) % 100 < 14) o.fillAir("minecraft:vine", "south=true", x, y, z1 - 1, x, y, z1 - 1);
                if (doorSide != 2 && bareSide != 2 && TempestRng.hash(seed, x, y, salt + 1) % 100 < 14) o.fillAir("minecraft:vine", "north=true", x, y, z2 + 1, x, y, z2 + 1);
            }
            for (int z = z1; z <= z2; z++) {
                if (doorSide != 3 && bareSide != 3 && TempestRng.hash(seed, z, y, salt + 2) % 100 < 14) o.fillAir("minecraft:vine", "east=true", x1 - 1, y, z, x1 - 1, y, z);
                if (doorSide != 1 && bareSide != 1 && TempestRng.hash(seed, z, y, salt + 3) % 100 < 14) o.fillAir("minecraft:vine", "west=true", x2 + 1, y, z, x2 + 1, y, z);
            }
        }
    }

    /**
     * The sky: patches of moss on the dome, glow berries, hanging roots and spore blossoms, shroomlights set into it, and chains with
     * lanterns over the lamp posts' roads. All of it hangs from the shell, in the air cells just under it.
     */
    private static void ceiling(NexusOps o, int f, long seed, List<int[]> lamps) {
        for (int z = -B; z <= B; z += 1) {
            int e = Footprint.extent(z, A, B);
            for (int x = -e; x <= e; x++) {
                int top = columnTop(x, z);
                if (top < WALL_H + 2) continue;
                int h = TempestRng.hash(seed, x, z, 21) % 1000;
                int y = f + top;                                                              // the topmost air cell; the shell is the cell above it
                if (h < 330) o.put(MOSS, x, y + 1, z);
                if (h >= 330 && h < 345) o.put(SHROOMLIGHT, x, y + 1, z);
                else if (h >= 345 && h < 380) vine(o, x, y, z, 2 + h % 5, seed);
                else if (h >= 380 && h < 400) o.fillAir("minecraft:hanging_roots", "", x, y, z, x, y, z);
                else if (h >= 400 && h < 408) o.fillAir("minecraft:spore_blossom", "", x, y, z, x, y, z);
            }
        }
        for (int[] l : lamps) {
            if (TempestRng.hash(seed, l[0], l[1], 23) % 100 >= 55) continue;
            int top = columnTop(l[0], l[1]), len = Math.min(9, Math.max(3, top - 8));
            if (top < 8) continue;
            int y = f + top;
            o.box(CHAIN, "axis=y", l[0], y - len + 1, l[1], l[0], y, l[1]);
            o.put(LANTERN, "hanging=true", l[0], y - len, l[1]);
        }
    }

    /** A hanging vine of glow berries `len` long under the shell cell above (x, y + 1, z). */
    private static void vine(NexusOps o, int x, int y, int z, int len, long seed) {
        for (int i = 0; i < len - 1; i++) o.fillAir("minecraft:cave_vines_plant", "berries=" + (TempestRng.hash(seed, x, z, 30 + i) % 3 != 0), x, y - i, z, x, y - i, z);
        o.fillAir("minecraft:cave_vines", "age=25,berries=true", x, y - (len - 1), z, x, y - (len - 1), z);
    }
}
