package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 ULT_ALL_CRUSH, Gildarts Clive's "All Crush" (Fairy Tail, Crash Magic): everything that is cracked comes apart together.
 * Palette of its own: white-hot, cyan-white crack edges, glass grey, cracked-reality black, rubble brown. Not a fist and not a ring: a fracture network.
 * <ul>
 *   <li>ticks 0-6 build-up: a white-hot pin of light at the feet, a ring of pressure that CONTRACTS onto it, black-and-white fracture planes and jagged fissures
 *       creeping out across the whole radius, a disc of cracked glass-space opening in the air facing the camera and splitting into a web, chunks of ground
 *       trembling and lifting.</li>
 *   <li>ticks 6-12 peak: the pressure ripple runs out over the radius, everything cracked shatters at once (from the middle to the rim): the disc bursts into
 *       glass shards and dust, the ground chunks and chips fly, a white flash and a flare, sparks, the dust column rises.</li>
 *   <li>ticks 12-20 aftermath: shards and rubble fall, the dust column and rim dust drift and thin out, the cracks cool from white to ghost black and fade.</li>
 * </ul>
 * Cost (worst case, detail 1): about 360 vertices (90 quads), every count constant, no state kept, all from seed and age; far players are cut by the manager's LOD.
 */
public class UltAllCrushLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation GROUND_CRACK = t("bfx_ground_crack"), GRAVITY_CRACK = t("gravity_crack"), ROCK_CRACK = t("earthfam_rock_crack"),
            CORUNDUM_CRACK = t("corundum_crack"), GLASS_CRACK = t("glass_crack"), GLASS_WEB = t("bfx_glass_web"), FISSURE = t("earth_crack"),
            ICE_FACETS = t("icefx_crack"), GLOW = t("glow"), FLARE = t("darkfx_flare"), SHOCK = t("darkfx_shock"), GLASS_SHARD = t("glass_shard"),
            GLASS_DUST = t("glass_dust"), CHUNK = t("earth_chunk"), STONE = t("earthfam_stone_shard"), DUST_PUFF = t("earth_dust"), GLINT = t("glint_0");
    private static final ResourceLocation[] CHIPS = {t("earth_chip_0"), t("earth_chip_1"), t("earth_chip_2"), t("earth_chip_3"), t("earth_chip_4"), t("earth_chip_5")};

    private static final int WHITE = 0xFFFFFFFF, CYAN = 0xFFA6F1FF, EDGE = 0xFFD2F7FF, GLASS = 0xFFB9C2CC, VOID = 0xFF04050A, RUBBLE = 0xFFE3D2BC, DUST_LO = 0xFF9C8B76, DUST_HI = 0xFFD9D2C8;

    /** the moment everything cracked lets go (ticks) */
    private static final float HIT = 8.8f;

    private static final Vector3f A = new Vector3f(), B = new Vector3f(), C = new Vector3f(), D = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_ALL_CRUSH); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFBFEFFF; }

    // ---------------------------------------------------------------------------------------------------- helpers
    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, cl(alpha)); }

    /** a flat square on the ground, turned by rot */
    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float cx, float cy, float cz, float half, float rot, int col) {
        if (half < 0.05f || (col >>> 24) < 3 || !buf.hasBudget(4)) return;
        float c = Mth.cos(rot) * half, s = Mth.sin(rot) * half;
        A.set(cx - c + s, cy, cz - s - c);
        B.set(cx + c + s, cy, cz + s - c);
        C.set(cx + c - s, cy, cz + s + c);
        D.set(cx - c - s, cy, cz - s + c);
        int g = bl.grade(col, 1f);
        buf.quad(tex, bl, A, B, C, D, 0f, 0f, 1f, 1f, g, g);
    }

    /** a flat ribbon on the ground from (ax,az) to (bx,bz): U along the length, V across */
    private static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float ax, float az, float bx, float bz, float y, float width, int col) {
        float dx = bx - ax, dz = bz - az, len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 0.15f || (col >>> 24) < 3 || !buf.hasBudget(4)) return;
        float sx = -dz / len * width * 0.5f, sz = dx / len * width * 0.5f;
        A.set(ax - sx, y, az - sz);
        B.set(bx - sx, y, bz - sz);
        C.set(bx + sx, y, bz + sz);
        D.set(ax + sx, y, az + sz);
        int g = bl.grade(col, 1f);
        buf.quad(tex, bl, A, B, C, D, 0f, 0f, 1f, 1f, g, g);
    }

    /** a camera-facing w x h card at (x,y,z), turned by rot */
    private static void card(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float w, float h, float rot, int col) {
        if (w < 0.02f || (col >>> 24) < 3 || !buf.hasBudget(4)) return;
        float cs = Mth.cos(rot), sn = Mth.sin(rot);
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rx = (r.x * cs + u.x * sn) * w * 0.5f, ry = (r.y * cs + u.y * sn) * w * 0.5f, rz = (r.z * cs + u.z * sn) * w * 0.5f;
        float ux = (u.x * cs - r.x * sn) * h * 0.5f, uy = (u.y * cs - r.y * sn) * h * 0.5f, uz = (u.z * cs - r.z * sn) * h * 0.5f;
        A.set(x - rx - ux, y - ry - uy, z - rz - uz);
        B.set(x + rx - ux, y + ry - uy, z + rz - uz);
        C.set(x + rx + ux, y + ry + uy, z + rz + uz);
        D.set(x - rx + ux, y - ry + uy, z - rz + uz);
        int g = bl.grade(col, 1f);
        buf.quad(tex, bl, A, B, C, D, 0f, 0f, 1f, 1f, g, g);
    }

    // ---------------------------------------------------------------------------------------------------- render
    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick);
        final float R = Math.max(3f, inst.power);
        final float k12 = R / 12f;
        final long seed = inst.seed;
        final float fade = 1f - sub(age, 15f, 20f);
        if (fade <= 0.01f) return;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final float cx = c.x, cy = c.y, cz = c.z;

        // the crack network: grows from the middle to the rim over ticks 0-9, glows hotter as it loads, white at the hit, cools to ghost black after
        final float spread = VfxAnim.easeOutCubic(sub(age, 0f, 9.5f));
        final float load = 0.3f + 0.7f * sub(age, 0f, HIT);
        final float cool = 1f - 0.72f * sub(age, HIT + 1f, 17f);
        final float heat = (age < HIT ? load : cool) * (0.92f + 0.08f * Mth.sin(age * 2.7f));
        final float flash = cl(1f - Math.abs(age - HIT - 0.4f) / 3.2f);                      // 1 at the hit
        final int edge = VfxVertexBuffer.lerpColor(EDGE, WHITE, flash);
        final int hot = VfxVertexBuffer.lerpColor(CYAN, WHITE, 0.35f + 0.65f * flash);

        // the dark under everything: the pressure dims the ground so the white cracks cut
        ground(buf, GLOW, VfxBlend.ALPHA, cx, cy + 0.03f, cz, R * (0.35f + 0.75f * spread), 0f, a(VOID, 0.5f * sub(age, 0f, 4f) * fade));

        // black fracture planes (cracked reality), slightly fatter and turned off the white ones so the white reads as a bright edge
        final int ink = a(VOID, 0.82f * sub(age, 0.4f, 3f) * fade);
        ground(buf, GROUND_CRACK, VfxBlend.ALPHA, cx, cy + 0.045f, cz, R * 1.08f * spread, 0.05f, ink);
        ground(buf, GRAVITY_CRACK, VfxBlend.ALPHA, cx, cy + 0.047f, cz, R * 1.0f * spread * sub(age, 0.8f, 9f), 0.77f, ink);
        ground(buf, ROCK_CRACK, VfxBlend.ALPHA, cx, cy + 0.049f, cz, R * 0.92f * spread * sub(age, 1.2f, 9.5f), 1.9f, ink);

        // the jagged fissures racing along the ground: dark core, bright cyan-white edges (the texture), reaching the rim
        final int fn = ctx.seg(8, 4);
        final int fis = a(edge, (0.55f + 0.45f * heat) * fade);
        for (int k = 0; k < fn; k++) {
            float ang = Mth.TWO_PI * k / fn + (hash(seed, k, 1) - 0.5f) * 0.5f;
            float len = R * (0.55f + 0.45f * hash(seed, k, 2)) * VfxAnim.easeOutCubic(sub(age, 0.3f + 0.15f * (k % 3), 8f));
            if (len < 0.3f) continue;
            float ca = Mth.cos(ang), sa = Mth.sin(ang), w = (0.7f + 0.5f * hash(seed, k, 4)) * Math.max(0.6f, k12);
            float px = cx, pz = cz;
            for (int j = 1; j <= 3; j++) {                                                    // three jagged legs, each kinked off the line a little
                float f = j / 3f, off = (hash(seed, k * 4 + j, 3) - 0.5f) * len * 0.2f * (j < 3 ? 1f : 0.6f);
                float qx = cx + ca * len * f - sa * off, qz = cz + sa * len * f + ca * off;
                strip(buf, FISSURE, VfxBlend.ALPHA, px, pz, qx, qz, cy + 0.06f + 0.002f * j, w * (1.15f - 0.25f * j), fis);
                px = qx;
                pz = qz;
            }
        }

        // the cracked disc of glass-space in the air facing the camera: dark behind, faceted glass, then the white web; it bursts at the hit
        final float discIn = sub(age, 1.5f, 6f), alive = 1f - sub(age, HIT - 0.2f, HIT + 0.9f);
        final float dsz = R * (0.6f + 0.85f * VfxAnim.easeOutCubic(discIn));
        final float dx = cx, dy = cy + R * 0.42f, dz = cz;
        if (alive > 0.01f) {
            float dA = alive * discIn;
            card(ctx, buf, GLOW, VfxBlend.ALPHA, dx, dy, dz, dsz * 1.02f, dsz * 1.02f, 0f, a(VOID, 0.85f * dA));
            card(ctx, buf, ICE_FACETS, VfxBlend.ALPHA, dx, dy, dz, dsz * 0.97f, dsz * 0.97f, 0.1f, a(GLASS, 0.6f * dA));
            float web = sub(age, 2.5f, 8f);
            card(ctx, buf, SHOCK, VfxBlend.ADD, dx, dy, dz, dsz * 1.0f, dsz * 1.0f, age * 0.05f, a(CYAN, 0.8f * dA));
            card(ctx, buf, GLASS_WEB, VfxBlend.ADD, dx, dy, dz, dsz * (0.45f + 0.6f * web), dsz * (0.45f + 0.6f * web), 0.25f, a(hot, (0.45f + 0.55f * web) * dA));
            card(ctx, buf, GLASS_CRACK, VfxBlend.ADD, dx, dy, dz, dsz * (0.3f + 0.55f * web), dsz * (0.3f + 0.55f * web), 1.4f, a(WHITE, 0.8f * web * dA));
        }

        // the white-hot pin at the feet, loading
        card(ctx, buf, GLOW, VfxBlend.ADD, cx, cy + 0.25f, cz, (0.7f + 1.6f * sub(age, 0f, HIT)) * k12 * 1.4f, (0.7f + 1.6f * sub(age, 0f, HIT)) * k12 * 1.4f, 0f, a(WHITE, (0.45f + 0.5f * sub(age, 0f, 6f)) * fade * (1f - 0.8f * sub(age, HIT + 2f, 16f))));

        // the bright crack lines on top (additive): all four planes
        final int wh = a(hot, 0.95f * heat * fade);
        ground(buf, GROUND_CRACK, VfxBlend.ADD, cx, cy + 0.07f, cz, R * 1.05f * spread, 0.05f, wh);
        ground(buf, GRAVITY_CRACK, VfxBlend.ADD, cx, cy + 0.075f, cz, R * 0.98f * spread * sub(age, 0.8f, 9f), 0.77f, a(wh, 0.85f));
        ground(buf, ROCK_CRACK, VfxBlend.ADD, cx, cy + 0.08f, cz, R * 0.9f * spread * sub(age, 1.2f, 9.5f), 1.9f, a(wh, 0.8f));
        ground(buf, CORUNDUM_CRACK, VfxBlend.ADD, cx, cy + 0.085f, cz, R * 0.62f * spread * sub(age, 1.6f, 8.5f), 2.65f, a(WHITE, 0.9f * heat * fade));

        // pressure: a ring that contracts onto the pin (ticks 0-7), then the ripple that runs out over the whole radius and a lagging second one
        float inRing = sub(age, 0f, 7f);
        if (inRing > 0f && age < 8.5f) ground(buf, SHOCK, VfxBlend.ADD, cx, cy + 0.1f, cz, R * (1.15f - 0.9f * VfxAnim.easeInOutSine(inRing)), age * 0.2f, a(CYAN, 0.65f * Mth.sin(inRing * Mth.PI) * fade));
        float out1 = VfxAnim.easeOutCubic(sub(age, 6.2f, 14f));
        if (out1 > 0f) ground(buf, SHOCK, VfxBlend.ADD, cx, cy + 0.11f, cz, R * (0.15f + 0.95f * out1), -age * 0.12f, a(WHITE, 0.95f * (1f - sub(age, 6.2f, 14.5f)) * fade));
        float out2 = VfxAnim.easeOutCubic(sub(age, 7.8f, 16f));
        if (out2 > 0f) ground(buf, SHOCK, VfxBlend.ADD, cx, cy + 0.12f, cz, R * (0.1f + 0.85f * out2), age * 0.1f, a(CYAN, 0.6f * (1f - sub(age, 7.8f, 16f)) * fade));

        // the hit: flash, flare, ground glare
        if (age > HIT - 0.8f) {
            float fa = 1f - sub(age, HIT - 0.8f, HIT + 5f);
            float fz = (0.5f + 1.4f * VfxAnim.easeOutCubic(sub(age, HIT - 0.8f, HIT + 4f))) * R;
            card(ctx, buf, GLOW, VfxBlend.ADD, cx, cy + R * 0.2f, cz, fz, fz, 0f, a(WHITE, 0.8f * fa * fa));
            card(ctx, buf, FLARE, VfxBlend.ADD, cx, cy + R * 0.2f, cz, fz * 1.15f, fz * 0.9f, 0.4f, a(EDGE, fa));
            ground(buf, GLOW, VfxBlend.ADD, cx, cy + 0.13f, cz, R * (0.35f + 0.65f * VfxAnim.easeOutCubic(sub(age, HIT - 0.8f, HIT + 5f))), 0f, a(WHITE, 0.8f * fa));
        }

        // the disc bursting: glass shards thrown out in the camera plane, a cloud of glass dust
        final float st = age - HIT;
        if (st > -0.3f) {
            final Vector3f cr = ctx.camRight, cu = ctx.camUp;
            final int sn = ctx.seg(10, 5);
            for (int k = 0; k < sn; k++) {
                float ang = hash(seed, k, 20) * Mth.TWO_PI, rr = dsz * 0.5f * (0.15f + 0.85f * hash(seed, k, 21));
                float t = Math.max(0f, st);
                float out = 1f + (0.22f + 0.2f * hash(seed, k, 22)) * t;
                float px = Mth.cos(ang) * rr * out, py = Mth.sin(ang) * rr * out - 0.5f * 0.1f * t * t * (0.6f + hash(seed, k, 23));
                float sa = (st < 0f ? 0.9f : 1f) * fade * sub(st, -0.3f, 0.4f);
                float sz = R * (0.12f + 0.13f * hash(seed, k, 24));
                float spin = hash(seed, k, 25) * Mth.TWO_PI + t * (0.3f + 0.5f * hash(seed, k, 26));
                int sc = VfxVertexBuffer.lerpColor(GLASS, WHITE, 0.5f + 0.5f * hash(seed, k, 27));
                card(ctx, buf, GLASS_SHARD, VfxBlend.ALPHA, dx + cr.x * px + cu.x * py, dy + cr.y * px + cu.y * py, dz + cr.z * px + cu.z * py, sz * 0.5f, sz, spin, a(sc, 0.95f * sa));
            }
            float gdust = sub(st, 0f, 4f);
            for (int k = 0; k < 3; k++) {
                float s2 = dsz * (0.7f + 0.55f * gdust + 0.15f * k);
                card(ctx, buf, GLASS_DUST, VfxBlend.ALPHA, dx, dy, dz, s2, s2, k * 2.1f + st * 0.05f, a(VfxVertexBuffer.lerpColor(WHITE, GLASS, k * 0.4f), 0.85f * (1f - sub(st, 0.5f, 8f)) * sub(st, -0.2f, 0.3f) * fade));
            }
        }

        // the ground comes apart from the middle to the rim: chunks tremble and lift before the hit, then fly; chips fly harder and smaller
        final int cn = ctx.seg(10, 5), pn = ctx.seg(10, 5);
        for (int k = 0; k < cn; k++) {
            float ang = hash(seed, k, 30) * Mth.TWO_PI, rr = R * (0.12f + 0.8f * hash(seed, k, 31)), launch = HIT + 1.6f * rr / R;
            float tt = age - launch, lift = 0.35f * k12 * (0.5f + hash(seed, k, 32)) * VfxAnim.easeOutCubic(sub(age, 3.5f, launch));
            float tr = age < launch ? (hash(seed, k, 33) - 0.5f) * 0.06f * Mth.sin(age * 9f + k) : 0f;
            float y, d = rr;
            if (tt > 0f) {
                float vy = (0.5f + 0.55f * hash(seed, k, 34)) * (0.8f + 0.2f * k12);
                y = lift + vy * tt - 0.5f * 0.09f * tt * tt;
                d = rr + (0.07f + 0.16f * hash(seed, k, 35)) * tt * k12;
            } else y = lift;
            y = Math.max(0.12f, y);
            float sz = (0.7f + 0.9f * hash(seed, k, 36)) * Math.max(0.7f, k12);
            float al = sub(age, 3.5f, 5.5f) * fade;
            card(ctx, buf, (k & 1) == 0 ? CHUNK : STONE, VfxBlend.ALPHA, cx + Mth.cos(ang) * d + tr, cy + y, cz + Mth.sin(ang) * d, sz, sz, hash(seed, k, 37) * 6.28f + Math.max(0f, tt) * (0.1f + 0.25f * hash(seed, k, 38)), a(RUBBLE, al));
        }
        for (int k = 0; k < pn; k++) {
            float ang = hash(seed, k, 40) * Mth.TWO_PI, rr = R * (0.08f + 0.88f * hash(seed, k, 41)), launch = HIT + 1.6f * rr / R, tt = age - launch;
            if (tt < 0f) continue;
            float vy = (0.7f + 0.9f * hash(seed, k, 42)) * (0.8f + 0.2f * k12), d = rr + (0.15f + 0.3f * hash(seed, k, 43)) * tt * k12;
            float y = Math.max(0.1f, 0.1f + vy * tt - 0.5f * 0.09f * tt * tt);
            float sz = (0.3f + 0.35f * hash(seed, k, 44)) * Math.max(0.7f, k12);
            card(ctx, buf, CHIPS[k % CHIPS.length], VfxBlend.ALPHA, cx + Mth.cos(ang) * d, cy + y, cz + Mth.sin(ang) * d, sz, sz, tt * (0.4f + hash(seed, k, 45)) + k, a(WHITE, fade * sub(tt, 0f, 0.5f)));
        }

        // the dust column rising from the middle and a ring of dust running to the rim
        for (int k = 0; k < 8; k++) {
            float s0 = HIT + 0.5f + 0.35f * k, tt = age - s0;
            if (tt < 0f) continue;
            float u = sub(tt, 0f, 9f), jx = (hash(seed, k, 50) - 0.5f) * R * 0.12f * (0.6f + u), jz = (hash(seed, k, 51) - 0.5f) * R * 0.12f * (0.6f + u);
            float al = Math.min(sub(tt, 0f, 1.5f), 1f - sub(u, 0.45f, 1f)) * 0.8f * fade;
            float sz = R * (0.28f + 0.03f * k) * (0.55f + 0.75f * VfxAnim.easeOutCubic(u));
            card(ctx, buf, DUST_PUFF, VfxBlend.ALPHA, cx + jx, cy + 0.5f + R * (0.075f * k + 0.3f * VfxAnim.easeOutCubic(u)), cz + jz, sz, sz, hash(seed, k, 52) * 6.28f + age * 0.03f,
                    a(VfxVertexBuffer.lerpColor(DUST_LO, DUST_HI, k / 7f), al));
        }
        float rim = VfxAnim.easeOutCubic(sub(age, HIT - 0.2f, 15f));
        for (int k = 0; k < 6; k++) {
            if (rim <= 0f) break;
            float ang = Mth.TWO_PI * k / 6f + hash(seed, k, 60) * 0.7f, r2 = R * (0.2f + 0.8f * rim) * (0.85f + 0.2f * hash(seed, k, 61));
            float al = 0.6f * (1f - sub(age, 12f, 19.5f)) * sub(age, HIT - 0.2f, HIT + 1.2f);
            float sz = R * (0.22f + 0.1f * hash(seed, k, 62)) * (0.7f + 0.5f * rim);
            card(ctx, buf, DUST_PUFF, VfxBlend.ALPHA, cx + Mth.cos(ang) * r2, cy + 0.5f + R * 0.06f * rim, cz + Mth.sin(ang) * r2, sz, sz * 0.8f, hash(seed, k, 63) * 6.28f, a(DUST_LO, al));
        }

        // white sparks flung from the hit
        for (int k = 0; k < 6; k++) {
            float tt = age - HIT - 0.3f * (k % 3);
            if (tt < 0f || tt > 7f) continue;
            float ang = hash(seed, k, 70) * Mth.TWO_PI, el = 0.2f + 0.9f * hash(seed, k, 71), sp = (0.9f + 1.1f * hash(seed, k, 72)) * k12 * 1.2f;
            float dd = sp * tt, hx = Mth.cos(ang) * Mth.cos(el) * dd, hz = Mth.sin(ang) * Mth.cos(el) * dd, hy = Mth.sin(el) * dd * 0.9f - 0.04f * tt * tt;
            card(ctx, buf, GLINT, VfxBlend.ADD, cx + hx, cy + 0.3f + Math.max(0f, hy), cz + hz, R * 0.055f, R * 0.055f, tt * 0.5f, a(WHITE, (1f - tt / 7f) * fade));
        }
    }
}
