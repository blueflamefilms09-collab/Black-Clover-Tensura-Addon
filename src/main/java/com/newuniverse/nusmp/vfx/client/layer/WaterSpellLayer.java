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
 *   <li>WATER_JAVELIN, Aqua Javelin (0.31): a high-pressure lance of water shot from 'from' to 'to': a dense bright core, two currents
 *       twisting round it, a spray cone peeling off the head and pressure rings left in the air behind it.</li>
 *   <li>WATER_NEST, Sea Dragon's Nest (0.31): a dome of whirling water rises over 'from', its bands turning against each other,
 *       spouts arcing over it and ripples on the ground. power = radius.</li>
 *   <li>WATER_DRESS, Valkyrie Dress (0.31): flowing water armour on the caster: a skirt of currents, a breastplate band, a crown ring
 *       over the head and droplets orbiting. Follows the caster; power = size.</li>
 * </ul>
 */
public class WaterSpellLayer extends AbstractVfxLayer {
    private static final int FOAM = 0xFFEAF8FF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WATER_DRAGON, VfxShape.WATER_CRADLE, VfxShape.WATER_BURST,
            VfxShape.WATER_JAVELIN, VfxShape.WATER_NEST, VfxShape.WATER_DRESS); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case WATER_DRAGON -> 30;
            case WATER_CRADLE -> 160;
            case WATER_JAVELIN -> 14;
            case WATER_NEST -> 200;
            case WATER_DRESS -> 600;
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
            case WATER_JAVELIN -> javelin(inst, ctx, buf);
            case WATER_NEST -> nest(inst, ctx, buf);
            case WATER_DRESS -> dress(inst, ctx, buf);
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

    // ------------------------------------------------------------------ Aqua Javelin (0.31)
    private void javelin(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float travel = VfxAnim.easeOutCubic(Mth.clamp(age / (inst.duration * 0.45f), 0, 1));
        float fade = life(inst, age, 0, inst.duration * 0.5f);
        float headD = len * travel, trail = Math.min(headD, 10f);
        Vector3f head = new Vector3f(dir).mul(headD).add(a), tail = new Vector3f(dir).mul(headD - trail).add(a);
        int col = inst.color;

        // the dense core
        buf.beam(ctx, WATER_FLOW, VfxBlend.WATER, tail, head, 0.2f * p, 0.7f * p, 4, -age * 0.5f,
                VfxVertexBuffer.withAlpha(col, 0.4f * fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tail, head, 0.1f * p, 0.35f * p, 2, 0,
                VfxVertexBuffer.withAlpha(FOAM, 0.2f * fade), VfxVertexBuffer.withAlpha(FOAM, 0.8f * fade));
        // two currents twisting round it
        int segs = ctx.seg(10, 5);
        for (int strand = 0; strand < 2; strand++) {
            Vector3f prev = null;
            for (int k = 0; k <= segs; k++) {
                float f = (float) k / segs, phi = f * trail * 1.4f - age * 1.2f + strand * Mth.PI, rad = 0.4f * p * (0.4f + 0.6f * f);
                Vector3f q = new Vector3f(tail).lerp(head, f).add(new Vector3f(sd).mul(Mth.cos(phi) * rad)).add(new Vector3f(up).mul(Mth.sin(phi) * rad));
                if (prev != null) {
                    int c0 = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), (0.3f + 0.7f * f) * fade);
                    buf.beam(ctx, WATER_FLOW, VfxBlend.WATER, prev, q, 0.22f * p, 0.22f * p, 1, -age * 0.2f, c0, c0);
                }
                prev = q;
            }
        }
        // the spearhead and the spray cone peeling off it
        Vector3f tip = new Vector3f(head).add(new Vector3f(dir).mul(0.9f * p)), back = new Vector3f(head).sub(new Vector3f(dir).mul(0.9f * p));
        buf.beam(ctx, WATER_DROP, VfxBlend.WATER, back, tip, 0.9f * p, 0.2f * p, 1, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        hoop(buf, WATER_BAND, VfxBlend.WATER, VfxPose.facing(new Vector3f(head).sub(new Vector3f(dir).mul(0.8f * p)), dir).spin(age * 0.5f),
                0.25f * p, 0.9f * p, 0.45f * p, ctx.seg(12, 8), 2, age * 0.1f, VfxVertexBuffer.withAlpha(FOAM, 0.7f * fade));
        VfxBloom.glow(ctx, buf, head, 0.7f * p, col, fade);
        // pressure rings left in the air where it passed
        for (int k = 0; k < 3; k++) {
            float d = (k + 1) / 4f * len;
            if (d > headD) continue;
            float since = (headD - d) / Math.max(1, len) * 3f;
            float rr = (0.5f + 1.4f * Math.min(1, since)) * p;
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.facing(new Vector3f(dir).mul(d).add(a), dir), rr * 0.85f, rr, ctx.seg(12, 8), 1, 0,
                    VfxVertexBuffer.withAlpha(FOAM, 0.6f * fade * Mth.clamp(1 - since, 0, 1)));
        }
        // droplets shed
        int s = ctx.seg(10, 5);
        for (int i = 0; i < s; i++) {
            float f = hash(inst.seed, i, 1);
            if (len * f > headD) continue;
            float dt = (headD - len * f) / 6f;
            Vector3f q = new Vector3f(dir).mul(len * f).add(a).add(new Vector3f(sd).mul((hash(inst.seed, i, 2) - 0.5f) * 2 * dt * p)).add(0, -dt * dt * p, 0);
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, q, 0.2f * p, 0, VfxVertexBuffer.withAlpha(WHITE, fade * Mth.clamp(1 - dt * 0.5f, 0, 1)));
        }
    }

    // ------------------------------------------------------------------ Sea Dragon's Nest (0.31)
    private void nest(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));
        float fade = life(inst, age, 0, 16);
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color;
        // the dome: four latitude bands, each a slanted ribbon, rising from the ground and turning against each other
        int bands = 4;
        float top = rise * Mth.HALF_PI * 0.92f;
        for (int k = 0; k < bands; k++) {
            float p0 = top * k / bands, p1 = top * (k + 1) / bands, pm = (p0 + p1) * 0.5f;
            float rb = r * Mth.cos(p0), rt = r * Mth.cos(p1), hw = r * (Mth.sin(p1) - Mth.sin(p0)) * 0.5f;
            VfxPose pose = VfxPose.ground(new Vector3f(g).add(0, r * Mth.sin(pm), 0));
            hoop(buf, WATER_BAND, VfxBlend.WATER, pose, rb, rt, hw * 1.08f, ctx.seg(12, 8), 3, age * 0.02f * (k % 2 == 0 ? 1 : -1),
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.35f + 0.1f * k), (0.7f + 0.08f * k) * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, r * 0.95f * rise, 0), r * 0.35f, col, 0.4f * fade);
        // spouts arcing up over the dome from the rim
        int sp = 3, segs = 4;
        for (int i = 0; i < sp; i++) {
            float a0 = Mth.TWO_PI * i / sp + age * 0.015f;
            float lt = ((age / 30f) + i / (float) sp) % 1f;
            Vector3f prev = null;
            for (int k = 0; k <= segs; k++) {
                float f = (float) k / segs * lt;
                float ang = a0 + f * Mth.PI * 0.8f, h = Mth.sin(f * Mth.PI) * r * 1.25f;
                float rad = r * (1.05f - 0.5f * Mth.sin(f * Mth.PI));
                Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, h, Mth.sin(ang) * rad);
                if (prev != null) {
                    int c0 = VfxVertexBuffer.withAlpha(WHITE, fade * (1 - lt * 0.6f));
                    buf.beam(ctx, WATER_FLOW, VfxBlend.WATER, prev, q, 0.5f, 0.5f, 1, -age * 0.15f, c0, c0);
                }
                prev = q;
            }
        }
        // drops falling off the bands
        int d = ctx.seg(8, 4);
        for (int i = 0; i < d; i++) {
            float ph = hash(inst.seed, i, 1) * 16f, lt = ((age + ph) % 16f) / 16f;
            float ang = hash(inst.seed, i, 2) * Mth.TWO_PI, h = r * rise * (0.3f + 0.6f * hash(inst.seed, i, 3));
            float rad = Mth.sqrt(Math.max(0, r * r - h * h)) * 1.02f;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, h - lt * lt * h, Mth.sin(ang) * rad);
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, q, 0.3f, 0, VfxVertexBuffer.withAlpha(WHITE, fade * (1 - lt)));
        }
        // ripples round its foot
        float lt = (age / 30f) % 1f, rr = r * (1.0f + 0.5f * lt);
        buf.ring(WATER_BAND, VfxBlend.WATER, VfxPose.ground(new Vector3f(g).add(0, 0.06f, 0)), rr * 0.9f, rr, ctx.seg(12, 8), 3, age * 0.02f,
                VfxVertexBuffer.withAlpha(FOAM, (1 - lt) * 0.6f * fade));
    }

    // ------------------------------------------------------------------ Valkyrie Dress (0.31)
    private void dress(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float form = VfxAnim.easeOutBack(Mth.clamp(age / 12f, 0, 1));
        float fade = life(inst, age, 0, 12);
        if (form <= 0.01f) return;
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.45f);
        // the skirt: two layers of currents flowing down from the waist and flaring out
        for (int k = 0; k < 2; k++) {
            float yTop = 1.05f - k * 0.18f, yBot = 0.25f - k * 0.1f, mid = (yTop + yBot) * 0.5f;
            float flare = (0.62f + 0.12f * k + 0.04f * Mth.sin(age * 0.15f + k)) * p * form;
            hoop(buf, WATER_FLOW, VfxBlend.WATER, VfxPose.ground(new Vector3f(g).add(0, mid, 0)), flare, 0.36f * p * form, (yTop - yBot) * 0.5f,
                    ctx.seg(12, 8), 2, age * 0.03f * (k == 0 ? 1 : -1), VfxVertexBuffer.withAlpha(pale, (0.65f - 0.15f * k) * fade));
        }
        // the breastplate band and the belt
        hoop(buf, WATER_BAND, VfxBlend.WATER, VfxPose.ground(new Vector3f(g).add(0, 1.2f, 0)), 0.36f * p * form, 0.33f * p * form, 0.14f,
                ctx.seg(10, 8), 2, -age * 0.04f, VfxVertexBuffer.withAlpha(WHITE, 0.8f * fade));
        hoop(buf, WATER_BAND, VfxBlend.WATER, VfxPose.ground(new Vector3f(g).add(0, 1.04f, 0)), 0.38f * p * form, 0.38f * p * form, 0.05f,
                ctx.seg(10, 8), 2, age * 0.06f, VfxVertexBuffer.withAlpha(FOAM, 0.9f * fade));
        // the crown: a ring of water over the head, with a glow
        Vector3f crown = new Vector3f(g).add(0, 2.05f * p, 0);
        buf.ring(WATER_BAND, VfxBlend.WATER, VfxPose.ground(crown).spin(age * 0.05f), 0.24f * p * form, 0.34f * p * form, ctx.seg(12, 8), 2, age * 0.05f,
                VfxVertexBuffer.withAlpha(WHITE, 0.85f * fade));
        VfxBloom.glow(ctx, buf, crown, 0.35f * p, col, 0.5f * fade);
        // droplets orbiting like the dress's lace
        int d = ctx.seg(10, 5);
        for (int i = 0; i < d; i++) {
            float ang = Mth.TWO_PI * i / d + age * 0.06f, h = 0.4f + 1.2f * hash(inst.seed, i, 1) + 0.1f * Mth.sin(age * 0.1f + i);
            float rad = (0.7f + 0.15f * Mth.sin(age * 0.08f + i * 2)) * p * form;
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, new Vector3f(g).add(Mth.cos(ang) * rad, h, Mth.sin(ang) * rad), 0.18f * p, 0,
                    VfxVertexBuffer.withAlpha(WHITE, 0.9f * fade));
        }
        // water dripping from the hem
        int s = ctx.seg(6, 3);
        for (int i = 0; i < s; i++) {
            float ph = hash(inst.seed, i, 2) * 12f, lt = ((age + ph) % 12f) / 12f, ang = hash(inst.seed, i, 3) * Mth.TWO_PI;
            float rad = 0.7f * p * form;
            buf.billboard(ctx, WATER_DROP, VfxBlend.WATER, new Vector3f(g).add(Mth.cos(ang) * rad, 0.15f - 0.3f * lt * lt, Mth.sin(ang) * rad), 0.14f * p, 0,
                    VfxVertexBuffer.withAlpha(WHITE, fade * (1 - lt)));
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
