package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.sinweapons.SinRules;
import com.newuniverse.nusmp.sinweapons.SinVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: the Berserk of Gluttony weapons used to answer every technique and hit with the same generic particle puff. This layer draws ONE themed picture per weapon and per technique instead
 * (docs/sin_weapons_abilities.md), after the standard of the Black Sword Greed ({@link GreedLayer}): shape + colour + trail of its own. One shape ({@code VfxShape.SIN_WEAPON}), the weapon and the style are in the
 * power ({@link SinVfx}). The textures are the ones the Greed look already owns (white masks, tinted here), so no new file exists.
 * <ul>
 *   <li>Black Axe of Sloth (dark violet): Dead Weight three heavy rings closing and pressing down with motes falling; Stillness a frozen field of motionless hanging threads; Crushing Gravity dark shards falling
 *       into the ground, a crater and a shock ring; hit: a violet ring and falling motes; Sinking Weight: arrows of weight pressing down.</li>
 *   <li>Black Gunblade of Envy (green): Green-Eyed Shot a bending tracer from a green eye at the muzzle to a green eye at the target; Green Eyes four slit-pupil eyes opening along a line; Stolen Life wisps
 *       drawn in to the wielder; hit: an eye flash; Glaring Eyes: an eye over the head; Covet: a wavering thread from the victim to the wielder.</li>
 *   <li>Black Spear of Vanity (magenta): Vain Thrust a thrust ending in a mirror disc that shatters into shards; Reflected Thrust a mirror pane with a thrust each way; Narcissus a framed mirror with orbiting
 *       shards and a cone of light; hit: mirror shards; Vain Shine: a star glint on the wielder.</li>
 *   <li>Black Chakrams (red): Spinning Ring two spinning ring arcs round the wielder; Thrown Rings two spinning rings flying out and back; Binding Embrace three hoops closing in; hit: a red ring flash;
 *       Hungry Rings: threads of life back to the wielder.</li>
 *   <li>Seifort Holy Sword (rose gold): Grand Cross a cross of light (a stem and a crossbar) with feathers; Purge a column of light, rings running out and feathers rising; Holy Ground a lit circle with a
 *       cross on the ground; hit: a cross glint and feathers; Smiting Light: a short column of light.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; each style stays far below it (the worst, counted at full detail by the preview, is
 * listed in the doc) and every ring, arc and beam checks {@code hasBudget}; {@code ctx.seg} thins the segments by distance and the particle setting. Nothing is kept between frames (short-lived pose vectors only,
 * like every layer); every loop is a constant of at most 12. A followed effect whose creature is gone draws nothing. {@code sinweapons.classicVfx} on: the layer draws nothing (the server's small puff stays).
 */
public final class SinWeaponLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName("greed_" + n); }

    private static final ResourceLocation GLOW = t("glow"), CORE = t("core"), RING = t("ring"), SLASH = t("slash"), STREAK = t("streak"), GLINT = t("glint"), FEATHER = t("feather"),
            SCALE = t("scale"), VOID = t("void"), FLASH = t("flash");
    private static final ResourceLocation SHARD = VfxTextures.SHARD, SPARK = VfxTextures.SPARK;

    private static final SinRules.Kind[] KINDS = SinRules.Kind.values();
    /** The dark partner of each weapon's colour (drawn with the alpha blend, so it stays dark on a bright sky) and a pale tint for cores. */
    private static final int SL_DARK = 0xFF24103F, EN_DARK = 0xFF0A2C14, VA_DARK = 0xFF3A0F30, CH_DARK = 0xFF3A0810, HO_PALE = 0xFFFFF1D6, WHITE = 0xFFFFFFFF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SIN_WEAPON); }

    @Override
    public int defaultDuration(VfxShape shape) { return 22; }

    @Override
    public int defaultColor(VfxShape shape) { return 0xFFE8B98A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (SinVfx.classic()) return;
        int style = SinVfx.styleOf(inst.power);
        if (SinVfx.kindOf(inst.power) == SinRules.Kind.SLOTH.ordinal() && (style == SinVfx.TECH || style == SinVfx.ABILITY_B)) VfxShake.add(inst.payload.from(), style == SinVfx.ABILITY_B ? 1.0f : 0.6f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (SinVfx.classic()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;
        SinRules.Kind k = KINDS[SinVfx.kindOf(inst.power)];
        int style = SinVfx.styleOf(inst.power);
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        switch (k) {
            case SLOTH -> sloth(inst, ctx, buf, style, age, a, b);
            case ENVY -> envy(inst, ctx, buf, style, age, a, b);
            case VANITY -> vanity(inst, ctx, buf, style, age, a, b);
            case CHAKRAMS -> chakrams(inst, ctx, buf, style, age, a, b);
            case HOLY -> holy(inst, ctx, buf, style, age, a, b);
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

    private static Vector3f dirOr(Vector3f a, Vector3f b) {
        Vector3f d = new Vector3f(b).sub(a);
        return d.lengthSquared() < 1.0e-6f ? new Vector3f(0, 0, 1) : d.normalize();
    }

    /** A horizontal-ish sideways vector of a direction (never zero). */
    private static Vector3f sideOf(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0f, 1f, 0f);
        if (s.lengthSquared() < 1.0e-6f) s.set(1, 0, 0);
        return s.normalize();
    }

    private static VfxPose camPose(Vector3f c) { return VfxPose.facing(c, new Vector3f(c).negate()); }

    /** A flat ring lying on the ground plane at {@code centre} + {@code y}. */
    private static void ring(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, float y, float r0, float r1, int segs, float rep, float scroll, int col) {
        buf.ring(tex, bl, VfxPose.ground(new Vector3f(centre).add(0, y, 0)), r0, r1, segs, rep, scroll, col);
    }

    /** A soft glow with a bright core: 2 quads. */
    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, int col, float alpha) {
        if (alpha <= 0.01f) return;
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, size, 0, al(col, 0.6f * alpha));
        buf.billboard(ctx, CORE, VfxBlend.ADD, c, size * 0.38f, 0, al(hot(col, 0.7f), alpha));
    }

    /** A glowing ribbon with a bright thin core: 2 quads per segment. */
    private static void bolt(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, Vector3f q, float w0, float w1, int col, float alpha, int segs) {
        if (alpha <= 0.01f) return;
        buf.beam(ctx, STREAK, VfxBlend.ADD, p, q, w0, w1, segs, 0, al(col, 0.5f * alpha), al(col, 0.5f * alpha));
        buf.beam(ctx, STREAK, VfxBlend.ADD, p, q, w0 * 0.35f, w1 * 0.35f, 1, 0, al(hot(col, 0.7f), alpha), al(hot(col, 0.7f), alpha));
    }

    /** A point on the line {@code p} -> {@code q} at {@code s}, bent sideways by {@code bend} blocks along a sine and wobbling with the age (the bending tracer, the wavering thread). */
    private static Vector3f bent(Vector3f p, Vector3f q, Vector3f side, float s, float bend, float age) {
        return lerp(p, q, s).add(new Vector3f(side).mul(bend * Mth.sin(Mth.PI * s) * Mth.sin(age * 0.8f + s * 9f)));
    }

    // ------------------------------------------------------------------ Black Axe of Sloth
    private void sloth(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, Vector3f a, Vector3f b) {
        int col = inst.color;
        float R = (float) SinVfx.reach(SinRules.Kind.SLOTH, style);
        RandomSource r = inst.random();
        switch (style) {
            case SinVfx.TECH -> {                                            // Dead Weight: three heavy rings close in and press down, motes fall
                float f = life(inst, age, 2, 8);
                for (int i = 0; i < 3; i++) {
                    float q = Mth.clamp((age - i * 3f) / 12f, 0, 1);
                    if (q <= 0) continue;
                    float rr = R * (1.05f - 0.8f * VfxAnim.easeInCubic(q));
                    ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, rr * 0.84f, rr, ctx.seg(10, 6), 3, i * 0.2f, al(SL_DARK, 0.9f * f));
                    if (i == 0) ring(buf, RING, VfxBlend.ADD, a, 0.07f, rr * 0.9f, rr * 1.02f, ctx.seg(10, 6), 3, 0.1f, al(col, 0.6f * f));
                }
                buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(a).add(0, 0.04f, 0)), R * 0.85f, al(SL_DARK, 0.5f * f));
                fallingMotes(ctx, buf, a, R, r, age, f, col, ctx.seg(10, 5));
            }
            case SinVfx.ABILITY_A -> {                                       // Stillness: a frozen field, threads hang without a tremor
                float f = life(inst, age, 4, 10);
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 0.92f, R, ctx.seg(12, 6), 4, 0, al(SL_DARK, 0.9f * f));
                ring(buf, RING, VfxBlend.ADD, a, 0.06f, R * 0.5f, R * 0.56f, ctx.seg(10, 6), 3, 0, al(col, 0.5f * f));
                buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(a).add(0, 0.04f, 0)), R * 0.8f, al(SL_DARK, 0.4f * f));
                for (int i = 0; i < 8; i++) {
                    float ang = i * Mth.TWO_PI / 8f;
                    Vector3f foot = new Vector3f(a).add(Mth.cos(ang) * R * 0.88f, 0.1f, Mth.sin(ang) * R * 0.88f);
                    Vector3f top = new Vector3f(foot).add(0, 3.2f * VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1)), 0);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, top, foot, 0.14f, 0.05f, 1, 0, al(col, 0.55f * f), al(col, 0.1f * f));
                    buf.billboard(ctx, GLINT, VfxBlend.ADD, top, 0.55f, 0.785f, al(hot(col, 0.4f), 0.8f * f));
                }
            }
            case SinVfx.ABILITY_B -> {                                       // Crushing Gravity: shards fall into the ground, a crater, a shock ring
                float f = life(inst, age, 0, 8);
                float fall = VfxAnim.easeInCubic(Mth.clamp(age / 5f, 0, 1));
                if (age < 6f) {
                    for (int i = 0; i < 7; i++) {
                        float ang = i * 0.9f, rad = R * (0.12f + 0.3f * (i % 3) / 2f);
                        Vector3f foot = new Vector3f(a).add(Mth.cos(ang) * rad, 0.1f, Mth.sin(ang) * rad);
                        Vector3f tip = new Vector3f(foot).add(0, 7f * (1f - fall), 0);
                        buf.beam(ctx, STREAK, VfxBlend.ALPHA, new Vector3f(tip).add(0, 1.6f, 0), tip, 0.1f, 0.22f, 1, 0, al(SL_DARK, 0.2f), al(SL_DARK, 0.95f));
                        buf.billboard(ctx, SHARD, VfxBlend.ADD, tip, 0.42f, ang, al(col, 0.8f));
                    }
                }
                float q = Mth.clamp((age - 5f) / 10f, 0, 1);
                if (age >= 5f) {
                    float e = VfxAnim.easeOutCubic(q);
                    buf.plane(VOID, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(a).add(0, 0.04f, 0)), R * 0.75f * e + 0.2f, al(SL_DARK, 0.85f * (1f - 0.6f * q) * f));
                    ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 1.1f * e * 0.8f, R * 1.1f * e + 0.1f, ctx.seg(12, 6), 3, 0, al(SL_DARK, 0.9f * (1f - q)));
                    ring(buf, RING, VfxBlend.ADD, a, 0.07f, R * 1.1f * e * 0.88f, R * 1.1f * e + 0.1f, ctx.seg(12, 6), 3, 0.3f, al(col, 0.7f * (1f - q)));
                    if (q < 0.4f) glow(ctx, buf, new Vector3f(a).add(0, 0.3f, 0), R * 0.8f * (1f - q * 2f), col, 0.6f * (1f - q * 2.5f));
                    for (int i = 0; i < 5; i++) {
                        float ang = r.nextFloat() * Mth.TWO_PI;
                        float d = R * 0.9f * e * (0.4f + 0.6f * r.nextFloat());
                        buf.billboard(ctx, SCALE, VfxBlend.ALPHA, new Vector3f(a).add(Mth.cos(ang) * d, 0.2f + 0.8f * q * r.nextFloat(), Mth.sin(ang) * d), 0.3f, ang, al(SL_DARK, 0.8f * (1f - q)));
                    }
                }
            }
            case SinVfx.PASSIVE -> {                                         // Sinking Weight: arrows of weight press the creature down
                float f = life(inst, age, 1, 5);
                for (int i = 0; i < 4; i++) {
                    float ang = i * Mth.HALF_PI + 0.4f;
                    Vector3f tip = new Vector3f(a).add(Mth.cos(ang) * 0.35f, -0.2f - 0.4f * VfxAnim.easeOutCubic(Mth.clamp(age / 4f, 0, 1)), Mth.sin(ang) * 0.35f);
                    buf.beam(ctx, STREAK, VfxBlend.ALPHA, new Vector3f(tip).add(Mth.cos(ang) * 0.3f, 1.1f, Mth.sin(ang) * 0.3f), tip, 0.04f, 0.2f, 1, 0, al(SL_DARK, 0.1f), al(SL_DARK, 0.95f * f));
                }
                ring(buf, RING, VfxBlend.ADD, a, -0.7f, 0.25f, 0.6f + 0.3f * age / 10f, ctx.seg(10, 6), 2, 0, al(col, 0.7f * f));
                glow(ctx, buf, new Vector3f(a).add(0, -0.4f, 0), 0.9f, col, 0.6f * f);
            }
            default -> {                                                     // a plain hit: a violet ring and a few falling motes
                float f = life(inst, age, 1, 5);
                ring(buf, RING, VfxBlend.ALPHA, a, -0.5f, 0.2f + 0.05f * age, 0.45f + 0.18f * age, ctx.seg(10, 6), 2, 0, al(SL_DARK, 0.9f * f));
                ring(buf, RING, VfxBlend.ADD, a, -0.48f, 0.3f + 0.05f * age, 0.45f + 0.18f * age, ctx.seg(10, 6), 2, 0, al(col, 0.6f * f));
                fallingMotes(ctx, buf, new Vector3f(a).add(0, -0.6f, 0), 0.6f, r, age, f, col, 3);
            }
        }
    }

    /** {@code n} heavy motes falling in a disc of radius {@code R}: 1 quad each. */
    private static void fallingMotes(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float R, RandomSource r, float age, float f, int col, int n) {
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.07f + r.nextFloat());
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * rad, 3.2f * (1f - ph) + 0.1f, Mth.sin(ang) * rad);
            buf.billboard(ctx, SCALE, VfxBlend.ALPHA, at, 0.3f, ang + age * 0.1f, al(SL_DARK, 0.9f * f * (1f - ph * 0.4f)));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, at, 0.3f, 0, al(col, 0.35f * f));
        }
    }

    // ------------------------------------------------------------------ Black Gunblade of Envy
    /** A green eye: the lens flare in the colour, a dark slit pupil, 2 quads. {@code open} 0..1. */
    private static void eye(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float open, int col, float alpha) {
        if (alpha <= 0.01f || open <= 0.01f) return;
        buf.billboard(ctx, GLINT, VfxBlend.ADD, c, size * (0.4f + 0.6f * open), 0, al(col, 0.85f * alpha));
        buf.billboard(ctx, CORE, VfxBlend.ALPHA, c, size * 0.2f * open, 0, al(EN_DARK, 0.9f * alpha));
    }

    private void envy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, Vector3f a, Vector3f b) {
        int col = inst.color;
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b), side = sideOf(dir);
        float dist = a.distance(b);
        switch (style) {
            case SinVfx.TECH -> {                                            // Green-Eyed Shot: a tracer that bends, an eye at the muzzle and one at the target
                float travel = Mth.clamp(dist / 16f, 2f, 5f), f = life(inst, age, 0, 7);
                float headT = Mth.clamp(age / travel, 0, 1), tailT = Mth.clamp((age - travel) / 10f, 0, 1);       // the tail waits for the head, then the tracer fades where it flew
                int pieces = ctx.seg(8, 4);
                float bend = 0.55f * (1f - 0.5f * headT);
                Vector3f prev = bent(a, b, side, tailT, bend, age);
                for (int i = 1; i <= pieces && headT - tailT > 0.001f; i++) {
                    Vector3f p = bent(a, b, side, Mth.lerp(i / (float) pieces, tailT, headT), bend, age);
                    float w = 0.2f + 0.3f * i / pieces;
                    buf.beam(ctx, STREAK, VfxBlend.ADD, prev, p, w * 1.6f, w * 1.6f, 1, 0, al(col, 0.4f * f), al(col, 0.4f * f));
                    buf.beam(ctx, STREAK, VfxBlend.ALPHA, prev, p, w * 0.7f, w * 0.7f, 1, 0, al(EN_DARK, 0.5f * f), al(hot(col, 0.6f), 0.95f * f));
                    prev = p;
                }
                if (headT < 1f) glow(ctx, buf, bent(a, b, side, headT, bend, age), 0.7f, col, f);
                eye(ctx, buf, a, 1.7f, VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1)), col, Mth.clamp(1f - age / 12f, 0, 1));
                float ia = age - travel;
                if (ia >= 0) {
                    float q = Mth.clamp(ia / 8f, 0, 1);
                    eye(ctx, buf, b, 2.0f, VfxAnim.easeOutCubic(Mth.clamp(ia / 2f, 0, 1)), col, 1f - q);
                    buf.ring(RING, VfxBlend.ADD, camPose(b), 0.3f + 0.9f * q, 0.45f + 1.1f * q, ctx.seg(12, 6), 3, 0, al(col, 0.8f * (1f - q)));
                    for (int i = 0; i < ctx.seg(4, 2); i++) {
                        float ang = r.nextFloat() * Mth.TWO_PI;
                        buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(b).add(Mth.cos(ang) * q * 1.2f, Mth.sin(ang) * q * 1.2f, 0), 0.25f, ang, al(col, 1f - q));
                    }
                }
            }
            case SinVfx.ABILITY_A -> {                                       // Green Eyes: four eyes open along the line, the line pulses between them
                float f = life(inst, age, 2, 8);
                bolt(ctx, buf, a, b, 0.18f, 0.18f, col, (0.25f + 0.2f * VfxAnim.pulse(age, 2f)) * f, 1);
                for (int i = 0; i < 4; i++) {
                    float s = (i + 1) / 5f, open = VfxAnim.easeOutBack(Mth.clamp((age - i * 2f) / 4f, 0, 1));
                    Vector3f at = lerp(a, b, s).add(0, 0.15f * Mth.sin(age * 0.4f + i), 0);
                    eye(ctx, buf, at, 1.5f + 0.5f * s, Mth.clamp(open, 0, 1.2f), col, f);
                    glow(ctx, buf, at, 0.9f, col, 0.35f * f);
                }
            }
            case SinVfx.ABILITY_B -> {                                       // Stolen Life: wisps are drawn in to the wielder, health flashes at the chest
                float R = (float) SinVfx.reach(SinRules.Kind.ENVY, style), f = life(inst, age, 1, 6);
                Vector3f chest = new Vector3f(a).add(0, 1.1f, 0);
                float rq = Mth.clamp(age / 12f, 0, 1);
                ring(buf, RING, VfxBlend.ADD, a, 0.06f, R * (1f - 0.75f * rq) * 0.85f, R * (1f - 0.75f * rq), ctx.seg(12, 6), 3, 0, al(col, 0.6f * f));
                for (int i = 0; i < 8; i++) {
                    float ang = i * Mth.TWO_PI / 8f + 0.3f, q = Mth.clamp((age - (i % 4) * 1.2f) / 11f, 0, 1);
                    if (q <= 0f || q >= 1f) continue;
                    float rr = R * (1f - VfxAnim.easeInCubic(q)), tail = Math.max(0f, q - 0.28f);
                    float rt = R * (1f - VfxAnim.easeInCubic(tail));
                    Vector3f head = new Vector3f(a).add(Mth.cos(ang + q * 2f) * rr, 0.5f + 0.6f * q, Mth.sin(ang + q * 2f) * rr);
                    Vector3f tl = new Vector3f(a).add(Mth.cos(ang + tail * 2f) * rt, 0.5f + 0.6f * tail, Mth.sin(ang + tail * 2f) * rt);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, tl, head, 0.03f, 0.2f, 1, 0, al(col, 0.1f * f), al(col, 0.9f * f));
                    buf.billboard(ctx, CORE, VfxBlend.ADD, head, 0.22f, 0, al(hot(col, 0.6f), f));
                }
                if (age > 9f) {
                    float q = Mth.clamp((age - 9f) / 8f, 0, 1);
                    glow(ctx, buf, chest, 1.6f * (1f - 0.4f * q), col, 1f - q);
                    buf.ring(RING, VfxBlend.ADD, camPose(chest), 0.3f + 0.9f * q, 0.45f + 1.0f * q, ctx.seg(12, 6), 3, 0, al(col, 0.8f * (1f - q)));
                }
            }
            case SinVfx.PASSIVE -> {                                         // Glaring Eyes: an eye opens over the creature's head and blinks
                float f = life(inst, age, 1, 3);
                Vector3f at = new Vector3f(a).add(0, 0.9f, 0);
                eye(ctx, buf, at, 1.1f, Mth.clamp(Mth.abs(Mth.cos(age * 0.5f)) + 0.2f, 0, 1), col, f);
                glow(ctx, buf, at, 0.8f, col, 0.4f * f);
            }
            case SinVfx.STEAL -> {                                           // Covet: a thread wavers from the victim to the wielder
                float f = life(inst, age, 0, 5), headT = Mth.clamp(age / 6f, 0, 1), tailT = Mth.clamp(age / 6f - 0.4f, 0, 1);
                int pieces = ctx.seg(6, 3);
                Vector3f prev = bent(a, b, side, tailT, 0.4f, age);
                for (int i = 1; i <= pieces && headT - tailT > 0.001f; i++) {
                    Vector3f p = bent(a, b, side, Mth.lerp(i / (float) pieces, tailT, headT), 0.4f, age);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, prev, p, 0.2f, 0.2f, 1, 0, al(col, 0.5f * f), al(col, 0.8f * f));
                    prev = p;
                }
                eye(ctx, buf, a, 1.2f, VfxAnim.easeOutCubic(Mth.clamp(age / 2f, 0, 1)), col, f);
                if (headT >= 1f) glow(ctx, buf, b, 1.0f, col, f);
            }
            default -> {                                                     // a plain hit: an eye flash and a few sparks
                float f = life(inst, age, 0, 6);
                eye(ctx, buf, a, 1.3f, VfxAnim.easeOutCubic(Mth.clamp(age / 2f, 0, 1)), col, f);
                for (int i = 0; i < ctx.seg(4, 2); i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, d = 0.6f * (age / 8f);
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * d, Mth.sin(ang) * d, 0), 0.2f, ang, al(col, f));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Black Spear of Vanity
    /** A mirror shard burst: {@code n} shards flying out of {@code c}, 1 quad each. */
    private static void shards(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, RandomSource r, float q, float reach, int n, int col, float alpha) {
        for (int i = 0; i < n; i++) {
            float u = r.nextFloat() * 2f - 1f, phi = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(1f - u * u), d = reach * VfxAnim.easeOutCubic(q) * (0.5f + 0.5f * r.nextFloat());
            Vector3f at = new Vector3f(c).add(s * Mth.cos(phi) * d, u * d, s * Mth.sin(phi) * d);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, at, 0.34f * (1f - 0.5f * q), phi + q * 5f, al(hot(col, 0.4f), alpha));
        }
    }

    /** A mirror: a frame ring, a faint glass and a few glints on a plane facing {@code normal}. 6 to 8 quads. */
    private static void mirror(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f normal, float rad, float age, int col, float alpha) {
        VfxPose pose = VfxPose.facing(c, normal);
        buf.plane(GLOW, VfxBlend.ADD, pose, rad, al(col, 0.28f * alpha));
        buf.ring(RING, VfxBlend.ALPHA, pose, rad * 0.9f, rad, ctx.seg(12, 6), 4, 0, al(VA_DARK, 0.95f * alpha));
        buf.ring(RING, VfxBlend.ADD, pose, rad * 0.94f, rad * 1.06f, ctx.seg(12, 6), 4, age * 0.02f, al(col, 0.75f * alpha));
        buf.billboard(ctx, GLINT, VfxBlend.ADD, pose.point(-rad * 0.4f, rad * 0.4f), rad * 0.7f, 0.785f, al(WHITE, 0.7f * alpha));
    }

    private void vanity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, Vector3f a, Vector3f b) {
        int col = inst.color;
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b);
        float dist = a.distance(b);
        switch (style) {
            case SinVfx.TECH -> {                                            // Vain Thrust: the thrust ends in a mirror disc that shatters
                float f = life(inst, age, 0, 7), ext = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
                Vector3f head = new Vector3f(dir).mul(dist * ext).add(a), side = sideOf(dir).mul(0.7f);
                bolt(ctx, buf, a, head, 0.5f, 0.35f, col, f, 2);
                buf.beam(ctx, STREAK, VfxBlend.ALPHA, new Vector3f(a).add(side), new Vector3f(head).add(side), 0.2f, 0.14f, 1, 0, al(VA_DARK, 0.5f * f), al(col, 0.5f * f));
                if (age < 6f) mirror(ctx, buf, head, dir, 0.7f * (1f - age / 12f), age, col, f);
                if (age >= 2f) shards(ctx, buf, head, r, Mth.clamp((age - 2f) / 8f, 0, 1), 1.8f, ctx.seg(8, 4), col, 1f - Mth.clamp((age - 2f) / 9f, 0, 1));
            }
            case SinVfx.ABILITY_A -> {                                       // Reflected Thrust: a mirror pane, a thrust each way
                float f = life(inst, age, 1, 7), ext = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
                Vector3f fwd = new Vector3f(dir).mul(dist * ext).add(a), back = new Vector3f(dir).mul(-dist * ext).add(a);
                mirror(ctx, buf, a, dir, 1.0f, age, col, f);
                bolt(ctx, buf, a, fwd, 0.45f, 0.3f, col, f, 1);
                bolt(ctx, buf, a, back, 0.45f, 0.3f, col, 0.75f * f, 1);
                if (age >= 2f) {
                    float q = Mth.clamp((age - 2f) / 8f, 0, 1);
                    shards(ctx, buf, fwd, r, q, 1.2f, ctx.seg(4, 2), col, f);
                    shards(ctx, buf, back, r, q, 1.2f, ctx.seg(4, 2), col, 0.8f * f);
                }
            }
            case SinVfx.ABILITY_B -> {                                       // Narcissus: a framed mirror ahead, shards orbit it, a cone of light flows to it
                float R = (float) SinVfx.reach(SinRules.Kind.VANITY, style), f = life(inst, age, 3, 8), open = VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1));
                Vector3f m = new Vector3f(dir).mul(R * 0.5f).add(a);
                VfxPose pose = VfxPose.facing(m, dir);
                buf.beam(ctx, STREAK, VfxBlend.ADD, a, new Vector3f(dir).mul(R).add(a), 0.3f, R * 0.9f, 1, 0, al(col, 0.14f * f), al(col, 0.0f));
                mirror(ctx, buf, m, dir, 1.5f * Mth.clamp(open, 0, 1.1f), age, col, f);
                buf.ring(RING, VfxBlend.ADD, pose, 0.8f * Mth.clamp(open, 0, 1.1f), 1.0f * Mth.clamp(open, 0, 1.1f), ctx.seg(10, 6), 3, -age * 0.03f, al(hot(col, 0.3f), 0.7f * f));
                for (int i = 0; i < 6; i++) {
                    float ang = age * 0.15f + i * 1.047f;
                    buf.billboard(ctx, SHARD, VfxBlend.ADD, pose.point(Mth.cos(ang) * 1.3f, Mth.sin(ang) * 1.3f), 0.32f, ang, al(hot(col, 0.4f), 0.9f * f));
                }
                buf.billboard(ctx, GLINT, VfxBlend.ADD, m, 1.3f + 0.4f * VfxAnim.pulse(age, 2f), 0, al(WHITE, 0.75f * f));
            }
            case SinVfx.PASSIVE -> {                                         // Vain Shine: the wielder glints, sparkles rise
                float f = life(inst, age, 1, 5), s = 1.3f + 0.4f * VfxAnim.pulse(age, 3f);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, a, s, 0, al(hot(col, 0.5f), 0.9f * f));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, a, s * 0.8f, 0.785f, al(WHITE, 0.8f * f));
                glow(ctx, buf, a, 1.4f, col, 0.4f * f);
                for (int i = 0; i < ctx.seg(5, 3); i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI;
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * 0.5f, 0.1f + age * 0.08f * (0.5f + r.nextFloat()), Mth.sin(ang) * 0.5f), 0.2f, ang, al(col, f));
                }
            }
            default -> {                                                     // a plain hit: mirror shards fly out
                float f = 1f - Mth.clamp(age / 9f, 0, 1);
                glow(ctx, buf, a, 0.9f, col, f);
                shards(ctx, buf, a, r, Mth.clamp(age / 9f, 0, 1), 1.2f, ctx.seg(6, 3), col, f);
            }
        }
    }

    // ------------------------------------------------------------------ Black Chakrams
    /** One spinning ring lying flat at {@code c}, spikes and all: ring + glow, 2 x segments quads. */
    private static void spinRing(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float rad, float age, float spin, int col, float alpha) {
        VfxPose pose = VfxPose.ground(c).spin(spin);
        buf.ring(RING, VfxBlend.ALPHA, pose, rad * 0.62f, rad, ctx.seg(10, 6), 3, age * 0.1f, al(CH_DARK, 0.95f * alpha));
        buf.ring(RING, VfxBlend.ADD, pose, rad * 0.8f, rad * 1.05f, ctx.seg(10, 6), 3, age * 0.1f, al(col, 0.7f * alpha));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, rad * 2.2f, 0, al(col, 0.3f * alpha));
    }

    private void chakrams(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, Vector3f a, Vector3f b) {
        int col = inst.color;
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b);
        float dist = a.distance(b);
        switch (style) {
            case SinVfx.TECH -> {                                            // Spinning Ring: two ring arcs whirl round the wielder
                float R = (float) SinVfx.reach(SinRules.Kind.CHAKRAMS, style), f = life(inst, age, 1, 7);
                VfxPose pose = VfxPose.ground(new Vector3f(a).add(0, 1.1f, 0));
                int segs = ctx.seg(8, 4);
                for (int j = 0; j < 2; j++) {
                    float start = age * 0.9f + j * Mth.PI;
                    buf.arc(SLASH, VfxBlend.ALPHA, pose, R, 1.0f, start, 1.9f, segs, al(CH_DARK, 0.95f * f));
                    buf.arc(SLASH, VfxBlend.ADD, pose, R + 0.1f, 0.8f, start, 1.9f, segs, al(col, 0.8f * f));
                    float ha = start + 1.9f;
                    buf.billboard(ctx, GLOW, VfxBlend.ADD, pose.point(Mth.cos(ha) * R, Mth.sin(ha) * R), 0.8f, 0, al(col, 0.8f * f));
                }
                buf.arc(SLASH, VfxBlend.ADD, pose, R * 0.62f, 0.4f, -age * 1.2f, 1.5f, segs, al(hot(col, 0.3f), 0.6f * f));
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * 0.9f, R, ctx.seg(12, 6), 4, age * 0.04f, al(CH_DARK, 0.6f * f));
            }
            case SinVfx.ABILITY_A -> {                                       // Thrown Rings: two rings fly out spinning and come back
                float f = life(inst, age, 0, 5), q = Mth.clamp(age / 16f, 0, 1), s = Mth.sin(Mth.PI * q);
                Vector3f side = sideOf(dir);
                for (int i = -1; i <= 1; i += 2) {
                    Vector3f off = new Vector3f(side).mul(0.7f * i * (0.4f + s));
                    Vector3f pos = lerp(a, b, s).add(off);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(a).add(off), pos, 0.1f, 0.25f, 1, 0, al(col, 0.05f), al(col, 0.45f * f));
                    spinRing(ctx, buf, pos, 0.95f, age, age * (1.4f * i), col, f);
                }
                if (age > 14f) glow(ctx, buf, a, 1.2f, col, 1f - Mth.clamp((age - 14f) / 5f, 0, 1));
            }
            case SinVfx.ABILITY_B -> {                                       // Binding Embrace: three hoops close in on what they hold
                float R = (float) SinVfx.reach(SinRules.Kind.CHAKRAMS, style), f = life(inst, age, 1, 6);
                for (int i = 0; i < 3; i++) {
                    float q = Mth.clamp((age - i * 2f) / 12f, 0, 1);
                    float rr = 0.5f + (R - 0.5f) * (1f - VfxAnim.easeInOutSine(q));
                    VfxPose pose = VfxPose.ground(new Vector3f(a).add(0, 0.45f + 0.5f * i, 0));
                    Vector3f n = new Vector3f(pose.normal()).mul(0.14f);
                    int segs = ctx.seg(10, 6);
                    for (int j = 0; j < segs; j++) {
                        float a0 = Mth.TWO_PI * j / segs, a1 = Mth.TWO_PI * (j + 1) / segs;
                        Vector3f m0 = pose.point(Mth.cos(a0) * rr, Mth.sin(a0) * rr), m1 = pose.point(Mth.cos(a1) * rr, Mth.sin(a1) * rr);
                        float u0 = age * 0.05f + 4f * j / segs, u1 = age * 0.05f + 4f * (j + 1) / segs;
                        buf.quad(RING, VfxBlend.ALPHA, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0, u1, 1, al(col, 0.9f * f), al(col, 0.9f * f));
                    }
                }
                ring(buf, RING, VfxBlend.ADD, a, 0.06f, R * 0.6f * (1f - 0.7f * Mth.clamp(age / 14f, 0, 1)), R * 0.7f * (1f - 0.7f * Mth.clamp(age / 14f, 0, 1)) + 0.2f, ctx.seg(10, 6), 3, 0, al(col, 0.6f * f));
                if (age > 11f) glow(ctx, buf, new Vector3f(a).add(0, 0.9f, 0), 1.6f, col, 1f - Mth.clamp((age - 11f) / 8f, 0, 1));
            }
            case SinVfx.PASSIVE, SinVfx.STEAL -> {                           // Hungry Rings: threads of life run back to the wielder
                float f = life(inst, age, 0, 4);
                Vector3f side = sideOf(dir);
                for (int i = -1; i <= 1; i += 2) {
                    float headT = Mth.clamp(age / 6f, 0, 1), tailT = Mth.clamp(age / 6f - 0.4f, 0, 1);
                    if (headT - tailT < 0.001f) continue;
                    Vector3f p = bent(a, b, side, tailT, 0.3f * i, 0f), q = bent(a, b, side, headT, 0.3f * i, 0f);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, p, q, 0.12f, 0.2f, 1, 0, al(col, 0.2f * f), al(col, 0.9f * f));
                }
                glow(ctx, buf, a, 0.9f, col, f);
            }
            default -> {                                                     // a plain hit: a red ring flashes round the blow
                float f = 1f - Mth.clamp(age / 8f, 0, 1);
                float rr = 0.4f + 0.8f * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
                VfxPose pose = camPose(a);
                buf.ring(RING, VfxBlend.ALPHA, pose, rr * 0.7f, rr, ctx.seg(12, 6), 3, 0, al(CH_DARK, 0.9f * f));
                buf.ring(RING, VfxBlend.ADD, pose, rr * 0.8f, rr * 1.05f, ctx.seg(12, 6), 3, 0.2f, al(col, 0.9f * f));
                glow(ctx, buf, a, 0.8f, col, f);
                for (int i = 0; i < ctx.seg(3, 2); i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, d = rr * 1.2f;
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * d, Mth.sin(ang) * d, 0), 0.2f, ang, al(col, f));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Seifort Holy Sword
    /** {@code n} feathers drifting: rising ({@code up} > 0) or falling in a disc / round a line, 1 quad each. */
    private static void feathers(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float R, RandomSource r, float age, float f, boolean rising, int col, int n) {
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.045f + r.nextFloat());
            float y = rising ? 3.0f * ph : 3.0f * (1f - ph);
            buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rad, y + 0.1f, Mth.sin(ang) * rad), 0.55f, ang + age * 0.06f, al(col, 0.85f * f * Math.min(1f, 3f * (1f - ph))));
        }
    }

    private void holy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int style, float age, Vector3f a, Vector3f b) {
        int col = inst.color;
        RandomSource r = inst.random();
        Vector3f dir = dirOr(a, b);
        float dist = a.distance(b);
        switch (style) {
            case SinVfx.TECH -> {                                            // Grand Cross: a stem and a crossbar of light, feathers falling along it
                float f = life(inst, age, 0, 9), ext = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
                Vector3f head = new Vector3f(dir).mul(dist * ext).add(a);
                Vector3f right = new Vector3f(dir).cross(0f, 1f, 0f);
                if (right.lengthSquared() < 1.0e-6f) right.set(1, 0, 0);
                right.normalize();
                bolt(ctx, buf, a, head, 1.3f, 1.0f, col, f, 2);
                float barT = Mth.clamp((age - 1.5f) / 2.5f, 0, 1);
                Vector3f cb = new Vector3f(dir).mul(dist * 8f / 12f).add(a);
                if (barT > 0.01f && ext * dist > dist * 8f / 12f) {
                    Vector3f l = new Vector3f(cb).sub(new Vector3f(right).mul(5f * barT)), rr = new Vector3f(cb).add(new Vector3f(right).mul(5f * barT));
                    bolt(ctx, buf, l, rr, 0.9f, 0.9f, col, f, 2);
                    bolt(ctx, buf, new Vector3f(cb).add(0, -2.6f * barT, 0), new Vector3f(cb).add(0, 2.6f * barT, 0), 0.9f, 0.9f, col, f, 1);       // the upright bar: from the wielder the cross reads as a plus
                    buf.billboard(ctx, GLINT, VfxBlend.ADD, cb, 2.6f * barT * (1f - 0.3f * (age / inst.duration)), age * 0.02f, al(hot(col, 0.6f), 0.95f * f));
                }
                glow(ctx, buf, a, 1.3f, col, 0.9f * f);
                feathers(ctx, buf, lerp(a, b, 0.5f), 1.0f, r, age, f, false, col, ctx.seg(6, 3));
            }
            case SinVfx.ABILITY_A -> {                                       // Purge: a column of light, rings run out, feathers rise
                float R = (float) SinVfx.reach(SinRules.Kind.HOLY, style), f = life(inst, age, 2, 9);
                Vector3f top = new Vector3f(a).add(0, 7f * VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1)), 0);
                bolt(ctx, buf, a, top, 1.0f, 0.5f, col, f, 2);
                for (int i = 0; i < 2; i++) {
                    float q = Mth.clamp((age - i * 3f) / 12f, 0, 1);
                    if (q <= 0f || q >= 1f) continue;
                    float rr = R * VfxAnim.easeOutCubic(q);
                    ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, rr * 0.86f, rr + 0.1f, ctx.seg(12, 6), 3, 0, al(col, 0.55f * (1f - q)));
                    ring(buf, RING, VfxBlend.ADD, a, 0.07f, rr * 0.9f, rr + 0.1f, ctx.seg(12, 6), 3, 0.2f, al(hot(col, 0.5f), 0.9f * (1f - q)));
                }
                glow(ctx, buf, new Vector3f(a).add(0, 0.4f, 0), R * 0.8f, col, 0.8f * f);
                feathers(ctx, buf, a, R * 0.85f, r, age, f, true, col, ctx.seg(8, 4));
                for (int i = 0; i < 3; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, d = R * 0.7f * r.nextFloat();
                    buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * d, 0.4f + 2f * r.nextFloat(), Mth.sin(ang) * d), 0.7f, 0, al(HO_PALE, 0.8f * f));
                }
            }
            case SinVfx.ABILITY_B -> {                                       // Holy Ground: a lit circle with a cross on the ground, motes rise, feathers fall
                float R = (float) SinVfx.reach(SinRules.Kind.HOLY, style), f = life(inst, age, 4, 9), open = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
                buf.plane(GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(a).add(0, 0.04f, 0)), R * open, al(col, 0.3f * f));
                ring(buf, RING, VfxBlend.ALPHA, a, 0.05f, R * open * 0.9f, R * open, ctx.seg(12, 6), 4, 0, al(col, 0.6f * f));
                ring(buf, RING, VfxBlend.ADD, a, 0.07f, R * open * 0.7f, R * open * 0.78f, ctx.seg(12, 6), 3, age * 0.02f, al(hot(col, 0.4f), 0.8f * f));
                float arm = R * 0.7f * open;
                bolt(ctx, buf, new Vector3f(a).add(-arm, 0.07f, 0), new Vector3f(a).add(arm, 0.07f, 0), 0.4f, 0.4f, col, f, 1);
                bolt(ctx, buf, new Vector3f(a).add(0, 0.07f, -arm), new Vector3f(a).add(0, 0.07f, arm), 0.4f, 0.4f, col, f, 1);
                for (int i = 0; i < 6; i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI, d = R * 0.85f * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.05f + r.nextFloat());
                    buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * d, 2.6f * ph, Mth.sin(ang) * d), 0.25f, ang, al(hot(col, 0.5f), 0.9f * f * (1f - ph)));
                }
                feathers(ctx, buf, a, R * 0.8f, r, age, f, false, col, ctx.seg(4, 2));
            }
            case SinVfx.PASSIVE -> {                                         // Smiting Light: a short column of light on the undead, motes of fire rise
                float f = life(inst, age, 1, 5);
                Vector3f top = new Vector3f(a).add(0, 1.4f, 0), foot = new Vector3f(a).add(0, -0.9f, 0);
                bolt(ctx, buf, foot, top, 0.7f, 0.3f, col, f, 1);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, a, 1.2f, 0, al(hot(col, 0.6f), 0.9f * f));
                for (int i = 0; i < ctx.seg(3, 2); i++) {
                    float ang = r.nextFloat() * Mth.TWO_PI;
                    buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * 0.3f, -0.5f + age * 0.15f * (0.5f + r.nextFloat()), Mth.sin(ang) * 0.3f), 0.35f, 0, al(col, 0.8f * f));
                }
            }
            default -> {                                                     // a plain hit: a cross glint and two feathers
                float f = 1f - Mth.clamp(age / 9f, 0, 1);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, a, 1.6f * f + 0.3f, 0, al(hot(col, 0.5f), 0.95f * f));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, a, 1.2f * f + 0.2f, Mth.HALF_PI, al(hot(col, 0.5f), 0.85f * f));
                glow(ctx, buf, a, 0.9f, col, 0.6f * f);
                feathers(ctx, buf, new Vector3f(a).add(0, -0.6f, 0), 0.5f, r, age, f, false, col, 2);
            }
        }
    }
}
