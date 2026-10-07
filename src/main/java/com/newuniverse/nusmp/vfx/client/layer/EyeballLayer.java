package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.EyeballFx.*;

/**
 * Eyeball Magic (floating, fleshy eyes that roam, track and share sight), drawn from tools/gen_eyeball_textures.py.
 * Palette: cream sclera with red capillaries, a fibrous iris painted by the effect colour (default blood red 'FF3A3A'), a black
 * predator slit, wet glints, maroon flesh ('5A1A2A'). The colour is mixed 40 % into the magic's own red, so a white tint still reads as
 * Eyeball Magic. Every eye is a stack of layers: the iris slides over the ball to where the gaze meets it and is foreshortened there,
 * so the pupils really rove.
 * <ul>
 *   <li>EYEBALL_FX1 = CAST / PROJECTILE, the Seeking Eye. 'from' = the caster's hand / eye, 'to' = the target, power = size scale
 *       (0.6 .. 3), duration = flight ticks (default 16). First fifth: a fleshy eye tears open in the air at 'from' (lids spread, the
 *       pupil snaps from wide to a slit, imploding rings and a growing glow gather). Then the eyeball pulls free and flies to 'to' on a
 *       lazy corkscrew, rolling, with two optic-nerve tendrils snaking behind it, ghost eyes fading in its wake, crackling vein bolts,
 *       droplets shed along the way and a hairline sight line with travelling beads joining it to a lock-on reticle that closes on
 *       the target. The last quarter: the eye pops into a flash, an iris ring and flying vein fragments at 'to'.</li>
 *   <li>EYEBALL_FX2 = ZONE / FIELD, the Evil Eye field. 'from' = centre on the ground, power = RADIUS (the sigil, web, wall and ping all
 *       reach it), duration = life ticks (default 80), fades in over 8 and out over the last 12. A veined membrane lies on the ground,
 *       the gaze sigil turns on it and a radar sweep runs against it, iris rings ping outward twice a second; a low wall of veined flesh
 *       rises round the rim; six to eleven floating eyes open one after another round the edge, their pupils roving from spot to spot
 *       and snapping to slits when a ping passes them, nerves swaying under them; blood motes drift up. When it ends the eyes close.</li>
 *   <li>EYEBALL_FX3 = IMPACT / SIGNATURE, the Great Eye. 'from' = centre, 'to' = optional direction hint (the eye looks along it and a
 *       gaze lance and the rings follow it), power = scale, duration = life ticks (default 28). A flash; a huge eye pops open, the pupil
 *       slams from wide to a slit and beats twice; a web of veins flares behind it, iris rings and one distortion ring sweep out, five
 *       nerve whips lash outward and fall back, little eyes stare back at the centre as they are flung away with nerves trailing,
 *       tears and vein fragments fly and fall; then the eye closes into a red afterglow with motes drifting up.</li>
 * </ul>
 * Each shape stays under 400 vertices (checked over every tick and several powers by tools/vfx_preview).
 */
public class EyeballLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.EYEBALL_FX1, VfxShape.EYEBALL_FX2, VfxShape.EYEBALL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case EYEBALL_FX1 -> 16;
            case EYEBALL_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF3A3A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.EYEBALL_FX3) VfxShake.add(inst.payload.from(), 0.35f * Math.min(2f, inst.power), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case EYEBALL_FX1 -> cast(inst, ctx, buf);
            case EYEBALL_FX2 -> field(inst, ctx, buf);
            case EYEBALL_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: the Seeking Eye
    /** A point {@code dist} blocks along the flight from the start (negative = straight behind it): the line plus a lazy corkscrew. */
    private static Vector3f at(Vector3f start, Vector3f dir, Vector3f u, Vector3f v, float len, float dist, float p, float ph) {
        float q = len > 1e-3f ? Mth.clamp(dist / len, 0f, 1f) : 0f;
        float r = 0.30f * p * Mth.sin(q * Mth.PI) * Mth.clamp(len / 3f, 0f, 1f), ang = q * Mth.TWO_PI * 1.4f + ph;
        return new Vector3f(start).add(new Vector3f(dir).mul(dist)).add(new Vector3f(u).mul(Mth.cos(ang) * r)).add(new Vector3f(v).mul(Mth.sin(ang) * r));
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Math.max(0.4f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len > 1e-3f) dir.div(len); else dir.set(0, 0, 1);
        Vector3f u = side(dir), v = new Vector3f(dir).cross(u).normalize();
        int tint = tint(inst), glow = mix(tint, HOT, 0.35f);
        float ph = hash(inst.seed, 0, 1) * Mth.TWO_PI;
        float tl = 0.20f * D, ta = 0.74f * D;                                       // launch and arrival
        float f = Mth.clamp((age - tl) / (ta - tl), 0f, 1f), s = 0.4f * f + 0.6f * f * f;
        float charge = easeOut(age / tl), ti = Mth.clamp((age - ta) / Math.max(1f, D - ta), 0f, 1f);
        boolean flying = age >= tl;
        float travelled = s * len, dFly = 0.60f * p;
        Vector3f head = at(from, dir, u, v, len, travelled, p, ph);
        float vanish = flying ? 1f - Mth.clamp((age - ta) / (0.09f * D + 0.6f), 0f, 1f) : 1f;
        float roll = travelled * 1.3f / p;
        float scr = (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight));  // where the flight points on the screen

        // -- ALPHA, first: shed droplets and the fragments thrown out at the landing
        if (flying) {
            for (int i = 0; i < 6; i++) {
                float born = tl + hash(inst.seed, i, 2) * (ta - tl) * 0.85f, tt = (age - born) / (0.30f * D + 2f);
                if (tt <= 0f || tt >= 1f) continue;
                float qb = Mth.clamp((born - tl) / (ta - tl), 0f, 1f), sb = 0.4f * qb + 0.6f * qb * qb;
                Vector3f q = at(from, dir, u, v, len, sb * len, p, ph).add(new Vector3f(spread(inst.seed, i, 3)).mul(0.9f * p * easeOut(tt))).add(0, -1.1f * p * tt * tt, 0);
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.22f * p * (1f - 0.4f * tt), 0f, col(0xFFE0243A, 1f - tt * tt));
            }
        }
        if (age >= ta) {
            for (int i = 0; i < 6; i++) {
                Vector3f q = new Vector3f(to).add(new Vector3f(spread(inst.seed, i, 8)).mul(1.5f * p * easeOut(ti))).add(0, -0.6f * p * ti * ti, 0);
                buf.billboard(ctx, FRAG, VfxBlend.ALPHA, q, 0.32f * p, i * 1.3f + ti * 5f, col(WHITE, 1f - ti * ti));
            }
        }
        // -- the optic-nerve tendrils snaking behind the eye
        if (flying && vanish > 0.02f) {
            for (int k = 0; k < 2; k++) {
                Vector3f[] pts = new Vector3f[7];
                for (int i = 0; i < 7; i++) {
                    Vector3f q = at(from, dir, u, v, len, travelled - (0.12f + 0.40f * i) * p, p, ph);
                    float w = 0.28f * p * i / 6f * Mth.sin(age * 0.45f + i * 0.9f + k * 2.4f) * (k == 0 ? 1f : -1f);
                    pts[i] = q.add(new Vector3f(u).mul(w)).add(new Vector3f(v).mul(0.35f * w));
                }
                ribbon(buf, NERVE, VfxBlend.ALPHA, pts, 0.26f * p, 0.04f * p, col(WHITE, vanish), col(WHITE, vanish * 0.8f), 0f, 1f);
            }
        }
        // -- the eyes: a fleshy lid-eye tears open at the hand, then the free eyeball flies
        float lidA = 1f - Mth.clamp((age - tl) / (0.16f * D + 0.5f), 0f, 1f);
        if (lidA > 0.02f) {
            float open = age < tl ? VfxAnim.easeOutBack(Mth.clamp(age / (tl * 0.70f), 0f, 1f)) : Mth.lerp(1f - lidA, 1f, 0.35f);
            Vector3f gz = new Vector3f(dir).add(new Vector3f(u).mul(0.30f * Mth.sin(age * 0.8f + ph))).add(new Vector3f(v).mul(0.25f * Mth.cos(age * 0.6f)));
            eye(buf, ctx, from, 0.88f * p, gz, 0f, Mth.lerp(charge, 0.95f, 0.22f), true, open, tint, lidA, true, charge * charge);
        }
        if (flying && vanish > 0.02f) {
            eye(buf, ctx, head, dFly * (1f + 0.5f * (1f - vanish)), new Vector3f(to).sub(head), roll, 0.30f, true, 1f, tint, vanish, false, 0.25f + 0.5f * f);
        }
        // -- ADD: imploding rings while it gathers, ghost eyes, crackling vein bolts, the sight line, the reticle and the landing
        if (age < tl + 1f) {
            for (int k = 0; k < 2; k++) {
                float q = Mth.clamp(charge * 1.3f - 0.3f * k, 0f, 1f);
                buf.billboard(ctx, RIPPLE, VfxBlend.ADD, from, p * (2.8f - 2.0f * q), age * 0.25f * (k == 0 ? 1f : -1f), col(glow, 0.75f * Mth.sin(q * Mth.PI) * lidA));
            }
        }
        if (flying && vanish > 0.02f) {
            for (int i = 1; i <= 4; i++) {
                float dist = travelled - 0.80f * p * i;
                if (dist < 0f) continue;
                buf.billboard(ctx, MINI, VfxBlend.ADD, at(from, dir, u, v, len, dist, p, ph), dFly * (1f - 0.13f * i), scr, col(tint, 0.5f * (1f - i / 5f) * vanish));
            }
            int tick = (int) (age * 0.5f);
            for (int k = 0; k < 2; k++) {
                Vector3f[] pts = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    Vector3f q = at(from, dir, u, v, len, travelled - (0.05f + 0.55f * i) * p, p, ph);
                    if (i > 0) {
                        q.add(new Vector3f(u).mul((hash(inst.seed, tick * 7 + i, 20 + k) - 0.5f) * 0.9f * p));
                        q.add(new Vector3f(v).mul((hash(inst.seed, tick * 7 + i, 30 + k) - 0.5f) * 0.9f * p));
                    }
                    pts[i] = q;
                }
                ribbon(buf, BEAM, VfxBlend.ADD, pts, 0.16f * p, 0.05f * p, col(glow, 0.9f * vanish), col(glow, 0.1f), 0f, 1f);
            }
        }
        if (age > tl * 0.35f) {
            float k = Mth.clamp((age - tl * 0.35f) / (tl * 0.65f), 0f, 1f) * (1f - ti);
            strip(buf, BEAM, VfxBlend.ADD, flying ? head : from, to, 0.10f * p, 0.05f * p, 2.2f, -age * 0.16f, col(glow, 0.55f * k), col(glow, 0.40f * k));
        }
        if (age > tl * 0.3f) {
            float k = sstep(tl * 0.3f, tl * 1.1f, age);
            float w = p * (1.0f + 1.8f * sq(1f - f) + 2.2f * ti);
            buf.billboard(ctx, RETICLE, VfxBlend.ADD, to, w, -age * 0.22f, col(glow, 0.85f * k * (1f - ti)));
        }
        if (age >= ta - 1f) {
            float k = Mth.clamp((age - (ta - 1f)) / (D - ta + 1f), 0f, 1f);
            VfxBloom.glow(ctx, buf, to, 1.5f * p * (0.5f + k), glow, (1f - k) * (1f - k) * 1.2f);
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, to, 2f * p * (0.35f + 1.6f * easeOut(k)), age * 0.2f, col(tint, 0.9f * (1f - k)));
        }
    }

    // ------------------------------------------------------------------ FX2: the Evil Eye field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power);
        float fade = life(inst, age, 8f, 12f);
        if (fade <= 0.01f) return;
        Vector3f g = ctx.rel(inst.from(ctx));
        int tint = tint(inst), glow = mix(tint, HOT, 0.30f);
        float open = easeOut(age / 12f), rr = R * open, beat = thump(age, 26f);
        float H = Mth.clamp(R * 0.30f, 1.5f, 3.4f) * easeOut(age / 18f);
        VfxPose floor = VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0));

        // -- ALPHA: the veined membrane on the ground, the wall of flesh round the rim, blood motes drifting up
        buf.plane(WEB, VfxBlend.ALPHA, floor.spin(0.4f + age * 0.003f), rr * 1.04f, col(DARK_FLESH, 0.66f * fade));
        if (H > 0.05f) {
            float rep = Math.max(2f, Math.round(Mth.TWO_PI * rr / (H * 4f)));
            hoop(buf, BAND, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g).add(0, H * 0.5f, 0)), rr * 0.985f, H * 0.5f, ctx.seg(12, 8), rep, age * 0.0015f,
                    col(mix(tint, WET_RED, 0.3f), 0.80f * fade));
        }
        for (int i = 0; i < 8; i++) {
            float life = (age * 0.022f + hash(inst.seed, i, 41)) % 1f;
            float ang = hash(inst.seed, i, 42) * Mth.TWO_PI, rho = R * Mth.sqrt(hash(inst.seed, i, 43)) * 0.95f * open;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rho + 0.3f * Mth.sin(age * 0.1f + i), 0.2f + life * (H + 1.8f), Mth.sin(ang) * rho);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.20f + 0.10f * hash(inst.seed, i, 44), 0f, col(WET_RED, Mth.sin(life * Mth.PI) * fade));
        }

        // -- the floating eyes round the rim
        int n = Mth.clamp(Math.round(R * 1.15f), 6, 11);
        float eyeD = Mth.clamp(0.52f + 0.07f * R, 0.62f, 1.25f);
        for (int i = 0; i < n; i++) {
            float h1 = hash(inst.seed, i, 1), h2 = hash(inst.seed, i, 2), h3 = hash(inst.seed, i, 3);
            float ang = Mth.TWO_PI * (i + 0.35f * (h1 - 0.5f)) / n + age * 0.0075f, rho = R * (0.60f + 0.34f * h2);
            float y = 1.0f + (0.6f + 1.8f * h3) * Math.min(1f, R / 5f) + 0.16f * Mth.sin(age * 0.11f + i * 2.3f);
            Vector3f pos = new Vector3f(g).add(Mth.cos(ang) * rho * open, y, Mth.sin(ang) * rho * open);
            float ow = VfxAnim.easeOutBack(Mth.clamp((age - 2f - i * 1.6f) / 9f, 0f, 1f)) * sstep(0f, 1f, Mth.clamp((inst.duration - age) / 10f, 0f, 1f));
            if (ow <= 0.03f) continue;
            float d = eyeD * (0.8f + 0.4f * h3);
            float react = 0f;                                                       // a ping passing the eye
            for (int k = 0; k < 2; k++) {
                float pp = ((age + k * 15f) % 30f) / 30f, ringR = R * (0.08f + 0.92f * easeOut(pp));
                react += (float) Math.exp(-sq((ringR - rho) / (0.14f * R))) * (1f - pp);
            }
            react = Mth.clamp(react, 0f, 1f);
            Vector3f gz = glance(inst.seed, i, age, g, R).sub(pos);
            float dil = 0.52f - 0.34f * react + 0.08f * beat;
            tendril(buf, ctx, new Vector3f(pos).add(0, -0.35f * d, 0), new Vector3f(0, -1, 0), (1.1f * d + 0.6f * h2) * Math.min(1f, ow), 0.30f * d, 3, 0.22f, i * 1.9f, age, col(WHITE, fade * Mth.clamp(ow, 0f, 1f)));
            if (i < 7) eye(buf, ctx, pos, d * Math.max(0.1f, ow), gz, 0f, dil, h1 > 0.3f, Mth.clamp(ow, 0f, 1f), tint, fade, ow < 0.97f, react * 0.9f);
            else buf.billboard(ctx, MINI, VfxBlend.ALPHA, pos, d * Math.max(0.1f, ow), (float) Math.atan2(gz.dot(ctx.camUp), gz.dot(ctx.camRight)), col(WHITE, fade));
        }

        // -- ADD: the ground glow, the sigil turning, the sweep turning against it, the pings, a glow at the heart
        buf.plane(WEB, VfxBlend.ADD, floor.lift(0.01f).spin(-age * 0.004f + 0.9f), rr, col(tint, (0.30f + 0.40f * beat) * fade));
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.012f), rr, col(glow, 0.80f * fade));
        buf.plane(SCAN, VfxBlend.ADD, floor.lift(0.03f).spin(-age * 0.06f), rr * 0.94f, col(mix(glow, WHITE, 0.2f), 0.85f * fade));
        for (int k = 0; k < 2; k++) {
            float pp = ((age + k * 15f) % 30f) / 30f;
            buf.plane(RIPPLE, VfxBlend.ADD, floor.lift(0.04f + 0.002f * k).spin(age * 0.02f * (k == 0 ? 1f : -1f)), R * (0.08f + 0.92f * easeOut(pp)) * open, col(tint, (1f - pp) * 0.85f * fade * sstep(0f, 0.1f, pp)));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.3f, 0), 0.9f + 0.1f * R, glow, (0.25f + 0.25f * beat) * fade);
    }

    // ------------------------------------------------------------------ FX3: the Great Eye
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Math.max(0.4f, inst.power);
        float k = age * 28f / D;                                                    // time in default ticks
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        boolean directed = hint.lengthSquared() > 0.0025f;
        Vector3f dir = directed ? hint.normalize() : null;
        int tint = tint(inst), glow = mix(tint, HOT, 0.35f);
        float fadeOut = 1f - sstep(22f, 28f, k);

        float pop = VfxAnim.easeOutBack(Mth.clamp((k - 0.4f) / 6f, 0f, 1f)), closing = 1f - easeIn((k - 20.5f) / 6.5f);
        float open = pop * closing;
        float dil = Mth.lerp(easeOut((k - 0.5f) / 8.5f), 0.95f, 0.22f) + 0.16f * (float) (Math.exp(-sq((k - 13f) / 1.1f)) + Math.exp(-sq((k - 19f) / 1.1f)));
        float dHero = 2.9f * p * (0.55f + 0.45f * Mth.clamp(pop, 0f, 1.1f));
        float flare = Mth.clamp(1f - sstep(0.5f, 6f, k) * 0.7f, 0f, 1f) * closing;

        // -- ALPHA: the web of veins behind the eye, tears and fragments flung out, nerve whips, little eyes staring back
        float web = easeOut(k / 9f);
        if (web > 0.02f) {
            float wa = 1f - sstep(14f, 28f, k);
            buf.billboard(ctx, WEB, VfxBlend.ALPHA, c, 7.4f * p * web, k * 0.02f, col(DARK_FLESH, 0.75f * wa));
        }
        for (int i = 0; i < 7; i++) {                                               // tears
            float tt = Mth.clamp((k - 1.0f) / (13f + 6f * hash(inst.seed, i, 5)), 0f, 1f);
            if (tt <= 0f || tt >= 1f) continue;
            Vector3f d = spread(inst.seed, i, 6);
            if (directed) d.add(new Vector3f(dir).mul(1.1f)).normalize();
            float sp = (2.2f + 2.8f * hash(inst.seed, i, 7)) * p;
            Vector3f q = new Vector3f(c).add(d.mul(sp * easeOut(tt))).add(0, -1.4f * p * tt * tt, 0);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.26f * p * (1f - 0.3f * tt), 0f, col(0xFFE0243A, 1f - tt * tt));
        }
        for (int i = 0; i < 5; i++) {                                               // vein fragments
            float tt = Mth.clamp((k - 1.5f) / (12f + 6f * hash(inst.seed, i, 9)), 0f, 1f);
            if (tt <= 0f || tt >= 1f) continue;
            Vector3f d = spread(inst.seed, i, 10);
            if (directed) d.add(new Vector3f(dir).mul(1.1f)).normalize();
            Vector3f q = new Vector3f(c).add(d.mul((2.4f + 2.2f * hash(inst.seed, i, 11)) * p * easeOut(tt))).add(0, -1.0f * p * tt * tt, 0);
            buf.billboard(ctx, FRAG, VfxBlend.ALPHA, q, 0.40f * p, hash(inst.seed, i, 12) * 6f + tt * 6f * (hash(inst.seed, i, 13) - 0.5f), col(WHITE, 1f - tt * tt));
        }
        float whip = easeOut((k - 1f) / 5f) * (1f - sstep(9f, 19f, k));
        if (whip > 0.02f) {                                                         // five nerve whips lashing out and falling back
            for (int i = 0; i < 5; i++) {
                float ang = Mth.TWO_PI * (i + 0.3f * hash(inst.seed, i, 14)) / 5f + 0.6f;
                Vector3f rad = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang)));
                if (directed) rad.add(new Vector3f(dir).mul(0.6f)).normalize();
                float L = (2.4f + 2.0f * hash(inst.seed, i, 15)) * p * whip;
                tendril(buf, ctx, new Vector3f(c).add(new Vector3f(rad).mul(0.55f * p)), rad, L, 0.34f * p, 4, 0.35f, i * 1.3f, age, col(WHITE, Math.min(1f, whip * 1.6f)));
            }
        }
        for (int i = 0; i < 5; i++) {                                               // flung eyes with nerves trailing, staring back at the centre
            float tt = Mth.clamp((k - 1.0f) / (15f + 6f * hash(inst.seed, i, 16)), 0f, 1f);
            if (tt <= 0f || tt >= 1f) continue;
            Vector3f d = spread(inst.seed, i, 17);
            if (directed) d.add(new Vector3f(dir).mul(1.1f)).normalize();
            Vector3f q = new Vector3f(c).add(new Vector3f(d).mul((2.0f + 2.4f * hash(inst.seed, i, 18)) * p * easeOut(tt))).add(0, -0.9f * p * tt * tt, 0);
            float sz = 0.40f * p * (0.8f + 0.5f * hash(inst.seed, i, 19)), al = 1f - sstep(0.7f, 1f, tt);
            tendril(buf, ctx, q, new Vector3f(d).negate(), 0.9f * p * (1f - 0.4f * tt), 0.15f * p, 2, 0.1f, i, age, col(WHITE, al));
            Vector3f back = new Vector3f(c).sub(q);
            buf.billboard(ctx, MINI, VfxBlend.ALPHA, q, sz, (float) Math.atan2(back.dot(ctx.camUp), back.dot(ctx.camRight)), col(WHITE, al));
        }
        // -- the great eye itself
        eye(buf, ctx, c, dHero, dir, 0f, dil, true, open, tint, fadeOut, true, flare);

        // -- ADD: flash, web glow, iris rings, the gaze lance, afterglow
        if (k < 5f) VfxBloom.glow(ctx, buf, c, 5.5f * p, glow, (1f - k / 5f) * 1.1f);
        if (web > 0.02f) buf.billboard(ctx, WEB, VfxBlend.ADD, c, 7.0f * p * web, -k * 0.03f, col(tint, 0.65f * (1f - sstep(8f, 22f, k))));
        for (int r = 0; r < 2; r++) {
            float q = Mth.clamp((k - 2.5f * r) / (13f + 2f * r), 0f, 1f);
            if (q <= 0f || q >= 1f) continue;
            ring(buf, ctx, RIPPLE, VfxBlend.ADD, c, dir, 2f * (0.9f + (4.6f + 2.6f * r) * easeOut(q)) * p, k * 0.12f * (r == 0 ? 1f : -1f), col(r == 0 ? glow : tint, 0.9f * sq(1f - q)));
        }
        float wq = Mth.clamp(k / 10f, 0f, 1f);
        if (wq > 0f && wq < 1f) buf.billboard(ctx, WARP, VfxBlend.NEGATIVE, c, 2f * (1f + 5.0f * easeOut(wq)) * p, k * 0.05f, grey(0.55f * (1f - wq)));
        if (directed && k > 0.5f) {
            float ext = easeOut((k - 0.5f) / 3f) * 7f * p;
            strip(buf, BEAM, VfxBlend.ADD, c, new Vector3f(c).add(new Vector3f(dir).mul(ext)), 0.45f * p, 0.15f * p, 2.5f, -k * 0.4f, col(glow, 0.9f * fadeOut), col(glow, 0.1f * fadeOut));
        }
        if (k > 6f) VfxBloom.glow(ctx, buf, c, 1.8f * p, glow, 0.55f * (1f - sstep(8f, 28f, k)));
        for (int i = 0; i < 6; i++) {                                               // motes drifting up out of the afterglow
            float tt = (k - 10f - 2f * i) / 16f;
            if (tt <= 0f || tt >= 1f) continue;
            float ang = hash(inst.seed, i, 21) * Mth.TWO_PI, rho = (0.5f + 1.6f * hash(inst.seed, i, 22)) * p;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rho, 0.3f * p + tt * 2.0f * p, Mth.sin(ang) * rho);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, q, 0.28f * p * (1f - 0.5f * tt), 0f, col(tint, Mth.sin(tt * Mth.PI) * 0.8f));
        }
    }
}
