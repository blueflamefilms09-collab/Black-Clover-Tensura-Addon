package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Snow Magic: cold, white and crystalline. Textures from tools/gen_snow_rebuild.py (textures/particle/snow_*.png). Sizes: VfxInstance multiplies the
 * payload power by 3 for FX1 / FX3 and by 1.5 for FX2, so {@code inst.power} is that product.
 * <ul>
 *   <li>SNOW_FX1, Snow Bullet / Crescent / Drift (cast): a big six-armed flake turns open at 'from' (the charge), then a packed white head with a flake
 *       halo runs to 'to'; snowflakes spiral round the path (a helix, additive glints on them), a soft streak and ice shards trail behind it, and a flake
 *       bursts open at 'to'. 'from' = hand, 'to' = target, default 16 ticks.</li>
 *   <li>SNOW_FX2, Blizzard / Frost Crystal pulse / Whiteout (zone): a great flake sigil and a ring with tick marks turn on the ground, low drifting snow
 *       clouds hug the floor, snowflakes fall through the whole area on slanted lines, wind streaks sweep round the rim, glints twinkle. 'from' = centre,
 *       power = radius, fades in over 8 ticks and out over 12, default 80 ticks.</li>
 *   <li>SNOW_FX3, impact / golem call / crumble (burst): a white flash with a flake that unfolds and a ring across the ground, ice shards thrown outwards,
 *       puffs of powder rising, glints. 'from' = centre, default 22 ticks.</li>
 * </ul>
 * Alpha blend first (snow is solid-ish white), additive last (glints, halo glow). About 150 vertices of 400.
 */
public class SnowLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation PUFF = t("snow_puff"), FLAKE = t("snow_flake"), STREAK = t("snow_streak"), RING = t("snow_ring"), GLINT = t("snow_glint"), SHARD = t("snow_shard");

    private static final int WHITE = 0xFFF4FBFF, ICE = 0xFFA6DCFF, DEEP = 0xFF5AA8E8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SNOW_FX1, VfxShape.SNOW_FX2, VfxShape.SNOW_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SNOW_FX1 -> 16;
            case SNOW_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE7F7FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SNOW_FX1 -> cast(inst, ctx, buf);
            case SNOW_FX2 -> zone(inst, ctx, buf);
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

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.3f;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        int white = inst.color | 0xFF000000;
        white = VfxVertexBuffer.lerpColor(white, WHITE, 0.6f);

        float run = VfxAnim.easeInOutSine(sub(p, 0.12f, 0.85f)) * 0.4f + sub(p, 0.12f, 0.85f) * 0.6f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);
        float cp = sub(p, 0f, 0.25f), cf = 1f - sub(p, 0.2f, 0.45f);

        // 1. streak and shards trailing (alpha first)
        if (run > 0.02f && live > 0f) {
            Vector3f tail = lerp(f, to, Math.max(0f, run - 0.4f));
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ALPHA, tail, head, P * 0.55f, P * 0.2f, 1, 0f, a(white, 0.0f), a(white, 0.8f * live));
            for (int k = 0; k < 5; k++) {
                float tk = Math.max(0f, run - 0.06f * (k + 1) * (0.7f + hash(seed, k, 1)));
                Vector3f sp = lerp(f, to, tk).add(new Vector3f(side).mul((hash(seed, k, 2) - 0.5f) * P * 0.8f)).add(new Vector3f(up).mul((hash(seed, k, 3) - 0.5f) * P * 0.6f));
                sprite(ctx, buf, SHARD, VfxBlend.ALPHA, sp, P * 0.28f, hash(seed, k, 4) * 6.28f + age * 0.2f, a(ICE, 0.9f * live * (1f - k * 0.12f)));
            }
        }
        // 2. the flake helix round the path
        int n = ctx.seg(8, 4);
        for (int k = 0; k < n; k++) {
            float tk = run - 0.05f * k;
            if (tk <= 0.02f) continue;
            float ang = age * 0.55f + k * 1.1f, r = P * (0.18f + 0.1f * k / n);
            Vector3f sp = lerp(f, to, tk).add(new Vector3f(side).mul(Mth.cos(ang) * r)).add(new Vector3f(up).mul(Mth.sin(ang) * r));
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, sp, P * (0.3f - 0.015f * k), age * 0.1f + k, a(white, (0.95f - 0.09f * k) * live));
        }
        // 3. the head: a packed snowball with a flake halo
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, PUFF, VfxBlend.ALPHA, head, P * 0.7f, age * 0.05f, a(white, 0.98f * live));
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, head, P * 1.1f, -age * 0.12f, a(white, 0.8f * live));
        }
        // 4. additive: the charge flake at 'from', glow on the head, the bloom at 'to'
        if (cf > 0f) {
            sprite(ctx, buf, FLAKE, VfxBlend.ADD, f, P * (1.9f - 0.7f * VfxAnim.easeOutCubic(cp)), age * 0.15f, a(ICE, 0.8f * cf));
            sprite(ctx, buf, GLINT, VfxBlend.ADD, f, P * (0.5f + 1.2f * cp), 0f, a(WHITE, 0.9f * cf));
        }
        if (run > 0.01f && live > 0f) sprite(ctx, buf, GLINT, VfxBlend.ADD, head, P * (0.55f + 0.15f * Mth.sin(age * 0.6f)), age * 0.2f, a(ICE, 0.95f * live));
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            sprite(ctx, buf, FLAKE, VfxBlend.ADD, to, P * (0.6f + 1.8f * VfxAnim.easeOutCubic(bp)), age * 0.1f, a(ICE, 0.8f * (1f - bp)));
            sprite(ctx, buf, GLINT, VfxBlend.ADD, to, P * (0.5f + 0.9f * bp), 0f, a(WHITE, 0.8f * (1f - bp)));
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
        int white = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, WHITE, 0.6f);

        // 1. ground: the flake sigil and the tick ring (alpha), low drifting clouds
        ground(buf, FLAKE, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.05f, 0), R * 0.95f * open, age * 0.01f, a(white, 0.6f * fade));
        ground(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), R * open, -age * 0.015f, a(ICE, 0.7f * fade));
        int low = ctx.seg(8, 4);
        for (int k = 0; k < low; k++) {
            float ang = hash(seed, k, 1) * Mth.TWO_PI + age * 0.02f * (k % 2 == 0 ? 1 : -1), rr = R * (0.2f + 0.75f * hash(seed, k, 2));
            sprite(ctx, buf, PUFF, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rr, 0.3f + 0.15f * Mth.sin(age * 0.07f + k), Mth.sin(ang) * rr), R * 0.42f, hash(seed, k, 3) * 6.28f, a(white, 0.55f * fade));
        }
        // 2. snowflakes falling on slanted lines through the area
        int n = ctx.seg(22, 10);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.018f + hash(seed, k, 4)) % 1f;
            float ang = hash(seed, k, 5) * Mth.TWO_PI, rr = R * Mth.sqrt(hash(seed, k, 6));
            float slant = (ph - 0.5f) * R * 0.5f;
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * rr + slant, R * 1.15f * (1f - ph) + 0.1f, Mth.sin(ang) * rr);
            sprite(ctx, buf, FLAKE, VfxBlend.ALPHA, pos, R * (0.07f + 0.08f * hash(seed, k, 7)), age * 0.04f + k, a(white, Mth.sin(ph * Mth.PI) * 0.9f * fade));
        }
        // 3. additive: wind streaks sweeping round the rim, glints
        for (int k = 0; k < 4; k++) {
            float ang = age * 0.06f + k * Mth.HALF_PI, h = 0.4f + R * 0.18f * k;
            Vector3f p0 = new Vector3f(c).add(Mth.cos(ang) * R * 0.9f, h, Mth.sin(ang) * R * 0.9f);
            Vector3f p1 = new Vector3f(c).add(Mth.cos(ang + 0.7f) * R * 0.9f, h + 0.2f, Mth.sin(ang + 0.7f) * R * 0.9f);
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, R * 0.25f, R * 0.06f, 1, 0f, a(ICE, 0.0f), a(ICE, 0.55f * fade));
        }
        for (int k = 0; k < 8; k++) {
            float tw = Mth.sin(age * 0.2f + k * 2.3f) * 0.5f + 0.5f;
            tw *= tw;
            float ang = hash(seed, k, 8) * Mth.TWO_PI, rr = R * 0.8f * hash(seed, k, 9);
            sprite(ctx, buf, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + R * 0.5f * hash(seed, k, 10), Mth.sin(ang) * rr), R * (0.06f + 0.12f * tw), age * 0.03f, a(WHITE, 0.9f * tw * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.3f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int white = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, WHITE, 0.6f);
        float out = 1f - sub(p, 0.55f, 1f);

        float rp = VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
        ground(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), P * (0.4f + 1.6f * rp), age * 0.06f, a(ICE, 0.8f * (1f - rp * 0.8f) * out));
        int n = ctx.seg(8, 4);
        for (int k = 0; k < n; k++) {                                                           // powder puffs rising
            float ang = hash(seed, k, 1) * Mth.TWO_PI, d = P * (0.2f + 0.8f * hash(seed, k, 2)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            sprite(ctx, buf, PUFF, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.1f + 0.7f * sub(p, 0f, 1f)), Mth.sin(ang) * d),
                    P * (0.35f + 0.4f * hash(seed, k, 3)), hash(seed, k, 4) * 6.28f, a(white, 0.7f * out));
        }
        for (int k = 0; k < 9; k++) {                                                           // ice shards thrown out
            float ang = hash(seed, k, 5) * Mth.TWO_PI, spd = 0.7f + 0.9f * hash(seed, k, 6);
            float d = P * spd * VfxAnim.easeOutCubic(sub(p, 0f, 0.75f)), h = P * (0.2f + 0.7f * hash(seed, k, 7)) * Mth.sin(sub(p, 0f, 1f) * 2.4f);
            sprite(ctx, buf, SHARD, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, Math.max(0.05f, h), Mth.sin(ang) * d), P * 0.3f, ang + age * 0.15f, a(ICE, 0.95f * out));
        }
        float fp = sub(p, 0f, 0.5f);
        sprite(ctx, buf, FLAKE, VfxBlend.ADD, new Vector3f(c).add(0, P * 0.3f, 0), P * (0.5f + 1.7f * VfxAnim.easeOutCubic(fp)), age * 0.12f, a(ICE, 0.85f * (1f - sub(p, 0.3f, 0.9f))));
        float fl = 1f - sub(p, 0f, 0.28f);
        if (fl > 0f) sprite(ctx, buf, GLINT, VfxBlend.ADD, new Vector3f(c).add(0, P * 0.3f, 0), P * (1.0f + 1.6f * (1f - fl)), 0f, a(WHITE, 0.9f * fl));
        for (int k = 0; k < 7; k++) {
            float ang = hash(seed, k, 8) * Mth.TWO_PI, d = P * (0.3f + 0.9f * hash(seed, k, 9)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            float tw = Mth.sin(age * 0.5f + k * 2f) * 0.5f + 0.5f;
            sprite(ctx, buf, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.8f * hash(seed, k, 10)), Mth.sin(ang) * d), P * (0.08f + 0.12f * tw), 0f, a(WHITE, 0.9f * out * tw));
        }
    }
}
