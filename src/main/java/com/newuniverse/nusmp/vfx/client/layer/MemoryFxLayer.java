package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Memory Magic (1.93.0 rebuild): lavender light, film strips, photo frames and reels, a moment kept and shown again. Textures: tools/gen_memory_rebuild_textures.py
 * (particle/memory_*.png). The three shapes of the shape protocol:
 * <ul>
 *   <li>MEMORY_FX1, CAST / PROJECTILE, "Recollection". A small film reel and a frame contract on 'from' (charge, 22 percent of the life); then a head of light runs 'from' to 'to'
 *       (accelerating) with a strip of film streaming behind it, a trail of small photo frames that fade as they age and a second, ghostly copy that follows a moment later (the
 *       echo); at the end a frame opens at 'to' with a spark and a ring. power = size (cast shapes are drawn 3 times larger by the manager: 1 usual), duration = flight ticks (default 16).</li>
 *   <li>MEMORY_FX2, ZONE / FIELD, "Forgetting Field". A ring of film frames turning on the ground to the radius, a slow reel turning over the middle, frames drifting up and
 *       dissolving, film strips standing at the rim and unwinding, sparks of forgotten light. from = centre on the ground (or the followed foe), power = radius (zone shapes are drawn
 *       1.5 times larger), duration = life (default 80; in 8, out 12).</li>
 *   <li>MEMORY_FX3, IMPACT / SIGNATURE, "Flashback". A white flash, a reel opening and turning over the middle, photo frames flung out and spinning, a ring of film frames running out
 *       on the ground, glass shards and sparks, a ghostly second flash a few ticks later (the replay). from = centre, to = optional direction hint, power = scale, duration = life (default 26).</li>
 * </ul>
 * The colour tints the light (default lavender 0xFFBCA8FF). Budget: well under the 400 vertices of an activation.
 */
public class MemoryFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FILM = t("memory_film_v"), FRAME = t("memory_frame"), REEL = t("memory_reel"), SHARD = t("memory_shard"), RIPPLE = t("memory_ripple"),
            SPARK = t("memory_spark"), GLOW = VfxTextures.GLOW;

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MEMORY_FX1, VfxShape.MEMORY_FX2, VfxShape.MEMORY_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case MEMORY_FX1 -> 16;
            case MEMORY_FX2 -> 80;
            default -> 26;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFBCA8FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MEMORY_FX1 -> cast(inst, ctx, buf);
            case MEMORY_FX2 -> zone(inst, ctx, buf);
            case MEMORY_FX3 -> flashback(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static int hot(int argb, float k) { return VfxVertexBuffer.whiten(argb, k); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }
    private static float ramp(float t, float s, float e) { return Mth.clamp((t - s) / Math.max(1e-4f, e - s), 0f, 1f); }
    private static float out(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    private static float life(VfxInstance inst, float age, float in, float outT) {
        return Math.min(in <= 0 ? 1f : Mth.clamp(age / in, 0f, 1f), outT <= 0 ? 1f : Mth.clamp((inst.duration - age) / outT, 0f, 1f));
    }

    private static void glow(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, int col, float k) {
        if (size > 0.01f && k > 0.01f) buf.billboard(ctx, GLOW, VfxBlend.ADD, p, size, 0f, a(col, k));
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, Vector3f c, float lift, float half, float spin, int col) {
        if (half > 0.02f) buf.plane(tex, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, lift, 0)).spin(spin), half, col);
    }

    // ------------------------------------------------------------------ FX1: Recollection
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float u = Mth.clamp(inst.power, 0.5f, 6f) / 3f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        boolean still = dist < 0.4f;
        if (still) dir.set(0, 0, 1); else dir.mul(1f / dist);
        int col = inst.color | 0xFF000000;
        float tl = 0.22f * d, ta = 0.78f * d;

        float tc = ramp(age, 0f, tl), cf = tc * (1f - ramp(age, tl, tl * 1.5f + 1f));
        if (still) cf = Math.max(cf, 1f - ramp(age, 0f, d));
        if (cf > 0.02f) {
            VfxPose face = VfxPose.facing(new Vector3f(from), new Vector3f(dir).lerp(new Vector3f(ctx.camRight).cross(ctx.camUp), 0.5f));
            buf.plane(REEL, VfxBlend.ADD, face.spin(age * 0.3f), (1.0f - 0.45f * tc) * u * 0.9f, a(col, 0.85f * cf));
            buf.plane(FRAME, VfxBlend.ADD, face.lift(0.04f).spin(-age * 0.05f), (0.8f - 0.3f * tc) * u, a(hot(col, 0.4f), 0.8f * cf));
            glow(buf, ctx, from, 0.8f * u, col, 0.55f * cf);
        }
        if (still) return;

        float s = ramp(age, tl, ta), se = 0.4f * s + 0.6f * s * s;
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        if (s > 0f && s < 1f) {
            float trail = Math.min(dist * se, 3.0f * u + 0.6f);
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
            buf.beam(ctx, FILM, VfxBlend.ADD, head, tail, 0.55f * u, 0.4f * u, 2, -age * 0.05f, a(hot(col, 0.4f), 0.95f), a(col, 0f));
            Vector3f echoHead = new Vector3f(dir).mul(dist * (0.4f * Math.max(0f, s - 0.12f) + 0.6f * Math.max(0f, s - 0.12f) * Math.max(0f, s - 0.12f))).add(from);
            buf.beam(ctx, FILM, VfxBlend.ADD, echoHead, new Vector3f(echoHead).sub(new Vector3f(dir).mul(trail * 0.7f)), 0.45f * u, 0.3f * u, 1, age * 0.05f, a(col, 0.45f), a(col, 0f));
            glow(buf, ctx, head, 0.9f * u, col, 0.7f);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, head, 0.9f * u, age * 0.3f, a(WHITE, 0.95f));
            for (int k = 0; k < 4; k++) {                                                    // frames left along the path
                float f = 0.16f + 0.2f * k;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(trail * f)).add(new Vector3f(ctx.camUp).mul(Mth.sin(age * 0.3f + k * 1.7f) * 0.15f * u));
                buf.billboard(ctx, FRAME, VfxBlend.ADD, p, (0.55f - 0.08f * k) * u, 0.2f * (k - 1.5f), a(k % 2 == 0 ? hot(col, 0.3f) : col, 0.85f * (1f - f)));
            }
        }
        float ti = ramp(age, ta - 1f, d);
        if (ti > 0f) {
            float k = out(ti);
            buf.billboard(ctx, FRAME, VfxBlend.ADD, to, 1.6f * u * k, age * 0.08f, a(hot(col, 0.4f), 0.9f * (1f - ti)));
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(new Vector3f(to), new Vector3f(dir).negate()).spin(age * 0.15f), 1.5f * u * k, a(col, 0.8f * (1f - ti)));
            buf.billboard(ctx, SPARK, VfxBlend.ADD, to, 1.1f * u * k, age * 0.2f, a(WHITE, 0.9f * (1f - ti)));
            glow(buf, ctx, to, 1.2f * u * (1f - 0.4f * ti), col, 0.6f * (1f - ti));
        }
    }

    // ------------------------------------------------------------------ FX2: Forgetting Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.0f, inst.power / 1.5f);
        float fade = life(inst, age, 8f, 12f), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color | 0xFF000000;
        float r = R * open;
        ground(buf, GLOW, c, 0.04f, r * 1.05f, 0f, a(col, 0.18f * fade));
        ground(buf, RIPPLE, c, 0.05f, r * 1.04f, age * 0.02f, a(col, 0.9f * fade));
        ground(buf, RIPPLE, c, 0.07f, r * 0.64f, -age * 0.035f, a(hot(col, 0.4f), 0.55f * fade));
        Vector3f over = new Vector3f(c).add(0, 2.0f + 0.1f * Mth.sin(age * 0.07f), 0);               // a slow reel turning over the middle
        buf.plane(REEL, VfxBlend.ADD, VfxPose.facing(over, new Vector3f(0, 1, 0)).spin(age * 0.05f), Math.min(2.2f, 0.7f + r * 0.2f) * open, a(col, 0.55f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                               // film strips standing on the rim, unwinding
            float ang = Mth.TWO_PI * i / n + age * 0.006f, hgt = (1.5f + 0.4f * h(inst, i, 1) + 0.2f * Mth.sin(age * 0.09f + i)) * open;
            Vector3f foot = new Vector3f(Mth.cos(ang) * r * 0.97f, 0.05f, Mth.sin(ang) * r * 0.97f).add(c);
            buf.beam(ctx, FILM, VfxBlend.ADD, foot, new Vector3f(foot).add(0, hgt, 0), 0.3f, 0.2f, 1, -age * 0.012f * (i % 2 == 0 ? 1 : -1), a(col, 0.7f * fade), a(col, 0.1f));
        }
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {                                                               // frames drifting up and dissolving
            float ph = (age * 0.014f + h(inst, i, 2)) % 1f, ang = h(inst, i, 3) * Mth.TWO_PI, rr = r * (0.15f + 0.75f * h(inst, i, 4));
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.2f + ph * 2.4f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, FRAME, VfxBlend.ADD, p, 0.5f + 0.15f * h(inst, i, 5), Mth.sin(ph * 4f + i) * 0.4f, a(i % 3 == 0 ? hot(col, 0.4f) : col, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        for (int i = 0; i < ctx.seg(6, 3); i++) {
            float ph = (age * 0.02f + h(inst, i, 6)) % 1f, ang = h(inst, i, 7) * Mth.TWO_PI, rr = r * h(inst, i, 8);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rr, 0.1f + ph * 2.0f, Mth.sin(ang) * rr).add(c), 0.28f, age * 0.1f + i, a(WHITE, 0.85f * Mth.sin(ph * Mth.PI) * fade));
        }
        glow(buf, ctx, new Vector3f(c).add(0, 0.8f, 0), r * 0.6f, col, 0.15f * fade);
    }

    // ------------------------------------------------------------------ FX3: Flashback
    private void flashback(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), t = Mth.clamp(age / d, 0f, 1f);
        float u = Mth.clamp(inst.power, 0.4f, 6f) / 3f;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dirHint = ctx.rel(inst.to(ctx)).sub(c);
        int col = inst.color | 0xFF000000;
        float flash = 1f - ramp(t, 0f, 0.25f), flash2 = Math.max(0f, 1f - Math.abs(t - 0.38f) / 0.12f);   // the replay: a second, fainter flash
        Vector3f mid = new Vector3f(c).add(0, 0.9f * u, 0);
        glow(buf, ctx, mid, (1.4f + 1.5f * out(t)) * u, col, 0.5f * (1f - t));
        glow(buf, ctx, mid, 1.2f * u * flash, WHITE, 0.9f * flash);
        glow(buf, ctx, mid, 1.0f * u * flash2, col, 0.6f * flash2);
        buf.plane(REEL, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), new Vector3f(ctx.camRight).cross(ctx.camUp)).spin(age * 0.2f), (0.5f + 1.9f * out(ramp(t, 0f, 0.6f))) * u, a(hot(col, 0.4f), 0.9f * (1f - ramp(t, 0.35f, 1f))));
        ground(buf, RIPPLE, c, 0.05f, 3.0f * u * out(ramp(t, 0f, 0.8f)), age * 0.06f, a(col, 0.9f * (1f - ramp(t, 0.4f, 1f))));
        ground(buf, RIPPLE, c, 0.07f, 2.0f * u * out(ramp(t, 0.35f, 1f)), -age * 0.08f, a(hot(col, 0.4f), 0.7f * (1f - ramp(t, 0.6f, 1f))));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                               // frames flung out, spinning
            float ang = h(inst, i, 1) * Mth.TWO_PI, sp = (1.0f + 1.7f * h(inst, i, 2)) * u;
            if (dirHint.lengthSquared() > 0.5f) ang = ang * 0.6f + (float) Math.atan2(dirHint.z, dirHint.x) * 0.4f;
            float f = ramp(t, 0.02f, 1f);
            Vector3f p = new Vector3f(Mth.cos(ang) * sp * out(f), (0.3f + 1.8f * Mth.sin(Mth.PI * f * 0.9f) * (0.5f + h(inst, i, 3))) * u, Mth.sin(ang) * sp * out(f)).add(c);
            buf.billboard(ctx, FRAME, VfxBlend.ADD, p, (0.5f + 0.2f * h(inst, i, 4)) * u, f * 8f * (h(inst, i, 5) - 0.5f), a(i % 3 == 0 ? hot(col, 0.4f) : col, 0.9f * (1f - ramp(f, 0.6f, 1f))));
        }
        int m = ctx.seg(10, 5);
        for (int i = 0; i < m; i++) {                                                               // glass shards and sparks
            float ang = h(inst, i, 6) * Mth.TWO_PI, el = (h(inst, i, 7) - 0.3f) * 1.4f, sp = (0.8f + 1.8f * h(inst, i, 8)) * u, f = out(ramp(t, 0.02f, 0.9f));
            Vector3f p = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el) + 0.2f, Mth.sin(ang) * Mth.cos(el)).mul(sp * f).add(mid);
            buf.billboard(ctx, i % 2 == 0 ? SHARD : SPARK, VfxBlend.ADD, p, 0.36f * u * (1f - 0.5f * f), ang + age * 0.2f, a(i % 3 == 0 ? WHITE : col, 0.9f * (1f - ramp(t, 0.5f, 1f))));
        }
    }
}
