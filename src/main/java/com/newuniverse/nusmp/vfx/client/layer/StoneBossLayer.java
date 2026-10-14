package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.35.0: the Goliath's look (stone shockwave rings, dust columns, boulder trails, earth fissure lines). Everything is drawn: no block is
 * placed, moved or broken. Each shape has a TELEGRAPH voice (seed bit 0 = 0: dim, steady, closing) and a STRIKE voice (seed bit 0 = 1).
 * 'color' is the heat of the stone's cracks (amber by default; the Gates make it red). At most about 280 vertices each.
 * <ul>
 *   <li>BOSS_STONE_RING: 'from' = centre, power = reach. Telegraph: the outline of the wave on the floor with a ring that closes in.
 *       Strike: a thick ring of grey dust runs out to the reach, throwing chips and a skirt of dust.</li>
 *   <li>BOSS_STONE_COLUMN: 'from' = base, power = height scale. A column of dust rising in puffs round a hot core; the strike shoots it up.</li>
 *   <li>BOSS_STONE_TRAIL: 'from' = start, 'to' = landing spot. A boulder crossing the air with a trail of dust and embers; the telegraph
 *       shows the shadow at the landing spot, the strike lands it with a ring.</li>
 *   <li>BOSS_STONE_FISSURE: 'from' -> 'to', power = width. A crack in the floor: the telegraph is a dim dotted line, the strike runs a
 *       glowing jagged crack along it with plumes of dust.</li>
 * </ul>
 */
public class StoneBossLayer extends AbstractVfxLayer {
    private static final int AMBER = 0xFFFFAA33, DUST = 0xFF6E695F, ROCK = 0xFF54504A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BOSS_STONE_RING, VfxShape.BOSS_STONE_COLUMN, VfxShape.BOSS_STONE_TRAIL, VfxShape.BOSS_STONE_FISSURE); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BOSS_STONE_RING -> 30;
            case BOSS_STONE_COLUMN -> 26;
            case BOSS_STONE_TRAIL -> 20;
            default -> 36;
        };
    }

    @Override public int defaultColor(VfxShape s) { return AMBER; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BOSS_STONE_RING -> ring(inst, ctx, buf);
            case BOSS_STONE_COLUMN -> column(inst, ctx, buf);
            case BOSS_STONE_TRAIL -> trail(inst, ctx, buf);
            case BOSS_STONE_FISSURE -> fissure(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ ring
    private void ring(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), reach = Math.max(1.5f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.08, 0));
        if (!strike) {
            float alpha = VfxAnim.fadeInOut(t, 0.1f, 0.15f);
            BossFx.groundRing(ctx, buf, c, VfxTextures.RUNE_RING, VfxBlend.ADD, reach, 0.5f, 36, 0f, Math.max(6f, reach), age * 0.01f, BossFx.a(col, alpha * 0.8f));
            BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, Math.max(0.5f, reach * (1f - t)), 0.35f, 28, 0f, 1f, 0f, BossFx.a(BossFx.hot(col, 0.5f), alpha));
            int n = ctx.seg(10, 4);
            for (int i = 0; i < n; i++) {                                                   // dust drawn in towards the stamping foot
                float a = Mth.TWO_PI * i / n + age * 0.02f, k = (age * 0.06f + BossFx.rnd(inst.seed, i)) % 1f;
                BossFx.puff(ctx, buf, BossFx.onCircle(c, Math.min(reach, 4f) * (1f - k), a, 0.2f), 0.7f, a, BossFx.a(DUST, alpha * 0.5f * k));
            }
            return;
        }
        float e = VfxAnim.easeOutCubic(t), alpha = 1f - VfxAnim.easeInCubic(t);
        float rr = reach * e;
        BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, rr, 0.45f, 24, 0f, 1f, 0f, BossFx.a(BossFx.hot(col, 0.35f), alpha));
        BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, rr, 1.8f, 20, 0f, 1f, 0f, BossFx.a(col, alpha * 0.55f));
        int skirt = ctx.seg(24, 10);
        for (int i = 0; i < skirt; i++) {                                                     // the wall of dust the wave pushes: soft grey puffs along the edge, thrown outward
            float a = Mth.TWO_PI * i / skirt + BossFx.rnd(inst.seed, i + 100) * 0.2f;
            float jr = (BossFx.rnd(inst.seed, i + 200) - 0.5f) * 1.2f;
            BossFx.puff(ctx, buf, BossFx.onCircle(c, Math.max(0.2f, rr + jr), a, 0.5f + 0.7f * t), 2.0f + 1.8f * t, a + age * 0.05f, BossFx.a(DUST, alpha * 0.6f));
        }
        int chips = ctx.seg(12, 5);
        for (int i = 0; i < chips; i++) {
            float a = Mth.TWO_PI * i / chips + BossFx.rnd(inst.seed, i) * 0.3f;
            float hop = Mth.sin(Mth.PI * ((age * 0.15f + BossFx.rnd(inst.seed, i + 30)) % 1f)) * 1.1f;
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ALPHA, BossFx.onCircle(c, rr, a, 0.15f + hop), 0.45f, a + age * 0.3f, BossFx.a(ROCK, alpha));
        }
        if (t < 0.35f) VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.4f, 0), Math.min(3f, reach * 0.15f + 1f), col, 1f - t * 2.5f);
    }

    // ------------------------------------------------------------------ column
    private void column(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), s = Math.max(0.6f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.08, 0));
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.15f, 0.2f) * 0.7f;
        float height = (strike ? 5.5f : 2.5f) * s * (strike ? VfxAnim.easeOutCubic(Math.min(1f, t * 3f)) : 0.4f + 0.6f * t);
        BossFx.band(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, s * 1.2f, 0.4f, 18, 0f, BossFx.a(col, alpha * 0.9f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c), new Vector3f(c).add(0, height, 0), 0.5f * s, 0.15f * s, 3, -age * 0.06f,
                BossFx.a(BossFx.hot(col, 0.5f), alpha * 0.8f), BossFx.a(col, alpha * 0.2f));
        int n = ctx.seg(9, 4);
        for (int i = 0; i < n; i++) {
            float k = (float) i / n, a = age * 0.25f + i * 1.7f;
            float rad = (0.5f + 0.9f * k) * s * (strike ? 1f : 0.7f);
            Vector3f p = new Vector3f(c).add(Mth.cos(a) * rad, height * k, Mth.sin(a) * rad);
            BossFx.puff(ctx, buf, p, (1.2f + 1.5f * k) * s, a, BossFx.a(DUST, alpha * (0.9f - 0.5f * k)));
        }
        if (strike) VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.6f, 0), 1.5f * s, col, 1f - t);
    }

    // ------------------------------------------------------------------ trail
    private void trail(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), s = Math.max(0.5f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color;
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vector3f a = ctx.rel(from), b = ctx.rel(to.add(0, 0.08, 0));
        if (!strike) {                                                                       // the shadow the stone will fall on
            float alpha = VfxAnim.fadeInOut(t, 0.15f, 0.15f);
            BossFx.band(ctx, buf, b, VfxTextures.GLOW, VfxBlend.ADD, s * (1.4f - 0.6f * t), 0.3f, 18, 0f, BossFx.a(col, alpha * 0.8f));
            BossFx.band(ctx, buf, b, VfxTextures.GLOW, VfxBlend.ALPHA, s * 0.7f, s * 1.2f, 14, 0f, BossFx.a(0xFF1E1C18, alpha * 0.5f));
            return;
        }
        float p = VfxAnim.easeInCubic(Math.min(1f, t / 0.85f));
        Vector3f head = new Vector3f(a).lerp(b, p);
        int n = ctx.seg(8, 3);
        for (int i = 1; i <= n; i++) {
            float k = (float) i / n, q = Math.max(0f, p - k * 0.3f);
            Vector3f tp = new Vector3f(a).lerp(b, q).add((BossFx.rnd(inst.seed, i) - 0.5f) * 0.4f, 0, (BossFx.rnd(inst.seed, i + 20) - 0.5f) * 0.4f);
            BossFx.puff(ctx, buf, tp, (1.8f - 0.8f * k) * s, age + i, BossFx.a(DUST, 0.85f * (1f - k) * (t < 0.9f ? 1f : 1f - (t - 0.9f) * 10f)));
            if (i % 2 == 0) BossFx.spark(ctx, buf, tp, 0.3f * s, age + i, BossFx.a(col, 0.8f * (1f - k)));
        }
        if (t < 0.88f) {
            for (int i = 0; i < 3; i++)
                buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ALPHA, new Vector3f(head).add((BossFx.rnd(inst.seed, i + 50) - 0.5f) * 0.5f * s, (BossFx.rnd(inst.seed, i + 60) - 0.5f) * 0.5f * s, 0),
                        (1.7f - 0.3f * i) * s, age * 0.3f + i, BossFx.a(ROCK, 1f));
            VfxBloom.glow(ctx, buf, head, 0.6f * s, col, 0.8f);
        } else {
            float k = (t - 0.88f) / 0.12f;
            BossFx.band(ctx, buf, b, VfxTextures.GLOW, VfxBlend.ADD, s * (0.6f + 1.8f * k), 0.5f, 22, 0f, BossFx.a(BossFx.hot(col, 0.4f), 1f - k * 0.6f));
            VfxBloom.glow(ctx, buf, new Vector3f(b).add(0, 0.4f, 0), s, col, 1f - k * 0.5f);
        }
    }

    // ------------------------------------------------------------------ fissure
    private void fissure(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), w = Math.max(0.4f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color;
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vector3f a = ctx.rel(from.add(0, 0.09, 0)), b = ctx.rel(to.add(0, 0.09, 0));
        if (!strike) {
            float alpha = VfxAnim.fadeInOut(t, 0.12f, 0.15f) * 0.7f;
            int dots = ctx.seg(14, 6);
            for (int i = 0; i < dots; i++) {
                float k = (float) i / (dots - 1);
                Vector3f p = new Vector3f(a).lerp(b, k).add((BossFx.rnd(inst.seed, i) - 0.5f) * 0.5f, 0, (BossFx.rnd(inst.seed, i + 30) - 0.5f) * 0.5f);
                BossFx.spark(ctx, buf, p, 0.45f * w, age * 0.1f + i, BossFx.a(col, alpha * (0.5f + 0.5f * VfxAnim.pulse(age + i * 3, 2f))));
            }
            BossFx.glowLineFlat(ctx, buf, a, b, 0.05f * w, col, alpha * 0.5f);
            return;
        }
        float run = VfxAnim.easeOutCubic(Math.min(1f, t / 0.4f)), alpha = 1f - VfxAnim.easeInCubic(Math.max(0f, (t - 0.45f) / 0.55f));
        Vector3f end = new Vector3f(a).lerp(b, run);
        BossFx.glowLineFlat(ctx, buf, a, end, 0.9f * w, col, alpha * 0.3f);
        BossFx.jagged(ctx, buf, a, end, ctx.seg(10, 4), 0.7f, inst.seed, 0, 0.5f * w, col, alpha);
        BossFx.glowLine(ctx, buf, a, end, 0.12f * w, BossFx.hot(col, 0.5f), alpha * 0.7f);
        int plumes = ctx.seg(6, 3);
        for (int i = 0; i < plumes; i++) {
            float k = (i + 0.5f) / plumes;
            if (k > run) break;
            float rise = Mth.clamp((t - k * 0.4f) * 3f, 0f, 1f);
            Vector3f p = new Vector3f(a).lerp(b, k).add(0, 0.3f + 1.6f * rise, 0);
            BossFx.puff(ctx, buf, p, (1.4f + 1.4f * rise) * w, i + age * 0.1f, BossFx.a(DUST, alpha * 0.7f * (1f - rise * 0.6f)));
        }
    }
}
