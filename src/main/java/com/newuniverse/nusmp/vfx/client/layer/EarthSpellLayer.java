package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Earth Magic (Gnome; Ground Wall, Rising Ground, Mother Earth spells), drawn after the anime: real 3D stone, lit from above.
 * <ul>
 *   <li>STONE_SPIKES: jagged stone spikes burst out of the ground one after another from 'from' to 'to' (one per block, two ticks
 *       apart, like the spell's hits), each with a puff of dust and a chunk of rock thrown up, then sink back.</li>
 *   <li>EARTH_RISE, Ground Wall: the ground cracks along the wall's base, dust billows up and rubble flies as the wall rises.</li>
 *   <li>EARTH_FISSURE, Mother Earth Split: a crack races from 'from' to 'to', glowing faintly with mana, and slabs of stone shove up
 *       and lean out along both sides of it.</li>
 * </ul>
 */
public class EarthSpellLayer extends AbstractVfxLayer {
    private static final int STONE = 0xFFFFFFFF, DUST = 0xC0C9B08A, CRACK = 0xFF2A1C12;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.STONE_SPIKES, VfxShape.EARTH_RISE, VfxShape.EARTH_FISSURE); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case STONE_SPIKES -> 44;
            case EARTH_RISE -> 30;
            default -> 40;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFB45A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        VfxShake.add(inst.payload.from(), inst.shape == VfxShape.EARTH_FISSURE ? 1.4f : 0.7f, inst.shape == VfxShape.EARTH_FISSURE ? 18 : 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case STONE_SPIKES -> spikes(inst, ctx, buf);
            case EARTH_RISE -> rise(inst, ctx, buf);
            case EARTH_FISSURE -> fissure(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ stone spikes
    private void spikes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir);
        int n = Math.min(12, Math.max(1, Math.round(len)));
        float sink = Mth.clamp((inst.duration - age) / 10f, 0, 1);

        // the line they erupt along
        float crackT = Mth.clamp(age / (n * 2f + 2), 0, 1);
        groundStrip(buf, EARTH_CRACK, VfxBlend.ALPHA, new Vector3f(a).add(0, 0.03f, 0), new Vector3f(dir).mul(len * crackT).add(a).add(0, 0.03f, 0),
                0.5f * p, 0, len / 4f, VfxVertexBuffer.withAlpha(CRACK, 0.8f * sink));
        for (int k = 1; k <= n; k++) {
            float t0 = k * 2f, local = age - t0;
            if (local < 0) continue;
            float up = VfxAnim.easeOutBack(Mth.clamp(local / 4f, 0, 1)) * sink;
            float h = (1.5f + 0.9f * hash(inst.seed, k, 1)) * p * up;
            float rad = 0.42f * p * (0.8f + 0.4f * hash(inst.seed, k, 2));
            Vector3f base = new Vector3f(dir).mul(k * len / n).add(a).add(new Vector3f(sd).mul((hash(inst.seed, k, 3) - 0.5f) * 0.6f * p)).add(0, -0.15f, 0);
            Vector3f tip = new Vector3f(base).add(new Vector3f(sd).mul((hash(inst.seed, k, 4) - 0.5f) * 0.5f * h)).add(new Vector3f(dir).mul(0.15f * h)).add(0, h, 0);
            if (h > 0.02f) spike(buf, base, tip, rad, hash(inst.seed, k, 5) * Mth.HALF_PI, STONE);
            // dust puff as it breaks the surface
            float dt = Mth.clamp(local / 12f, 0, 1);
            buf.billboard(ctx, EARTH_DUST, VfxBlend.ALPHA, new Vector3f(base).add(0, 0.3f + dt * 0.6f, 0), (0.8f + 1.4f * dt) * p, k,
                    VfxVertexBuffer.withAlpha(DUST, (1 - dt) * 0.9f));
            // a chunk of rock thrown up
            float ct = Mth.clamp(local / 14f, 0, 1);
            if (ct < 1) {
                float ang = hash(inst.seed, k, 6) * Mth.TWO_PI;
                Vector3f q = new Vector3f(base).add(Mth.cos(ang) * 1.4f * p * ct, (3.2f * ct - 3.6f * ct * ct) * p + 0.3f, Mth.sin(ang) * 1.4f * p * ct);
                buf.billboard(ctx, EARTH_CHUNK, VfxBlend.ALPHA, q, 0.32f * p, local * 0.4f, STONE);
            }
        }
    }

    // ------------------------------------------------------------------ Ground Wall
    private void rise(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = Math.max(0.01f, path.length());
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir);
        float fade = life(inst, age, 0, 10), open = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
        // the ground splitting along both faces of the wall
        for (int s = -1; s <= 1; s += 2) {
            Vector3f off = new Vector3f(sd).mul(s * 0.75f * p).add(0, 0.03f, 0);
            groundStrip(buf, EARTH_CRACK, VfxBlend.ALPHA, new Vector3f(a).add(off).sub(new Vector3f(dir).mul(0.5f)), new Vector3f(b).add(off).add(new Vector3f(dir).mul(0.5f)),
                    0.6f * p * open, 0, len / 3f, VfxVertexBuffer.withAlpha(CRACK, 0.85f * fade));
        }
        // dust billowing out from the base
        int d = ctx.seg(10, 5);
        for (int i = 0; i < d; i++) {
            float s = hash(inst.seed, i, 1), dt = Mth.clamp((age - s * 4) / 22f, 0, 1);
            if (dt <= 0) continue;
            float out = (hash(inst.seed, i, 2) < 0.5f ? -1 : 1) * (0.8f + 1.6f * dt) * p;
            Vector3f q = new Vector3f(a).lerp(b, s).add(new Vector3f(sd).mul(out)).add(0, 0.4f + 1.3f * dt * p, 0);
            buf.billboard(ctx, EARTH_DUST, VfxBlend.ALPHA, q, (1.0f + 2.0f * dt) * p, i, VfxVertexBuffer.withAlpha(DUST, (1 - dt) * fade));
        }
        // rubble flying up and falling back
        int r = ctx.seg(16, 8);
        for (int i = 0; i < r; i++) {
            float ct = Mth.clamp((age - hash(inst.seed, i, 3) * 4) / 16f, 0, 1);
            if (ct <= 0 || ct >= 1) continue;
            float out = (hash(inst.seed, i, 4) - 0.5f) * 3.2f * p;
            Vector3f q = new Vector3f(a).lerp(b, hash(inst.seed, i, 5)).add(new Vector3f(sd).mul(out * ct))
                    .add(0, (5f * ct - 5.6f * ct * ct) * p + 0.2f, 0);
            buf.billboard(ctx, EARTH_CHUNK, VfxBlend.ALPHA, q, (0.2f + 0.25f * hash(inst.seed, i, 6)) * p, age * 0.4f + i, STONE);
        }
        // a ring of dust rolling out from the middle of the wall
        Vector3f mid = new Vector3f(a).lerp(b, 0.5f);
        float rt = Mth.clamp(age / 18f, 0, 1);
        hoop(buf, EARTH_DUST, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(mid).add(0, 0.3f, 0)), (1 + 3.5f * rt) * p, (1.3f + 4f * rt) * p, 0.35f * p,
                ctx.seg(12, 8), 2, age * 0.01f, VfxVertexBuffer.withAlpha(DUST, (1 - rt) * 0.8f));
    }

    // ------------------------------------------------------------------ Mother Earth Split
    private void fissure(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        if (len < 0.01f) return;
        Vector3f dir = new Vector3f(path).div(len), sd = side(dir);
        float run = Mth.clamp(age / 12f, 0, 1);              // how far the crack has raced
        float fade = life(inst, age, 0, 12);
        int col = inst.color;

        // the zigzag crack
        int n = ctx.seg(10, 6);
        Vector3f prev = new Vector3f(a).add(0, 0.04f, 0);
        for (int k = 1; k <= n; k++) {
            float f = (float) k / n;
            if (f > run + 1f / n) break;
            float jit = (k == n ? 0 : (hash(inst.seed, k, 1) - 0.5f) * 0.8f * p);
            Vector3f q = new Vector3f(dir).mul(len * Math.min(f, run)).add(a).add(new Vector3f(sd).mul(jit)).add(0, 0.04f, 0);
            float w = 0.7f * p * Mth.clamp((run - f) * 4 + 0.4f, 0.3f, 1);
            groundStrip(buf, EARTH_CRACK, VfxBlend.ALPHA, prev, q, w, k * 0.3f, k * 0.3f + 0.3f, VfxVertexBuffer.withAlpha(CRACK, fade));
            groundStrip(buf, EARTH_CRACK, VfxBlend.ADD, prev, q, w * 0.45f, k * 0.3f, k * 0.3f + 0.3f,
                    VfxVertexBuffer.withAlpha(col, (0.45f + 0.25f * Mth.sin(age * 0.6f + k)) * fade));
            prev = q;
        }
        // slabs of stone shoved up and leaning out on alternating sides as the crack passes
        int m = ctx.seg(8, 4);
        float sink = Mth.clamp((inst.duration - age) / 10f, 0, 1);
        for (int i = 0; i < m; i++) {
            float f = (i + 0.5f) / m;
            float lt = Mth.clamp((run - f) * 6f, 0, 1);
            if (lt <= 0) continue;
            float s = (i % 2 == 0 ? 1 : -1);
            float h = (0.9f + 0.7f * hash(inst.seed, i, 2)) * p * VfxAnim.easeOutBack(lt) * sink;
            Vector3f out = new Vector3f(sd).mul(s);
            Vector3f base = new Vector3f(dir).mul(len * f).add(a).add(new Vector3f(out).mul(0.8f * p)).add(0, -0.3f * p, 0);
            Vector3f along = new Vector3f(dir).rotateY((hash(inst.seed, i, 3) - 0.5f) * 0.5f);
            if (h > 0.02f) slab(buf, base, along, out, 0.65f * p, 0.18f * p, h, 0.45f * lt, STONE);
        }
        // dust and stones thrown up along it
        int d = ctx.seg(8, 4);
        for (int i = 0; i < d; i++) {
            float f = hash(inst.seed, i, 4), dt = Mth.clamp((run - f) * 3f, 0, 1);
            if (dt <= 0) continue;
            Vector3f base = new Vector3f(dir).mul(len * f).add(a);
            float tt = Mth.clamp((age - f * 12) / 20f, 0, 1);
            buf.billboard(ctx, EARTH_DUST, VfxBlend.ALPHA, new Vector3f(base).add(0, 0.5f + tt * 1.2f * p, 0), (1 + 1.8f * tt) * p, i,
                    VfxVertexBuffer.withAlpha(DUST, (1 - tt) * 0.9f * fade));
            float ct = Mth.clamp((age - f * 12) / 14f, 0, 1);
            if (ct < 1) {
                float side = (hash(inst.seed, i, 5) - 0.5f) * 3f * p;
                Vector3f q = new Vector3f(base).add(new Vector3f(sd).mul(side * ct)).add(0, (3.6f * ct - 4f * ct * ct) * p + 0.2f, 0);
                buf.billboard(ctx, EARTH_CHUNK, VfxBlend.ALPHA, q, 0.3f * p, age * 0.4f + i, STONE);
            }
        }
    }
}
