package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * 1.35.0 Dante Zogratis (Lucifero, gravity): wells of warped space, shock rings, compressing lanes and black spheres. Violet-black with a
 * hot orange core for his Body Magic. All five shapes are cheap (at most about 35 quads).
 * <ul>
 *   <li>GRAVITY_WELL: at 'from' a disc of space bent into a funnel: bright rings crowd toward a dark centre and turn slowly, ring after ring
 *       falls inward, motes spiral in. power = radius; duration = how long the well is open. The telegraph form (seed bit 0) is dimmer and its
 *       rim ring closes in over the whole life, so the time left is the radius left.</li>
 *   <li>GRAVITY_IMPACT: at 'from' a crushing blow: ground cracks flash and stay a moment, two rings of bent space run outward, a standing ring of
 *       dust rises, rock shards fly. power = radius.</li>
 *   <li>GRAVITY_LANES: a lane of chevrons from 'from' to 'to' on the ground (power = width). The telegraph form crawls toward 'to' dimly; the
 *       strike form flashes bright and sends streaks down the lane.</li>
 *   <li>GRAVITY_SINGULARITY: a black sphere over 'from' that swells while motes fall into it, wrapped in a lensing ring and a tilted accretion
 *       disc; in its last ticks it shrinks to a point and flashes. power = radius of the zone it belongs to (the sphere is a third of it).</li>
 *   <li>GRAVITY_AURA: the heat and weight round the followed body: rising embers, a pulsing ring at the feet and a veil of warped air.
 *       power = size.</li>
 * </ul>
 */
public class ZogratisGravityLayer extends AbstractVfxLayer {
    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.GRAVITY_WELL, VfxShape.GRAVITY_IMPACT, VfxShape.GRAVITY_LANES, VfxShape.GRAVITY_SINGULARITY, VfxShape.GRAVITY_AURA);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case GRAVITY_WELL -> 60;
            case GRAVITY_IMPACT -> 22;
            case GRAVITY_LANES -> 24;
            case GRAVITY_SINGULARITY -> 60;
            default -> 40;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9A5BE0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case GRAVITY_WELL -> well(inst, ctx, buf);
            case GRAVITY_IMPACT -> impact(inst, ctx, buf);
            case GRAVITY_LANES -> lanes(inst, ctx, buf);
            case GRAVITY_SINGULARITY -> singularity(inst, ctx, buf);
            case GRAVITY_AURA -> aura(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ GRAVITY_WELL
    private void well(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        boolean tell = telegraph(inst);
        float fade = ElementFx.life(inst, age, 6, Math.min(12, inst.duration / 3f));
        float open = outCubic(Mth.clamp(age / 8f, 0f, 1f));
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float r = R * open;
        ground(buf, UMBRA, VfxBlend.ALPHA, c, 0.03f, r * 0.62f, 0f, a(BLACK, 0.55f * fade));                 // the pit
        ground(buf, WARP_DISC, VfxBlend.ADD, c, 0.05f, r, -age * 0.035f, a(col, (tell ? 0.45f : 0.8f) * fade));
        ground(buf, WARP_DISC, VfxBlend.ADD, c, 0.06f, r * 0.62f, age * 0.06f, a(hot(col, 0.35f), 0.5f * fade));
        for (int k = 0; k < 2; k++) {                                                                             // rings falling inward
            float ph = (age * 0.05f + k * 0.5f) % 1f;
            float rr = r * (1f - ph);
            ground(buf, LENS, VfxBlend.ADD, c, 0.07f, rr / 0.62f, 0f, a(hot(col, 0.2f), Mth.sin(ph * Mth.PI) * 0.65f * fade));
        }
        if (tell) ground(buf, LENS, VfxBlend.ADD, c, 0.08f, R * (1f - 0.92f * t) / 0.9f, 0f, a(WHITE, 0.85f * fade));    // closing rim = time left
        else ground(buf, LENS, VfxBlend.ADD, c, 0.08f, R / 0.9f, 0f, a(hot(col, 0.4f), 0.55f * fade));
        int motes = ctx.seg(10, 4);
        for (int k = 0; k < motes; k++) {                                                                         // dust and rock spiralling in
            float ph = (age * 0.04f + hash(inst.seed, k, 1)) % 1f;
            float rad = r * (1f - ph * ph), ang = hash(inst.seed, k, 2) * Mth.TWO_PI + ph * 2.4f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, 0.15f + ph * 0.9f * (1f - ph), Mth.sin(ang) * rad);
            buf.billboard(ctx, k % 3 == 0 ? SHARD : SPARK, VfxBlend.ADD, p, 0.22f + 0.18f * (1f - ph), ph * 4f, a(k % 2 == 0 ? col : hot(col, 0.5f), 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ GRAVITY_IMPACT
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float spin = hash(inst.seed, 0, 5) * Mth.TWO_PI;
        float grow = outCubic(Mth.clamp(t * 3f, 0f, 1f)), keep = 1f - ramp(t, 0.35f, 1f);
        ground(buf, GROUND_CRACK, VfxBlend.ALPHA, c, 0.03f, R * (0.55f + 0.45f * grow), spin, a(BLACK, 0.8f * keep));
        ground(buf, GROUND_CRACK, VfxBlend.ADD, c, 0.04f, R * (0.55f + 0.45f * grow), spin, a(col, 0.95f * (1f - t) * (1f - t) + 0.1f * keep));
        float e1 = outCubic(Mth.clamp(t * 1.5f, 0f, 1f)), e2 = outCubic(Mth.clamp(t * 1.2f - 0.1f, 0f, 1f));
        ground(buf, LENS, VfxBlend.ADD, c, 0.06f, R * e1 / 0.9f, 0f, a(hot(col, 0.5f), 1f - e1));
        ground(buf, WARP_DISC, VfxBlend.ADD, c, 0.07f, R * e2 * 1.05f, spin, a(col, 0.7f * (1f - e2)));
        float flash = 1f - ramp(t, 0f, 0.25f);
        if (flash > 0f) glow(buf, ctx, new Vector3f(c).add(0, 0.5f, 0), R * (0.7f + flash), a(hot(col, 0.7f), flash));
        VfxPose ring = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float dustR = R * outCubic(Mth.clamp(t * 1.4f, 0f, 1f)), dustH = (0.35f + 0.8f * (1f - t)) * Math.min(2f, R * 0.18f + 0.5f);
        ElementFx.hoop(buf, DUST, VfxBlend.ALPHA, ring, Math.max(0.2f, dustR), dustH, ctx.seg(14, 8), 3f, 0f, a(0xFF9B8E86, 0.55f * (1f - t)));
        int n = ctx.seg(8, 4);
        for (int k = 0; k < n; k++) {                                                                             // shards thrown up and out
            float ang = hash(inst.seed, k, 6) * Mth.TWO_PI, sp = 0.5f + hash(inst.seed, k, 7);
            float d = R * 0.7f * sp * outCubic(t), h = 3.2f * sp * (age / 20f) * (1f - age / 20f * 0.9f);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * d, Math.max(0.1f, h), Mth.sin(ang) * d);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, p, 0.3f + 0.2f * hash(inst.seed, k, 8), age * 0.5f + k, a(0xFF5C5560, 1f - ramp(t, 0.6f, 1f)));
        }
    }

    // ------------------------------------------------------------------ GRAVITY_LANES
    private void lanes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), w = Math.max(0.5f, inst.power);
        boolean tell = telegraph(inst);
        float fade = ElementFx.life(inst, age, 4, Math.min(8, inst.duration / 3f));
        int col = inst.color | 0xFF000000;
        Vector3f a0 = ctx.rel(inst.from(ctx)).add(0, 0.06f, 0), b0 = ctx.rel(inst.to(ctx)).add(0, 0.06f, 0);
        float len = new Vector3f(b0).sub(a0).length();
        if (len < 0.2f) return;
        if (tell) {
            lane(buf, CHEVRON, VfxBlend.ADD, a0, b0, w, 0.5f, age * 0.06f, a(col, 0.55f * fade));
            lane(buf, CHEVRON, VfxBlend.ADD, a0, b0, w * 0.5f, 0.5f, age * 0.1f, a(hot(col, 0.5f), 0.5f * fade));
            float ph = t;                                                                                         // a bright marker runs down the lane: the strike
            Vector3f p = new Vector3f(a0).lerp(b0, ph);
            glow(buf, ctx, p.add(0, 0.2f, 0), 0.8f * w, a(WHITE, 0.8f * fade));
            return;
        }
        float flash = 1f - ramp(t, 0f, 0.5f);
        lane(buf, CHEVRON, VfxBlend.ADD, a0, b0, w * 1.1f, 0.5f, -age * 0.4f, a(hot(col, 0.4f), (0.35f + 0.65f * flash) * fade));
        lane(buf, CHEVRON, VfxBlend.ALPHA, a0, b0, w * 0.9f, 0.5f, 0f, a(BLACK, 0.35f * fade));
        int n = ctx.seg(5, 3);
        for (int k = 0; k < n; k++) {                                                                             // streaks racing down the lane
            float ph = (age * 0.12f + hash(inst.seed, k, 1)) % 1f;
            Vector3f p = new Vector3f(a0).lerp(b0, ph), q = new Vector3f(a0).lerp(b0, Math.max(0f, ph - 0.12f));
            float off = (hash(inst.seed, k, 2) - 0.5f) * w * 0.8f;
            Vector3f side = ElementFx.side(new Vector3f(b0).sub(a0).normalize()).mul(off);
            ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, q.add(side).add(0, 0.3f, 0), p.add(side).add(0, 0.3f, 0), 0.35f, a(WHITE, 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------ GRAVITY_SINGULARITY
    private void singularity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float swell = outCubic(Mth.clamp(age / 14f, 0f, 1f));
        float collapse = ramp(t, 0.88f, 1f);
        float r0 = Math.max(0.15f, R * 0.33f * swell * (1f - 0.85f * collapse) * (1f + 0.06f * Mth.sin(age * 0.7f)));
        float fade = ElementFx.life(inst, age, 4, 3);
        buf.billboard(ctx, UMBRA, VfxBlend.ALPHA, c, r0 * 2f, 0f, a(BLACK, 0.97f * fade));
        buf.billboard(ctx, LENS, VfxBlend.ADD, c, r0 * 2f / 0.62f, age * 0.03f, a(hot(col, 0.3f), 0.95f * fade));
        buf.billboard(ctx, LENS, VfxBlend.ADD, c, r0 * 3.1f / 0.62f, -age * 0.02f, a(col, (0.45f + 0.2f * Mth.sin(age * 0.3f)) * fade));
        VfxPose disc = VfxPose.facing(c, new Vector3f(0.35f, 1f, 0.2f)).spin(age * 0.09f);
        buf.plane(WARP_DISC, VfxBlend.ADD, disc, r0 * 2.6f, a(col, 0.6f * fade));
        buf.plane(WARP_DISC, VfxBlend.ADD, disc.lift(0.02f).spin(-age * 0.18f), r0 * 1.7f, a(hot(col, 0.5f), 0.7f * fade));
        int n = ctx.seg(12, 5);
        for (int k = 0; k < n; k++) {                                                                             // everything falls in
            float ph = (age * 0.035f + hash(inst.seed, k, 1)) % 1f;
            float rad = r0 + (R - r0) * (1f - ph) * (1f - ph), ang = hash(inst.seed, k, 2) * Mth.TWO_PI + ph * 3.2f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, Mth.sin(ang * 1.7f) * rad * 0.25f, Mth.sin(ang) * rad);
            glow(buf, ctx, p, 0.18f + 0.22f * ph, a(k % 3 == 0 ? WHITE : col, 0.9f * fade * ph));
        }
        if (collapse > 0.4f) glow(buf, ctx, c, R * 1.4f * collapse, a(WHITE, 0.7f * (1f - collapse) + 0.2f));
    }

    // ------------------------------------------------------------------ GRAVITY_AURA
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        float fade = ElementFx.life(inst, age, 8, Math.min(14, inst.duration / 3f));
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.35f);
        ground(buf, LENS, VfxBlend.ADD, c, 0.05f, S * 1.5f * pulse / 0.62f * 0.5f, 0f, a(hot(col, 0.3f), 0.7f * fade));
        ground(buf, WARP_DISC, VfxBlend.ADD, c, 0.06f, S * 1.8f, age * 0.07f, a(col, 0.4f * fade));
        int n = ctx.seg(10, 4);
        for (int k = 0; k < n; k++) {                                                                             // embers rising off the shoulders
            float ph = (age * 0.03f + hash(inst.seed, k, 1)) % 1f;
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI + age * 0.05f;
            float rad = S * (0.55f + 0.25f * hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, ph * S * 2.4f, Mth.sin(ang) * rad);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.25f * (1f - ph * 0.6f), age * 0.2f + k, a(k % 2 == 0 ? col : hot(col, 0.7f), 0.9f * fade * (1f - ph)));
        }
        Vector3f body = new Vector3f(c).add(0, S * 1.0f, 0);
        buf.billboard(ctx, LENS, VfxBlend.ADD, body, S * 3.2f / 0.62f * 0.5f, age * 0.02f, a(hot(col, 0.4f), 0.28f * fade * pulse));
    }
}
