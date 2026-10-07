package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BodyFx.*;

/**
 * Body Magic (Titan): pulsing muscle, rapid steam, veins that glow with the heartbeat, and Body Compression (the user restructures a
 * limb into a rifled gun barrel and fires a compressed round). Textures from tools/gen_body_textures.py, primitives in BodyFx.
 * The palette is bronze-orange light over dark muscle tissue with white steam; the spell's tint is only mixed in a third of the way, so
 * the effect reads as Body Magic whatever colour it is given.
 * <ul>
 *   <li>BODY_FX1, cast / projectile (Body Compression): 'from' = origin, 'to' = target, power = size (1 = a round about a block wide),
 *       duration = flight ticks (16). A barrel of flesh forms a little ahead of 'from': three compression rings collapse into it, a
 *       rifled muzzle ring with a fibre tube behind it and a hot core. At the launch a muzzle star flashes and a steam blast shoots
 *       forward; the round (dark flesh body, glowing banded shell, white-hot nose) flies to 'to' with two fibre strands spinning round it
 *       like rifling, a ghosted afterimage, a cone of shock discs and a flowing fibre ribbon, leaving puffs of steam hanging along the
 *       path. At 'to' a small flash: star, shock disc facing the shooter, steam puffs and sparks.</li>
 *   <li>BODY_FX2, zone / field: 'from' = ground centre, power = radius in blocks, duration = life (80). A dark muscle-tissue disc with a
 *       vein web that throbs with a double heartbeat (a hard thump, a softer one 7 ticks later, every 24 ticks), a sigil ring of twelve
 *       muscle bellies with an ECG line turning over it and a smaller one against it, heartbeat rings sweeping out to the rim on each
 *       beat, a rim of twisted muscle ropes dissolving into steam, steam jets venting from the rim in turn, embers rising and a low
 *       mist. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>BODY_FX3, impact / burst / signature: 'from' = centre, 'to' - 'from' = optional direction (zero = all round), power = scale,
 *       duration = life (28). A two-tick inhale (fibre rays collapsing in), then the bang: white flash and muzzle star over a fibre
 *       starburst, a heartbeat shock ring, its echo, the sigil ring turning against them and a web of glowing veins burned into the
 *       plane; steam jets blast out (a cone round the direction when there is one, all round otherwise) with tendon strands whipping
 *       out between them, embers, a blast streak along the direction, then billows of steam rise through the afterglow.</li>
 * </ul>
 */
public class BodyLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BODY_FX1, VfxShape.BODY_FX2, VfxShape.BODY_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BODY_FX1 -> 16;
            case BODY_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF8A4A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.BODY_FX3) VfxShake.add(inst.payload.from(), 0.28f * Math.min(2.5f, inst.power), 9);
        else if (inst.shape == VfxShape.BODY_FX2 && inst.power >= 4f) VfxShake.add(inst.payload.from(), 0.22f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BODY_FX1 -> cast(inst, ctx, buf);
            case BODY_FX2 -> zone(inst, ctx, buf);
            case BODY_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: Body Compression
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, P = Mth.clamp(inst.power, 0.5f, 4f);
        final long seed = inst.seed;
        final int col = glow(inst), hot = VfxVertexBuffer.whiten(col, 0.7f);
        Vector3f org = ctx.rel(inst.from(ctx)), tgt = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(tgt).sub(org);
        float len = dir.length();
        if (len < 0.05f) { dir.set(0f, 0f, 1f); len = 0.6f; tgt = new Vector3f(org).add(0f, 0f, 0.6f); }
        else dir.div(len);
        final Vector3f sd = side(dir), up = upOf(dir, sd);
        final float muzzleD = Math.min(0.8f, len * 0.4f);
        final Vector3f muzzle = new Vector3f(org).add(new Vector3f(dir).mul(muzzleD));
        final float run = Math.max(0.1f, len - muzzleD);
        final float launch = 0.14f * D, hit = 0.80f * D;
        final float fly = Mth.clamp((age - launch) / (hit - launch), 0f, 1f);
        final float charged = Mth.clamp(age / launch, 0f, 1f);
        final float flash = 1f - step(launch, launch + 0.24f * D, age);

        // steam left hanging along the path, each puff born when the round passes it
        int np = ctx.seg(Mth.clamp(Math.round(run / (1.7f * P + 0.5f)), 3, 8), 3);
        for (int k = 0; k < np; k++) {
            float s = (k + 0.5f) / np, born = launch + s * (hit - launch), x = (age - born) / (0.60f * D + 2f);
            if (x <= 0f || x >= 1f) continue;
            float h1 = hash(seed, k, 21), h2 = hash(seed, k, 22);
            Vector3f p = new Vector3f(muzzle).add(new Vector3f(dir).mul(run * s))
                    .add(new Vector3f(sd).mul((h1 - 0.5f) * 0.9f * P * (0.4f + x))).add(new Vector3f(up).mul((h2 - 0.5f) * 0.5f * P))
                    .add(0f, (0.15f + 0.9f * ease(x)) * P, 0f);
            float al = (float) Math.pow(1f - x, 1.4f) * Mth.clamp(x * 7f, 0f, 1f) * near(p), size = (0.7f + 1.5f * ease(x)) * P * (0.8f + 0.4f * h1);
            puff(buf, ctx, k & 3, VfxBlend.ALPHA, p, size, h2 * Mth.TWO_PI + x, alpha(STEAM_WHITE, 0.72f * al));
            if (x < 0.3f && buf.hasBudget(8)) puff(buf, ctx, (k + 1) & 3, VfxBlend.ADD, p, size * 0.9f, h1 * Mth.TWO_PI, alpha(col, 0.5f * (1f - x / 0.3f)));
        }

        // the barrel forms out of the arm: compression rings collapse into it, a muzzle ring of flesh with rifling, a fibre tube behind it
        float bf = (1f - step(0.30f * D, 0.62f * D, age)) * Mth.clamp(age / (0.06f * D), 0f, 1f);
        if (bf > 0.01f) {
            float open = VfxAnim.easeOutBack(Mth.clamp(age / (0.17f * D), 0f, 1f)), half = 0.48f * P * open, barrel = Math.max(0.5f, muzzleD + 0.15f) * open;
            VfxPose face = VfxPose.facing(muzzle, dir).spin(age * 0.45f);
            VfxPose rear = VfxPose.facing(new Vector3f(muzzle).sub(new Vector3f(dir).mul(barrel * 0.5f)), dir);
            tube(buf, FIBRE, VfxBlend.ALPHA, rear, half * 0.92f, barrel * 0.5f, 8, 2f, age * 0.01f, 1.2f, alpha(FLESH_DEEP, 0.9f * bf), alpha(FLESH_MID, 0.9f * bf));
            buf.plane(RIFLING, VfxBlend.ALPHA, face, half, alpha(FLESH_MID, 0.95f * bf));
            buf.plane(RIFLING, VfxBlend.ADD, face.lift(0.01f), half * 1.04f, alpha(col, 0.85f * bf));
            for (int k = 0; k < 3; k++) {
                float ck = Mth.clamp((age - k * 0.035f * D) / (0.20f * D), 0f, 1f);
                if (ck <= 0f || ck >= 1f) continue;
                buf.billboard(ctx, PULSE, VfxBlend.ADD, muzzle, (3.0f - 2.4f * VfxAnim.easeInCubic(ck)) * P, k * 1.9f + age * 0.05f,
                        alpha(k == 1 ? hot : col, 0.75f * Mth.sin(ck * Mth.PI)));
            }
        }
        VfxBloom.glow(ctx, buf, muzzle, (0.35f + 0.55f * charged) * P, col, 0.8f * (0.30f + 0.70f * charged) * Math.max(flash, bf));

        // the shot: muzzle star, a steam blast shooting forward and four puffs venting round the barrel
        if (age >= launch * 0.75f && flash > 0.01f) {
            float e = ease((age - launch * 0.75f) / (0.14f * D));
            buf.billboard(ctx, MUZZLE, VfxBlend.ADD, muzzle, (0.9f + 2.3f * e) * P, 0.25f, alpha(hot, 0.85f * flash));
            buf.billboard(ctx, MUZZLE, VfxBlend.ADD, muzzle, (0.5f + 1.5f * e) * P, 0.25f + Mth.PI / 8f, alpha(col, 0.7f * flash));
            Vector3f blastEnd = new Vector3f(muzzle).add(new Vector3f(dir).mul((0.8f + 2.8f * e) * P));
            int steam = alpha(STEAM_WHITE, 0.55f * flash * near(muzzle)), fire = alpha(col, 0.6f * flash * flash);
            strip(buf, JET, VfxBlend.ALPHA, muzzle, blastEnd, 1.5f * P, 1.5f * P, 1f, 0f, steam, steam);
            strip(buf, JET, VfxBlend.ADD, muzzle, blastEnd, 1.1f * P, 1.1f * P, 1f, 0f, fire, fire);
        }
        float vx = (age - launch) / (0.55f * D);
        if (vx > 0f && vx < 1f) {
            for (int k = 0; k < 4; k++) {
                float ang = hash(seed, k, 1) * Mth.TWO_PI, out = (0.25f + 1.1f * ease(vx)) * P;
                Vector3f p = new Vector3f(muzzle).add(new Vector3f(sd).mul(Mth.cos(ang) * out)).add(new Vector3f(up).mul(Mth.sin(ang) * out + 0.35f * P * vx))
                        .sub(new Vector3f(dir).mul(0.3f * P * vx));
                puff(buf, ctx, k, VfxBlend.ALPHA, p, (0.5f + 0.9f * ease(vx)) * P, ang, alpha(STEAM_WHITE, 0.7f * (1f - vx) * (1f - vx) * near(p)));
            }
        }

        // the round
        final float pa = Mth.clamp((hit + 0.07f * D - age) / (0.07f * D), 0f, 1f);
        if (fly > 0f && pa > 0.01f) {
            Vector3f hd = new Vector3f(muzzle).add(new Vector3f(dir).mul(run * fly));
            final float bl = 2.0f * P, wd = 0.95f * P;
            Vector3f tl = new Vector3f(hd).sub(new Vector3f(dir).mul(bl)), nose = new Vector3f(hd).add(new Vector3f(dir).mul(0.25f * P));
            int dark = alpha(FLESH_DEEP, 0.95f * pa), shell = alpha(col, pa), core = alpha(hot, 0.95f * pa);
            strip(buf, SLUG, VfxBlend.ALPHA, tl, nose, wd * 0.9f, wd * 0.9f, 1f, 0f, dark, dark);
            // the afterimage: a long fibre ribbon flowing back from the round, fading to nothing, and two ghosts of the round
            float tailLen = Math.min(run * fly + 0.3f, 5f + 3.5f * P);
            Vector3f rb = new Vector3f(hd).sub(new Vector3f(dir).mul(bl * 0.45f)), ra = new Vector3f(rb).sub(new Vector3f(dir).mul(tailLen));
            float vb = -age * 0.4f;
            strip(buf, FIBRE, VfxBlend.ADD, ra, rb, 0.10f * P, 0.55f * P, vb + tailLen / (1.2f * P), vb, alpha(col, 0f), alpha(col, 0.6f * pa));
            for (int k = 1; k <= 2; k++) {
                Vector3f gh = new Vector3f(hd).sub(new Vector3f(dir).mul(bl * (0.9f + 1.15f * k)));
                int gc = alpha(col, (0.42f - 0.15f * k) * pa);
                strip(buf, SLUG, VfxBlend.ADD, new Vector3f(gh).sub(new Vector3f(dir).mul(bl * 0.8f)), new Vector3f(gh).add(new Vector3f(dir).mul(0.15f * P)),
                        wd * (0.85f - 0.15f * k), wd * (0.85f - 0.15f * k), 1f, 0f, gc, gc);
            }
            // two fibre strands spinning round the round: the rifling
            int n = ctx.seg(8, 5);
            float spin = hash(seed, 0, 40) * Mth.TWO_PI;
            for (int k = 0; k < 2; k++) {
                Vector3f prev = null;
                for (int i = 0; i <= n; i++) {
                    float f = (float) i / n, behind = tailLen * 0.8f * (1f - f), rad = 0.30f * P * (0.25f + 0.75f * f);
                    float phi = spin + behind * 1.7f / P - age * 1.2f + k * Mth.PI;
                    Vector3f q = new Vector3f(hd).sub(new Vector3f(dir).mul(behind)).add(new Vector3f(sd).mul(Mth.cos(phi) * rad)).add(new Vector3f(up).mul(Mth.sin(phi) * rad));
                    if (prev != null)
                        strip(buf, FIBRE, VfxBlend.ADD, prev, q, 0.20f * P, 0.20f * P, 1f + f * 0.6f, 1f + (f + 1f / n) * 0.6f, alpha(col, (0.15f + 0.85f * (f - 1f / n)) * pa), alpha(col, (0.15f + 0.85f * f) * pa));
                    prev = q;
                }
            }
            // the glowing shell and its white-hot nose
            strip(buf, SLUG, VfxBlend.ADD, tl, nose, wd * 1.1f, wd * 1.1f, 1f, 0f, shell, shell);
            Vector3f hl = new Vector3f(hd).sub(new Vector3f(dir).mul(bl * 0.8f));
            strip(buf, SLUG, VfxBlend.ADD, hl, nose, wd * 0.55f, wd * 0.55f, 1f, 0f, core, core);
            // shock discs trailing behind it in a cone
            for (int k = 0; k < 4; k++) {
                Vector3f rp = new Vector3f(hd).sub(new Vector3f(dir).mul((0.8f + 1.35f * k) * P));
                float rr = (0.45f + 0.28f * k) * P;
                buf.plane(PULSE, VfxBlend.ADD, VfxPose.facing(rp, dir).spin(k * 1.3f + age * 0.1f), rr / 0.86f, alpha(k == 0 ? hot : col, (0.62f - 0.12f * k) * pa));
            }
            VfxBloom.glow(ctx, buf, hd, 0.75f * P, col, 0.95f * pa);
            buf.billboard(ctx, MUZZLE, VfxBlend.ADD, new Vector3f(hd).add(new Vector3f(dir).mul(0.2f * P)), 1.4f * P, age * 0.5f, alpha(hot, 0.85f * pa));
        }
        // embers shed along the flight
        int ne = ctx.seg(6, 3);
        for (int k = 0; k < ne && buf.hasBudget(4); k++) {
            float born = launch + hash(seed, k, 11) * (hit - launch) * 0.9f, x = (age - born) / (0.30f * D + 2f);
            if (x <= 0f || x >= 1f) continue;
            float along = run * (born - launch) / (hit - launch);
            Vector3f off = new Vector3f(sd).mul((hash(seed, k, 12) - 0.5f) * 2f * (0.35f + 1.2f * x) * P).add(new Vector3f(up).mul((hash(seed, k, 13) - 0.5f) * 2f * (0.35f + 1.0f * x) * P - 0.6f * x * x * P));
            Vector3f p = new Vector3f(muzzle).add(new Vector3f(dir).mul(along)).add(off);
            ember(buf, ctx, p, new Vector3f(off).add(new Vector3f(dir).mul(0.2f)), 0.22f * P * (1f - 0.5f * x), alpha(k % 3 == 0 ? hot : col, 1f - x));
        }
        // the small flash where it lands: star, shock disc facing the shooter, steam and sparks
        if (age >= hit - 0.4f) {
            float ia = Mth.clamp((age - hit) / Math.max(1f, D - hit), 0f, 1f), e = ease(ia * 2.2f), fa = (float) Math.pow(1f - ia, 1.4f);
            VfxBloom.glow(ctx, buf, tgt, (0.9f + 0.9f * e) * P, col, fa);
            buf.billboard(ctx, MUZZLE, VfxBlend.ADD, tgt, (0.8f + 2.0f * e) * P, 0.5f + ia, alpha(hot, fa));
            buf.plane(PULSE, VfxBlend.ADD, VfxPose.facing(tgt, dir).spin(age * 0.3f), (0.35f + 1.9f * e) * P / 0.86f, alpha(col, 0.9f * fa));
            for (int k = 0; k < 4 && buf.hasBudget(4); k++) {
                float ang = hash(seed, k, 31) * Mth.TWO_PI, out = (0.3f + 1.2f * e) * P;
                Vector3f p = new Vector3f(tgt).add(new Vector3f(sd).mul(Mth.cos(ang) * out)).add(new Vector3f(up).mul(Mth.sin(ang) * out)).sub(new Vector3f(dir).mul(0.2f * P));
                puff(buf, ctx, k, VfxBlend.ALPHA, p, (0.6f + 0.8f * e) * P, ang, alpha(STEAM_WHITE, 0.65f * (1f - ia) * near(p)));
            }
            for (int k = 0; k < 5 && buf.hasBudget(4); k++) {
                float ang = hash(seed, k, 32) * Mth.TWO_PI, spd = (1.4f + 1.6f * hash(seed, k, 33)) * P;
                Vector3f v = new Vector3f(sd).mul(Mth.cos(ang)).add(new Vector3f(up).mul(Mth.sin(ang))).sub(new Vector3f(dir).mul(0.5f)).normalize();
                Vector3f p = new Vector3f(tgt).add(new Vector3f(v).mul(spd * e)).add(0f, -0.8f * ia * ia * P, 0f);
                ember(buf, ctx, p, v, 0.22f * P * (1f - ia), alpha(hot, 1f - ia));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Titan field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.2f, inst.power);
        final float fade = life(inst, age, 8f, 12f);
        if (fade <= 0.01f) return;
        final float grow = ease(age / 10f), r = R * (0.3f + 0.7f * grow), beat = beat(age), ph = age % 24f;
        final long seed = inst.seed;
        final int col = glow(inst), hot = VfxVertexBuffer.whiten(col, 0.65f), vein = VfxVertexBuffer.lerpColor(VEIN, col | 0xFF000000, 0.45f) | 0xFF000000;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final VfxPose ground = VfxPose.ground(new Vector3f(c).add(0f, 0.03f, 0f));

        // the floor: dark muscle tissue, the vein web throbbing with the heart (two webs turning against each other), the sigil ring on top
        buf.plane(FLESH, VfxBlend.ALPHA, ground.spin(age * 0.006f), r / 0.94f, alpha(FLESH_MID, 0.78f * fade));
        float vk = 0.38f + 0.62f * beat;
        buf.plane(VEINS, VfxBlend.ADD, ground.lift(0.01f).spin(-age * 0.010f), r / 0.92f, alpha(vein, 0.85f * vk * fade));
        buf.plane(VEINS, VfxBlend.ADD, ground.lift(0.012f).spin(age * 0.018f + 1.3f), r * 0.6f / 0.92f, alpha(hot, 0.45f * vk * fade));
        buf.plane(RING, VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.012f), r / 0.965f, alpha(FLESH_DEEP, 0.6f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.022f).spin(age * 0.012f), r / 0.965f, alpha(col, 0.8f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.024f).spin(-age * 0.03f + 0.7f), r * 0.55f / 0.965f, alpha(hot, 0.4f * fade));
        // the heartbeat: on each beat a ring sweeps out to the rim
        for (int k = 0; k < 2; k++) {
            float x = (ph - 7f * k) / 14f;
            if (x <= 0f || x >= 1f) continue;
            buf.plane(PULSE, VfxBlend.ADD, ground.lift(0.03f), r * (0.10f + 0.90f * ease(x)) / 0.86f,
                    alpha(k == 0 ? hot : col, (float) Math.pow(1f - x, 1.3f) * (k == 0 ? 0.95f : 0.7f) * fade));
        }
        // the rim: twisted muscle ropes rising from a hot base and dissolving into steam
        final float H = Mth.clamp(0.45f * R + 0.9f, 1.5f, 4.2f) * grow;
        if (H > 0.1f) {
            int segs = ctx.seg(Mth.clamp(Math.round(R * 1.8f), 9, 16), 8);
            float reps = Math.max(2f, Math.round(Mth.TWO_PI * r / (2f * H)));
            float swell = 1f + 0.035f * beat;
            wall(buf, WALL, VfxBlend.ALPHA, c, r * swell, H * (1f + 0.08f * beat), segs, reps, age * 0.004f, alpha(0xFFD8946E, 0.92f * fade), alpha(STEAM_SHADE, 0f));
            wall(buf, WALL, VfxBlend.ADD, c, r * swell * 1.004f, H * (1f + 0.08f * beat), segs, reps, age * 0.004f, alpha(col, (0.40f + 0.45f * beat) * fade), alpha(col, 0f));
        }
        // steam vents along the rim, each blowing in its own turn
        int nj = ctx.seg(6, 4);
        for (int i = 0; i < nj; i++) {
            float ang = Mth.TWO_PI * (i + 0.6f * hash(seed, i, 1)) / nj + age * 0.004f;
            float cyc = (age + hash(seed, i, 2) * 24f) % 24f / 24f;
            float hh = (0.6f + 1.2f * ease(cyc)) * Math.max(1.2f, H), w = (0.7f + 0.5f * cyc) * Math.max(1.2f, H);
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * r * 0.97f, 0.05f, Mth.sin(ang) * r * 0.97f);
            Vector3f tip = new Vector3f(base).add(Mth.cos(ang) * 0.45f * hh, hh, Mth.sin(ang) * 0.45f * hh);
            int sc = alpha(STEAM_WHITE, (float) Math.pow(Mth.sin(cyc * Mth.PI), 0.9f) * 0.5f * fade * near(base));
            strip(buf, JET, VfxBlend.ALPHA, base, tip, w, w, 1f, 0f, sc, sc);
        }
        // a low mist drifting over the floor, embers rising off it
        int nf = ctx.seg(4, 2);
        for (int i = 0; i < nf; i++) {
            float ang = hash(seed, i, 6) * Mth.TWO_PI + age * (0.01f + 0.004f * i), rho = r * (0.25f + 0.5f * hash(seed, i, 7));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rho, 0.25f + 0.1f * Mth.sin(age * 0.07f + i), Mth.sin(ang) * rho);
            puff(buf, ctx, i & 3, VfxBlend.ALPHA, p, Math.min(r * 0.55f, 3.2f), ang, alpha(STEAM_WHITE, 0.2f * fade * near(p)));
        }
        int nm = ctx.seg(10, 5);
        for (int i = 0; i < nm; i++) {
            float lt = (age * 0.02f + hash(seed, i, 3)) % 1f, ang = hash(seed, i, 4) * Mth.TWO_PI, rho = r * 0.95f * Mth.sqrt(hash(seed, i, 5));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rho, 0.1f + lt * (1.2f + 0.25f * Math.min(R, 8f)), Mth.sin(ang) * rho);
            ember(buf, ctx, p, new Vector3f(0f, 1f, 0f), 0.18f + 0.025f * Math.min(R, 8f), alpha(i % 3 == 0 ? hot : col, Mth.sin(lt * Mth.PI) * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0f, 0.3f, 0f), Math.min(0.35f * R, 1.8f), col, (0.25f + 0.55f * beat) * fade);
    }

    // ------------------------------------------------------------------ FX3: Titan burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, P = Mth.clamp(inst.power, 0.4f, 4f);
        final float t = Mth.clamp(age / D, 0f, 1f);
        final long seed = inst.seed;
        final int col = glow(inst), hot = VfxVertexBuffer.whiten(col, 0.7f), vein = VfxVertexBuffer.lerpColor(VEIN, col | 0xFF000000, 0.45f) | 0xFF000000;
        final Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        final boolean dirOn = hint.lengthSquared() > 0.12f;
        final Vector3f dir = dirOn ? hint.normalize() : new Vector3f(0f, 1f, 0f);
        final Vector3f sd = side(dir), up = upOf(dir, sd);
        final float boom = 2.4f, bt = age - boom;
        final VfxPose plane = dirOn ? VfxPose.facing(c, dir) : VfxPose.ground(c);

        // the inhale: fibre rays collapse in; then the bang
        if (age < boom + 0.5f) {
            float k = Mth.clamp(age / boom, 0f, 1f);
            buf.billboard(ctx, BURST, VfxBlend.ADD, c, (3.4f - 2.5f * ease(k)) * P, age * 0.2f, alpha(col, 0.95f * k));
            VfxBloom.glow(ctx, buf, c, (0.3f + 0.5f * k) * P, hot, 0.8f * k);
        }
        if (bt >= 0f) {
            float fl = 1f - step(0f, 9f, bt), e = ease(bt / 5f);
            VfxBloom.glow(ctx, buf, c, (1.0f + 0.9f * e) * P, col, Math.max(0.9f * fl, 0.45f * (1f - t) * (1f - t)));
            buf.billboard(ctx, MUZZLE, VfxBlend.ADD, c, 3.3f * P * VfxAnim.easeOutBack(Mth.clamp(bt / 4f, 0f, 1f)), 0.2f + age * 0.02f, alpha(hot, 0.9f * (float) Math.pow(fl, 1.2f)));
            buf.billboard(ctx, BURST, VfxBlend.ADD, c, 4.8f * P * ease(bt / 7f), -age * 0.03f, alpha(col, 0.8f * fl));
        }
        // shock rings: the heart-thump, its echo, the sigil turning against them; the veins burned into the plane
        float x1 = bt / 13f, x2 = (bt - 5f) / 14f, x3 = (bt - 1f) / 20f, x4 = (bt - 0.5f) / 16f;
        if (x1 > 0f && x1 < 1f) buf.plane(PULSE, VfxBlend.ADD, plane, (0.3f + 3.8f * ease(x1)) * P / 0.86f, alpha(hot, (float) Math.pow(1f - x1, 1.1f)));
        if (x2 > 0f && x2 < 1f) buf.plane(PULSE, VfxBlend.ADD, plane.lift(0.01f), (0.2f + 3.0f * ease(x2)) * P / 0.86f, alpha(col, 0.75f * (float) Math.pow(1f - x2, 1.1f)));
        if (x3 > 0f && x3 < 1f) buf.plane(RING, VfxBlend.ADD, plane.lift(0.02f).spin(-age * 0.07f), (0.2f + 2.7f * ease(x3)) * P / 0.965f, alpha(col, 0.85f * (1f - x3) * (1f - x3)));
        if (x4 > 0f) {
            float ex = (0.15f + 2.9f * ease(x4)) * P, sa = (1f - step(0.40f, 1f, t)) * Mth.clamp(bt / 2f, 0f, 1f);
            buf.plane(FLESH, VfxBlend.ALPHA, plane.lift(-0.01f).spin(age * 0.01f), ex * 0.95f / 0.94f, alpha(FLESH_MID, 0.5f * sa));
            buf.plane(VEINS, VfxBlend.ADD, plane.lift(0.015f).spin(age * 0.02f), ex / 0.92f, alpha(vein, 0.9f * sa));
        }
        // the muscle band: a ring of twisted fibre swelling out of the centre
        float xb = (bt - 0.5f) / 15f;
        if (xb > 0f && xb < 1f) {
            float rb = (0.25f + 3.3f * ease(xb)) * P, hb = (0.28f + 0.2f * (1f - xb)) * P, bal = (float) Math.pow(1f - xb, 1.2f);
            int bs = ctx.seg(10, 7);
            float reps = Mth.clamp(Math.round(0.8f * rb / hb), 2, 24);
            tube(buf, BAND, VfxBlend.ALPHA, plane, rb, hb, bs, reps, 0f, 1f, alpha(FLESH_MID, 0.9f * bal), alpha(FLESH_MID, 0.9f * bal));
            tube(buf, BAND, VfxBlend.ADD, plane, rb * 1.01f, hb, bs, reps, 0f, 1f, alpha(col, 0.75f * bal), alpha(col, 0.75f * bal));
        }
        // steam jets blasting out (a cone round the hint, or all round), tendon strands whipping out between them
        int nj = ctx.seg(10, 6);
        for (int j = 0; j < nj; j++) {
            float h1 = hash(seed, j, 31), h2 = hash(seed, j, 32), h3 = hash(seed, j, 33), h4 = hash(seed, j, 34);
            Vector3f v;
            if (dirOn && j % 4 != 3) v = new Vector3f(dir).add(new Vector3f(sd).mul((h1 - 0.5f) * 1.0f)).add(new Vector3f(up).mul((h2 - 0.5f) * 1.0f)).normalize();
            else {
                float az = Mth.TWO_PI * (j + 0.4f * h1) / nj, el = j % 5 == 0 ? 0.9f + 0.4f * h2 : -0.05f + 0.6f * h2;
                v = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            }
            float x = (bt - 0.4f * h3) / (0.62f * D);
            if (x <= 0f || x >= 1f) continue;
            float e = ease(x * 1.15f), reach = (0.6f + 3.8f * (0.65f + 0.5f * h4) * e) * P, jl = (1.4f + 1.6f * h3) * P * (0.4f + 0.6f * e);
            Vector3f jb = new Vector3f(c).add(new Vector3f(v).mul(reach)), ja = new Vector3f(c).add(new Vector3f(v).mul(Math.max(0.2f * P, reach - jl)));
            float w = (1.1f + 0.9f * h1) * P * (0.55f + 0.45f * e), al = (float) Math.pow(1f - x, 1.2f);
            int sc = alpha(STEAM_WHITE, 0.85f * al * near(jb));
            strip(buf, JET, VfxBlend.ALPHA, ja, jb, w, w, 1f, 0f, sc, sc);
            if (x < 0.5f) {
                int fc = alpha(col, 0.8f * (1f - x / 0.5f));
                strip(buf, JET, VfxBlend.ADD, ja, jb, w * 0.8f, w * 0.8f, 1f, 0f, fc, fc);
            }
        }
        int ns = ctx.seg(8, 5);
        for (int k = 0; k < ns; k++) {
            float h1 = hash(seed, k, 41), h2 = hash(seed, k, 42), h3 = hash(seed, k, 43);
            Vector3f v;
            if (dirOn && k % 3 != 2) v = new Vector3f(dir).add(new Vector3f(sd).mul((h1 - 0.5f) * 1.4f)).add(new Vector3f(up).mul((h2 - 0.5f) * 1.4f)).normalize();
            else {
                float az = Mth.TWO_PI * (k + 0.5f * h1) / ns + 0.4f, el = 0.05f + 0.55f * h2;
                v = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            }
            float x = (bt - 0.2f * h3) / 15f;
            if (x <= 0f || x >= 1f) continue;
            float e = ease(x * 1.1f), reach = (0.7f + 3.6f * e * (0.7f + 0.4f * h1)) * P, sl = (2.2f + 1.2f * h2) * P * (1f - 0.35f * x);
            Vector3f sb = new Vector3f(c).add(new Vector3f(v).mul(reach)), st = new Vector3f(c).add(new Vector3f(v).mul(Math.max(0.1f, reach - sl)));
            float w = (h3 > 0.5f ? 0.5f : -0.5f) * P, al = (float) Math.pow(1f - x, 1.1f);
            strip(buf, STRAND, VfxBlend.ADD, sb, st, w * 1.5f, w * 1.5f, 1f, 0f, alpha(col, al), alpha(col, al));
            strip(buf, STRAND, VfxBlend.ADD, sb, st, w, w, 1f, 0f, alpha(hot, 0.8f * al), alpha(hot, 0.8f * al));
        }
        // a blast streak along the direction
        if (dirOn && bt > 0f) {
            float e = ease(bt / 6f), al = (float) Math.pow(1f - Mth.clamp(bt / 12f, 0f, 1f), 1.4f);
            Vector3f far = new Vector3f(c).add(new Vector3f(dir).mul((0.8f + 3.8f * e) * P));
            int sc = alpha(col, 0.8f * al), hc = alpha(hot, 0.7f * al);
            strip(buf, SLUG, VfxBlend.ADD, c, far, 1.3f * P, 1.3f * P, 1f, 0f, sc, sc);
            strip(buf, SLUG, VfxBlend.ADD, c, far, 0.6f * P, 0.6f * P, 1f, 0f, hc, hc);
        }
        // embers thrown out, then billows of steam rising through the afterglow
        int ne = ctx.seg(12, 6);
        for (int k = 0; k < ne && buf.hasBudget(4); k++) {
            float h1 = hash(seed, k, 61), h2 = hash(seed, k, 62), h3 = hash(seed, k, 63), h4 = hash(seed, k, 64);
            float x = (bt - 0.3f * h1) / (0.7f * D);
            if (x <= 0f || x >= 1f) continue;
            float az = h2 * Mth.TWO_PI, el = -0.2f + 1.0f * h3, spd = (2.6f + 3.0f * h4) * P;
            Vector3f v = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el) + 0.35f, Mth.sin(az) * Mth.cos(el)).normalize();
            Vector3f p = new Vector3f(c).add(new Vector3f(v).mul(spd * ease(x) * 0.9f)).add(0f, -1.6f * x * x * P, 0f);
            Vector3f vel = new Vector3f(v).mul(spd * (1f - x)).add(0f, -3.2f * x * P, 0f);
            ember(buf, ctx, p, vel, (0.16f + 0.1f * h4) * (0.7f + 0.3f * P) * (1f - 0.5f * x), alpha(k % 3 == 0 ? hot : col, 1f - x));
        }
        int nb = ctx.seg(5, 3);
        for (int k = 0; k < nb && buf.hasBudget(4); k++) {
            float born = 3f + 2.5f * hash(seed, k, 51), x = (age - born) / Math.max(4f, D - born);
            if (x <= 0f || x >= 1f) continue;
            float az = hash(seed, k, 52) * Mth.TWO_PI, rr = (0.3f + 1.2f * hash(seed, k, 53)) * P, rise = ease(x);
            Vector3f p = new Vector3f(c).add(Mth.cos(az) * rr, (0.2f + 1.5f * rise) * P, Mth.sin(az) * rr);
            if (dirOn) p.add(new Vector3f(dir).mul(0.6f * P * rise));
            float al = step(0f, 0.15f, x) * (1f - step(0.4f, 1f, x));
            puff(buf, ctx, k & 3, VfxBlend.ALPHA, p, (1.4f + 1.5f * rise) * P, az + x, alpha(STEAM_WHITE, 0.6f * al * near(p)));
        }
    }
}
