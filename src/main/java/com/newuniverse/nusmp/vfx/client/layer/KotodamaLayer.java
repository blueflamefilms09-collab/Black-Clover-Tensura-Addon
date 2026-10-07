package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.47 Kotodama Magic (Word Soul), Zagred's: indigo, violet and ink-black. Textures from tools/gen_kotodama_textures.py.
 * <ul>
 *   <li>KOTO_WORDS: the spoken command as demonic glyphs in mid-air in front of the speaker. They burn in one by one (a white
 *       flash settling to violet) on a slab of black smoke, a rune ring pulses out behind them, then they shiver and burn away
 *       upward. The seed's low 4 bits are the word (glyph count and choice), the next 4 the speaker's tier (the devil's loan is
 *       fainter).</li>
 *   <li>KOTO_AURA: long (over 40 ticks), following: the corrupted grimoire's aura, purple-black smoke billowing and clinging
 *       round the body, rising, with a glowing five-leaf emblem turning on the ground. Short: a burst ("Halt", "Heal"), a ring
 *       of runes and smoke rushing out to 6 x power blocks.</li>
 *   <li>KOTO_SHATTER: a projectile breaking apart: a crack star, black-glass shards flung out, and the freed magicules streaming
 *       back to 'to' (the speaker) as violet motes.</li>
 *   <li>KOTO_SWORDS: a demon sword dropping point-first 'from' (the sky) -> 'to' (the ground), a black wake behind it, stuck in
 *       the ground for a moment with a violet ring.</li>
 *   <li>KOTO_TRIDENT: thrown (nothing followed): an otherworldly trident flying 'from' -> 'to' with a violet trail and smoke;
 *       followed: the trident forming in the hand, glyph sparks drawn into it.</li>
 *   <li>KOTO_SLUDGE: underworld sludge welling out over the ground to 4 x power blocks: a black-violet pool with a glowing rim,
 *       bubbles that swell and pop, and wisps of black smoke.</li>
 * </ul>
 */
public class KotodamaLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation GLYPHS = t("koto_glyphs"), SMOKE = t("koto_smoke"), CLOVER = t("koto_clover"), RING = t("koto_ring"),
            CRACK = t("koto_crack"), SWORD = t("koto_sword"), TRIDENT = t("koto_trident"), POOL = t("koto_sludge"), SHARD = VfxTextures.SHARD,
            GLOW = VfxTextures.GLOW;
    static final int VIOLET = 0xFF8A4CFF, INDIGO = 0xFF3A2A9A, INK = 0xFF0C0814, PALE = 0xFFD8C4FF;
    /** Glyph counts (the spoken word's letters): HALT, SHATTER, HEAL, SLUDGE, TRIDENT, SWORDS, then the 0.48 words SEAL, REJECT,
     *  FALL, REVEAL, SLEEP, PETRIFY, COWER, BANISH, REVERSE, DRAIN. */
    static final int[] LETTERS = {4, 7, 4, 6, 7, 6, 4, 6, 4, 6, 5, 7, 5, 6, 7, 5};

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.KOTO_WORDS, VfxShape.KOTO_AURA, VfxShape.KOTO_SHATTER, VfxShape.KOTO_SWORDS, VfxShape.KOTO_TRIDENT, VfxShape.KOTO_SLUDGE);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case KOTO_WORDS -> 40;
            case KOTO_AURA -> 70;
            case KOTO_SLUDGE -> 200;
            case KOTO_SHATTER -> 24;
            default -> 14;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return VIOLET; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case KOTO_WORDS -> words(inst, ctx, buf);
            case KOTO_AURA -> { if (inst.duration > 40) aura(inst, ctx, buf); else burst(inst, ctx, buf); }
            case KOTO_SHATTER -> shatter(inst, ctx, buf);
            case KOTO_SWORDS -> sword(inst, ctx, buf);
            case KOTO_TRIDENT -> trident(inst, ctx, buf);
            case KOTO_SLUDGE -> sludge(inst, ctx, buf);
            default -> { }
        }
    }

    /** One cell of the 4x4 glyph atlas, as a quad centred at c spanning right/up. */
    static void glyph(VfxVertexBuffer buf, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float half, int cell, int argb) {
        float u0 = (cell & 3) / 4f, v0 = (cell >> 2 & 3) / 4f;
        Vector3f r = new Vector3f(right).mul(half), u = new Vector3f(up).mul(half);
        int col = blend.grade(argb, 1f);
        buf.quad(GLYPHS, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                u0, v0, u0 + 0.25f, v0 + 0.25f, col, col);
    }

    // ================================================================ the spoken word
    private void words(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = Math.max(0.6f, inst.power);
        int word = (int) (inst.seed & 15), tier = (int) (inst.seed >> 4 & 15);
        int n = LETTERS[Math.floorMod(word, LETTERS.length)];
        float strength = tier == 1 ? 0.6f : 1f;                               // the devil's loan burns fainter
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f right = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
        float size = 0.6f * P, gap = size * 1.0f, out = Mth.clamp((age - (inst.duration - 12)) / 12f, 0, 1);
        float fade = life(inst, age, 2, 10) * strength;
        // the black smoke slab the words are burned into
        for (int k = 0; k < 5; k++) {
            float x = (k - 2) * gap * n / 5f, rot = hash(inst.seed, k, 1) * Mth.TWO_PI + age * 0.02f;
            Vector3f p = new Vector3f(c).add(new Vector3f(right).mul(x)).add(new Vector3f(up).mul((hash(inst.seed, k, 2) - 0.5f) * size + out * 0.6f));
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, size * (2.2f + out), rot, VfxVertexBuffer.withAlpha(INK, 0.7f * fade));
        }
        // the rune ring pulsing out behind the word
        float rt = Mth.clamp(age / 14f, 0, 1);
        if (rt < 1) {
            VfxPose pose = new VfxPose(new Vector3f(c).sub(new Vector3f(right).cross(up).mul(-0.05f)), right, up, new Vector3f(right).cross(up));
            float R = (0.5f + 1.4f * VfxAnim.easeOutCubic(rt)) * P * Math.max(1, n / 4f);
            buf.ring(RING, VfxBlend.ADD, pose.spin(age * 0.05f), R * 0.82f, R, ctx.seg(24, 12), 4, age * 0.02f, VfxVertexBuffer.withAlpha(VIOLET, (1 - rt) * 0.9f * strength));
        }
        // the glyphs, one by one: a white flash settling to violet, a shiver, then burning away upward
        for (int k = 0; k < n; k++) {
            float appear = Mth.clamp((age - k * 1.6f) / 4f, 0, 1);
            if (appear <= 0) continue;
            float pop = VfxAnim.easeOutBack(appear), flash = Mth.clamp(1 - (age - k * 1.6f) / 6f, 0, 1);
            float shiver = (hash(inst.seed, k, (int) age) - 0.5f) * 0.04f * P;
            float x = (k - (n - 1) / 2f) * gap;
            Vector3f p = new Vector3f(c).add(new Vector3f(right).mul(x + shiver)).add(new Vector3f(up).mul(out * (0.8f + hash(inst.seed, k, 3)) * P));
            int cell = (word * 5 + k * 3 + (int) (hash(inst.seed, k, 4) * 3)) & 15;
            int col = VfxVertexBuffer.lerpColor(VIOLET, 0xFFFFFFFF, flash);
            float a = fade * (1 - out);
            glyph(buf, VfxBlend.ALPHA, p, right, up, size * 0.62f * pop, cell, VfxVertexBuffer.withAlpha(INK, 0.8f * a));       // a dark halo cut
            glyph(buf, VfxBlend.ADD, p, right, up, size * 0.5f * pop, cell, VfxVertexBuffer.withAlpha(col, a));
            VfxBloom.glow(ctx, buf, p, size * (1.2f + flash), VIOLET, (0.35f + 0.6f * flash) * a);
        }
        // a thin underline of light
        float line = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1)) * (1 - out);
        Vector3f l0 = new Vector3f(c).add(new Vector3f(up).mul(-size * 0.75f)), half = new Vector3f(right).mul(gap * n * 0.55f * line);
        DreamPaintLayer.strokePart(buf, ctx, GLOW, VfxBlend.ADD, new Vector3f(l0).sub(half), new Vector3f(l0).add(half), 0, 1, 0.06f * P,
                VfxVertexBuffer.withAlpha(PALE, 0.8f * fade));
    }

    // ================================================================ the grimoire's aura
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power;
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 10, 14);
        int glow = inst.color;
        // the five-leaf emblem turning on the ground, glowing and pulsing
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.25f);
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.03f, 0)).spin(age * 0.02f);
        buf.plane(CLOVER, VfxBlend.ADD, ground, 1.4f * P, VfxVertexBuffer.withAlpha(glow, 0.85f * pulse * fade));
        buf.plane(CLOVER, VfxBlend.ALPHA, ground.lift(0.01f), 1.55f * P, VfxVertexBuffer.withAlpha(INK, 0.35f * fade));
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.15f, 0), 2.2f * P, glow, 0.3f * pulse * fade);
        // smoke clinging round the body and billowing up (dark, alpha) with violet licks (additive)
        int puffs = ctx.seg(18, 10);
        for (int k = 0; k < puffs; k++) {
            float ph = (age * 0.018f + hash(inst.seed, k, 1)) % 1f;           // each puff rises and fades on its own cycle
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI + age * 0.03f * (k % 2 == 0 ? 1 : -1);
            float r = (0.45f + 0.35f * hash(inst.seed, k, 3)) * P * (1 + 0.6f * ph);
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * r, 0.1f + ph * 2.4f * P, Mth.sin(ang) * r);
            float a = Mth.sin(ph * Mth.PI) * fade;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, (0.9f + 1.4f * ph) * P, ang + ph * 2, VfxVertexBuffer.withAlpha(k % 3 == 0 ? INDIGO : INK, 0.8f * a));
            if (k % 2 == 0) buf.billboard(ctx, SMOKE, VfxBlend.ADD, p, (0.45f + 0.6f * ph) * P, -ang, VfxVertexBuffer.withAlpha(glow, 0.22f * a));
        }
        // motes rising off the emblem
        for (int k = 0; k < 6; k++) {
            float ph = (age * 0.03f + hash(inst.seed, k, 5)) % 1f, ang = hash(inst.seed, k, 6) * Mth.TWO_PI;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * 0.9f * P, ph * 2.8f * P, Mth.sin(ang) * 0.9f * P);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, p, 0.18f * P, 0, VfxVertexBuffer.withAlpha(PALE, Mth.sin(ph * Mth.PI) * fade));
        }
    }

    /** "Halt" / "Heal": a ring of runes and smoke rushing out over the ground. */
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx));
        float R = 6 * P * VfxAnim.easeOutCubic(Mth.clamp(t / 0.6f, 0, 1)), fade = life(inst, age, 0, 10);
        int col = inst.color;
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.06f, 0));
        buf.ring(RING, VfxBlend.ADD, ground.spin(age * 0.04f), Math.max(0, R - 0.9f * Math.max(1, P)), R, ctx.seg(32, 16), 8, age * 0.03f,
                VfxVertexBuffer.withAlpha(col, 0.9f * fade));
        buf.plane(CLOVER, VfxBlend.ADD, ground.spin(-age * 0.05f), Math.min(R, 2.5f), VfxVertexBuffer.withAlpha(col, fade * (1 - t)));
        int n = ctx.seg(16, 8);
        for (int k = 0; k < n; k++) {
            float ang = Mth.TWO_PI * k / n + hash(inst.seed, k, 1) * 0.3f;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * R, 0.4f + 0.5f * hash(inst.seed, k, 2), Mth.sin(ang) * R);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, 1.4f + 0.4f * P, ang, VfxVertexBuffer.withAlpha(INK, 0.5f * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 1, 0), 2.5f, col, 0.6f * fade * (1 - t));
    }

    // ================================================================ "Shatter"
    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx)), home = ctx.rel(inst.to(ctx));
        float fade = life(inst, age, 0, 8), crack = VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1));
        buf.billboard(ctx, CRACK, VfxBlend.ADD, c, 1.6f * P * crack, hash(inst.seed, 0, 1) * Mth.TWO_PI, VfxVertexBuffer.withAlpha(PALE, fade * (1 - t)));
        VfxBloom.glow(ctx, buf, c, 1.4f * P, inst.color, 0.8f * Mth.clamp(1 - age / 6f, 0, 1));
        for (int k = 0; k < 8; k++) {                                         // black-glass shards
            Vector3f d = new Vector3f(hash(inst.seed, k, 2) - 0.5f, hash(inst.seed, k, 3) - 0.3f, hash(inst.seed, k, 4) - 0.5f).normalize();
            float s = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1)) * 1.4f * P;
            Vector3f p = new Vector3f(c).add(new Vector3f(d).mul(s)).add(0, -0.6f * t * t, 0);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, p, 0.3f * P, age * 0.4f + k, VfxVertexBuffer.withAlpha(INK, 0.9f * fade));
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.22f * P, age * 0.4f + k, VfxVertexBuffer.withAlpha(VIOLET, 0.7f * fade));
        }
        if (new Vector3f(home).sub(c).lengthSquared() > 0.04f) {               // the freed magicules flow home
            Vector3f mid = new Vector3f(c).add(home).mul(0.5f).add(0, 1.2f, 0);
            for (int k = 0; k < 10; k++) {
                float u = Mth.clamp((age - 4 - k * 0.8f) / 12f, 0, 1);
                if (u <= 0 || u >= 1) continue;
                float e = VfxAnim.easeInOutSine(u), a1 = 1 - e;
                Vector3f p = new Vector3f(c).mul(a1 * a1).add(new Vector3f(mid).mul(2 * a1 * e)).add(new Vector3f(home).mul(e * e));
                p.add((hash(inst.seed, k, 7) - 0.5f) * 0.5f, (hash(inst.seed, k, 8) - 0.5f) * 0.5f, 0);
                buf.billboard(ctx, GLOW, VfxBlend.ADD, p, 0.22f * P, 0, VfxVertexBuffer.withAlpha(VIOLET, Mth.sin(u * Mth.PI)));
            }
        }
    }

    // ================================================================ demon swords
    private void sword(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power;
        Vector3f sky = ctx.rel(inst.from(ctx)), ground = ctx.rel(inst.to(ctx));
        float fall = VfxAnim.easeInCubic(Mth.clamp(age / 8f, 0, 1)), fade = life(inst, age, 1, 5);
        Vector3f dir = new Vector3f(ground).sub(sky);
        if (dir.lengthSquared() < 1e-4f) return;
        dir.normalize();
        float L = 3.2f * P;
        Vector3f tip = new Vector3f(sky).lerp(ground, fall).add(new Vector3f(dir).mul(0.6f * fall));   // sinks into the ground at the end
        Vector3f hilt = new Vector3f(tip).sub(new Vector3f(dir).mul(L));
        if (fall < 1) buf.beam(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(hilt).sub(new Vector3f(dir).mul(3f * P * fall)), hilt, 0.1f, 0.6f * P, 3, age * 0.1f,
                VfxVertexBuffer.withAlpha(INK, 0f), VfxVertexBuffer.withAlpha(INK, 0.6f * fade));
        DreamPaintLayer.strokePart(buf, ctx, SWORD, VfxBlend.ALPHA, tip, hilt, 0, 1, 1.0f * P, VfxVertexBuffer.withAlpha(0xFF1A1226, fade));
        DreamPaintLayer.strokePart(buf, ctx, SWORD, VfxBlend.ADD, tip, hilt, 0, 1, 1.05f * P, VfxVertexBuffer.withAlpha(VIOLET, 0.55f * fade));
        if (fall >= 1) {
            float s = Mth.clamp((age - 8) / 6f, 0, 1);
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.05f, 0)), 0.2f + 1.6f * s * P, 0.5f + 1.8f * s * P, ctx.seg(16, 10), 3, 0,
                    VfxVertexBuffer.withAlpha(VIOLET, (1 - s) * fade));
            VfxBloom.glow(ctx, buf, new Vector3f(ground).add(0, 0.3f, 0), 1.4f * P, VIOLET, 0.8f * (1 - s));
        }
    }

    // ================================================================ the otherworldly trident
    private void trident(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        if (inst.payload.followEntity() >= 0) {                               // forming in the hand: sparks drawn in, a flare
            Vector3f c = ctx.rel(inst.to(ctx));
            float fade = life(inst, age, 0, 8);
            for (int k = 0; k < 10; k++) {
                float u = Mth.clamp((age - k * 1.2f) / 10f, 0, 1);
                if (u <= 0 || u >= 1) continue;
                Vector3f d = new Vector3f(hash(inst.seed, k, 1) - 0.5f, hash(inst.seed, k, 2) - 0.2f, hash(inst.seed, k, 3) - 0.5f).normalize().mul(2.2f * (1 - u));
                glyph(buf, VfxBlend.ADD, new Vector3f(c).add(d), ctx.camRight, ctx.camUp, 0.14f, k * 5 & 15, VfxVertexBuffer.withAlpha(VIOLET, Mth.sin(u * Mth.PI) * fade));
            }
            VfxBloom.glow(ctx, buf, c, 1.6f * P, VIOLET, (0.4f + 0.4f * Mth.sin(age * 0.6f)) * fade);
            return;
        }
        float fly = VfxAnim.easeOutCubic(Mth.clamp(age / (inst.duration * 0.6f), 0, 1)), fade = life(inst, age, 0, 5);
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) return;
        dir.normalize();
        Vector3f head = new Vector3f(a).lerp(b, fly), tail = new Vector3f(head).sub(new Vector3f(dir).mul(3.2f * P));
        Vector3f wake = new Vector3f(a).lerp(head, 0.35f);
        buf.beam(ctx, GLOW, VfxBlend.ADD, wake, tail, 0.05f, 0.35f * P, 4, 0, VfxVertexBuffer.withAlpha(VIOLET, 0f), VfxVertexBuffer.withAlpha(VIOLET, 0.7f * fade));
        DreamPaintLayer.strokePart(buf, ctx, TRIDENT, VfxBlend.ALPHA, tail, head, 0, 1, 1.3f * P, VfxVertexBuffer.withAlpha(0xFF140E20, fade));
        DreamPaintLayer.strokePart(buf, ctx, TRIDENT, VfxBlend.ADD, tail, head, 0, 1, 1.35f * P, VfxVertexBuffer.withAlpha(VIOLET, 0.6f * fade));
        VfxBloom.glow(ctx, buf, head, 1.1f * P, VIOLET, 0.7f * fade);
        for (int k = 0; k < 5; k++) {                                         // smoke shed along the path
            float u = hash(inst.seed, k, 4) * fly;
            Vector3f p = new Vector3f(a).lerp(b, u).add(0, 0.2f * (age - u * 8) * 0.05f, 0);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, 0.6f + 0.5f * (fly - u), k, VfxVertexBuffer.withAlpha(INK, 0.45f * fade * (1 - (fly - u))));
        }
    }

    // ================================================================ underworld sludge
    private void sludge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx));
        float R = 4 * P * VfxAnim.easeOutCubic(Mth.clamp(t / 0.45f, 0, 1)), fade = life(inst, age, 4, 20);
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.02f, 0));
        buf.plane(POOL, VfxBlend.ALPHA, ground.spin(age * 0.004f), R, VfxVertexBuffer.withAlpha(0xFF0A0612, 0.7f * fade));
        buf.plane(POOL, VfxBlend.ADD, ground.lift(0.01f).spin(age * 0.004f), R, VfxVertexBuffer.withAlpha(VIOLET, 0.16f * fade));   // a violet sheen on the veins
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.01f), Math.max(0, R - 0.5f), R + 0.2f, ctx.seg(28, 14), 6, age * 0.01f,
                VfxVertexBuffer.withAlpha(VIOLET, 0.6f * fade));
        int n = ctx.seg(12, 6);
        for (int k = 0; k < n; k++) {                                         // bubbles swelling and popping
            float ph = (age * 0.04f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI, r = (float) Math.sqrt(hash(inst.seed, k, 3)) * R;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * r, 0.1f + 0.15f * ph, Mth.sin(ang) * r);
            float s = ph < 0.85f ? 0.15f + 0.35f * ph : 0.6f * (1 - (ph - 0.85f) / 0.15f);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, p, s, 0, VfxVertexBuffer.withAlpha(VIOLET, 0.6f * fade));
        }
        for (int k = 0; k < 8; k++) {                                         // black wisps
            float ph = (age * 0.015f + hash(inst.seed, k, 5)) % 1f, ang = hash(inst.seed, k, 6) * Mth.TWO_PI, r = hash(inst.seed, k, 7) * R;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * r, 0.3f + ph * 2.2f, Mth.sin(ang) * r);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, 0.9f + 1.4f * ph, ang + ph, VfxVertexBuffer.withAlpha(INK, 0.55f * Mth.sin(ph * Mth.PI) * fade));
        }
    }
}
