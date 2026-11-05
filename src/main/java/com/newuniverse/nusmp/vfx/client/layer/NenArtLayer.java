package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.57.0 Nen (Hunter x Hunter), the remade look (docs/nen_rework.md; textures from tools/gen_nen_textures.py; the 3.48.5 look stays in {@link NenLayer} for {@code nenVfxV2 = false}).
 * Aura in the anime is flame: tongues of light that leave the skin and lick upwards with a white-hot core inside a coloured body. So the aura is real tongues (one tapered ribbon of a flame
 * texture each, a white core ribbon inside it), not puffs, and every hatsu has a signature piece:
 * <ul>
 *   <li>NEN_FLOW (follows the caster; power = 1 + kind: 0 Ten calm veil, 1 Zetsu a few grey tongues sinking, 2 Ren a tall violent blaze with a halo, 3 Ken a dense low shell over a hex sigil, 4 Ko
 *       tongues drawn into one fist that swells with the stored aura (+ charge fraction), 5 Vow violet with orbiting chain links, 6 Shu a stream along the held weapon, 7 Ryu streamers
 *       winding round the body with orbs; the colour is the user's Nen type).</li>
 *   <li>NEN_BURST: flash, starburst, ground shock ring (Ren adds a pillar and a second ring); NEN_SENSE: the En lattice sphere with rings and a sweeping scan; NEN_EYES: the Gyo slit eyes and a
 *       light cone; NEN_FIST: the fist's streak, afterimages, impact with cracks and shards; NEN_BULLET: a homing orb with a curved ribbon trail; NEN_SURGE: lightning (jagged bolt + forks), gum
 *       (a sagging glossy rope) or a rubber shock dome; NEN_LINK: a sagging chain of aura links with a cuff; NEN_NEEDLE: the needle, then strings up to a control bar and a mark; NEN_DOMAIN: the
 *       Specialization field, a hex lattice disc with pulses and rising motes.</li>
 * </ul>
 * Budget: every shape stays under the 400 vertices of an activation (the buffer never lets it pass; worst measured with tools/vfx_preview is in the doc: Ren aura ~300, the rest under 260),
 * constant loop counts, deterministic from the seed and the age, no per-frame lists; {@code ctx.seg} (the distance level of detail of the manager, which also culls far effects) thins the tongues,
 * motes and chain pieces to half or less at a distance.
 */
public class NenArtLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation[] FLAME = {t("nen_flame_0"), t("nen_flame_1"), t("nen_flame_2"), t("nen_flame_3")};
    private static final ResourceLocation RIBBON = t("nen_ribbon"), CORE = t("nen_core"), BURST = t("nen_burst"), RING = t("nen_ring"), HEX = t("nen_hex"), SPHERE = t("nen_sphere"),
            EYE = t("nen_eye"), BOLT = t("nen_bolt"), LINK = t("nen_link"), NEEDLE = t("nen_needle"), GUM = t("nen_gum"), SHARD = t("nen_shard"), CRACK = t("nen_crack"),
            SPARK = t("nen_spark"), MARK = t("nen_mark"), SMOKE = t("chain_smoke");

    private static final int WHITE = 0xFFFFFFFF;
    /** The default colour of each aura kind (the server sends the user's type colour; this is the fallback). */
    private static final int[] KIND_COLOR = {0xFFDCEEFF, 0xFF8C93A8, 0xFFFFC060, 0xFF9CC4FF, 0xFFFF8A4A, 0xFFB070FF, 0xFF7CFFD8, 0xFFFF7ADA};

    @Override public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.NEN_FLOW, VfxShape.NEN_BURST, VfxShape.NEN_SENSE, VfxShape.NEN_EYES, VfxShape.NEN_FIST, VfxShape.NEN_BULLET, VfxShape.NEN_SURGE, VfxShape.NEN_LINK,
                VfxShape.NEN_NEEDLE, VfxShape.NEN_DOMAIN);
    }

    @Override public int defaultDuration(VfxShape s) {
        return switch (s) { case NEN_FLOW, NEN_SENSE -> 50; case NEN_EYES -> 26; case NEN_BURST -> 18; case NEN_FIST -> 20; case NEN_BULLET -> 10; case NEN_SURGE -> 14; case NEN_LINK -> 14;
            case NEN_NEEDLE -> 40; default -> 120; };
    }

    @Override public int defaultColor(VfxShape s) {
        return switch (s) { case NEN_BURST, NEN_FIST -> 0xFFFFB038; case NEN_SENSE -> 0xFF6AFFC0; case NEN_EYES -> 0xFF7AE0FF; case NEN_BULLET -> 0xFF58D8FF; case NEN_SURGE -> 0xFFB8D0FF;
            case NEN_LINK -> 0xFF48E8C0; case NEN_NEEDLE -> 0xFFE8A0FF; case NEN_DOMAIN -> 0xFFFF6E9E; default -> 0xFFDCEEFF; };
    }

    // ---------------------------------------------------------------- helpers
    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    // scratch vectors: the layer draws on the render thread only, and the buffer copies what it is given
    private static final Vector3f P = new Vector3f(), Q = new Vector3f();

    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.01f && buf.hasBudget(4)) buf.billboard(ctx, tex, VfxBlend.ADD, P.set(x, y, z), size, rot, col);
    }

    /** A tapered ribbon between two points that faces the camera (texture: base at the first point, tip at the second). */
    private static void rib(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float ax, float ay, float az, float bx, float by, float bz, float wa, float wb, int ca, int cb) {
        if (buf.hasBudget(4)) buf.beam(ctx, tex, VfxBlend.ADD, P.set(ax, ay, az), Q.set(bx, by, bz), wa, wb, 1, 0f, ca, cb);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, float cx, float cy, float cz, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, VfxBlend.ADD, VfxPose.ground(new Vector3f(cx, cy, cz)).spin(spin), radius, col);
    }

    /** A rectangle facing the camera, w x h (the eyes, the marks). */
    private static void wide(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float w, float h, int col) {
        if (w < 0.01f || !buf.hasBudget(4)) return;
        float hw = w * 0.5f, hh = h * 0.5f;
        Vector3f r = ctx.camRight, u = ctx.camUp;
        Vector3f p0 = new Vector3f(x - r.x * hw - u.x * hh, y - r.y * hw - u.y * hh, z - r.z * hw - u.z * hh);
        Vector3f p1 = new Vector3f(x + r.x * hw - u.x * hh, y + r.y * hw - u.y * hh, z + r.z * hw - u.z * hh);
        Vector3f p2 = new Vector3f(x + r.x * hw + u.x * hh, y + r.y * hw + u.y * hh, z + r.z * hw + u.z * hh);
        Vector3f p3 = new Vector3f(x - r.x * hw + u.x * hh, y - r.y * hw + u.y * hh, z - r.z * hw + u.z * hh);
        buf.quad(tex, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, col, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        switch (inst.shape) {
            case NEN_FLOW -> flow(inst, ctx, buf, age);
            case NEN_BURST -> burst(inst, ctx, buf, age);
            case NEN_SENSE -> sense(inst, ctx, buf, age);
            case NEN_EYES -> eyes(inst, ctx, buf, age);
            case NEN_FIST -> fist(inst, ctx, buf, age);
            case NEN_BULLET -> bullet(inst, ctx, buf, age);
            case NEN_SURGE -> surge(inst, ctx, buf, age);
            case NEN_LINK -> link(inst, ctx, buf, age);
            case NEN_NEEDLE -> needle(inst, ctx, buf, age);
            case NEN_DOMAIN -> domain(inst, ctx, buf, age);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the aura of a window
    //                                  Ten   Zetsu  Ren   Ken   Ko    Vow   Shu   Ryu
    private static final int[] N =       {14,   6,    26,   16,   14,   16,   0,    10};
    private static final float[] RAD =   {0.42f, 0.30f, 0.80f, 0.55f, 0.55f, 0.60f, 0f, 0.58f};
    private static final float[] HGT =   {2.1f, 1.0f, 3.4f, 1.5f, 2.3f, 2.8f, 0f, 2.4f};
    private static final float[] WID =   {0.62f, 0.40f, 1.05f, 0.85f, 0.62f, 0.80f, 0f, 0.70f};
    private static final float[] LEAN =  {0.10f, -0.10f, 0.28f, 0.0f, 0.0f, 0.18f, 0f, 0.12f};
    private static final float[] ALPHA = {0.60f, 0.34f, 0.90f, 0.85f, 0.85f, 0.80f, 0f, 0.75f};
    private static final float[] FLICK = {0.20f, 0.12f, 0.55f, 0.10f, 0.35f, 0.30f, 0f, 0.25f};

    private void flow(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        int kind = Mth.clamp((int) inst.power - 1, 0, 7);
        float charge = Mth.clamp(inst.power - (int) inst.power, 0f, 0.99f);
        float fade = sub(age, 0f, 5f) * (1f - sub(age, inst.duration - 8f, inst.duration));
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : KIND_COLOR[kind];
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);          // forward
        float rx = fz, rz = -fx;                                                                  // right
        float fistX = c.x + fx * 0.55f + rx * 0.3f, fistY = c.y + 1.15f, fistZ = c.z + fz * 0.55f + rz * 0.3f;
        float pulse = 0.5f + 0.5f * Mth.sin(age * (kind == 2 ? 0.5f : 0.2f));

        // ---- the tongues
        if (kind != 6) {
            int n = ctx.seg(N[kind], Math.max(4, N[kind] / 2));
            float wid = WID[kind], hgt = HGT[kind] * (kind == 4 ? 0.8f + 0.3f * charge : 1f);
            for (int k = 0; k < n; k++) {
                float h1 = hash(seed, k, 1), h2 = hash(seed, k, 2), h3 = hash(seed, k, 3);
                float ang = Mth.TWO_PI * k / n + h1 * 0.7f + age * (h3 < 0.5f ? 0.012f : -0.012f) * (kind == 2 ? 2f : 1f);
                float rr = RAD[kind] * (0.8f + 0.4f * h2) * (kind == 1 ? 1f - 0.4f * sub(age, 0f, inst.duration) : 1f);
                float fl = 0.82f + FLICK[kind] * 0.45f * Mth.sin(age * 0.32f + k * 1.9f) + 0.1f * Mth.sin(age * 0.77f + k * 0.7f);
                float len = hgt * (kind == 2 ? 0.8f : 0.55f) * (0.5f + 0.5f * h3) * fl;                  // tongues start along the body, not only at the feet
                float y0 = c.y + 0.05f + (kind == 3 ? 0.5f : kind == 1 ? 0.6f : 1.2f) * h1;
                float x0 = c.x + Mth.cos(ang) * rr, z0 = c.z + Mth.sin(ang) * rr;
                float sway = Mth.sin(age * 0.17f + k * 2.3f) * (0.12f + 0.25f * FLICK[kind]) * len * 0.35f;
                float x1 = x0 + Mth.cos(ang) * LEAN[kind] * len + Mth.cos(ang + 1.57f) * sway;
                float z1 = z0 + Mth.sin(ang) * LEAN[kind] * len + Mth.sin(ang + 1.57f) * sway;
                float y1 = kind == 1 ? y0 + len * (0.6f - 0.3f * pulse) : y0 + len;
                if (kind == 4) {                                                               // Ko: the tongues are drawn into the fist
                    float pull = 0.35f + 0.5f * charge + 0.1f * sub(age, 0f, 20f);
                    x1 = x1 + (fistX - x1) * pull; z1 = z1 + (fistZ - z1) * pull; y1 = y1 + (fistY - y1) * pull * 0.6f;
                }
                float w = wid * (0.8f + 0.4f * h2) * (0.75f + 0.25f * fl);
                float al = ALPHA[kind] * fade * (0.7f + 0.3f * fl);
                ResourceLocation tex = FLAME[k & 3];
                rib(ctx, buf, tex, x0, y0, z0, x1, y1, z1, w, w * 0.8f, a(col, al), a(col, al * 0.25f));
                if (kind != 1) rib(ctx, buf, tex, x0, y0 + len * 0.03f, z0, x0 + (x1 - x0) * 0.82f, y0 + len * 0.82f, z0 + (z1 - z0) * 0.82f, w * 0.42f, w * 0.3f, a(WHITE, al * 0.55f), a(WHITE, 0f));
            }
        } else {                                                                              // Shu: the weapon hand streams forward
            int n = ctx.seg(6, 3);
            for (int k = 0; k < n; k++) {
                float h1 = hash(seed, k, 1), h2 = hash(seed, k, 2);
                float len = (1.1f + 0.7f * h2) * (0.85f + 0.15f * Mth.sin(age * 0.4f + k * 1.7f));
                float off = (h1 - 0.5f) * 0.28f;
                float x0 = fistX, y0 = fistY, z0 = fistZ;
                float x1 = x0 + fx * len + rx * off * 2f, y1 = y0 + 0.1f + (h1 - 0.5f) * 0.35f + 0.1f * Mth.sin(age * 0.3f + k), z1 = z0 + fz * len + rz * off * 2f;
                rib(ctx, buf, FLAME[k & 3], x0, y0, z0, x1, y1, z1, 0.5f, 0.3f, a(col, 0.9f * fade), a(col, 0.1f));
                rib(ctx, buf, FLAME[k & 3], x0, y0, z0, x0 + (x1 - x0) * 0.7f, y0 + (y1 - y0) * 0.7f, z0 + (z1 - z0) * 0.7f, 0.22f, 0.14f, a(WHITE, 0.6f * fade), a(WHITE, 0f));
            }
            glow(ctx, buf, CORE, fistX, fistY, fistZ, 0.9f + 0.2f * pulse, 0f, a(col, 0.8f * fade));
            glow(ctx, buf, CORE, c.x, c.y + 1.0f, c.z, 1.6f, 0f, a(col, 0.18f * fade));
        }

        // ---- the body shell and the ground
        float shell = switch (kind) { case 0 -> 0.22f; case 2 -> 0.45f; case 3 -> 0.30f; case 5 -> 0.25f; case 7 -> 0.22f; default -> 0.08f; };
        if (shell > 0.1f) glow(ctx, buf, CORE, c.x, c.y + 1.0f, c.z, kind == 2 ? 3.0f + 0.5f * pulse : 2.2f, 0f, a(col, shell * fade));
        float gr = switch (kind) { case 1 -> 1.1f - 0.5f * sub(age, 0f, inst.duration); case 2 -> 1.9f + 0.5f * pulse; case 3 -> 1.35f; default -> 1.0f; };
        ground(buf, RING, c.x, c.y + 0.05f, c.z, gr, age * (kind == 3 ? 0.01f : 0.03f), a(col, (kind == 1 ? 0.28f : 0.6f) * fade));
        if (kind == 3 || kind == 5 || kind == 2) ground(buf, HEX, c.x, c.y + 0.08f, c.z, gr * (kind == 2 ? 1.3f : 0.9f), -age * 0.02f, a(kind == 5 ? KIND_COLOR[5] : WHITE, (kind == 2 ? 0.28f : 0.4f) * fade * (0.6f + 0.4f * pulse)));

        // ---- the signature of each kind
        if (kind == 1 && buf.hasBudget(4)) buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, P.set(c.x, c.y + 0.9f, c.z), 1.7f, age * 0.01f, a(0xFF202838, 0.3f * fade));     // the one dark thing: a faint smoke, additive light cannot darken
        if (kind == 2) {                                                                      // Ren: a flaring halo and sparks flung out
            glow(ctx, buf, BURST, c.x, c.y + 1.0f, c.z, 3.4f + 1.0f * pulse, age * 0.02f, a(col, 0.35f * fade));
            int sp = ctx.seg(8, 3);
            for (int k = 0; k < sp; k++) {
                float ph = (age * 0.045f + hash(seed, k, 8)) % 1f, ang = hash(seed, k, 9) * Mth.TWO_PI + k;
                float r = 0.6f + 1.8f * ph;
                glow(ctx, buf, SPARK, c.x + Mth.cos(ang) * r, c.y + 0.4f + 2.6f * ph, c.z + Mth.sin(ang) * r, 0.34f * (1f - ph), ang, a(WHITE, 0.9f * (1f - ph) * fade));
            }
        }
        if (kind == 4) {                                                                      // Ko: the fist
            float g = 0.45f + 0.8f * charge + 0.1f * pulse;
            glow(ctx, buf, CORE, fistX, fistY, fistZ, 0.7f + 1.5f * g, 0f, a(col, 0.95f * fade));
            glow(ctx, buf, BURST, fistX, fistY, fistZ, 0.5f + 1.3f * g, age * 0.15f, a(WHITE, (0.4f + 0.5f * charge) * fade));
            glow(ctx, buf, CORE, fistX, fistY, fistZ, 0.3f + 0.7f * g, 0f, a(WHITE, 0.9f * fade));
        }
        if (kind == 5) {                                                                      // Vow: chains of light winding round the body
            int links = ctx.seg(7, 3);
            for (int k = 0; k < links; k++) {
                float ang = Mth.TWO_PI * k / links + age * 0.09f, y = c.y + 0.25f + 0.34f * k;
                float nx = Mth.cos(ang + 0.5f), nz = Mth.sin(ang + 0.5f);
                float x = c.x + Mth.cos(ang) * 0.72f, z = c.z + Mth.sin(ang) * 0.72f;
                rib(ctx, buf, LINK, x - nx * 0.2f, y, z - nz * 0.2f, x + nx * 0.2f, y + 0.07f, z + nz * 0.2f, 0.2f, 0.2f, a(col, 0.9f * fade), a(col, 0.9f * fade));
            }
        }
        if (kind == 7) {                                                                      // Ryu: streamers winding round the body, orbs riding them
            int segs = ctx.seg(9, 4);
            for (int hx = 0; hx < 2; hx++) {
                float px = 0, py = 0, pz = 0;
                for (int k = 0; k <= segs; k++) {
                    float f = (float) k / segs, ang = age * 0.1f * (hx == 0 ? 1 : -1) + f * 7.0f + hx * 3.14f;
                    float x = c.x + Mth.cos(ang) * 0.66f, y = c.y + 0.1f + f * 2.1f, z = c.z + Mth.sin(ang) * 0.66f;
                    if (k > 0) rib(ctx, buf, RIBBON, px, py, pz, x, y, z, 0.2f * Mth.sin(f * Mth.PI) + 0.04f, 0.2f * Mth.sin((f + 1f / segs) * Mth.PI) + 0.04f, a(col, 0.8f * fade), a(col, 0.8f * fade));
                    px = x; py = y; pz = z;
                }
            }
            int orbs = ctx.seg(3, 2);
            for (int k = 0; k < orbs; k++) {
                float ang = Mth.TWO_PI * k / orbs + age * 0.13f;
                glow(ctx, buf, CORE, c.x + Mth.cos(ang) * 0.7f, c.y + 0.5f + 0.7f * k, c.z + Mth.sin(ang) * 0.7f, 0.5f, 0f, a(col, 0.85f * fade));
            }
        }
        // embers rising off the aura
        int em = kind == 1 || kind == 2 || kind == 6 ? 0 : ctx.seg(6, 2);
        for (int k = 0; k < em; k++) {
            float ph = (age * 0.022f + hash(seed, k, 11)) % 1f, ang = hash(seed, k, 12) * Mth.TWO_PI;
            float r = RAD[Math.min(kind, 7)] * (1.0f + 0.6f * ph);
            glow(ctx, buf, SPARK, c.x + Mth.cos(ang + age * 0.02f) * r, c.y + 0.3f + HGT[kind] * 0.9f * ph, c.z + Mth.sin(ang + age * 0.02f) * r, 0.22f * (1f - ph), ang, a(WHITE, 0.7f * (1f - ph) * fade));
        }
    }

    // ---------------------------------------------------------------- Ren's burst and every blow
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float s = Math.max(0.6f, inst.power);
        boolean ren = s >= 1.8f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFFFFB038;
        float flash = 1f - sub(age, 0f, 6f), wave = VfxAnim.easeOutCubic(sub(age, 0f, 11f)), out = 1f - sub(age, 5f, inst.duration), w2 = VfxAnim.easeOutCubic(sub(age, 3f, 14f));
        glow(ctx, buf, CORE, c.x, c.y, c.z, s * (1.4f + 2.2f * sub(age, 0f, 8f)), 0f, a(col, 0.85f * flash));
        glow(ctx, buf, CORE, c.x, c.y, c.z, s * (0.8f + 1.2f * sub(age, 0f, 6f)), 0f, a(WHITE, 0.95f * flash));
        glow(ctx, buf, BURST, c.x, c.y, c.z, s * (1.6f + 3.2f * wave), age * 0.04f, a(col, 0.9f * out * (1f - 0.5f * wave)));
        glow(ctx, buf, BURST, c.x, c.y, c.z, s * (1.0f + 2.0f * wave), -age * 0.07f, a(WHITE, 0.6f * out * (1f - wave)));
        ground(buf, RING, c.x, c.y - (ren ? 1.0f : 0.4f), c.z, s * 2.6f * wave, 0f, a(col, 0.9f * out));
        ground(buf, RING, c.x, c.y - (ren ? 1.0f : 0.4f) + 0.02f, c.z, s * 1.9f * w2, age * 0.05f, a(WHITE, 0.6f * out));
        if (ren) {
            float up = VfxAnim.easeOutCubic(sub(age, 0f, 8f));
            rib(ctx, buf, FLAME[0], c.x, c.y - 1.0f, c.z, c.x, c.y - 1.0f + 6.5f * up, c.z, 1.5f * (1f - 0.5f * sub(age, 4f, inst.duration)), 0.7f, a(col, 0.8f * out), a(col, 0.05f));
            rib(ctx, buf, RIBBON, c.x, c.y - 1.0f, c.z, c.x, c.y - 1.0f + 6.5f * up, c.z, 0.55f, 0.25f, a(WHITE, 0.7f * out), a(WHITE, 0f));
        }
        int rays = ctx.seg(ren ? 12 : 8, 4);
        for (int k = 0; k < rays; k++) {
            float ang = Mth.TWO_PI * k / rays + hash(seed, k, 5), el = (hash(seed, k, 6) - 0.4f) * 1.3f;
            float r0 = s * 0.5f * wave, r1 = s * (1.2f + 2.2f * wave * (0.6f + 0.4f * hash(seed, k, 7)));
            float dx = Mth.cos(ang) * Mth.cos(el), dy = Mth.sin(el), dz = Mth.sin(ang) * Mth.cos(el);
            rib(ctx, buf, RIBBON, c.x + dx * r0, c.y + dy * r0, c.z + dz * r0, c.x + dx * r1, c.y + dy * r1, c.z + dz * r1, 0.2f * s, 0.03f, a(k % 2 == 0 ? WHITE : col, 0.9f * out), a(col, 0f));
        }
    }

    // ---------------------------------------------------------------- En: the lattice sphere
    private void sense(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float R = Math.max(3f, (float) Math.floor(inst.power));
        boolean refresh = inst.power - (float) Math.floor(inst.power) >= 0.25f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF6AFFC0;
        float open = refresh ? 1f : VfxAnim.easeOutCubic(sub(age, 0f, 12f));
        float in = sub(age, 0f, refresh ? 4f : 1f), out = 1f - sub(age, inst.duration - 8f, inst.duration), f = in * out;
        if (f <= 0.01f) return;
        float cy = c.y + 1.0f, r = R * open;
        glow(ctx, buf, SPHERE, c.x, cy, c.z, r * 2.05f, 0f, a(col, 0.6f * f));
        int lat = ctx.seg(5, 3);
        for (int k = 0; k < lat; k++) {                                                        // latitude rings that breathe
            float y = ((k + 0.5f) / lat - 0.5f) * 2f * r * 0.85f + 0.12f * r * Mth.sin(age * 0.05f + k);
            float rad = Mth.sqrt(Math.max(0.01f, r * r - y * y));
            ground(buf, RING, c.x, cy + y, c.z, rad, age * (0.012f + 0.004f * k) * (k % 2 == 0 ? 1 : -1), a(col, 0.42f * f));
        }
        float sw = (age * 0.03f) % 1f, sy = -r + 2f * r * sw, srad = Mth.sqrt(Math.max(0.01f, r * r - sy * sy));
        ground(buf, RING, c.x, cy + sy, c.z, srad, 0f, a(WHITE, 0.9f * f * Mth.sin(sw * Mth.PI)));                                 // the scan
        ground(buf, HEX, c.x, c.y + 0.06f, c.z, r, age * 0.01f, a(col, 0.5f * f));
        ground(buf, RING, c.x, c.y + 0.08f, c.z, r, 0f, a(col, 0.8f * f));
        int motes = ctx.seg(18, 6);
        for (int k = 0; k < motes; k++) {
            float u = hash(seed, k, 3) * 2f - 1f, ang = hash(seed, k, 4) * Mth.TWO_PI + age * 0.006f, s = Mth.sqrt(1f - u * u);
            float tw = 0.35f + 0.65f * Mth.sin(age * 0.35f + k * 1.7f) * Mth.sin(age * 0.35f + k * 1.7f);
            glow(ctx, buf, SPARK, c.x + Mth.cos(ang) * s * r, cy + u * r, c.z + Mth.sin(ang) * s * r, 0.5f, ang, a(k % 4 == 0 ? WHITE : col, 0.85f * tw * f));
        }
        if (!refresh) glow(ctx, buf, CORE, c.x, cy, c.z, 3f * (1f - open) + 1f, 0f, a(WHITE, 0.7f * (1f - open) * f));
    }

    // ---------------------------------------------------------------- Gyo: the eyes
    private void eyes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF7AE0FF;
        float close = sub(age, 0f, 8f), open = sub(age, 5f, 12f), out = 1f - sub(age, 16f, inst.duration), f = open * out;
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD), rx = fz, rz = -fx;
        float hx = c.x + fx * 0.22f, hy = c.y + 1.64f, hz = c.z + fz * 0.22f;
        glow(ctx, buf, BURST, c.x, hy, c.z, 2.2f * (1f - 0.55f * close), age * 0.2f, a(col, 0.55f * close * out));
        for (int e = -1; e <= 1; e += 2) {
            float ex = hx + rx * 0.11f * e, ez = hz + rz * 0.11f * e;
            wide(ctx, buf, EYE, ex, hy, ez, 0.62f * open, 0.31f * open, a(col, 0.95f * f));
            glow(ctx, buf, CORE, ex, hy, ez, 0.55f * open, 0f, a(WHITE, 0.55f * f));
            for (int k = 0; k < 2; k++) {                                                      // the light cone: two soft beams from each eye
                float spread = (k == 0 ? -0.35f : 0.35f);
                rib(ctx, buf, RIBBON, ex, hy, ez, ex + fx * 3.6f + rx * spread * 1.5f, hy - 0.2f, ez + fz * 3.6f + rz * spread * 1.5f, 0.12f, 0.9f, a(col, 0.28f * f), a(col, 0f));
            }
        }
        wide(ctx, buf, MARK, hx + fx * 1.4f, hy - 0.1f, hz + fz * 1.4f, 0.8f * open, 0.8f * open, a(col, 0.55f * f));
    }

    // ---------------------------------------------------------------- Enhancement: the fist
    private void fist(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float charge = Mth.clamp(inst.power - 1f, 0f, 1f), s = 1f + 0.8f * charge;
        long seed = inst.seed;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        int col = inst.color != 0 ? inst.color : 0xFFFFB038;
        float run = VfxAnim.easeOutCubic(sub(age, 0f, 5f)), out = 1f - sub(age, 6f, inst.duration);
        float hx = from.x + (to.x - from.x) * run, hy = from.y + (to.y - from.y) * run, hz = from.z + (to.z - from.z) * run;
        float trail = 1f - sub(age, 4f, 14f);
        if (run > 0.03f && trail > 0.01f) {
            rib(ctx, buf, FLAME[1], from.x, from.y, from.z, hx, hy, hz, 0.35f * s, 1.4f * s, a(col, 0.1f * trail), a(col, 0.9f * trail));
            rib(ctx, buf, RIBBON, from.x, from.y, from.z, hx, hy, hz, 0.1f, 0.55f * s, a(WHITE, 0.2f * trail), a(WHITE, 0.95f * trail));
            for (int k = 1; k <= 3; k++) {                                                     // afterimages of the fist
                float q = run * k / 4f;
                glow(ctx, buf, CORE, from.x + (to.x - from.x) * q, from.y + (to.y - from.y) * q, from.z + (to.z - from.z) * q, (0.5f + 0.4f * charge) * (0.4f + 0.2f * k), 0f, a(col, 0.5f * trail * k / 3f));
            }
        }
        if (age < 7f) {
            glow(ctx, buf, CORE, hx, hy, hz, (0.9f + 0.9f * charge) * (1f - 0.3f * run), 0f, a(col, 0.95f));
            glow(ctx, buf, BURST, hx, hy, hz, 1.0f + 0.9f * charge, age * 0.4f, a(WHITE, 0.85f));
        }
        float t = sub(age, 5f, inst.duration);
        if (age >= 5f) {
            float flash = 1f - sub(age, 5f, 11f), wave = VfxAnim.easeOutCubic(sub(age, 5f, 14f));
            glow(ctx, buf, CORE, to.x, to.y, to.z, s * (1.5f + 2.4f * t), 0f, a(col, 0.9f * flash));
            glow(ctx, buf, CORE, to.x, to.y, to.z, s * (0.9f + 1.0f * t), 0f, a(WHITE, flash));
            glow(ctx, buf, BURST, to.x, to.y, to.z, s * (1.8f + 3.0f * wave), age * 0.06f, a(col, 0.85f * out * (1f - 0.5f * wave)));
            ground(buf, RING, to.x, to.y - 0.85f, to.z, s * 2.8f * wave, 0f, a(col, 0.85f * out));
            ground(buf, CRACK, to.x, to.y - 0.88f, to.z, s * 2.1f * sub(age, 5f, 9f), seed % 6, a(col, 0.9f * (1f - sub(age, 10f, inst.duration))));
            int sh = ctx.seg(8, 4);
            for (int k = 0; k < sh; k++) {                                                      // armour splinters thrown out
                float ang = Mth.TWO_PI * k / sh + hash(seed, k, 1) * 0.6f, spd = 1.6f + 1.6f * hash(seed, k, 2), el = 0.5f + hash(seed, k, 3);
                float d = (age - 5f) * 0.12f * spd;
                float x = to.x + Mth.cos(ang) * d * 2.0f, z = to.z + Mth.sin(ang) * d * 2.0f, y = to.y + el * d * 1.6f - d * d * 1.2f;
                glow(ctx, buf, SHARD, x, y, z, 0.3f + 0.2f * hash(seed, k, 4), age * 0.4f + k, a(k % 2 == 0 ? WHITE : col, 0.95f * out));
            }
        }
    }

    // ---------------------------------------------------------------- Emission: a homing bullet
    private void bullet(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        long seed = inst.seed;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF58D8FF;
        float run = sub(age, 0f, 6.5f), out = 1f - sub(age, 6f, inst.duration);
        float bend = (hash(seed, 1, 1) - 0.5f) * 2.4f;
        float dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z, len = Math.max(0.5f, Mth.sqrt(dx * dx + dy * dy + dz * dz));
        float px = -dz / len, pz = dx / len;                                                   // sideways of the path
        float mx = (from.x + to.x) * 0.5f + px * bend, my = (from.y + to.y) * 0.5f + 0.5f + Math.abs(bend) * 0.25f, mz = (from.z + to.z) * 0.5f + pz * bend;
        int n = ctx.seg(7, 4);
        float prx = 0, pry = 0, prz = 0;
        for (int k = 0; k <= n; k++) {
            float u = Math.max(0f, run - 0.5f * (1f - (float) k / n) * Math.min(1f, run * 3f)), iu = 1f - u;      // the trail: the path a little way behind the head
            float x = iu * iu * from.x + 2f * iu * u * mx + u * u * to.x, y = iu * iu * from.y + 2f * iu * u * my + u * u * to.y, z = iu * iu * from.z + 2f * iu * u * mz + u * u * to.z;
            if (k > 0 && run > 0.02f) {
                float w0 = 0.04f + 0.42f * (float) (k - 1) / n, w1 = 0.04f + 0.42f * (float) k / n;
                rib(ctx, buf, RIBBON, prx, pry, prz, x, y, z, w0, w1, a(col, 0.8f * (float) (k - 1) / n * out), a(col, 0.9f * (float) k / n * out));
                rib(ctx, buf, RIBBON, prx, pry, prz, x, y, z, w0 * 0.4f, w1 * 0.4f, a(WHITE, 0.5f * (float) (k - 1) / n * out), a(WHITE, 0.9f * (float) k / n * out));
            }
            prx = x; pry = y; prz = z;
        }
        if (run < 1f) {
            glow(ctx, buf, CORE, prx, pry, prz, 0.8f, 0f, a(col, 0.95f));
            glow(ctx, buf, SPARK, prx, pry, prz, 0.9f, age * 0.5f, a(WHITE, 0.95f));
        }
        if (age < 3f) glow(ctx, buf, CORE, from.x, from.y, from.z, 0.8f * (1f - age / 3f), 0f, a(WHITE, 0.8f));
        if (run >= 1f) {
            float w = sub(age, 6f, inst.duration);
            glow(ctx, buf, BURST, to.x, to.y, to.z, 0.8f + 1.8f * w, age * 0.3f, a(col, 0.9f * (1f - w)));
            glow(ctx, buf, CORE, to.x, to.y, to.z, 1.4f * (1f - w), 0f, a(WHITE, 0.9f * (1f - w)));
        }
    }

    // ---------------------------------------------------------------- Transmutation: lightning, gum, rubber
    private void surge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        int prop = Mth.clamp((int) inst.power - 1, 0, 2);
        float radius = (inst.power - (int) inst.power) * 10f;
        long seed = inst.seed;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        float out = 1f - sub(age, inst.duration - 5f, inst.duration);
        if (prop == 0) {                                                                       // electricity: a bolt that re-forms every two ticks
            int col = inst.color != 0 ? inst.color : 0xFFB8D0FF;
            long fs = seed + (long) (age / 2f) * 7919L;
            float dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z, len = Math.max(0.5f, Mth.sqrt(dx * dx + dy * dy + dz * dz));
            float ux = dx / len, uy = dy / len, uz = dz / len, sx = -uz, sz = ux;               // a sideways direction
            int n = ctx.seg(8, 4);
            float ppx = from.x, ppy = from.y, ppz = from.z;
            for (int k = 1; k <= n; k++) {
                float f = (float) k / n, j = k == n ? 0f : 1f, amp = 0.5f * Mth.sin(f * Mth.PI) + 0.12f;
                float x = from.x + dx * f + (sx * (hash(fs, k, 1) - 0.5f) * 2f + 0f) * amp * j, y = from.y + dy * f + (hash(fs, k, 2) - 0.5f) * 1.2f * amp * j, z = from.z + dz * f + (sz * (hash(fs, k, 3) - 0.5f) * 2f) * amp * j;
                rib(ctx, buf, BOLT, ppx, ppy, ppz, x, y, z, 0.7f, 0.7f, a(col, 0.8f * out), a(col, 0.8f * out));
                rib(ctx, buf, BOLT, ppx, ppy, ppz, x, y, z, 0.24f, 0.24f, a(WHITE, 0.95f * out), a(WHITE, 0.95f * out));
                if (k == 3 || k == 6) rib(ctx, buf, BOLT, x, y, z, x + sx * (k == 3 ? 1.1f : -1.1f), y + 0.4f, z + sz * (k == 3 ? 1.1f : -1.1f), 0.3f, 0.1f, a(col, 0.7f * out), a(col, 0f));
                ppx = x; ppy = y; ppz = z;
            }
            glow(ctx, buf, CORE, to.x, to.y, to.z, 1.6f * out, 0f, a(col, 0.9f * out));
            glow(ctx, buf, BURST, to.x, to.y, to.z, 1.4f * out, age * 0.5f, a(WHITE, 0.9f * out));
            glow(ctx, buf, CORE, from.x, from.y, from.z, 0.8f * (1f - sub(age, 0f, 4f)), 0f, a(WHITE, 0.8f));
        } else if (prop == 1) {                                                                // gum: a glossy rope that sags, stretches and snaps tight
            int col = inst.color != 0 ? inst.color : 0xFFFF7AC8;
            float tight = VfxAnim.easeOutCubic(sub(age, 0f, 8f)), sag = (1f - tight) * 0.9f + 0.1f * Mth.sin(age * 0.5f);
            float stretch = 1f - 0.4f * sub(age, 8f, 14f);
            int n = ctx.seg(6, 3);
            float ppx = from.x, ppy = from.y, ppz = from.z;
            for (int k = 1; k <= n; k++) {
                float f = (float) k / n, run = Math.min(1f, tight * 1.6f);
                float x = from.x + (to.x - from.x) * f * run, y = from.y + (to.y - from.y) * f * run - sag * Mth.sin(f * Mth.PI), z = from.z + (to.z - from.z) * f * run;
                float w = (0.26f + 0.12f * Mth.sin(f * 9f + age * 0.4f)) * stretch;
                rib(ctx, buf, GUM, ppx, ppy, ppz, x, y, z, w, w, a(col, 0.9f * out), a(col, 0.9f * out));
                ppx = x; ppy = y; ppz = z;
            }
            glow(ctx, buf, CORE, ppx, ppy, ppz, 0.9f * out, 0f, a(col, 0.9f * out));
            glow(ctx, buf, CORE, from.x, from.y, from.z, 0.6f * out, 0f, a(col, 0.8f * out));
            glow(ctx, buf, SPARK, ppx, ppy, ppz, 0.7f * sub(age, 5f, 9f) * out, age * 0.2f, a(WHITE, 0.8f * out));
        } else {                                                                               // rubber: a shock dome from the feet
            int col = inst.color != 0 ? inst.color : 0xFFFFD25A;
            float R = Math.max(2f, radius), w = VfxAnim.easeOutCubic(sub(age, 0f, 14f)), w2 = VfxAnim.easeOutCubic(sub(age, 3f, 17f));
            ground(buf, RING, from.x, from.y + 0.05f, from.z, R * w, 0f, a(col, 0.95f * out));
            ground(buf, RING, from.x, from.y + 0.07f, from.z, R * 0.7f * w2, age * 0.05f, a(WHITE, 0.7f * out));
            ground(buf, HEX, from.x, from.y + 0.04f, from.z, R * 0.9f * w, -age * 0.04f, a(col, 0.45f * out));
            glow(ctx, buf, BURST, from.x, from.y + 0.5f, from.z, 1.5f + 2.5f * w, age * 0.08f, a(col, 0.8f * out * (1f - w * 0.6f)));
            glow(ctx, buf, CORE, from.x, from.y + 0.3f, from.z, 2.2f * (1f - sub(age, 0f, 7f)), 0f, a(WHITE, 0.9f));
            int rays = ctx.seg(8, 4);
            for (int k = 0; k < rays; k++) {
                float ang = Mth.TWO_PI * k / rays + hash(seed, k, 5), r0 = R * 0.2f * w, r1 = R * (0.5f + 0.5f * hash(seed, k, 6)) * w;
                rib(ctx, buf, RIBBON, from.x + Mth.cos(ang) * r0, from.y + 0.2f, from.z + Mth.sin(ang) * r0, from.x + Mth.cos(ang) * r1, from.y + 0.9f * (1f - w * 0.5f), from.z + Mth.sin(ang) * r1, 0.25f, 0.04f, a(col, 0.8f * out), a(col, 0f));
            }
        }
    }

    // ---------------------------------------------------------------- Conjuration: a chain
    private void link(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF48E8C0;
        float f = sub(age, 0f, 3f) * (1f - sub(age, inst.duration - 5f, inst.duration));
        float dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z, len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f || f <= 0.01f) return;
        float drop = Math.min(0.9f, len * 0.12f) * (1f - 0.7f * VfxAnim.easeOutCubic(sub(age, 0f, 6f)));
        int pieces = Mth.clamp(ctx.seg(Math.round(len / 0.5f), 4), 4, 14);
        float ppx = from.x, ppy = from.y, ppz = from.z;
        for (int k = 1; k <= pieces; k++) {
            float q = (float) k / pieces;
            float x = from.x + dx * q, y = from.y + dy * q - drop * Mth.sin(q * Mth.PI), z = from.z + dz * q;
            rib(ctx, buf, RIBBON, ppx, ppy, ppz, x, y, z, 0.3f, 0.3f, a(col, 0.28f * f), a(col, 0.28f * f));                    // the glow the links sit in
            rib(ctx, buf, LINK, ppx, ppy, ppz, x, y, z, 0.2f, 0.2f, a(k % 2 == 0 ? WHITE : col, 0.95f * f), a(k % 2 == 0 ? WHITE : col, 0.95f * f));
            ppx = x; ppy = y; ppz = z;
        }
        glow(ctx, buf, CORE, from.x, from.y, from.z, 0.6f * f, 0f, a(col, 0.8f * f));
        glow(ctx, buf, CORE, to.x, to.y, to.z, 1.2f * f, 0f, a(col, 0.7f * f));
        wide(ctx, buf, MARK, to.x, to.y, to.z, 1.5f, 1.5f, a(col, 0.55f * f));                                                  // the cuff
        ground(buf, RING, to.x, to.y - 0.9f, to.z, 1.0f, age * 0.12f, a(col, 0.8f * f));
    }

    // ---------------------------------------------------------------- Manipulation: the needle and its strings
    private void needle(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        long seed = inst.seed;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        int col = inst.color != 0 ? inst.color : 0xFFE8A0FF;
        boolean again = inst.duration <= 26;                                                  // a refresh: the needle is already in
        float a0 = again ? age + 6f : age;
        float run = VfxAnim.easeOutCubic(sub(a0, 0f, 5f)), out = 1f - sub(age, inst.duration - 8f, inst.duration), in = sub(age, 0f, again ? 3f : 1f);
        float dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z, len = Math.max(0.5f, Mth.sqrt(dx * dx + dy * dy + dz * dz)), ux = dx / len, uy = dy / len, uz = dz / len;
        float hx = from.x + dx * run, hy = from.y + dy * run, hz = from.z + dz * run;
        if (a0 < 6f) {
            rib(ctx, buf, NEEDLE, hx - ux * 1.5f, hy - uy * 1.5f, hz - uz * 1.5f, hx, hy, hz, 0.22f, 0.22f, a(col, 0.95f), a(WHITE, 0.95f));
            rib(ctx, buf, RIBBON, hx - ux * 3f, hy - uy * 3f, hz - uz * 3f, hx, hy, hz, 0.02f, 0.25f, a(col, 0f), a(col, 0.6f));
        }
        if (a0 < 4.5f) return;
        float f = in * out;
        rib(ctx, buf, NEEDLE, to.x - ux * 0.9f, to.y - uy * 0.9f, to.z - uz * 0.9f, to.x + ux * 0.1f, to.y + uy * 0.1f, to.z + uz * 0.1f, 0.18f, 0.18f, a(col, 0.95f * f), a(WHITE, 0.95f * f));
        // the strings: from the creature up to a control bar
        float hubY = to.y + 2.4f + 0.12f * Mth.sin(age * 0.2f), hubX = to.x, hubZ = to.z;
        float hs = 0.55f;
        int strings = 5;
        for (int k = 0; k < strings; k++) {
            float ox = (k - 2) * 0.32f, oy = (k == 2 ? 0.7f : k == 1 || k == 3 ? 0.2f : -0.2f);
            float lx = to.x + ctx.camRight.x * ox, ly = to.y + oy, lz = to.z + ctx.camRight.z * ox;
            float bx = hubX + ctx.camRight.x * ox * 0.9f, bz = hubZ + ctx.camRight.z * ox * 0.9f;
            float sway = 0.06f * Mth.sin(age * 0.25f + k * 1.3f);
            rib(ctx, buf, RIBBON, lx, ly, lz, bx + sway, hubY, bz, 0.09f, 0.05f, a(col, 0.75f * f), a(col, 0.35f * f));
            glow(ctx, buf, CORE, lx, ly, lz, 0.28f, 0f, a(col, 0.8f * f));
        }
        rib(ctx, buf, RIBBON, hubX - ctx.camRight.x * hs, hubY, hubZ - ctx.camRight.z * hs, hubX + ctx.camRight.x * hs, hubY, hubZ + ctx.camRight.z * hs, 0.12f, 0.12f, a(col, 0.9f * f), a(col, 0.9f * f));
        wide(ctx, buf, MARK, hubX, hubY + 0.35f, hubZ, 0.75f, 0.75f, a(col, 0.9f * f));
        wide(ctx, buf, MARK, to.x, to.y, to.z, 1.4f + 0.12f * Mth.sin(age * 0.3f), 1.4f + 0.12f * Mth.sin(age * 0.3f), a(col, 0.5f * f));
        ground(buf, HEX, to.x, to.y - 0.9f, to.z, 1.1f, age * 0.04f, a(col, 0.7f * f));
        int sp = ctx.seg(4, 2);
        for (int k = 0; k < sp; k++) {
            float ph = (age * 0.03f + hash(seed, k, 2)) % 1f;
            glow(ctx, buf, SPARK, to.x + (hash(seed, k, 3) - 0.5f) * 1.0f, to.y - 0.6f + 2.6f * ph, to.z + (hash(seed, k, 4) - 0.5f) * 1.0f, 0.25f * (1f - ph), ph * 6f, a(WHITE, 0.7f * (1f - ph) * f));
        }
    }

    // ---------------------------------------------------------------- Specialization: the field
    private void domain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float R = Math.max(3f, inst.power);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFFFF6E9E;
        float open = VfxAnim.easeOutCubic(sub(age, 0f, 12f)), out = 1f - sub(age, inst.duration - 10f, inst.duration), f = out * sub(age, 0f, 3f);
        if (f <= 0.01f) return;
        float r = R * open;
        ground(buf, HEX, c.x, c.y + 0.05f, c.z, r, age * 0.012f, a(col, 0.6f * f));
        ground(buf, HEX, c.x, c.y + 0.07f, c.z, r * 0.62f, -age * 0.02f, a(WHITE, 0.4f * f));
        ground(buf, RING, c.x, c.y + 0.06f, c.z, r, 0f, a(col, 0.9f * f));
        float pf = (age % 20f) / 20f;                                                           // the pulse that goes out every second (the field's tick)
        ground(buf, RING, c.x, c.y + 0.1f, c.z, r * pf, 0f, a(WHITE, 0.75f * (1f - pf) * f));
        glow(ctx, buf, CORE, c.x, c.y + 0.3f, c.z, 1.6f + 0.4f * Mth.sin(age * 0.3f), 0f, a(col, 0.5f * f));
        int pil = ctx.seg(12, 6);
        for (int k = 0; k < pil; k++) {                                                         // light pillars rising on the rim
            float ang = Mth.TWO_PI * k / pil + age * 0.006f, h = 1.2f + 1.4f * (0.5f + 0.5f * Mth.sin(age * 0.15f + k * 1.9f)) * (0.6f + 0.4f * hash(seed, k, 1));
            float x = c.x + Mth.cos(ang) * r * 0.97f, z = c.z + Mth.sin(ang) * r * 0.97f;
            rib(ctx, buf, FLAME[k & 3], x, c.y, z, x, c.y + h, z, 0.5f, 0.35f, a(col, 0.55f * f), a(col, 0.03f));
        }
        int motes = ctx.seg(14, 5);
        for (int k = 0; k < motes; k++) {
            float ph = (age * 0.02f + hash(seed, k, 2)) % 1f, ang = hash(seed, k, 3) * Mth.TWO_PI, rr = r * Mth.sqrt(hash(seed, k, 4)) * 0.95f;
            glow(ctx, buf, SPARK, c.x + Mth.cos(ang) * rr, c.y + 0.2f + 2.4f * ph, c.z + Mth.sin(ang) * rr, 0.3f * Mth.sin(ph * Mth.PI), ang + age * 0.1f, a(k % 3 == 0 ? WHITE : col, 0.85f * f));
        }
    }
}
