package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.FungusFx.*;

/**
 * Fungus Magic (spores, mycelium and mushrooms: the spell pages Towering / Heavy / Running / Talking Mr. Mushroom and their kin).
 * Look: warm gold-ochre spore clouds with a toxic olive edge, cream mycelium threads, mushrooms of several species popping out of the
 * ground, gill-patterned sigils and fairy rings, glittering spores. Textures from tools/gen_fungus_textures.py. All three shapes follow the
 * attribute shape protocol; the effect colour is mixed into the palette (a white tint still reads as fungus).
 * <ul>
 *   <li>FUNGUS_FX1, Spore Shot (cast / projectile): 'from' = hand, 'to' = target, power = size (1 = normal), duration = flight ticks (16).
 *       A gill sigil spins open at the hand while spores converge into a warty puffball with a halo of hyphae; it launches along the path
 *       trailing a ribbon of spore smoke, two braided mycelium strands corkscrewing round it, puffs of cloud and glittering spores left behind
 *       as the afterimage; at 'to' a flash, a ring of spore dust, puffs and little mushrooms thrown out.</li>
 *   <li>FUNGUS_FX2, Spore Field (zone / dome): 'from' = centre on the ground, power = RADIUS in blocks, duration = life ticks (80).
 *       A mycelium web grows over a dark stain, a cap-gill disc and a fairy ring of mushroom caps turn against each other, a pulse of spores
 *       sweeps out every second, mushrooms of four species pop up round the rim and inside, a wall of spore haze with drifting puffs closes
 *       the edge, spores rise and magicules are siphoned inward along spirals. Fades in over 8 ticks, out over the last 12.</li>
 *   <li>FUNGUS_FX3, Mushroom Bloom (impact / burst / signature): 'from' = centre, 'to' = optional direction hint (leans the bloom and throws a
 *       spore jet), power = scale, duration = life ticks (28). Flash and star flare, two shock rings, clouds, a crown of mushrooms bursting
 *       out, cap and stem chunks, glittering spores, a mycelium patch that lingers; from power 1.2 a giant Mr. Mushroom (arms, legs, a face;
 *       the old tired one for odd seeds) shoots up in the middle.</li>
 * </ul>
 */
public class FungusLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.FUNGUS_FX1, VfxShape.FUNGUS_FX2, VfxShape.FUNGUS_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case FUNGUS_FX2 -> 80;
            case FUNGUS_FX3 -> 28;
            default -> 16;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC0A04A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.FUNGUS_FX3 && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 0.35f * Math.min(inst.power, 3f), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case FUNGUS_FX1 -> cast(inst, ctx, buf);
            case FUNGUS_FX2 -> zone(inst, ctx, buf);
            case FUNGUS_FX3 -> bloom(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: Spore Shot
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = inst.duration, t = age / D, p = Mth.clamp(inst.power, 0.5f, 4f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(b).sub(a);
        float len = path.length();
        Vector3f dir = len < 0.05f ? new Vector3f(0, 0, 1) : new Vector3f(path).div(len);
        final float launch = 0.22f, arrive = 0.80f;
        float flight = (arrive - launch) * D;
        float charge = c01(t / launch);
        float u = c01((t - launch) / (arrive - launch));
        float ue = u * (0.35f + 0.65f * u);
        float imp = c01((t - arrive) / (1 - arrive));
        float fade = life(inst, age, 0, 2.5f);
        int gold = mix(GOLD, inst.color, 0.30f), amber = mix(AMBER, inst.color, 0.35f), olive = mix(OLIVE, inst.color, 0.30f), cream = mix(CREAM, inst.color, 0.15f);
        Vector3f head = new Vector3f(a).fma(len * ue, dir);
        Vector3f sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();

        // --- the trail, back to front: smoke ribbon, afterimage puffs, glow stripe, braided strands, glitter
        float trailLen = Math.min(len * ue, 4.6f * p);
        Vector3f tail = new Vector3f(head).fma(-trailLen, dir);
        if (u > 0.01f && trailLen > 0.05f) {
            buf.beam(ctx, TRAIL, VfxBlend.ALPHA, tail, head, 0.3f * p, 1.15f * p, 2, -age * 0.12f, a(olive, 0f), a(olive, 0.6f * fade));
        }
        int np = ctx.seg(10, 5);
        for (int i = 0; i < np; i++) {
            float ui = (i + 0.5f) / np;
            if (u <= ui) continue;
            float ag = (u - ui) * flight + imp * (1 - arrive) * D;      // ticks since the head passed
            float lifeP = 13f;
            if (ag > lifeP) continue;
            float k = ag / lifeP;
            float eu = ui * (0.35f + 0.65f * ui);
            Vector3f q = new Vector3f(a).fma(len * eu, dir).fma((hash(inst.seed, i, 1) - 0.5f) * (0.3f + 0.9f * k) * p, sd)
                    .fma((hash(inst.seed, i, 2) - 0.3f) * 0.7f * k * p, up).add(0, 0.5f * k * p, 0);
            float sz = (0.7f + 1.5f * smooth(k * 1.4f)) * p * (0.8f + 0.5f * hash(inst.seed, i, 3));
            sprite(buf, ctx, (i & 1) == 0 ? PUFF : PUFF2, VfxBlend.ALPHA, new float[]{0, 0, 1, 1}, q, sz, hash(inst.seed, i, 4) * Mth.TWO_PI + k,
                    a(mix(DUST, olive, 0.45f), 0.85f * (float) Math.pow(1 - k, 1.2) * fade * near(q, 0.6f, 2.2f)));
        }
        if (u > 0.01f && trailLen > 0.05f) {
            buf.beam(ctx, TRAIL, VfxBlend.ADD, tail, head, 0.06f * p, 0.55f * p, 3, age * 0.2f, a(amber, 0f), a(gold, 0.7f * fade));
            // two braided mycelium strands corkscrewing round the path
            int segs = ctx.seg(9, 5);
            for (int strand = 0; strand < 2; strand++) {
                Vector3f prev = null;
                for (int k = 0; k <= segs; k++) {
                    float s = (float) k / segs;
                    float phi = s * trailLen * 2.3f - age * 0.85f + strand * Mth.PI;
                    float rad = 0.30f * p * (0.20f + 0.80f * s);
                    Vector3f q = new Vector3f(tail).lerp(head, s).fma(Mth.cos(phi) * rad, sd).fma(Mth.sin(phi) * rad, up);
                    if (prev != null) {
                        buf.beam(ctx, HYPHA, VfxBlend.ADD, prev, q, 0.16f * p, 0.16f * p, 1, age * 0.1f + strand * 0.37f,
                                a(cream, 0.75f * s * fade), a(cream, 0.75f * s * fade));
                    }
                    prev = q;
                }
            }
        }
        int nm = ctx.seg(10, 5);
        for (int i = 0; i < nm; i++) {
            float ui = hash(inst.seed, i, 5) * 0.96f;
            if (u <= ui) continue;
            float ag = (u - ui) * flight + imp * (1 - arrive) * D, k = ag / 15f;
            if (k > 1) continue;
            float eu = ui * (0.35f + 0.65f * ui);
            Vector3f q = new Vector3f(a).fma(len * eu, dir).fma((hash(inst.seed, i, 6) - 0.5f) * (0.5f + 1.4f * k) * p, sd)
                    .fma((hash(inst.seed, i, 7) - 0.5f) * (0.5f + 1.4f * k) * p, up).add(0, -0.6f * k * k * p, 0);
            float tw = 0.55f + 0.45f * Mth.sin(age * 0.7f + i * 2.3f);
            mote(buf, ctx, (i % 3 == 0) ? 3 : (i % 3 == 1 ? 0 : 2), q, (0.16f + 0.16f * hash(inst.seed, i, 8)) * p * (1 - 0.5f * k), age * 0.1f + i,
                    a(i % 2 == 0 ? gold : cream, (1 - k) * tw * fade));
        }

        // --- the charge at the hand: a spinning gill sigil, spores pouring into the pod
        if (t < launch + 0.12f) {
            float ch = easeOut(charge);
            float vanish = 1 - c01((t - launch) / 0.12f);
            Vector3f hc = new Vector3f(a).fma(0.35f * p, dir);
            VfxPose sig = VfxPose.facing(hc, dir).spin(age * 0.22f);
            buf.plane(GILLS, VfxBlend.ADD, sig, 0.62f * p * VfxAnim.easeOutBack(ch), a(amber, 0.62f * vanish * fade));
            buf.plane(GILLS, VfxBlend.ADD, sig.lift(0.04f).spin(-age * 0.5f), 0.40f * p * ch, a(gold, 0.40f * vanish * fade));
            VfxBloom.glow(ctx, buf, a, 0.40f * p * ch, amber, 0.7f * vanish * fade);
            int nc = ctx.seg(8, 4);
            for (int i = 0; i < nc; i++) {
                float ph = fract(charge * 1.15f + hash(inst.seed, i, 9));
                Vector3f dv = new Vector3f(hash(inst.seed, i, 10) - 0.5f, hash(inst.seed, i, 11) - 0.5f, hash(inst.seed, i, 12) - 0.5f).normalize();
                Vector3f q = new Vector3f(a).fma((1 - ph) * 1.5f * p, dv);
                mote(buf, ctx, i % 4 == 0 ? 3 : (i % 2 == 0 ? 0 : 2), q, 0.2f * p * (0.4f + 0.6f * ph), age * 0.2f + i, a(i % 2 == 0 ? gold : cream, ph * vanish * fade));
            }
        }

        // --- the head: a warty puffball with a halo of hyphae under a hot glow
        if (imp < 0.35f) {
            float hs = 0.95f * p * (0.25f + 0.75f * easeOut(charge)) * (1 - smooth(imp / 0.35f));
            float flick = 0.92f + 0.08f * Mth.sin(age * 1.3f);
            Vector3f hp = u > 0 ? head : new Vector3f(a).fma(0.2f * p * charge, dir);
            buf.billboard(ctx, PUFFBALL, VfxBlend.ALPHA, hp, hs * 1.5f, age * 0.22f, a(mix(WHITE, gold, 0.5f), fade));
            VfxBloom.glow(ctx, buf, hp, 0.50f * p * flick, amber, 0.55f * fade);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, hp, hs * 1.0f, -age * 0.1f, a(gold, 0.30f * fade * flick));
        }

        // --- landing: flash, ring of spore dust, puffs and little mushrooms thrown out
        if (imp > 0) {
            float e = easeOut(imp), inv = 1 - imp;
            Vector3f hit = b;
            buf.billboard(ctx, FLARE, VfxBlend.ADD, hit, (0.5f + 1.9f * e) * p, 0.4f + age * 0.05f, a(cream, (float) Math.pow(inv, 1.2) * fade));
            VfxBloom.glow(ctx, buf, hit, (0.5f + 0.9f * e) * p, gold, inv * fade);
            buf.billboard(ctx, SHOCK, VfxBlend.ADD, hit, (0.3f + 2.9f * e) * p, 0, a(gold, inv * 0.9f * fade));
            int nb = ctx.seg(6, 3);
            for (int i = 0; i < nb; i++) {
                Vector3f dv = new Vector3f(hash(inst.seed, i, 20) - 0.5f, hash(inst.seed, i, 21) - 0.4f, hash(inst.seed, i, 22) - 0.5f).normalize();
                Vector3f q = new Vector3f(hit).fma((0.2f + 1.0f * e) * p * (0.7f + 0.6f * hash(inst.seed, i, 23)), dv);
                sprite(buf, ctx, (i & 1) == 0 ? PUFF : PUFF2, VfxBlend.ALPHA, new float[]{0, 0, 1, 1}, q, (0.5f + 0.8f * e) * p, hash(inst.seed, i, 24) * 6.28f,
                        a(mix(DUST, olive, 0.4f), 0.6f * inv * inv * fade * near(q, 0.6f, 2.2f)));
            }
            for (int i = 0; i < 3; i++) {                                   // little mushrooms flung out
                float ang = hash(inst.seed, i, 25) * Mth.TWO_PI;
                Vector3f q = new Vector3f(hit).add(Mth.cos(ang) * 1.1f * e * p, (1.9f * e - 2.6f * e * e) * p + 0.1f, Mth.sin(ang) * 1.1f * e * p);
                sprite(buf, ctx, DEBRIS, VfxBlend.ALPHA, cell2(3), q, 0.34f * p, age * 0.35f + i * 2, a(mix(WHITE, inst.color, 0.15f), c01(inv * 1.6f) * fade));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Spore Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.2f, inst.power);
        float f = life(inst, age, 8, 12), g = easeOut(c01(age / 12f)), rad = R * g;
        float endScale = c01((inst.duration - age) / 12f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int gold = mix(GOLD, inst.color, 0.30f), amber = mix(AMBER, inst.color, 0.35f), olive = mix(OLIVE, inst.color, 0.30f), cream = mix(CREAM, inst.color, 0.15f),
                toxic = mix(TOXIC, inst.color, 0.2f), real = mix(WHITE, inst.color, 0.15f);
        float wallH = Mth.clamp(0.7f + 0.16f * R, 1.0f, 3.4f);
        float sc = Mth.clamp(1.0f + 0.08f * R, 1.1f, 2.0f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float pulseHz = 0.5f + 0.5f * Mth.sin(age * 0.16f);

        // --- the ground, back to front: dark stain, mycelium web (two webs turned against each other), gill disc, fairy ring, spore pulses
        buf.plane(PUFF2, VfxBlend.ALPHA, ground.spin(0.7f), rad * 1.12f, a(SOIL, 0.72f * f));
        VfxBloom.planeGlow(buf, ground, rad * 0.7f, amber, 0.22f * f);
        buf.plane(MYCELIUM, VfxBlend.ADD, ground.lift(0.01f), rad, a(cream, 0.48f * f * (0.8f + 0.2f * pulseHz)));
        buf.plane(MYCELIUM, VfxBlend.ADD, ground.lift(0.012f).spin(1.9f), rad * 0.8f, a(gold, 0.26f * f));
        buf.plane(GILLS, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.016f), rad * 0.88f, a(gold, 0.30f * f));
        buf.plane(RING, VfxBlend.ALPHA, ground.lift(0.03f).spin(-age * 0.010f), rad, a(mix(0xFF6B4E2C, inst.color, 0.2f), 0.85f * f));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.034f).spin(-age * 0.010f), rad, a(gold, 0.36f * f));
        for (int k = 0; k < 2; k++) {
            float ph = fract(age / 20f + k * 0.5f), e = easeOut(ph);
            buf.plane(SHOCK, VfxBlend.ADD, ground.lift(0.04f).spin(k * 1.3f), rad * (0.12f + 0.88f * e), a(k == 0 ? gold : toxic, (1 - ph) * 0.45f * f * (0.3f + 0.7f * ph)));
        }

        // --- mushrooms popping up one after another: the rim first, then the heart
        int m = Mth.clamp(Math.round(5 + R), 6, 11), ringN = Math.round(m * 0.65f);
        float[] px = new float[m], pz = new float[m], hh = new float[m];
        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) {
            float h1 = hash(inst.seed, i, 31), h2 = hash(inst.seed, i, 32);
            float ang, rr;
            if (i < ringN) { ang = Mth.TWO_PI * (i + 0.5f) / ringN + (h1 - 0.5f) * 0.5f; rr = R * (0.84f + 0.12f * h2); }
            else { ang = h1 * Mth.TWO_PI; rr = R * (0.15f + 0.50f * h2); }
            px[i] = Mth.cos(ang) * rr; pz[i] = Mth.sin(ang) * rr; hh[i] = hash(inst.seed, i, 33);
            order[i] = i;
        }
        java.util.Arrays.sort(order, (x, y) -> Float.compare((c.x + px[y]) * (c.x + px[y]) + (c.z + pz[y]) * (c.z + pz[y]),
                (c.x + px[x]) * (c.x + px[x]) + (c.z + pz[x]) * (c.z + pz[x])));      // far to near
        for (int oi = 0; oi < m; oi++) {
            int i = order[oi];
            float delay = 2 + 22 * (float) i / m + 3 * hash(inst.seed, i, 34);
            float grow = Math.max(0f, VfxAnim.easeOutBack(c01((age - delay) / 7f))) * endScale;
            if (grow <= 0.01f || Math.hypot(px[i], pz[i]) > rad + 0.3f) continue;
            int sp = (int) (hash(inst.seed, i, 35) * 4) & 3;
            float height = sc * (0.75f + 0.5f * hh[i]) * (sp == 0 ? 1.15f : 0.95f) * grow;
            Vector3f base = new Vector3f(c).add(px[i], 0.02f, pz[i]);
            float vis = 0.35f + 0.65f * near(new Vector3f(base).add(0, height * 0.5f, 0), 0.5f, 1.6f);
            if (i % 2 == 0 || sp == 3) {
                VfxBloom.glow(ctx, buf, new Vector3f(base).add(0, height * 0.72f, 0), height * 0.30f, sp == 3 ? toxic : gold, (sp == 3 ? 0.55f : 0.28f) * f * grow);
            }
            upright(buf, ctx, SHROOMS, VfxBlend.ALPHA, cell4(sp), base, height, height * 0.25f, a(real, f * vis));
        }

        // --- the wall of haze and the puffs drifting in it
        VfxPose mid = VfxPose.ground(new Vector3f(c).add(0, wallH * 0.5f, 0));
        float circ = Mth.TWO_PI * rad;
        wall(buf, HAZE, VfxBlend.ALPHA, mid, rad, wallH * 0.5f, ctx.seg(16, 10), Math.max(2f, Math.round(circ / (wallH * 4f))), age * 0.004f, a(mix(olive, DUST, 0.35f), 0.50f * f));
        wall(buf, HAZE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, wallH * 0.42f, 0)), rad * 0.94f, wallH * 0.42f, ctx.seg(12, 8),
                Math.max(2f, Math.round(circ * 0.94f / (wallH * 3.4f))), -age * 0.006f + 0.3f, a(gold, 0.10f * f));
        int pn = ctx.seg(9, 4);
        for (int i = 0; i < pn; i++) {
            float ang = Mth.TWO_PI * i / pn + hash(inst.seed, i, 41) * 0.6f + age * 0.0035f * (i % 2 == 0 ? 1 : -1);
            float rr = rad * (0.74f + 0.24f * hash(inst.seed, i, 42));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.35f + wallH * 0.75f * hash(inst.seed, i, 43) + 0.12f * Mth.sin(age * 0.05f + i), Mth.sin(ang) * rr);
            sprite(buf, ctx, (i & 1) == 0 ? PUFF : PUFF2, VfxBlend.ALPHA, new float[]{0, 0, 1, 1}, q, (1.0f + 0.8f * hash(inst.seed, i, 44)) * sc * 1.1f,
                    ang + age * 0.004f, a(mix(DUST, olive, 0.5f), 0.30f * f * near(q, 0.9f, 3.2f)));
        }

        // --- spores rising, magicules siphoned inward
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.45f, 0), 0.7f + 0.04f * R, gold, 0.8f * f * (0.8f + 0.2f * pulseHz));
        int nr = ctx.seg(16, 8);
        for (int i = 0; i < nr; i++) {
            float w = fract(age * 0.017f + hash(inst.seed, i, 51));
            float ang = hash(inst.seed, i, 52) * Mth.TWO_PI + w * 1.4f * (i % 2 == 0 ? 1 : -1), rr = rad * Mth.sqrt(hash(inst.seed, i, 53)) * 0.96f;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.15f + w * (wallH * 1.3f + 0.5f), Mth.sin(ang) * rr);
            float tw = 0.6f + 0.4f * Mth.sin(age * 0.5f + i * 1.7f);
            mote(buf, ctx, i % 4 == 0 ? 3 : (i % 4 == 1 ? 2 : 0), q, (0.13f + 0.12f * hash(inst.seed, i, 54)) * sc, age * 0.05f + i,
                    a(i % 3 == 0 ? toxic : gold, Mth.sin(w * Mth.PI) * tw * f * near(q, 0.5f, 1.6f)));
        }
        int ns = ctx.seg(8, 4);
        for (int i = 0; i < ns; i++) {
            float w = fract(age * 0.021f + hash(inst.seed, i, 61));
            float ang = hash(inst.seed, i, 62) * Mth.TWO_PI + w * 2.6f, rr = rad * (float) Math.pow(1 - w, 1.3f);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rr, 0.25f + 1.5f * w * w + 0.3f * (1 - w), Mth.sin(ang) * rr);
            mote(buf, ctx, i % 3 == 0 ? 1 : (i % 3 == 1 ? 0 : 2), q, (0.12f + 0.16f * w) * sc, i, a(w > 0.7f ? cream : gold, Mth.sin(Math.min(1f, w * 1.15f) * Mth.PI) * 0.9f * f * near(q, 0.5f, 1.6f)));
        }
    }

    // ------------------------------------------------------------------ FX3: Mushroom Bloom
    private void bloom(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = inst.duration, t = age / D, S = Mth.clamp(inst.power, 0.5f, 4f);
        float f = life(inst, age, 0, 8);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasDir = hint.length() > 0.15f;
        Vector3f dh = hasDir ? new Vector3f(hint).normalize() : new Vector3f(0, 1, 0);
        int gold = mix(GOLD, inst.color, 0.30f), amber = mix(AMBER, inst.color, 0.35f), olive = mix(OLIVE, inst.color, 0.30f), cream = mix(CREAM, inst.color, 0.15f),
                toxic = mix(TOXIC, inst.color, 0.2f), real = mix(WHITE, inst.color, 0.15f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float patch = c01(1 - (t - 0.30f) / 0.70f);

        // --- the ground: a stain and a mycelium patch that spreads and lingers
        float spread = easeOut(c01(age / 9f));
        buf.plane(PUFF2, VfxBlend.ALPHA, ground.spin(inst.seed % 7), 2.6f * S * spread, a(SOIL, 0.30f * patch));
        buf.plane(MYCELIUM, VfxBlend.ADD, ground.lift(0.01f).spin(inst.seed % 5), 2.4f * S * spread, a(cream, 0.75f * patch * (0.7f + 0.3f * Mth.sin(age * 0.4f))));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.03f), 1.5f * S * spread, a(gold, 0.35f * patch));
        float r1 = c01(age / 13f), r2 = c01(age / 9f);
        buf.plane(SHOCK, VfxBlend.ADD, ground.lift(0.03f), (0.4f + 3.5f * easeOut(r1)) * S, a(gold, (float) Math.pow(1 - r1, 0.8f) * 0.9f));
        buf.plane(SHOCK, VfxBlend.ALPHA, ground.lift(0.025f).spin(0.5f), (0.5f + 3.9f * easeOut(r1)) * S, a(mix(DUST, olive, 0.5f), (1 - r1) * 0.45f));

        // --- clouds blown out of the burst
        int np = ctx.seg(8, 4);
        for (int i = 0; i < np; i++) {
            float az = hash(inst.seed, i, 1) * Mth.TWO_PI, el = -0.1f + 0.9f * hash(inst.seed, i, 2);
            Vector3f dv = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el) + 0.15f, Mth.sin(az) * Mth.cos(el));
            if (hasDir) dv.fma(0.7f, dh);
            dv.normalize();
            float e = easeOut(c01(age / 16f)), k = c01((age - 2f) / 22f);
            Vector3f q = new Vector3f(c).fma((0.15f + 1.7f * e) * S * (0.7f + 0.6f * hash(inst.seed, i, 3)), dv).add(0, 0.5f * k * S, 0);
            sprite(buf, ctx, (i & 1) == 0 ? PUFF : PUFF2, VfxBlend.ALPHA, new float[]{0, 0, 1, 1}, q, (0.8f + 1.1f * e) * S * (0.7f + 0.6f * hash(inst.seed, i, 4)),
                    az + age * 0.02f, a(mix(DUST, olive, 0.4f), 0.55f * (1 - k) * (1 - k) * near(q, 0.8f, 2.8f)));
        }
        if (hasDir) {                                                          // a jet of spores along the hint
            Vector3f tip = new Vector3f(c).fma(Math.min(hint.length(), 3.5f * S) * easeOut(c01(age / 8f)), dh);
            buf.beam(ctx, TRAIL, VfxBlend.ADD, c, tip, 0.9f * S, 0.15f * S, 3, -age * 0.2f, a(gold, 0.7f * (1 - c01(age / 14f))), a(cream, 0f));
        }

        // --- Mr. Mushroom shoots up from the heart of a big bloom
        float big = smooth((S - 1.0f) / 0.8f);
        if (big > 0.02f) {
            float rise = Math.max(0f, VfxAnim.easeOutBack(c01((age - 2.5f) / 9f)));
            float shrink = 1 - smooth((t - 0.72f) / 0.28f);
            float hgt = 2.5f * S * rise * shrink;
            Vector3f lean = new Vector3f(Mth.sin(age * 0.45f) * 0.07f + (hasDir ? dh.x * 0.12f : 0), 1, (hasDir ? dh.z * 0.12f : 0)).normalize();
            along(buf, ctx, (inst.seed & 1) == 0 ? MR : MR_HEAVY, VfxBlend.ALPHA, new float[]{0, 0, 1, 1}, new Vector3f(c).add(0, -0.05f, 0), lean, hgt, hgt * 0.25f,
                    a(real, f * big));
        }

        // --- the crown of mushrooms bursting out
        int km = 6;
        for (int i = 0; i < km; i++) {
            float az = Mth.TWO_PI * (i + hash(inst.seed, i, 5) * 0.6f) / km, tilt = 0.35f + 0.55f * hash(inst.seed, i, 6);
            Vector3f d = new Vector3f(Mth.sin(tilt) * Mth.cos(az), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(az));
            if (hasDir) d.fma(0.45f, dh);
            d.normalize();
            float delay = hash(inst.seed, i, 7) * 4f;
            float grow = Math.max(0f, VfxAnim.easeOutBack(c01((age - delay) / 6f))) * (1 - smooth((t - 0.62f) / 0.38f));
            if (grow <= 0.01f) continue;
            float len = (0.85f + 0.65f * hash(inst.seed, i, 8)) * S * grow;
            int sp = (int) (hash(inst.seed, i, 9) * 4) & 3;
            Vector3f base = new Vector3f(c).add(0, -0.05f, 0);
            if (sp == 3) VfxBloom.glow(ctx, buf, new Vector3f(base).fma(len * 0.8f, d), len * 0.3f, toxic, 0.5f * f);
            along(buf, ctx, SHROOMS, VfxBlend.ALPHA, cell4(sp), base, d, len, len * 0.25f, a(real, f));
        }

        // --- chunks of cap and stem, spores
        int nd = ctx.seg(9, 4);
        for (int i = 0; i < nd; i++) {
            float az = hash(inst.seed, i, 10) * Mth.TWO_PI, pol = 0.25f + 0.9f * hash(inst.seed, i, 11);
            Vector3f v = new Vector3f(Mth.sin(pol) * Mth.cos(az), Mth.cos(pol), Mth.sin(pol) * Mth.sin(az)).mul((2.4f + 2.6f * hash(inst.seed, i, 12)) * S);
            if (hasDir) v.fma(1.6f * S, dh);
            float tau = age / 20f;
            Vector3f q = new Vector3f(c).fma(tau, v).add(0, -4.5f * tau * tau * S, 0);
            float life = c01((t - 0.0f) / 0.82f);
            if (life >= 1) continue;
            int cellK = (int) (hash(inst.seed, i, 13) * 4) & 3;
            sprite(buf, ctx, DEBRIS, VfxBlend.ALPHA, cell2(cellK), q, 0.30f * S * (0.6f + 0.8f * hash(inst.seed, i, 14)), age * (0.18f + 0.3f * hash(inst.seed, i, 15)) * (i % 2 == 0 ? 1 : -1),
                    a(real, c01((1 - life) * 3f) * f * near(q, 0.4f, 1.2f)));
        }
        int ns = ctx.seg(12, 6);
        for (int i = 0; i < ns; i++) {
            float az = hash(inst.seed, i, 16) * Mth.TWO_PI, el = (hash(inst.seed, i, 17) - 0.25f) * 1.9f;
            Vector3f dv = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            if (hasDir) dv.fma(0.5f, dh).normalize();
            float tau = age / 20f, reach = (1.2f + 1.8f * hash(inst.seed, i, 18)) * S;
            Vector3f q = new Vector3f(c).fma((1 - (float) Math.exp(-tau * 3.5f)) * reach, dv).add(0, tau * 0.7f * S, 0);
            float tw = 0.55f + 0.45f * Mth.sin(age * 0.6f + i * 2.1f);
            mote(buf, ctx, i % 3 == 0 ? 3 : (i % 3 == 1 ? 0 : 2), q, (0.16f + 0.16f * hash(inst.seed, i, 19)) * S, age * 0.1f + i,
                    a(i % 3 == 2 ? toxic : gold, (float) Math.pow(1 - t, 1.1f) * tw * near(q, 0.4f, 1.2f)));
        }

        // --- the flash, last so it burns over everything
        float fl = c01(age / 6f);
        buf.billboard(ctx, FLARE, VfxBlend.ADD, new Vector3f(c).add(0, 0.2f * S, 0), (1.0f + 3.4f * easeOut(fl)) * S, 0.3f + age * 0.05f, a(cream, (float) Math.pow(1 - fl, 1.4f)));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f * S, 0), (1.0f + 0.8f * easeOut(c01(age / 12f))) * S, gold, (1 - c01(age / 14f)) * 0.9f + 0.25f * patch);
        float vr = c01(age / 9f);
        buf.billboard(ctx, SHOCK, VfxBlend.ADD, new Vector3f(c).add(0, 0.3f * S, 0), 2f * (0.3f + 2.3f * easeOut(vr)) * S, age * 0.01f, a(cream, (float) Math.pow(1 - vr, 1.2f) * 0.8f));
    }

    private static float easeOut(float t) { return VfxAnim.easeOutCubic(c01(t)); }
}
