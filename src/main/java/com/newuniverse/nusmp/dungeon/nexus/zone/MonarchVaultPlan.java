package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayList;
import java.util.List;

/**
 * The Monarch's Vault, planned (docs/living_dungeon.md section 5, zone 1). A pure, deterministic layout with no Minecraft imports: the core
 * cell goes in, a {@link ZonePlan} comes out, the same on every machine.
 *
 * <p>A colossal, hollow chamber of obsidian, 44 blocks across the floor and 36 high at its heart, with a ceiling that comes down in a dome
 * toward the wall, closed by a three-block shell all round, so the flooded caves of the Dungeon cannot pour in. Eight huge pillars hold the
 * dome. In the middle a four-step dais carries the Monarch's throne. Gold, raw gold and gilded blackstone lie piled in mounds with the
 * things of the dead (pots, banners, anvils, enchanting tables, lanterns, skulls and bones) set on top: all of it decoration, nothing is
 * lootable. On one side, behind a wall and a gate the Mana Zone opens, lies the Treasury with the pedestals of the five relics; on the three
 * other sides an archway marks a sealed gate in the shell, which {@link MonarchWays} opens to a cave where one comes close.
 *
 * <p>Ops are ordered in phases: carve; floor and shell; structures (dais, throne, pillars, treasury, porches, statues); details (carpets,
 * lights, hoards, bones, chandeliers). Everything is written in the cell's own offsets from the centre.
 */
public final class MonarchVaultPlan {
    private MonarchVaultPlan() {}

    // ---------------------------------------------------------------- the size of the hall
    /** The radius of the hall's floor, the thickness of its shell, and the radius of the whole (the plan's footprint). */
    public static final int R = 44, WALL = 3, OUTER = R + WALL;
    /** The height of the air above the floor at the middle, how much the ceiling comes down at the wall, the roof's thickness and the foundation's. */
    public static final int HEIGHT = 36, DOME = 10, ROOF = 3, FOUNDATION = 3;
    /** The radii of the four steps of the dais (each one block higher than the last) and the height of its top above the floor. */
    public static final int[] DAIS = {18, 15, 12, 9};
    public static final int DAIS_TOP = DAIS.length;
    /** The hall's centre may sit this far from the middle of its core cell. */
    public static final int JITTER = 10;
    /** Where the walls of the Treasury stand, in the frame of its side (outward is +z): the wall toward the hall, the wall behind, the half-width. */
    public static final int TREASURY_NEAR = 32, TREASURY_FAR = 42, TREASURY_HALF = 10;
    /** How far the sealed gate of a porch is from the middle of the hall: where the opening starts and where the iron door stands (frame of its side). */
    public static final int GATE_FIRST = 43, GATE_DOOR = OUTER;

    // ---------------------------------------------------------------- blocks
    private static final String OBSIDIAN = "minecraft:obsidian", CRYING = "minecraft:crying_obsidian", GILDED = "minecraft:gilded_blackstone",
            BRICKS = "minecraft:polished_blackstone_bricks", CRACKED = "minecraft:cracked_polished_blackstone_bricks", SMOOTH = "minecraft:polished_blackstone",
            BLACKSTONE = "minecraft:blackstone", CHISELED = "minecraft:chiseled_polished_blackstone", GOLD = "minecraft:gold_block",
            RAW_GOLD = "minecraft:raw_gold_block", BONE = "minecraft:bone_block", SOUL_LANTERN = "minecraft:soul_lantern", LANTERN = "minecraft:lantern",
            CHAIN = "minecraft:chain", CARPET = "minecraft:purple_carpet", CARPET_EDGE = "minecraft:yellow_carpet", SKULL = "minecraft:skeleton_skull",
            WITHER_SKULL = "minecraft:wither_skeleton_skull";

    // ---------------------------------------------------------------- the four sides
    /**
     * A side of the hall seen as a local frame in which "outward" is +z and the ground to the right is +x: the same mapping NexusOps applies
     * for frame(0, 0, swap, su, sv), so a feature described once (a porch, the Treasury, an aisle) is drawn on any side. 0 north, 1 east, 2 south, 3 west.
     */
    record Side(int index, boolean swap, int su, int sv) {
        int wx(int lx, int lz) { return swap ? lz * sv : lx * su; }

        int wz(int lx, int lz) { return swap ? lx * su : lz * sv; }

        /** The side facing the other way. */
        int opposite() { return (index + 2) & 3; }
    }

    static final Side[] SIDES = {new Side(0, false, -1, -1), new Side(1, true, 1, 1), new Side(2, false, 1, 1), new Side(3, true, 1, -1)};

    private static void frame(NexusOps o, Side s) { o.frame(0, 0, s.swap(), s.su(), s.sv()); }

    // ---------------------------------------------------------------- the few numbers that place the hall
    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0xD6E8FEB86659FD93L ^ cellZ * 0xA0761D6478BD642FL ^ 0x2545F4914F6CDD1DL;
        h ^= h >>> 32;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return h;
    }

    public static int centreX(int cellX, int cellZ) { return NexusZone.centre(cellX) + new MonarchRng(mix(cellX, cellZ)).range(-JITTER, JITTER); }

    public static int centreZ(int cellX, int cellZ) {
        MonarchRng r = new MonarchRng(mix(cellX, cellZ));
        r.next();
        return NexusZone.centre(cellZ) + r.range(-JITTER, JITTER);
    }

    /** The y of the topmost floor block: -57 to -55, so a bit of rock is left under the foundation and the whole hall lies in the deepest tier. */
    public static int floorY(int cellX, int cellZ) {
        MonarchRng r = new MonarchRng(mix(cellX, cellZ));
        r.next();
        r.next();
        return -57 + r.below(3);
    }

    /** The side of the hall the Treasury is on (0 north, 1 east, 2 south, 3 west); the grand entrance is on the opposite side. */
    public static int treasurySide(int cellX, int cellZ) {
        MonarchRng r = new MonarchRng(mix(cellX, cellZ));
        r.next();
        r.next();
        r.next();
        return r.below(4);
    }

    /** Which of {@code n} relics is the central one. */
    public static int centralRelicIndex(int cellX, int cellZ, int n) {
        MonarchRng r = new MonarchRng(mix(cellX, cellZ));
        for (int i = 0; i < 4; i++) r.next();
        return n <= 0 ? 0 : r.below(n);
    }

    // ---------------------------------------------------------------- the dome
    private static final int[] REACH = new int[HEIGHT + 2];

    static {
        for (int h = 1; h <= HEIGHT; h++) {
            int r = 0;
            while (r < R && DOME * (r + 1) * (r + 1) < (HEIGHT + 1 - h) * R * R) r++;
            REACH[h] = r;
        }
        REACH[HEIGHT + 1] = -1;
    }

    /** The radius of the hall's air at the layer h above the floor (1 to HEIGHT); -1 above the ceiling. */
    public static int reach(int h) { return h < 1 || h > HEIGHT ? -1 : REACH[h]; }

    /** The height above the floor of the topmost air in the column at offset (x, z) from the centre; 0 outside the hall. */
    public static int columnTop(int x, int z) {
        long d2 = (long) x * x + (long) z * z;
        for (int h = HEIGHT; h >= 1; h--) if (d2 <= (long) REACH[h] * REACH[h]) return h;
        return 0;
    }

    // ---------------------------------------------------------------- queries other code needs
    /** A way into the hall: a sealed gate in the shell, to be opened to a cave. */
    public record Hall(int side, int firstX, int firstZ, int doorX, int doorZ, int dx, int dz) {}

    /** The three halls (sealed gates) that lead in, all sides but the Treasury's. In world coordinates; the passage runs from (firstX, firstZ) outward to the door cell. */
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
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), f = floorY(cellX, cellZ);
        Side s = SIDES[treasurySide(cellX, cellZ)];
        int a = cx + s.wx(-(TREASURY_HALF - 1), TREASURY_NEAR + 1), b = cx + s.wx(TREASURY_HALF - 1, TREASURY_FAR - 1);
        int c = cz + s.wz(-(TREASURY_HALF - 1), TREASURY_NEAR + 1), d = cz + s.wz(TREASURY_HALF - 1, TREASURY_FAR - 1);
        return new int[]{Math.min(a, b), f + 1, Math.min(c, d), Math.max(a, b), f + 7, Math.max(c, d)};
    }

    // ---------------------------------------------------------------- the plan
    public static ZonePlan of(int cellX, int cellZ) {
        long seed = mix(cellX, cellZ);
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), f = floorY(cellX, cellZ), ts = treasurySide(cellX, cellZ);
        Side treasury = SIDES[ts], grand = SIDES[treasury.opposite()];
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.MONARCH_VAULT, cellX, cellZ, cx, cz);
        b.floorY(f).topY(f + HEIGHT + ROOF).footprint(Footprint.Shape.OVAL, OUTER, OUTER);
        NexusOps o = b.ops();

        carve(o, f);
        o.phase();
        shell(o, f);
        floor(o, f, seed);
        o.phase();
        dais(o, f);
        frame(o, treasury);
        throne(o, f);
        o.resetFrame();
        for (int[] p : PILLARS) pillar(o, p[0], p[1], f);
        frame(o, treasury);
        treasuryRoom(o, f);
        o.resetFrame();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            frame(o, s);
            porch(o, f);
        }
        o.resetFrame();
        frame(o, grand);
        statues(o, f, 36, 26);
        frame(o, treasury);
        statues(o, f, 24);
        o.resetFrame();
        o.phase();
        carpets(o, f, ts);
        lights(o, f);
        List<int[]> taken = reservations(ts);
        hoards(o, f, seed, taken);
        ribs(o, f);
        chandeliers(o, f);

        // ---- where things are
        b.centralRelic(cx + treasury.wx(0, 39), f + 2, cz + treasury.wz(0, 39));
        int[][] minors = {{-6, 36}, {6, 36}, {-6, 40}, {6, 40}};
        for (int[] m : minors) b.minorRelic(cx + treasury.wx(m[0], m[1]), f + 2, cz + treasury.wz(m[0], m[1]));
        b.gate(cx + treasury.wx(0, TREASURY_NEAR), f + 1, cz + treasury.wz(0, TREASURY_NEAR));
        for (Side s : SIDES) if (s.index() != ts) b.entrance(cx + s.wx(0, GATE_FIRST - 1), f + 1, cz + s.wz(0, GATE_FIRST - 1));
        b.spawn(ZonePlan.ROLE_BOSS, cx, f + DAIS_TOP + 1, cz);
        b.spawn(ZonePlan.ROLE_ARRIVAL, cx + grand.wx(0, 40), f + 1, cz + grand.wz(0, 40));
        return b.build();
    }

    // ---------------------------------------------------------------- phase 0 and 1: the hall
    /** Clears the air of the hall: one disc per run of layers of the same radius. */
    private static void carve(NexusOps o, int f) {
        int h = 1;
        while (h <= HEIGHT) {
            int e = h;
            while (e < HEIGHT && REACH[e + 1] == REACH[h]) e++;
            o.carveDisc(REACH[h], REACH[h], f + h, f + e);
            h = e + 1;
        }
    }

    /** The foundation, the wall and the dome-shaped roof in obsidian: everything inside the round of OUTER that is not the air of the hall. */
    private static void shell(NexusOps o, int f) {
        o.disc(OBSIDIAN, "", OUTER, OUTER, f - FOUNDATION, f);
        int h = 1;
        while (h <= HEIGHT) {
            int e = h;
            while (e < HEIGHT && REACH[e + 1] == REACH[h]) e++;
            o.discHollow(OBSIDIAN, "", OUTER, OUTER, OUTER - REACH[h], f + h, f + e);
            h = e + 1;
        }
        o.disc(OBSIDIAN, "", OUTER, OUTER, f + HEIGHT + 1, f + HEIGHT + ROOF);
        // the face of the wall: a dark course at the foot, gilded bands, studs of crying obsidian that glow
        o.discHollow(BRICKS, "", R + 1, R + 1, 1, f + 1, f + 2);
        o.discHollow(GILDED, "", R + 1, R + 1, 1, f + 12, f + 12);
        o.discHollow(GILDED, "", R + 1, R + 1, 1, f + 24, f + 24);
        for (int k = 0; k < 36; k++) {
            double a = Math.PI * 2 * k / 36;
            o.put(CRYING, (int) Math.round(StrictMath.cos(a) * (R + 0.6)), f + 18, (int) Math.round(StrictMath.sin(a) * (R + 0.6)));
        }
    }

    /** The floor of the hall: dark stone in a patchwork, with rings of gold, glowing obsidian and gilded blackstone let into it. */
    private static void floor(NexusOps o, int f, long seed) {
        for (int z = -R; z <= R; z++) {
            int e = Footprint.extent(z, R, R);
            int start = -e;
            String cur = null;
            for (int x = -e; x <= e + 1; x++) {
                String b = x <= e ? floorBlock(seed, x, z) : null;
                if (b == null ? cur != null : !b.equals(cur)) {
                    if (cur != null) o.box(cur, start, f, z, x - 1, f, z);
                    cur = b;
                    start = x;
                }
            }
        }
    }

    static String floorBlock(long seed, int x, int z) {
        int r = (int) Math.round(Math.sqrt((double) x * x + (double) z * z));
        if (r == 19 || r == 20) return GOLD;
        if (r == 24) return CRYING;
        if (r == 30 || r == 31 || r == 42 || r == 43) return GILDED;
        int h = MonarchRng.hash(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 1) % 100;
        return h < 58 ? SMOOTH : h < 82 ? BRICKS : h < 94 ? CRACKED : BLACKSTONE;
    }

    // ---------------------------------------------------------------- phase 2: the dais, the throne, the pillars
    /** Four steps, each a block higher; a gilded rim on each, a sigil of gold on the top. */
    private static void dais(NexusOps o, int f) {
        for (int i = 0; i < DAIS.length; i++) {
            int y = f + 1 + i;
            o.disc(i % 2 == 0 ? BRICKS : SMOOTH, "", DAIS[i], DAIS[i], y, y);
            o.discHollow(GILDED, "", DAIS[i], DAIS[i], 1, y, y);
        }
        int top = f + DAIS_TOP;
        o.discHollow(GOLD, "", 7, 7, 1, top, top);
        o.discHollow(CRYING, "", 5, 5, 1, top, top);
        o.disc(OBSIDIAN, "", 2, 2, top, top);
    }

    /** The throne, behind the Monarch, in the frame of the Treasury's side (the throne's back is toward the Treasury). */
    private static void throne(NexusOps o, int f) {
        int top = f + DAIS_TOP;
        o.box(OBSIDIAN, -4, top + 1, 4, 4, top + 1, 8);
        o.box(GILDED, -4, top + 1, 4, 4, top + 1, 4);
        o.box(GOLD, -2, top + 2, 4, 2, top + 2, 6);                               // the seat
        o.box(OBSIDIAN, -4, top + 2, 7, 4, top + 9, 8);                           // the back
        o.box(OBSIDIAN, -2, top + 10, 7, 2, top + 14, 8);
        o.box(OBSIDIAN, -1, top + 15, 7, 1, top + 18, 8);
        o.box(GILDED, -4, top + 2, 7, -4, top + 9, 7);
        o.box(GILDED, 4, top + 2, 7, 4, top + 9, 7);
        o.box(GILDED, -2, top + 10, 7, -2, top + 14, 7);
        o.box(GILDED, 2, top + 10, 7, 2, top + 14, 7);
        o.put(CRYING, -1, top + 7, 7);                                            // two eyes in the stone
        o.put(CRYING, 1, top + 7, 7);
        o.box(GOLD, -1, top + 19, 7, 1, top + 19, 8);
        o.box(OBSIDIAN, -4, top + 2, 4, -4, top + 3, 6);                          // the arms
        o.box(OBSIDIAN, 4, top + 2, 4, 4, top + 3, 6);
        o.box(GOLD, -4, top + 4, 4, -4, top + 4, 6);
        o.box(GOLD, 4, top + 4, 4, 4, top + 4, 6);
        // bones at its feet
        o.box(BONE, -6, top + 1, 5, -5, top + 1, 7);
        o.box(BONE, 5, top + 1, 5, 6, top + 1, 7);
        o.put(BONE, "axis=x", -6, top + 2, 6);
        o.put(BONE, "axis=z", 6, top + 2, 6);
        o.put(SKULL, "rotation=4", -6, top + 3, 6);
        o.put(SKULL, "rotation=12", 6, top + 3, 6);
        o.put(WITHER_SKULL, "rotation=8", -5, top + 2, 5);
        o.put(SKULL, "rotation=7", 5, top + 2, 5);
    }

    /** The centres of the eight pillars: round the hall at 28, between the aisles. */
    static final int[][] PILLARS = {{26, 11}, {11, 26}, {-11, 26}, {-26, 11}, {-26, -11}, {-11, -26}, {11, -26}, {26, -11}};

    private static boolean pillarCell(int dx, int dz) { return Math.abs(dx) < 3 || Math.abs(dz) < 3; }

    /** A pillar of seven by seven blocks with the corners cut, from the floor up to the dome, which it follows. */
    private static void pillar(NexusOps o, int px, int pz, int f) {
        int low = Integer.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) if (pillarCell(dx, dz)) low = Math.min(low, columnTop(px + dx, pz + dz));
        int hi = f + low;
        o.box(BRICKS, px - 3, f + 1, pz - 2, px + 3, f + 2, pz + 2);
        o.box(BRICKS, px - 2, f + 1, pz - 3, px + 2, f + 2, pz + 3);
        o.box(CHISELED, px - 2, f + 3, pz - 2, px + 2, f + 3, pz + 2);
        o.box(OBSIDIAN, px - 2, f + 4, pz - 2, px + 2, hi - 3, pz + 2);
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -2; sz <= 2; sz += 4) o.box(GILDED, px + sx, f + 4, pz + sz, px + sx, hi - 3, pz + sz);
        for (int y = f + 10; y < hi - 4; y += 8) o.ring(GOLD, "", px - 2, pz - 2, px + 2, pz + 2, y, y);
        o.box(CHISELED, px - 2, hi - 2, pz - 2, px + 2, hi - 2, pz + 2);
        o.box(BRICKS, px - 3, hi - 1, pz - 2, px + 3, hi, pz + 2);
        o.box(BRICKS, px - 2, hi - 1, pz - 3, px + 2, hi, pz + 3);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!pillarCell(dx, dz)) continue;
                int top = f + columnTop(px + dx, pz + dz);
                if (top > hi) o.box(OBSIDIAN, px + dx, hi + 1, pz + dz, px + dx, top, pz + dz);
            }
        }
    }

    // ---------------------------------------------------------------- the Treasury (frame of its side)
    /** The Treasury: a closed room of obsidian and gold behind a wall with one doorway, the doorway left open for the mana gate. */
    private static void treasuryRoom(NexusOps o, int f) {
        int n = TREASURY_NEAR, far = TREASURY_FAR, h = TREASURY_HALF;
        o.box(BRICKS, -h, f, n, h, f, far);                                        // the floor, a strip of gold down the middle
        o.box(GOLD, -1, f, n + 1, 1, f, far - 1);
        o.box(OBSIDIAN, -h, f + 1, n, h, f + 7, n);                                // the wall toward the hall
        o.box(OBSIDIAN, -h, f + 1, far, h, f + 7, far);                            // the wall behind
        o.box(OBSIDIAN, -h, f + 1, n + 1, -h, f + 7, far - 1);                     // the walls at the sides
        o.box(OBSIDIAN, h, f + 1, n + 1, h, f + 7, far - 1);
        o.box(OBSIDIAN, -h, f + 8, n, h, f + 9, far);                              // the roof
        for (int sx = -(h - 1); sx <= h - 1; sx += 2 * (h - 1)) {                  // gilded posts in the corners
            o.box(GILDED, sx, f + 1, n + 1, sx, f + 7, n + 1);
            o.box(GILDED, sx, f + 1, far - 1, sx, f + 7, far - 1);
        }
        o.box(GILDED, -h, f + 5, n + 1, -h, f + 5, far - 1);                       // a gilded band on the side walls
        o.box(GILDED, h, f + 5, n + 1, h, f + 5, far - 1);
        // the doorway, two high and one wide: left open, the Mana Zone's gate is set into it when the Nexus is populated
        o.air(0, f + 1, n, 0, f + 2, n);
        o.box(GILDED, -1, f + 1, n, -1, f + 3, n);
        o.box(GILDED, 1, f + 1, n, 1, f + 3, n);
        o.box(CHISELED, 0, f + 3, n, 0, f + 3, n);
        o.box(CHISELED, -2, f + 4, n, 2, f + 4, n);
        o.box(GOLD, -2, f + 5, n, 2, f + 5, n);
        o.box(GILDED, -4, f + 1, n - 2, -4, f + 2, n - 2);                         // braziers on either side of the gate
        o.box(GILDED, 4, f + 1, n - 2, 4, f + 2, n - 2);
        o.put(SOUL_LANTERN, "hanging=false", -4, f + 3, n - 2);
        o.put(SOUL_LANTERN, "hanging=false", 4, f + 3, n - 2);
        // the pedestals' plinths: a broad one for the central relic, four small ones for the others
        o.box(GOLD, -2, f + 1, 37, 2, f + 1, 41);
        o.box(GILDED, -1, f + 1, 38, 1, f + 1, 40);
        for (int[] m : new int[][]{{-6, 36}, {6, 36}, {-6, 40}, {6, 40}}) {
            o.box(GOLD, m[0], f + 1, m[1], m[0], f + 1, m[1]);
            o.box(GILDED, m[0] - 1, f, m[1] - 1, m[0] + 1, f, m[1] + 1);
        }
        // lights, and heaps of plain gold along the walls
        for (int[] l : new int[][]{{-8, 34}, {8, 34}, {-8, 40}, {8, 40}, {0, 35}}) o.put(SOUL_LANTERN, "hanging=true", l[0], f + 7, l[1]);
        o.box(GOLD, -9, f + 1, 35, -9, f + 2, 38);
        o.box(GOLD, 9, f + 1, 35, 9, f + 2, 38);
        o.box(RAW_GOLD, -9, f + 3, 36, -9, f + 3, 37);
        o.box(RAW_GOLD, 9, f + 3, 36, 9, f + 3, 37);
    }

    // ---------------------------------------------------------------- the porches (frame of their side)
    /**
     * A porch at the foot of a sealed gate: two columns and a lintel round a face of chiseled stone set into the wall, with lanterns under
     * the lintel. The passage itself (from z=43 through the shell to the door at z=47) is opened later, where a cave comes close.
     */
    private static void porch(NexusOps o, int f) {
        for (int sx = -1; sx <= 1; sx += 2) {
            int a = sx * 3, b = sx * 5;
            o.box(OBSIDIAN, Math.min(a, b), f + 1, 41, Math.max(a, b), f + 7, 43);
            o.box(GILDED, sx * 3, f + 1, 41, sx * 3, f + 7, 41);
            o.box(GOLD, Math.min(a, b), f + 4, 41, Math.max(a, b), f + 4, 43);
            o.box(CHISELED, Math.min(a, b), f + 7, 41, Math.max(a, b), f + 7, 43);
        }
        o.box(OBSIDIAN, -5, f + 6, 41, 5, f + 6, 43);
        o.box(CHISELED, -5, f + 7, 41, 5, f + 7, 43);
        o.box(GILDED, -2, f + 1, GATE_FIRST, 2, f + 5, GATE_FIRST);               // the sealed face
        o.box(CHISELED, -1, f + 1, GATE_FIRST, 1, f + 4, GATE_FIRST);
        o.box(GOLD, -2, f + 5, GATE_FIRST, 2, f + 5, GATE_FIRST);
        o.put(SOUL_LANTERN, "hanging=true", -2, f + 5, 42);
        o.put(SOUL_LANTERN, "hanging=true", 2, f + 5, 42);
        o.box(GILDED, -7, f + 1, 40, -7, f + 2, 40);                               // two braziers in front
        o.box(GILDED, 7, f + 1, 40, 7, f + 2, 40);
        o.put(LANTERN, "hanging=false", -7, f + 3, 40);
        o.put(LANTERN, "hanging=false", 7, f + 3, 40);
    }

    // ---------------------------------------------------------------- the statues
    /** A pair of colossal sentinels at each given depth along the aisle of the current frame, each on a plinth, a blade of crying obsidian in its hands. */
    private static void statues(NexusOps o, int f, int... depths) {
        for (int z : depths) {
            statue(o, f, -5, z, 1);
            statue(o, f, 5, z, -1);
        }
    }

    /** One statue at (x, z): d is +1 if it faces the aisle toward +x, -1 toward -x. */
    private static void statue(NexusOps o, int f, int x, int z, int d) {
        o.box(BRICKS, x - 1, f + 1, z - 1, x + 1, f + 2, z + 1);
        o.box(GILDED, x - 1, f + 3, z - 1, x + 1, f + 3, z + 1);
        o.box(OBSIDIAN, x, f + 4, z - 1, x, f + 7, z - 1);                         // legs
        o.box(OBSIDIAN, x, f + 4, z + 1, x, f + 7, z + 1);
        o.box(OBSIDIAN, x - 1, f + 8, z - 2, x + 1, f + 12, z + 2);                // torso
        o.box(GILDED, x + d, f + 9, z - 1, x + d, f + 11, z + 1);                  // breastplate
        o.box(OBSIDIAN, x, f + 9, z - 3, x, f + 12, z - 3);                        // arms
        o.box(OBSIDIAN, x, f + 9, z + 3, x, f + 12, z + 3);
        o.box(OBSIDIAN, x - 1, f + 13, z - 1, x + 1, f + 15, z + 1);               // head
        o.put(GILDED, x, f + 16, z);
        o.put(CRYING, x + d, f + 14, z - 1);                                       // eyes
        o.put(CRYING, x + d, f + 14, z + 1);
        o.box(CRYING, x + d, f + 5, z + 3, x + d, f + 15, z + 3);                  // the blade, point up, in the right hand
        o.box(GILDED, x + d, f + 8, z + 2, x + d, f + 8, z + 4);                   // its cross-guard
        o.put(GOLD, x + d, f + 4, z + 3);
    }

    // ---------------------------------------------------------------- phase 3: carpets, lights
    /** A carpet down each aisle: purple with a yellow edge, from the dais to the porch, to the Treasury's gate on its side. */
    private static void carpets(NexusOps o, int f, int ts) {
        for (Side s : SIDES) {
            frame(o, s);
            int end = s.index() == ts ? TREASURY_NEAR - 1 : GATE_FIRST - 3;
            o.box(CARPET, -2, f + 1, 21, 2, f + 1, end);
            o.box(CARPET_EDGE, -3, f + 1, 21, -3, f + 1, end);
            o.box(CARPET_EDGE, 3, f + 1, 21, 3, f + 1, end);
        }
        o.resetFrame();
    }

    /** Lanterns on the lowest step of the dais and on the second ring of the floor. */
    private static void lights(NexusOps o, int f) {
        for (int[] p : new int[][]{{12, 12}, {-12, 12}, {12, -12}, {-12, -12}, {16, 6}, {16, -6}, {-16, 6}, {-16, -6}, {6, 16}, {-6, 16}, {6, -16}, {-6, -16}}) {
            o.put(SOUL_LANTERN, "hanging=false", p[0], f + 2, p[1]);
        }
    }

    // ---------------------------------------------------------------- the hoards
    /** A place that nothing may be piled on: a disc {x, z, radius, 0} or a rectangle {x1, z1, x2, z2, 1}. */
    private static List<int[]> reservations(int ts) {
        List<int[]> r = new ArrayList<>();
        r.add(new int[]{0, 0, DAIS[0] + 3, 0, 0});
        for (int[] p : PILLARS) r.add(new int[]{p[0], p[1], 5, 0, 0});
        for (Side s : SIDES) {
            int lat = s.index() == ts ? 12 : 8;
            int a = s.wx(-lat, 17), b = s.wx(lat, 47), c = s.wz(-lat, 17), d = s.wz(lat, 47);
            r.add(new int[]{Math.min(a, b), Math.min(c, d), Math.max(a, b), Math.max(c, d), 1});
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) r.add(new int[]{sx * 26 - 5, sz * 26 - 6, sx * 26 + 5, sz * 26 + 6, 1});     // the ribcages
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

    /** Gold mounds with the things of the dead on them, then small piles of bone and skulls, placed where there is room. */
    private static void hoards(NexusOps o, int f, long seed, List<int[]> taken) {
        MonarchRng r = new MonarchRng(seed ^ 0x5DEECE66DL);
        int made = 0;
        for (int attempt = 0; attempt < 3000 && made < 22; attempt++) {
            int radius = r.range(3, 7);
            double a = r.unit() * Math.PI * 2, d = 21 + r.unit() * 19;
            int x = (int) Math.round(StrictMath.cos(a) * d), z = (int) Math.round(StrictMath.sin(a) * d);
            if (Math.sqrt((double) x * x + (double) z * z) + radius > R - 1 || !clear(taken, x, z, radius)) continue;
            mound(o, f, seed, x, z, radius, r.range(5, 10), made, false);
            taken.add(new int[]{x, z, radius, 0, 0});
            made++;
        }
        int piles = 0;
        for (int attempt = 0; attempt < 2000 && piles < 16; attempt++) {
            int radius = r.range(2, 3);
            double a = r.unit() * Math.PI * 2, d = 21 + r.unit() * 21;
            int x = (int) Math.round(StrictMath.cos(a) * d), z = (int) Math.round(StrictMath.sin(a) * d);
            if (Math.sqrt((double) x * x + (double) z * z) + radius > R - 1 || !clear(taken, x, z, radius)) continue;
            mound(o, f, seed, x, z, radius, r.range(3, 5), 100 + piles, true);
            taken.add(new int[]{x, z, radius, 0, 0});
            piles++;
        }
    }

    /** One mound: a solid core of gold under a surface of mixed blocks, a few things standing on its crest. */
    private static void mound(NexusOps o, int f, long seed, int cx, int cz, int radius, int height, int salt, boolean bones) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double d = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (d > radius + 0.5) continue;
                double t = d / (radius + 0.5);
                int hh = (int) Math.round(height * (1 - t * t)) - (MonarchRng.hash(seed, cx + dx, cz + dz, salt) % 3 == 0 ? 1 : 0);
                if (hh <= 0) continue;
                int x = cx + dx, z = cz + dz, top = f + hh;
                if (hh >= 2) o.box(bones ? BONE : GOLD, x, f + 1, z, x, top - 1, z);
                int h = MonarchRng.hash(seed, x, z, 7 + salt) % 100;
                o.put(bones ? (h < 70 ? BONE : h < 85 ? RAW_GOLD : GOLD) : topBlock(h), bones && h < 70 && h % 2 == 0 ? "axis=x" : "", x, top, z);
                int c = MonarchRng.hash(seed, x, z, 13 + salt) % 100;
                if (t < 0.7 && c < (bones ? 22 : 17)) artifact(o, seed, x, top + 1, z, MonarchRng.hash(seed, x, z, 17 + salt), bones);
            }
        }
    }

    private static String topBlock(int h) {
        return h < 50 ? GOLD : h < 67 ? RAW_GOLD : h < 79 ? GILDED : h < 87 ? BONE : h < 90 ? "minecraft:diamond_block" : h < 93 ? "minecraft:emerald_block"
                : h < 97 ? "minecraft:amethyst_block" : "minecraft:lapis_block";
    }

    /** A thing set on a mound; chosen by c, turned by a hash. Decoration only: no chests, no barrels, nothing that holds an item. */
    private static void artifact(NexusOps o, long seed, int x, int y, int z, int c, boolean bones) {
        int turn = MonarchRng.hash(seed, x, z, 31) % 16;
        String face = new String[]{"north", "east", "south", "west"}[turn & 3];
        if (bones) {
            if (c % 3 == 0) o.put(SKULL, "rotation=" + turn, x, y, z);
            else if (c % 3 == 1) o.put(WITHER_SKULL, "rotation=" + turn, x, y, z);
            else o.put(BONE, "axis=" + (turn % 2 == 0 ? "x" : "z"), x, y, z);
            return;
        }
        switch (c % 11) {
            case 0 -> o.put("minecraft:decorated_pot", "facing=" + face, x, y, z);
            case 1 -> o.put("minecraft:enchanting_table", x, y, z);
            case 2 -> o.put("minecraft:anvil", "facing=" + face, x, y, z);
            case 3 -> o.put(SOUL_LANTERN, "hanging=false", x, y, z);
            case 4 -> o.put(LANTERN, "hanging=false", x, y, z);
            case 5 -> o.put("minecraft:purple_banner", "rotation=" + turn, x, y, z);
            case 6 -> o.put("minecraft:black_banner", "rotation=" + turn, x, y, z);
            case 7 -> o.put(SKULL, "rotation=" + turn, x, y, z);
            case 8 -> o.put(WITHER_SKULL, "rotation=" + turn, x, y, z);
            case 9 -> o.put("minecraft:candle", "candles=3,lit=true", x, y, z);
            default -> o.put("minecraft:amethyst_cluster", "facing=up", x, y, z);
        }
    }

    // ---------------------------------------------------------------- the ribcages and the chandeliers
    /** A giant's ribs in each corner between the pillars: five arches of bone and a spine along their crowns. */
    private static void ribs(NexusOps o, int f) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = sx * 26, cz = sz * 26;
                for (int k = -2; k <= 2; k++) {
                    int x = cx + k * 2;
                    int last = Integer.MIN_VALUE;
                    for (int s = 0; s <= 24; s++) {
                        double a = Math.PI * s / 24;
                        int z = cz + (int) Math.round(StrictMath.cos(a) * 4.5), y = f + 1 + (int) Math.round(StrictMath.sin(a) * 8);
                        int key = z * 1000 + y;
                        if (key == last) continue;
                        last = key;
                        o.put(BONE, "axis=y", x, y, z);
                    }
                }
                o.box(BONE, "axis=x", cx - 4, f + 9, cz, cx + 4, f + 9, cz);
                o.put(SKULL, "rotation=" + (sx > 0 ? 4 : 12), cx, f + 10, cz);
            }
        }
    }

    /** Chains from the dome with a lantern at the end: a crown over the dais, rings further out. */
    private static void chandeliers(NexusOps o, int f) {
        chandelier(o, f, 0, 0, 9, LANTERN);
        for (int[] p : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) chandelier(o, f, p[0], p[1], 5, SOUL_LANTERN);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8;
            chandelier(o, f, (int) Math.round(StrictMath.cos(a) * 22), (int) Math.round(StrictMath.sin(a) * 22), 6, LANTERN);
            double b = a + Math.PI / 8;
            chandelier(o, f, (int) Math.round(StrictMath.cos(b) * 36), (int) Math.round(StrictMath.sin(b) * 36), 6, SOUL_LANTERN);
        }
    }

    private static void chandelier(NexusOps o, int f, int x, int z, int length, String lantern) {
        int top = f + columnTop(x, z);
        o.box(CHAIN, "axis=y", x, top - length + 1, z, x, top, z);
        o.put(lantern, "hanging=true", x, top - length, z);
    }
}
