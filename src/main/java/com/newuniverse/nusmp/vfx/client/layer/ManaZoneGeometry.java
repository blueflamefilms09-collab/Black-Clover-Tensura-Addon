package com.newuniverse.nusmp.vfx.client.layer;

/**
 * The cached geometry and the small pure helpers of {@link ManaZoneLayer} (no Minecraft types, so the headless preview and the JUnit tests
 * compile it as it is). The sphere shell is ONE unit-sphere template per level of detail, built once when the class loads: each frame the
 * layer only scales it by the zone's radius and moves it to the zone's centre ("an instanced shell"), with no trigonometry and no allocation.
 * The same goes for the ring segments. Three levels of detail are chosen by the camera's distance; the shell at full detail is 96 quads
 * (384 vertices), the vertex buffer's own per-effect cap is 400.
 */
public final class ManaZoneGeometry {
    private ManaZoneGeometry() {}

    // ---------------------------------------------------------------- the three levels of detail
    /** Longitudes, latitudes and ring segments per level (0 = full detail). */
    static final int[] LON = {12, 8, 6}, LAT = {8, 6, 4}, RING = {32, 20, 12};
    /** Full-detail numbers (the budget test reads these). */
    public static final int SHELL_LON = LON[0], SHELL_LAT = LAT[0], RING_SEGMENTS = RING[0];
    /** Rising motes and converging streaks at full detail; the ground instance's budget is 5 + 2 + 2 rings + MOTES + STREAKS quads: 392 vertices. */
    public static final int MOTES = 20, STREAKS = 10;

    /** Unit-sphere vertices: for level L, vertex (i, j) at ((i * (LON + 1) + j) * 3), i = latitude row 0..LAT (top to bottom), j = 0..LON. */
    static final float[][] SHELL_POS = new float[3][];
    /** The unit normal of each quad's centre: (i * LON + j) * 3. */
    static final float[][] SHELL_NRM = new float[3][];
    static final float[][] RING_COS = new float[3][], RING_SIN = new float[3][];

    static {
        for (int l = 0; l < 3; l++) {
            int lon = LON[l], lat = LAT[l];
            float[] pos = new float[(lat + 1) * (lon + 1) * 3];
            for (int i = 0; i <= lat; i++) {
                double phi = Math.PI * i / lat - Math.PI / 2;           // -90 (bottom) .. +90 (top); row 0 is the bottom so v runs up
                double y = Math.sin(phi), r = Math.cos(phi);
                for (int j = 0; j <= lon; j++) {
                    double th = 2 * Math.PI * j / lon;
                    int k = (i * (lon + 1) + j) * 3;
                    pos[k] = (float) (Math.cos(th) * r);
                    pos[k + 1] = (float) y;
                    pos[k + 2] = (float) (Math.sin(th) * r);
                }
            }
            float[] nrm = new float[lat * lon * 3];
            for (int i = 0; i < lat; i++) {
                for (int j = 0; j < lon; j++) {
                    double phi = Math.PI * (i + 0.5) / lat - Math.PI / 2, th = 2 * Math.PI * (j + 0.5) / lon;
                    int k = (i * lon + j) * 3;
                    nrm[k] = (float) (Math.cos(th) * Math.cos(phi));
                    nrm[k + 1] = (float) Math.sin(phi);
                    nrm[k + 2] = (float) (Math.sin(th) * Math.cos(phi));
                }
            }
            float[] cs = new float[RING[l] + 1], sn = new float[RING[l] + 1];
            for (int s = 0; s <= RING[l]; s++) {
                cs[s] = (float) Math.cos(2 * Math.PI * s / RING[l]);
                sn[s] = (float) Math.sin(2 * Math.PI * s / RING[l]);
            }
            SHELL_POS[l] = pos;
            SHELL_NRM[l] = nrm;
            RING_COS[l] = cs;
            RING_SIN[l] = sn;
        }
    }

    // ---------------------------------------------------------------- level of detail (parity with mana.zone.ManaZoneMath.lodSegments, tested)
    /**
     * The level (0 full, 1, 2) for a camera {@code camDistance} blocks from the zone's centre, or -1 when the zone is too far to draw. A
     * detail setting under 1 (Decreased particles) drops one level.
     */
    public static int level(double camDistance, double radius, float detail) {
        double r = Math.max(1.0, radius);
        if (camDistance > r * 8 + 96) return -1;
        int l = camDistance <= r ? 0 : camDistance <= r * 2 ? 1 : 2;
        if (detail < 0.75f) l = Math.min(2, l + 1);
        return l;
    }

    /** Ease-out cubic (parity with ManaZoneMath.ease). */
    public static float ease(float x) {
        float c = x < 0 ? 0 : Math.min(x, 1);
        float k = 1 - c;
        return 1 - k * k * k;
    }

    // ---------------------------------------------------------------- deterministic hash (no allocation)
    /** A value in [0, 1) from a seed and a counter. */
    public static float hash01(long seed, int n) {
        long z = seed + (n + 1L) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) / (float) (1 << 24);
    }

    /** A hash of a world cell, for the procedural scenery of the Grand Magic Zones. */
    public static long cellSeed(long seed, int cx, int cy, int cz) {
        long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cy * 0xC2B2AE3D27D4EB4FL) ^ (cz * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    // ---------------------------------------------------------------- protocol constants (parity with GrandZoneRegion.Theme / Effect, tested)
    public static final int THEME_SEABED = 0, THEME_FOREST = 1, THEME_VOLCANO = 2, THEME_GRAVITO = 3, THEME_HAVEN = 4, THEME_WILD = 5;
    public static final int FX_GRAVITY = 1, FX_BOULDERS = 1 << 1, FX_CURRENTS = 1 << 2, FX_MAGMA = 1 << 3, FX_STORM = 1 << 4, FX_WITCH = 1 << 5, FX_CALM = 1 << 6, FX_PRESSURE = 1 << 7;
    /** Styles of the caster's magic (parity with ManaZoneStyle). */
    public static final int STYLE_ARCANE = 0, STYLE_FLAME = 1, STYLE_WATER = 2, STYLE_WIND = 3, STYLE_EARTH = 4, STYLE_LIGHT = 5, STYLE_DARK = 6, STYLE_SPACE = 7, STYLE_TIME = 8;

    /** The accent colour of a style's motes: the zone colour pulled towards the element (ARGB, alpha kept). */
    public static int accent(int style, int base) {
        int target = switch (style) {
            case STYLE_FLAME -> 0xFFFFB347;
            case STYLE_WATER -> 0xFF7FE3FF;
            case STYLE_WIND -> 0xFFC8FFD8;
            case STYLE_EARTH -> 0xFFE0C07A;
            case STYLE_LIGHT -> 0xFFFFF4B0;
            case STYLE_DARK -> 0xFFB48CFF;
            case STYLE_SPACE -> 0xFFB0A0FF;
            case STYLE_TIME -> 0xFFFFD86B;
            default -> base;
        };
        return mix(base, target, style == STYLE_ARCANE ? 0f : 0.45f);
    }

    static int mix(int a, int b, float t) {
        int aa = (int) ((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (aa << 24) | (r << 16) | (g << 8) | bl;
    }

    // ---------------------------------------------------------------- zones that have ended on the server (the client drops their drawing at once)
    private static final long[] ENDED = new long[16];
    private static int endedAt;

    /** Marks a zone serial as over: its running effects draw nothing from the next frame. A ring of 16, no allocation. */
    public static synchronized void markEnded(long serial) {
        for (long s : ENDED) if (s == serial) return;
        ENDED[endedAt] = serial;
        endedAt = (endedAt + 1) % ENDED.length;
    }

    public static synchronized boolean isEnded(long serial) {
        if (serial == 0) return false;
        for (long s : ENDED) if (s == serial) return true;
        return false;
    }

    public static synchronized void clearEnded() { java.util.Arrays.fill(ENDED, 0L); endedAt = 0; }
}
