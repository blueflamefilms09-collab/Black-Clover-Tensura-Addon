package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

/**
 * Why: this is the ABYSSAL CHASM, the "massive subterranean chasm" of the brief, as a pure function from a world position to a {@link BlockKind}. It is a pit
 * carved down from the plains whose radius narrows (a flared bowl: wide at the rim, nearly vertical at the bottom) from {@link HubLayout#CHASM_RIM_RADIUS} to
 * {@link HubLayout#CHASM_FLOOR_RADIUS} over {@link HubLayout#CHASM_DEPTH} blocks, whose wall is roughened by 3D simplex noise into ribs of rock that jut into the
 * pit, and whose strata change with depth (stone, deepslate, then blackstone and basalt near the bottom, with moss and sculk growing on the wall). A spiral RAMP
 * descends the wall from the rim door to a platform on the floor where the Abyssal gate stands, and behind the wall, in three rings of cells, the 3D Wave
 * Function Collapse ({@link ChasmTiles}) carves a network of CAVERNS that open onto the pit through mouths; the landings of the ramp each have a mouth
 * pre-collapsed behind them, so a side cave is always one step away from the road.
 *
 * <p>Order of authority (a voxel takes the first rule that claims it): (1) the RAMP, its support, its carved headroom and its rail, because the road to the gate
 * must exist whatever the noise or the tiles do; (2) the rim fence; (3) the gate, its frame and the platform; (4) the floor of the pit; (5) the open air of the
 * pit; (6) the caverns and the skin of the wall; (7) whatever the plains put there (rock). Like the Spire it is immutable once built and shares the Hub's noise.
 */
public final class ChasmModel {
    public static final int CX = HubLayout.CHASM_X, CZ = HubLayout.CHASM_Z, Y0 = HubLayout.SURFACE_Y;
    public static final int R0 = HubLayout.CHASM_RIM_RADIUS, R1 = HubLayout.CHASM_FLOOR_RADIUS;
    /** The standing level on the floor of the pit (it equals the bottom of the Abyssal gate plane) and the y of the top block under it. */
    public static final int FLOOR_STAND = HubLayout.ABYSS_GATE_Y - 2, FLOOR_TOP = FLOOR_STAND - 1;
    public static final double DOOR_ANGLE = 0.0;
    /** The top row of the highest cave cell: below the dirt and the grass of the plains. */
    public static final int CAVE_TOP = Y0 - 5;
    public static final int RINGS = 3;
    static final int WIDTH = 4, HEADROOM = 5;
    /** The cutoff beyond which the Chasm claims no column: the rim, the three rings of cells and some margin. */
    public static final double REACH = R0 + RINGS * 8 + 8;
    public static final int PLATFORM_RADIUS = 7;

    /** The nominal radius of the pit wall at each y from the floor to the rim (precomputed with StrictMath: the curve decides the shape of the whole pit). */
    private static final double[] WALL = new double[Y0 - FLOOR_STAND + 1];

    static {
        for (int y = FLOOR_STAND; y <= Y0; y++) {
            double t = (double) (Y0 - y) / (Y0 - FLOOR_STAND);
            WALL[y - FLOOR_STAND] = R1 + (R0 - R1) * StrictMath.pow(1.0 - t, 1.6);
        }
    }

    /** The radius of the pit wall at height y (flared: it is widest at the rim, and the wall becomes vertical towards the floor). */
    public static double rwall(int y) {
        if (y >= Y0) return WALL[WALL.length - 1];
        if (y <= FLOOR_STAND) return WALL[0];
        return WALL[y - FLOOR_STAND];
    }

    private final MacroParams params;
    private final Simplex3D noise;
    private final long seed;
    private final int cell, sectors, levels;
    private final Spiral ramp;
    private final Wfc3D.Result wfc;
    private final double arrivalAngle, arrivalCos, arrivalSin;
    private final int[] landingLevels;

    ChasmModel(long seed, MacroParams params, Simplex3D noise) {
        this.seed = seed;
        this.params = params;
        this.noise = noise;
        this.cell = params.cell();
        this.sectors = Math.max(16, (int) Math.round(Geo.TWO_PI * 40.0 / cell));
        this.levels = (CAVE_TOP - FLOOR_TOP + 1 + cell - 1) / cell;
        // landings: every ~18 blocks of descent, each at the standing level of a cave cell's floor so the mouth behind it is flush with the road
        int[] lv = new int[6];
        int n = 0, last = -1;
        for (int i = 0; i < 6; i++) {
            int target = Y0 - 15 - 18 * i;
            int level = (int) Math.round((double) (standAt(0) - target) / cell);
            if (level <= last) level = last + 1;
            if (standAt(level) < FLOOR_STAND + 8 || level >= levels) break;
            lv[n++] = level;
            last = level;
        }
        this.landingLevels = java.util.Arrays.copyOf(lv, n);
        int[] landingQ = new int[n + 1];
        for (int i = 0; i < n; i++) landingQ[i + 1] = 2 * (Y0 - standAt(landingLevels[i]));
        int qEnd = 2 * (Y0 - FLOOR_STAND);
        HelixPath path = Spiral.build(-1, DOOR_ANGLE, qEnd, q -> radiusOfRampAt(q), landingQ, 10, 8, WIDTH / 2.0);
        this.ramp = new Spiral(path, WIDTH, false, Y0, 0, HEADROOM);
        this.arrivalAngle = ramp.landingMid(qEnd);
        this.arrivalCos = StrictMath.cos(arrivalAngle);
        this.arrivalSin = StrictMath.sin(arrivalAngle);
        Wfc3D.Problem p = ChasmTiles.problem(sectors, levels);
        for (int i = 0; i < n; i++) {
            int[] ab = ramp.landingSteps(landingQ[i + 1]);
            int j = levels - 1 - landingLevels[i];
            for (int step = ab[0]; step <= ab[1]; step++) {
                double ang = ramp.path.angleOf(step) + ramp.path.dir * 0.5 / Math.max(1.0, ramp.path.radius(step));
                p.setFixed(Geo.sector(ang, DOOR_ANGLE, sectors), j, 0, ChasmTiles.MOUTH);
            }
        }
        long s = seed * 0x9E3779B97F4A7C15L + 0xA11CE5L;
        this.wfc = Wfc3D.solve(ChasmTiles.TILESET, p, s, params.wfcTries(), ChasmTiles.ROCK);
    }

    /** The standing level of the floor row of cave level {@code level}. */
    private int standAt(int level) { return CAVE_TOP - cell * (level + 1) + 2; }

    private static double radiusOfRampAt(int q) {
        int row = Math.floorDiv(2 * Y0 - q - 1, 2);
        return rwall(row) - WIDTH / 2.0;
    }

    public Spiral ramp() { return ramp; }

    public Wfc3D.Result wfc() { return wfc; }

    public int sectors() { return sectors; }

    public int levels() { return levels; }

    public int[] landingLevels() { return landingLevels.clone(); }

    /** The angle at which the ramp arrives on the floor (where the road to the gate platform leaves the wall). */
    public double arrivalAngle() { return arrivalAngle; }

    public MacroParams params() { return params; }

    public long[] wfcStats() { return new long[] {wfc.restarts, wfc.fallbackUsed ? 1 : 0, wfc.collapses, wfc.backtracks, wfc.nanos}; }

    /** The column of the Chasm at (x, z), or null when it claims nothing there. */
    public Column column(int x, int z) {
        double dx = x - CX, dz = z - CZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r > REACH) return null;
        return new Column(x, z, dx, dz, r);
    }

    public final class Column {
        final int x, z;
        final double dx, dz, r, ang;
        final Spiral.Hits hits = new Spiral.Hits();
        private boolean hitsFound;

        Column(int x, int z, double dx, double dz, double r) {
            this.x = x;
            this.z = z;
            this.dx = dx;
            this.dz = dz;
            this.r = r;
            this.ang = r < 1e-9 ? 0.0 : Geo.angle(dx, dz);
        }

        /** How far the noise pushes a rib of rock out of the wall into the pit at this height, in blocks (0 at the rim and in the open). */
        private double rib(int y) {
            double fade = Geo.smoothstep(Y0 - 3, Y0 - 16, y);
            if (fade <= 0.0) return 0.0;
            double n = noise.fbm(x * 0.07, y * 0.07, z * 0.07, 2, 2.0, 0.5);
            return params.chasmNoise() * Math.max(0.0, n + 0.15) * fade;
        }

        /** The kind at height y, or null when the Chasm does not claim the voxel (the terrain shows through). */
        public BlockKind kind(int y) {
            if (y > Y0 + 2 || y < FLOOR_STAND - 8) return null;
            if (!hitsFound) {
                ramp.find(ang, r, hits);
                hitsFound = true;
            }
            // 1. the ramp
            for (int i = 0; i < hits.n; i++) {
                BlockKind k = rampKind(hits.step[i], y);
                if (k != null) return k;
            }
            double rw = rwall(y);
            // 2. the fence round the rim, with a gap where the road arrives
            if ((y == Y0 || y == Y0 + 1) && r >= R0 + 0.5 && r < R0 + 1.5 && Math.abs(ang - DOOR_ANGLE) > 0.14) {
                if (y == Y0) return BlockKind.RAIL_DARK;
                if (((int) Math.floor((ang + Math.PI) * R0)) % 9 == 0) return BlockKind.SOUL_LANTERN;
                return null;
            }
            // 3. the gate, its platform and the way from the ramp to it
            if (y >= FLOOR_TOP && y <= FLOOR_STAND + 6 && r < rwall(FLOOR_STAND)) {
                BlockKind k = gateKind(y);
                if (k != null) return k;
            }
            // the ribs only matter near the wall: further in it is open air whatever the noise says, further out the wall's skin is out of reach
            double maxRib = params.chasmNoise() * 1.15;
            double rb = y > FLOOR_TOP && y <= CAVE_TOP + 4 && r >= rw - maxRib && r < rw + 2.0 ? rib(y) : 0.0;
            double wallR = rw - rb;
            // 4. the floor of the pit
            if (y == FLOOR_TOP && r < rwall(FLOOR_STAND) - rb) return floorKind();
            // 5. the open air of the pit
            if (y > FLOOR_TOP && (r < wallR || r < rw - maxRib)) return y >= Y0 ? null : BlockKind.AIR;
            if (y < FLOOR_TOP || y > CAVE_TOP + 1) return null;
            // 6. the caverns and the skin of the wall
            BlockKind cave = caveKind(y, Math.max(0.0, r - rw));
            if (cave != null) return cave;
            return skinKind(y, r - wallR);
        }

        private BlockKind rampKind(int step, int y) {
            int part = ramp.part(step, r, y);
            switch (part) {
                case Spiral.WALK -> { return ramp.isSlab(step) ? BlockKind.RAMP_SLAB : BlockKind.RAMP_BLOCK; }
                case Spiral.SUPPORT -> { return BlockKind.COBBLED_DEEPSLATE; }
                case Spiral.CURB -> { return BlockKind.RAMP_BLOCK; }
                case Spiral.RAIL -> { return BlockKind.RAIL_DARK; }
                case Spiral.HEAD -> {
                    // the chain of the railing runs along the top of the posts; every twelfth step carries a soul lantern instead
                    if (ramp.inRailColumn(step, r) && y == ramp.surfaceRow(step) + 2) return step % 12 == 6 ? BlockKind.SOUL_LANTERN : BlockKind.RAIL_CHAIN;
                    return BlockKind.AIR;
                }
                default -> { return null; }
            }
        }

        private BlockKind gateKind(int y) {
            int gx = HubLayout.ABYSS_GATE_X - CX, gy = HubLayout.ABYSS_GATE_Y, gz = HubLayout.ABYSS_GATE_Z - CZ;
            int ddx = (int) Math.round(dx) - gx, ddz = (int) Math.round(dz) - gz;
            if (ddz == 0 && Math.abs(ddx) <= 1 && y >= gy - 2 && y <= gy + 1) return BlockKind.GATE;
            if (ddz == 0 && Math.abs(ddx) == 2 && y >= gy - 2 && y <= gy + 2) return BlockKind.ABYSS_FRAME;
            if (ddz == 0 && Math.abs(ddx) <= 2 && y == gy + 2) return BlockKind.ABYSS_FRAME;
            if (y == FLOOR_TOP) {
                if (r < PLATFORM_RADIUS) {
                    int ri = (int) r;
                    return ri == 5 || ri <= 1 ? BlockKind.PLATFORM_DARK_TRIM : BlockKind.PLATFORM_DARK;
                }
                double along = dx * arrivalCos + dz * arrivalSin, perp = -dx * arrivalSin + dz * arrivalCos;
                if (along >= PLATFORM_RADIUS - 1 && Math.abs(perp) < 1.5) return BlockKind.PLATFORM_DARK_TRIM;
            }
            // two soul lanterns on posts at the platform's edge, either side of the gate
            if (ddz == 0 && Math.abs(ddx) == 5 && y == FLOOR_STAND) return BlockKind.POST;
            if (ddz == 0 && Math.abs(ddx) == 5 && y == FLOOR_STAND + 1) return BlockKind.SOUL_LANTERN;
            return null;
        }

        private BlockKind floorKind() {
            double n = noise.noise(x * 0.2, z * 0.2);
            if (n > 0.62) return BlockKind.MOSS;
            if (n < -0.62) return BlockKind.SCULK;
            return ((x + z) & 3) == 0 ? BlockKind.POLISHED_DEEPSLATE : BlockKind.DEEPSLATE_TILES;
        }

        private BlockKind caveKind(int y, double cr) {
            if (y > CAVE_TOP) return null;
            int level = (CAVE_TOP - y) / cell;
            if (level >= levels) return null;
            int ring = (int) (cr / cell);
            if (ring >= RINGS) return null;
            int s = Geo.sector(ang, DOOR_ANGLE, sectors);
            int tile = wfc.tileAt(s, levels - 1 - level, ring);
            if (tile <= ChasmTiles.ROCK) return null;
            int yBase = CAVE_TOP - cell * (level + 1) + 1;
            int a = Math.min(cell - 1, (int) (Geo.sectorFraction(ang, DOOR_ANGLE, sectors) * cell));
            int c = Math.min(cell - 1, (int) (cr - ring * cell));
            TilePattern pat = ChasmTiles.PATTERNS[tile][ChasmTiles.flavour(seed, s, level, ring)];
            BlockKind k = pat.sample(a, y - yBase, c, cell, cell, cell);
            return k == BlockKind.NATIVE ? null : k;
        }

        /** The look of the rock face of the pit: strata by depth, moss high up, sculk low down (null = the plain terrain). */
        private BlockKind skinKind(int y, double depth) {
            if (depth >= 2.0 || y > CAVE_TOP) return null;
            double n = noise.noise(x * 0.13, y * 0.13, z * 0.13);
            if (y < -20) {
                if (n < -0.58) return BlockKind.SCULK;
                if (n > 0.35) return BlockKind.BASALT;
                return n > 0.0 ? BlockKind.DEEPSLATE : BlockKind.BLACKSTONE;
            }
            if (y < 30) {
                if (n < -0.62) return BlockKind.SCULK;
                if (n > 0.5) return BlockKind.COBBLED_DEEPSLATE;
                return BlockKind.DEEPSLATE;
            }
            if (n > 0.66) return BlockKind.MOSS;
            return null;
        }
    }
}
