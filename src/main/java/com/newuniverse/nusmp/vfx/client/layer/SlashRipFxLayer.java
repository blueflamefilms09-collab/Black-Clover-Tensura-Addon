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
 * 1.93.0 Slash Magic (Jack the Ripper's mana blades), the own effects, to the bar of the Time Magic layers: big layered textured quads moved from the age alone,
 * the green of the grimoire (0x48FF7A) with white-hot edges. It is registered after the old SlashLayer (kept in its file) and takes over SLASH_FX1..3 from it.
 * <ul>
 *   <li>SLASH_FX1, CAST: 'from' = hand, 'to' = target, power = width (0.6 to 3). A glint and a rip mark flash at the hand over the first quarter; then a jagged
 *       crescent (slash_rip_crescent, bulging along the flight, rolled 25 degrees so the cut is diagonal) flies to the target with two fainter afterimages behind
 *       it, speed streaks along its path and shards shed from the tips; it lands in a crossed cut (slash_rip_cross) and a flash at 'to'. Default 14 ticks.</li>
 *   <li>SLASH_FX2, FIELD: 'from' = centre on the ground, power = radius. A serrated ring turns on the ground, three crescents orbit at chest height (each with a
 *       trailing streak), rip marks flash in the air over the area and cuts score the floor. In over 6 ticks, out over the last 10. Default 60.</li>
 *   <li>SLASH_FX3, BURST: 'from' = centre, 'to' = direction hint, power = scale. A white flash and glint, a big crossed cut facing the camera that opens and fades,
 *       a serrated ring running out on the ground, rip marks, and blade shards thrown out. Default 24.</li>
 * </ul>
 * Budget: FX1 about 140 vertices, FX2 about 200, FX3 about 180 (the preview reports it; the limit is 400).
 */
public class SlashRipFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation CRESCENT = t("slash_rip_crescent"), CROSS = t("slash_rip_cross"), STREAK = t("slash_rip_streak"), SHARD = t("slash_rip_shard"),
            RING = t("slash_rip_ring"), GLINT = t("slash_rip_glint"), MARK = t("slash_rip_mark");

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SLASH_FX1, VfxShape.SLASH_FX2, VfxShape.SLASH_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return switch (s) { case SLASH_FX1 -> 14; case SLASH_FX2 -> 60; default -> 24; }; }

    @Override public int defaultColor(VfxShape s) { return 0xFF48FF7A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.SLASH_FX3) VfxShake.add(inst.payload.from(), 0.5f * Math.min(2f, inst.power), 6);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SLASH_FX1 -> cast(inst, ctx, buf);
            case SLASH_FX2 -> field(inst, ctx, buf);
            case SLASH_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static int col(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

    /** A pose whose texture-up runs along {@code dir} and which is rolled by {@code roll} radians about it (the crescent bulges along its flight). */
    private static VfxPose along(Vector3f origin, Vector3f dir, float roll) {
        Vector3f up = new Vector3f(dir);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(0, 1, 0));
        if (side.lengthSquared() < 1.0e-4f) side.set(1, 0, 0);
        side.normalize();
        Vector3f lift = new Vector3f(side).cross(dir).normalize();
        Vector3f right = new Vector3f(side).mul(Mth.cos(roll)).add(new Vector3f(lift).mul(Mth.sin(roll)));
        Vector3f normal = new Vector3f(right).cross(up).normalize();
        return new VfxPose(new Vector3f(origin), right, up, normal);
    }

    // ------------------------------------------------------------------ CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), prog = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.6f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 1.0e-3f) dir.set(0, 0, 1); else dir.div(len);
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        float charge = Mth.clamp(age / (dur * 0.25f), 0, 1);

        float flash = (1f - charge) * 0.6f + 0.4f * (1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.3f) / 0.7f, 0, 1)));
        VfxBloom.glow(ctx, buf, from, 0.3f * size * (0.4f + 0.6f * charge), c, 0.6f * flash);
        buf.billboard(ctx, GLINT, VfxBlend.ADD, from, 1.1f * size * charge * (1f - 0.5f * prog), prog * 1.5f, col(WHITE, 0.9f * flash));
        buf.billboard(ctx, MARK, VfxBlend.ADD, from, 1.0f * size * VfxAnim.easeOutCubic(charge), 0.5f, col(hot, 0.8f * (1f - prog)));
        if (prog < 0.18f) return;

        float e = VfxAnim.easeOutCubic(Mth.clamp((prog - 0.18f) / 0.82f, 0, 1));
        Vector3f head = new Vector3f(from).lerp(to, e);
        float fade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.8f) / 0.2f, 0, 1));
        float roll = 0.43f;
        for (int k = 2; k >= 0; k--) {                                                       // the crescent and two afterimages behind it
            Vector3f at = new Vector3f(from).lerp(to, Math.max(0f, e - 0.1f * k)).sub(new Vector3f(dir).mul(0.12f * k * size));
            buf.plane(CRESCENT, VfxBlend.ADD, along(at, dir, roll + 0.05f * k), 0.9f * size * (1f - 0.1f * k), col(k == 0 ? hot : c, (k == 0 ? 0.95f : 0.4f / k) * fade));
        }
        for (int k = -1; k <= 1; k += 2) {                                                   // speed streaks along the flight
            Vector3f side = new Vector3f(dir).cross(ctx.camUp);
            if (side.lengthSquared() < 1.0e-4f) side.set(ctx.camRight);
            side.normalize().mul(k * 0.5f * size);
            Vector3f tail = new Vector3f(from).lerp(to, Math.max(0f, e - 0.45f)).add(side);
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(head).add(side), tail, 0.35f * size, 0.12f * size, 1, 0, col(c, 0.55f * fade), col(c, 0.0f));
        }
        RandomSource r = inst.random();
        for (int i = 0; i < 4; i++) {                                                        // shards shed from the tips
            float lag = 0.06f + 0.07f * i;
            Vector3f p = new Vector3f(from).lerp(to, Math.max(0f, e - lag)).add(0, (r.nextFloat() - 0.5f) * 0.9f * size, 0).add(new Vector3f(ctx.camRight).mul((r.nextFloat() - 0.5f) * 1.8f * size));
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.28f * size, age * 0.3f + i * 1.7f, col(hot, 0.8f * fade * (1f - 0.15f * i)));
        }
        if (prog > 0.82f) {                                                                  // the landing: a crossed cut
            float p = (prog - 0.82f) / 0.18f;
            VfxBloom.glow(ctx, buf, to, 0.5f * size, c, 0.9f * (1f - p));
            buf.billboard(ctx, CROSS, VfxBlend.ADD, to, 1.6f * size * VfxAnim.easeOutCubic(p), 0.2f, col(hot, 0.95f * (1f - p)));
        }
    }

    // ------------------------------------------------------------------ FIELD
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 6, 10), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
        float R = Math.max(1f, inst.power) * grow;
        if (R <= 0.05f || fade <= 0.01f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, floor, R * 0.8f, c, 0.4f * fade);
        buf.plane(RING, VfxBlend.ADD, floor.spin(age * 0.12f), R, col(c, 0.85f * fade));
        buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f).spin(-age * 0.07f), R * 0.62f, col(hot, 0.6f * fade));
        // three crescents orbiting at chest height, each with a trailing streak
        for (int k = 0; k < 3; k++) {
            float a = age * 0.32f + k * Mth.TWO_PI / 3f;
            Vector3f at = new Vector3f(Mth.cos(a) * R * 0.78f, 1.0f + 0.15f * Mth.sin(age * 0.2f + k), Mth.sin(a) * R * 0.78f).add(c0);
            Vector3f tangent = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            buf.plane(CRESCENT, VfxBlend.ADD, along(at, tangent, 0.2f), 0.5f * R * 0.5f + 0.4f, col(hot, 0.9f * fade));
            Vector3f tail = new Vector3f(at).sub(new Vector3f(tangent).mul(0.9f + 0.25f * R));
            buf.beam(ctx, STREAK, VfxBlend.ADD, at, tail, 0.5f, 0.15f, 1, 0, col(c, 0.6f * fade), col(c, 0f));
        }
        // rip marks flashing over the area, cuts scoring the floor
        int n = ctx.seg(5, 3);
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.09f + r.nextFloat()) % 1f;
            if (ph > 0.4f) continue;
            float k = ph / 0.4f, a = r.nextFloat() * Mth.TWO_PI + (int) (age * 0.09f) * 2.1f, d = Mth.sqrt(r.nextFloat()) * R * 0.9f;
            Vector3f p = new Vector3f(Mth.cos(a) * d, 0.5f + r.nextFloat() * 1.4f, Mth.sin(a) * d).add(c0);
            buf.billboard(ctx, MARK, VfxBlend.ADD, p, (0.7f + 0.3f * R * 0.3f) * (0.5f + 0.5f * k), a, col(WHITE, 0.9f * fade * (1f - k)));
        }
    }

    // ------------------------------------------------------------------ BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), p = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.5f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        float out = 1f - VfxAnim.easeInCubic(p);
        RandomSource r = inst.random();

        float flash = Mth.clamp(1f - age / 6f, 0, 1);
        VfxBloom.glow(ctx, buf, c0, 0.7f * size * (0.5f + flash), c, 0.4f + 0.7f * flash);
        buf.billboard(ctx, GLINT, VfxBlend.ADD, c0, 2.2f * size * (0.5f + 0.5f * flash), 0.3f, col(WHITE, flash));
        float open = VfxAnim.easeOutBack(Mth.clamp(p * 4f, 0, 1));
        buf.billboard(ctx, CROSS, VfxBlend.ADD, c0, 2.4f * size * open, r.nextFloat() * 0.6f - 0.3f, col(hot, 0.95f * out));
        buf.billboard(ctx, MARK, VfxBlend.ADD, c0, 1.6f * size * open, 1.2f, col(c, 0.8f * out));
        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        for (int k = 0; k < 2; k++) {
            float q = Mth.clamp((p - 0.07f * k) / (0.9f - 0.07f * k), 0, 1), s = VfxAnim.easeOutCubic(q);
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f * k).spin(age * (k == 0 ? 0.2f : -0.15f)), size * (0.3f + 2.4f * s), col(k == 0 ? hot : c, 0.9f * (1f - q)));
        }
        int n = ctx.seg(9, 4);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + r.nextFloat() * 0.5f, sp = (1.0f + r.nextFloat() * 1.3f) * size, up = (0.2f + r.nextFloat() * 1.0f) * size;
            float e = VfxAnim.easeOutCubic(Mth.clamp(p * 1.3f, 0, 1));
            Vector3f q = new Vector3f(Mth.cos(a) * sp * e, up * e - 0.6f * size * p * p, Mth.sin(a) * sp * e).add(c0);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, q, 0.34f * size, a + age * 0.35f, col(i % 2 == 0 ? hot : c, 0.9f * out));
        }
    }
}
