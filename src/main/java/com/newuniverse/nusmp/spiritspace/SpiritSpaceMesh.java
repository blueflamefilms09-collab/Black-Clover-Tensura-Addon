package com.newuniverse.nusmp.spiritspace;

/**
 * Why: the bodies of the Spirit of Space are not geo files. The orb's core is a UV sphere and the wisp's body a tube along an undulating spine, each drawn as several shells of scrolling starfield (the
 * parallax of the "tear in reality"), and the portal is two flat bands and a disc. The client draws them (client.prop.SpiritSpaceDraw); everything that does not need a game class is here, pure, so
 * the exact quad count of every level of detail is a number a unit test checks against the draw code's own loops (the draw code reads these constants, it has none of its own), the unit sphere tables
 * are built once, and the spine and its profiles can be tested for the length and the width the doc promises.
 *
 * <p>Levels of detail: 0 = full, 1 = half, 2 = low. Units are blocks.</p>
 */
public final class SpiritSpaceMesh {
    private SpiritSpaceMesh() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, LEVELS = 3;

    // ================================================================================================ the orb
    /** The core sphere: rings (pole to pole) and segments (round) per level of detail. */
    public static final int[] SPHERE_RINGS = {8, 6, 4}, SPHERE_SEGS = {12, 10, 6};
    /** Shells drawn over the sphere per level: the dark base and the starfield layers (full: base + far + mid + near). */
    public static final int[] ORB_LAYERS = {4, 3, 2};
    /** The core's radius, and the radius of each shell as a factor of it (base, far, mid, near): the shells sit a little above the base so they never z-fight with it. */
    public static final float CORE_RADIUS = 0.29f;
    public static final float[] SHELL = {1.0f, 1.012f, 1.03f, 1.05f};
    /** The quads of the orb's rift ring (the geo files, spirit_space_orb[_lod1|_lod2], counted by SpiritSpaceAssetsTest) and of the halo sprite behind the orb (levels 0 and 1 only). */
    public static final int[] ORB_GEO = {105, 60, 18}, ORB_HALO = {1, 1, 0};

    public static int sphereQuads(int lod) { return SPHERE_RINGS[lod] * SPHERE_SEGS[lod]; }

    public static int orbQuads(int lod) { return sphereQuads(lod) * ORB_LAYERS[lod] + ORB_GEO[lod] + ORB_HALO[lod]; }

    // ================================================================================================ the wisp
    /** The tube: segments along the spine and sides round it per level. */
    public static final int[] TUBE_SEGS = {24, 16, 8}, TUBE_SIDES = {8, 6, 4};
    /** Shells of the tube per level: the dark body, then the starfield layers (full: body + far + mid). */
    public static final int[] WISP_LAYERS = {3, 2, 1};
    /** The radius of each shell as a factor of the body's (body, far, mid). */
    public static final float[] WISP_SHELL = {1.0f, 1.05f, 1.10f};
    /** The cosmic fins along the spine: two side strips and the dorsal crest, each one quad per spine segment (level 2 has none). */
    public static final int[] FIN_STRIPS = {3, 3, 0};
    /** The quads of the wisp's head (geo files spirit_space_wisp[_lod1|_lod2]) and of its glow-only model (spirit_space_wisp_fx, drawn on levels 0 and 1). */
    public static final int[] WISP_GEO = {115, 91, 39}, WISP_FX = {37, 37, 0};
    /** The body's length (tail to neck), its largest radius and the radius at the neck. */
    public static final float WISP_LENGTH = 3.2f, WISP_RADIUS = 0.21f, WISP_NECK = 0.12f;

    public static int tubeQuads(int lod) { return TUBE_SEGS[lod] * TUBE_SIDES[lod]; }

    public static int finQuads(int lod) { return TUBE_SEGS[lod] * FIN_STRIPS[lod]; }

    public static int wispQuads(int lod) { return tubeQuads(lod) * WISP_LAYERS[lod] + finQuads(lod) + WISP_GEO[lod] + WISP_FX[lod]; }

    // ================================================================================================ the portal
    /** One ring of a portal: two bands of {@link #PORTAL_SEGS} quads and two starfield discs (a dark window with stars, scrolling and turning against each other) of {@link #DISC_RINGS} x {@link #PORTAL_SEGS}/2 quads each. */
    public static final int PORTAL_SEGS = 24, DISC_RINGS = 3;
    public static final float PORTAL_RX = 0.8f, PORTAL_RY = 1.2f;

    public static int portalQuads() { return 2 * PORTAL_SEGS + 2 * DISC_RINGS * (PORTAL_SEGS / 2); }

    // ================================================================================================ the unit sphere tables (built once)
    /** For each level: the grid of (rings + 1) x (segs + 1) unit vectors, x y z per vertex, and the u v of each vertex. */
    private static final float[][] SPHERE_XYZ = new float[LEVELS][], SPHERE_UV = new float[LEVELS][];

    static {
        for (int l = 0; l < LEVELS; l++) {
            int rings = SPHERE_RINGS[l], segs = SPHERE_SEGS[l];
            float[] xyz = new float[(rings + 1) * (segs + 1) * 3], uv = new float[(rings + 1) * (segs + 1) * 2];
            for (int i = 0; i <= rings; i++) {
                double t = Math.PI * i / rings;
                for (int j = 0; j <= segs; j++) {
                    double f = Math.PI * 2 * j / segs;
                    int k = i * (segs + 1) + j;
                    xyz[k * 3] = (float) (Math.sin(t) * Math.cos(f));
                    xyz[k * 3 + 1] = (float) Math.cos(t);
                    xyz[k * 3 + 2] = (float) (Math.sin(t) * Math.sin(f));
                    uv[k * 2] = j / (float) segs;
                    uv[k * 2 + 1] = i / (float) rings;
                }
            }
            SPHERE_XYZ[l] = xyz;
            SPHERE_UV[l] = uv;
        }
    }

    /** The unit vectors of a level's sphere grid (read only; index (ring * (segs + 1) + seg) * 3). */
    public static float[] sphereXyz(int lod) { return SPHERE_XYZ[lod]; }

    public static float[] sphereUv(int lod) { return SPHERE_UV[lod]; }

    // ================================================================================================ the spine and its profiles
    /** The wisp's body radius at {@code t} (0 = the tail's tip, 1 = the neck): a thin tail, the thickest at three quarters of the way, a slimmer neck. */
    public static float bodyRadius(float t) {
        t = Math.max(0f, Math.min(1f, t));
        if (t < 0.75f) {
            double k = Math.sin(t / 0.75 * Math.PI / 2);
            return (float) (0.02 + (WISP_RADIUS - 0.02) * Math.pow(k, 1.1));
        }
        double u = (t - 0.75) / 0.25;
        return (float) (WISP_RADIUS - (WISP_RADIUS - WISP_NECK) * Math.pow(u, 1.5));
    }

    /** The width of the side fins at {@code t}: broad over the body and a fan at the tail. */
    public static float finWidth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        double body = 0.26 * Math.sin(Math.PI * t);
        double fan = 0.30 * Math.exp(-Math.pow((t - 0.04) / 0.07, 2));
        return (float) (body + fan);
    }

    /** The height of the dorsal crest at segment {@code i} of {@code n}: a sawtooth, tall over the body. */
    public static float crestHeight(int i, int n) {
        float t = i / (float) Math.max(1, n);
        float saw = (i & 1) == 0 ? 1.0f : 0.55f;
        return 0.05f + 0.16f * saw * (float) Math.sin(Math.PI * Math.min(1f, t * 1.05f));
    }

    /**
     * The spine at {@code t} (0 tail .. 1 neck) at {@code age} ticks, in the wisp's own space (+z the way it faces, y up): written into {@code o} (x, y, z). It undulates side to side and up and down with a
     * wave that runs from the head to the tail, more at the tail than at the head, and arches over the owner a little.
     */
    public static void spine(float[] o, float t, float age) { spine(o, t, age, 0f); }

    /** The same with a coil from 0 (the free line) to 1 (a corkscrew round the wisp's own axis, the head out in front): the pose of a wisp gathering itself. */
    public static void spine(float[] o, float t, float age, float coil) {
        double s = age / 20.0, phase = s * 2.4 - t * 5.0;
        double calm = Math.pow(1.0 - t, 0.7);
        double x = 0.34 * calm * Math.sin(phase);
        double y = 0.24 * Math.pow(1.0 - t, 0.5) * Math.sin(phase * 0.8 + 1.3) + 0.30 * Math.sin(Math.PI * t) + 0.05 * Math.sin(s * 1.3);
        double z = (t - 0.5) * WISP_LENGTH;
        if (coil > 0f) {
            double a = (1.0 - t) * Math.PI * 2.6 + s * 0.8, r = 0.85 * (0.35 + 0.65 * (1.0 - t));
            double hx = r * Math.sin(a), hy = 0.45 + r * Math.cos(a) * 0.9, hz = (t - 0.5) * 1.3;
            x += (hx - x) * coil;
            y += (hy - y) * coil;
            z += (hz - z) * coil;
        }
        o[0] = (float) x;
        o[1] = (float) y;
        o[2] = (float) z;
    }
}
