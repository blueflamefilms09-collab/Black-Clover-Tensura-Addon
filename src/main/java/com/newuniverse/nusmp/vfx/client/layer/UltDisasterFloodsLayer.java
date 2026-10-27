package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Disaster of Floods" (Omniscient Reader's Viewpoint, burst radius 12-13): a dark-teal storm DELUGE, not a water wall. Seen from the caster's feet:
 * <ul>
 *   <li>ticks 0-6, the sky goes black (storm clouds close in), rain starts, a huge black-green whirlpool opens on the ground and a ring of surging crests
 *       rises round it with the first silhouettes (maws, horned heads, tentacles, fins) lifting out of the crest tops;</li>
 *   <li>ticks 6-12, the surge: the crest ring roars outward to the full radius, leaning over, a pale foam front running ahead of it, claws raking, lightning
 *       striking the tide;</li>
 *   <li>ticks 12-20, the tide drops and recedes: crests slump, the monsters sink, debris falls, foam rings fade on the dark water and the rain thins out.</li>
 * </ul>
 * Everything is drawn from tapered strips, soft billboards and ground planes (no flat paper shapes): two curved crest bands (leaning, jagged tops), 6 monster
 * silhouettes, rain cards, 2 lightning bolts. Deterministic from the instance seed and age. Worst case ~350 of the 400 vertices, 0 allocations per frame
 * apart from the camera-relative centre vector (all scratch vectors are fields).
 */
public class UltDisasterFloodsLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation WAVE = t("storm_wave"), CLOUD = t("storm_cloud"), RAIN = t("storm_rain"), BOLT = t("lightningfx_bolt"),
            SPIRAL = t("vortex_spiral"), SOUL = t("soul_vortex"), CORE = t("vortex_core"), RING = t("vortex_ring"), FOAM = t("bubble_foam_cluster"),
            STREAK = t("smoke_wisp"), BLOB = t("smoke_billow"), MAW = t("arc_maw"), SPIKE = t("spike_spike"), FIN = t("demon_water_spine"),
            CLAWS = t("demon_beast_claws"), SHARD = t("scale_fin"), MIST = t("smoke_wisp");

    private static final int DEEP = 0xFF050E10, SLATE = 0xFF1B2D31, MIDTEAL = 0xFF1B5552, TEAL = 0xFF3A8D82, FOAMC = 0xFFB4EBCB, WHITE = 0xFFEAFFF6,
            EYE = 0xFFC4FF84, STORM = 0xFF222E34;

    private static final int N_A = 14, N_B = 8;
    private static final int[] FOAM_J = {1, 5, 6, 8, 12, 13};
    private static final float[] STRIKE_T = {1.5f, 3.5f, 5f, 7.5f, 9.5f, 11f, 13f, 15.5f};

    // scratch (render runs on the one client thread)
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f(), tmp = new Vector3f(), tmp2 = new Vector3f();
    private final Vector3f[] tp = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private final Vector3f[] ts = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_DISASTER_FLOODS); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF3A8D82; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static float eo(float v) { return VfxAnim.easeOutCubic(cl(v)); }
    private static int mix(int c0, int c1, float t) { return VfxVertexBuffer.lerpColor(c0, c1, cl(t)); }

    // ------------------------------------------------------------------ the tide's own timeline
    /** Crest ring radius (ratio of R): opens at 0.42, surges out to 1.0 over ticks 6-12, drifts back in while it slumps. */
    private static float crestR(float age) {
        return 0.40f + 0.13f * eo(age / 6f) + 0.47f * eo(sub(age, 5.5f, 12f)) - 0.12f * sub(age, 12f, 20f) * sub(age, 12f, 20f);
    }

    /** Crest height (ratio of R): rises in the build-up, towers at the surge, collapses after tick 13. */
    private static float crestH(float age) {
        float up = 0.08f + 0.26f * eo(age / 6f) + 0.26f * eo(sub(age, 5f, 11f));
        float down = 1f - VfxAnim.easeInCubic(sub(age, 12.5f, 19.5f)) * 0.96f;
        return up * down;
    }

    private static float strike(float age, float t0) { return age < t0 ? 0f : cl(1f - (age - t0) / 2.4f); }

    // ------------------------------------------------------------------ draw
    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Mth.clamp(inst.power, 4f, 20f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float out = 1f - sub(age, 16.5f, 20f);
        if (out <= 0.01f) return;
        float build = eo(age / 6f), surge = sub(age, 5.5f, 12f);
        float storm = eo(age / 4f) * out;                       // the sky closing in
        int accent = inst.color | 0xFF000000;
        float rc = crestR(age) * R, hc = crestH(age) * R;
        float lean = 0.12f + 0.5f * surge * (1f - sub(age, 12f, 18f));
        float spin = age * 0.2f;
        int segs = ctx.seg(N_A, 8);                             // reduced detail: fewer crest segments (kept even)
        int na = segs % 2 == 0 ? segs : segs + 1;
        float cx = c.x, cy = c.y, cz = c.z;
        // which crest segment is the farthest from the camera (draw far to near)
        float farAng = (float) Math.atan2(cz, cx);

        // ---- ground: the black-green tide body, whirlpool, drain
        float water = build * out;
        buf.plane(CORE, VfxBlend.ALPHA, VfxPose.ground(tmp.set(cx, cy + 0.04f, cz)), R * 1.08f * (0.5f + 0.5f * build), a(mix(DEEP, MIDTEAL, 0.25f), 0.88f * water));
        buf.plane(SPIRAL, VfxBlend.ALPHA, VfxPose.ground(tmp.set(cx, cy + 0.06f, cz)).spin(spin), R * 0.62f * (0.35f + 0.65f * build), a(mix(SLATE, DEEP, 0.4f), 0.92f * water));
        buf.plane(CORE, VfxBlend.ALPHA, VfxPose.ground(tmp.set(cx, cy + 0.08f, cz)), R * 0.2f * build, a(0xFF000000, 0.95f * water));
        // foam fronts running on the water: the surge front and the trailing / receding one
        float front = rc * 1.04f;
        buf.plane(RING, VfxBlend.ALPHA, VfxPose.ground(tmp.set(cx, cy + 0.1f, cz)).spin(age * 0.05f), front, a(mix(MIDTEAL, FOAMC, 0.55f + 0.3f * surge), (0.5f + 0.35f * surge) * out * build));
        float trail = rc * (0.78f + 0.1f * (1f - surge)) ;
        if (age > 6f) buf.plane(RING, VfxBlend.ALPHA, VfxPose.ground(tmp.set(cx, cy + 0.11f, cz)).spin(-age * 0.04f), trail, a(mix(MIDTEAL, FOAMC, 0.35f), 0.45f * out * sub(age, 6f, 9f)));

        // ---- the storm sky (soft dark masses closing over the area)
        for (int k = 0; k < 4; k++) {
            float ang = k * Mth.HALF_PI + 0.6f + age * 0.012f * (k % 2 == 0 ? 1 : -1);
            float rr = R * (0.95f - 0.1f * build) * (0.75f + 0.3f * hash(seed, k, 1));
            tmp.set(cx + Mth.cos(ang) * rr, cy + R * (1.15f - 0.12f * build) + 0.4f * hash(seed, k, 2), cz + Mth.sin(ang) * rr);
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, tmp, R * (1.0f + 0.25f * hash(seed, k, 3)), hash(seed, k, 4) * 6.28f + age * 0.015f, a(mix(STORM, DEEP, 0.3f), 0.8f * storm));
        }

        // ---- the crests: inner band first, then the outer one (each drawn far to near)
        band(buf, WAVE, cx, cy, cz, N_B, rc * 0.64f, crestH(age - 2f) * R * 0.62f, lean * 0.7f, 0.3f, age, seed, 7, farAng, mix(DEEP, MIDTEAL, 0.2f), mix(MIDTEAL, TEAL, 0.6f), 0.92f * out * build);
        band(buf, WAVE, cx, cy, cz, na, rc, hc, lean, 0f, age, seed, 3, farAng, mix(DEEP, MIDTEAL, 0.3f), mix(TEAL, FOAMC, 0.3f), 0.96f * out);

        // ---- monsters riding the crest tops (vertices 0, 2, 4, 7, 9, 11 of the outer band)
        float lowM = 1f - sub(age, 12f, 18f);
        for (int m = 0; m < 6; m++) {
            int j = (m * na) / 6;
            float mr = VfxAnim.easeOutBack(sub(age, 1.5f + 0.5f * m, 7.5f + 0.4f * m)) * lowM * out;
            if (mr <= 0.02f) continue;
            float aj = Mth.TWO_PI * j / na;
            float hj = crestVertexH(hc, j % na, seed, 3, age);
            float rj = rc + lean * hj;
            float ox = Mth.cos(aj), oz = Mth.sin(aj);
            float bx = cx + ox * rj, by = cy + hj - 0.2f, bz = cz + oz * rj;
            float sc = R * (1.25f + 0.3f * hash(seed, m, 5)) / 13f;      // monster scale (1 at radius 13)
            tmp.set(bx, by + 0.4f * sc, bz);
            if (m != 1 && m != 5) buf.billboard(ctx, BLOB, VfxBlend.ALPHA, tmp, 4.2f * sc * mr, hash(seed, m, 6) * 6f, a(DEEP, 0.95f * out));
            switch (m) {
                case 0, 4 -> head(buf, ctx, bx, by, bz, ox, oz, sc, mr, age, m);
                case 1, 5 -> tentacles(buf, ctx, bx, by, bz, ox, oz, sc, mr, age, m, seed);
                case 2 -> maw(buf, ctx, bx, by, bz, ox, oz, sc, mr, age, m);
                default -> fin(buf, ctx, bx, by, bz, ox, oz, sc, mr, age, m);
            }
        }

        // ---- foam caps on the crest tops (the other vertices), flung up at the surge
        for (int k = 0; k < 6; k++) {
            int j = FOAM_J[k] % na;
            float aj = Mth.TWO_PI * j / na, hj = crestVertexH(hc, j, seed, 3, age), rj = rc + lean * hj;
            tmp.set(cx + Mth.cos(aj) * (rj + 0.3f * lean * hj), cy + hj + R * 0.015f, cz + Mth.sin(aj) * (rj + 0.3f * lean * hj));
            float fs = R * (0.11f + 0.05f * hash(seed, k, 9)) * (0.4f + 0.6f * build);
            buf.billboard(ctx, FOAM, VfxBlend.ALPHA, tmp, fs, hash(seed, k, 10) * 6.28f, a(mix(FOAMC, accent, 0.1f), 0.8f * out * sub(hc / R, 0.04f, 0.14f)));
        }

        // ---- debris thrown by the surge, falling in the aftermath
        for (int k = 0; k < 5; k++) {
            float q = sub(age, 6f + 0.3f * k, 19f);
            if (q <= 0f) continue;
            float ang = hash(seed, k, 11) * Mth.TWO_PI, d = R * (0.55f + 0.5f * hash(seed, k, 12)) * (0.6f + 0.4f * eo(q * 1.6f));
            float yy = R * (0.12f + 0.4f * hash(seed, k, 13)) * 4f * q * (1f - q);
            tmp.set(cx + Mth.cos(ang) * d, cy + 0.3f + yy, cz + Mth.sin(ang) * d);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, tmp, R * (0.06f + 0.04f * hash(seed, k, 14)), age * (0.3f + hash(seed, k, 15)) + k, a(mix(SLATE, MIDTEAL, 0.3f), 0.9f * out * (1f - q * q)));
        }

        // ---- the downpour: tall rain cards in front of everything (alpha, then they thin out)
        float rainA = storm * (1f - 0.7f * sub(age, 13f, 19f));
        Vector3f rr0 = tmp2.set(ctx.camRight.x, 0f, ctx.camRight.z);
        if (rr0.lengthSquared() < 1e-4f) rr0.set(1f, 0f, 0f);
        rr0.normalize();
        for (int k = 0; k < 7; k++) {
            float ang = hash(seed, k, 20) * Mth.TWO_PI, d = R * (0.1f + 0.95f * hash(seed, k, 21));
            float fall = ((age * 0.33f + hash(seed, k, 22)) % 1f) * R * 0.2f;
            float h = R * 0.85f, w = R * 0.2f;
            float x = cx + Mth.cos(ang) * d + 0.6f * Mth.sin(age * 0.2f + k), z = cz + Mth.sin(ang) * d;
            float yb = cy - 0.5f - fall + R * 0.05f;
            p0.set(x - rr0.x * w, yb, z - rr0.z * w); p1.set(x + rr0.x * w, yb, z + rr0.z * w);
            p2.set(x + rr0.x * w + 0.9f, yb + h, z + rr0.z * w); p3.set(x - rr0.x * w + 0.9f, yb + h, z - rr0.z * w);
            buf.quad(RAIN, VfxBlend.ALPHA, p0, p1, p2, p3, 0f, 0f, 1f, 1f, a(0xFFB8D6CF, 0.55f * rainA), a(0xFFB8D6CF, 0.12f * rainA));
        }

        // ---- additive: whirlpool light, peak flash, lightning
        buf.plane(SOUL, VfxBlend.ADD, VfxPose.ground(tmp.set(cx, cy + 0.12f, cz)).spin(-spin * 1.3f), R * 0.4f * (0.4f + 0.6f * build), a(mix(MIDTEAL, FOAMC, 0.35f), 0.4f * water));
        float pk = Mth.sin(cl((age - 5f) / 6f) * Mth.PI);
        if (pk > 0.02f) VfxBloom.glow(ctx, buf, tmp.set(cx, cy + R * 0.12f, cz), R * 0.16f, mix(TEAL, FOAMC, 0.5f), pk * 0.8f * out);
        for (int s = 0; s < 2; s++) {
            for (int q = s; q < STRIKE_T.length; q += 2) {
                float st = strike(age, STRIKE_T[q]);
                if (st <= 0.02f) continue;
                float ang = hash(seed, q, 30 + s) * Mth.TWO_PI, d = R * (0.35f + 0.65f * hash(seed, q, 31));
                lightning(buf, ctx, cx + Mth.cos(ang) * d, cy, cz + Mth.sin(ang) * d, R, st * (0.7f + 0.3f * (age * 7.3f % 1f)), q, seed);
            }
        }
    }

    // ------------------------------------------------------------------ crest band
    /** Height of crest vertex j (jagged, with a rolling wobble). */
    private static float crestVertexH(float h, int j, long seed, int salt, float age) {
        return h * (0.45f + 0.8f * hash(seed, j, salt)) * (0.9f + 0.1f * Mth.sin(age * 0.6f + j * 2.1f));
    }

    private void band(VfxVertexBuffer buf, ResourceLocation tex, float cx, float cy, float cz, int n, float rc, float h, float lean, float rot, float age, long seed, int salt,
                      float farAng, int cBot, int cTop, float alpha) {
        if (h < 0.05f || alpha <= 0.01f) return;
        int far = Math.floorMod(Math.round(farAng / Mth.TWO_PI * n - 0.5f - rot / Mth.TWO_PI * n), n);
        for (int d = 0; d < n; d++) {
            int i = Math.floorMod(far + ((d & 1) == 0 ? d / 2 : -(d + 1) / 2), n), i2 = (i + 1) % n;
            float a0 = Mth.TWO_PI * i / n + rot, a1 = Mth.TWO_PI * (i + 1) / n + rot;
            float h0 = crestVertexH(h, i, seed, salt, age), h1 = crestVertexH(h, i2, seed, salt, age);
            float r0 = rc + lean * h0, r1 = rc + lean * h1;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            p0.set(cx + c0 * rc, cy - 0.2f, cz + s0 * rc);
            p1.set(cx + c1 * rc, cy - 0.2f, cz + s1 * rc);
            p2.set(cx + c1 * r1, cy + h1, cz + s1 * r1);
            p3.set(cx + c0 * r0, cy + h0, cz + s0 * r0);
            boolean flip = (i & 1) == 1;
            buf.quad(tex, VfxBlend.ALPHA, p0, p1, p2, p3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, a(cBot, alpha), a(cTop, alpha));
        }
    }

    // ------------------------------------------------------------------ monsters
    /** A camera-facing card (width hw*2, height hh*2) with a roll and optional horizontal flip. */
    private void card(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float hw, float hh, float rot, boolean flip, int cb, int ct) {
        float cs = Mth.cos(rot), sn = Mth.sin(rot);
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rx = (r.x * cs + u.x * sn) * hw, ry = (r.y * cs + u.y * sn) * hw, rz = (r.z * cs + u.z * sn) * hw;
        float ux = (u.x * cs - r.x * sn) * hh, uy = (u.y * cs - r.y * sn) * hh, uz = (u.z * cs - r.z * sn) * hh;
        p0.set(x - rx - ux, y - ry - uy, z - rz - uz); p1.set(x + rx - ux, y + ry - uy, z + rz - uz);
        p2.set(x + rx + ux, y + ry + uy, z + rz + uz); p3.set(x - rx + ux, y - ry + uy, z - rz + uz);
        buf.quad(tex, bl, p0, p1, p2, p3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, cb, ct);
    }

    private void glowSpot(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float size, int col, float al) {
        if (al <= 0.02f) return;
        tmp.set(x, y, z);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, tmp, size, 0f, a(col, al));
    }

    /** Tapered, curving strip from (x,y,z): rises, bends outward (ox,oz) and sways, width w0 -> w1 over 3 camera-facing segments. */
    private void strip(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, float x, float y, float z, float ox, float oz, float len, float bend, float sway,
                       float w0, float w1, float phase, float age, int cb, int ct) {
        float tx = -oz, tz = ox;
        for (int i = 0; i < 4; i++) {
            float s = i / 3f;
            float sw = Mth.sin(age * 0.45f + phase + s * 3.2f) * sway * s;
            tp[i].set(x + ox * bend * s * s + tx * sw, y + len * s, z + oz * bend * s * s + tz * sw);
        }
        for (int i = 0; i < 4; i++) {
            Vector3f prev = tp[Math.max(0, i - 1)], next = tp[Math.min(3, i + 1)];
            tmp.set(next).sub(prev);
            tmp2.set(tp[i]).negate();                                   // to the camera (camera-relative space)
            Vector3f sd = ts[i].set(tmp).cross(tmp2);
            if (sd.lengthSquared() < 1e-6f) sd.set(ctx.camRight);
            sd.normalize().mul(Mth.lerp(i / 3f, w0, w1) * 0.5f);
        }
        for (int i = 0; i < 3; i++) {
            if (!buf.hasBudget(4)) return;
            float s0 = i / 3f, s1 = (i + 1) / 3f;
            p0.set(tp[i]).sub(ts[i]); p1.set(tp[i]).add(ts[i]); p2.set(tp[i + 1]).add(ts[i + 1]); p3.set(tp[i + 1]).sub(ts[i + 1]);
            buf.quad(tex, VfxBlend.ALPHA, p0, p1, p2, p3, 0f, 1f - s0 * 0.98f, 1f, 1f - s1 * 0.98f, mix(cb, ct, s0), mix(cb, ct, s1));
        }
    }

    /** A horned, jawed leviathan head on a thick neck, in profile facing out of the tide; eyes glow. */
    private void head(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float ox, float oz, float sc, float mr, float age, int m) {
        float side = ox * ctx.camRight.x + oz * ctx.camRight.z >= 0f ? 1f : -1f;           // screen side the head looks to
        float nod = 0.25f * Mth.sin(age * 0.35f + m);
        float neckH = 2.2f * sc * mr;
        strip(buf, ctx, STREAK, x, y - 0.4f, z, ox, oz, neckH, 1.2f * sc * mr, 0.5f * sc, 3.0f * sc, 1.9f * sc, m, age, DEEP, SLATE);
        float hx = x + ox * 1.2f * sc * mr, hy = y - 0.4f + neckH + 0.2f * sc * mr, hz = z + oz * 1.2f * sc * mr;
        float s = sc * mr;
        // skull and jaw: dark soft masses, snout further out
        tmp.set(hx, hy, hz);
        buf.billboard(ctx, BLOB, VfxBlend.ALPHA, tmp, 4.2f * s, 0.3f, a(TEAL, 0.7f));
        buf.billboard(ctx, BLOB, VfxBlend.ALPHA, tmp, 3.6f * s, 0.3f, a(DEEP, 0.97f));
        tmp.set(hx + ctx.camRight.x * side * 1.7f * s, hy - 0.1f * s + ctx.camRight.y * side * 1.7f * s, hz + ctx.camRight.z * side * 1.7f * s);
        buf.billboard(ctx, BLOB, VfxBlend.ALPHA, tmp, 2.4f * s, -0.4f, a(0xFF081416, 0.97f));
        // two horns (tapered cards, swept back and up)
        card(buf, ctx, SPIKE, VfxBlend.ALPHA, hx - ctx.camRight.x * side * 0.8f * s + ctx.camUp.x * 1.2f * s, hy - ctx.camRight.y * side * 0.8f * s + ctx.camUp.y * 1.2f * s,
                hz - ctx.camRight.z * side * 0.8f * s + ctx.camUp.z * 1.2f * s, 0.4f * s, 1.0f * s, -side * 0.55f + nod, false, a(SLATE, 0.97f), a(DEEP, 0.97f));
        // glowing eye and a hint of fangs
        glowSpot(buf, ctx, hx + ctx.camRight.x * side * 0.8f * s + ctx.camUp.x * 0.35f * s, hy + ctx.camRight.y * side * 0.8f * s + ctx.camUp.y * 0.35f * s,
                hz + ctx.camRight.z * side * 0.8f * s + ctx.camUp.z * 0.35f * s, 0.9f * s, EYE, 0.95f);
    }

    /** A lamprey / leviathan maw lifting out of the crest, ringed with teeth. */
    private void maw(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float ox, float oz, float sc, float mr, float age, int m) {
        float s = sc * mr, neckH = 1.2f * sc * mr;
        strip(buf, ctx, STREAK, x, y - 0.4f, z, ox, oz, neckH, 1.0f * sc * mr, 0.4f * sc, 3.6f * sc, 2.4f * sc, m, age, DEEP, SLATE);
        float mx = x + ox * 1.0f * sc * mr, my = y - 0.2f + neckH + 0.2f * s, mz = z + oz * 1.0f * sc * mr;
        card(buf, ctx, MAW, VfxBlend.ALPHA, mx, my, mz, 1.7f * s, 1.7f * s, Mth.sin(age * 0.25f + m) * 0.25f, false, a(mix(SLATE, WHITE, 0.25f), 0.97f), a(mix(SLATE, WHITE, 0.25f), 0.97f));
    }

    /** Two thick tentacles thrashing up out of the crest. */
    private void tentacles(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float ox, float oz, float sc, float mr, float age, int m, long seed) {
        for (int k = 0; k < 2; k++) {
            float off = (k == 0 ? -0.9f : 1.0f) * sc;
            float len = (k == 0 ? 6.4f : 5.0f) * sc * mr;
            strip(buf, ctx, STREAK, x - oz * off, y - 0.5f, z + ox * off, ox * (k == 0 ? 1f : -0.6f), oz * (k == 0 ? 1f : -0.6f), len, 3.2f * sc * mr * (k == 0 ? 1f : 0.6f), 1.1f * sc,
                    2.6f * sc, 0.5f * sc, m * 1.7f + k * 2.3f, age, DEEP, mix(SLATE, MIDTEAL, 0.5f));
        }
    }

    /** A saw-backed fin cutting the crest, with a pale claw-rake across it at the surge. */
    private void fin(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float ox, float oz, float sc, float mr, float age, int m) {
        float s = sc * mr, tx = -oz, tz = ox, w = 1.5f * s, h = 5.0f * s;
        p0.set(x - tx * w, y - 0.5f, z - tz * w); p1.set(x + tx * w, y - 0.5f, z + tz * w);
        p2.set(x + tx * 0.2f * s + ox * 1.6f * s, y + h, z + tz * 0.2f * s + oz * 1.6f * s); p3.set(x - tx * 0.2f * s + ox * 1.6f * s, y + h, z - tz * 0.2f * s + oz * 1.6f * s);
        buf.quad(FIN, VfxBlend.ALPHA, p0, p1, p2, p3, 0f, 0f, 1f, 1f, a(mix(DEEP, SLATE, 0.3f), 0.97f), a(mix(SLATE, MIDTEAL, 0.5f), 0.97f));
        float rake = sub(age, 6.5f, 11f);
        if (rake > 0f && rake < 1f)
            card(buf, ctx, CLAWS, VfxBlend.ADD, x + ox * 1.5f * s, y + 3.2f * s, z + oz * 1.5f * s, 3.0f * s, 3.0f * s, -0.9f + 1.6f * rake, false, a(FOAMC, 0.85f * Mth.sin(rake * Mth.PI)), a(WHITE, 0.85f * Mth.sin(rake * Mth.PI)));
    }

    // ------------------------------------------------------------------ lightning
    private void lightning(VfxVertexBuffer buf, VfxRenderContext ctx, float x, float y, float z, float R, float st, int q, long seed) {
        Vector3f r = tmp2.set(ctx.camRight.x, 0f, ctx.camRight.z);
        if (r.lengthSquared() < 1e-4f) r.set(1f, 0f, 0f);
        r.normalize();
        float top = R * 1.0f, w = R * 0.16f, lx = (hash(seed, q, 33) - 0.5f) * R * 0.5f;
        float rx = r.x, rz = r.z;
        p0.set(x - rx * w, y, z - rz * w); p1.set(x + rx * w, y, z + rz * w);
        p2.set(x + lx + rx * w, y + top, z + rz * w); p3.set(x + lx - rx * w, y + top, z - rz * w);
        buf.quad(BOLT, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, a(0xFFE6FFF2, st), a(0xFF9EE8D8, st));
        glowSpot(buf, ctx, x, y + 0.4f, z, R * 0.55f, 0xFFB6FFE6, 0.7f * st);
        glowSpot(buf, ctx, x + lx, y + top, z, R * 0.9f, 0xFF8CE0D0, 0.35f * st);
    }
}
