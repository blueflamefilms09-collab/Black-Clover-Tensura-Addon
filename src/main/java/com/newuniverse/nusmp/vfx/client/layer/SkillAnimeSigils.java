package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer.*;

/**
 * SKILL_SIGIL for {@link SkillAnimeLayer}: the signature of each smaller skill, one style per {@link SkillAnimeVfx#SIGIL_STYLES} entry (power = style + 1, radius = the from -> to distance, at least 0.8;
 * from = the feet unless said). Each is a handful of pieces from the layer's kit, far under the 100-quad cap (the preview's worst case is in docs/vfx_remake/skill_anime.md):
 * <ul>
 *   <li>FAITH: a golden halo settles over the head and a pillar of light falls from the sky, sparkles rise (Faith's prayer and bestowal).</li>
 *   <li>GODDESS: a sunburst behind the caster, shafts of light fall round the radius, a halo spreads on the ground (Goddess of Faith).</li>
 *   <li>FORGE: Hephaestus's forge, from = the anvil point: a fountain of hot sparks, two hammer-strike rings, a heat ring on the ground.</li>
 *   <li>MALLET: Uchide no Kozuchi's level boost: three golden rings climb the body growing, golden sparkles rain, a star over the head.</li>
 *   <li>WILL: Immortal / Unbreakable Will: golden flames burst up round the body, a shockwave runs out to the radius.</li>
 *   <li>HELL: Hell Walker / Hell Conqueror: the ground cracks open red under the feet, ink-black hellfire with red hearts round the radius, embers.</li>
 *   <li>GRAVITY: Futsunomitama: a violet seal on the ground, pressure rings fall from above and crush down, dark streaks fall.</li>
 *   <li>FIRE_FORCE: Devil's Footprints / Adolla Burst: flame jets from the feet, a ring of ink-edged flames bursts out to the radius, a shock ring.</li>
 * </ul>
 */
final class SkillAnimeSigils {
    private SkillAnimeSigils() {}

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        float R = Math.max(0.8f, from.distance(to));
        switch (SkillAnimeVfx.sigil(inst.power)) {
            case 1 -> goddess(inst, ctx, buf, age, from, R);
            case 2 -> forge(inst, ctx, buf, age, from, R);
            case 3 -> mallet(inst, ctx, buf, age, from);
            case 4 -> will(inst, ctx, buf, age, from, R);
            case 5 -> hell(inst, ctx, buf, age, from, R);
            case 6 -> gravity(inst, ctx, buf, age, from, R);
            case 7 -> fireForce(inst, ctx, buf, age, from, R);
            default -> faith(inst, ctx, buf, age, from);
        }
    }

    private static void faith(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), settle = VfxAnim.easeOutCubic(ramp(age, 0, 7f));
        Vector3f head = new Vector3f(feet).add(0, 2.15f + 1.4f * (1 - settle), 0);
        buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(feet).add(0, 9f, 0), feet, 2.2f, 1.4f, 1, 0, al(col, 0.15f * fade), al(hot(col, 0.4f), 0.6f * fade * (1 - 0.5f * ramp(age, 6f, 10f))));
        buf.plane(HALO, VfxBlend.ALPHA, VfxPose.ground(head), 0.55f, al(col, fade));
        buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(head), 0.55f, al(hot(col, 0.4f), 0.6f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 1.1f, 0, al(col, 0.5f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), 0.7f, 1.0f, ctx.seg(12, 6), 3, age * 0.03f, al(col, 0.7f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ph = Mth.frac(age * 0.05f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI + ph * 2f, rad = 0.5f + 0.3f * r.nextFloat();
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, ph * 2.4f, Mth.sin(ang) * rad).add(feet), 0.35f, ang, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    private static void goddess(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        Vector3f behind = new Vector3f(feet).add(0, 2.2f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, behind, 4.5f * open, 0, al(col, 0.45f * fade));
        buf.billboard(ctx, RAYS, VfxBlend.ADD, behind, 5f * open, age * 0.01f, al(hot(col, 0.4f), 0.85f * fade));
        buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), R * VfxAnim.easeOutCubic(ramp(age, 0, 10f)), al(col, 0.7f * fade));
        RandomSource r = inst.random();
        int shafts = ctx.seg(8, 4);
        for (int i = 0; i < shafts; i++) {
            float ang = Mth.TWO_PI * i / shafts + r.nextFloat() * 0.4f, start = 2f + i * 1.2f, f = ramp(age, start, 2f) * Mth.clamp(1 - (age - start - 8f) / 4f, 0, 1);
            if (f <= 0.01f) continue;
            Vector3f g = new Vector3f(Mth.cos(ang) * R * 0.7f, 0, Mth.sin(ang) * R * 0.7f).add(feet);
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(g).add(0, 8f, 0), g, 1.0f, 0.7f, 1, 0, al(col, 0.1f * f * fade), al(hot(col, 0.5f), 0.8f * f * fade));
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(g).add(0, 0.3f, 0), 0.8f, ang, al(WHITE, f * fade));
        }
        float rr = 0.5f + R * VfxAnim.easeOutCubic(ramp(age, 0, 12f));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.08f, 0)), rr * 0.88f, rr, ctx.seg(16, 8), 4, 0, al(col, fade * (1 - ramp(age, 4f, 14f))));
    }

    private static void forge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 0, 6);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 2.2f, 0, al(col, 0.7f * fade));
        for (int k = 0; k < 2; k++) {                                                  // two hammer strikes
            float local = age - k * 7f;
            if (local < 0 || local > 8f) continue;
            flash(ctx, buf, c, 1.4f, k * 0.7f, col, 1 - local / 5f);
            float rr = 0.3f + 1.6f * VfxAnim.easeOutCubic(local / 8f);
            buf.ring(RING, VfxBlend.ADD, camPose(c), rr * 0.75f, rr, ctx.seg(12, 6), 3, 0, al(hot(col, 0.4f), 1 - local / 8f));
        }
        RandomSource r = inst.random();
        int sparks = ctx.seg(14, 6);
        for (int i = 0; i < sparks; i++) {                                              // a fountain of sparks on short arcs
            float start = (i % 2) * 7f + r.nextFloat() * 2f, ph = ramp(age, start, 9f);
            if (ph <= 0 || ph >= 1) continue;
            Vector3f v = randomDir(r);
            v.y = Math.abs(v.y) * 1.3f + 0.4f;
            float sp = 2.2f + 1.4f * r.nextFloat();
            float t0 = Math.max(0, ph - 0.2f);
            Vector3f p1 = new Vector3f(v).mul(sp * ph).add(0, -3f * ph * ph, 0).add(c), p0 = new Vector3f(v).mul(sp * t0).add(0, -3f * t0 * t0, 0).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, 0.1f, 0.22f, 1, 0, al(col, 0.7f * fade), al(hot(col, 0.7f), (1 - ph) * fade));
        }
        float rr = Math.min(R, 3f) * VfxAnim.easeOutCubic(ramp(age, 0, 8f));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)), rr * 0.8f, rr, ctx.seg(14, 7), 4, age * 0.03f, al(col, 0.6f * fade));
    }

    private static void mallet(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8);
        int seg = ctx.seg(12, 6);
        for (int k = 0; k < 3; k++) {                                                  // the rings climb the body, growing: the level boost
            float ph = ramp(age, k * 3f, 10f);
            if (ph <= 0 || ph >= 1) continue;
            float rr = 0.6f + 0.9f * ph;
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(feet).add(0, 0.1f + 2.2f * VfxAnim.easeOutCubic(ph), 0)), rr * 0.72f, rr, seg, 3, age * 0.04f, al(col, (1 - ph) * fade));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float ph = Mth.frac(age * 0.045f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI, rad = 1.1f * Mth.sqrt(r.nextFloat());
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, 3.2f * (1 - ph), Mth.sin(ang) * rad).add(feet), 0.4f, age * 0.1f + i, al(hot(col, 0.4f), Mth.sin(ph * Mth.PI) * fade));
        }
        float pop = VfxAnim.easeOutBack(ramp(age, 2f, 5f));
        Vector3f top = new Vector3f(feet).add(0, 2.5f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, top, 1.6f * pop, 0, al(col, 0.6f * fade));
        buf.billboard(ctx, SPARKLE, VfxBlend.ADD, top, 1.5f * pop, age * 0.05f, al(WHITE, fade));
        buf.beam(ctx, STREAK, VfxBlend.ADD, feet, new Vector3f(feet).add(0, 3f, 0), 1.4f, 0.8f, 1, 0, al(col, 0.45f * fade), al(col, 0.1f * fade));
    }

    private static void will(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet, float R) {
        int col = inst.color;
        float fade = life(inst, age, 0, 8), rise = VfxAnim.easeOutBack(ramp(age, 0, 4f));
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // golden flames burst up round the body
            float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.5f, h = (1.8f + 0.6f * Mth.sin(age * 0.6f + i * 2.1f)) * rise * fade;
            Vector3f at = new Vector3f(Mth.cos(ang) * 0.5f, 0, Mth.sin(ang) * 0.5f).add(feet);
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.6f, h, al(col, 0.85f * fade));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.3f, h * 0.65f, al(hot(col, 0.7f), fade));
        }
        Vector3f chest = new Vector3f(feet).add(0, 1.1f, 0);
        flash(ctx, buf, chest, 2.4f, 0.4f, col, 1 - ramp(age, 0, 5f));
        float rr = 0.5f + R * VfxAnim.easeOutCubic(ramp(age, 0, 9f));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.3f, 0)), rr * 0.82f, rr, ctx.seg(18, 9), 4, 0, al(hot(col, 0.2f), fade * (1 - ramp(age, 3f, 12f))));
        buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), 1.4f * rise, al(col, 0.6f * fade));
    }

    private static void hell(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 5f));
        VfxPose ground = VfxPose.ground(new Vector3f(feet).add(0, 0.06f, 0));
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.spin(age * 0.01f), Math.min(R, 6f) * open, al(INK, 0.85f * fade));
        buf.plane(GLOW, VfxBlend.ADD, ground, Math.min(R, 6f) * 1.1f * open, al(col, 0.4f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {                                                  // ink-black hellfire with red hearts
            float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.4f + age * 0.01f, rad = R * (0.45f + 0.35f * r.nextFloat());
            float h = (1.2f + 0.8f * r.nextFloat() + 0.3f * Mth.sin(age * 0.5f + i * 1.3f)) * open * fade;
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(feet);
            upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.7f, h, al(INK, 0.9f * fade));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.35f, h * 0.6f, al(col, fade));
        }
        int embers = ctx.seg(8, 4);
        for (int i = 0; i < embers; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.05f + r.nextFloat());
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, ph * 3f, Mth.sin(ang) * rad).add(feet), 0.16f, 0, al(hot(col, 0.3f), Mth.sin(ph * Mth.PI) * fade));
        }
        buf.ring(RING, VfxBlend.ADD, ground, R * 0.9f * open, R * open, ctx.seg(16, 8), 4, age * 0.02f, al(col, 0.7f * fade));
    }

    private static void gravity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 5f));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(-age * 0.01f), R * open, al(col, 0.75f * fade));
        buf.plane(GLOW, VfxBlend.ALPHA, ground, R * 1.15f * open, al(INK, 0.45f * fade));
        int seg = ctx.seg(14, 7);
        for (int k = 0; k < 3; k++) {                                                  // pressure rings fall from above and crush down
            float ph = Mth.frac(age * 0.045f + k / 3f);
            float y = 5f * (1 - VfxAnim.easeInCubic(ph)), rr = R * (0.55f + 0.45f * ph);
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.1f + y, 0)), rr * 0.85f, rr, seg, 4, 0, al(hot(col, 0.15f), Mth.sin(ph * Mth.PI) * open * fade));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.08f + r.nextFloat());
            Vector3f top = new Vector3f(Mth.cos(ang) * rad, 4.5f * (1 - ph), Mth.sin(ang) * rad).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(top).add(0, 1.4f, 0), top, 0.08f, 0.18f, 1, 0, al(col, 0.1f * fade), al(hot(col, 0.3f), 0.8f * fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 0.5f, 0), R * 0.9f, 0, al(col, 0.3f * fade));
    }

    private static void fireForce(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f feet, float R) {
        int col = inst.color;
        float fade = life(inst, age, 0, 6);
        RandomSource r = inst.random();
        float jet = 1 - ramp(age, 4f, 8f);                                               // the flame jets out of the soles: the devil's footprints
        if (jet > 0.01f) {
            for (int s = -1; s <= 1; s += 2) {
                Vector3f foot = new Vector3f(s * 0.15f, 0.1f, 0).add(feet), down = new Vector3f(foot).add(0, -1.3f * jet, 0);
                buf.beam(ctx, FLAME, VfxBlend.ALPHA, foot, down, 0.75f, 0.75f, 1, 0, al(INK, 0.85f * jet), al(INK, 0.85f * jet));
                buf.beam(ctx, FLAME, VfxBlend.ADD, foot, down, 0.45f, 0.45f, 1, 0, al(hot(col, 0.3f), jet), al(hot(col, 0.3f), jet));
            }
        }
        float out = VfxAnim.easeOutCubic(ramp(age, 0, 8f));
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {                                                  // a ring of ink-edged flames bursts out
            float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.3f, rad = 0.4f + (R - 0.4f) * out, h = (1.3f + 0.6f * r.nextFloat()) * (1 - 0.6f * out) * fade;
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(feet);
            upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.7f, h, al(INK, 0.8f * fade * (1 - out * 0.5f)));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.4f, h * 0.7f, al(col, fade));
        }
        float rr = 0.5f + R * 1.1f * out;
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.2f, 0)), rr * 0.85f, rr, ctx.seg(16, 8), 4, 0, al(col, fade * (1 - ramp(age, 3f, 10f))));
        flash(ctx, buf, new Vector3f(feet).add(0, 0.6f, 0), 2.2f, 0.2f, col, 1 - ramp(age, 0, 5f));
    }
}
