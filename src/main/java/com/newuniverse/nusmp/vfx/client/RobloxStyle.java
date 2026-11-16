package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.RobloxMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The "Roblox style" toolkit (docs/vfx_roblox_pass.md): a handful of real meshes instead of piles of camera-facing sprites. Pure geometry on top of {@link VfxVertexBuffer#quad}, client only.
 * <ul>
 *   <li>{@link #swoosh}: a ribbon through a path with a tapering width, a colour ramp and a scrolling V: the swoosh, the slash, the tapered trail, the beam shell (texture {@code rbx_swoosh} / {@code rbx_bolt}).</li>
 *   <li>{@link #ring} / {@link #shock}: a flat ring band, and 2-3 of them layered with offset timing racing out with a quartic ease (texture {@code rbx_ring}).</li>
 *   <li>{@link #cone}: a frustum with a scrolling, optionally twisting texture: tornado, vortex, crown, spike ({@code rbx_swirl}, {@code rbx_rock}).</li>
 *   <li>{@link #flipbook}: one camera-facing quad of a 4x4 sheet, cross-faded to the next frame ({@code rbx_flip_burst}, {@code rbx_flip_fire}).</li>
 *   <li>{@link #coreShell}: a tinted alpha shell, a wide additive halo and a white-hot core ({@code rbx_glow}).</li>
 *   <li>{@link #decal}: a flat ground ring decal ({@code rbx_decal_ring}); {@link #streak}: a stretched spark ({@code rbx_streak}).</li>
 * </ul>
 * <b>Cost (rule 11).</b> Every call writes a fixed number of quads ({@link RobloxMath#swooshQuads} ... : the effect's worst case is the sum, pinned by RobloxStyleBudgetTest) and goes through
 * {@link VfxVertexBuffer#quad}, which refuses a quad past the effect's vertex budget, so nothing here can pass the engine's 100 quads per effect; the segment counts the callers pass come from {@code ctx.seg}
 * (the particle setting times the distance level of detail of {@link VfxManager}, which also culls far effects). Nothing is allocated per call: the vectors, the path and the basis are static scratch of the
 * render thread (one thread draws every effect, one at a time, as in the other layers' scratch). All coordinates are camera-relative (what the buffer wants). Nothing is kept between frames, so nothing needs
 * cleaning on logout, death or a dimension change.
 */
public final class RobloxStyle {
    private RobloxStyle() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(RobloxMath.fileName(n)); }

    public static final ResourceLocation SWOOSH = t("swoosh"), BOLT = t("bolt"), SWIRL = t("swirl"), RING = t("ring"), DECAL = t("decal_ring"), GLOW = t("glow"), STREAK = t("streak"),
            FLIP_BURST = t("flip_burst"), FLIP_FIRE = t("flip_fire"), ROCK = t("rock");

    public static final int MAX_PATH = 18;
    /** The path a ribbon is drawn through: x, y, z per point, tail first. Filled by {@link #line}, {@link #jagged}, {@link #spiral} (or by hand). */
    public static final float[] PATH = new float[MAX_PATH * 3];

    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), P2 = new Vector3f(), P3 = new Vector3f(), C = new Vector3f();
    /** The basis of the plane a ring / cone stands on (set by {@link #basis}). */
    private static final float[] E1 = new float[3], E2 = new float[3], NA = new float[3];
    private static final float[] CELL = new float[4];

    private static void q(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                          float u0, float v0, float u1, float v1, int c0, int c1) {
        P0.set(x0, y0, z0);
        P1.set(x1, y1, z1);
        P2.set(x2, y2, z2);
        P3.set(x3, y3, z3);
        buf.quad(tex, blend, P0, P1, P2, P3, u0, v0, u1, v1, c0, c1);
    }

    /** An orthonormal basis of the plane whose normal is (ax, ay, az): E1, E2 (the same rule as VfxPose.facing) and the unit normal NA. */
    private static void basis(float ax, float ay, float az) {
        float l = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (l < 1e-6f) { ax = 0; ay = 1; az = 0; l = 1; }
        ax /= l; ay /= l; az /= l;
        NA[0] = ax; NA[1] = ay; NA[2] = az;
        float rx = 0, ry = 1, rz = 0;
        if (Math.abs(ay) > 0.95f) { rx = 1; ry = 0; }
        float ex = ry * az - rz * ay, ey = rz * ax - rx * az, ez = rx * ay - ry * ax;          // ref x n
        float el = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
        ex /= el; ey /= el; ez /= el;
        E1[0] = ex; E1[1] = ey; E1[2] = ez;
        float fx = ay * ez - az * ey, fy = az * ex - ax * ez, fz = ax * ey - ay * ex;          // n x e1
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        E2[0] = fx / fl; E2[1] = fy / fl; E2[2] = fz / fl;
    }

    // ------------------------------------------------------------------ paths
    /** A straight path of n points from a to b into {@link #PATH}. */
    public static void line(int n, float ax, float ay, float az, float bx, float by, float bz) {
        n = Math.min(n, MAX_PATH);
        for (int i = 0; i < n; i++) {
            float s = n == 1 ? 0 : (float) i / (n - 1);
            PATH[i * 3] = ax + (bx - ax) * s;
            PATH[i * 3 + 1] = ay + (by - ay) * s;
            PATH[i * 3 + 2] = az + (bz - az) * s;
        }
    }

    /** A zig-zag from a to b: the interior points are pushed sideways by up to {@code jitter} (deterministic from the seed and the step). */
    public static void jagged(int n, float ax, float ay, float az, float bx, float by, float bz, float jitter, long seed, int step) {
        line(n, ax, ay, az, bx, by, bz);
        basis(bx - ax, by - ay, bz - az);
        for (int i = 1; i < Math.min(n, MAX_PATH) - 1; i++) {
            float j1 = (RobloxMath.hash(seed, i, step) - 0.5f) * 2f * jitter, j2 = (RobloxMath.hash(seed, i, step + 977) - 0.5f) * 2f * jitter;
            PATH[i * 3] += E1[0] * j1 + E2[0] * j2;
            PATH[i * 3 + 1] += E1[1] * j1 + E2[1] * j2;
            PATH[i * 3 + 2] += E1[2] * j1 + E2[2] * j2;
        }
    }

    /** A helix of n points round the axis a -> b: radius r0 at a to r1 at b, {@code turns} turns, starting at angle {@code phase}. */
    public static void spiral(int n, float ax, float ay, float az, float bx, float by, float bz, float r0, float r1, float turns, float phase) {
        n = Math.min(n, MAX_PATH);
        basis(bx - ax, by - ay, bz - az);
        for (int i = 0; i < n; i++) {
            float s = n == 1 ? 0 : (float) i / (n - 1), r = r0 + (r1 - r0) * s, ang = phase + Mth.TWO_PI * turns * s;
            float c = Mth.cos(ang) * r, sn = Mth.sin(ang) * r;
            PATH[i * 3] = ax + (bx - ax) * s + E1[0] * c + E2[0] * sn;
            PATH[i * 3 + 1] = ay + (by - ay) * s + E1[1] * c + E2[1] * sn;
            PATH[i * 3 + 2] = az + (bz - az) * s + E1[2] * c + E2[2] * sn;
        }
    }

    // ------------------------------------------------------------------ swoosh / trail
    /**
     * A camera-facing ribbon through the first {@code n} points of {@code path} (tail first). Width tapers from {@code tailW} to {@code headW} ({@link RobloxMath#taper}, exponent {@code widthPow}), the colour ramps
     * from {@code colTail} to {@code colHead}, V (along the ribbon) runs from {@code vFrom} at the tail to {@code vTo} at the head (scroll both to make it flow; U is across, 0 -> 1). n - 1 quads.
     */
    public static void swoosh(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float[] path, int n, float tailW, float headW, float widthPow,
                              float vFrom, float vTo, int colTail, int colHead) {
        n = Math.min(n, path.length / 3);
        if (n < 2) return;
        float plx = 0, ply = 0, plz = 0, prx = 0, pry = 0, prz = 0, pv = 0;
        int pc = 0;
        for (int i = 0; i < n; i++) {
            int i0 = Math.max(0, i - 1) * 3, i1 = Math.min(n - 1, i + 1) * 3, k = i * 3;
            float tx = path[i1] - path[i0], ty = path[i1 + 1] - path[i0 + 1], tz = path[i1 + 2] - path[i0 + 2];
            float px = path[k], py = path[k + 1], pz = path[k + 2];
            float sx = ty * pz - tz * py, sy = tz * px - tx * pz, sz = tx * py - ty * px;      // tangent x position: faces the camera (it sits at the origin)
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1e-6f) { sx = ctx.camRight.x; sy = ctx.camRight.y; sz = ctx.camRight.z; sl = 1f; }
            float s = (float) i / (n - 1);
            float w = 0.5f * RobloxMath.taper(s, tailW, headW, widthPow) / sl;
            float lx = px - sx * w, ly = py - sy * w, lz = pz - sz * w, rx = px + sx * w, ry = py + sy * w, rz = pz + sz * w;
            int col = VfxVertexBuffer.lerpColor(colTail, colHead, s);
            float v = vFrom + (vTo - vFrom) * s;
            if (i > 0) q(buf, tex, blend, plx, ply, plz, prx, pry, prz, rx, ry, rz, lx, ly, lz, 0, v, 1, pv, pc, col);
            plx = lx; ply = ly; plz = lz; prx = rx; pry = ry; prz = rz; pc = col; pv = v;
        }
    }

    /** One straight ribbon a -> b with an explicit side direction (unit): the crossed second plane of a beam. 1 quad. */
    public static void sideRibbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float sx, float sy, float sz,
                                  float w0, float w1, float vFrom, float vTo, int c0, int c1) {
        float h0 = w0 * 0.5f, h1 = w1 * 0.5f;
        q(buf, tex, blend, ax - sx * h0, ay - sy * h0, az - sz * h0, ax + sx * h0, ay + sy * h0, az + sz * h0, bx + sx * h1, by + sy * h1, bz + sz * h1, bx - sx * h1, by - sy * h1, bz - sz * h1,
                0, vTo, 1, vFrom, c0, c1);
    }

    /** A circular arc of n points on the plane through (cx, cy, cz) with normal (ax, ay, az), radius r, from angle {@code start} sweeping {@code sweep} radians (angles measured in the plane's basis, see {@link #angleIn}) into {@link #PATH}. */
    public static void arcPath(int n, float cx, float cy, float cz, float ax, float ay, float az, float radius, float start, float sweep) {
        n = Math.min(n, MAX_PATH);
        basis(ax, ay, az);
        for (int i = 0; i < n; i++) {
            float ang = start + sweep * (n == 1 ? 0f : (float) i / (n - 1)), c = Mth.cos(ang) * radius, s = Mth.sin(ang) * radius;
            PATH[i * 3] = cx + E1[0] * c + E2[0] * s;
            PATH[i * 3 + 1] = cy + E1[1] * c + E2[1] * s;
            PATH[i * 3 + 2] = cz + E1[2] * c + E2[2] * s;
        }
    }

    /** The angle, in the basis of the plane with normal (ax, ay, az), of the direction (dx, dy, dz) projected on it. */
    public static float angleIn(float ax, float ay, float az, float dx, float dy, float dz) {
        basis(ax, ay, az);
        return (float) Math.atan2(dx * E2[0] + dy * E2[1] + dz * E2[2], dx * E1[0] + dy * E1[1] + dz * E1[2]);
    }

    /** out = the unit direction at angle {@code ang} in the plane with normal (ax, ay, az). */
    public static void planeDir(float ax, float ay, float az, float ang, float[] out) {
        basis(ax, ay, az);
        float c = Mth.cos(ang), s = Mth.sin(ang);
        out[0] = E1[0] * c + E2[0] * s;
        out[1] = E1[1] * c + E2[1] * s;
        out[2] = E1[2] * c + E2[2] * s;
    }

    // ------------------------------------------------------------------ rings
    /** A flat ring band on the plane through (cx, cy, cz) with normal (ax, ay, az): outer radius {@code outerR}, {@code width} wide, U round it ({@code uRepeat} times, + {@code uScroll}), V 0 inner -> 1 outer. segs quads. */
    public static void ring(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float ax, float ay, float az, float outerR, float width, int segs,
                            float uRepeat, float uScroll, int argb) {
        if (outerR <= 0.01f || segs < 3) return;
        basis(ax, ay, az);
        float inner = Math.max(0f, outerR - width);
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uScroll + uRepeat * i / segs, u1 = uScroll + uRepeat * (i + 1) / segs;
            q(buf, tex, blend,
                    cx + E1[0] * c0 * inner + E2[0] * s0 * inner, cy + E1[1] * c0 * inner + E2[1] * s0 * inner, cz + E1[2] * c0 * inner + E2[2] * s0 * inner,
                    cx + E1[0] * c1 * inner + E2[0] * s1 * inner, cy + E1[1] * c1 * inner + E2[1] * s1 * inner, cz + E1[2] * c1 * inner + E2[2] * s1 * inner,
                    cx + E1[0] * c1 * outerR + E2[0] * s1 * outerR, cy + E1[1] * c1 * outerR + E2[1] * s1 * outerR, cz + E1[2] * c1 * outerR + E2[2] * s1 * outerR,
                    cx + E1[0] * c0 * outerR + E2[0] * s0 * outerR, cy + E1[1] * c0 * outerR + E2[1] * s0 * outerR, cz + E1[2] * c0 * outerR + E2[2] * s0 * outerR,
                    u0, 1f, u1, 0f, argb, argb);
        }
    }

    /**
     * The expanding shock ring: {@code layers} (1-3) {@link #ring}s whose timing is offset (0.14 apart) and which each race out with the quartic ease ({@link RobloxMath#shockRadius}), thin as they go and fading;
     * the later ones a little smaller. t = 0..1 progress of the burst. layers x segs quads.
     */
    public static void shock(VfxVertexBuffer buf, float cx, float cy, float cz, float ax, float ay, float az, float t, float maxR, float width, int layers, int segs, int argb, float fade) {
        for (int k = 0; k < layers; k++) {
            float off = k * 0.14f, lt = RobloxMath.clamp01((t - off) / (1f - off));
            if (lt <= 0f || lt >= 1f) continue;
            float r = maxR * (1f - 0.16f * k) * RobloxMath.shockRadius(lt);
            float w = Math.max(0.04f, width * (1f - 0.55f * lt) * (1f - 0.1f * k));
            float a = fade * (float) Math.pow(1f - lt, 1.4) * (1f - 0.25f * k);
            ring(buf, RING, VfxBlend.ADD, cx, cy, cz, ax, ay, az, r, w, segs, 3, k * 0.37f, VfxVertexBuffer.withAlpha(argb, a));
        }
    }

    // ------------------------------------------------------------------ cone / vortex
    /**
     * The side of a frustum standing on (cx, cy, cz) along the axis (ax, ay, az): radius baseR at the foot, topR at the top ({@code height} up), {@code bulge} swells the middle, the whole thing {@code spin} radians
     * round and twisted {@code twist} radians foot to top. U wraps round it ({@code uRepeat} times, + {@code uScroll}), V runs foot -> top ({@code vFrom} -> {@code vTo}). sides x rings quads.
     */
    public static void cone(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float ax, float ay, float az, float baseR, float topR, float height,
                            int sides, int rings, float bulge, float spin, float twist, float uRepeat, float uScroll, float vFrom, float vTo, int colBase, int colTop) {
        if (sides < 3 || rings < 1) return;
        basis(ax, ay, az);
        for (int j = 0; j < rings; j++) {
            float h0 = (float) j / rings, h1 = (float) (j + 1) / rings;
            float r0 = (baseR + (topR - baseR) * h0) * (1f + bulge * Mth.sin(h0 * Mth.PI)), r1 = (baseR + (topR - baseR) * h1) * (1f + bulge * Mth.sin(h1 * Mth.PI));
            float y0 = height * h0, y1 = height * h1;
            int k0 = VfxVertexBuffer.lerpColor(colBase, colTop, h0), k1 = VfxVertexBuffer.lerpColor(colBase, colTop, h1);
            float v0 = vFrom + (vTo - vFrom) * h0, v1 = vFrom + (vTo - vFrom) * h1;
            for (int i = 0; i < sides; i++) {
                float a00 = spin + Mth.TWO_PI * i / sides + twist * h0, a10 = spin + Mth.TWO_PI * (i + 1) / sides + twist * h0;
                float a01 = spin + Mth.TWO_PI * i / sides + twist * h1, a11 = spin + Mth.TWO_PI * (i + 1) / sides + twist * h1;
                float c00 = Mth.cos(a00) * r0, s00 = Mth.sin(a00) * r0, c10 = Mth.cos(a10) * r0, s10 = Mth.sin(a10) * r0;
                float c01 = Mth.cos(a01) * r1, s01 = Mth.sin(a01) * r1, c11 = Mth.cos(a11) * r1, s11 = Mth.sin(a11) * r1;
                float u0 = uScroll + uRepeat * i / sides, u1 = uScroll + uRepeat * (i + 1) / sides;
                q(buf, tex, blend,
                        cx + NA[0] * y0 + E1[0] * c00 + E2[0] * s00, cy + NA[1] * y0 + E1[1] * c00 + E2[1] * s00, cz + NA[2] * y0 + E1[2] * c00 + E2[2] * s00,
                        cx + NA[0] * y0 + E1[0] * c10 + E2[0] * s10, cy + NA[1] * y0 + E1[1] * c10 + E2[1] * s10, cz + NA[2] * y0 + E1[2] * c10 + E2[2] * s10,
                        cx + NA[0] * y1 + E1[0] * c11 + E2[0] * s11, cy + NA[1] * y1 + E1[1] * c11 + E2[1] * s11, cz + NA[2] * y1 + E1[2] * c11 + E2[2] * s11,
                        cx + NA[0] * y1 + E1[0] * c01 + E2[0] * s01, cy + NA[1] * y1 + E1[1] * c01 + E2[1] * s01, cz + NA[2] * y1 + E1[2] * c01 + E2[2] * s01,
                        u0, v1, u1, v0, k0, k1);
            }
        }
    }

    // ------------------------------------------------------------------ flip-book / glow / decal / streak
    /** One cell of a flip-book sheet as a camera-facing quad of side {@code size}, turned {@code rot}; t = 0..1 picks the frame; {@code crossfade} blends in the next frame (1 or 2 quads). */
    public static void flipbook(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float size, float rot, float t, boolean crossfade, int argb) {
        int frames = RobloxMath.FLIP_COLS * RobloxMath.FLIP_ROWS, f = RobloxMath.flipFrame(t, frames);
        float fr = crossfade && f + 1 < frames ? RobloxMath.flipBlend(t, frames) : 0f;
        cell(buf, ctx, tex, blend, cx, cy, cz, size, rot, f, fr > 0f ? VfxVertexBuffer.withAlpha(argb, 1f - fr) : argb);
        if (fr > 0.02f) cell(buf, ctx, tex, blend, cx, cy, cz, size, rot, f + 1, VfxVertexBuffer.withAlpha(argb, fr));
    }

    private static void cell(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float size, float rot, int frame, int col) {
        RobloxMath.flipCell(frame, RobloxMath.FLIP_COLS, RobloxMath.FLIP_ROWS, CELL);
        float c = Mth.cos(rot) * size * 0.5f, s = Mth.sin(rot) * size * 0.5f;
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rxx = r.x * c + u.x * s, rxy = r.y * c + u.y * s, rxz = r.z * c + u.z * s;
        float ryx = u.x * c - r.x * s, ryy = u.y * c - r.y * s, ryz = u.z * c - r.z * s;
        q(buf, tex, blend, cx - rxx - ryx, cy - rxy - ryy, cz - rxz - ryz, cx + rxx - ryx, cy + rxy - ryy, cz + rxz - ryz, cx + rxx + ryx, cy + rxy + ryy, cz + rxz + ryz, cx - rxx + ryx, cy - rxy + ryy, cz - rxz + ryz,
                CELL[0], CELL[1], CELL[2], CELL[3], col, col);
    }

    /** A glowing ball: a tinted alpha shell (keeps the colour saturated on a bright sky; skipped when {@code shell} is false), a wide additive halo, a white-hot core. 2-3 quads. */
    public static void coreShell(VfxVertexBuffer buf, VfxRenderContext ctx, float cx, float cy, float cz, float size, int shellArgb, int coreArgb, float k, boolean shell) {
        if (k <= 0.01f || size <= 0.001f) return;
        C.set(cx, cy, cz);
        if (shell) buf.billboard(ctx, GLOW, VfxBlend.ALPHA, C, size, 0f, VfxVertexBuffer.withAlpha(shellArgb, 0.6f * k));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, C, size * 2.1f, 0f, VfxVertexBuffer.withAlpha(shellArgb, 0.38f * k));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, C, size * 0.55f, 0f, VfxVertexBuffer.withAlpha(coreArgb, Math.min(1f, k)));
    }

    /** A flat ground decal ring (a plane of side 2 * radius, turned {@code spin}) on the plane with normal (ax, ay, az). 1 quad. */
    public static void decal(VfxVertexBuffer buf, float cx, float cy, float cz, float ax, float ay, float az, float radius, float spin, int argb) {
        if (radius <= 0.01f) return;
        basis(ax, ay, az);
        float c = Mth.cos(spin) * radius, s = Mth.sin(spin) * radius;
        float rx = E1[0] * c + E2[0] * s, ry = E1[1] * c + E2[1] * s, rz = E1[2] * c + E2[2] * s;
        float ux = E2[0] * c - E1[0] * s, uy = E2[1] * c - E1[1] * s, uz = E2[2] * c - E1[2] * s;
        q(buf, DECAL, VfxBlend.ADD, cx - rx - ux, cy - ry - uy, cz - rz - uz, cx + rx - ux, cy + ry - uy, cz + rz - uz, cx + rx + ux, cy + ry + uy, cz + rz + uz, cx - rx + ux, cy - ry + uy, cz - rz + uz,
                0, 0, 1, 1, argb, argb);
    }

    /** A stretched spark: the head at (hx, hy, hz), the tail {@code length} back along the unit direction (dx, dy, dz), camera-facing, {@code width} wide. 1 quad. */
    public static void streak(VfxVertexBuffer buf, float hx, float hy, float hz, float dx, float dy, float dz, float length, float width, int argb) {
        float tx = hx - dx * length, ty = hy - dy * length, tz = hz - dz * length;
        float sx = dy * hz - dz * hy, sy = dz * hx - dx * hz, sz = dx * hy - dy * hx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-6f) return;
        float w = width * 0.5f / sl;
        sx *= w; sy *= w; sz *= w;
        q(buf, STREAK, VfxBlend.ADD, tx - sx, ty - sy, tz - sz, tx + sx, ty + sy, tz + sz, hx + sx, hy + sy, hz + sz, hx - sx, hy - sy, hz - sz, 0, 0, 1, 1, argb, argb);
    }
}
