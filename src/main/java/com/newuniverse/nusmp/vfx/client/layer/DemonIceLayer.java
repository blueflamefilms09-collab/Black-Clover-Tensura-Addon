package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.DemonIceFx.*;

/**
 * Demon Ice Magic (Demon Frost): corrupted ice, black glassy crystal whose seams leak violet-blue light, frost that crawls as ferns and
 * cracks, black cold fog, angular frost runes, and a flash that INVERTS the world (NEGATIVE blend) when something freezes. Textures from
 * tools/gen_demon_ice_textures.py (demon_ice_*). The crystal sprites are drawn twice: ALPHA in a near-black tint (the body) and ADD in
 * blue / violet (only the seams and edges light up). 'color' is a tint mixed 35 % into the palette (white gives pale blue, the default
 * is the owner's royal blue), so the effect always reads as this magic.
 * <ul>
 *   <li>DEMON_ICE_FX1, Frost Lance (cast / projectile): 'from' = hand / eye, 'to' = target, power = size (1 = a 2.6 block lance), duration =
 *       flight ticks (default 16). The first quarter: a spinning six-armed frost sigil opens facing the target, shards of black ice are
 *       drawn into it and a flash gathers. Then a long black-crystal spear (serrated blade, barbed collars, splintering tail, a second
 *       plane spinning round its axis so it reads as 3D) shoots out leading three fainter afterimages, a spray of frost dust and
 *       black fog puffs that stay where it passed, splinters orbiting it and shards shed from its path. At 'to': a shatter star, a
 *       frost ring, a flash of inverted colours and a spray of fragments.</li>
 *   <li>DEMON_ICE_FX2, Rimeborn Domain (zone / field / dome): 'from' = centre on the ground, power = RADIUS, duration = life ticks
 *       (default 80), fades in over 8 ticks and out over the last 12. A frozen black sheet with a glowing fracture network, a slowly
 *       turning rune circle with a counter-turning frost-fern star, black fog rolling over it, a wall of black crystals with a
 *       veil of cold light standing on the rim (it grows out of the ground and sinks at the end) and a ribbon of runes circling
 *       it, crystal clusters thrusting up inside, ice dust rising and snow falling, a pulse ring sweeping out every second.</li>
 *   <li>DEMON_ICE_FX3, Absolute Zero Bloom (impact / burst / signature): 'from' = centre, 'to' = optional direction (to - from, may be
 *       zero: then it bursts all round, else the spikes fan toward it), power = scale, duration = life ticks (default 28). A
 *       flash that inverts the colours behind it, a shatter star and white-hot bloom, nine black-crystal spikes thrusting out
 *       (they retract in the afterglow), a rune band and two frost rings flying outward, a cracked black frost sheet spreading over the
 *       ground, fragments flung on arcs, and cold fog and drifting snow that linger after the light is gone.</li>
 * </ul>
 */
public class DemonIceLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_ICE_FX1, VfxShape.DEMON_ICE_FX2, VfxShape.DEMON_ICE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DEMON_ICE_FX1 -> 16;
            case DEMON_ICE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF3A6AFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.DEMON_ICE_FX3 && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 0.45f * Math.min(inst.power, 2.5f), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DEMON_ICE_FX1 -> lance(inst, ctx, buf);
            case DEMON_ICE_FX2 -> domain(inst, ctx, buf);
            case DEMON_ICE_FX3 -> bloom(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: Frost Lance
    private void lance(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Mth.clamp(inst.power, 0.5f, 4f);
        final Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        final Vector3f path = new Vector3f(b).sub(a);
        final float len = Math.max(0.01f, path.length());
        final Vector3f dir = len > 0.05f ? new Vector3f(path).div(len) : new Vector3f(0, 0, 1);
        final int glow = glow(inst.color), edge = edge(glow), hot = hot(glow);
        final float charge = Math.max(2f, D * 0.25f), startFly = charge * 0.75f, land = Math.max(startFly + 2f, D * 0.74f);
        final float chargeT = Mth.clamp(age / charge, 0f, 1f);
        final float flyT = Mth.clamp((age - startFly) / (land - startFly), 0f, 1f), e = ease(flyT);
        final float landT = Mth.clamp((age - land) / Math.max(1f, D - land), 0f, 1f);

        // ---- the gathering: a frost sigil opens facing the target, black shards are drawn in, a flash builds
        final float sig = 1f - Mth.clamp((age - charge) / (D * 0.38f), 0f, 1f);
        if (sig > 0.01f) {
            float grow = VfxAnim.easeOutBack(chargeT);
            Vector3f at = new Vector3f(a).add(new Vector3f(dir).mul(0.25f * p));
            facing(buf, SIGIL, VfxBlend.ADD, at, dir, -age * 0.16f, 1.0f * p * grow, a(edge, 0.75f * sig));
            facing(buf, FLAKE, VfxBlend.ADD, new Vector3f(at).add(new Vector3f(dir).mul(0.03f)), dir, age * 0.12f, 0.62f * p * grow, a(hot, 0.95f * sig));
            VfxBloom.glow(ctx, buf, at, 0.75f * p * (0.35f + 0.65f * chargeT), glow, 0.85f * sig);
            if (chargeT < 1f) {
                for (int i = 0; i < 6; i++) {
                    float ang = Mth.TWO_PI * i / 6 + hash(inst.seed, i, 1) * 0.7f + chargeT * 1.3f;
                    float rr = 1.8f * p * (float) Math.pow(1f - chargeT, 1.4) + 0.1f * p;
                    Vector3f q = new Vector3f(at).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * rr)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * rr));
                    float rot = ang + 2.36f;
                    float al = (0.25f + 0.75f * chargeT) * (1f - chargeT * chargeT * chargeT);
                    int k = i % 2 == 0 ? 0 : 1 + (i / 2) % 3;
                    cell(buf, ctx, SHARDS, VfxBlend.ALPHA, q, 0.46f * p, k == 0 ? rot : age * 0.3f + i, k, SHARD_CELLS, a(BODY, al));
                    cell(buf, ctx, SHARDS, VfxBlend.ADD, q, 0.46f * p, k == 0 ? rot : age * 0.3f + i, k, SHARD_CELLS, a(edge, 0.9f * al));
                }
            }
        }

        // ---- the lance and everything it drags behind it
        final float lanceLen = 2.6f * p * (1f - 0.5f * landT), lanceW = lanceLen * 0.25f * 1.15f;
        final float grow = ease((age - charge * 0.45f) / (charge * 0.55f));
        final float lanceA = grow * (1f - landT * landT);
        if (lanceA > 0.02f) {
            Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(len * e + lanceLen * 0.55f * (1f - e) * grow));
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(lanceLen * grow));
            // afterimages, oldest first
            for (int k = 3; k >= 1; k--) {
                Vector3f off = new Vector3f(dir).mul(-0.85f * p * k * Math.min(1f, flyT * 3f));
                strip(buf, LANCE, VfxBlend.ADD, new Vector3f(tail).add(off), new Vector3f(head).add(off), lanceW * 0.9f, lanceW * 0.9f, a(glow, 0.0f), a(edge, 0.30f / k * lanceA));
            }
            // the frost trail: narrow and hot at the head, spreading dust behind it
            float trail = Math.min(len * e + lanceLen, 6f * p + lanceLen);
            Vector3f t0 = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
            strip(buf, SPRAY, VfxBlend.ALPHA, t0, head, 1.7f * p, 0.45f * p, a(INK, 0.0f), a(INK, 0.55f * lanceA));
            strip(buf, SPRAY, VfxBlend.ADD, t0, head, 1.5f * p, 0.4f * p, a(glow, 0.0f), a(hot, 0.9f * lanceA));
            // the body: facing the camera, and a second plane turning about the axis
            strip(buf, LANCE, VfxBlend.ALPHA, tail, head, lanceW, lanceW, a(BODY, lanceA), a(BODY, lanceA));
            strip(buf, LANCE, VfxBlend.ADD, tail, head, lanceW, lanceW, a(glow, 0.85f * lanceA), a(edge, lanceA));
            Vector3f s1 = new Vector3f(dir).cross(new Vector3f(head).add(tail).mul(-0.5f));
            if (s1.lengthSquared() > 1e-9f) {
                s1.normalize();
                float spin = age * 0.42f;
                Vector3f s2 = new Vector3f(s1).mul(Mth.cos(spin)).add(new Vector3f(dir).cross(s1).mul(Mth.sin(spin)));
                stripSide(buf, LANCE, VfxBlend.ALPHA, tail, head, s2, lanceW * 0.85f, lanceW * 0.85f, a(BODY, 0.9f * lanceA), a(BODY, 0.9f * lanceA));
                stripSide(buf, LANCE, VfxBlend.ADD, tail, head, s2, lanceW * 0.85f, lanceW * 0.85f, a(edge, 0.55f * lanceA), a(edge, 0.7f * lanceA));
                // three splinters orbiting the shaft
                Vector3f u1 = new Vector3f(s1).cross(dir).normalize();
                for (int i = 0; i < 3; i++) {
                    float phi = age * 0.65f + i * 2.09f, along = 0.15f + 0.6f * hash(inst.seed, i, 2);
                    Vector3f q = new Vector3f(tail).lerp(head, along).add(new Vector3f(s1).mul(Mth.cos(phi) * 0.42f * p)).add(new Vector3f(u1).mul(Mth.sin(phi) * 0.42f * p));
                    cell(buf, ctx, SHARDS, VfxBlend.ALPHA, q, 0.3f * p, age * 0.4f + i * 2f, i % 4, SHARD_CELLS, a(BODY, lanceA));
                    cell(buf, ctx, SHARDS, VfxBlend.ADD, q, 0.3f * p, age * 0.4f + i * 2f, i % 4, SHARD_CELLS, a(edge, 0.8f * lanceA));
                }
            }
            VfxBloom.glow(ctx, buf, head, 0.7f * p, glow, 0.85f * lanceA);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, head, 0.95f * p * (0.8f + 0.2f * Mth.sin(age * 1.3f)), age * 0.3f, a(hot, lanceA));
        }

        // ---- black fog left where it passed, and shards shed from the path
        final float span = land - startFly;
        int puffs = Mth.clamp(Math.round(len / 1.7f), 3, 6);
        for (int i = 0; i < puffs; i++) {
            float s = (i + 0.5f) / puffs;
            float tPass = startFly + (1f - (float) Math.cbrt(1f - s)) * span;
            float lt = (age - tPass) / Math.max(5f, D * 0.8f);
            if (lt <= 0f || lt >= 1f) continue;
            Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * s)).add(0, 0.2f * p * lt + (hash(inst.seed, i, 3) - 0.5f) * 0.3f * p, 0);
            float sz = (0.9f + 1.5f * ease(lt)) * p;
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, sz, hash(inst.seed, i, 4) * Mth.TWO_PI + lt, a(INK, 0.55f * (1f - lt)));
            if (i % 2 == 0) buf.billboard(ctx, MIST, VfxBlend.ADD, q, sz * 0.8f, hash(inst.seed, i, 5) * Mth.TWO_PI - lt, a(glow, 0.22f * (1f - lt)));
        }
        for (int i = 0; i < 4; i++) {
            float s = 0.2f + 0.7f * hash(inst.seed, i, 6);
            float tPass = startFly + (1f - (float) Math.cbrt(1f - s)) * span;
            float lt = (age - tPass) / Math.max(6f, D * 0.7f);
            if (lt <= 0f || lt >= 1f) continue;
            float sgn = hash(inst.seed, i, 7) - 0.5f;
            Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * s)).add(new Vector3f(ctx.camRight).mul(sgn * 1.6f * p * lt)).add(0, 0.3f * p * lt - 1.0f * p * lt * lt, 0);
            cell(buf, ctx, SHARDS, VfxBlend.ALPHA, q, 0.32f * p, age * 0.5f * sgn + i, i % SHARD_CELLS, SHARD_CELLS, a(BODY, 1f - lt));
            cell(buf, ctx, SHARDS, VfxBlend.ADD, q, 0.32f * p, age * 0.5f * sgn + i, i % SHARD_CELLS, SHARD_CELLS, a(edge, 0.9f * (1f - lt)));
        }

        // ---- the landing: shatter star, frost ring, a flash of inverted colour, fragments
        if (flyT >= 1f || age > land - 1f) {
            float it = Mth.clamp((age - (land - 1f)) / Math.max(2f, D - land + 1f), 0f, 1f), ie = ease(it);
            float fade = (1f - it) * (1f - it);
            if (it < 0.7f) buf.billboard(ctx, INVERT, VfxBlend.NEGATIVE, b, 1.9f * p * (0.5f + 0.5f * ie), 0, dim(0xFFA8C0FF, 1f - it / 0.7f));
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, (0.5f + 1.7f * ie) * p, hash(inst.seed, 8, 1) * Mth.TWO_PI, a(hot, fade));
            facing(buf, RING, VfxBlend.ADD, b, dir, hash(inst.seed, 8, 2) * 6f, (0.3f + 1.7f * ie) * p, a(edge, fade * 0.9f));
            VfxBloom.glow(ctx, buf, b, 1.1f * p * (1f - 0.4f * it), glow, fade);
            for (int i = 0; i < 5; i++) {
                float ang = Mth.TWO_PI * i / 5 + hash(inst.seed, i, 9), el = (hash(inst.seed, i, 10) - 0.4f) * 1.6f;
                float d = (0.2f + 1.5f * ie * (0.6f + 0.6f * hash(inst.seed, i, 11))) * p;
                Vector3f q = new Vector3f(b).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * d - 0.9f * it * it * p, Mth.sin(ang) * Mth.cos(el) * d);
                cell(buf, ctx, SHARDS, VfxBlend.ALPHA, q, 0.34f * p, age * 0.6f + i, (i + 1) % SHARD_CELLS, SHARD_CELLS, a(BODY, 1f - it));
                cell(buf, ctx, SHARDS, VfxBlend.ADD, q, 0.34f * p, age * 0.6f + i, (i + 1) % SHARD_CELLS, SHARD_CELLS, a(edge, 0.9f * (1f - it)));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Rimeborn Domain
    private void domain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, R = Math.max(1.5f, inst.power);
        final float fade = life(inst, age, 8f, 12f);
        if (fade <= 0.005f) return;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final int glow = glow(inst.color), edge = edge(glow), hot = hot(glow);
        final float pulse = 0.5f + 0.5f * Mth.sin(age * 0.19f);
        final float rise = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)) * Mth.clamp((D - age) / 10f, 0f, 1f);
        final float wh = Mth.clamp(0.28f * R + 0.8f, 1.4f, 4.2f);

        // ---- the frozen sheet and its cracks, the black fog rolling over it
        flat(buf, CRACKS, VfxBlend.ALPHA, c, 0.03f, 0.4f, R * 1.06f, a(0xFF141A44, 0.88f * fade));
        flat(buf, CRACKS, VfxBlend.ADD, c, 0.045f, 0.4f, R * 1.06f, a(glow, (0.45f + 0.35f * pulse) * fade));
        int puffs = ctx.seg(5, 3);
        for (int i = 0; i < puffs; i++) {
            float ang = Mth.TWO_PI * i / puffs + age * (i % 2 == 0 ? 0.012f : -0.009f) + hash(inst.seed, i, 1);
            float rr = R * (0.28f + 0.5f * hash(inst.seed, i, 2));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.45f + 0.15f * Mth.sin(age * 0.07f + i), Mth.sin(ang) * rr);
            float sz = Math.min(R * 0.8f, 4.5f) * (0.9f + 0.3f * hash(inst.seed, i, 3));
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, sz, ang + age * 0.01f, a(INK, 0.42f * fade));
            if (i % 2 == 0) buf.billboard(ctx, MIST, VfxBlend.ADD, q, sz * 0.8f, -ang, a(glow, 0.2f * fade));
        }

        // ---- the rune circle turning one way, the frost-fern star the other
        flat(buf, SIGIL, VfxBlend.ADD, c, 0.06f, age * 0.013f, R * 0.97f, a(edge, 0.8f * fade));
        flat(buf, FLAKE, VfxBlend.ADD, c, 0.075f, -age * 0.021f, R * 0.64f, a(hot, (0.55f + 0.25f * pulse) * fade));
        float sw = (age % 20f) / 20f;
        flat(buf, RING, VfxBlend.ADD, c, 0.09f, age * 0.03f, Math.max(0.2f, R * (0.1f + 0.9f * ease(sw))), a(VIOLET, (1f - sw) * 0.75f * fade));

        // ---- the rim: black crystals and the veil of cold light, and a ribbon of runes circling it
        if (rise > 0.02f) {
            int segs = ctx.seg(16, 10);
            float hh = wh * 1.45f * rise;
            float rep = Math.max(2f, Math.round(Mth.TWO_PI * R / (4f * wh * 1.45f)));
            hoop(buf, WALL, VfxBlend.ALPHA, c, R * 0.99f, -0.05f, hh, segs, rep, age * 0.0016f, a(BODY, 0.97f * fade), a(BODY, 0.97f * fade));
            hoop(buf, WALL, VfxBlend.ADD, c, R * 0.99f, -0.05f, hh, segs, rep, age * 0.0016f, a(glow, 0.9f * fade), a(edge, 0.9f * fade));
            hoop(buf, RUNES, VfxBlend.ADD, c, R * 0.985f, wh * 0.46f * rise, 0.42f * Math.min(wh, 3f) * rise, ctx.seg(12, 8), Math.max(2f, Math.round(Mth.TWO_PI * R / (8f * 0.42f * Math.min(wh, 3f)))),
                    -age * 0.0035f, a(hot, 0.65f * fade), a(hot, 0.65f * fade));
        }

        // ---- crystal clusters thrusting up inside, each one on its own beat
        for (int i = 0; i < 5; i++) {
            float per = 30f + 4f * i, ph = age + per * hash(inst.seed, i, 4);
            int cyc = (int) Math.floor(ph / per);
            float lt = (ph / per) - cyc;
            float up = VfxAnim.easeOutBack(Mth.clamp(lt / 0.2f, 0f, 1f)) * (1f - smooth(0.72f, 1f, lt));
            if (up <= 0.02f) continue;
            float ang = hash(inst.seed + cyc * 31L, i, 5) * Mth.TWO_PI, rr = R * (0.2f + 0.62f * hash(inst.seed + cyc * 31L, i, 6));
            float h = (0.9f + 0.8f * hash(inst.seed + cyc * 31L, i, 7)) * Math.min(1.5f, 0.55f + 0.1f * R) * up;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * rr, -0.05f, Mth.sin(ang) * rr);
            stand(buf, CRYSTAL, VfxBlend.ALPHA, base, h * 0.27f, h, 0f, a(BODY, fade));
            stand(buf, CRYSTAL, VfxBlend.ADD, base, h * 0.27f, h, 0f, a(edge, 0.85f * fade));
        }

        // ---- ice dust rising, snow falling
        int motes = ctx.seg(6, 3);
        for (int i = 0; i < motes; i++) {
            float ph = (age * 0.028f + hash(inst.seed, i, 8)) % 1f, ang = hash(inst.seed, i, 9) * Mth.TWO_PI + age * 0.01f;
            float rr = R * (0.12f + 0.8f * hash(inst.seed, i, 10));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + ph * wh * 1.7f, Mth.sin(ang) * rr);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, (0.22f + 0.25f * hash(inst.seed, i, 11)) * (0.5f + 0.5f * Mth.sin(ph * Mth.PI)), age * 0.05f, a(hot, Mth.sin(ph * Mth.PI) * fade));
        }
        int snow = ctx.seg(8, 4);
        for (int i = 0; i < snow; i++) {
            float ph = (age * (0.02f + 0.012f * hash(inst.seed, i, 12)) + hash(inst.seed, i, 13)) % 1f, ang = hash(inst.seed, i, 14) * Mth.TWO_PI;
            float rr = R * (0.1f + 0.85f * hash(inst.seed, i, 15));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr + Mth.sin(age * 0.05f + i) * 0.25f, wh * 2.2f * (1f - ph) + 0.1f, Mth.sin(ang) * rr);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.13f, age * 0.08f + i, a(FROST, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), Math.min(R * 0.5f, 3f), glow, 0.28f * fade * (0.7f + 0.3f * pulse));
    }

    // ------------------------------------------------------------------ FX3: Absolute Zero Bloom
    private void bloom(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Mth.clamp(inst.power, 0.5f, 4f);
        final float t = Mth.clamp(age / D, 0f, 1f), e = ease(age / 9f);
        final Vector3f c = ctx.rel(inst.from(ctx));
        final Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        final boolean aimed = hint.length() > 0.3f;
        final Vector3f dirn = aimed ? new Vector3f(hint).normalize() : new Vector3f(0, 1, 0);
        final int glow = glow(inst.color), edge = edge(glow), hot = hot(glow);
        final float fade = 1f - VfxAnim.easeInCubic(t);

        // ---- the cracked black sheet spreading over the ground, and fog rising out of it
        float sheet = ease(age / 12f), sheetFade = 1f - smooth(0.55f, 1f, t);
        flat(buf, CRACKS, VfxBlend.ALPHA, new Vector3f(c).add(0, -0.45f * p, 0), 0f, hash(inst.seed, 0, 1) * 6f, 2.9f * p * sheet, a(0xFF141A44, 0.9f * sheetFade));
        flat(buf, CRACKS, VfxBlend.ADD, new Vector3f(c).add(0, -0.45f * p, 0), 0.02f, hash(inst.seed, 0, 1) * 6f, 2.9f * p * sheet, a(glow, (0.25f + 0.85f * fade) * sheetFade));
        if (t > 0.15f) {
            for (int i = 0; i < 3; i++) {
                float st = Mth.clamp((t - 0.15f) / 0.85f, 0f, 1f);
                Vector3f q = new Vector3f(c).add((hash(inst.seed, i, 2) - 0.5f) * 2.4f * p, -0.1f * p + st * 1.6f * p, (hash(inst.seed, i, 3) - 0.5f) * 2.4f * p);
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, (1.6f + 1.6f * st) * p, hash(inst.seed, i, 4) * Mth.TWO_PI + st, a(INK, 0.5f * (1f - st)));
            }
        }

        // ---- the spikes: black crystals thrust out of the centre, fanning toward the hint if there is one, and retract in the afterglow
        for (int i = 0; i < 9; i++) {
            float delay = hash(inst.seed, i, 5) * 3.5f;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 6f, 0f, 1f));
            float retract = 1f - smooth(0.5f, 0.95f, t);
            if (grow <= 0.01f || retract <= 0.01f) continue;
            Vector3f d = new Vector3f(hash(inst.seed, i, 6) - 0.5f, hash(inst.seed, i, 7) * 0.9f - 0.2f, hash(inst.seed, i, 8) - 0.5f);
            if (aimed && i < 6) d.mul(0.55f).add(new Vector3f(dirn).mul(1.0f)); else if (!aimed && i < 3) d.add(0, 0.9f, 0);
            if (d.lengthSquared() < 1e-6f) d.set(0, 1, 0);
            d.normalize();
            float L = (1.5f + 1.5f * hash(inst.seed, i, 9)) * p * grow * (0.35f + 0.65f * retract), w = L * 0.27f;
            Vector3f from = new Vector3f(c).add(new Vector3f(d).mul(0.15f * p));
            Vector3f to = new Vector3f(from).add(new Vector3f(d).mul(L));
            strip(buf, LANCE, VfxBlend.ALPHA, from, to, w, w, a(BODY, 0f), a(BODY, retract));
            strip(buf, LANCE, VfxBlend.ADD, from, to, w, w, a(glow, 0.5f * retract), a(edge, 0.95f * retract));
        }

        // ---- rings and the rune band flying outward
        float ringR = (0.35f + 3.4f * e) * p;
        flat(buf, RING, VfxBlend.ALPHA, new Vector3f(c).add(0, -0.4f * p, 0), 0.01f, 0.5f, ringR * 0.86f, a(INK, 0.6f * Math.max(0f, 1f - t * 1.4f)));
        flat(buf, RING, VfxBlend.ADD, new Vector3f(c).add(0, -0.4f * p, 0), 0.03f, 1.1f, ringR, a(edge, 1.1f * fade));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, (0.8f + 3.2f * ease(age / 6f)) * p, age * 0.04f, a(hot, 0.85f * (1f - ease(age / 14f))));
        float bandT = ease(age / 14f);
        if (bandT < 0.999f && ctx.detail > 0.2f) {
            float bh = 0.5f * p * (1f - bandT * 0.6f);
            hoop(buf, RUNES, VfxBlend.ADD, new Vector3f(c).add(0, -0.3f * p, 0), (0.4f + 2.4f * bandT) * p, 0f, bh, ctx.seg(12, 8), 3, age * 0.01f, a(hot, 0.9f * (1f - bandT)), a(edge, 0.9f * (1f - bandT)));
        }

        // ---- the flash: inverted colours, the shatter star, white-hot bloom
        float flash = Mth.clamp(1f - age / 6f, 0f, 1f);
        if (flash > 0f) buf.billboard(ctx, INVERT, VfxBlend.NEGATIVE, c, (2.0f + 2.6f * (1f - flash)) * p, 0, dim(0xFFB4C8FF, flash));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.7f + 3.6f * e) * p, age * 0.05f, a(hot, (1f - ease(age / 16f))));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (0.5f + 2.2f * e) * p, -age * 0.07f + 0.6f, a(edge, 0.7f * fade));
        VfxBloom.glow(ctx, buf, c, 2.2f * p * (1f - 0.45f * t), glow, 0.9f * fade + 0.1f);

        // ---- fragments flung on arcs
        for (int i = 0; i < 8; i++) {
            float ang = hash(inst.seed, i, 10) * Mth.TWO_PI, up = 0.3f + hash(inst.seed, i, 11);
            float d = (1.0f + 2.8f * hash(inst.seed, i, 12)) * p * ease(age / 16f);
            float lt = Mth.clamp(age / D, 0f, 1f);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, up * d * 0.8f - 3.4f * lt * lt * p, Mth.sin(ang) * d);
            if (aimed) q.add(new Vector3f(dirn).mul(d * 0.6f));
            float al = 1f - smooth(0.6f, 1f, lt);
            cell(buf, ctx, SHARDS, VfxBlend.ALPHA, q, (0.28f + 0.3f * hash(inst.seed, i, 13)) * p, age * 0.4f * (hash(inst.seed, i, 14) - 0.5f) * 2f + i, i % SHARD_CELLS, SHARD_CELLS, a(BODY, al));
            cell(buf, ctx, SHARDS, VfxBlend.ADD, q, (0.28f + 0.3f * hash(inst.seed, i, 13)) * p, age * 0.4f * (hash(inst.seed, i, 14) - 0.5f) * 2f + i, i % SHARD_CELLS, SHARD_CELLS, a(edge, 0.9f * al));
        }
        // ---- snow drifting in the afterglow
        for (int i = 0; i < 5; i++) {
            float ph = (t * 0.9f + hash(inst.seed, i, 15)) % 1f, ang = hash(inst.seed, i, 16) * Mth.TWO_PI;
            float d = (0.5f + 2.2f * hash(inst.seed, i, 17)) * p * ease(age / 18f);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * d, 1.2f * p * (1f - ph) - 0.2f * p, Mth.sin(ang) * d);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.16f * p, age * 0.07f + i, a(FROST, 0.8f * Mth.sin(ph * Mth.PI) * (0.3f + 0.7f * smooth(0.1f, 0.5f, t))));
        }
    }
}
