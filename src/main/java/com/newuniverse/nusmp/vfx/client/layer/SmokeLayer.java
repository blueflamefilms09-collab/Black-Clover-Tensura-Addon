package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Smoke Magic, drawn after the way a smoke caster works in the story: nothing is sharp, everything rolls, billows, curls and hides. Every pass names its
 * texture and blend (tools/gen_smoke_rebuild.py writes textures/particle/smoke_*.png). One note on sizes: VfxInstance multiplies the payload power by 3 for FX1 /
 * FX3 and by 1.5 for FX2, so {@code inst.power} here is already that product (a spell passes 0.34 for a bolt and 2.2 / 1.5 for a 2.2 block zone).
 * <ul>
 *   <li>SMOKE_FX1, Smoke Bullet / Serpent / Step / Wave (cast): at 'from' an ember flare and a small sigil gather for the first quarter, then a dense head of
 *       smoke (alpha blend) with a hot ember core (add) runs to 'to' trailing fourteen puffs that swell, drift sideways and fade behind it, three curling wisps
 *       and a few sparks; at the end a billow blooms at 'to'. 'from' = hand, 'to' = target, power = size, default 16 ticks.</li>
 *   <li>SMOKE_FX2, Smoke Screen / the cloud a bullet leaves (zone): a smoke sigil turns on the ground over the real area with a counter-turning ember sigil
 *       under it, a ring of billows rolls round and up through the radius, a wall of big billows stands on the rim, curls rise and sparks climb. Fades in over 8
 *       ticks and out over 12. 'from' = centre on the ground, power = radius, default 80 ticks.</li>
 *   <li>SMOKE_FX3, Smoke Clones / the cloud of a vanishing clone / impact (burst): an ember flash and a ring that runs out along the ground, ten billows
 *       thrown outwards that slow and swell, curls and sparks, four big puffs lingering up. 'from' = centre, power = size, default 22 ticks.</li>
 * </ul>
 * Order of drawing is the buffer's own (alpha first, additive last); inside one blend the passes are called back to front. About 160 vertices of 400.
 */
public class SmokeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation PUFF = t("smoke_puff"), BILLOW = t("smoke_billow"), WISP = t("smoke_wisp"), RING = t("smoke_ring"), SIGIL = t("smoke_sigil"),
            CURL = t("smoke_curl"), EMBER = t("smoke_ember");

    private static final int DARK = 0xFF5E5674, MID = 0xFF8C82A4, LIGHT = 0xFFCDC6DE, FIRE = 0xFFFFA050, HOT = 0xFFFFE2A8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SMOKE_FX1, VfxShape.SMOKE_FX2, VfxShape.SMOKE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SMOKE_FX1 -> 16;
            case SMOKE_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF82788F; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SMOKE_FX1 -> cast(inst, ctx, buf);
            case SMOKE_FX2 -> zone(inst, ctx, buf);
            default -> burst(inst, ctx, buf);
        }
    }

    // ------------------------------------------------------------------------------------------------ small helpers
    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static Vector3f lerp(Vector3f p, Vector3f q, float t) { return new Vector3f(p).lerp(q, t); }
    private static int tint(VfxInstance inst, int base) { return VfxVertexBuffer.lerpColor(base, inst.color | 0xFF000000, 0.3f); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, p, size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, float radius, float spin, int col) {
        if (buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(centre).spin(spin), radius, col);
    }

    // ------------------------------------------------------------------------------------------------ FX1: Smoke Bullet
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.min(3.2f, Math.max(0.5f, inst.power)) * 1.5f;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        int dark = tint(inst, DARK), mid = tint(inst, MID), light = tint(inst, LIGHT);

        // the charge: an ember flare and a turning sigil at 'from' for the first quarter
        float cp = sub(p, 0f, 0.25f), cf = 1f - sub(p, 0.2f, 0.45f);
        if (cf > 0f) {
            sprite(ctx, buf, SIGIL, VfxBlend.ALPHA, f, P * (1.6f - 0.7f * VfxAnim.easeOutCubic(cp)), age * 0.2f, a(mid, 0.7f * cf));
            sprite(ctx, buf, EMBER, VfxBlend.ADD, f, P * (0.5f + 1.2f * VfxAnim.easeOutCubic(cp)), 0f, a(FIRE, 0.9f * cf));
        }
        // the run: the head leaves 'from' after the charge and arrives at 0.85 of the life
        float run = VfxAnim.easeInOutSine(sub(p, 0.12f, 0.85f)) * 0.55f + sub(p, 0.12f, 0.85f) * 0.45f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);

        // 1. the trail of puffs behind the head (alpha blend, back to front: the oldest first)
        int n = ctx.seg(14, 7);
        for (int k = n - 1; k >= 0; k--) {
            float lag = (k + 1f) / (n + 1f), tk = Math.max(0f, run - lag * 0.45f);
            if (tk <= 0.001f || run < 0.01f) continue;
            float ageK = lag;                                               // 0 young .. 1 old
            Vector3f pos = lerp(f, to, tk);
            float w = (0.12f + 0.4f * ageK) * P * 0.55f;
            pos.add(new Vector3f(side).mul((hash(seed, k, 1) - 0.5f) * 2f * w)).add(new Vector3f(up).mul((hash(seed, k, 2) - 0.3f) * 1.4f * w + ageK * 0.25f * P));
            float size = P * (0.45f + 0.75f * ageK) * (0.8f + 0.4f * hash(seed, k, 3));
            sprite(ctx, buf, k % 2 == 0 ? PUFF : BILLOW, VfxBlend.ALPHA, pos, size, hash(seed, k, 4) * 6.28f + age * 0.03f * (k % 2 == 0 ? 1 : -1),
                    a(VfxVertexBuffer.lerpColor(mid, dark, ageK * 0.6f), (0.8f - 0.55f * ageK) * live));
        }
        // 2. the head: a dense billow with a hot core
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, BILLOW, VfxBlend.ALPHA, head, P * 0.95f, age * 0.07f, a(dark, 0.92f * live));
            sprite(ctx, buf, PUFF, VfxBlend.ALPHA, new Vector3f(head).add(new Vector3f(dir).mul(P * 0.15f)), P * 0.62f, -age * 0.09f, a(light, 0.6f * live));
        }
        // 3. curling wisps streaming back
        for (int k = 0; k < 3; k++) {
            if (run < 0.05f) break;
            float off = (hash(seed, k, 9) - 0.5f) * 2f;
            Vector3f tail = lerp(f, to, Math.max(0f, run - 0.3f - 0.1f * k)).add(new Vector3f(side).mul(off * P * 0.5f)).add(new Vector3f(up).mul(P * (0.2f + 0.2f * k)));
            Vector3f hd = new Vector3f(head).add(new Vector3f(side).mul(off * P * 0.2f));
            if (buf.hasBudget(4)) buf.beam(ctx, WISP, VfxBlend.ALPHA, tail, hd, P * 0.55f, P * 0.18f, 1, age * 0.02f, a(light, 0.55f * live), a(light, 0.1f * live));
        }
        // 4. the core and the sparks it sheds (additive, last)
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, EMBER, VfxBlend.ADD, head, P * 0.45f, 0f, a(HOT, 0.95f * live));
            for (int k = 0; k < 6; k++) {
                float tk = Math.max(0f, run - (0.05f + 0.07f * k) * (0.6f + hash(seed, k, 5)));
                Vector3f sp = lerp(f, to, tk).add(new Vector3f(up).mul(((hash(seed, k, 6) - 0.2f) * 0.5f + age * 0.01f) * P)).add(new Vector3f(side).mul((hash(seed, k, 7) - 0.5f) * P * 0.6f));
                sprite(ctx, buf, EMBER, VfxBlend.ADD, sp, P * 0.1f * (1.2f - 0.2f * k * 0.2f), 0f, a(FIRE, (0.9f - 0.1f * k) * live * 0.8f));
            }
        }
        // 5. the bloom where it ends
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            sprite(ctx, buf, BILLOW, VfxBlend.ALPHA, to, P * (0.6f + 1.5f * VfxAnim.easeOutCubic(bp)), age * 0.04f, a(mid, 0.7f * (1f - bp)));
            sprite(ctx, buf, EMBER, VfxBlend.ADD, to, P * (0.4f + 0.8f * bp), 0f, a(FIRE, 0.6f * (1f - bp)));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: Smoke Screen
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power);
        long seed = inst.seed;
        float open = VfxAnim.easeOutCubic(cl(age / 8f));
        float fade = cl(Math.min(age / 8f, (inst.duration - age) / 12f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int dark = tint(inst, DARK), mid = tint(inst, MID), light = tint(inst, LIGHT);

        // 1. the sigil on the ground: smoke grey over the real area, an ember sigil turning against it (alpha first, additive after)
        ground(buf, SIGIL, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.06f, 0), R * open, age * 0.012f, a(mid, 0.6f * fade));
        // 2. the wall of billows on the rim (back to front: far side drawn first is not needed, they are camera facing)
        int rim = ctx.seg(9, 5);
        for (int k = 0; k < rim; k++) {
            float ang = Mth.TWO_PI * k / rim + age * 0.004f;
            float sway = Mth.sin(age * 0.05f + k * 1.7f);
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * R * 0.94f * open, R * (0.2f + 0.04f * sway), Mth.sin(ang) * R * 0.94f * open);
            sprite(ctx, buf, k % 2 == 0 ? BILLOW : PUFF, VfxBlend.ALPHA, pos, R * (0.5f + 0.1f * sway), hash(seed, k, 1) * 6.28f + age * 0.01f, a(dark, 0.72f * fade));
        }
        // 3. billows rolling round and up through the area
        int n = ctx.seg(14, 7);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.011f + hash(seed, k, 2)) % 1f;
            float ang = hash(seed, k, 3) * Mth.TWO_PI + age * 0.02f * (k % 2 == 0 ? 1f : -1f), rr = R * (0.2f + 0.7f * hash(seed, k, 4));
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + ph * R * 0.75f, Mth.sin(ang) * rr);
            sprite(ctx, buf, PUFF, VfxBlend.ALPHA, pos, R * (0.3f + 0.35f * ph), hash(seed, k, 5) * 6.28f + age * 0.02f, a(VfxVertexBuffer.lerpColor(mid, light, ph), Mth.sin(ph * Mth.PI) * 0.7f * fade));
        }
        // 4. curls climbing the wall
        for (int k = 0; k < 5; k++) {
            float ph = (age * 0.014f + hash(seed, k, 6)) % 1f, ang = hash(seed, k, 7) * Mth.TWO_PI;
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * R * 0.85f, 0.5f + ph * R * 0.7f, Mth.sin(ang) * R * 0.85f);
            sprite(ctx, buf, CURL, VfxBlend.ALPHA, pos, R * 0.34f, ang + ph, a(light, Mth.sin(ph * Mth.PI) * 0.55f * fade));
        }
        // 5. the ember sigil and the sparks (additive, last)
        ground(buf, SIGIL, VfxBlend.ADD, new Vector3f(c).add(0, 0.09f, 0), R * 0.62f * open, -age * 0.03f, a(FIRE, 0.4f * fade * (0.7f + 0.3f * Mth.sin(age * 0.15f))));
        for (int k = 0; k < 8; k++) {
            float ph = (age * 0.02f + hash(seed, k, 8)) % 1f, ang = hash(seed, k, 9) * Mth.TWO_PI;
            float rr = R * 0.75f * hash(seed, k, 10);
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * rr, 0.15f + ph * R * 0.8f, Mth.sin(ang) * rr);
            sprite(ctx, buf, EMBER, VfxBlend.ADD, pos, R * 0.07f, 0f, a(FIRE, Mth.sin(ph * Mth.PI) * 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3: impact / vanish
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.min(3.2f, Math.max(0.5f, inst.power)) * 1.4f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int dark = tint(inst, DARK), mid = tint(inst, MID), light = tint(inst, LIGHT);
        float out = 1f - sub(p, 0.55f, 1f);

        // 1. the ring running out along the ground
        float rp = VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
        ground(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), P * (0.4f + 1.5f * rp), age * 0.05f, a(mid, 0.7f * (1f - rp * 0.8f) * out));
        // 2. billows thrown outwards, slowing as they swell
        int n = ctx.seg(10, 5);
        for (int k = 0; k < n; k++) {
            float ang = hash(seed, k, 1) * Mth.TWO_PI, spd = 0.55f + 0.9f * hash(seed, k, 2);
            float d = P * spd * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f)), up = P * (0.2f + 0.5f * hash(seed, k, 3)) * sub(p, 0f, 0.9f);
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * d, up + 0.1f, Mth.sin(ang) * d);
            float sz = P * (0.35f + 0.55f * hash(seed, k, 4)) * (0.5f + 0.9f * VfxAnim.easeOutCubic(sub(p, 0f, 0.6f)));
            sprite(ctx, buf, k % 3 == 0 ? BILLOW : PUFF, VfxBlend.ALPHA, pos, sz, hash(seed, k, 5) * 6.28f + age * 0.05f, a(k % 2 == 0 ? dark : mid, 0.75f * out));
        }
        // 3. four big puffs lingering up
        for (int k = 0; k < 4; k++) {
            float ph = sub(p, 0.1f + 0.05f * k, 1f);
            if (ph <= 0f) continue;
            float ang = hash(seed, k, 6) * Mth.TWO_PI;
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * P * 0.35f, P * (0.3f + 0.8f * ph), Mth.sin(ang) * P * 0.35f);
            sprite(ctx, buf, BILLOW, VfxBlend.ALPHA, pos, P * (0.7f + 0.5f * ph), hash(seed, k, 7) * 6.28f, a(light, 0.45f * (1f - ph)));
        }
        // 4. curls flying out
        for (int k = 0; k < 6; k++) {
            float ang = hash(seed, k, 8) * Mth.TWO_PI, d = P * (0.5f + 0.7f * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f)));
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.3f * hash(seed, k, 9)), Mth.sin(ang) * d);
            sprite(ctx, buf, CURL, VfxBlend.ALPHA, pos, P * 0.5f, ang + p * 2f, a(light, 0.6f * out));
        }
        // 5. the flash and the sparks (additive, last)
        float fp = 1f - sub(p, 0f, 0.3f);
        if (fp > 0f) sprite(ctx, buf, EMBER, VfxBlend.ADD, new Vector3f(c).add(0, P * 0.3f, 0), P * (1.2f + 1.4f * (1f - fp)), 0f, a(HOT, 0.85f * fp));
        for (int k = 0; k < 8; k++) {
            float ang = hash(seed, k, 10) * Mth.TWO_PI, d = P * (0.4f + 0.9f * hash(seed, k, 11)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.8f * hash(seed, k, 12)) * sub(p, 0f, 0.8f), Mth.sin(ang) * d);
            sprite(ctx, buf, EMBER, VfxBlend.ADD, pos, P * 0.09f, 0f, a(FIRE, 0.9f * out * (1f - sub(p, 0.5f, 1f))));
        }
    }
}
