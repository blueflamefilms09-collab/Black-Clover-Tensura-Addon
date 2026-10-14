package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.35.0: the Devil Avatar's look (hellfire circles, chains, devil sigils, brimstone cracks). Four shapes, each in two voices: the TELEGRAPH
 * (seed bit 0 = 0: the wind-up, dim and closing) and the STRIKE (seed bit 0 = 1: the move lands, bright and fast). 'color' is the Avatar's
 * own tint (violet for Lucifero, crimson for Megicula); with no tint the effect is hellfire orange. At most about 280 vertices each.
 * <ul>
 *   <li>BOSS_DEVIL_SIGIL: 'from' = floor centre, power = radius. A devil circle with an inverted pentagram, a ring of hellfire round its rim,
 *       a ring that closes in during the telegraph, tongues of flame rising from the rim; the strike flares and erupts.</li>
 *   <li>BOSS_DEVIL_CHAINS: 'from' = floor centre, 'to' = an anchor overhead (the roof), power = radius. Six iron chains hang from the anchor
 *       to a ring on the floor and haul tight; the strike snaps them taut with a flash.</li>
 *   <li>BOSS_DEVIL_CRACKS: 'from' = centre, power = reach. Brimstone cracks run out of the centre in all directions, glowing, with a jet of
 *       hellfire at the end of each; the strike opens them wide and sends a shock ring. SPOKE MODE: when 'to' lies more than 0.3 blocks to the
 *       side of 'from', its horizontal distance is the number of evenly spaced cracks (3 to 9) and its direction the first crack's.</li>
 *   <li>BOSS_DEVIL_BRAND: 'from' = floor centre of the branded one, power = radius. A small sigil of crossed lines in a ring, a pillar of
 *       dark fire falling from the roof onto it, collapsing in the strike.</li>
 * </ul>
 */
public class DevilBossLayer extends AbstractVfxLayer {
    private static final int HELLFIRE = 0xFFFF4A2A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BOSS_DEVIL_SIGIL, VfxShape.BOSS_DEVIL_CHAINS, VfxShape.BOSS_DEVIL_CRACKS, VfxShape.BOSS_DEVIL_BRAND); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BOSS_DEVIL_SIGIL -> 40;
            case BOSS_DEVIL_CHAINS -> 30;
            case BOSS_DEVIL_CRACKS -> 24;
            default -> 36;
        };
    }

    @Override public int defaultColor(VfxShape s) { return HELLFIRE; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BOSS_DEVIL_SIGIL -> sigil(inst, ctx, buf);
            case BOSS_DEVIL_CHAINS -> chains(inst, ctx, buf);
            case BOSS_DEVIL_CRACKS -> cracks(inst, ctx, buf);
            case BOSS_DEVIL_BRAND -> brand(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ sigil
    private void sigil(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(0.8f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.55f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.07, 0));
        VfxPose pose = VfxPose.ground(c);
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.12f, 0.15f) * 0.7f;
        float scale = strike ? 1f + 0.3f * VfxAnim.easeOutCubic(t) : VfxAnim.circleOpen(t);
        buf.plane(VfxTextures.DEVIL_CIRCLE, VfxBlend.ADD, pose.spin(age * 0.03f), r * scale, BossFx.a(col, alpha * 0.85f));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, pose.lift(0.02f).spin(-age * 0.05f), r * 0.68f * scale, BossFx.a(core, alpha * 0.6f));
        BossFx.star(ctx, buf, c, 5, r * 0.9f * scale, -Mth.HALF_PI + age * 0.012f, strike ? 0.28f : 0.16f, core, alpha);
        BossFx.groundRing(ctx, buf, c, VfxTextures.FLAME, VfxBlend.ADD, r * scale, Math.min(1.1f, 0.3f + r * 0.1f), 26, 0f, 7f, age * 0.03f, BossFx.a(col, alpha));
        if (!strike) BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, Math.max(0.3f, r * (1f - 0.85f * t)), 0.28f, 18, 0f, 1f, 0f, BossFx.a(core, alpha));
        int tongues = ctx.seg(8, 4);
        for (int i = 0; i < tongues; i++) {
            float a = Mth.TWO_PI * i / tongues + age * 0.01f, k = (age * 0.05f + BossFx.rnd(inst.seed, i)) % 1f;
            float h = (strike ? 2.4f : 1.2f) * k;
            Vector3f p = BossFx.onCircle(c, r * 0.97f * scale, a, h);
            buf.billboard(ctx, VfxTextures.FLAME, VfxBlend.ADD, p, (strike ? 1.5f : 0.9f) * (1f - k * 0.5f), a, BossFx.a(col, alpha * (1f - k)));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f, 0), r * (strike ? 0.5f : 0.25f), col, alpha);
    }

    // ------------------------------------------------------------------ chains
    private void chains(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(1f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, iron = 0xFF5B4952;
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vector3f top = ctx.rel(to.y > from.y + 1 ? to : from.add(0, 12, 0)), c = ctx.rel(from.add(0, 0.1, 0));
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) * 0.8f : VfxAnim.fadeInOut(t, 0.15f, 0.2f);
        float reach = strike ? 1f : VfxAnim.easeOutCubic(Math.min(1f, t * 1.8f));
        int n = ctx.seg(6, 3), links = 6;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + 0.3f;
            Vector3f foot = BossFx.onCircle(c, r * 0.85f, ang, 0.05f);
            Vector3f anchor = new Vector3f(top).add(Mth.cos(ang) * 0.6f, 0, Mth.sin(ang) * 0.6f);
            float sag = strike ? 0f : 0.5f * (1f - reach * 0.6f);
            Vector3f prev = null;
            for (int k = 0; k <= links; k++) {
                float s = (float) k / links * reach;
                Vector3f p = new Vector3f(anchor).lerp(foot, s);
                p.add((Mth.cos(ang)) * Mth.sin(Mth.PI * s) * sag, 0, Mth.sin(ang) * Mth.sin(Mth.PI * s) * sag);
                if (k > 0) {
                    buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ALPHA, p, 1.0f, (k % 2) * Mth.HALF_PI + ang, BossFx.a(iron, alpha));
                    BossFx.spark(ctx, buf, p, strike ? 0.9f : 0.55f, age * 0.2f + k, BossFx.a(col, alpha * (k % 2 == 0 ? 0.9f : 0.5f)));
                }
                prev = p;
            }
            if (prev != null) BossFx.glowLine(ctx, buf, anchor, prev, strike ? 0.12f : 0.07f, col, alpha * 0.8f);
            if (strike) BossFx.spark(ctx, buf, foot, 1.2f * (1f - t), age, BossFx.a(BossFx.hot(col, 0.6f), alpha));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.2f, 0), r * 0.3f, col, alpha * (strike ? 1f : 0.5f));
    }

    // ------------------------------------------------------------------ cracks
    private void cracks(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(1.2f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.08, 0));
        float grow = strike ? VfxAnim.easeOutCubic(Math.min(1f, t * 4f)) : VfxAnim.easeOutCubic(Math.min(1f, t * 1.6f)) * 0.7f;
        float alpha = strike ? 1f - VfxAnim.easeInCubic(Math.max(0f, (t - 0.4f) / 0.6f)) : VfxAnim.fadeInOut(t, 0.1f, 0.2f) * 0.6f;
        float flick = 0.8f + 0.2f * VfxAnim.pulse(age, 3f);
        // spoke mode: when 'to' is off to the side of 'from', its horizontal distance is the number of evenly spaced cracks and its direction the first one
        Vec3 f0 = inst.from(ctx), t0 = inst.to(ctx);
        double hx = t0.x - f0.x, hz = t0.z - f0.z, hd = Math.sqrt(hx * hx + hz * hz);
        boolean spokes = hd > 0.3;
        float baseAng = spokes ? (float) Math.atan2(hz, hx) : 0f;
        int n = spokes ? Mth.clamp(Math.round((float) hd), 3, 9) : ctx.seg(7, 4);
        for (int i = 0; i < n; i++) {
            float ang = spokes ? baseAng + Mth.TWO_PI * i / n : Mth.TWO_PI * i / n + (BossFx.rnd(inst.seed, i) - 0.5f) * 0.5f;
            float len = (spokes ? r : r * (0.7f + 0.3f * BossFx.rnd(inst.seed, i + 40))) * grow;
            Vector3f end = BossFx.onCircle(c, len, ang, 0f);
            BossFx.glowLineFlat(ctx, buf, new Vector3f(c), end, strike ? 0.9f : 0.5f, col, alpha * 0.35f);
            BossFx.jagged(ctx, buf, new Vector3f(c), end, 4, 0.9f, inst.seed, i * 20, strike ? 0.34f : 0.2f, core, alpha * flick);
            if (grow > 0.5f) {
                float k = (age * 0.07f + BossFx.rnd(inst.seed, i + 80)) % 1f;
                Vector3f jet = new Vector3f(end).add(0, (strike ? 1.8f : 0.8f) * k, 0);
                buf.billboard(ctx, VfxTextures.FLAME, VfxBlend.ADD, jet, (strike ? 1.4f : 0.8f) * (1f - 0.5f * k), ang, BossFx.a(core, alpha * (1f - k)));
            }
        }
        if (strike) BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, r * VfxAnim.easeOutCubic(t), 0.5f, 14, 0f, 1f, 0f, BossFx.a(core, alpha * 0.8f));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f, 0), r * 0.25f, col, alpha);
    }

    // ------------------------------------------------------------------ brand
    private void brand(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(0.8f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, core = BossFx.hot(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.07, 0));
        float alpha = strike ? 1f - VfxAnim.easeInCubic(t) : VfxAnim.fadeInOut(t, 0.12f, 0.15f);
        float rr = strike ? r : Math.max(0.3f, r * (1f - 0.35f * t));
        BossFx.groundRing(ctx, buf, c, VfxTextures.FLAME, VfxBlend.ADD, rr, 0.45f, 22, 0f, 6f, -age * 0.04f, BossFx.a(col, alpha));
        BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, rr * 0.55f, 0.18f, 14, 0f, 1f, 0f, BossFx.a(core, alpha * 0.8f));
        float spin = age * 0.03f;
        for (int i = 0; i < 2; i++) {                                                    // the crossed lines of the brand
            float a = spin + Mth.HALF_PI * i + Mth.PI * 0.25f;
            BossFx.glowLine(ctx, buf, BossFx.onCircle(c, rr * 0.8f, a, 0.06f), BossFx.onCircle(c, rr * 0.8f, a + Mth.PI, 0.06f), 0.09f, core, alpha);
        }
        // the pillar of dark fire from the roof: dim and growing in the telegraph, white-hot and gone in the strike
        float top = 10f * (strike ? 1f - t : VfxAnim.easeOutCubic(t));
        float w = strike ? 1.4f * (1f - t) + 0.3f : 0.3f + 0.5f * t;
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c).add(0, top, 0), new Vector3f(c), w, w * 1.2f, 1, 0f,
                BossFx.a(col, alpha * 0.4f), BossFx.a(core, alpha * 0.8f));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), r * (strike ? 0.6f : 0.3f), col, alpha);
    }
}
