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
 * 1.93.0 Permeation Magic (things that pass through things), the own effects, to the bar of the Time Magic layers: layered textured quads moved from the age alone, pale
 * cyan (0x65D7E8) and white, always see-through: rings, waves and outlines rather than solids. The old shared wiki-magic layer still draws the wiki's generic pages.
 * <ul>
 *   <li>PERMEATION_FX1, CAST (a passing-through strike): 'from' = hand, 'to' = target, power = width (0.6 to 3). A ring of dashes opens at the hand; a ghost streak
 *       runs to the target with a tunnel of rings standing along its path, each ring swelling and fading in a wave that travels with the head, and a ghost figure
 *       rides the head; at 'to' a ripple spreads facing back along the path as the ghost goes through, with bubbles. Default 14 ticks.</li>
 *   <li>PERMEATION_FX2, FIELD (a ghost zone): 'from' = centre on the ground, power = radius. A ring of dashes and a ripple on the floor, a dome of horizontal rings
 *       at four heights turning against each other, curtains of wave streaks round the rim, ghosts drifting across and bubbles rising. In over 8 ticks, out over the
 *       last 12. Default 80.</li>
 *   <li>PERMEATION_FX3, BURST (a door through the air): 'from' = centre, 'to' = direction hint, power = scale. A doorway opens facing the hint and shuts again, ripples
 *       spread over its plane and along the floor, ghosts rise out of it and fade, bubbles scatter. Default 28.</li>
 * </ul>
 * Budget: FX1 about 120 vertices, FX2 about 200, FX3 about 150 (the preview reports it; the limit is 400).
 */
public class PermeationFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RIPPLE = t("permeation_ripple"), RING = t("permeation_ring"), DOOR = t("permeation_door"), GHOST = t("permeation_ghost"),
            LATTICE = t("permeation_lattice"), STREAK = t("permeation_streak"), MOTE = t("permeation_mote"), WAVE = t("permeation_wave");

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.PERMEATION_FX1, VfxShape.PERMEATION_FX2, VfxShape.PERMEATION_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return switch (s) { case PERMEATION_FX1 -> 14; case PERMEATION_FX2 -> 80; default -> 28; }; }

    @Override public int defaultColor(VfxShape s) { return 0xFF65D7E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case PERMEATION_FX1 -> pass(inst, ctx, buf);
            case PERMEATION_FX2 -> zone(inst, ctx, buf);
            case PERMEATION_FX3 -> door(inst, ctx, buf);
            default -> { }
        }
    }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static int col(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

    /** A camera-facing sprite of width 2w and height 2h (the ghost and door textures are tall). */
    private static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, int argb) {
        Vector3f r = new Vector3f(ctx.camRight).mul(w), u = new Vector3f(ctx.camUp).mul(h);
        int k = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u), 0, 0, 1, 1, k, k);
    }

    // ------------------------------------------------------------------ CAST
    private void pass(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), prog = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.6f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 1.0e-3f) dir.set(0, 0, 1); else dir.div(len);
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / (dur * 0.25f), 0, 1)) * (1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.5f) / 0.5f, 0, 1)));
        if (open > 0.02f) {
            VfxBloom.glow(ctx, buf, from, 0.3f * size * open, c, 0.5f);
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(from, dir).spin(age * 0.25f), 0.5f * size * open, col(c, 0.9f * open));
        }
        if (prog < 0.15f) return;
        float e = VfxAnim.easeOutCubic(Mth.clamp((prog - 0.15f) / 0.6f, 0, 1));
        float fade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.75f) / 0.25f, 0, 1));
        Vector3f head = new Vector3f(from).lerp(to, e);
        buf.beam(ctx, STREAK, VfxBlend.ADD, from, head, 0.7f * size, 0.35f * size, 1, 0, col(c, 0.6f * fade), col(hot, 0.85f * fade));
        // the tunnel of rings standing along the path, a swell travelling with the head
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {
            float f = (i + 0.5f) / n;
            if (f > e) break;
            float d = Mth.clamp(1f - Math.abs(f - e) * 3.2f, 0f, 1f);
            Vector3f at = new Vector3f(from).lerp(to, f);
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(at, dir).spin(age * 0.2f + i), size * (0.28f + 0.3f * d), col(i % 2 == 0 ? hot : c, (0.35f + 0.55f * d) * fade));
        }
        // the ghost rides the head
        sprite(buf, ctx, GHOST, VfxBlend.ADD, head, 0.45f * size, 0.9f * size, col(hot, 0.55f * fade));
        VfxBloom.glow(ctx, buf, head, 0.25f * size, c, 0.7f * fade);
        if (prog > 0.7f) {                                                                   // going through at 'to'
            float p = (prog - 0.7f) / 0.3f;
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(to, dir), 1.4f * size * VfxAnim.easeOutCubic(p), col(hot, 0.85f * (1f - p)));
            RandomSource r = inst.random();
            for (int i = 0; i < 4; i++) {
                float a = Mth.TWO_PI * i / 4 + r.nextFloat();
                buf.billboard(ctx, MOTE, VfxBlend.ADD, new Vector3f(to).add(Mth.cos(a) * 0.9f * size * p, Mth.sin(a) * 0.9f * size * p + 0.3f * p, 0), 0.22f * size, 0, col(WHITE, 0.9f * (1f - p)));
            }
        }
    }

    // ------------------------------------------------------------------ FIELD (a ghost zone)
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float R = Math.max(1f, inst.power) * grow;
        if (R <= 0.05f || fade <= 0.01f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, floor, R * 0.9f, c, 0.3f * fade);
        buf.plane(RING, VfxBlend.ADD, floor.spin(age * 0.02f), R, col(c, 0.85f * fade));
        buf.plane(LATTICE, VfxBlend.ADD, floor.lift(0.02f).spin(-age * 0.01f), R * 0.9f, col(c, 0.35f * fade));
        float wave = (age % 36f) / 36f;
        buf.plane(RIPPLE, VfxBlend.ADD, floor.lift(0.04f), R * (0.2f + 0.8f * wave), col(WHITE, 0.6f * fade * (1f - wave)));
        // a dome of horizontal rings at four heights, turning against each other
        float h = Math.min(3.6f, 1.5f + 0.35f * R);
        for (int k = 0; k < 4; k++) {
            float f = (k + 1) / 5f, rad = R * Mth.sqrt(Math.max(0.05f, 1f - f * f));
            VfxPose lvl = VfxPose.ground(new Vector3f(c0).add(0, 0.2f + h * f + 0.1f * Mth.sin(age * 0.07f + k), 0));
            buf.plane(RING, VfxBlend.ADD, lvl.spin(age * 0.04f * (k % 2 == 0 ? 1 : -1)), rad, col(k % 2 == 0 ? hot : c, 0.5f * fade));
        }
        // curtains of wave streaks round the rim
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + age * 0.004f;
            float sway = Mth.sin(age * 0.1f + i * 1.3f) * 0.25f;
            Vector3f base = new Vector3f(Mth.cos(a) * R * 0.97f, 0.05f, Mth.sin(a) * R * 0.97f).add(c0);
            buf.beam(ctx, STREAK, VfxBlend.ADD, base, new Vector3f(base).add(sway, h * 0.9f, sway * 0.5f), 0.7f, 0.5f, 1, 0, col(c, 0.35f * fade), col(hot, 0.15f * fade));
        }
        // ghosts drifting across, bubbles rising
        for (int i = 0; i < 2; i++) {
            float ph = (age * 0.012f + i * 0.5f) % 1f;
            Vector3f g = new Vector3f((ph * 2f - 1f) * R * 0.7f, 0.9f + 0.1f * Mth.sin(age * 0.1f + i), (i == 0 ? 0.3f : -0.3f) * R).add(c0);
            sprite(buf, ctx, GHOST, VfxBlend.ADD, g, 0.4f, 0.8f, col(hot, 0.45f * fade * Mth.sin(ph * Mth.PI)));
        }
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.02f + r.nextFloat()) % 1f, a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.85f;
            buf.billboard(ctx, MOTE, VfxBlend.ADD, new Vector3f(Mth.cos(a) * d, 0.2f + ph * h, Mth.sin(a) * d).add(c0), 0.14f + 0.08f * Mth.sin(ph * Mth.PI), 0, col(WHITE, 0.8f * fade * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ BURST (a door through the air)
    private void door(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), p = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.5f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        Vector3f dir = new Vector3f(ctx.rel(inst.to(ctx))).sub(c0);
        if (dir.lengthSquared() < 1.0e-3f) dir.set(0, 0, 1);
        dir.y = 0;
        if (dir.lengthSquared() < 1.0e-3f) dir.set(0, 0, 1);
        dir.normalize();
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        float out = 1f - VfxAnim.easeInCubic(p);
        RandomSource r = inst.random();

        float open = VfxAnim.easeOutBack(Mth.clamp(p * 4f, 0, 1)) * (1f - VfxAnim.easeInCubic(Mth.clamp((p - 0.65f) / 0.35f, 0, 1)));
        VfxBloom.glow(ctx, buf, c0, 0.8f * size * Math.max(0.2f, open), c, 0.4f * open);
        // the doorway in its plane: right = across, up = world up (a quad of width 2w, height 2h)
        Vector3f right = new Vector3f(dir).cross(new Vector3f(0, 1, 0)).normalize();
        float w = 0.62f * size * open, h = 1.2f * size * open;
        if (open > 0.03f) {
            Vector3f cc = new Vector3f(c0);
            buf.quad(DOOR, VfxBlend.ADD, new Vector3f(cc).sub(new Vector3f(right).mul(w)).sub(0, h, 0), new Vector3f(cc).add(new Vector3f(right).mul(w)).sub(0, h, 0),
                    new Vector3f(cc).add(new Vector3f(right).mul(w)).add(0, h, 0), new Vector3f(cc).sub(new Vector3f(right).mul(w)).add(0, h, 0), 0, 0, 1, 1, col(hot, 0.95f * open), col(hot, 0.95f * open));
            VfxPose plane = new VfxPose(new Vector3f(cc), right, new Vector3f(0, 1, 0), dir);
            buf.plane(RIPPLE, VfxBlend.ADD, plane.lift(0.03f).spin(age * 0.3f), 0.9f * size * open, col(c, 0.55f * open));
        }
        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, -h * 0.98f + 0.04f, 0));
        for (int k = 0; k < 2; k++) {
            float q = Mth.clamp((p - 0.08f * k) / (0.9f - 0.08f * k), 0, 1), s = VfxAnim.easeOutCubic(q);
            buf.plane(RIPPLE, VfxBlend.ADD, floor.lift(0.02f * k), size * (0.3f + 2.0f * s), col(k == 0 ? hot : c, 0.8f * (1f - q)));
        }
        // ghosts rising out of the doorway
        for (int i = 0; i < 3; i++) {
            float q = Mth.clamp(p * 1.4f - i * 0.12f, 0, 1);
            if (q <= 0f) continue;
            Vector3f g = new Vector3f(c0).add(new Vector3f(right).mul((i - 1) * 0.5f * size)).add(0, -h * 0.4f + q * 1.6f * size, 0).add(new Vector3f(dir).mul(q * 0.5f * size));
            sprite(buf, ctx, GHOST, VfxBlend.ADD, g, 0.3f * size, 0.6f * size, col(hot, 0.6f * Mth.sin(q * Mth.PI)));
        }
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + r.nextFloat(), sp = (0.6f + r.nextFloat() * 0.9f) * size * VfxAnim.easeOutCubic(Mth.clamp(p * 1.3f, 0, 1));
            buf.billboard(ctx, MOTE, VfxBlend.ADD, new Vector3f(c0).add(Mth.cos(a) * sp, Mth.sin(a) * sp * 0.8f, Mth.sin(a * 1.7f) * sp * 0.4f), 0.2f * size, 0, col(WHITE, 0.85f * out));
        }
    }
}
