package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Ash Magic: grey, dry and cursed: ash that drifts, flakes that fall, splintered branches of the ash tree that reach up, embers in the dark. Textures from
 * tools/gen_ash_rebuild.py (textures/particle/ash_*.png). Sizes: VfxInstance multiplies the payload power by 3 for FX1 / FX3 and by 1.5 for FX2, so
 * {@code inst.power} is that product.
 * <ul>
 *   <li>ASH_FX1, Ash Bullets / Cursed Ash (cast): a dull ember gathers at 'from', then a bolt of ash runs to 'to': a dark ash head with an ember core, a trail
 *       of eleven cloud puffs that swell and grey out, burnt flakes tumbling off it, a splintered tendril or two peeling away; where it ends an ash bloom
 *       and sparks. 'from' = hand, 'to' = target.</li>
 *   <li>ASH_FX2, Ash Cloud / Ash Absorbing Formation / ash trap (zone): the sigil of the ash tree turns on the ground over the real area in grey with an ember
 *       sigil counter-turning under it and a ring on the rim, splintered tendrils stand round the rim and sway, a drift of ash clouds rolls in to the middle
 *       with flakes spiralling in and embers rising. 'from' = centre, power = radius, fades in over 8 and out over 12 ticks.</li>
 *   <li>ASH_FX3, impact / trap spring / call (burst): an ember flash, a ring of ash across the ground, ash puffs thrown out, flakes bursting, tendrils
 *       springing up round the middle, sparks. 'from' = centre.</li>
 * </ul>
 * Alpha blend first (the ash is opaque-ish and dark), additive last (embers, the ember sigil). About 150 vertices of 400.
 */
public class AshLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation CLOUD = t("ash_cloud"), FLAKE = t("ash_flake"), EMBER = t("ash_ember"), SIGIL = t("ash_sigil"), TENDRIL = t("ash_tendril"), RING = t("ash_ring");

    private static final int DARK = 0xFF4A484E, MID = 0xFF8C8A90, PALE = 0xFFCFCDD2, FIRE = 0xFFFF7A38, HOT = 0xFFFFD08A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ASH_FX1, VfxShape.ASH_FX2, VfxShape.ASH_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case ASH_FX1 -> 14;
            case ASH_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFB0AAA2; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case ASH_FX1 -> cast(inst, ctx, buf);
            case ASH_FX2 -> zone(inst, ctx, buf);
            default -> burst(inst, ctx, buf);
        }
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static Vector3f lerp(Vector3f p, Vector3f q, float t) { return new Vector3f(p).lerp(q, t); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, p, size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, float radius, float spin, int col) {
        if (buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(centre).spin(spin), radius, col);
    }

    /** A tendril standing on the ground at 'base', as tall as 'h', turned to face the camera round the vertical axis. */
    private static void tendril(VfxVertexBuffer buf, Vector3f base, float w, float h, int col) {
        if (!buf.hasBudget(4) || h < 0.05f) return;
        Vector3f r = new Vector3f(-base.z, 0f, base.x);
        if (r.lengthSquared() < 1e-6f) r.set(1f, 0f, 0f);
        r.normalize().mul(w * 0.5f);
        Vector3f up = new Vector3f(0f, h, 0f);
        int c = VfxBlend.ALPHA.grade(col, 1f);
        buf.quad(TENDRIL, VfxBlend.ALPHA, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(base).add(r).add(up), new Vector3f(base).sub(r).add(up), 0f, 0f, 1f, 1f, c, c);
    }

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.min(3.2f, Math.max(0.5f, inst.power)) * 1.4f;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        int dark = VfxVertexBuffer.lerpColor(DARK, inst.color | 0xFF000000, 0.25f), mid = VfxVertexBuffer.lerpColor(MID, inst.color | 0xFF000000, 0.25f);

        float run = VfxAnim.easeInOutSine(sub(p, 0.12f, 0.85f)) * 0.4f + sub(p, 0.12f, 0.85f) * 0.6f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);
        float cp = sub(p, 0f, 0.25f), cf = 1f - sub(p, 0.2f, 0.45f);

        int n = ctx.seg(11, 6);                                                            // the trail of cloud puffs, oldest first
        for (int k = n - 1; k >= 0; k--) {
            float lag = (k + 1f) / (n + 1f), tk = Math.max(0f, run - lag * 0.45f);
            if (tk <= 0.001f) continue;
            Vector3f pos = lerp(f, to, tk).add(new Vector3f(side).mul((hash(seed, k, 1) - 0.5f) * 2f * P * (0.1f + 0.3f * lag))).add(new Vector3f(up).mul((hash(seed, k, 2) - 0.3f) * P * 0.5f * lag));
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, pos, P * (0.4f + 0.7f * lag) * (0.8f + 0.4f * hash(seed, k, 3)), hash(seed, k, 4) * 6.28f + age * 0.03f, a(VfxVertexBuffer.lerpColor(dark, mid, lag), (0.85f - 0.55f * lag) * live));
        }
        for (int k = 0; k < 5; k++) {                                                      // burnt flakes tumbling off
            float tk = Math.max(0f, run - (0.05f + 0.07f * k));
            if (tk <= 0.01f) continue;
            Vector3f fp = lerp(f, to, tk).add(new Vector3f(side).mul((hash(seed, k, 5) - 0.5f) * P * 0.9f)).add(new Vector3f(up).mul(-(0.1f + 0.5f * (run - tk)) * P));
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, fp, P * 0.2f, hash(seed, k, 6) * 6.28f + age * 0.15f, a(PALE, 0.9f * live));
        }
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, head, P * 0.9f, age * 0.06f, a(dark, 0.95f * live));
            for (int k = 0; k < 2; k++) {
                Vector3f tp = new Vector3f(head).add(new Vector3f(side).mul((k == 0 ? -1 : 1) * P * 0.5f)).add(new Vector3f(dir).mul(-P * 0.3f));
                sprite(ctx, buf, TENDRIL, VfxBlend.ALPHA, tp, P * 0.6f, (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) - Mth.HALF_PI + (k == 0 ? -0.6f : 0.6f), a(mid, 0.85f * live));
            }
        }
        if (cf > 0f) sprite(ctx, buf, EMBER, VfxBlend.ADD, f, P * (0.5f + 1.1f * cp), 0f, a(FIRE, 0.9f * cf));
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, EMBER, VfxBlend.ADD, head, P * 0.5f, 0f, a(HOT, 0.9f * live));
            for (int k = 0; k < 6; k++) {
                float tk = Math.max(0f, run - (0.04f + 0.06f * k) * (0.6f + hash(seed, k, 7)));
                Vector3f sp = lerp(f, to, tk).add(new Vector3f(up).mul((hash(seed, k, 8) - 0.2f) * P * 0.5f)).add(new Vector3f(side).mul((hash(seed, k, 9) - 0.5f) * P * 0.6f));
                sprite(ctx, buf, EMBER, VfxBlend.ADD, sp, P * 0.1f, 0f, a(FIRE, (0.9f - 0.1f * k) * live));
            }
        }
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, to, P * (0.6f + 1.4f * VfxAnim.easeOutCubic(bp)), age * 0.04f, a(mid, 0.7f * (1f - bp)));
            sprite(ctx, buf, EMBER, VfxBlend.ADD, to, P * (0.4f + 0.8f * bp), 0f, a(FIRE, 0.6f * (1f - bp)));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power);
        long seed = inst.seed;
        float open = VfxAnim.easeOutCubic(cl(age / 8f));
        float fade = cl(Math.min(age / 8f, (inst.duration - age) / 12f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int dark = VfxVertexBuffer.lerpColor(DARK, inst.color | 0xFF000000, 0.25f), mid = VfxVertexBuffer.lerpColor(MID, inst.color | 0xFF000000, 0.25f);

        ground(buf, SIGIL, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.05f, 0), R * 0.95f * open, age * 0.012f, a(mid, 0.7f * fade));
        ground(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), R * open, -age * 0.01f, a(dark, 0.85f * fade));
        int tn = ctx.seg(9, 5);
        for (int k = 0; k < tn; k++) {                                                      // tendrils round the rim, swaying
            float ang = Mth.TWO_PI * k / tn + 0.3f * hash(seed, k, 1), sway = Mth.sin(age * 0.06f + k * 1.9f);
            float h = R * (0.5f + 0.25f * hash(seed, k, 2) + 0.05f * sway) * open;
            tendril(buf, new Vector3f(c).add(Mth.cos(ang) * R * 0.92f * open, 0.02f, Mth.sin(ang) * R * 0.92f * open), R * 0.34f, h, a(dark, 0.9f * fade));
        }
        int n = ctx.seg(10, 5);
        for (int k = 0; k < n; k++) {                                                       // ash clouds rolling in to the middle
            float ph = (age * 0.012f + hash(seed, k, 3)) % 1f, ang = hash(seed, k, 4) * Mth.TWO_PI + ph * 3f;
            float rr = R * (0.95f - 0.8f * ph);
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rr, 0.25f + R * 0.25f * Mth.sin(ph * Mth.PI), Mth.sin(ang) * rr), R * (0.3f + 0.2f * ph), hash(seed, k, 5) * 6.28f, a(mid, Mth.sin(ph * Mth.PI) * 0.6f * fade));
        }
        for (int k = 0; k < 8; k++) {                                                       // flakes spiralling in
            float ph = (age * 0.02f + hash(seed, k, 6)) % 1f, ang = hash(seed, k, 7) * Mth.TWO_PI + ph * 4f, rr = R * (1f - 0.85f * ph);
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rr, 0.3f + R * 0.5f * ph, Mth.sin(ang) * rr), R * 0.09f, age * 0.1f + k, a(PALE, Mth.sin(ph * Mth.PI) * 0.9f * fade));
        }
        ground(buf, SIGIL, VfxBlend.ADD, new Vector3f(c).add(0, 0.09f, 0), R * 0.62f * open, -age * 0.03f, a(FIRE, 0.4f * fade * (0.7f + 0.3f * Mth.sin(age * 0.15f))));
        for (int k = 0; k < 8; k++) {
            float ph = (age * 0.02f + hash(seed, k, 8)) % 1f, ang = hash(seed, k, 9) * Mth.TWO_PI, rr = R * 0.8f * hash(seed, k, 10);
            sprite(ctx, buf, EMBER, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * rr, 0.15f + ph * R * 0.8f, Mth.sin(ang) * rr), R * 0.07f, 0f, a(FIRE, Mth.sin(ph * Mth.PI) * 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.min(3.2f, Math.max(0.5f, inst.power)) * 1.4f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int dark = VfxVertexBuffer.lerpColor(DARK, inst.color | 0xFF000000, 0.25f), mid = VfxVertexBuffer.lerpColor(MID, inst.color | 0xFF000000, 0.25f);
        float out = 1f - sub(p, 0.55f, 1f);

        float rp = VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
        ground(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), P * (0.4f + 1.5f * rp), age * 0.05f, a(mid, 0.8f * (1f - rp * 0.8f) * out));
        int n = ctx.seg(8, 4);
        for (int k = 0; k < n; k++) {
            float ang = hash(seed, k, 1) * Mth.TWO_PI, d = P * (0.3f + 0.9f * hash(seed, k, 2)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.15f + 0.5f * sub(p, 0f, 0.9f)), Mth.sin(ang) * d), P * (0.4f + 0.5f * hash(seed, k, 3)), hash(seed, k, 4) * 6.28f + age * 0.04f, a(k % 2 == 0 ? dark : mid, 0.8f * out));
        }
        for (int k = 0; k < 5; k++) {                                                        // tendrils springing up round the middle
            float ang = Mth.TWO_PI * k / 5f + hash(seed, k, 5), h = P * (0.7f + 0.5f * hash(seed, k, 6)) * VfxAnim.easeOutBack(sub(p, 0.05f + 0.04f * k, 0.45f)) * (1f - sub(p, 0.6f, 1f));
            tendril(buf, new Vector3f(c).add(Mth.cos(ang) * P * 0.6f, 0.02f, Mth.sin(ang) * P * 0.6f), P * 0.5f, h, a(dark, 0.95f));
        }
        for (int k = 0; k < 8; k++) {
            float ang = hash(seed, k, 7) * Mth.TWO_PI, d = P * (0.4f + 1.0f * hash(seed, k, 8)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.75f));
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.7f * hash(seed, k, 9)) * Mth.sin(sub(p, 0f, 1f) * 2.6f) + 0.1f, Mth.sin(ang) * d), P * 0.2f, ang + age * 0.15f, a(PALE, 0.9f * out));
        }
        float fl = 1f - sub(p, 0f, 0.28f);
        if (fl > 0f) sprite(ctx, buf, EMBER, VfxBlend.ADD, new Vector3f(c).add(0, P * 0.3f, 0), P * (1.2f + 1.4f * (1f - fl)), 0f, a(HOT, 0.85f * fl));
        for (int k = 0; k < 8; k++) {
            float ang = hash(seed, k, 10) * Mth.TWO_PI, d = P * (0.4f + 0.9f * hash(seed, k, 11)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
            sprite(ctx, buf, EMBER, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.8f * hash(seed, k, 12)) * sub(p, 0f, 0.8f), Mth.sin(ang) * d), P * 0.09f, 0f, a(FIRE, 0.9f * out * (1f - sub(p, 0.5f, 1f))));
        }
    }
}
