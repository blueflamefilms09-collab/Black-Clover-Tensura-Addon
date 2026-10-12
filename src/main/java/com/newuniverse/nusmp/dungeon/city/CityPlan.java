package com.newuniverse.nusmp.dungeon.city;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Golden City, planned (docs/living_dungeon.md section 7). A pure, deterministic layout with no Minecraft imports: the cell of the map
 * (a 128-block square, see DungeonTier.CITY_CELL) goes in, an ordered list of placement ops comes out, written in abstract palette entries
 * that only {@link CityBuilder} maps to blocks. The same cell always gives the same city.
 *
 * <p>The district is an oval of 72 to 96 blocks across, sealed in a black vault: a carved cavern with a flat roof and a two-block shell, so
 * the flooded caves round it cannot pour in. Inside the shell stands a broken outer wall with two gates, then four avenues running to a
 * round plaza with a moat (the magicule reservoir) and a dais with the Aurelian Altar, with towers, halls, shrines and arches between the
 * avenues, some of them ruined. Ops are ordered: carve, then floor and shell, then walls and buildings, then details.
 */
public final class CityPlan {
    /** The width of a map cell (DungeonTier.CITY_CELL). */
    public static final int CELL = 128;
    /** The radius of the plaza. */
    public static final int PLAZA = 11;
    /** The roof of the vault, and the topmost layer of air under it. */
    public static final int CAP_Y = -31, CEILING_Y = -32;
    /** The highest block a building may use. */
    public static final int TOP_LIMIT = -34;

    public static final int PHASE_CARVE = 0, PHASE_FLOOR = 1, PHASE_BUILD = 2, PHASE_DETAIL = 3;

    /** Variants: a facing, and one flag that flips the block (upside-down stairs, top slab, hanging lantern, downward cluster). */
    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, FLIP = 4;

    /** The abstract palette; the glue maps each to a block (and its variant to a block state). */
    public enum Pal {
        AIR, GOLD, GILDED, BRICKS, CRACKED, SMOOTH, CHISELED, BLACKSTONE, STAIRS, SLAB, AMETHYST, CLUSTER, SEA_LANTERN, LANTERN, SOUL_LANTERN,
        CHAIN, CANDLE, WATER, END_ROD, CARPET_GOLD, CARPET_PURPLE, GLASS, CRYING, ALTAR
    }

    /** CARVE clears everything in its box, BOX fills it with one block, BLOCK is a box of one. */
    public enum Kind { CARVE, BOX, BLOCK }

    /** One placement: a box (inclusive corners, x1 <= x2 and so on) of one palette entry. */
    public record Op(Kind kind, Pal pal, int variant, int x1, int y1, int z1, int x2, int y2, int z2) {
        public int sx() { return x2 - x1 + 1; }

        public int sy() { return y2 - y1 + 1; }

        public int sz() { return z2 - z1 + 1; }

        public long volume() { return (long) sx() * sy() * sz(); }
    }

    /** A building of the city, by the world position of its middle. */
    public record Building(String type, int x, int z, boolean ruined) {}

    /** Candle count in a variant (1 to 4). */
    public static int candles(int n) { return (n - 1) << 3; }

    // ---------------------------------------------------------------- what the plan holds
    public final int cellX, cellZ;
    /** The middle of the district, and its half-widths along x and z (36 to 48). */
    public final int centreX, centreZ, semiX, semiZ;
    /** The y of the walking surface; a player stands one above. */
    public final int floorY;
    /** 0: the gates are at the east and west ends; 1: at the north and south ends. */
    public final int gateAxis;
    public final List<Op> ops;
    private final int[] phaseStart;
    public final int altarX, altarY, altarZ;
    /** Where the Gilded Sentinels stand (the block their feet are in). */
    public final List<int[]> sentinels;
    public final List<Building> buildings;
    private final long seed;

    // ---------------------------------------------------------------- the shape of the district
    /** Is the offset (u, v) from the centre inside the oval with half-widths A and B? Integer arithmetic only. */
    public static boolean inside(int u, int v, int A, int B) {
        return (long) u * u * B * B + (long) v * v * A * A <= (long) A * A * B * B;
    }

    /** The largest u with (u, v) inside the oval of half-widths A and B; -1 if the row v is outside it. */
    public static int extent(int v, int A, int B) {
        long rhs = (long) A * A * ((long) B * B - (long) v * v);
        if (rhs < 0) return -1;
        long t = rhs / ((long) B * B);
        int u = (int) Math.sqrt((double) t);
        while ((long) u * u > t) u--;
        while ((long) (u + 1) * (u + 1) <= t) u++;
        return u;
    }

    private static long mix(int cellX, int cellZ) {
        long h = cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ 0x6A09E667F3BCC909L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return h;
    }

    /** a, b, jitter x, jitter z, floor y, gate axis: the few numbers that place the district, cheap to get without planning the city. */
    private static int[] params(int cellX, int cellZ) {
        CityRng r = new CityRng(mix(cellX, cellZ));
        int a = r.range(36, 48), b = r.range(36, 48);
        int jx = r.range(-10, 10), jz = r.range(-10, 10);
        int fy = r.range(-54, -52);
        int axis = r.below(2);
        return new int[]{a, b, jx, jz, fy, axis};
    }

    public static int centreX(int cellX, int cellZ) { return cellX * CELL + CELL / 2 + params(cellX, cellZ)[2]; }

    public static int centreZ(int cellX, int cellZ) { return cellZ * CELL + CELL / 2 + params(cellX, cellZ)[3]; }

    public static int floorY(int cellX, int cellZ) { return params(cellX, cellZ)[4]; }

    /** The nearest city cell to a world position, searching the cells within `rings` of its own; null if there is none. */
    public static int[] nearest(int x, int z, int rings, CellTest isCity) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (int dx = -rings; dx <= rings; dx++) {
            for (int dz = -rings; dz <= rings; dz++) {
                if (!isCity.isCity(cx + dx, cz + dz)) continue;
                long ddx = centreX(cx + dx, cz + dz) - x, ddz = centreZ(cx + dx, cz + dz) - z;
                long d = ddx * ddx + ddz * ddz;
                if (d < bestD) {
                    bestD = d;
                    best = new int[]{cx + dx, cz + dz};
                }
            }
        }
        return best;
    }

    /** Is this 128-block cell one of the Golden City's? (DungeonTier.isCityCell in the game.) */
    public interface CellTest { boolean isCity(int cellX, int cellZ); }

    // ---------------------------------------------------------------- the plan
    public static CityPlan of(int cellX, int cellZ) { return new CityPlan(cellX, cellZ); }

    private CityPlan(int cellX, int cellZ) {
        this.cellX = cellX;
        this.cellZ = cellZ;
        int[] p = params(cellX, cellZ);
        semiX = p[0];
        semiZ = p[1];
        centreX = cellX * CELL + CELL / 2 + p[2];
        centreZ = cellZ * CELL + CELL / 2 + p[3];
        floorY = p[4];
        gateAxis = p[5];
        seed = mix(cellX, cellZ);
        altarX = centreX;
        altarY = floorY + 3;
        altarZ = centreZ;
        CityRng r = new CityRng(seed ^ 0x5DEECE66DL);
        CityOps o = new CityOps(centreX, centreZ);
        int[] starts = new int[5];

        starts[PHASE_CARVE] = o.size();
        carve(o);

        starts[PHASE_FLOOR] = o.size();
        floor(o);

        starts[PHASE_BUILD] = o.size();
        List<Lot> lots = placeLots(r);
        plaza(o);
        wall(o);
        gates(o);
        gateReliefs(o);
        List<Building> built = new ArrayList<>();
        boolean[] ruin = decideRuins(r, lots);
        for (int i = 0; i < lots.size(); i++) {
            Lot l = lots.get(i);
            o.frame(l.cu, l.cv, l.swap, l.su, l.sv);
            switch (l.type) {
                case Lot.TOWER -> CityBuildings.tower(o, r, floorY, l.p1, towerHeight(r), ruin[i]);
                case Lot.HALL -> CityBuildings.hall(o, r, floorY, l.p1, l.p2, ruin[i]);
                case Lot.SHRINE -> CityBuildings.shrine(o, r, floorY, l.p1, ruin[i]);
                case Lot.OBELISK -> CityBuildings.obelisk(o, r, floorY, ruin[i]);
                default -> CityBuildings.arch(o, r, floorY, ruin[i]);
            }
            o.resetFrame();
            built.add(new Building(Lot.NAMES[l.type], centreX + l.cu, centreZ + l.cv, ruin[i]));
        }

        starts[PHASE_DETAIL] = o.size();
        details(o, lots);
        starts[4] = o.size();

        List<int[]> guards = new ArrayList<>();
        int[][] spots = {{4, 4}, {-4, -4}, {4, -4}, {-4, 4}};
        int n = 2 + r.below(3);
        for (int i = 0; i < n; i++) guards.add(new int[]{centreX + spots[i][0], floorY + 1, centreZ + spots[i][1]});

        this.phaseStart = starts;
        this.ops = Collections.unmodifiableList(o.list());
        this.sentinels = Collections.unmodifiableList(guards);
        this.buildings = Collections.unmodifiableList(built);
    }

    private int towerHeight(CityRng r) {
        int hmax = -36 - floorY;                                   // the platform at floorY + h, merlons above it, a lantern above those
        return r.range(12, hmax);
    }

    // ---------------------------------------------------------------- queries for the glue
    public int phaseStart(int phase) { return phaseStart[phase]; }

    /** Which phase an op index belongs to. */
    public int phaseOf(int opIndex) {
        for (int ph = PHASE_DETAIL; ph > PHASE_CARVE; ph--) if (opIndex >= phaseStart[ph]) return ph;
        return PHASE_CARVE;
    }

    /** Is this world column inside the district (its oval)? */
    public boolean contains(int x, int z) { return inside(x - centreX, z - centreZ, semiX, semiZ); }

    public int minX() { return centreX - semiX; }

    public int maxX() { return centreX + semiX; }

    public int minZ() { return centreZ - semiZ; }

    public int maxZ() { return centreZ + semiZ; }

    /** The chunks (x, z) the district touches: all of them must be loaded before building. */
    public List<int[]> chunks() {
        List<int[]> out = new ArrayList<>();
        for (int cx = Math.floorDiv(minX(), 16); cx <= Math.floorDiv(maxX(), 16); cx++) {
            for (int cz = Math.floorDiv(minZ(), 16); cz <= Math.floorDiv(maxZ(), 16); cz++) {
                int du = gap(centreX, cx * 16, cx * 16 + 15), dv = gap(centreZ, cz * 16, cz * 16 + 15);
                if (inside(du, dv, semiX, semiZ)) out.add(new int[]{cx, cz});
            }
        }
        return out;
    }

    private static int gap(int c, int lo, int hi) { return c < lo ? lo - c : Math.max(0, c - hi); }

    // ---------------------------------------------------------------- 1. carve
    /** The cavern: the whole oval inside the two-block shell, from the floor up to the ceiling. */
    private void carve(CityOps o) {
        int ia = semiX - 2, ib = semiZ - 2;
        for (int v = -ib; v <= ib; v++) {
            int ext = extent(v, ia, ib);
            if (ext >= 0) o.carve(-ext, floorY + 1, v, ext, CEILING_Y, v);
        }
    }

    // ---------------------------------------------------------------- 2. foundation, shell, roof, floor
    private void floor(CityOps o) {
        int ia = semiX - 2, ib = semiZ - 2;
        for (int v = -semiZ; v <= semiZ; v++) {
            int ext = extent(v, semiX, semiZ);
            if (ext < 0) continue;
            o.box(CityPlan.Pal.BLACKSTONE, -ext, floorY - 3, v, ext, floorY - 1, v);                    // the foundation
            o.box(CityPlan.Pal.BLACKSTONE, -ext, CAP_Y, v, ext, CAP_Y, v);                              // the roof
            int in = Math.abs(v) <= ib ? extent(v, ia, ib) : -1;
            if (in < 0) {
                o.box(CityPlan.Pal.BLACKSTONE, -ext, floorY + 1, v, ext, CEILING_Y, v);                // a row of pure shell
            } else {
                if (in + 1 <= ext) {
                    o.box(CityPlan.Pal.BLACKSTONE, -ext, floorY + 1, v, -in - 1, CEILING_Y, v);
                    o.box(CityPlan.Pal.BLACKSTONE, in + 1, floorY + 1, v, ext, CEILING_Y, v);
                }
            }
        }
        paint(o, floorY, semiX, semiZ, this::floorAt);
        // the moat is cut into the floor: two layers of water over sea lanterns, so the reservoir glows
        paint(o, floorY - 1, PLAZA, PLAZA, (u, v) -> isMoat(u, v) ? code(Pal.WATER, 0) : -1);
        paint(o, floorY - 2, PLAZA, PLAZA, (u, v) -> isMoat(u, v) ? code(Pal.SEA_LANTERN, 0) : -1);
    }

    private static boolean isMoat(int u, int v) {
        int d2 = u * u + v * v;
        return d2 >= 36 && d2 <= 64 && Math.abs(u) > 2 && Math.abs(v) > 2;
    }

    private static int code(Pal p, int variant) { return p.ordinal() * 64 + variant; }

    private interface Cell { int at(int u, int v); }

    /** Paints one layer over an oval as runs of equal blocks along u. A cell function answers a palette code, or -1 to leave the cell alone. */
    private static void paint(CityOps o, int y, int A, int B, Cell f) {
        Pal[] all = Pal.values();
        for (int v = -B; v <= B; v++) {
            int ext = extent(v, A, B);
            if (ext < 0) continue;
            int run = -1, start = 0;
            for (int u = -ext; u <= ext; u++) {
                int c = f.at(u, v);
                if (c != run) {
                    if (run >= 0) o.box(all[run >> 6], run & 63, start, y, v, u - 1, y, v);
                    run = c;
                    start = u;
                }
            }
            if (run >= 0) o.box(all[run >> 6], run & 63, start, y, v, ext, y, v);
        }
    }

    /** The walking surface: plaza rings, the moat with its four bridges, avenues with a gold line, and worn bricks elsewhere. */
    private int floorAt(int u, int v) {
        int d2 = u * u + v * v;
        if (d2 <= PLAZA * PLAZA) {
            if (d2 <= 16) return code(Pal.GILDED, 0);
            if (d2 < 36) return code(((u + v) & 1) == 0 ? Pal.GILDED : Pal.SMOOTH, 0);
            if (d2 <= 64) return code(Math.abs(u) <= 2 || Math.abs(v) <= 2 ? Pal.GOLD : Pal.WATER, 0);
            return code(d2 >= 110 ? Pal.GOLD : Pal.SMOOTH, 0);
        }
        int au = Math.abs(u), av = Math.abs(v);
        if (au <= 3) return code(au == 3 ? Pal.GILDED : au == 0 ? Pal.GOLD : Pal.SMOOTH, 0);
        if (av <= 3) return code(av == 3 ? Pal.GILDED : av == 0 ? Pal.GOLD : Pal.SMOOTH, 0);
        int h = CityRng.hash(seed, u, v, 11) % 100;
        return code(h < 5 ? Pal.CRACKED : h < 7 ? Pal.BLACKSTONE : h < 8 ? Pal.GILDED : Pal.BRICKS, 0);
    }

    // ---------------------------------------------------------------- 3. the plaza, the wall and its gates, the buildings
    /** The dais with the altar on it, candles and amethyst on its top step, and an amethyst spire at each corner of the plaza. */
    private void plaza(CityOps o) {
        int fy = floorY;
        paint(o, fy + 1, 4, 4, (u, v) -> {
            int d2 = u * u + v * v;
            if (d2 <= 9) return code(Pal.GILDED, 0);
            return d2 <= 16 ? code(Pal.STAIRS, inward(u, v)) : -1;
        });
        paint(o, fy + 2, 2, 2, (u, v) -> {
            int d2 = u * u + v * v;
            if (d2 <= 2) return code(Pal.GOLD, 0);
            return d2 <= 5 ? code(Pal.STAIRS, inward(u, v)) : -1;
        });
        o.put(Pal.ALTAR, 0, 0, altarY, 0);
        for (int s = -1; s <= 1; s += 2) {
            o.put(Pal.CANDLE, candles(4), s, fy + 3, 0);
            o.put(Pal.CANDLE, candles(4), 0, fy + 3, s);
            for (int t = -1; t <= 1; t += 2) {
                o.put(Pal.CLUSTER, 0, s, fy + 3, t);
                o.box(Pal.GILDED, 0, 7 * s, fy + 1, 7 * t, 7 * s, fy + 1, 7 * t);                     // a spire at each corner
                o.box(Pal.AMETHYST, 0, 7 * s, fy + 2, 7 * t, 7 * s, fy + 3, 7 * t);
                o.put(Pal.CLUSTER, 0, 7 * s, fy + 4, 7 * t);
            }
        }
    }

    /** The facing a stair takes to climb toward the middle. */
    private static int inward(int u, int v) {
        if (Math.abs(u) >= Math.abs(v)) return u > 0 ? WEST : EAST;
        return v > 0 ? NORTH : SOUTH;
    }

    private boolean inGateHouse(int u, int v) {
        return gateAxis == 0 ? Math.abs(v) <= 5 && Math.abs(u) >= semiX / 2 : Math.abs(u) <= 5 && Math.abs(v) >= semiZ / 2;
    }

    /** The broken outer wall: a ring three blocks thick inside the shell, whole in places, rubble in others, missing in the rest. */
    private void wall(CityOps o) {
        int ia = semiX - 2, ib = semiZ - 2, ac = semiX - 5, bc = semiZ - 5, fy = floorY;
        for (int v = -ib; v <= ib; v++) {
            int ext = extent(v, ia, ib);
            int in = Math.abs(v) <= bc ? extent(v, ac, bc) : -1;
            for (int u = -ext; u <= ext; u++) {
                if (Math.abs(u) <= in || inGateHouse(u, v)) continue;
                int patch = CityRng.hash(seed, u >> 2, v >> 2, 21) % 100;
                int h = patch < 14 ? 0 : patch < 38 ? 1 + CityRng.hash(seed, u, v, 22) % 3 : 7;
                if (h == 7 && ((u + v) & 1) == 0) h = 8;                                               // battlements
                if (h <= 0) continue;
                boolean cracked = CityRng.hash(seed, u >> 1, v >> 1, 23) % 100 < 25;
                o.box(cracked ? Pal.CRACKED : Pal.BRICKS, 0, u, fy + 1, v, u, fy + h, v);
                if (h >= 7 && CityRng.hash(seed, u, v, 24) % 5 == 0) o.put(Pal.GILDED, 0, u, fy + h, v);
            }
        }
    }

    /** Two gate houses at opposite ends of the gate axis. Each ends in a sealed golden door set in the shell, with a glowing sigil. */
    private void gates(CityOps o) {
        int A = gateAxis == 0 ? semiX : semiZ, fy = floorY;
        int xi = A - 5, xo = A - 2;
        for (int side = -1; side <= 1; side += 2) {
            o.frame(0, 0, gateAxis == 1, side, 1);
            for (int s = -1; s <= 1; s += 2) {
                o.box(Pal.BRICKS, 0, xi, fy + 1, 4 * s, xo, fy + 10, 5 * s);
                o.box(Pal.GILDED, 0, xi, fy + 1, 4 * s, xi, fy + 10, 4 * s);
                o.box(Pal.GOLD, 0, xi, fy + 11, 4 * s, xo, fy + 11, 5 * s);
                o.put(Pal.LANTERN, 0, xi, fy + 12, 4 * s);
                o.box(Pal.STAIRS, (s == 1 ? NORTH : SOUTH) | FLIP, xi, fy + 7, 3 * s, xo - 2, fy + 7, 3 * s);   // corbels under the lintel
            }
            o.box(Pal.BRICKS, 0, xi, fy + 8, -3, xo, fy + 10, 3);                                       // the lintel
            o.box(Pal.GOLD, 0, xi, fy + 8, -3, xi, fy + 8, 3);
            o.box(Pal.GILDED, 0, xo - 1, fy + 1, -3, xo, fy + 7, 3);                                    // the sealed door
            o.box(Pal.GOLD, 0, xo - 1, fy + 1, -3, xo - 1, fy + 7, -3);
            o.box(Pal.GOLD, 0, xo - 1, fy + 1, 3, xo - 1, fy + 7, 3);
            o.box(Pal.GOLD, 0, xo - 1, fy + 7, -3, xo - 1, fy + 7, 3);
            o.box(Pal.CRACKED, 0, xo - 1, fy + 1, -2, xo - 1, fy + 2, -1);
            o.box(Pal.AMETHYST, 0, xo - 1, fy + 3, 0, xo - 1, fy + 5, 0);                               // the sigil
            o.box(Pal.AMETHYST, 0, xo - 1, fy + 4, -1, xo - 1, fy + 4, 1);
            o.put(Pal.SEA_LANTERN, 0, xo - 1, fy + 4, 0);
            o.put(Pal.LANTERN, FLIP, xi, fy + 7, 0);                                                    // a lantern hung under the lintel
        }
        o.resetFrame();
    }

    /** The outside face of each sealed gate, seen from the flooded caves: a gold-edged gilded door set flush in the black shell, so the way in can be found. */
    private void gateReliefs(CityOps o) {
        int A = gateAxis == 0 ? semiX : semiZ, fy = floorY;
        for (int side = -1; side <= 1; side += 2) {
            for (int along = A - 1; along <= A; along++) {
                for (int across = -4; across <= 4; across++) {
                    int u = gateAxis == 0 ? side * along : across, v = gateAxis == 0 ? across : side * along;
                    if (!inside(u, v, semiX, semiZ)) continue;
                    if (Math.abs(across) == 4) {
                        o.box(Pal.GOLD, 0, u, fy + 1, v, u, fy + 8, v);
                    } else {
                        o.box(Pal.GILDED, 0, u, fy + 1, v, u, fy + 7, v);
                        o.put(Pal.GOLD, 0, u, fy + 8, v);
                    }
                }
            }
        }
    }

    /** The middle of the outer face of each of the two gates, as world x and z. */
    public int[][] gateFaces() {
        int A = gateAxis == 0 ? semiX : semiZ;
        return gateAxis == 0 ? new int[][]{{centreX - A, centreZ}, {centreX + A, centreZ}} : new int[][]{{centreX, centreZ - A}, {centreX, centreZ + A}};
    }

    // ---------------------------------------------------------------- the lots the buildings stand on
    /** A building site: its centre (district coordinates), its type and size, and the frame it is built in. */
    private record Lot(int type, int cu, int cv, int p1, int p2, boolean swap, int su, int sv) {
        static final int TOWER = 0, HALL = 1, SHRINE = 2, OBELISK = 3, ARCH = 4;
        static final String[] NAMES = {"tower", "hall", "shrine", "obelisk", "arch"};

        /** Half-widths of the footprint along u and v. */
        int hu() {
            return switch (type) {
                case HALL, ARCH -> swap ? p2 : p1;
                case OBELISK -> 1;
                default -> p1;
            };
        }

        int hv() {
            return switch (type) {
                case HALL, ARCH -> swap ? p1 : p2;
                case OBELISK -> 1;
                default -> p1;
            };
        }
    }

    private boolean fits(Lot l, List<Lot> others) {
        int hu = l.hu(), hv = l.hv();
        if (!inside(Math.abs(l.cu) + hu, Math.abs(l.cv) + hv, semiX - 7, semiZ - 7)) return false;
        int nu = Math.abs(l.cu) - hu, nv = Math.abs(l.cv) - hv;
        if (nu < 6 || nv < 6 || nu * nu + nv * nv <= (PLAZA + 3) * (PLAZA + 3)) return false;
        for (Lot o : others) if (Math.abs(l.cu - o.cu) <= hu + o.hu() + 2 && Math.abs(l.cv - o.cv) <= hv + o.hv() + 2) return false;
        return true;
    }

    private Lot trySite(CityRng r, int type, int p1, int p2, List<Lot> lots) {
        for (int t = 0; t < 300; t++) {
            boolean swap = type == Lot.HALL && r.chance(50);
            int qu = r.chance(50) ? 1 : -1, qv = r.chance(50) ? 1 : -1;
            Lot probe = new Lot(type, 0, 0, p1, p2, swap, 1, 1);
            int cu = qu * (6 + probe.hu() + r.below(semiX - 12)), cv = qv * (6 + probe.hv() + r.below(semiZ - 12));
            boolean doorToU = type == Lot.TOWER && Math.abs(cu) < Math.abs(cv);                          // a tower's door faces the nearer avenue
            Lot l = type == Lot.TOWER ? new Lot(type, cu, cv, p1, p2, doorToU, 1, doorToU ? Integer.signum(cu) : Integer.signum(cv))
                    : new Lot(type, cu, cv, p1, p2, swap, 1, 1);
            if (fits(l, lots)) return l;
        }
        return null;
    }

    private List<Lot> placeLots(CityRng r) {
        List<Lot> lots = new ArrayList<>();
        boolean compact = semiX + semiZ < 82;
        // the two avenue ends that are not gates are closed by an arch
        for (int s = -1; s <= 1; s += 2) {
            if (gateAxis == 0) lots.add(new Lot(Lot.ARCH, 0, s * (semiZ - 9), 5, 1, false, 1, 1));
            else lots.add(new Lot(Lot.ARCH, s * (semiX - 9), 0, 5, 1, true, 1, 1));
        }
        int[][] hallSizes = compact ? new int[][]{{6, 3}, {6, 4}} : new int[][]{{6, 4}, {8, 4}, {8, 5}};
        int halls = compact ? 1 : r.range(2, 3), towers = r.range(3, 4), shrines = r.range(2, 3);
        for (int i = 0; i < halls; i++) {
            int[] size = hallSizes[r.below(hallSizes.length)];
            addSite(lots, trySite(r, Lot.HALL, size[0], size[1], lots));
        }
        for (int i = 0; i < towers; i++) addSite(lots, trySite(r, Lot.TOWER, compact ? 3 : r.range(3, 4), 0, lots));
        for (int i = 0; i < shrines; i++) addSite(lots, trySite(r, Lot.SHRINE, compact ? 2 : 3, 0, lots));
        for (int i = r.below(3); i > 0 && lots.size() < 14; i--) addSite(lots, trySite(r, Lot.OBELISK, 1, 0, lots));
        for (int guard = 0; guard < 12 && lots.size() < 8; guard++) addSite(lots, trySite(r, Lot.OBELISK, 1, 0, lots));
        return lots;
    }

    private static void addSite(List<Lot> lots, Lot l) {
        if (l != null) lots.add(l);
    }

    /** Some buildings are ruined: at least two, and at least two stand whole. */
    private boolean[] decideRuins(CityRng r, List<Lot> lots) {
        boolean[] ruin = new boolean[lots.size()];
        int n = 0;
        for (int i = 0; i < ruin.length; i++) {
            ruin[i] = r.chance(lots.get(i).type == Lot.ARCH ? 40 : 35);
            if (ruin[i]) n++;
        }
        for (int i = 0; n < 2 && i < ruin.length; i++) if (!ruin[i]) { ruin[i] = true; n++; }
        for (int i = ruin.length - 1; ruin.length - n < 2 && i >= 0; i--) if (ruin[i]) { ruin[i] = false; n--; }
        return ruin;
    }

    // ---------------------------------------------------------------- 4. details
    /** Lamp posts along the avenues, chandeliers under the roof, and a night sky of lantern stars and amethyst in the ceiling. */
    private void details(CityOps o, List<Lot> lots) {
        int fy = floorY;
        for (int axis = 0; axis < 2; axis++) {
            int len = axis == 0 ? semiX : semiZ;
            for (int dir = -1; dir <= 1; dir += 2) {
                boolean arch = (axis == 0) != (gateAxis == 0);                                           // this avenue ends in an arch
                for (int t = PLAZA + 4; t <= len - 11; t += 7) {
                    if (arch && Math.abs(t - (len - 9)) <= 3) continue;
                    for (int side = -1; side <= 1; side += 2) {
                        int u = axis == 0 ? dir * t : 4 * side, v = axis == 0 ? 4 * side : dir * t;
                        if (CityRng.hash(seed, u, v, 31) % 100 < 20) {
                            o.box(Pal.CRACKED, 0, u, fy + 1, v, u, fy + 1 + CityRng.hash(seed, u, v, 32) % 2, v);          // a broken post
                        } else {
                            o.box(Pal.BRICKS, 0, u, fy + 1, v, u, fy + 3, v);
                            o.put(Pal.GILDED, 0, u, fy + 2, v);
                            o.put(Pal.LANTERN, 0, u, fy + 4, v);
                        }
                    }
                }
                for (int t = PLAZA + 10; t <= len - 14; t += 14) chandelier(o, axis == 0 ? dir * t : 0, axis == 0 ? 0 : dir * t, 3 + (t & 1), false);
            }
        }
        for (int s = -1; s <= 1; s += 2) for (int t = -1; t <= 1; t += 2) chandelier(o, 8 * s, 8 * t, 4, true);
        shellTrim(o);
        int ac = semiX - 5, bc = semiZ - 5;
        for (int v = -bc; v <= bc; v++) {
            int ext = extent(v, ac, bc);
            for (int u = -ext; u <= ext; u++) {
                int h = CityRng.hash(seed, u, v, 41) % 100;
                if (h > 4 || occupied(lots, u, v)) continue;
                if (h == 0) o.put(Pal.SEA_LANTERN, 0, u, CAP_Y, v);
                else if (h <= 2) o.put(Pal.AMETHYST, 0, u, CAP_Y, v);
                else o.put(Pal.CLUSTER, FLIP, u, CEILING_Y, v);
            }
        }
    }

    /** A gold band round the inside of the shell high above the wall, with a sea lantern in it every few blocks, so the vault has a skyline. */
    private void shellTrim(CityOps o) {
        int ia = semiX - 2, ib = semiZ - 2, k = 0;
        for (int v = -semiZ; v <= semiZ; v++) {
            int ext = extent(v, semiX, semiZ);
            for (int u = -ext; u <= ext; u++) {
                if (inside(u, v, ia, ib)) continue;
                if (!(inside(u + 1, v, ia, ib) || inside(u - 1, v, ia, ib) || inside(u, v + 1, ia, ib) || inside(u, v - 1, ia, ib))) continue;
                o.put(Pal.GILDED, 0, u, floorY + 12, v);
                if (++k % 6 == 0) o.put(Pal.SEA_LANTERN, 0, u, floorY + 15, v);
            }
        }
    }

    private static boolean occupied(List<Lot> lots, int u, int v) {
        for (Lot l : lots) if (Math.abs(u - l.cu) <= l.hu() + 2 && Math.abs(v - l.cv) <= l.hv() + 2) return true;
        return false;
    }

    /** A chain from the roof with a lantern at its end. */
    private void chandelier(CityOps o, int u, int v, int length, boolean soul) {
        o.box(Pal.CHAIN, 0, u, CEILING_Y - length + 1, v, u, CEILING_Y, v);
        o.put(soul ? Pal.SOUL_LANTERN : Pal.LANTERN, FLIP, u, CEILING_Y - length, v);
    }
}
