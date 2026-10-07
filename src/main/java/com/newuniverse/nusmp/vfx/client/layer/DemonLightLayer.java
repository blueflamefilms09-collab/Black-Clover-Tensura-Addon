package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Demon Light Magic (Sword of Judgment / Shaft of Divine Punishment), drawn after the owner's still: BLACK light. Long ragged shards of torn
 * black glass fan out from one point, each edged by a thin white-grey rim and a pale halo; small black four-pointed spiked crosses with ladder
 * bars and a white outline float among them; bundles of short parallel hatch lines mark speed. A magenta bloom tints every rim and glow.
 * <ul>
 *   <li>DEMON_LIGHT_FX1 (cast / projectile): a black spiked cross opens at 'from' while six torn shards converge on it and a magenta flare
 *       gathers; then a black lance of shard glass (two smaller shards riding beside it, three fading afterimages, a magenta streak back to
 *       'from', speed hatching, small crosses dropped along the path) flies from 'from' to 'to' and ends in a small shard burst and flash
 *       at 'to'. power = size scale, duration = flight ticks (default 16), colour tints the magenta bloom.</li>
 *   <li>DEMON_LIGHT_FX2 (zone / field): a black cracked pool on the ground with a pale rim, a rotating sigil (cardinal spiked crosses, tick
 *       band, octagram) and a counter-rotating thorn ring, pulses of light running outwards, a wall of black shards rising round the edge and
 *       leaning inwards, a translucent shaft of black light falling down the middle, black crosses and motes rising. 'from' = centre on the
 *       ground, power = radius in blocks, duration = life ticks (default 80); fades in over 8 and out over 12.</li>
 *   <li>DEMON_LIGHT_FX3 (impact / burst): white-magenta flash and flare, a black starburst of ragged spikes (two, counter-rotating) opening
 *       behind it, shock rings (one flat on the ground), a dozen torn shards and six crosses flung outwards, hatch lines racing out, one big
 *       spear along 'to - from' (when given), and a lingering magenta afterglow with rising motes. power = scale, duration = life ticks
 *       (default 28).</li>
 * </ul>
 */
public class DemonLightLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation SHARD_A = t("demon_light_shard_a");
    public static final ResourceLocation SHARD_B = t("demon_light_shard_b");
    public static final ResourceLocation SHARD_A_RIM = t("demon_light_shard_a_rim");
    public static final ResourceLocation SHARD_B_RIM = t("demon_light_shard_b_rim");
    public static final ResourceLocation BURST = t("demon_light_burst");
    public static final ResourceLocation BURST_RIM = t("demon_light_burst_rim");
    public static final ResourceLocation CROSS = t("demon_light_cross");
    public static final ResourceLocation CROSS_RIM = t("demon_light_cross_rim");
    public static final ResourceLocation HATCH = t("demon_light_hatch");
    public static final ResourceLocation BEAM = t("demon_light_beam");
    public static final ResourceLocation BEAM_RIM = t("demon_light_beam_rim");
    public static final ResourceLocation STREAK = t("demon_light_streak");
    public static final ResourceLocation FLARE = t("demon_light_flare");
    public static final ResourceLocation RING = t("demon_light_ring");
    public static final ResourceLocation MOTE = t("demon_light_mote");
    public static final ResourceLocation SIGIL = t("demon_light_sigil");
    public static final ResourceLocation SIGIL_INNER = t("demon_light_sigil_inner");
    public static final ResourceLocation POOL = t("demon_light_pool");
    public static final ResourceLocation POOL_RIM = t("demon_light_pool_rim");

    private static final int MAGENTA = 0xFFFF3AD0;
    private static final int PALE = 0xFFF4EEFF;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_LIGHT_FX1, VfxShape.DEMON_LIGHT_FX2, VfxShape.DEMON_LIGHT_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DEMON_LIGHT_FX1 -> 16;
            case DEMON_LIGHT_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return MAGENTA; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DEMON_LIGHT_FX1 -> cast(inst, ctx, buf);
            case DEMON_LIGHT_FX2 -> zone(inst, ctx, buf);
            case DEMON_LIGHT_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ colours and curves
    /** The bloom: the magic's magenta, pulled a little towards the caster's tint. */
    private static int bloom(VfxInstance inst) { return VfxVertexBuffer.lerpColor(MAGENTA, inst.color | 0xFF000000, 0.3f) | 0xFF000000; }

    /** The rim light: pale white with a breath of the bloom. */
    private static int rim(VfxInstance inst, float k) { return VfxVertexBuffer.lerpColor(PALE, bloom(inst), k) | 0xFF000000; }

    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        return Math.min(sat(age / in), sat((inst.duration - age) / out));
    }

    private static int wa(int argb, float a) { return VfxVertexBuffer.withAlpha(argb | 0xFF000000, sat(a)); }

    // ------------------------------------------------------------------ primitives
    /** Point in the view plane around c at angle a, distance r. */
    private static Vector3f vp(VfxRenderContext ctx, Vector3f c, float a, float r) {
        return new Vector3f(ctx.camRight).mul(Mth.cos(a) * r).add(new Vector3f(ctx.camUp).mul(Mth.sin(a) * r)).add(c);
    }

    /** Screen angle of a view-space vector (so a billboard's u axis runs along it). */
    private static float screenAngle(VfxRenderContext ctx, Vector3f v) { return (float) Math.atan2(v.dot(ctx.camUp), v.dot(ctx.camRight)); }

    /** One torn black shard from a (base) to b (tip): black body on ALPHA, pale rim light on ADD. */
    private static void blade(VfxRenderContext ctx, VfxVertexBuffer buf, boolean second, Vector3f a, Vector3f b, float width, float alpha, int rimCol, float rimAlpha) {
        if (alpha <= 0.02f) return;
        int body = wa(WHITE, alpha);
        buf.beam(ctx, second ? SHARD_B : SHARD_A, VfxBlend.ALPHA, a, b, width, width, 1, 0, body, body);
        int glow = wa(rimCol, rimAlpha * alpha);
        buf.beam(ctx, second ? SHARD_B_RIM : SHARD_A_RIM, VfxBlend.ADD, a, b, width, width, 1, 0, glow, glow);
    }

    /** A black spiked cross with white outline plus its rim glow. */
    private static void cross(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f pos, float size, float rot, float alpha, int rimCol) {
        if (alpha <= 0.02f || size <= 0.01f) return;
        buf.billboard(ctx, CROSS, VfxBlend.ALPHA, pos, size, rot, wa(WHITE, alpha));
        buf.billboard(ctx, CROSS_RIM, VfxBlend.ADD, pos, size * 1.12f, rot, wa(rimCol, 0.8f * alpha));
    }

    /** A black ragged starburst with rim light. */
    private static void burst(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f pos, float size, float rot, float alpha, int rimCol) {
        if (alpha <= 0.02f || size <= 0.01f) return;
        buf.billboard(ctx, BURST, VfxBlend.ALPHA, pos, size, rot, wa(WHITE, alpha));
        buf.billboard(ctx, BURST_RIM, VfxBlend.ADD, pos, size * 1.04f, rot, wa(rimCol, 0.4f * alpha));
    }

    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f pos, float size, int argb, float alpha) {
        if (alpha <= 0.02f) return;
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, pos, size, 0, wa(argb, alpha));
    }

    private static Vector3f dirOf(VfxInstance inst, VfxRenderContext ctx, Vector3f fallback) {
        Vector3f d = new Vector3f(ctx.rel(inst.to(ctx))).sub(ctx.rel(inst.from(ctx)));
        if (d.lengthSquared() < 1e-4f) return new Vector3f(fallback);
        return d;
    }

    // ------------------------------------------------------------------ FX1: cast / lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = sat(age / Math.max(1f, inst.duration));
        float pw = Mth.clamp(inst.power, 0.6f, 3.0f);
        RandomSource rnd = inst.random();
        Vector3f f = ctx.rel(inst.from(ctx));
        Vector3f dv = dirOf(inst, ctx, new Vector3f(0, 0, 4));
        float dist = dv.length();
        Vector3f dn = new Vector3f(dv).div(dist);
        Vector3f side = new Vector3f(dn).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        int bl = bloom(inst), rm = rim(inst, 0.35f);

        float charge = sat(t / 0.26f), fly = sat((t - 0.16f) / 0.66f), e = (float) Math.pow(fly, 1.45);
        float land = sat((t - 0.8f) / 0.2f);
        Vector3f head = new Vector3f(dn).mul(dist * e).add(f);

        // gathering: converging shards, cross opening, magenta flare
        float gather = charge * sat((0.34f - t) / 0.1f);
        if (gather > 0.02f) {
            float rot0 = age * 0.09f;
            for (int i = 0; i < 6; i++) {
                float a = Mth.TWO_PI * i / 6f + rot0 * (i % 2 == 0 ? 1 : -0.6f);
                float r = (1.7f * (1 - VfxAnim.easeOutCubic(charge)) + 0.28f) * pw;
                Vector3f b = vp(ctx, f, a, r * 0.3f), s = vp(ctx, f, a, r + 0.55f * pw);
                blade(ctx, buf, i % 2 == 1, s, b, 0.34f * pw, gather, rm, 0.8f);
            }
        }
        float crossA = sat(charge * 1.6f) * sat((0.5f - t) / 0.14f);
        cross(ctx, buf, f, 1.5f * pw * VfxAnim.easeOutBack(sat(charge * 1.3f)), age * 0.05f, crossA, rm);
        glow(ctx, buf, f, (0.8f + 2.0f * charge) * pw, bl, 0.7f * sat((0.5f - t) / 0.2f));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, f, (1.0f + 2.4f * charge) * pw, age * 0.06f, wa(rim(inst, 0.2f), 0.9f * sat((0.4f - t) / 0.14f) * charge));

        // flight
        if (fly > 0f && land < 1f) {
            float vis = 1f - land;
            float len = Math.min(4.0f * pw, Math.max(1.2f, dist * e * 0.9f + 0.8f));
            // magenta streak from the origin to the head
            buf.beam(ctx, STREAK, VfxBlend.ADD, f, head, 0.55f * pw, 0.35f * pw, 1, 0, wa(bl, 0f), wa(rim(inst, 0.5f), 0.45f * vis));
            // afterimages
            for (int k = 3; k >= 1; k--) {
                float ek = (float) Math.pow(sat(fly - 0.065f * k), 1.45);
                Vector3f hk = new Vector3f(dn).mul(dist * ek).add(f);
                Vector3f tk = new Vector3f(hk).sub(new Vector3f(dn).mul(len * (0.95f - 0.08f * k)));
                blade(ctx, buf, k % 2 == 0, tk, hk, 1.1f * pw * (1f - 0.1f * k), 0.34f * vis / k + 0.08f * vis, bl, 0.5f);
            }
            // companions riding beside the lance
            for (int s = -1; s <= 1; s += 2) {
                float sw = (0.38f + 0.1f * Mth.sin(age * 0.5f + s)) * pw * s;
                Vector3f hb = new Vector3f(head).sub(new Vector3f(dn).mul(0.5f * pw)).add(new Vector3f(side).mul(sw));
                Vector3f tb = new Vector3f(hb).sub(new Vector3f(dn).mul(len * 0.62f)).add(new Vector3f(side).mul(sw * 0.5f));
                blade(ctx, buf, s > 0, tb, hb, 0.7f * pw, vis, rm, 0.8f);
            }
            // the lance
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dn).mul(len));
            blade(ctx, buf, false, tail, head, 1.25f * pw, vis, rm, 0.8f);
            glow(ctx, buf, head, 1.5f * pw, bl, 0.75f * vis);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 0.9f * pw, age * 0.2f, wa(PALE, 0.7f * vis));
            // speed hatching
            float ha = screenAngle(ctx, dn);
            for (int i = 0; i < 3; i++) {
                float off = (i - 1) * 0.6f * pw + (rnd.nextFloat() - 0.5f) * 0.2f;
                Vector3f hp = new Vector3f(head).sub(new Vector3f(dn).mul((0.9f + 0.5f * i) * pw)).add(new Vector3f(side).mul(off));
                buf.billboard(ctx, HATCH, VfxBlend.ADD, hp, 1.5f * pw, ha, wa(rim(inst, 0.3f), 0.55f * vis));
            }
        } else {
            rnd.nextFloat();
        }
        // crosses dropped along the path
        for (int i = 0; i < 5; i++) {
            float u = 0.2f + 0.14f * i, spawnE = u;
            float off = (rnd.nextFloat() - 0.5f) * 1.6f * pw, sz = (0.28f + 0.2f * rnd.nextFloat()) * pw;
            float since = e - spawnE;
            if (since <= 0f && fly < 1f) continue;
            float a = sat(1f - (t - (0.16f + 0.66f * (float) Math.pow(spawnE, 1f / 1.45f))) / 0.28f);
            Vector3f p = new Vector3f(dn).mul(dist * spawnE).add(f).add(new Vector3f(side).mul(off)).add(new Vector3f(ctx.camUp).mul(0.2f * pw * (t - 0.3f)));
            cross(ctx, buf, p, sz * (0.6f + 0.4f * sat(since * 6f)), age * (i % 2 == 0 ? 0.1f : -0.08f), a * 0.9f, rm);
        }
        // impact at 'to'
        if (land > 0f) {
            Vector3f to = new Vector3f(dn).mul(dist).add(f);
            float k = VfxAnim.easeOutCubic(land);
            glow(ctx, buf, to, (1.0f + 2.4f * k) * pw, bl, 0.9f * (1f - land));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, to, (1.2f + 2.0f * k) * pw, land * 1.2f, wa(PALE, 1f - land));
            burst(ctx, buf, to, (0.8f + 2.2f * k) * pw, land * 0.8f, 1f - land * land, rm);
        }
    }

    // ------------------------------------------------------------------ FX2: zone
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.5f, inst.power);
        float life = life(inst, age, 8f, 12f);
        if (life <= 0.01f) return;
        RandomSource rnd = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        int bl = bloom(inst), rm = rim(inst, 0.3f);
        float open = VfxAnim.easeOutCubic(sat(age / 10f));
        VfxPose ground = VfxPose.ground(c);

        // black cracked pool
        buf.plane(POOL, VfxBlend.ALPHA, ground.lift(0.02f).spin(0.4f), R * 1.02f * open, wa(WHITE, 0.95f * life));
        buf.plane(POOL_RIM, VfxBlend.ADD, ground.lift(0.03f).spin(0.4f), R * 1.04f * open, wa(bl, 0.55f * life));
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.04f), R * 0.9f * open, wa(bl, 0.28f * life * (0.8f + 0.2f * VfxAnim.pulse(age, 1.5f))));
        // sigils
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.06f).spin(age * 0.012f), R * 1.0f * open, wa(rim(inst, 0.5f), 0.85f * life));
        buf.plane(SIGIL_INNER, VfxBlend.ADD, ground.lift(0.07f).spin(-age * 0.02f + 1f), R * 0.62f * open, wa(bl, 0.8f * life));
        // pulses running outwards
        for (int k = 0; k < 2; k++) {
            float ph = ((age / 28f) + k * 0.5f) % 1f;
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.08f), R * (0.15f + 1.0f * VfxAnim.easeOutCubic(ph)) * open, wa(rim(inst, 0.4f), 0.8f * life * (1f - ph) * (1f - ph)));
        }

        // wall of black shards rising round the rim, leaning inwards
        int n = ctx.seg(10, 6);
        float hBase = 1.0f + 0.35f * R;
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + 0.2f, rr = R * (0.9f + 0.1f * rnd.nextFloat()), hv = 0.65f + 0.7f * rnd.nextFloat();
            float grow = VfxAnim.easeOutBack(sat((age - 2f - i * 0.8f) / 11f)) * sat(life * 1.3f);
            if (grow <= 0.02f) continue;
            float h = Math.min(hBase * hv, 6f) * grow * (1f + 0.05f * Mth.sin(age * 0.2f + i));
            Vector3f base = new Vector3f(c.x + Mth.cos(a) * rr, c.y, c.z + Mth.sin(a) * rr);
            Vector3f tip = new Vector3f(base).add(0, h, 0).sub(new Vector3f(Mth.cos(a), 0, Mth.sin(a)).mul(h * 0.22f));
            blade(ctx, buf, i % 2 == 1, base, tip, Math.min(h * 0.45f, 1.6f), Math.min(1f, life * 1.1f), rm, 0.85f);
        }

        // shaft of black light down the middle
        float shaftH = 5f + R;
        float shaftA = life * (0.35f + 0.25f * VfxAnim.pulse(age, 1.2f)) * VfxAnim.easeOutCubic(sat((age - 4f) / 10f));
        float sw = Math.min(0.4f * R + 0.3f, 1.5f);
        Vector3f top = new Vector3f(c).add(0, shaftH, 0);
        buf.beam(ctx, BEAM, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.05f, 0), top, sw, sw * 0.85f, 1, 0, wa(WHITE, shaftA), wa(WHITE, shaftA * 0.4f));
        buf.beam(ctx, BEAM_RIM, VfxBlend.ADD, new Vector3f(c).add(0, 0.05f, 0), top, sw * 1.05f, sw * 0.9f, 1, -age * 0.02f, wa(bl, shaftA * 1.4f), wa(bl, 0f));
        buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(c).add(0, 0.05f, 0), top, sw * 0.4f, sw * 0.3f, 1, -age * 0.03f, wa(rim(inst, 0.2f), shaftA * 0.9f), wa(PALE, 0f));

        // rising crosses and motes
        for (int i = 0; i < 6; i++) {
            float ph = ((age * 0.018f) + i / 6f + rnd.nextFloat()) % 1f;
            float a = Mth.TWO_PI * i / 6f + age * 0.012f, rr = R * (0.25f + 0.6f * rnd.nextFloat());
            Vector3f p = new Vector3f(c.x + Mth.cos(a) * rr, c.y + 0.2f + ph * (2.2f + 0.25f * R), c.z + Mth.sin(a) * rr);
            cross(ctx, buf, p, (0.38f + 0.2f * rnd.nextFloat()) * Math.min(1.6f, 0.6f + 0.2f * R), age * (i % 2 == 0 ? 0.04f : -0.05f), life * Mth.sin(ph * Mth.PI), rm);
        }
        for (int i = 0; i < 8; i++) {
            float ph = ((age * 0.03f) + i / 8f + rnd.nextFloat()) % 1f;
            float a = rnd.nextFloat() * Mth.TWO_PI, rr = R * 0.95f * (float) Math.sqrt(rnd.nextFloat());
            Vector3f p = new Vector3f(c.x + Mth.cos(a) * rr, c.y + 0.1f + ph * (1.8f + 0.2f * R), c.z + Mth.sin(a) * rr);
            buf.billboard(ctx, MOTE, VfxBlend.ADD, p, 0.22f + 0.1f * rnd.nextFloat(), age * 0.1f + i, wa(i % 2 == 0 ? PALE : bl, life * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ FX3: impact
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = sat(age / Math.max(1f, inst.duration));
        float pw = Mth.clamp(inst.power, 0.6f, 3.0f);
        RandomSource rnd = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        int bl = bloom(inst), rm = rim(inst, 0.3f);
        Vector3f d = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasDir = d.lengthSquared() > 0.09f;
        float fadeOut = sat((1f - t) / 0.45f);

        // flash
        float fl = sat(t / 0.22f);
        glow(ctx, buf, c, pw * 3.0f * VfxAnim.easeOutCubic(fl), bl, 0.7f * sat(1f - t / 0.3f));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, pw * 4.0f * VfxAnim.easeOutCubic(fl), age * 0.05f, wa(PALE, 0.8f * sat(1f - t / 0.2f)));
        // black starbursts, counter-rotating
        float grow = VfxAnim.easeOutCubic(sat(t / 0.5f));
        burst(ctx, buf, c, pw * (1.6f + 4.6f * grow), 0.3f + t * 0.5f, sat(t / 0.05f) * fadeOut, rm);
        burst(ctx, buf, c, pw * (0.8f + 2.6f * grow), 1.3f - t * 0.7f, sat(t / 0.05f) * fadeOut * 0.9f, rim(inst, 0.7f));
        // shock rings: one facing the camera, one flat on the ground
        float rg = VfxAnim.easeOutCubic(sat(t / 0.6f));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, pw * (1.0f + 7.0f * rg), 0f, wa(rim(inst, 0.5f), 0.9f * (1f - sat(t / 0.6f))));
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(c).lift(-0.0f), pw * (0.6f + 3.6f * rg), wa(bl, 0.8f * (1f - sat(t / 0.65f))));

        // torn shards flung out
        float s = VfxAnim.easeOutCubic(sat(t / 0.65f));
        for (int i = 0; i < 12; i++) {
            float a = Mth.TWO_PI * i / 12f + (rnd.nextFloat() - 0.5f) * 0.4f, sp = 0.7f + 0.6f * rnd.nextFloat(), ln = (1.1f + 1.3f * rnd.nextFloat()) * pw;
            float r0 = (0.3f + 3.0f * s * sp) * pw;
            Vector3f b0 = vp(ctx, c, a, r0), b1 = vp(ctx, c, a + 0.04f * (i % 3 - 1), r0 + ln * (0.4f + 0.6f * s));
            blade(ctx, buf, i % 2 == 1, b0, b1, 0.5f * pw * (0.8f + 0.5f * (i % 3) / 2f), fadeOut * sat(t / 0.04f), rm, 0.9f);
        }
        // the spear along the hint direction
        if (hasDir) {
            Vector3f dn = new Vector3f(d).normalize();
            float L = 4.2f * pw * (0.4f + 0.6f * s);
            Vector3f a0 = new Vector3f(dn).mul(0.2f * pw + 1.2f * pw * s).add(c);
            blade(ctx, buf, false, a0, new Vector3f(a0).add(new Vector3f(dn).mul(L)), 0.85f * pw, fadeOut * sat(t / 0.04f), rm, 1f);
        }
        // crosses
        for (int i = 0; i < 6; i++) {
            float a = Mth.TWO_PI * i / 6f + 0.5f + (rnd.nextFloat() - 0.5f) * 0.5f, sp = 0.6f + 0.8f * rnd.nextFloat();
            Vector3f p = vp(ctx, c, a, (0.5f + 3.4f * s * sp) * pw);
            p.add(0, -0.4f * pw * t * t, 0);
            cross(ctx, buf, p, (0.38f + 0.25f * rnd.nextFloat()) * pw, age * (i % 2 == 0 ? 0.12f : -0.1f), fadeOut * sat(t / 0.06f), rm);
        }
        // hatch lines racing out
        for (int i = 0; i < 4; i++) {
            float a = Mth.TWO_PI * i / 4f + 0.8f + (rnd.nextFloat() - 0.5f) * 0.6f;
            Vector3f p = vp(ctx, c, a, (1.6f + 3.2f * s) * pw);
            buf.billboard(ctx, HATCH, VfxBlend.ADD, p, 1.6f * pw, a, wa(rim(inst, 0.3f), 0.6f * sat(1f - t / 0.5f) * sat(t / 0.05f)));
        }
        // afterglow
        float after = sat((t - 0.15f) / 0.2f) * sat((1f - t) / 0.7f);
        glow(ctx, buf, c, 2.4f * pw, bl, 0.45f * after);
        for (int i = 0; i < 5; i++) {
            float ph = (t * 0.9f + i / 5f + rnd.nextFloat()) % 1f;
            Vector3f p = new Vector3f(c).add((rnd.nextFloat() - 0.5f) * 2.4f * pw, ph * 1.8f * pw, (rnd.nextFloat() - 0.5f) * 2.4f * pw);
            buf.billboard(ctx, MOTE, VfxBlend.ADD, p, 0.3f * pw, age * 0.1f + i, wa(i % 2 == 0 ? PALE : bl, after * Mth.sin(ph * Mth.PI)));
        }
    }
}
