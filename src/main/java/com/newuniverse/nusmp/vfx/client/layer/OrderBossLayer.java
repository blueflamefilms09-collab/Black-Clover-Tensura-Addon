package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.35.0: the God of Order's look (white-gold law sigils, grid lines, correction beams, ledger glyphs). Strict, straight and symmetrical: no
 * flame, no water, only lines, hexagons and written light. Each shape has a TELEGRAPH voice (seed bit 0 = 0: steady, closing) and a STRIKE voice
 * (seed bit 0 = 1: the verdict lands). 'color' is the gold of the Order (white-gold by default). At most about 300 vertices each.
 * <ul>
 *   <li>BOSS_ORDER_SIGIL: 'from' = floor centre, power = radius. A gold circle with a hexagon and a ring of ledger script turning against it;
 *       the telegraph closes a ring on it, the strike stamps it down in a flash with six columns of light at the corners.</li>
 *   <li>BOSS_ORDER_GRID: 'from' = floor centre, 'to' = a point along the grid's first axis, power = radius. A lattice of straight lines
 *       laid over the floor (the ledger's rulings); the strike runs a bright pulse out along it and thickens every line once.</li>
 *   <li>BOSS_ORDER_BEAM: 'from' -> 'to', power = width in blocks. The correction beam: a thin line and a ring on the target in the telegraph;
 *       in the strike a wide white-gold beam with script spiralling along it and a stamped ring where it lands.</li>
 *   <li>BOSS_ORDER_LEDGER: 'from' = floor centre, 'to' = the halo's centre (a point overhead), power = radius. A halo of ledger script turning
 *       upright round the figure, glyphs orbiting it and ruled lines of light rising from the floor; the strike rings the halo out.</li>
 * </ul>
 */
public class OrderBossLayer extends AbstractVfxLayer {
    private static final int GOLD = 0xFFE8B923, WHITE_GOLD = 0xFFFFF0B8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BOSS_ORDER_SIGIL, VfxShape.BOSS_ORDER_GRID, VfxShape.BOSS_ORDER_BEAM, VfxShape.BOSS_ORDER_LEDGER); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BOSS_ORDER_SIGIL -> 40;
            case BOSS_ORDER_GRID -> 36;
            case BOSS_ORDER_BEAM -> 16;
            default -> 60;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GOLD; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BOSS_ORDER_SIGIL -> sigil(inst, ctx, buf);
            case BOSS_ORDER_GRID -> grid(inst, ctx, buf);
            case BOSS_ORDER_BEAM -> beam(inst, ctx, buf);
            case BOSS_ORDER_LEDGER -> ledger(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ sigil
    private void sigil(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(1f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.07, 0));
        VfxPose pose = VfxPose.ground(c);
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.12f, 0.15f);
        float scale = strike ? 1.25f - 0.25f * VfxAnim.easeOutCubic(Math.min(1f, t * 4f)) : VfxAnim.circleOpen(t);
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, pose.spin(age * 0.02f), r * scale, BossFx.a(col, alpha * 0.8f));
        buf.plane(VfxTextures.ELF_CIRCLE, VfxBlend.ADD, pose.lift(0.02f).spin(-age * 0.035f), r * 0.66f * scale, BossFx.a(core, alpha * 0.6f));
        BossFx.groundRing(ctx, buf, c, VfxTextures.RUNE_RING, VfxBlend.ADD, r * 0.9f * scale, Math.min(1f, 0.3f + r * 0.07f), 28, 0f, 8f, age * 0.012f, BossFx.a(core, alpha));
        float rot = age * 0.008f;
        for (int i = 0; i < 6; i++) {                                                       // the hexagon of the law
            float a0 = rot + Mth.TWO_PI * i / 6f, a1 = rot + Mth.TWO_PI * (i + 1) / 6f;
            BossFx.glowLineFlat(ctx, buf, BossFx.onCircle(c, r * 0.97f * scale, a0, 0.06f), BossFx.onCircle(c, r * 0.97f * scale, a1, 0.06f), strike ? 0.22f : 0.12f, core, alpha);
        }
        if (!strike) BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, Math.max(0.3f, r * (1.2f - 1.0f * t)), 0.22f, 20, 0f, 1f, 0f, BossFx.a(WHITE_GOLD, alpha));
        if (strike) {
            for (int i = 0; i < 6; i++) {
                Vector3f foot = BossFx.onCircle(c, r * 0.97f, rot + Mth.TWO_PI * i / 6f, 0f);
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(foot).add(0, 6f * (1f - t), 0), foot, 0.5f * (1f - t) + 0.1f, 0.5f * (1f - t) + 0.1f, 1, 0,
                        BossFx.a(core, alpha * 0.3f), BossFx.a(WHITE_GOLD, alpha));
            }
            VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.4f, 0), r * 0.5f, col, alpha);
        }
    }

    // ------------------------------------------------------------------ grid
    private void grid(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(2f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.6f);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vec3 d = to.subtract(from).multiply(1, 0, 1);
        float yaw = d.lengthSqr() < 1e-4 ? 0f : (float) Math.atan2(d.z, d.x);
        Vector3f c = ctx.rel(from.add(0, 0.07, 0));
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.15f, 0.15f) * 0.8f;
        float extent = strike ? 1f : VfxAnim.easeOutCubic(Math.min(1f, t * 2.5f));
        int lines = ctx.seg(7, 4);
        float step = 2f * r / (lines - 1);
        float cy = Mth.cos(yaw), sy = Mth.sin(yaw);
        float pulse = strike ? t * 1.6f : -1f;                                                       // the ring of brightness that runs out
        for (int axis = 0; axis < 2; axis++) {
            for (int i = 0; i < lines; i++) {
                float off = -r + i * step, half = (float) Math.sqrt(Math.max(0f, r * r - off * off)) * extent;
                if (half < 0.2f) continue;
                float ax = axis == 0 ? -half : off, az = axis == 0 ? off : -half, bx = axis == 0 ? half : off, bz = axis == 0 ? off : half;
                Vector3f p = new Vector3f(c.x + ax * cy - az * sy, c.y, c.z + ax * sy + az * cy);
                Vector3f q = new Vector3f(c.x + bx * cy - bz * sy, c.y, c.z + bx * sy + bz * cy);
                float lum = strike ? Math.max(0f, 1f - Math.abs(Math.abs(off) / r - pulse) * 3f) : 0f;
                BossFx.glowLineFlat(ctx, buf, p, q, strike ? 0.2f + 0.16f * lum : 0.14f, lum > 0.2f ? WHITE_GOLD : core, alpha * (0.7f + 0.3f * lum));
            }
        }
        BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, r * extent, 0.2f, 28, 0f, 1f, 0f, BossFx.a(col, alpha));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f, 0), r * 0.18f, col, alpha * (0.4f + 0.2f * VfxAnim.pulse(age, 2f)));
    }

    // ------------------------------------------------------------------ beam
    private void beam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), w = Math.max(0.3f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.7f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        if (!strike) {
            float alpha = VfxAnim.fadeInOut(t, 0.12f, 0.15f) * 0.8f;
            BossFx.glowLineFlat(ctx, buf, a, b, 0.07f, core, alpha * 0.7f);
            BossFx.groundRing(ctx, buf, b, VfxTextures.RUNE_RING, VfxBlend.ADD, Math.max(0.5f, w * 1.6f * (1.4f - 0.6f * t)), 0.3f, 18, 0f, 6f, age * 0.03f, BossFx.a(col, alpha));
            BossFx.spark(ctx, buf, a, 0.5f + 0.5f * t, age, BossFx.a(core, alpha));
            return;
        }
        float fade = 1f - VfxAnim.easeInCubic(t), grow = VfxAnim.easeOutBack(Math.min(1f, t * 6f)), width = w * grow * (1f - 0.5f * t);
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, width * 1.6f, width * 1.6f, 1, 0, BossFx.a(col, fade * 0.5f), BossFx.a(col, fade * 0.5f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, width * 0.7f, width * 0.7f, 2, 0, BossFx.a(core, fade), BossFx.a(core, fade));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len > 0.1f) {
            dir.div(len);
            Vector3f side = new Vector3f(dir).cross(0, 1, 0);
            if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
            side.normalize();
            Vector3f up = new Vector3f(side).cross(dir).normalize();
            int n = ctx.seg(12, 5);
            for (int i = 0; i < n; i++) {                                                      // script spiralling along the beam
                float k = ((float) i / n + age * 0.03f) % 1f, ang = k * 18f + i;
                Vector3f p = new Vector3f(a).add(new Vector3f(dir).mul(len * k)).add(new Vector3f(side).mul(Mth.cos(ang) * width * 0.9f)).add(new Vector3f(up).mul(Mth.sin(ang) * width * 0.9f));
                BossFx.spark(ctx, buf, p, 0.4f, ang, BossFx.a(WHITE_GOLD, fade));
            }
        }
        BossFx.groundRing(ctx, buf, b, VfxTextures.GLOW, VfxBlend.ADD, w * 1.2f + 2.2f * VfxAnim.easeOutCubic(t), 0.4f, 22, 0f, 1f, 0f, BossFx.a(core, fade));
        VfxBloom.glow(ctx, buf, b, w * 1.2f, col, fade);
        VfxBloom.glow(ctx, buf, a, w * 0.8f, col, fade);
    }

    // ------------------------------------------------------------------ ledger
    private void ledger(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(1f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.6f);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vector3f c = ctx.rel(from.add(0, 0.07, 0));
        float haloY = to.y > from.y + 0.5 ? (float) (to.y - from.y) : 2.9f;
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.1f, 0.12f);
        VfxPose halo = VfxPose.facing(new Vector3f(c).add(0, haloY, 0), new Vector3f(0, 0, 1)).spin(age * 0.03f);
        float hr = Math.min(r, 2.2f) * (strike ? 1f + 1.2f * VfxAnim.easeOutCubic(t) : 1f);
        buf.ring(VfxTextures.RUNE_RING, VfxBlend.ADD, halo, hr * 0.86f, hr, ctx.seg(20, 8), 6f, age * 0.01f, BossFx.a(core, alpha));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, halo.lift(0.02f).spin(-age * 0.05f), hr * 0.8f, BossFx.a(col, alpha * 0.35f));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                        // glyphs orbiting the halo
            float a = Mth.TWO_PI * i / n + age * 0.06f;
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, halo.point(Mth.cos(a) * hr * 1.08f, Mth.sin(a) * hr * 1.08f), 0.32f, a, BossFx.a(WHITE_GOLD, alpha));
        }
        for (int k = 0; k < 3; k++) {                                                        // ruled lines of light rising off the floor
            float rise = (age * 0.03f + k / 3f) % 1f;
            BossFx.groundRing(ctx, buf, new Vector3f(c).add(0, 0.1f + haloY * rise, 0), VfxTextures.GLOW, VfxBlend.ADD, r * (0.9f - 0.5f * rise), 0.12f, 12, 0f, 1f, 0f,
                    BossFx.a(core, alpha * (1f - rise) * 0.8f));
        }
        BossFx.groundRing(ctx, buf, c, VfxTextures.RUNE_RING, VfxBlend.ADD, r, 0.5f, 16, 0f, 6f, -age * 0.02f, BossFx.a(col, alpha * 0.8f));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, haloY, 0), hr * 0.5f, col, alpha * (strike ? 1f : 0.5f));
    }
}
