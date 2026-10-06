package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Wind Magic (Yuno, Sylph), drawn after the anime:
 * <ul>
 *   <li>WIND_TORNADO, Spirit Storm / Tornado Fang: a pale green-white tornado widening upward around 'from' (follows the caster),
 *       its walls of wisps spinning faster near the ground, streaks spiralling up it, leaves caught in it and dust kicked up at its
 *       foot. power = radius at the base.</li>
 *   <li>WIND_GALE, Swallow's Gale / Gust Lane: swallows made of wind dart from 'from' to 'to' along a lane of rushing streaks,
 *       rings of wind rolling down it, leaves tumbling.</li>
 *   <li>WIND_EMPEROR, Slicing Wind Emperor (0.31): a huge crescent made of many layered wind blades sweeps from 'from' to 'to',
 *       widening as it flies, streaks rushing off its tips.</li>
 *   <li>WIND_ZEPHYR, Spirit of Zephyr (0.31): the wind spirit wraps the caster: a mantle of spinning gale bands, four wings of wind
 *       fanning out round them, streaks spiralling up and motes of mana rising. Follows the caster; power = size.</li>
 * </ul>
 */
public class WindSpellLayer extends AbstractVfxLayer {
    private static final int LEAF = 0xFF7CC46A, DUST = 0xA0D8D2C0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WIND_TORNADO, VfxShape.WIND_GALE, VfxShape.WIND_EMPEROR, VfxShape.WIND_ZEPHYR); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case WIND_TORNADO -> 100;
            case WIND_EMPEROR -> 20;
            case WIND_ZEPHYR -> 600;
            default -> 24;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFA6FFD6; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.WIND_TORNADO && inst.power >= 1.5f) VfxShake.add(inst.payload.from(), 0.6f, 12);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case WIND_TORNADO -> tornado(inst, ctx, buf);
            case WIND_GALE -> gale(inst, ctx, buf);
            case WIND_EMPEROR -> emperor(inst, ctx, buf);
            case WIND_ZEPHYR -> zephyr(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Spirit Storm
    private void tornado(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        float fade = life(inst, age, 0, 15);
        float height = 2.8f * r * open;
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color;

        // the funnel: four rings of wisps, narrow at the foot, wide at the top, spinning faster lower down
        int rings = 4;
        for (int k = 0; k < rings; k++) {
            float f0 = (float) k / rings, f1 = (float) (k + 1) / rings;
            float hw = height / rings * 0.62f, cy = height * (f0 + f1) * 0.5f;
            float rb = r * (0.3f + 0.7f * f0) * open, rt = r * (0.3f + 0.7f * f1) * open;
            float sway = Mth.sin(age * 0.07f + k * 0.8f) * 0.15f * r * f0;
            VfxPose pose = VfxPose.ground(new Vector3f(g).add(sway, cy, sway * 0.5f));
            hoop(buf, WIND_BAND, VfxBlend.ALPHA, pose, rb, rt, hw, ctx.seg(12, 8), 3, age * (0.09f - 0.015f * k),
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), (0.75f - 0.1f * k) * fade));
        }
        // bright streaks spiralling up the walls
        int s = ctx.seg(6, 3), segs = 5;
        for (int i = 0; i < s; i++) {
            float a0 = Mth.TWO_PI * i / s - age * 0.25f, ph = hash(inst.seed, i, 1);
            Vector3f prev = null;
            for (int k = 0; k <= segs; k++) {
                float f = Mth.clamp(ph * 0.3f + k / (float) segs * 0.7f, 0, 1);
                float rad = r * (0.32f + 0.7f * f) * open * 1.05f, ang = a0 + f * 3.2f;
                Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, height * f, Mth.sin(ang) * rad);
                if (prev != null) streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, prev, q, 0.1f * r, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.7f), 0.8f * fade));
                prev = q;
            }
        }
        // leaves caught in it
        int l = ctx.seg(6, 3);
        for (int i = 0; i < l; i++) {
            float ph = hash(inst.seed, i, 2) * 30f, lt = ((age + ph) % 30f) / 30f;
            float ang = hash(inst.seed, i, 3) * Mth.TWO_PI - age * 0.3f;
            float rad = r * (0.4f + 0.75f * lt) * open;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, height * lt, Mth.sin(ang) * rad);
            buf.billboard(ctx, WIND_LEAF, VfxBlend.ALPHA, q, 0.3f * Math.min(r, 2f), age * 0.4f + i, VfxVertexBuffer.withAlpha(LEAF, fade * Mth.clamp((1 - lt) * 3, 0, 1)));
        }
        // dust kicked up round its foot
        VfxPose foot = VfxPose.ground(new Vector3f(g).add(0, 0.25f, 0));
        hoop(buf, EARTH_DUST, VfxBlend.ALPHA, foot, r * 0.45f * open, r * 0.85f * open, 0.3f * r, ctx.seg(12, 8), 2, -age * 0.05f,
                VfxVertexBuffer.withAlpha(DUST, 0.7f * fade));
    }

    // ------------------------------------------------------------------ Swallow's Gale
    private void gale(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float fade = life(inst, age, 0, 6);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);

        // swallows made of wind, darting down the lane on staggered, weaving paths
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {
            float start = i * 2f, t = Mth.clamp((age - start) / 12f, 0, 1);
            if (t <= 0 || t >= 1) continue;
            float lat = (hash(inst.seed, i, 1) - 0.5f) * 1.6f * p, wv = Mth.sin(t * 9f + i) * 0.5f * p;
            Vector3f pos = new Vector3f(dir).mul(len * VfxAnim.easeInOutSine(t)).add(a)
                    .add(new Vector3f(sd).mul(lat + wv)).add(new Vector3f(up).mul((hash(inst.seed, i, 2) - 0.3f) * 1.2f * p));
            float alpha = fade * Mth.clamp(Math.min(t, 1 - t) * 8, 0, 1);
            Vector3f back = new Vector3f(pos).sub(new Vector3f(dir).mul(0.9f * p)), front = new Vector3f(pos).add(new Vector3f(dir).mul(0.45f * p));
            buf.beam(ctx, WIND_SWALLOW, VfxBlend.ALPHA, back, front, 1.5f * p, 1.5f * p, 1, 0, VfxVertexBuffer.withAlpha(light, alpha), VfxVertexBuffer.withAlpha(light, alpha));
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, new Vector3f(pos).sub(new Vector3f(dir).mul(3.5f * p)), back, 0.4f * p, VfxVertexBuffer.withAlpha(light, 0.7f * alpha));
        }
        // rushing streaks filling the lane
        int s = ctx.seg(14, 6);
        for (int i = 0; i < s; i++) {
            float speed = 0.05f + 0.04f * hash(inst.seed, i, 3);
            float pos = (hash(inst.seed, i, 4) + age * speed) % 1f;
            float ang = hash(inst.seed, i, 5) * Mth.TWO_PI, rad = (0.3f + hash(inst.seed, i, 6)) * 1.1f * p;
            Vector3f off = new Vector3f(sd).mul(Mth.cos(ang) * rad).add(new Vector3f(up).mul(Mth.sin(ang) * rad));
            Vector3f q0 = new Vector3f(dir).mul(len * pos).add(a).add(off), q1 = new Vector3f(dir).mul(len * Math.min(1, pos + 0.22f)).add(a).add(off);
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, q0, q1, 0.3f * p, VfxVertexBuffer.withAlpha(light, 0.6f * fade * Mth.sin(pos * Mth.PI)));
        }
        // rings of wind rolling down the lane, widening as they go
        for (int k = 0; k < 3; k++) {
            float lt = ((age / inst.duration) * 1.5f + k / 3f) % 1f;
            float rr = (0.6f + 1.0f * lt) * p;
            VfxPose pose = VfxPose.facing(new Vector3f(dir).mul(len * lt).add(a), dir).spin(age * 0.2f);
            buf.ring(WIND_BAND, VfxBlend.ALPHA, pose, rr * 0.8f, rr, ctx.seg(12, 8), 2, age * 0.05f,
                    VfxVertexBuffer.withAlpha(light, 0.7f * fade * Mth.sin(lt * Mth.PI)));
        }
        // a few leaves tumbling along
        int l = ctx.seg(6, 3);
        for (int i = 0; i < l; i++) {
            float pos = (hash(inst.seed, i, 7) + age * 0.045f) % 1f;
            Vector3f q = new Vector3f(dir).mul(len * pos).add(a).add(new Vector3f(sd).mul((hash(inst.seed, i, 8) - 0.5f) * 2 * p))
                    .add(new Vector3f(up).mul(Mth.sin(age * 0.3f + i) * 0.5f * p));
            buf.billboard(ctx, WIND_LEAF, VfxBlend.ALPHA, q, 0.28f * p, age * 0.5f + i, VfxVertexBuffer.withAlpha(LEAF, fade));
        }
    }
    // ------------------------------------------------------------------ Slicing Wind Emperor (0.31)
    private void emperor(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float travel = VfxAnim.easeOutCubic(Mth.clamp(age / (inst.duration * 0.75f), 0, 1));
        float fade = life(inst, age, 0, inst.duration * 0.3f);
        Vector3f head = new Vector3f(dir).mul(len * travel).add(a);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);
        float radius = (2.2f + 2.0f * travel) * p, sweep = 2.3f;
        // the blades: layered crescents, each tilted a little round the flight line and trailing a step behind the one before
        int blades = 6, segs = ctx.seg(10, 6);
        for (int k = 0; k < blades; k++) {
            float tilt = (k - (blades - 1) * 0.5f) * 0.22f, lag = k * 0.45f * p;
            // the blade's plane is tipped up 50 degrees from the flight line, so it reads both from behind the caster and from the side
            Vector3f upK = new Vector3f(up).mul(Mth.cos(tilt)).add(new Vector3f(sd).mul(Mth.sin(tilt)));
            Vector3f across = new Vector3f(upK).cross(dir).normalize();
            Vector3f fwd = new Vector3f(dir).mul(Mth.cos(0.87f)).add(new Vector3f(upK).mul(Mth.sin(0.87f))).normalize();
            Vector3f n = new Vector3f(fwd).cross(across).normalize();
            Vector3f centre = new Vector3f(head).sub(new Vector3f(fwd).mul(radius + lag));
            VfxPose pose = new VfxPose(centre, fwd, across, n);
            float r = radius * (1 - 0.05f * k);
            buf.arc(WIND_BAND, VfxBlend.ALPHA, pose, r, (1.3f - 0.12f * k) * p, -sweep / 2, sweep, segs,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.3f + 0.1f * k), (0.85f - 0.1f * k) * fade));
            if (k == 0) buf.arc(VfxTextures.WIND_SLASH, VfxBlend.ADD, pose, r, 0.9f * p, -sweep / 2, sweep, segs, VfxVertexBuffer.withAlpha(light, fade));
        }
        // streaks rushing back off the tips and the edge
        int s = ctx.seg(8, 4);
        for (int i = 0; i < s; i++) {
            float th = (hash(inst.seed, i, 1) - 0.5f) * sweep;
            Vector3f centre = new Vector3f(head).sub(new Vector3f(dir).mul(radius));
            Vector3f q = new Vector3f(centre).add(new Vector3f(dir).mul(Mth.cos(th) * radius)).add(new Vector3f(sd).mul(Mth.sin(th) * radius));
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, new Vector3f(q).sub(new Vector3f(dir).mul(3f * p)), q, 0.25f * p, VfxVertexBuffer.withAlpha(light, 0.7f * fade));
        }
        VfxBloom.glow(ctx, buf, head, 0.8f * p, col, 0.6f * fade);
    }

    // ------------------------------------------------------------------ Spirit of Zephyr (0.31)
    private void zephyr(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float form = VfxAnim.easeOutBack(Mth.clamp(age / 12f, 0, 1));
        float fade = life(inst, age, 0, 12);
        if (form <= 0.01f) return;
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);
        // the mantle: two gale bands spinning round the body in opposite directions, and one under the feet
        hoop(buf, WIND_BAND, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g).add(0, 0.55f, 0)), 0.75f * p * form, 0.55f * p * form, 0.4f,
                ctx.seg(12, 8), 3, age * 0.12f, VfxVertexBuffer.withAlpha(light, 0.6f * fade));
        hoop(buf, WIND_BAND, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g).add(0, 1.15f, 0)), 0.55f * p * form, 0.7f * p * form, 0.22f,
                ctx.seg(12, 8), 3, -age * 0.1f, VfxVertexBuffer.withAlpha(light, 0.45f * fade));
        buf.ring(WIND_BAND, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0)).spin(age * 0.1f), 0.5f * p * form, 1.3f * p * form,
                ctx.seg(12, 8), 3, age * 0.05f, VfxVertexBuffer.withAlpha(light, 0.5f * fade));
        // four wings of wind fanning out from the shoulders, slowly turning, beating
        int segs = ctx.seg(8, 5);
        for (int w = 0; w < 4; w++) {
            float yaw = Mth.TWO_PI * w / 4 + age * 0.01f, beat = 0.2f * Mth.sin(age * 0.2f + w);
            Vector3f out = new Vector3f(Mth.cos(yaw), 0, Mth.sin(yaw));
            VfxPose pose = new VfxPose(new Vector3f(g).add(0, 1.35f, 0), out, new Vector3f(0, 1, 0), new Vector3f(out).cross(0, 1, 0).normalize());
            buf.arc(WIND_BAND, VfxBlend.ALPHA, pose, 1.3f * p * form, 0.7f * p, -0.5f + beat, 1.6f, segs, VfxVertexBuffer.withAlpha(light, 0.7f * fade));
        }
        // streaks spiralling up round the body
        int s = ctx.seg(4, 2), sg = 4;
        for (int i = 0; i < s; i++) {
            float a0 = Mth.TWO_PI * i / s + age * 0.3f;
            Vector3f prev = null;
            for (int k = 0; k <= sg; k++) {
                float f = (float) k / sg, ang = a0 + f * 2.4f, rad = (0.6f + 0.2f * f) * p * form;
                Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, 0.1f + 1.9f * f, Mth.sin(ang) * rad);
                if (prev != null) streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, prev, q, 0.08f * p, VfxVertexBuffer.withAlpha(light, 0.7f * fade));
                prev = q;
            }
        }
        // motes of mana rising
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ph = hash(inst.seed, i, 1) * 20f, lt = ((age + ph) % 20f) / 20f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI + lt;
            float rad = (0.4f + 0.8f * hash(inst.seed, i, 3)) * p;
            buf.billboard(ctx, VfxTextures.MANA_MOTE, VfxBlend.ADD, new Vector3f(g).add(Mth.cos(ang) * rad, 0.2f + 2.2f * lt, Mth.sin(ang) * rad), 0.18f * p, 0,
                    VfxVertexBuffer.withAlpha(light, fade * Mth.sin(lt * Mth.PI)));
        }
    }
}
