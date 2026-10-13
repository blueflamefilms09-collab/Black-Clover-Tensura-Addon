package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * The Underworld Gate, planned (docs/living_dungeon.md section 5, zone 2; the Spade Kingdom's dark, gravity-warped ruin). A pure,
 * deterministic layout with no Minecraft imports: the core cell goes in, a {@link ZonePlan} comes out, the same on every machine.
 *
 * <p>A round hall of obsidian, 44 blocks in radius and 34 high, sealed by a three-block shell so the flooded caves cannot pour in. In its
 * middle stands the Gate itself: a tower of blackstone 29 wide and 25 deep whose front is cut by a vast round arch, the vestibule, closed at
 * its end by a glowing violet portal of purple glass over pearlescent froglight with a black spade on it. A doorway one block wide and two
 * high stands at the foot of the portal, and a mana gate is set into it when the Nexus is populated; behind it, in the body of the tower,
 * lies the Treasury with the pedestals of the five relics. In front of the Gate a processional way runs between broken colonnades to the
 * shell. Round the tower the ruin is arranged against gravity: a skywalk of half-block steps spirals 1.6 turns from the floor up to a
 * balcony of the Gate; six islands of ruin hang in the air, each held up by a core of violet light and joined to the skywalk by a spur; five
 * inverted towers hang from the roof like stalactites; leaning pillars and broken arches stand on the floor. Five gravity zones
 * ({@link GravityCycle}) are marked in the floor. Four porches in the shell mark sealed gates which {@link UnderworldWays} opens to the caves.
 *
 * <p>Everything is described in a local frame (+v is the front of the Gate, u is across) turned by {@link #gateSide} so the Gate faces any
 * of the four directions. Ops are ordered in four phases: carve; foundation, shell and floor; structures; details.
 */
public final class UnderworldGatePlan {
    private UnderworldGatePlan() {}

    // ---------------------------------------------------------------- the size of the hall
    /** The radius of the hall's air, the thickness of its shell, and the radius of the whole (the plan's footprint). */
    public static final int R = 44, WALL = 3, OUTER = R + WALL;
    /** The layers of air above the floor, the roof's thickness and the foundation's. */
    public static final int HEIGHT = 34, ROOF = 3, FOUNDATION = 3;
    /** The hall's centre may sit this far from the middle of its core cell. */
    public static final int JITTER = 10;

    // ---------------------------------------------------------------- the Gate (heights are layers above the floor block; h = 1 is the first layer of air)
    /** The dais: half-width, back edge, front edge, top layer. The tower on it: half-width across, half-depth, top layer. */
    public static final int DAIS_U = 17, DAIS_BACK = -14, DAIS_FRONT = 13, DAIS_TOP = 3;
    public static final int TOWER_U = 14, TOWER_V = 12, TOWER_TOP = 28;
    /** The vestibule: half-width, where its air starts, the front face of the tower, the apex of the arch. */
    public static final int VEST_W = 7, VEST_BACK = 4, VEST_FRONT = 12, ARCH_TOP = 19;
    /** The wall of the portal (it holds the doorway): its back and front layers. */
    public static final int PORTAL_BACK = 2, PORTAL_FRONT = 3;
    /** The Treasury's air: half-width, back, front. */
    public static final int TREAS_U = 10, TREAS_BACK = -9, TREAS_FRONT = 1;

    // ---------------------------------------------------------------- the skywalk (a spiral of half-block steps)
    /** Its radius, the number of half-block levels it climbs, its half-width, the angles it starts and ends at (degrees counter-clockwise from +u, unwrapped). */
    public static final int SP_R = 29, SP_LEVELS = 36;
    public static final double SP_BAND = 1.6, SP_A0 = 128.0, SP_A1 = 720.0;
    /** The two gaps of the skywalk as ranges of its unwrapped angle: each about two blocks wide, jumpable. */
    public static final double[][] SP_GAPS = {{183.0, 188.0}, {350.0, 355.0}};

    // ---------------------------------------------------------------- the islands, hangers, pillars and arches
    /** Island: angle on the plan, unwrapped angle of the skywalk it is joined to, radius of its top, ruin variant, depth of its cone. */
    static final double[][] ISLES = {{35, 395, 5, 0, 4}, {145, 505, 5, 1, 7}, {215, 575, 5, 2, 7}, {250, 610, 4, 0, 7}, {290, 650, 4, 1, 7}, {325, 685, 5, 2, 7}};
    static final int ISLE_RING = 37;
    /** Inverted towers: angle, ring radius, radius at the roof, length. */
    static final int[][] HANGERS = {{0, 36, 5, 15}, {60, 36, 4, 13}, {120, 36, 4, 14}, {180, 36, 5, 16}, {270, 36, 5, 15}};
    /** Leaning pillars: angle, ring radius, height, lean (0 +u, 1 +v, 2 -u, 3 -v): all lean toward the Gate. */
    static final int[][] PILLARS = {{10, 38, 15, 2}, {62, 37, 12, 3}, {118, 37, 13, 3}, {172, 38, 16, 0}, {228, 37, 12, 0}, {312, 37, 14, 2}, {266, 39, 10, 1}};
    /** Ruined arches: angle, ring radius, which side is broken. */
    static final int[][] ARCHES = {{58, 33, 0}, {122, 33, 1}, {238, 34, 1}, {302, 34, 0}, {200, 33, 0}, {340, 33, 1}};
    /** The five gravity zones: u, v, radius, height of the top, kind (0 float, 1 well, 2 tide), phase in the round. */
    static final int[][] ZONES = {{0, 36, 7, 16, 0, 0}, {0, 22, 6, 14, 1, 310}, {-30, 2, 9, 26, 2, 90}, {30, -2, 9, 26, 2, 640}, {0, -31, 10, 28, 0, 450}};
    private static final GravityCycle.Kind[] KINDS = {GravityCycle.Kind.FLOAT, GravityCycle.Kind.WELL, GravityCycle.Kind.TIDE};
    /** Lamp posts round the Gate: u, v. */
    static final int[][] LAMPS = {{-20, 17}, {20, 17}, {-22, 0}, {22, 0}, {-17, -19}, {17, -19}, {0, -24}};

    // ---------------------------------------------------------------- blocks
    private static final String OBSIDIAN = "minecraft:obsidian", CRYING = "minecraft:crying_obsidian", GILDED = "minecraft:gilded_blackstone",
            BRICKS = "minecraft:polished_blackstone_bricks", CRACKED = "minecraft:cracked_polished_blackstone_bricks", POLISHED = "minecraft:polished_blackstone",
            BLACKSTONE = "minecraft:blackstone", CHISELED = "minecraft:chiseled_polished_blackstone", DEEPSLATE = "minecraft:deepslate",
            DEEP_BRICKS = "minecraft:deepslate_bricks", DEEP_CRACKED = "minecraft:cracked_deepslate_bricks", DEEP_TILES = "minecraft:deepslate_tiles",
            DEEP_POLISHED = "minecraft:polished_deepslate", DEEP_CHISELED = "minecraft:chiseled_deepslate", COBBLED_DEEP = "minecraft:cobbled_deepslate",
            AMETHYST = "minecraft:amethyst_block", GLASS = "minecraft:purple_stained_glass", FROGLIGHT = "minecraft:pearlescent_froglight",
            SOUL_LANTERN = "minecraft:soul_lantern", CHAIN = "minecraft:chain|axis=y", END_ROD_DOWN = "minecraft:end_rod|facing=down",
            END_ROD_UP = "minecraft:end_rod|facing=up", BONE = "minecraft:bone_block", GOLD = "minecraft:gold_block", CARPET = "minecraft:purple_carpet",
            SLAB = "minecraft:polished_blackstone_slab", STAIRS = "minecraft:polished_blackstone_brick_stairs", CLUSTER_DOWN = "minecraft:amethyst_cluster|facing=down",
            CLUSTER_UP = "minecraft:amethyst_cluster|facing=up", DEEP_STAIRS = "minecraft:deepslate_brick_stairs", BASALT = "minecraft:smooth_basalt",
            SKULL = "minecraft:skeleton_skull";

    /** The role of the one Devil that guards the Treasury's gate. */
    public static final String ROLE_GATEKEEPER = "gatekeeper";

    /**
     * The role of the floor boss, the Devil Avatar: it stands on the way in front of the Gate, at the foot of the dais steps (the steps end 16
     * blocks in front of the Gate's middle; the way, 11 blocks wide, begins at 17), so the Treasury behind the vestibule is reached past it.
     * Its fight is the whole front yard: the gravity zones of the way, the colonnade, 56 blocks of leash round this spot.
     */
    public static final String ROLE_AVATAR = "avatar";
    /** How far in front of the Gate's middle (local +v) the Avatar stands: two blocks past the foot of the steps. */
    public static final int AVATAR_V = 19;

    // ---------------------------------------------------------------- the four sides
    /**
     * A side seen as a local frame in which "outward" is +v and the ground to the right is +u: the same mapping NexusOps applies for
     * frame(0, 0, swap, su, sv), so a feature described once is drawn on any side. 0 north, 1 east, 2 south, 3 west.
     */
    record Side(int index, boolean swap, int su, int sv) {
        int wx(int lu, int lv) { return swap ? lv * sv : lu * su; }

        int wz(int lu, int lv) { return swap ? lu * su : lv * sv; }
    }

    static final Side[] SIDES = {new Side(0, false, -1, -1), new Side(1, true, 1, 1), new Side(2, false, 1, 1), new Side(3, true, 1, -1)};

    // ---------------------------------------------------------------- the few numbers that place the hall
    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0xA24BAED4963EE407L ^ cellZ * 0x9FB21C651E98DF25L ^ 0x2E1B21385C26C927L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 29;
        return h;
    }

    private static SplittableRandom rng(int cellX, int cellZ, long salt) { return new SplittableRandom(mix(cellX, cellZ) ^ salt * 0x9E3779B97F4A7C15L); }

    public static int centreX(int cellX, int cellZ) { return NexusZone.centre(cellX) + rng(cellX, cellZ, 1).nextInt(-JITTER, JITTER + 1); }

    public static int centreZ(int cellX, int cellZ) { return NexusZone.centre(cellZ) + rng(cellX, cellZ, 2).nextInt(-JITTER, JITTER + 1); }

    /** The y of the topmost floor block: -56 to -54, so the foundation does not reach the bedrock and the hall lies in the deepest tier. */
    public static int floorY(int cellX, int cellZ) { return -56 + rng(cellX, cellZ, 3).nextInt(3); }

    /** The side the Gate faces (0 north, 1 east, 2 south, 3 west): the gate in the shell on that side leads straight to it. */
    public static int gateSide(int cellX, int cellZ) { return rng(cellX, cellZ, 4).nextInt(4); }

    /** Which of {@code n} relics is the central one. */
    public static int centralRelicIndex(int cellX, int cellZ, int n) { return n <= 0 ? 0 : rng(cellX, cellZ, 5).nextInt(n); }

    private record Layout(int cx, int cz, int f, Side side, long seed) {
        int wx(int u, int v) { return cx + side.wx(u, v); }

        int wz(int u, int v) { return cz + side.wz(u, v); }

        ZonePlan.Spot spot(int u, int h, int v) { return new ZonePlan.Spot(wx(u, v), f + h, wz(u, v)); }
    }

    private static Layout layout(int cellX, int cellZ) {
        return new Layout(centreX(cellX, cellZ), centreZ(cellX, cellZ), floorY(cellX, cellZ), SIDES[gateSide(cellX, cellZ)], mix(cellX, cellZ));
    }

    // ---------------------------------------------------------------- the shapes
    /** The half-width of the arch of the vestibule at the layer h (VEST_BACK to ARCH_TOP): straight to 13, a round head above; -1 outside. */
    public static int archHalf(int h) {
        if (h < VEST_BACK || h > ARCH_TOP) return -1;
        if (h <= 13) return VEST_W;
        double dy = (h - 13.0) / 6.6;
        return (int) Math.max(0, Math.min(VEST_W, Math.round(7.4 * Math.sqrt(Math.max(0.0, 1.0 - dy * dy)))));
    }

    /**
     * A spade (the Spade Kingdom's mark) on a plane: x across, y up, the point at the top, centred on the origin; 2.3 wide and 2.9 high.
     * A heart turned over, with a stem. Pure and exact.
     */
    public static boolean spade(double x, double y) {
        double yy = -(y - 0.725) - 0.35;                                  // the heart's own y: turned over, and moved so the figure is centred
        double a = x * x + yy * yy - 1.0;
        boolean body = a * a * a - x * x * yy * yy * yy <= 0;
        double s = -y - 0.65;                                             // how far under the lobes the stem reaches
        boolean stem = s > 0 && s < 0.85 && Math.abs(x) <= 0.18 + 0.28 * Math.max(0.0, s - 0.55) / 0.3;
        return body || stem;
    }

    /** The angle in degrees [0, 360) of the offset (u, v) from the centre. */
    static double angle(int u, int v) {
        double a = Math.toDegrees(StrictMath.atan2(v, u));
        return a < 0 ? a + 360.0 : a;
    }

    /** The half-block level (1 to SP_LEVELS) the skywalk has at an unwrapped angle. */
    static int spiralLevel(double unwrapped) {
        return 1 + (int) Math.floor((SP_LEVELS - 1) * (unwrapped - SP_A0) / (SP_A1 - SP_A0));
    }

    private static boolean inGap(double unwrapped) {
        for (double[] g : SP_GAPS) if (unwrapped >= g[0] && unwrapped <= g[1]) return true;
        return false;
    }

    /** The layer of the block a half-block level stands in: level 2m is a top slab in layer m, level 2m+1 a bottom slab in layer m+1. */
    static int levelLayer(int level) { return level / 2; }

    /** One cell of the skywalk: offset (u, v), half-block level, unwrapped angle. */
    record SpCell(int u, int v, int level, double unwrapped) {}

    /** All cells of the skywalk (gaps left out). */
    static List<SpCell> spiral() {
        List<SpCell> out = new ArrayList<>();
        for (int u = -SP_R - 3; u <= SP_R + 3; u++) {
            for (int v = -SP_R - 3; v <= SP_R + 3; v++) {
                double r = Math.sqrt((double) u * u + (double) v * v);
                if (Math.abs(r - SP_R) > SP_BAND) continue;
                double a = angle(u, v);
                for (int k = 0; k <= 2; k++) {
                    double un = a + 360.0 * k;
                    if (un < SP_A0 || un > SP_A1 || inGap(un)) continue;
                    out.add(new SpCell(u, v, spiralLevel(un), un));
                }
            }
        }
        return out;
    }

    /** An island: centre offset, top block layer, radius, the skywalk's level where it is joined, angle on the plan, ruin variant, depth of the cone. */
    record Isle(int u, int v, int layer, int radius, int level, double angleDeg, int variant, int depth) {}

    static List<Isle> isles() {
        List<Isle> out = new ArrayList<>();
        for (double[] d : ISLES) {
            int level = spiralLevel(d[1]);
            if (level % 2 == 1) level--;                                  // an island's top is a whole block: the spur joins at the even level under it
            int u = (int) Math.round(ISLE_RING * StrictMath.cos(Math.toRadians(d[0]))), v = (int) Math.round(ISLE_RING * StrictMath.sin(Math.toRadians(d[0])));
            out.add(new Isle(u, v, levelLayer(level), (int) d[2], level, d[0], (int) d[3], (int) d[4]));
        }
        return out;
    }

    /** The radius of an inverted tower at `depth` layers under the roof. */
    static int hangerRadius(int rt, int len, int depth) { return Math.max(1, rt - depth * (rt - 1) / len); }

    // ---------------------------------------------------------------- the zones, gates and queries other code needs
    /** The five gravity zones of the hall, in world coordinates (see {@link GravityCycle}). */
    public static List<GravityCycle.Zone> zones(int cellX, int cellZ) {
        Layout l = layout(cellX, cellZ);
        List<GravityCycle.Zone> out = new ArrayList<>();
        for (int i = 0; i < ZONES.length; i++) {
            int[] z = ZONES[i];
            out.add(new GravityCycle.Zone(i, l.wx(z[0], z[1]), l.wz(z[0], z[1]), z[2], l.f() + 1, l.f() + z[3], KINDS[z[4]], z[5]));
        }
        return out;
    }

    /** A sealed gate in the shell, to be opened to a cave: the first wall cell to cut, the cell of the door, the way it faces outward. All in world coordinates. */
    public record Gate(int side, int firstX, int firstZ, int doorX, int doorZ, int dx, int dz) {}

    /** The four gates of the shell, one on each side. The passage runs from (firstX, firstZ) outward to the door cell. */
    public static List<Gate> gates(int cellX, int cellZ) {
        int cx = centreX(cellX, cellZ), cz = centreZ(cellX, cellZ);
        List<Gate> out = new ArrayList<>();
        for (Side s : SIDES) out.add(new Gate(s.index(), cx + s.wx(0, R + 2), cz + s.wz(0, R + 2), cx + s.wx(0, OUTER), cz + s.wz(0, OUTER), s.wx(0, 1), s.wz(0, 1)));
        return out;
    }

    /** The air of the vestibule, where the Gatekeeper stands guard: {minX, minY, minZ, maxX, maxY, maxZ}, inclusive, world coordinates. */
    public static int[] vestibuleBounds(int cellX, int cellZ) {
        Layout l = layout(cellX, cellZ);
        int a = l.wx(-VEST_W, VEST_BACK), b = l.wx(VEST_W, VEST_FRONT), c = l.wz(-VEST_W, VEST_BACK), d = l.wz(VEST_W, VEST_FRONT);
        return new int[]{Math.min(a, b), l.f() + DAIS_TOP + 1, Math.min(c, d), Math.max(a, b), l.f() + ARCH_TOP, Math.max(c, d)};
    }

    /** The air of the Treasury: {minX, minY, minZ, maxX, maxY, maxZ}, inclusive, world coordinates. */
    public static int[] treasuryBounds(int cellX, int cellZ) {
        Layout l = layout(cellX, cellZ);
        int a = l.wx(-TREAS_U, TREAS_BACK), b = l.wx(TREAS_U, TREAS_FRONT), c = l.wz(-TREAS_U, TREAS_BACK), d = l.wz(TREAS_U, TREAS_FRONT);
        return new int[]{Math.min(a, b), l.f() + DAIS_TOP + 1, Math.min(c, d), Math.max(a, b), l.f() + DAIS_TOP + 11, Math.max(c, d)};
    }

    /** How far into the ruin a point is, 0 at the shell and 1 at the Gate: what the Disciples' share of a Devil's power rises with. */
    public static double depth(int cellX, int cellZ, double x, double z) {
        double dx = x - centreX(cellX, cellZ), dz = z - centreZ(cellX, cellZ);
        double d = Math.sqrt(dx * dx + dz * dz);
        return Math.max(0.0, Math.min(1.0, (R - d) / (R - 20.0)));
    }

    // ---------------------------------------------------------------- building blocks of a plan
    /** Writes ops in the local frame of the Gate's side, with heights counted from the floor block. */
    private static final class Pen {
        final NexusOps o;
        final int f;
        final Side side;

        Pen(NexusOps o, int f, Side side) {
            this.o = o;
            this.f = f;
            this.side = side;
            local();
        }

        /** The frame of the Gate's side. */
        void local() { o.frame(0, 0, side.swap(), side.su(), side.sv()); }

        /** A frame for one of the four world sides (outward is +v), not turned by the Gate's. */
        void world(Side s) { o.frame(0, 0, s.swap(), s.su(), s.sv()); }

        /** A block spec is "block" or "block|props". */
        void box(String spec, int u1, int h1, int v1, int u2, int h2, int v2) {
            int bar = spec.indexOf('|');
            if (bar < 0) o.box(spec, "", u1, f + h1, v1, u2, f + h2, v2);
            else o.box(spec.substring(0, bar), spec.substring(bar + 1), u1, f + h1, v1, u2, f + h2, v2);
        }

        void put(String spec, int u, int h, int v) { box(spec, u, h, v, u, h, v); }

        void air(int u1, int h1, int v1, int u2, int h2, int v2) { o.air(u1, f + h1, v1, u2, f + h2, v2); }

        void fillAir(String spec, int u1, int h1, int v1, int u2, int h2, int v2) {
            int bar = spec.indexOf('|');
            if (bar < 0) o.fillAir(spec, "", u1, f + h1, v1, u2, f + h2, v2);
            else o.fillAir(spec.substring(0, bar), spec.substring(bar + 1), u1, f + h1, v1, u2, f + h2, v2);
        }

        void ring(String spec, int u1, int v1, int u2, int v2, int h1, int h2) {
            int bar = spec.indexOf('|');
            if (bar < 0) o.ring(spec, "", u1, v1, u2, v2, f + h1, f + h2);
            else o.ring(spec.substring(0, bar), spec.substring(bar + 1), u1, v1, u2, v2, f + h1, f + h2);
        }

        /** A filled disc of radius r round the local point (ou, ov), from layer h1 to h2. */
        void disc(String spec, int ou, int ov, int r, int h1, int h2) {
            int bar = spec.indexOf('|');
            String block = bar < 0 ? spec : spec.substring(0, bar), props = bar < 0 ? "" : spec.substring(bar + 1);
            o.frame(side.wx(ou, ov), side.wz(ou, ov), false, 1, 1);
            o.disc(block, props, r, r, f + h1, f + h2);
            local();
        }
    }

    /** What a cell of a painted layer is: a block spec, or null to leave it. */
    private interface Cells { String at(int u, int v); }

    /** Paints one layer h over a rectangle, joining runs of the same block along u into single boxes. */
    private static void paint(Pen p, int h, int u1, int v1, int u2, int v2, Cells c) {
        for (int v = v1; v <= v2; v++) {
            String cur = null;
            int start = 0;
            for (int u = u1; u <= u2 + 1; u++) {
                String b = u <= u2 ? c.at(u, v) : null;
                if (!Objects.equals(b, cur)) {
                    if (cur != null) p.box(cur, start, h, v, u - 1, h, v);
                    cur = b;
                    start = u;
                }
            }
        }
    }

    /** A disc on the floor that the ground obstacles keep clear of: local centre, radius. */
    record GroundObstacle(int u, int v, int radius) {
        boolean near(int ou, int ov, int margin) {
            int dx = ou - u, dz = ov - v, m = radius + margin;
            return dx * dx + dz * dz <= m * m;
        }
    }

    // ---------------------------------------------------------------- the plan
    public static ZonePlan of(int cellX, int cellZ) {
        Layout l = layout(cellX, cellZ);
        int f = l.f();
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.UNDERWORLD_GATE, cellX, cellZ, l.cx(), l.cz());
        b.floorY(f).topY(f + HEIGHT + ROOF).footprint(Footprint.Shape.OVAL, OUTER, OUTER);
        NexusOps o = b.ops();
        Pen p = new Pen(o, f, l.side());
        SplittableRandom r = rng(cellX, cellZ, 9);

        // phase 0: the air of the hall
        o.carveDisc(R, R, f + 1, f + HEIGHT);
        o.phase();

        // phase 1: foundation, shell, roof and floor
        shell(p);
        floor(p, l.seed());
        o.phase();

        // phase 2: structures
        List<GroundObstacle> obstacles = new ArrayList<>();
        dais(p);
        tower(p);
        decay(p, rng(cellX, cellZ, 10));
        vestibule(p);
        treasury(p);
        colonnade(p, r, obstacles);
        spiral(p);
        for (Isle isle : isles()) island(p, isle, r);
        for (int[] h : HANGERS) hanger(p, h);
        for (int[] pl : PILLARS) pillar(p, pl, obstacles);
        for (int[] a : ARCHES) ruinedArch(p, a, r, obstacles);
        for (Side s : SIDES) {
            p.world(s);
            porch(p);
        }
        p.local();
        o.phase();

        // phase 3: details
        road(p);
        glyphs(p);
        lamps(p, obstacles);
        List<int[]> posts = elitePosts(obstacles);
        for (int[] e : posts) obstacles.add(new GroundObstacle(e[0], e[1], 1));        // rubble keeps clear of where the Devils stand
        rubble(p, r, obstacles);
        roofHangings(p, r);
        debris(p, r, isles());
        shellStuds(p);

        // ---- where things are
        b.centralRelic(l.wx(0, -8), f + DAIS_TOP + 2, l.wz(0, -8));
        for (int[] m : minorPedestals()) b.minorRelic(l.wx(m[0], m[1]), f + DAIS_TOP + 1, l.wz(m[0], m[1]));
        b.gate(l.wx(0, PORTAL_FRONT), f + DAIS_TOP + 1, l.wz(0, PORTAL_FRONT));
        for (Side s : SIDES) b.entrance(l.cx() + s.wx(0, R), f + 1, l.cz() + s.wz(0, R));
        ZonePlan.Spot keeper = l.spot(0, DAIS_TOP + 1, 8);
        b.spawn(ROLE_GATEKEEPER, keeper.x(), keeper.y(), keeper.z());
        for (int[] e : posts) b.spawn(ZonePlan.ROLE_ELITE, l.wx(e[0], e[1]), f + 1, l.wz(e[0], e[1]));
        for (int[] m : minionSpots(obstacles, posts)) b.spawn(ZonePlan.ROLE_MINION, l.wx(m[0], m[1]), f + 1, l.wz(m[0], m[1]));
        b.spawn(ZonePlan.ROLE_ARRIVAL, l.wx(0, 41), f + 1, l.wz(0, 41));
        b.spawn(ROLE_AVATAR, l.wx(0, AVATAR_V), f + 1, l.wz(0, AVATAR_V));
        return b.build();
    }

    /** The pedestals of the four minor relics (local offsets). */
    static int[][] minorPedestals() { return new int[][]{{-8, -2}, {8, -2}, {-8, -6}, {8, -6}}; }

    /** How many Devils of lesser rank have a post. */
    public static final int POSTS = 7;

    /** Where the Devils of lesser rank may stand guard (local offsets), in order of preference: the colonnade, the flanks, the back. */
    static final int[][] POST_CANDIDATES = {{-13, 31}, {13, 31}, {-24, 8}, {24, 8}, {-22, -12}, {22, -12}, {0, -29}, {-24, -4}, {24, -4}, {-9, -30}, {9, -30}, {-13, 27}, {13, 27}};

    /** The posts of the Devils of lesser rank: the first POSTS candidates that are clear of the obstacles of the floor. */
    static List<int[]> elitePosts(List<GroundObstacle> obstacles) {
        List<int[]> out = new ArrayList<>();
        for (int[] c : POST_CANDIDATES) {
            if (out.size() >= POSTS) break;
            boolean clash = laneOrWay(c[0], c[1]) && Math.abs(c[0]) <= 12;
            for (GroundObstacle ob : obstacles) if (ob.near(c[0], c[1], 2)) clash = true;
            if (!clash) out.add(c);
        }
        return out;
    }

    // ---------------------------------------------------------------- phase 1
    private static void shell(Pen p) {
        NexusOps o = p.o;
        int f = p.f;
        o.disc(DEEPSLATE, "", OUTER, OUTER, f - FOUNDATION, f);
        o.discHollow(OBSIDIAN, "", OUTER, OUTER, WALL, f + 1, f + HEIGHT);
        o.disc(OBSIDIAN, "", OUTER, OUTER, f + HEIGHT + 1, f + HEIGHT + ROOF);
        // the face of the wall: a dark course, with bands of carved deepslate
        o.discHollow(DEEP_BRICKS, "", R + 1, R + 1, 1, f + 1, f + HEIGHT);
        for (int h : new int[]{1, 9, 17, 25, 33}) o.discHollow(DEEP_CHISELED, "", R + 1, R + 1, 1, f + h, f + h);
    }

    private static void shellStuds(Pen p) {
        for (int k = 0; k < 48; k++) {
            double a = Math.PI * 2 * k / 48;
            int u = (int) Math.round(StrictMath.cos(a) * (R + 0.8)), v = (int) Math.round(StrictMath.sin(a) * (R + 0.8));
            if (Math.abs(u) <= 3 || Math.abs(v) <= 3) continue;           // the porches keep their faces
            p.put(CRYING, u, 21, v);
        }
    }

    private static String floorBlock(long seed, int u, int v) {
        long h = (Math.floorDiv(u, 3) * 0x9E3779B97F4A7C15L) ^ (Math.floorDiv(v, 3) * 0xC2B2AE3D27D4EB4FL) ^ seed;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        int k = (int) Math.floorMod(h, 20L);
        if (k < 11) return DEEP_TILES;
        if (k < 14) return DEEP_POLISHED;
        if (k < 17) return DEEP_CRACKED;
        if (k < 19) return BLACKSTONE;
        return BASALT;
    }

    private static void floor(Pen p, long seed) {
        paint(p, 0, -R, -R, R, R, (u, v) -> (long) u * u + (long) v * v <= (long) R * R ? floorBlock(seed, u, v) : null);
    }

    // ---------------------------------------------------------------- phase 2: the Gate
    private static void dais(Pen p) {
        p.box(DEEP_BRICKS, -DAIS_U, 1, DAIS_BACK, DAIS_U, DAIS_TOP - 1, DAIS_FRONT);
        p.box(POLISHED, -DAIS_U, DAIS_TOP, DAIS_BACK, DAIS_U, DAIS_TOP, DAIS_FRONT);
        p.ring(CHISELED, -DAIS_U, DAIS_BACK, DAIS_U, DAIS_FRONT, DAIS_TOP, DAIS_TOP);
        p.ring(GILDED, -DAIS_U + 1, DAIS_BACK + 1, DAIS_U - 1, DAIS_FRONT - 1, DAIS_TOP, DAIS_TOP);
        // the steps to the front: three courses of stairs, nineteen wide, with the plain wall of the dais on either side
        for (int k = 0; k < 3; k++) {
            int v = DAIS_FRONT + 1 + k, h = DAIS_TOP - k;
            if (h - 1 >= 1) p.box(DEEP_BRICKS, -9, 1, v, 9, h - 1, v);
            p.box(STAIRS + "|facing=north,half=bottom", -9, h, v, 9, h, v);
            p.box(DEEP_BRICKS, -DAIS_U, 1, v, -10, h, v);
            p.box(DEEP_BRICKS, 10, 1, v, DAIS_U, h, v);
        }
    }

    private static void tower(Pen p) {
        // the body and its crown
        p.box(BRICKS, -TOWER_U, 4, -TOWER_V, TOWER_U, TOWER_TOP, TOWER_V);
        p.box(BRICKS, -13, TOWER_TOP + 1, -11, 13, TOWER_TOP + 2, 11);
        p.box(BRICKS, -12, TOWER_TOP + 3, -10, 12, TOWER_TOP + 3, 10);
        for (int su : new int[]{-1, 1}) {
            for (int sv : new int[]{-1, 1}) {                              // four turrets with a light on top
                int u = su * 11, v = sv * 9;
                p.box(BRICKS, u - 1, TOWER_TOP + 3, v - 1, u + 1, TOWER_TOP + 4, v + 1);
                p.box(CHISELED, u - 1, TOWER_TOP + 5, v - 1, u + 1, TOWER_TOP + 5, v + 1);
                p.put(END_ROD_UP, u, HEIGHT, v);
            }
        }
        // bands of carved stone round the body, two of gilded stone, and pilasters on the front
        for (int h : new int[]{8, 14, 20, 26}) p.ring(CHISELED, -TOWER_U, -TOWER_V, TOWER_U, TOWER_V, h, h);
        p.ring(GILDED, -TOWER_U, -TOWER_V, TOWER_U, TOWER_V, 17, 17);
        p.ring(GILDED, -TOWER_U, -TOWER_V, TOWER_U, TOWER_V, 23, 23);
        for (int u : new int[]{-12, -9, 9, 12}) p.box(CHISELED, u, 4, TOWER_V, u, TOWER_TOP - 1, TOWER_V);
        p.box(DEEP_BRICKS, -TOWER_U, TOWER_TOP, -TOWER_V - 1, TOWER_U, TOWER_TOP, TOWER_V + 1);       // the cornice
        // the crest above the arch: a spade of glowing stone
        for (int h = ARCH_TOP + 1; h <= TOWER_TOP - 1; h++) {
            for (int u = -6; u <= 6; u++) if (spade(u / 3.0, (h - 23.8) / 2.65)) p.put(CRYING, u, h, TOWER_V);
        }
    }

    /**
     * The Gate is a ruin too: cracked stones over its sides and back, and small dents where the outer layer has fallen away. The Treasury's walls
     * are at least three blocks thick, and a dent is one block deep, so nothing here reaches through.
     */
    private static void decay(Pen p, SplittableRandom r) {
        for (int n = 0; n < 90; n++) {
            int face = r.nextInt(3), h = 6 + r.nextInt(TOWER_TOP - 6);
            boolean dent = r.nextInt(4) == 0;
            int u = face == 2 ? r.nextInt(2 * TOWER_U - 1) - (TOWER_U - 1) : (face == 0 ? -TOWER_U : TOWER_U);
            int v = face == 2 ? -TOWER_V : r.nextInt(2 * TOWER_V - 1) - (TOWER_V - 1);
            if (dent) p.air(u, h, v, u, h, v);
            else p.put(CRACKED, u, h, v);
        }
    }

    private static boolean archAir(int u, int h) { return h >= VEST_BACK && h <= ARCH_TOP && Math.abs(u) <= archHalf(h); }

    private static void vestibule(Pen p) {
        // the barrel-vaulted hall: its air, from the portal wall out through the front face
        for (int h = VEST_BACK; h <= ARCH_TOP; h++) {
            int w = archHalf(h);
            p.air(-w, h, VEST_BACK, w, h, VEST_FRONT);
        }
        // the archivolt: every stone of the front face that touches the opening
        for (int h = VEST_BACK; h <= ARCH_TOP + 1; h++) {
            for (int u = -VEST_W - 1; u <= VEST_W + 1; u++) {
                if (archAir(u, h)) continue;
                boolean edge = false;
                for (int du = -1; du <= 1; du++) for (int dh = -1; dh <= 1; dh++) if (archAir(u + du, h + dh)) edge = true;
                if (edge) p.put(CHISELED, u, h, VEST_FRONT);
            }
        }
        // the wall at the end of the hall and the portal in it: glass in front, glowing froglight behind, a black spade on it
        for (int h = VEST_BACK; h <= ARCH_TOP; h++) {
            int w = archHalf(h);
            p.box(OBSIDIAN, -w, h, PORTAL_BACK, w, h, PORTAL_FRONT);
        }
        for (int h = 7; h <= ARCH_TOP - 2; h++) {
            int w = archHalf(h) - 1;
            if (w < 0) continue;
            p.box(FROGLIGHT, -w, h, PORTAL_BACK, w, h, PORTAL_BACK);
            for (int u = -w; u <= w; u++) p.put(spade(u / 3.5, (h - 12.6) / 3.5) ? OBSIDIAN : GLASS, u, h, PORTAL_FRONT);
        }
        // the doorway at its foot: one wide, two high, framed in gilded stone; the mana gate is set into it
        p.air(0, 4, PORTAL_BACK, 0, 5, PORTAL_FRONT);
        p.box(GILDED, -1, 4, PORTAL_FRONT, -1, 6, PORTAL_FRONT);
        p.box(GILDED, 1, 4, PORTAL_FRONT, 1, 6, PORTAL_FRONT);
        p.box(CHISELED, -1, 6, PORTAL_FRONT, 1, 6, PORTAL_FRONT);
        p.put(SOUL_LANTERN, -2, 4, PORTAL_FRONT + 1);
        p.put(SOUL_LANTERN, 2, 4, PORTAL_FRONT + 1);
        // the floor of the hall: a spade of black stone and glowing stone pointing at the portal, lines of gold either side
        paint(p, DAIS_TOP, -4, PORTAL_FRONT + 2, 4, VEST_FRONT - 1, (u, v) -> spade(u / 2.2, -(v - 8.0) / 2.2) ? ((Math.abs(u) + v) % 3 == 0 ? CRYING : OBSIDIAN) : null);
        p.box(GOLD, -5, DAIS_TOP, PORTAL_FRONT + 1, -5, DAIS_TOP, VEST_FRONT);
        p.box(GOLD, 5, DAIS_TOP, PORTAL_FRONT + 1, 5, DAIS_TOP, VEST_FRONT);
        // lanterns hung from the vault
        for (int v : new int[]{6, 9}) {
            for (int u : new int[]{-5, 5}) {
                int top = -1;
                for (int h = 14; h <= ARCH_TOP; h++) if (archHalf(h) < Math.abs(u)) { top = h; break; }
                if (top < 0) continue;
                p.box(CHAIN, u, top - 4, v, u, top - 1, v);
                p.put(SOUL_LANTERN + "|hanging=true", u, top - 5, v);
            }
        }
    }

    /** The highest layer of air of the Treasury over a column u. */
    private static int vaultTop(int u) {
        int a = Math.abs(u);
        return a <= 3 ? 14 : a <= 7 ? 12 : 10;
    }

    private static void treasury(Pen p) {
        // the air: a vaulted room of three heights
        p.air(-TREAS_U, 4, TREAS_BACK, TREAS_U, 10, TREAS_FRONT);
        p.air(-7, 11, TREAS_BACK, 7, 12, TREAS_FRONT);
        p.air(-3, 13, TREAS_BACK, 3, 14, TREAS_FRONT);
        // the floor: polished stone edged with gold, a carpet down the middle to the plinth
        p.box(POLISHED, -TREAS_U, DAIS_TOP, TREAS_BACK, TREAS_U, DAIS_TOP, TREAS_FRONT);
        p.ring(GOLD, -TREAS_U + 1, TREAS_BACK + 1, TREAS_U - 1, TREAS_FRONT - 1, DAIS_TOP, DAIS_TOP);
        p.box(CARPET, -1, 4, -5, 1, 4, TREAS_FRONT);
        // pedestal pads for the four minor relics, lanterns beside them, and the plinth of the central one
        for (int[] m : minorPedestals()) {
            p.box(GILDED, m[0] - 1, DAIS_TOP, m[1] - 1, m[0] + 1, DAIS_TOP, m[1] + 1);
            p.put(CRYING, m[0], DAIS_TOP, m[1]);
            int outward = m[0] < 0 ? -1 : 1;
            p.put(SOUL_LANTERN, m[0] + outward, 4, m[1] - 1);
            p.put(SOUL_LANTERN, m[0] + outward, 4, m[1] + 1);
        }
        p.box(GILDED, -2, 4, -9, 2, 4, -7);
        p.box(STAIRS + "|facing=north,half=bottom", -2, 4, -6, 2, 4, -6);
        for (int su : new int[]{-1, 1}) {
            p.put(SOUL_LANTERN, su * 2, 5, -9);
            p.put(SOUL_LANTERN, su * 2, 5, -7);
        }
        // a spade of glowing stone on the back wall over the plinth, lights in the roof and the walls, crystals in the corners
        for (int h = 6; h <= 14; h++) {
            for (int u = -5; u <= 5; u++) if (h <= vaultTop(u) && spade(u / 1.7, (h - 10.0) / 1.9)) p.put(CRYING, u, h, TREAS_BACK - 1);
        }
        for (int v : new int[]{-7, -3, 0}) {
            p.put(END_ROD_DOWN, -2, 14, v);
            p.put(END_ROD_DOWN, 2, 14, v);
        }
        for (int v : new int[]{-6, -1}) {
            p.put(CRYING, -TREAS_U - 1, 7, v);
            p.put(CRYING, TREAS_U + 1, 7, v);
        }
        for (int[] c : new int[][]{{-9, -8}, {9, -8}, {-9, 0}, {9, 0}}) p.put(CLUSTER_UP, c[0], 4, c[1]);
    }

    // ---------------------------------------------------------------- phase 2: the colonnade, the skywalk, the islands, the rest
    private static void colonnade(Pen p, SplittableRandom r, List<GroundObstacle> obstacles) {
        int[] rows = {20, 24, 35, 40};
        for (int su : new int[]{-1, 1}) {
            for (int v : rows) {
                int u = su * 10;
                int hc = 5 + r.nextInt(8);
                boolean broken = r.nextInt(3) == 0;
                p.box(CHISELED, u - 1, 1, v - 1, u + 1, 1, v + 1);
                p.box(BRICKS, u - 1, 2, v - 1, u + 1, hc, v + 1);
                p.ring(CHISELED, u - 1, v - 1, u + 1, v + 1, hc - 1, hc - 1);
                if (broken) {
                    p.box(CRACKED, u - 1, hc, v - 1, u + 1, hc, v + 1);
                    p.fillAir(DEEP_STAIRS + "|facing=north,half=bottom", u - 1, hc + 1, v, u + 1, hc + 1, v);
                } else {
                    p.box(CHISELED, u - 2, hc + 1, v - 2, u + 2, hc + 1, v + 2);
                    p.put(SOUL_LANTERN, u, hc + 2, v);
                }
                obstacles.add(new GroundObstacle(u, v, 2));
            }
        }
    }

    private static void spiral(Pen p) {
        TreeMap<Long, List<Integer>> rows = new TreeMap<>();                // (level, v) -> the u of its cells
        for (SpCell c : spiral()) {
            boolean stud = Math.sqrt((double) c.u() * c.u() + (double) c.v() * c.v()) > SP_R + 0.9 && Math.floorMod((int) Math.floor(c.unwrapped()), 12) == 0;
            if (stud) {                                                    // a glowing block set into the outer edge now and then
                p.put(CRYING, c.u(), c.level() % 2 == 0 ? levelLayer(c.level()) : levelLayer(c.level()) + 1, c.v());
                continue;
            }
            rows.computeIfAbsent(((long) c.level() << 32) | ((c.v() + 1000) & 0xFFFFFFFFL), k -> new ArrayList<>()).add(c.u());
        }
        for (var e : rows.entrySet()) {
            int level = (int) (e.getKey() >> 32), v = (int) (e.getKey() & 0xFFFFFFFFL) - 1000;
            List<Integer> us = e.getValue();
            Collections.sort(us);
            int start = us.get(0), prev = start;
            for (int i = 1; i <= us.size(); i++) {
                int u = i < us.size() ? us.get(i) : Integer.MAX_VALUE;
                if (u != prev + 1) {
                    steps(p, level, start, prev, v, v);
                    start = u;
                }
                prev = u;
            }
        }
        // the end of the skywalk is a flat bridge to a balcony on the east side of the Gate
        int top = SP_LEVELS, layer = levelLayer(top);
        steps(p, top, TOWER_U + 1, SP_R - 2, -1, 1);
        p.box(BRICKS, TOWER_U + 1, layer, -4, TOWER_U + 4, layer, 4);
        p.ring(CRYING, TOWER_U + 1, -4, TOWER_U + 4, 4, layer, layer);
        p.put(SOUL_LANTERN, TOWER_U + 3, layer + 1, -3);
        p.put(SOUL_LANTERN, TOWER_U + 3, layer + 1, 3);
        p.put(CRYING, TOWER_U + 4, layer + 1, 0);
    }

    /** A run of steps of one half-block level: an even level is a top slab in layer level/2, an odd one a bottom slab in the layer above that. */
    private static void steps(Pen p, int level, int u1, int u2, int v1, int v2) {
        if (level % 2 == 0) p.box(SLAB + "|type=top", u1, levelLayer(level), v1, u2, levelLayer(level), v2);
        else p.box(SLAB + "|type=bottom", u1, levelLayer(level) + 1, v1, u2, levelLayer(level) + 1, v2);
    }

    private static void island(Pen p, Isle isle, SplittableRandom r) {
        int u0 = isle.u(), v0 = isle.v(), top = isle.layer(), ri = isle.radius(), depth = isle.depth();
        // the body: a cone of rock under a top of ruin stone
        p.disc(BRICKS, u0, v0, ri, top, top);
        p.disc(DEEPSLATE, u0, v0, ri, top - 1, top - 1);
        for (int d = 2; d <= depth; d++) {
            int rr = Math.max(1, ri - (d - 1) * (ri - 1) / (depth - 1));
            p.disc(d % 3 == 0 ? COBBLED_DEEP : DEEPSLATE, u0, v0, rr, top - d, top - d);
        }
        // the violet core that holds it up
        int tip = top - depth - 1;
        p.put(CRYING, u0, tip, v0);
        p.put(FROGLIGHT, u0, tip - 1, v0);
        p.put(CLUSTER_DOWN, u0, tip - 2, v0);
        // worn patches and a ring of carved stone on the top
        p.disc(CRACKED, u0 + 1, v0 - 1, Math.max(1, ri - 3), top, top);
        p.disc(DEEP_CHISELED, u0, v0, Math.max(1, ri - 4), top, top);
        // a ruin on it
        switch (isle.variant()) {
            case 0 -> {                                                    // four broken columns round an altar
                int[] hs = {3, 4, 2, 4};
                int k = 0;
                for (int su : new int[]{-1, 1}) for (int sv : new int[]{-1, 1}) p.box(BRICKS, u0 + su * 2, top + 1, v0 + sv * 2, u0 + su * 2, top + hs[k++], v0 + sv * 2);
                p.put(CRYING, u0, top + 1, v0);
                p.put(SOUL_LANTERN, u0, top + 2, v0);
            }
            case 1 -> {                                                    // a freestanding arch
                p.box(BRICKS, u0 - 2, top + 1, v0, u0 - 2, top + 4, v0);
                p.box(BRICKS, u0 + 2, top + 1, v0, u0 + 2, top + 4, v0);
                p.box(CHISELED, u0 - 2, top + 5, v0, u0 + 1, top + 5, v0);
                p.put(CRYING, u0, top + 1, v0);
                p.put(SOUL_LANTERN + "|hanging=true", u0, top + 4, v0);
            }
            default -> {                                                   // a pair of obelisks and a glowing crystal
                p.box(DEEP_BRICKS, u0 - 2, top + 1, v0 - 1, u0 - 2, top + 5, v0 - 1);
                p.box(DEEP_BRICKS, u0 + 2, top + 1, v0 + 1, u0 + 2, top + 4, v0 + 1);
                p.put(CRYING, u0 - 2, top + 6, v0 - 1);
                p.put(AMETHYST, u0, top + 1, v0);
                p.put(CLUSTER_UP, u0, top + 2, v0);
            }
        }
        if (r.nextInt(2) == 0) p.put(BONE, u0 + 1, top + 1, v0 + 2);
        // the spur that joins it to the skywalk: slabs at the island's own level along the line from the skywalk to the island
        double ang = Math.toRadians(isle.angleDeg());
        double ca = StrictMath.cos(ang), sa = StrictMath.sin(ang);
        for (double t = SP_R + SP_BAND - 0.5; t <= ISLE_RING - ri + 1; t += 0.5) {
            for (int s = -1; s <= 1; s++) {
                int u = (int) Math.round(t * ca - s * sa), v = (int) Math.round(t * sa + s * ca);
                p.box(SLAB + "|type=top", u, levelLayer(isle.level()), v, u, levelLayer(isle.level()), v);
            }
        }
    }

    private static void hanger(Pen p, int[] h) {
        int u0 = (int) Math.round(h[1] * StrictMath.cos(Math.toRadians(h[0]))), v0 = (int) Math.round(h[1] * StrictMath.sin(Math.toRadians(h[0])));
        int rt = h[2], len = h[3], top = HEIGHT;                            // the top layer of air; the roof is above it
        int bottom = top - len + 1;
        int d = 0;
        while (d < len) {
            int rr = hangerRadius(rt, len, d), span = 1;
            while (d + span < len && hangerRadius(rt, len, d + span) == rr) span++;
            p.disc((d / 4) % 2 == 0 ? BRICKS : DEEP_BRICKS, u0, v0, rr, top - d - span + 1, top - d);
            d += span;
        }
        for (int k = 0; k * 5 < len; k++) {                                // carved bands and glowing windows
            int hh = top - 2 - k * 5;
            if (hh <= bottom + 1) break;
            p.disc(CHISELED, u0, v0, hangerRadius(rt, len, top - hh), hh, hh);
            int rr = hangerRadius(rt, len, top - hh + 1);
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) p.put(CRYING, u0 + dir[0] * rr, hh - 1, v0 + dir[1] * rr);
        }
        p.put(CRYING, u0, bottom - 1, v0);
        p.put(FROGLIGHT, u0, bottom - 2, v0);
        p.put(CLUSTER_DOWN, u0, bottom - 3, v0);
    }

    private static void pillar(Pen p, int[] a, List<GroundObstacle> obstacles) {
        int u0 = (int) Math.round(a[1] * StrictMath.cos(Math.toRadians(a[0]))), v0 = (int) Math.round(a[1] * StrictMath.sin(Math.toRadians(a[0])));
        int hp = a[2];
        int[][] dir = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        int du = dir[a[3]][0], dv = dir[a[3]][1];
        int seg = 0;
        for (int h = 1; h <= hp; h += 4) {
            int top = Math.min(hp, h + 3);
            int u = u0 + du * seg, v = v0 + dv * seg;
            p.box(seg % 2 == 0 ? BRICKS : DEEP_BRICKS, u, h, v, u + 1, top, v + 1);
            p.box(CHISELED, u, top, v, u + 1, top, v + 1);
            seg++;
        }
        int u = u0 + du * (seg - 1), v = v0 + dv * (seg - 1);
        p.put(CRYING, u, hp + 1, v);
        p.put(CRYING, u + 1, hp, v + 1);
        obstacles.add(new GroundObstacle(u0 + du * 2, v0 + dv * 2, 4));
    }

    private static void ruinedArch(Pen p, int[] a, SplittableRandom r, List<GroundObstacle> obstacles) {
        int u0 = (int) Math.round(a[1] * StrictMath.cos(Math.toRadians(a[0]))), v0 = (int) Math.round(a[1] * StrictMath.sin(Math.toRadians(a[0])));
        // the arch spans the tangent of the ring, so that the path along the ring walks through it
        double ang = Math.toRadians(a[0] + 90);
        boolean alongU = Math.abs(StrictMath.cos(ang)) >= Math.abs(StrictMath.sin(ang));
        int hLeft = 8, hRight = 8;
        if (a[2] == 0) hRight = 3 + r.nextInt(3);
        else hLeft = 3 + r.nextInt(3);
        for (int side = -1; side <= 1; side += 2) {
            int hh = side < 0 ? hLeft : hRight;
            int pu = alongU ? u0 + side * 3 : u0, pv = alongU ? v0 : v0 + side * 3;
            p.box(CHISELED, pu - 1, 1, pv - 1, pu + 1, 1, pv + 1);
            p.box(BRICKS, pu - 1, 2, pv - 1, pu + 1, hh, pv + 1);
            if (hh == 8) p.box(CHISELED, pu - 1, hh + 1, pv - 1, pu + 1, hh + 1, pv + 1);
        }
        if (hLeft == 8 && hRight == 8) {
            if (alongU) p.box(CHISELED, u0 - 3, 9, v0 - 1, u0 + 3, 9, v0 + 1);
            else p.box(CHISELED, u0 - 1, 9, v0 - 3, u0 + 1, 9, v0 + 3);
        } else {
            int s = hLeft == 8 ? -1 : 1;                                   // the intact side keeps a stub of the lintel
            if (alongU) p.box(CHISELED, u0 + s * 3 - 1, 9, v0 - 1, u0 + s, 9, v0 + 1);
            else p.box(CHISELED, u0 - 1, 9, v0 + s * 3 - 1, u0 + 1, 9, v0 + s);
        }
        int low = Math.min(hLeft, hRight);
        p.put(CRYING, alongU ? u0 + 3 : u0, low + 1, alongU ? v0 : v0 + 3);
        obstacles.add(new GroundObstacle(u0, v0, 5));
    }

    /** The porch of a sealed gate in the shell, drawn in the frame of one world side: a niche, two columns and a lintel, a glowing seal. */
    private static void porch(Pen p) {
        int t = R + 1;
        p.air(-1, 1, t, 1, 4, t);
        p.box(CRYING, -1, 1, t + 1, 1, 4, t + 1);                          // the seal; the way cuts it where a cave is near
        p.box(DEEP_CHISELED, -2, 1, t, -2, 6, t);
        p.box(DEEP_CHISELED, 2, 1, t, 2, 6, t);
        p.box(GILDED, -1, 5, t, 1, 5, t);
        p.box(DEEP_CHISELED, -2, 6, t, 2, 6, t);
        p.put(DEEP_CHISELED, -2, 1, t - 1);
        p.put(DEEP_CHISELED, 2, 1, t - 1);
        p.put(SOUL_LANTERN, -2, 2, t - 1);
        p.put(SOUL_LANTERN, 2, 2, t - 1);
        p.put(CRYING, 0, 6, t);
    }

    // ---------------------------------------------------------------- phase 3: details
    private static void road(Pen p) {
        // the processional way from the steps to the shell
        paint(p, 0, -5, DAIS_FRONT + 4, 5, R, (u, v) -> {
            if (Math.abs(u) == 5) return Math.floorMod(v, 4) == 0 ? CRYING : OBSIDIAN;
            return ((v / 2) % 2 == 0) ? POLISHED : DEEP_POLISHED;
        });
    }

    private static void glyphs(Pen p) {
        for (int[] g : ZONES) {
            int ru = g[0], rv = g[1], rad = g[2];
            paint(p, 0, ru - rad, rv - rad, ru + rad, rv + rad, (u, v) -> {
                double d = Math.sqrt((double) (u - ru) * (u - ru) + (double) (v - rv) * (v - rv));
                if ((long) u * u + (long) v * v > (long) (R - 1) * (R - 1)) return null;
                if (Math.abs(d - rad) < 0.7) return OBSIDIAN;
                if (Math.abs(d - rad * 0.5) < 0.6) return AMETHYST;
                if (d < 1.5) return CRYING;
                return null;
            });
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * k / 4;
                int u = ru + (int) Math.round(StrictMath.cos(a) * (rad - 1.0)), v = rv + (int) Math.round(StrictMath.sin(a) * (rad - 1.0));
                if ((long) u * u + (long) v * v <= (long) (R - 1) * (R - 1)) p.put(CRYING, u, 0, v);
            }
        }
    }

    private static void lamps(Pen p, List<GroundObstacle> obstacles) {
        for (int[] l : LAMPS) {
            p.box(DEEP_BRICKS, l[0], 1, l[1], l[0], 3, l[1]);
            p.put(CRYING, l[0], 4, l[1]);
            p.put(SOUL_LANTERN, l[0], 5, l[1]);
            obstacles.add(new GroundObstacle(l[0], l[1], 1));
        }
    }

    private static boolean laneOrWay(int u, int v) {
        if (Math.abs(u) <= 6 && v >= 17) return true;                       // the way to the Gate
        if (Math.abs(u) <= 19 && Math.abs(v) <= 17) return true;           // the Gate
        return (Math.abs(u) <= 4 || Math.abs(v) <= 4) && StrictMath.hypot(u, v) > 28;     // the lanes to the gates of the shell
    }

    private static void rubble(Pen p, SplittableRandom r, List<GroundObstacle> obstacles) {
        for (int n = 0; n < 34; n++) {
            double a = Math.toRadians(r.nextInt(360)), rad = 22 + r.nextInt(20);
            int u = (int) Math.round(StrictMath.cos(a) * rad), v = (int) Math.round(StrictMath.sin(a) * rad);
            int kind = r.nextInt(4);
            if (laneOrWay(u, v) || (long) u * u + (long) v * v > (long) (R - 3) * (R - 3)) continue;
            boolean clash = false;
            for (GroundObstacle ob : obstacles) if (ob.near(u, v, 1)) clash = true;
            if (clash) continue;
            switch (kind) {
                case 0 -> {
                    p.fillAir(COBBLED_DEEP, u - 1, 1, v - 1, u + 1, 1, v + 1);
                    p.fillAir(COBBLED_DEEP, u, 2, v, u, 2, v);
                }
                case 1 -> {
                    p.fillAir(DEEP_CRACKED, u, 1, v, u + 1, 1, v);
                    p.fillAir(BONE, u + 1, 2, v, u + 1, 2, v);
                }
                case 2 -> {
                    p.fillAir(BLACKSTONE, u - 1, 1, v, u + 1, 1, v + 1);
                    p.fillAir(SKULL, u, 2, v, u, 2, v);
                }
                default -> {
                    p.fillAir(CRACKED, u, 1, v, u, 2, v);
                    p.fillAir(CLUSTER_UP, u + 1, 1, v, u + 1, 1, v);
                }
            }
            obstacles.add(new GroundObstacle(u, v, 2));
        }
    }

    private static void roofHangings(Pen p, SplittableRandom r) {
        for (int n = 0; n < 40; n++) {
            double a = Math.toRadians(r.nextInt(360)), rad = 4 + r.nextInt(R - 8);
            int u = (int) Math.round(StrictMath.cos(a) * rad), v = (int) Math.round(StrictMath.sin(a) * rad);
            int len = 2 + r.nextInt(5);
            boolean lantern = n % 4 == 0;
            if (Math.abs(u) <= 16 && Math.abs(v) <= 14) continue;          // the crown of the Gate is under here
            p.fillAir(BLACKSTONE, u - 1, HEIGHT, v - 1, u + 1, HEIGHT, v + 1);
            p.fillAir(DEEPSLATE, u, HEIGHT - len, v, u, HEIGHT - 1, v);
            if (lantern) {
                p.fillAir(CHAIN, u + 2, HEIGHT - 3, v, u + 2, HEIGHT - 1, v);
                p.fillAir(SOUL_LANTERN + "|hanging=true", u + 2, HEIGHT - 4, v, u + 2, HEIGHT - 4, v);
            }
        }
    }

    /** Small fragments of the ruin hanging in the air where the gravity let go of them, some with a speck of violet light under them. Only air is filled, so nothing is overwritten. */
    private static void debris(Pen p, SplittableRandom r, List<Isle> isles) {
        for (int n = 0; n < 40; n++) {
            double a = Math.toRadians(r.nextInt(360)), rad = r.nextBoolean() ? 15 + r.nextInt(11) : 34 + r.nextInt(7);
            int u = (int) Math.round(StrictMath.cos(a) * rad), v = (int) Math.round(StrictMath.sin(a) * rad), h = 7 + r.nextInt(24);
            int shape = r.nextInt(3), kind = r.nextInt(4);
            boolean speck = r.nextInt(3) == 0;
            if ((Math.abs(u) <= 19 && Math.abs(v) <= 17) || (Math.abs(u) <= 6 && v >= 17)) continue;     // not in the yard of the Gate, nor over the way to it
            boolean nearIsle = false;
            for (Isle i : isles) if (StrictMath.hypot(u - i.u(), v - i.v()) < i.radius() + 7) nearIsle = true;
            if (nearIsle) continue;
            String block = kind == 0 ? BLACKSTONE : kind == 1 ? CRACKED : kind == 2 ? DEEPSLATE : DEEP_CRACKED;
            switch (shape) {
                case 0 -> p.fillAir(block, u, h, v, u, h, v);
                case 1 -> p.fillAir(block, u, h, v, u + 1, h, v + 1);
                default -> {
                    p.fillAir(block, u - 1, h, v, u + 1, h, v);
                    p.fillAir(block, u, h, v - 1, u, h, v + 1);
                }
            }
            if (speck) p.fillAir(CRYING, u, h - 1, v, u, h - 1, v);
        }
    }

    // ---------------------------------------------------------------- where the creatures go
    /** The spots on the floor where Disciples may appear (local offsets): rings at three radii, clear of the obstacles, the way, the Gate and the lanes to the shell. */
    static List<int[]> minionSpots(List<GroundObstacle> obstacles, List<int[]> posts) {
        List<int[]> out = new ArrayList<>();
        for (int ring = 0; ring < 3; ring++) {
            int rad = ring == 0 ? 33 : ring == 1 ? 40 : 24;
            int step = ring == 2 ? 30 : 20;
            for (int deg = ring * 7; deg < 360; deg += step) {
                int u = (int) Math.round(StrictMath.cos(Math.toRadians(deg)) * rad), v = (int) Math.round(StrictMath.sin(Math.toRadians(deg)) * rad);
                boolean clash = laneOrWay(u, v);
                for (GroundObstacle ob : obstacles) if (ob.near(u, v, 2)) clash = true;
                for (int[] e : posts) if (Math.abs(e[0] - u) <= 3 && Math.abs(e[1] - v) <= 3) clash = true;
                if (!clash) out.add(new int[]{u, v});
            }
        }
        return out;
    }
}
