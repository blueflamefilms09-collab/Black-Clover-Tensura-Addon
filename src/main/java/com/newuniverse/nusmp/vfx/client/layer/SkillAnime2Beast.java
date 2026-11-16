package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer.*;
import static com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Layer.*;

/**
 * ANIME2_BEAST for {@link SkillAnime2Layer}: the Beast Will moves (docs/beast_skills.md). The move decides the SHAPE (the family: a line, a rush, a burst, a volley, a seal circle, an aura round the
 * caster) and the beast's element decides what the shape is made of (the accent, one themed piece per element: flames, droplets, ice shards, lightning, rocks, feathers, ink, light, bubbles, leaves,
 * spirit wisps, void runes), so 27 beasts need 12 element looks, not 27 effect scripts. A finisher ({@link SkillAnime2Vfx#beastBig}) is the same family bigger with a rune halo and more accents.
 * Line / rush / volley run from -> to; burst / seal / aura are centred on from and their radius is the from -> to distance. Worst case about 75 quads (the preview measures it).
 */
final class SkillAnime2Beast {
    private SkillAnime2Beast() {}

    /** The element colours, in the order of {@link SkillAnime2Vfx#ELEMENTS}. */
    static final int[] ELEMENT_COLORS = {0xFFFF6A1E, 0xFF3FA8FF, 0xFFBFEFFF, 0xFFFFF04D, 0xFFB08A50, 0xFFB8FFF0, 0xFF6A3AA8, 0xFFFFE7A0, 0xFF9ADB3A, 0xFF4CD65A, 0xFF9AF0FF, 0xFFB080FF};

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        int el = SkillAnime2Vfx.beastElement(inst.power);
        boolean big = SkillAnime2Vfx.beastBig(inst.power);
        int col = VfxVertexBuffer.lerpColor(ELEMENT_COLORS[el], inst.color, 0.4f);
        switch (SkillAnime2Vfx.beastFamily(inst.power)) {
            case 1 -> rush(inst, ctx, buf, el, big, col);
            case 2 -> burst(inst, ctx, buf, el, big, col);
            case 3 -> volley(inst, ctx, buf, el, big, col);
            case 4 -> seal(inst, ctx, buf, el, big, col);
            case 5 -> aura(inst, ctx, buf, el, big, col);
            default -> line(inst, ctx, buf, el, big, col);
        }
    }

    // ------------------------------------------------------------------ the element accents
    /** One themed piece of the element at {@code p}, about {@code s} blocks; {@code rot} turns the flat ones. 1 to 4 quads. */
    static void accent(VfxRenderContext ctx, VfxVertexBuffer buf, int el, Vector3f p, float s, float rot, int col, float a) {
        if (a <= 0.02f) return;
        switch (el) {
            case 0 -> {                                                                                            // fire: a flame tongue and its glow
                upright(buf, FLAME, VfxBlend.ADD, p, s * 0.7f, s * 1.4f, al(col, a));
                buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(p).add(0, s * 0.5f, 0), s * 1.2f, 0, al(col, 0.3f * a));
            }
            case 1 -> buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, p, s, rot, al(col, a));                           // water: a droplet
            case 2 -> {                                                                                            // ice: a shard and a small snowflake
                buf.billboard(ctx, SHARD, VfxBlend.ALPHA, p, s, rot, al(col, a));
                buf.billboard(ctx, SNOWFLAKE, VfxBlend.ADD, p, s * 0.9f, -rot, al(hot(col, 0.5f), 0.4f * a));
            }
            case 3 -> {                                                                                            // lightning: a short forked bolt
                Vector3f d = randomDir(RandomSource.create(Float.floatToIntBits(p.x * 31 + p.y * 17 + p.z * 7) + (long) (rot * 3)));
                zigzag(ctx, buf, p, new Vector3f(d).mul(s * 1.4f).add(p), 3, s * 0.25f, s * 0.12f, (long) (rot * 100) + 11, col, a);
            }
            case 4 -> buf.billboard(ctx, ROCK, VfxBlend.ALPHA, p, s, rot, al(col, a));                              // earth: a rock chunk
            case 5 -> {                                                                                            // wind: a feather and a curled streak
                buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, p, s * 1.3f, rot, al(col, a));
                buf.billboard(ctx, GLOW, VfxBlend.ADD, p, s * 0.8f, 0, al(col, 0.25f * a));
            }
            case 6 -> {                                                                                            // dark: ink smoke with a red glint (alpha blend, so it stays black on a bright sky)
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, p, s * 1.2f, rot, al(INK, 0.85f * a));
                buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, s * 0.45f, rot * 2, al(col, 0.8f * a));
            }
            case 7 -> {                                                                                            // light: a twinkle with rays
                buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, s * 1.2f, rot, al(hot(col, 0.5f), a));
                buf.billboard(ctx, RAYS, VfxBlend.ADD, p, s * 1.1f, rot * 0.3f, al(col, 0.3f * a));
            }
            case 8 -> {                                                                                            // poison: a bubble and a drop
                buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, p, s, 0, al(col, a));
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, p, s * 1.2f, rot, al(col, 0.3f * a));
            }
            case 9 -> {                                                                                            // nature: a leaf and a petal
                buf.billboard(ctx, LEAF, VfxBlend.ALPHA, p, s * 1.1f, rot, al(col, a));
                buf.billboard(ctx, PETAL, VfxBlend.ALPHA, p, s * 0.6f, -rot, al(hot(col, 0.4f), 0.8f * a));
            }
            case 10 -> {                                                                                           // spirit: a wisp (glow, trail and twinkle)
                buf.billboard(ctx, GLOW, VfxBlend.ADD, p, s * 1.4f, 0, al(col, 0.5f * a));
                buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, s * 0.7f, rot, al(hot(col, 0.6f), a));
            }
            default -> {                                                                                           // void / aether: a small rune that turns, on a dark heart
                buf.billboard(ctx, CORE, VfxBlend.ALPHA, p, s * 0.5f, 0, al(INK, 0.9f * a));
                buf.billboard(ctx, RUNE, VfxBlend.ADD, p, s * 1.5f, rot, al(col, a));
            }
        }
    }

    private static Vector3f up(Vector3f dir, Vector3f side) { return new Vector3f(dir).cross(side).normalize(); }

    // ------------------------------------------------------------------ LINE: a beam, a breath, a disc
    private static void line(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1)), side = sideOf(dir), up = up(dir, side);
        float fade = life(inst, age, 0, 7), grow = VfxAnim.easeOutCubic(ramp(age, 0, 5f)), k = big ? 1.4f : 1f;
        Vector3f head = lerp(a, b, grow);
        buf.beam(ctx, BOLT, VfxBlend.ALPHA, a, head, 1.8f * k, 1.2f * k, 1, 0, al(col, 0.8f * fade), al(col, 0.5f * fade));      // BOLT / STREAK are narrow in the middle of their quad, so the widths are about 4x the visible body
        buf.beam(ctx, STREAK, VfxBlend.ADD, a, head, 1.3f * k, 0.9f * k, 1, 0, al(hot(col, 0.5f), 0.95f * fade), al(hot(col, 0.4f), 0.7f * fade));
        int n = ctx.seg(big ? 9 : 6, 4);
        RandomSource r = inst.random();
        for (int i = 0; i < n; i++) {                                                  // the element's pieces ride along the beam on a helix
            float ph = Mth.frac(i / (float) n + age * 0.09f + r.nextFloat() * 0.05f), ang = ph * 9f + i * 2.4f;
            float rad = (0.25f + 0.45f * Mth.sin(ph * Mth.PI)) * k;
            Vector3f at = lerp(a, head, ph).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(new Vector3f(up).mul(Mth.sin(ang) * rad));
            accent(ctx, buf, el, at, 0.45f * k, ang, col, fade * Mth.sin(ph * Mth.PI));
        }
        if (age < 7f) flash(ctx, buf, a, 1.2f * k, age * 0.4f, col, 1 - age / 7f);
        if (grow >= 1f) {
            float land = age - 5f;
            if (land < 8f) flash(ctx, buf, b, 1.6f * k, land * 0.3f, col, 1 - land / 8f);
            buf.ring(RING, VfxBlend.ADD, VfxPose.facing(b, dir), 0.25f * k + land * 0.1f, 0.5f * k + land * 0.22f, ctx.seg(12, 6), 3, 0, al(col, Math.max(0, 1 - land / 9f) * fade));
        }
        if (big) buf.plane(RUNE, VfxBlend.ADD, VfxPose.facing(a, dir).spin(age * 0.08f), 1.5f * VfxAnim.easeOutBack(ramp(age, 0, 4f)), al(col, 0.8f * fade));
    }

    // ------------------------------------------------------------------ RUSH: a dash, a pounce, a charge
    private static void rush(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1)), side = sideOf(dir);
        float fade = life(inst, age, 0, 6), u = VfxAnim.easeOutCubic(ramp(age, 0, 5f)), k = big ? 1.35f : 1f;
        Vector3f head = lerp(a, b, u), tail = lerp(a, b, Math.max(0, u - 0.55f));
        buf.beam(ctx, BOLT, VfxBlend.ALPHA, tail, head, 0.4f * k, 2.4f * k, 1, 0, al(col, 0.1f * fade), al(col, 0.75f * fade));
        buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.3f, 1.8f * k, 1, 0, al(col, 0.1f * fade), al(hot(col, 0.5f), 0.9f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 1.5f * k, 0, al(col, 0.6f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(big ? 10 : 7, 4);
        for (int i = 0; i < n; i++) {                                                  // the element's pieces shed behind the dash and fall away
            float born = r.nextFloat() * 0.8f, ph = Mth.clamp((u - born) * 2.2f + ramp(age, 5f, 14f) * 0.5f, 0, 1);
            if (u <= born || ph >= 1f) continue;
            Vector3f at = lerp(a, b, born).add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 1.2f)).add(0, (r.nextFloat() - 0.2f) * 1.1f - ph * 0.6f, 0);
            accent(ctx, buf, el, at, 0.4f * k, i * 1.7f + age * 0.1f, col, (1 - ph) * fade);
        }
        if (u >= 1f) {                                                                 // the pounce lands: rake marks and a flash
            float land = age - 5f;
            if (land < 9f) {
                float f = 1 - land / 9f;
                for (int c = 0; c < (big ? 2 : 1); c++) buf.billboard(ctx, CLAW, VfxBlend.ALPHA, new Vector3f(b).add(0, 0.2f * c, 0), (1.6f + 0.5f * c) * k, 0.6f - 1.1f * c, al(INK, 0.9f * f));
                for (int c = 0; c < (big ? 2 : 1); c++) buf.billboard(ctx, CLAW, VfxBlend.ADD, new Vector3f(b).add(0, 0.2f * c, 0), (1.6f + 0.5f * c) * k, 0.6f - 1.1f * c, al(hot(col, 0.6f), 0.9f * f));
                flash(ctx, buf, b, 1.4f * k, land * 0.3f, col, f);
            }
        }
    }

    // ------------------------------------------------------------------ BURST: a roar, a quake, a stomp
    private static void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(c.distance(ctx.rel(inst.to(ctx))), 1.5f, 14f), fade = life(inst, age, 0, 8), k = big ? 1.3f : 1f;
        float open = VfxAnim.easeOutCubic(ramp(age, 0, 9f));
        Vector3f ground = new Vector3f(c).add(0, 0.08f, 0);
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(ground), R * open * 0.85f, R * open, ctx.seg(20, 10), 5, 0, al(col, 0.85f * fade * (1 - 0.5f * open)));
        buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(ground), R * open * 0.5f, R * open * 0.58f, ctx.seg(16, 8), 3, age * 0.02f, al(col, 0.6f * fade));
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.02f, 0)), R * 0.7f * open, al(col, 0.4f * fade));
        flash(ctx, buf, new Vector3f(c).add(0, 1f, 0), 2.6f * k * (1 - ramp(age, 0, 9f) * 0.6f), age * 0.2f, col, 1 - ramp(age, 0, 10f));
        buf.beam(ctx, STREAK, VfxBlend.ADD, ground, new Vector3f(ground).add(0, 3.5f * k * open, 0), 1.3f * k, 0.5f, 1, 0, al(col, 0.5f * fade * (1 - ramp(age, 4f, 14f))), al(hot(col, 0.6f), 0.15f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(big ? 14 : 10, 5);
        for (int i = 0; i < n; i++) {                                                  // the element's pieces fly up and out of the ring
            float ph = ramp(age, i * 0.3f, 12f);
            if (ph <= 0f || ph >= 1f) continue;
            float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.6f, rad = R * (0.35f + 0.6f * r.nextFloat()) * VfxAnim.easeOutCubic(ph);
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0.2f + 2.4f * Mth.sin(ph * Mth.PI) * (0.5f + r.nextFloat()), Mth.sin(ang) * rad).add(c);
            accent(ctx, buf, el, at, 0.5f * k, ang + ph * 4f, col, fade * (1 - ph * 0.6f));
        }
        if (big) buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 3f, 0)), R * 0.4f * open, al(col, 0.5f * fade));
    }

    // ------------------------------------------------------------------ VOLLEY: a barrage of the element's pieces from -> to
    private static void volley(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1)), side = sideOf(dir), up = up(dir, side);
        float fade = life(inst, age, 0, 6), k = big ? 1.3f : 1f;
        int n = ctx.seg(big ? 9 : 6, 4);
        RandomSource r = inst.random();
        float fly = Mth.clamp(a.distance(b) / 7f, 3f, 6f);
        for (int i = 0; i < n; i++) {
            float t0 = i * 1.1f, local = age - t0;
            Vector3f from = new Vector3f(a).add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 1.6f)).add(new Vector3f(up).mul((r.nextFloat() - 0.2f) * 1.4f));
            Vector3f to = new Vector3f(b).add((r.nextFloat() - 0.5f) * 1.6f, (r.nextFloat() - 0.5f) * 1.6f, (r.nextFloat() - 0.5f) * 1.6f);
            if (local < 0 || local > fly + 6f) continue;
            if (local < fly) {
                float u = local / fly;
                Vector3f head = lerp(from, to, u), tail = lerp(from, to, Math.max(0, u - 0.4f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.2f, 1.0f * k, 1, 0, al(col, 0.2f * fade), al(hot(col, 0.5f), 0.85f * fade));
                accent(ctx, buf, el, head, 0.5f * k, i * 1.3f + age * 0.3f, col, fade);
            } else {
                float land = local - fly;
                flash(ctx, buf, to, 0.9f * k, land * 0.4f + i, col, 1 - land / 6f);
            }
        }
    }

    // ------------------------------------------------------------------ SEAL: a circle on the ground, pillars of the element round it
    private static void seal(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(c.distance(ctx.rel(inst.to(ctx))), 1.2f, 10f), fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 7f)), k = big ? 1.3f : 1f;
        Vector3f ground = new Vector3f(c).add(0, 0.07f, 0);
        buf.plane(RUNE, VfxBlend.ALPHA, VfxPose.ground(ground).spin(age * 0.04f), R * open, al(col, 0.9f * fade));
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.02f, 0)).spin(-age * 0.06f), R * 0.72f * open, al(hot(col, 0.5f), 0.7f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(ground), R * 0.96f, R * open, ctx.seg(16, 8), 4, age * 0.03f, al(col, 0.7f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 0.6f, 0), R * 1.2f, 0, al(col, 0.3f * fade));
        int m = ctx.seg(big ? 8 : 6, 4);
        for (int i = 0; i < m; i++) {                                                  // pillars of the element rise from the circle's edge one after another
            float ph = ramp(age, 3f + i * 1.0f, 8f), ang = Mth.TWO_PI * i / m + 0.4f;
            if (ph <= 0f) continue;
            Vector3f at = new Vector3f(Mth.cos(ang) * R * 0.9f, 0, Mth.sin(ang) * R * 0.9f).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, at, new Vector3f(at).add(0, 2.6f * k * VfxAnim.easeOutCubic(ph), 0), 1.0f, 0.35f, 1, 0, al(col, 0.55f * fade), al(hot(col, 0.5f), 0.1f * fade));
            accent(ctx, buf, el, new Vector3f(at).add(0, 0.2f + 1.8f * ph, 0), 0.5f * k, age * 0.1f + i, col, fade * (1 - 0.3f * ph));
        }
        if (age < 9f) flash(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), 1.8f * k, age * 0.3f, col, 1 - age / 9f);
    }

    // ------------------------------------------------------------------ AURA: a guard, a veil, a blessing round the caster
    private static void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int el, boolean big, int col) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(c.distance(ctx.rel(inst.to(ctx))), 1.2f, 12f), fade = life(inst, age, 3, 9), k = big ? 1.3f : 1f;
        Vector3f ground = new Vector3f(c).add(0, 0.06f, 0);
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(ground).spin(age * 0.05f), Math.min(R, 2.2f) * k * VfxAnim.easeOutBack(ramp(age, 0, 6f)), al(col, 0.8f * fade));
        float rr = R * VfxAnim.easeOutCubic(ramp(age, 0, 12f));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.03f, 0)), rr * 0.9f, rr, ctx.seg(18, 9), 4, 0, al(col, 0.7f * fade * (1 - ramp(age, 6f, 18f))));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 1f, 0), 3.2f * k, 0, al(col, 0.28f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(big ? 12 : 9, 5);
        for (int i = 0; i < n; i++) {                                                  // the element's pieces spiral up the body
            float ph = Mth.frac(age * 0.04f + i / (float) n), ang = ph * 7f + i * 2.1f + r.nextFloat(), rad = (0.7f + 0.3f * r.nextFloat()) * k;
            accent(ctx, buf, el, new Vector3f(Mth.cos(ang) * rad, ph * 2.6f, Mth.sin(ang) * rad).add(c), 0.4f * k, ang, col, fade * Mth.sin(ph * Mth.PI));
        }
    }
}
