package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.CurseFx.*;

/**
 * Curse Magic (Hex Magic, the curses of Megicula; the Agrippa family's art), drawn as black, jagged, glowing matter: thorned black
 * marks and angular runes that burn deep violet, ash, corruption veins, soul wisps. Textures from tools/gen_curse_textures.py; each
 * one is drawn twice, ALPHA in near-black (the cursed mass) and ADD in violet (only strokes, cracks and rims light up), so every
 * piece is a black body with light in it. {@code inst.color} is mixed into the violet palette, never replaces it: a white tint
 * still reads as Curse Magic.
 * <ul>
 *   <li>CURSE_FX1, the Hex Bolt (CAST / PROJECTILE): 'from' = the caster's hand, 'to' = the target point, power = size (1 = a
 *       needle about a third of a block thick, 0.6 to 3 and up), duration = flight ticks (16). A jagged sigil opens in front of the
 *       hand and ash motes gather into a swelling flash (first quarter); then a barbed black needle with a jagged violet core and a
 *       hot white head, writhing as it flies, races to the target with a ghost echo behind it, five runes cork-screwing round it
 *       and a trail of ash smoke and embers. At 'to' it brands the target: a starburst flash, the curse brand stamped in the air,
 *       a jagged shock ring and splinters thrown back.</li>
 *   <li>CURSE_FX2, the Hex Field (ZONE / FIELD / DOME): 'from' = the centre on the ground, power = the RADIUS in blocks,
 *       duration = life ticks (80). Corruption spreads over the ground in black blight with glowing branching veins; over it turn two
 *       counter-rotating rune sigils with a heartbeat brand at the centre; jagged shock rings pulse out every second; round the rim
 *       a wall of black thorns with glowing cracks and a row of runes leans inward, black thorns rise out of the earth along it,
 *       and violet embers, drifting soul wisps and ash rise inside. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>CURSE_FX3, the Curse Brand (IMPACT / BURST / SIGNATURE): 'from' = the centre, 'to' = an optional direction hint
 *       (to - from; a spray of splinters, tendrils and a streak of light along it), power = scale, duration = life ticks (28). Ash
 *       and light are sucked in (anticipation), then a black sun detonates behind a jagged white-violet starburst with an inverted
 *       flash, jagged shock rings run out, obsidian splinters and thorned tendrils fly out, runes unfurl on a ring, and the
 *       five-bladed curse brand burns in the air as the afterglow while ash falls and wisps rise.</li>
 * </ul>
 */
public class CurseLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CURSE_FX1, VfxShape.CURSE_FX2, VfxShape.CURSE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CURSE_FX1 -> 16;
            case CURSE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9A2AFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.CURSE_FX3) VfxShake.add(inst.payload.from(), Math.min(1.6f, 0.35f * inst.power), 9);
        else if (inst.shape == VfxShape.CURSE_FX2 && inst.power >= 3f) VfxShake.add(inst.payload.from(), 0.4f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CURSE_FX1 -> bolt(inst, ctx, buf);
            case CURSE_FX2 -> field(inst, ctx, buf);
            case CURSE_FX3 -> brand(inst, ctx, buf);
            default -> { }
        }
    }

    private static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

    /** The palette of one instance: the violet mixed with the spell's tint. */
    private static final class Pal {
        final int glow, hi, deep, hot;
        Pal(VfxInstance inst) {
            glow = mix(VIOLET, inst.color, 0.35f);
            hi = mix(ORCHID, inst.color, 0.30f);
            deep = mix(DEEP, inst.color, 0.30f);
            hot = mix(HOT, inst.color, 0.15f);
        }
    }

    // ================================================================== CURSE_FX1: the Hex Bolt
    private void bolt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Mth.clamp(inst.power, 0.5f, 4f);
        final Pal pal = new Pal(inst);
        final Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        final Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.05f) { dir.set(ctx.camRight); len = 0.05f; } else dir.div(len);
        final Vector3f sd = side(dir), up2 = new Vector3f(sd).cross(dir).normalize();

        final float t0 = 0.16f * D, t1 = 0.80f * D;                              // launch and arrival (ticks)
        final float fade = life(inst, age, 0, 0.18f * D);
        float u = clamp01((age - t0) / (t1 - t0));
        float s = (float) Math.pow(u, 1.35);                                    // the head's place on the path
        final Vector3f head = new Vector3f(dir).mul(len * s).add(a);
        final float launched = sstep(t0, t0 + 0.06f * D, age), arrive = 1f - sstep(t1, t1 + 0.14f * D, age);
        final float bodyA = launched * arrive * fade;
        final float bodyLen = Math.min(len * s, 3.4f * p);

        // ---- the charge: a sigil opens in front of the hand, ash motes gather, a jagged flash swells
        float chg = clamp01(age / (0.25f * D));
        float sigA = sstep(0f, 0.06f * D, age) * (1f - sstep(0.32f * D, 0.58f * D, age));
        Vector3f mc = new Vector3f(a).add(new Vector3f(dir).mul(0.30f * p));
        if (sigA > 0.01f) {
            float grow = VfxAnim.easeOutBack(clamp01(age / (0.15f * D))) * (1f - 0.25f * sstep(0.30f * D, 0.58f * D, age));
            float r = 0.95f * p * grow;
            VfxPose face = VfxPose.facing(mc, dir).spin(age * 0.24f);
            inkGlow(buf, SIGIL, face, r, al(INK, 0.85f * sigA), al(pal.hi, 0.95f * sigA));
            VfxPose back = VfxPose.facing(new Vector3f(mc).add(new Vector3f(dir).mul(0.22f * p)), dir).spin(-age * 0.4f + 1f);
            buf.plane(SIGIL, VfxBlend.ADD, back, r * 0.6f, al(pal.hot, 0.6f * sigA));
            VfxPose spin = VfxPose.facing(new Vector3f(mc).add(new Vector3f(dir).mul(0.08f * p)), dir).spin(-age * 0.3f);
            inkGlow(buf, SPIRAL, spin, r * 0.62f, al(INDIGO, 0.9f * sigA), al(FLECK, 0.75f * sigA));
            buf.billboard(ctx, FLASH, VfxBlend.ADD, mc, p * (0.25f + 0.55f * chg * chg), age * 0.3f, al(pal.hot, 0.8f * sigA * chg));
            VfxBloom.glow(ctx, buf, mc, 0.45f * p * (0.4f + 0.6f * chg), pal.glow, sigA);
            int nm = ctx.seg(5, 3);
            float moteA = (float) Math.sin(Mth.PI * clamp01(age / (0.3f * D)));
            for (int i = 0; i < nm; i++) {
                float rr = (1.2f + 0.8f * hash(inst.seed, i, 2)) * p * (1f - chg) * (1f - chg) + 0.05f;
                Vector3f q = new Vector3f(mc).add(unit(inst.seed, i, 1).mul(rr));
                buf.billboard(ctx, FLAKES, VfxBlend.ADD, q, 0.32f * p, hash(inst.seed, i, 3) * Mth.TWO_PI + age * 0.3f, al(pal.hi, 0.9f * moteA));
            }
        }

        // ---- the afterimage: the needle's echo, a few ticks late and fatter
        if (bodyA > 0.01f) {
            float u2 = clamp01((age - 3.2f - t0) / (t1 - t0));
            float s2 = (float) Math.pow(u2, 1.35);
            float l2 = Math.min(len * s2, 3.4f * p) * 1.1f;
            if (l2 > 0.1f) {
                Vector3f h2 = new Vector3f(dir).mul(len * s2).add(a);
                Vector3f[] pts = new Vector3f[3];
                float[] wd = {1.5f * p, 1.5f * p, 1.5f * p};
                int[] cg = new int[3];
                for (int k = 0; k < 3; k++) {
                    float f = k / 2f;
                    pts[k] = new Vector3f(h2).sub(new Vector3f(dir).mul(l2 * (1 - f))).add(new Vector3f(sd).mul(0.12f * p * Mth.sin(age * 0.6f + k)));
                    cg[k] = al(pal.deep, bodyA * 0.34f * (0.2f + 0.8f * f));
                }
                ribbon(buf, BOLT, VfxBlend.ADD, pts, wd, cg);
            }
        }

        // ---- the needle: a black barbed shaft with a jagged violet core and a white-hot head, writhing as it flies
        if (bodyA > 0.01f && bodyLen > 0.08f) {
            int n = 5;
            Vector3f[] pts = new Vector3f[n];
            float[] wdI = new float[n], wdG = new float[n];
            int[] ci = new int[n], cg = new int[n];
            for (int k = 0; k < n; k++) {
                float f = k / (float) (n - 1);                                   // 0 = tail, 1 = head
                float env = Mth.sin(Mth.PI * f) * 0.9f + 0.1f * (1 - f);
                pts[k] = new Vector3f(head).sub(new Vector3f(dir).mul(bodyLen * (1 - f)))
                        .add(new Vector3f(sd).mul(Mth.sin(age * 0.85f + k * 1.7f) * 0.10f * p * env))
                        .add(new Vector3f(up2).mul(Mth.cos(age * 0.7f + k * 2.3f) * 0.08f * p * env));
                wdI[k] = 1.0f * p;
                wdG[k] = 0.96f * p;
                ci[k] = al(INK, 0.97f * bodyA * (0.45f + 0.55f * f));
                cg[k] = al(VfxVertexBuffer.lerpColor(pal.deep, pal.hot, f * f), bodyA * (0.30f + 0.70f * f));
            }
            ribbon(buf, BOLT, VfxBlend.ALPHA, pts, wdI, ci);
            ribbon(buf, BOLT, VfxBlend.ADD, pts, wdG, cg);
            float hf = launched * arrive * fade;
            buf.billboard(ctx, FLASH, VfxBlend.ADD, head, 0.95f * p, age * 0.45f, al(pal.hot, 0.95f * hf));
            buf.billboard(ctx, FLASH, VfxBlend.ADD, head, 0.65f * p, -age * 0.6f, al(pal.hi, 0.8f * hf));
            VfxBloom.glow(ctx, buf, head, 0.5f * p, pal.glow, hf);

            // five runes cork-screwing round the shaft, flickering through the alphabet
            for (int k = 0; k < 5; k++) {
                float f = (k + 0.5f) / 5f;
                float phi = age * 0.55f + k * 1.26f, rad = (0.30f + 0.28f * f) * p;
                Vector3f q = new Vector3f(head).sub(new Vector3f(dir).mul(bodyLen * (1 - f)))
                        .add(new Vector3f(sd).mul(Mth.cos(phi) * rad)).add(new Vector3f(up2).mul(Mth.sin(phi) * rad));
                cell(buf, ctx, GLYPHS, 4, (k * 5 + (int) (age * 0.2f)) & 15, VfxBlend.ADD, q, 0.42f * p, age * 0.1f + k, al(pal.hi, bodyA * (0.45f + 0.55f * f)));
            }

            // a halo of runes spinning round the head, tilted and precessing
            Vector3f axis = new Vector3f(dir).mul(0.75f)
                    .add(new Vector3f(sd).mul(Mth.cos(age * 0.2f) * 0.5f)).add(new Vector3f(up2).mul(Mth.sin(age * 0.2f) * 0.5f));
            VfxPose haloPose = VfxPose.facing(new Vector3f(head).sub(new Vector3f(dir).mul(0.35f * p)), axis);
            hoop(buf, BAND, VfxBlend.ALPHA, haloPose, 0.58f * p, 0.58f * p, 0.115f * p, ctx.seg(8, 6), 2f, age * 0.05f, al(INK, 0.9f * hf));
            hoop(buf, BAND, VfxBlend.ADD, haloPose, 0.58f * p, 0.58f * p, 0.11f * p, ctx.seg(8, 6), 2f, age * 0.05f, al(pal.hi, 0.9f * hf));

            // two black tendrils streaming behind the head, writhing
            for (int t = 0; t < 2; t++) {
                Vector3f[] tp = new Vector3f[3];
                float[] tw = new float[3];
                int[] tc = new int[3], tg = new int[3];
                float sgn = t == 0 ? 1f : -1f;
                for (int m = 0; m < 3; m++) {
                    float f = m / 2f;                                            // 0 = anchored on the shaft, 1 = the free tip
                    tp[m] = new Vector3f(head).sub(new Vector3f(dir).mul((0.5f + 1.7f * f) * p))
                            .add(new Vector3f(sd).mul(sgn * (0.12f + 0.45f * f) * p + Mth.sin(age * 0.7f + t * 2.1f + m * 1.3f) * 0.18f * p * f))
                            .add(new Vector3f(up2).mul(Mth.cos(age * 0.6f + t * 1.7f + m) * 0.2f * p * f));
                    tw[m] = 0.62f * p;
                    tc[m] = al(INK, 0.95f * bodyA * (1f - 0.5f * f));
                    tg[m] = al(VfxVertexBuffer.lerpColor(pal.deep, pal.hi, 1f - f), bodyA * 0.9f * (1f - 0.6f * f));
                }
                ribbon(buf, TENDRIL, VfxBlend.ALPHA, tp, tw, tc);
                ribbon(buf, TENDRIL, VfxBlend.ADD, tp, tw, tg);
            }
        }

        // ---- ash smoke left along the path, and embers drifting off it
        int np = ctx.seg(6, 3);
        for (int i = 0; i < np; i++) {
            float f = (i + 0.35f + 0.3f * hash(inst.seed, i, 4)) / np;
            float born = t0 + (t1 - t0) * (float) Math.pow(f, 1.0 / 1.35);
            float k = (age - born) / (0.55f * D);
            if (k < 0f || k > 1f) continue;
            Vector3f q = new Vector3f(dir).mul(len * f).add(a)
                    .add(new Vector3f(sd).mul((hash(inst.seed, i, 5) - 0.5f) * 0.4f * p)).add(new Vector3f(up2).mul((hash(inst.seed, i, 6) - 0.5f) * 0.3f * p)).add(0, k * 0.5f * p, 0);
            float size = (0.55f + 1.1f * k) * p, rot = hash(inst.seed, i, 7) * Mth.TWO_PI + k * 0.8f, fa = (1 - k) * (1 - k);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, size, rot, al(INK, 0.8f * fa));
            buf.billboard(ctx, SMOKE, VfxBlend.ADD, q, size * 0.96f, rot, al(pal.deep, 0.38f * fa));
        }
        int ne = ctx.seg(6, 3);
        for (int i = 0; i < ne; i++) {
            float f = 0.1f + 0.85f * hash(inst.seed, i, 8);
            float born = t0 + (t1 - t0) * (float) Math.pow(f, 1.0 / 1.35);
            float k = (age - born) / (0.5f * D);
            if (k < 0f || k > 1f) continue;
            Vector3f q = new Vector3f(dir).mul(len * f).add(a).add(unit(inst.seed, i, 9).mul(k * 0.9f * p)).add(0, k * 0.7f * p, 0);
            buf.billboard(ctx, FLAKES, VfxBlend.ADD, q, 0.5f * p * (1 - 0.4f * k), age * 0.3f + i, al(pal.hi, 1f - k));
        }

        // ---- the brand: at 'to' the target is marked
        float ti = t1 - 0.02f * D;
        if (age >= ti) {
            float k = clamp01((age - ti) / (D - ti)), pop = VfxAnim.easeOutBack(clamp01((age - ti) / 3.2f));
            float vis = 1f - k * k;
            float fl = 1f - sstep(0f, 0.5f, k);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, b, 1.6f * p * pop, age * 0.3f, al(pal.hot, fl));
            buf.billboard(ctx, FLASH, VfxBlend.ADD, b, 1.1f * p * pop, -age * 0.4f, al(pal.hi, 0.85f * fl));
            VfxBloom.glow(ctx, buf, b, 1.0f * p, pal.glow, vis);
            inkGlow(buf, ctx, SPIRAL, b, 1.25f * p * pop, -age * 0.12f, al(INDIGO, 0.9f * vis), al(FLECK, 0.8f * vis));
            inkGlow(buf, ctx, BRAND, b, 1.6f * p * pop, age * 0.07f, al(INK, 0.9f * vis), al(VfxVertexBuffer.lerpColor(pal.glow, pal.hot, 0.25f), 0.95f * vis));
            float rr = 0.35f * p + 1.5f * p * VfxAnim.easeOutCubic(k);
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(b, dir).spin(age * 0.2f), ringHalf(rr), al(pal.hi, 0.9f * (1f - k)));
            int ns = ctx.seg(5, 3);
            for (int i = 0; i < ns; i++) {
                Vector3f v = unit(inst.seed, i, 20).add(new Vector3f(dir).mul(-0.9f)).normalize();
                Vector3f q = new Vector3f(b).add(new Vector3f(v).mul(VfxAnim.easeOutCubic(k) * (1.4f + 1.4f * hash(inst.seed, i, 21)) * p)).add(0, -0.9f * p * k * k, 0);
                float sz = (0.38f + 0.3f * hash(inst.seed, i, 22)) * p, rot = age * (0.25f + hash(inst.seed, i, 23)) + i;
                cell(buf, ctx, SHARDS, 2, 2 + (i & 1), VfxBlend.ALPHA, q, sz, rot, al(SHADE, vis));
                cell(buf, ctx, SHARDS, 2, 2 + (i & 1), VfxBlend.ADD, q, sz, rot, al(pal.hi, 0.85f * vis));
            }
        }
    }

    // ================================================================== CURSE_FX2: the Hex Field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, R = Math.max(1.2f, inst.power);
        final Pal pal = new Pal(inst);
        final float fade = life(inst, age, 8, 12);
        final float open = VfxAnim.easeOutCubic(clamp01(age / 12f)), pop = VfxAnim.easeOutBack(clamp01(age / 10f));
        final float beat = 0.55f + 0.45f * Mth.sin(age * 0.16f);
        final Vector3f c = ctx.rel(inst.from(ctx));
        final VfxPose g = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));

        // ---- the ground: black blight spreads, glowing veins branch through it
        float blightR = R * (0.35f + 0.80f * open);
        VfxPose vp = g.spin(0.6f + age * 0.0015f);
        buf.plane(VEINS, VfxBlend.ALPHA, vp, blightR, al(INK, 0.9f * fade));
        buf.plane(VEINS, VfxBlend.ADD, vp.lift(0.012f), blightR, al(pal.glow, (0.24f + 0.24f * beat) * fade));

        // ---- the sigil: a ring of runes, and a smaller one turning against it
        float sr = R * open;
        VfxPose sp = g.lift(0.02f).spin(age * 0.012f);
        inkGlow(buf, SIGIL, sp, sr, al(INK, 0.95f * fade), al(pal.glow, 0.85f * fade));
        VfxPose sp2 = g.lift(0.035f).spin(-age * 0.03f + 1f);
        inkGlow(buf, SIGIL, sp2, sr * 0.56f, al(INK, 0.7f * fade), al(pal.glow, 0.55f * fade));

        // ---- the Mark of Megicula's Curse: a fuzzy indigo spiral under the brand, turning slowly
        VfxPose mp = g.lift(0.04f).spin(-age * 0.03f);
        inkGlow(buf, SPIRAL, mp, Math.min(R * 0.5f, 4.2f) * pop, al(INDIGO, 0.9f * fade), al(FLECK, (0.55f + 0.3f * beat) * fade));

        // ---- the brand at the heart of it, beating
        float bs = Math.min(R * 0.32f, 2.8f) * (1f + 0.05f * Mth.sin(age * 0.35f)) * pop;
        VfxPose bp = g.lift(0.05f).spin(age * 0.02f);
        inkGlow(buf, BRAND, bp, bs, al(INK, 0.85f * fade), al(VfxVertexBuffer.lerpColor(pal.glow, pal.hot, 0.3f), (0.6f + 0.4f * beat) * fade));

        // ---- jagged shock rings pulsing out of the heart
        if (age > 5f) {
            for (int j = 0; j < 2; j++) {
                float k = fract(age / 26f + j * 0.5f);
                float rr = R * (0.10f + 0.88f * k);
                buf.plane(RING, VfxBlend.ADD, g.lift(0.07f + 0.004f * j).spin(j * 1.3f), ringHalf(rr), al(pal.hi, 0.75f * (1f - k) * Math.min(1f, k * 8f) * fade));
            }
        }

        // ---- the wall: black thorns with glowing cracks and a row of runes, leaning inward
        float hFull = Mth.clamp(0.9f + 0.26f * R, 1.6f, 4.6f);
        float hh = hFull * VfxAnim.easeOutCubic(clamp01((age - 2f) / 12f));
        if (hh > 0.05f) {
            int seg = ctx.seg(12, 8);
            float rep = Math.max(2f, Math.round(Mth.TWO_PI * R / (4f * hFull)));
            VfxPose wp = VfxPose.ground(new Vector3f(c).add(0, hh * 0.5f + 0.03f, 0));
            hoop(buf, WALL, VfxBlend.ALPHA, wp, R * 0.995f, R * 0.80f, hh * 0.5f, seg, rep, age * 0.0015f, al(INK, 0.92f * fade));
            hoop(buf, WALL, VfxBlend.ADD, wp, R * 0.995f, R * 0.80f, hh * 0.5f, seg, rep, age * 0.0015f, al(pal.glow, (0.50f + 0.20f * beat) * fade));
        }

        // ---- a ribbon of runes orbiting the dome, tilted, turning slowly
        float bandH = hFull * 0.92f * VfxAnim.easeOutCubic(clamp01((age - 6f) / 14f));
        if (bandH > 0.05f) {
            float br = R * 0.84f, halfW = Mth.clamp(0.10f + 0.022f * R, 0.14f, 0.42f);
            Vector3f axis = new Vector3f(0.09f * Mth.sin(age * 0.021f), 1f, 0.09f * Mth.cos(age * 0.021f));
            float bRep = Math.max(2f, Math.round(Mth.TWO_PI * br / (8f * halfW * 2f)));
            VfxPose bandPose = VfxPose.facing(new Vector3f(c).add(0, bandH, 0), axis);
            hoop(buf, BAND, VfxBlend.ALPHA, bandPose, br, br, halfW * 1.05f, ctx.seg(8, 6), bRep, age * 0.004f, al(INK, 0.85f * fade));
            hoop(buf, BAND, VfxBlend.ADD, bandPose, br, br, halfW, ctx.seg(8, 6), bRep, age * 0.004f, al(pal.hi, 0.8f * fade));
        }

        // ---- thorns rising out of the earth along the rim
        int nt = ctx.seg(9, 5);
        float ph = Mth.clamp(0.9f + 0.16f * R, 1.2f, 3.2f);
        for (int i = 0; i < nt; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * hash(inst.seed, i, 1)) / nt + 0.2f;
            float rho = R * (0.90f + 0.10f * hash(inst.seed, i, 2));
            float grow = VfxAnim.easeOutBack(clamp01((age - 1f - 7f * hash(inst.seed, i, 4)) / 9f)) * fade;
            float h = ph * (0.65f + 0.7f * hash(inst.seed, i, 3)) * grow;
            if (h < 0.05f) continue;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * rho, 0.02f, Mth.sin(ang) * rho);
            int cellIdx = (i % 3 == 2) ? 1 : 0;
            float sway = Mth.sin(age * 0.07f + i * 1.9f) * 0.06f * h;
            thorn(buf, VfxBlend.ALPHA, cellIdx, base, h, sway, al(SHADE, 0.97f * fade), al(SHADE, 0.97f * fade));
            thorn(buf, VfxBlend.ADD, cellIdx, base, h, sway, al(pal.glow, 0.7f * fade), al(pal.hi, 0.85f * fade));
        }

        // ---- violet embers rising, ash falling, soul wisps drifting up
        int nm = ctx.seg(9, 4);
        for (int i = 0; i < nm; i++) {
            float ang = hash(inst.seed, i, 5) * Mth.TWO_PI, rr = R * Mth.sqrt(hash(inst.seed, i, 6)) * 0.95f;
            float phs = fract(age * 0.016f * (0.8f + 0.4f * hash(inst.seed, i, 7)) + hash(inst.seed, i, 8));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr + Mth.sin(age * 0.1f + i) * 0.12f, 0.1f + phs * (1.7f + 0.5f * hash(inst.seed, i, 9)), Mth.sin(ang) * rr);
            buf.billboard(ctx, FLAKES, VfxBlend.ADD, q, (0.30f + 0.3f * hash(inst.seed, i, 10)) * Math.min(1.5f, 0.6f + 0.1f * R),
                    age * 0.02f + i, al(pal.hi, 0.9f * (float) Math.sin(Mth.PI * phs) * fade));
        }
        int nf = ctx.seg(4, 2);
        for (int i = 0; i < nf; i++) {
            float ang = hash(inst.seed, i, 11) * Mth.TWO_PI, rr = R * Mth.sqrt(hash(inst.seed, i, 12)) * 0.9f;
            float phs = fract(age * 0.012f + hash(inst.seed, i, 13));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 2.6f - phs * 2.5f, Mth.sin(ang) * rr);
            buf.billboard(ctx, FLAKES, VfxBlend.ALPHA, q, 0.7f, age * 0.03f + i, al(INK, 0.9f * (float) Math.sin(Mth.PI * phs) * fade));
        }
        int nw = ctx.seg(4, 2);
        for (int i = 0; i < nw; i++) {
            float phs = fract(age * 0.010f + i / (float) nw);
            float ang = i * 2.4f + phs * 4.5f, rr = R * (0.25f + 0.45f * hash(inst.seed, i, 14));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.5f + phs * 2.8f, Mth.sin(ang) * rr);
            float sz = 1.0f + 0.5f * hash(inst.seed, i, 15);
            tall(buf, ctx, WISP, VfxBlend.ADD, q, 0.5f * sz, sz, Mth.sin(age * 0.1f + i) * 0.25f, al(pal.hi, 0.6f * (float) Math.sin(Mth.PI * phs) * fade));
        }
        // a little smoke drifting along the foot of the wall
        int ns = ctx.seg(4, 2);
        for (int i = 0; i < ns; i++) {
            float ang = Mth.TWO_PI * i / ns + age * 0.006f + hash(inst.seed, i, 16);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * R * 0.96f, 0.5f + 0.25f * Mth.sin(age * 0.05f + i), Mth.sin(ang) * R * 0.96f);
            float sz = Math.min(3.2f, 1.5f + 0.12f * R);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, sz, age * 0.01f + i, al(INK, 0.55f * fade));
            buf.billboard(ctx, SMOKE, VfxBlend.ADD, q, sz * 0.95f, age * 0.01f + i, al(pal.deep, 0.25f * fade));
        }
    }

    // ================================================================== CURSE_FX3: the Curse Brand
    private void brand(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Mth.clamp(inst.power, 0.5f, 4f);
        final Pal pal = new Pal(inst);
        final Vector3f c = ctx.rel(inst.from(ctx));
        final Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        final float hl = hint.length();
        final boolean directed = hl > 0.2f;
        if (directed) hint.div(hl); else hint.set(0, 0, 0);

        final float ta = Math.max(2f, 0.12f * D);                                // anticipation ticks
        final float fade = life(inst, age, 0, 0.30f * D);
        final float k = clamp01((age - ta) / (D - ta));                          // burst progress
        final float e = VfxAnim.easeOutCubic(clamp01((age - ta) / (0.45f * D)));  // how far the burst has spread

        // ---- anticipation: ash and light are sucked in, a ring contracts, a black point swells
        if (age < ta + 1f) {
            float an = clamp01(age / ta);
            int nm = ctx.seg(7, 4);
            for (int i = 0; i < nm; i++) {
                float rr = (1.0f + 1.4f * hash(inst.seed, i, 1)) * p * (1f - an) * (1f - an) + 0.08f;
                Vector3f q = new Vector3f(c).add(unit(inst.seed, i, 2).mul(rr));
                buf.billboard(ctx, FLAKES, VfxBlend.ADD, q, 0.4f * p, hash(inst.seed, i, 3) * Mth.TWO_PI + age * 0.4f, al(pal.hi, 0.35f + 0.65f * an));
            }
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(c, new Vector3f(ctx.camRight).cross(ctx.camUp)), ringHalf(p * (2.2f * (1f - an) + 0.3f)), al(pal.glow, 0.9f * an));
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, c, p * (0.2f + 0.6f * an), age * 0.2f, al(INK, 0.95f));
            VfxBloom.glow(ctx, buf, c, 0.5f * p * an, pal.glow, an);
        }

        // ---- the black sun: a mass of black smoke ringed by a violet corona (hollow in the middle, so the black body reads), a hot flash at the hit
        float sun = VfxAnim.easeOutCubic(clamp01((age - ta + 1f) / 6f));
        float sunSize = p * (0.5f + 1.7f * sun) * (1f - 0.3f * sstep(0.35f, 1f, k));
        float sunA = 0.97f * (1f - sstep(0.62f, 1f, k));
        if (sun > 0f && sunA > 0.01f) {
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, c, sunSize, age * 0.05f, al(INK, sunA));
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, c, sunSize * 0.72f, -age * 0.07f + 1.3f, al(INK, sunA));
            float cor = 1f - sstep(0.30f, 0.92f, k);
            buf.billboard(ctx, CORONA, VfxBlend.ADD, c, sunSize * 2.15f, age * 0.04f, al(pal.glow, 0.95f * cor));
            buf.billboard(ctx, CORONA, VfxBlend.ADD, c, sunSize * 1.7f, -age * 0.07f + 0.8f, al(pal.hi, 0.8f * cor));
            VfxBloom.glow(ctx, buf, c, 1.1f * p * (1f - 0.4f * k), pal.glow, (1f - k) * 0.8f);
            float core = 1f - sstep(0f, 0.22f, k);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, c, p * (1.0f + 1.4f * sun), age * 0.2f, al(pal.hot, core));
            if (age - ta < 4.5f) {                                               // the hit: the world inverts for a few ticks
                float inv = 1f - clamp01((age - ta + 1f) / 5.5f);
                buf.billboard(ctx, FLASH, VfxBlend.NEGATIVE, c, p * (3.2f + 2.0f * sun), age * 0.1f, dim(pal.glow, 0.75f * inv));
            }
            if (directed) {                                                      // a streak of light along the hint
                float roll = (float) Math.atan2(hint.dot(ctx.camUp), hint.dot(ctx.camRight)) - Mth.HALF_PI;
                Vector3f mid = new Vector3f(c).add(new Vector3f(hint).mul(1.2f * p * e));
                tall(buf, ctx, FLASH, VfxBlend.ADD, mid, 0.8f * p * (1f - k), 4.6f * p * e, roll, al(pal.hot, 0.9f * (1f - sstep(0f, 0.5f, k))));
            }
        }

        // ---- the signature: the five-bladed curse brand burns in the air, then fades as the afterglow
        float bvis = sstep(0.03f, 0.18f, k) * (1f - sstep(0.62f, 1f, k));
        if (bvis > 0.01f) {
            inkGlow(buf, ctx, SPIRAL, c, p * 3.9f * VfxAnim.easeOutBack(clamp01((age - ta) / 8f)), -age * 0.05f, al(INDIGO, 0.85f * bvis), al(FLECK, 0.7f * bvis));
            float bsz = p * 3.4f * VfxAnim.easeOutBack(clamp01((age - ta) / 7f)) * (1f + 0.04f * Mth.sin(age * 0.5f));
            inkGlow(buf, ctx, BRAND, c, bsz, age * 0.035f, al(INK, 0.9f * bvis), al(VfxVertexBuffer.lerpColor(pal.glow, pal.hot, 0.3f), (0.65f + 0.35f * Mth.sin(age * 0.45f)) * bvis));
        }

        // ---- jagged shock rings
        float flatY = -0.0f;
        for (int j = 0; j < 2; j++) {
            float kj = clamp01((age - ta - 1.5f * j) / (0.7f * D));
            if (kj <= 0f || kj >= 1f) continue;
            float rr = p * (0.5f + (3.8f - 0.9f * j) * VfxAnim.easeOutCubic(kj));
            float ra = (1f - kj) * (1f - kj) * 0.9f;
            if (j == 0) buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, flatY, 0)).spin(age * 0.04f), ringHalf(rr), al(pal.hi, ra));
            else buf.billboard(ctx, RING, VfxBlend.ADD, c, ringHalf(rr) * 2f, age * 0.1f, al(pal.glow, ra));
        }

        // ---- obsidian splinters flung out (gravity pulls them down)
        int nsh = ctx.seg(9, 5);
        if (k > 0f) {
            float eh = VfxAnim.easeOutCubic(clamp01(k * 1.6f));
            for (int i = 0; i < nsh; i++) {
                Vector3f v = unit(inst.seed, i, 30);
                if (directed) v.mul(0.7f).add(new Vector3f(hint).mul(0.9f * (0.5f + hash(inst.seed, i, 31)))).normalize();
                float speed = (2.4f + 3.0f * hash(inst.seed, i, 32)) * p;
                Vector3f q = new Vector3f(c).add(new Vector3f(v).mul(speed * eh)).add(0, -2.4f * p * k * k, 0);
                float sz = (0.32f + 0.4f * hash(inst.seed, i, 33)) * p, rot = age * (0.2f + 0.5f * hash(inst.seed, i, 34)) + i;
                float va = 1f - sstep(0.55f, 1f, k);
                int cellIdx = i & 3;
                cell(buf, ctx, SHARDS, 2, cellIdx, VfxBlend.ALPHA, q, sz, rot, al(SHADE, va));
                cell(buf, ctx, SHARDS, 2, cellIdx, VfxBlend.ADD, q, sz, rot, al(pal.hi, 0.8f * va));
            }
        }

        // ---- thorned tendrils whipping out and curling back
        int ntd = ctx.seg(4, 2);
        float grow = e * (1f - sstep(0.55f, 1f, k));
        if (grow > 0.02f) {
            for (int i = 0; i < ntd; i++) {
                Vector3f d = unit(inst.seed, i, 40);
                if (directed) d.mul(0.8f).add(new Vector3f(hint).mul(0.8f)).normalize();
                Vector3f sdv = side(d), upv = new Vector3f(sdv).cross(d).normalize();
                float curl = (hash(inst.seed, i, 41) < 0.5f ? 1f : -1f) * (0.5f + 0.5f * hash(inst.seed, i, 42));
                Vector3f[] pts = new Vector3f[3];
                float[] wd = new float[3];
                int[] ci = new int[3], cg = new int[3];
                for (int m = 0; m < 3; m++) {
                    float f = m / 2f;
                    float reach = (0.3f + 2.4f * f) * p * grow;
                    float bend = (float) Math.sin(Mth.PI * f) * 0.7f * p * curl * grow;
                    pts[m] = new Vector3f(c).add(new Vector3f(d).mul(reach)).add(new Vector3f(sdv).mul(bend * Mth.cos(age * 0.2f + i))).add(new Vector3f(upv).mul(bend * Mth.sin(age * 0.2f + i)));
                    wd[m] = 0.7f * p * (0.6f + 0.4f * grow);
                    ci[m] = al(INK, 0.95f * fade);
                    cg[m] = al(VfxVertexBuffer.lerpColor(pal.deep, pal.hi, f), (0.45f + 0.55f * f) * fade);
                }
                ribbon(buf, TENDRIL, VfxBlend.ALPHA, pts, wd, ci);
                ribbon(buf, TENDRIL, VfxBlend.ADD, pts, wd, cg);
            }
        }

        // ---- runes unfurling on a ring, spinning out
        float ru = sstep(0.0f, 0.2f, k) * (1f - sstep(0.5f, 0.95f, k));
        if (ru > 0.01f) {
            int nr = ctx.seg(8, 4);
            for (int i = 0; i < nr; i++) {
                float ang = Mth.TWO_PI * i / nr + age * 0.06f;
                float rr = p * (1.3f + 1.7f * e);
                Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, Mth.sin(i * 1.7f + age * 0.05f) * 0.5f * p, Mth.sin(ang) * rr);
                cell(buf, ctx, GLYPHS, 4, (i * 3 + 1) & 15, VfxBlend.ADD, q, 0.5f * p, age * 0.05f + i, al(pal.hi, 0.9f * ru));
            }
        }

        // ---- the afterglow: ash falling, embers and soul wisps rising
        int na = ctx.seg(5, 2);
        for (int i = 0; i < na; i++) {
            float st = clamp01((age - ta - 2f - 2f * hash(inst.seed, i, 50)) / (0.6f * D));
            if (st <= 0f || st >= 1f) continue;
            Vector3f q = new Vector3f(c).add((hash(inst.seed, i, 51) - 0.5f) * 3.2f * p * (0.4f + st), 0.5f * p - st * 1.8f * p, (hash(inst.seed, i, 52) - 0.5f) * 3.2f * p * (0.4f + st));
            buf.billboard(ctx, FLAKES, VfxBlend.ALPHA, q, 0.8f * p, age * 0.04f + i, al(INK, 0.9f * (1f - st)));
            buf.billboard(ctx, FLAKES, VfxBlend.ADD, new Vector3f(q).add(0, 0.12f * p, 0), 0.6f * p, age * 0.07f + i, al(pal.hi, 0.9f * (1f - st) * (1f - st)));
        }
        int nw = ctx.seg(3, 2);
        for (int i = 0; i < nw; i++) {
            float st = clamp01((age - ta - 3f - 2f * i) / (0.6f * D));
            if (st <= 0f || st >= 1f) continue;
            float ang = i * 2.1f + st * 3f;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * (0.7f + 0.8f * st) * p, 0.4f * p + st * 2.4f * p, Mth.sin(ang) * (0.7f + 0.8f * st) * p);
            float sz = 1.2f * p;
            tall(buf, ctx, WISP, VfxBlend.ADD, q, 0.5f * sz, sz, Mth.sin(age * 0.12f + i) * 0.3f, al(pal.hi, 0.9f * (float) Math.sin(Mth.PI * st)));
        }
    }
}
