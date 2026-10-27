package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava "Hell Flare" (Tensura): a field of black-red hellfire round the caster. Not cheerful fire: deep crimson, an orange-red hot edge, black smoke, a little white-hot at the core.
 * <ul>
 *   <li>0-6: a runic circle of hellfire is drawn on the ground out to the full radius, low flames at its rim, embers rising.</li>
 *   <li>6-18: three concentric rings of black-red flame leave the centre one after another (the first fastest, so it ends outermost), tall flame tongues standing on each ring,
 *       a dark-red fireball core with a white-hot heart near the centre, a hot flash as each ring is thrown.</li>
 *   <li>18-30: the rings hang, the flames shrink to low ember licks, smoke and sparks rise, the circle and the core fade.</li>
 * </ul>
 * {@code inst.power} = the outer radius (~18). Worst case about 370 of the 400 vertices (all quads, priority order: ground, flames, core, then the optional smoke / embers which the
 * budget and {@link VfxRenderContext#seg} thin first), constant loop counts, deterministic from seed and age, no allocation beyond a few short-lived vectors per quad.
 */
public class VeldHellFlareLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation TONGUE = t("demon_fire_tongue"), HOTTONGUE = t("fire_tongue"), SIGIL = t("demon_fire_sigil"), RING = t("demon_fire_ring"),
            SCORCH = t("demon_fire_scorch"), ORB = t("demon_fire_orb"), CORONA = t("demon_fire_corona"), FLASH = t("demon_fire_flash"), SMOKE = t("demon_fire_smoke"),
            EMBER = t("demon_fire_ember"), GLOW = t("glow");

    private static final int RINGS = 3, RING_TONGUES = 8, RIM_TONGUES = 8, SMOKES = 8, EMBERS = 10;
    /** Ring i leaves the centre at START[i], reaches its final radius (RADIUS[i] * R) at START[i] + TRAVEL[i]: the first ring is the fastest. */
    private static final float[] START = {6.0f, 8.0f, 10.5f}, TRAVEL = {7.5f, 8.5f, 8.5f}, RADIUS = {1.0f, 0.68f, 0.38f}, HEIGHT = {0.58f, 0.52f, 0.46f};

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_HELL_FLARE); }
    @Override public int defaultDuration(VfxShape s) { return 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFFC4180E; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.02f && buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    /** A flame standing on (bx, by, bz) (camera relative), turned to face the camera about the vertical axis, its tip swayed by (sx, sz); texture bottom at the base. */
    private static void tongue(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float u0, float u1, float bx, float by, float bz, float w, float h,
                               float sx, float sz, int cBase, int cTip) {
        if (h < 0.05f || !buf.hasBudget(4)) return;
        float rx = ctx.camRight.x, rz = ctx.camRight.z, rl = Mth.sqrt(rx * rx + rz * rz);
        if (rl < 1e-4f) { rx = 1f; rz = 0f; rl = 1f; }
        rx = rx / rl * w * 0.5f; rz = rz / rl * w * 0.5f;
        int c0 = bl.grade(cBase, 1f), c1 = bl.grade(cTip, 0f);
        buf.quad(tex, bl, new Vector3f(bx - rx, by, bz - rz), new Vector3f(bx + rx, by, bz + rz), new Vector3f(bx + rx + sx, by + h, bz + rz + sz), new Vector3f(bx - rx + sx, by + h, bz - rz + sz),
                u0, 0f, u1, 1f, c0, c1);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(6f, inst.power), s = R / 18f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float out = 1f - sub(age, 25f, 30f);
        if (out <= 0.01f) return;

        // the palette: the (server-overridable) colour is the crimson, the rest is mixed from it
        int base = inst.color, black = VfxVertexBuffer.lerpColor(base, 0xFF000000, 0.72f), edge = VfxVertexBuffer.lerpColor(base, 0xFFFF6A1C, 0.6f), hot = VfxVertexBuffer.lerpColor(edge, 0xFFFFE2B8, 0.55f);
        int deep = VfxVertexBuffer.lerpColor(base, 0xFF000000, 0.35f), ember = VfxVertexBuffer.lerpColor(base, 0xFF000000, 0.5f);
        float shrink = sub(age, 18f, 30f);                                 // 18-30: the flames sink to low licks

        // ---- ground: the burnt scar, the runic circle drawn out to the full radius, its rim
        float draw = VfxAnim.easeOutCubic(sub(age, 0f, 6f));
        float scar = VfxAnim.easeOutCubic(sub(age, 0.5f, 16f));
        ground(buf, SCORCH, VfxBlend.ALPHA, c, 0.03f, R * (0.25f + 0.78f * scar), age * 0.004f, a(black, 0.92f * out * sub(age, 0.5f, 4f)));
        float circle = out * (0.5f + 0.5f * (1f - sub(age, 20f, 25f)));
        ground(buf, SIGIL, VfxBlend.ALPHA, c, 0.06f, R * draw, age * 0.012f, a(base, 0.95f * circle * sub(age, 0f, 1.5f)));
        ground(buf, SIGIL, VfxBlend.ADD, c, 0.08f, R * draw, age * 0.012f, a(edge, 0.30f * circle * sub(age, 0f, 1.5f) * (0.75f + 0.25f * Mth.sin(age * 0.7f))));
        ground(buf, RING, VfxBlend.ALPHA, c, 0.10f, R * draw * 1.01f, 0f, a(deep, 0.95f * out * sub(age, 0f, 1.5f)));
        ground(buf, RING, VfxBlend.ADD, c, 0.12f, R * draw * 1.01f, 0f, a(edge, 0.35f * out * sub(age, 0f, 1.5f)));

        // ---- low flames along the rim of the circle, drawn with it
        float rimH = R * (0.075f + 0.05f * (1f - sub(age, 6f, 12f))) * (1f - 0.55f * shrink);
        for (int k = 0; k < RIM_TONGUES; k++) {
            float ang = Mth.TWO_PI * (k + 0.5f * hash(seed, k, 1)) / RIM_TONGUES, rr = R * draw * 0.99f;
            float fl = 0.7f + 0.3f * Mth.sin(age * 0.9f + k * 1.9f), f = sub(age, 0.4f + 0.25f * k, 2.2f + 0.25f * k);
            float h = rimH * fl * f * (0.8f + 0.5f * hash(seed, k, 2));
            tongue(ctx, buf, TONGUE, VfxBlend.ALPHA, (k & 1) * 0.5f, (k & 1) * 0.5f + 0.5f, c.x + Mth.cos(ang) * rr, c.y + 0.05f, c.z + Mth.sin(ang) * rr, h * 1.0f + 0.5f * s, h,
                    0.25f * h * Mth.sin(age * 0.3f + k), 0.25f * h * Mth.cos(age * 0.27f + k), a(base, 0.95f * out * f), a(black, 0.85f * out * f));
        }

        // ---- the three rings of black-red flame
        for (int i = 0; i < RINGS; i++) {
            if (age < START[i]) continue;
            float e = sub(age, START[i], START[i] + TRAVEL[i]);
            float rr = R * RADIUS[i] * (0.12f + 0.88f * VfxAnim.easeOutCubic(e));
            float born = sub(age, START[i], START[i] + 2f), life = out * born * (1f - 0.35f * shrink);
            // the rim of the ring on the ground, hot where the flame stands
            ground(buf, RING, VfxBlend.ADD, c, 0.14f + 0.02f * i, rr, age * 0.02f * (i + 1), a(edge, 0.45f * life * (1f - 0.55f * shrink)));
            float grow = 0.35f + 0.65f * sub(age, START[i], START[i] + 4f);
            float hMax = R * HEIGHT[i] * grow * (1f - 0.8f * VfxAnim.easeInOutSine(sub(age, 17f, 25f)));
            int n = ctx.seg(RING_TONGUES, 6);
            float w = Math.min(Mth.TWO_PI * rr / n * 1.35f, hMax * 0.9f + 0.8f * s);
            for (int k = 0; k < n; k++) {
                float ph = hash(seed, k + 17 * i, 3);
                float ang = Mth.TWO_PI * (k + 0.4f * ph) / n + i * 0.7f + age * 0.012f * (i % 2 == 0 ? 1 : -1);
                float fl = 0.62f + 0.38f * Mth.sin(age * (0.55f + 0.2f * ph) + k * 2.3f + i), h = hMax * (0.55f + 0.7f * ph) * fl;
                float x = c.x + Mth.cos(ang) * rr, z = c.z + Mth.sin(ang) * rr;
                // the tip leans along the ring (a slow turn) so the field seems to roil
                float lean = 0.22f * h * Mth.sin(age * 0.33f + k), sx = -Mth.sin(ang) * lean * 1.6f, sz = Mth.cos(ang) * lean * 1.6f;
                tongue(ctx, buf, TONGUE, VfxBlend.ALPHA, ((k + i) & 1) * 0.5f, ((k + i) & 1) * 0.5f + 0.5f, x, c.y + 0.05f, z, w, h, sx, sz, a(base, 0.97f * life), a(black, 0.9f * life));
                tongue(ctx, buf, TONGUE, VfxBlend.ADD, ((k + i) & 1) * 0.5f, ((k + i) & 1) * 0.5f + 0.5f, x, c.y + 0.07f, z, w, h, sx, sz, a(edge, 0.85f * life), a(base, 0.35f * life));
            }
        }

        // ---- the fireball core: dark red, a white-hot heart, flashes as the rings are thrown
        float coreIn = VfxAnim.easeOutBack(sub(age, 3.5f, 8f)), coreOut = 1f - VfxAnim.easeInOutSine(sub(age, 18f, 29f));
        float pulse = 1f + 0.08f * Mth.sin(age * 0.9f);
        float cs = R * 0.55f * coreIn * coreOut * pulse;
        if (cs > 0.05f) {
            float cy = c.y + 1.3f * s + 0.35f * s * Mth.sin(age * 0.25f);
            sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, cy, c.z, cs * 1.5f, 0f, a(base, 0.55f * coreOut));
            sprite(ctx, buf, ORB, VfxBlend.ALPHA, c.x, cy, c.z, cs, age * 0.18f, a(ember, 0.98f * coreOut));
            sprite(ctx, buf, CORONA, VfxBlend.ADD, c.x, cy, c.z, cs * 1.75f, -age * 0.05f, a(base, 0.85f * coreOut));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, cy, c.z, cs * 0.55f, 0f, a(hot, 0.9f * coreOut * (0.8f + 0.2f * Mth.sin(age * 1.3f))));
            for (int k = 0; k < 4; k++) {
                float ang = Mth.TWO_PI * k / 4f + age * 0.14f, rr = cs * 0.32f, h = cs * (0.8f + 0.4f * Mth.sin(age * 0.7f + k * 1.7f));
                tongue(ctx, buf, TONGUE, VfxBlend.ALPHA, (k & 1) * 0.5f, (k & 1) * 0.5f + 0.5f, c.x + Mth.cos(ang) * rr, cy - cs * 0.1f, c.z + Mth.sin(ang) * rr, cs * 0.7f, h,
                        -Mth.sin(ang) * h * 0.25f, Mth.cos(ang) * h * 0.25f, a(base, 0.9f * coreOut), a(black, 0.8f * coreOut));
            }
        }
        for (int i = 0; i < RINGS; i++) {                                  // the flash as each ring is thrown
            float fl = 1f - sub(age, START[i], START[i] + 3f);
            if (age >= START[i] && fl > 0.02f) sprite(ctx, buf, FLASH, VfxBlend.ADD, c.x, c.y + 1.3f * s, c.z, R * (0.5f - 0.1f * i) * (0.5f + 0.5f * (1f - fl)), age * 0.1f + i, a(hot, 0.9f * fl));
        }

        // ---- smoke that comes up off the field once the rings hang
        float smk = sub(age, 11f, 18f) * out;
        if (smk > 0.01f) {
            int n = ctx.seg(SMOKES, 4);
            for (int k = 0; k < n; k++) {
                float ang = hash(seed, k, 11) * Mth.TWO_PI, rr = R * (0.12f + 0.82f * Mth.sqrt(hash(seed, k, 12)));
                float rise = (1.0f + 4.5f * hash(seed, k, 13)) * s + 0.22f * s * Math.max(0f, age - 12f) * (0.5f + hash(seed, k, 14));
                sprite(ctx, buf, SMOKE, VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr, c.y + rise, c.z + Mth.sin(ang) * rr, R * (0.2f + 0.14f * hash(seed, k, 15)), hash(seed, k, 16) * 6.28f + age * 0.02f,
                        a(black, 0.55f * smk));
            }
        }

        // ---- embers and sparks rising, from the circle first, then from the rings
        int ne = ctx.seg(EMBERS, 5);
        for (int k = 0; k < ne; k++) {
            float cyc = (age * 0.05f + hash(seed, k, 21)) % 1f, env = Mth.sin(Mth.PI * cyc);
            int src = age < 6f ? -1 : k % RINGS;
            float rr = src < 0 ? R * draw * (0.3f + 0.7f * hash(seed, k, 22)) : R * RADIUS[src] * (0.12f + 0.88f * VfxAnim.easeOutCubic(sub(age, START[src], START[src] + TRAVEL[src]))) * (0.9f + 0.2f * hash(seed, k, 22));
            float ang = hash(seed, k, 23) * Mth.TWO_PI + 0.4f * cyc;
            float rise = (0.5f + 7.0f * cyc) * s * (0.6f + 0.8f * hash(seed, k, 24));
            float al = env * out * sub(age, 0.5f, 3f);
            sprite(ctx, buf, EMBER, VfxBlend.ADD, c.x + Mth.cos(ang) * rr + 0.4f * s * Mth.sin(age * 0.3f + k), c.y + 0.3f * s + rise, c.z + Mth.sin(ang) * rr, (0.45f + 0.5f * hash(seed, k, 25)) * s * 1.3f, 0f,
                    a(k % 3 == 0 ? hot : edge, 0.95f * al));
        }
    }
}
