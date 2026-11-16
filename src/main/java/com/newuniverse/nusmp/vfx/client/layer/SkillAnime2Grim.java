package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer.*;
import static com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Layer.*;

/**
 * ANIME2_GRIMOIRE for {@link SkillAnime2Layer}: the five grimoire attributes whose casts and impacts were the weakest (the generic magic circle and its burst): Cotton (soft wool clouds), Mirror (flying
 * facets and a hex mirror), Plant (leaves, petals and vines), Sealing (a rune seal, bands and chains), Thread (stitched strands). Each has two phases: the CAST (from = the caster's feet, follows them)
 * and the IMPACT (from = where the spell lands). The scale is the spell's own power argument (castCircle / impact), so a big page draws bigger. Worst case about 50 quads (the preview measures it).
 */
final class SkillAnime2Grim {
    private SkillAnime2Grim() {}

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f at = ctx.rel(inst.from(ctx));
        float s = 0.7f + 0.35f * SkillAnime2Vfx.grimoireScale(inst.power), fade = life(inst, age, 1, 7);
        boolean hit = SkillAnime2Vfx.grimoirePhase(inst.power) == 1;
        switch (SkillAnime2Vfx.grimoireStyle(inst.power)) {
            case 1 -> mirror(inst, ctx, buf, age, at, s, fade, hit);
            case 2 -> plant(inst, ctx, buf, age, at, s, fade, hit);
            case 3 -> sealing(inst, ctx, buf, age, at, s, fade, hit);
            case 4 -> thread(inst, ctx, buf, age, at, s, fade, hit);
            default -> cotton(inst, ctx, buf, age, at, s, fade, hit);
        }
    }

    // ------------------------------------------------------------------ COTTON
    private static void cotton(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float s, float fade, boolean hit) {
        int col = inst.color, pink = 0xFFFFB8D8;
        RandomSource r = inst.random();
        int n = ctx.seg(hit ? 9 : 8, 4);
        if (hit) {                                                                     // a burst of wool: the puffs fly out and settle
            buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 0.5f, 0), 2.6f * s, 0, al(pink, 0.4f * (1 - ramp(age, 0, 10f))));
            for (int i = 0; i < n; i++) {
                float ph = ramp(age, 0, 16f), sp = (0.8f + 1.4f * r.nextFloat()) * s;
                Vector3f d = randomDir(r);
                Vector3f p = new Vector3f(d).mul(sp * VfxAnim.easeOutCubic(ph)).add(0, 0.4f - 0.9f * ph * ph, 0).add(c);
                buf.billboard(ctx, PUFF, VfxBlend.ALPHA, p, (0.55f + 0.4f * r.nextFloat()) * s, i * 0.8f + ph, al(col, (1 - ph * ph) * fade));
            }
            for (int i = 0; i < 3; i++) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(c).add((i - 1) * 0.6f * s, 0.6f + 0.2f * i, 0), 0.5f * s, age * 0.2f + i, al(pink, fade * (1 - ramp(age, 4f, 12f))));
            return;
        }
        float R = 1.1f * s;
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)), R * 0.8f, R * VfxAnim.easeOutBack(ramp(age, 0, 6f)), ctx.seg(14, 7), 3, age * 0.02f, al(pink, 0.6f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 1f, 0), 2.8f * s, 0, al(pink, 0.22f * fade));
        for (int i = 0; i < n; i++) {                                                  // wool clouds rise round the caster, turning slowly
            float ph = Mth.frac(age * 0.045f + i / (float) n), ang = i * 2.4f + ph * 3f, rad = R * (0.7f + 0.3f * Mth.sin(i * 1.7f));
            buf.billboard(ctx, PUFF, VfxBlend.ALPHA, new Vector3f(Mth.cos(ang) * rad, 0.1f + ph * 2.2f, Mth.sin(ang) * rad).add(c), (0.4f + 0.35f * Mth.sin(ph * Mth.PI)) * s, ang, al(col, Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ MIRROR
    private static void mirror(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float s, float fade, boolean hit) {
        int col = inst.color;
        RandomSource r = inst.random();
        int n = ctx.seg(hit ? 9 : 6, 4);
        if (hit) {                                                                     // the mirror shatters: facets burst out and glint
            flash(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), 1.8f * s, age * 0.3f, col, 1 - ramp(age, 0, 8f));
            for (int i = 0; i < n; i++) {
                float ph = ramp(age, 0, 15f);
                Vector3f p = randomDir(r).mul(1.6f * s * VfxAnim.easeOutCubic(ph)).add(0, 0.5f - 0.6f * ph * ph, 0).add(c);
                buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, (0.4f + 0.3f * r.nextFloat()) * s, i * 1.3f + age * 0.3f, al(col, (1 - ph) * fade));
                if (i % 3 == 0) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, 0.55f * s, age * 0.4f, al(WHITE, (1 - ph) * fade));
            }
            buf.ring(RING, VfxBlend.ADD, camPose(new Vector3f(c).add(0, 0.5f, 0)), 0.3f * s + age * 0.14f, 0.5f * s + age * 0.26f, ctx.seg(12, 6), 3, 0, al(col, Math.max(0, 1 - age / 10f) * fade));
            return;
        }
        float R = 1.25f * s;
        buf.plane(HEX, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)).spin(age * 0.03f), R * VfxAnim.easeOutBack(ramp(age, 0, 6f)), al(col, 0.8f * fade));
        buf.plane(HEX, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.08f, 0)).spin(-age * 0.05f), R * 0.8f * VfxAnim.easeOutBack(ramp(age, 0, 6f)), al(WHITE, 0.4f * fade));
        for (int i = 0; i < n; i++) {                                                  // facets orbit the caster at chest height
            float ang = Mth.TWO_PI * i / n + age * 0.14f, rad = R * 0.85f;
            Vector3f p = new Vector3f(Mth.cos(ang) * rad, 1.1f + 0.25f * Mth.sin(age * 0.2f + i * 2f), Mth.sin(ang) * rad).add(c);
            buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, 0.5f * s, ang * 2f, al(col, fade));
            if (((int) (age * 0.4f) + i) % 4 == 0) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, 0.6f * s, age * 0.3f, al(WHITE, fade));
        }
    }

    // ------------------------------------------------------------------ PLANT
    private static void plant(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float s, float fade, boolean hit) {
        int col = inst.color, petal = 0xFFFFB0D0;
        RandomSource r = inst.random();
        int n = ctx.seg(hit ? 7 : 8, 4);
        if (hit) {                                                                     // sprouts leap from the ground, leaves and petals burst
            for (int i = 0; i < n; i++) {
                float f = ramp(age, i * 0.5f, 5f) * Mth.clamp(1 - (age - 10f - i * 0.3f) / 6f, 0, 1);
                if (f <= 0.01f) continue;
                float ang = Mth.TWO_PI * i / n + r.nextFloat(), rad = (0.3f + 0.9f * r.nextFloat()) * s;
                upright(buf, LEAF, VfxBlend.ALPHA, new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(c), 0.55f * s, (0.9f + 0.6f * r.nextFloat()) * s * f, al(col, fade * f));
            }
            for (int i = 0; i < 4; i++) {
                float ph = ramp(age, 0, 14f);
                Vector3f p = randomDir(r).mul(1.4f * s * VfxAnim.easeOutCubic(ph)).add(0, 0.5f + 0.4f * ph, 0).add(c);
                buf.billboard(ctx, PETAL, VfxBlend.ALPHA, p, 0.4f * s, age * 0.2f + i, al(petal, (1 - ph) * fade));
            }
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)), 0.3f * s + age * 0.1f, 0.5f * s + age * 0.2f, ctx.seg(12, 6), 3, 0, al(col, Math.max(0, 1 - age / 11f) * fade));
            return;
        }
        float R = 1.2f * s;
        buf.plane(SIGIL, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)).spin(age * 0.03f), R * VfxAnim.easeOutBack(ramp(age, 0, 6f)), al(col, 0.7f * fade));
        for (int v = 0; v < 2; v++) {                                                  // two vines climb the caster in a spiral
            Vector3f prev = new Vector3f(c);
            float grow = VfxAnim.easeOutCubic(ramp(age, 0, 9f));
            for (int i = 1; i <= 3; i++) {
                float t = i / 3f * grow, ang = t * 7f + v * Mth.PI;
                Vector3f p = new Vector3f(Mth.cos(ang) * 0.75f * s, t * 2.4f, Mth.sin(ang) * 0.75f * s).add(c);
                buf.beam(ctx, STREAK, VfxBlend.ALPHA, prev, p, 0.2f, 0.16f, 1, 0, al(0xFF2E8B3A, 0.9f * fade), al(col, 0.9f * fade));
                prev = p;
            }
        }
        for (int i = 0; i < n; i++) {                                                  // leaves and petals spiral up
            float ph = Mth.frac(age * 0.04f + i / (float) n), ang = ph * 6f + i * 2.2f, rad = (0.6f + 0.3f * Mth.sin(i * 2f)) * s;
            Vector3f p = new Vector3f(Mth.cos(ang) * rad, ph * 2.4f, Mth.sin(ang) * rad).add(c);
            if (i % 3 == 2) buf.billboard(ctx, PETAL, VfxBlend.ALPHA, p, 0.4f * s, ang, al(petal, Mth.sin(ph * Mth.PI) * fade));
            else buf.billboard(ctx, LEAF, VfxBlend.ALPHA, p, 0.55f * s, ang, al(col, Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ SEALING
    private static void sealing(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float s, float fade, boolean hit) {
        int col = inst.color;
        if (hit) {                                                                     // a seal mark slams on the target, chains close in from four sides
            Vector3f mid = new Vector3f(c).add(0, 0.9f, 0);
            float close = VfxAnim.easeInCubic(ramp(age, 0, 5f));
            buf.plane(RUNE, VfxBlend.ALPHA, camPose(mid).spin(age * 0.08f), 1.1f * s * VfxAnim.easeOutBack(ramp(age, 1f, 5f)), al(col, 0.9f * fade));
            buf.plane(RUNE, VfxBlend.ADD, camPose(mid).spin(-age * 0.12f), 0.85f * s * VfxAnim.easeOutBack(ramp(age, 1f, 5f)), al(hot(col, 0.6f), 0.7f * fade));
            for (int i = 0; i < 4; i++) {
                float ang = Mth.HALF_PI * i + 0.4f;
                Vector3f from = new Vector3f(Mth.cos(ang) * 2.6f * s, 0.8f * Mth.sin(ang * 2f), Mth.sin(ang) * 2.6f * s).add(mid);
                buf.beam(ctx, CHAIN, VfxBlend.ALPHA, from, lerp(from, mid, close * 0.85f), 0.24f, 0.24f, 1, 0, al(col, fade), al(col, fade));
            }
            if (age > 4f) flash(ctx, buf, mid, 1.6f * s, age * 0.3f, col, 1 - ramp(age, 4f, 10f));
            return;
        }
        float R = 1.25f * s, open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        Vector3f ground = new Vector3f(c).add(0, 0.06f, 0);
        buf.plane(RUNE, VfxBlend.ALPHA, VfxPose.ground(ground).spin(age * 0.05f), R * open, al(col, 0.9f * fade));
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.02f, 0)).spin(-age * 0.08f), R * 0.7f * open, al(hot(col, 0.5f), 0.6f * fade));
        for (int k = 0; k < 2; k++) {                                                  // two golden bands turn round the body
            VfxPose band = VfxPose.ground(new Vector3f(c).add(0, 0.5f + 0.9f * k, 0));
            hoop(buf, SWIRL, VfxBlend.ALPHA, band, R * (0.75f + 0.1f * k) * open, 0.16f, ctx.seg(14, 8), 2f, (k == 0 ? 1 : -1) * age * 0.03f, al(col, 0.9f * fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 1f, 0), 2.6f * s, 0, al(col, 0.2f * fade));
    }

    // ------------------------------------------------------------------ THREAD
    private static void thread(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float s, float fade, boolean hit) {
        int col = inst.color;
        RandomSource r = inst.random();
        if (hit) {                                                                     // a net: strands from a ring close on the point, a knot flashes
            Vector3f mid = new Vector3f(c).add(0, 0.8f, 0);
            float close = VfxAnim.easeOutCubic(ramp(age, 0, 6f));
            int n = ctx.seg(6, 4);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.4f;
                Vector3f from = new Vector3f(Mth.cos(ang) * 2.2f * s, 0.6f * Mth.sin(ang * 3f), Mth.sin(ang) * 2.2f * s).add(mid);
                buf.beam(ctx, STITCH, VfxBlend.ADD, from, lerp(from, mid, close), 0.18f, 0.18f, 1, 0, al(hot(col, 0.4f), 0.85f * fade), al(col, 0.85f * fade));
            }
            if (age > 3f) flash(ctx, buf, mid, 1.3f * s, age * 0.3f, col, 1 - ramp(age, 3f, 10f));
            buf.billboard(ctx, CORE, VfxBlend.ADD, mid, 0.35f * s, 0, al(WHITE, fade * close));
            return;
        }
        Vector3f hand = new Vector3f(c).add(0, 1.2f, 0);
        int n = ctx.seg(6, 3);
        float grow = VfxAnim.easeOutCubic(ramp(age, 0, 8f));
        for (int i = 0; i < n; i++) {                                                  // strands fan out from the hands and arc over
            float ang = Mth.TWO_PI * i / n + age * 0.03f, len = (2.0f + 0.8f * Mth.sin(i * 2.3f)) * s * grow;
            Vector3f dir = new Vector3f(Mth.cos(ang), 0.5f, Mth.sin(ang)).normalize();
            Vector3f mid = new Vector3f(dir).mul(len * 0.5f).add(0, 0.35f * s, 0).add(hand), end = new Vector3f(dir).mul(len).add(0, -0.2f, 0).add(hand);
            buf.beam(ctx, STITCH, VfxBlend.ADD, hand, mid, 0.16f * s, 0.16f * s, 1, 0, al(hot(col, 0.4f), 0.9f * fade), al(col, 0.9f * fade));
            buf.beam(ctx, STITCH, VfxBlend.ADD, mid, end, 0.16f * s, 0.12f * s, 1, 0, al(col, 0.9f * fade), al(col, 0.5f * fade));
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, end, 0.4f * s, age * 0.3f + i, al(hot(col, 0.6f), fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, hand, 1.3f * s, 0, al(col, 0.35f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)), 0.8f * s, 1.0f * s * grow, ctx.seg(12, 6), 3, age * 0.02f, al(col, 0.5f * fade));
    }
}
