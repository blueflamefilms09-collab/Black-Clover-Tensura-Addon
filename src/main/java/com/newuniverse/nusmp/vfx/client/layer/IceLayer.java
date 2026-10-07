package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.IceFx.*;

/**
 * Ice Magic (Heavenly Ice Fang): clear pale ice, NOT the black glass of Demon Ice. Long faceted blades whose facets run from deep blue
 * shade to white, hard white edges that glow, frost ferns, six-fold snowflakes, breath mist and glints. Textures from
 * tools/gen_icefx_textures.py (icefx_*). The crystal sprites are drawn twice: ALPHA in a blue body tint (the glass) and ADD in pale
 * cyan / white (only the lit facets and the edges flare). 'color' is a tint mixed 30 % into the palette, so the effect always reads as
 * this magic. Power is the size already scaled by the game (about 3 for a natural 1.0 cast and impact, the radius times 1.5 for a zone).
 * <ul>
 *   <li>ICE_FX1, Heavenly Ice Fang (cast / projectile): 'from' = hand / eye, 'to' = target, power = size (3 = a 5 block fang), duration =
 *       flight ticks (default 16). The first quarter: a rime ring and a snowflake open facing the target, ice shards are drawn into
 *       them and a cold flash gathers. Then a long faceted fang (a second plane spinning round its axis so it reads as 3D) shoots out
 *       trailing a fan of long ice blades like the sheet of spikes in the art, two faint afterimages, a stream of frost dust and
 *       breath mist that stays where it passed, splinters orbiting it and shards shed from its path. At 'to': a shatter star, a rime
 *       ring, a frost patch, a cold flash and a spray of fragments.</li>
 *   <li>ICE_FX2, Winter Field (zone / field / dome): 'from' = centre on the ground, power = RADIUS, duration = life ticks (default 80),
 *       fades in over 8 ticks and out over the last 12. A cracked ice sheet with a turning frost-fern star and rime rings, a pulse
 *       ring sweeping out every second, breath mist rolling low over it, a rim of ice spikes that grows out of the ground and
 *       sinks at the end, ice clusters thrusting up inside, glints rising and snowflakes falling.</li>
 *   <li>ICE_FX3, Shatter (impact / burst / signature): 'from' = centre, 'to' = optional direction (to - from, may be zero: then it
 *       bursts all round, else the blades fan toward it), power = scale, duration = life ticks (default 28). A freezing flash, a
 *       shatter star, eleven long ice blades thrusting out (they melt away in the afterglow), a crown of ice clusters, rime rings
 *       flying outward, a cracked frost sheet and fern spreading over the ground, shards flung on arcs, then breath mist and
 *       drifting snow that linger after the light is gone.</li>
 * </ul>
 */
public class IceLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ICE_FX1, VfxShape.ICE_FX2, VfxShape.ICE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case ICE_FX1 -> 16;
            case ICE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9FE8FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.ICE_FX3 && inst.power >= 3.6f) VfxShake.add(inst.payload.from(), 0.15f * Math.min(inst.power, 9f), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case ICE_FX1 -> fang(inst, ctx, buf);
            case ICE_FX2 -> field(inst, ctx, buf);
            case ICE_FX3 -> shatter(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: Heavenly Ice Fang
    private void fang(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, P = Mth.clamp(inst.power, 1f, 12f);
        final Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        final Vector3f path = new Vector3f(b).sub(a);
        final float len = Math.max(0.01f, path.length());
        final Vector3f dir = len > 0.05f ? new Vector3f(path).div(len) : new Vector3f(0, 0, 1);
        final int glow = glow(inst.color), hot = hot(glow);
        final float charge = Math.max(2f, D * 0.25f), startFly = charge * 0.75f, land = Math.max(startFly + 2f, D * 0.74f);
        final float chargeT = Mth.clamp(age / charge, 0f, 1f);
        final float flyT = Mth.clamp((age - startFly) / (land - startFly), 0f, 1f), e = ease(flyT);
        final float landT = Mth.clamp((age - land) / Math.max(1f, D - land), 0f, 1f);

        // ---- the gathering: a rime ring and a snowflake open facing the target, shards are drawn in, a cold flash builds
        final float sig = 1f - Mth.clamp((age - charge) / (D * 0.38f), 0f, 1f);
        if (sig > 0.01f) {
            float grow = VfxAnim.easeOutBack(chargeT);
            Vector3f at = new Vector3f(a).add(new Vector3f(dir).mul(0.1f * P));
            facing(buf, RIME, VfxBlend.ADD, at, dir, -age * 0.14f, 0.62f * P * grow, a(glow, 0.8f * sig));
            buf.billboard(ctx, FLAKE, VfxBlend.ADD, at, 0.8f * P * grow, age * 0.1f, a(hot, 0.95f * sig));
            VfxBloom.glow(ctx, buf, at, 0.5f * P * (0.35f + 0.65f * chargeT), glow, 0.9f * sig);
            if (chargeT < 1f) {
                for (int i = 0; i < 4; i++) {
                    float ang = Mth.TWO_PI * i / 4 + hash(inst.seed, i, 1) * 0.7f + chargeT * 1.3f;
                    float rr = 1.0f * P * (float) Math.pow(1f - chargeT, 1.4) + 0.06f * P;
                    Vector3f q = new Vector3f(at).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * rr)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * rr));
                    iceCell(buf, ctx, q, 0.3f * P, ang + 2.36f + age * 0.2f * (i % 2), i % SHARD_CELLS, glow, (0.25f + 0.75f * chargeT) * (1f - chargeT * chargeT * chargeT));
                }
            }
        }

        // ---- the fang and everything it drags behind it
        final float fangLen = 1.8f * P * (1f - 0.5f * landT), fangW = fangLen * 0.3f;
        final float grow = ease((age - charge * 0.45f) / (charge * 0.55f));
        final float fangA = grow * (1f - landT * landT);
        if (fangA > 0.02f) {
            Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(len * e + fangLen * 0.55f * (1f - e) * grow));
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(fangLen * grow));
            // afterimages
            for (int k = 2; k >= 1; k--) {
                Vector3f off = new Vector3f(dir).mul(-0.5f * P * k * Math.min(1f, flyT * 3f));
                strip(buf, FANG, VfxBlend.ADD, new Vector3f(tail).add(off), new Vector3f(head).add(off), fangW * 0.9f, fangW * 0.9f, a(glow, 0f), a(glow, 0.32f / k * fangA));
            }
            // the frost stream: narrow and white at the head, spreading dust behind it
            float trail = Math.min(len * e + fangLen, 4.5f * P + fangLen);
            Vector3f t0 = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
            strip(buf, TRAIL, VfxBlend.ADD, t0, head, 1.5f * P, 0.4f * P, a(glow, 0f), a(hot, 0.9f * fangA));
            // the fan of long blades swept back from the head, like the sheet of spikes in the art
            Vector3f s1 = new Vector3f(dir).cross(new Vector3f(head).add(tail).mul(-0.5f));
            if (s1.lengthSquared() > 1e-9f) {
                s1.normalize();
                for (int i = 0; i < 5; i++) {
                    float f = (i - 2f) / 2f, sp = f * (0.55f + 0.2f * hash(inst.seed, i, 2));
                    float bl = fangLen * (0.8f + 0.45f * hash(inst.seed, i, 3)) * (1f - 0.35f * Math.abs(f)) * grow;
                    Vector3f d = new Vector3f(dir).mul(-Mth.cos(sp)).add(new Vector3f(s1).mul(Mth.sin(sp)));
                    Vector3f base = new Vector3f(head).sub(new Vector3f(dir).mul(fangLen * (0.15f + 0.1f * Math.abs(f))));
                    iceBlade(buf, SPIKE, base, new Vector3f(base).add(d.mul(bl)), bl * 0.2f, glow, 0.75f * fangA);
                }
            }
            // the body: facing the camera, and a second plane turning about the axis
            iceBlade(buf, FANG, tail, head, fangW, glow, fangA);
            if (s1.lengthSquared() > 1e-9f) {
                float spin = age * 0.42f;
                Vector3f s2 = new Vector3f(s1).mul(Mth.cos(spin)).add(new Vector3f(dir).cross(s1).mul(Mth.sin(spin)));
                stripSide(buf, FANG, VfxBlend.ALPHA, tail, head, s2, fangW * 0.85f, fangW * 0.85f, a(body(glow), 0.7f * fangA), a(body(glow), 0.7f * fangA));
                stripSide(buf, FANG, VfxBlend.ADD, tail, head, s2, fangW * 0.85f, fangW * 0.85f, a(glow, 0.45f * fangA), a(hot, 0.6f * fangA));
                // splinters orbiting the shaft
                Vector3f u1 = new Vector3f(s1).cross(dir).normalize();
                for (int i = 0; i < 4; i++) {
                    float phi = age * 0.6f + i * 1.57f * (i % 2 == 0 ? 1f : -1f), along = 0.2f + 0.6f * hash(inst.seed, i, 4);
                    Vector3f q = new Vector3f(tail).lerp(head, along).add(new Vector3f(s1).mul(Mth.cos(phi) * 0.3f * P)).add(new Vector3f(u1).mul(Mth.sin(phi) * 0.3f * P));
                    iceCell(buf, ctx, q, 0.22f * P, age * 0.4f + i * 2f, i % SHARD_CELLS, glow, fangA);
                }
            }
            // a cold halo and a glint on the head
            VfxBloom.glow(ctx, buf, head, 0.6f * P, glow, 0.8f * fangA);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, head, 0.7f * P * (0.8f + 0.2f * Mth.sin(age * 1.3f)), age * 0.3f, a(WHITE, fangA));
        }

        // ---- breath mist left where it passed, and shards shed from the path
        final float span = land - startFly;
        int puffs = Mth.clamp(Math.round(len / 2.2f), 3, 4);
        for (int i = 0; i < puffs; i++) {
            float s = (i + 0.5f) / puffs;
            float tPass = startFly + (1f - (float) Math.cbrt(1f - s)) * span;
            float lt = (age - tPass) / Math.max(5f, D * 0.8f);
            if (lt <= 0f || lt >= 1f) continue;
            Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * s)).add(0, 0.12f * P * lt + (hash(inst.seed, i, 5) - 0.5f) * 0.2f * P, 0);
            float sz = (0.5f + 0.9f * ease(lt)) * P;
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, sz, hash(inst.seed, i, 6) * Mth.TWO_PI + lt, a(0xFFD2EEFF, 0.42f * (1f - lt)));
            if (i % 2 == 0) buf.billboard(ctx, MIST, VfxBlend.ADD, q, sz * 0.8f, hash(inst.seed, i, 7) * Mth.TWO_PI - lt, a(glow, 0.2f * (1f - lt)));
        }
        for (int i = 0; i < 3; i++) {
            float s = 0.2f + 0.7f * hash(inst.seed, i, 8);
            float tPass = startFly + (1f - (float) Math.cbrt(1f - s)) * span;
            float lt = (age - tPass) / Math.max(6f, D * 0.7f);
            if (lt <= 0f || lt >= 1f) continue;
            float sgn = hash(inst.seed, i, 9) - 0.5f;
            Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * s)).add(new Vector3f(ctx.camRight).mul(sgn * 1.0f * P * lt)).add(0, 0.2f * P * lt - 0.6f * P * lt * lt, 0);
            iceCell(buf, ctx, q, 0.24f * P, age * 0.5f * sgn + i, i % SHARD_CELLS, glow, 1f - lt);
        }

        // ---- the landing: shatter star, rime ring, frost patch, cold flash, fragments
        if (flyT >= 1f || age > land - 1f) {
            float it = Mth.clamp((age - (land - 1f)) / Math.max(2f, D - land + 1f), 0f, 1f), ie = ease(it);
            float fade = (1f - it) * (1f - it);
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, (0.4f + 1.5f * ie) * P, hash(inst.seed, 8, 1) * Mth.TWO_PI, a(hot, fade));
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, (0.3f + 0.9f * ie) * P, -hash(inst.seed, 8, 3) * Mth.TWO_PI, a(glow, 0.7f * fade));
            facing(buf, RIME, VfxBlend.ADD, b, dir, hash(inst.seed, 8, 2) * 6f, (0.2f + 1.1f * ie) * P, a(glow, fade * 0.9f));
            facing(buf, CRACK, VfxBlend.ALPHA, b, dir, hash(inst.seed, 8, 4) * 6f, (0.15f + 0.8f * ie) * P, a(body(glow), 0.8f * (1f - it * it)));
            facing(buf, CRACK, VfxBlend.ADD, b, dir, hash(inst.seed, 8, 4) * 6f, (0.15f + 0.8f * ie) * P, a(glow, 0.7f * (1f - it * it)));
            VfxBloom.glow(ctx, buf, b, 0.8f * P * (1f - 0.4f * it), glow, fade);
            for (int i = 0; i < 5; i++) {
                float ang = Mth.TWO_PI * i / 5 + hash(inst.seed, i, 10), el = (hash(inst.seed, i, 11) - 0.4f) * 1.6f;
                float d = (0.15f + 1.1f * ie * (0.6f + 0.6f * hash(inst.seed, i, 12))) * P;
                Vector3f q = new Vector3f(b).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * d - 0.6f * it * it * P, Mth.sin(ang) * Mth.cos(el) * d);
                iceCell(buf, ctx, q, 0.26f * P, age * 0.6f + i, (i + 1) % SHARD_CELLS, glow, 1f - it);
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Winter Field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, R = Math.max(1.5f, inst.power);
        final float fade = life(inst, age, 8f, 12f);
        if (fade <= 0.005f) return;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final int glow = glow(inst.color), hot = hot(glow);
        final float pulse = 0.5f + 0.5f * Mth.sin(age * 0.19f);
        final float rise = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)) * Mth.clamp((D - age) / 10f, 0f, 1f);
        final float wh = Mth.clamp(0.2f * R + 0.8f, 1.2f, 3.6f);

        // ---- the cracked ice sheet, the frost-fern star and the rime rings
        flat(buf, CRACK, VfxBlend.ALPHA, c, 0.03f, 0.4f, R * 1.06f, a(body(glow), 0.8f * fade));
        flat(buf, CRACK, VfxBlend.ADD, c, 0.045f, 0.4f, R * 1.06f, a(glow, (0.3f + 0.3f * pulse) * fade));
        flat(buf, FERN, VfxBlend.ADD, c, 0.06f, -age * 0.012f, R * 0.74f, a(hot, (0.45f + 0.25f * pulse) * fade));
        flat(buf, RIME, VfxBlend.ADD, c, 0.075f, age * 0.01f, R * 0.99f, a(hot, 0.85f * fade));
        float sw = (age % 20f) / 20f;
        flat(buf, RIME, VfxBlend.ADD, c, 0.09f, age * 0.03f, Math.max(0.2f, R * (0.1f + 0.9f * ease(sw))), a(glow, (1f - sw) * 0.8f * fade));

        // ---- breath mist rolling low over the field
        int puffs = ctx.seg(4, 3);
        for (int i = 0; i < puffs; i++) {
            float ang = Mth.TWO_PI * i / puffs + age * (i % 2 == 0 ? 0.012f : -0.009f) + hash(inst.seed, i, 1);
            float rr = R * (0.25f + 0.55f * hash(inst.seed, i, 2));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.4f + 0.12f * Mth.sin(age * 0.07f + i), Mth.sin(ang) * rr);
            float sz = Math.min(R * 0.7f, 4.5f) * (0.9f + 0.3f * hash(inst.seed, i, 3));
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, sz, ang + age * 0.01f, a(0xFFD2EEFF, 0.4f * fade));
            if (i % 2 == 0) buf.billboard(ctx, MIST, VfxBlend.ADD, q, sz * 0.8f, -ang, a(glow, 0.18f * fade));
        }

        // ---- the rim: a ring of ice spikes growing out of the ground
        if (rise > 0.02f) {
            int segs = ctx.seg(12, 8);
            float hh = wh * 2.2f * rise;
            float rep = Math.max(2f, Math.round(Mth.TWO_PI * R / (3.2f * wh * 2.2f)));
            hoop(buf, WALL, VfxBlend.ALPHA, c, R * 0.99f, -0.05f, hh, segs, rep, age * 0.0012f, a(body(glow), 0.92f * fade), a(body(glow), 0.92f * fade));
            hoop(buf, WALL, VfxBlend.ADD, c, R * 0.99f, -0.05f, hh, segs, rep, age * 0.0012f, a(glow, 0.55f * fade), a(hot, 0.7f * fade));
            // a second, lower row set back inside the first and turning the other way gives the rim depth
            hoop(buf, WALL, VfxBlend.ALPHA, c, R * 0.9f, -0.05f, hh * 0.6f, segs, rep * 0.8f, -age * 0.0015f + 0.3f, a(body(glow), 0.85f * fade), a(body(glow), 0.85f * fade));
            hoop(buf, WALL, VfxBlend.ADD, c, R * 0.9f, -0.05f, hh * 0.6f, segs, rep * 0.8f, -age * 0.0015f + 0.3f, a(glow, 0.45f * fade), a(hot, 0.6f * fade));
        }

        // ---- ice clusters thrusting up inside, each one on its own beat
        for (int i = 0; i < 4; i++) {
            float per = 30f + 4f * i, ph = age + per * hash(inst.seed, i, 4);
            int cyc = (int) Math.floor(ph / per);
            float lt = (ph / per) - cyc;
            float up = VfxAnim.easeOutBack(Mth.clamp(lt / 0.2f, 0f, 1f)) * (1f - smooth(0.72f, 1f, lt));
            if (up <= 0.02f) continue;
            float ang = hash(inst.seed + cyc * 31L, i, 5) * Mth.TWO_PI, rr = R * (0.2f + 0.62f * hash(inst.seed + cyc * 31L, i, 6));
            float h = (1.2f + 1.0f * hash(inst.seed + cyc * 31L, i, 7)) * Math.min(2.0f, 0.7f + 0.12f * R) * up;
            iceStand(buf, new Vector3f(c).add(Mth.cos(ang) * rr, -0.05f, Mth.sin(ang) * rr), h * 0.27f, h, 0f, glow, fade);
        }

        // ---- glints rising, snowflakes falling
        int motes = ctx.seg(5, 3);
        for (int i = 0; i < motes; i++) {
            float ph = (age * 0.028f + hash(inst.seed, i, 8)) % 1f, ang = hash(inst.seed, i, 9) * Mth.TWO_PI + age * 0.01f;
            float rr = R * (0.12f + 0.8f * hash(inst.seed, i, 10));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + ph * wh * 1.5f, Mth.sin(ang) * rr);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, (0.3f + 0.3f * hash(inst.seed, i, 11)) * (0.5f + 0.5f * Mth.sin(ph * Mth.PI)), age * 0.05f, a(WHITE, Mth.sin(ph * Mth.PI) * fade));
        }
        int snow = ctx.seg(6, 3);
        for (int i = 0; i < snow; i++) {
            float ph = (age * (0.02f + 0.012f * hash(inst.seed, i, 12)) + hash(inst.seed, i, 13)) % 1f, ang = hash(inst.seed, i, 14) * Mth.TWO_PI;
            float rr = R * (0.1f + 0.85f * hash(inst.seed, i, 15));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr + Mth.sin(age * 0.05f + i) * 0.25f, wh * 2.0f * (1f - ph) + 0.1f, Mth.sin(ang) * rr);
            buf.billboard(ctx, FLAKE, VfxBlend.ADD, q, 0.3f + 0.12f * hash(inst.seed, i, 16), age * 0.04f + i, a(FROST, 0.85f * Mth.sin(ph * Mth.PI) * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), Math.min(R * 0.5f, 3f), glow, 0.25f * fade * (0.7f + 0.3f * pulse));
    }

    // ------------------------------------------------------------------ FX3: Shatter
    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, P = Mth.clamp(inst.power, 1f, 12f);
        final float t = Mth.clamp(age / D, 0f, 1f), e = ease(age / 9f);
        final Vector3f c = ctx.rel(inst.from(ctx));
        final Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        final boolean aimed = hint.length() > 0.3f;
        final Vector3f dirn = aimed ? new Vector3f(hint).normalize() : new Vector3f(0, 1, 0);
        final int glow = glow(inst.color), hot = hot(glow);
        final float fade = 1f - VfxAnim.easeInCubic(t);

        // ---- the cracked frost sheet and fern spreading over the ground, breath mist rising out of it
        float sheet = ease(age / 12f), sheetFade = 1f - smooth(0.55f, 1f, t);
        Vector3f gc = new Vector3f(c).add(0, -0.12f * P, 0);
        flat(buf, CRACK, VfxBlend.ALPHA, gc, 0f, hash(inst.seed, 0, 1) * 6f, 1.0f * P * sheet, a(body(glow), 0.85f * sheetFade));
        flat(buf, CRACK, VfxBlend.ADD, gc, 0.02f, hash(inst.seed, 0, 1) * 6f, 1.0f * P * sheet, a(glow, (0.25f + 0.7f * fade) * sheetFade));
        flat(buf, FERN, VfxBlend.ADD, gc, 0.03f, hash(inst.seed, 0, 2) * 6f + age * 0.01f, 0.8f * P * sheet, a(hot, (0.3f + 0.6f * fade) * sheetFade));
        if (t > 0.12f) {
            for (int i = 0; i < 3; i++) {
                float st = Mth.clamp((t - 0.12f) / 0.88f, 0f, 1f);
                Vector3f q = new Vector3f(c).add((hash(inst.seed, i, 3) - 0.5f) * 0.9f * P, -0.05f * P + st * 0.5f * P, (hash(inst.seed, i, 4) - 0.5f) * 0.9f * P);
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, (0.6f + 0.6f * st) * P, hash(inst.seed, i, 5) * Mth.TWO_PI + st, a(0xFFD2EEFF, 0.42f * (1f - st)));
            }
        }

        // ---- the blades: long ice spikes thrust out of the centre, fanning toward the hint if there is one, and melting in the afterglow
        for (int i = 0; i < 11; i++) {
            float delay = hash(inst.seed, i, 6) * 3.5f;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 6f, 0f, 1f));
            float retract = 1f - smooth(0.5f, 0.95f, t);
            if (grow <= 0.01f || retract <= 0.01f) continue;
            Vector3f d = new Vector3f(hash(inst.seed, i, 7) - 0.5f, hash(inst.seed, i, 8) * 0.9f - 0.15f, hash(inst.seed, i, 9) - 0.5f);
            if (aimed && i < 7) d.mul(0.55f).add(new Vector3f(dirn).mul(1.0f)); else if (!aimed && i < 4) d.add(0, 0.9f, 0);
            if (d.lengthSquared() < 1e-6f) d.set(0, 1, 0);
            d.normalize();
            float L = (0.85f + 0.7f * hash(inst.seed, i, 10)) * P * grow * (0.35f + 0.65f * retract), w = L * 0.2f;
            Vector3f from = new Vector3f(c).add(new Vector3f(d).mul(0.05f * P));
            iceBlade(buf, SPIKE, from, new Vector3f(from).add(new Vector3f(d).mul(L)), w, glow, retract);
        }

        // ---- a crown of ice clusters thrust up round the centre
        for (int i = 0; i < 4; i++) {
            float delay = 1f + hash(inst.seed, i, 20) * 3f;
            float up = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 7f, 0f, 1f)) * (1f - smooth(0.55f, 0.95f, t));
            if (up <= 0.02f) continue;
            float ang = Mth.TWO_PI * (i + 0.5f * hash(inst.seed, i, 21)) / 4f + 0.4f, rr = (0.35f + 0.3f * hash(inst.seed, i, 22)) * P;
            float h = (0.4f + 0.3f * hash(inst.seed, i, 23)) * P * up;
            iceStand(buf, new Vector3f(c).add(Mth.cos(ang) * rr, -0.12f * P, Mth.sin(ang) * rr), h * 0.3f, h, 0f, glow, 1f - smooth(0.7f, 1f, t));
        }

        // ---- rime rings flying outward
        float ringR = (0.12f + 1.15f * e) * P;
        flat(buf, RIME, VfxBlend.ADD, gc, 0.03f, 1.1f, ringR, a(glow, 0.85f * fade));
        buf.billboard(ctx, RIME, VfxBlend.ADD, c, (0.3f + 1.1f * ease(age / 6f)) * P, age * 0.04f, a(hot, 0.85f * (1f - ease(age / 14f))));

        // ---- the flash, the shatter star, a white-hot bloom
        float flash = Mth.clamp(1f - age / 6f, 0f, 1f);
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.35f + 1.35f * e) * P, age * 0.05f, a(hot, 1f - ease(age / 16f)));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.25f + 0.8f * e) * P, -age * 0.07f + 0.6f, a(glow, 0.7f * fade));
        VfxBloom.glow(ctx, buf, c, 0.8f * P * (1f - 0.45f * t), glow, 0.9f * fade + 0.1f + 0.4f * flash);

        // ---- shards flung on arcs
        for (int i = 0; i < 6; i++) {
            float ang = hash(inst.seed, i, 11) * Mth.TWO_PI, up = 0.3f + hash(inst.seed, i, 12);
            float d = (0.4f + 1.0f * hash(inst.seed, i, 13)) * P * ease(age / 16f);
            float lt = Mth.clamp(age / D, 0f, 1f);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, up * d * 0.8f - 1.1f * lt * lt * P, Mth.sin(ang) * d);
            if (aimed) q.add(new Vector3f(dirn).mul(d * 0.6f));
            iceCell(buf, ctx, q, (0.2f + 0.2f * hash(inst.seed, i, 14)) * P, age * 0.4f * (hash(inst.seed, i, 15) - 0.5f) * 2f + i, i % SHARD_CELLS, glow, 1f - smooth(0.6f, 1f, lt));
        }
        // ---- snow drifting in the afterglow
        for (int i = 0; i < 4; i++) {
            float ph = (t * 0.9f + hash(inst.seed, i, 16)) % 1f, ang = hash(inst.seed, i, 17) * Mth.TWO_PI;
            float d = (0.2f + 0.8f * hash(inst.seed, i, 18)) * P * ease(age / 18f);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, 0.5f * P * (1f - ph) - 0.05f * P, Mth.sin(ang) * d);
            buf.billboard(ctx, FLAKE, VfxBlend.ADD, q, 0.14f * P, age * 0.07f + i, a(FROST, 0.9f * Mth.sin(ph * Mth.PI) * (0.3f + 0.7f * smooth(0.1f, 0.5f, t))));
        }
    }
}
