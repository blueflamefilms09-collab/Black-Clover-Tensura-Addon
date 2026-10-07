package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.56 Secre Swallowtail's Seal Magic (light-blue / cyan neon, square-edged geometry, white speed lines). Textures: tools/gen_seal_secre_textures.py (secre_*).
 * Default colour 0xFF5AD8FF; every shape tints its body with inst.color and pushes the cores toward white.
 * <ul>
 *   <li>SEAL_BASIC, the basic seal with its random combo: a dynamic magic circle lying on the ground at 'from', power = radius, duration = seal time (60).
 *       The spell rolls the combo and encodes it in the instance: inst.color = the elemental tint (red 0xFFFF4A3A burns: embers rise;
 *       blue 0xFF4AA8FF slows: the circle turns at half speed and frost nodes drift; purple 0xFFB04AFF crushes: streaks are pulled inward);
 *       inst.seed = shape + 3 * (orbs - 1), shape 0 = hexagram, 1 = concentric rings, 2 = star, orbs 1..5 (any other seed is folded in with
 *       floorMod, so seed 0..14 covers every combo). Glowing orbs circle the seal, one per orb.</li>
 *   <li>SEAL_BRANCH, Branching Array: from = the hands, power = reach. A white-blue core flashes and sharp square-edged lines (right angles only) shoot
 *       out along the six axes and fork twice into a 3D web with nodes on every tip; the web turns slowly and fades (duration 36).</li>
 *   <li>SEAL_CUBE, Eternal Prison: from = centre of the target, power = edge length, duration = prison time (60). A translucent glowing cube pops in
 *       round it, bright edges and corner nodes, white speed lines and plus-shaped sparks shooting off the edges; in the last 5 ticks it
 *       cracks open in a bright breaking flash.</li>
 *   <li>SEAL_WOUND, Wound Sealing: from = the grimoire, to = the ally, duration 40. A gentle double ribbon of blue energy weaves in an arc to the
 *       ally with small plus-shaped glints drifting along it, a soft glow round the grimoire, and a soft sealing flash with rising pluses at the ally.</li>
 *   <li>SEAL_ORBIT, Orbital Bind: from = centre of the ally, power = radius, duration 80. Three fast, precessing blue rings orbit a faint sphere with bright
 *       heads and speed trails; pluses glint on the shell.</li>
 * </ul>
 */
public class SealSecreLayer extends AbstractVfxLayer {
    static ResourceLocation t(String n) { return VfxTextures.byName(n); }
    static final ResourceLocation HEX = t("secre_hex"), RINGS = t("secre_rings"), STAR = t("secre_star"), CIRCLE = t("secre_circle"), LINE = t("secre_line"),
            NODE = t("secre_node"), FACE = t("secre_face"), SPEED = t("secre_speed"), PLUS = t("secre_plus"), CORE = t("secre_core"), HOOP = t("secre_hoop"),
            FLASH = t("secre_flash"), STREAM = t("secre_stream"), ORB = t("secre_orb");
    private static final Vector3f[] AXES = {new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SEAL_BASIC, VfxShape.SEAL_BRANCH, VfxShape.SEAL_CUBE, VfxShape.SEAL_WOUND, VfxShape.SEAL_ORBIT); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SEAL_BASIC -> 60;
            case SEAL_BRANCH -> 36;
            case SEAL_CUBE -> 60;
            case SEAL_WOUND -> 40;
            default -> 80;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF5AD8FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SEAL_BASIC -> basic(inst, ctx, buf);
            case SEAL_BRANCH -> branch(inst, ctx, buf);
            case SEAL_CUBE -> cube(inst, ctx, buf);
            case SEAL_WOUND -> wound(inst, ctx, buf);
            case SEAL_ORBIT -> orbit(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static Vector3f rotY(Vector3f v, float a) {
        float c = Mth.cos(a), s = Mth.sin(a);
        return new Vector3f(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }

    private static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, Vector3f side, float w0, float w1,
                               float v0, float v1, int argb) {
        int col = blend.grade(argb, 1f);
        Vector3f s0 = new Vector3f(side).mul(w0 * 0.5f), s1 = new Vector3f(side).mul(w1 * 0.5f);
        buf.quad(tex, blend, new Vector3f(a).sub(s0), new Vector3f(a).add(s0), new Vector3f(b).add(s1), new Vector3f(b).sub(s1), 0, v0, 1, v1, col, col);
    }

    /** Camera-facing side vector for a strip running along a to b. */
    private static Vector3f sideOf(Vector3f a, Vector3f b) {
        Vector3f d = new Vector3f(b).sub(a);
        Vector3f toCam = new Vector3f(a).add(b).mul(-0.5f);
        Vector3f s = d.cross(toCam);
        if (s.lengthSquared() < 1e-8f) return new Vector3f(1, 0, 0);
        return s.normalize();
    }

    private static int tint(int color) {
        int r = (color >> 16) & 255;
        return r > 200 ? 0 : (r > 130 ? 2 : 1);   // 0 red, 1 blue, 2 purple
    }

    // ------------------------------------------------------------------ SEAL_BASIC
    private void basic(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float f = life(inst, age, 8, 12), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        int design = (int) Math.floorMod(inst.seed, 3L), orbs = 1 + (int) Math.floorMod(inst.seed / 3L, 5L), tn = tint(inst.color);
        float spd = tn == 1 ? 0.5f : 1f;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.9f);
        VfxPose ground = VfxPose.ground(c);
        ResourceLocation main = design == 0 ? HEX : (design == 1 ? RINGS : STAR);
        VfxBloom.planeGlow(buf, ground, p, col, 0.7f * f);
        buf.plane(main, VfxBlend.ADD, ground.spin(age * 0.02f * spd), p * open, VfxVertexBuffer.withAlpha(col, f * 0.95f));
        buf.plane(CIRCLE, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.045f * spd), 0.7f * p * open, VfxVertexBuffer.withAlpha(light, f * 0.9f));
        buf.plane(design == 0 ? STAR : HEX, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.07f * spd), 0.38f * p * open, VfxVertexBuffer.withAlpha(hot, f * 0.85f));
        buf.ring(HOOP, VfxBlend.ADD, ground.lift(0.04f), p * 1.0f, p * 1.1f, ctx.seg(16, 10), 8, age * 0.02f * spd, VfxVertexBuffer.withAlpha(light, f * 0.8f));
        // the orbs
        for (int i = 0; i < orbs; i++) {
            float ang = age * 0.09f * spd + Mth.TWO_PI * i / orbs, hgt = 0.45f + 0.15f * Mth.sin(age * 0.13f + i * 1.9f);
            float rad = 0.92f * p * open;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rad, hgt, Mth.sin(ang) * rad);
            Vector3f tail = new Vector3f(c).add(Mth.cos(ang - 0.5f) * rad, hgt, Mth.sin(ang - 0.5f) * rad);
            streak(buf, ctx, SPEED, VfxBlend.ADD, tail, q, 0.16f, VfxVertexBuffer.withAlpha(WHITE, f * 0.8f));
            buf.billboard(ctx, ORB, VfxBlend.ADD, q, 0.42f, 0f, VfxVertexBuffer.withAlpha(hot, f));
            VfxBloom.glow(ctx, buf, q, 0.3f, col, f * 0.8f);
        }
        // the tint's own motion
        for (int i = 0; i < 6; i++) {
            float ph = (age * 0.03f + hash(inst.seed, i, 4)) % 1f, ang = hash(inst.seed, i, 1) * Mth.TWO_PI, rad = (0.2f + 0.75f * hash(inst.seed, i, 2)) * p;
            if (tn == 0) {
                Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rad, ph * 1.8f, Mth.sin(ang) * rad);
                buf.billboard(ctx, PLUS, VfxBlend.ADD, q, 0.22f, age * 0.1f, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
            } else if (tn == 2) {
                float r0 = (1 - ph) * p, r1 = r0 + 0.5f;
                streak(buf, ctx, SPEED, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * (r1 > p ? p : r1), 0.06f, Mth.sin(ang) * (r1 > p ? p : r1)),
                        new Vector3f(c).add(Mth.cos(ang) * r0, 0.06f, Mth.sin(ang) * r0), 0.14f, VfxVertexBuffer.withAlpha(light, f * Mth.sin(ph * Mth.PI)));
            } else if (i < 4) {
                Vector3f q = new Vector3f(c).add(Mth.cos(ang + age * 0.01f) * rad, 0.3f + ph * 0.8f, Mth.sin(ang + age * 0.01f) * rad);
                buf.billboard(ctx, NODE, VfxBlend.ADD, q, 0.26f, age * 0.03f, VfxVertexBuffer.withAlpha(light, f * Mth.sin(ph * Mth.PI)));
            }
        }
    }

    // ------------------------------------------------------------------ SEAL_BRANCH
    private static int perp(int axis, float h) {
        int[] opts = new int[4];
        int n = 0;
        for (int b = 0; b < 6; b++) if (b / 2 != axis / 2) opts[n++] = b;
        return opts[Math.min(3, (int) (h * 4))];
    }

    private void branch(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float f = life(inst, age, 0, 10);
        float gt = Mth.clamp(age / (inst.duration * 0.42f), 0, 1) * 1.25f;
        float yaw = age * 0.012f + hash(inst.seed, 0, 9) * Mth.PI;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f), hot = VfxVertexBuffer.whiten(col, 0.9f);
        float[] trunkLen = {0.36f, 0.26f, 0.18f};
        float[] subLen = {0.2f, 0.12f};
        // the ground array under the web
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.85f, 0)).spin(age * 0.03f), 0.8f * p * open, VfxVertexBuffer.withAlpha(col, f * 0.6f));
        for (int k = 0; k < 6; k++) {
            Vector3f pos = new Vector3f(c);
            int axis = k;
            for (int s = 0; s < 3; s++) {
                float start = s * 0.2f, g = Mth.clamp((gt - start) / 0.28f, 0, 1);
                if (g <= 0) break;
                Vector3f dir = rotY(AXES[axis], yaw);
                Vector3f end = new Vector3f(pos).add(new Vector3f(dir).mul(trunkLen[s] * p * g));
                streak(buf, ctx, LINE, VfxBlend.ADD, pos, end, 0.16f * (1 - 0.2f * s), VfxVertexBuffer.withAlpha(s == 0 ? hot : light, f));
                if (s < 2 && g >= 1) {
                    // a fork off the end of this segment
                    int sub = perp(axis, hash(inst.seed, k * 3 + s, 5));
                    Vector3f sp = new Vector3f(end);
                    int sa = sub;
                    for (int j = 0; j < 2; j++) {
                        float sg = Mth.clamp((gt - (start + 0.28f + j * 0.16f)) / 0.24f, 0, 1);
                        if (sg <= 0) break;
                        Vector3f se = new Vector3f(sp).add(new Vector3f(rotY(AXES[sa], yaw)).mul(subLen[j] * p * sg));
                        streak(buf, ctx, LINE, VfxBlend.ADD, sp, se, 0.11f, VfxVertexBuffer.withAlpha(light, f * 0.9f));
                        if (j == 1 || sg < 1) buf.billboard(ctx, NODE, VfxBlend.ADD, se, 0.3f, yaw, VfxVertexBuffer.withAlpha(hot, f));
                        sp = se;
                        sa = perp(sa, hash(inst.seed, k * 3 + s + 20 * (j + 1), 6));
                    }
                }
                pos = end;
                if (g >= 1) axis = perp(axis, hash(inst.seed, k, 7 + s));
            }
            if (gt >= 0.2f) buf.billboard(ctx, NODE, VfxBlend.ADD, pos, 0.4f, yaw, VfxVertexBuffer.withAlpha(WHITE, f));
        }
        // the core
        float flare = 1 - Mth.clamp(age / (inst.duration * 0.5f), 0, 1);
        buf.billboard(ctx, CORE, VfxBlend.ADD, c, (0.9f + 1.6f * flare) * Math.min(p, 3f) * 0.6f, age * 0.05f, VfxVertexBuffer.withAlpha(hot, f));
        VfxBloom.glow(ctx, buf, c, 0.5f * Math.min(p, 3f) * 0.6f, col, f);
        buf.billboard(ctx, RINGS, VfxBlend.ADD, c, 1.6f * p * VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1)), age * 0.1f,
                VfxVertexBuffer.withAlpha(light, f * (1 - Mth.clamp(age / 10f, 0, 1))));
    }

    // ------------------------------------------------------------------ SEAL_CUBE
    private void cube(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, dur = inst.duration;
        float br = Mth.clamp((age - (dur - 5f)) / 5f, 0, 1);
        float f = Mth.clamp(age / 3f, 0, 1) * (1 - br);
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 7f, 0, 1));
        float e = 0.5f * p * pop * (1 + 0.3f * br);
        float yaw = age * 0.012f + 0.4f;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.9f);
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.3f);
        // faces
        for (int i = 0; i < 6; i++) {
            Vector3f n = rotY(AXES[i], yaw);
            buf.plane(FACE, VfxBlend.ADD, VfxPose.facing(new Vector3f(c).add(new Vector3f(n).mul(e)), n), e, VfxVertexBuffer.withAlpha(col, f * 0.6f * pulse + 0.4f * br * (1 - br) * 2));
        }
        // corners and edges
        Vector3f[] cn = new Vector3f[8];
        for (int i = 0; i < 8; i++) cn[i] = new Vector3f(c).add(rotY(new Vector3f((i & 1) == 0 ? -e : e, (i & 2) == 0 ? -e : e, (i & 4) == 0 ? -e : e), yaw));
        for (int i = 0; i < 8; i++) {
            for (int b = 0; b < 3; b++) {
                int j = i ^ (1 << b);
                if (j > i) streak(buf, ctx, LINE, VfxBlend.ADD, cn[i], cn[j], 0.1f + 0.04f * p, VfxVertexBuffer.withAlpha(hot, f));
            }
            buf.billboard(ctx, NODE, VfxBlend.ADD, cn[i], 0.3f + 0.05f * p, yaw, VfxVertexBuffer.withAlpha(WHITE, f));
        }
        // speed lines and sparks shooting off the edges
        for (int i = 0; i < 14; i++) {
            int a = (int) (hash(inst.seed, i, 1) * 8) & 7, b = a ^ (1 << (int) (hash(inst.seed, i, 2) * 3));
            Vector3f on = new Vector3f(cn[a]).lerp(cn[b], hash(inst.seed, i, 3));
            Vector3f d = new Vector3f(on).sub(c);
            if (d.lengthSquared() < 1e-6f) continue;
            d.normalize();
            float ph = (age * 0.2f + hash(inst.seed, i, 4)) % 1f;
            float off = 0.1f + ph * 1.1f * Math.max(0.6f, p * 0.5f), ln = (0.5f + 0.8f * hash(inst.seed, i, 5)) * Math.max(0.7f, p * 0.5f);
            Vector3f s0 = new Vector3f(on).add(new Vector3f(d).mul(off)), s1 = new Vector3f(on).add(new Vector3f(d).mul(off + ln));
            streak(buf, ctx, SPEED, VfxBlend.ADD, s0, s1, 0.14f, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
        }
        for (int i = 0; i < 10; i++) {
            int a = (int) (hash(inst.seed, i + 40, 1) * 8) & 7, b = a ^ (1 << (int) (hash(inst.seed, i + 40, 2) * 3));
            Vector3f on = new Vector3f(cn[a]).lerp(cn[b], hash(inst.seed, i + 40, 3));
            Vector3f d = new Vector3f(on).sub(c).normalize();
            float ph = (age * 0.1f + hash(inst.seed, i + 40, 4)) % 1f;
            buf.billboard(ctx, PLUS, VfxBlend.ADD, new Vector3f(on).add(new Vector3f(d).mul(ph * 0.9f)), 0.26f, age * 0.1f + i, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
        }
        VfxBloom.glow(ctx, buf, c, 0.8f * p, col, f * 0.5f);
        // the breaking flash
        if (br > 0) {
            buf.billboard(ctx, FLASH, VfxBlend.ADD, c, 3.6f * p * (0.4f + 0.6f * br), age * 0.2f, VfxVertexBuffer.withAlpha(hot, 1 - br * 0.6f));
            buf.billboard(ctx, CORE, VfxBlend.ADD, c, 2.4f * p, 0f, VfxVertexBuffer.withAlpha(WHITE, 1 - br * 0.5f));
            VfxBloom.glow(ctx, buf, c, 1.4f * p, col, 1f);
            buf.billboard(ctx, RINGS, VfxBlend.ADD, c, 5.5f * p * br, age * 0.1f, VfxVertexBuffer.withAlpha(light, 1 - br));
        }
    }

    // ------------------------------------------------------------------ SEAL_WOUND
    private void wound(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration;
        float f = life(inst, age, 4, 8);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1e-3f) { d.set(0, 0, 1); len = 1; } else d.div(len);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f), hot = VfxVertexBuffer.whiten(col, 0.9f);
        float grow = VfxAnim.easeInOutSine(Mth.clamp(age / (dur * 0.35f), 0, 1));
        float arr = Mth.clamp((age - dur * 0.3f) / (dur * 0.7f), 0, 1);
        Vector3f m = new Vector3f(a).add(b).mul(0.5f).add(0, Math.min(1.2f, 0.2f * len + 0.2f), 0);
        // the grimoire's aura
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.facing(new Vector3f(a).add(new Vector3f(d).mul(0.3f)), d).spin(age * 0.08f), 0.55f, VfxVertexBuffer.withAlpha(col, f * 0.8f));
        VfxBloom.glow(ctx, buf, a, 0.55f, col, f);
        // the double ribbon
        int n = 9;
        for (int r = 0; r < 2; r++) {
            Vector3f prev = null;
            Vector3f prevSide = null;
            for (int i = 0; i <= n; i++) {
                float s = grow * i / n, o = 1 - s;
                Vector3f pt = new Vector3f(a).mul(o * o).add(new Vector3f(m).mul(2 * o * s)).add(new Vector3f(b).mul(s * s));
                float sw = 0.08f * Mth.sin(Mth.PI * s) * Mth.cos(s * 9f - age * 0.25f + r * Mth.PI);
                Vector3f sd = sideOf(a, b);
                pt.add(new Vector3f(sd).mul(sw));
                if (prev != null) ribbon(buf, STREAM, VfxBlend.ADD, prev, pt, sideOf(prev, pt), 0.4f, 0.4f, (float) (i - 1) / n, (float) i / n, VfxVertexBuffer.withAlpha(r == 0 ? col : light, f * 0.8f));
                prev = pt;
            }
        }
        // plus glints drifting along it
        for (int i = 0; i < 8; i++) {
            float s = ((age * 0.025f + i / 8f) % 1f) * grow, o = 1 - s;
            Vector3f pt = new Vector3f(a).mul(o * o).add(new Vector3f(m).mul(2 * o * s)).add(new Vector3f(b).mul(s * s));
            pt.add(0, 0.15f * Mth.sin(age * 0.2f + i), 0);
            buf.billboard(ctx, PLUS, VfxBlend.ADD, pt, 0.2f + 0.08f * (i % 3), age * 0.05f + i, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(Mth.PI * Mth.clamp(s / Math.max(grow, 0.01f), 0, 1))));
        }
        // the soft sealing flash at the ally
        if (arr > 0) {
            float k = Mth.sin(Mth.PI * Mth.clamp(arr * 1.1f, 0, 1));
            VfxBloom.glow(ctx, buf, b, 0.9f, col, f * k);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, b, 1.8f * (0.4f + 0.6f * arr), age * 0.05f, VfxVertexBuffer.withAlpha(hot, f * k * 0.7f));
            buf.billboard(ctx, RINGS, VfxBlend.ADD, b, 2.4f * VfxAnim.easeOutCubic(arr), age * 0.08f, VfxVertexBuffer.withAlpha(light, f * (1 - arr)));
            for (int i = 0; i < 6; i++) {
                float ph = (arr * 1.2f + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI;
                buf.billboard(ctx, PLUS, VfxBlend.ADD, new Vector3f(b).add(Mth.cos(ang) * 0.45f, -0.4f + ph * 1.2f, Mth.sin(ang) * 0.45f), 0.26f, 0f,
                        VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
            }
        }
    }

    // ------------------------------------------------------------------ SEAL_ORBIT
    private void orbit(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float f = life(inst, age, 6, 10), pop = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.9f);
        float r = p * pop;
        // the sphere
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 2.1f * r, 0f, VfxVertexBuffer.withAlpha(col, f * 0.3f));
        buf.billboard(ctx, ORB, VfxBlend.ADD, c, 0.5f * r, 0f, VfxVertexBuffer.withAlpha(hot, f * 0.7f));
        VfxBloom.glow(ctx, buf, c, 0.4f * r, col, f * 0.6f);
        for (int k = 0; k < 3; k++) {
            float tilt = 0.45f + 0.5f * k, phi = age * (k % 2 == 0 ? 0.05f : -0.065f) + k * 2.1f;
            Vector3f axis = new Vector3f(Mth.sin(tilt) * Mth.cos(phi), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(phi));
            VfxPose pose = VfxPose.facing(c, axis);
            float rr = r * (0.98f + 0.07f * k);
            hoop(buf, HOOP, VfxBlend.ADD, pose, rr, 0.07f * p + 0.04f, ctx.seg(16, 10), 4, age * (0.09f + 0.05f * k) * (k % 2 == 0 ? 1 : -1),
                    VfxVertexBuffer.withAlpha(k == 1 ? light : col, f * 0.9f));
            float th = age * (0.32f + 0.08f * k) * (k % 2 == 0 ? 1 : -1);
            Vector3f head = pose.point(Mth.cos(th) * rr, Mth.sin(th) * rr);
            float dirSign = k % 2 == 0 ? 1 : -1;
            for (int j = 1; j <= 2; j++) {
                float tt = th - dirSign * 0.35f * j;
                streak(buf, ctx, SPEED, VfxBlend.ADD, pose.point(Mth.cos(tt) * rr, Mth.sin(tt) * rr), head, 0.16f, VfxVertexBuffer.withAlpha(WHITE, f * (0.9f - 0.3f * j)));
            }
            buf.billboard(ctx, ORB, VfxBlend.ADD, head, 0.36f, 0f, VfxVertexBuffer.withAlpha(hot, f));
        }
        for (int i = 0; i < 6; i++) {
            float az = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.05f, el = (hash(inst.seed, i, 2) - 0.5f) * Mth.PI * 0.9f;
            float ph = (age * 0.04f + hash(inst.seed, i, 3)) % 1f;
            Vector3f q = new Vector3f(c).add(Mth.cos(az) * Mth.cos(el) * r, Mth.sin(el) * r, Mth.sin(az) * Mth.cos(el) * r);
            buf.billboard(ctx, PLUS, VfxBlend.ADD, q, 0.24f, age * 0.08f + i, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
        }
    }
}
