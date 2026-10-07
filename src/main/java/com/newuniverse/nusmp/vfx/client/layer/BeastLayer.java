package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BeastFx.*;

/**
 * Beast Magic VFX, built to the owner's anime still: a translucent, cel-shaded orange beast-fire (hot yellow core, orange body,
 * darker red-orange edge, thin dark linework) that streams off a clawed arm in ragged licks and reads as an animal spirit, not as fire.
 * Textures: tools/gen_beast_textures.py; shared sprites and quad builders: {@link BeastFx}.
 * <ul>
 *   <li><b>BEAST_FX1 = cast / projectile (Beast Rush).</b> from = hand, to = target, power = size (0.6..3), duration = flight ticks.
 *       First quarter: a roar star and a glow gather at the hand while torn licks are drawn into it. Then a spirit beast head
 *       made of flame (facing the way it runs, mane streaming back) charges from -> to with two crossed flame streams behind it,
 *       three fading afterimages, a dark soot haze and shed licks. At the target: a flash, a three-gash claw mark, a shock ring and flung fangs.</li>
 *   <li><b>BEAST_FX2 = zone (Beast Territory).</b> from = centre on the ground, power = radius, duration = life. A turning fang / claw-spiral
 *       sigil (a second one counter-rotating inside it) on scorched ground, two rows of flame tongues round the rim that flicker out of
 *       phase, three spirit beasts circling the rim in opposite directions with flame streams, paw prints stepping round inside,
 *       embers rising and a pulse ring every second. Fades in over 8 ticks, out over the last 12.</li>
 *   <li><b>BEAST_FX3 = impact / signature (Roar).</b> from = centre, to = optional direction hint, power = scale, duration = life. A white-hot
 *       flash and roar star, a claw X slashed through the point (along the hint), double shock rings, flame tongues leaping up, fangs and
 *       embers flung out, a spirit beast head roaring out of the burst along the hint, then a scorch paw mark and a lingering haze.</li>
 * </ul>
 * colour: the tint is mixed 30 % into the orange (white gives a pale fire), never replaces it.
 */
public class BeastLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BEAST_FX1, VfxShape.BEAST_FX2, VfxShape.BEAST_FX3); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.BEAST_FX2 ? 80 : s == VfxShape.BEAST_FX3 ? 28 : 16; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFB060; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.BEAST_FX1) cast(inst, ctx, buf);
        else if (inst.shape == VfxShape.BEAST_FX2) zone(inst, ctx, buf);
        else impact(inst, ctx, buf);
    }

    private static void bloom(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, int g, float k) {
        if (k <= 0.01f) return;
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, size, 0, a(hot(g), 0.8f * k));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, size * 2.2f, 0, a(g, 0.35f * k));
    }

    /** Screen-space angle that turns an image whose "up" is the sprite's long axis onto the projected direction d. */
    private static float screenAngle(VfxRenderContext ctx, Vector3f d) {
        return (float) Math.atan2(-d.dot(ctx.camRight), d.dot(ctx.camUp));
    }

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(4, inst.duration), P = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) { dir.set(ctx.camRight); len = 0.01f; } else dir.div(len);
        int g = glow(inst.color), fl = flame(inst.color);
        float charge = D * 0.25f, land = D * 0.70f;
        float cT = Mth.clamp(age / charge, 0f, 1f), cFade = 1f - smooth(charge, charge + D * 0.15f, age);
        float run = Mth.clamp((age - D * 0.2f) / (land - D * 0.2f), 0f, 1f), fly = run * run * (3f - 2f * run);
        Vector3f head = new Vector3f(a).lerp(b, fly);

        // 1. charge at the hand: roar star turning in, glow swelling, licks drawn into the palm
        if (cFade > 0.01f) {
            float swell = 0.4f + 0.6f * ease(cT);
            bloom(ctx, buf, a, 0.9f * P * swell, g, cFade);
            buf.billboard(ctx, BURST, VfxBlend.ADD, a, 1.7f * P * (1.15f - 0.5f * cT), age * 0.35f, a(g, 0.85f * cFade));
            buf.billboard(ctx, BURST, VfxBlend.ADD, a, 1.0f * P * (1.15f - 0.5f * cT), -age * 0.5f, a(hot(g), 0.8f * cFade));
            for (int k = 0; k < 4; k++) {
                float ang = hash(inst.seed, k, 1) * Mth.TWO_PI, rr = (1.6f - 1.3f * cT) * P;
                Vector3f p = new Vector3f(a).add(Mth.cos(ang) * rr, Mth.sin(ang) * rr * 0.8f, (hash(inst.seed, k, 2) - 0.5f) * rr);
                cell(buf, ctx, LICKS, VfxBlend.ADD, p, 0.6f * P * (1f - 0.4f * cT), ang + Mth.HALF_PI + 0.2f, k % LICK_CELLS, LICK_CELLS, a(fl, cFade));
            }
        }
        // 2. the charging beast spirit: soot haze, afterimages, two crossed streams, the head
        if (age >= D * 0.2f && age < D) {
            float vis = 1f;
            float tail = Math.min(len * fly + 0.01f, 4.8f * P);
            Vector3f back = new Vector3f(head).sub(new Vector3f(dir).mul(tail));
            Vector3f up2 = new Vector3f(dir).cross(ElementFx.side(dir)).normalize();
            float streamK = 1f - smooth(land, D, age);
            buf.billboard(ctx, HAZE, VfxBlend.ALPHA, new Vector3f(head).sub(new Vector3f(dir).mul(tail * 0.45f)), 2.2f * P, age * 0.1f, a(SOOT, 0.30f * streamK));
            strip(buf, STREAM, VfxBlend.ADD, head, back, 1.25f * P, 0.2f * P, a(fl, 0.95f * streamK), a(fl, 0.0f));
            stripSide(buf, STREAM, VfxBlend.ADD, head, back, up2, 1.0f * P, 0.15f * P, a(fl, 0.8f * streamK), a(fl, 0.0f));
            for (int k = 3; k >= 1; k--) {
                Vector3f gp = new Vector3f(head).sub(new Vector3f(dir).mul(Math.min(tail, 0.75f * P * k)));
                along(buf, ctx, SPIRIT, VfxBlend.ADD, gp, dir, 0.85f * P * (1f - 0.1f * k), 0.85f * P * (1f - 0.1f * k), a(fl, 0.36f / k * streamK));
            }
            float shiver = 1f + 0.05f * Mth.sin(age * 2.7f);
            if (streamK > 0.01f) {
                bloom(ctx, buf, head, 1.3f * P, g, streamK * 0.8f);
                along(buf, ctx, SPIRIT, VfxBlend.ADD, head, dir, 1.0f * P * shiver, 1.0f * P * shiver, a(fl, vis * streamK));
            }
            for (int k = 0; k < 6; k++) {                                       // licks shed off the path
                float u = hash(inst.seed, k, 3) * fly, born = u * (land - D * 0.2f) + D * 0.2f, tt = (age - born) / (D * 0.5f);
                if (tt < 0f || tt > 1f) continue;
                Vector3f p = new Vector3f(a).lerp(b, u).add((hash(inst.seed, k, 4) - 0.5f) * 0.8f * P, (0.2f + tt * 0.9f) * P * (0.6f + hash(inst.seed, k, 5)), (hash(inst.seed, k, 6) - 0.5f) * 0.8f * P);
                cell(buf, ctx, LICKS, VfxBlend.ADD, p, 0.55f * P * (1f - 0.5f * tt), (hash(inst.seed, k, 7) - 0.5f) * 0.8f, (k + 1) % LICK_CELLS, LICK_CELLS, a(fl, 1f - tt));
            }
        }
        // 3. landing at the target: flash, claw gashes, shock ring, flung fangs
        if (age >= land) {
            float s = Mth.clamp((age - land) / Math.max(1f, D - land), 0f, 1f), k = 1f - s;
            bloom(ctx, buf, b, 1.6f * P * (0.6f + 0.6f * s), g, k);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, b, 2.6f * P * (0.5f + ease(s)), 0.3f, a(hot(g), k));
            buf.billboard(ctx, CLAW, VfxBlend.ADD, b, 2.0f * P * (0.7f + 0.4f * ease(s)), screenAngle(ctx, new Vector3f(0.3f, 1f, 0f)) , a(g, 0.95f * k));
            buf.billboard(ctx, RING, VfxBlend.ADD, b, 3.0f * P * ease(s) + 0.3f, 0f, a(g, 0.8f * k));
            for (int q = 0; q < 4; q++) {
                float ang = hash(inst.seed, q, 8) * Mth.TWO_PI;
                Vector3f p = new Vector3f(b).add(Mth.cos(ang) * 1.4f * P * ease(s), Mth.sin(ang) * 1.4f * P * ease(s) - 0.3f * s * s, 0f);
                cell(buf, ctx, FANGS, VfxBlend.ADD, p, 0.5f * P, ang, q, FANG_CELLS, a(hot(g), k));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        int g = glow(inst.color), fl = flame(inst.color);
        float fade = life(inst, age, 8, 12), open = ease(age / 8f), r = R * open;
        if (fade <= 0.01f) return;
        float pul = VfxAnim.pulse(age, 1.4f);

        // ground: scorched earth, the sigil and its counter-rotating inner twin, the rim ring
        flat(buf, HAZE, VfxBlend.ALPHA, c, 0.03f, age * 0.01f, r * 1.05f, a(SOOT, 0.45f * fade));
        flat(buf, SIGIL, VfxBlend.ADD, c, 0.05f, age * 0.012f, r, a(g, (0.55f + 0.25f * pul) * fade));
        flat(buf, SIGIL, VfxBlend.ADD, c, 0.06f, -age * 0.02f + 1f, r * 0.62f, a(red(g), 0.5f * fade));
        flat(buf, RING, VfxBlend.ADD, c, 0.07f, 0f, r * (0.97f + 0.03f * pul), a(hot(g), 0.65f * fade));
        float ph = (age % 20f) / 20f;                                           // a pulse ring sweeping out every second
        flat(buf, RING, VfxBlend.ADD, c, 0.08f, age * 0.03f, r * (0.15f + 0.85f * ph), a(g, (1f - ph) * 0.5f * fade));
        bloom(ctx, buf, new Vector3f(c).add(0, 0.2f, 0), 1.2f * Math.min(R, 3f), g, 0.45f * fade * (0.8f + 0.2f * pul));

        // the wall of flame tongues round the rim: two rows, out of phase
        int n = Mth.clamp(Math.round(R * 2.4f), 8, 14);
        float h = 1.5f + 0.18f * Math.min(R, 8f);
        for (int row = 0; row < 2; row++) {
            for (int k = 0; k < n; k++) {
                float ang = (k + row * 0.5f) / n * Mth.TWO_PI + age * 0.004f * (row == 0 ? 1 : -1);
                float rr = r * (row == 0 ? 1f : 0.9f);
                float flick = 0.75f + 0.25f * Mth.sin(age * 0.45f + k * 1.9f + row * 2f) + 0.1f * hash(inst.seed, k, row);
                Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr, 0.02f, Mth.sin(ang) * rr);
                stand(buf, FLAME, VfxBlend.ADD, p, 0.55f * (R > 4f ? 1.2f : 1f), h * flick * (row == 0 ? 1f : 0.7f), 0.2f * Mth.sin(age * 0.2f + k), a(fl, (row == 0 ? 0.95f : 0.6f) * fade * open));
            }
        }
        // spirit beasts circling the rim in opposite directions, each with a flame stream behind it
        for (int k = 0; k < 3; k++) {
            float dirn = k == 1 ? -1f : 1f, ang = k * Mth.TWO_PI / 3f + dirn * age * 0.075f / Math.max(1f, R * 0.35f);
            float rr = r * (k == 1 ? 0.72f : 0.95f);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr, 0.85f + 0.12f * Mth.sin(age * 0.3f + k), Mth.sin(ang) * rr);
            Vector3f tan = new Vector3f(-Mth.sin(ang) * dirn, 0f, Mth.cos(ang) * dirn);
            Vector3f back = new Vector3f(p).sub(new Vector3f(tan).mul(2.0f));
            strip(buf, STREAM, VfxBlend.ADD, p, back, 0.9f, 0.15f, a(fl, 0.85f * fade), a(fl, 0f));
            along(buf, ctx, SPIRIT, VfxBlend.ADD, p, tan, 0.8f, 0.8f, a(fl, 0.95f * fade));
        }
        // paw prints stepping round inside the sigil
        for (int k = 0; k < 6; k++) {
            float step = ((age * 0.05f) + k / 6f) % 1f, ang = (k + 0.5f * (k % 2)) / 6f * Mth.TWO_PI + age * 0.01f;
            float rr = r * (0.35f + 0.28f * (k % 2));
            float pa = Mth.sin(step * Mth.PI);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr, 0.09f, Mth.sin(ang) * rr);
            flat(buf, PAW, VfxBlend.ADD, p, 0f, -ang + Mth.HALF_PI, 0.35f, a(g, pa * 0.8f * fade));
        }
        // embers rising
        for (int k = 0; k < 10; k++) {
            float u = (age * 0.018f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI, rr = r * (0.2f + 0.8f * hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr + 0.3f * Mth.sin(age * 0.2f + k), 0.2f + u * (1.5f + h), Mth.sin(ang) * rr);
            cell(buf, ctx, LICKS, VfxBlend.ADD, p, 0.4f * (1f - 0.5f * u), 0.15f * Mth.sin(age * 0.3f + k), k % LICK_CELLS, LICK_CELLS, a(fl, Mth.sin(u * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(8, inst.duration), t = Mth.clamp(age / D, 0f, 1f), P = Math.max(0.5f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        int g = glow(inst.color), fl = flame(inst.color);
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        if (hint.lengthSquared() < 0.04f) hint.set(ctx.camRight); else hint.normalize();
        float flash = 1f - smooth(0f, 0.28f, t), grow = ease(t * 2.2f), tail = 1f - smooth(0.55f, 1f, t);

        // ground: scorch paw mark and shock rings (the afterglow outlives the flash)
        flat(buf, HAZE, VfxBlend.ALPHA, c, 0.03f, 0.5f, 2.4f * P * grow, a(SOOT, 0.4f * tail));
        flat(buf, PAW, VfxBlend.ADD, c, 0.05f, hint.x > 0 ? 0.2f : -0.2f, 0.9f * P, a(g, smooth(0.1f, 0.25f, t) * tail * 0.75f));
        flat(buf, RING, VfxBlend.ADD, c, 0.06f, age * 0.05f, 4.2f * P * ease(t * 1.5f), a(g, (1f - Mth.clamp(t * 1.5f, 0f, 1f)) * 0.85f));
        float t2 = Mth.clamp(t * 1.5f - 0.2f, 0f, 1f);
        flat(buf, RING, VfxBlend.ADD, c, 0.07f, -age * 0.05f, 2.8f * P * ease(t2), a(hot(g), (1f - t2) * 0.7f * (t2 > 0f ? 1f : 0f)));

        // flame tongues leaping up, wide at the base
        for (int k = 0; k < 6; k++) {
            float ang = k / 6f * Mth.TWO_PI + 0.4f, lag = 0.04f * k, tk = Mth.clamp((t - lag) / 0.5f, 0f, 1f);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * 0.7f * P * (0.4f + tk), 0f, Mth.sin(ang) * 0.7f * P * (0.4f + tk));
            stand(buf, FLAME, VfxBlend.ADD, p, 0.6f * P, (0.4f + 1.9f * ease(tk)) * P * (0.8f + 0.4f * hash(inst.seed, k, 1)), 0.25f * P, a(fl, Mth.sin(Mth.clamp(tk, 0f, 1f) * Mth.PI) * 0.4f + (1f - tk) * 0.6f));
        }
        // the beast head roaring out of the burst along the hint
        float sp = smooth(0.04f, 0.24f, t) * (1f - smooth(0.55f, 0.95f, t));
        if (sp > 0.01f) {
            Vector3f hp = new Vector3f(c).add(0f, 0.7f * P, 0f).add(new Vector3f(hint).mul(1.2f * P * ease(t * 2f)));
            Vector3f hd = new Vector3f(hint).sub(new Vector3f(ctx.camRight).mul(0f));
            along(buf, ctx, SPIRIT, VfxBlend.ADD, hp, hd, 1.9f * P * (0.8f + 0.3f * ease(t * 3f)), 1.9f * P * (0.8f + 0.3f * ease(t * 3f)), a(fl, sp * 0.8f));
        }
        // the flash: white-hot core, roar star, glint and the claw X
        Vector3f mid = new Vector3f(c).add(0f, 0.6f * P, 0f);
        bloom(ctx, buf, mid, 1.8f * P * (0.5f + grow), g, 0.8f * flash + 0.2f * tail);
        buf.billboard(ctx, BURST, VfxBlend.ADD, mid, 5.2f * P * (0.35f + 0.65f * grow), age * 0.06f, a(g, 0.9f * (1f - smooth(0.1f, 0.7f, t))));
        buf.billboard(ctx, BURST, VfxBlend.ADD, mid, 3.0f * P * (0.35f + 0.65f * grow), -age * 0.1f, a(hot(g), 0.5f * flash));
        buf.billboard(ctx, GLINT, VfxBlend.ADD, mid, 6f * P * grow, 0.2f, a(hot(g), 0.5f * flash));
        float ca = screenAngle(ctx, hint) + 0.5f, cs = smooth(0.02f, 0.14f, t), cf = 1f - smooth(0.35f, 0.85f, t);
        buf.billboard(ctx, CLAW, VfxBlend.ADD, mid, 4.2f * P * (0.8f + 0.25f * grow), ca, a(g, 0.75f * cs * cf));
        float cs2 = smooth(0.08f, 0.2f, t);
        buf.billboard(ctx, CLAW, VfxBlend.ADD, mid, 3.6f * P * (0.8f + 0.25f * grow), ca - 1.15f, a(red(g), 0.7f * cs2 * cf));
        // fangs and licks flung out
        for (int k = 0; k < 8; k++) {
            float ang = k / 8f * Mth.TWO_PI + hash(inst.seed, k, 2) * 0.5f, sp2 = 2.2f + 1.6f * hash(inst.seed, k, 3), tk = Mth.clamp(t * 1.25f, 0f, 1f);
            float dx = Mth.cos(ang), dy = Mth.sin(ang);
            Vector3f p = new Vector3f(mid).add(new Vector3f(ctx.camRight).mul(dx * sp2 * P * ease(tk))).add(new Vector3f(ctx.camUp).mul(dy * sp2 * P * ease(tk) - 0.8f * tk * tk * P));
            cell(buf, ctx, FANGS, VfxBlend.ADD, p, 0.55f * P, ang - Mth.HALF_PI + age * 0.1f * (k % 2 == 0 ? 1 : -1), k % FANG_CELLS, FANG_CELLS, a(hot(g), 1f - tk));
        }
        for (int k = 0; k < 10; k++) {
            float ang = hash(inst.seed, k, 4) * Mth.TWO_PI, sp2 = 1.2f + 2.4f * hash(inst.seed, k, 5), tk = Mth.clamp((t - 0.05f) * 1.1f, 0f, 1f);
            Vector3f p = new Vector3f(mid).add(Mth.cos(ang) * sp2 * P * ease(tk), 0.3f * P + 1.4f * P * tk * hash(inst.seed, k, 6), Mth.sin(ang) * sp2 * P * ease(tk));
            cell(buf, ctx, LICKS, VfxBlend.ADD, p, 0.5f * P * (1f - 0.5f * tk), 0.3f * Mth.sin(age * 0.3f + k), k % LICK_CELLS, LICK_CELLS, a(fl, 1f - tk));
        }
        buf.billboard(ctx, HAZE, VfxBlend.ADD, new Vector3f(mid).add(0f, 0.6f * P * t, 0f), 4.5f * P, 0f, a(red(g), 0.3f * tail * smooth(0.1f, 0.4f, t)));
    }
}
