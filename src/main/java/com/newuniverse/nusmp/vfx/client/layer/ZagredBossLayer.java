package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BossFx3.*;

/**
 * 1.35.0: the VFX of Zagred's moves. The signature is a book being censored and rewritten: black redaction bars, glyphs that rewrite
 * themselves, text that corrupts the air, void stones that carry a rule. Textures: the Kotodama glyph atlas, ring, smoke and the shared
 * glow / shard sprites (no new textures). Every shape fits the 400-vertex activation budget and draws nothing in the world.
 * <ul>
 *   <li>ZAGRED_INK_BARS (Redact wind-up and cut): from -> to is the lane; black bars slide across it one after the other like a
 *       censored paragraph, glyphs on them, the lane edges glowing red; in the last quarter the bars flash white and ink columns
 *       rise. power = lane half width, duration = wind-up + strike.</li>
 *   <li>ZAGRED_GLYPH_RING (Halt, Fall, every word's wind-up): a ring on the floor at 'from' closes in while twelve glyphs rewrite
 *       themselves (their cell keeps changing), a second ring counter-turns inside, and a white flash ends it. power = radius.</li>
 *   <li>ZAGRED_RULE_STONE (Overwrite): a void stone at 'from': a column of ink, a violet needle of light, a slab with the rule's glyph
 *       bobbing over a rune ring, shards orbiting. The seed's low 4 bits pick the glyph. power = size.</li>
 *   <li>ZAGRED_TEXT_CORRUPT (Overwrite rule in force, phase change, Reverse): glyphs rise out of the floor over `power` blocks of
 *       radius, flicker between red and pale, jump sideways in glitches and are struck through with ink. duration = life.</li>
 *   <li>ZAGRED_REWRITE_STRIKE (the strike of a word, the void lance): a bright sentence from 'from' to 'to' with letters running
 *       along it; at 'to' the letters burst out as glyph shards over a growing ring and an ink splat. power = impact radius.</li>
 * </ul>
 */
public class ZagredBossLayer extends AbstractVfxLayer {
    static final int VIOLET = 0x8A4CFF, INK = 0x0C0814, PALE = 0xD8C4FF, RED = 0xF2194D, WHITE = 0xFFFFFF;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.ZAGRED_INK_BARS, VfxShape.ZAGRED_GLYPH_RING, VfxShape.ZAGRED_RULE_STONE, VfxShape.ZAGRED_TEXT_CORRUPT, VfxShape.ZAGRED_REWRITE_STRIKE);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case ZAGRED_INK_BARS -> 28;
            case ZAGRED_GLYPH_RING -> 20;
            case ZAGRED_RULE_STONE -> 60;
            case ZAGRED_TEXT_CORRUPT -> 40;
            default -> 16;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF000000 | VIOLET; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case ZAGRED_INK_BARS -> inkBars(inst, ctx, buf);
            case ZAGRED_GLYPH_RING -> glyphRing(inst, ctx, buf);
            case ZAGRED_RULE_STONE -> ruleStone(inst, ctx, buf);
            case ZAGRED_TEXT_CORRUPT -> textCorrupt(inst, ctx, buf);
            case ZAGRED_REWRITE_STRIKE -> rewriteStrike(inst, ctx, buf);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- redaction bars
    private void inkBars(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        dir.y = 0f;
        float len = dir.length();
        if (len < 0.5f) return;
        dir.div(len);
        Vector3f side = new Vector3f(-dir.z, 0f, dir.x);
        float half = Math.max(0.6f, inst.power), fade = life(age, inst.duration, 3, 4);
        float strike = win(prog, 0.72f, 0.86f), flash = strike * (1f - win(prog, 0.86f, 1f));
        int bars = 4;
        float bw = half * 2f / bars;
        // the lane's edges, glowing red, and a faint ink floor
        for (int s = -1; s <= 1; s += 2) {
            Vector3f e0 = new Vector3f(a).add(new Vector3f(side).mul(s * half)), e1 = new Vector3f(a).add(new Vector3f(dir).mul(len)).add(new Vector3f(side).mul(s * half));
            strip(buf, VfxTextures.GLOW, VfxBlend.ADD, e0, e1, 0.12f, 1f, rgbA(RED, 0.8f * fade), rgbA(RED, 0.8f * fade));
        }
        for (int i = 0; i < bars; i++) {
            float grow = ease(win(prog, i * 0.06f, 0.5f + i * 0.06f)) * len;
            if (grow < 0.05f) continue;
            float off = -half + bw * (i + 0.5f);
            Vector3f s0 = new Vector3f(a).add(new Vector3f(side).mul(off)).add(0f, 0.04f + 0.005f * i, 0f);
            Vector3f s1 = new Vector3f(s0).add(new Vector3f(dir).mul(grow));
            strip(buf, VfxTextures.GLOW, VfxBlend.ALPHA, s0, s1, bw * 0.46f, 1f, rgbA(INK, 0.9f * fade), rgbA(INK, 0.9f * fade));
            if (flash > 0f) strip(buf, VfxTextures.GLOW, VfxBlend.ADD, s0, s1, bw * 0.5f, 1f, rgbA(WHITE, flash * fade), rgbA(RED, flash * fade));
            // the sentence under the bar, glyph by glyph, until the bar covers it
            int letters = Math.min(5, (int) (len / 1.4f));
            for (int j = 0; j < letters; j++) {
                float t = (j + 0.5f) / letters * len;
                if (t > grow) break;
                int cell = (int) (h(inst.seed, i * 8 + j) * 16f) & 15;
                Vector3f c = new Vector3f(s0).add(new Vector3f(dir).mul(t)).add(0f, 0.03f, 0f);
                KotodamaLayer.glyph(buf, VfxBlend.ADD, c, dir, side, bw * 0.38f, cell, rgbA(PALE, (0.55f + 0.3f * Mth.sin(age * 0.6f + j)) * fade));
            }
        }
        if (strike > 0f) {                                                 // the cut: columns of ink rise along the lane
            int cols = Math.min(6, 2 + (int) (len / 3f));
            for (int k = 0; k < cols; k++) {
                float t = (k + 0.5f) / cols * len, jit = (h(inst.seed, 100 + k) - 0.5f) * half;
                Vector3f base = new Vector3f(a).add(new Vector3f(dir).mul(t)).add(new Vector3f(side).mul(jit));
                float hgt = (2.2f + 2.4f * h(inst.seed, 120 + k)) * strike;
                pillar(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, base, hgt, 0.5f, rgbA(WHITE, 0.9f * flash), rgbA(VIOLET, 0f));
                buf.billboard(ctx, KotodamaLayer.SMOKE, VfxBlend.ALPHA, new Vector3f(base).add(0f, hgt * 0.6f, 0f), 1.4f + strike, age * 0.05f + k, rgbA(INK, 0.7f * fade * (1f - flash * 0.5f)));
            }
        }
    }

    // ---------------------------------------------------------------- rewriting ring
    private void glyphRing(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Math.max(1.5f, inst.power), fade = life(age, inst.duration, 2, 4);
        int rgb = inst.color & 0xFFFFFF;
        float shrink = 1f - 0.3f * ease(win(prog, 0f, 0.85f));
        float rr = R * shrink;
        disc(buf, VfxTextures.DEVIL_CIRCLE, VfxBlend.ALPHA, c, rr, age * 0.02f, 0.03f, rgbA(INK, (0.35f + 0.35f * prog) * fade));
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, rr * 0.9f, rr, ctx.seg(20, 8), 3f, age * 0.01f, age * 0.03f, 0.05f, rgbA(rgb, 0.85f * fade));
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, rr * 0.5f, rr * 0.6f, ctx.seg(14, 7), 2f, 0f, -age * 0.05f, 0.06f, rgbA(PALE, 0.6f * fade));
        Vector3f right = new Vector3f(1, 0, 0), up = new Vector3f(0, 0, 1);
        int n = 12;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.02f;
            int cell = (int) (age * 0.45f + h(inst.seed, i) * 16f) & 15;              // the letter keeps rewriting itself
            boolean hot = ((int) (age * 0.3f) + i) % 5 == 0;
            Vector3f g = new Vector3f(c).add(Mth.cos(ang) * rr * 0.78f, 0.08f, Mth.sin(ang) * rr * 0.78f);
            KotodamaLayer.glyph(buf, VfxBlend.ADD, g, right, up, 0.32f + 0.05f * R / 6f, cell, rgbA(hot ? WHITE : rgb, (hot ? 1f : 0.8f) * fade));
        }
        float flash = win(prog, 0.82f, 0.92f) * (1f - win(prog, 0.92f, 1f));
        if (flash > 0f) disc(buf, VfxTextures.GLOW, VfxBlend.ADD, c, rr * 1.05f, 0f, 0.07f, rgbA(WHITE, 0.7f * flash));
    }

    // ---------------------------------------------------------------- void rule stone
    private void ruleStone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = Math.max(0.6f, inst.power), fade = life(age, inst.duration, 8, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, 0.7f * P, 1.0f * P, ctx.seg(12, 6), 2f, 0f, age * 0.04f, 0.05f, rgbA(rgb, 0.7f * fade));
        pillar(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, c, 3.2f * P, 0.22f * P, rgbA(WHITE, 0.55f * fade), rgbA(VIOLET, 0f));
        for (int k = 0; k < 3; k++) {
            float rise = (age * 0.02f + k / 3f) % 1f;
            buf.billboard(ctx, KotodamaLayer.SMOKE, VfxBlend.ALPHA, new Vector3f(c).add(0f, 0.4f + rise * 2.6f * P, 0f), (0.9f + rise) * P, age * 0.03f + k * 2f, rgbA(INK, 0.7f * fade * (1f - rise * 0.6f)));
        }
        Vector3f slab = new Vector3f(c).add(0f, (1.5f + 0.15f * Mth.sin(age * 0.12f)) * P, 0f);
        buf.billboard(ctx, KotodamaLayer.SMOKE, VfxBlend.ALPHA, slab, 1.1f * P, 0f, rgbA(INK, 0.85f * fade));
        KotodamaLayer.glyph(buf, VfxBlend.ADD, slab, ctx.camRight, ctx.camUp, 0.42f * P, (int) (inst.seed & 15), rgbA(PALE, (0.8f + 0.2f * Mth.sin(age * 0.5f)) * fade));
        for (int k = 0; k < 4; k++) {
            float ang = age * 0.07f + Mth.HALF_PI * k;
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, new Vector3f(slab).add(Mth.cos(ang) * 0.8f * P, 0.15f * Mth.sin(ang * 2f), Mth.sin(ang) * 0.8f * P), 0.22f * P, ang, rgbA(rgb, 0.8f * fade));
        }
    }

    // ---------------------------------------------------------------- text corrupting the air
    private void textCorrupt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(2f, inst.power), fade = life(age, inst.duration, 6, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        disc(buf, KotodamaLayer.SMOKE, VfxBlend.ALPHA, c, R, age * 0.01f, 0.03f, rgbA(INK, 0.45f * fade));
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, R * 0.94f, R, ctx.seg(18, 8), 4f, -age * 0.01f, age * 0.02f, 0.05f, rgbA(inst.color & 0xFFFFFF, 0.6f * fade));
        int n = ctx.seg(22, 8);
        for (int i = 0; i < n; i++) {
            float ang = h(inst.seed, i) * Mth.TWO_PI, rad = R * Mth.sqrt(h(inst.seed, i + 50)) * 0.95f;
            float rise = (age * (0.012f + 0.01f * h(inst.seed, i + 90)) + h(inst.seed, i + 130)) % 1f;
            boolean glitch = ((int) (age * 0.5f) + i) % 6 == 0;
            float jx = glitch ? (h(inst.seed + (long) (age * 0.5f), i) - 0.5f) * 0.9f : 0f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad + jx, 0.3f + rise * 3.6f, Mth.sin(ang) * rad);
            int cell = (int) (age * 0.35f + h(inst.seed, i + 170) * 16f) & 15;
            float a = Mth.sin(rise * Mth.PI) * fade;
            KotodamaLayer.glyph(buf, VfxBlend.ADD, p, ctx.camRight, ctx.camUp, glitch ? 0.4f : 0.26f, cell, rgbA(i % 3 == 0 ? RED : PALE, a));
            if (i % 3 == 1) {                                                  // struck through with ink
                Vector3f l = new Vector3f(p).sub(new Vector3f(ctx.camRight).mul(0.34f)), r = new Vector3f(p).add(new Vector3f(ctx.camRight).mul(0.34f));
                Vector3f u = new Vector3f(ctx.camUp).mul(0.04f);
                buf.quad(VfxTextures.GLOW, VfxBlend.ALPHA, new Vector3f(l).sub(u), new Vector3f(r).sub(u), new Vector3f(r).add(u), new Vector3f(l).add(u), 0f, 0f, 1f, 1f, rgbA(INK, 0.95f * a), rgbA(INK, 0.95f * a));
            }
        }
    }

    // ---------------------------------------------------------------- the sentence lands
    private void rewriteStrike(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), fade = life(age, inst.duration, 1, 6);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float I = Math.max(1f, inst.power);
        int rgb = inst.color & 0xFFFFFF;
        float reach = ease(win(prog, 0f, 0.4f));
        Vector3f tip = new Vector3f(a).lerp(b, reach);
        DarkMagicFx.line(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, a, tip, 6, 0.5f * Math.min(I, 2f), rgbA(rgb, 0.85f * fade));
        DarkMagicFx.line(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, a, tip, 6, 0.18f, rgbA(WHITE, fade));
        for (int j = 0; j < 6; j++) {                                          // letters running along the sentence
            float t = ((j / 6f) + prog * 1.5f) % 1f * reach;
            Vector3f p = new Vector3f(a).lerp(b, t);
            KotodamaLayer.glyph(buf, VfxBlend.ADD, p, ctx.camRight, ctx.camUp, 0.3f, (int) (h(inst.seed, j) * 16f) & 15, rgbA(PALE, fade));
        }
        float hit = win(prog, 0.38f, 1f);
        if (hit <= 0f) return;
        float rad = I * (0.4f + 1.2f * ease(hit));
        band(buf, KotodamaLayer.RING, VfxBlend.ADD, b, rad * 0.8f, rad, ctx.seg(14, 7), 3f, 0f, hit, 0.05f, rgbA(rgb, (1f - hit) * fade));
        buf.billboard(ctx, KotodamaLayer.SMOKE, VfxBlend.ALPHA, new Vector3f(b).add(0f, 0.3f, 0f), I * (0.8f + hit), age * 0.1f, rgbA(INK, 0.8f * (1f - hit)));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(b).add(0f, 0.3f, 0f), I * 2f * (1f - hit), 0f, rgbA(WHITE, 0.9f * (1f - hit)));
        for (int k = 0; k < 8; k++) {                                          // the sentence breaks into letters that fly out
            float ang = Mth.TWO_PI * k / 8f + h(inst.seed, 40 + k), d = I * (0.3f + 1.6f * hit) * (0.6f + 0.4f * h(inst.seed, 60 + k));
            Vector3f p = new Vector3f(b).add(Mth.cos(ang) * d, 0.3f + 1.2f * hit * h(inst.seed, 80 + k), Mth.sin(ang) * d);
            KotodamaLayer.glyph(buf, VfxBlend.ADD, p, ctx.camRight, ctx.camUp, 0.24f, (int) (h(inst.seed, 20 + k) * 16f) & 15, rgbA(k % 2 == 0 ? RED : PALE, (1f - hit) * fade));
        }
    }
}
