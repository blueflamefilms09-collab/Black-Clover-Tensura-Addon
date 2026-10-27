package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's Death Tornado: a dark funnel walking over the ground, built from the wind textures of the Wind Tornado but in charcoal, ash grey and deep violet.
 * <ul>
 *   <li>the funnel: four stacked, ragged, spinning hoops of dark wind, narrow at the foot and wide at the top (about 2.5 x the radius high), a smoky core behind them, a
 *       storm-cloud lid on top and pale ash streaks spiralling up its walls;</li>
 *   <li>debris (stone chunks, planks, ash flakes) orbiting upward in spirals, a dark stain and a ring of kicked-up dust on the ground;</li>
 *   <li>a few thin crimson lightning bolts that flicker inside the funnel (three slots, each lit for under two ticks of every ten).</li>
 * </ul>
 * The server re-sends the effect every 10 ticks while the tornado walks and each copy lives 14 ticks, so it fades in over ages 0-3 and out over 11-14, and everything that moves is
 * periodic in 10 ticks (the phase is {@code age / 10} turns, no seed in the structure): an old and a new copy overlapping are the same funnel in the same pose and read as one
 * continuous tornado. Only the lightning picks its places from the seed. {@code inst.power} = the radius at the top (~5.5), the foot is the point 'from'.
 * Worst case about 376 of the 400 vertices (all quads), constant loop counts, no randomness except the seed hash; {@link VfxRenderContext#seg} thins the optional debris / dust.
 */
public class VeldTornadoLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation BAND = t("wind_band"), STREAK = t("wind_streak"), CLOUD = t("storm_cloud"), SMOKE = t("smoke_billow"), DUST = t("earth_dust"),
            STONE = t("earthfam_rock_chunk"), PLANK = t("earth_chunk"), FLAKE = t("ash_flake"), BOLT = t("lightningfx_bolt"), RING = t("ash_ring"), GLOW = t("lightningfx_spark");

    private static final int CHARCOAL = 0xFF25232B, SHADOW = 0xFF382B4D, SLATE = 0xFF3B3944, VIOLET = 0xFF2E2342, ASH = 0xFFB9B7C2, DUSTC = 0xFF8B8794, CRIMSON = 0xFFFF2236, HOT = 0xFFFF7A86;
    private static final int RINGS = 4, SEGS = 10, PUFFS = 4, CORE = 8, DEBRIS = 10, DUSTS = 6, STREAKS = 2, SLOTS = 3;
    private static final float[] RING_H = {0.12f, 0.36f, 0.62f, 0.88f}, RING_HW = {0.17f, 0.2f, 0.2f, 0.2f};
    private static final int[] RING_COL = {0xFF45414F, 0xFF3A2F54, 0xFF53505E, 0xFF40335C};
    private static final int[] RING_WRAP = {3, 3, 3, 3};                    // wraps of the texture per 10 ticks (3 repeats round the hoop: one turn in 10 ticks)
    private static final float PERIOD = 10f;

    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f(), tmp = new Vector3f(), tmp2 = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_TORNADO); }
    @Override public int defaultDuration(VfxShape s) { return 14; }
    @Override public int defaultColor(VfxShape s) { return 0xFF2E2342; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static float smooth(float x) { return x * x * (3f - 2f * x); }

    /** Funnel radius at height fraction h (0 foot, 1 top). */
    private static float radiusAt(float R, float h) { return R * (0.13f + 0.87f * (float) Math.pow(h, 1.55)); }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, tmp.set(x, y, z), size, rot, col);
    }

    /** One ragged hoop of the funnel: bottom and top radius from the profile, wobbling with a pattern that rotates one turn per ten ticks. */
    private void hoop(VfxVertexBuffer buf, int k, float cx, float cy, float cz, float rBot, float rTop, float hh, float phase, float rag, int col) {
        float scroll = RING_WRAP[k] * phase;
        for (int i = 0; i < SEGS; i++) {
            if (!buf.hasBudget(4)) return;
            float ang0 = Mth.TWO_PI * i / SEGS, ang1 = Mth.TWO_PI * (i + 1) / SEGS;
            float j0 = 1f + rag * (0.55f * Mth.sin(3f * ang0 - Mth.TWO_PI * 3f * phase + k * 2.1f) + 0.45f * (hash(7, k, i % SEGS) - 0.5f) * 2f);
            float j1 = 1f + rag * (0.55f * Mth.sin(3f * ang1 - Mth.TWO_PI * 3f * phase + k * 2.1f) + 0.45f * (hash(7, k, (i + 1) % SEGS) - 0.5f) * 2f);
            float c0 = Mth.cos(ang0), s0 = Mth.sin(ang0), c1 = Mth.cos(ang1), s1 = Mth.sin(ang1);
            float u0 = scroll + 3f * i / SEGS, u1 = scroll + 3f * (i + 1) / SEGS;
            p0.set(cx + c0 * rBot * j0, cy - hh, cz + s0 * rBot * j0);
            p1.set(cx + c1 * rBot * j1, cy - hh, cz + s1 * rBot * j1);
            p2.set(cx + c1 * rTop * j1, cy + hh, cz + s1 * rTop * j1);
            p3.set(cx + c0 * rTop * j0, cy + hh, cz + s0 * rTop * j0);
            buf.quad(BAND, VfxBlend.ALPHA, p0, p1, p2, p3, u0, 0f, u1, 1f, col, col);
        }
    }

    private void bolt(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f a, Vector3f b, Vector3f c, float w, int col, float alpha) {
        if (alpha <= 0.02f) return;
        buf.beam(ctx, BOLT, VfxBlend.ADD, a, b, w * 0.8f, w, 1, 0f, a(col, alpha), a(col, alpha));
        buf.beam(ctx, BOLT, VfxBlend.ADD, b, c, w, w * 0.6f, 1, 0f, a(col, alpha), a(col, alpha));
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = smooth(sub(age, 0f, 3f)) * (1f - smooth(sub(age, 11f, 14f)));
        if (fade <= 0.01f) return;
        float R = Math.max(1.5f, inst.power), H = 2.55f * R;
        float phase = age / PERIOD;                                    // turns of the periodic motion; the same pose again every 10 ticks
        float tau = Mth.TWO_PI * phase;
        long seed = inst.seed;
        Vector3f g = ctx.rel(inst.from(ctx));

        // the funnel leans a little as it goes, periodic like everything else
        float lean = 0.07f * R;

        // ---- ground: a dark stain under the foot and the ring of dust it kicks up
        if (buf.hasBudget(4)) buf.plane(RING, VfxBlend.ALPHA, VfxPose.ground(tmp2.set(g.x, g.y + 0.06f, g.z)).spin(tau * 0.5f), R * 0.95f, a(CHARCOAL, 0.55f * fade));
        if (buf.hasBudget(4)) buf.plane(DUST, VfxBlend.ALPHA, VfxPose.ground(tmp2.set(g.x, g.y + 0.08f, g.z)).spin(-tau * 0.5f), R * 0.75f, a(VIOLET, 0.55f * fade));
        int nd = ctx.seg(DUSTS, 4);
        for (int k = 0; k < nd; k++) {
            float ang = Mth.TWO_PI * k / nd + tau, rr = R * (0.45f + 0.4f * hash(5, k, 1));
            float lift = 0.5f + 0.5f * hash(5, k, 2) + 0.35f * Mth.sin(tau * 2f + k * 1.7f);
            sprite(ctx, buf, DUST, VfxBlend.ALPHA, g.x + Mth.cos(ang) * rr, g.y + lift, g.z + Mth.sin(ang) * rr, R * (0.55f + 0.3f * hash(5, k, 3)), tau * 2f * (k % 2 == 0 ? 1 : -1) + k,
                    a(k % 3 == 0 ? DUSTC : SLATE, 0.6f * fade));
        }

        // ---- the smoky core behind the walls, so the funnel is never see-through
        for (int k = 0; k < CORE; k++) {
            float h = 0.08f + 0.12f * k, rr = radiusAt(R, h), orb = tau + k * 2.4f;
            float cx = g.x + Mth.sin(tau + 2.2f * h) * lean * h + Mth.cos(orb) * rr * 0.4f, cz = g.z + Mth.cos(tau + 2.2f * h) * lean * h + Mth.sin(orb) * rr * 0.4f;
            sprite(ctx, buf, SMOKE, VfxBlend.ALPHA, cx, g.y + H * h, cz, rr * 2.1f + 1.3f, tau * 1.5f + k * 1.3f, a(k % 2 == 0 ? CHARCOAL : VIOLET, 0.9f * fade));
        }

        // ---- the walls: four ragged hoops of dark wind
        for (int k = 0; k < RINGS; k++) {
            float hc = RING_H[k], hw = RING_HW[k];
            float lo = Math.max(0f, hc - hw), hi = Math.min(1.04f, hc + hw);
            float cx = g.x + Mth.sin(tau + 2.2f * hc) * lean * hc, cz = g.z + Mth.cos(tau + 2.2f * hc) * lean * hc;
            hoop(buf, k, cx, g.y + H * (lo + hi) * 0.5f, cz, radiusAt(R, lo), radiusAt(R, hi), H * (hi - lo) * 0.5f, phase, 0.13f + 0.04f * hc, a(RING_COL[k], (0.92f - 0.05f * k) * fade));
        }

        // ---- pale ash streaks winding up the walls
        for (int i = 0; i < STREAKS; i++) {
            float a0 = tau + Mth.TWO_PI * i / STREAKS;
            Vector3f prev = null;
            for (int k = 0; k <= 4; k++) {
                float f = 0.06f + 0.9f * k / 4f, rad = radiusAt(R, f) * 1.08f, ang = a0 + f * 3.4f;
                Vector3f q = new Vector3f(g.x + Mth.sin(tau + 2.2f * f) * lean * f + Mth.cos(ang) * rad, g.y + H * f, g.z + Mth.cos(tau + 2.2f * f) * lean * f + Mth.sin(ang) * rad);
                if (prev != null && buf.hasBudget(4)) ElementFx.streak(buf, ctx, STREAK, VfxBlend.ALPHA, prev, q, 0.22f * R * (0.5f + f), a(ASH, 0.75f * fade));
                prev = q;
            }
        }

        // ---- debris: stone, planks and flakes carried up in spirals, fading in at the ground and out at the top
        int nb = ctx.seg(DEBRIS, 6);
        for (int k = 0; k < nb; k++) {
            float tk = (float) ((k * 0.61803 + phase) % 1.0), f = 0.04f + 0.96f * tk;
            float ang = tau + 5.0f * k + f * 6.5f, rad = radiusAt(R, f) * (1.0f + 0.35f * hash(9, k, 1));
            float vis = sub(tk, 0f, 0.1f) * (1f - sub(tk, 0.86f, 1f));
            ResourceLocation tex = k % 3 == 0 ? PLANK : (k % 3 == 1 ? STONE : FLAKE);
            float size = (0.55f + 0.5f * hash(9, k, 2)) * (0.7f + 0.5f * f) * Math.min(1.4f, R / 4f);
            sprite(ctx, buf, tex, VfxBlend.ALPHA, g.x + Mth.sin(tau + 2.2f * f) * lean * f + Mth.cos(ang) * rad, g.y + H * f + 0.3f, g.z + Mth.cos(tau + 2.2f * f) * lean * f + Mth.sin(ang) * rad,
                    size, tau * 3f * (k % 2 == 0 ? 1 : -1) + k * 2.3f, a(tex == PLANK ? 0xFF4A3F3A : (tex == STONE ? 0xFF8A8794 : 0xFF2A2830), 0.95f * vis * fade));
        }

        // ---- the storm lid on top
        for (int k = 0; k < PUFFS; k++) {
            float ang = Mth.TWO_PI * k / PUFFS - tau * 0.6f, rr = k == 0 ? 0f : R * (0.32f + 0.2f * hash(3, k, 1));
            sprite(ctx, buf, CLOUD, VfxBlend.ALPHA, g.x + Mth.sin(tau + 2.2f) * lean + Mth.cos(ang) * rr, g.y + H * 0.97f + 0.2f * k + 0.3f * Mth.sin(tau * 2f + k), g.z + Mth.cos(tau + 2.2f) * lean + Mth.sin(ang) * rr,
                    R * (1.25f + 0.3f * hash(3, k, 2)), 0.3f * k + tau * 0.3f, a(k % 2 == 0 ? CHARCOAL : SHADOW, 0.9f * fade));
        }

        // ---- red lightning flickering inside: three slots, each lit for about 1.6 ticks of every 10
        for (int j = 0; j < SLOTS; j++) {
            float slotAt = 3.6f + 2.4f * j + (j == 1 ? 0.4f : 0f);           // ages 3.6, 6.4, 8.4: past the fade-in, before the fade-out
            float dt = age - slotAt;
            if (dt < 0f || dt > 1.6f) continue;
            float flick = dt < 0.5f ? 1f : (dt < 0.8f ? 0.25f : (1f - sub(dt, 0.8f, 1.6f)));
            float ang = hash(seed, j, 1) * Mth.TWO_PI, h0 = 0.25f + 0.4f * hash(seed, j, 2), h1 = h0 + 0.22f, h2 = h1 + 0.2f;
            float r0 = radiusAt(R, h0) * 0.55f, r1 = radiusAt(R, h1) * (0.15f + 0.5f * hash(seed, j, 3)), r2 = radiusAt(R, h2) * 0.6f;
            float side = hash(seed, j, 4) < 0.5f ? 1f : -1f;
            Vector3f pa = new Vector3f(g.x + Mth.cos(ang) * r0, g.y + H * h0, g.z + Mth.sin(ang) * r0);
            Vector3f pb = new Vector3f(g.x + Mth.cos(ang + 0.5f * side) * r1, g.y + H * h1, g.z + Mth.sin(ang + 0.5f * side) * r1);
            Vector3f pc = new Vector3f(g.x + Mth.cos(ang - 0.3f * side) * r2, g.y + H * h2, g.z + Mth.sin(ang - 0.3f * side) * r2);
            float al = flick * fade;
            bolt(ctx, buf, pa, pb, pc, 1.5f * Math.min(1.3f, R / 4f), CRIMSON, 1f * al);
            sprite(ctx, buf, GLOW, VfxBlend.ADD, pb.x, pb.y, pb.z, R * 0.9f, 0f, a(CRIMSON, 0.7f * al));
        }
    }
}
