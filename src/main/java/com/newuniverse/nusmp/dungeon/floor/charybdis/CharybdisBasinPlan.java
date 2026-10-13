package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayList;
import java.util.List;

/**
 * The Charybdis Basin, planned (section 2, Type B of the design: "The Deep Waterways: a submerged labyrinth"; the floor boss's arena). A pure,
 * deterministic layout with no Minecraft imports: the cell goes in, a {@link ZonePlan} comes out, the same on every machine.
 *
 * <p>A drowned circular basin 56 blocks across inside, 64 with its wall, 24 blocks deep, FLOODED to the roof: every cell of it that is not
 * ruin holds a water source (the last op of the structure writes them, and only into air, so nothing built is overwritten). It is sealed with
 * prismarine like the rest of the Dungeon's stone, so the water stays in and the caves round it stay out. In the middle a sunken dais in three
 * steps, with a well at the bottom that the leviathan rises from; round it the floor is a patchwork of prismarine with rings of dark prismarine
 * and sea lanterns, twelve pillars of uneven height wrapped in kelp and sea pickles, eight broken arches, coral, and sea lanterns in the walls
 * and the roof (the water is dark). Nothing is placed in the band the boss swims in (8 to 14 blocks from the middle, 2 to 8 above the floor).
 *
 * <p>On one side, a dry Treasury: a sealed room of prismarine standing on the floor, an air pocket with the five relics' pedestals, reached by
 * a short flooded corridor and a doorway that stays solid until the mana gate is set into it. On the three other sides an airlock: a dry
 * chamber in the wall with an open iron door to the basin (a door's cell holds no water) and a sealed face to the outside, which
 * {@link CharybdisGates} opens to the nearest cave and hangs a second door in. The airlocks are the plan's entrances and arrival spot, the
 * places the collapse stands its Root Gates, and somewhere to breathe.
 *
 * <p>Ops are ordered in phases: carve; foundation, walls, roof and floor; the ruin and the rooms as solid stone; the flood; the dry rooms
 * cut out of the solid (so no air cell ever touches water while the world is being written).
 */
public final class CharybdisBasinPlan {
    private CharybdisBasinPlan() {}

    // ---------------------------------------------------------------- the size of the basin
    /** The radius of the water, the thickness of the wall, and the radius of the whole (the plan's footprint). */
    public static final int R = 28, WALL = 4, OUTER = R + WALL;
    /** The height of the water above the floor, the roof's thickness and the foundation's. */
    public static final int HEIGHT = 24, ROOF = 2, FOUNDATION = 5;
    /** The y of the topmost floor block: the Deep Waterways run from -40 to 24, and this basin from -15 to 16 inside them. */
    public static final int FLOOR_Y = -10;
    /** The basin's centre may sit this far from the middle of its cell. */
    public static final int JITTER = 10;
    /** The boss hovers this far above the floor in the middle (its arena point); it swims between 2 and 8 above the floor. */
    public static final int HOVER = 5;
    /** The radii of the three steps of the sunken dais and the depth of each below the floor (the well is the deepest). */
    public static final int STEP_1 = 11, STEP_2 = 7, WELL = 4;
    public static final int DEPTH_1 = 1, DEPTH_2 = 2, DEPTH_WELL = 3;
    /** The swimming band of the boss, round the middle: radii and heights above the floor that no ruin may touch. */
    public static final int BAND_MIN = 8, BAND_MAX = 14, BAND_LOW = 2, BAND_HIGH = 8;
    /** Where the Treasury's walls stand in the frame of its side (outward is +z): the corridor, the near wall, the back wall, the half-width. */
    public static final int CORRIDOR_FROM = 16, TREASURY_NEAR = 23, TREASURY_FAR = 31, TREASURY_HALF = 6;
    /** The airlock: the basin-side wall, the first cell of the sealed face and the outer door cell (frame of its side). */
    public static final int LOCK_INNER = 22, GATE_FIRST = 31, GATE_DOOR = 32;

    // ---------------------------------------------------------------- blocks
    private static final String BRICKS = "minecraft:prismarine_bricks", PRISM = "minecraft:prismarine", DARK = "minecraft:dark_prismarine",
            LANTERN = "minecraft:sea_lantern", STONE = "minecraft:stone_bricks", MOSSY = "minecraft:mossy_stone_bricks", WATER = "minecraft:water",
            AIR = "minecraft:air", KELP = "minecraft:kelp", KELP_PLANT = "minecraft:kelp_plant", PICKLE = "minecraft:sea_pickle",
            SEAGRASS = "minecraft:seagrass", DOOR = "minecraft:iron_door", SLAB = "minecraft:prismarine_brick_slab", GLASS = "minecraft:glass";
    private static final String[] CORAL_BLOCKS = {"minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
            "minecraft:fire_coral_block", "minecraft:horn_coral_block"};
    private static final String[] CORAL_FANS = {"minecraft:tube_coral_fan", "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan",
            "minecraft:fire_coral_fan", "minecraft:horn_coral_fan"};

    // ---------------------------------------------------------------- the four sides
    /**
     * A side of the basin seen as a local frame in which "outward" is +z and the ground to the right is +x: the same mapping NexusOps applies
     * for frame(0, 0, swap, su, sv), so a feature described once (an airlock, the Treasury) is drawn on any side. 0 north, 1 east, 2 south, 3 west.
     */
    record Side(int index, boolean swap, int su, int sv) {
        int wx(int lx, int lz) { return swap ? lz * sv : lx * su; }

        int wz(int lx, int lz) { return swap ? lx * su : lz * sv; }

        int opposite() { return (index + 2) & 3; }
    }

    static final Side[] SIDES = {new Side(0, false, -1, -1), new Side(1, true, 1, 1), new Side(2, false, 1, 1), new Side(3, true, 1, -1)};

    private static void frame(NexusOps o, Side s) { o.frame(0, 0, s.swap(), s.su(), s.sv()); }

    // ---------------------------------------------------------------- the few numbers that place the basin
    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0xA24BAED4963EE407L ^ cellZ * 0x9FB21C651E98DF25L ^ 0x2545F4914F6CDD1DL;
        h ^= h >>> 32;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return h;
    }

    public static int centreX(int cellX, int cellZ) { return NexusZone.centre(cellX) + new CharybdisRng(mix(cellX, cellZ)).range(-JITTER, JITTER); }

    public static int centreZ(int cellX, int cellZ) {
        CharybdisRng r = new CharybdisRng(mix(cellX, cellZ));
        r.next();
        return NexusZone.centre(cellZ) + r.range(-JITTER, JITTER);
    }

    /** The side of the basin the Treasury is on (0 north, 1 east, 2 south, 3 west). */
    public static int treasurySide(int cellX, int cellZ) {
        CharybdisRng r = new CharybdisRng(mix(cellX, cellZ));
        r.next();
        r.next();
        return r.below(4);
    }

    /** Which of {@code n} relics is the central one. */
    public static int centralRelicIndex(int cellX, int cellZ, int n) {
        CharybdisRng r = new CharybdisRng(mix(cellX, cellZ));
        for (int i = 0; i < 3; i++) r.next();
        return n <= 0 ? 0 : r.below(n);
    }

    // ---------------------------------------------------------------- queries other code needs
    /** An airlock: a sealed face in the wall, to be opened to a cave. */
    public record Hall(int side, int firstX, int firstZ, int doorX, int doorZ, int dx, int dz) {}

    /** The three airlocks (all sides but the Treasury's). In world coordinates; the passage runs from (firstX, firstZ) outward to the door cell. */
    public static List<Hall> halls(int cellX, int cellZ) {
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), ts = treasurySide(cellX, cellZ);
        List<Hall> out = new ArrayList<>();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            out.add(new Hall(s.index(), cx + s.wx(0, GATE_FIRST), cz + s.wz(0, GATE_FIRST), cx + s.wx(0, GATE_DOOR), cz + s.wz(0, GATE_DOOR), s.wx(0, 1), s.wz(0, 1)));
        }
        return out;
    }

    /** The air of the Treasury: {minX, minY, minZ, maxX, maxY, maxZ}, inclusive, in world coordinates. */
    public static int[] treasuryBounds(int cellX, int cellZ) {
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ);
        Side s = SIDES[treasurySide(cellX, cellZ)];
        int a = cx + s.wx(-4, TREASURY_NEAR + 1), b = cx + s.wx(4, TREASURY_FAR - 1), c = cz + s.wz(-4, TREASURY_NEAR + 1), d = cz + s.wz(4, TREASURY_FAR - 1);
        return new int[]{Math.min(a, b), FLOOR_Y + 1, Math.min(c, d), Math.max(a, b), FLOOR_Y + 5, Math.max(c, d)};
    }

    /** The spot (x, z) in a side's frame, in world coordinates. */
    private static int[] at(int cx, int cz, Side s, int lx, int lz) { return new int[]{cx + s.wx(lx, lz), cz + s.wz(lx, lz)}; }

    // ---------------------------------------------------------------- the plan
    public static ZonePlan of(int cellX, int cellZ) {
        long seed = mix(cellX, cellZ);
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), f = FLOOR_Y, ts = treasurySide(cellX, cellZ);
        Side treasury = SIDES[ts];
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.CHARYBDIS_BASIN, cellX, cellZ, cx, cz);
        b.floorY(f).topY(f + HEIGHT + ROOF).footprint(Footprint.Shape.OVAL, OUTER, OUTER);
        NexusOps o = b.ops();

        // ---- phase 0: carve the whole cylinder out of the rock (and out of the cave water that may be in it)
        o.carveDisc(OUTER, OUTER, f - FOUNDATION, f + HEIGHT + ROOF);
        o.phase();
        // ---- phase 1: the foundation, the wall ring, the roof, the floor
        shell(o, f);
        floor(o, f, seed);
        o.phase();
        // ---- phase 2: the ruin, the lamps and the rooms, all as solid stone
        dais(o, f);
        wallLamps(o, f);
        List<int[]> taken = reservations(treasury);
        pillars(o, f, seed, taken);
        arches(o, f, seed, taken);
        lamps(o, f);
        corals(o, f, seed, taken);
        frame(o, treasury);
        treasuryBlock(o, f);
        o.resetFrame();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            frame(o, s);
            lockBlock(o, f);
            o.resetFrame();
        }
        o.phase();
        // ---- phase 3: the flood
        flood(o, f);
        o.phase();
        // ---- phase 4: the dry rooms, cut out of the solid after the water is in
        frame(o, treasury);
        treasuryRoom(o, f);
        o.resetFrame();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            frame(o, s);
            lockRoom(o, f);
            o.resetFrame();
        }

        // ---- where things are
        int[] central = at(cx, cz, treasury, 0, 28);
        b.centralRelic(central[0], f + 2, central[1]);
        for (int[] m : new int[][]{{-3, 26}, {3, 26}, {-3, 29}, {3, 29}}) {
            int[] p = at(cx, cz, treasury, m[0], m[1]);
            b.minorRelic(p[0], f + 2, p[1]);
        }
        int[] gate = at(cx, cz, treasury, 0, TREASURY_NEAR);
        b.gate(gate[0], f + 1, gate[1]);
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            int[] p = at(cx, cz, s, 0, 26);
            b.entrance(p[0], f + 1, p[1]);
        }
        b.spawn(ZonePlan.ROLE_BOSS, cx, f + HOVER, cz);
        List<ZonePlan.Spot> arrival = new ArrayList<>();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            int[] p = at(cx, cz, s, 0, 25);
            arrival.add(new ZonePlan.Spot(p[0], f + 1, p[1]));
        }
        for (ZonePlan.Spot a : arrival) b.spawn(ZonePlan.ROLE_ARRIVAL, a.x(), a.y(), a.z());
        for (int k = 0; k < 24; k++) {                                                                    // where the megalodons come out of the wall
            double a = Math.toRadians(15 + 30 * k);
            b.spawn(ZonePlan.ROLE_MINION, cx + (int) Math.round(StrictMath.cos(a) * (R - 3)), f + (k % 2 == 0 ? 5 : 13), cz + (int) Math.round(StrictMath.sin(a) * (R - 3)));
        }
        return b.build();
    }

    // ---------------------------------------------------------------- phase 1: the shell and the floor
    private static void shell(NexusOps o, int f) {
        o.disc(BRICKS, "", OUTER, OUTER, f - FOUNDATION, f - 1);
        o.discHollow(BRICKS, "", OUTER, OUTER, WALL, f + 1, f + HEIGHT);
        o.disc(BRICKS, "", OUTER, OUTER, f + HEIGHT + 1, f + HEIGHT + ROOF);
        // the face of the wall: a dark course at the foot and under the roof, and a band at the middle
        o.discHollow(DARK, "", R + 1, R + 1, 1, f + 1, f + 2);
        o.discHollow(DARK, "", R + 1, R + 1, 1, f + 12, f + 12);
        o.discHollow(DARK, "", R + 1, R + 1, 1, f + HEIGHT - 1, f + HEIGHT);
        o.discHollow(PRISM, "", OUTER, OUTER, 1, f + 3, f + 11);                           // the outer skin: rough prismarine under the rock
    }

    /** The floor of the basin, a row at a time, runs of the same block as one box. */
    private static void floor(NexusOps o, int f, long seed) {
        for (int z = -OUTER; z <= OUTER; z++) {
            int e = Footprint.extent(z, OUTER, OUTER);
            if (e < 0) continue;
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

    static String floorBlock(long seed, int x, int z) {
        int r = (int) Math.round(Math.sqrt((double) x * x + (double) z * z));
        if (r == 15 || r == 21 || r == 27) return DARK;
        int h = CharybdisRng.hash(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 1) % 100;
        return h < 50 ? BRICKS : h < 72 ? PRISM : h < 86 ? DARK : h < 95 ? STONE : MOSSY;
    }

    // ---------------------------------------------------------------- phase 2: the sunken dais, the lamps, the ruin
    /** Three steps down: the floor drops one, two and three blocks, each step edged in dark stone, a lantern at the bottom of the well. */
    private static void dais(NexusOps o, int f) {
        o.disc(AIR, "", STEP_1, STEP_1, f, f);
        o.disc(AIR, "", STEP_2, STEP_2, f - DEPTH_2 + 1, f);
        o.disc(AIR, "", WELL, WELL, f - DEPTH_WELL + 1, f);
        o.discHollow(DARK, "", STEP_1, STEP_1, 2, f - DEPTH_1, f - DEPTH_1);                // each step's floor is the foundation, edged in a dark ring
        o.discHollow(DARK, "", STEP_2, STEP_2, 2, f - DEPTH_2, f - DEPTH_2);
        o.disc(DARK, "", WELL, WELL, f - DEPTH_WELL, f - DEPTH_WELL);
        o.disc(LANTERN, "", 1, 1, f - DEPTH_WELL, f - DEPTH_WELL);
        for (int k = 0; k < 8; k++) {                                                                      // lamps in the rim of the first step
            double a = Math.PI * 2 * k / 8 + Math.PI / 8;
            o.put(LANTERN, (int) Math.round(StrictMath.cos(a) * (STEP_1 - 0.5)), f - DEPTH_1, (int) Math.round(StrictMath.sin(a) * (STEP_1 - 0.5)));
        }
    }

    /** The cell of the wall's inner layer in a direction: always outside the water (the radius is pushed out until it is). */
    private static int[] ringCell(double angle, double radius) {
        double rad = radius;
        while (true) {
            int x = (int) Math.round(StrictMath.cos(angle) * rad), z = (int) Math.round(StrictMath.sin(angle) * rad);
            if ((long) x * x + (long) z * z > (long) R * R) return new int[]{x, z};
            rad += 0.25;
        }
    }

    /** Sea lanterns let into the inner face of the wall: two rings of thirty. */
    private static void wallLamps(NexusOps o, int f) {
        for (int k = 0; k < 30; k++) {
            int[] a = ringCell(Math.PI * 2 * k / 30, R + 0.5), c = ringCell(Math.PI * 2 * (k + 0.5) / 30, R + 0.5);
            o.put(LANTERN, a[0], f + 7, a[1]);
            o.put(LANTERN, c[0], f + 18, c[1]);
        }
    }

    /** Lamps in the floor (a ring of twelve at 18) and in the roof (rings of 4, 8 and 12). */
    private static void lamps(NexusOps o, int f) {
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            o.put(LANTERN, (int) Math.round(StrictMath.cos(a) * 18), f, (int) Math.round(StrictMath.sin(a) * 18));
        }
        int[][] rings = {{6, 4}, {16, 8}, {24, 12}};
        for (int[] ring : rings) {
            for (int k = 0; k < ring[1]; k++) {
                double a = Math.PI * 2 * k / ring[1] + ring[0] * 0.1;
                o.put(LANTERN, (int) Math.round(StrictMath.cos(a) * ring[0]), f + HEIGHT + 1, (int) Math.round(StrictMath.sin(a) * ring[0]));
            }
        }
    }

    // ---------------------------------------------------------------- the ruin
    /** A place nothing may be built on: a disc {x, z, radius, 0} or a rectangle {x1, z1, x2, z2, 1} (offsets from the middle). */
    private static List<int[]> reservations(Side treasury) {
        List<int[]> r = new ArrayList<>();
        r.add(new int[]{0, 0, STEP_1 + 2, 0, 0});
        int a = treasury.wx(-9, CORRIDOR_FROM - 1), b = treasury.wx(9, TREASURY_FAR + 1), c = treasury.wz(-9, CORRIDOR_FROM - 1), d = treasury.wz(9, TREASURY_FAR + 1);
        r.add(new int[]{Math.min(a, b), Math.min(c, d), Math.max(a, b), Math.max(c, d), 1});
        for (Side s : SIDES) {
            if (s.index() == treasury.index()) continue;
            int p = s.wx(-6, LOCK_INNER - 3), q = s.wx(6, 31), u = s.wz(-6, LOCK_INNER - 3), v = s.wz(6, 31);
            r.add(new int[]{Math.min(p, q), Math.min(u, v), Math.max(p, q), Math.max(u, v), 1});
        }
        return r;
    }

    private static boolean clear(List<int[]> taken, int x, int z, int radius) {
        for (int[] t : taken) {
            if (t[4] == 0) {
                long dx = x - t[0], dz = z - t[1], need = radius + t[2];
                if (dx * dx + dz * dz < need * need) return false;
            } else {
                int nx = Math.max(t[0], Math.min(x, t[2])), nz = Math.max(t[1], Math.min(z, t[3]));
                long dx = x - nx, dz = z - nz;
                if (dx * dx + dz * dz < (long) (radius + 1) * (radius + 1)) return false;
            }
        }
        return true;
    }

    /** The pillar angles: twelve round the basin at radius 20, in step with the lamps of the wall. */
    static int[] pillarCentre(int k) {
        double a = Math.PI * 2 * k / 12 + Math.PI / 12;
        return new int[]{(int) Math.round(StrictMath.cos(a) * 20), (int) Math.round(StrictMath.sin(a) * 20)};
    }

    /** Twelve pillars of uneven height, some holding up the roof and most broken, each wrapped in kelp with sea pickles at its foot. */
    private static void pillars(NexusOps o, int f, long seed, List<int[]> taken) {
        for (int k = 0; k < 12; k++) {
            int[] p = pillarCentre(k);
            if (!clear(taken, p[0], p[1], 2)) continue;
            int h = CharybdisRng.hash(seed, p[0], p[1], 3) % 100;
            int top = h < 35 ? f + HEIGHT : f + 6 + h % 12;                                              // a third reach the roof, the rest stop between 6 and 17 up
            o.box(PRISM, p[0] - 2, f + 1, p[1] - 1, p[0] + 2, f + 1, p[1] + 1);
            o.box(PRISM, p[0] - 1, f + 1, p[1] - 2, p[0] + 1, f + 1, p[1] + 2);
            o.box(BRICKS, p[0] - 1, f + 2, p[1] - 1, p[0] + 1, top, p[1] + 1);
            for (int y = f + 5; y < top; y += 6) o.box(DARK, p[0] - 1, y, p[1] - 1, p[0] + 1, y, p[1] + 1);
            if (top < f + HEIGHT) {                                                                     // a broken top: a few stumps on the crown
                for (int s = 0; s < 3; s++) {
                    int dx = CharybdisRng.hash(seed, p[0], p[1], 10 + s) % 3 - 1, dz = CharybdisRng.hash(seed, p[0], p[1], 20 + s) % 3 - 1;
                    o.put(BRICKS, p[0] + dx, top + 1, p[1] + dz);
                }
            }
            o.put(LANTERN, p[0], f + 4, p[1] - 1);
            int upper = Math.min(top - 1, f + 12);
            if (upper >= f + 7) o.put(LANTERN, p[0], upper, p[1] + 1);
            // kelp round the foot, as tall as the pillar's stumps or a little more, and pickles in the gaps
            int[][] kelp = {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
            for (int s = 0; s < kelp.length; s++) {
                int kh = 5 + CharybdisRng.hash(seed, p[0] + kelp[s][0], p[1] + kelp[s][1], 30) % 9;
                o.box(KELP_PLANT, "", p[0] + kelp[s][0], f + 1, p[1] + kelp[s][1], p[0] + kelp[s][0], f + kh - 1, p[1] + kelp[s][1]);
                o.put(KELP, "age=25", p[0] + kelp[s][0], f + kh, p[1] + kelp[s][1]);
            }
            o.put(PICKLE, "pickles=4,waterlogged=true", p[0] + 2, f + 2, p[1]);                         // on the pillar's foot
            o.put(PICKLE, "pickles=3,waterlogged=true", p[0] - 2, f + 2, p[1]);
            o.put(PICKLE, "pickles=2,waterlogged=true", p[0], f + 2, p[1] + 2);
        }
    }

    /** Eight broken arches in pairs on the four diagonals, at 24.5 from the middle: two columns, a lintel that stops short of the lower one, a lamp for a keystone. */
    private static void arches(NexusOps o, int f, long seed, List<int[]> taken) {
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4 + (k / 2) * Math.PI / 2 + (k % 2 == 0 ? -1 : 1) * Math.toRadians(11.25);
            double tx = -StrictMath.sin(a), tz = StrictMath.cos(a);
            int ax = (int) Math.round(StrictMath.cos(a) * 24.5), az = (int) Math.round(StrictMath.sin(a) * 24.5);
            int lx = (int) Math.round(ax + tx * 3.5), lz = (int) Math.round(az + tz * 3.5), rx = (int) Math.round(ax - tx * 3.5), rz = (int) Math.round(az - tz * 3.5);
            if (!clear(taken, ax, az, 1)) continue;
            int tallL = f + 12, tallR = f + 7 + CharybdisRng.hash(seed, ax, az, 40) % 4;               // the right column is the broken one
            column(o, lx, lz, f, tallL);
            column(o, rx, rz, f, tallR);
            // the lintel: along the line from the tall column's top, 70% of the way across
            int steps = 7;
            for (int s = 0; s <= steps * 7 / 10; s++) {
                int x = (int) Math.round(lx + (rx - lx) * (double) s / steps), z = (int) Math.round(lz + (rz - lz) * (double) s / steps);
                o.put(s == 0 ? DARK : BRICKS, x, tallL + 1, z);
                o.put(BRICKS, x, tallL, z);
            }
            int mx = (int) Math.round(lx + (rx - lx) * 0.35), mz = (int) Math.round(lz + (rz - lz) * 0.35);
            o.put(LANTERN, mx, tallL, mz);
            o.put(PICKLE, "pickles=3,waterlogged=true", rx, tallR + 1, rz);
        }
    }

    private static void column(NexusOps o, int x, int z, int f, int top) {
        o.box(PRISM, x, f + 1, z, x + 1, f + 1, z + 1);
        o.box(BRICKS, x, f + 2, z, x + 1, top, z + 1);
        o.box(DARK, x, f + 6, z, x + 1, f + 6, z + 1);
    }

    /** Coral growing on the floor in patches, with a fan or two on each, away from the other structures. */
    private static void corals(NexusOps o, int f, long seed, List<int[]> taken) {
        CharybdisRng r = new CharybdisRng(seed ^ 0x5DEECE66DL);
        int made = 0;
        for (int attempt = 0; attempt < 600 && made < 16; attempt++) {
            double a = r.unit() * Math.PI * 2, d = 13 + r.unit() * 13;
            int x = (int) Math.round(StrictMath.cos(a) * d), z = (int) Math.round(StrictMath.sin(a) * d);
            if ((long) x * x + (long) z * z > (long) (R - 2) * (R - 2) || !clear(taken, x, z, 3)) continue;
            // keep clear of the pillars and arches (they were reserved by their own placement only if placed; check the ring they stand on)
            boolean near = false;
            for (int k = 0; k < 12 && !near; k++) {
                int[] p = pillarCentre(k);
                near = (p[0] - x) * (p[0] - x) + (p[1] - z) * (p[1] - z) < 16;
            }
            for (int k = 0; k < 8 && !near; k++) {
                double aa = Math.PI / 4 + (k / 2) * Math.PI / 2 + (k % 2 == 0 ? -1 : 1) * Math.toRadians(11.25);
                double ax = StrictMath.cos(aa) * 24.5, az = StrictMath.sin(aa) * 24.5;
                near = (ax - x) * (ax - x) + (az - z) * (az - z) < 25;
            }
            if (near) continue;
            int kind = r.below(CORAL_BLOCKS.length);
            o.put(CORAL_BLOCKS[kind], x, f + 1, z);
            o.put(CORAL_FANS[kind], "waterlogged=true", x, f + 2, z);
            o.put(CORAL_BLOCKS[(kind + 1) % CORAL_BLOCKS.length], x + 1, f + 1, z);
            o.put(CORAL_BLOCKS[(kind + 2) % CORAL_BLOCKS.length], x, f + 1, z + 1);
            o.put(CORAL_FANS[(kind + 2) % CORAL_FANS.length], "waterlogged=true", x, f + 2, z + 1);
            o.put(SEAGRASS, x - 1, f + 1, z);
            o.put(SEAGRASS, x, f + 1, z - 1);
            taken.add(new int[]{x, z, 2, 0, 0});
            made++;
        }
    }

    // ---------------------------------------------------------------- phase 3: the flood
    /** Water in every cell of the basin that is still air, down into the dais (the wells are air now): a source in each, the last thing the structure writes. */
    private static void flood(NexusOps o, int f) {
        for (int v = -R; v <= R; v++) {
            int e = Footprint.extent(v, R, R);
            if (e >= 0) o.fillAir(WATER, "", -e, f - DEPTH_WELL, v, e, f + HEIGHT, v);
        }
    }

    // ---------------------------------------------------------------- the Treasury (frame of its side)
    /** The solid block of the Treasury, the corridor to it and the sealed doorway: the room itself is cut out of it after the flood. */
    private static void treasuryBlock(NexusOps o, int f) {
        int n = TREASURY_NEAR, far = TREASURY_FAR, h = TREASURY_HALF;
        o.box(BRICKS, -h, f + 1, n, h, f + 7, far);                                                    // the room as a solid block
        o.box(DARK, -h, f + 1, n, -h, f + 7, n);                                                       // dark corners and a dark course
        o.box(DARK, h, f + 1, n, h, f + 7, n);
        o.box(DARK, -h, f + 7, n, h, f + 7, far);
        o.box(DARK, -h, f + 3, n, h, f + 3, n);
        // the corridor: walls two thick at x = +-2..3, a roof, open to the water at its mouth; its inside is left as water for the flood
        o.box(BRICKS, -3, f + 1, CORRIDOR_FROM, -2, f + 4, n - 1);
        o.box(BRICKS, 2, f + 1, CORRIDOR_FROM, 3, f + 4, n - 1);
        o.box(BRICKS, -3, f + 4, CORRIDOR_FROM, 3, f + 4, n - 1);
        o.box(DARK, -3, f + 1, CORRIDOR_FROM, -3, f + 4, CORRIDOR_FROM);                               // the mouth: two posts and lamps
        o.box(DARK, 3, f + 1, CORRIDOR_FROM, 3, f + 4, CORRIDOR_FROM);
        o.put(LANTERN, -3, f + 3, CORRIDOR_FROM);
        o.put(LANTERN, 3, f + 3, CORRIDOR_FROM);
        o.put(LANTERN, 0, f + 4, CORRIDOR_FROM + 3);
        o.put(LANTERN, 0, f + 4, n - 2);
        o.box(DARK, -1, f + 3, n, 1, f + 3, n);                                                         // the lintel over the doorway
        o.put(LANTERN, 0, f + 4, n);
    }

    /** The room: nine wide, seven deep, five high, a lit ceiling, the plinths of the relics. The doorway (0, f+1..f+2, near) stays solid until the gate is set in it. */
    private static void treasuryRoom(NexusOps o, int f) {
        int n = TREASURY_NEAR, far = TREASURY_FAR;
        o.air(-4, f + 1, n + 1, 4, f + 5, far - 1);
        o.put(LANTERN, 0, f + 5, n + 2);
        o.put(LANTERN, -3, f + 5, n + 5);
        o.put(LANTERN, 3, f + 5, n + 5);
        o.put(LANTERN, 0, f + 5, far - 1);
        o.box(DARK, -1, f + 1, 27, 1, f + 1, 29);                                                       // the broad plinth of the central relic, one block up
        o.box(DARK, -3, f + 1, 26, -3, f + 1, 26);                                                      // and the four small ones
        o.box(DARK, 3, f + 1, 26, 3, f + 1, 26);
        o.box(DARK, -3, f + 1, 29, -3, f + 1, 29);
        o.box(DARK, 3, f + 1, 29, 3, f + 1, 29);
    }

    // ---------------------------------------------------------------- the airlocks (frame of their side)
    /** The solid block of an airlock: a wall into the basin, the chamber's walls and roof, the porch lamps. */
    private static void lockBlock(NexusOps o, int f) {
        o.box(BRICKS, -3, f + 1, LOCK_INNER, 3, f + 5, 28);
        o.box(DARK, -3, f + 1, LOCK_INNER, -3, f + 5, LOCK_INNER);
        o.box(DARK, 3, f + 1, LOCK_INNER, 3, f + 5, LOCK_INNER);
        o.box(DARK, -3, f + 3, LOCK_INNER, 3, f + 3, LOCK_INNER);
        o.put(LANTERN, -2, f + 5, LOCK_INNER);
        o.put(LANTERN, 2, f + 5, LOCK_INNER);
        o.put(LANTERN, -2, f + 2, LOCK_INNER - 1);
        o.put(LANTERN, 2, f + 2, LOCK_INNER - 1);
    }

    /** The chamber cut out of it: five wide, eight deep, three high, a lit ceiling, and the open iron door (both halves) in the basin-side wall. */
    private static void lockRoom(NexusOps o, int f) {
        o.air(-2, f + 1, LOCK_INNER + 1, 2, f + 3, 30);
        o.put(LANTERN, 0, f + 4, 26);
        o.put(LANTERN, 0, f + 4, 29);
        o.put(DOOR, "facing=south,half=lower,hinge=left,open=true", 0, f + 1, LOCK_INNER);
        o.put(DOOR, "facing=south,half=upper,hinge=left,open=true", 0, f + 2, LOCK_INNER);
        o.put(LANTERN, 0, f + 3, LOCK_INNER);
    }
}
