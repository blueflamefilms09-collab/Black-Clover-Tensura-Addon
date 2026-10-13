package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayList;
import java.util.List;

/**
 * The Goliath's Pit, planned (the Upper Catacombs' floor boss arena): a pure, deterministic layout with no Minecraft imports. The cell goes
 * in, a {@link ZonePlan} comes out, the same on every machine.
 *
 * <p>A ruined colosseum of pale stone bricks, 56 blocks across the floor and 30 high at its heart, closed by a four-block shell all round and
 * a shallow dome (so the Dungeon's caves cannot pour in). Two rings of broken pillars of uneven height stand on the floor; behind them,
 * against the wall, two tiers of ruined galleries with a balustrade of posts, the lower one reached by four ramps of fallen stone. In the
 * middle a nine-block disc of bedrock, cracked with obsidian and crying obsidian, is the seal the Goliath rises from. On one side a Treasury
 * of pale quartz and gold stands behind a one-block doorway for the mana gate, with the pedestals of the relics; on the three other sides a
 * porch marks a sealed gate in the wall, which {@link GoliathPitGates} opens to a cave where one comes close. Light comes from lanterns on
 * the pillars and posts and from chains of lanterns hung from the dome.
 *
 * <p>Ops are ordered in phases: carve; the foundation, shell and floor; structures (seal, wall, galleries, pillars, ramps, Treasury,
 * porches); details (moss, rubble, lights). Everything is written in the cell's own offsets from the centre.
 */
public final class GoliathPitPlan {
    private GoliathPitPlan() {}

    // ---------------------------------------------------------------- the size of the pit
    /** The radius of the pit's floor, the thickness of its wall, and the radius of the whole (the plan's footprint). */
    public static final int R = 28, WALL = 4, OUTER = R + WALL;
    /** The height of the air above the floor at the middle, how much the ceiling comes down at the wall, the roof's thickness and the foundation's. */
    public static final int HEIGHT = 30, DOME = 9, ROOF = 3, FOUNDATION = 3;
    /** The radius of the bedrock seal (nine blocks across), of the outer and of the inner ring of pillars. */
    public static final int DISC = 4, RING = 22, INNER_RING = 13;
    /** The heights of the two gallery decks above the floor. */
    public static final int TIER1 = 5, TIER2 = 11;
    /** The pit's centre may sit this far from the middle of its cell. */
    public static final int JITTER = 10;
    /** The floor is 40 to 42: the Upper Catacombs begin at 24 and the roof stays under 100. */
    public static final int FLOOR_MIN = 40, FLOOR_SPREAD = 3;
    /** Where the walls of the Treasury stand, in the frame of its side (outward is +z): the wall toward the pit, the wall behind, the half-width. */
    public static final int TREASURY_NEAR = 18, TREASURY_FAR = 28, TREASURY_HALF = 6;
    /** How far the sealed face of a porch is from the middle of the pit: where the opening starts and where the iron door stands (frame of its side). */
    public static final int GATE_FIRST = R, GATE_DOOR = OUTER;

    // ---------------------------------------------------------------- blocks
    private static final String BRICKS = "minecraft:stone_bricks", CRACKED = "minecraft:cracked_stone_bricks", MOSSY = "minecraft:mossy_stone_bricks",
            CHISELED = "minecraft:chiseled_stone_bricks", ANDESITE = "minecraft:polished_andesite", DIORITE = "minecraft:polished_diorite",
            CALCITE = "minecraft:calcite", SMOOTH = "minecraft:smooth_stone", COBBLE = "minecraft:cobblestone", MOSSY_COBBLE = "minecraft:mossy_cobblestone",
            BEDROCK = "minecraft:bedrock", OBSIDIAN = "minecraft:obsidian", CRYING = "minecraft:crying_obsidian", GOLD = "minecraft:gold_block",
            QUARTZ = "minecraft:quartz_bricks", QUARTZ_SMOOTH = "minecraft:smooth_quartz", QUARTZ_CHISELED = "minecraft:chiseled_quartz_block",
            QUARTZ_PILLAR = "minecraft:quartz_pillar", LANTERN = "minecraft:lantern", SOUL_LANTERN = "minecraft:soul_lantern", CHAIN = "minecraft:chain",
            WALL_POST = "minecraft:stone_brick_wall", SLAB = "minecraft:stone_brick_slab", MOSS = "minecraft:moss_block", MOSS_CARPET = "minecraft:moss_carpet";

    // ---------------------------------------------------------------- the four sides
    /**
     * A side of the pit seen as a local frame in which "outward" is +z and the ground to the right is +x: the same mapping NexusOps applies
     * for frame(0, 0, swap, su, sv), so a feature described once (a porch, the Treasury) is drawn on any side. 0 north, 1 east, 2 south, 3 west.
     */
    record Side(int index, boolean swap, int su, int sv) {
        /** The world offset of the local point (lx, lz). */
        int wx(int lx, int lz) { return swap ? lz * sv : lx * su; }

        int wz(int lx, int lz) { return swap ? lx * su : lz * sv; }

        /** The local point of the world offset (x, z): the inverse of {@link #wx} and {@link #wz} (the signs are their own inverses). */
        int lx(int x, int z) { return swap ? z * su : x * su; }

        int lz(int x, int z) { return swap ? x * sv : z * sv; }

        /** The side facing the other way. */
        int opposite() { return (index + 2) & 3; }
    }

    static final Side[] SIDES = {new Side(0, false, -1, -1), new Side(1, true, 1, 1), new Side(2, false, 1, 1), new Side(3, true, 1, -1)};

    private static void frame(NexusOps o, Side s) { o.frame(0, 0, s.swap(), s.su(), s.sv()); }

    // ---------------------------------------------------------------- the few numbers that place the pit
    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0xA0761D6478BD642FL ^ cellZ * 0xD6E8FEB86659FD93L ^ 0x7F4A7C15F39CC060L;
        h ^= h >>> 31;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return h;
    }

    public static int centreX(int cellX, int cellZ) { return NexusZone.centre(cellX) + new PitRng(mix(cellX, cellZ)).range(-JITTER, JITTER); }

    public static int centreZ(int cellX, int cellZ) {
        PitRng r = new PitRng(mix(cellX, cellZ));
        r.next();
        return NexusZone.centre(cellZ) + r.range(-JITTER, JITTER);
    }

    /** The y of the topmost floor block: 40 to 42, in the Upper Catacombs (y of 24 and up) and well under the roof of the dimension. */
    public static int floorY(int cellX, int cellZ) {
        PitRng r = new PitRng(mix(cellX, cellZ));
        r.next();
        r.next();
        return FLOOR_MIN + r.below(FLOOR_SPREAD);
    }

    /** The side of the pit the Treasury is on (0 north, 1 east, 2 south, 3 west); the grand entrance, where one arrives, is on the opposite side. */
    public static int treasurySide(int cellX, int cellZ) {
        PitRng r = new PitRng(mix(cellX, cellZ));
        r.next();
        r.next();
        r.next();
        return r.below(4);
    }

    /** Which of {@code n} relics is the central one. */
    public static int centralRelicIndex(int cellX, int cellZ, int n) {
        PitRng r = new PitRng(mix(cellX, cellZ));
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

    /** The radius of the pit's air at the layer h above the floor (1 to HEIGHT); -1 above the ceiling. */
    public static int reach(int h) { return h < 1 || h > HEIGHT ? -1 : REACH[h]; }

    /** The height above the floor of the topmost air in the column at offset (x, z) from the centre; 0 outside the pit. */
    public static int columnTop(int x, int z) {
        long d2 = (long) x * x + (long) z * z;
        for (int h = HEIGHT; h >= 1; h--) if (d2 <= (long) REACH[h] * REACH[h]) return h;
        return 0;
    }

    // ---------------------------------------------------------------- queries other code needs
    /** A way into the pit: a sealed gate in the wall, to be opened to a cave. */
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
        return new int[]{Math.min(a, b), f + 1, Math.min(c, d), Math.max(a, b), f + 5, Math.max(c, d)};
    }

    /** The floor block in the middle of the bedrock seal, in world coordinates: where the Goliath rises. */
    public static int[] sealCentre(int cellX, int cellZ) { return new int[]{centreX(cellX, cellZ), floorY(cellX, cellZ), centreZ(cellX, cellZ)}; }

    /** One pillar: its centre (offsets from the pit's centre), the height of the top of its shaft above the floor, and whether it still has its capital. */
    public record Pillar(int x, int z, int top, boolean whole) {}

    /** The angles (degrees from +x towards +z) of the pillars of the outer ring: none at the four axes, where the porches and the Treasury are. */
    private static final int[] OUTER_ANGLES = {27, 63, 117, 153, 207, 243, 297, 333};

    /** The pillars that stand: eight in the outer ring, eight in the inner; none in front of the Treasury. Offsets from the centre. */
    public static List<Pillar> pillars(int cellX, int cellZ) {
        long seed = mix(cellX, cellZ);
        Side t = SIDES[treasurySide(cellX, cellZ)];
        List<Pillar> out = new ArrayList<>();
        for (int k = 0; k < 16; k++) {
            boolean outer = k < OUTER_ANGLES.length;
            double a = Math.toRadians(outer ? OUTER_ANGLES[k] : 22.5 + 45.0 * (k - OUTER_ANGLES.length));
            int radius = outer ? RING : INNER_RING;
            int x = (int) Math.round(StrictMath.cos(a) * radius), z = (int) Math.round(StrictMath.sin(a) * radius);
            if (inTreasurySector(t, x, z)) continue;
            int roll = PitRng.hash(seed, k, 0, 3) % 100;
            int top = roll < 22 ? 4 + roll % 3 : roll < 50 ? 7 + roll % 4 : roll < 80 ? 11 + roll % 4 : 15 + roll % 4;
            boolean whole = top >= 11 && PitRng.hash(seed, x, z, 61) % 100 < 55;
            out.add(new Pillar(x, z, top, whole));
        }
        return out;
    }

    // ---------------------------------------------------------------- the plan
    public static ZonePlan of(int cellX, int cellZ) {
        long seed = mix(cellX, cellZ);
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ), f = floorY(cellX, cellZ), ts = treasurySide(cellX, cellZ);
        Side treasury = SIDES[ts], grand = SIDES[treasury.opposite()];
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.GOLIATH_PIT, cellX, cellZ, cx, cz);
        b.floorY(f).topY(f + HEIGHT + ROOF).footprint(Footprint.Shape.OVAL, OUTER, OUTER);
        NexusOps o = b.ops();
        List<Pillar> pillars = pillars(cellX, cellZ);

        carve(o, f);
        o.phase();
        shell(o, f, seed);
        floor(o, f, seed);
        o.phase();
        seal(o, f, seed);
        wallFace(o, f, seed);
        deck(o, f + TIER1, 24, seed, 1, treasury);
        deck(o, f + TIER2, 25, seed, 2, treasury);
        for (Pillar p : pillars) pillar(o, f, p);
        ramps(o, f);
        frame(o, treasury);
        treasuryRoom(o, f);
        o.resetFrame();
        for (Side s : SIDES) {
            if (s.index() == ts) continue;
            frame(o, s);
            porch(o, f);
        }
        o.resetFrame();
        o.phase();
        moss(o, f, seed, pillars, treasury);
        rubble(o, f, seed, pillars, treasury);
        lights(o, f, seed, pillars, treasury);
        chandeliers(o, f);

        // ---- where things are
        b.centralRelic(cx + treasury.wx(0, 25), f + 2, cz + treasury.wz(0, 25));
        int[][] minors = {{-4, 21}, {4, 21}, {-4, 25}, {4, 25}};
        for (int[] m : minors) b.minorRelic(cx + treasury.wx(m[0], m[1]), f + 2, cz + treasury.wz(m[0], m[1]));
        b.gate(cx + treasury.wx(0, TREASURY_NEAR), f + 1, cz + treasury.wz(0, TREASURY_NEAR));
        for (Side s : SIDES) if (s.index() != ts) b.entrance(cx + s.wx(0, GATE_FIRST - 1), f + 1, cz + s.wz(0, GATE_FIRST - 1));
        b.spawn(ZonePlan.ROLE_BOSS, cx, f + 1, cz);
        b.spawn(ZonePlan.ROLE_ARRIVAL, cx + grand.wx(0, GATE_FIRST - 4), f + 1, cz + grand.wz(0, GATE_FIRST - 4));
        return b.build();
    }

    // ---------------------------------------------------------------- phase 0 and 1: the pit
    /** The runs of layers (h1..h2) that share one radius of air. */
    private static List<int[]> runs() {
        List<int[]> out = new ArrayList<>();
        int h = 1;
        while (h <= HEIGHT) {
            int e = h;
            while (e < HEIGHT && REACH[e + 1] == REACH[h]) e++;
            out.add(new int[]{h, e});
            h = e + 1;
        }
        return out;
    }

    /** Clears the air of the pit: one disc per run of layers of the same radius. */
    private static void carve(NexusOps o, int f) {
        for (int[] r : runs()) o.carveDisc(REACH[r[0]], REACH[r[0]], f + r[0], f + r[1]);
    }

    /** The foundation, the wall and the dome-shaped roof in stone bricks: everything inside the round of OUTER that is not the air of the pit; the surface the dome shows is dressed. */
    private static void shell(NexusOps o, int f, long seed) {
        o.disc(BRICKS, "", OUTER, OUTER, f - FOUNDATION, f);
        int n = 0;
        for (int[] r : runs()) {
            o.discHollow(BRICKS, "", OUTER, OUTER, OUTER - REACH[r[0]], f + r[0], f + r[1]);
            if (REACH[r[0]] < R) {                                                      // the dome's own surface: courses of brick, a chiselled band now and then
                int roll = PitRng.hash(seed, n++, 0, 11) % 100;
                String face = roll < 55 ? BRICKS : roll < 75 ? CRACKED : roll < 90 ? MOSSY : CHISELED;
                o.discHollow(face, "", REACH[r[0]] + 1, REACH[r[0]] + 1, 1, f + r[0], f + r[1]);
            }
        }
        o.disc(BRICKS, "", OUTER, OUTER, f + HEIGHT + 1, f + HEIGHT + ROOF);
        o.disc(CHISELED, "", 3, 3, f + HEIGHT + 1, f + HEIGHT + 1);                    // a medallion in the middle of the ceiling
        o.discHollow(CHISELED, "", 6, 6, 1, f + HEIGHT + 1, f + HEIGHT + 1);
    }

    /** The floor of the pit: pale stone in rings and patches, with the bands of calcite and chiselled brick let into it. */
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
        if (r <= DISC) return BRICKS;
        if (r == DISC + 1) return OBSIDIAN;
        if (r == DISC + 2) return SMOOTH;
        if (r == 14 || r == 15) return CALCITE;
        if (r == 21) return CHISELED;
        if (r >= R - 1) return SMOOTH;
        int h = PitRng.hash(seed, Math.floorDiv(x, 2), Math.floorDiv(z, 2), 1) % 100;
        if (r <= 12) return ((Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) & 1) == 0 ? ANDESITE : (h < 70 ? DIORITE : CRACKED);
        return h < 52 ? BRICKS : h < 70 ? CRACKED : h < 84 ? MOSSY : h < 94 ? ANDESITE : DIORITE;
    }

    // ---------------------------------------------------------------- phase 2: the seal, the walls, the galleries, the pillars
    /** The cells of the radial cracks round the seal, as {x, z, crying}: nine arms that wander outward a little way, a few of them glowing. */
    static List<int[]> cracks(long seed) {
        PitRng r = new PitRng(seed ^ 0x5DEECE66DL);
        List<int[]> out = new ArrayList<>();
        for (int arm = 0; arm < 9; arm++) {
            double a = (arm + r.unit() * 0.6) * Math.PI * 2 / 9;
            double x = Math.cos(a) * (DISC + 1), z = Math.sin(a) * (DISC + 1);
            int len = r.range(7, 15);
            for (int s = 0; s < len; s++) {
                a += (r.unit() - 0.5) * 0.7;
                x += Math.cos(a);
                z += Math.sin(a);
                int ix = (int) Math.round(x), iz = (int) Math.round(z);
                if (ix * ix + iz * iz <= DISC * DISC) continue;
                out.add(new int[]{ix, iz, r.chance(22) ? 1 : 0});
                if (s > 2 && r.chance(12)) {                                                    // a branch
                    double ba = a + (r.chance(50) ? 1 : -1) * (0.7 + r.unit() * 0.5), bx = x, bz = z;
                    for (int t = 0, bl = r.range(2, 5); t < bl; t++) {
                        bx += Math.cos(ba);
                        bz += Math.sin(ba);
                        out.add(new int[]{(int) Math.round(bx), (int) Math.round(bz), 0});
                    }
                }
            }
        }
        return out;
    }

    /** The bedrock seal the Goliath rises from, with the cracks of obsidian and crying obsidian running out of it. */
    private static void seal(NexusOps o, int f, long seed) {
        for (int[] c : cracks(seed)) o.put(c[2] == 1 ? CRYING : OBSIDIAN, c[0], f, c[1]);
        o.disc(BEDROCK, "", DISC, DISC, f - 1, f);
        o.discHollow(CRYING, "", DISC + 1, DISC + 1, 1, f, f);                           // a ring of glowing stone round it, broken here and there
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12 + 0.3;
            if (PitRng.hash(seed, k, 5, 9) % 100 < 45) o.put(OBSIDIAN, (int) Math.round(Math.cos(a) * (DISC + 1)), f, (int) Math.round(Math.sin(a) * (DISC + 1)));
        }
    }

    /** The face of the wall the air sees: a plinth of polished stone, courses of brick, chiselled bands where the decks meet it, cracked and mossy patches. */
    private static void wallFace(NexusOps o, int f, long seed) {
        for (int z = -(R + 1); z <= R + 1; z++) {
            for (int x = -(R + 1); x <= R + 1; x++) {
                long d2 = (long) x * x + (long) z * z;
                if (d2 <= (long) R * R || d2 > (long) (R + 1) * (R + 1)) continue;            // the single ring of the wall that touches the air
                int y = 1;
                while (y <= 21) {
                    String b = faceBlock(seed, x, z, y);
                    int e = y;
                    while (e < 21 && faceBlock(seed, x, z, e + 1).equals(b)) e++;
                    o.box(b, x, f + y, z, x, f + e, z);
                    y = e + 1;
                }
            }
        }
    }

    private static String faceBlock(long seed, int x, int z, int y) {
        if (y <= 2) return ANDESITE;
        if (y == TIER1 || y == TIER2) return CHISELED;
        int h = PitRng.hash(seed, Math.floorDiv(x, 2), Math.floorDiv(z, 2) * 31 + y / 2, 21) % 100;
        return h < 62 ? BRICKS : h < 80 ? CRACKED : h < 94 ? MOSSY : DIORITE;
    }

    /** Is the cell (x, z) in the sector the Treasury and its approach take (in its frame: within 9 of the axis, from the middle of the floor outward)? */
    static boolean inTreasurySector(Side t, int x, int z) { return Math.abs(t.lx(x, z)) <= TREASURY_HALF + 3 && t.lz(x, z) > TREASURY_NEAR - 6; }

    /** Is the cell (x, z) in front of a porch (within 6 of the axis of a gate, from the pillar ring outward)? */
    static boolean inPorchSector(int x, int z) { return (Math.abs(x) <= 6 && Math.abs(z) > RING - 2) || (Math.abs(z) <= 6 && Math.abs(x) > RING - 2); }

    /** Where a ramp starts along its diagonal, and how many blocks it climbs for each block it runs (under 1.4, so that two cells side by side never differ by more than one). */
    static final double RAMP_START = 16.0, RAMP_SLOPE = 0.75;

    /** The ramp of the diagonal (sx, sz) is a strip along the diagonal, 5 wide, from radius 16 out to the wall: the along-distance and the sideways distance of a cell from its axis. */
    private static double along(int sx, int sz, int x, int z) { return (sx * x + sz * z) / Math.sqrt(2); }

    private static double side(int sx, int sz, int x, int z) { return (sx * z - sz * x) / Math.sqrt(2); }

    /** Is this cell on one of the four ramps (a strip 2.5 either side of the diagonal, from radius 16 on), or within `margin` blocks of one? */
    static boolean nearRamp(int x, int z, int margin) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                if (along(sx, sz, x, z) >= RAMP_START - margin && Math.abs(side(sx, sz, x, z)) <= 2.5 + margin) return true;
            }
        }
        return false;
    }

    private static boolean deckCell(long seed, int x, int z, int salt, Side treasury) {
        if (inTreasurySector(treasury, x, z) || inPorchSector(x, z)) return false;
        if (salt == 1 && nearRamp(x, z, 2)) return true;                                               // the ramps lead onto a deck that is whole
        return PitRng.hash(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), salt) % 100 >= 14;      // patches of 3 x 3 are missing
    }

    /** One tier of the ruined galleries: a deck of stone brick from radius `inner` to the wall, with missing patches, and a balustrade of posts on its inner edge. */
    private static void deck(NexusOps o, int y, int inner, long seed, int salt, Side treasury) {
        for (int z = -R; z <= R; z++) {
            String cur = null;
            int start = 0;
            for (int x = -R; x <= R + 1; x++) {
                long d2 = (long) x * x + (long) z * z;
                String b = null;
                if (x <= R && d2 >= (long) inner * inner && d2 <= (long) R * R && deckCell(seed, x, z, salt, treasury)) {
                    int h = PitRng.hash(seed, x, z, 40 + salt) % 100;
                    b = h < 72 ? BRICKS : h < 86 ? MOSSY : CRACKED;
                }
                if (b == null ? cur != null : !b.equals(cur)) {
                    if (cur != null) o.box(cur, start, y, z, x - 1, y, z);
                    cur = b;
                    start = x;
                }
            }
        }
        for (int z = -R; z <= R; z++) {                                                    // the balustrade: posts on the inner edge of the deck, some of them gone
            for (int x = -R; x <= R; x++) {
                long d2 = (long) x * x + (long) z * z;
                if (d2 < (long) inner * inner || d2 >= (long) (inner + 1) * (inner + 1) || !deckCell(seed, x, z, salt, treasury)) continue;
                if (salt == 1 && nearRamp(x, z, 0)) continue;                              // the ramps come out on the deck: no post in the way
                if (PitRng.hash(seed, x, z, 50 + salt) % 100 < 58) o.put(WALL_POST, x, y + 1, z);
            }
        }
    }

    /** One pillar: a plinth, a shaft three blocks square with a band of chiselled brick every fourth block, and a capital, or a broken top. */
    private static void pillar(NexusOps o, int f, Pillar p) {
        int x = p.x(), z = p.z(), shaft = f + p.top();
        o.box(ANDESITE, x - 2, f + 1, z - 2, x + 2, f + 1, z + 2);
        o.box(BRICKS, x - 1, f + 2, z - 1, x + 1, shaft - 1, z + 1);
        for (int y = f + 4; y < shaft - 1; y += 4) o.box(CHISELED, x - 1, y, z - 1, x + 1, y, z + 1);
        if (p.whole()) {
            o.box(CHISELED, x - 2, shaft, z - 2, x + 2, shaft, z + 2);                    // a capital
            o.box(ANDESITE, x - 1, shaft + 1, z - 1, x + 1, shaft + 1, z + 1);
        } else {
            o.put(CRACKED, x, shaft, z);                                                   // a broken top: some of the last course is missing, the rest cracked
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int h = PitRng.hash(x * 7 + dx, x, z + dz, 62) % 100;
                    if (h < 45) o.put(CRACKED, x + dx, shaft, z + dz);
                    else if (h < 70) o.put(SLAB, "type=bottom", x + dx, shaft, z + dz);
                }
            }
        }
    }

    /** Four ramps of fallen stone, one on each diagonal, climbing from the floor at radius 16 to the first deck: never more than a block higher from one cell to the next, five wide. */
    private static void ramps(NexusOps o, int f) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int z = -R; z <= R; z++) {
                    for (int x = -R; x <= R; x++) {
                        double s = along(sx, sz, x, z), u = side(sx, sz, x, z);
                        if (s < RAMP_START || Math.abs(u) > 2.5 || (long) x * x + (long) z * z > (long) R * R) continue;
                        int height = Math.min(TIER1, (int) Math.floor((s - RAMP_START) * RAMP_SLOPE) + 1);       // level with the first deck from about radius 21 to the wall
                        int h = PitRng.hash(0x51ED270BL, x, z, 71) % 100;
                        o.box(h < 40 ? BRICKS : h < 62 ? COBBLE : h < 80 ? MOSSY_COBBLE : h < 92 ? CRACKED : ANDESITE, x, f + 1, z, x, f + height, z);
                    }
                }
            }
        }
    }

    /** A heap of rubble: a paraboloid of blocks of the given radius and height (in blocks above the floor). */
    private static void heap(NexusOps o, int f, long seed, int cx, int cz, int radius, int height) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double d = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (d > radius + 0.5) continue;
                int x = cx + dx, z = cz + dz;
                if ((long) x * x + (long) z * z > (long) (R - 1) * (R - 1)) continue;
                double t = d / (radius + 0.5);
                int hh = (int) Math.round(height * (1 - t * t));
                if (hh <= 0) continue;
                int h = PitRng.hash(seed, x, z, 72) % 100;
                o.box(h < 40 ? BRICKS : h < 62 ? COBBLE : h < 80 ? MOSSY_COBBLE : h < 92 ? CRACKED : ANDESITE, x, f + 1, z, x, f + hh, z);
            }
        }
    }

    // ---------------------------------------------------------------- the Treasury (frame of its side)
    /** The Treasury: a closed room of pale quartz and gold against the wall, behind a wall with one doorway, the doorway left open for the mana gate. */
    private static void treasuryRoom(NexusOps o, int f) {
        int n = TREASURY_NEAR, far = TREASURY_FAR, h = TREASURY_HALF;
        o.box(QUARTZ_SMOOTH, -h, f, n, h, f, far);                                          // the floor, a strip of gold down the middle
        o.box(GOLD, -1, f, n + 1, 1, f, far - 1);
        o.box(QUARTZ, -h, f + 1, n, h, f + 7, n);                                           // the wall toward the pit
        o.box(QUARTZ, -h, f + 1, far, h, f + 7, far);                                       // the wall behind
        o.box(QUARTZ, -h, f + 1, n + 1, -h, f + 7, far - 1);                                // the walls at the sides
        o.box(QUARTZ, h, f + 1, n + 1, h, f + 7, far - 1);
        o.box(QUARTZ, -h, f + 6, n, h, f + 7, far);                                         // the roof
        for (int sx = -(h - 1); sx <= h - 1; sx += 2 * (h - 1)) {                           // gold posts in the corners
            o.box(GOLD, sx, f + 1, n + 1, sx, f + 5, n + 1);
            o.box(GOLD, sx, f + 1, far - 1, sx, f + 5, far - 1);
        }
        o.box(QUARTZ_CHISELED, -h, f + 4, n + 1, -h, f + 4, far - 1);                       // a chiselled band on the side walls
        o.box(QUARTZ_CHISELED, h, f + 4, n + 1, h, f + 4, far - 1);
        // the doorway, two high and one wide: left open, the Mana Zone's gate is set into it when the Nexus is populated
        o.air(0, f + 1, n, 0, f + 2, n);
        o.box(QUARTZ_PILLAR, -1, f + 1, n, -1, f + 3, n);
        o.box(QUARTZ_PILLAR, 1, f + 1, n, 1, f + 3, n);
        o.put(QUARTZ_CHISELED, 0, f + 3, n);
        o.box(QUARTZ_CHISELED, -2, f + 4, n, 2, f + 4, n);
        o.box(GOLD, -2, f + 5, n, 2, f + 5, n);
        o.box(GOLD, -3, f + 1, n - 1, -3, f + 2, n - 1);                                    // braziers on either side of the gate
        o.box(GOLD, 3, f + 1, n - 1, 3, f + 2, n - 1);
        o.put(SOUL_LANTERN, "hanging=false", -3, f + 3, n - 1);
        o.put(SOUL_LANTERN, "hanging=false", 3, f + 3, n - 1);
        // the pedestals' plinths: a broad one for the central relic, a small one for each of the others
        o.box(GOLD, -2, f + 1, 24, 2, f + 1, 26);
        o.box(QUARTZ_CHISELED, -1, f + 1, 25, 1, f + 1, 25);
        for (int[] m : new int[][]{{-4, 21}, {4, 21}, {-4, 25}, {4, 25}}) {
            o.put(GOLD, m[0], f + 1, m[1]);
            o.box(QUARTZ_PILLAR, m[0] - 1, f, m[1] - 1, m[0] + 1, f, m[1] + 1);
        }
        // lights hang from the roof
        for (int[] l : new int[][]{{-4, 20}, {4, 20}, {0, 22}, {-4, 27}, {4, 27}, {0, 27}}) o.put(SOUL_LANTERN, "hanging=true", l[0], f + 5, l[1]);
        o.box(GOLD, -5, f + 1, 27, -5, f + 2, 27);                                           // a little gold heaped in the corners
        o.box(GOLD, 5, f + 1, 27, 5, f + 2, 27);
    }

    // ---------------------------------------------------------------- the porches (frame of their side)
    /**
     * A porch at the foot of a sealed gate: two columns and a lintel round a face of polished stone set into the wall, with lanterns under the
     * lintel. The passage itself (from z=28 through the wall to the door at z=32) is opened later, where a cave comes close.
     */
    private static void porch(NexusOps o, int f) {
        for (int sx = -1; sx <= 1; sx += 2) {
            int a = sx * 3, b = sx * 5;
            o.box(BRICKS, Math.min(a, b), f + 1, 24, Math.max(a, b), f + 7, 26);
            o.box(CHISELED, Math.min(a, b), f + 4, 24, Math.max(a, b), f + 4, 26);
            o.box(CHISELED, Math.min(a, b), f + 7, 24, Math.max(a, b), f + 7, 26);
            o.put(GOLD, sx * 3, f + 1, 24);
        }
        o.box(BRICKS, -5, f + 6, 24, 5, f + 6, 26);
        o.box(CHISELED, -5, f + 7, 24, 5, f + 7, 26);
        o.box(DIORITE, -2, f + 1, GATE_FIRST, 2, f + 5, GATE_FIRST);                         // the sealed face
        o.box(CHISELED, -1, f + 1, GATE_FIRST, 1, f + 4, GATE_FIRST);
        o.box(GOLD, -2, f + 5, GATE_FIRST, 2, f + 5, GATE_FIRST);
        o.put(LANTERN, "hanging=true", -2, f + 5, 25);
        o.put(LANTERN, "hanging=true", 2, f + 5, 25);
        o.box(ANDESITE, -7, f + 1, 22, -7, f + 2, 22);                                       // two braziers in front
        o.box(ANDESITE, 7, f + 1, 22, 7, f + 2, 22);
        o.put(LANTERN, "hanging=false", -7, f + 3, 22);
        o.put(LANTERN, "hanging=false", 7, f + 3, 22);
    }

    // ---------------------------------------------------------------- phase 3: moss, rubble, lights
    /** Is the cell (x, z) taken by something that nothing may be dropped on? */
    private static boolean busy(List<Pillar> pillars, Side treasury, int x, int z) {
        long d2 = (long) x * x + (long) z * z;
        if (d2 <= (long) (DISC + 3) * (DISC + 3) || d2 >= (long) (R - 1) * (R - 1)) return true;
        if (inTreasurySector(treasury, x, z) || inPorchSector(x, z) || nearRamp(x, z, 2)) return true;
        for (Pillar p : pillars) if (Math.abs(x - p.x()) <= 3 && Math.abs(z - p.z()) <= 3) return true;
        return false;
    }

    /** Moss on the floor in patches: blocks of moss with a carpet over most of them. */
    private static void moss(NexusOps o, int f, long seed, List<Pillar> pillars, Side treasury) {
        PitRng r = new PitRng(seed ^ 0x2545F4914F6CDD1DL);
        int made = 0;
        for (int attempt = 0; attempt < 400 && made < 26; attempt++) {
            double a = r.unit() * Math.PI * 2, d = 8 + r.unit() * 19;
            int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d), radius = r.range(1, 3);
            if (busy(pillars, treasury, x, z) || busy(pillars, treasury, x + radius, z) || busy(pillars, treasury, x - radius, z)
                    || busy(pillars, treasury, x, z + radius) || busy(pillars, treasury, x, z - radius)) continue;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (dx * dx + dz * dz > radius * radius + 1) continue;
                    if (r.chance(35)) continue;
                    o.put(MOSS, x + dx, f, z + dz);
                    if (r.chance(75)) o.put(MOSS_CARPET, x + dx, f + 1, z + dz);
                }
            }
            made++;
        }
    }

    /** Fallen blocks: small heaps of rubble on the floor between the pillars, and what fell from the top of every broken pillar round its foot. */
    private static void rubble(NexusOps o, int f, long seed, List<Pillar> pillars, Side treasury) {
        PitRng r = new PitRng(seed ^ 0x9E3779B1L);
        int made = 0;
        for (int attempt = 0; attempt < 600 && made < 22; attempt++) {
            double a = r.unit() * Math.PI * 2, d = 9 + r.unit() * 17;
            int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d), radius = r.range(1, 2);
            if (busy(pillars, treasury, x, z)) continue;
            heap(o, f, seed + made, x, z, radius, r.range(1, 3));
            made++;
        }
        for (Pillar p : pillars) {
            if (p.whole() || p.top() >= 11) continue;
            for (int k = 0; k < 5; k++) {
                int x = p.x() + r.range(-3, 3), z = p.z() + r.range(-3, 3);
                if (Math.abs(x - p.x()) <= 2 && Math.abs(z - p.z()) <= 2) continue;
                if ((long) x * x + (long) z * z >= (long) (R - 1) * (R - 1) || inTreasurySector(treasury, x, z) || inPorchSector(x, z) || nearRamp(x, z, 1)) continue;
                o.put(r.chance(50) ? CRACKED : COBBLE, x, f + 1, z);
                if (r.chance(35)) o.put(SLAB, "type=bottom", x, f + 2, z);
            }
        }
    }

    /** Lanterns on top of the pillars, on stone posts at four places round the seal, and soul lanterns standing on the first deck. */
    private static void lights(NexusOps o, int f, long seed, List<Pillar> pillars, Side treasury) {
        for (Pillar p : pillars) o.put(LANTERN, "hanging=false", p.x(), f + p.top() + (p.whole() ? 2 : 1), p.z());
        for (int k = 0; k < 4; k++) {
            double a = Math.PI * 2 * k / 4 + Math.PI / 4;
            int x = (int) Math.round(Math.cos(a) * 9), z = (int) Math.round(Math.sin(a) * 9);
            o.box(ANDESITE, x, f + 1, z, x, f + 3, z);
            o.put(SOUL_LANTERN, "hanging=false", x, f + 4, z);
        }
        for (int k = 0; k < 18; k++) {                                                      // only on a deck that is there, and where nothing else stands
            double a = Math.PI * 2 * k / 18 + 0.2;
            int x = (int) Math.round(Math.cos(a) * 26), z = (int) Math.round(Math.sin(a) * 26);
            long d2 = (long) x * x + (long) z * z;
            if (d2 < 24L * 24L || d2 > (long) R * R || !deckCell(seed, x, z, 1, treasury) || inPorchSector(x, z) || nearRamp(x, z, 1)) continue;
            o.put(SOUL_LANTERN, "hanging=false", x, f + TIER1 + 1, z);
        }
    }

    /** Chains from the dome with a lantern at the end: a crown over the seal, rings further out. */
    private static void chandeliers(NexusOps o, int f) {
        chandelier(o, f, 0, 0, 9, LANTERN);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8;
            chandelier(o, f, (int) Math.round(StrictMath.cos(a) * 12), (int) Math.round(StrictMath.sin(a) * 12), 7, SOUL_LANTERN);
            double b = a + Math.PI / 8;
            chandelier(o, f, (int) Math.round(StrictMath.cos(b) * 20), (int) Math.round(StrictMath.sin(b) * 20), 6, LANTERN);
        }
    }

    private static void chandelier(NexusOps o, int f, int x, int z, int length, String lantern) {
        int top = f + columnTop(x, z);
        o.box(CHAIN, "axis=y", x, top - length + 1, z, x, top, z);
        o.put(lantern, "hanging=true", x, top - length, z);
    }
}
