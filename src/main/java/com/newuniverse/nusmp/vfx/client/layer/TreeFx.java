package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxAnim;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 1.93.0 nature-magic rebuild: the drawing blocks the six layers of Tree, Vine, Plant, Poison Plant, Mud and Mucus Magic ({@link TreeLayer}, {@link VineLayer}, {@link PlantLayer},
 * {@link PoisonPlantLayer}, {@link MudLayer}, {@link MucusLayer}) are composed from. Every block is a few quads (a vertex budget of 400 per activation is shared by all of them) and works
 * from the age and the seed only, never from state, like the Time Magic layers: camera-facing ribbons, twisting strands, standing sprites that grow out of the ground, sigils lying on the
 * ground, debris that flies out and falls, sprites that rise and fade. Positions are camera relative (the layer passes {@code ctx.rel(...)}).
 */
public final class TreeFx {
    private TreeFx() {}

    public static final int WHITE = 0xFFFFFFFF;

    // ------------------------------------------------------------------ small helpers
    public static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }

    public static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }

    /** The colour mixed toward the caster's tint by k (the sprites are coloured themselves, the tint only leans them). */
    public static int lean(int base, VfxInstance inst, float k) { return VfxVertexBuffer.lerpColor(base, inst.color | 0xFF000000, k); }

    public static float sm(float e0, float e1, float x) {
        float k = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return k * k * (3 - 2 * k);
    }

    public static float life(VfxInstance inst, float age, float in, float out) { return ElementFx.life(inst, age, in, out); }

    /** A direction on the unit sphere (or the upper cap) from two 0..1 numbers. */
    public static Vector3f dirOf(float u, float v, boolean up) {
        float z = up ? 0.12f + 0.88f * u : u * 2f - 1f;
        float s = Mth.sqrt(Math.max(0f, 1f - z * z)), ang = v * Mth.TWO_PI;
        return new Vector3f(Mth.cos(ang) * s, z, Mth.sin(ang) * s);
    }

    // ------------------------------------------------------------------ flat shapes
    /** Camera-facing ribbon from a (texture bottom) to b (texture top); the sprite's V runs along it from vA at a to vB at b. */
    public static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wA, float wB, float vA, float vB, int cA, int cB) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-8f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-10f) return;
        side.normalize();
        Vector3f sa = new Vector3f(side).mul(wA * 0.5f), sb = new Vector3f(side).mul(wB * 0.5f);
        buf.quad(tex, blend, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb),
                0, vB, 1, vA, blend.grade(cA, 0.8f), blend.grade(cB, 0.8f));
    }

    /** An upright sprite that turns about the vertical axis to face the camera, base centre on the ground, lean shifts the top sideways. */
    public static void standing(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f base, float w, float hgt, boolean flip, float lean, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0); else r.normalize();
        Vector3f hw = new Vector3f(r).mul(w * 0.5f);
        Vector3f up = new Vector3f(r).mul(lean).add(0, hgt, 0);
        int top = blend.grade(argb, 0.8f), bot = blend.grade(argb, 0.5f);
        buf.quad(tex, blend, new Vector3f(base).sub(hw), new Vector3f(base).add(hw), new Vector3f(base).add(hw).add(up), new Vector3f(base).sub(hw).add(up),
                flip ? 1 : 0, 0, flip ? 0 : 1, 1, bot, top);
    }

    /** A flat sprite lying on the ground at c (camera relative), turned by spin radians. */
    public static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float radius, float spin, int argb, float lift) {
        buf.plane(tex, blend, VfxPose.ground(c).spin(spin).lift(lift), radius, argb);
    }

    // ------------------------------------------------------------------ the line from F to T
    /** The frame of a line from F to T: axis (unit), length, and two unit vectors across it. */
    public record Line(Vector3f f, Vector3f t, Vector3f axis, float len, Vector3f u, Vector3f v) {
        public static Line of(Vector3f f, Vector3f t) {
            Vector3f d = new Vector3f(t).sub(f);
            if (d.lengthSquared() < 0.09f) { d.set(0, 0, 3); t = new Vector3f(f).add(d); }
            float len = d.length();
            Vector3f axis = new Vector3f(d).div(len);
            Vector3f u = new Vector3f(axis).cross(0, 1, 0);
            if (u.lengthSquared() < 1e-4f) u.set(1, 0, 0);
            u.normalize();
            Vector3f v = new Vector3f(axis).cross(u).normalize();
            return new Line(f, t, axis, len, u, v);
        }

        public Vector3f at(float s) { return new Vector3f(f).add(axis.x * len * s, axis.y * len * s, axis.z * len * s); }
    }

    /** A point of a strand winding round the line: s = 0..1 along it, amp = radius of the winding at its fullest, freq = turns, spin = phase, strand = 0..1 offset. */
    public static Vector3f wind(Line l, float s, float amp, float freq, float spin, float strand) {
        float env = (float) Math.pow(Mth.sin(Mth.PI * Mth.clamp(s, 0f, 1f)), 0.6);
        float ph = s * freq * Mth.TWO_PI - spin + strand * Mth.PI;
        float c = Mth.cos(ph) * amp * env, sn = Mth.sin(ph) * amp * env;
        Vector3f p = l.at(s);
        return p.add(l.u.x * c + l.v.x * sn, l.u.y * c + l.v.y * sn, l.u.z * c + l.v.z * sn);
    }

    /**
     * Strands winding round the line between tail and head (both 0..1), each a chain of n ribbon pieces textured with tex (V runs along it). Returns nothing; the head position is l.at(head).
     * width is the full width of a piece at its fullest; the alpha rises along the strand (tail faint, head strong).
     */
    public static void strands(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Line l, float tail, float head, int strands, int n, float amp, float freq, float spin,
                               float width, int argb, float fade) {
        for (int st = 0; st < strands; st++) {
            Vector3f prev = wind(l, tail, amp, freq, spin, st / (float) strands);
            for (int i = 1; i <= n; i++) {
                float s1 = Mth.lerp(i / (float) n, tail, head);
                Vector3f cur = wind(l, s1, amp, freq, spin, st / (float) strands);
                float f0 = (i - 1f) / n, f1 = i / (float) n;
                float w0 = width * (0.45f + 0.55f * sm(0f, 0.6f, f0)), w1 = width * (0.45f + 0.55f * sm(0f, 0.6f, f1));
                strip(buf, tex, blend, prev, cur, w0, w1, s1 * l.len / (4f * width), (s1 + 0.0f) * l.len / (4f * width) + 0.2f, a(argb, fade * (0.4f + 0.6f * f0)), a(argb, fade * (0.4f + 0.6f * f1)));
                prev = cur;
            }
        }
    }

    // ------------------------------------------------------------------ bursts
    /** An expanding, turning star with a glow: the flash of an impact. t = 0..1 of the burst. */
    public static void flash(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation star, ResourceLocation glow, Vector3f c, float size, float t, float age, int argb, int glowArgb) {
        if (t <= 0f || t >= 1f) return;
        float e = VfxAnim.easeOutCubic(t), out = 1f - t;
        buf.billboard(ctx, star, VfxBlend.ALPHA, c, (0.5f + 1.8f * e) * size, age * 0.12f, a(argb, out));
        buf.billboard(ctx, glow, VfxBlend.ADD, c, (0.7f + 1.5f * e) * size, 0f, a(glowArgb, out * 0.7f));
    }

    /**
     * n pieces of debris thrown out of c: they fly out fast and fall under gravity, turning, fading near the end. t = 0..1; dirs come from the seed; the spread is in blocks.
     */
    public static void debris(VfxVertexBuffer buf, VfxRenderContext ctx, VfxInstance inst, ResourceLocation tex, Vector3f c, int n, float spread, float size, float t, float age, int argb, int salt, boolean up) {
        if (t <= 0f || t >= 1f) return;
        float e = VfxAnim.easeOutCubic(Mth.clamp(t * 1.4f, 0, 1)), out = 1f - t;
        for (int i = 0; i < n; i++) {
            Vector3f d = dirOf(h(inst, i, salt), h(inst, i, salt + 1), up).mul(spread * (0.55f + 0.45f * h(inst, i, salt + 2)) * e);
            Vector3f p = new Vector3f(c).add(d.x, d.y - 0.9f * t * t * spread * 0.5f, d.z);
            buf.billboard(ctx, tex, VfxBlend.ALPHA, p, size * (0.7f + 0.6f * h(inst, i, salt + 3)), age * 0.25f * (i % 2 == 0 ? 1 : -1) + i * 1.7f, a(argb, out));
        }
    }

    /** n sprites that start at random spots on the ground disc of the given radius round c and rise (or sink when rise < 0), fading in and out; each on its own phase. */
    public static void drift(VfxVertexBuffer buf, VfxRenderContext ctx, VfxInstance inst, ResourceLocation tex, VfxBlend blend, Vector3f c, int n, float radius, float rise, float size,
                             float age, float speed, int argb, float fade, int salt) {
        for (int i = 0; i < n; i++) {
            float k = (h(inst, i, salt) + age * speed) % 1f;
            float ang = h(inst, i, salt + 1) * Mth.TWO_PI + age * 0.02f * (i % 2 == 0 ? 1 : -1), r = radius * Mth.sqrt(h(inst, i, salt + 2));
            float y = rise >= 0 ? rise * k : rise * (k - 1f);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * r, y, Mth.sin(ang) * r);
            buf.billboard(ctx, tex, blend, p, size * (0.6f + 0.6f * h(inst, i, salt + 3)), age * 0.12f + i * 2f, a(argb, fade * Mth.sin(Mth.PI * k)));
        }
    }

    /** n sprites standing in a ring of radius r round c (they grow out of the ground one after another from t0 with an overshoot), each turned a little differently. */
    public static void ringOfStanding(VfxVertexBuffer buf, VfxRenderContext ctx, VfxInstance inst, ResourceLocation tex, VfxBlend blend, Vector3f c, int n, float radius, float w, float hgt,
                                      float age, float t0, float stagger, float sway, int argb, float fade, int salt) {
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * h(inst, i, salt)) / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - t0 - i * stagger) / 8f, 0, 1));
            if (grow <= 0.01f) continue;
            float jitter = 0.85f + 0.3f * h(inst, i, salt + 1);
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * radius, 0, Mth.sin(ang) * radius);
            standing(buf, ctx, tex, blend, base, w * jitter, hgt * grow * jitter, h(inst, i, salt + 2) > 0.5f, sway * Mth.sin(age * 0.11f + i * 1.7f) * w, a(argb, fade));
        }
    }
}
