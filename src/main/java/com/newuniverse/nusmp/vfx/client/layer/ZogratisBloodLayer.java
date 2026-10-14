package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * 1.35.0 Vanica Zogratis (Megicula, blood and decay): crimson arcs, lances, scythe crescents, thorny sigils and falling blood.
 * <ul>
 *   <li>BLOOD_CRESCENT: a scythe crescent round 'from' opening toward 'to'; power = radius. Telegraph form (seed bit 0): the sector it will
 *       cut, dim, with a bright marker sweeping across it as the wind-up runs. Strike form: the crescent sweeps, white-hot edge, droplets flung.</li>
 *   <li>BLOOD_LANCE: a lance of blood from 'from' to 'to'; power = width. Telegraph: a crawling dashed aim line and a small sigil at the tail.
 *       Strike: the barbed lance flies down the line trailing a ribbon, a splash where it ends.</li>
 *   <li>BLOOD_SIGIL: a thorny sigil on the ground at 'from'; power = radius; its rim closes in over the life (time left) and thorns rise in
 *       the last third (seed bit 0 = telegraph: no thorns, dimmer).</li>
 *   <li>BLOOD_ARCS: three crimson arcs leap from 'from' to 'to' (whip, dash, bonds); power = size.</li>
 *   <li>BLOOD_DOWNPOUR: spears of blood falling on 'from' and splashing; power = radius.</li>
 * </ul>
 */
public class ZogratisBloodLayer extends AbstractVfxLayer {
    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.BLOOD_CRESCENT, VfxShape.BLOOD_LANCE, VfxShape.BLOOD_SIGIL, VfxShape.BLOOD_ARCS, VfxShape.BLOOD_DOWNPOUR);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BLOOD_CRESCENT -> 14;
            case BLOOD_LANCE -> 12;
            case BLOOD_SIGIL -> 40;
            case BLOOD_ARCS -> 14;
            default -> 18;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFD01830; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BLOOD_CRESCENT -> crescent(inst, ctx, buf);
            case BLOOD_LANCE -> lance(inst, ctx, buf);
            case BLOOD_SIGIL -> sigil(inst, ctx, buf);
            case BLOOD_ARCS -> arcs(inst, ctx, buf);
            case BLOOD_DOWNPOUR -> downpour(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ BLOOD_CRESCENT
    private void crescent(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        boolean tell = telegraph(inst);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(c);
        d.y = 0f;
        if (d.lengthSquared() < 1e-4f) d.set(0, 0, 1);
        d.normalize();
        VfxPose pose = VfxPose.facing(c, new Vector3f(0.18f, 1f, 0f).add(new Vector3f(d).mul(0.0f)));
        float mid = (float) Math.atan2(d.dot(pose.up()), d.dot(pose.right()));
        float fade = ElementFx.life(inst, age, 2, Math.min(6, inst.duration / 2f));
        float half = 1.15f;                                                                                       // 66 degrees each side
        int segs = ctx.seg(12, 6);
        if (tell) {
            buf.arc(BAND, VfxBlend.ADD, pose, R, 1.4f, mid - half, 2 * half, segs, a(col, 0.5f * fade));
            buf.arc(BAND, VfxBlend.ADD, pose.lift(0.02f), R * 0.5f, 0.8f, mid - half, 2 * half, segs, a(col, 0.3f * fade));
            float head = mid - half + 2 * half * t;                                                              // the blade's marker
            Vector3f p = pose.point(Mth.cos(head) * R, Mth.sin(head) * R);
            glow(buf, ctx, p, 0.9f, a(hot(col, 0.6f), 0.9f * fade));
            Vector3f q = pose.point(Mth.cos(head) * R * 0.2f, Mth.sin(head) * R * 0.2f);
            ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, q, p, 0.18f, a(WHITE, 0.5f * fade));
            return;
        }
        float sweep = outCubic(Mth.clamp(t * 2.2f, 0f, 1f));
        float tail = ease(Mth.clamp(t * 1.6f - 0.15f, 0f, 1f));
        float start = mid + half - 2 * half * sweep, len = 2 * half * (sweep - tail);
        if (len > 0.05f) {
            VfxPose tilt = VfxPose.facing(c, new Vector3f(0.3f * d.x, 1f, 0.3f * d.z));
            float m2 = (float) Math.atan2(d.dot(tilt.up()), d.dot(tilt.right()));
            float s2 = m2 + half - 2 * half * sweep;
            buf.arc(BAND, VfxBlend.ADD, tilt, R * 0.98f, 2.4f, s2, len, segs, a(col, fade));
            buf.arc(BAND, VfxBlend.ADD, tilt.lift(0.03f), R * 0.98f, 1.0f, s2, len, segs, a(WHITE, 0.95f * fade));
            buf.arc(BAND, VfxBlend.ALPHA, tilt.lift(-0.02f), R * 0.92f, 3.0f, s2, len, segs, a(0xFF3A0810, 0.55f * fade));
        }
        float flash = 1f - ramp(t, 0f, 0.3f);
        if (flash > 0f) glow(buf, ctx, new Vector3f(c).add(0, 1f, 0), R * 0.5f * (1f + flash), a(hot(col, 0.5f), 0.6f * flash));
        int n = ctx.seg(8, 3);
        for (int k = 0; k < n; k++) {                                                                             // droplets flung off the edge
            float ang = mid + (hash(inst.seed, k, 1) - 0.5f) * 2 * half;
            float dist = R * (0.8f + 1.0f * outCubic(t) * (0.5f + hash(inst.seed, k, 2)));
            Vector3f p = pose.point(Mth.cos(ang) * dist, Mth.sin(ang) * dist).add(0, 0.3f - 0.8f * t * t, 0);
            Vector3f v = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            along(buf, ctx, DROPLET, VfxBlend.ALPHA, p, v, 0.32f, a(0xFFB01020, (1f - ramp(t, 0.5f, 1f))));
        }
    }

    // ------------------------------------------------------------------ BLOOD_LANCE
    private void lance(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), W = Math.max(0.3f, inst.power);
        boolean tell = telegraph(inst);
        int col = inst.color | 0xFF000000;
        Vector3f a0 = ctx.rel(inst.from(ctx)), b0 = ctx.rel(inst.to(ctx));
        Vector3f dir = dir(a0, b0);
        float len = new Vector3f(b0).sub(a0).length();
        if (len < 0.3f) return;
        float fade = ElementFx.life(inst, age, 2, Math.min(5, inst.duration / 3f));
        if (tell) {
            int dashes = ctx.seg(8, 4);
            for (int k = 0; k < dashes; k++) {
                float s0 = ((k + (age * 0.08f) % 1f) / dashes) % 1f, s1 = s0 + 0.5f / dashes;
                if (s1 > 1f) continue;
                ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, new Vector3f(a0).lerp(b0, s0), new Vector3f(a0).lerp(b0, s1), 0.14f * W, a(hot(col, 0.3f), 0.7f * fade));
            }
            glow(buf, ctx, a0, 0.5f * W + 0.3f * Mth.sin(age * 0.5f), a(col, 0.8f * fade));
            ground(buf, BLOOD_SIGIL, VfxBlend.ADD, new Vector3f(a0).add(0, -0.7f, 0), 0.02f, 0.6f * W + 0.2f, age * 0.12f, a(col, 0.35f * fade));
            return;
        }
        float fly = outCubic(Mth.clamp(t * 1.5f, 0f, 1f));
        Vector3f head = new Vector3f(a0).lerp(b0, fly);
        float L = Math.min(len * 0.5f, 3.2f);
        Vector3f tailP = new Vector3f(head).sub(new Vector3f(dir).mul(L));
        Vector3f nose = new Vector3f(head).add(new Vector3f(dir).mul(0.4f));
        ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, new Vector3f(a0).lerp(head, 0f), head, 0.5f * W, a(col, 0.7f * fade));
        spear(buf, LANCE, VfxBlend.ALPHA, tailP, nose, 1.1f * W + 0.45f, a(0xFFE01830, fade));
        spear(buf, LANCE, VfxBlend.ADD, tailP, nose, 1.1f * W + 0.45f, a(hot(col, 0.5f), 0.7f * fade));
        glow(buf, ctx, nose, 0.7f * W + 0.2f, a(WHITE, 0.8f * fade));
        if (t > 0.55f) {                                                                                          // the splash where it ends
            float k = ramp(t, 0.55f, 1f);
            glow(buf, ctx, b0, 1.4f * W * (1f - k) + 0.3f, a(hot(col, 0.4f), 0.8f * (1f - k)));
            int n = ctx.seg(5, 3);
            for (int i = 0; i < n; i++) {
                float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, sp = 0.6f + hash(inst.seed, i, 2);
                Vector3f p = new Vector3f(b0).add(Mth.cos(ang) * sp * k * 1.4f, 0.4f * sp * Mth.sin(k * Mth.PI), Mth.sin(ang) * sp * k * 1.4f);
                buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, p, 0.3f, ang, a(0xFFB01020, 1f - k));
            }
        }
    }

    // ------------------------------------------------------------------ BLOOD_SIGIL
    private void sigil(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        boolean tell = telegraph(inst);
        float fade = ElementFx.life(inst, age, 6, Math.min(10, inst.duration / 3f));
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = outCubic(Mth.clamp(age / 8f, 0f, 1f)), r = R * open;
        ground(buf, UMBRA, VfxBlend.ALPHA, c, 0.03f, r, 0f, a(0xFF2A0508, 0.5f * fade));                         // the stain
        ground(buf, BLOOD_SIGIL, VfxBlend.ADD, c, 0.05f, r, age * 0.025f, a(col, (tell ? 0.55f : 0.9f) * fade));
        ground(buf, BLOOD_SIGIL, VfxBlend.ADD, c, 0.06f, r * 0.55f, -age * 0.05f, a(hot(col, 0.4f), 0.45f * fade));
        ground(buf, LENS, VfxBlend.ADD, c, 0.07f, R * (1f - 0.9f * t) / 0.9f, 0f, a(WHITE, 0.75f * fade));        // closing rim: time left
        int n = ctx.seg(8, 4);
        for (int k = 0; k < n; k++) {                                                                             // droplets rising out of the stain
            float ph = (age * 0.035f + hash(inst.seed, k, 1)) % 1f;
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI, rad = r * (0.2f + 0.7f * hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, ph * 1.8f, Mth.sin(ang) * rad);
            buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, p, 0.28f, 0f, a(0xFFC01428, fade * (1f - ph)));
        }
        if (!tell && t > 0.6f) {                                                                                  // thorns rise
            float up = ease(ramp(t, 0.6f, 0.95f));
            int th = ctx.seg(12, 5);
            for (int k = 0; k < th; k++) {
                float ang = (k + hash(inst.seed, k, 4) * 0.5f) / th * Mth.TWO_PI, rad = R * (0.35f + 0.6f * hash(inst.seed, k, 5));
                Vector3f base = new Vector3f(c).add(Mth.cos(ang) * rad, 0f, Mth.sin(ang) * rad);
                float h = (1.2f + 1.6f * hash(inst.seed, k, 6)) * up;
                spear(buf, LANCE, VfxBlend.ALPHA, base, new Vector3f(base).add(0, h, 0), 0.4f, a(0xFFD01830, fade));
            }
        }
    }

    // ------------------------------------------------------------------ BLOOD_ARCS
    private void arcs(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), S = Math.max(0.3f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f a0 = ctx.rel(inst.from(ctx)), b0 = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(b0).sub(a0);
        float len = d.length();
        if (len < 0.3f) return;
        Vector3f side = ElementFx.side(new Vector3f(d).normalize());
        float grow = outCubic(Mth.clamp(t * 2.2f, 0f, 1f)), fadeOut = 1f - ramp(t, 0.5f, 1f);
        int segs = ctx.seg(6, 3);
        for (int k = 0; k < 3; k++) {
            float bow = (k - 1) * 0.35f * len * (0.6f + 0.4f * hash(inst.seed, k, 1)) + (hash(inst.seed, k, 2) - 0.5f) * 0.6f;
            Vector3f mid = new Vector3f(a0).lerp(b0, 0.5f).add(new Vector3f(side).mul(bow)).add(0, len * 0.12f * (1f + k * 0.3f), 0);
            float tailS = ramp(t, 0.35f, 1f);
            curve(buf, RIBBON, VfxBlend.ADD, a0, mid, b0, tailS * grow, grow, segs, 0.8f * S, true, a(col, 0.5f * fadeOut), a(hot(col, 0.6f), 0.95f * fadeOut));
            if (grow < 1f || t < 0.6f) glow(buf, ctx, bez(a0, mid, b0, grow), 0.45f * S, a(WHITE, 0.85f * fadeOut));
        }
        glow(buf, ctx, a0, 0.9f * S * (1f - t), a(col, 0.7f * (1f - t)));
        glow(buf, ctx, b0, 1.1f * S * ramp(t, 0.3f, 0.5f) * (1f - t), a(hot(col, 0.5f), 0.8f * (1f - t)));
    }

    // ------------------------------------------------------------------ BLOOD_DOWNPOUR
    private void downpour(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(0.8f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 2, Math.min(6, inst.duration / 3f));
        ground(buf, LENS, VfxBlend.ADD, c, 0.05f, R / 0.9f, 0f, a(col, 0.6f * fade));
        int n = ctx.seg(12, 5);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.1f + hash(inst.seed, k, 1)) % 1f;
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI, rad = R * 0.9f * (float) Math.sqrt(hash(inst.seed, k, 3));
            float y = 9f * (1f - ph);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, y, Mth.sin(ang) * rad);
            Vector3f tip = new Vector3f(p).add(0, -2.6f, 0);
            spear(buf, LANCE, VfxBlend.ALPHA, p, tip, 0.45f, a(0xFFD01830, fade));
            spear(buf, LANCE, VfxBlend.ADD, p, tip, 0.45f, a(hot(col, 0.4f), 0.45f * fade));
        }
        int sp = ctx.seg(5, 3);
        for (int k = 0; k < sp; k++) {                                                                            // splashes where they land
            float ph = (age * 0.1f + hash(inst.seed, k, 1)) % 1f;
            if (ph > 0.25f) continue;
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI, rad = R * 0.9f * (float) Math.sqrt(hash(inst.seed, k, 3));
            ground(buf, SPLAT, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rad, 0f, Mth.sin(ang) * rad), 0.04f, 0.5f + ph * 2f, ang, a(0xFFA01020, fade * (1f - ph * 3f)));
        }
        if (t > 0.1f) glow(buf, ctx, new Vector3f(c).add(0, 0.4f, 0), R * 0.8f, a(hot(col, 0.3f), 0.25f * fade));
    }
}
