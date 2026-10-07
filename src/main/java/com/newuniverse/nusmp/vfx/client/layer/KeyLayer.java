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
 * Key Magic: ornate golden keys that open spatial rifts and ethereal doors (gold metal, violet space behind the doors).
 * The palette is always gold + violet; {@code inst.color} is mixed into the gold (40 %).
 * <ul>
 *   <li>KEY_FX1 = CAST / PROJECTILE. 'from' = hand, 'to' = target, power = size, duration = flight ticks (16). First quarter: a keyhole
 *       opens at the hand inside a contracting ring while light motes are drawn in. Then a tall golden key flies to the target,
 *       turning about its shaft, with three fading afterimages, a violet-gold streak trail and twinkling glints along the path. At the
 *       end the key turns the lock: a keyhole flashes open at the target inside an expanding ring with a few gold shards.</li>
 *   <li>KEY_FX2 = ZONE / FIELD / DOME. 'from' = ground centre, power = RADIUS (the sigil is drawn to it), duration = life ticks (80). A
 *       rotating keyhole sigil on the ground with a counter-rotating inner sigil, a rim of ethereal doors that swing open one after the
 *       other, a giant key hanging over the central keyhole in a column of light, shock rings pulsing outwards and gold motes rising.
 *       Fades in over 8 ticks, out over the last 12.</li>
 *   <li>KEY_FX3 = IMPACT / BURST / SIGNATURE. 'from' = centre, 'to' = optional direction hint (shards are thrown that way), power =
 *       scale, duration = life ticks (28). A giant key drops from above into a spatial rift, a white flash, the rift opens as a
 *       violet-white slit with a keyhole ring, a sigil and two shock rings expand on the ground, shards, small keys and sparks fly
 *       out, and a lingering rift glow fades.</li>
 * </ul>
 */
public class KeyLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation KEY = t("key_key");
    public static final ResourceLocation KEYHOLE = t("key_keyhole");
    public static final ResourceLocation DOOR = t("key_door");
    public static final ResourceLocation SIGIL = t("key_sigil");
    public static final ResourceLocation RING = t("key_ring");
    public static final ResourceLocation RIFT = t("key_rift");
    public static final ResourceLocation GLINT = t("key_glint");
    public static final ResourceLocation SHARD = t("key_shard");
    public static final ResourceLocation STREAK = t("key_streak");

    private static final int GOLD = 0xFFFFD04A;
    private static final int SPACE = 0xFF8A62F0;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.KEY_FX1, VfxShape.KEY_FX2, VfxShape.KEY_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case KEY_FX1 -> 16;
            case KEY_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GOLD; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.KEY_FX3) VfxShake.add(inst.payload.from(), 0.9f * Math.min(2f, inst.power), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case KEY_FX1 -> cast(inst, ctx, buf);
            case KEY_FX2 -> zone(inst, ctx, buf);
            case KEY_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    private static float clamp01(float v) { return Mth.clamp(v, 0f, 1f); }

    /** ramps 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : clamp01(age / in);
        float b = out <= 0 ? 1 : clamp01((inst.duration - age) / out);
        return Math.min(a, b);
    }

    private static int gold(VfxInstance inst) { return VfxVertexBuffer.lerpColor(GOLD, inst.color | 0xFF000000, 0.4f); }

    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, clamp01(k)); }

    // ------------------------------------------------------------------ FX1: the key is thrown
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(4f, inst.duration), p = Math.max(0.4f, inst.power);
        int gold = gold(inst), hot = VfxVertexBuffer.whiten(gold, 0.6f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        RandomSource r = inst.random();

        float chargeEnd = 0.25f * d, arrive = 0.78f * d;
        // ---- charge at the hand: a keyhole opens inside a contracting ring, motes are drawn in
        float u = clamp01(age / chargeEnd);
        float chargeFade = age < chargeEnd ? 1f : clamp01(1f - (age - chargeEnd) / (0.2f * d));
        if (chargeFade > 0.01f) {
            float open = VfxAnim.easeOutBack(u);
            buf.billboard(ctx, RING, VfxBlend.ADD, from, p * (1.9f - 1.0f * u), age * 0.12f, a(gold, 0.75f * chargeFade * (0.3f + 0.7f * u)));
            buf.billboard(ctx, KEYHOLE, VfxBlend.ADD, from, p * 0.95f * open, 0, a(hot, 0.9f * chargeFade));
            VfxBloom.glow(ctx, buf, from, p * 0.55f * u, SPACE, 0.55f * chargeFade);
            int n = ctx.seg(5, 3);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + r.nextFloat() + age * 0.25f, rad = (0.5f + 0.7f * r.nextFloat()) * p * (1f - u * 0.85f);
                Vector3f q = new Vector3f(Mth.cos(ang) * rad, Mth.sin(ang * 1.3f) * rad * 0.8f, Mth.sin(ang) * rad * 0.6f).add(from);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, p * 0.3f, ang, a(hot, 0.9f * chargeFade * u));
            }
        }

        // ---- flight
        float f = clamp01((age - chargeEnd) / (arrive - chargeEnd));
        if (age >= chargeEnd * 0.9f && age < arrive + 1f) {
            float fe = 0.3f * f + 0.7f * f * f;
            Vector3f path = new Vector3f(to).sub(from);
            float total = path.length();
            Vector3f dir = total < 1e-4f ? new Vector3f(0, 0, 1) : new Vector3f(path).div(total);
            Vector3f pos = new Vector3f(dir).mul(total * fe).add(from);
            float keyLen = 2.0f * p, spin = Math.abs(Mth.cos(age * 0.55f)) * 0.7f + 0.3f;
            float vis = clamp01((age - chargeEnd * 0.9f) / 2f) * (age < arrive ? 1f : clamp01(arrive + 1f - age));
            // trail: streak from behind the key to the key
            float trailLen = Math.min(total * fe, 3.2f * p);
            Vector3f tail = new Vector3f(dir).mul(-trailLen).add(pos);
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, new Vector3f(pos).add(new Vector3f(dir).mul(keyLen * 0.2f)), 1.3f * p, 1.3f * p, 1, 0,
                    a(SPACE, 0.0f), a(VfxVertexBuffer.lerpColor(gold, WHITE, 0.3f), 0.85f * vis));
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(dir).mul(-trailLen * 0.55f).add(pos), pos, 0.34f * p, 0.34f * p, 1, 0.3f,
                    a(gold, 0f), a(WHITE, 0.9f * vis));
            VfxBloom.glow(ctx, buf, pos, 0.9f * p, SPACE, 0.65f * vis);
            // afterimages
            for (int k = 3; k >= 1; k--) {
                Vector3f gp = new Vector3f(dir).mul(-keyLen * 0.55f * k).add(pos);
                keyQuad(buf, KEY, VfxBlend.ADD, gp, dir, keyLen * (1f - 0.06f * k), 0.5f * p * spin * (1f + 0.12f * k), a(k == 1 ? gold : SPACE, 0.34f * vis / k));
            }
            // the key: solid gold plus an additive copy for the glow
            keyQuad(buf, KEY, VfxBlend.ALPHA, pos, dir, keyLen, 0.95f * p * spin, a(gold, vis));
            keyQuad(buf, KEY, VfxBlend.ADD, pos, dir, keyLen * 1.04f, 1.0f * p * spin, a(hot, 0.45f * vis));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(dir).mul(keyLen * 0.45f).add(pos), 0.9f * p, age * 0.2f, a(WHITE, 0.9f * vis));
            // glints twinkling along the path
            int n = ctx.seg(4, 2);
            for (int i = 0; i < n; i++) {
                float s = (i + 1f) / (n + 1f) * fe;
                Vector3f q = new Vector3f(dir).mul(total * s).add(from);
                q.add((r.nextFloat() - 0.5f) * 0.5f * p, (r.nextFloat() - 0.5f) * 0.5f * p, (r.nextFloat() - 0.5f) * 0.5f * p);
                float tw = Mth.sin(age * 0.9f + i * 2.1f) * 0.5f + 0.5f;
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.4f * p * (0.4f + tw), age * 0.15f + i, a(hot, 0.8f * tw * vis));
            }
        }

        // ---- the lock turns at the target
        if (age > arrive - 1f) {
            float v = clamp01((age - arrive + 1f) / Math.max(2f, d - arrive + 1f));
            float fade = 1f - v * v;
            float open = VfxAnim.easeOutCubic(clamp01(v * 2.2f));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, to, p * (1.4f + 1.2f * open), 0.4f * v, a(WHITE, 0.95f * fade));
            buf.billboard(ctx, KEYHOLE, VfxBlend.ADD, to, p * (0.5f + 1.0f * open), 0, a(hot, 0.85f * fade));
            buf.billboard(ctx, RING, VfxBlend.ADD, to, p * (0.8f + 2.4f * open), -v, a(gold, 0.8f * fade));
            VfxBloom.glow(ctx, buf, to, p * 0.9f, SPACE, 0.8f * fade);
            int n = ctx.seg(5, 3);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.8f, sp = (0.6f + r.nextFloat() * 0.8f) * p * open;
                Vector3f q = new Vector3f(Mth.cos(ang) * sp, Mth.sin(ang) * sp * 0.9f - 0.3f * v * p, 0).add(to);
                buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, 0.32f * p, ang * 2f + v * 4f, a(gold, fade));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: the sigil and the doors
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int gold = gold(inst), hot = VfxVertexBuffer.whiten(gold, 0.55f);
        Vector3f c = ctx.rel(inst.from(ctx));
        RandomSource r = inst.random();
        float grow = VfxAnim.easeOutCubic(clamp01(age / 10f));
        float rad = R * grow;

        // ground: violet space under gold sigils, one turning each way
        VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, floor, rad * 1.35f, a(SPACE, 0.33f * fade));
        buf.plane(SIGIL, VfxBlend.ALPHA, floor.lift(0.01f).spin(age * 0.018f), rad, a(VfxVertexBuffer.lerpColor(gold, 0xFF6A4A10, 0.35f), 0.6f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.018f), rad, a(hot, 0.85f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.03f).spin(-age * 0.03f + 0.5f), rad * 0.5f, a(WHITE, 0.55f * fade));

        // pulses
        for (int k = 0; k < 2; k++) {
            float ph = ((age + k * 20f) % 40f) / 40f;
            float e = VfxAnim.easeOutCubic(ph);
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.04f), rad * (0.15f + 0.9f * e), a(gold, 0.8f * (1f - ph) * fade));
        }

        // rim of ethereal doors, opening one after the other
        int doors = Mth.clamp(Math.round(R * 1.1f), 5, 8);
        float h = Mth.clamp(1.5f + R * 0.25f, 1.7f, 3.4f), w = h * 0.5f * 0.5f;
        for (int i = 0; i < doors; i++) {
            float ang = Mth.TWO_PI * i / doors + 0.4f;
            float open = VfxAnim.easeOutBack(clamp01((age - 3f - i * 2.2f) / 10f));
            if (open <= 0.01f) continue;
            Vector3f base = new Vector3f(Mth.cos(ang) * rad * 0.9f, 0.05f, Mth.sin(ang) * rad * 0.9f).add(c);
            Vector3f side = new Vector3f(-Mth.sin(ang), 0, Mth.cos(ang)).mul(w * Math.max(0.05f, open));
            Vector3f up = new Vector3f(0, h * Mth.clamp(open, 0f, 1.05f), 0);
            float flick = 0.8f + 0.2f * Mth.sin(age * 0.4f + i * 1.7f);
            VfxBloom.glow(ctx, buf, new Vector3f(base).add(0, h * 0.5f, 0), h * 0.4f, SPACE, 0.4f * fade * flick);
            standQuad(buf, DOOR, VfxBlend.ALPHA, base, side, up, a(VfxVertexBuffer.lerpColor(gold, 0xFF6A4A10, 0.25f), fade));
            standQuad(buf, DOOR, VfxBlend.ADD, base, side, up, a(hot, 0.55f * fade * flick));
        }

        // the great key hanging over the central keyhole inside a column of light
        float hang = (1.4f + R * 0.18f) + 0.12f * Mth.sin(age * 0.15f);
        float keyH = Mth.clamp(1.4f + R * 0.3f, 1.8f, 3.6f);
        float spin = Math.abs(Mth.cos(age * 0.07f)) * 0.8f + 0.2f;
        Vector3f top = new Vector3f(0, hang + keyH, 0).add(c), bottom = new Vector3f(0, 0.1f, 0).add(c);
        buf.beam(ctx, STREAK, VfxBlend.ADD, top, bottom, keyH * 0.5f, keyH * 0.3f, 1, 0, a(SPACE, 0.0f), a(gold, 0.55f * fade));
        Vector3f kc = new Vector3f(0, hang + keyH * 0.5f, 0).add(c);
        keyQuad(buf, KEY, VfxBlend.ALPHA, kc, new Vector3f(0, -1, 0), keyH, keyH * 0.42f * spin, a(gold, fade));
        keyQuad(buf, KEY, VfxBlend.ADD, kc, new Vector3f(0, -1, 0), keyH * 1.03f, keyH * 0.44f * spin, a(hot, 0.4f * fade));
        VfxBloom.glow(ctx, buf, kc, keyH * 0.5f, gold, 0.5f * fade);

        // rising motes
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, dist = Mth.sqrt(r.nextFloat()) * rad * 0.95f;
            float speed = 0.012f + 0.02f * r.nextFloat(), phase = r.nextFloat();
            float y = ((age * speed + phase) % 1f) * (2f + R * 0.3f);
            float tw = Mth.sin(age * 0.3f + i * 1.9f) * 0.5f + 0.5f;
            Vector3f q = new Vector3f(Mth.cos(ang) * dist, y, Mth.sin(ang) * dist).add(c);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.28f + 0.2f * tw, age * 0.1f + i, a(hot, (0.35f + 0.6f * tw) * fade * (1f - y / (2.5f + R * 0.3f))));
        }
    }

    // ------------------------------------------------------------------ FX3: the key drops and the rift opens
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), p = Math.max(0.4f, inst.power);
        int gold = gold(inst), hot = VfxVertexBuffer.whiten(gold, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        RandomSource r = inst.random();
        float hit = 5f;                                    // tick of the impact
        float since = Math.max(0f, age - hit), life = clamp01(since / (d - hit));
        float fade = 1f - life * life;

        // ---- the key drops from above into the rift
        float drop = clamp01(age / hit);
        if (age < hit + 9f) {
            float h = 4.2f * p * (1f - drop * drop) + 0.2f * p;
            float out = age < hit ? 1f : clamp01(1f - (age - hit) / 9f);
            float keyH = 2.2f * p;
            Vector3f kc = new Vector3f(0, 1.1f * p + h + keyH * 0.5f, 0).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(kc).add(0, keyH * 1.6f * (1f - drop * 0.6f), 0), new Vector3f(kc).add(0, -keyH * 0.45f, 0), 0.7f * p, 0.7f * p, 1, 0,
                    a(SPACE, 0f), a(gold, 0.8f * out * drop));
            keyQuad(buf, KEY, VfxBlend.ALPHA, kc, new Vector3f(0, -1, 0), keyH, 0.85f * p * (0.35f + 0.65f * Math.abs(Mth.cos(age * 0.6f))), a(gold, out));
            keyQuad(buf, KEY, VfxBlend.ADD, kc, new Vector3f(0, -1, 0), keyH * 1.05f, 0.9f * p, a(hot, 0.45f * out));
        }

        // ---- the rift: a vertical slit that opens, turns into a thin line and fades; a keyhole ring around it
        float slitOpen = VfxAnim.easeOutBack(clamp01((age - hit * 0.6f) / 6f));
        float slitFade = age < hit ? clamp01(age / hit) * 0.5f : fade;
        float riftH = 3.0f * p, riftW = riftH * 0.5f * (0.45f + 0.55f * (1f - life * 0.7f));
        Vector3f mid = new Vector3f(0, 1.1f * p, 0).add(c);
        if (slitOpen > 0.01f) {
            VfxBloom.glow(ctx, buf, mid, riftH * 0.45f, SPACE, 0.9f * slitFade);
            Vector3f side = new Vector3f(ctx.camRight).mul(riftW * 0.5f * slitOpen);
            Vector3f up = new Vector3f(0, riftH * 0.5f * Math.min(1f, slitOpen), 0);
            Vector3f lo = new Vector3f(mid).sub(up);
            standQuad(buf, RIFT, VfxBlend.ADD, lo, side, new Vector3f(up).mul(2f), a(VfxVertexBuffer.lerpColor(SPACE, WHITE, 0.55f), 0.95f * slitFade));
            standQuad(buf, RIFT, VfxBlend.ADD, lo, new Vector3f(side).mul(0.6f), new Vector3f(up).mul(2f), a(hot, 0.7f * slitFade));
        }
        // flash
        float flash = age < hit ? 0f : clamp01(1f - since / 7f);
        if (flash > 0.01f) {
            buf.billboard(ctx, GLINT, VfxBlend.ADD, mid, p * (3.5f + 3f * (1f - flash)), 0.2f * since, a(WHITE, flash));
            buf.billboard(ctx, KEYHOLE, VfxBlend.ADD, mid, p * (1.0f + 2.2f * VfxAnim.easeOutCubic(clamp01(since / 8f))), 0, a(hot, 0.8f * flash));
        }

        // ---- ground: sigil flash and two shock rings
        if (age >= hit - 1f) {
            VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
            float e = VfxAnim.easeOutCubic(clamp01(since / 10f));
            buf.plane(SIGIL, VfxBlend.ADD, floor.spin(since * 0.05f), p * (0.9f + 1.3f * e), a(hot, 0.75f * fade * fade));
            for (int k = 0; k < 2; k++) {
                float rt = clamp01((since - k * 3f) / (d - hit - 4f));
                if (rt <= 0f) continue;
                float ee = VfxAnim.easeOutCubic(rt);
                buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f * (k + 1)), p * (0.4f + (2.8f + k * 1.2f) * ee), a(k == 0 ? gold : SPACE, 0.9f * (1f - rt)));
            }
        }

        // ---- debris: shards and small keys flying out, sparks, biased along the hint
        if (age >= hit) {
            Vector3f bias = hint.lengthSquared() < 1e-4f ? new Vector3f() : new Vector3f(hint).normalize().mul(0.9f);
            int shards = ctx.seg(9, 4);
            for (int i = 0; i < shards; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, up = 0.3f + r.nextFloat() * 0.9f, sp = (1.5f + r.nextFloat() * 2.5f) * p;
                float tt = since / 20f;
                Vector3f q = new Vector3f(Mth.cos(ang) * sp * 0.6f + bias.x * sp * 0.5f, up * p * 2.2f + bias.y * sp * 0.4f, Mth.sin(ang) * sp * 0.6f + bias.z * sp * 0.5f)
                        .mul(VfxAnim.easeOutCubic(clamp01(tt * 1.3f))).add(0, -1.8f * p * tt * tt, 0).add(c).add(0, 1.0f * p, 0);
                buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, (0.28f + 0.3f * r.nextFloat()) * p, ang * 2f + since * (0.2f + 0.3f * r.nextFloat()), a(gold, fade));
            }
            int keys = ctx.seg(4, 2);
            for (int i = 0; i < keys; i++) {
                float ang = Mth.TWO_PI * i / keys + r.nextFloat() * 1.2f, sp = (2.2f + r.nextFloat() * 1.8f) * p;
                float ee = VfxAnim.easeOutCubic(clamp01(since / 16f));
                Vector3f dir = new Vector3f(Mth.cos(ang), 0.35f + 0.3f * r.nextFloat(), Mth.sin(ang)).normalize();
                Vector3f q = new Vector3f(dir).mul(sp * ee).add(0, 1.1f * p - 0.8f * p * life * life, 0).add(c);
                keyQuad(buf, KEY, VfxBlend.ALPHA, q, dir, 0.75f * p, 0.3f * p * (0.4f + 0.6f * Math.abs(Mth.cos(since * 0.5f + i))), a(gold, fade));
            }
            int sparks = ctx.seg(8, 4);
            for (int i = 0; i < sparks; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, el = (r.nextFloat() - 0.3f) * 1.2f, sp = (1.8f + r.nextFloat() * 3.2f) * p;
                float ee = VfxAnim.easeOutCubic(clamp01(since / 14f));
                Vector3f q = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el)).mul(sp * ee).add(c).add(0, 1.1f * p, 0);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.4f * p * (1f - life), ang + since * 0.2f, a(i % 2 == 0 ? hot : SPACE, 0.9f * fade));
            }
        }
        // afterglow
        if (age >= hit) VfxBloom.glow(ctx, buf, mid, 1.2f * p, gold, 0.5f * fade * fade);
    }

    // ------------------------------------------------------------------ primitives
    /**
     * A quad whose texture top points along {@code head} (camera relative), turned to face the camera about that axis. {@code width}
     * is across the key, {@code len} along it. Texture: tail at the bottom (key.png: bow below, bit above).
     */
    static void keyQuad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f centre, Vector3f head, float len, float width, int argb) {
        if (head.lengthSquared() < 1e-8f || centre.lengthSquared() < 1e-8f) return;
        Vector3f d = new Vector3f(head).normalize();
        Vector3f toCam = new Vector3f(centre).negate().normalize();
        Vector3f side = new Vector3f(d).cross(toCam);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(width * 0.5f);
        Vector3f h = new Vector3f(d).mul(len * 0.5f);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(centre).sub(side).sub(h), new Vector3f(centre).add(side).sub(h),
                new Vector3f(centre).add(side).add(h), new Vector3f(centre).sub(side).add(h), 0, 0, 1, 1, col, col);
    }

    /** An upright quad: {@code base} = bottom centre, {@code side} = half width vector, {@code up} = full height vector. */
    static void standQuad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, Vector3f side, Vector3f up, int argb) {
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(base).sub(side), new Vector3f(base).add(side),
                new Vector3f(base).add(side).add(up), new Vector3f(base).sub(side).add(up), 0, 0, 1, 1, col, col);
    }
}
