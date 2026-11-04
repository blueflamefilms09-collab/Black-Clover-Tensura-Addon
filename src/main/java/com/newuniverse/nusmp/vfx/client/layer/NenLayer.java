package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.48.5 Nen (Hunter x Hunter). Aura is drawn as it is in the anime: flame-like streams of light that leave the skin and rise, not a particle puff.
 * <ul>
 *   <li>NEN_AURA ({@code power} = kind 0 Ten, 1 Zetsu, 2 Ren, 3 Ken, 4 Ko, 5 Vow; follows the caster, resent every two seconds while the window is open): Ten hugs the body in a calm
 *       white-blue veil; Zetsu is almost nothing, a few grey motes sinking inwards; Ren flares wide and tall in gold-white with a pulsing halo; Ken is dense and heavy with two slow
 *       rings; Ko pulls the aura into one bright fist-sized core; Vow adds violet chains.</li>
 *   <li>NEN_EN: a sphere of aura opens round the caster to {@code power} blocks: ground rings and a shell of motes that twinkle.</li>
 *   <li>NEN_GYO: aura gathers into the eyes: a ring closes on the face and two flares open.</li>
 *   <li>NEN_IMPACT: the hatsu blow lands: flash, shock ring, radial streaks. {@code power} = size.</li>
 *   <li>NEN_BEAM: the emission bolt: a bright orb races from to to, a layered beam follows it.</li>
 * </ul>
 * Worst case under 120 vertices for every shape (all quads, constant loop counts, deterministic from seed and age); the detail scale thins only the wisps.
 */
public class NenLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation[] WISP = {t("aura_wisp_0"), t("aura_wisp_1"), t("aura_wisp_2"), t("aura_wisp_3"), t("aura_wisp_4"), t("aura_wisp_5")};
    private static final ResourceLocation GLOW = t("glow"), SPARK = t("spark"), HALO = t("bfx_halo"), RING = t("shock_ring_0"), STREAK = t("spike_streak"), FLARE = t("spike_flare"),
            EYE = t("eye_flare"), SMOKE = t("smoke_wisp_0");

    private static final int[] KIND_COLOR = {0xFFDCEEFF, 0xFF8C93A8, 0xFFFFE08A, 0xFF9CC4FF, 0xFFFF9A5A, 0xFFB070FF, 0xFF7CFFD8, 0xFFFF7ADA};
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.NEN_AURA, VfxShape.NEN_EN, VfxShape.NEN_GYO, VfxShape.NEN_IMPACT, VfxShape.NEN_BEAM); }

    @Override public int defaultDuration(VfxShape s) {
        return switch (s) { case NEN_AURA -> 50; case NEN_EN -> 40; case NEN_GYO -> 20; case NEN_IMPACT -> 14; default -> 14; };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFDCEEFF; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.01f && buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = sub(age, 0f, 3f) * (1f - sub(age, inst.duration - 6f, inst.duration));
        if (fade <= 0.01f && inst.shape == VfxShape.NEN_AURA) return;
        switch (inst.shape) {
            case NEN_AURA -> aura(inst, ctx, buf, age, fade);
            case NEN_EN -> en(inst, ctx, buf, age);
            case NEN_GYO -> gyo(inst, ctx, buf, age);
            case NEN_IMPACT -> impact(inst, ctx, buf, age);
            case NEN_BEAM -> beam(inst, ctx, buf, age);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the aura of a window
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, float fade) {
        int kind = Mth.clamp(Math.round(inst.power), 0, 7);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : KIND_COLOR[kind];
        // wisps: [count, radius, height, speed, size, alpha] per kind
        int n = switch (kind) { case 0 -> 10; case 1 -> 4; case 2 -> 20; case 3 -> 14; case 4 -> 8; case 6 -> 6; case 7 -> 10; default -> 12; };
        float rad = switch (kind) { case 0 -> 0.5f; case 1 -> 0.35f; case 2 -> 0.75f; case 3 -> 0.6f; case 4 -> 0.45f; case 6 -> 0.4f; case 7 -> 0.7f; default -> 0.65f; };
        float hgt = switch (kind) { case 0 -> 2.1f; case 1 -> 1.2f; case 2 -> 3.2f; case 3 -> 2.0f; case 4 -> 2.2f; case 6 -> 1.9f; case 7 -> 2.3f; default -> 2.6f; };
        float spd = switch (kind) { case 0 -> 0.022f; case 1 -> -0.014f; case 2 -> 0.05f; case 3 -> 0.015f; case 4 -> 0.035f; case 6 -> 0.02f; case 7 -> 0.04f; default -> 0.03f; };
        float size = switch (kind) { case 0 -> 1.1f; case 1 -> 0.7f; case 2 -> 1.7f; case 3 -> 1.5f; case 4 -> 1.0f; case 6 -> 0.8f; case 7 -> 1.2f; default -> 1.4f; };
        float al = switch (kind) { case 0 -> 0.7f; case 1 -> 0.3f; case 2 -> 0.85f; case 3 -> 0.8f; case 4 -> 0.8f; case 6 -> 0.55f; case 7 -> 0.85f; default -> 0.75f; };
        float flare = kind == 2 ? 0.5f + 0.5f * Mth.sin(age * 0.5f) : 1f;               // Ren breathes
        int count = ctx.seg(n, Math.max(3, n / 2));
        for (int k = 0; k < count; k++) {
            float ph = hash(seed, k, 1), ang = Mth.TWO_PI * k / count + ph * 1.7f + age * (kind == 3 ? 0.012f : 0.03f);
            float rise = (age * spd * (0.7f + 0.6f * hash(seed, k, 2)) + ph) % 1f;
            if (rise < 0f) rise += 1f;
            float up = kind == 1 ? 1.2f * (1f - rise) : rise * hgt;                     // Zetsu sinks, the rest rise
            float r = rad * (kind == 2 ? 0.8f + 0.8f * rise : 1f - 0.3f * rise) * (0.8f + 0.4f * flare);
            if (kind == 4) r *= 1f - 0.55f * sub(age, 0f, inst.duration);              // Ko draws in
            float tail = Mth.sin(rise * Mth.PI);                                       // fades at both ends of its rise
            float x = c.x + Mth.cos(ang) * r, z = c.z + Mth.sin(ang) * r, y = c.y + 0.1f + up;
            sprite(ctx, buf, WISP[k % 6], VfxBlend.ADD, x, y, z, size * (0.8f + 0.4f * tail), ph * 6.28f + age * 0.04f, a(k % 3 == 0 ? WHITE : col, al * tail * fade));
        }
        // the ground: Ten/Ken a steady ring, Ren a pulsing wide one, Zetsu a ring that closes, Vow violet chain rings
        float pulse = 0.5f + 0.5f * Mth.sin(age * (kind == 2 ? 0.55f : 0.2f));
        float gr = switch (kind) { case 1 -> 1.1f - 0.5f * sub(age, 0f, inst.duration); case 2 -> 1.6f + 0.5f * pulse; case 3 -> 1.2f; default -> 1.0f; };
        ground(buf, RING, VfxBlend.ADD, c, 0.06f, gr, age * (kind == 3 ? 0.01f : 0.03f), a(col, (kind == 1 ? 0.25f : 0.5f) * fade));
        if (kind == 2 || kind == 3 || kind == 5) ground(buf, HALO, VfxBlend.ADD, c, 0.1f, gr * 0.8f, -age * 0.025f, a(kind == 5 ? KIND_COLOR[5] : WHITE, 0.35f * fade * (0.6f + 0.4f * pulse)));
        if (kind == 3) ground(buf, RING, VfxBlend.ADD, c, 0.14f, gr * 0.6f, age * 0.02f, a(WHITE, 0.3f * fade));
        // the core: Ren a blaze round the chest, Ko the bright fist of gathered aura, Ten a faint skin glow
        if (kind == 2) sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y + 1.0f, c.z, 2.6f + 0.5f * pulse, 0f, a(col, 0.45f * fade));
        if (kind == 4) {
            float g = sub(age, 0f, 25f);
            sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y + 1.1f, c.z, 0.6f + 0.9f * g, 0f, a(col, 0.9f * fade));
            sprite(ctx, buf, FLARE, VfxBlend.ADD, c.x, c.y + 1.1f, c.z, 0.5f + 0.8f * g, age * 0.15f, a(WHITE, 0.8f * g * fade));
        }
        if (kind == 0 || kind == 3) sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y + 1.0f, c.z, 2.0f, 0f, a(col, 0.18f * fade));
        if (kind == 5) {                                                                // chains of violet light spiralling
            int links = ctx.seg(8, 4);
            for (int k = 0; k < links; k++) {
                float ang = Mth.TWO_PI * k / links + age * 0.08f, y = c.y + 0.3f + (k * 0.27f);
                sprite(ctx, buf, SPARK, VfxBlend.ADD, c.x + Mth.cos(ang) * 0.75f, y, c.z + Mth.sin(ang) * 0.75f, 0.35f, ang, a(KIND_COLOR[5], 0.8f * fade));
            }
        }
        if (kind == 6) {                                                                // Shu: a thin veil and a bright glow in front of the hands
            sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y + 1.3f, c.z, 1.2f, 0f, a(col, 0.35f * fade));
        }
        if (kind == 7) {                                                                // Ryu: glows orbiting the body
            int orbs = ctx.seg(4, 2);
            for (int k = 0; k < orbs; k++) {
                float ang = Mth.TWO_PI * k / orbs + age * 0.12f, y = c.y + 0.4f + 0.45f * k;
                sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x + Mth.cos(ang) * 0.7f, y, c.z + Mth.sin(ang) * 0.7f, 0.7f, 0f, a(col, 0.7f * fade));
            }
        }
        if (kind == 1) sprite(ctx, buf, SMOKE, VfxBlend.ALPHA, c.x, c.y + 0.9f, c.z, 1.6f, age * 0.01f, a(col, 0.12f * fade));
    }

    // ---------------------------------------------------------------- En: the sphere
    private void en(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float R = Math.max(3f, inst.power);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF6AFFC0;
        float open = VfxAnim.easeOutCubic(sub(age, 0f, 10f)), out = 1f - sub(age, inst.duration - 12f, inst.duration);
        if (out <= 0.01f) return;
        for (int k = 0; k < 3; k++) {                                                   // rings that rise and fall through the sphere
            float f = (age * 0.04f + k / 3f) % 1f, y = 0.1f + R * 0.9f * Mth.sin(f * Mth.PI);
            ground(buf, RING, VfxBlend.ADD, c, y, R * open * Mth.cos((f - 0.5f) * 1.2f), age * 0.02f * (k + 1), a(col, 0.45f * out));
        }
        ground(buf, HALO, VfxBlend.ADD, c, 0.08f, R * open, 0f, a(col, 0.6f * out));
        int motes = ctx.seg(30, 10);
        for (int k = 0; k < motes; k++) {                                               // motes on the shell, twinkling
            float u = hash(seed, k, 3) * 2f - 1f, ang = hash(seed, k, 4) * Mth.TWO_PI, s = Mth.sqrt(1f - u * u);
            float tw = 0.4f + 0.6f * Mth.sin(age * 0.4f + k * 1.7f) * Mth.sin(age * 0.4f + k * 1.7f);
            sprite(ctx, buf, SPARK, VfxBlend.ADD, c.x + Mth.cos(ang) * s * R * open, c.y + 1f + u * R * open * 0.6f, c.z + Mth.sin(ang) * s * R * open, 0.5f, 0f, a(k % 4 == 0 ? WHITE : col, 0.8f * tw * out));
        }
        sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y + 1f, c.z, 3f * (1f - open) + 1f, 0f, a(WHITE, 0.6f * (1f - open) * out));
    }

    // ---------------------------------------------------------------- Gyo: the eyes
    private void gyo(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF7AE0FF;
        float close = sub(age, 0f, 8f), open = sub(age, 6f, 12f), out = 1f - sub(age, 14f, inst.duration);
        Vector3f head = new Vector3f(c.x, c.y + 1.62f, c.z);
        sprite(ctx, buf, HALO, VfxBlend.ADD, head.x, head.y, head.z, 1.6f * (1f - 0.7f * close), age * 0.2f, a(col, 0.7f * close * out));
        float yaw = inst.followYaw(ctx);
        float dx = Float.isNaN(yaw) ? 0f : -Mth.sin(yaw * Mth.DEG_TO_RAD) * 0.25f, dz = Float.isNaN(yaw) ? 0f : Mth.cos(yaw * Mth.DEG_TO_RAD) * 0.25f;
        for (int e = -1; e <= 1; e += 2) {
            float sx = Float.isNaN(yaw) ? 0.1f * e : Mth.cos(yaw * Mth.DEG_TO_RAD) * 0.1f * e, sz = Float.isNaN(yaw) ? 0f : Mth.sin(yaw * Mth.DEG_TO_RAD) * 0.1f * e;
            sprite(ctx, buf, EYE, VfxBlend.ADD, head.x + dx + sx, head.y + 0.02f, head.z + dz + sz, 0.55f * open, 0f, a(col, 0.95f * open * out));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, head.x + dx + sx, head.y + 0.02f, head.z + dz + sz, 0.9f * open, 0f, a(WHITE, 0.5f * open * out));
        }
    }

    // ---------------------------------------------------------------- Hatsu: the blow lands
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        float s = Math.max(0.6f, inst.power);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color != 0 ? inst.color : 0xFFFF9A3A;
        float t = sub(age, 0f, inst.duration), flash = 1f - sub(age, 0f, 5f), wave = VfxAnim.easeOutCubic(sub(age, 0f, 9f)), out = 1f - sub(age, 8f, inst.duration);
        sprite(ctx, buf, FLARE, VfxBlend.ADD, c.x, c.y, c.z, s * (1.5f + 2f * t), age * 0.1f, a(WHITE, 0.95f * flash));
        sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x, c.y, c.z, s * (1.2f + 2.4f * t), 0f, a(col, 0.8f * flash));
        sprite(ctx, buf, HALO, VfxBlend.ADD, c.x, c.y, c.z, s * 4f * wave, 0f, a(col, 0.7f * out * (1f - wave * 0.6f)));
        ground(buf, RING, VfxBlend.ADD, c, -0.4f, s * 2.4f * wave, 0f, a(WHITE, 0.6f * out));
        int rays = ctx.seg(10, 5);
        for (int k = 0; k < rays; k++) {
            float ang = Mth.TWO_PI * k / rays + hash(seed, k, 5), el = (hash(seed, k, 6) - 0.5f) * 1.6f;
            float r0 = s * 0.4f * wave, r1 = s * (0.9f + 1.6f * wave * (0.6f + 0.4f * hash(seed, k, 7)));
            Vector3f d = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el));
            Vector3f p0 = new Vector3f(d).mul(r0).add(c), p1 = new Vector3f(d).mul(r1).add(c);
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, 0.22f * s, 0.02f, 1, 0f, a(k % 2 == 0 ? WHITE : col, 0.9f * out), a(col, 0f));
        }
    }

    // ---------------------------------------------------------------- Hatsu: Emission, the bolt
    private void beam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age) {
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        int col = inst.color != 0 ? inst.color : 0xFF6AD8FF;
        float run = VfxAnim.easeOutCubic(sub(age, 0f, 6f)), out = 1f - sub(age, 6f, inst.duration);
        Vector3f head = new Vector3f(from).lerp(to, run);
        float w = 0.9f + 0.25f * Mth.sin(age * 1.3f);
        if (run > 0.02f) {
            buf.beam(ctx, GLOW, VfxBlend.ADD, from, head, w * 0.9f, w * 0.5f, 4, 0f, a(col, 0.7f * out), a(col, 0.9f * out));
            buf.beam(ctx, STREAK, VfxBlend.ADD, from, head, w * 0.35f, w * 0.2f, 4, -age * 0.15f, a(WHITE, 0.95f * out), a(WHITE, 0.95f * out));
        }
        sprite(ctx, buf, GLOW, VfxBlend.ADD, head.x, head.y, head.z, w * 2.4f, 0f, a(col, 0.85f * out));
        sprite(ctx, buf, FLARE, VfxBlend.ADD, head.x, head.y, head.z, w * 1.8f, age * 0.3f, a(WHITE, 0.95f * out));
        sprite(ctx, buf, GLOW, VfxBlend.ADD, from.x, from.y, from.z, w * 2.0f * (1f - sub(age, 0f, 6f)), 0f, a(WHITE, 0.8f));
        if (run >= 0.98f) sprite(ctx, buf, HALO, VfxBlend.ADD, to.x, to.y, to.z, w * 3f * sub(age, 5f, 11f), 0f, a(col, 0.7f * out));
    }
}
