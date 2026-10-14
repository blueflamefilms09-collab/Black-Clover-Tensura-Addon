package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BossFx3.*;

/**
 * 1.35.0: the VFX of the Blood-Red Commander of the Monarch's vault. The signature is a war host: crimson banners that rise round the
 * danger, crimson chains that lash and cinch, the pale vault light that falls when it wakes or changes phase, and a crimson cut that
 * leaves a blood stain. Textures: the Blood Magic and Chain Magic sprites and the shared glow / spark (no new textures).
 * <ul>
 *   <li>CMD_BANNERS (Domain Slam, Shadow Rain, Authority wind-up): at 'from' eight banners on dark poles rise round a ring of power
 *       blocks, a blood sigil turns on the floor between them and flares when the wind-up ends. power = radius.</li>
 *   <li>CMD_CHAINS (Soul Rend, Crimson Lunge lane, the Authority field): three crimson chains lash from 'from' to 'to' in a
 *       twisting braid, then cinch tight; a cuff sigil closes at 'to'. power = size.</li>
 *   <li>CMD_VAULT_LIGHT (waking, a phase roar, the summoning): a shaft of pale light falls on 'from' with dust motes in it and a
 *       sigil on the floor. power = radius, duration = how long the roar lasts.</li>
 *   <li>CMD_CRIMSON_STRIKE (Crimson Cross cut, Executioner's Arc cut, the lunge's end): a crimson streak from 'from' to 'to' with a
 *       white edge, a blood splat on the floor at 'to', threads and droplets thrown out. power = size.</li>
 * </ul>
 */
public class CommanderBossLayer extends AbstractVfxLayer {
    static final int CRIMSON = 0xD0102A, PINK = 0xFF8CB4, DARK = 0x2A0610, PALE = 0xFFE6C8, WHITE = 0xFFFFFF;
    private static final ResourceLocation CHAIN = VfxTextures.byName("chain_band"), CHAIN_STRIP = VfxTextures.byName("chain_strip"),
            CUFF = VfxTextures.byName("chain_cuff");

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.CMD_BANNERS, VfxShape.CMD_CHAINS, VfxShape.CMD_VAULT_LIGHT, VfxShape.CMD_CRIMSON_STRIKE);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CMD_BANNERS -> 36;
            case CMD_CHAINS -> 24;
            case CMD_VAULT_LIGHT -> 60;
            default -> 16;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF000000 | CRIMSON; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CMD_BANNERS -> banners(inst, ctx, buf);
            case CMD_CHAINS -> chains(inst, ctx, buf);
            case CMD_VAULT_LIGHT -> vaultLight(inst, ctx, buf);
            case CMD_CRIMSON_STRIKE -> strike(inst, ctx, buf);
            default -> { }
        }
    }

    private void banners(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), R = Math.max(2f, inst.power), fade = life(age, inst.duration, 3, 6);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float rise = ease(win(prog, 0f, 0.45f)), flare = win(prog, 0.8f, 0.92f) * (1f - win(prog, 0.92f, 1f));
        buf.plane(BloodLayer.SIGIL, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.02f).lift(0.03f), R, rgbA(rgb, (0.35f + 0.4f * prog) * fade));
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, R * 0.95f, R, ctx.seg(16, 8), 4f, 0f, -age * 0.015f, 0.05f, rgbA(PINK, 0.6f * fade));
        int n = 8;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n, x = Mth.cos(ang) * R * 0.92f, z = Mth.sin(ang) * R * 0.92f;
            float poleH = (2.6f + 0.5f * h(inst.seed, i)) * rise;
            Vector3f base = new Vector3f(c).add(x, 0f, z);
            pillar(ctx, buf, CHAIN_STRIP, VfxBlend.ALPHA, base, poleH, 0.16f, rgbA(DARK, fade), rgbA(DARK, fade));
            Vector3f top = new Vector3f(base).add(0f, poleH, 0f);
            Vector3f tangent = new Vector3f(-Mth.sin(ang), 0f, Mth.cos(ang));                  // the cloth hangs along the ring, rippling
            float wave = Mth.sin(age * 0.25f + i) * 0.18f, w = 0.55f * rise, hang = 1.5f * rise;
            Vector3f t0 = new Vector3f(top).sub(new Vector3f(tangent).mul(w)), t1 = new Vector3f(top).add(new Vector3f(tangent).mul(w));
            Vector3f b1 = new Vector3f(t1).add(wave, -hang, wave), b0 = new Vector3f(t0).add(-wave, -hang, -wave);
            int top2 = rgbA(rgb, 0.95f * fade), bot = rgbA(DARK, 0.9f * fade);
            buf.quad(BloodLayer.STREAK, VfxBlend.ALPHA, b0, b1, t1, t0, 0f, 0f, 1f, 1f, bot, top2);
            buf.billboard(ctx, BloodLayer.FLARE, VfxBlend.ADD, new Vector3f(top).add(0f, 0.15f, 0f), 0.5f * rise, 0f, rgbA(PINK, (0.5f + flare) * fade));
        }
        if (flare > 0f) buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(c).lift(0.07f), R * 1.05f, rgbA(WHITE, 0.6f * flare));
    }

    private void chains(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = Math.max(0.6f, inst.power), fade = life(age, inst.duration, 1, 6);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.5f) return;
        dir.div(len);
        Vector3f s1 = DarkMagicFx.perp(dir, ctx), s2 = new Vector3f(dir).cross(s1).normalize();
        float lash = ease(win(prog, 0f, 0.4f)), cinch = ease(win(prog, 0.4f, 0.75f));
        int rgb = inst.color & 0xFFFFFF, segs = ctx.seg(8, 5);
        for (int k = 0; k < 3; k++) {
            Vector3f[] p = new Vector3f[segs];
            float[] w = new float[segs];
            int[] col = new int[segs];
            float ph = k * Mth.TWO_PI / 3f + age * 0.25f;
            for (int i = 0; i < segs; i++) {
                float t = (float) i / (segs - 1), reach = t * lash;
                float amp = (1f - cinch) * 0.9f * P * Mth.sin(t * Mth.PI) + 0.12f;
                float ang = ph + t * 7f;
                p[i] = new Vector3f(a).lerp(b, reach).add(new Vector3f(s1).mul(Mth.cos(ang) * amp)).add(new Vector3f(s2).mul(Mth.sin(ang) * amp));
                w[i] = 0.28f * P;
                col[i] = rgbA(i % 2 == 0 ? rgb : PINK, (0.95f - 0.15f * t) * fade);
            }
            DarkMagicFx.ribbon(buf, ctx, CHAIN, VfxBlend.ALPHA, p, w, col, 0f, len * 0.6f);
        }
        Vector3f tip = new Vector3f(a).lerp(b, lash);
        buf.billboard(ctx, BloodLayer.FLARE, VfxBlend.ADD, tip, 0.9f * P, age * 0.2f, rgbA(PINK, 0.8f * fade));
        if (cinch > 0f) {
            buf.plane(CUFF, VfxBlend.ADD, VfxPose.facing(b, new Vector3f(dir).negate()).spin(age * 0.1f), (1.6f - 0.8f * cinch) * P, rgbA(rgb, 0.9f * fade));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, b, 1.4f * P * cinch, 0f, rgbA(WHITE, 0.5f * (1f - win(prog, 0.75f, 1f)) * fade));
        }
    }

    private void vaultLight(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power), fade = life(age, inst.duration, 10, 14);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c).add(0f, 14f, 0f), c, R * 1.1f, R * 1.9f, 2, age * 0.01f, rgbA(PALE, 0f), rgbA(PALE, 0.5f * fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c).add(0f, 14f, 0f), c, R * 0.35f, R * 0.7f, 2, 0f, rgbA(WHITE, 0f), rgbA(WHITE, 0.55f * fade));
        buf.plane(BloodLayer.SIGIL, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.03f).lift(0.04f), R * 1.3f, rgbA(rgb, 0.6f * fade));
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(c).lift(0.06f), R * 1.4f, rgbA(PALE, 0.35f * fade));
        int n = ctx.seg(12, 5);
        for (int i = 0; i < n; i++) {                                           // dust falling through the shaft
            float fall = (age * 0.03f + h(inst.seed, i)) % 1f, ang = h(inst.seed, i + 40) * Mth.TWO_PI, rad = R * Mth.sqrt(h(inst.seed, i + 80));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * rad, 9f * (1f - fall), Mth.sin(ang) * rad), 0.22f, age * 0.1f + i, rgbA(PALE, 0.8f * fade * Mth.sin(fall * Mth.PI)));
        }
    }

    private void strike(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = Math.max(0.6f, inst.power), fade = life(age, inst.duration, 1, 8);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float reach = ease(win(prog, 0f, 0.35f));
        Vector3f tip = new Vector3f(a).lerp(b, reach);
        DarkMagicFx.line(buf, ctx, BloodLayer.STREAK, VfxBlend.ADD, a, tip, 6, 0.9f * P, rgbA(rgb, fade));
        DarkMagicFx.line(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, a, tip, 6, 0.22f * P, rgbA(WHITE, fade));
        float hit = win(prog, 0.3f, 1f);
        if (hit <= 0f) return;
        Vector3f g = new Vector3f(b).add(0f, 0.04f, 0f);
        buf.plane(BloodLayer.SPLAT, VfxBlend.ALPHA, VfxPose.ground(g).spin(h(inst.seed, 1) * Mth.TWO_PI), P * (0.8f + 0.9f * ease(Math.min(1f, hit * 2f))), rgbA(rgb, 0.85f * (1f - 0.5f * hit) * fade));
        buf.plane(BloodLayer.SIGIL, VfxBlend.ADD, VfxPose.ground(g).lift(0.02f), P * 1.6f * ease(hit), rgbA(PINK, 0.8f * (1f - hit) * fade));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(b).add(0f, 0.5f, 0f), P * 2.2f * (1f - hit), 0f, rgbA(WHITE, 0.8f * (1f - hit)));
        for (int k = 0; k < 6; k++) {
            float ang = Mth.TWO_PI * k / 6f + h(inst.seed, k), d = P * (0.4f + 1.5f * hit) * (0.6f + 0.4f * h(inst.seed, k + 9));
            Vector3f p = new Vector3f(b).add(Mth.cos(ang) * d, 0.2f + 1.3f * hit * (1f - hit) * 2f, Mth.sin(ang) * d);
            buf.billboard(ctx, BloodLayer.DROPLET, VfxBlend.ALPHA, p, 0.26f * P, ang, rgbA(rgb, (1f - hit) * fade));
        }
    }
}
