package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Elf Ring: Thousand Elf" (DanMachi elf mage chorus): a vast elven circle of nested rings that turn against each other opens in the ground inside a crown of
 * standing leaf petals; ten small elf circles unfold in a ring in the air, tilted to look down at the middle, and sing one after another (in a scattered order, like a chorus) each
 * a borrowed spell in its own colour (fire, ice, lightning, wind, light): a bolt runs from the circle to the ground inside the ring, lands with a flare, a ring and a column of
 * its element. Then every circle sends a thread of white-gold to the middle, a white-gold pillar and a star close the verse, and the circles fold away into the middle.
 * <p>Ticks (20): 0-6 circles open, petals rise; 5-12 the chorus fires; 12-16 converging flash; 16-20 fold-away.
 * Worst case: under 400 vertices (ground 6 quads, 12 petals, 10 circles, at most ~5 bolts in flight / landing at once, 8 drifting leaves, the final flash), the buffer hard-caps it.
 * No allocation but the beam helper's three vectors per bolt, nothing is read from the world, everything comes from the instance seed and age.
 */
public class UltThousandElfLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation ELF = t("elf_circle"), RINGS = t("secre_rings"), SOLOMON = t("solomon_circle"), STAR_CIRCLE = t("magic_circle"), RIM = t("wmc_ring_0"),
            GLOW = t("glow"), SPARK = t("spark"), FLARE = t("demon_light_flare"), HALO = t("halo_ring_0"), SWIRL = t("wind_swirl_0"),
            PETAL = t("bfx_feather"), PILLAR = t("solomon_pillar"),
            LEAF0 = t("leaf_green_0"), LEAF1 = t("leaf_green_1"), LEAF2 = t("leaf_green_2"), FEATHER = t("feather_white_1"),
            S_FIRE = t("rune_stroke_flame"), S_WATER = t("rune_stroke_water"), S_WIND = t("rune_stroke_wind"), S_GOLD = t("rune_stroke_gold"), BOLT = t("lightningfx_bolt"),
            SPIKE = t("icefx_spike");

    private static final int EMERALD = 0xFF3FE08A, DEEP = 0xFF179A5E, GOLD = 0xFFFFE6A0, WHITE = 0xFFFFFFFF;
    /** fire, ice, lightning, wind, light */
    private static final int[] ELEM = {0xFFFF7234, 0xFF7FD6FF, 0xFFFFF05A, 0xFF8CFFB4, 0xFFFFF2C4};
    private static final ResourceLocation[] BOLT_TEX = {S_FIRE, S_WATER, BOLT, S_WIND, S_GOLD};
    private static final ResourceLocation[] COL_TEX = {S_FIRE, SPIKE, BOLT, S_WIND, S_GOLD};
    private static final ResourceLocation[] LEAVES = {LEAF0, LEAF1, LEAF2, FEATHER};
    private static final int N = 10, PETALS = 12;

    private final Vector3f a = new Vector3f(), b = new Vector3f(), q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f(), n = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_THOUSAND_ELF); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF58E8A0; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int al(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    /** 0 -> 1 -> 0 bump over [s, e]. */
    private static float bump(float v, float s, float e) { return Mth.sin(sub(v, s, e) * Mth.PI); }

    private void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius <= 0.01f || (col >>> 24) < 3 || !buf.hasBudget(4)) return;
        a.set(c.x, c.y + y, c.z);
        buf.plane(tex, bl, VfxPose.ground(a).spin(spin), radius, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(4f, inst.power);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutCubic(sub(age, 0f, 6f));
        float out = 1f - sub(age, 16f, 20f);
        if (out <= 0.01f) return;
        int accent = VfxVertexBuffer.lerpColor(GOLD, inst.color | 0xFF000000, 0.2f);
        float foldAll = VfxAnim.easeInCubic(sub(age, 15.5f, 20f));

        // ---- the vast circle: a pool of light, a gold outer rune ring, the elf circle against it, turning rings and the star circle in the middle
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.9f);
        ground(buf, GLOW, VfxBlend.ADD, c, 0.03f, R * 1.05f * open, 0f, al(DEEP, 0.2f * out));
        ground(buf, SOLOMON, VfxBlend.ADD, c, 0.05f, R * 0.98f * open, age * 0.018f, al(GOLD, 0.8f * out));
        ground(buf, ELF, VfxBlend.ADD, c, 0.07f, R * 0.78f * open, -age * 0.045f, al(EMERALD, 0.9f * out * pulse));
        ground(buf, RINGS, VfxBlend.ADD, c, 0.09f, R * 0.52f * open, age * 0.07f, al(WHITE, 0.8f * out));
        ground(buf, STAR_CIRCLE, VfxBlend.ADD, c, 0.11f, R * 0.30f * open, -age * 0.11f, al(accent, 0.9f * out));

        // ---- the ten small circles of the chorus, with their bolts
        float ringR = R * 0.72f;
        for (int k = 0; k < N; k++) {
            int ord = (k * 3) % N;                                           // scattered firing order: the chorus does not sweep round the ring
            float ang = Mth.TWO_PI * k / N + 0.31f + age * 0.012f;
            float appear = VfxAnim.easeOutBack(sub(age, k * 0.3f, k * 0.3f + 3.5f));
            float fire = 5.2f + ord * 0.72f;
            int e = k % 5;
            int ec = ELEM[e];
            float cos = Mth.cos(ang), sin = Mth.sin(ang);
            float rr = ringR * (1f - 0.9f * foldAll), h = 4.6f + 0.35f * Mth.sin(age * 0.35f + k) + 2.2f * foldAll;
            float sing = bump(age, fire - 0.8f, fire + 3.5f);                // the circle lights up while it sings
            float size = 2.4f * appear * (1f - 0.9f * foldAll) * (1f + 0.25f * sing);
            float mx = c.x + cos * rr, my = c.y + h, mz = c.z + sin * rr;
            if (size > 0.05f && buf.hasBudget(4)) {
                a.set(mx, my, mz);
                n.set(-cos * 0.8f, -0.6f, -sin * 0.8f);
                int cc = VfxVertexBuffer.lerpColor(EMERALD, ec, 0.35f + 0.5f * sing);
                buf.plane(ELF, VfxBlend.ADD, VfxPose.facing(a, n).spin(age * 0.16f * (k % 2 == 0 ? 1 : -1)), size, al(cc, (0.7f + 0.3f * sing) * out));
            }
            // target on the ground inside the ring
            float tang = ang + (hash(seed, k, 1) - 0.5f) * 1.3f, tr = R * (0.16f + 0.3f * hash(seed, k, 2));
            float tx = c.x + Mth.cos(tang) * tr, ty = c.y + 0.15f, tz = c.z + Mth.sin(tang) * tr;
            float fa = age - fire;
            if (fa < 0f || fa > 8f) continue;
            // the bolt: head runs from the circle to the target in 2.2 ticks, the tail follows
            float head = sub(fa, 0f, 2.2f), tail = sub(fa, 1.4f, 4.2f);
            float fade = (1f - sub(fa, 2.6f, 4.2f)) * out;
            if (tail < 1f && fade > 0f) {
                float hx = mx + (tx - mx) * head, hy = my + (ty - my) * head, hz = mz + (tz - mz) * head;
                a.set(mx + (tx - mx) * tail, my + (ty - my) * tail, mz + (tz - mz) * tail);
                b.set(hx, hy, hz);
                int bc = VfxVertexBuffer.lerpColor(ec, WHITE, 0.25f);
                buf.beam(ctx, BOLT_TEX[e], VfxBlend.ADD, a, b, 0.5f, 1.8f, 1, 0f, al(bc, 0.6f * fade), al(bc, 1f * fade));
                if (buf.hasBudget(4)) buf.billboard(ctx, GLOW, VfxBlend.ADD, b, 3.2f, 0f, al(ec, 0.9f * fade));
            }
            // landing: flare, ring on the ground and a column of the element
            float la = fa - 2.0f;
            if (la > 0f && la < 5.5f) {
                float lp = la / 5.5f, lf = (1f - lp) * out;
                a.set(tx, ty + 0.8f, tz);
                if (buf.hasBudget(4)) buf.billboard(ctx, FLARE, VfxBlend.ADD, a, 3.8f * (0.6f + 0.6f * VfxAnim.easeOutCubic(lp)), lp * 2f, al(VfxVertexBuffer.lerpColor(ec, WHITE, 0.4f), 0.9f * lf));
                q0.set(tx, c.y, tz);
                if (e == 3) ground(buf, SWIRL, VfxBlend.ADD, q0, 0.14f, 0.9f + 1.6f * VfxAnim.easeOutCubic(lp), -lp * 5f, al(ec, 0.8f * lf));
                else ground(buf, HALO, VfxBlend.ADD, q0, 0.14f, 0.7f + 1.8f * VfxAnim.easeOutCubic(lp), 0f, al(ec, 0.85f * lf));
                float colH = (e == 4 ? 9f : e == 2 ? 8f : 6f) * VfxAnim.easeOutCubic(sub(la, 0f, 1.6f));
                if (colH > 0.1f) {
                    a.set(tx, c.y, tz);
                    b.set(tx, c.y + colH, tz);
                    float cw = (e == 4 ? 1.3f : 2.2f) * (1f - 0.5f * lp);
                    buf.beam(ctx, COL_TEX[e], VfxBlend.ADD, a, b, cw, cw * 0.45f, 1, 0f, al(ec, 0.9f * lf), al(VfxVertexBuffer.lerpColor(ec, WHITE, 0.5f), 0.35f * lf));
                }
            }
        }

        // ---- the verse closes: threads of white-gold from every circle to the middle, then the pillar, the star and the ring
        float conv = bump(age, 11.5f, 16f);
        if (conv > 0.01f) {
            float cp = sub(age, 11.5f, 14f);
            for (int k = 0; k < N; k++) {
                float ang = Mth.TWO_PI * k / N + 0.31f + age * 0.012f;
                float rr = ringR * (1f - 0.9f * foldAll);
                a.set(c.x + Mth.cos(ang) * rr, c.y + 4.6f + 2.2f * foldAll, c.z + Mth.sin(ang) * rr);
                b.set(a).lerp(q0.set(c.x, c.y + 1.2f, c.z), cp);
                buf.beam(ctx, S_GOLD, VfxBlend.ADD, a, b, 0.14f, 0.45f, 1, 0f, al(GOLD, 0.2f * conv), al(WHITE, 0.95f * conv));
            }
        }
        float fl = bump(age, 13f, 19f);
        if (fl > 0.01f) {
            float fe = VfxAnim.easeOutCubic(sub(age, 13f, 17f));
            a.set(c.x, c.y, c.z);
            b.set(c.x, c.y + 15f * (0.4f + 0.6f * fe), c.z);
            buf.beam(ctx, PILLAR, VfxBlend.ADD, a, b, 3.6f * (0.5f + 0.5f * fl), 1.6f, 1, 0f, al(GOLD, 0.95f * fl), al(WHITE, 0.4f * fl));
            a.set(c.x, c.y + 1.4f, c.z);
            if (buf.hasBudget(4)) buf.billboard(ctx, GLOW, VfxBlend.ADD, a, 5f + 6f * fe, 0f, al(WHITE, 0.6f * fl));
            if (buf.hasBudget(4)) buf.billboard(ctx, SPARK, VfxBlend.ADD, a, 11f + 10f * fe, age * 0.12f, al(accent, 0.95f * fl));
            ground(buf, ELF, VfxBlend.ADD, c, 0.16f, R * (0.25f + 0.75f * fe), age * 0.2f, al(GOLD, 0.8f * fl));
        }

        // ---- the crown of leaf petals standing round the rim (rises with the opening, lies down again as the circles fold)
        float bloom = VfxAnim.easeOutBack(sub(age, 1f, 8f)) * (1f - foldAll);
        for (int k = 0; k < PETALS; k++) {
            if (!buf.hasBudget(4)) break;
            float ang = Mth.TWO_PI * (k + 0.5f) / PETALS + 0.1f, cos = Mth.cos(ang), sin = Mth.sin(ang);
            float len = R * (0.26f + 0.08f * (k % 2)), wid = len * 0.75f;
            float tilt = 0.1f + 1.05f * Mth.clamp(bloom, 0f, 1.15f);
            float baseR = R * 0.94f * open;
            float tc = Mth.cos(tilt), ts = Mth.sin(tilt);
            float bx = c.x + cos * baseR, bz = c.z + sin * baseR, by = c.y + 0.1f;
            float tx = bx + cos * len * tc, tz = bz + sin * len * tc, ty = by + len * ts;
            q0.set(bx - sin * wid * 0.5f, by, bz + cos * wid * 0.5f);
            q1.set(bx + sin * wid * 0.5f, by, bz - cos * wid * 0.5f);
            q2.set(tx + sin * wid * 0.12f, ty, tz - cos * wid * 0.12f);
            q3.set(tx - sin * wid * 0.12f, ty, tz + cos * wid * 0.12f);
            int c0 = al(DEEP, 1f * out * cl(bloom * 2f)), c1 = al(VfxVertexBuffer.lerpColor(EMERALD, GOLD, 0.65f), 1f * out * cl(bloom * 2f));
            buf.quad(PETAL, VfxBlend.ADD, q0, q1, q2, q3, 0f, 0f, 1f, 1f, c0, c1);
        }

        // ---- leaves and feathers drifting up through the circle
        for (int k = 0; k < 8; k++) {
            if (!buf.hasBudget(4)) break;
            float ph = (age * 0.05f + hash(seed, k, 3)) % 1f, ang = hash(seed, k, 4) * Mth.TWO_PI + ph * 2.6f, rr = R * (0.15f + 0.7f * hash(seed, k, 5));
            a.set(c.x + Mth.cos(ang) * rr, c.y + 0.4f + ph * 7f * (0.6f + 0.6f * hash(seed, k, 6)), c.z + Mth.sin(ang) * rr);
            float vis = Mth.sin(ph * Mth.PI) * open * out;
            buf.billboard(ctx, LEAVES[k % 4], VfxBlend.ALPHA, a, 0.75f + 0.5f * hash(seed, k, 7), age * 0.2f + k * 1.7f, al(WHITE, 0.95f * vis));
        }
    }
}
