package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Eye Magic: eyes that open, watch and lock on. Palette: hot rose-pink light, a gold slit pupil, plum shadow, white-hot flares.
 * <ul>
 *   <li>EYE_FX1 (cast / projectile): an almond eye snaps open at 'from' with a lens flare and gathering rays (first quarter),
 *       then a piercing gaze flies to 'to': a spiralling light ribbon with a small iris as its head, a spinning tracking
 *       reticle around the head, echo eyes left along the path, and at 'to' the reticle closes in on the target with a glint
 *       burst. power = size (0.6 to 3), duration = flight ticks (16).</li>
 *   <li>EYE_FX2 (zone / field): the Watching Field. A rotating eye sigil (twelve small eyes, interlaced triangles) on the ground,
 *       a counter-rotating tracking reticle inside it, a glowing rim with light pillars, six floating eyes on the rim that
 *       blink and turn their irises toward the centre, rising glints, a slit-pupil column over the middle and a pulse ring
 *       every 30 ticks. 'from' = centre on the ground, power = radius in blocks, duration = life (80); fades in over 8
 *       and out over the last 12 ticks.</li>
 *   <li>EYE_FX3 (impact / signature): a huge eye opens at 'from' (a squeezed glint, the lid parts, the iris turns, the slit
 *       pupil snaps wide) behind a rotating gaze-ray burst and an anamorphic lens flare; two reticle shockwaves and the
 *       sigil spread over the ground, lash shards and glints fly out (biased along to - from when given) and the eye
 *       closes into an afterglow. power = scale, duration = life (28).</li>
 * </ul>
 * Colour: inst.color is mixed 35% into the rose-pink, so a white tint still reads as Eye Magic.
 */
public class EyeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation IRIS = t("eye_iris");
    public static final ResourceLocation ALMOND = t("eye_almond");
    public static final ResourceLocation SCLERA = t("eye_sclera");
    public static final ResourceLocation SLIT = t("eye_slit");
    public static final ResourceLocation FLARE = t("eye_flare");
    public static final ResourceLocation RAYS = t("eye_rays");
    public static final ResourceLocation GLINT = t("eye_glint");
    public static final ResourceLocation RETICLE = t("eye_reticle");
    public static final ResourceLocation SIGIL = t("eye_sigil");
    public static final ResourceLocation TRAIL = t("eye_trail");
    public static final ResourceLocation SHARD = t("eye_shard");

    private static final int PINK = 0xFFFF4A9A;
    private static final int GOLD = 0xFFFFC85A;
    private static final int PLUM = 0xFF6A2A4A;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.EYE_FX1, VfxShape.EYE_FX2, VfxShape.EYE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case EYE_FX1 -> 16;
            case EYE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return PINK; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case EYE_FX1 -> cast(inst, ctx, buf);
            case EYE_FX2 -> field(inst, ctx, buf);
            case EYE_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private static int tint(VfxInstance inst) {
        return 0xFF000000 | (VfxVertexBuffer.lerpColor(PINK, inst.color | 0xFF000000, 0.35f) & 0xFFFFFF);
    }

    private static int a(int c, float alpha) { return VfxVertexBuffer.withAlpha(c, Mth.clamp(alpha, 0f, 1f)); }

    private static float ease(float x) { return VfxAnim.easeOutCubic(Mth.clamp(x, 0f, 1f)); }

    // ------------------------------------------------------------------ primitives
    /** A camera-facing rectangle with separate half width / half height (eyelids squash it). */
    static void wide(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float hw, float hh, int argb) {
        if (hw <= 0.001f || hh <= 0.001f) return;
        Vector3f rx = new Vector3f(ctx.camRight).mul(hw), ry = new Vector3f(ctx.camUp).mul(hh);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry),
                new Vector3f(c).sub(rx).add(ry), 0, 0, 1, 1, col, col);
    }

    /**
     * One eye facing the camera: dark sclera, iris, glowing slit pupil, lid outline. width = full width, open 0..1 lid opening,
     * look = iris offset (camera units, -1..1), slit = pupil width factor.
     */
    static void eye(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float width, float open, float lookX, float lookY, float slit,
                    int col, float fade) {
        if (open <= 0.03f || fade <= 0.01f || !buf.hasBudget(16)) return;
        float hw = width * 0.5f, hh = width * 0.5f * open;
        wide(ctx, buf, SCLERA, VfxBlend.ALPHA, c, hw, hh, a(PLUM, 0.8f * fade));
        float is = width * 0.36f;
        Vector3f ic = new Vector3f(c).add(new Vector3f(ctx.camRight).mul(lookX * width * 0.12f)).add(new Vector3f(ctx.camUp).mul(lookY * width * 0.05f * open));
        float ih = Math.min(1f, open * 1.25f);
        wide(ctx, buf, IRIS, VfxBlend.ALPHA, ic, is * 0.5f, is * 0.5f * ih, a(col, fade));
        wide(ctx, buf, SLIT, VfxBlend.ADD, ic, is * 0.16f * slit, is * 0.62f * ih, a(VfxVertexBuffer.lerpColor(GOLD, WHITE, 0.45f), fade));
        wide(ctx, buf, ALMOND, VfxBlend.ADD, c, hw, hh * 1.0f, a(VfxVertexBuffer.whiten(col, 0.35f), fade));
    }

    private static float blink(float age, float period, float phase) {
        float x = ((age + phase) % period) / period;
        return x > 0.9f ? Math.abs(x - 0.95f) / 0.05f : 1f;
    }

    // ------------------------------------------------------------------ FX1: the piercing gaze
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(4f, inst.duration), t = Mth.clamp(age / D, 0f, 1f);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        int col = tint(inst);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        float charge = ease(t / 0.25f);
        float travel = VfxAnim.easeInOutSine(Mth.clamp((t - 0.18f) / 0.62f, 0f, 1f));
        float hit = Mth.clamp((t - 0.78f) / 0.22f, 0f, 1f);
        float srcFade = 1f - ease((t - 0.3f) / 0.5f);
        Vector3f head = new Vector3f(from).lerp(to, travel);

        // 1. the eye opening at the origin, with gathering rays and a flare
        if (srcFade > 0.02f) {
            float open = VfxAnim.easeOutBack(Mth.clamp(t / 0.16f, 0f, 1f));
            VfxBloom.glow(ctx, buf, from, 0.45f * P * (0.6f + charge), col, 0.9f * srcFade);
            buf.billboard(ctx, RAYS, VfxBlend.ADD, from, 2.4f * P * (1.3f - 0.5f * charge), age * 0.12f, a(col, 0.55f * srcFade * (1f - charge * 0.4f)));
            eye(ctx, buf, from, 0.95f * P, Mth.clamp(open, 0f, 1.1f), 0f, 0f, 1f + 0.5f * (1f - charge), col, srcFade);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, from, 2.6f * P, 0, a(GOLD, 0.8f * Math.max(0f, 1f - Math.abs(t - 0.14f) * 6f)));
        }

        // 2. the ribbon of light and its echoes
        if (travel > 0.01f) {
            int c0 = a(col, 0.95f), c1 = a(GOLD, 0.0f);
            buf.beam(ctx, TRAIL, VfxBlend.ADD, head, from, 0.26f * P, 0.04f * P, 4, -age * 0.12f, a(WHITE, 0.95f), a(col, 0.1f));
            buf.beam(ctx, TRAIL, VfxBlend.ADD, head, from, 0.7f * P, 0.12f * P, 3, age * 0.05f, a(col, 0.45f), c1);
            for (int i = 1; i <= 3; i++) {                      // echo eyes along the path, each fading on its own beat
                float f = 0.2f + 0.22f * i;
                if (f > travel) continue;
                float life = Mth.clamp(1f - (travel - f) * 2.2f, 0f, 1f) * (1f - hit);
                if (life <= 0.02f) continue;
                Vector3f e = new Vector3f(from).lerp(head, f);
                wide(ctx, buf, ALMOND, VfxBlend.ADD, e, 0.28f * P, 0.28f * P * 0.5f * (0.4f + 0.6f * life), a(col, 0.75f * life));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, e, 0.4f * P, age * 0.08f + i, a(WHITE, 0.8f * life));
            }
        }

        // 3. the head: a small iris with a spinning reticle
        if (travel > 0.01f && hit < 0.6f) {
            float hf = 1f - hit / 0.6f;
            VfxBloom.glow(ctx, buf, head, 0.3f * P, col, 1.0f * hf);
            buf.billboard(ctx, IRIS, VfxBlend.ALPHA, head, 0.42f * P, age * 0.05f, a(col, hf));
            buf.billboard(ctx, SLIT, VfxBlend.ADD, head, 0.4f * P, 0, a(GOLD, hf));
            buf.billboard(ctx, RETICLE, VfxBlend.ADD, head, 0.95f * P, age * 0.35f, a(VfxVertexBuffer.whiten(col, 0.4f), 0.85f * hf));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 1.5f * P, 0, a(WHITE, 0.55f * hf));
            RandomSource r = inst.random();
            for (int i = 0; i < 4; i++) {                       // sparks shed behind the head
                float lag = 0.06f + r.nextFloat() * 0.2f, ang = r.nextFloat() * Mth.TWO_PI;
                float tt = Math.max(0f, travel - lag);
                Vector3f s = new Vector3f(from).lerp(to, tt).add(Mth.cos(ang) * 0.25f * P * lag * 4, Mth.sin(ang) * 0.25f * P * lag * 4, 0);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, s, 0.28f * P, age * 0.2f + i, a(GOLD, 0.7f * hf * (1f - lag * 3f)));
            }
        }

        // 4. the lock: the reticle closes on the target, glints burst out
        if (hit > 0f) {
            float close = ease(hit * 1.6f), fl = (float) Math.sin(Mth.clamp(hit, 0f, 1f) * Math.PI);
            VfxBloom.glow(ctx, buf, to, 0.55f * P, col, 1.1f * (1f - hit * 0.6f));
            buf.billboard(ctx, RETICLE, VfxBlend.ADD, to, P * (2.3f - 1.5f * close), age * -0.25f, a(col, 0.95f * (1f - hit * 0.5f)));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, to, 2.8f * P, 0, a(WHITE, 0.9f * fl));
            buf.billboard(ctx, RAYS, VfxBlend.ADD, to, 1.8f * P * (0.5f + hit), age * 0.1f, a(GOLD, 0.7f * (1f - hit)));
            RandomSource r = inst.random();
            for (int i = 0; i < 6; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, sp = 0.6f + r.nextFloat() * 0.9f;
                Vector3f g = new Vector3f(to).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * sp * hit * P)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * sp * hit * P));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, g, 0.35f * P, ang + age * 0.1f, a(WHITE, 1f - hit));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: the watching field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float life = Math.min(Mth.clamp(age / 8f, 0f, 1f), Mth.clamp((inst.duration - age) / 12f, 0f, 1f));
        if (life <= 0.01f) return;
        float R = Math.max(1f, inst.power), open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f));
        int col = tint(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);

        // ground: light, sigil, counter-rotating reticle
        VfxBloom.planeGlow(buf, floor, R, col, 0.55f * life);
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.01f).spin(age * 0.012f), R * open, a(col, 0.95f * life * pulse));
        buf.plane(RETICLE, VfxBlend.ADD, floor.lift(0.02f).spin(-age * 0.03f), R * 0.64f * open, a(VfxVertexBuffer.whiten(col, 0.35f), 0.85f * life));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, floor.lift(0.03f), R * 0.965f, R * 1.02f, ctx.seg(16, 8), 1, 0, a(GOLD, 0.7f * life * pulse));
        // pulse ring: a reticle shockwave every 30 ticks
        float pp = (age % 30f) / 30f;
        if (age > 6f) buf.plane(RETICLE, VfxBlend.ADD, floor.lift(0.04f).spin(pp * 2f), R * (0.2f + 0.85f * ease(pp)), a(WHITE, 0.55f * (1f - pp) * life));

        // rim pillars and the central watching slit
        RandomSource r = inst.random();
        int np = Math.max(5, Math.min(12, (int) (R * 1.6f)));
        for (int i = 0; i < np; i++) {
            float ang = Mth.TWO_PI * i / np + age * 0.004f, fl = 0.5f + 0.5f * Mth.sin(age * 0.3f + i * 2.1f);
            float h = (1.1f + 0.5f * fl) * Math.min(1.6f, 0.7f + R * 0.1f) * open;
            Vector3f b = new Vector3f(c).add(Mth.cos(ang) * R * 0.97f, 0.05f, Mth.sin(ang) * R * 0.97f);
            Vector3f top = new Vector3f(b).add(0, h, 0);
            buf.beam(ctx, TRAIL, VfxBlend.ADD, b, top, 0.22f, 0.05f, 1, age * 0.04f + i, a(col, 0.7f * life * (0.5f + 0.5f * fl)), a(col, 0.2f * life));
        }
        float sh = Math.min(3.4f, 1.6f + R * 0.25f) * open;
        wide(ctx, buf, SLIT, VfxBlend.ADD, new Vector3f(c).add(0, 0.05f + sh * 0.5f, 0), sh * 0.12f * (0.8f + 0.4f * pulse), sh * 0.5f, a(VfxVertexBuffer.lerpColor(GOLD, col, 0.3f), 0.8f * life));

        // six floating eyes on the rim, blinking out of step and turning toward the centre
        float es = Mth.clamp(0.8f + R * 0.16f, 1.0f, 2.0f);
        int ne = R < 3f ? 4 : 6;
        for (int i = 0; i < ne; i++) {
            float ang = Mth.TWO_PI * i / ne + age * 0.01f + 0.4f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * R * 0.86f, 1.5f + es * 0.4f + 0.12f * Mth.sin(age * 0.09f + i * 1.7f), Mth.sin(ang) * R * 0.86f);
            float bl = blink(age, 34f + i * 5f, i * 11f);
            // iris offset: toward the centre as seen from the camera
            float toC = (new Vector3f(c).sub(p).dot(ctx.camRight));
            float lx = Mth.clamp(toC / R, -1f, 1f);
            eye(ctx, buf, p, es, open * bl, lx, -0.3f, 1f, col, life);
        }

        // rising glints
        int nm = ctx.seg(10, 5);
        for (int i = 0; i < nm; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.92f, ph = r.nextFloat(), sp = 0.012f + r.nextFloat() * 0.01f;
            float u = (age * sp + ph) % 1f;
            Vector3f g = new Vector3f(c).add(Mth.cos(ang + age * 0.01f) * d, 0.2f + u * (1.8f + R * 0.1f), Mth.sin(ang + age * 0.01f) * d);
            float tw = Mth.sin(u * Mth.PI);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, g, 0.3f + 0.15f * tw, age * 0.05f + i, a(i % 3 == 0 ? GOLD : WHITE, tw * life));
        }
    }

    // ------------------------------------------------------------------ FX3: the great eye
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(8f, inst.duration), t = Mth.clamp(age / D, 0f, 1f);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        int col = tint(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vec3 dv = inst.to(ctx).subtract(inst.from(ctx));
        Vector3f dir = new Vector3f((float) dv.x, (float) dv.y, (float) dv.z);
        boolean hasDir = dir.lengthSquared() > 0.04f;
        if (hasDir) dir.normalize();

        float opening = VfxAnim.easeOutBack(Mth.clamp((t - 0.08f) / 0.22f, 0f, 1f));
        float closing = 1f - ease((t - 0.62f) / 0.3f);
        float openAmt = Math.max(0f, opening) * closing;
        float burst = Mth.clamp((t - 0.3f) / 0.7f, 0f, 1f);                // after the snap
        float flash = Math.max(0f, 1f - Math.abs(t - 0.32f) * 7f);
        float fade = 1f - ease((t - 0.7f) / 0.3f);
        float antic = Mth.clamp(t / 0.1f, 0f, 1f) * (1f - Mth.clamp((t - 0.1f) / 0.05f, 0f, 1f));

        // back: plum vignette, ground sigil and the two reticle shockwaves
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, c, 5.5f * P * ease(t * 3f), 0, a(PLUM, 0.45f * fade));
        float gy = -Math.min(0.9f * P, 1.4f);
        VfxPose gp = VfxPose.ground(new Vector3f(c).add(0, gy + 0.05f, 0));
        if (t > 0.05f) buf.plane(SIGIL, VfxBlend.ADD, gp.spin(age * 0.05f), P * (1.2f + 2.4f * ease(t * 2f)), a(col, 0.9f * fade));
        for (int k = 0; k < 2; k++) {
            float s = Mth.clamp((t - 0.3f - k * 0.1f) / 0.55f, 0f, 1f);
            if (s > 0f) buf.plane(RETICLE, VfxBlend.ADD, gp.lift(0.01f * (k + 1)).spin(k == 0 ? s * 1.5f : -s * 1.2f), P * (0.4f + (4.2f - k * 1.0f) * ease(s)), a(k == 0 ? WHITE : GOLD, 0.8f * (1f - s)));
        }

        // gaze rays and the glow behind the eye
        float rs = 3.6f * P * (0.4f + ease(t * 2.2f));
        buf.billboard(ctx, RAYS, VfxBlend.ADD, c, rs * 1.9f, age * 0.03f, a(col, 0.55f * fade * Math.min(1f, t * 5f)));
        buf.billboard(ctx, RAYS, VfxBlend.ADD, c, rs * 1.2f, -age * 0.06f, a(VfxVertexBuffer.whiten(GOLD, 0.2f), 0.55f * fade * Math.min(1f, t * 5f)));
        VfxBloom.glow(ctx, buf, c, (0.5f + 0.35f * flash) * P * 1.5f, col, (0.5f + 0.8f * flash) * Math.max(fade, 0.25f));

        // the eye itself: pupil snaps wide at the flash, then narrows
        float slit = 0.7f + 1.5f * flash + 0.3f * (1f - burst);
        eye(ctx, buf, c, 3.3f * P, Mth.clamp(openAmt, 0f, 1.12f), Mth.sin(age * 0.15f) * 0.6f * burst, 0.1f, slit, col, Math.max(fade, 0f));
        // anticipation glint squeezing in before the lid parts
        if (antic > 0f) buf.billboard(ctx, GLINT, VfxBlend.ADD, c, 1.4f * P * (1.2f - antic * 0.7f), age * 0.3f, a(WHITE, antic));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, 8f * P, 0, a(WHITE, 0.95f * flash));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, 5.2f * P, Mth.HALF_PI, a(col, 0.5f * flash));

        // debris: lash shards and glints flying out
        RandomSource r = inst.random();
        int ns = ctx.seg(9, 5);
        for (int i = 0; i < ns && t > 0.3f; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, el = (r.nextFloat() - 0.35f) * 1.1f, sp = (2.6f + r.nextFloat() * 3.2f) * P;
            Vector3f v = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el));
            if (hasDir) v.add(new Vector3f(dir).mul(0.7f)).normalize();
            float d = sp * ease(burst * 1.1f), grav = -0.9f * burst * burst * P;
            Vector3f tip = new Vector3f(c).add(v.x * d, v.y * d + grav, v.z * d);
            Vector3f tail = new Vector3f(tip).sub(new Vector3f(v).mul(0.55f * P * (1f - burst * 0.5f)));
            buf.beam(ctx, SHARD, VfxBlend.ADD, tail, tip, 0.2f * P, 0.2f * P, 1, 0, a(i % 2 == 0 ? col : GOLD, 1f - burst), a(WHITE, 1f - burst));
        }
        int ng = ctx.seg(9, 4);
        for (int i = 0; i < ng && t > 0.28f; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, sp = (1.2f + r.nextFloat() * 4f) * P, ph = r.nextFloat();
            float d = sp * ease(burst);
            Vector3f g = new Vector3f(c).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * d)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * d * 0.8f + 0.3f * burst));
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.5f + ph * 6f);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, g, (0.3f + 0.3f * r.nextFloat()) * P, age * 0.1f + i, a(i % 2 == 0 ? WHITE : GOLD, (1f - burst) * (0.4f + 0.6f * tw)));
        }
        // afterglow: a lingering slit of light where the eye was
        if (t > 0.6f) wide(ctx, buf, SLIT, VfxBlend.ADD, c, 0.25f * P * fade, 1.6f * P * fade, a(col, 0.6f * fade));
    }
}
