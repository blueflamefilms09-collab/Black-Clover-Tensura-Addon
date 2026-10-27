package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's "Megiddo" (Tensura, the holy-light barrage from the sky): one clean pillar of pale gold-white light from the sky to the ground, white core, a thin cyan edge.
 * <ul>
 *   <li>0-6 (anticipation): a faint lock-on line hangs from the sky to the ground point while a thin bright streak drops down it, accelerating, and reaches the ground at age 5.6; on the
 *       ground a small golden circle with rays opens.</li>
 *   <li>6 (IMPACT, the server sends the effect 6 ticks before the damage lands): the line thickens at once into the full pillar, a large flash and a ring shockwave (white-cyan, gold and a
 *       spiked halo) race out to ~3x the radius, sparks and light shards fly up and out.</li>
 *   <li>8-14: the pillar holds and shimmers (a noisy light ribbon slides over it, the cyan edge flickers) while four halo rings slide down it.</li>
 *   <li>14-18: the pillar narrows and fades from the top down, leaving a glowing cracked scorch ring and rising motes.</li>
 * </ul>
 * {@code inst.from} = the point in the sky (~60 up), {@code inst.to} = the ground point, {@code inst.power} = the beam radius (~3.5). The pillar is a handful of long camera-facing
 * ribbons (one quad each), never many segments. Worst case about 300 of the 400 vertices (all quads), constant loop counts, deterministic from seed and age; the
 * {@link VfxRenderContext#seg} detail scale only thins the optional sparks / shards / motes.
 */
public class VeldMegiddoLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SHAFT = t("judgment_shaft"), BEAM = t("judgment_beam"), THIN = t("celestial_shaft"), NOISE = t("bfx_pillar"),
            RING_C = t("stv_b_ring"), RING_G = t("spiritlight_ring"), HALO = t("bfx_halo"), RING_SOFT = t("demon_light_ring"), SIGIL = t("seal_rings"), RAYS = t("stv_b_rays"),
            GLOW = t("stv_b_glow"), FLARE = t("stv_b_flare"), STAR = t("light_star"), CROSS = t("spark"), CRACKS = t("judgment_cracks_ground"), MOTE = t("judgment_mote"),
            GLINT = t("celestial_glint"), SPECK = t("solomon_mote");

    private static final int WHITE = 0xFFFFFFFF, GOLD = 0xFFFFF1B0, DEEP_GOLD = 0xFFFFD36B, CYAN = 0xFF9FEAFF, SCORCH = 0xFF4A3412;
    private static final int SPARKS = 14, SHARDS = 10, MOTES = 12, HALOS = 4;
    private static final float IMPACT = 6.0f, DROP_END = 5.6f, SKY_LINE = 28f, HALO_SPAN = 36f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_MEGIDDO); }
    @Override public int defaultDuration(VfxShape s) { return 18; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFF1B0; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.02f && buf.hasBudget(4)) buf.billboard(ctx, tex, VfxBlend.ADD, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && (col >>> 24) > 2 && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    /** A ribbon from 'lo' to 'hi', turned to face the camera round its own axis; colour lo -> hi, width wLo -> wHi, texture window v0..v1 (v1 at the 'lo' end). */
    private static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f lo, Vector3f hi, float wLo, float wHi, int cLo, int cHi, float v0, float v1) {
        if (!buf.hasBudget(4) || (cLo >>> 24) + (cHi >>> 24) < 4) return;
        float ax = hi.x - lo.x, ay = hi.y - lo.y, az = hi.z - lo.z;
        float mx = (lo.x + hi.x) * 0.5f, my = (lo.y + hi.y) * 0.5f, mz = (lo.z + hi.z) * 0.5f;
        // side = axis x (towards the camera, which sits at the origin of the camera-relative space)
        float sx = ay * -mz - az * -my, sy = az * -mx - ax * -mz, sz = ax * -my - ay * -mx;
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-6f) return;
        sx /= sl; sy /= sl; sz /= sl;
        float l = wLo * 0.5f, h = wHi * 0.5f;
        buf.quad(tex, bl, new Vector3f(lo.x - sx * l, lo.y - sy * l, lo.z - sz * l), new Vector3f(lo.x + sx * l, lo.y + sy * l, lo.z + sz * l),
                new Vector3f(hi.x + sx * h, hi.y + sy * h, hi.z + sz * h), new Vector3f(hi.x - sx * h, hi.y - sy * h, hi.z - sz * h), 0f, v0, 1f, v1, bl.grade(cLo, 1f), bl.grade(cHi, 1f));
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power), s = R / 3.5f;
        long seed = inst.seed;
        Vector3f sky = ctx.rel(inst.from(ctx)), c = ctx.rel(inst.to(ctx));
        float height = sky.y - c.y;
        if (height < 2f) return;
        float out = 1f - sub(age, 16.2f, 18f);
        if (out <= 0.01f) return;
        float dirX = sky.x - c.x, dirZ = sky.z - c.z;                       // the pillar's lean (0 when straight)

        float post = age - IMPACT;                                          // ticks since the impact
        boolean hit = post >= -0.05f;

        // ---------------------------------------------------------------- ground: the golden circle with rays (anticipation) that the impact blows open
        float open = VfxAnim.easeOutCubic(sub(age, 0.2f, 5.5f));
        float blow = sub(age, IMPACT, IMPACT + 5f);
        float circ = R * (0.55f + 0.5f * open + 1.3f * VfxAnim.easeOutCubic(blow));
        float circA = (0.2f + 0.8f * sub(age, 0.2f, 3f)) * (1f - 0.75f * sub(age, IMPACT + 1f, IMPACT + 8f)) * out;
        ground(buf, SIGIL, VfxBlend.ADD, c, 0.06f, circ, age * 0.05f, a(DEEP_GOLD, 0.85f * circA));
        ground(buf, RAYS, VfxBlend.ADD, c, 0.08f, circ * 1.25f, -age * 0.07f, a(GOLD, 0.55f * circA));
        ground(buf, GLOW, VfxBlend.ADD, c, 0.1f, circ * 1.1f, 0f, a(GOLD, 0.55f * circA * (1f - 0.7f * blow)));

        // ---------------------------------------------------------------- anticipation: the lock-on line and the bright streak that drops down it
        if (age < IMPACT + 0.4f) {
            float lineA = sub(age, 0f, 1.5f) * (1f - sub(age, IMPACT - 0.2f, IMPACT + 0.4f));
            ribbon(buf, THIN, VfxBlend.ADD, c, sky, R * 0.16f, R * 0.1f, a(GOLD, 0.5f * lineA), a(GOLD, 0.12f * lineA), 0f, 1f);
            float f = cl(age / DROP_END), e = f * f * (0.35f + 0.65f * f);
            float headY = Mth.lerp(e, sky.y, c.y);
            float tailY = Math.min(sky.y, headY + SKY_LINE * (1f + 3f * sub(age, DROP_END - 0.6f, IMPACT)));
            float hf = (headY - c.y) / height, tf = (tailY - c.y) / height;
            Vector3f head = new Vector3f(c.x + dirX * hf, headY, c.z + dirZ * hf), tail = new Vector3f(c.x + dirX * tf, tailY, c.z + dirZ * tf);
            float sa = sub(age, 0f, 0.8f) * (1f - sub(age, IMPACT - 0.1f, IMPACT + 0.4f));
            ribbon(buf, THIN, VfxBlend.ADD, head, tail, R * 0.45f, R * 0.16f, a(CYAN, 0.8f * sa), a(CYAN, 0.0f), 0f, 1f);
            ribbon(buf, BEAM, VfxBlend.ADD, head, tail, R * 0.26f, R * 0.08f, a(WHITE, sa), a(GOLD, 0f), 0f, 1f);
            if (buf.hasBudget(8)) {
                sprite(ctx, buf, STAR, head.x, head.y, head.z, R * (1.6f + 1.5f * f), age * 0.3f, a(WHITE, 0.9f * sa));
                sprite(ctx, buf, GLOW, head.x, head.y, head.z, R * (1.2f + 1.2f * f), 0f, a(GOLD, 0.6f * sa));
            }
        }

        // ---------------------------------------------------------------- impact: the flash at the foot of the pillar and the shock rings
        if (hit) {
            float fl = 1f - sub(post, 0f, 3.6f);
            if (fl > 0f) {
                float fe = 1f - fl;
                float y = c.y + R * 0.9f;
                sprite(ctx, buf, FLARE, c.x, y, c.z, R * (5f + 5f * fe), post * 0.05f, a(WHITE, 0.95f * fl));
                sprite(ctx, buf, STAR, c.x, y, c.z, R * (3.5f + 6f * fe), 0.4f + post * 0.12f, a(GOLD, 0.9f * fl));
                sprite(ctx, buf, CROSS, c.x, y, c.z, R * (3f + 4f * fe), 0.2f, a(WHITE, 0.8f * fl * fl));
                sprite(ctx, buf, GLOW, c.x, y, c.z, R * (3.2f + 3f * fe), 0f, a(WHITE, 0.85f * fl));
            }
            float w1 = VfxAnim.easeOutCubic(sub(post, 0f, 5.2f)), w2 = VfxAnim.easeOutCubic(sub(post, 0.6f, 6.4f)), w3 = VfxAnim.easeOutCubic(sub(post, 1.4f, 8f));
            float f1 = (1f - sub(post, 2.2f, 6f)) * out, f2 = (1f - sub(post, 3f, 7.4f)) * out, f3 = (1f - sub(post, 4f, 9.5f)) * out;
            ground(buf, RING_C, VfxBlend.ADD, c, 0.12f, R * (0.4f + 2.6f * w1), 0f, a(WHITE, 0.95f * f1));
            ground(buf, HALO, VfxBlend.ADD, c, 0.14f, R * (0.3f + 2.4f * w2), post * 0.06f, a(GOLD, 0.8f * f2));
            ground(buf, RING_G, VfxBlend.ADD, c, 0.16f, R * (0.3f + 2.0f * w3), 0f, a(DEEP_GOLD, 0.8f * f3));
            ground(buf, RING_SOFT, VfxBlend.ADD, c, 0.18f, R * (0.2f + 3.1f * w1), 0f, a(CYAN, 0.55f * f1 * sub(post, 0f, 0.8f)));
        }

        // ---------------------------------------------------------------- the pillar: cyan edge, gold body, white core and a shimmer of light sliding over it
        if (hit) {
            float grow = VfxAnim.easeOutBack(sub(post, 0f, 1.5f));
            float thin = 0.16f * (1f - sub(post, 0f, 0.5f));
            float wf = Math.max(thin, grow);
            float fade = sub(age, 14f, 18f);
            float topF = VfxAnim.easeInCubic(fade);                         // the top comes down: the pillar is eaten from above
            float top = 1f - topF;
            float narrow = 1f - 0.72f * VfxAnim.easeInCubic(fade);
            float hold = 1f - 0.9f * sub(age, 16.4f, 18f);
            float flicker = 1f + 0.07f * Mth.sin(age * 2.3f + hash(seed, 1, 1) * 6f) + 0.05f * (hash(seed, (int) (age * 3f), 2) - 0.5f);
            float burst = 1f + 0.55f * (1f - sub(post, 0f, 2.6f));          // wider and whiter in the first ticks
            float wp = wf * narrow * flicker;
            Vector3f hi = new Vector3f(c.x + dirX * top, c.y + height * top, c.z + dirZ * top);
            float edgeA = (0.55f + 0.2f * hash(seed, (int) (age * 2.5f), 3)) * hold;
            ribbon(buf, SHAFT, VfxBlend.ADD, c, hi, R * 3.0f * wp * burst, R * 2.8f * wp * burst, a(CYAN, 0.42f * edgeA), a(CYAN, 0.22f * edgeA), 0f, 1f);
            ribbon(buf, SHAFT, VfxBlend.ADD, c, hi, R * 2.3f * wp * burst, R * 2.15f * wp * burst, a(DEEP_GOLD, 0.8f * hold), a(DEEP_GOLD, 0.55f * hold), 0f, 1f);
            ribbon(buf, SHAFT, VfxBlend.ADD, c, hi, R * 1.7f * wp * burst, R * 1.6f * wp * burst, a(GOLD, 0.8f * hold), a(GOLD, 0.6f * hold), 0f, 1f);
            ribbon(buf, BEAM, VfxBlend.ADD, c, hi, R * 0.95f * wp * burst, R * 0.9f * wp * burst, a(WHITE, 0.9f * hold), a(WHITE, 0.7f * hold), 0f, 1f);
            ribbon(buf, THIN, VfxBlend.ADD, c, hi, R * 0.6f * wp, R * 0.55f * wp, a(WHITE, hold), a(WHITE, hold), 0f, 1f);
            float shim = sub(post, 1.2f, 3f) * hold;
            float sc = (age * 0.11f) % 0.5f, sc2 = (age * 0.073f + 0.23f) % 0.5f;
            ribbon(buf, NOISE, VfxBlend.ADD, c, hi, R * 2.1f * wp, R * 1.95f * wp, a(DEEP_GOLD, 0.6f * shim), a(DEEP_GOLD, 0.4f * shim), sc, sc + 0.5f);
            ribbon(buf, NOISE, VfxBlend.ADD, c, hi, R * 1.4f * wp, R * 1.3f * wp, a(GOLD, 0.5f * shim), a(GOLD, 0.3f * shim), sc2, sc2 + 0.5f);

            // the halo rings slide down the pillar (tilted a little towards the camera so they read as rings from any side)
            float hl = sub(age, 7.6f, 8.6f) * (1f - sub(age, 13.6f, 15.5f));
            if (hl > 0f) {
                float tx = -c.x, tz = -c.z, tl = Mth.sqrt(tx * tx + tz * tz) + 1e-4f;
                Vector3f tilt = new Vector3f(tx / tl * 0.55f, 1f, tz / tl * 0.55f);
                for (int k = 0; k < HALOS; k++) {
                    float u = sub(age, 7.4f + 1.1f * k, 12.6f + 1.1f * k);
                    if (u <= 0f || u >= 1f) continue;
                    float hy = HALO_SPAN * s * (1f - u) * (1f - u * 0.2f) + 0.5f;
                    float rr = R * (1.55f + 0.25f * Mth.sin(u * 6.28f + k)) * narrow;
                    float al = Mth.sin(u * Mth.PI) * hl * out;
                    float lean = hy / height;
                    Vector3f at = new Vector3f(c.x + dirX * lean, c.y + hy, c.z + dirZ * lean);
                    if (buf.hasBudget(8)) {
                        buf.plane(RING_SOFT, VfxBlend.ADD, VfxPose.facing(at, tilt).spin(age * 0.1f + k), rr, a(k % 2 == 0 ? GOLD : CYAN, 0.8f * al));
                        buf.plane(RING_C, VfxBlend.ADD, VfxPose.facing(at, tilt), rr * 0.97f, a(WHITE, 0.55f * al));
                    }
                }
            }
        }

        // ---------------------------------------------------------------- sparks and light shards thrown up and out of the impact
        if (hit && post < 12f) {
            int n = ctx.seg(SPARKS, 6);
            for (int k = 0; k < n; k++) {
                float ang = Mth.TWO_PI * (k + hash(seed, k, 4)) / n, vh = (0.5f + 1.1f * hash(seed, k, 5)) * s * 0.55f, vy = (1.6f + 2.4f * hash(seed, k, 6)) * s * 0.55f, g = 0.2f * s * 0.55f;
                float tt = post * (0.9f + 0.2f * hash(seed, k, 7));
                float al = (1f - sub(post, 6f + 4f * hash(seed, k, 8), 12f)) * sub(post, 0f, 0.4f) * out;
                float py = Math.max(0f, vy * tt - 0.5f * g * tt * tt);
                sprite(ctx, buf, k % 3 == 0 ? CROSS : MOTE, c.x + Mth.cos(ang) * vh * tt * 1.4f, c.y + 0.3f + py, c.z + Mth.sin(ang) * vh * tt * 1.4f, (0.35f + 0.5f * hash(seed, k, 9)) * s * 1.3f, age * 0.2f + k, a(k % 4 == 1 ? CYAN : (k % 2 == 0 ? WHITE : GOLD), 0.95f * al));
            }
            int m = ctx.seg(SHARDS, 4);
            for (int k = 0; k < m; k++) {
                float ang = Mth.TWO_PI * (k + 0.5f * hash(seed, k, 10)) / m, vh = (1.2f + 1.6f * hash(seed, k, 11)) * s * 0.55f, vy = (2.4f + 3.2f * hash(seed, k, 12)) * s * 0.55f, g = 0.3f * s * 0.55f;
                float tt = post;
                float al = (1f - sub(post, 3f + 3f * hash(seed, k, 13), 7.5f)) * sub(post, 0f, 0.3f) * out;
                if (al <= 0.01f) continue;
                float cx = Mth.cos(ang), cz = Mth.sin(ang);
                float px = c.x + cx * vh * tt, py = c.y + 0.3f + Math.max(0f, vy * tt - 0.5f * g * tt * tt), pz = c.z + cz * vh * tt;
                float vy2 = vy - g * tt, len = (1.3f + 1.4f * hash(seed, k, 14)) * s;
                float dl = Mth.sqrt(vh * vh + vy2 * vy2) + 1e-4f;
                Vector3f head = new Vector3f(px, py, pz), tail = new Vector3f(px - cx * vh / dl * len, Math.max(c.y + 0.1f, py - vy2 / dl * len), pz - cz * vh / dl * len);
                ribbon(buf, THIN, VfxBlend.ADD, tail, head, 0.05f * s, 0.4f * s, a(k % 3 == 0 ? CYAN : GOLD, 0f), a(WHITE, 0.95f * al), 0f, 1f);
            }
        }

        // ---------------------------------------------------------------- aftermath: the glowing, cracked scorch ring and the motes rising off it
        if (post > 1f) {
            float sf = sub(post, 1f, 4f), sl = sf * (1f - 0.35f * sub(age, 15f, 18f)) * out;
            ground(buf, GLOW, VfxBlend.ALPHA, c, 0.04f, R * 2.3f, 0f, a(SCORCH, 0.6f * sf * out));
            ground(buf, GLOW, VfxBlend.ADD, c, 0.1f, R * 1.9f, 0f, a(DEEP_GOLD, 0.4f * sl));
            ground(buf, CRACKS, VfxBlend.ADD, c, 0.13f, R * 2.2f * (0.75f + 0.25f * VfxAnim.easeOutCubic(sf)), hash(seed, 0, 20) * 6.28f, a(GOLD, 0.8f * sl));
            ground(buf, RING_SOFT, VfxBlend.ADD, c, 0.15f, R * 1.75f, 0f, a(GOLD, 0.55f * sl * (0.8f + 0.2f * Mth.sin(age * 0.9f))));
            ground(buf, RING_G, VfxBlend.ADD, c, 0.17f, R * 1.35f, age * 0.03f, a(WHITE, 0.45f * sl));
        }
        float mo = sub(age, 9f, 12f) * out;
        if (mo > 0f) {
            int n = ctx.seg(MOTES, 4);
            for (int k = 0; k < n; k++) {
                float ang = hash(seed, k, 15) * Mth.TWO_PI, rr = R * (0.5f + 1.7f * Mth.sqrt(hash(seed, k, 16)));
                float life = 6f + 4f * hash(seed, k, 17), ph = ((age - 9f) / life + hash(seed, k, 18)) % 1f;
                float rise = (0.5f + 6f * ph) * s + 0.6f * s * hash(seed, k, 19);
                float tw = 0.6f + 0.4f * Mth.sin(age * 0.6f + k * 1.9f);
                sprite(ctx, buf, k % 3 == 0 ? GLINT : SPECK, c.x + Mth.cos(ang) * rr + 0.3f * s * Mth.sin(age * 0.2f + k), c.y + rise, c.z + Mth.sin(ang) * rr,
                        (0.5f + 0.5f * hash(seed, k, 21)) * s * 0.9f, age * 0.1f, a(k % 4 == 0 ? CYAN : GOLD, 0.9f * mo * tw * Mth.sin(ph * Mth.PI)));
            }
        }
    }
}
