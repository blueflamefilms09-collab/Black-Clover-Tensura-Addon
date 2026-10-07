package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Corundum Magic (Ideal Closer): flat-shaded, angular, faceted lavender-grey stone with a thin dark outline round every facet, as in the
 * owner's still, and ruby (red) / sapphire (blue) gems cut into it, as in the owner spec. Stone is drawn ALPHA (solid, outlined), gems
 * and light ADD. Three shapes, each with its own silhouette and its own texture set:
 * <ul>
 *   <li>CORUNDUM_FX1, CAST / PROJECTILE, "Corundum Lance". A hexagonal sigil and a ruby hex ring contract on the caster while stone
 *       chips are pulled in and a six-ray star flares; then a faceted boulder with a cut gem in it and a long hex-prism lance
 *       streak from 'from' to 'to' (accelerating), dragging a sapphire / white streak, three lance afterimages, orbiting chips and
 *       shed chips, and ends in a hex shock ring, a star flare and a small starburst of stone spikes at 'to'.
 *       from = origin, to = target, power = size (1.0 = a lance about 1.5 blocks long), duration = flight ticks (default 16;
 *       charge 0..22%, flight to 78%, impact to 100%).</li>
 *   <li>CORUNDUM_FX2, ZONE / FIELD / DOME, "Corundum Fortress". A cracked ground with a turning hexagram sigil and counter-turning hex
 *       rings; a faceted stone wall rises round the rim, with ruby / sapphire / stone crystal spires on top of it (the gem ones glint),
 *       a crystal cluster grows in the centre, stone chips and gem glints rise, and a hex pulse ring rolls out every 26 ticks.
 *       from = centre on the ground, power = RADIUS in blocks, duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>CORUNDUM_FX3, IMPACT / SIGNATURE, "Ideal Closer". A white star flash, a ground crack and sigil flare, a crown of stone claws
 *       thrusts up and fans out (the fingers of the stone giant), two hex shock rings and a counter-rotating spike starburst expand,
 *       faceted boulders and chips fly out and land, ruby / sapphire sparks fly, dust and a lingering gem glow remain; from power 1.1 up
 *       a big cut gem rises and spins in the middle. from = centre, to = optional direction hint (to - from biases the debris and
 *       sparks; zero = all around), power = scale (0.6 to 3.0), duration = life ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 15 percent into the stone palette, so a white tint still reads as corundum.
 */
public class CorundumLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FACET = t("corundum_facet");
    public static final ResourceLocation SHARD = t("corundum_shard");
    public static final ResourceLocation GEM = t("corundum_gem");
    public static final ResourceLocation STAR = t("corundum_star");
    public static final ResourceLocation BAND = t("corundum_band");
    public static final ResourceLocation HEX = t("corundum_hex");
    public static final ResourceLocation SIGIL = t("corundum_sigil");
    public static final ResourceLocation STREAK = t("corundum_streak");
    public static final ResourceLocation CRACK = t("corundum_crack");
    public static final ResourceLocation BURST = t("corundum_burst");
    public static final ResourceLocation CHIP = t("corundum_chip");

    private static final int STONE = 0xFFC9CDE8;
    private static final int INKY = 0xFF262C4A;
    private static final int RUBY = 0xFFE0344A, RUBY_L = 0xFFFF6A7A;
    private static final int SAPH = 0xFF3F74E8, SAPH_L = 0xFF9CC4FF;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CORUNDUM_FX1, VfxShape.CORUNDUM_FX2, VfxShape.CORUNDUM_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CORUNDUM_FX1 -> 16;
            case CORUNDUM_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF6A7A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CORUNDUM_FX1 -> cast(inst, ctx, buf);
            case CORUNDUM_FX2 -> zone(inst, ctx, buf);
            case CORUNDUM_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static int stone(VfxInstance inst) { return VfxVertexBuffer.lerpColor(STONE, inst.color | 0xFF000000, 0.15f); }
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }
    private static int gem(int i) { return (i & 1) == 0 ? RUBY_L : SAPH_L; }
    private static int gemDeep(int i) { return (i & 1) == 0 ? RUBY : SAPH; }

    private static float sm(float e0, float e1, float x) {
        float k = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return k * k * (3 - 2 * k);
    }

    private static float easeOut(float x) { return VfxAnim.easeOutCubic(Mth.clamp(x, 0, 1)); }

    /** A unit vector from two hashes: uniform on the sphere when up = false, on the upper hemisphere when up = true. */
    private static Vector3f dirOf(float u, float v, boolean up) {
        float z = up ? 0.12f + 0.88f * u : u * 2f - 1f;
        float s = Mth.sqrt(Math.max(0f, 1f - z * z)), ang = v * Mth.TWO_PI;
        return new Vector3f(Mth.cos(ang) * s, z, Mth.sin(ang) * s);
    }

    /** A camera-facing ribbon from a (texture bottom) to b (texture top, the tip). Used for shards and spires. */
    private static void strut(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float halfW, int argb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(halfW);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(a).sub(side), new Vector3f(a).add(side), new Vector3f(b).add(side), new Vector3f(b).sub(side), 0, 0, 1, 1, col, col);
    }

    // ------------------------------------------------------------------ FX1: Corundum Lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) { dir.set(0, 0, 1); dist = 0.05f; } else dir.mul(1f / dist);
        int st = stone(inst);

        float tl = 0.22f * d, ta = 0.78f * d;
        float s = Mth.clamp((age - tl) / (ta - tl), 0, 1), se = 0.45f * s + 0.55f * s * s;
        float ti = Mth.clamp((age - ta) / Math.max(1f, d - ta), 0, 1);
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        VfxPose basis = VfxPose.facing(new Vector3f(0, 0, 0), dir);

        // 1. charge: hex sigil and ruby ring contract on the caster, a star flares, chips are drawn in
        float tc = Mth.clamp(age / tl, 0, 1);
        float cf = tc * (1f - Mth.clamp((age - tl) / (0.35f * tl + 1f), 0, 1));
        if (cf > 0.02f && buf.hasBudget(48)) {
            VfxPose mz = VfxPose.facing(new Vector3f(dir).mul(0.35f * P).add(from), dir);
            float shrink = 1.5f - 0.6f * tc;
            buf.plane(SIGIL, VfxBlend.ADD, mz.spin(age * 0.25f), 0.95f * P * shrink, a(SAPH_L, 0.9f * cf));
            buf.plane(HEX, VfxBlend.ADD, mz.lift(0.05f).spin(-age * 0.35f), 0.7f * P * shrink, a(RUBY_L, 0.85f * cf));
            VfxBloom.glow(ctx, buf, from, 0.5f * P * (0.4f + 0.6f * tc), RUBY_L, cf);
            buf.billboard(ctx, STAR, VfxBlend.ADD, from, 1.3f * P * tc, age * 0.2f, a(WHITE, 0.9f * cf));
            for (int i = 0; i < 6; i++) {
                Vector3f rv = dirOf(h(inst, i, 2), h(inst, i, 1), false);
                float rad = (1.5f * (1f - tc) + 0.12f) * P * (0.7f + 0.5f * h(inst, i, 3));
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, rv.mul(rad).add(from), 0.17f * P, h(inst, i, 4) * 6f + age * 0.3f, a(st, cf));
            }
        }

        // 2. flight
        float appear = Mth.clamp((age - tl) / 2f, 0, 1);
        float vis = appear * (1f - sm(0f, 0.4f, ti));
        if (vis > 0.02f && age >= tl) {
            float len = Math.min(dist * se, 3.4f * P + 0.5f);
            Vector3f tail = new Vector3f(dir).mul(-len).add(head);
            // streak: wide sapphire body, thin white core
            if (len > 0.1f && buf.hasBudget(24)) {
                buf.beam(ctx, STREAK, VfxBlend.ADD, head, tail, 1.3f * P, 0.2f * P, 3, age * 0.15f, a(SAPH_L, 0.55f * vis), a(SAPH, 0f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, head, tail, 0.45f * P, 0.08f * P, 3, -age * 0.2f, a(WHITE, 0.95f * vis), a(RUBY_L, 0f));
            }
            // three lance afterimages, each further back and dimmer
            for (int k = 1; k <= 3 && buf.hasBudget(4); k++) {
                float sk = Math.max(0f, s - 0.07f * k), sek = 0.45f * sk + 0.55f * sk * sk;
                Vector3f hk = new Vector3f(dir).mul(dist * sek).add(from);
                if (sk <= 0f) break;
                strut(buf, SHARD, VfxBlend.ADD, new Vector3f(dir).mul(-1.4f * P).add(hk), new Vector3f(dir).mul(0.3f * P).add(hk), 0.24f * P, a(gem(k), 0.42f * vis / k));
            }
            // the faceted boulder behind the lance and the lance itself
            if (buf.hasBudget(32)) {
                buf.billboard(ctx, FACET, VfxBlend.ALPHA, new Vector3f(dir).mul(-0.75f * P).add(head), 0.8f * P, age * 0.35f, a(st, vis));
                strut(buf, SHARD, VfxBlend.ALPHA, new Vector3f(dir).mul(-1.1f * P).add(head), new Vector3f(dir).mul(0.55f * P).add(head), 0.27f * P, a(st, vis));
                float pul = 0.75f + 0.25f * Mth.sin(age * 1.3f);
                buf.billboard(ctx, GEM, VfxBlend.ALPHA, new Vector3f(dir).mul(-0.1f * P).add(head), 0.5f * P, age * 0.5f, a(gemDeep(inst.shape.ordinal()), vis));
                VfxBloom.glow(ctx, buf, new Vector3f(dir).mul(0.3f * P).add(head), 0.45f * P * pul, RUBY_L, vis);
                buf.billboard(ctx, STAR, VfxBlend.ADD, new Vector3f(dir).mul(0.5f * P).add(head), 1.1f * P * pul, age * 0.3f, a(WHITE, 0.9f * vis));
            }
            // orbiting chips on the lance, and chips shed behind it that drop
            for (int i = 0; i < 4 && buf.hasBudget(4); i++) {
                float ang = age * 0.7f + i * 1.9f;
                Vector3f p = new Vector3f(dir).mul(-(0.2f + i * 0.4f) * P).add(head)
                        .add(new Vector3f(basis.right()).mul(Mth.cos(ang) * 0.5f * P)).add(new Vector3f(basis.up()).mul(Mth.sin(ang) * 0.5f * P));
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, p, 0.18f * P, ang * 1.7f, a(st, vis));
            }
            for (int i = 0; i < 5 && buf.hasBudget(4); i++) {
                float born = tl + (0.2f + 0.7f * h(inst, i, 7)) * (ta - tl), ageC = age - born;
                if (ageC < 0 || ageC > 9f) continue;
                float bs = Mth.clamp((born - tl) / (ta - tl), 0, 1), bse = 0.45f * bs + 0.55f * bs * bs;
                Vector3f p = new Vector3f(dir).mul(dist * bse).add(from);
                p.add((h(inst, i, 8) - 0.5f) * 0.5f * P, -0.03f * ageC * ageC * 0.5f * P, (h(inst, i, 9) - 0.5f) * 0.5f * P);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, p, 0.2f * P, ageC * 0.5f + i, a(st, 1f - ageC / 9f));
            }
        }

        // 3. impact at 'to': hex shock ring, star flare, a small spike burst and flying chips
        if (age >= ta - 1f && buf.hasBudget(40)) {
            float e = easeOut(ti), fl = 1f - ti;
            VfxPose ip = VfxPose.facing(new Vector3f(to), dir);
            buf.plane(HEX, VfxBlend.ADD, ip.spin(ti * 1.2f), (0.4f + 1.7f * e) * P, a(RUBY_L, 0.9f * fl * fl));
            buf.billboard(ctx, BURST, VfxBlend.ALPHA, to, 1.5f * P * (0.4f + 0.6f * e), ti * 0.8f, a(st, Mth.clamp(1.6f * fl, 0, 1)));
            VfxBloom.glow(ctx, buf, to, 0.7f * P * (1f + 0.5f * e), SAPH_L, fl);
            buf.billboard(ctx, STAR, VfxBlend.ADD, to, 1.9f * P * (0.7f + 0.5f * e), 0.4f, a(WHITE, fl));
            for (int i = 0; i < 5; i++) {
                Vector3f dv = dirOf(h(inst, i, 12), h(inst, i, 13), false);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, dv.mul(0.35f * P + 1.5f * P * e).add(to), 0.2f * P * (1f - 0.4f * ti), i * 1.3f + ti * 6f, a(st, fl));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Corundum Fortress
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(20f, inst.duration);
        float R = Math.max(1.5f, inst.power);
        float f = Mth.clamp(Math.min(age / 8f, (d - age) / 12f), 0f, 1f);
        if (f <= 0.01f) return;
        float grow = easeOut(age / 14f);
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose gp = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        int st = stone(inst);
        float hs = Math.min(2.3f, 0.45f * R + 0.8f);                       // wall height

        // 1. cracked ground, a glow under it, the turning sigil and counter-turning hex rings
        buf.plane(CRACK, VfxBlend.ALPHA, gp.spin(0.3f), R * 1.08f * grow, a(INKY, 0.85f * f));
        VfxBloom.planeGlow(buf, gp.lift(0.01f), R * 0.7f, gem((int) (age / 14f)), 0.6f * f * (0.7f + 0.3f * VfxAnim.pulse(age, 1.2f)));
        buf.plane(SIGIL, VfxBlend.ADD, gp.lift(0.02f).spin(age * 0.02f), R * 0.98f * grow, a(SAPH, 0.6f * f));
        buf.plane(HEX, VfxBlend.ADD, gp.lift(0.03f).spin(-age * 0.03f), R * 0.7f * grow, a(RUBY, 0.55f * f));
        buf.plane(HEX, VfxBlend.ADD, gp.lift(0.04f).spin(age * 0.012f + 0.5f), R * 1.0f * grow, a(SAPH_L, 0.16f * f));

        // 2. the faceted stone wall rising round the rim
        int n = Math.min(16, ctx.seg(16, 8));
        float wr = R * 0.97f;
        for (int i = 0; i < n && buf.hasBudget(4); i++) {
            float rise = VfxAnim.easeOutBack(Mth.clamp((age - 2f - 0.5f * (i % 8)) / 14f, 0, 1));
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            float y = hs * Math.max(0f, rise) * (0.85f + 0.3f * h(inst, i, 21));
            Vector3f b0 = new Vector3f(Mth.cos(a0) * wr, 0, Mth.sin(a0) * wr).add(c), b1 = new Vector3f(Mth.cos(a1) * wr, 0, Mth.sin(a1) * wr).add(c);
            int cb = a(st, 0.95f * f), ct = a(VfxVertexBuffer.lerpColor(st, gemDeep(i / 2), 0.12f), 0.7f * f);
            buf.quad(BAND, VfxBlend.ALPHA, b0, b1, new Vector3f(b1).add(0, y, 0), new Vector3f(b0).add(0, y, 0), 0, 0, 1, 1, cb, ct);
        }

        // 3. crystal spires on the rim: ruby, sapphire and stone, the gem ones glint
        int sp = 8;
        float sh = Math.min(3.2f, 0.4f * R + 1.1f);
        for (int i = 0; i < sp && buf.hasBudget(8); i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * h(inst, i, 31)) / sp;
            float rise = VfxAnim.easeOutBack(Mth.clamp((age - 5f - 1.2f * i) / 14f, 0, 1));
            if (rise <= 0f) continue;
            float hh = sh * (0.7f + 0.6f * h(inst, i, 32)) * rise;
            Vector3f base = new Vector3f(Mth.cos(ang) * R * 0.99f, -0.05f, Mth.sin(ang) * R * 0.99f).add(c);
            Vector3f tip = new Vector3f(base).add(-Mth.cos(ang) * 0.12f * hh, hh, -Mth.sin(ang) * 0.12f * hh);
            int kind = i % 3;
            int col = kind == 0 ? VfxVertexBuffer.lerpColor(st, RUBY, 0.55f) : kind == 1 ? VfxVertexBuffer.lerpColor(st, SAPH, 0.55f) : st;
            strut(buf, SHARD, VfxBlend.ALPHA, base, tip, 0.3f * Math.min(1.5f, 0.6f + 0.2f * R), a(col, f));
            if (kind < 2) buf.billboard(ctx, STAR, VfxBlend.ADD, new Vector3f(base).lerp(tip, 0.82f), (0.7f + 0.4f * Mth.sin(age * 0.3f + i * 2f)) * 0.9f,
                    age * 0.05f, a(kind == 0 ? RUBY_L : SAPH_L, 0.8f * f * rise));
        }

        // 4. central crystal cluster with a gem glow
        float cr = VfxAnim.easeOutBack(Mth.clamp((age - 3f) / 16f, 0, 1));
        float ch = Math.min(1.9f, 0.4f * R + 0.7f) * Math.max(0f, cr);
        if (ch > 0.05f && buf.hasBudget(24)) {
            strut(buf, SHARD, VfxBlend.ALPHA, new Vector3f(c).add(-0.18f, -0.05f, 0), new Vector3f(c).add(-0.4f * ch * 0.35f, ch * 0.75f, 0.1f), 0.26f, a(VfxVertexBuffer.lerpColor(st, SAPH, 0.5f), f));
            strut(buf, SHARD, VfxBlend.ALPHA, new Vector3f(c).add(0.2f, -0.05f, 0.05f), new Vector3f(c).add(0.35f * ch * 0.4f, ch * 0.65f, -0.1f), 0.24f, a(VfxVertexBuffer.lerpColor(st, SAPH, 0.5f), f));
            strut(buf, SHARD, VfxBlend.ALPHA, new Vector3f(c).add(0, -0.05f, 0), new Vector3f(c).add(0, ch, 0), 0.36f, a(VfxVertexBuffer.lerpColor(st, RUBY, 0.55f), f));
            VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, ch * 0.55f, 0), 0.5f + 0.1f * VfxAnim.pulse(age, 1.5f), RUBY_L, 0.8f * f);
        }

        // 5. rising chips and gem glints
        for (int i = 0; i < 8 && buf.hasBudget(4); i++) {
            float life = 36f, ph = ((age + h(inst, i, 41) * life) % life) / life;
            float ang = h(inst, i, 42) * Mth.TWO_PI + ph * 1.5f, rr = R * (0.15f + 0.8f * h(inst, i, 43));
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.15f + ph * (1.6f + 1.2f * h(inst, i, 44)), Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, CHIP, VfxBlend.ALPHA, p, 0.17f, age * 0.12f + i, a(st, f * Mth.sin(ph * Mth.PI)));
        }
        for (int i = 0; i < 6 && buf.hasBudget(4); i++) {
            float life = 24f, ph = ((age + h(inst, i, 51) * life) % life) / life;
            float ang = h(inst, i, 52) * Mth.TWO_PI, rr = R * (0.1f + 0.85f * h(inst, i, 53));
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.3f + 1.4f * h(inst, i, 54) + ph * 0.5f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, STAR, VfxBlend.ADD, p, 0.55f * Mth.sin(ph * Mth.PI), ph * 1.5f, a(gem(i), 0.9f * f));
        }

        // 6. hex pulse rings rolling out every 26 ticks
        for (int k = 0; k < 2 && buf.hasBudget(4); k++) {
            float ph = ((age + k * 13f) % 26f) / 26f;
            if (age < 8f) break;
            buf.plane(HEX, VfxBlend.ADD, gp.lift(0.05f).spin(ph * 0.8f), R * (0.2f + 0.8f * easeOut(ph)), a(k == 0 ? RUBY_L : SAPH_L, 0.7f * f * (1f - ph)));
        }
    }

    // ------------------------------------------------------------------ FX3: Ideal Closer
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(10f, inst.duration);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        float t = Mth.clamp(age / d, 0, 1);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f((float) (inst.to(ctx).x - inst.from(ctx).x), 0, (float) (inst.to(ctx).z - inst.from(ctx).z));
        boolean biased = hint.lengthSquared() > 0.04f;
        if (biased) hint.normalize();
        VfxPose gp = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        int st = stone(inst);
        float flash = (float) Math.exp(-age / 3.5f);
        float e = easeOut(t * 2f);

        // 1. ground: crack and sigil flare, two hex shock rings
        buf.plane(CRACK, VfxBlend.ALPHA, gp.spin(h(inst, 0, 1) * 6f), (0.5f + 1.7f * e) * P, a(INKY, 0.9f * (1f - sm(0.55f, 1f, t))));
        buf.plane(SIGIL, VfxBlend.ADD, gp.lift(0.02f).spin(age * 0.12f), 1.7f * P * easeOut(age / 5f), a(SAPH_L, 0.85f * (1f - sm(0.2f, 0.75f, t))));
        for (int k = 0; k < 2; k++) {
            float tk = Mth.clamp((age - 1f - 3f * k) / (0.7f * d), 0, 1);
            if (tk <= 0f || tk >= 1f) continue;
            buf.plane(HEX, VfxBlend.ADD, gp.lift(0.04f + 0.01f * k).spin((k == 0 ? 1 : -1) * tk * 1.4f), P * (0.3f + 3.2f * easeOut(tk)) * (1f - 0.25f * k),
                    a(k == 0 ? RUBY_L : SAPH_L, 0.95f * (1f - tk) * (1f - tk)));
        }

        // 2. the crown of stone claws thrusting up and fanning out
        int claws = 9;
        float thrust = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1));
        float sink = 1f - sm(0.55f, 0.95f, t);
        for (int i = 0; i < claws && buf.hasBudget(4); i++) {
            float ang = Mth.TWO_PI * (i + 0.4f * h(inst, i, 61)) / claws;
            float tilt = 0.3f + 0.5f * h(inst, i, 62) + (i % 2 == 0 ? 0f : 0.25f);
            float len = (1.0f + 1.2f * h(inst, i, 63)) * P * Math.max(0f, thrust) * (0.6f + 0.4f * sink);
            float rr0 = 0.15f * P;
            Vector3f base = new Vector3f(Mth.cos(ang) * rr0, -0.05f, Mth.sin(ang) * rr0).add(c);
            Vector3f tip = new Vector3f(Mth.cos(ang) * Mth.sin(tilt) * len, Mth.cos(tilt) * len * (0.5f + 0.5f * sink), Mth.sin(ang) * Mth.sin(tilt) * len).add(base);
            int col = i % 4 == 0 ? VfxVertexBuffer.lerpColor(st, RUBY, 0.55f) : i % 4 == 2 ? VfxVertexBuffer.lerpColor(st, SAPH, 0.55f) : st;
            strut(buf, SHARD, VfxBlend.ALPHA, base, tip, 0.2f * P * (0.8f + 0.4f * h(inst, i, 64)), a(col, sink));
        }

        // 3. starburst of spikes (stone, then a counter-rotating gem copy)
        Vector3f mid = new Vector3f(c).add(0, 0.7f * P, 0);
        buf.billboard(ctx, BURST, VfxBlend.ALPHA, mid, 3.3f * P * e, age * 0.03f, a(st, 1f - sm(0.3f, 0.75f, t)));
        buf.billboard(ctx, BURST, VfxBlend.ADD, mid, 4.4f * P * easeOut(age / 8f), -age * 0.06f, a(gem((int) inst.seed), 0.75f * (1f - sm(0.1f, 0.6f, t))));

        // 4. the flash
        if (flash > 0.02f) {
            buf.billboard(ctx, STAR, VfxBlend.ADD, mid, 3.4f * P * (0.6f + 0.5f * (1f - flash)), age * 0.1f, a(WHITE, flash));
            buf.billboard(ctx, STAR, VfxBlend.ADD, mid, 2.4f * P, 0.52f - age * 0.08f, a(SAPH_L, 0.8f * flash));
            VfxBloom.glow(ctx, buf, mid, 1.1f * P, RUBY_L, flash);
        }

        // 5. flying boulders and chips: ballistic, they land and fade
        for (int i = 0; i < 12 && buf.hasBudget(4); i++) {
            float ang = h(inst, i, 71) * Mth.TWO_PI;
            float vh = (0.1f + 0.2f * h(inst, i, 72)) * P;
            Vector3f hd = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
            if (biased) hd.mul(0.55f).add(new Vector3f(hint).mul(0.45f)).normalize();
            float vy = (0.25f + 0.25f * h(inst, i, 73)) * Mth.sqrt(P);
            float y = Math.max(0.08f, 0.2f + vy * age - 0.5f * 0.035f * age * age);
            float landed = Math.min(age, 2f * vy / 0.035f);
            Vector3f p = new Vector3f(hd).mul(vh * landed + 0.2f * P).add(c).add(0, y, 0);
            boolean big = i < 3;
            float fade = 1f - sm(0.65f, 1f, t);
            buf.billboard(ctx, big ? FACET : CHIP, VfxBlend.ALPHA, p, (big ? 0.5f : 0.22f) * P * (0.7f + 0.5f * h(inst, i, 74)), age * (0.12f + 0.2f * h(inst, i, 75)) + i, a(st, fade));
        }

        // 6. ruby / sapphire sparks
        for (int i = 0; i < 8 && buf.hasBudget(4); i++) {
            Vector3f dv = dirOf(h(inst, i, 81), h(inst, i, 82), true);
            if (biased) dv.mul(0.6f).add(new Vector3f(hint).mul(0.4f)).normalize();
            float sk = easeOut(age / (0.6f * d));
            float fade = 1f - sm(0.25f, 0.65f, t);
            buf.billboard(ctx, STAR, VfxBlend.ADD, dv.mul((0.5f + 2.6f * h(inst, i, 83)) * P * sk).add(c).add(0, 0.2f, 0), 0.5f * P * (1f - 0.5f * t), age * 0.2f + i, a(gem(i), fade));
        }

        // 7. a big cut gem rises in the middle for the large versions
        if (P >= 1.1f && buf.hasBudget(16)) {
            float gt = Mth.clamp((age - 2f) / (d - 2f), 0, 1);
            float gv = sm(0f, 0.15f, gt) * (1f - sm(0.55f, 1f, gt));
            if (gv > 0.02f) {
                Vector3f gp2 = new Vector3f(c).add(0, (1.2f + 0.9f * gt) * P, 0);
                VfxBloom.glow(ctx, buf, gp2, 0.9f * P, gem((int) inst.seed + 1), 0.8f * gv);
                buf.billboard(ctx, GEM, VfxBlend.ALPHA, gp2, 1.0f * P, age * 0.15f, a(gemDeep((int) inst.seed + 1), gv));
            }
        }

        // 8. afterglow and dust
        float tail = sm(0.15f, 0.4f, t) * (1f - sm(0.6f, 1f, t));
        if (tail > 0.02f) {
            VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f * P, 0), 0.8f * P, SAPH_L, 0.45f * tail);
            for (int i = 0; i < 3 && buf.hasBudget(4); i++) {
                float ang = Mth.TWO_PI * i / 3f + h(inst, i, 91);
                Vector3f p = new Vector3f(Mth.cos(ang) * P * (0.6f + 1.4f * e), 0.3f * P + 0.5f * t * P, Mth.sin(ang) * P * (0.6f + 1.4f * e)).add(c);
                buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, 0.9f * P, age * 0.02f + i, a(VfxVertexBuffer.lerpColor(st, INKY, 0.35f), 0.35f * tail));
            }
        }
    }
}
