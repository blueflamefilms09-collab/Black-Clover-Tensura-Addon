package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ground plan of a Golden City's biosphere, worked out on a grid of columns before a single op is written (docs/living_dungeon.md,
 * "The Golden City"). A pure, deterministic description with no Minecraft imports: the core cell goes in, a {@link GoldenGrid} comes out,
 * the same on every machine. {@link GoldenCityPlan} turns it into ops, {@link Biosphere} reads it back to keep the garden alive, and
 * the tests walk it.
 *
 * <p>The garden is one cavern about 100 blocks across (a rounded square with a wobbling edge, in the 128-block core cell), 30 to 50 blocks
 * high, walled in by a shell three blocks thick all round so the flooded caves cannot pour in. The old Golden City ({@link CityPlan}) stands
 * in the middle, on the same floor; round it runs a four-block promenade, and four spokes lead from the promenade to four gates in the
 * shell. Between them lie meadows, moss gardens, pools (some under a waterfall), groves, giant trees and mushrooms. All coordinates are
 * offsets (u, v) from the middle of the core cell; the arrays are indexed [u + OFF][v + OFF]. Heights are offsets above {@link #floorY}.
 */
final class GoldenGrid {
    /** The footprint's half-width, how far the air reaches along an axis, the thickness of the shell, the rock under the floor. */
    static final int HALF = 60, REACH = 56, WALL = 3, FOUNDATION = 6;
    static final int OFF = 62, N = 2 * OFF + 1;
    /** The air above the floor at the lowest (the rim) and the highest (the middle). */
    static final int CEIL_MIN = 30, CEIL_MAX = 50;
    /** The first height (above the floor) at which a drape of ivy hangs on the old city's wall: above the head of anyone on the promenade. A ladder of ivy starts on the ground, because climbing needs the climber's feet in the ivy. */
    static final int IVY_FROM = 4;

    /** What a column carries above the ground. */
    static final byte NONE = 0, PATH = 1, GRASS = 2, FLOWER = 3, FERN = 4, MOSS = 5, POOL = 6, SHORE = 7, FIXED = 8, GROVE = 9, CITY = 10, LICHEN = 11;
    /** The kinds of big things that stand in the garden. */
    static final int GREAT_TREE = 0, TREE = 1, GIANT_MUSHROOM = 2, MUSHROOM = 3;
    /** The four sides: east, south, west, north. */
    static final int[] SIDE_DX = {1, 0, -1, 0}, SIDE_DZ = {0, 1, 0, -1};

    /** A gate in the shell: side, lateral offset, the last air cell along the side's axis (tP), the first cell of the promenade (tRing), and that last air cell as (u, v). */
    record Gate(int side, int w, int tP, int tRing, int u, int v) {
        int dx() { return SIDE_DX[side]; }

        int dz() { return SIDE_DZ[side]; }
    }

    /** A pool: its middle and radius; `falls` if a waterfall pours into it, `roof` if it lies on the old city's roof. */
    record Pool(int u, int v, int r, boolean falls, boolean roof) {
        Pool(int u, int v, int r, boolean falls) { this(u, v, r, falls, false); }
    }

    /** A waterfall: the pool column it lands in (u, v), the spring cell in the shell, the direction from one to the other, and the height of the spring above the floor. */
    record Fall(int u, int v, int srcU, int srcV, int dx, int dz, int height) {}

    /** A big thing: kind, middle, canopy or cap radius, the vertical half-size of the canopy, the height of the trunk above the ground, a variant number. */
    record Site(int kind, int u, int v, int r, int ry, int ht, int variant) {
        int trunk() { return kind == GREAT_TREE ? 2 : kind == GIANT_MUSHROOM ? 1 : 0; }
    }

    /** A bare patch where a mini-grove sprouts and is pruned again; the number only keeps its time apart from the others'. */
    record Grove(int u, int v, int phase) {}

    /** A vine that hangs from the block at `anchor` (an offset above the floor) as far down as `max` cells; `canopy` if it hangs from a tree. */
    record Vine(int u, int v, int anchor, int max, boolean canopy) {}

    // ---------------------------------------------------------------- what the grid holds
    final long seed;
    final int cellX, cellZ, cx, cz, floorY;
    final CityPlan city;
    /** The middle of the old city as an offset from the middle of the cell. */
    final int cityU, cityV;

    final boolean[][] inG = new boolean[N][N];
    /** -1 outside, 0 in the garden, 1 .. WALL in the shell. */
    final int[][] shellDist = new int[N][N];
    final int[][] ceilH = new int[N][N];
    final int[][] shellTop = new int[N][N];
    final int[][] ground = new int[N][N];
    final byte[][] cover = new byte[N][N];
    final boolean[][] path = new boolean[N][N];
    final boolean[][] water = new boolean[N][N];
    double[][] dEdge, dCity, dPath, dWater;
    /** The offset above the floor of the first layer over the old city's roof: the soil of the roof garden lies in it (the roof itself is the layer under it). */
    final int roofBase;
    /** The columns of the roof garden: the city's oval, where it lies in the cavern. */
    final boolean[][] roof = new boolean[N][N];
    /** The distance of each roof column from the roof's edge (0 off the roof). */
    double[][] dRoof;
    /** Where climbable ivy stands against the city's wall: {u, v, faces} (bit 1 north, 2 east, 4 south, 8 west: the sides the ivy clings to) and whether it is a whole ladder up to the roof (1) or a drape (0). */
    final List<int[]> ivy = new ArrayList<>();

    final List<Gate> gates = new ArrayList<>();
    final List<Pool> pools = new ArrayList<>();
    final List<Fall> falls = new ArrayList<>();
    final List<Site> sites = new ArrayList<>();
    final List<Grove> groves = new ArrayList<>();
    final List<Vine> vines = new ArrayList<>();
    /** Places a creature can be put down: {u, v, feet y offset}. */
    final List<int[]> stand = new ArrayList<>();
    private final java.util.Set<Long> anchors = new java.util.HashSet<>();
    /** The columns under a crown or a cap: nothing hangs from the ceiling there. */
    final boolean[][] crown = new boolean[N][N];

    private static final Map<Long, GoldenGrid> CACHE = new LinkedHashMap<Long, GoldenGrid>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, GoldenGrid> eldest) { return size() > 4; }
    };

    /** The grid of a core cell (kept for a few: working it out is cheap but not free). */
    static synchronized GoldenGrid of(int cellX, int cellZ) {
        long key = ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
        GoldenGrid g = CACHE.get(key);
        if (g == null) {
            g = new GoldenGrid(cellX, cellZ);
            CACHE.put(key, g);
        }
        return g;
    }

    /** A grid worked out afresh, not the cached one (for tests that compare two runs). */
    static GoldenGrid fresh(int cellX, int cellZ) { return new GoldenGrid(cellX, cellZ); }

    private GoldenGrid(int cellX, int cellZ) {
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.seed = GoldenRng.mix(cellX, cellZ);
        this.city = CityPlan.of(cellX, cellZ);
        this.cx = NexusZone.centre(cellX);
        this.cz = NexusZone.centre(cellZ);
        this.cityU = city.centreX - cx;
        this.cityV = city.centreZ - cz;
        this.floorY = city.floorY;
        this.roofBase = CityPlan.CAP_Y + 1 - city.floorY;
        for (int[] row : shellDist) java.util.Arrays.fill(row, -1);
        buildMask();
        buildShellAndCeiling();
        buildPaths();
        buildRoof();
        buildPools();
        buildGround();
        buildSites();
        buildRoofSites();
        buildCover();
        buildStand();
        buildIvy();
        for (Vine vine : vines) anchors.add(anchorKey(vine.u(), vine.v(), vine.anchor()));
        for (Site s : sites) {
            for (int du = -s.r(); du <= s.r(); du++) {
                for (int dv = -s.r(); dv <= s.r(); dv++) if (du * du + dv * dv <= s.r() * s.r() && inRange(s.u() + du, s.v() + dv)) crown[s.u() + du + OFF][s.v() + dv + OFF] = true;
            }
        }
    }

    private static long anchorKey(int u, int v, int y) { return ((long) (u + OFF) * 1000L + (v + OFF)) * 1000L + (y + 100); }

    /** Does a vine hang from this block (a cell of a canopy, as an offset above the floor)? */
    boolean isVineAnchor(int u, int v, int y) { return anchors.contains(anchorKey(u, v, y)); }

    // ---------------------------------------------------------------- reading
    /** Is this column part of the roof garden? */
    boolean isRoof(int u, int v) { return inRange(u, v) && roof[u + OFF][v + OFF]; }

    /** Is this column at the edge of the roof garden, with the open air of the garden beside it? (A hedge stands there.) */
    boolean isRoofEdge(int u, int v) {
        if (!isRoof(u, v)) return false;
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        for (int k = 0; k < 4; k++) if (g(u + dx[k], v + dz[k]) && !isRoof(u + dx[k], v + dz[k])) return true;
        return false;
    }

    /** The straight-line distance of an offset (exactly rounded, so every machine agrees). */
    static double dist(double du, double dv) { return Math.sqrt(du * du + dv * dv); }

    static boolean inRange(int u, int v) { return u >= -OFF && u <= OFF && v >= -OFF && v <= OFF; }

    /** Is the column in the garden's air? */
    boolean g(int u, int v) { return inRange(u, v) && inG[u + OFF][v + OFF]; }

    /** Is the column inside the old city's oval, grown by `grow` blocks on each half-width? */
    boolean inCity(int u, int v, int grow) { return Footprint.inside(u - cityU, v - cityV, city.semiX + grow, city.semiZ + grow); }

    int ceil(int u, int v) { return inRange(u, v) ? ceilH[u + OFF][v + OFF] : 0; }

    int groundAt(int u, int v) { return inRange(u, v) ? ground[u + OFF][v + OFF] : 0; }

    byte coverAt(int u, int v) { return inRange(u, v) ? cover[u + OFF][v + OFF] : NONE; }

    int shellAt(int u, int v) { return inRange(u, v) ? shellDist[u + OFF][v + OFF] : -1; }

    /** Is this column part of the sealed garden (air or shell)? */
    boolean garden(int u, int v) { return shellAt(u, v) >= 0; }

    /** The position, as a column offset, of the cell `t` along the axis of a side and `w` across it. */
    static int sideU(int side, int t, int w) { return SIDE_DX[side] != 0 ? SIDE_DX[side] * t : w; }

    static int sideV(int side, int t, int w) { return SIDE_DX[side] != 0 ? w : SIDE_DZ[side] * t; }

    /** The y (as an offset above the floor) of the top block of the ground in a column. */
    int topAt(int u, int v) { return groundAt(u, v); }

    // ---------------------------------------------------------------- 1. the cavern
    private void buildMask() {
        double[] wob = new double[16];
        for (int i = 0; i < 16; i++) wob[i] = GoldenRng.unit(seed, i, 0, 1) * 2.0 - 1.0;
        for (int u = -REACH; u <= REACH; u++) {
            for (int v = -REACH; v <= REACH; v++) {
                double t = GoldenRng.turn16(u, v);
                int i0 = (int) Math.floor(t);
                double f = t - i0;
                double w = wob[i0 % 16] + (wob[(i0 + 1) % 16] - wob[i0 % 16]) * GoldenRng.smooth(f);
                double radius = 52.5 + 3.5 * w;                          // 49 .. 56 along the line of the angle
                double r2 = radius * radius, uu = u, vv = v;
                inG[u + OFF][v + OFF] = uu * uu * uu * uu + vv * vv * vv * vv <= r2 * r2 || inCity(u, v, 4);      // a rounded square, and room round the city
            }
        }
        for (int pass = 0; pass < 3; pass++) {                           // no spikes, no pinholes
            boolean[][] snap = new boolean[N][N];
            for (int i = 0; i < N; i++) snap[i] = inG[i].clone();
            for (int u = -REACH; u <= REACH; u++) {
                for (int v = -REACH; v <= REACH; v++) {
                    int nb = (at(snap, u + 1, v) ? 1 : 0) + (at(snap, u - 1, v) ? 1 : 0) + (at(snap, u, v + 1) ? 1 : 0) + (at(snap, u, v - 1) ? 1 : 0);
                    boolean cur = snap[u + OFF][v + OFF];
                    if (cur && nb <= 1 && !inCity(u, v, 4)) inG[u + OFF][v + OFF] = false;
                    else if (!cur && nb >= 3) inG[u + OFF][v + OFF] = true;
                }
            }
        }
        keepComponentOf(Math.max(-REACH, Math.min(REACH, cityU)), Math.max(-REACH, Math.min(REACH, cityV)));
    }

    private static boolean at(boolean[][] a, int u, int v) { return inRange(u, v) && a[u + OFF][v + OFF]; }

    /** Drops every part of the garden that is not joined to the column (su, sv). */
    private void keepComponentOf(int su, int sv) {
        boolean[][] seen = new boolean[N][N];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (g(su, sv)) {
            seen[su + OFF][sv + OFF] = true;
            queue.add(new int[]{su, sv});
        }
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int k = 0; k < 4; k++) {
                int u = c[0] + dx[k], v = c[1] + dz[k];
                if (g(u, v) && !seen[u + OFF][v + OFF]) {
                    seen[u + OFF][v + OFF] = true;
                    queue.add(new int[]{u, v});
                }
            }
        }
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) inG[i][j] = inG[i][j] && seen[i][j];
    }

    /** The distance, in blocks, from each column to the nearest column where `src` is true (a chamfer sweep: close to the straight-line distance). */
    static double[][] chamfer(boolean[][] src) {
        final double far = 1.0e6, diag = 1.4142135623730951;
        double[][] d = new double[N][N];
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) d[i][j] = src[i][j] ? 0.0 : far;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double m = d[i][j];
                if (i > 0) {
                    m = Math.min(m, d[i - 1][j] + 1.0);
                    if (j > 0) m = Math.min(m, d[i - 1][j - 1] + diag);
                    if (j < N - 1) m = Math.min(m, d[i - 1][j + 1] + diag);
                }
                if (j > 0) m = Math.min(m, d[i][j - 1] + 1.0);
                d[i][j] = m;
            }
        }
        for (int i = N - 1; i >= 0; i--) {
            for (int j = N - 1; j >= 0; j--) {
                double m = d[i][j];
                if (i < N - 1) {
                    m = Math.min(m, d[i + 1][j] + 1.0);
                    if (j < N - 1) m = Math.min(m, d[i + 1][j + 1] + diag);
                    if (j > 0) m = Math.min(m, d[i + 1][j - 1] + diag);
                }
                if (j < N - 1) m = Math.min(m, d[i][j + 1] + 1.0);
                d[i][j] = m;
            }
        }
        return d;
    }

    private void buildShellAndCeiling() {
        boolean[][] outside = new boolean[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                outside[i][j] = !inG[i][j];
                if (inG[i][j]) shellDist[i][j] = 0;
            }
        }
        dEdge = chamfer(outside);
        for (int k = 1; k <= WALL; k++) {
            for (int u = -OFF; u <= OFF; u++) {
                for (int v = -OFF; v <= OFF; v++) {
                    if (shellDist[u + OFF][v + OFF] != -1) continue;
                    if (shellAtDist(u + 1, v, k - 1) || shellAtDist(u - 1, v, k - 1) || shellAtDist(u, v + 1, k - 1) || shellAtDist(u, v - 1, k - 1)) shellDist[u + OFF][v + OFF] = k;
                }
            }
        }
        for (int u = -OFF; u <= OFF; u++) {
            for (int v = -OFF; v <= OFF; v++) {
                if (!inG[u + OFF][v + OFF]) continue;
                double t = GoldenRng.smooth(dEdge[u + OFF][v + OFF] / 26.0);
                double wave = GoldenRng.noise(seed, u, v, 14, 3);
                ceilH[u + OFF][v + OFF] = CEIL_MIN + (int) Math.round((CEIL_MAX - CEIL_MIN - 4) * t + 4.0 * wave);
            }
        }
        for (int u = -OFF; u <= OFF; u++) {
            for (int v = -OFF; v <= OFF; v++) {
                if (shellDist[u + OFF][v + OFF] < 1) continue;
                int top = 0;
                for (int du = -WALL; du <= WALL; du++) {
                    for (int dv = -WALL + Math.abs(du); dv <= WALL - Math.abs(du); dv++) {
                        if (g(u + du, v + dv)) top = Math.max(top, ceilH[u + du + OFF][v + dv + OFF]);
                    }
                }
                shellTop[u + OFF][v + OFF] = top + WALL;
            }
        }
    }

    private boolean shellAtDist(int u, int v, int d) { return inRange(u, v) && shellDist[u + OFF][v + OFF] == d; }

    // ---------------------------------------------------------------- 2. the promenade, the spokes, the gates
    private void buildPaths() {
        boolean[][] cityProper = new boolean[N][N];
        for (int u = -OFF; u <= OFF; u++) {
            for (int v = -OFF; v <= OFF; v++) {
                cityProper[u + OFF][v + OFF] = inCity(u, v, 0);
                if (inG[u + OFF][v + OFF] && inCity(u, v, 4) && !inCity(u, v, 0)) path[u + OFF][v + OFF] = true;
            }
        }
        dCity = chamfer(cityProper);
        for (int side = 0; side < 4; side++) {
            Gate gate = chooseGate(side, 5);
            if (gate == null) gate = chooseGate(side, 2);
            if (gate == null) continue;
            gates.add(gate);
            for (int t = gate.tRing(); t <= gate.tP(); t++) {
                for (int l = -1; l <= 1; l++) {
                    int u = sideU(side, t, gate.w() + l), v = sideV(side, t, gate.w() + l);
                    if (g(u, v) && !inCity(u, v, 0)) path[u + OFF][v + OFF] = true;
                }
            }
        }
        dPath = chamfer(path);
    }

    /** A gate on a side: the first lateral offset (counted outward from a hashed start) where the shell is thick round the passage and the approach to the promenade is clear. */
    private Gate chooseGate(int side, int minRun) {
        int start = GoldenRng.hash(seed, side, 0, 5) % 21 - 10;
        for (int k = 0; k < 81; k++) {
            int w = start + ((k & 1) == 0 ? k / 2 : -(k + 1) / 2);
            if (w < -40 || w > 40) continue;
            if (!g(sideU(side, 0, w), sideV(side, 0, w))) continue;
            int tP = 0;
            while (tP + 1 <= REACH && g(sideU(side, tP + 1, w), sideV(side, tP + 1, w))) tP++;
            boolean ok = true;
            for (int d = 1; d <= WALL && ok; d++) {
                for (int l = -1; l <= 1; l++) if (g(sideU(side, tP + d, w + l), sideV(side, tP + d, w + l))) ok = false;
            }
            for (int d = 1; d <= WALL && ok; d++) if (shellAt(sideU(side, tP + d, w), sideV(side, tP + d, w)) != d) ok = false;       // the passage runs through the layers of the shell in order
            if (shellAt(sideU(side, tP + WALL + 1, w), sideV(side, tP + WALL + 1, w)) != -1) ok = false;                          // and comes out in the rock, where the way to a cave starts
            if (!ok) continue;
            int tRing = -1;
            for (int t = tP; t >= 0; t--) {
                if (inCity(sideU(side, t, w), sideV(side, t, w), 4)) {
                    tRing = t;
                    break;
                }
            }
            if (tRing < 0 || tP - tRing < minRun) continue;
            for (int t = tRing; t <= tP && ok; t++) {
                for (int l = -1; l <= 1; l++) {
                    int u = sideU(side, t, w + l), v = sideV(side, t, w + l);
                    if (!g(u, v) || inCity(u, v, 0)) ok = false;
                }
            }
            if (!ok) continue;
            return new Gate(side, w, tP, tRing, sideU(side, tP, w), sideV(side, tP, w));
        }
        return null;
    }

    // ---------------------------------------------------------------- 2b. the roof of the old city
    /** The roof garden's columns and how far each is from the roof's edge. */
    private void buildRoof() {
        boolean[][] off = new boolean[N][N];
        for (int u = -OFF; u <= OFF; u++) {
            for (int v = -OFF; v <= OFF; v++) {
                roof[u + OFF][v + OFF] = g(u, v) && inCity(u, v, 0);
                off[u + OFF][v + OFF] = !roof[u + OFF][v + OFF];
            }
        }
        dRoof = chamfer(off);
    }

    /** One or two ponds on the roof, in the first layer of soil: a single layer of water on the roof's own stone, with soil all round. */
    private void buildRoofPonds() {
        GoldenRng r = new GoldenRng(seed ^ 0x3C6EF372FE94F82BL);
        int made = 0;
        for (int trial = 0; trial < 300 && made < 2; trial++) {
            int u = r.range(-45, 45), v = r.range(-45, 45), rad = r.range(3, 5);
            if (!isRoof(u, v) || dRoof[u + OFF][v + OFF] < rad + 7) continue;
            boolean free = true;
            for (int du = -rad - 2; du <= rad + 2 && free; du++) {
                for (int dv = -rad - 2; dv <= rad + 2; dv++) {
                    int d4 = 4 * (du * du + dv * dv);
                    if (d4 > (2 * rad + 4) * (2 * rad + 4)) continue;
                    if (!isRoof(u + du, v + dv) || water[u + du + OFF][v + dv + OFF]) {
                        free = false;
                        break;
                    }
                }
            }
            for (Pool p : pools) if (p.roof() && (p.u() - u) * (p.u() - u) + (p.v() - v) * (p.v() - v) < (p.r() + rad + 6) * (p.r() + rad + 6)) free = false;
            if (!free) continue;
            pools.add(new Pool(u, v, rad, false, true));
            for (int du = -rad; du <= rad; du++) {
                for (int dv = -rad; dv <= rad; dv++) {
                    if (4 * (du * du + dv * dv) <= (2 * rad - 1) * (2 * rad - 1)) {
                        cover[u + du + OFF][v + dv + OFF] = POOL;
                        water[u + du + OFF][v + dv + OFF] = true;
                    }
                }
            }
            made++;
        }
    }

    // ---------------------------------------------------------------- 3. pools and waterfalls
    private void buildPools() {
        boolean[][] none = new boolean[N][N];
        dWater = chamfer(none);
        GoldenRng r = new GoldenRng(seed ^ 0x51ED270B3F2C1A97L);
        for (int trial = 0; trial < 300 && pools.size() < 4; trial++) {
            int u = r.range(-50, 50), v = r.range(-50, 50), rad = r.range(3, 6);
            if (!g(u, v)) continue;
            if (dEdge[u + OFF][v + OFF] < rad + 3 || dCity[u + OFF][v + OFF] < rad + 5 || dPath[u + OFF][v + OFF] < rad + 3) continue;
            if (!poolFree(u, v, rad)) continue;
            addPool(u, v, rad, false);
        }
        List<int[]> cands = new ArrayList<>();
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        for (int u = -REACH; u <= REACH; u++) {
            for (int v = -REACH; v <= REACH; v++) {
                if (!g(u, v)) continue;
                int out = -1, outs = 0;
                for (int k = 0; k < 4; k++) {
                    if (!g(u + dx[k], v + dz[k])) {
                        out = k;
                        outs++;
                    }
                }
                if (outs != 1) continue;
                int su = u + dx[out], sv = v + dz[out], inside = 0;
                for (int k = 0; k < 4; k++) if (g(su + dx[k], sv + dz[k])) inside++;
                if (inside != 1) continue;
                if (dCity[u + OFF][v + OFF] < 12 || dPath[u + OFF][v + OFF] < 8 || ceilH[u + OFF][v + OFF] < 24) continue;
                cands.add(new int[]{u, v, dx[out], dz[out], GoldenRng.hash(seed, u, v, 9)});
            }
        }
        cands.sort((a, b) -> a[4] != b[4] ? Integer.compare(a[4], b[4]) : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        for (int[] c : cands) {
            if (falls.size() >= 2) break;
            boolean apart = true;
            for (Pool p : pools) if (!p.roof() && Math.abs(p.u() - c[0]) + Math.abs(p.v() - c[1]) < 22) apart = false;
            if (!apart) continue;
            int pu = c[0] - 3 * c[2], pv = c[1] - 3 * c[3];
            if (!g(pu, pv) || dPath[pu + OFF][pv + OFF] < 6 || dCity[pu + OFF][pv + OFF] < 8 || !poolFree(pu, pv, 4)) continue;
            int height = Math.min(ceilH[c[0] + OFF][c[1] + OFF] - 4, 12 + GoldenRng.hash(seed, c[0], c[1], 10) % 8);
            addPool(pu, pv, 4, true);
            falls.add(new Fall(c[0], c[1], c[0] + c[2], c[1] + c[3], c[2], c[3], height));
        }
        buildRoofPonds();
        dWater = chamfer(water);
    }

    /** Is the pool (middle, radius r) clear: its core in the garden, off the city and the paths and other pools; its ring, wherever it is in the garden, off the paths? */
    private boolean poolFree(int pu, int pv, int r) {
        for (int du = -r - 1; du <= r + 1; du++) {
            for (int dv = -r - 1; dv <= r + 1; dv++) {
                int d4 = 4 * (du * du + dv * dv);
                int u = pu + du, v = pv + dv;
                boolean core = d4 <= (2 * r - 1) * (2 * r - 1), ring = !core && d4 <= (2 * r + 2) * (2 * r + 2);
                if (!core && !ring) continue;
                if (core && (!g(u, v) || inCity(u, v, 2) || path[u + OFF][v + OFF] || water[u + OFF][v + OFF])) return false;
                if (ring && g(u, v) && (inCity(u, v, 1) || water[u + OFF][v + OFF])) return false;
            }
        }
        for (Pool p : pools) {
            if (p.roof()) continue;
            int du = p.u() - pu, dv = p.v() - pv;
            double need = p.r() + r + 5;
            if (du * du + dv * dv < need * need) return false;
        }
        return true;
    }

    private void addPool(int pu, int pv, int r, boolean waterfall) {
        pools.add(new Pool(pu, pv, r, waterfall));
        for (int du = -r - 1; du <= r + 1; du++) {
            for (int dv = -r - 1; dv <= r + 1; dv++) {
                int d4 = 4 * (du * du + dv * dv);
                int u = pu + du, v = pv + dv;
                if (!g(u, v) || path[u + OFF][v + OFF]) continue;
                if (d4 <= (2 * r - 1) * (2 * r - 1)) {
                    cover[u + OFF][v + OFF] = POOL;
                    water[u + OFF][v + OFF] = true;
                } else if (d4 <= (2 * r + 2) * (2 * r + 2) && cover[u + OFF][v + OFF] != POOL && !inCity(u, v, 1)) {
                    cover[u + OFF][v + OFF] = SHORE;
                    water[u + OFF][v + OFF] = true;
                }
            }
        }
    }

    // ---------------------------------------------------------------- 4. the lie of the land
    private void buildGround() {
        for (int u = -REACH; u <= REACH; u++) {
            for (int v = -REACH; v <= REACH; v++) {
                if (!g(u, v)) continue;
                if (roof[u + OFF][v + OFF]) {                                  // the roof garden: gentle hills over the roof's own level
                    double rn = GoldenRng.fbm(seed, u, v, 16, 23);
                    int rraw = (int) Math.floor(rn * rn * 6.0);
                    double rflat = Math.min(dRoof[u + OFF][v + OFF] - 4.0, dWater[u + OFF][v + OFF] - 3.0);
                    double rw = rflat <= 0 ? 0 : Math.min(1.0, rflat / 6.0);
                    ground[u + OFF][v + OFF] = roofBase + Math.max(0, Math.min(3, (int) Math.round(rraw * rw)));
                    continue;
                }
                double n = GoldenRng.fbm(seed, u, v, 20, 7);
                int raw = (int) Math.floor(n * n * 10.0);
                double flat = Math.min(Math.min(dCity[u + OFF][v + OFF] - 6.0, dPath[u + OFF][v + OFF] - 2.0), dWater[u + OFF][v + OFF] - 3.0);
                double w = flat <= 0 ? 0 : Math.min(1.0, flat / 8.0);
                ground[u + OFF][v + OFF] = Math.max(0, Math.min(8, (int) Math.round(raw * w)));
            }
        }
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        boolean changed = true;
        for (int pass = 0; changed && pass < 60; pass++) {                // nothing steeper than a step, so everything can be walked
            changed = false;
            for (int u = -REACH; u <= REACH; u++) {
                for (int v = -REACH; v <= REACH; v++) {
                    if (!g(u, v)) continue;
                    int h = ground[u + OFF][v + OFF];
                    for (int k = 0; k < 4; k++) {
                        if (g(u + dx[k], v + dz[k]) && roof[u + dx[k] + OFF][v + dz[k] + OFF] == roof[u + OFF][v + OFF] && h > ground[u + dx[k] + OFF][v + dz[k] + OFF] + 1) {
                            h = ground[u + dx[k] + OFF][v + dz[k] + OFF] + 1;
                            changed = true;
                        }
                    }
                    ground[u + OFF][v + OFF] = h;
                }
            }
        }
    }

    // ---------------------------------------------------------------- 5. trees, mushrooms, groves
    private void buildSites() {
        GoldenRng r = new GoldenRng(seed ^ 0x2545F4914F6CDD1DL);
        place(r, GREAT_TREE, 4, 8, 11);
        place(r, GIANT_MUSHROOM, 6, 4, 7);
        place(r, TREE, 14, 4, 6);
        place(r, MUSHROOM, 16, 2, 2);
        for (int trial = 0; trial < 900 && groves.size() < 14; trial++) {
            int u = r.range(-52, 52), v = r.range(-52, 52);
            if (!g(u, v) || ground[u + OFF][v + OFF] > 4 || !flat(u, v)) continue;
            if (dEdge[u + OFF][v + OFF] < 4 || dCity[u + OFF][v + OFF] < 5 || dPath[u + OFF][v + OFF] < 3 || dWater[u + OFF][v + OFF] < 3) continue;
            boolean clear = true;
            for (Site s : sites) {
                double need = (s.kind() == GIANT_MUSHROOM ? s.r() * 0.8 : s.trunk()) + 3 + 2;
                if (dist(u - s.u(), v - s.v()) < need) clear = false;
            }
            for (Grove o : groves) if (dist(u - o.u(), v - o.v()) < 9) clear = false;
            if (!clear) continue;
            groves.add(new Grove(u, v, r.below(1000)));
            for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) cover[u + du + OFF][v + dv + OFF] = du == 0 && dv == 0 ? GROVE : FIXED;
        }
        for (int i = 0; i < sites.size(); i++) {                           // the vines of the crowns, but none where another crown or a grove is in the way
            Site s = sites.get(i);
            if ((s.kind() != TREE && s.kind() != GREAT_TREE) || isRoof(s.u(), s.v())) continue;
            for (Vine vine : GoldenFlora.canopyVines(this, s)) if (!inAnotherCrown(vine.u(), vine.v(), i) && !nearGrove(vine.u(), vine.v())) vines.add(vine);
        }
    }

    private boolean inAnotherCrown(int u, int v, int except) {
        for (int j = 0; j < sites.size(); j++) {
            if (j == except) continue;
            Site o = sites.get(j);
            int du = u - o.u(), dv = v - o.v();
            if (du * du + dv * dv <= (o.r() + 1) * (o.r() + 1)) return true;
        }
        return false;
    }

    private boolean nearGrove(int u, int v) {
        for (Grove gr : groves) if (Math.abs(u - gr.u()) <= 2 && Math.abs(v - gr.v()) <= 2) return true;
        return false;
    }

    /** Trees, mushrooms and groves on the roof: the same shapes as on the ground, kept off the roof's edge and its ponds and under the ceiling. */
    private void buildRoofSites() {
        GoldenRng r = new GoldenRng(seed ^ 0x1B873593CC9E2D51L);
        placeRoof(r, GREAT_TREE, 2, 8, 10);
        placeRoof(r, GIANT_MUSHROOM, 4, 4, 6);
        placeRoof(r, TREE, 7, 4, 6);
        placeRoof(r, MUSHROOM, 12, 2, 2);
        int made = 0;
        for (int trial = 0; trial < 600 && made < 6; trial++) {
            int u = r.range(-50, 50), v = r.range(-50, 50);
            if (!isRoof(u, v) || !flat(u, v) || dRoof[u + OFF][v + OFF] < 6 || dWater[u + OFF][v + OFF] < 3) continue;
            if (ceilH[u + OFF][v + OFF] - ground[u + OFF][v + OFF] < 12) continue;
            boolean clear = true;
            for (Site s : sites) {
                double need = (s.kind() == GIANT_MUSHROOM ? s.r() * 0.8 : s.trunk()) + 3 + 2;
                if (dist(u - s.u(), v - s.v()) < need) clear = false;
            }
            for (Grove o : groves) if (dist(u - o.u(), v - o.v()) < 9) clear = false;
            if (!clear) continue;
            groves.add(new Grove(u, v, r.below(1000)));
            made++;
            for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) cover[u + du + OFF][v + dv + OFF] = du == 0 && dv == 0 ? GROVE : FIXED;
        }
        for (int i = 0; i < sites.size(); i++) {                           // the vines of the roof's crowns
            Site s = sites.get(i);
            if (!isRoof(s.u(), s.v()) || (s.kind() != TREE && s.kind() != GREAT_TREE)) continue;
            for (Vine vine : GoldenFlora.canopyVines(this, s)) if (!inAnotherCrown(vine.u(), vine.v(), i) && !nearGrove(vine.u(), vine.v())) vines.add(vine);
        }
    }

    private void placeRoof(GoldenRng r, int kind, int count, int rMin, int rMax) {
        int placed = 0;
        for (int trial = 0; trial < 600 && placed < count; trial++) {
            int u = r.range(-50, 50), v = r.range(-50, 50), rad = r.range(rMin, rMax);
            if (!isRoof(u, v)) continue;
            int trunk = new Site(kind, u, v, rad, 0, 0, 0).trunk();
            if (dRoof[u + OFF][v + OFF] < rad + 4 || dWater[u + OFF][v + OFF] < trunk + 2.5) continue;
            boolean clear = true;
            for (Site s : sites) {
                double d = dist(u - s.u(), v - s.v());
                if (d < s.trunk() + trunk + 2 || d < (rad + s.r()) * 0.55) clear = false;
            }
            for (Grove o : groves) if (dist(u - o.u(), v - o.v()) < rad * 0.6 + trunk + 4) clear = false;
            if (!clear) continue;
            int room = ceilH[u + OFF][v + OFF] - ground[u + OFF][v + OFF];
            int ht, ry = 0;
            switch (kind) {
                case GREAT_TREE -> {
                    ht = Math.min(room - 8, r.range(24, 32));
                    ry = 4;
                    if (ht < 18) continue;
                }
                case TREE -> {
                    ht = Math.min(r.range(7, 11), room - 8);
                    ry = 2;
                    if (ht < 5) continue;
                }
                case GIANT_MUSHROOM -> {
                    ht = r.range(6, 12);
                    if (ht + rad / 2 + 6 > room) continue;
                }
                default -> ht = r.range(2, 4);
            }
            Site s = new Site(kind, u, v, rad, ry, ht, r.below(1000));
            sites.add(s);
            int fix = s.trunk() + 1;
            for (int du = -fix; du <= fix; du++) {
                for (int dv = -fix; dv <= fix; dv++) {
                    if (g(u + du, v + dv) && du * du + dv * dv <= fix * fix + 1) cover[u + du + OFF][v + dv + OFF] = FIXED;
                }
            }
            placed++;
        }
    }

    /** Is the 3 x 3 of ground round a column all at one height, and all in the garden and clear of a path? */
    private boolean flat(int u, int v) {
        int h = ground[u + OFF][v + OFF];
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                if (!g(u + du, v + dv) || ground[u + du + OFF][v + dv + OFF] != h || path[u + du + OFF][v + dv + OFF] || water[u + du + OFF][v + dv + OFF]) return false;
            }
        }
        return true;
    }

    private void place(GoldenRng r, int kind, int count, int rMin, int rMax) {
        int placed = 0;
        for (int trial = 0; trial < 600 && placed < count; trial++) {
            int u = r.range(-52, 52), v = r.range(-52, 52), rad = r.range(rMin, rMax);
            if (!g(u, v)) continue;
            Site probe = new Site(kind, u, v, rad, 0, 0, 0);
            int trunk = probe.trunk();
            if (dEdge[u + OFF][v + OFF] < rad + 2 || dCity[u + OFF][v + OFF] < (kind == GREAT_TREE ? trunk + 4 : rad + 3)) continue;
            if (dPath[u + OFF][v + OFF] < trunk + 2.5 || dWater[u + OFF][v + OFF] < trunk + 2.5) continue;
            boolean clear = true;
            for (Site s : sites) {
                double d = dist(u - s.u(), v - s.v());
                if (d < s.trunk() + trunk + 2 || d < (rad + s.r()) * 0.55) clear = false;
            }
            for (Grove o : groves) if (dist(u - o.u(), v - o.v()) < rad * 0.6 + trunk + 4) clear = false;
            if (!clear) continue;
            int ceil = ceilH[u + OFF][v + OFF];
            int ht, ry = 0;
            switch (kind) {
                case GREAT_TREE -> {
                    ht = Math.min(ceil - 8 - ground[u + OFF][v + OFF], r.range(24, 32));
                    ry = 4;
                    if (ht < 18) continue;
                    int underside = ground[u + OFF][v + OFF] + ht - 5;                 // the canopy may spread over the city's roof, but only well above it
                    if (dCity[u + OFF][v + OFF] < rad + 3 && underside < CityPlan.CAP_Y - floorY + 2) continue;
                }
                case TREE -> {
                    ht = Math.min(r.range(7, 11), ceil - 8 - ground[u + OFF][v + OFF]);
                    ry = 2;
                    if (ht < 5) continue;
                }
                case GIANT_MUSHROOM -> ht = r.range(6, 12);
                default -> ht = r.range(2, 4);
            }
            Site s = new Site(kind, u, v, rad, ry, ht, r.below(1000));
            sites.add(s);
            int fix = s.trunk() + 1;
            for (int du = -fix; du <= fix; du++) {
                for (int dv = -fix; dv <= fix; dv++) {
                    if (g(u + du, v + dv) && du * du + dv * dv <= fix * fix + 1) cover[u + du + OFF][v + dv + OFF] = FIXED;
                }
            }
            placed++;
        }
    }

    // ---------------------------------------------------------------- 6. what grows on the ground
    /** The zone of the garden a column lies in: 0 default meadow, 1 flower meadow, 2 moss garden. */
    int zoneAt(int u, int v) {
        double n = GoldenRng.fbm(seed, u, v, 26, 11);
        return n > 0.62 ? 1 : n < 0.30 ? 2 : 0;
    }

    private void buildCover() {
        for (int u = -REACH; u <= REACH; u++) {
            for (int v = -REACH; v <= REACH; v++) {
                if (!g(u, v)) continue;
                int i = u + OFF, j = v + OFF;
                if (cover[i][j] != NONE) continue;                          // pools, shores and fixed places are decided already
                if (path[i][j]) {
                    cover[i][j] = PATH;
                    continue;
                }
                if (roof[i][j] && isRoofEdge(u, v)) {                        // the hedge along the roof's edge
                    cover[i][j] = FIXED;
                    continue;
                }
                if (!roof[i][j] && inCity(u, v, 1)) {
                    cover[i][j] = CITY;
                    continue;
                }
                double r = GoldenRng.unit(seed, u, v, 13);
                byte k;
                switch (zoneAt(u, v)) {
                    case 1 -> k = r < 0.55 ? FLOWER : r < 0.80 ? GRASS : r < 0.90 ? LICHEN : NONE;
                    case 2 -> k = r < 0.50 ? MOSS : r < 0.70 ? FERN : r < 0.78 ? LICHEN : r < 0.88 ? GRASS : NONE;
                    default -> k = r < 0.40 ? GRASS : r < 0.50 ? FERN : r < 0.60 ? FLOWER : r < 0.66 ? LICHEN : r < 0.72 ? MOSS : NONE;
                }
                if (groundMaterial(u, v) == 4) k = NONE;                    // nothing grows on a golden froglight
                cover[i][j] = k;
            }
        }
        for (Gate gate : gates) {                                           // the arch of each gate stands on its last cells
            for (int l = -1; l <= 1; l++) {
                int u = sideU(gate.side(), gate.tP(), gate.w() + l), v = sideV(gate.side(), gate.tP(), gate.w() + l);
                if (g(u, v)) cover[u + OFF][v + OFF] = FIXED;
            }
        }
    }

    // ---------------------------------------------------------------- 8. ivy on the old city's wall
    /**
     * Ivy on the wall of the old city: a few whole ladders from the ground to the roof (the way up to the roof garden: ivy is climbed, and
     * it is passable, so the promenade stays open), and drapes that hang from the roof's edge, never lower than {@link #IVY_FROM}. They stand in
     * the promenade's cells next to the wall; a ladder is a handful of cells, so the old city's ways, which look for open cells outside the
     * wall, find plenty of rows without one.
     */
    private void buildIvy() {
        int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};                      // north, east, south, west
        List<int[]> cells = new ArrayList<>();
        for (int u = -REACH; u <= REACH; u++) {
            for (int v = -REACH; v <= REACH; v++) {
                if (!g(u, v) || roof[u + OFF][v + OFF] || !path[u + OFF][v + OFF]) continue;
                int faces = 0;
                for (int k = 0; k < 4; k++) if (isRoof(u + dx[k], v + dz[k])) faces |= 1 << k;
                if (faces != 0) cells.add(new int[]{u, v, faces, GoldenRng.hash(seed, u, v, 95)});
            }
        }
        cells.sort((a, b) -> a[3] != b[3] ? Integer.compare(a[3], b[3]) : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        List<int[]> ladders = new ArrayList<>();
        for (int[] c : cells) {
            if (ladders.size() >= 12) break;
            boolean apart = true;
            for (int[] l : ladders) if (Math.abs(l[0] - c[0]) + Math.abs(l[1] - c[1]) < 10) apart = false;
            if (apart) ladders.add(c);
        }
        for (int[] c : cells) {
            boolean ladder = ladders.contains(c);
            if (ladder || GoldenRng.hash(seed, c[0], c[1], 96) % 100 < 38) ivy.add(new int[]{c[0], c[1], c[2], ladder ? 1 : 0});
        }
    }

    /** The material of the top block of the ground in a column: 0 grass, 1 moss, 2 podzol, 3 rooted dirt, 4 golden froglight. */
    int groundMaterial(int u, int v) {
        double r = GoldenRng.unit(seed, u, v, 17);
        if (r < 0.012) return 4;
        switch (zoneAt(u, v)) {
            case 2: return r < 0.85 ? 1 : 2;
            case 1: return r < 0.70 ? 0 : r < 0.90 ? 1 : r < 0.96 ? 2 : 3;
            default: return r < 0.50 ? 0 : r < 0.85 ? 1 : r < 0.93 ? 2 : 3;
        }
    }

    // ---------------------------------------------------------------- 7. where a creature can be put down
    private void buildStand() {
        for (int u = -REACH; u <= REACH; u += 4) {
            for (int v = -REACH; v <= REACH; v += 4) {
                int uu = u + GoldenRng.hash(seed, u, v, 21) % 3 - 1, vv = v + GoldenRng.hash(seed, u, v, 22) % 3 - 1;
                if (!g(uu, vv) || dEdge[uu + OFF][vv + OFF] < 3 || roof[uu + OFF][vv + OFF]) continue;
                byte k = cover[uu + OFF][vv + OFF];
                if (k == POOL || k == SHORE || k == FIXED || k == CITY || k == GROVE) continue;
                boolean nearGate = false;
                for (Gate gate : gates) if (Math.abs(gate.u() - uu) + Math.abs(gate.v() - vv) < 14) nearGate = true;
                if (nearGate) continue;
                stand.add(new int[]{uu, vv, ground[uu + OFF][vv + OFF] + 1});
            }
        }
        Collections.shuffle(stand, new java.util.Random(seed));
    }
}
