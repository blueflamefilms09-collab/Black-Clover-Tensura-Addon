package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.lords.LordRules;
import com.newuniverse.nusmp.lords.LordVfxCodes;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: the Lords of the Mortal Sins (the Black Axe of Sloth, the Black Gunblade of Envy, the Black Spear of Vanity, the holy sword Seifort) and the Black Sword Greed's Hunger of the Blade each get pictures of their
 * own, drawn after the same standard as the Time Magic ({@link TimeMagicLayer}): every picture has a build-up, a held moment and a release, a light and a dark partner colour, rings that open and close with an easing, and a
 * silhouette that tells the weapon before you read it. Five shapes ({@code LORD_SLOTH ... LORD_GREED}), the style and the radius are in the power ({@link LordVfxCodes}).
 * <ul>
 *   <li>Sloth (violet, gravity): Titan's Verdict three stomps along a line, each a pressing ring and a crater; Drowsing Chains six chains drawn in to a breathing circle; Lord of Slothful Weight a well of closing
 *       rings with threads hanging motionless; the Weight of Sloth sags rings and threads round the wielder.</li>
 *   <li>Envy (green, eyes): Jealous Volley three tracers from a green eye; Coveting Coil a double helix drawn from the victim to the wielder; Lord of Green-Eyed Envy a ring of eyes opening, each tied to the centre; the
 *       Hollow of Envy wisps leaking out of the chest under a watching eye.</li>
 *   <li>Vanity (magenta, mirrors): Reflected Gaze a mirror pane throwing a cone of light; Fan of Vanity a peacock fan of three feathered rays with eyes at the tips; Lord of Empty Vanity a hall of mirrors; the Cracked
 *       Mirror cracks spreading from the chest.</li>
 *   <li>Seifort (rose gold, light): Pillar of Judgement a column of light on a lit cross; Send Forth a beam with crosses of light; the decree a circle of pillars with feathers; Penance a dim halo and falling ash.</li>
 *   <li>Greed (crimson, maw): the ravenous state teeth closing round the feet; the Half-Starved awakening two jaws closing over a burst; the hit a hungry slash.</li>
 *   <li>All five: a mastery level-up (rings climbing the body and rising motes) and a small hit flash.</li>
 * </ul>
 * The textures are the white masks the repo already owns, tinted here (the Greed look, the eye, chain, feather and cross sets), so no new file exists.
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; each picture stays far below it (the preview counts the worst), every ring, arc and beam checks
 * {@code hasBudget}, {@code ctx.seg} thins the segments by distance and the particle setting, every loop is a constant of at most 12, nothing is kept between frames, and a followed effect whose creature is gone draws nothing.
 * {@code lords.classicVfx} on: the layer draws nothing.
 */
public final class LordLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation GLOW = t("greed_glow"), CORE = t("greed_core"), RING = t("greed_ring"), SLASH = t("greed_slash"), STREAK = t("greed_streak"), GLINT = t("greed_glint"),
            SCALE = t("greed_scale"), VOID = t("greed_void"), FLASH = t("greed_flash"), FEATHER = t("bfx_feather"), CROSS = t("demon_light_cross"), CHAIN = t("chain_band"),
            EYE = t("eye_almond"), SLIT = t("eye_slit");
    private static final ResourceLocation SHARD = VfxTextures.SHARD, SPARK = VfxTextures.SPARK;

    private static final LordRules.Sin[] SINS = LordRules.Sin.values();
    /** The dark partner of each sin's colour (drawn with the alpha blend, so it stays dark on a bright sky) and the pale tint of a core. */
    private static final int[] DARK = {0xFF24103F, 0xFF0A2C14, 0xFF3A0F30, 0xFF5A3A10, 0xFF3A0508};
    private static final int PALE = 0xFFFFF1D6;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.LORD_SLOTH, VfxShape.LORD_ENVY, VfxShape.LORD_VANITY, VfxShape.LORD_HOLY, VfxShape.LORD_GREED); }

    @Override
    public int defaultDuration(VfxShape shape) { return 26; }

    @Override
    public int defaultColor(VfxShape shape) { return 0xFFE8B98A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (LordVfxCodes.classic()) return;
        int sin = LordVfxCodes.sinOf(inst.power), style = LordVfxCodes.styleOf(inst.power);
        boolean heavy = (sin == LordRules.Sin.SLOTH.ordinal() && (style == LordVfxCodes.ART1 || style == LordVfxCodes.DECREE)) || (sin == LordRules.Sin.HOLY.ordinal() && style == LordVfxCodes.DECREE);
        if (heavy) VfxShake.add(inst.payload.from(), style == LordVfxCodes.DECREE ? 1.0f : 0.6f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (LordVfxCodes.classic()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;
        LordRules.Sin sin = SINS[LordVfxCodes.sinOf(inst.power)];
        int style = LordVfxCodes.styleOf(inst.power);
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.0f, LordVfxCodes.radiusOf(inst.power));
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        if (style == LordVfxCodes.LEVEL) { level(inst, ctx, buf, sin, age, a); return; }
        if (style == LordVfxCodes.HIT) { hit(inst, ctx, buf, sin, age, a); return; }
        switch (sin) {
            case SLOTH -> sloth(inst, ctx, buf, style, age, R, a, b);
            case ENVY -> envy(inst, ctx, buf, style, age, R, a, b);
            case VANITY -> vanity(inst, ctx, buf, style, age, R, a, b);
            case HOLY -> holy(inst, ctx, buf, style, age, R, a, b);
            case GREED -> greed(inst, ctx, buf, style, age, R, a);
        }
    }

    // ------------------------------------------------------------------ shared pieces
    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float y = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(x, y);
    }

    private static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0, 1)); }

    private static int hot(int argb, float x) { return VfxVertexBuffer.whiten(argb, x); }

    private static Vector3f lerp(Vector3f a, Vector3f b, float x) { return new Vector3f(a).lerp(b, x); }

    private static Vector3f up(Vector3f c, float y) { return new Vector3f(c).add(0, y, 0); }

    private static Vector3f dirOr(Vector3f a, Vector3f b) {
        Vector3f d = new Vector3f(b).sub(a);
        return d.lengthSquared() < 1.0e-6f ? new Vector3f(0, 0, 1) : d.normalize();
    }

    private static Vector3f sideOf(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0f, 1f, 0f);
        if (s.lengthSquared() < 1.0e-6f) s.set(1, 0, 0);
        return s.normalize();
    }

    private static VfxPose camPose(Vector3f c) { return VfxPose.facing(c, new Vector3f(c).negate()); }

    private static void ring(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float r0, float r1, int segs, float rep, float scroll, int col) {
        buf.ring(tex, bl, VfxPose.ground(up(c, y)), r0, r1, segs, rep, scroll, col);
    }

    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, int col, float alpha) {
        if (alpha <= 0.01f) return;
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, size, 0, al(col, 0.6f * alpha));
        buf.billboard(ctx, CORE, VfxBlend.ADD, c, size * 0.38f, 0, al(hot(col, 0.7f), alpha));
    }

    private static void bolt(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, Vector3f q, float w0, float w1, int col, float alpha, int segs) {
        if (alpha <= 0.01f) return;
        buf.beam(ctx, STREAK, VfxBlend.ADD, p, q, w0, w1, segs, 0, al(col, 0.5f * alpha), al(col, 0.5f * alpha));
        buf.beam(ctx, STREAK, VfxBlend.ADD, p, q, w0 * 0.35f, w1 * 0.35f, 1, 0, al(hot(col, 0.7f), alpha), al(hot(col, 0.7f), alpha));
    }

    private static Vector3f bent(Vector3f p, Vector3f q, Vector3f side, float s, float bend, float age) {
        return lerp(p, q, s).add(new Vector3f(side).mul(bend * Mth.sin(Mth.PI * s) * Mth.sin(age * 0.8f + s * 9f)));
    }

    /** An eye: the almond lens in the colour, a dark slit pupil: 2 quads. {@code open} 0..1. */
    private static void eye(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float open, int col, float alpha, int dark) {
        if (alpha <= 0.01f || open <= 0.01f) return;
        buf.billboard(ctx, EYE, VfxBlend.ADD, c, size * (0.4f + 0.6f * open), 0, al(col, 0.9f * alpha));
        buf.billboard(ctx, SLIT, VfxBlend.ALPHA, c, size * 0.5f * open, 0, al(dark, 0.95f * alpha));
    }

    /** A cut-glass pane standing on its edge facing {@code toward}: a dark body, a lit rim and a glint: 3 quads. */
    private static void pane(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f toward, float half, int col, int dark, float alpha) {
        if (alpha <= 0.01f) return;
        VfxPose pose = VfxPose.facing(c, toward);
        buf.plane(SHARD, VfxBlend.ALPHA, pose, half * 1.05f, al(dark, 0.7f * alpha));
        buf.plane(SHARD, VfxBlend.ADD, pose, half, al(col, 0.55f * alpha));
        buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(c).add(new Vector3f(pose.right()).mul(half * 0.3f)).add(new Vector3f(pose.up()).mul(half * 0.4f)), half * 1.1f, 0.785f, al(hot(col, 0.7f), alpha));
    }

    // ------------------------------------------------------------------ mastery level-up and a plain hit (every sin)
    private void level(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, LordRules.Sin sin, float age, Vector3f a) {
        int col = inst.color, dark = DARK[sin.ordinal()];
        float f = life(inst, age, 2, 14), e = VfxAnim.easeOutCubic(Mth.clamp(age / 22f, 0, 1));
        RandomSource r = inst.random();
        for (int i = 0; i < 3; i++) {                                         // three rings climb the body one after another
            float q = Mth.clamp((age - i * 5f) / 22f, 0, 1);
            if (q <= 0) continue;
            float y = 0.1f + 2.0f * VfxAnim.easeOutCubic(q), w = 0.9f - 0.35f * q, ff = f * (1f - q * 0.7f);
            ring(buf, RING, VfxBlend.ALPHA, a, y, w * 0.82f, w, ctx.seg(10, 6), 2, i * 0.3f, al(dark, 0.8f * ff));
            ring(buf, RING, VfxBlend.ADD, a, y + 0.01f, w * 0.88f, w * 1.02f, ctx.seg(10, 6), 2, i * 0.3f, al(hot(col, 0.3f), 0.85f * ff));
        }
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(up(a, 0.06f)), 1.4f * e + 0.2f, al(col, 0.5f * f * (1f - e * 0.5f)));
        for (int i = 0; i < 6; i++) {                                         // motes rise off the ground
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 0.9f * r.nextFloat(), ph = Mth.frac(age * 0.045f + r.nextFloat());
            buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * rad, 2.4f * ph, Mth.sin(ang) * rad), 0.3f, ang, al(hot(col, 0.5f), f * (1f - ph)));
        }
        glow(ctx, buf, up(a, 1.0f), 1.8f * f, col, 0.5f * f);
    }

    private void hit(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, LordRules.Sin sin, float age, Vector3f a) {
        int col = inst.color, dark = DARK[sin.ordinal()];
        float f = life(inst, age, 1, 6), q = Mth.clamp(age / inst.duration, 0, 1);
        RandomSource r = inst.random();
        switch (sin) {
            case SLOTH -> {                                                   // a violet ring pressing down
                ring(buf, RING, VfxBlend.ALPHA, a, -0.5f, 0.2f + 0.05f * age, 0.45f + 0.18f * age, ctx.seg(10, 6), 2, 0, al(dark, 0.9f * f));
                ring(buf, RING, VfxBlend.ADD, a, -0.48f, 0.3f + 0.05f * age, 0.45f + 0.18f * age, ctx.seg(10, 6), 2, 0, al(col, 0.6f * f));
            }
            case ENVY -> eye(ctx, buf, a, 1.3f, VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1)), col, f, dark);
            case VANITY -> {
                for (int i = 0; i < 5; i++) {
                    float ang = i * 1.26f + r.nextFloat(), d = 0.15f + 0.8f * VfxAnim.easeOutCubic(q);
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * d, Mth.sin(ang) * d, 0), 0.32f, ang, al(col, f));
                }
            }
            case HOLY -> {
                buf.billboard(ctx, CROSS, VfxBlend.ADD, a, 1.2f * VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1)), 0, al(hot(col, 0.5f), 0.9f * f));
                buf.billboard(ctx, FEATHER, VfxBlend.ADD, up(a, -0.2f * age), 0.4f, age * 0.2f, al(PALE, 0.8f * f));
            }
            case GREED -> {
                Vector3f d = new Vector3f(ctx.camRight).mul(0.9f).sub(new Vector3f(ctx.camUp).mul(0.9f * VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1))));
                buf.beam(ctx, STREAK, VfxBlend.ALPHA, new Vector3f(a).sub(d), new Vector3f(a).add(d), 0.3f, 0.3f, 1, 0, al(dark, 0.8f * f), al(dark, 0.8f * f));
                bolt(ctx, buf, new Vector3f(a).sub(d), new Vector3f(a).add(d), 0.2f, 0.2f, col, 0.9f * f, 1);
            }
        }
        glow(ctx, buf, a, 0.9f, col, 0.6f * f);
    }

    // ------------------------------------------------------------------ Sloth: the Black Axe, gravity
    private void sloth(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, float R, Vector3f a, Vector3f b) {
        int col = inst.color, dark = DARK[0];
        RandomSource r = inst.random();
        switch (style) {
            case LordVfxCodes.DECREE -> {                                     // Lord of Slothful Weight: a well of closing rings, threads hanging without a tremor
                float f = life(inst, age, 6, 14), e = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
                buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(up(a, 0.04f)), R * e, al(dark, 0.55f * f));
                for (int i = 0; i < 3; i++) {
                    float q = Mth.frac(age / 40f + i / 3f), rr = R * (1.0f - 0.85f * VfxAnim.easeInCubic(q));
                    ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, rr * 0.86f, rr, ctx.seg(12, 6), 4, i * 0.2f, al(dark, 0.9f * f * (0.4f + 0.6f * q)));
                    ring(buf, RING, VfxBlend.ADD, a, 0.07f, rr * 0.93f, rr * 1.01f, ctx.seg(12, 6), 4, 0, al(col, 0.55f * f * q));
                }
                for (int i = 0; i < 8; i++) {                                 // the threads of weight: they do not sway
                    float ang = i * Mth.TWO_PI / 8f + 0.2f, rad = R * (0.35f + 0.5f * ((i * 5) % 8) / 8f);
                    Vector3f foot = new Vector3f(a).add(Mth.cos(ang) * rad, 0.1f, Mth.sin(ang) * rad);
                    Vector3f top = up(foot, 4.2f * e);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, top, foot, 0.12f, 0.05f, 1, 0, al(col, 0.5f * f), al(col, 0.1f * f));
                    buf.billboard(ctx, GLINT, VfxBlend.ADD, top, 0.5f, 0.785f, al(hot(col, 0.4f), 0.8f * f));
                }
                glow(ctx, buf, up(a, 0.4f), R * 0.5f * e, col, 0.4f * f);
            }
            case LordVfxCodes.ART2 -> {                                       // Drowsing Chains: six chains drawn in to a circle that breathes
                float f = life(inst, age, 3, 9), pull = VfxAnim.easeInOutSine(Mth.clamp(age / 16f, 0, 1));
                float breathe = 1f + 0.06f * Mth.sin(age * 0.35f);
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 0.5f * breathe * 0.85f, R * 0.5f * breathe, ctx.seg(12, 6), 3, 0, al(dark, 0.9f * f));
                ring(buf, RING, VfxBlend.ADD, a, 0.07f, R * 0.5f * breathe * 0.92f, R * 0.5f * breathe * 1.01f, ctx.seg(12, 6), 3, age * 0.01f, al(col, 0.6f * f));
                for (int i = 0; i < 6; i++) {
                    float ang = i * Mth.TWO_PI / 6f;
                    float rr = R * (1.0f - 0.55f * pull);
                    Vector3f out = new Vector3f(a).add(Mth.cos(ang) * rr, 0.25f, Mth.sin(ang) * rr);
                    Vector3f in = up(a, 0.55f);
                    buf.beam(ctx, CHAIN, VfxBlend.ALPHA, out, in, 0.22f, 0.22f, ctx.seg(3, 1), 0, al(dark, 0.95f * f), al(dark, 0.95f * f));
                    buf.beam(ctx, STREAK, VfxBlend.ADD, out, in, 0.12f, 0.12f, 1, 0, al(col, 0.45f * f), al(col, 0.15f * f));
                    buf.billboard(ctx, SCALE, VfxBlend.ALPHA, out, 0.32f, ang, al(dark, 0.9f * f));
                }
                glow(ctx, buf, up(a, 0.5f), 1.4f, col, 0.45f * f);
            }
            case LordVfxCodes.STATE -> {                                      // the Weight of Sloth: rings sag at the feet, threads droop from the shoulders
                float f = life(inst, age, 8, 14);
                for (int i = 0; i < 2; i++) {
                    float rr = 0.7f + 0.25f * i + 0.05f * Mth.sin(age * 0.2f + i);
                    ring(buf, RING, VfxBlend.ALPHA, a, 0.04f + 0.02f * i, rr * 0.8f, rr, ctx.seg(10, 6), 3, 0, al(dark, 0.9f * f));
                    ring(buf, RING, VfxBlend.ADD, a, 0.06f + 0.02f * i, rr * 0.9f, rr * 1.02f, ctx.seg(10, 6), 3, 0, al(col, 0.5f * f));
                }
                for (int i = 0; i < 4; i++) {
                    float ang = i * Mth.HALF_PI + 0.4f, sag = VfxAnim.easeOutCubic(Mth.clamp((age - i) / 14f, 0, 1));
                    Vector3f top = new Vector3f(a).add(Mth.cos(ang) * 0.4f, 1.6f, Mth.sin(ang) * 0.4f);
                    Vector3f end = new Vector3f(top).add(Mth.cos(ang) * 0.3f * sag, -1.5f * sag, Mth.sin(ang) * 0.3f * sag);
                    buf.beam(ctx, STREAK, VfxBlend.ALPHA, top, end, 0.1f, 0.06f, 1, 0, al(dark, 0.8f * f), al(dark, 0.3f * f));
                }
                for (int i = 0; i < 3; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, ph = Mth.frac(age * 0.05f + r.nextFloat());
                    buf.billboard(ctx, SCALE, VfxBlend.ALPHA, new Vector3f(a).add(Mth.cos(ang) * 0.6f, 2.0f * (1f - ph), Mth.sin(ang) * 0.6f), 0.22f, ang, al(dark, 0.8f * f));
                }
            }
            default -> {                                                      // Titan's Verdict (and any other art): three stomps along the line, each a pressing ring and a crater
                float f = life(inst, age, 0, 9);
                for (int i = 0; i < 3; i++) {
                    float s = (i + 1) / 3f, start = i * 3f, q = Mth.clamp((age - start) / 12f, 0, 1);
                    Vector3f at = lerp(a, b, s);
                    float drop = VfxAnim.easeInCubic(Mth.clamp((age - start + 3f) / 3f, 0, 1));
                    if (age >= start - 3f && age < start) buf.beam(ctx, STREAK, VfxBlend.ALPHA, up(at, 6f * (1f - drop) + 1.5f), up(at, 6f * (1f - drop)), 0.3f, 0.3f, 1, 0, al(dark, 0.3f), al(dark, 0.95f));
                    if (q <= 0) continue;
                    float e = VfxAnim.easeOutCubic(q), rr = 0.9f + 2.8f * e;
                    buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(up(at, 0.04f)), 2.0f * (0.5f + 0.5f * e), al(dark, 0.85f * (1f - 0.6f * q) * f));
                    ring(buf, RING, VfxBlend.ALPHA, at, 0.05f, rr * 0.8f, rr, ctx.seg(10, 6), 3, 0, al(dark, 0.9f * (1f - q)));
                    ring(buf, RING, VfxBlend.ADD, at, 0.07f, rr * 0.88f, rr, ctx.seg(10, 6), 3, 0.3f, al(col, 0.7f * (1f - q)));
                    if (q < 0.5f) glow(ctx, buf, up(at, 0.3f), 2.6f * (1f - q * 2f), col, 0.7f * (1f - q * 2f));
                }
                for (int i = 0; i < 4; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, ph = Mth.frac(age * 0.06f + r.nextFloat());
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, new Vector3f(lerp(a, b, 0.3f + 0.2f * i)).add(Mth.cos(ang) * 0.8f, 1.8f * (1f - ph) + 0.1f, Mth.sin(ang) * 0.8f), 0.3f, ang, al(col, 0.7f * f));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Envy: the Black Gunblade, eyes and theft
    private void envy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, float R, Vector3f a, Vector3f b) {
        int col = inst.color, dark = DARK[1];
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b), side = sideOf(dir);
        float dist = a.distance(b);
        switch (style) {
            case LordVfxCodes.DECREE -> {                                     // Lord of Green-Eyed Envy: a ring of eyes opens one after another, each tied to the centre; the green dome swells
                float f = life(inst, age, 4, 14);
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 0.8f * 0.9f, R * 0.8f, ctx.seg(12, 6), 4, 0, al(dark, 0.9f * f));
                ring(buf, RING, VfxBlend.ADD, a, 0.07f, R * 0.8f * 0.95f, R * 0.8f * 1.01f, ctx.seg(12, 6), 4, age * 0.01f, al(col, 0.6f * f));
                for (int i = 0; i < 8; i++) {
                    float ang = i * Mth.TWO_PI / 8f, open = VfxAnim.easeOutBack(Mth.clamp((age - i * 2f) / 5f, 0, 1));
                    Vector3f at = new Vector3f(a).add(Mth.cos(ang) * R * 0.8f, 1.4f + 0.3f * Mth.sin(age * 0.2f + i), Mth.sin(ang) * R * 0.8f);
                    eye(ctx, buf, at, 2.8f, open, col, f, dark);
                    if (open > 0.5f) buf.beam(ctx, STREAK, VfxBlend.ADD, at, up(a, 1.0f), 0.1f, 0.04f, 1, 0, al(col, 0.45f * f), al(col, 0.1f * f));
                }
                glow(ctx, buf, up(a, 0.8f), R * 0.9f * VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1)), col, 0.35f * f);
            }
            case LordVfxCodes.ART2 -> {                                       // Coveting Coil: two strands wind from the victim to the wielder, drawn along the line
                float f = life(inst, age, 2, 8), reach = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
                int pieces = ctx.seg(8, 4);
                for (int strand = 0; strand < 2; strand++) {
                    Vector3f prev = a;
                    for (int i = 1; i <= pieces; i++) {
                        float s = (float) i / pieces * reach, ang = s * 14f + age * 0.5f + strand * Mth.PI, rad = 0.45f * Mth.sin(Mth.PI * s / Math.max(0.01f, reach));
                        Vector3f p = lerp(a, b, s).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(0, Mth.sin(ang) * rad, 0);
                        buf.beam(ctx, STREAK, VfxBlend.ADD, prev, p, 0.2f, 0.2f, 1, 0, al(col, 0.7f * f), al(col, 0.7f * f));
                        prev = p;
                    }
                }
                eye(ctx, buf, b, 1.6f, VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1)), col, f, dark);
                for (int i = 0; i < 3; i++) {
                    float ph = Mth.frac(age * 0.08f + i / 3f);
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, lerp(b, a, ph), 0.3f, ph * 6f, al(hot(col, 0.5f), f));
                }
                glow(ctx, buf, a, 0.9f, col, 0.5f * f);
            }
            case LordVfxCodes.STATE -> {                                      // the Hollow of Envy: wisps leak out of the chest under a watching eye
                float f = life(inst, age, 8, 14);
                for (int i = 0; i < 4; i++) {
                    float ph = Mth.frac(age * 0.04f + i * 0.25f), ang = i * 1.7f + age * 0.05f;
                    Vector3f at = new Vector3f(a).add(Mth.cos(ang) * 0.35f * ph, 0.5f + 1.7f * ph, Mth.sin(ang) * 0.35f * ph);
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, at, 0.28f * (1f - 0.5f * ph), ang, al(col, 0.8f * f * (1f - ph)));
                }
                eye(ctx, buf, up(a, 2.5f), 0.9f, 0.5f + 0.5f * Mth.sin(age * 0.3f) * Mth.sin(age * 0.3f), col, 0.8f * f, dark);
                ring(buf, RING, VfxBlend.ADD, a, 0.05f, 0.5f, 0.6f, ctx.seg(10, 6), 3, age * 0.02f, al(col, 0.4f * f));
            }
            default -> {                                                      // Jealous Volley: three tracers from a green eye, each bending its own way
                float travel = Mth.clamp(dist / 16f, 2f, 5f), f = life(inst, age, 0, 7);
                for (int k = -1; k <= 1; k++) {
                    Vector3f end = new Vector3f(b).add(new Vector3f(side).mul(k * dist * 0.12f));
                    float headT = Mth.clamp((age - (k + 1)) / travel, 0, 1), tailT = Mth.clamp((age - (k + 1) - travel) / 10f, 0, 1);
                    int pieces = ctx.seg(6, 3);
                    Vector3f prev = bent(a, end, side, tailT, 0.5f * (1f - 0.5f * headT), age);
                    for (int i = 1; i <= pieces && headT - tailT > 0.001f; i++) {
                        Vector3f p = bent(a, end, side, Mth.lerp(i / (float) pieces, tailT, headT), 0.5f * (1f - 0.5f * headT), age);
                        float w = 0.16f + 0.2f * i / pieces;
                        buf.beam(ctx, STREAK, VfxBlend.ADD, prev, p, w * 1.5f, w * 1.5f, 1, 0, al(col, 0.4f * f), al(col, 0.4f * f));
                        buf.beam(ctx, STREAK, VfxBlend.ALPHA, prev, p, w * 0.6f, w * 0.6f, 1, 0, al(dark, 0.5f * f), al(hot(col, 0.6f), 0.95f * f));
                        prev = p;
                    }
                    if (headT >= 1f && age - (k + 1) - travel < 6f) eye(ctx, buf, end, 1.1f, VfxAnim.easeOutCubic(Mth.clamp((age - (k + 1) - travel) / 2f, 0, 1)), col, f, dark);
                }
                eye(ctx, buf, a, 1.8f, VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1)), col, Mth.clamp(1f - age / 12f, 0, 1), dark);
            }
        }
    }

    // ------------------------------------------------------------------ Vanity: the Black Spear, mirrors and a peacock fan
    private void vanity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, float R, Vector3f a, Vector3f b) {
        int col = inst.color, dark = DARK[2];
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b), side = sideOf(dir);
        switch (style) {
            case LordVfxCodes.DECREE -> {                                     // Lord of Empty Vanity: a hall of mirrors, six panes round a dome of light, shards orbiting
                float f = life(inst, age, 5, 14), e = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
                ring(buf, RING, VfxBlend.ADD, a, 0.05f, R * 0.7f * 0.94f, R * 0.7f * 1.01f, ctx.seg(12, 6), 4, 0, al(col, 0.55f * f));
                for (int i = 0; i < 6; i++) {
                    float ang = i * Mth.TWO_PI / 6f + age * 0.01f;
                    Vector3f at = new Vector3f(a).add(Mth.cos(ang) * R * 0.7f * e, 1.7f, Mth.sin(ang) * R * 0.7f * e);
                    pane(ctx, buf, at, new Vector3f(a).sub(at).add(0, 0.0001f, 0), 1.2f * e, col, dark, f);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, at, up(a, 1.2f), 0.09f, 0.04f, 1, 0, al(col, 0.35f * f), al(col, 0.08f * f));
                }
                for (int i = 0; i < 6; i++) {
                    float ang = age * 0.08f + i * 1.05f;
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * R * 0.35f, 1.0f + 0.7f * Mth.sin(ang * 2f), Mth.sin(ang) * R * 0.35f), 0.34f, ang, al(hot(col, 0.5f), f));
                }
                glow(ctx, buf, up(a, 1.2f), R * 0.9f * e, col, 0.4f * f);
            }
            case LordVfxCodes.ART2 -> {                                       // Fan of Vanity: three feathered rays fan out, an eye on each tip
                float f = life(inst, age, 1, 9), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
                for (int k = -1; k <= 1; k++) {
                    Vector3f end = new Vector3f(a).add(new Vector3f(dir).mul(dirLen(a, b) * grow)).add(new Vector3f(side).mul(k * dirLen(a, b) * 0.28f * grow));
                    bolt(ctx, buf, a, end, 0.16f, 0.3f, col, 0.8f * f, 1);
                    for (int i = 1; i <= 3; i++) buf.billboard(ctx, FEATHER, VfxBlend.ADD, lerp(a, end, i / 4f), 0.5f, k * 0.5f + i, al(hot(col, 0.4f), 0.7f * f));
                    eye(ctx, buf, end, 1.0f, VfxAnim.easeOutBack(Mth.clamp((age - 3f) / 4f, 0, 1)), col, f, dark);
                }
                glow(ctx, buf, a, 1.1f, col, 0.6f * f);
            }
            case LordVfxCodes.STATE -> {                                      // the Cracked Mirror: cracks spread from the chest, a glint pulses in the crack
                float f = life(inst, age, 6, 14), spread = VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));
                Vector3f c = up(a, 1.2f);
                for (int i = 0; i < 6; i++) {
                    float ang = i * 1.05f + 0.3f, len = (0.5f + 0.5f * ((i * 3) % 4) / 3f) * spread;
                    Vector3f mid = new Vector3f(c).add(Mth.cos(ang) * len * 0.5f, Mth.sin(ang) * len * 0.5f + 0.05f, 0.15f * Mth.sin(i * 5f));
                    Vector3f tip = new Vector3f(c).add(Mth.cos(ang + 0.25f) * len, Mth.sin(ang + 0.25f) * len, 0);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, c, mid, 0.07f, 0.04f, 1, 0, al(hot(col, 0.5f), 0.9f * f), al(col, 0.7f * f));
                    buf.beam(ctx, STREAK, VfxBlend.ADD, mid, tip, 0.04f, 0.02f, 1, 0, al(col, 0.7f * f), al(col, 0.2f * f));
                }
                buf.billboard(ctx, GLINT, VfxBlend.ADD, c, 0.8f + 0.2f * Mth.sin(age * 0.3f), age * 0.05f, al(hot(col, 0.7f), 0.8f * f));
            }
            default -> {                                                      // Reflected Gaze: a pane throws a cone of light and shards
                float f = life(inst, age, 2, 10);
                Vector3f at = new Vector3f(a).add(new Vector3f(dir).mul(1.3f));
                pane(ctx, buf, at, dir, 1.0f * VfxAnim.easeOutBack(Mth.clamp(age / 4f, 0, 1)), col, dark, f);
                float len = dirLen(a, b) * VfxAnim.easeOutCubic(Mth.clamp((age - 2f) / 6f, 0, 1));
                for (int k = -2; k <= 2; k++) {
                    Vector3f end = new Vector3f(at).add(new Vector3f(dir).mul(len)).add(new Vector3f(side).mul(k * len * 0.12f));
                    bolt(ctx, buf, at, end, 0.12f, 0.3f, col, 0.55f * f, 1);
                }
                for (int i = 0; i < 5; i++) {
                    float ph = Mth.frac(age * 0.07f + r.nextFloat());
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, lerp(at, new Vector3f(at).add(new Vector3f(dir).mul(len)), ph).add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 1.2f * ph)), 0.28f, ph * 8f, al(col, f * (1f - ph)));
                }
            }
        }
    }

    private static float dirLen(Vector3f a, Vector3f b) { return Mth.clamp(a.distance(b), 3f, 24f); }

    // ------------------------------------------------------------------ Seifort: the holy sword, light
    private void holy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, float R, Vector3f a, Vector3f b) {
        int col = inst.color, dark = DARK[3];
        RandomSource r = inst.random();
        switch (style) {
            case LordVfxCodes.DECREE -> {                                     // the decree: a lit circle, six pillars round a seventh, feathers falling, one wide ring running out
                float f = life(inst, age, 6, 16), e = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
                buf.plane(RING, VfxBlend.ADD, VfxPose.ground(up(a, 0.05f)), R * e, al(col, 0.4f * f));
                buf.plane(CROSS, VfxBlend.ADD, VfxPose.ground(up(a, 0.07f)), R * 0.7f * e, al(hot(col, 0.4f), 0.6f * f));
                for (int i = 0; i < 7; i++) {
                    Vector3f foot = i == 6 ? new Vector3f(a) : new Vector3f(a).add(Mth.cos(i * 1.047f) * R * 0.6f, 0, Mth.sin(i * 1.047f) * R * 0.6f);
                    float grow = VfxAnim.easeOutCubic(Mth.clamp((age - i * 1.5f) / 8f, 0, 1)), w = i == 6 ? 1.1f : 0.6f;
                    buf.beam(ctx, STREAK, VfxBlend.ADD, up(foot, 12f * grow), foot, w, w * 1.3f, 1, 0, al(col, 0.55f * f), al(hot(col, 0.5f), 0.8f * f));
                }
                float q = Mth.clamp((age - 6f) / 22f, 0, 1);
                ring(buf, RING, VfxBlend.ADD, a, 0.1f, R * 1.1f * q * 0.9f, R * 1.1f * q + 0.1f, ctx.seg(12, 6), 3, 0, al(hot(col, 0.4f), 0.7f * (1f - q) * f));
                for (int i = 0; i < 6; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, rad = R * 0.9f * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.04f + r.nextFloat());
                    buf.billboard(ctx, FEATHER, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * rad, 7f * (1f - ph), Mth.sin(ang) * rad), 0.5f, ang + ph * 3f, al(PALE, 0.8f * f));
                }
            }
            case LordVfxCodes.ART2 -> {                                       // Send Forth: a wide beam, a cross of light at four points, feathers shed
                float f = life(inst, age, 1, 9), len = VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1));
                Vector3f end = lerp(a, b, len);
                bolt(ctx, buf, a, end, 0.5f, 0.9f, col, 0.8f * f, 1);
                for (int i = 1; i <= 4; i++) {
                    float s = i / 4f * len;
                    buf.billboard(ctx, CROSS, VfxBlend.ADD, lerp(a, b, s), 1.3f * (0.6f + 0.4f * s), 0, al(hot(col, 0.5f), 0.85f * f));
                    buf.billboard(ctx, FEATHER, VfxBlend.ADD, up(lerp(a, b, s), 0.5f), 0.5f, i * 1.3f + age * 0.1f, al(PALE, 0.75f * f));
                }
                glow(ctx, buf, a, 1.2f, col, 0.6f * f);
                glow(ctx, buf, end, 1.6f, col, 0.6f * f);
            }
            case LordVfxCodes.STATE -> {                                      // Penance: a dim halo over the head, ash feathers falling
                float f = life(inst, age, 8, 14);
                buf.ring(RING, VfxBlend.ADD, VfxPose.ground(up(a, 2.4f)), 0.28f, 0.4f, ctx.seg(10, 6), 2, age * 0.02f, al(col, 0.55f * f));
                buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(up(a, 2.38f)), 0.24f, 0.42f, ctx.seg(10, 6), 2, 0, al(dark, 0.5f * f));
                for (int i = 0; i < 4; i++) {
                    float ph = Mth.frac(age * 0.035f + i * 0.25f), ang = i * 1.6f + age * 0.03f;
                    buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, new Vector3f(a).add(Mth.cos(ang) * 0.5f, 2.2f * (1f - ph), Mth.sin(ang) * 0.5f), 0.34f, ang, al(dark, 0.7f * f));
                }
            }
            default -> {                                                      // Pillar of Judgement: a column of light on a lit cross, rings climbing it, feathers rising
                float f = life(inst, age, 2, 10), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1));
                Vector3f foot = new Vector3f(b);
                buf.plane(CROSS, VfxBlend.ADD, VfxPose.ground(up(foot, 0.06f)), 2.2f * grow, al(hot(col, 0.4f), 0.8f * f));
                buf.plane(RING, VfxBlend.ADD, VfxPose.ground(up(foot, 0.05f)), 2.8f * grow, al(col, 0.45f * f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, up(foot, 14f * grow), foot, 1.4f, 1.8f, 1, 0, al(col, 0.45f * f), al(col, 0.7f * f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, up(foot, 14f * grow), foot, 0.5f, 0.7f, 1, 0, al(hot(col, 0.7f), 0.8f * f), al(PALE, 0.95f * f));
                for (int i = 0; i < 3; i++) {
                    float q = Mth.frac(age / 18f + i / 3f);
                    ring(buf, RING, VfxBlend.ADD, foot, 0.2f + 8f * q, 0.8f, 1.1f, ctx.seg(10, 6), 2, 0, al(hot(col, 0.4f), 0.6f * f * (1f - q)));
                }
                for (int i = 0; i < 5; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, ph = Mth.frac(age * 0.05f + r.nextFloat());
                    buf.billboard(ctx, FEATHER, VfxBlend.ADD, new Vector3f(foot).add(Mth.cos(ang) * 1.2f, 7f * ph, Mth.sin(ang) * 1.2f), 0.4f, ang, al(PALE, 0.8f * f * (1f - ph * 0.5f)));
                }
                glow(ctx, buf, up(foot, 0.4f), 2.4f, col, 0.5f * f);
            }
        }
    }

    // ------------------------------------------------------------------ Greed: the Black Sword's hunger
    private void greed(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, float R, Vector3f a) {
        int col = inst.color, dark = DARK[4];
        RandomSource r = inst.random();
        if (style == LordVfxCodes.HALF) {                                     // Half-Starved awakening: two jaws close over a burst, the ground cracks into a ring
            float f = life(inst, age, 2, 12), close = VfxAnim.easeInCubic(Mth.clamp(age / 12f, 0, 1)), burst = Mth.clamp((age - 12f) / 14f, 0, 1);
            for (int k = -1; k <= 1; k += 2) {                              // each jaw: a row of seven teeth on an arc, closing on the wielder
                float gap = 1.7f * (1f - close) + 0.15f;
                for (int t = 0; t < 7; t++) {
                    float ang = (t - 3) * 0.42f;
                    Vector3f at = new Vector3f(a).add(new Vector3f(ctx.camRight).mul(Mth.sin(ang) * 1.5f)).add(0, 1.2f + k * (gap + 0.25f * Mth.cos(ang)), 0);
                    buf.billboard(ctx, SHARD, VfxBlend.ALPHA, at, 0.62f, k > 0 ? Mth.PI : 0f, al(dark, 0.92f * f));
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, at, 0.5f, k > 0 ? Mth.PI : 0f, al(hot(col, 0.2f), 0.85f * f));
                }
            }
            if (burst > 0f) {
                float e = VfxAnim.easeOutCubic(burst);
                buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(up(a, 0.04f)), 0.4f + R * 0.5f * e, al(dark, 0.8f * (1f - burst * 0.5f) * f));
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 0.5f * e * 0.8f, R * 0.5f * e + 0.1f, ctx.seg(12, 6), 3, 0, al(dark, 0.9f * (1f - burst)));
                ring(buf, RING, VfxBlend.ADD, a, 0.07f, R * 0.5f * e * 0.9f, R * 0.5f * e + 0.1f, ctx.seg(12, 6), 3, 0.3f, al(col, 0.8f * (1f - burst)));
                glow(ctx, buf, up(a, 1.2f), 2.6f * (1f - burst), col, 0.8f * (1f - burst));
            }
            for (int i = 0; i < 6; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, ph = Mth.frac(age * 0.05f + r.nextFloat());
                buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * 1.6f * (1f - ph), 0.2f + 2.2f * ph, Mth.sin(ang) * 1.6f * (1f - ph)), 0.26f, ang, al(hot(col, 0.4f), f * (1f - ph * 0.5f)));
            }
            return;
        }
        float f = life(inst, age, 8, 14);                                     // the ravenous state: eight teeth close in round the feet, the ring pulses with the hunger
        float hunger = 0.5f + 0.5f * Mth.sin(age * 0.25f);
        ring(buf, RING, VfxBlend.ALPHA, a, 0.04f, 0.75f, 0.95f, ctx.seg(10, 6), 3, 0, al(dark, 0.9f * f));
        ring(buf, RING, VfxBlend.ADD, a, 0.06f, 0.8f, 0.92f, ctx.seg(10, 6), 3, age * 0.02f, al(col, (0.4f + 0.3f * hunger) * f));
        for (int i = 0; i < 8; i++) {
            float ang = i * Mth.TWO_PI / 8f, rr = 1.2f - 0.35f * hunger;
            Vector3f at = new Vector3f(a).add(Mth.cos(ang) * rr, 0.25f, Mth.sin(ang) * rr);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, at, 0.36f, ang + Mth.HALF_PI, al(dark, 0.9f * f));
            buf.billboard(ctx, SHARD, VfxBlend.ADD, at, 0.28f, ang + Mth.HALF_PI, al(col, 0.7f * f));
        }
        glow(ctx, buf, up(a, 0.3f), 1.4f, col, (0.25f + 0.2f * hunger) * f);
    }
}
