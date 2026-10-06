package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Water Magic (Noelle Silva, Undine), drawn after the anime:
 * <ul>
 *   <li>WATER_DRAGON, Sea Dragon's Roar: a dragon of water surges from 'from' to 'to', its sinuous flowing body whipping behind its
 *       head, foam ringing its jaws and spray peeling off. Duration = flight time.</li>
 *   <li>WATER_CRADLE, Sea Dragon's Cradle: an enormous smooth sphere of whirling water around the caster, two currents winding round
 *       it and a ring of watery globs orbiting it. power = radius.</li>
 *   <li>WATER_BURST: the splash: a crown of water thrown up and out, droplets, ripples spreading over the ground.</li>
 * </ul>
 */
public class WaterSpellLayer extends AbstractVfxLayer {
    private static final int FOAM = 0xFFEAF8FF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WATER_DRAGON, VfxShape.WATER_CRADLE, VfxShape.WATER_BURST); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case WATER_DRAGON -> 30;
            case WATER_CRADLE -> 160;
            default -> 24;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF6EC3FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.WATER_BURST && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 0.9f * inst.power, 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case WATER_DRAGON -> dragon(inst, ctx, buf);
            case WATER_CRADLE -> cradle(inst, ctx, buf);
            case WATER_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Sea Dragon's Roar
    private void dragon(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float fly = Math.max(1, inst.duration - 6);
        float travel = VfxAnim.easeInOutSine(Mth.clamp(age / fly, 0, 1));
        float fade = life(inst, age, 2, 6);
        float headD = len * travel;
        int col = inst.color;

        // the body: points trailing behind the head, swinging more toward the tail
        int n = ctx.seg(12, 6);
        float segLen = 0.6f * p;
        Vector3f[] pts = new Vector3f[n + 1];
        float[] width = new float[n + 1];
        for (int k = 0; k <= n; k++) {
            float d = Math.max(0, headD - k * segLen);
            float f = (float) k / n;
            float wave = d * 0.9f - age * 0.5f;
            pts[k] = new Vector3f(dir).mul(d).add(a)
                    .add(new Vector3f(sd).mul(Mth.sin(wave) * 0.6f * p * f))
                    .add(new Vector3f(up).mul(Mth.cos(wave * 0.8f) * 0.35f * p * f));
            width[k] = 1.7f * p * (1 - f * 0.8f);
        }
        for (int k = 0; k < n; k++) {
            float f = (float) k / n;
            int c0 = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), fade * (1 - f * 0.5f));
            buf.beam(ctx, WATER_FLOW, VfxBlend.WATER, pts[k + 1], pts[k], width[k + 1], width[k], 1, -age * 0.12f + k * 0.35f, c0, c0);
        }
        for (int k = 0; k < n; k++) {
            int c0 = VfxVertexBuffer.withAlpha(FOAM, 0.45f * fade * (1 - (float) k / n));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, pts[k + 1], pts[k], width[k + 1] * 0.35f, width[k] * 0.35f, 1, 0, c0, c0);
        }
        // the head: snout first along the flight path
        Vector3f head = pts[0];
        Vector3f back = new Vector3f(head).sub(new Vector3f(dir).mul(2.0f * p)), front = new Vector3f(head).add(new Vector3f(dir).mul(0.9f * p));
        VfxBloom.glow(ctx, buf, head, 0.9f * p, col, 0.6f * fade);
        buf.beam(ctx, WATER_DRAGON, VfxBlend.ALPHA, back, front, 2.4f * p, 2.4f * p, 1, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        VfxPose collar = VfxPose.facing(new Vector3f(head).sub(new Vector3f(dir).mul(1.6f * p)), dir).spin(age * 0.3f);   // foam swirling round the neck
        buf.ring(WATER_BAND, VfxBlend.WATER, collar, 0.75f * p, 1.1f * p, ctx.seg(12, 8), 2, age * 0.05f, VfxVertexBuffer.withAlpha(FOAM, 0.55f * fade));
        // spray peeling off the body and falling
        int s = ctx.seg(16, 8);
        for (int i = 0; i < s; i++) {
            float ph = hash(inst.seed, i, 1) * 10f, life = ((age + ph) % 10f) / 10f;
            int k = Math.min(n, (int) (hash(inst.seed, i, 2) * n));
            Vector3f out = new Vector3f(sd).mul((hash(inst.seed, i, 3) - 0.5f) * 2f).add(new Vector3f(up).mul(hash(inst.seed, i, 4)));
            Vector3f q = new Vector3f(pts[k]).add(out.mul(1.6f * p * life)).add(0, -1.5f * p * life * life, 0);
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, q, 0.28f * p * (1 - life * 0.5f), 0, VfxVertexBuffer.withAlpha(WHITE, fade * (1 - life)));
        }
    }

    // ------------------------------------------------------------------ Sea Dragon's Cradle
    private void cradle(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        float r = inst.power * open;
        if (r <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color;

        buf.billboard(ctx, WATER_SPHERE, VfxBlend.WATER, c, r * 2f, age * 0.02f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.85f * fade));
        VfxBloom.glow(ctx, buf, c, r * 0.4f, col, 0.35f * fade);
        // two currents winding round the sphere in opposite directions
        for (int k = 0; k < 2; k++) {
            float tilt = k == 0 ? 0.35f : -0.6f, yaw = k * 1.7f + age * 0.01f * (k == 0 ? 1 : -1);
            Vector3f axis = new Vector3f(Mth.sin(tilt) * Mth.cos(yaw), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(yaw));
            hoop(buf, WATER_BAND, VfxBlend.WATER, VfxPose.facing(c, axis), r * 1.03f, r * 0.17f, ctx.seg(16, 10), 3,
                    age * 0.025f * (k == 0 ? 1 : -1), VfxVertexBuffer.withAlpha(WHITE, 0.85f * fade));
        }
        // the ring of watery globs orbiting it
        int g = ctx.seg(10, 6);
        for (int i = 0; i < g; i++) {
            float ang = Mth.TWO_PI * i / g + age * 0.03f;
            float bob = Mth.sin(age * 0.12f + i * 1.3f) * 0.12f * r;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * r * 1.32f, bob - 0.1f * r, Mth.sin(ang) * r * 1.32f);
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, q, r * (0.26f + 0.06f * Mth.sin(age * 0.2f + i)), 0, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        // ripples spreading on the ground under it
        float groundY = -1f;                       // the sphere is centred one block above the caster's feet
        for (int k = 0; k < 2; k++) {
            float lt = ((age / 40f) + k * 0.5f) % 1f;
            float rr = r * (0.6f + 1.2f * lt);
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, groundY + 0.06f, 0)), rr * 0.94f, rr, ctx.seg(16, 10), 1, 0,
                    VfxVertexBuffer.withAlpha(FOAM, (1 - lt) * 0.6f * fade));
        }
    }

    // ------------------------------------------------------------------ splash
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float t = Mth.clamp(age / inst.duration, 0, 1), e = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float fade = 1 - VfxAnim.easeInCubic(t);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f floor = new Vector3f(c).add(0, -0.5f * p, 0);
        int col = inst.color;
        VfxBloom.glow(ctx, buf, c, 0.9f * p, col, fade * (1 - t));
        buf.plane(VfxTextures.WATER_SPLASH, VfxBlend.WATER, VfxPose.ground(new Vector3f(floor).add(0, 0.04f, 0)).spin(age * 0.04f), 2.2f * p * e,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.3f), fade));
        // the crown: a wall of water thrown up and outward, collapsing
        float cr = (0.6f + 1.8f * e) * p, ch = 1.1f * p * e * (1 - t * 0.8f);
        hoop(buf, WATER_BAND, VfxBlend.WATER, VfxPose.ground(new Vector3f(floor).add(0, ch, 0)), cr * 0.85f, cr * 1.15f, ch, ctx.seg(16, 10), 3, age * 0.02f,
                VfxVertexBuffer.withAlpha(WHITE, fade));
        // droplets in arcs
        int d = ctx.seg(18, 8);
        for (int i = 0; i < d; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, sp = 0.6f + hash(inst.seed, i, 2);
            float dist = 2.6f * p * sp * e;
            Vector3f q = new Vector3f(floor).add(Mth.cos(ang) * dist, (2.4f * sp * t - 3.6f * t * t) * p + 0.3f, Mth.sin(ang) * dist);
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, q, 0.26f * p, 0, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        // ripples
        for (int k = 0; k < 2; k++) {
            float lt = Mth.clamp(t * 1.4f - k * 0.25f, 0, 1);
            if (lt <= 0) continue;
            float rr = (0.8f + 3.6f * VfxAnim.easeOutCubic(lt)) * p;
            VfxPose pose = VfxPose.ground(new Vector3f(floor).add(0, 0.05f + k * 0.02f, 0));
            buf.ring(WATER_BAND, VfxBlend.WATER, pose, rr * 0.92f, rr, ctx.seg(16, 10), 3, age * 0.02f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), (1 - lt) * 0.6f));
            if (k == 0) buf.ring(VfxTextures.GLOW, VfxBlend.ADD, pose, rr * 0.97f, rr * 1.02f, ctx.seg(16, 10), 1, 0, VfxVertexBuffer.withAlpha(FOAM, (1 - lt) * 0.6f));
        }
    }
}
