package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BlackOilFx.*;

/**
 * Black Oil Magic (an iridescent, viscous, highly explosive fluid), drawn after the owner's anime still: a huge round pool of glowing
 * magenta-red liquid, churned like cooled lava with lighter pink ridges, held in a thick, ragged, dripping black tar rim, with small white
 * candle flames floating above it and the whole scene lit magenta. Black tar bodies (ALPHA, near-black with wet highlights), the pool's own
 * light and the candle flames (ADD), and a faint iridescent oil-slick sheen on top (ADD).
 * <ul>
 *   <li>BLACK_OIL_FX1, Oil Shot (cast / projectile): a black oil orb gathers at 'from' (drops sucked in, a ring closing, one small white
 *       candle flame flickering on it), then flies to 'to' as a glossy teardrop with a pink halo, trailing a sagging viscous string that
 *       drips, with ghost afterimages and pink sparks, and splats on arrival (flash, splat, a ring). 'power' = size (the orb is about
 *       0.5 x power blocks), duration = flight ticks (default 16), colour = tint mixed into the pink.</li>
 *   <li>BLACK_OIL_FX2, Oil Pool (zone / field): the owner's still. A pool of churned magenta liquid spreads from 'from' to the radius
 *       'power' with counter-rotating pink ridges, an iridescent sheen and expanding ripples, inside a ragged black tar rim whose
 *       tendrils sway upward; four (five over radius 5.5) white candle flames float over it with halos; bubbles swell and pop and pink motes
 *       rise. Fades in over 8 ticks, out over the last 12 (default 80).</li>
 *   <li>BLACK_OIL_FX3, Oil Burst (impact / signature): the oil blows up. A short implosion, then a white-pink flash with a jagged burst, a
 *       black splat spreading on the ground (or on the plane facing 'to - from' when given), tar tendrils spouting up and collapsing, a
 *       dozen oil drops flung out on ballistic arcs, two shock rings, and white candle flames burning on the splat in the afterglow.
 *       'power' = scale, 'to - from' = the direction the spouts and drops lean to (zero = straight up), duration default 28.</li>
 * </ul>
 * Everything is a pure function of the age and the instance seed; each shape stays under the 400-vertex budget.
 */
public class BlackOilLayer extends AbstractVfxLayer {
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BLACK_OIL_FX1, VfxShape.BLACK_OIL_FX2, VfxShape.BLACK_OIL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BLACK_OIL_FX1 -> 16;
            case BLACK_OIL_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF2D9A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BLACK_OIL_FX1 -> shot(inst, ctx, buf);
            case BLACK_OIL_FX2 -> pool(inst, ctx, buf);
            case BLACK_OIL_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ================================================================== FX1: Oil Shot
    private void shot(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = Math.max(6f, inst.duration);
        if (age >= D) return;
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        long seed = inst.seed;
        int pink = mix(PINK, inst.color, 0.22f), rose = mix(ROSE, inst.color, 0.18f), hot = mix(HOT, inst.color, 0.12f);

        Vector3f f = ctx.rel(inst.from(ctx)), t = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(t).sub(f);
        float len = dir.length();
        if (len < 0.4f) { dir.set(0, 0, 1).mul(0.4f); t = new Vector3f(f).add(dir); len = 0.4f; }
        dir.div(len);
        Vector3f side = new Vector3f(dir).cross(UP);
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();

        float charge = clamp01(age / (0.25f * D));
        float u = clamp01((age - 0.2f * D) / (0.62f * D));
        float ue = 1f - (float) Math.pow(1f - u, 1.7);
        float arrive = 0.82f * D;
        float hq = ue;
        float trailFrac = Math.min(0.75f, (2.6f * P + 0.15f * len) / len);
        float tq = Math.max(0f, hq - trailFrac);
        float retract = clamp01((age - arrive) / (0.16f * D));
        tq = Mth.lerp(retract * retract, tq, 1f);
        Vector3f head = new Vector3f(f).lerp(t, hq);

        float hs = P * (0.34f + 0.16f * VfxAnim.easeOutBack(charge));
        hs *= 1f - sstep(0.82f * D, 0.93f * D, age);

        // ---- charge: drops sucked in, a closing ring, a small white flame on the orb
        if (age < 0.34f * D) {
            float ce = VfxAnim.easeOutCubic(clamp01(age / (0.28f * D)));
            float cf = 1f - sstep(0.26f * D, 0.34f * D, age);
            int n = ctx.seg(5, 3);
            for (int i = 0; i < n; i++) {
                Vector3f d0 = unit(seed, i, 3);
                float rad = P * (1.35f * (1f - ce) + 0.16f);
                Vector3f p = new Vector3f(f).add(new Vector3f(d0).mul(rad));
                Vector3f toward = new Vector3f(f).sub(p);
                sprite(buf, ctx, DROP, VfxBlend.ALPHA, p, P * 0.11f, P * 0.22f, rollToward(ctx, new Vector3f(toward).negate()), VfxVertexBuffer.withAlpha(TAR, cf * (0.4f + 0.6f * ce)));
            }
            sprite(buf, ctx, RIPPLE, VfxBlend.ADD, f, P * (2.4f * (1f - ce) + 0.6f), P * (2.4f * (1f - ce) + 0.6f), age * 0.2f, VfxVertexBuffer.withAlpha(rose, 0.8f * ce * cf));
            float fl = 0.85f + 0.15f * Mth.sin(age * 1.9f);
            int cell = ((int) (age / 2.5f)) & 1;
            Vector3f fb = new Vector3f(f).add(0, hs * 0.38f, 0);
            flame(buf, ctx, VfxBlend.ALPHA, cell, fb, P * 0.55f * fl * ce, Mth.sin(age * 0.8f) * 0.1f, VfxVertexBuffer.withAlpha(WHITE, cf));
            VfxBloom.glow(ctx, buf, new Vector3f(fb).add(0, P * 0.14f, 0), P * 0.45f, hot, 0.8f * cf * ce);
        }

        // ---- the trail: a sagging viscous string, black body + pink wet highlights
        if (age >= 0.18f * D && tq < 0.995f) {
            int n = 7;
            Vector3f[] pts = new Vector3f[n];
            float[] w = new float[n];
            int[] cb = new int[n], ca = new int[n];
            for (int i = 0; i < n; i++) {
                float s = (float) i / (n - 1);
                float q = Mth.lerp(s, hq, tq);
                Vector3f p = new Vector3f(f).lerp(t, q);
                p.y -= 0.30f * P * (float) Math.pow(s, 1.4) * (0.4f + 0.6f * u);
                p.add(new Vector3f(side).mul(0.07f * P * s * Mth.sin(age * 0.45f + s * 6.5f)));
                pts[i] = p;
                w[i] = P * Mth.lerp((float) Math.pow(s, 0.8), 0.50f, 0.07f);
                cb[i] = VfxVertexBuffer.withAlpha(TAR, Mth.lerp(s, 1f, 0.12f));
                ca[i] = VfxVertexBuffer.withAlpha(pink, Mth.lerp(s, 0.85f, 0f));
            }
            ribbon(buf, STREAK, VfxBlend.ALPHA, pts, w, cb);
            ribbon(buf, STREAK, VfxBlend.ADD, pts, w, ca);

            // ghost afterimages along the string
            for (int i = 0; i < 3; i++) {
                float s = 0.26f + 0.27f * i;
                Vector3f p = new Vector3f(f).lerp(t, Mth.lerp(s, hq, tq));
                p.y -= 0.30f * P * (float) Math.pow(s, 1.4) * (0.4f + 0.6f * u);
                float k = hs * (0.9f - 0.2f * i);
                sprite(buf, ctx, DROP, VfxBlend.ADD, p, k * 0.8f, k * 1.5f, rollToward(ctx, new Vector3f(dir).negate()),
                        VfxVertexBuffer.withAlpha(rose, 0.36f * (1f - 0.3f * i) * (1f - retract)));
            }
            // pink sparks shed by the trail
            int ns = ctx.seg(4, 2);
            for (int i = 0; i < ns; i++) {
                float s = 0.12f + 0.8f * hash(seed, i, 11);
                Vector3f p = new Vector3f(f).lerp(t, Mth.lerp(s, hq, tq));
                p.add(new Vector3f(side).mul((hash(seed, i, 12) - 0.5f) * 0.5f * P)).add(0, (hash(seed, i, 13) - 0.3f) * 0.4f * P - 0.15f * P * s, 0);
                float tw = 0.5f + 0.5f * Mth.sin(age * 1.3f + i * 2.1f);
                sprite(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, p, P * 0.16f, P * 0.16f, 0, VfxVertexBuffer.withAlpha(hot, tw * (1f - s) * (1f - retract)));
            }
        }

        // ---- drips falling off the string
        for (int i = 0; i < 4; i++) {
            float born = (0.30f + 0.12f * i + 0.04f * hash(seed, i, 21)) * D;
            float tau = age - born;
            if (tau <= 0f || tau > 9f) continue;
            float bu = clamp01((born - 0.2f * D) / (0.62f * D));
            float bq = 1f - (float) Math.pow(1f - bu, 1.7f);
            Vector3f p = new Vector3f(f).lerp(t, Math.max(0f, bq - 0.04f * i));
            p.add(0, -0.012f * tau * tau * P - 0.08f * P, 0).add(new Vector3f(side).mul((hash(seed, i, 22) - 0.5f) * 0.2f * P));
            float sz = P * (0.15f + 0.07f * hash(seed, i, 23));
            sprite(buf, ctx, DROP, VfxBlend.ALPHA, p, sz * 0.5f, sz, 0, VfxVertexBuffer.withAlpha(TAR, 1f - tau / 9f));
        }

        // ---- the head: a glossy teardrop with a pink halo
        if (hs > 0.02f) {
            float roll = rollToward(ctx, new Vector3f(dir).negate());          // the tip trails behind
            VfxBloom.glow(ctx, buf, head, hs * 1.15f, pink, 0.85f);
            sprite(buf, ctx, DROP, VfxBlend.ALPHA, head, hs * 0.95f, hs * 1.75f, roll, TAR);
            sprite(buf, ctx, DROP, VfxBlend.ADD, head, hs * 0.95f, hs * 1.75f, roll, VfxVertexBuffer.withAlpha(rose, 0.75f));
        }

        // ---- impact at 'to': flash, splat, ring
        float ik = clamp01((age - 0.78f * D) / (0.22f * D));
        if (ik > 0f) {
            float eo = VfxAnim.easeOutCubic(ik), fa = 1f - ik;
            float sp = P * (0.7f + 1.5f * eo);
            sprite(buf, ctx, SPLAT, VfxBlend.ALPHA, t, sp, sp, seed * 0.001f % 6.28f, VfxVertexBuffer.withAlpha(TAR, 1f - 0.5f * ik));
            sprite(buf, ctx, SPLAT, VfxBlend.ADD, t, sp, sp, seed * 0.001f % 6.28f, VfxVertexBuffer.withAlpha(rose, 0.7f * fa));
            sprite(buf, ctx, BURST, VfxBlend.ADD, t, P * (0.8f + 1.8f * eo), P * (0.8f + 1.8f * eo), age * 0.1f, VfxVertexBuffer.withAlpha(hot, fa * 0.9f));
            sprite(buf, ctx, RIPPLE, VfxBlend.ADD, t, P * (0.6f + 2.8f * eo), P * (0.6f + 2.8f * eo), 0, VfxVertexBuffer.withAlpha(pink, fa * 0.8f));
            VfxBloom.glow(ctx, buf, t, P * 0.7f * (1f - 0.5f * ik), hot, 0.9f * fa);
        }
    }

    // ================================================================== FX2: Oil Pool
    private void pool(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        float R = Mth.clamp(inst.power, 0.6f, 24f);
        long seed = inst.seed;
        int pink = mix(PINK, inst.color, 0.22f), poolCol = mix(POOL_COL, inst.color, 0.18f), rose = mix(ROSE, inst.color, 0.18f), hot = mix(HOT, inst.color, 0.12f);
        Vector3f g = ctx.rel(inst.from(ctx));
        float spread = VfxAnim.easeOutCubic(clamp01(age / 10f));
        float rr = R * spread;
        float rimR = R * VfxAnim.easeOutCubic(clamp01((age - 2f) / 10f));
        if (rr < 0.05f) return;
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.22f);
        VfxPose floor = VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0));
        float cs = 0.26f + 0.05f * Math.min(R, 8f);                        // candle size

        // ---- the pool: churned liquid, then its own light, ridges counter-rotating in two scales, the iridescent sheen
        flat(buf, POOL, VfxBlend.ALPHA, floor, rr * 1.13f, age * 0.004f, VfxVertexBuffer.withAlpha(poolCol, 0.97f * fade));
        // the tar rim (ALPHA body, then the wet pink highlights)
        float rimHalf = rimR / 0.84f;
        flat(buf, RIM, VfxBlend.ALPHA, floor.lift(0.012f), rimHalf, age * 0.0015f, VfxVertexBuffer.withAlpha(TAR, fade));

        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, floor.lift(-0.01f), rr * 1.45f, VfxVertexBuffer.withAlpha(pink, (0.42f + 0.14f * pulse) * fade));
        flat(buf, POOL, VfxBlend.ADD, floor.lift(0.004f), rr * 1.13f, age * 0.004f, VfxVertexBuffer.withAlpha(pink, 0.30f * fade));
        flat(buf, RIDGES, VfxBlend.ADD, floor.lift(0.008f), rr * 1.04f, -age * 0.007f, VfxVertexBuffer.withAlpha(rose, 0.95f * fade));
        flat(buf, RIDGES, VfxBlend.ADD, floor.lift(0.010f), rr * 0.78f, age * 0.0045f + 2.1f, VfxVertexBuffer.withAlpha(hot, 0.5f * fade));
        flat(buf, SHEEN, VfxBlend.ADD, floor.lift(0.014f), rr * 0.98f, -age * 0.006f + 1.3f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, (0.26f + 0.08f * pulse) * fade));
        flat(buf, RIM, VfxBlend.ADD, floor.lift(0.016f), rimHalf, age * 0.0015f, VfxVertexBuffer.withAlpha(pink, (0.55f + 0.30f * pulse) * fade));

        // ---- ripples travelling over the surface
        for (int k = 0; k < 2; k++) {
            float ph = fract(age / 38f + k * 0.5f + 0.13f * hash(seed, 0, 5));
            float rad = rr * (0.10f + 0.84f * ph);
            flat(buf, RIPPLE, VfxBlend.ADD, floor.lift(0.02f), rad / 0.70f, k * 1.7f, VfxVertexBuffer.withAlpha(rose, 0.65f * (float) Math.sin(Math.PI * ph) * fade));
        }

        // ---- tar tendrils swaying up from the rim
        int nt = ctx.seg(12, 6);
        float hMax = 0.40f + 0.55f * Math.min(R, 8f) / 8f;
        float grow = VfxAnim.easeOutCubic(clamp01((age - 3f) / 10f));
        for (int i = 0; i < nt; i++) {
            float a = Mth.TWO_PI * (i + 0.5f * hash(seed, i, 31)) / nt;
            float rad = rimR * (0.97f + 0.07f * hash(seed, i, 32));
            Vector3f base = new Vector3f(g).add(Mth.cos(a) * rad, 0.07f, Mth.sin(a) * rad);
            float h = hMax * (0.45f + 0.55f * hash(seed, i, 33)) * grow * (0.84f + 0.16f * Mth.sin(age * 0.13f + i * 1.7f));
            float sway = Mth.sin(age * 0.1f + i * 2.3f) * 0.16f * h;
            Vector3f lean = new Vector3f(-Mth.sin(a) * sway + Mth.cos(a) * 0.12f * h, 0, Mth.cos(a) * sway + Mth.sin(a) * 0.12f * h);
            standing(buf, TENDRIL, VfxBlend.ALPHA, base, UP, h, h * 0.30f, lean, VfxVertexBuffer.withAlpha(TAR, fade),
                    VfxVertexBuffer.withAlpha(mix(TAR, pink, 0.25f), 0.9f * fade));
        }

        // ---- four (five) candles floating over the pool, white flames with halos
        int nc = R >= 5.5f ? 5 : 4;
        float S = cs * 1.3f;
        for (int i = 0; i < nc; i++) {
            float a = 0.5f + Mth.TWO_PI * i / nc + 0.5f * hash(seed, i, 41) + age * 0.0016f;
            float d = rr * (0.40f + 0.14f * hash(seed, i, 42));
            float bob = 0.05f * Mth.sin(age * 0.08f + i * 2.0f);
            Vector3f pos = new Vector3f(g).add(Mth.cos(a) * d, 0.10f + cs * 0.35f + bob, Mth.sin(a) * d);
            float fl = 0.9f + 0.1f * Mth.sin(age * 1.7f + i * 2.6f) + 0.05f * Mth.sin(age * 3.1f + i);
            int cell = ((int) ((age + i * 2f) / 3f)) & 1;
            float fh = cs * 2.9f * fl * (0.4f + 0.6f * clamp01((age - 4f - i * 1.5f) / 8f));
            Vector3f fb = new Vector3f(pos).add(0, S * 0.70f, 0);
            Vector3f spot = new Vector3f(g).add(pos.x - g.x, 0.07f, pos.z - g.z);
            buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(spot), cs * 2.4f, VfxVertexBuffer.withAlpha(hot, 0.45f * fade * fl));
            sprite(buf, ctx, CANDLE, VfxBlend.ALPHA, new Vector3f(pos).add(0, S * 0.45f, 0), S, S, 0, VfxVertexBuffer.withAlpha(TAR, fade));
            flame(buf, ctx, VfxBlend.ALPHA, cell, fb, fh, Mth.sin(age * 0.7f + i) * 0.07f, VfxVertexBuffer.withAlpha(WHITE, fade));
            flame(buf, ctx, VfxBlend.ADD, cell, fb, fh * 1.04f, Mth.sin(age * 0.7f + i) * 0.07f, VfxVertexBuffer.withAlpha(hot, 0.5f * fade));
            VfxBloom.glow(ctx, buf, new Vector3f(fb).add(0, fh * 0.35f, 0), fh * 0.55f, mix(hot, pink, 0.4f), 0.8f * fade * fl);
        }

        // ---- bubbles swelling and bursting, motes rising
        int nb = ctx.seg(6, 3);
        for (int i = 0; i < nb; i++) {
            float a = hash(seed, i, 51) * Mth.TWO_PI, d = Mth.sqrt(hash(seed, i, 52)) * rr * 0.80f;
            float period = 28f + 40f * hash(seed, i, 53);
            float ph = fract(age / period + hash(seed, i, 54));
            float sz = (0.14f + 0.20f * hash(seed, i, 55)) * (0.7f + 0.3f * Math.min(R, 8f) / 8f) * (0.4f + 0.6f * sstep(0f, 0.6f, ph));
            float pop = sstep(0.84f, 1f, ph);
            Vector3f p = new Vector3f(g).add(Mth.cos(a) * d, 0.08f + sz * 0.45f, Mth.sin(a) * d);
            sprite(buf, ctx, BUBBLE, VfxBlend.ADD, p, sz * (1f + 0.9f * pop), sz * (1f + 0.9f * pop), 0, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.95f * sstep(0f, 0.12f, ph) * (1f - pop) * fade));
        }
        int nm = ctx.seg(6, 3);
        for (int i = 0; i < nm; i++) {
            float a = hash(seed, i, 61) * Mth.TWO_PI, d = Mth.sqrt(hash(seed, i, 62)) * rr * 0.85f;
            float ph = fract(age / (36f + 30f * hash(seed, i, 63)) + hash(seed, i, 64));
            Vector3f p = new Vector3f(g).add(Mth.cos(a) * d + 0.15f * Mth.sin(age * 0.09f + i), 0.15f + ph * (0.8f + 0.2f * Math.min(R, 6f)), Mth.sin(a) * d + 0.15f * Mth.cos(age * 0.07f + i));
            float sz = 0.10f + 0.12f * hash(seed, i, 65);
            sprite(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, p, sz, sz, 0, VfxVertexBuffer.withAlpha(hot, (float) Math.sin(Math.PI * ph) * 0.9f * fade));
        }
    }

    // ================================================================== FX3: Oil Burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = Math.max(8f, inst.duration);
        if (age >= D) return;
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        float k = age / D;
        long seed = inst.seed;
        int pink = mix(PINK, inst.color, 0.22f), rose = mix(ROSE, inst.color, 0.18f), hot = mix(HOT, inst.color, 0.12f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vec3 h = inst.to(ctx).subtract(inst.from(ctx));
        boolean aimed = h.lengthSqr() > 0.04;
        Vector3f n = aimed ? new Vector3f((float) h.x, (float) h.y, (float) h.z).normalize() : new Vector3f(UP);
        float tAnt = 0.12f;
        float kk = clamp01((k - tAnt) / (1f - tAnt));              // 0 at the blast, 1 at the end
        float out = 1f - sstep(0.78f, 1f, k);                      // global fade
        VfxPose floor = VfxPose.facing(new Vector3f(c).add(new Vector3f(n).mul(0.05f)), n);

        // ---- anticipation: the oil sucks in and darkens
        if (k < tAnt + 0.03f) {
            float ak = clamp01(k / tAnt);
            float sz = P * 0.9f * (1f - 0.45f * ak);
            sprite(buf, ctx, SPLAT, VfxBlend.ALPHA, c, sz, sz, age * 0.3f, VfxVertexBuffer.withAlpha(TAR, 0.95f * (1f - sstep(tAnt, tAnt + 0.03f, k))));
            sprite(buf, ctx, RIPPLE, VfxBlend.ADD, c, P * (3.0f * (1f - ak) + 0.6f), P * (3.0f * (1f - ak) + 0.6f), 0, VfxVertexBuffer.withAlpha(pink, 0.9f * ak));
            VfxBloom.glow(ctx, buf, c, P * 0.6f * ak, pink, 0.9f * ak);
        }
        if (k < tAnt) return;

        float eo = VfxAnim.easeOutCubic(clamp01(kk * 2.2f));

        // ---- the splat on the ground (or on the plane facing the hint)
        float splat = P * (0.5f + 1.7f * VfxAnim.easeOutCubic(clamp01(kk * 1.6f)));
        flat(buf, SPLAT, VfxBlend.ALPHA, floor, splat, seed * 0.0013f % 6.28f, VfxVertexBuffer.withAlpha(TAR, out));
        flat(buf, SPLAT, VfxBlend.ADD, floor.lift(0.006f), splat, seed * 0.0013f % 6.28f, VfxVertexBuffer.withAlpha(rose, 0.8f * (1f - sstep(0f, 0.8f, kk)) * out));
        flat(buf, SHEEN, VfxBlend.ADD, floor.lift(0.01f), splat * 0.9f, -age * 0.03f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.28f * out * sstep(0.05f, 0.3f, kk)));

        // ---- tar spouts: tendrils shooting up from the splat, then collapsing
        int ns = ctx.seg(6, 4);
        float rise = VfxAnim.easeOutCubic(clamp01(kk / 0.30f)) * (1f - sstep(0.42f, 0.80f, kk));
        for (int i = 0; i < ns; i++) {
            float a = Mth.TWO_PI * (i + hash(seed, i, 71) * 0.6f) / ns;
            float rad = P * (0.18f + 0.55f * hash(seed, i, 72));
            Vector3f base = floor.point(Mth.cos(a) * rad, Mth.sin(a) * rad);
            float hh = P * (0.9f + 1.2f * hash(seed, i, 73)) * rise;
            if (hh < 0.05f) continue;
            Vector3f lean = floor.point(Mth.cos(a) * hh * 0.35f, Mth.sin(a) * hh * 0.35f).sub(floor.origin());
            if (aimed) lean.add(new Vector3f(n).mul(hh * 0.1f));
            standing(buf, TENDRIL, VfxBlend.ALPHA, base, n, hh, hh * 0.28f, lean, VfxVertexBuffer.withAlpha(TAR, out), VfxVertexBuffer.withAlpha(mix(TAR, pink, 0.3f), out));
        }

        // ---- the flash: a jagged white-pink burst over a hot core, two shock rings
        float flash = 1f - sstep(0f, 0.38f, kk);
        if (flash > 0.01f) {
            float bs = P * (1.4f + 3.2f * eo);
            sprite(buf, ctx, BURST, VfxBlend.ADD, c, bs, bs, 0.4f, VfxVertexBuffer.withAlpha(hot, flash));
            sprite(buf, ctx, BURST, VfxBlend.ADD, c, bs * 0.7f, bs * 0.7f, -0.2f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, flash * 0.8f));
            VfxBloom.glow(ctx, buf, c, P * 1.1f * (1f - 0.4f * kk), hot, 1.1f * flash);
        }
        float r1 = clamp01(kk * 1.35f), r2 = clamp01((kk - 0.10f) * 1.35f);
        flat(buf, RIPPLE, VfxBlend.ADD, floor.lift(0.02f), P * (0.5f + 3.6f * VfxAnim.easeOutCubic(r1)) / 0.7f, 0.5f, VfxVertexBuffer.withAlpha(pink, (1f - r1) * 0.9f));
        if (r2 > 0f) flat(buf, RIPPLE, VfxBlend.ADD, floor.lift(0.022f), P * (0.4f + 2.4f * VfxAnim.easeOutCubic(r2)) / 0.7f, 1.9f, VfxVertexBuffer.withAlpha(rose, (1f - r2) * 0.7f));

        // ---- oil drops flung out on ballistic arcs (the tip trails behind)
        int nd = ctx.seg(14, 7);
        float tau = Math.max(0f, age - tAnt * D);
        for (int i = 0; i < nd; i++) {
            Vector3f v = unit(seed, i, 81);
            v.y = Math.abs(v.y) * 0.8f + 0.2f;                       // mostly upward / outward
            if (aimed) v.add(new Vector3f(n).mul(0.9f)); else v.add(0, 0.55f, 0);
            v.normalize().mul((0.10f + 0.16f * hash(seed, i, 82)) * (float) Math.pow(P, 0.7));
            Vector3f p = new Vector3f(c).add(new Vector3f(v).mul(tau)).sub(0, 0.022f * tau * tau * 0.5f * (float) Math.pow(P, 0.5), 0);
            Vector3f vel = new Vector3f(v).sub(0, 0.022f * tau * (float) Math.pow(P, 0.5), 0);
            float sz = P * (0.16f + 0.22f * hash(seed, i, 83));
            float roll = rollToward(ctx, new Vector3f(vel).negate());
            float a = 1f - sstep(0.62f, 1f, k);
            sprite(buf, ctx, DROP, VfxBlend.ALPHA, p, sz * 0.55f, sz * 1.05f, roll, VfxVertexBuffer.withAlpha(TAR, a));
            if ((i & 1) == 0) sprite(buf, ctx, DROP, VfxBlend.ADD, p, sz * 0.55f, sz * 1.05f, roll, VfxVertexBuffer.withAlpha(rose, 0.6f * a));
        }

        // ---- the afterglow: pink light on the splat and white candle flames burning on it
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, floor.lift(-0.01f), P * 2.5f * (0.7f + 0.3f * eo), VfxVertexBuffer.withAlpha(pink, 0.55f * (1f - sstep(0.15f, 1f, k))));
        int nf = ctx.seg(5, 3);
        for (int i = 0; i < nf; i++) {
            float start = 0.14f + 0.05f * i + 0.08f * hash(seed, i, 91);
            float g = clamp01((k - start) / 0.12f);
            if (g <= 0f) continue;
            float a = Mth.TWO_PI * (i + hash(seed, i, 92) * 0.5f) / nf;
            float rad = P * (0.20f + 0.50f * hash(seed, i, 93));
            Vector3f base = floor.point(Mth.cos(a) * rad, Mth.sin(a) * rad);
            float fl = 0.88f + 0.12f * Mth.sin(age * 1.8f + i * 2.4f);
            float fh = P * 0.78f * fl * VfxAnim.easeOutCubic(g) * (1f - sstep(0.75f, 1f, k));
            int cell = ((int) ((age + i * 2f) / 3f)) & 1;
            flame(buf, ctx, VfxBlend.ALPHA, cell, base, fh, Mth.sin(age * 0.7f + i) * 0.08f, VfxVertexBuffer.withAlpha(WHITE, 1f - sstep(0.8f, 1f, k)));
            VfxBloom.glow(ctx, buf, new Vector3f(base).add(0, fh * 0.5f, 0), fh * 0.6f, mix(hot, pink, 0.4f), 0.75f * (1f - sstep(0.75f, 1f, k)));
        }
    }
}
