package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: Floor 0 is the only place in the Mega-Dungeon that is drawn by a chunk generator, and a chunk generator is the hardest kind of code to get right
 * without a game (a mistake shows up as a world that looks wrong or a server that never finishes a chunk). So the whole Hub is one PURE function,
 * {@code (seed, x, y, z) -> BlockKind}, with no Minecraft type in it: it can be unit tested (the walkability search proves the two gates are reachable), drawn as
 * PNG slices by {@code MacroPreview} (the owner can look at it before the game exists), and the Minecraft part ({@link SanctuaryGenerator}) shrinks to "ask the
 * model, place the block". The model is built once per world seed and cached (building it solves the Wave Function Collapse of the landmarks, which is cheap
 * but not free), then only read, so the generator's worker threads share it without locks.
 *
 * <p>What it draws: (a) the PLAINS, a gentle rolling heightfield of 2D simplex noise (amplitude {@link MacroParams#hillAmplitude}, wavelength about 48) that
 * is blended to exactly {@link HubLayout#SURFACE_Y} inside the plaza and over the {@value #BLEND} blocks after it, flat round both landmarks, and flat along the
 * two roads; grass over dirt over stone over deepslate (a noisy seam near y 0) over bedrock with a noisy second layer; (b) the two ROADS of stone brick from the
 * plaza to each door with a lantern post every 12 blocks; (c) the HOLY SPIRE ({@link SpireModel}) to the east; (d) the ABYSSAL CHASM ({@link ChasmModel}) to the
 * west. Order of authority: landmark over road over terrain.
 *
 * <p>Convention (shared with the plaza template and the hub self-tests): {@code surfaceHeight} is the y of the first free cell above the ground, so the grass
 * block is at {@code surfaceHeight - 1} and a player stands at {@code surfaceHeight}.
 */
public final class HubModel {
    public static final int MIN_Y = -64, MAX_Y = 319, HEIGHT = 384;
    /** The distance over which the hills fade in after a flat zone. */
    static final int BLEND = 24;
    private static final int SPIRE_FLAT = HubLayout.SPIRE_BASE_RADIUS + 16;
    private static final int CHASM_FLAT = HubLayout.CHASM_RIM_RADIUS + 18 + 6;
    /** The roads run along z = 0 between these x values (their ends lie deep inside the flat zones of the plaza and the landmarks). */
    private static final int ROAD_FROM = HubLayout.CHASM_X + 26, ROAD_TO = HubLayout.SPIRE_X - 16;

    private record Key(long seed, MacroParams params) {}

    private static final ConcurrentHashMap<Key, HubModel> CACHE = new ConcurrentHashMap<>();

    /** The model of this world seed (built on first use, then shared). */
    public static HubModel of(long seed) { return of(seed, MacroParams.DEFAULT); }

    public static HubModel of(long seed, MacroParams params) {
        Key key = new Key(seed, params);
        HubModel m = CACHE.get(key);
        if (m != null) return m;
        m = new HubModel(seed, params);
        if (CACHE.size() >= 6) CACHE.clear();
        HubModel prev = CACHE.putIfAbsent(key, m);
        return prev != null ? prev : m;
    }

    /** Forgets every cached model (a test, or a server that changed the config). */
    public static void clearCache() { CACHE.clear(); }

    /** A new model that is neither read from nor put into the cache (the self-test that re-solves both Wave Function Collapse problems measures a real build). */
    public static HubModel fresh(long seed, MacroParams params) { return new HubModel(seed, params); }

    private final long seed;
    private final MacroParams params;
    private final Simplex3D noise;
    private final SpireModel spire;
    private final ChasmModel chasm;
    private final long buildNanos;

    private HubModel(long seed, MacroParams params) {
        long t0 = System.nanoTime();
        this.seed = seed;
        this.params = params;
        this.noise = new Simplex3D(seed * 31L + 0x5A17L);
        this.spire = params.landmarks() ? new SpireModel(seed, params, noise) : null;
        this.chasm = params.landmarks() ? new ChasmModel(seed, params, noise) : null;
        this.buildNanos = System.nanoTime() - t0;
    }

    public long seed() { return seed; }

    public MacroParams params() { return params; }

    /** The Spire, or null when the landmarks are switched off in the config. */
    public SpireModel spire() { return spire; }

    /** The Chasm, or null when the landmarks are switched off in the config. */
    public ChasmModel chasm() { return chasm; }

    /** Nanoseconds the construction took (it includes both Wave Function Collapse solves). */
    public long buildNanos() { return buildNanos; }

    // ---- the plains

    private static double dist(double x, double z, double cx, double cz) {
        double dx = x - cx, dz = z - cz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 0 where the hills must not rise (plaza, landmarks, roads), 1 in open country, smooth in between. */
    private double hillMask(int x, int z) {
        double m = Geo.smoothstep(HubLayout.PLAZA_RADIUS, HubLayout.PLAZA_RADIUS + BLEND, dist(x, z, HubLayout.PLAZA_X, HubLayout.PLAZA_Z));
        m = Math.min(m, Geo.smoothstep(SPIRE_FLAT, SPIRE_FLAT + BLEND, dist(x, z, HubLayout.SPIRE_X, HubLayout.SPIRE_Z)));
        m = Math.min(m, Geo.smoothstep(CHASM_FLAT, CHASM_FLAT + BLEND, dist(x, z, HubLayout.CHASM_X, HubLayout.CHASM_Z)));
        if (x >= ROAD_FROM && x <= ROAD_TO) m = Math.min(m, Geo.smoothstep(3, 15, Math.abs(z - HubLayout.PLAZA_Z)));
        return m;
    }

    /** The y of the first free cell above the ground of the plains at this column (landmarks and roads not included). */
    public int surfaceHeight(int x, int z) {
        double mask = hillMask(x, z);
        if (mask <= 0.0 || params.hillAmplitude() <= 0.0) return HubLayout.SURFACE_Y;
        double h = noise.fbm(x / 48.0, z / 48.0, 2, 2.0, 0.5) * params.hillAmplitude() * mask;
        return HubLayout.SURFACE_Y + (int) Math.round(h);
    }

    /** A deterministic value in [0, 1) for a position (the seams of the strata, the second bedrock layer). */
    static double hash01(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L + y * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    private BlockKind terrainKind(int x, int y, int z, int h) {
        if (y >= h) return BlockKind.AIR;
        if (y == MIN_Y) return BlockKind.BEDROCK;
        if (y == MIN_Y + 1 && hash01(x, y, z) < 0.5) return BlockKind.BEDROCK;
        if (y == h - 1) return BlockKind.GRASS;
        if (y >= h - 4) return BlockKind.DIRT;
        return rockKind(x, y, z);
    }

    /** Stone above y 0, deepslate below y -8, a noisy mix between (the same seam vanilla uses, so the Hub does not look foreign). */
    static BlockKind rockKind(int x, int y, int z) {
        if (y >= 0) return BlockKind.STONE;
        if (y < -8) return BlockKind.DEEPSLATE;
        return hash01(x, y, z) < (-y) / 8.0 ? BlockKind.DEEPSLATE : BlockKind.STONE;
    }

    /** Is the column on a road (3 wide, along z = plaza z)? */
    public static boolean onRoad(int x, int z) {
        if (Math.abs(z - HubLayout.PLAZA_Z) > 1) return false;
        return (x >= 12 && x <= HubLayout.SPIRE_DOOR_X - 1) || (x <= -12 && x >= HubLayout.CHASM_DOOR_X + 1);
    }

    /** Is the column a lamp post of the road (a wall post with a lantern on top, every 12 blocks, one block beside the verge)? */
    public static boolean lampPost(int x, int z) {
        if (Math.abs(z - HubLayout.PLAZA_Z) != 3) return false;
        if (x >= 24 && x <= HubLayout.SPIRE_DOOR_X - 12) return x % 12 == 0;
        if (x <= -24 && x >= HubLayout.CHASM_DOOR_X + 12) return (-x) % 12 == 0;
        return false;
    }

    // ---- everything

    /** The kind of block at a position. Outside the dimension's height the answer is air (above) or bedrock (below). */
    public BlockKind kindAt(int x, int y, int z) {
        if (y > MAX_Y) return BlockKind.AIR;
        if (y < MIN_Y) return BlockKind.BEDROCK;
        int h = surfaceHeight(x, z);
        BlockKind k = base(x, y, z, h);
        if (spire == null) {
            BlockKind o = miniGate(x, y, z);
            return o != null ? o : k;
        }
        SpireModel.Column sc = spire.column(x, z);
        if (sc != null) {
            BlockKind o = sc.kind(y);
            if (o != null) return o;
        }
        ChasmModel.Column cc = chasm.column(x, z);
        if (cc != null) {
            BlockKind o = cc.kind(y);
            if (o != null) return o;
        }
        return k;
    }

    private BlockKind base(int x, int y, int z, int h) {
        if (y == h - 1 && onRoad(x, z)) return BlockKind.PATH;
        if (y == h && lampPost(x, z)) return BlockKind.POST;
        if (y == h + 1 && lampPost(x, z)) return BlockKind.LANTERN;
        return terrainKind(x, y, z, h);
    }

    /** Fills {@code out[y - MIN_Y]} for every y of the dimension with the kind of the block at (x, y, z). {@code out} must hold {@value #HEIGHT} entries. */
    public void fillColumn(int x, int z, BlockKind[] out) { fillColumn(x, z, out, MIN_Y, MAX_Y); }

    /**
     * Like {@link #fillColumn(int, int, BlockKind[])} but only for the heights y0 to y1 inclusive (the other entries of {@code out} are left alone). Why: a search
     * or a picture that looks at one landmark needs a hundred rows, not the whole 384, and the column code is the expensive part of the model.
     */
    public void fillColumn(int x, int z, BlockKind[] out, int y0, int y1) {
        int lo = Math.max(MIN_Y, y0), hi = Math.min(MAX_Y, y1);
        int h = surfaceHeight(x, z);
        SpireModel.Column sc = spire == null ? null : spire.column(x, z);
        ChasmModel.Column cc = chasm == null ? null : chasm.column(x, z);
        boolean mini = spire == null && (Math.abs(x - HubLayout.HOLY_GATE_X) <= 4 && Math.abs(z - HubLayout.HOLY_GATE_Z) <= 4 || Math.abs(x - HubLayout.ABYSS_GATE_X) <= 5 && Math.abs(z - HubLayout.ABYSS_GATE_Z) <= 5);
        for (int y = lo; y <= hi; y++) {
            BlockKind k = null;
            if (sc != null) k = sc.kind(y);
            if (k == null && cc != null) k = cc.kind(y);
            if (k == null && mini) k = miniGate(x, y, z);
            out[y - MIN_Y] = k != null ? k : base(x, y, z, h);
        }
    }

    /** The y of the first free cell above the highest block that is not air in this column (what the heightmap would call the surface), landmarks included. */
    public int topY(int x, int z) {
        SpireModel.Column sc = spire == null ? null : spire.column(x, z);
        ChasmModel.Column cc = chasm == null ? null : chasm.column(x, z);
        boolean mini = spire == null && (Math.abs(x - HubLayout.HOLY_GATE_X) <= 4 && Math.abs(z - HubLayout.HOLY_GATE_Z) <= 4);
        if (sc == null && cc == null && !mini) {
            int h = surfaceHeight(x, z);
            return lampPost(x, z) ? h + 2 : h;
        }
        int h = surfaceHeight(x, z);
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            BlockKind k = null;
            if (sc != null) k = sc.kind(y);
            if (k == null && cc != null) k = cc.kind(y);
            if (k == null && mini) k = miniGate(x, y, z);
            if (k == null) k = base(x, y, z, h);
            if (k != BlockKind.AIR) return y + 1;
        }
        return MIN_Y;
    }

    /**
     * With the landmarks switched off the two gates still have to exist where {@link HubLayout} says, because the hub's gate lookup is by position: the Holy gate
     * stands on a small quartz platform floating at its height, the Abyssal gate in a small sealed pocket in the rock at its depth. Nobody can walk to them (an
     * admin reaches them with a teleport); they keep the Hub functional while the landmark code is off.
     */
    private BlockKind miniGate(int x, int y, int z) {
        int hx = x - HubLayout.HOLY_GATE_X, hz = z - HubLayout.HOLY_GATE_Z, hy = y - HubLayout.HOLY_GATE_Y;
        if (Math.abs(hx) <= 3 && Math.abs(hz) <= 3 && hy >= -3 && hy <= 2) {
            if (hy == -3) return BlockKind.SMOOTH_QUARTZ;
            if (hz == 0 && Math.abs(hx) <= 1 && hy >= -2 && hy <= 1) return BlockKind.GATE;
            if (hz == 0 && Math.abs(hx) == 2 && hy >= -2 && hy <= 2) return BlockKind.GATE_FRAME;
            if (hz == 0 && Math.abs(hx) <= 2 && hy == 2) return BlockKind.GATE_FRAME;
            return BlockKind.AIR;
        }
        int ax = x - HubLayout.ABYSS_GATE_X, az = z - HubLayout.ABYSS_GATE_Z, ay = y - HubLayout.ABYSS_GATE_Y;
        if (Math.abs(ax) <= 4 && Math.abs(az) <= 4 && ay >= -3 && ay <= 3) {
            if (ay == -3) return BlockKind.PLATFORM_DARK;
            if (az == 0 && Math.abs(ax) <= 1 && ay >= -2 && ay <= 1) return BlockKind.GATE;
            if (az == 0 && Math.abs(ax) == 2 && ay >= -2 && ay <= 2) return BlockKind.ABYSS_FRAME;
            if (az == 0 && Math.abs(ax) <= 2 && ay == 2) return BlockKind.ABYSS_FRAME;
            return BlockKind.AIR;
        }
        return null;
    }

    /** Statistics of the Wave Function Collapse of both landmarks: {restarts, fallbacks, collapses, backtracks, nanos} summed over their problems. */
    public long[] wfcStats() {
        if (spire == null) return new long[5];
        long[] a = spire.wfcStats(), b = chasm.wfcStats();
        long[] s = new long[5];
        for (int i = 0; i < 5; i++) s[i] = a[i] + b[i];
        return s;
    }
}
