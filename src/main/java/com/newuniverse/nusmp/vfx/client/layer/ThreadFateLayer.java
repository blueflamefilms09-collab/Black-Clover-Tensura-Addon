package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Why: Vanessa's Thread Magic got four visuals that all have to read as the same material, red thread, and sit next to the existing
 * thread effects (Arachne's Web, the puppet strings, the Rouge cat on the head) without looking like a different mod: the same
 * wind-streak thread texture and the same palette (0xFF3355, lightened toward white where threads cross or flash), glints running
 * along the strands, and bloom at the points of action. Every effect eases in, holds and unravels out rather than popping.
 * <ul>
 *   <li>ROUGE_FATE_TAP: 'to' (followed with the ally) is the chest. A flash, a ring of threads that burst outward and curl back, a
 *       turning loop of thread being re-woven and a spray of glints: fate rewritten. Power >= 2.5 is Rouge unravelling instead:
 *       more and longer strands drifting up and apart, no ring.</li>
 *   <li>THREAD_COCOON: a cocoon wound round the followed body from the feet upward (rings of thread plus long strands and a faint
 *       web membrane), pulsing while it holds and coming apart in the last few ticks. Power = size (height / 1.8).</li>
 *   <li>THREAD_TRAP_LINE: three taut filaments from 'from' to 'to' that flash white-hot when laid and settle to a faint red.</li>
 *   <li>THREAD_SKIN: a faint skin of thread loops spiralling up the followed body, a few glints on it. Power = size.</li>
 * </ul>
 * Budget: every shape stays well under the 400 vertex cap of one activation (a quad is 4 vertices) and thins with the particle setting.
 */
public class ThreadFateLayer extends AbstractVfxLayer {
    private static final int RED = 0xFFFF3355;
    /** The web membrane texture Arachne's Web already uses. */
    private static final net.minecraft.resources.ResourceLocation WEB = VfxTextures.byName("arc_web");

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.ROUGE_FATE_TAP, VfxShape.THREAD_COCOON, VfxShape.THREAD_TRAP_LINE, VfxShape.THREAD_SKIN);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case ROUGE_FATE_TAP -> 30;
            case THREAD_COCOON -> 80;
            case THREAD_TRAP_LINE -> 50;
            case THREAD_SKIN -> 30;
            default -> 30;
        };
    }

    @Override public int defaultColor(VfxShape s) { return RED; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case ROUGE_FATE_TAP -> tap(inst, ctx, buf);
            case THREAD_COCOON -> cocoon(inst, ctx, buf);
            case THREAD_TRAP_LINE -> trap(inst, ctx, buf);
            case THREAD_SKIN -> skin(inst, ctx, buf);
            default -> { }
        }
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    private static float ramp(float t, float s, float e) { return Mth.clamp((t - s) / Math.max(1e-4f, e - s), 0f, 1f); }

    /** One thread: a camera-facing streak. */
    private static void thread(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, Vector3f q, float width, int argb) {
        streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, p, q, width, argb);
    }

    private static void glint(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, float spin, int argb) {
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, size, spin, argb);
    }

    // ------------------------------------------------------------------ Rouge taps / unravels
    private void tap(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        boolean unravel = inst.power >= 2.5f;
        float fade = life(inst, age, 1, unravel ? 14 : 12);
        int col = inst.color | 0xFF000000, hot = VfxVertexBuffer.whiten(col, 0.55f);
        Vector3f c = ctx.rel(inst.to(ctx));
        float burst = VfxAnim.easeOutCubic(Mth.clamp(age / (unravel ? 16f : 9f), 0, 1));
        float flash = (1f - ramp(age, 0, 8)) * (unravel ? 0.6f : 1f);
        VfxBloom.glow(ctx, buf, c, (unravel ? 1.0f : 1.9f) * (0.4f + 0.6f * burst), col, 0.7f * fade * (0.35f + flash));
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, c, (unravel ? 0.8f : 1.6f) * flash + 0.001f, age * 0.06f, a(0xFFFFFFFF, flash * fade));
        int n = ctx.seg(unravel ? 12 : 9, 5);
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, tilt = (hash(inst.seed, i, 2) - 0.5f) * (unravel ? 1.8f : 1.1f);
            Vector3f d = new Vector3f(Mth.cos(ang), tilt, Mth.sin(ang)).normalize();
            float reach = (unravel ? 1.5f : 1.1f) * (0.6f + 0.8f * hash(inst.seed, i, 3)) * burst;
            Vector3f lift = new Vector3f(0, unravel ? 0.9f * burst : 0.15f * Mth.sin(burst * Mth.PI), 0);
            Vector3f p0 = new Vector3f(c).add(new Vector3f(d).mul(reach * 0.18f));
            Vector3f mid = new Vector3f(c).add(new Vector3f(d).mul(reach * 0.62f)).add(lift).add(0, 0.12f * Mth.sin(age * 0.3f + i), 0);
            Vector3f p1 = new Vector3f(c).add(new Vector3f(d).mul(reach)).add(new Vector3f(lift).mul(1.4f));
            thread(buf, ctx, p0, mid, 0.05f, a(col, 0.95f * fade));
            thread(buf, ctx, mid, p1, 0.035f, a(hot, 0.8f * fade));
            if (i % 2 == 0) glint(buf, ctx, p1, 0.26f, age * 0.2f + i, a(0xFFFFFFFF, fade * (1f - burst * 0.5f)));
        }
        if (unravel) {
            int m = ctx.seg(6, 3);
            for (int k = 0; k < m; k++) {                                                        // motes of thread rising off her
                float ph = (age * 0.03f + hash(inst.seed, k, 5)) % 1f;
                Vector3f p = new Vector3f(c).add((hash(inst.seed, k, 6) - 0.5f) * 0.9f, ph * 1.4f - 0.2f, (hash(inst.seed, k, 7) - 0.5f) * 0.9f);
                glint(buf, ctx, p, 0.2f, age * 0.1f + k, a(hot, 0.85f * fade * (1f - ph)));
            }
            return;
        }
        // the ring of "fate rewritten": a turning loop of thread being woven closed
        float ringR = 0.45f + 0.9f * VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        int segs = ctx.seg(14, 8);
        float woven = ramp(age, 1, 11);
        for (int k = 0; k < segs; k++) {
            if ((float) k / segs > woven) break;
            float a0 = Mth.TWO_PI * k / segs + age * 0.12f, a1 = Mth.TWO_PI * (k + 1) / segs + age * 0.12f;
            Vector3f u = new Vector3f(Mth.cos(a0) * ringR, 0.18f * Mth.sin(a0 * 2f + age * 0.2f), Mth.sin(a0) * ringR);
            Vector3f v = new Vector3f(Mth.cos(a1) * ringR, 0.18f * Mth.sin(a1 * 2f + age * 0.2f), Mth.sin(a1) * ringR);
            thread(buf, ctx, new Vector3f(c).add(u), new Vector3f(c).add(v), 0.05f, a(hot, 0.9f * fade));
        }
        int g = ctx.seg(6, 3);
        for (int k = 0; k < g; k++) {
            float ph = ((age * 0.05f) + hash(inst.seed, k, 8)) % 1f, ang = hash(inst.seed, k, 9) * Mth.TWO_PI + age * 0.1f;
            glint(buf, ctx, new Vector3f(c).add(Mth.cos(ang) * ringR, ph * 1.0f - 0.2f, Mth.sin(ang) * ringR), 0.22f, age * 0.2f, a(0xFFFFFFFF, fade * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ the cocoon
    private void cocoon(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        float wind = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));                       // wound from the feet up
        float undo = ramp(age, inst.duration - 7f, inst.duration);                        // and coming apart at the end
        float fade = life(inst, age, 2, 7);
        int col = inst.color | 0xFF000000, hot = VfxVertexBuffer.whiten(col, 0.5f);
        Vector3f foot = ctx.rel(inst.from(ctx));
        float H = 1.8f * S, R = 0.55f * S;
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.35f);
        buf.billboard(ctx, WEB, VfxBlend.ALPHA, new Vector3f(foot).add(0, H * 0.5f, 0), 2.3f * S * wind + 0.01f, age * 0.01f, a(col, 0.28f * fade));
        VfxBloom.glow(ctx, buf, new Vector3f(foot).add(0, H * 0.5f, 0), 1.6f * S * wind, col, 0.35f * fade * pulse);
        int rings = ctx.seg(6, 4), segs = ctx.seg(6, 4);
        for (int r = 0; r < rings; r++) {
            float h = H * (r + 0.5f) / rings;
            if (h > H * wind) break;
            float rad = R * (0.7f + 0.3f * Mth.sin(Mth.PI * (r + 0.5f) / rings)) * (1f + undo * 0.9f);
            float spin = age * 0.05f * (r % 2 == 0 ? 1 : -1) + r * 0.7f;
            for (int k = 0; k < segs; k++) {
                float a0 = spin + Mth.TWO_PI * k / segs, a1 = spin + Mth.TWO_PI * (k + 1) / segs;
                Vector3f p = new Vector3f(foot).add(Mth.cos(a0) * rad, h + 0.05f * Mth.sin(a0 * 3 + age * 0.2f), Mth.sin(a0) * rad);
                Vector3f q = new Vector3f(foot).add(Mth.cos(a1) * rad, h + 0.05f * Mth.sin(a1 * 3 + age * 0.2f), Mth.sin(a1) * rad);
                thread(buf, ctx, p, q, 0.06f * S, a(r % 2 == 0 ? col : hot, 0.95f * fade * (1f - undo * 0.5f)));
            }
        }
        int strands = ctx.seg(5, 3);
        for (int s = 0; s < strands; s++) {                                                // long strands over the top
            float ang = hash(inst.seed, s, 1) * Mth.TWO_PI;
            Vector3f lo = new Vector3f(foot).add(Mth.cos(ang) * R * 0.8f, 0.05f, Mth.sin(ang) * R * 0.8f);
            Vector3f mid = new Vector3f(foot).add(Mth.cos(ang + 0.9f) * R * 1.0f, H * 0.5f * wind, Mth.sin(ang + 0.9f) * R * 1.0f);
            Vector3f hi = new Vector3f(foot).add(Mth.cos(ang + 1.8f) * R * 0.6f, H * wind, Mth.sin(ang + 1.8f) * R * 0.6f);
            thread(buf, ctx, lo, mid, 0.045f * S, a(hot, 0.9f * fade));
            thread(buf, ctx, mid, hi, 0.045f * S, a(hot, 0.9f * fade));
            float gl = ((age * 0.04f) + hash(inst.seed, s, 2)) % 1f;
            glint(buf, ctx, gl < 0.5f ? new Vector3f(lo).lerp(mid, gl * 2f) : new Vector3f(mid).lerp(hi, gl * 2f - 1f), 0.22f, age * 0.3f, a(0xFFFFFFFF, fade * wind));
        }
        if (undo > 0f) {                                                                    // loose ends drifting off as it unravels
            int m = ctx.seg(6, 3);
            for (int k = 0; k < m; k++) {
                float ang = hash(inst.seed, k, 3) * Mth.TWO_PI, h = H * hash(inst.seed, k, 4);
                Vector3f p = new Vector3f(foot).add(Mth.cos(ang) * R * (1f + undo), h + undo * 0.5f, Mth.sin(ang) * R * (1f + undo));
                glint(buf, ctx, p, 0.2f, age * 0.15f + k, a(hot, 0.8f * (1f - undo)));
            }
        }
    }

    // ------------------------------------------------------------------ a laid trap line
    private void trap(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), H = Math.max(0.5f, inst.power);
        float fade = life(inst, age, 1, 12);
        float laid = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));                      // shot from the first node to the next
        float flash = 1f - ramp(age, 4, 22);
        int col = inst.color | 0xFF000000, hot = VfxVertexBuffer.whiten(col, 0.7f);
        Vector3f p = ctx.rel(inst.from(ctx)), q = ctx.rel(inst.to(ctx));
        Vector3f end = new Vector3f(p).lerp(q, laid);
        float len = new Vector3f(q).sub(p).length();
        for (int k = 0; k < 3; k++) {
            float h = H * (0.2f + 0.35f * k) - 0.35f;
            float sag = 0.08f * Mth.sin(Mth.PI * laid) * (1f + 0.5f * k);
            Vector3f a0 = new Vector3f(p).add(0, h, 0), b0 = new Vector3f(end).add(0, h, 0);
            Vector3f m = new Vector3f(a0).lerp(b0, 0.5f).sub(0, sag + 0.04f * Mth.sin(age * 0.25f + k), 0);
            int c = VfxVertexBuffer.lerpColor(col, hot, flash);
            thread(buf, ctx, a0, m, 0.035f, a(c, (0.35f + 0.6f * flash) * fade));
            thread(buf, ctx, m, b0, 0.035f, a(c, (0.35f + 0.6f * flash) * fade));
            float gl = ((age * 0.05f) + k * 0.31f) % 1f;
            glint(buf, ctx, new Vector3f(a0).lerp(b0, gl), 0.16f, age * 0.2f, a(0xFFFFFFFF, fade * (0.3f + 0.7f * flash) * Mth.sin(gl * Mth.PI)));
        }
        glint(buf, ctx, end, 0.5f * (0.4f + flash), age * 0.1f, a(hot, fade * (0.4f + flash)));
        if (len > 0.1f) VfxBloom.glow(ctx, buf, new Vector3f(p).lerp(q, 0.5f).add(0, H * 0.4f - 0.35f, 0), 0.8f, col, 0.25f * fade * flash);
    }

    // ------------------------------------------------------------------ the thread skin
    private void skin(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        float fade = life(inst, age, 6, 8);
        int col = inst.color | 0xFF000000, hot = VfxVertexBuffer.whiten(col, 0.45f);
        Vector3f foot = ctx.rel(inst.from(ctx));
        float R = 0.46f * S, H = 1.8f * S;
        int loops = 3, segs = ctx.seg(8, 5);
        for (int l = 0; l < loops; l++) {
            float baseH = H * (0.18f + 0.34f * l);
            float spin = age * 0.07f * (l % 2 == 0 ? 1 : -1) + l * 1.3f;
            for (int k = 0; k < segs; k++) {
                float a0 = spin + Mth.TWO_PI * k / segs, a1 = spin + Mth.TWO_PI * (k + 1) / segs;
                float rad0 = R * (1f + 0.08f * Mth.sin(a0 * 2f + age * 0.15f)), rad1 = R * (1f + 0.08f * Mth.sin(a1 * 2f + age * 0.15f));
                Vector3f p = new Vector3f(foot).add(Mth.cos(a0) * rad0, baseH + 0.22f * S * Mth.sin(a0), Mth.sin(a0) * rad0);
                Vector3f q = new Vector3f(foot).add(Mth.cos(a1) * rad1, baseH + 0.22f * S * Mth.sin(a1), Mth.sin(a1) * rad1);
                thread(buf, ctx, p, q, 0.035f * S, a(l == 1 ? hot : col, 0.5f * fade));
            }
        }
        int g = ctx.seg(4, 2);
        for (int k = 0; k < g; k++) {
            float ph = ((age * 0.03f) + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI + age * 0.08f;
            glint(buf, ctx, new Vector3f(foot).add(Mth.cos(ang) * R, ph * H, Mth.sin(ang) * R), 0.16f * S, age * 0.2f, a(0xFFFFFFFF, 0.8f * fade * Mth.sin(ph * Mth.PI)));
        }
    }
}
