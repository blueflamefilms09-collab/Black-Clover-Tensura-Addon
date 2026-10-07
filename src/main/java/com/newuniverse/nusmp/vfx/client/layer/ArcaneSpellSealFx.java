package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.56: the extra layers of SEAL_CHAINS and SEAL_TRINITY (amped up: counter-rotating rune rings, a hot core, more chains and seal circles
 * converging, a crystallising flash, glints). Textures: tools/gen_seal_textures.py (seal_*). Called from the end of the two shape's methods.
 */
final class ArcaneSpellSealFx {
    private ArcaneSpellSealFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }
    static final ResourceLocation RINGS = t("seal_rings"), STAR = t("seal_star"), BAND = t("seal_band"), LOCK = t("seal_lock"), GLINT = t("seal_glint"),
            CRYSTAL = t("seal_crystal"), BURST = t("seal_burst"), STREAK = t("seal_streak");

    /** A camera-facing quad of half size hw x hh, texture up turned by ang. */
    private static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, Vector3f c, float hw, float hh, float ang, int argb) {
        float co = Mth.cos(ang), si = Mth.sin(ang);
        Vector3f r = new Vector3f(ctx.camRight).mul(co).add(new Vector3f(ctx.camUp).mul(si)).mul(hw);
        Vector3f u = new Vector3f(ctx.camUp).mul(co).sub(new Vector3f(ctx.camRight).mul(si)).mul(hh);
        int col = VfxBlend.ADD.grade(argb, 1f);
        buf.quad(tex, VfxBlend.ADD, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, col, col);
    }

    /** Seal chains: c = the target (camera relative). */
    static void chainsExtra(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, float p, float fade, float close, int col, int light) {
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.9f * p + 0.07f, 0));
        // two more rune rings, counter-rotating under the first
        buf.plane(RINGS, VfxBlend.ADD, ground.spin(-age * 0.05f), 1.25f * p, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        buf.plane(STAR, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.08f), 0.8f * p, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        // a rune band turning round the target
        hoop(buf, BAND, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.2f * p, 0)), (2.3f - 1.4f * close) * p, 0.22f * p, ctx.seg(12, 8), 5, -age * 0.02f,
                VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        // a fourth chain
        Vector3f axis = new Vector3f(Mth.sin(1.1f) * Mth.cos(age * 0.015f + 4.2f), Mth.cos(1.1f), Mth.sin(1.1f) * Mth.sin(age * 0.015f + 4.2f));
        hoop(buf, ArcaneSpellLayer.CHAIN, VfxBlend.ALPHA, VfxPose.facing(new Vector3f(c).add(0, 0.35f * p, 0), axis), (2.4f - 1.7f * close) * p, 0.1f * p, ctx.seg(10, 8), 4,
                -age * 0.025f, VfxVertexBuffer.withAlpha(light, fade));
        // the hot core, and the padlock closing when the chains are tight
        float tight = Mth.clamp((age - 6f) / 8f, 0, 1);
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.9f + 0.5f * Mth.sin(age * 0.3f)) * p, age * 0.05f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.8f), 0.55f * fade));
        buf.billboard(ctx, LOCK, VfxBlend.ADD, c, (2.2f - 1.4f * VfxAnim.easeOutBack(tight)) * p, 0f,
                VfxVertexBuffer.withAlpha(WHITE, fade * tight * (1 - Mth.clamp((age - 22f) / 10f, 0, 1)) * 0.85f));
        // glints sliding along the chains
        for (int i = 0; i < 4; i++) {
            float ang = age * 0.04f + i * Mth.HALF_PI, r = (2.0f - 1.3f * close) * p, hgt = Mth.sin(age * 0.07f + i * 2f) * 0.5f * p;
            buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * r, hgt, Mth.sin(ang) * r), 0.3f * p, age * 0.15f + i, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
    }

    /** Trinity seal: c = the target, conv = convergence 0..1, burst = crystallise 0..1. */
    static void trinityExtra(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, float p, float fade, float conv, float burst, int col, int light) {
        int hot = VfxVertexBuffer.whiten(col, 0.85f);
        for (int k = 0; k < 3; k++) {
            float a = Mth.TWO_PI * k / 3 + age * 0.04f, rad = 2.4f * p * (1 - conv);
            Vector3f at = new Vector3f(c).add(Mth.cos(a) * rad, 0.6f * p * Mth.sin(conv * Mth.PI), Mth.sin(a) * rad);
            Vector3f face = new Vector3f(c).sub(at);
            if (face.lengthSquared() < 1e-4f) face.set(0, 1, 0);
            // an inner star turning against each seal circle, and a light thread to the centre
            buf.plane(STAR, VfxBlend.ADD, VfxPose.facing(new Vector3f(at).add(new Vector3f(face).normalize().mul(0.04f)), face).spin(-age * 0.14f), 0.55f * p * (1 - 0.4f * conv),
                    VfxVertexBuffer.withAlpha(hot, fade * (1 - burst) * 0.9f));
            streak(buf, ctx, STREAK, VfxBlend.ADD, at, c, 0.16f * p, VfxVertexBuffer.withAlpha(light, fade * (1 - burst) * conv));
        }
        // glints drawn into the centre
        for (int i = 0; i < 6; i++) {
            float ph = (age * 0.03f + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI;
            float r = 2.6f * p * (1 - ph) * (1 - 0.5f * conv);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * r, (hash(inst.seed, i, 3) - 0.5f) * p, Mth.sin(ang) * r), 0.26f * p, age * 0.2f + i,
                    VfxVertexBuffer.withAlpha(WHITE, fade * (1 - burst) * Mth.sin(ph * Mth.PI)));
        }
        // a hot core that swells as they close
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.6f + 1.4f * conv) * p, age * 0.07f, VfxVertexBuffer.withAlpha(hot, fade * 0.6f * (1 - 0.5f * burst)));
        // the crystallising flash: a hard flash, a padlock, crystals flying out
        if (burst > 0) {
            float e = VfxAnim.easeOutCubic(burst), flash = 1 - burst;
            buf.billboard(ctx, BURST, VfxBlend.ADD, c, 4.2f * p * (0.3f + 0.7f * e), age * 0.1f, VfxVertexBuffer.withAlpha(WHITE, Mth.clamp(flash * 1.5f, 0, 1) * fade));
            buf.billboard(ctx, LOCK, VfxBlend.ADD, c, 1.4f * p * (1.6f - 0.6f * e), 0f, VfxVertexBuffer.withAlpha(WHITE, Mth.clamp(burst * 5f, 0, 1) * fade * 0.8f));
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * i / 6 + hash(inst.seed, i, 5) * 0.8f, d = (0.6f + 1.6f * hash(inst.seed, i, 6)) * p * e;
                Vector3f q = new Vector3f(c).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * d)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * d));
                sprite(buf, ctx, CRYSTAL, q, 0.1f * p, 0.2f * p, ang - Mth.HALF_PI + burst * 2f, VfxVertexBuffer.withAlpha(i % 2 == 0 ? light : hot, fade * flash));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(q).mul(1f).add(new Vector3f(ctx.camUp).mul(0.1f)), 0.3f * p, ang, VfxVertexBuffer.withAlpha(WHITE, fade * flash));
            }
        }
    }
}
