package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.EarthFxStyle;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.EarthFamilyFx.*;

/**
 * 1.93.0 The effects of Sandstone, Rock, Red Ochre, Stone, Earth and Mineral magic (drawn to the standard of the Time Magic layers: layered, anticipation, impact, afterglow).
 * One layer, three shapes, six identities: the magic is read from the colour the spell passes ({@link EarthFxStyle#of}), so the same shape reads as sand, boulders,
 * pigment, masonry, soil or crystal.
 * <ul>
 *   <li>EARTHFAM_CAST, CAST / PROJECTILE: a sigil and a ring contract at 'from' while debris is pulled in; then the magic's own body flies from 'from' to 'to' dragging
 *       a grainy streak (a swirling sand cloud; a spinning boulder with dust puffs and a hot seam; a blob of pigment with falling drops; a masonry block with an orbiting
 *       rune; a clod sprouting grass blades; a crystal lance with glints) and bursts at 'to'. power = size (1.0 = about a block), default 16 ticks.</li>
 *   <li>EARTHFAM_ZONE, ZONE / FIELD: a sigil turns on a stained floor, and the rim of the area rises: curtains of sand, stone spikes, handprints, stone slabs, spikes with
 *       grass, standing crystals; motes rise, a pulse ring rolls out every 26 ticks. from = centre on the ground, power = RADIUS in blocks, default 80 ticks.</li>
 *   <li>EARTHFAM_BURST, IMPACT / SIGNATURE: a flash, a puff of the magic's body, ground rings, a stamped sigil with the magic's own decal (fissures, a pigment splat,
 *       crystals standing on a ring, roots), and the debris thrown out (grains spiralling, boulders in a heavy arc, drops, masonry hovering then settling, clods, crystals
 *       growing outward). from = centre, to = optional direction hint, power = scale (0.6 to 3.0), default 28 ticks.</li>
 * </ul>
 * Budget: at most about 60 quads in any frame (the vertex cap is 400); the sprites are tools/gen_earth_family_textures.py.
 */
public class EarthFamilyLayer extends AbstractVfxLayer {
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_ZONE, VfxShape.EARTHFAM_BURST); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case EARTHFAM_CAST -> 16;
            case EARTHFAM_ZONE -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return EarthFxStyle.color(EarthFxStyle.ROCK); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case EARTHFAM_CAST -> cast(inst, ctx, buf);
            case EARTHFAM_ZONE -> zone(inst, ctx, buf);
            case EARTHFAM_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }

    private static float life(VfxInstance inst, float age, float in, float out) { return ElementFx.life(inst, age, in, out); }

    /** The palette of this effect: body colour, glow colour, dark colour (ARGB). The spell's colour is mixed in a little. */
    private static int[] palette(VfxInstance inst, int style) {
        int base = VfxVertexBuffer.lerpColor(EarthFxStyle.color(style), inst.color | 0xFF000000, 0.15f);
        return new int[]{base, EarthFxStyle.glow(style), EarthFxStyle.dark(style)};
    }

    // ------------------------------------------------------------------ FX1: cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration), P = Mth.clamp(inst.power, 0.4f, 4f);
        int st = EarthFxStyle.of(inst.color);
        int[] pal = palette(inst, st);
        int col = pal[0], glow = pal[1], dark = pal[2], hot = VfxVertexBuffer.whiten(glow, 0.45f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) { dir.set(0, 0, 1); dist = 0.05f; } else dir.mul(1f / dist);

        float tl = 0.18f * d, ta = 0.78f * d;
        float s = Mth.clamp((age - tl) / (ta - tl), 0, 1), se = 0.55f * s + 0.45f * s * s;
        float ti = Mth.clamp((age - ta) / Math.max(1f, d - ta), 0, 1);
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);

        // 1. charge: the sigil and a ring contract on the caster, debris is drawn in
        float tc = Mth.clamp(age / tl, 0, 1);
        float cf = tc * (1f - Mth.clamp((age - tl) / (0.9f * tl + 1f), 0, 1));
        if (cf > 0.02f) {
            VfxPose mz = VfxPose.facing(new Vector3f(dir).mul(0.35f * P).add(from), dir);
            float shrink = 1.5f - 0.6f * tc;
            buf.plane(SIGIL[st], VfxBlend.ADD, mz.spin(age * 0.25f), 0.85f * P * shrink, a(glow, 0.85f * cf));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, mz.lift(0.04f), 0.6f * P * shrink, 0.72f * P * shrink, ctx.seg(16, 8), 1, 0, a(hot, 0.7f * cf));
            VfxBloom.glow(ctx, buf, from, 0.5f * P * (0.4f + 0.6f * tc), glow, cf);
            for (int i = 0; i < 4; i++) {
                float rad = (1.5f * (1f - tc) + 0.12f) * P * (0.7f + 0.5f * h(inst, i, 3));
                Vector3f pos = dirOf(h(inst, i, 2), h(inst, i, 1), false).mul(rad).add(from);
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, 0.2f * P, h(inst, i, 4) * 6f + age * 0.3f, a(VfxVertexBuffer.lerpColor(col, glow, tc), cf));
            }
        }
        // 2. flight
        if (s > 0f) {
            float live = 1f - ti;
            float actual = Math.min(Math.min(dist * 0.6f, 4.0f * P), dist * se) * (1f - 0.7f * ti);
            Vector3f tail = new Vector3f(dir).mul(-actual).add(head);
            ElementFx.streak(buf, ctx, TRAIL, st == EarthFxStyle.MINERAL || st == EarthFxStyle.STONE ? VfxBlend.ADD : VfxBlend.ALPHA, tail, head, 0.9f * P * (0.8f + 0.2f * live),
                    a(st == EarthFxStyle.MINERAL || st == EarthFxStyle.STONE ? glow : col, 0.55f * live));
            Vector3f core = new Vector3f(dir).mul(-actual * 0.5f).add(head);
            ElementFx.streak(buf, ctx, TRAIL, VfxBlend.ADD, core, head, 0.24f * P, a(hot, 0.45f * live));
            body(inst, ctx, buf, st, head, dir, age, P, live, col, glow, dark, hot);
            VfxBloom.glow(ctx, buf, head, 0.45f * P, glow, 0.4f * live);
        }
        // 3. impact
        if (age >= ta) {
            float e = VfxAnim.easeOutCubic(ti), inv = 1f - ti;
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, to, 2.6f * P * (0.5f + 0.5f * e), 0, a(hot, 0.85f * inv));
            buf.billboard(ctx, PUFF[st], st == EarthFxStyle.MINERAL ? VfxBlend.ADD : VfxBlend.ALPHA, to, (0.6f + 2.4f * e) * P, age * 0.05f, a(st == EarthFxStyle.MINERAL ? glow : col, (st == EarthFxStyle.MINERAL ? 0.4f : 0.8f) * inv));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.05f, 0)), Math.max(0.1f, 1.6f * P * e - 0.2f * P), 1.6f * P * e + 0.1f, ctx.seg(16, 8), 1, 0, a(hot, 0.7f * inv));
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                Vector3f vd = dirOf(h(inst, i, 11), h(inst, i, 12), true).mul((0.9f + 1.4f * h(inst, i, 13)) * P);
                float T = ti * d / 20f;
                Vector3f pos = new Vector3f(vd).mul(T * 1.2f).add(to).add(0, -0.5f * 3.2f * T * T, 0);
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, 0.2f * P * (1f - 0.5f * ti), h(inst, i, 14) * 6f + age * 0.5f, a(VfxVertexBuffer.lerpColor(col, dark, ti), inv));
            }
        }
    }

    /** The thrown body of each magic, at 'head', flying along 'dir'. */
    private void body(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, int st, Vector3f head, Vector3f dir, float age, float P, float live, int col, int glow, int dark, int hot) {
        Vector3f s1 = ElementFx.side(dir), s2 = new Vector3f(dir).cross(s1).normalize();
        switch (st) {
            case EarthFxStyle.SANDSTONE -> {
                buf.billboard(ctx, PUFF[st], VfxBlend.ALPHA, head, 2.6f * P, age * 0.08f, a(col, 0.95f * live));
                int n = ctx.seg(6, 3);
                for (int i = 0; i < n; i++) {
                    float ang = age * 0.5f + i * Mth.TWO_PI / n, r = (0.4f + 0.12f * (i % 3)) * P;
                    Vector3f pos = new Vector3f(head).add(new Vector3f(s1).mul(Mth.cos(ang) * r)).add(new Vector3f(s2).mul(Mth.sin(ang) * r)).sub(new Vector3f(dir).mul(0.12f * i));
                    buf.billboard(ctx, SPARK[st], VfxBlend.ALPHA, pos, 0.5f * P, 0, a(VfxVertexBuffer.lerpColor(col, hot, 0.3f), live));
                }
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, head, 0.5f * P, age * 0.2f, a(col, live));
            }
            case EarthFxStyle.ROCK -> {
                buf.billboard(ctx, ACCENT[st], VfxBlend.ADD, head, 1.5f * P, age * 0.04f, a(glow, 0.5f * live));                     // the hot seam round it
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, head, 1.35f * P, age * 0.3f, a(col, live));
                for (int i = 0; i < 3; i++)
                    buf.billboard(ctx, PUFF[st], VfxBlend.ALPHA, new Vector3f(dir).mul(-0.55f * P * (i + 1)).add(head), (1.0f + 0.3f * i) * P, i * 1.7f + age * 0.05f, a(dark, (0.5f - 0.13f * i) * live));
            }
            case EarthFxStyle.RED_OCHRE -> {
                buf.billboard(ctx, PUFF[st], VfxBlend.ALPHA, head, 1.5f * P, age * 0.06f, a(col, live));
                buf.billboard(ctx, PUFF[st], VfxBlend.ADD, head, 1.55f * P, -age * 0.08f, a(glow, 0.22f * live));
                for (int i = 0; i < 4; i++) {
                    float u = (age * 0.09f + h(inst, i, 5)) % 1f;
                    Vector3f pos = new Vector3f(dir).mul(-u * 1.6f * P).add(head).add(new Vector3f(s1).mul((h(inst, i, 6) - 0.5f) * 0.9f * P)).add(0, -u * u * 0.9f * P, 0);
                    buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, 0.26f * P * (1f - 0.5f * u), 0, a(col, (1f - u) * live));
                }
            }
            case EarthFxStyle.STONE -> {
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, head, 1.25f * P, age * 0.22f, a(col, live));
                for (int i = 0; i < 2; i++) {
                    float ang = age * 0.3f + i * Mth.PI;
                    Vector3f pos = new Vector3f(head).add(new Vector3f(s1).mul(Mth.cos(ang) * 0.9f * P)).add(new Vector3f(s2).mul(Mth.sin(ang) * 0.9f * P));
                    buf.billboard(ctx, ACCENT[st], VfxBlend.ADD, pos, 0.55f * P, -ang * 0.5f, a(glow, 0.9f * live));
                }
                for (int i = 0; i < 3; i++)
                    buf.billboard(ctx, SPARK[st], VfxBlend.ALPHA, new Vector3f(dir).mul(-0.5f * P * (i + 1)).add(head).add(0, 0.1f * P * (i % 2), 0), 0.28f * P, i * 2f + age * 0.3f, a(col, (0.8f - 0.2f * i) * live));
            }
            case EarthFxStyle.EARTH -> {
                buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, head, 1.4f * P, age * 0.18f, a(col, live));
                for (int i = 0; i < 4; i++) {
                    Vector3f up = new Vector3f(s2).mul(0.5f).add(new Vector3f(s1).mul((i - 1.5f) * 0.28f)).normalize();
                    Vector3f b0 = new Vector3f(up).mul(0.5f * P).add(head);
                    strut(buf, EARTH_BLADE, VfxBlend.ALPHA, b0, new Vector3f(up).mul(1.05f * P).add(head), 0.09f * P, a(0xFF5A9A34, live));
                }
                for (int i = 0; i < 2; i++)
                    buf.billboard(ctx, PUFF[st], VfxBlend.ALPHA, new Vector3f(dir).mul(-0.6f * P * (i + 1)).add(head), (1.0f + 0.25f * i) * P, i * 1.9f, a(dark, (0.5f - 0.15f * i) * live));
            }
            default -> {                                                                                    // mineral: a lance of crystal
                Vector3f a0 = new Vector3f(dir).mul(-1.6f * P).add(head), b0 = new Vector3f(dir).mul(0.9f * P).add(head);
                strut(buf, MINERAL_SHARD, VfxBlend.ALPHA, a0, b0, 0.3f * P, a(col, live));
                strut(buf, MINERAL_SHARD, VfxBlend.ADD, a0, b0, 0.34f * P, a(glow, 0.4f * live));
                for (int i = 0; i < 3; i++) {
                    float tw = Mth.sin(age * 0.5f + i * 2.1f);
                    buf.billboard(ctx, SPARK[st], VfxBlend.ADD, new Vector3f(dir).mul((0.9f - 0.9f * i) * P).add(head).add(new Vector3f(s1).mul(0.25f * P * tw)), 0.7f * P, age * 0.1f + i, a(hot, 0.8f * live));
                }
            }
        }
    }

    // ------------------------------------------------------------------ FX2: zone
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int st = EarthFxStyle.of(inst.color);
        int[] pal = palette(inst, st);
        int col = pal[0], glow = pal[1], dark = pal[2], hot = VfxVertexBuffer.whiten(glow, 0.45f);
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1)), Ro = R * open;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.04f, 0);
        VfxPose ground = VfxPose.ground(g);

        // the stained floor and a light under everything
        buf.plane(PUFF[st], VfxBlend.ALPHA, ground.lift(-0.01f).spin(0.7f), Ro * 1.05f, a(dark, 0.55f * fade));
        VfxBloom.planeGlow(buf, ground.lift(0.01f), Ro * 0.7f, glow, 0.4f * fade);
        // the sigil: a dark body and a lit engraving turning one way, a smaller echo the other way
        buf.plane(SIGIL[st], VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.012f), Ro * 0.9f, a(dark, 0.6f * fade));
        buf.plane(SIGIL[st], VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.012f), Ro * 0.9f, a(glow, 0.85f * fade));
        buf.plane(SIGIL[st], VfxBlend.ADD, ground.lift(0.04f).spin(-age * 0.03f), Ro * 0.5f, a(hot, 0.4f * fade));
        // the rim: each magic stands up its own thing round the edge
        int n = ctx.seg(st == EarthFxStyle.ROCK || st == EarthFxStyle.EARTH ? 8 : st == EarthFxStyle.STONE ? 7 : 10, 5);   // spikes cost 16 vertices, slabs 20: fewer of them
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + 0.25f * h(inst, i, 1), rr = Ro * (0.93f + 0.06f * h(inst, i, 2));
            float rise = VfxAnim.easeOutBack(Mth.clamp((age - 2 - i * 0.7f) / 9f, 0, 1));
            float hh = (0.9f + 0.9f * h(inst, i, 3)) * (0.7f + 0.12f * Math.min(R, 8f)) * rise;
            if (hh < 0.05f) continue;
            Vector3f base = new Vector3f(g).add(Mth.cos(ang) * rr, -0.02f, Mth.sin(ang) * rr);
            Vector3f out = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang)), along = new Vector3f(-Mth.sin(ang), 0, Mth.cos(ang));
            switch (st) {
                case EarthFxStyle.SANDSTONE -> {                                                              // curtains of sand pouring down from the rim
                    float flow = (age * 0.03f + h(inst, i, 4)) % 1f;
                    strut(buf, SAND_STREAM, VfxBlend.ALPHA, new Vector3f(base).add(0, hh * 1.5f, 0), base, 0.28f * (0.7f + 0.3f * h(inst, i, 5)), a(col, 0.55f * fade * (0.6f + 0.4f * Mth.sin(flow * Mth.TWO_PI))));
                }
                case EarthFxStyle.ROCK, EarthFxStyle.EARTH -> ElementFx.spike(buf, base, new Vector3f(base).add(0, hh, 0), hh * 0.24f, h(inst, i, 6) * 3f, st == EarthFxStyle.ROCK ? col : 0xFF000000 | 0x9A7040);
                case EarthFxStyle.RED_OCHRE -> flat(buf, ACCENT[st], VfxBlend.ADD, new Vector3f(base).add(0, 0.03f, 0), ang + Mth.HALF_PI, 0.7f + 0.25f * rise, 0.7f + 0.25f * rise, a(glow, 0.65f * fade * rise));
                case EarthFxStyle.STONE -> ElementFx.slab(buf, base, along, out, 0.42f + 0.16f * h(inst, i, 7), 0.16f, hh * 1.1f, 0.12f, col);
                default -> {                                                                                  // mineral: crystals standing
                    Vector3f tip = new Vector3f(base).add(out.x * 0.15f * hh, hh * 1.2f, out.z * 0.15f * hh);
                    strut(buf, MINERAL_SHARD, VfxBlend.ALPHA, base, tip, hh * 0.2f, a(col, fade));
                    strut(buf, MINERAL_SHARD, VfxBlend.ADD, base, tip, hh * 0.24f, a(glow, 0.35f * fade));
                }
            }
        }
        if (st == EarthFxStyle.EARTH) {                                                                       // grass growing round the rim between the spikes
            for (int i = 0; i < ctx.seg(6, 3); i++) {
                float ang = Mth.TWO_PI * (i + 0.5f) / ctx.seg(6, 3), rr = Ro * 0.9f, gr = VfxAnim.easeOutCubic(Mth.clamp((age - 6 - i) / 10f, 0, 1));
                Vector3f base = new Vector3f(g).add(Mth.cos(ang) * rr, 0, Mth.sin(ang) * rr);
                strut(buf, EARTH_BLADE, VfxBlend.ALPHA, base, new Vector3f(base).add(0, 0.8f * gr, 0), 0.14f, a(0xFF5A9A34, fade));
            }
        }
        // pulses rolling out from the middle
        for (int k = 0; k < 2; k++) {
            float ph = (age - k * 13f) / 26f;
            if (ph <= 0f) continue;
            ph = ph % 1f;
            float rr = Math.max(0.2f, Ro * ph);
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.07f), Math.max(0.05f, rr - 0.12f * R * 0.25f), rr + 0.1f, ctx.seg(20, 10), 1, 0, a(hot, Mth.sqrt(1f - ph) * 0.6f * fade));
        }
        // rising motes
        int nm = ctx.seg(6, 3);
        for (int i = 0; i < nm; i++) {
            float f = (age * 0.018f + h(inst, i, 8)) % 1f;
            float ang = h(inst, i, 9) * Mth.TWO_PI, rr = Mth.sqrt(h(inst, i, 10)) * Ro * 0.85f;
            Vector3f pos = new Vector3f(g).add(Mth.cos(ang) * rr, f * 2.4f + 0.1f, Mth.sin(ang) * rr);
            boolean light = st == EarthFxStyle.MINERAL || st == EarthFxStyle.STONE || st == EarthFxStyle.SANDSTONE;
            buf.billboard(ctx, SPARK[st], light ? VfxBlend.ADD : VfxBlend.ALPHA, pos, st == EarthFxStyle.MINERAL ? 0.5f : 0.24f, h(inst, i, 11) * 6f + age * 0.1f, a(light ? hot : col, Mth.sin(f * Mth.PI) * fade));
        }
        buf.billboard(ctx, PUFF[st], VfxBlend.ALPHA, new Vector3f(g).add(0, 0.5f, 0), Math.max(2.2f, 1.1f * Ro), -age * 0.012f, a(col, 0.28f * fade * open));
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.3f, 0), 0.5f + 0.1f * R, glow, 0.45f * fade * open);
    }

    // ------------------------------------------------------------------ FX3: burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(10f, inst.duration), S = Mth.clamp(inst.power, 0.5f, 4f);
        float fade = life(inst, age, 0, 12), tt = Mth.clamp(age / d, 0, 1);
        int st = EarthFxStyle.of(inst.color);
        int[] pal = palette(inst, st);
        int col = pal[0], glow = pal[1], dark = pal[2], hot = VfxVertexBuffer.whiten(glow, 0.5f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        float hl = hint.length();
        Vector3f hd = hl > 0.2f ? hint.mul(1f / hl) : new Vector3f();
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        float e6 = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1)), e16 = VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));
        float stamp = fade * (1f - 0.5f * tt);

        // the ground: the sigil stamped, and each magic's own mark
        buf.plane(SIGIL[st], VfxBlend.ALPHA, ground.lift(0.01f).spin(age * 0.04f), 1.9f * S * e6, a(dark, 0.55f * stamp));
        buf.plane(SIGIL[st], VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.04f), 1.9f * S * e6, a(glow, 0.75f * stamp));
        switch (st) {
            case EarthFxStyle.ROCK -> buf.plane(ACCENT[st], VfxBlend.ADD, ground.lift(0.015f).spin(h(inst, 0, 1) * 6f), 2.6f * S * e6, a(glow, 0.8f * stamp));         // fissures
            case EarthFxStyle.RED_OCHRE -> buf.plane(PUFF[st], VfxBlend.ALPHA, ground.lift(0.012f).spin(h(inst, 0, 2) * 6f), 2.4f * S * e6, a(col, 0.9f * stamp));        // a pigment splat
            case EarthFxStyle.MINERAL -> buf.plane(MINERAL_RING, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.02f), 2.6f * S * e6, a(glow, 0.7f * stamp));
            case EarthFxStyle.SANDSTONE -> buf.plane(PUFF[st], VfxBlend.ALPHA, ground.lift(0.011f).spin(1.3f), 2.4f * S * e6, a(col, 0.55f * stamp));
            case EarthFxStyle.STONE -> buf.plane(ROCK_RING, VfxBlend.ALPHA, ground.lift(0.012f).spin(0.3f), 2.4f * S * e6, a(col, 0.8f * stamp));
            default -> {                                                                                         // earth: roots running out of the centre
                for (int i = 0; i < 4; i++) {
                    float ang = i * Mth.HALF_PI + h(inst, i, 3) * 0.6f;
                    flat(buf, ACCENT[st], VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * 1.1f * S * e16, 0.03f, Mth.sin(ang) * 1.1f * S * e16), ang, 2.2f * S * e16, 0.8f * S, a(col, 0.9f * stamp));
                }
            }
        }
        // flash, the puff of the magic's own body, ground rings
        float flash = 1f - VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 4.0f * S * (0.4f + 0.6f * e6), 0, a(hot, flash));
        buf.billboard(ctx, PUFF[st], st == EarthFxStyle.MINERAL ? VfxBlend.ADD : VfxBlend.ALPHA, new Vector3f(c).add(0, 0.6f * S * e16, 0), (1.0f + 3.0f * S * e16), age * 0.03f, a(st == EarthFxStyle.MINERAL ? glow : col, (st == EarthFxStyle.MINERAL ? 0.3f : 0.75f) * (1f - sm(2f, 20f, age)) * fade));
        float e2 = VfxAnim.easeOutCubic(Mth.clamp((age - 3f) / 16f, 0, 1));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.05f), Math.max(0.05f, 4.0f * S * e16 - 0.35f * S), 4.0f * S * e16 + 0.1f, ctx.seg(20, 10), 1, 0, a(hot, (1f - e16) * 0.85f));
        if (age > 3f) buf.ring(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.06f), Math.max(0.05f, 2.8f * S * e2 - 0.25f * S), 2.8f * S * e2 + 0.1f, ctx.seg(20, 10), 1, 0, a(col, (1f - e2) * 0.7f));
        if (st == EarthFxStyle.ROCK && age > 1f) buf.plane(ROCK_RING, VfxBlend.ADD, ground.lift(0.07f).spin(0.6f), Math.max(0.3f, 3.2f * S * e16), a(glow, (1f - e16) * 0.7f));
        // the debris, each magic moving its own way
        int nd = ctx.seg(10, 5);
        float T = age / 20f;
        for (int i = 0; i < nd; i++) {
            float hv = h(inst, i, 4), hw = h(inst, i, 5);
            Vector3f vd = dirOf(hv, hw, true).mul((1.3f + 2.0f * h(inst, i, 6)) * S).add(new Vector3f(hd).mul(1.6f * S));
            float life = 1f - sm(0.55f, 1f, tt);
            float sz = 0.42f * S * (0.7f + 0.5f * h(inst, i, 7));
            switch (st) {
                case EarthFxStyle.SANDSTONE -> {                                                               // grains blown up in a spiral
                    float ang = h(inst, i, 8) * Mth.TWO_PI + age * 0.18f, r = (0.4f + 1.4f * h(inst, i, 9)) * S * e16;
                    Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * r, (0.2f + 2.4f * h(inst, i, 10)) * S * e16 * (0.5f + 0.5f * tt), Mth.sin(ang) * r);
                    buf.billboard(ctx, i % 3 == 0 ? DEBRIS[st] : SPARK[st], VfxBlend.ALPHA, pos, sz * 1.1f, age * 0.2f + i, a(VfxVertexBuffer.lerpColor(hot, col, tt), life));
                }
                case EarthFxStyle.ROCK, EarthFxStyle.EARTH -> {                                                // a heavy arc
                    Vector3f pos = new Vector3f(vd).mul(T).add(c).add(0, -0.5f * 4.2f * T * T, 0);
                    buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, sz * (st == EarthFxStyle.ROCK ? 1.3f : 1.1f), h(inst, i, 8) * 6f + age * (0.3f + h(inst, i, 9)), a(VfxVertexBuffer.lerpColor(col, dark, tt * 0.5f), life));
                }
                case EarthFxStyle.RED_OCHRE -> {                                                               // drops flung out, a long arc
                    Vector3f pos = new Vector3f(vd).mul(T * 1.1f).add(c).add(0, -0.5f * 3.0f * T * T, 0);
                    buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, sz * 1.1f, 0, a(VfxVertexBuffer.lerpColor(glow, col, tt), life));
                }
                case EarthFxStyle.STONE -> {                                                                   // blocks rise, hover, then settle
                    float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1)) * (1f - sm(0.6f, 1f, tt));
                    Vector3f pos = new Vector3f(c).add(vd.x * 0.5f, 0.3f + (0.8f + 0.9f * hv) * S * rise, vd.z * 0.5f);
                    buf.billboard(ctx, DEBRIS[st], VfxBlend.ALPHA, pos, sz * 1.2f, h(inst, i, 8) * 6f + age * 0.06f, a(col, life));
                    if (i % 3 == 0) buf.billboard(ctx, ACCENT[st], VfxBlend.ADD, new Vector3f(pos).add(0, 0.35f * S, 0), 0.3f * S, 0, a(glow, 0.8f * life));
                }
                default -> {                                                                                   // mineral: crystals growing outward from the middle
                    float grow = VfxAnim.easeOutBack(Mth.clamp((age - 0.5f * (i % 3)) / 6f, 0, 1)) * (1f - sm(0.5f, 0.95f, tt));
                    Vector3f pd = new Vector3f(vd.x, 0.9f + 0.5f * hv, vd.z).normalize();
                    float len = (1.0f + 1.3f * h(inst, i, 6)) * S * grow;
                    if (len < 0.05f) continue;
                    Vector3f b0 = new Vector3f(pd).mul(0.2f * S).add(c);
                    Vector3f tip = new Vector3f(pd).mul(len).add(c);
                    strut(buf, MINERAL_SHARD, VfxBlend.ALPHA, b0, tip, len * 0.14f, a(col, fade));
                    strut(buf, MINERAL_SHARD, VfxBlend.ADD, b0, tip, len * 0.17f, a(glow, 0.35f * fade));
                }
            }
        }
        // glints and sparks
        int ns = ctx.seg(4, 2);
        for (int i = 0; i < ns; i++) {
            float tw = Mth.sin(age * (0.25f + 0.05f * i) + i * 2.3f);
            tw = tw * tw;
            Vector3f pos = dirOf(h(inst, i, 12), h(inst, i, 13), true).mul((0.6f + 1.4f * h(inst, i, 14)) * S * e16).add(c);
            buf.billboard(ctx, SPARK[EarthFxStyle.MINERAL], VfxBlend.ADD, pos, 0.5f * S * tw, age * 0.1f, a(hot, tw * fade * 0.9f));
        }
        float ag = (1f - tt) * (1f - tt);
        VfxBloom.glow(ctx, buf, c, 0.9f * S, glow, 0.5f * ag);
        VfxBloom.planeGlow(buf, ground.lift(0.015f), 1.6f * S, glow, 0.5f * ag);
    }
}
