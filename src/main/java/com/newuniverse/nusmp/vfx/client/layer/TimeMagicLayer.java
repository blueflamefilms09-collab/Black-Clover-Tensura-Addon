package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Time Magic (Julius Novachrono), drawn after the anime:
 * <ul>
 *   <li>TIME_STASIS: Chrono Stasis. A pale-blue glass sphere around the target, a white ribbon of Roman numerals orbiting it at
 *       a tilt, four-pointed sparkles twinkling inside. 'from' = sphere centre (usually follows the frozen entity), power = radius.</li>
 *   <li>TIME_CLOCK: Chrono Anastasis. A huge blue-violet clock face with white numerals opens over the area, its arrow hands
 *       sweep backwards and gold light rains from it to the ground. 'from' = ground centre, 'to' = clock centre, power = radius.</li>
 *   <li>TIME_REWIND: Time Reversal / the passive rewind. A small dial under 'from' with hands running backwards and a numeral
 *       ribbon unwinding around it. power = radius.</li>
 *   <li>TIME_ACCEL: Time Acceleration. Two numeral ribbons whirling forward around the caster and a dial whose hands race.</li>
 * </ul>
 */
public class TimeMagicLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation SPHERE = t("time_sphere");
    public static final ResourceLocation BAND = t("time_numeral_band");
    public static final ResourceLocation FACE = t("time_clock_face");
    public static final ResourceLocation NUMERALS = t("time_clock_numerals");
    public static final ResourceLocation HAND = t("time_clock_hand");
    public static final ResourceLocation SPARKLE = t("time_sparkle");
    public static final ResourceLocation STREAK = t("time_streak");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD86B;
    /** Ribbon texture is 16:1 (512 x 32), so one repeat covers 16 ribbon widths. */
    private static final float BAND_ASPECT = 16f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.TIME_STASIS, VfxShape.TIME_CLOCK, VfxShape.TIME_REWIND, VfxShape.TIME_ACCEL); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case TIME_STASIS -> 60;
            case TIME_CLOCK -> 80;
            case TIME_ACCEL -> 40;
            default -> 30;
        };
    }

    @Override public int defaultColor(VfxShape s) { return s == VfxShape.TIME_CLOCK ? 0xFF9C9CF5 : 0xFFB4CCFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.TIME_CLOCK) VfxShake.add(inst.payload.from(), 1.6f, 14);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case TIME_STASIS -> stasis(inst, ctx, buf);
            case TIME_CLOCK -> clock(inst, ctx, buf);
            case TIME_REWIND -> dial(inst, ctx, buf, -1f);
            case TIME_ACCEL -> dial(inst, ctx, buf, 1f);
            default -> { }
        }
    }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    // ------------------------------------------------------------------ Chrono Stasis
    private void stasis(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1));
        float fade = life(inst, age, 0, 8);
        float radius = 0.6f * inst.power * open;
        if (radius <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color;

        // the glass bubble and a faint glow inside it
        buf.billboard(ctx, SPHERE, VfxBlend.ALPHA, c, radius * 2f, 0, VfxVertexBuffer.withAlpha(col, fade));
        VfxBloom.glow(ctx, buf, c, radius * 0.45f, col, 0.35f * fade);

        // the numeral ribbon: tilted 15-35 degrees, slowly precessing, numerals crawling around it
        RandomSource r = inst.random();
        float tilt = 0.26f + r.nextFloat() * 0.35f;
        float yaw = r.nextFloat() * Mth.TWO_PI + age * 0.012f;
        Vector3f axis = new Vector3f(Mth.sin(tilt) * Mth.cos(yaw), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(yaw));
        float bandR = radius * 1.2f, halfW = radius * 0.12f;
        float repeats = Math.max(1, Math.round(Mth.TWO_PI * bandR / (BAND_ASPECT * halfW * 2)));
        hoop(buf, BAND, VfxBlend.ALPHA, VfxPose.facing(c, axis), bandR, halfW, ctx.seg(32, 16), repeats, age * 0.008f,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.7f), fade));

        // sparkles: fixed spots inside the sphere, each twinkling on its own beat
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {
            float u = r.nextFloat() * 2 - 1, phi = r.nextFloat() * Mth.TWO_PI, d = 0.25f + 0.55f * r.nextFloat();
            float s = Mth.sqrt(1 - u * u);
            Vector3f q = new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi)).mul(radius * d).add(c);
            float tw = Mth.sin(age * (0.18f + 0.1f * r.nextFloat()) + r.nextFloat() * Mth.TWO_PI);
            tw = tw * tw * tw;
            if (tw <= 0.05f) continue;
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, q, radius * (0.45f + 0.35f * tw), age * 0.02f, VfxVertexBuffer.withAlpha(WHITE, tw * fade));
        }
    }

    // ------------------------------------------------------------------ Chrono Anastasis
    private void clock(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float grow = VfxAnim.easeOutCubic(Mth.clamp(age / 20f, 0, 1));
        float fade = life(inst, age, 0, 15);
        float big = inst.power * grow;
        if (big <= 0.05f) return;
        Vector3f ground = ctx.rel(inst.from(ctx)), c = ctx.rel(inst.to(ctx));
        int col = inst.color;

        // the face looks down at the people under it, so the numerals read from below
        VfxPose face = VfxPose.facing(c, new Vector3f(0, -1, 0));
        VfxBloom.planeGlow(buf, face.lift(-0.05f), big, col, 0.5f * fade);
        buf.plane(FACE, VfxBlend.ALPHA, face, big, VfxVertexBuffer.withAlpha(col, 0.85f * fade));
        buf.plane(NUMERALS, VfxBlend.ALPHA, face.lift(0.03f), big, VfxVertexBuffer.withAlpha(WHITE, fade));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, face, big * 0.97f, big * 1.05f, ctx.seg(24, 12), 1, 0,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.7f * fade));

        // hands sweep backwards: the minute hand fast, the hour hand a twelfth of that
        float sweep = -age * 0.16f;
        hand(buf, face.lift(0.06f), big * 0.64f, sweep, VfxVertexBuffer.withAlpha(WHITE, fade));
        hand(buf, face.lift(0.05f), big * 0.42f, sweep / 12f + 1.1f, VfxVertexBuffer.withAlpha(WHITE, fade));

        // gold light raining from the face to the ground, each curtain of streaks flickering on its own
        float drop = c.y - ground.y;
        RandomSource r = inst.random();
        int n = ctx.seg(36, 14);
        for (int i = 0; i < n; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * big * 0.92f;
            float delay = 6 + r.nextFloat() * 10, phase = r.nextFloat() * Mth.TWO_PI;
            float width = (0.45f + r.nextFloat() * 0.35f) * Math.max(1f, inst.power / 8f);   // each quad is a curtain of ~7 lines
            float show = VfxAnim.easeOutCubic(Mth.clamp((age - delay) / 10f, 0, 1));
            if (show <= 0) continue;
            float flicker = 0.55f + 0.45f * Mth.sin(age * 0.9f + phase);
            Vector3f top = new Vector3f(Mth.cos(a) * d, -0.1f, Mth.sin(a) * d).add(c);
            Vector3f bottom = new Vector3f(top).add(0, -drop * show, 0);
            int k = VfxVertexBuffer.withAlpha(GOLD, flicker * fade);
            buf.beam(ctx, STREAK, VfxBlend.ADD, top, bottom, width, width, 1, 0, k, k);
        }
        // where the clock's reach meets the ground
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.06f, 0)), big * 0.98f, big, ctx.seg(24, 12), 1, 0,
                VfxVertexBuffer.withAlpha(GOLD, 0.6f * fade));
    }

    // ------------------------------------------------------------------ rewind / acceleration dial
    /** dir -1 = rewind (everything runs backwards), +1 = acceleration (everything races forwards). */
    private void dial(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float dir) {
        float age = inst.ageTicks(ctx.partialTick);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1));
        float fade = life(inst, age, 0, 10);
        float radius = inst.power * open;
        if (radius <= 0.01f) return;
        Vector3f base = ctx.rel(inst.from(ctx));
        int col = inst.color;

        VfxPose floor = VfxPose.ground(new Vector3f(base).add(0, 0.05f, 0));
        buf.plane(FACE, VfxBlend.ALPHA, floor, radius, VfxVertexBuffer.withAlpha(col, 0.6f * fade));
        buf.plane(NUMERALS, VfxBlend.ALPHA, floor.lift(0.01f), radius, VfxVertexBuffer.withAlpha(WHITE, fade));
        float speed = dir > 0 ? 0.55f : 0.35f;
        hand(buf, floor.lift(0.02f), radius * 0.64f, -dir * age * speed, VfxVertexBuffer.withAlpha(WHITE, fade));
        hand(buf, floor.lift(0.025f), radius * 0.42f, -dir * age * speed / 12f + 2f, VfxVertexBuffer.withAlpha(WHITE, fade));

        int band = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.7f), 0.9f * fade);
        int rings = dir > 0 ? 2 : 1;
        for (int k = 0; k < rings; k++) {
            float h = dir > 0 ? 0.45f + k * 1.0f : 0.9f;
            float bandR = radius * (dir > 0 ? 0.85f - k * 0.1f : 0.9f), halfW = 0.11f * Math.max(0.6f, inst.power);
            float wobble = (k == 0 ? 0.12f : -0.12f) * dir;
            Vector3f axis = new Vector3f(Mth.sin(wobble), Mth.cos(wobble), 0);
            float repeats = Math.max(1, Math.round(Mth.TWO_PI * bandR / (BAND_ASPECT * halfW * 2)));
            float scroll = dir * age * (dir > 0 ? 0.06f + k * 0.03f : 0.035f);
            hoop(buf, BAND, VfxBlend.ALPHA, VfxPose.facing(new Vector3f(base).add(0, h, 0), axis), bandR, halfW, ctx.seg(24, 12), repeats, scroll, band);
        }
        VfxBloom.glow(ctx, buf, new Vector3f(base).add(0, 0.2f, 0), radius * 0.4f, col, 0.4f * fade);
    }

    // ------------------------------------------------------------------ primitives
    /**
     * A ribbon standing on a circle (like a ring around a planet): width runs along the pose normal, U wraps {@code repeats}
     * times around and scrolls by {@code uScroll}. Seen from both sides (the vertex buffer disables culling).
     */
    static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments,
                     float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n),
                    u0, 0, u1, 1, col, col);
        }
    }

    /** A clock hand lying on the pose plane, pivot at the origin, pointing at {@code angle}. Texture: pivot at the bottom, tip at the top. */
    static void hand(VfxVertexBuffer buf, VfxPose pose, float length, float angle, int argb) {
        float dx = Mth.cos(angle), dy = Mth.sin(angle);
        float half = length * 0.125f;               // texture is 1:4
        float back = length * 0.05f;                // the pivot disc sits a little past the centre
        float sx = -dy * half, sy = dx * half;
        buf.quad(HAND, VfxBlend.ALPHA,
                pose.point(-dx * back - sx, -dy * back - sy), pose.point(-dx * back + sx, -dy * back + sy),
                pose.point(dx * length + sx, dy * length + sy), pose.point(dx * length - sx, dy * length - sy),
                0, 0, 1, 1, argb, argb);
    }
}
