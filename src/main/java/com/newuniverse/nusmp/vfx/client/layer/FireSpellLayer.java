package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Fire Magic (Fuegoleon / Mereoleona Vermillion, Salamander), drawn after the anime:
 * <ul>
 *   <li>FIRE_LION, Leo Rugiens: a lion of flame bounds from 'from' to 'to', a blazing mane round its roaring head and a body of
 *       fire streaming behind, embers flying off. power = size.</li>
 *   <li>FIRE_SPEAR, Sol Linea: a spear of flame whose head races ahead while three fire strands spiral round its white-hot core.</li>
 *   <li>FIRE_PILLAR, Ignis Columna / Calderos: a column of fire bursts out of a scorched ring and roars upward. power = radius.</li>
 *   <li>FIRE_BURST: flash, shock ring, a crown of flame, flying tongues, embers and a wisp of smoke.</li>
 *   <li>FIRE_SPIRAL, Spiral Flame (0.31): a vortex of flame drilling from 'from' to 'to': three strands corkscrewing round a white-hot
 *       core, widening toward the head, where a ring of fire spins like a drill bit.</li>
 *   <li>FIRE_WILD, Wild Bursting Flame (0.31): flame bursts out of the caster in every direction, three waves one after another,
 *       each throwing a ring of tongues outward over a spreading band of fire. power = radius.</li>
 * </ul>
 * Flame textures carry their own white-yellow / orange / red colours; the effect colour tints the glows and rings.
 */
public class FireSpellLayer extends AbstractVfxLayer {
    private static final int HOT = 0xFFFFF4C8, EMBER = 0xFFFF9A3C, RED = 0xFFE5401A, SMOKE = 0x90403430;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.FIRE_LION, VfxShape.FIRE_SPEAR, VfxShape.FIRE_PILLAR, VfxShape.FIRE_BURST,
            VfxShape.FIRE_SPIRAL, VfxShape.FIRE_WILD); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case FIRE_LION -> 34;
            case FIRE_SPEAR -> 16;
            case FIRE_PILLAR -> 30;
            case FIRE_SPIRAL -> 22;
            case FIRE_WILD -> 30;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF8A2A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.FIRE_LION) VfxShake.add(inst.payload.to(), 1.2f * inst.power, 16);
        else if (inst.shape == VfxShape.FIRE_PILLAR && inst.power >= 1.5f) VfxShake.add(inst.payload.from(), 0.8f, 10);
        else if (inst.shape == VfxShape.FIRE_WILD) VfxShake.add(inst.payload.from(), 0.5f * inst.power, 14);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case FIRE_LION -> lion(inst, ctx, buf);
            case FIRE_SPEAR -> spear(inst, ctx, buf);
            case FIRE_PILLAR -> pillar(inst, ctx, buf);
            case FIRE_BURST -> burst(inst, ctx, buf, ctx.rel(inst.from(ctx)), inst.power, inst.ageTicks(ctx.partialTick), inst.duration);
            case FIRE_SPIRAL -> spiral(inst, ctx, buf);
            case FIRE_WILD -> wild(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Leo Rugiens
    private void lion(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len);
        float charge = inst.duration * 0.65f;
        float travel = VfxAnim.easeOutCubic(Mth.clamp(age / charge, 0, 1));
        float arrived = Mth.clamp((age - charge) / (inst.duration - charge), 0, 1);       // 0 while charging, then 0 -> 1
        float fade = life(inst, age, 3, 8);
        Vector3f head = new Vector3f(dir).mul(len * travel).add(a).add(0, 0.6f * p * Mth.sin(travel * Mth.PI), 0);   // the leap
        float headSize = 3.4f * p * (1 + 0.5f * arrived) * Mth.clamp(age / 4f, 0, 1);

        // the body: a stream of fire behind the head, wide at the shoulders
        float bodyLen = Math.min(len * travel, 5.5f * p);
        Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(bodyLen)).sub(0, 0.3f * p, 0);
        buf.beam(ctx, FIRE_RIBBON, VfxBlend.FIRE, tail, head, 0.6f * p, 2.8f * p, 6, -age * 0.2f,
                VfxVertexBuffer.withAlpha(RED, 0.5f * fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tail, head, 0.4f * p, 1.8f * p, 3, 0,
                VfxVertexBuffer.withAlpha(EMBER, 0.3f * fade), VfxVertexBuffer.withAlpha(HOT, 0.7f * fade));

        // flames licking up off the body
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float s = hash(inst.seed, i, 1), ph = hash(inst.seed, i, 2) * 8;
            float life = ((age + ph) % 8f) / 8f;
            Vector3f q = new Vector3f(tail).lerp(head, s).add(side(dir).mul((hash(inst.seed, i, 3) - 0.5f) * 1.2f * p)).add(0, life * 1.2f * p, 0);
            float sz = (1.2f + 0.8f * s) * p * (1 - life * 0.6f);
            pointed(buf, ctx, FIRE_TONGUE, VfxBlend.FIRE, q, sz, Mth.HALF_PI + (hash(inst.seed, i, 4) - 0.5f) * 0.6f,
                    VfxVertexBuffer.withAlpha(WHITE, fade * (1 - life)));
        }

        // the mane: tongues of flame radiating round the head, flickering
        int m = ctx.seg(16, 8);
        for (int i = 0; i < m; i++) {
            float ang = Mth.TWO_PI * i / m + Mth.sin(age * 0.2f + i) * 0.08f;
            float flick = 0.8f + 0.3f * Mth.sin(age * 0.9f + i * 1.7f);
            Vector3f q = new Vector3f(head)
                    .add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * headSize * 0.52f))
                    .add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * headSize * 0.52f));
            // the mane sits on the rim and stays orange so the face stays readable
            pointed(buf, ctx, FIRE_TONGUE, VfxBlend.FIRE, q, headSize * 0.32f * flick, ang, VfxVertexBuffer.withAlpha(EMBER, 0.75f * fade));
        }

        // the head itself, swelling and fading as it hits
        VfxBloom.glow(ctx, buf, head, headSize * 0.3f, inst.color, 0.6f * fade);
        buf.billboard(ctx, FIRE_LION, VfxBlend.ALPHA, head, headSize, Mth.sin(age * 0.15f) * 0.05f,
                VfxVertexBuffer.withAlpha(WHITE, fade * (1 - arrived * 0.8f)));

        // embers thrown off as it runs
        int e = ctx.seg(12, 6);
        for (int i = 0; i < e; i++) {
            float ph = hash(inst.seed, i, 5) * 12f, life = ((age + ph) % 12f) / 12f;
            Vector3f q = new Vector3f(tail).lerp(head, hash(inst.seed, i, 6))
                    .add(side(dir).mul((hash(inst.seed, i, 7) - 0.5f) * 2.4f * p * life)).add(0, life * 1.8f * p, 0);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.22f * p * (1 - life), age * 0.3f + i,
                    VfxVertexBuffer.withAlpha(EMBER, fade * (1 - life)));
        }
        // the roar's shock ring where it lands
        if (arrived > 0) {
            float r = (0.6f + 3.4f * VfxAnim.easeOutCubic(arrived)) * p;
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.facing(head, dir), r * 0.8f, r, ctx.seg(16, 10), 1, 0,
                    VfxVertexBuffer.withAlpha(HOT, (1 - arrived) * fade));
        }
    }

    // ------------------------------------------------------------------ Sol Linea
    private void spear(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float travel = VfxAnim.easeOutCubic(Mth.clamp(age / (inst.duration * 0.55f), 0, 1));
        float fade = life(inst, age, 0, inst.duration * 0.45f);
        float headD = len * travel, trail = Math.min(headD, 9f);
        Vector3f head = new Vector3f(dir).mul(headD).add(a), tail = new Vector3f(dir).mul(headD - trail).add(a);

        // white-hot core
        buf.beam(ctx, FIRE_RIBBON, VfxBlend.FIRE, tail, head, 0.2f * p, 0.8f * p, 4, -age * 0.3f,
                VfxVertexBuffer.withAlpha(RED, 0.4f * fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        // three strands of fire spiralling round it
        int segs = ctx.seg(10, 5);
        for (int strand = 0; strand < 3; strand++) {
            Vector3f prev = null;
            for (int k = 0; k <= segs; k++) {
                float s = (float) k / segs;
                float phi = s * trail * 1.6f - age * 0.9f + strand * Mth.TWO_PI / 3;
                float rad = 0.55f * p * (0.35f + 0.65f * s);
                Vector3f q = new Vector3f(tail).lerp(head, s)
                        .add(new Vector3f(sd).mul(Mth.cos(phi) * rad)).add(new Vector3f(up).mul(Mth.sin(phi) * rad));
                if (prev != null) {
                    int c0 = VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(RED, EMBER, s), (s * 0.8f + 0.2f) * fade);
                    buf.beam(ctx, FIRE_RIBBON, VfxBlend.FIRE, prev, q, 0.3f * p, 0.3f * p, 1, 0, c0, c0);
                }
                prev = q;
            }
        }
        // the spearhead: a flame tongue pointing forward, tip first
        Vector3f tip = new Vector3f(head).add(new Vector3f(dir).mul(0.8f * p)), back = new Vector3f(head).sub(new Vector3f(dir).mul(2.0f * p));
        buf.beam(ctx, FIRE_TONGUE, VfxBlend.FIRE, tip, back, 1.4f * p, 1.4f * p, 1, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        VfxBloom.glow(ctx, buf, head, 0.8f * p, inst.color, fade);
        // embers shed from the trail
        int e = ctx.seg(12, 6);
        for (int i = 0; i < e; i++) {
            float s = hash(inst.seed, i, 1), drift = (age - s * inst.duration * 0.55f) * 0.08f;
            if (drift < 0) continue;
            Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * s))
                    .add(new Vector3f(sd).mul((hash(inst.seed, i, 2) - 0.5f) * drift * 3)).add(0, drift * 2, 0);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.16f * p, age * 0.4f, VfxVertexBuffer.withAlpha(EMBER, fade * Mth.clamp(1.5f - drift, 0, 1)));
        }
    }

    // ------------------------------------------------------------------ Ignis Columna / Calderos
    private void pillar(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float rise = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1));
        float fade = life(inst, age, 0, 10);
        float height = 4.2f * p * rise, r = 0.85f * p;
        Vector3f g = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0));

        VfxBloom.planeGlow(buf, ground, r * 1.1f, inst.color, 0.8f * fade);
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground, r * 1.1f, r * 1.45f, ctx.seg(16, 10), 1, 0, VfxVertexBuffer.withAlpha(inst.color, 0.8f * fade));
        // a ring of fire round the base, and the column itself: tall flame tongues reaching up, flickering against each other
        hoop(buf, FIRE_BAND, VfxBlend.FIRE, VfxPose.ground(new Vector3f(g).add(0, height * 0.14f, 0)), r * 1.05f, r * 0.85f, height * 0.16f,
                ctx.seg(12, 8), 3, age * 0.03f, VfxVertexBuffer.withAlpha(WHITE, fade));
        for (int k = 0; k < 4; k++) {
            float ang = Mth.TWO_PI * k / 4 + age * 0.05f;
            float flick = 0.85f + 0.15f * Mth.sin(age * 0.8f + k * 2.1f);
            Vector3f base = new Vector3f(g).add(Mth.cos(ang) * r * 0.25f, -0.2f, Mth.sin(ang) * r * 0.25f);
            Vector3f top = new Vector3f(base).add(Mth.sin(age * 0.3f + k) * 0.25f * p, height * flick * (k == 0 ? 1.1f : 0.85f), 0);
            buf.beam(ctx, FIRE_TONGUE, VfxBlend.FIRE, top, base, r * 2.0f, r * 2.0f, 1, 0,
                    VfxVertexBuffer.withAlpha(WHITE, fade * (k == 0 ? 1f : 0.7f)), VfxVertexBuffer.withAlpha(WHITE, fade * (k == 0 ? 1f : 0.7f)));
        }
        // loose tongues climbing the column
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.08f, ph = hash(inst.seed, i, 2) * 10f;
            float life = ((age + ph) % 10f) / 10f;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * r * 0.8f, height * (0.2f + 0.8f * life), Mth.sin(ang) * r * 0.8f);
            pointed(buf, ctx, FIRE_TONGUE, VfxBlend.FIRE, q, r * 1.3f * (1 - life * 0.5f), Mth.HALF_PI, VfxVertexBuffer.withAlpha(WHITE, fade * (1 - life)));
        }
        // embers spiralling up out of the top
        int e = ctx.seg(14, 6);
        for (int i = 0; i < e; i++) {
            float ph = hash(inst.seed, i, 3) * 16f, life = ((age + ph) % 16f) / 16f;
            float ang = hash(inst.seed, i, 4) * Mth.TWO_PI + life * 3f;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * r * (0.6f + life), height * (0.5f + life * 0.9f), Mth.sin(ang) * r * (0.6f + life));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p * (1 - life), age * 0.3f, VfxVertexBuffer.withAlpha(EMBER, fade * (1 - life)));
        }
    }

    // ------------------------------------------------------------------ Spiral Flame (0.31)
    private void spiral(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float travel = VfxAnim.easeOutCubic(Mth.clamp(age / (inst.duration * 0.6f), 0, 1));
        float fade = life(inst, age, 0, inst.duration * 0.4f);
        float headD = len * travel;
        Vector3f head = new Vector3f(dir).mul(headD).add(a);

        // the white-hot core, thin at the caster and thick at the head
        buf.beam(ctx, FIRE_RIBBON, VfxBlend.FIRE, a, head, 0.15f * p, 0.9f * p, 4, -age * 0.35f,
                VfxVertexBuffer.withAlpha(RED, 0.35f * fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        // three strands corkscrewing round it, the vortex widening toward the head
        int segs = ctx.seg(12, 6);
        for (int strand = 0; strand < 3; strand++) {
            Vector3f prev = null;
            for (int k = 0; k <= segs; k++) {
                float f = (float) k / segs, d = headD * f;
                float phi = f * 9f - age * 0.9f + strand * Mth.TWO_PI / 3;
                float rad = (0.25f + 1.15f * f * f) * p;
                Vector3f q = new Vector3f(dir).mul(d).add(a).add(new Vector3f(sd).mul(Mth.cos(phi) * rad)).add(new Vector3f(up).mul(Mth.sin(phi) * rad));
                if (prev != null) {
                    int c0 = VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(RED, HOT, f), (0.3f + 0.7f * f) * fade);
                    buf.beam(ctx, FIRE_RIBBON, VfxBlend.FIRE, prev, q, (0.25f + 0.35f * f) * p, (0.25f + 0.35f * f) * p, 1, 0, c0, c0);
                }
                prev = q;
            }
        }
        // the drill bit: a spinning ring of fire at the head, and a flame tongue leading it
        VfxPose bit = VfxPose.facing(head, dir).spin(age * 0.6f);
        hoop(buf, FIRE_BAND, VfxBlend.FIRE, bit, 1.3f * p, 0.5f * p, 0.45f * p, ctx.seg(12, 8), 3, age * 0.08f, VfxVertexBuffer.withAlpha(WHITE, fade));
        Vector3f tip = new Vector3f(head).add(new Vector3f(dir).mul(1.2f * p)), back = new Vector3f(head).sub(new Vector3f(dir).mul(1.6f * p));
        buf.beam(ctx, FIRE_TONGUE, VfxBlend.FIRE, tip, back, 1.8f * p, 1.8f * p, 1, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
        VfxBloom.glow(ctx, buf, head, 1.1f * p, inst.color, fade);
        // embers flung off the vortex
        int e = ctx.seg(12, 6);
        for (int i = 0; i < e; i++) {
            float f = hash(inst.seed, i, 1), drift = Math.max(0, travel - f) * 3f;
            if (f > travel) continue;
            float ang = hash(inst.seed, i, 2) * Mth.TWO_PI + age * 0.2f, rad = (0.4f + 1.2f * f + drift) * p;
            Vector3f q = new Vector3f(dir).mul(len * f).add(a).add(new Vector3f(sd).mul(Mth.cos(ang) * rad)).add(new Vector3f(up).mul(Mth.sin(ang) * rad)).add(0, drift * 0.5f, 0);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p, age * 0.4f + i, VfxVertexBuffer.withAlpha(EMBER, fade * Mth.clamp(1.4f - drift * 0.5f, 0, 1)));
        }
    }

    // ------------------------------------------------------------------ Wild Bursting Flame (0.31)
    private void wild(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float fade = life(inst, age, 0, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 1, 0), 1.4f, inst.color, fade * (0.6f + 0.4f * Mth.sin(age * 0.8f)));
        // three waves of tongues thrown outward, 6 ticks apart
        int n = ctx.seg(9, 5);
        for (int w = 0; w < 3; w++) {
            float local = age - w * 6f, t = Mth.clamp(local / 14f, 0, 1);
            if (local < 0 || t >= 1) continue;
            float e = VfxAnim.easeOutCubic(t), wf = 1 - t;
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * (i + 0.5f * w) / n + (hash(inst.seed, i + w * 31, 1) - 0.5f) * 0.4f;
                float lift = (hash(inst.seed, i + w * 31, 2) - 0.3f) * 0.9f;
                Vector3f out = new Vector3f(Mth.cos(ang), lift, Mth.sin(ang)).normalize();
                Vector3f base = new Vector3f(c).add(0, 1, 0).add(new Vector3f(out).mul(r * 0.9f * e));
                Vector3f tip = new Vector3f(base).add(new Vector3f(out).mul((0.8f + 1.4f * wf) * Math.min(r, 3f)));
                buf.beam(ctx, FIRE_TONGUE, VfxBlend.FIRE, tip, base, 1.1f * Math.min(r, 3f) * (0.6f + 0.4f * wf), 1.1f * Math.min(r, 3f) * (0.6f + 0.4f * wf), 1, 0,
                        VfxVertexBuffer.withAlpha(WHITE, fade * wf), VfxVertexBuffer.withAlpha(WHITE, fade * wf));
            }
            // the band of fire spreading over the ground with the wave, and its shock ring
            float rr = r * (0.3f + 0.9f * e);
            hoop(buf, FIRE_BAND, VfxBlend.FIRE, ground.lift(0.35f * wf), rr * 0.92f, rr, 0.4f * wf + 0.05f, ctx.seg(12, 8), 3, age * 0.03f,
                    VfxVertexBuffer.withAlpha(WHITE, fade * wf));
            if (w == 0) buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground, rr * 1.05f, rr * 1.2f, ctx.seg(16, 10), 1, 0, VfxVertexBuffer.withAlpha(HOT, fade * wf));
        }
        // embers raining out of it
        int k = ctx.seg(12, 6);
        for (int i = 0; i < k; i++) {
            float ph = hash(inst.seed, i, 4) * 14f, lt = ((age + ph) % 14f) / 14f;
            float ang = hash(inst.seed, i, 5) * Mth.TWO_PI, d = r * (0.3f + 1.0f * lt);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, 1 + 2.2f * lt - 2.6f * lt * lt, Mth.sin(ang) * d);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.22f, age * 0.4f + i, VfxVertexBuffer.withAlpha(EMBER, fade * (1 - lt)));
        }
    }

    // ------------------------------------------------------------------ explosion
    static void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float p, float age, int duration) {
        float t = Mth.clamp(age / duration, 0, 1), e = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
        float fade = 1 - VfxAnim.easeInCubic(t);
        VfxBloom.glow(ctx, buf, c, 1.3f * p * (1 - t * 0.5f), inst.color, fade);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 3.2f * p * e, 0, VfxVertexBuffer.withAlpha(HOT, (1 - t) * (1 - t)));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.4f * p, 0));
        float r = 3.2f * p * e;
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground, r * 0.82f, r, ctx.seg(16, 10), 1, 0, VfxVertexBuffer.withAlpha(inst.color, fade));
        hoop(buf, FIRE_BAND, VfxBlend.FIRE, ground.lift(0.5f * p * (1 - t)), r * 0.75f, r * 0.85f, 0.55f * p * (1 - t), ctx.seg(12, 8), 3, age * 0.02f,
                VfxVertexBuffer.withAlpha(WHITE, fade));
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + hash(inst.seed, i, 1) * 0.5f;
            float d = 2.4f * p * e * (0.7f + 0.5f * hash(inst.seed, i, 2));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, 1.4f * p * e - 2.5f * t * t * p, Mth.sin(ang) * d);
            pointed(buf, ctx, FIRE_TONGUE, VfxBlend.FIRE, q, 1.1f * p * (1 - t * 0.6f), Mth.HALF_PI + (hash(inst.seed, i, 3) - 0.5f), VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        int k = ctx.seg(14, 6);
        for (int i = 0; i < k; i++) {
            float ang = hash(inst.seed, i, 4) * Mth.TWO_PI, up = 0.5f + hash(inst.seed, i, 5);
            float d = 3.8f * p * e;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, up * d - 3f * t * t * p, Mth.sin(ang) * d);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p, age * 0.4f + i, VfxVertexBuffer.withAlpha(EMBER, fade));
        }
        if (t > 0.3f) {
            int sm = ctx.seg(4, 2);
            for (int i = 0; i < sm; i++) {
                float st = (t - 0.3f) / 0.7f;
                Vector3f q = new Vector3f(c).add((hash(inst.seed, i, 6) - 0.5f) * 2 * p, st * 2.2f * p, (hash(inst.seed, i, 7) - 0.5f) * 2 * p);
                buf.billboard(ctx, EARTH_DUST, VfxBlend.ALPHA, q, (1.2f + st * 1.6f) * p, i, VfxVertexBuffer.withAlpha(SMOKE, 1 - st));
            }
        }
    }
}
