package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Flame Magic.
 * FLAME_TRAIL: a roaring stream from caster to target - layered flame ribbons with scrolling
 * texture (hot white core inside an orange body), ember sparks drifting off it, glowing head.
 * FLAME_EXPLOSION: fireball at 'to' - expanding flame petals, heat flash, ember spray, shake.
 */
public class FlameMagicLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.FLAME_TRAIL ? 24 : 26; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFF6A1E; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.FLAME_EXPLOSION && inst.power >= 1.5f) VfxShake.add(inst.payload.to(), 0.9f * inst.power, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.FLAME_TRAIL) trail(inst, ctx, buf); else explosion(inst, ctx, buf);
    }

    private void trail(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 fromW = inst.from(ctx), toW = inst.to(ctx);
        Vector3f a = ctx.rel(fromW), b = ctx.rel(toW);
        // The head travels out, the tail follows -> a moving stream.
        float head = VfxAnim.slashHead(t), tail = Math.max(0, VfxAnim.slashTail(t) - 0.1f);
        Vector3f h = new Vector3f(b).sub(a).mul(head).add(a), tl = new Vector3f(b).sub(a).mul(tail).add(a);
        float grow = VfxAnim.flameGrow(t, age, inst.seed) * inst.power;
        int body = inst.color, core = VfxVertexBuffer.whiten(body, 0.75f);
        float alpha = VfxAnim.fadeInOut(t, 0.05f, 0.3f);
        int seg = ctx.seg(10, 4);
        // Outer body, inner core (both scroll), slight width flicker.
        buf.beam(ctx, VfxTextures.FLAME, VfxBlend.FIRE, tl, h, 0.25f * grow, 1.1f * grow, seg, -age * 0.08f,
                VfxVertexBuffer.withAlpha(body, alpha * 0.3f), VfxVertexBuffer.withAlpha(body, alpha));
        buf.beam(ctx, VfxTextures.FLAME, VfxBlend.FIRE, tl, h, 0.1f * grow, 0.5f * grow, seg, -age * 0.13f,
                VfxVertexBuffer.withAlpha(core, alpha * 0.4f), VfxVertexBuffer.withAlpha(core, alpha));
        // Burning head
        VfxBloom.glow(ctx, buf, h, 0.8f * grow, body, alpha * 1.2f);
        buf.billboard(ctx, VfxTextures.FLAME, VfxBlend.FIRE, h, 1.4f * grow, age * 0.15f, VfxVertexBuffer.withAlpha(core, alpha));
        // Embers peeling off the stream
        RandomSource r = inst.random();
        int embers = ctx.seg(18, 6);
        for (int i = 0; i < embers; i++) {
            float along = Mth.lerp(r.nextFloat(), tail, head);
            float life = (age * 0.05f + r.nextFloat()) % 1f;
            Vector3f p = new Vector3f(b).sub(a).mul(along).add(a)
                    .add((r.nextFloat() - 0.5f) * grow, life * 1.2f * grow, (r.nextFloat() - 0.5f) * grow);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.FIRE, p, 0.18f * (1 - life), r.nextFloat() * 6,
                    VfxVertexBuffer.withAlpha(core, alpha * (1 - life)));
        }
    }

    private void explosion(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.to(ctx));
        float p = inst.power, fade = 1 - VfxAnim.easeInCubic(t);
        float size = VfxAnim.explosionBurst(t) * 3.2f * p;
        int body = inst.color, core = VfxVertexBuffer.whiten(body, 0.8f);
        // Heat flash
        VfxBloom.glow(ctx, buf, c, size * 0.6f, body, fade * 1.6f);
        // Flame petals bursting outward in a sphere (each is a flame billboard)
        RandomSource r = inst.random();
        int petals = ctx.seg(22, 8);
        for (int i = 0; i < petals; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, pitch = (r.nextFloat() - 0.5f) * Mth.PI;
            float d = size * 0.45f * (0.6f + 0.4f * r.nextFloat());
            Vector3f dir = new Vector3f(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch));
            buf.billboard(ctx, VfxTextures.FLAME, VfxBlend.FIRE, new Vector3f(dir).mul(d).add(c), size * 0.5f * (1 - t * 0.5f),
                    yaw + age * 0.05f, VfxVertexBuffer.withAlpha(i % 3 == 0 ? core : body, fade));
        }
        // White-hot core
        buf.billboard(ctx, VfxTextures.MAGIC_EXPLOSION, VfxBlend.FIRE, c, size * 0.7f, age * 0.1f, VfxVertexBuffer.withAlpha(core, fade));
        // Ground scorch ring
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.4f * p, 0));
        float rr = size * (0.4f + t * 0.6f);
        buf.ring(VfxTextures.GLOW, VfxBlend.FIRE, ground, rr * 0.85f, rr, ctx.seg(24, 10), 1, 0, VfxVertexBuffer.withAlpha(body, fade));
        // Ember spray
        int embers = ctx.seg(24, 8);
        for (int i = 0; i < embers; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, up = r.nextFloat();
            float d = size * VfxAnim.easeOutCubic(t) * (0.8f + r.nextFloat());
            Vector3f e = new Vector3f(Mth.cos(yaw) * d, up * d - t * t * 2f, Mth.sin(yaw) * d).add(c);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.FIRE, e, 0.2f * p, yaw, VfxVertexBuffer.withAlpha(core, fade));
        }
    }
}
