package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: this is the portal's drawing recipe in one place. It turns a {@link PortalFrame} (where the gate is, how open, where the
 * viewer stands) and a cached {@link PortalMesh} into quads for a {@link PortalSink}, back to front:
 * <ol>
 *   <li>a dark backdrop disc (only without the shader, which tints itself),</li>
 *   <li>3 to 5 nested depth shells, each smaller, deeper and turning the other way, slid by the viewer's angle ({@link PortalMath#parallax})
 *       so looking through the gate at a slant shows depth,</li>
 *   <li>the event horizon: rings of a polar swirl texture with a dark core, differently fast per ring,</li>
 *   <li>a soft rim glow, the outer ring of runic segments turning slowly, and a few arcs of rim lightning that crawl.</li>
 * </ol>
 * Everything is placed from the cached sector tables with a handful of multiplications; the composer owns a few scratch arrays and
 * allocates nothing per frame. It has no Minecraft types, so the preview and the unit tests run exactly this code.
 */
public final class PortalComposer {
    private static final float TAU = (float) (Math.PI * 2);
    /** How many times the swirl and shell textures wrap round the full circle. */
    private static final float SWIRL_REPEAT = 2f, SHELL_REPEAT = 3f;

    private final float[] p = new float[12];
    private final float[] par = new float[2];

    private PortalFrame f;
    private PortalSink sink;
    private PortalMesh mesh;

    // ------------------------------------------------------------------ colours
    private static int clamp255(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    /** Base colour scaled by mul (>1 pushes toward white) with alpha 0..1. */
    static int col(int rgb, float mul, float alpha) {
        float r = ((rgb >> 16) & 255), g = ((rgb >> 8) & 255), b = (rgb & 255);
        if (mul > 1f) { float k = Math.min(1f, mul - 1f); r += (255 - r) * k; g += (255 - g) * k; b += (255 - b) * k; mul = 1f; }
        return (clamp255(alpha * 255f) << 24) | (clamp255(r * mul) << 16) | (clamp255(g * mul) << 8) | clamp255(b * mul);
    }

    // ------------------------------------------------------------------ placement
    private void corner(int k, float lx, float ly, float lz) {
        p[k * 3] = f.cx + f.rx * lx + f.ux * ly + f.nx * lz;
        p[k * 3 + 1] = f.cy + f.ry * lx + f.uy * ly + f.ny * lz;
        p[k * 3 + 2] = f.cz + f.rz * lx + f.uz * ly + f.nz * lz;
    }

    /**
     * One annular cell between radii r0 and r1 (fractions of the oval) from sector i to i+1 of n, the whole ring turned by (rc, rs)
     * (cos, sin of the turn) and moved by (ox, oy) blocks at depth z blocks.
     */
    private void cell(PortalPass pass, int n, int i, float r0, float r1, float rc, float rs, float ox, float oy, float z,
                      float u0, float u1, float vIn, float vOut, int cIn, int cOut) {
        float c0 = mesh.cos(n, i) * rc - mesh.sin(n, i) * rs, s0 = mesh.sin(n, i) * rc + mesh.cos(n, i) * rs;
        float c1 = mesh.cos(n, i + 1) * rc - mesh.sin(n, i + 1) * rs, s1 = mesh.sin(n, i + 1) * rc + mesh.cos(n, i + 1) * rs;
        corner(0, ox + c0 * r0 * f.hw, oy + s0 * r0 * f.hh, z);
        corner(1, ox + c1 * r0 * f.hw, oy + s1 * r0 * f.hh, z);
        corner(2, ox + c1 * r1 * f.hw, oy + s1 * r1 * f.hh, z);
        corner(3, ox + c0 * r1 * f.hw, oy + s0 * r1 * f.hh, z);
        sink.quad(pass, p, u0, u1, vIn, vOut, cIn, cOut);
    }

    // ------------------------------------------------------------------ the gate
    public void compose(PortalMesh mesh, PortalFrame frame, PortalSink sink) {
        this.mesh = mesh; this.f = frame; this.sink = sink;
        if (f.hw < 0.01f || f.hh < 0.01f || f.fade <= 0.01f) return;
        PortalSpec s = mesh.spec;
        if (!f.shader) backdrop(s);
        shells(s);
        horizon(s);
        rim(s);
        runes(s);
        bolts(s);
    }

    private void backdrop(PortalSpec s) {
        int n = s.horizonSectors();
        int in = col(f.argb, 0.10f, 0.85f * f.fade), out = col(f.argb, 0.30f, 0.55f * f.fade);
        for (int i = 0; i < n; i++) cell(PortalPass.BACKDROP, n, i, 0f, 1f, 1f, 0f, 0f, 0f, -0.01f * f.size, 0, 1, 0, 1, in, out);
    }

    private void shells(PortalSpec s) {
        int n = s.shellSectors(), count = s.shells();
        for (int i = count - 1; i >= 0; i--) {                                       // deepest first
            float scale = PortalMath.shellScale(i, count);
            float depth = PortalMath.shellDepth(i) * f.size;
            float room = 1f - scale;
            PortalMath.parallax(f.vx, f.vy, f.vz, depth, room * f.hw * 0.85f, room * f.hh * 0.85f, par);
            float turn = (i % 2 == 0 ? 1f : -1f) * (f.age * (0.020f + 0.012f * i)) + PortalMath.hash(f.seed, i, 3) * TAU;
            float rc = (float) Math.cos(turn), rs = (float) Math.sin(turn);
            float bright = 1.05f - 0.10f * i;
            float alpha = 0.60f * (1f - 0.10f * i) * f.fade;
            int cIn = col(f.argb, bright * 0.55f, alpha * 0.35f), cOut = col(f.argb, bright + 0.15f, alpha);
            float scroll = f.age * 0.004f * (i % 2 == 0 ? 1 : -1);
            for (int k = 0; k < n; k++) {
                float u0 = SHELL_REPEAT * k / n + scroll, u1 = SHELL_REPEAT * (k + 1) / n + scroll;
                cell(PortalPass.SHELL, n, k, scale * 0.66f, scale, rc, rs, par[0], par[1], -0.05f * (i + 1) * f.size, u0, u1, 0f, 1f, cIn, cOut);
            }
        }
    }

    private void horizon(PortalSpec s) {
        int n = s.horizonSectors(), rings = s.horizonRings();
        for (int j = 0; j < rings; j++) {
            float r0 = (float) j / rings, r1 = (float) (j + 1) / rings, rm = (r0 + r1) * 0.5f;
            float scroll = PortalMath.swirlTurns(rm, f.age) * SWIRL_REPEAT;
            // dark in the core, bright towards the rim: additive, so the dark core simply shows what is behind it
            int cIn = col(f.argb, 0.20f + 0.95f * r0, f.fade * (0.20f + 0.65f * r0));
            int cOut = col(f.argb, 0.20f + 0.95f * r1, f.fade * (0.20f + 0.65f * r1));
            for (int i = 0; i < n; i++) {
                float u0 = SWIRL_REPEAT * i / n + scroll, u1 = SWIRL_REPEAT * (i + 1) / n + scroll;
                cell(PortalPass.HORIZON, n, i, r0, r1, 1f, 0f, 0f, 0f, 0.005f * f.size, u0, u1, r0, r1, cIn, cOut);
            }
        }
    }

    private void rim(PortalSpec s) {
        int n = s.horizonSectors();
        float pulse = 0.8f + 0.2f * (float) Math.sin(f.age * 0.25f);
        int mid = col(f.argb, 1.35f, 0.85f * f.fade * pulse);
        // the band is brightest in the middle of its width: the texture's v gradient carries that, the vertex colour only fades it
        for (int i = 0; i < n; i++) cell(PortalPass.RIM, n, i, 0.90f, 1.10f, 1f, 0f, 0f, 0f, 0.02f * f.size, 0, 1, 0, 1, mid, mid);
    }

    private void runes(PortalSpec s) {
        int n = s.ringSegments();
        float turn = f.age * 0.025f;
        float rc = (float) Math.cos(turn), rs = (float) Math.sin(turn);
        float z = 0.04f * f.size;
        if (s.ringBase()) {
            int base = col(f.argb, 0.7f, 0.30f * f.fade);
            for (int i = 0; i < n; i++) cell(PortalPass.RIM, n, i, 1.03f, 1.30f, rc, rs, 0f, 0f, z * 0.5f, 0, 1, 0, 1, base, base);
        }
        for (int i = 0; i < n; i++) {
            // each segment covers 82 percent of its slot, centred, so the ring reads as separate runic plates
            int g = PortalMath.glyph(f.seed, i);
            float flick = 0.55f + 0.45f * (float) Math.sin(f.age * 0.15f + i * 1.7f);
            int c = col(f.argb, 1.3f, 0.95f * f.fade * flick);
            float a0 = i + 0.09f, a1 = i + 0.91f;
            float c0 = interp(n, a0, true, rc, rs), s0 = interp(n, a0, false, rc, rs), c1 = interp(n, a1, true, rc, rs), s1 = interp(n, a1, false, rc, rs);
            corner(0, c0 * 1.07f * f.hw, s0 * 1.07f * f.hh, z);
            corner(1, c1 * 1.07f * f.hw, s1 * 1.07f * f.hh, z);
            corner(2, c1 * 1.27f * f.hw, s1 * 1.27f * f.hh, z);
            corner(3, c0 * 1.27f * f.hw, s0 * 1.27f * f.hh, z);
            sink.quad(PortalPass.RUNES, p, g * 0.25f, g * 0.25f + 0.25f, 0f, 1f, c, c);
        }
    }

    /** cos or sin of the (fractional) sector position a of n, turned by (rc, rs). */
    private float interp(int n, float a, boolean cosine, float rc, float rs) {
        int i = (int) Math.floor(a);
        float t = a - i;
        float c = mesh.cos(n, i) * (1 - t) + mesh.cos(n, i + 1) * t, s = mesh.sin(n, i) * (1 - t) + mesh.sin(n, i + 1) * t;
        float len = (float) Math.sqrt(c * c + s * s);
        if (len > 1e-6f) { c /= len; s /= len; }
        return cosine ? c * rc - s * rs : s * rc + c * rs;
    }

    private void bolts(PortalSpec s) {
        int count = s.bolts(), segs = s.boltSegments();
        float turn = f.age * 0.012f;
        for (int b = 0; b < count; b++) {
            if (!PortalMath.boltLit(f.seed, b, f.age)) continue;
            float start = PortalMath.hash(f.seed, b, 9) * TAU + turn, span = 0.55f + 0.35f * PortalMath.hash(f.seed, b, 10);
            float px = 0, py = 0;
            for (int k = 0; k <= segs; k++) {
                float a = start + span * k / segs;
                float r = 1.10f + 0.07f * PortalMath.boltJitter(f.seed, b, k, f.age) + (k == 0 || k == segs ? -0.05f : 0f);
                float x = (float) Math.cos(a) * r * f.hw, y = (float) Math.sin(a) * r * f.hh;
                if (k > 0) {
                    float rad = 0.028f * f.hw;
                    float cx = (float) Math.cos(a), cy = (float) Math.sin(a);
                    float pa = a - span / segs, pcx = (float) Math.cos(pa), pcy = (float) Math.sin(pa);
                    float zb = 0.05f * f.size;
                    corner(0, px - pcx * rad, py - pcy * rad * f.hh / f.hw, zb);
                    corner(1, x - cx * rad, y - cy * rad * f.hh / f.hw, zb);
                    corner(2, x + cx * rad, y + cy * rad * f.hh / f.hw, zb);
                    corner(3, px + pcx * rad, py + pcy * rad * f.hh / f.hw, zb);
                    int c = col(f.argb, 1.6f, 0.9f * f.fade);
                    sink.quad(PortalPass.BOLT, p, (float) (k - 1) / segs, (float) k / segs, 0f, 1f, c, c);
                }
                px = x; py = y;
            }
        }
    }

    // ------------------------------------------------------------------ the thread between a pair
    private final float[] tp = new float[3 * 16];

    /**
     * A thin, wavering thread from gate A's rim to gate B's rim. Both ends are camera-relative; the ribbon turns to face the camera
     * (at the origin). {@code alpha} should already include the distance fade; at most 15 segments.
     */
    public void thread(PortalSink sink, int argb, float ax, float ay, float az, float bx, float by, float bz, float ticks, int segs, float width, float alpha) {
        segs = Math.max(1, Math.min(15, segs));
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f || alpha <= 0.01f) return;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        // side = dir x toCamera (toCamera = -mid, the camera is at the origin)
        float sx = dy * -mz - dz * -my, sy = dz * -mx - dx * -mz, sz = dx * -my - dy * -mx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-5f) return;
        sx /= sl; sy /= sl; sz /= sl;
        for (int k = 0; k <= segs; k++) {
            float t = (float) k / segs, env = (float) Math.sin(Math.PI * t);      // waves vanish at both rims
            float wave = (float) Math.sin(t * 9f - ticks * 0.3f) * 0.12f * env + (float) Math.sin(t * 23f + ticks * 0.5f) * 0.04f * env;
            tp[k * 3] = ax + dx * t + sx * wave;
            tp[k * 3 + 1] = ay + dy * t + sy * wave + (float) Math.sin(t * Math.PI) * 0.25f;     // a slight sag upwards, like a held thread
            tp[k * 3 + 2] = az + dz * t + sz * wave;
        }
        float hw = width * 0.5f;
        for (int k = 0; k < segs; k++) {
            float e0 = (float) Math.sin(Math.PI * k / segs), e1 = (float) Math.sin(Math.PI * (k + 1) / segs);
            float w0 = hw * (0.35f + 0.65f * e0), w1 = hw * (0.35f + 0.65f * e1);
            p[0] = tp[k * 3] - sx * w0; p[1] = tp[k * 3 + 1] - sy * w0; p[2] = tp[k * 3 + 2] - sz * w0;
            p[3] = tp[k * 3 + 3] - sx * w1; p[4] = tp[k * 3 + 4] - sy * w1; p[5] = tp[k * 3 + 5] - sz * w1;
            p[6] = tp[k * 3 + 3] + sx * w1; p[7] = tp[k * 3 + 4] + sy * w1; p[8] = tp[k * 3 + 5] + sz * w1;
            p[9] = tp[k * 3] + sx * w0; p[10] = tp[k * 3 + 1] + sy * w0; p[11] = tp[k * 3 + 2] + sz * w0;
            float a0 = alpha * (0.4f + 0.6f * e0), a1 = alpha * (0.4f + 0.6f * e1);
            float scroll = -ticks * 0.05f;
            // the sink's two colours are the two long edges; the thread is one colour whose strength follows the taper (cross-section glow is in the texture)
            int c = col(argb, 1.3f, (a0 + a1) * 0.5f);
            sink.quad(PortalPass.THREAD, p, (float) k / segs * len * 0.25f + scroll, (float) (k + 1) / segs * len * 0.25f + scroll, 0f, 1f, c, c);
        }
    }
}
