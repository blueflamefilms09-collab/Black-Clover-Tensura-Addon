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
 * 1.93.0 Dark Magic (Yami Sukehiro: Dark Cloaked), drawn toward the Time Magic layers' quality: layered, eased, every sprite from textures/particle/darkrb_*.png
 * (tools/gen_dark_fx_textures.py). Darkness has to DARKEN: the black underlays are alpha blended (VfxBlend.ALPHA, drawn first), the violet edges, runes and cores additive on top.
 * <ul>
 *   <li>DARKFX_CAST: the dark sigil and a black bloom at 'from', then three cut streaks run to 'to' over a black underlay, winding tendrils of shadow behind them and a small corona at the
 *       head; the cut ends in a crack in the air and a flash. 'from' = origin, 'to' = target, power = size.</li>
 *   <li>DARKFX_ZONE: a black hole on the ground at 'from' (a black disc, the violet accretion disc turning over it), a rune ring at the rim, shadow pillars standing round it and motes
 *       spiralling in. power = radius.</li>
 *   <li>DARKFX_BURST: the black moon rising (a dark disc and a violet corona), three cracks of the void thrown across, a shock rune ring, shadow flung out. power = size.</li>
 * </ul>
 * At most 400 vertices an activation.
 */
public class DarkFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SLASH = t("darkrb_slash"), MOON = t("darkrb_moon"), HOLE = t("darkrb_hole"), TENDRIL = t("darkrb_tendril"), RUNE = t("darkrb_rune"),
            WISP = t("darkrb_wisp"), CRACK = t("darkrb_crack");
    private static final int BLACK = 0xFF0A0612;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DARKFX_CAST, VfxShape.DARKFX_ZONE, VfxShape.DARKFX_BURST); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.DARKFX_ZONE ? 80 : s == VfxShape.DARKFX_BURST ? 30 : 20; }

    @Override public int defaultColor(VfxShape s) { return 0xFFA24CFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.DARKFX_BURST && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 1.0f * inst.power, 12);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DARKFX_CAST -> cast(inst, ctx, buf);
            case DARKFX_ZONE -> zone(inst, ctx, buf);
            case DARKFX_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static void strip(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wa, float wb, int n, int ca, int cb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(0.5f).negate().normalize();
        Vector3f side = new Vector3f(dir).cross(toCam);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize();
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            Vector3f c0 = new Vector3f(dir).mul(t0).add(a), c1 = new Vector3f(dir).mul(t1).add(a);
            Vector3f s0 = new Vector3f(side).mul(Mth.lerp(t0, wa, wb) * 0.5f), s1 = new Vector3f(side).mul(Mth.lerp(t1, wa, wb) * 0.5f);
            int col = blend.grade(VfxVertexBuffer.lerpColor(ca, cb, (t0 + t1) * 0.5f), 1f - t0);
            buf.quad(tex, blend, new Vector3f(c0).sub(s0), new Vector3f(c1).sub(s1), new Vector3f(c1).add(s1), new Vector3f(c0).add(s0), t0, 0, t1, 1, col, col);
        }
    }

    private static Vector3f ease(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    // ------------------------------------------------------------------ DARKFX_CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, fade = life(inst, age, 0, 6);
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.6f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 0.05f) { dir.set(0, 0, 1); len = 0.05f; }
        dir.div(len);
        float charge = Mth.clamp(age / (inst.duration * 0.25f), 0, 1), run = Mth.clamp((age - inst.duration * 0.1f) / (inst.duration * 0.55f), 0, 1);
        float head = VfxAnim.easeOutCubic(run), tail = Math.max(0f, head - 0.7f);
        float open = VfxAnim.easeOutBack(charge) * (1f - 0.6f * Mth.clamp((age - inst.duration * 0.3f) / (inst.duration * 0.3f), 0, 1));
        VfxPose face = VfxPose.facing(new Vector3f(from).add(new Vector3f(dir).mul(0.2f * p)), dir);
        if (open > 0.02f) {
            buf.billboard(ctx, WISP, VfxBlend.ALPHA, from, 1.8f * p * open, age * 0.03f, VfxVertexBuffer.withAlpha(BLACK, 0.8f * fade));
            buf.plane(RUNE, VfxBlend.ADD, face.spin(age * 0.08f), 0.6f * p * open, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
            VfxBloom.glow(ctx, buf, from, 0.4f * p * open, col, 0.6f * fade);
        }
        if (head <= 0.001f) return;
        Vector3f up = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f side = new Vector3f(dir).cross(up).normalize(), up2 = new Vector3f(side).cross(dir).normalize();
        Vector3f hp = ease(from, to, head), tp = ease(from, to, tail);
        float w = 1.1f * p;
        strip(ctx, buf, SLASH, VfxBlend.ALPHA, tp, hp, w * 0.2f, w * 1.1f, ctx.seg(8, 4), VfxVertexBuffer.withAlpha(BLACK, 0f), VfxVertexBuffer.withAlpha(BLACK, 0.9f * fade));
        for (int k = 0; k < 3; k++) {                                                           // three violet cut streaks, fanned a little
            float off = (k - 1) * 0.22f * p;
            Vector3f a = new Vector3f(tp).add(new Vector3f(up2).mul(off)), b = new Vector3f(hp).add(new Vector3f(up2).mul(off * 0.4f));
            strip(ctx, buf, SLASH, VfxBlend.ADD, a, b, w * 0.15f, w * (k == 1 ? 0.5f : 0.35f), ctx.seg(6, 3), VfxVertexBuffer.withAlpha(col, 0f), VfxVertexBuffer.withAlpha(k == 1 ? pale : col, 0.95f * fade));
        }
        for (int k = 0; k < 2; k++) {                                                           // tendrils of shadow winding behind the head
            Vector3f prev = null;
            int pieces = ctx.seg(6, 3);
            for (int i = 0; i <= pieces; i++) {
                float s = Mth.lerp(i / (float) pieces, tail, head), ang = s * 11f - age * 0.28f + k * Mth.PI, rad = 0.26f * p * (0.4f + 0.6f * (i / (float) pieces));
                Vector3f pos = ease(from, to, s).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(new Vector3f(up2).mul(Mth.sin(ang) * rad));
                if (prev != null) strip(ctx, buf, TENDRIL, VfxBlend.ALPHA, prev, pos, 0.28f * p, 0.3f * p, 1, VfxVertexBuffer.withAlpha(BLACK, 0.3f * fade * (i / (float) pieces)), VfxVertexBuffer.withAlpha(BLACK, 0.9f * fade));
                prev = pos;
            }
        }
        buf.billboard(ctx, MOON, VfxBlend.ADD, hp, 0.9f * p, age * 0.1f, VfxVertexBuffer.withAlpha(col, 0.6f * fade));
        VfxBloom.glow(ctx, buf, hp, 0.5f * p, col, 0.7f * fade);
        if (head > 0.85f) {                                                                     // the cut ends in a crack in the air
            float k = (head - 0.85f) / 0.15f;
            VfxPose crack = VfxPose.facing(to, new Vector3f(dir).cross(up).normalize());
            buf.plane(CRACK, VfxBlend.ADD, crack.spin(0.3f), 1.5f * p * k, VfxVertexBuffer.withAlpha(pale, 0.9f * (1f - 0.5f * k) * fade));
            buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.08f, 0)), 0.4f * p + 0.8f * p * k, VfxVertexBuffer.withAlpha(col, (1f - k) * 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ DARKFX_ZONE
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        buf.plane(WISP, VfxBlend.ALPHA, ground.spin(age * 0.01f), R * 1.15f * open, VfxVertexBuffer.withAlpha(BLACK, 0.92f * fade));              // the black underlay: the ground darkens first
        buf.plane(HOLE, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.045f), R * 1.05f * open, VfxVertexBuffer.withAlpha(col, 0.85f * fade));
        buf.plane(HOLE, VfxBlend.ADD, ground.lift(0.05f).spin(-age * 0.07f), R * 0.62f * open, VfxVertexBuffer.withAlpha(pale, 0.7f * fade));
        buf.plane(RUNE, VfxBlend.ADD, ground.lift(0.07f).spin(-age * 0.02f), R * 1.0f * open, VfxVertexBuffer.withAlpha(col, 0.55f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {                                                           // pillars of shadow standing round the rim
            float a = i * Mth.TWO_PI / n + r.nextFloat() * 0.3f, ph = r.nextFloat() * Mth.TWO_PI, rad = R * 0.98f;
            float h = (1.2f + 1.0f * r.nextFloat()) * Math.max(1f, R * 0.35f) * (0.8f + 0.2f * Mth.sin(age * 0.2f + ph)) * open;
            Vector3f base = new Vector3f(c).add(Mth.cos(a) * rad, 0.05f, Mth.sin(a) * rad);
            Vector3f top = new Vector3f(base).add(-Mth.cos(a) * 0.3f * h + 0.2f * Mth.sin(age * 0.1f + ph), h, -Mth.sin(a) * 0.3f * h);
            strip(ctx, buf, TENDRIL, VfxBlend.ALPHA, base, top, 0.5f, 0.12f, 2, VfxVertexBuffer.withAlpha(BLACK, 0.9f * fade), VfxVertexBuffer.withAlpha(BLACK, 0.3f * fade));
            strip(ctx, buf, TENDRIL, VfxBlend.ADD, base, top, 0.14f, 0.05f, 2, VfxVertexBuffer.withAlpha(col, 0.8f * fade), VfxVertexBuffer.withAlpha(pale, 0.2f * fade));
        }
        int m = ctx.seg(14, 6);
        for (int i = 0; i < m; i++) {                                                           // motes spiralling in towards the middle
            float ph = Mth.frac(age * 0.02f * (1f + 0.5f * r.nextFloat()) + r.nextFloat()), a = r.nextFloat() * Mth.TWO_PI + ph * 5.0f;
            float d = R * (1f - ph);
            buf.billboard(ctx, WISP, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * d, 0.1f + 0.5f * Mth.sin(ph * Mth.PI), Mth.sin(a) * d), 0.3f * (1f - 0.6f * ph), a,
                    VfxVertexBuffer.withAlpha(i % 2 == 0 ? col : pale, Mth.sin(ph * Mth.PI) * 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ DARKFX_BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick), p = inst.power, fade = 1f - VfxAnim.easeInCubic(t);
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float rise = VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0, 1));
        Vector3f moon = new Vector3f(c).add(0, (0.6f + 1.4f * rise) * p, 0);
        buf.billboard(ctx, WISP, VfxBlend.ALPHA, moon, (2.0f + 1.6f * rise) * p, age * 0.02f, VfxVertexBuffer.withAlpha(BLACK, 0.85f * fade));
        buf.billboard(ctx, MOON, VfxBlend.ALPHA, moon, (0.7f + 1.1f * rise) * p, age * 0.01f, VfxVertexBuffer.withAlpha(0xFF14101E, Math.min(1f, 0.5f + rise) * fade));
        buf.billboard(ctx, MOON, VfxBlend.ADD, moon, (1.2f + 2.2f * rise) * p, -age * 0.03f, VfxVertexBuffer.withAlpha(col, 0.5f * fade));
        VfxBloom.glow(ctx, buf, moon, 0.9f * p, col, 0.6f * fade);
        for (int k = 0; k < 2; k++) {                                                           // shock runes running out over the ground
            float lt = Mth.clamp(t * 1.5f - k * 0.2f, 0, 1);
            if (lt <= 0f) continue;
            buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.07f + 0.01f * k, 0)).spin(age * 0.06f * (k == 0 ? 1 : -1)), (0.4f + 2.6f * VfxAnim.easeOutCubic(lt)) * p,
                    VfxVertexBuffer.withAlpha(k == 0 ? col : pale, (1f - lt) * 0.9f));
        }
        RandomSource r = inst.random();
        for (int i = 0; i < 3; i++) {                                                           // cracks of the void thrown across the middle, at three angles
            float a = r.nextFloat() * Mth.PI, grow = VfxAnim.easeOutCubic(Mth.clamp(t * 2.2f - i * 0.15f, 0, 1));
            if (grow <= 0f) continue;
            Vector3f n = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            buf.plane(CRACK, VfxBlend.ADD, VfxPose.facing(new Vector3f(c).add(0, 0.9f * p, 0), n).spin(r.nextFloat() * 0.6f - 0.3f), 2.0f * p * grow, VfxVertexBuffer.withAlpha(pale, 0.85f * fade));
        }
        int s = ctx.seg(8, 3);
        for (int i = 0; i < s; i++) {                                                           // shadow flung out
            float a = r.nextFloat() * Mth.TWO_PI, d = (0.4f + 1.4f * r.nextFloat()) * p * VfxAnim.easeOutCubic(t);
            buf.billboard(ctx, WISP, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(a) * d, 0.2f + 0.7f * t * p, Mth.sin(a) * d), (0.5f + 0.7f * t) * p, a + age * 0.03f,
                    VfxVertexBuffer.withAlpha(BLACK, 0.7f * Mth.sin(Mth.clamp(t * 1.2f, 0, 1) * Mth.PI)));
        }
    }
}
