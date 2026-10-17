package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

/**
 * Why: this is the HOLY SPIRE, the "hollow soaring spire" of the brief, as a pure function from a world position to a {@link BlockKind}. It is a tower of white
 * stone whose outside tapers (smoothstep) from {@link HubLayout#SPIRE_BASE_RADIUS} to {@link HubLayout#SPIRE_TOP_RADIUS} and is roughened by 3D simplex noise,
 * whose inside is a hollow shaft, and whose walkway is a continuous spiral stair hugging the inner wall from the front door up to a crown platform that carries
 * the Holy gate. Between the landings the wall swells into GALLERIES, rings of trial chambers assembled by the 3D Wave Function Collapse ({@link SpireTiles}).
 *
 * <p>Order of authority (a voxel takes the first rule that claims it), which is the design: (1) the STAIR and its carved headroom, because the road to the gate
 * must exist whatever the noise or the tiles do; (2) the CROWN (platform, bridge, rails, the gate and its frame); (3) the GALLERY tile that covers the voxel;
 * (4) the plain SHELL with its windows, gold bands, battlements and the eight pinnacles of the crown; (5) the empty shaft (with its one great chandelier on the axis). Everything is derived from {@link HubLayout}, the seed and the
 * {@link MacroParams}, nothing is stored per voxel, and the model is immutable once built, so the chunk generator's threads can share it.
 *
 * <p>The galleries: one every {@value #GALLERY_STEP} blocks of height, at the landing level of the stair, two cells high, {@value #DEPTH} blocks deep from the
 * inner wall. Each gallery is its own small Wave Function Collapse problem whose ring is cut into as many sectors as fit round the shaft at that height and is
 * ROTATED so that sector 0 is centred on the landing; that cell is pre-collapsed to the doorway tile, so the door onto the landing is exactly in front of the
 * landing whatever the solver chooses for the rest.
 */
public final class SpireModel {
    public static final int CX = HubLayout.SPIRE_X, CZ = HubLayout.SPIRE_Z, Y0 = HubLayout.SURFACE_Y, HEIGHT = HubLayout.SPIRE_HEIGHT;
    /** The first y above the tower. */
    public static final int TOP = Y0 + HEIGHT;
    /** Thickness of the plain shell in blocks. */
    static final double WALL = 3.5;
    static final int WIDTH = 3;
    static final int HEADROOM = 5;
    /** The spacing of the landings (and so of the galleries and the gold bands), and how many galleries there are. */
    public static final int GALLERY_STEP = 24, GALLERIES = 6;
    /** How far the cells of a gallery reach from the inner wall of the shaft. */
    public static final int DEPTH = 8;
    /** The top block row of the crown platform (the standing level of the Holy gate is one above it). */
    public static final int PLATFORM_TOP = HubLayout.HOLY_GATE_Y - 3;
    public static final int DISC_RADIUS = 9;
    /** The lowest block of the great chandelier on the axis of the shaft. */
    static final int CHANDELIER_FOOT = Y0 + 20;
    /** The angle of the front door (west). */
    public static final double DOOR_ANGLE = Math.PI;
    /** The cutoff radius beyond which the Spire claims no column. */
    public static final double REACH = HubLayout.SPIRE_BASE_RADIUS + 12;

    private static final int WINDOWS = 16, CROWN_OPENINGS = 12, TEETH = 48;
    /** Eight slender pinnacles rise from the parapet, this many blocks above the top of the tower, at this distance from the axis. */
    static final int PINNACLE_HEIGHT = 9;
    private static final double PINNACLE_RADIUS = HubLayout.SPIRE_TOP_RADIUS - 2.0;

    /** What one gallery looks like, for the preview and the statistics. */
    public record Gallery(int index, int standY, int sectors, double centre, Wfc3D.Result wfc) {}

    private final MacroParams params;
    private final Simplex3D noise;
    private final Spiral stair;
    private final Gallery[] galleries;
    private final double arrivalAngle, arrivalCos, arrivalSin;
    private final int cell;

    SpireModel(long seed, MacroParams params, Simplex3D noise) {
        this.params = params;
        this.noise = noise;
        this.cell = params.cell();
        int qEnd = 2 * (HubLayout.HOLY_GATE_Y - 2 - Y0);
        int[] landings = new int[GALLERIES];
        for (int g = 0; g < GALLERIES; g++) landings[g] = 2 * GALLERY_STEP * g;
        HelixPath path = Spiral.build(1, DOOR_ANGLE, qEnd, q -> radiusOfStairAt(q), landings, 10, 8, WIDTH / 2.0);
        this.stair = new Spiral(path, WIDTH, true, Y0, 4, HEADROOM);
        this.arrivalAngle = stair.landingMid(qEnd);
        this.arrivalCos = StrictMath.cos(arrivalAngle);
        this.arrivalSin = StrictMath.sin(arrivalAngle);
        this.galleries = new Gallery[GALLERIES];
        for (int g = 0; g < GALLERIES; g++) {
            int standY = Y0 + GALLERY_STEP * g;
            double centre = g == 0 ? DOOR_ANGLE : stair.landingMid(2 * GALLERY_STEP * g);
            int n = Math.max(8, (int) Math.round(Geo.TWO_PI * rin(standY) / cell));
            Wfc3D.Problem p = SpireTiles.problem(n, g == 0);
            long s = seed * 0x9E3779B97F4A7C15L + 0x51ED270B + g * 0xD1B54A32D192ED03L;
            Wfc3D.Result res = Wfc3D.solve(SpireTiles.TILESET, p, s, params.wfcTries(), SpireTiles.PIER);
            galleries[g] = new Gallery(g, standY, n, centre, res);
        }
    }

    // ---- geometry of the tower

    /** The outside radius of the nominal cone at height y. */
    public static double outer(double y) {
        double t = Geo.clamp01((y - Y0) / HEIGHT);
        return Geo.lerp(HubLayout.SPIRE_BASE_RADIUS, HubLayout.SPIRE_TOP_RADIUS, Geo.smoothstep(0.0, 1.0, t));
    }

    /** The radius of the inner face of the wall at height y. */
    public static double rin(double y) { return outer(y) - WALL; }

    /** Centre radius of the stair's walkway at half step q: it hugs the inner wall at the height of the walking surface's block row. */
    private static double radiusOfStairAt(int q) {
        int row = Math.floorDiv(2 * Y0 + q - 1, 2);
        return rin(row) - WIDTH / 2.0;
    }

    public Spiral stair() { return stair; }

    public Gallery gallery(int g) { return galleries[g]; }

    public int galleryCount() { return galleries.length; }

    /** The angle at which the stair arrives on the crown (where the bridge to the gate leaves the wall). */
    public double arrivalAngle() { return arrivalAngle; }

    public MacroParams params() { return params; }

    /** Total statistics of the Wave Function Collapse of all galleries: {restarts, fallbacks, collapses, backtracks, nanos}. */
    public long[] wfcStats() {
        long[] s = new long[5];
        for (Gallery g : galleries) {
            s[0] += g.wfc().restarts;
            s[1] += g.wfc().fallbackUsed ? 1 : 0;
            s[2] += g.wfc().collapses;
            s[3] += g.wfc().backtracks;
            s[4] += g.wfc().nanos;
        }
        return s;
    }

    // ---- the columns

    /** The column of the Spire at (x, z), or null when the Spire claims nothing there. */
    public Column column(int x, int z) {
        double dx = x - CX, dz = z - CZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r > REACH) return null;
        return new Column(x, z, dx, dz, r);
    }

    /** One column of the Spire; {@link #kind} answers for every height. Cheap to create, not thread-safe (one per call site). */
    public final class Column {
        final int x, z;
        final double dx, dz, r, ang;
        final Spiral.Hits hits = new Spiral.Hits();
        private boolean hitsFound;

        /** Is this column one of the eight pinnacles? */
        final boolean pinnacle;

        Column(int x, int z, double dx, double dz, double r) {
            this.x = x;
            this.z = z;
            this.dx = dx;
            this.dz = dz;
            this.r = r;
            this.ang = r < 1e-9 ? 0.0 : Geo.angle(dx, dz);
            boolean p = false;
            if (Math.abs(r - PINNACLE_RADIUS) < 1.5) {
                for (int k = 0; k < 8 && !p; k++) {
                    double a = (k + 0.5) * Math.PI / 4.0;
                    p = x == CX + (int) Math.round(PINNACLE_RADIUS * StrictMath.cos(a)) && z == CZ + (int) Math.round(PINNACLE_RADIUS * StrictMath.sin(a));
                }
            }
            this.pinnacle = p;
        }

        /** A quartz pillar with a cap of gold trim and a lantern on top, from the cornice of the parapet up. */
        private BlockKind pinnacleKind(int y) {
            if (y < TOP + PINNACLE_HEIGHT - 1) return BlockKind.QUARTZ_PILLAR;
            return y == TOP + PINNACLE_HEIGHT - 1 ? BlockKind.GOLD_TRIM : BlockKind.LANTERN;
        }

        /** The kind at height y, or null when the Spire does not claim the voxel (the terrain shows through). */
        public BlockKind kind(int y) {
            if (y >= TOP) return y < TOP + PINNACLE_HEIGHT + 1 && pinnacle ? pinnacleKind(y) : null;
            if (y < Y0 - 1) return null;
            if (!hitsFound) {
                stair.find(ang, r, hits);
                hitsFound = true;
            }
            // 1. the stair and its headroom
            for (int i = 0; i < hits.n; i++) {
                BlockKind k = stairKind(hits.step[i], y);
                if (k != null) return k;
            }
            double rinY = rin(y);
            // 2. the crown
            if (y >= PLATFORM_TOP - 2) {
                BlockKind k = crownKind(y, rinY);
                if (k != null) return k;
            }
            // 3. the great chandelier: a chain on the axis hangs from the underside of the crown platform, with a lantern every twenty blocks and one at its foot
            if (x == CX && z == CZ && y >= CHANDELIER_FOOT && y < PLATFORM_TOP - 2) return (y - CHANDELIER_FOOT) % 20 == 0 ? BlockKind.LANTERN_HANGING : BlockKind.CHAIN_Y;
            // 4. the ground hall floor
            if (y == Y0 - 1 && r < rinY) return hallFloor();
            // 5. the gallery cells
            int rel = y - (Y0 - 1);
            int gi = rel / GALLERY_STEP;
            if (gi < GALLERIES && rel % GALLERY_STEP < 2 * cell) {
                double c = r - rinY;
                if (c < 0) return BlockKind.AIR;
                if (c < DEPTH) return galleryKind(gi, rel % GALLERY_STEP, c, y);
                return null;
            }
            // 6. the shell and the shaft
            return shellKind(y, rinY);
        }

        private BlockKind stairKind(int step, int y) {
            int part = stair.part(step, r, y);
            switch (part) {
                case Spiral.WALK -> { return stair.isSlab(step) ? BlockKind.SMOOTH_QUARTZ_SLAB : BlockKind.SMOOTH_QUARTZ; }
                case Spiral.SUPPORT -> { return BlockKind.QUARTZ_BRICKS; }
                case Spiral.CURB -> { return BlockKind.QUARTZ_BRICKS; }
                case Spiral.RAIL -> { return BlockKind.RAIL_LIGHT; }
                case Spiral.HEAD -> {
                    // a lantern on the post of the railing every eighth step lights the climb
                    if (y == stair.surfaceRow(step) + 2 && step % 8 == 4 && stair.inRailColumn(step, r)) return BlockKind.LANTERN;
                    return BlockKind.AIR;
                }
                default -> { return null; }
            }
        }

        private BlockKind hallFloor() {
            int ri = (int) r;
            if (ri < 3) return BlockKind.CHISELED_QUARTZ;
            if (ri == 3 || ri == 14 || ri == 25) return BlockKind.GOLD_TRIM;
            double f = Geo.sectorFraction(ang, 0.0, 32);
            boolean odd = ((ri / 3) + (int) (f * 2.0)) % 2 == 0;
            return odd ? BlockKind.SMOOTH_QUARTZ : BlockKind.QUARTZ_BRICKS;
        }

        private BlockKind wallMaterial(int y) {
            double n = noise.noise(x * 0.45, y * 0.45, z * 0.45);
            if (n > 0.38) return BlockKind.CALCITE;
            if (n < -0.38) return BlockKind.SMOOTH_QUARTZ;
            return BlockKind.QUARTZ_BRICKS;
        }

        private BlockKind galleryKind(int gi, int row, double c, int y) {
            Gallery g = galleries[gi];
            int tier = row / cell, b = row % cell;
            double f = Geo.sectorFraction(ang, g.centre(), g.sectors());
            int a = Math.min(cell - 1, (int) (f * cell));
            int s = Geo.sector(ang, g.centre(), g.sectors());
            int tile = g.wfc().tileAt(s, tier, 0);
            if (tile < 0) tile = SpireTiles.PIER;
            BlockKind k = SpireTiles.PATTERNS[tile].sample(a, b, (int) c, cell, cell, DEPTH);
            return k == BlockKind.NATIVE ? wallMaterial(y) : k;
        }

        private BlockKind shellKind(int y, double rinY) {
            if (r < rinY) return BlockKind.AIR;
            double n = noise.fbm(x * 0.06, y * 0.06, z * 0.06, 2, 2.0, 0.5);
            double rout = rinY + WALL + params.spireNoise() * n;
            if (r >= rout) return null;
            int m = Math.floorMod(y - Y0, GALLERY_STEP);
            int band = Math.floorDiv(y - Y0, GALLERY_STEP);
            if (y >= PLATFORM_TOP + 1) return crownWall(y, rinY);
            if (m == 22) return BlockKind.GOLD_TRIM;
            if (m >= 14 && m <= 20 && y < PLATFORM_TOP - 2) {
                double f = Geo.sectorFraction(ang, band * Math.PI / WINDOWS, WINDOWS);
                double d = Math.abs((f - 0.5) * Geo.TWO_PI * r / WINDOWS);
                double half = m >= 19 ? 0.55 : 1.05;
                if (d < half) return BlockKind.GLASS;
            }
            return wallMaterial(y);
        }

        private BlockKind crownWall(int y, double rinY) {
            if (y == TOP - 1) return BlockKind.GOLD_TRIM;
            if (y == PLATFORM_TOP + 2) return BlockKind.GOLD_TRIM;
            if (y == TOP - 2) {
                double f = Geo.sectorFraction(ang, 0.0, TEETH);
                return f < 0.5 ? wallMaterial(y) : BlockKind.AIR;
            }
            if (y >= PLATFORM_TOP + 3) {
                int m = y - (PLATFORM_TOP + 3);
                double f = Geo.sectorFraction(ang, Math.PI / CROWN_OPENINGS, CROWN_OPENINGS);
                double d = Math.abs((f - 0.5) * Geo.TWO_PI * r / CROWN_OPENINGS);
                double half = m >= 4 ? 0.7 : 1.6;
                if (m <= 4 && d < half) return BlockKind.AIR;
            }
            return wallMaterial(y);
        }

        /** The crown platform, its bridge, the rails and the Holy gate. */
        private BlockKind crownKind(int y, double rinY) {
            // the Holy gate: a 3 wide, 4 high plane of gate blocks with a gold frame, standing on the platform at the axis
            int gx = HubLayout.HOLY_GATE_X - CX, gy = HubLayout.HOLY_GATE_Y, gz = HubLayout.HOLY_GATE_Z - CZ;
            int ddx = (int) Math.round(dx) - gx, ddz = (int) Math.round(dz) - gz;
            if (ddz == 0 && Math.abs(ddx) <= 1 && y >= gy - 2 && y <= gy + 1) return BlockKind.GATE;
            if (ddz == 0 && Math.abs(ddx) == 2 && y >= gy - 2 && y <= gy + 2) return BlockKind.GATE_FRAME;
            if (ddz == 0 && Math.abs(ddx) <= 2 && y == gy + 2) return BlockKind.GATE_FRAME;

            double along = dx * arrivalCos + dz * arrivalSin, perp = -dx * arrivalSin + dz * arrivalCos;
            boolean bridge = along >= 0 && Math.abs(perp) < 2.5 && r < rinY + 0.5;
            boolean disc = r < DISC_RADIUS;
            if (y >= PLATFORM_TOP - 2 && y <= PLATFORM_TOP) {
                if (!disc && !bridge) return null;
                if (r >= rinY) return null;
                if (y < PLATFORM_TOP) return BlockKind.QUARTZ_BRICKS;
                return platformTop(disc);
            }
            if (y == PLATFORM_TOP + 1 && r < rinY) {
                boolean bridgeEdge = bridge && Math.abs(perp) >= 1.5;
                boolean discRim = disc && r >= DISC_RADIUS - 1 && !(along >= 0 && Math.abs(perp) < 2.5);
                if (bridgeEdge && !disc) return BlockKind.RAIL_LIGHT;
                if (discRim) return BlockKind.RAIL_LIGHT;
            }
            return null;
        }

        private BlockKind platformTop(boolean disc) {
            if (disc) {
                int ri = (int) r;
                if (ri <= 1) return BlockKind.CHISELED_QUARTZ;
                if (ri == 4 || ri == 8) return BlockKind.GOLD_TRIM;
                return ((int) (Geo.sectorFraction(ang, 0.0, 16) * 2.0) + ri) % 2 == 0 ? BlockKind.SMOOTH_QUARTZ : BlockKind.QUARTZ_BRICKS;
            }
            return BlockKind.SMOOTH_QUARTZ;
        }
    }
}
