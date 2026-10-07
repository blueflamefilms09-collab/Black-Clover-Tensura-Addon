package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hoop;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.life;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.side;

/**
 * Barrier Magic: refractive honeycomb glass. Every effect is built from hexagonal panes, a fresnel-rimmed glass sphere and fine
 * silver / sky-blue light; textures come from tools/gen_barrier_textures.py. The palette is deep steel-blue (0x2A6A8A), sky blue
 * (0x6AD0FF) and silver-white; {@code inst.color} is mixed in at about a third.
 * <ul>
 *   <li>BARRIER_FX1 (cast / projectile): a barrier lance. A ring of six hexagon panes closes in on the hand (charge), then a
 *       glass lance flies from {@code from} to {@code to}: a hex-chevron beam, a bevelled hexagon head with a counter-turning
 *       white pane in front of it, and a tunnel of hexagon plates that stay behind as an afterimage while glass splinters drop
 *       off. It ends in a lens flare, a pulse ring and a crack web on a plane facing the flight. power = size (0.6 to 3.0),
 *       duration = flight ticks.</li>
 *   <li>BARRIER_FX2 (zone / dome): a geodesic dome of radius {@code power} around {@code from}. On the ground a sigil turns one
 *       way and a honeycomb the other, with pulse rings running out; three bands of hex wall make the shell, a light wave
 *       climbs through the bands, hexagon panes blink on the surface, a faint fresnel sphere gives it the glass silhouette
 *       and motes rise inside. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>BARRIER_FX3 (impact / burst): the barrier shatters. A glint flash, hexagon panes expanding as concentric shockwaves,
 *       a ground pulse, a crack web, a glass bubble that blooms and pops, then splinters and tumbling hexagon fragments flying
 *       out (biased along {@code to - from} when given) with a lingering honeycomb on the floor. power = scale.</li>
 * </ul>
 */
public class BarrierLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation CELL = t("barrier_hex_cell"), GRID = t("barrier_hex_grid"), BAND = t("barrier_hex_band"),
            SHARD = t("barrier_shard"), SIGIL = t("barrier_sigil"), FLARE = t("barrier_flare"), BEAM = t("barrier_beam"),
            CRACK = t("barrier_crack"), ORB = t("barrier_orb"), RING = t("barrier_ring");

    static final int DEEP = 0xFF2A6A8A, SKY = 0xFF6AD0FF, SILVER = 0xFFDDEBFF, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BARRIER_FX1, VfxShape.BARRIER_FX2, VfxShape.BARRIER_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BARRIER_FX1 -> 16;
            case BARRIER_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF6AD0FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BARRIER_FX1 -> cast(inst, ctx, buf);
            case BARRIER_FX2 -> dome(inst, ctx, buf);
            case BARRIER_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Palette colour mixed a third of the way with the spell tint. */
    private static int mix(VfxInstance inst, int base) {
        return VfxVertexBuffer.lerpColor(base, inst.color | 0xFF000000, 0.33f);
    }

    /** A flat quad centred on c spanned by the unit vectors right / up, half extents hw / hh. */
    private static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float hw, float hh, int argb) {
        Vector3f r = new Vector3f(right).mul(hw), u = new Vector3f(up).mul(hh);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, argb, argb);
    }

    /** A pane facing along n, rolled by 'roll', half-size h. */
    private static void plate(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f n, float roll, float h, int argb) {
        Vector3f r0 = side(n), u0 = new Vector3f(r0).cross(n).normalize();
        float co = Mth.cos(roll), si = Mth.sin(roll);
        Vector3f r = new Vector3f(r0).mul(co).add(new Vector3f(u0).mul(si));
        Vector3f u = new Vector3f(u0).mul(co).sub(new Vector3f(r0).mul(si));
        panel(buf, tex, blend, c, r, u, h, h, argb);
    }

    private static Vector3f dirOf(Vector3f a, Vector3f b, float[] len) {
        Vector3f d = new Vector3f(b).sub(a);
        len[0] = d.length();
        if (len[0] < 0.2f) { d.set(0, 0, 1); len[0] = 6f; } else d.div(len[0]);
        return d;
    }

    // ------------------------------------------------------------------ FX1: barrier lance

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0, 1), s = Math.max(0.3f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        float[] L = new float[1];
        Vector3f dir = dirOf(c0, ctx.rel(inst.to(ctx)), L);
        Vector3f c1 = new Vector3f(c0).add(new Vector3f(dir).mul(L[0]));
        int deep = mix(inst, DEEP), sky = mix(inst, SKY), silver = mix(inst, SILVER);

        // charge: six hexagon panes close in on the hand, a ring and a glint
        float ct = Mth.clamp(t / 0.26f, 0, 1);
        if (t < 0.34f) {
            float cf = (1 - Mth.clamp((t - 0.22f) / 0.12f, 0, 1));
            float rr = (0.15f + 0.85f * (1 - VfxAnim.easeOutCubic(ct))) * s;
            for (int i = 0; i < 6; i++) {
                float a = Mth.TWO_PI * i / 6 + ct * 1.8f;
                Vector3f side = side(dir), up = new Vector3f(side).cross(dir).normalize();
                Vector3f p = new Vector3f(c0).add(new Vector3f(side).mul(Mth.cos(a) * rr)).add(new Vector3f(up).mul(Mth.sin(a) * rr)).add(new Vector3f(dir).mul(0.15f * s * ct));
                plate(buf, CELL, VfxBlend.ADD, p, dir, a + ct * 2f, 0.17f * s * (0.6f + 0.4f * ct), VfxVertexBuffer.withAlpha(i % 2 == 0 ? sky : silver, 0.85f * cf));
            }
            plate(buf, RING, VfxBlend.ADD, c0, dir, ct * 1.2f, (0.35f + 0.55f * (1 - ct)) * s, VfxVertexBuffer.withAlpha(sky, 0.8f * cf));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, c0, (0.5f + 1.1f * ct) * s, ct * 0.8f, VfxVertexBuffer.withAlpha(silver, cf));
        }

        // flight
        float ft = Mth.clamp((t - 0.2f) / 0.58f, 0, 1);
        float pos = VfxAnim.easeInOutSine(ft);
        Vector3f head = new Vector3f(c0).add(new Vector3f(dir).mul(L[0] * pos));
        if (t >= 0.18f && t < 0.94f) {
            float flightFade = ft >= 1 ? Mth.clamp(1 - (t - 0.78f) / 0.16f, 0, 1) : 1f;
            float travelled = L[0] * pos;
            float tail = Math.min(travelled, 3.6f * s);
            Vector3f tailP = new Vector3f(head).sub(new Vector3f(dir).mul(tail));
            float scroll = -age * 0.22f;
            // halo, then the hex-chevron beam, then a white-hot filament
            buf.beam(ctx, BEAM, VfxBlend.ADD, tailP, head, 0.2f * s, 1.25f * s, 4, scroll * 0.5f, VfxVertexBuffer.withAlpha(deep, 0.55f * flightFade), VfxVertexBuffer.withAlpha(sky, 0.7f * flightFade));
            buf.beam(ctx, BEAM, VfxBlend.ADD, tailP, head, 0.1f * s, 0.62f * s, 6, scroll, VfxVertexBuffer.withAlpha(sky, 0.5f * flightFade), VfxVertexBuffer.withAlpha(silver, 1.0f * flightFade));
            // the tunnel of hexagon plates left behind, each turning a little differently
            for (int i = 1; i <= 6; i++) {
                float back = (0.3f + 0.42f * i) * s;
                if (back > travelled) break;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(back));
                float a = (1 - i / 7.2f) * flightFade;
                float size = s * (0.52f - 0.045f * i) * (1 + 0.05f * Mth.sin(age * 0.6f + i));
                plate(buf, CELL, VfxBlend.ADD, p, dir, i * 0.52f + age * 0.09f * (i % 2 == 0 ? 1 : -1), size, VfxVertexBuffer.withAlpha(i % 3 == 0 ? silver : sky, 0.78f * a));
            }
            // the lance head: glass body, bevelled rim, a counter-turning white pane just ahead of it
            float hr = age * 0.14f;
            plate(buf, CELL, VfxBlend.ALPHA, head, dir, hr, 0.46f * s, VfxVertexBuffer.withAlpha(deep, 0.78f * flightFade));
            plate(buf, CELL, VfxBlend.ADD, new Vector3f(head).add(new Vector3f(dir).mul(0.1f * s)), dir, hr, 0.5f * s, VfxVertexBuffer.withAlpha(sky, 0.95f * flightFade));
            plate(buf, CELL, VfxBlend.ADD, new Vector3f(head).add(new Vector3f(dir).mul(0.34f * s)), dir, -hr * 1.7f, 0.27f * s, VfxVertexBuffer.withAlpha(WHITE, 0.95f * flightFade));
            buf.billboard(ctx, CELL, VfxBlend.ADD, head, 0.8f * s, -hr, VfxVertexBuffer.withAlpha(silver, 0.7f * flightFade));
            VfxBloom.glow(ctx, buf, head, 1.1f * s, sky, 1.0f * flightFade);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 0.95f * s, age * 0.2f, VfxVertexBuffer.withAlpha(silver, 0.8f * flightFade));
            // glass splinters shed behind the head
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                float lt = ((age * 0.09f) + hash(inst.seed, i, 1)) % 1f;
                float back = lt * 2.4f * s;
                if (back > travelled) continue;
                float ang = hash(inst.seed, i, 2) * Mth.TWO_PI, spread = (0.2f + 0.9f * lt) * s * 0.6f;
                Vector3f sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(back)).add(new Vector3f(sd).mul(Mth.cos(ang) * spread)).add(new Vector3f(up).mul(Mth.sin(ang) * spread - 0.25f * lt * lt * s));
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, (0.2f + 0.14f * hash(inst.seed, i, 3)) * s, ang + age * 0.3f, VfxVertexBuffer.withAlpha(silver, (1 - lt) * 0.9f * flightFade));
            }
        }

        // impact at the target: glint, pulse ring on the plane the lance hit, crack web, a few splinters
        if (t > 0.74f) {
            float it = Mth.clamp((t - 0.74f) / 0.26f, 0, 1), e = VfxAnim.easeOutCubic(it), a = (1 - it) * (1 - it * 0.3f);
            VfxBloom.glow(ctx, buf, c1, 1.5f * s * (1 - 0.4f * it), sky, 1.0f * a);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, c1, (1.2f + 1.6f * e) * s, 0.4f + it, VfxVertexBuffer.withAlpha(silver, a));
            plate(buf, RING, VfxBlend.ADD, c1, dir, 0, (0.3f + 1.6f * e) * s, VfxVertexBuffer.withAlpha(sky, 0.9f * a));
            plate(buf, CRACK, VfxBlend.ADD, new Vector3f(c1).sub(new Vector3f(dir).mul(0.05f)), dir, 0.3f, (0.4f + 0.75f * e) * s, VfxVertexBuffer.withAlpha(silver, 0.85f * a));
            plate(buf, CELL, VfxBlend.ADD, c1, dir, it * 1.5f, (0.35f + 0.9f * e) * s, VfxVertexBuffer.withAlpha(sky, 0.55f * a));
            for (int i = 0; i < 5; i++) {
                float ang = hash(inst.seed, i, 7) * Mth.TWO_PI, el = (hash(inst.seed, i, 8) - 0.5f) * 1.4f, d = (0.3f + 1.4f * e) * s * (0.6f + hash(inst.seed, i, 9));
                Vector3f p = new Vector3f(c1).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * d, Mth.sin(ang) * Mth.cos(el) * d);
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.28f * s, ang + it * 4f, VfxVertexBuffer.withAlpha(silver, a));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: geodesic dome

    private void dome(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 8, 12), open = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        int deep = mix(inst, DEEP), sky = mix(inst, SKY), silver = mix(inst, SILVER);
        float shimmer = 0.85f + 0.15f * Mth.sin(age * 0.35f);

        // ground: the sigil turns one way, a honeycomb the other, a glow under both
        VfxPose floor = VfxPose.ground(new Vector3f(g).add(0, 0.04f, 0));
        VfxBloom.planeGlow(buf, floor, R * open, deep, 0.9f * fade);
        buf.plane(GRID, VfxBlend.ADD, floor.spin(-age * 0.012f), R * 0.98f * open, VfxVertexBuffer.withAlpha(sky, 0.55f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.01f).spin(age * 0.02f), R * open, VfxVertexBuffer.withAlpha(silver, 0.95f * fade * shimmer));
        for (int k = 0; k < 2; k++) {
            float p = (age * 0.022f + k * 0.5f) % 1f;
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f), R * (0.2f + 0.85f * p), VfxVertexBuffer.withAlpha(sky, 0.7f * fade * (1 - p) * open));
        }

        // the shell: three bands of honeycomb wall, a light wave climbing through them
        float[] ang = {0f, 0.46f, 0.92f, 1.34f};
        int segs = ctx.seg(16, 8);
        float wave = (age * 0.07f) % 5f;
        for (int b = 0; b < 3; b++) {
            float a0 = ang[b] * open, a1 = ang[b + 1] * open;
            float y0 = R * Mth.sin(a0), y1 = R * Mth.sin(a1);
            float lit = Mth.clamp(1 - Math.abs(wave - 0.5f - b) / 1.1f, 0, 1);
            VfxPose p = VfxPose.ground(new Vector3f(g)).lift((y0 + y1) * 0.5f);
            hoop(buf, BAND, VfxBlend.ADD, p, R * Mth.cos(a0), R * Mth.cos(a1), (y1 - y0) * 0.5f, segs, 3f, (b % 2 == 0 ? 1 : -1) * age * 0.0016f,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(sky, silver, lit), (0.42f + 0.5f * lit) * fade));
        }
        // fresnel silhouette of the glass sphere
        buf.billboard(ctx, ORB, VfxBlend.ADD, new Vector3f(g).add(0, 0.05f, 0), 2.04f * R * open, 0, VfxVertexBuffer.withAlpha(sky, 0.5f * fade * shimmer));
        buf.billboard(ctx, ORB, VfxBlend.ADD, new Vector3f(g).add(0, 0.05f, 0), 1.96f * R * open, 0, VfxVertexBuffer.withAlpha(deep, 0.35f * fade));

        // hexagon panes blinking on the surface
        for (int i = 0; i < 5; i++) {
            float az = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.004f * (i % 2 == 0 ? 1 : -1), el = 0.25f + hash(inst.seed, i, 2) * 0.95f;
            float ph = (age * 0.04f + hash(inst.seed, i, 3)) % 1f, blink = Mth.sin(ph * Mth.PI);
            Vector3f n = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            Vector3f pos = new Vector3f(g).add(new Vector3f(n).mul(R * 0.985f * open));
            Vector3f right = new Vector3f(n).cross(0, 1, 0);
            if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
            right.normalize();
            Vector3f up = new Vector3f(right).cross(n).normalize();
            float h = R * 0.15f * (0.7f + 0.5f * hash(inst.seed, i, 4)) * open;
            panel(buf, CELL, VfxBlend.ADD, pos, right, up, h, h, VfxVertexBuffer.withAlpha(i % 2 == 0 ? silver : sky, 0.85f * blink * blink * fade));
        }
        // motes rising inside
        int m = ctx.seg(12, 5);
        for (int i = 0; i < m; i++) {
            float lt = ((age * 0.018f) + hash(inst.seed, i, 5)) % 1f, az = hash(inst.seed, i, 6) * Mth.TWO_PI;
            float rr = R * 0.9f * (float) Math.sqrt(hash(inst.seed, i, 7)) * open, hy = lt * R * 0.8f;
            float lim = Mth.sqrt(Math.max(0.01f, 1 - hy * hy / (R * R))) ;
            Vector3f p = new Vector3f(g).add(Mth.cos(az) * rr * lim, 0.1f + hy, Mth.sin(az) * rr * lim);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.2f * (0.7f + 0.6f * hash(inst.seed, i, 8)), age * 0.1f + i,
                    VfxVertexBuffer.withAlpha(i % 3 == 0 ? silver : sky, Mth.sin(lt * Mth.PI) * fade));
        }
        // opening flash
        float fl = Mth.clamp(1 - age / 10f, 0, 1);
        if (fl > 0) VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.3f, 0), R * 0.8f, sky, fl);
    }

    // ------------------------------------------------------------------ FX3: the barrier shatters

    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0, 1), s = Math.max(0.3f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        boolean has = hint.lengthSquared() > 0.04f;
        Vector3f dir = has ? hint.normalize() : new Vector3f(0, 1, 0);
        int deep = mix(inst, DEEP), sky = mix(inst, SKY), silver = mix(inst, SILVER);
        float tail = life(inst, age, 0, 8), e = VfxAnim.easeOutCubic(Mth.clamp(t * 1.5f, 0, 1));

        // afterglow on the floor, honeycomb fading slowly
        VfxPose floor = VfxPose.ground(new Vector3f(c));
        float fl = Mth.clamp(1 - age / 7f, 0, 1);
        VfxBloom.glow(ctx, buf, c, 2.4f * s * (1 - 0.3f * t), sky, 0.9f * fl + 0.25f * (1 - t) * tail);
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, (1.4f + 3.4f * e) * s, t * 1.2f, VfxVertexBuffer.withAlpha(silver, fl));
        buf.plane(GRID, VfxBlend.ADD, floor.lift(-0.9f * s).spin(t * 0.6f), (1.2f + 2.4f * e) * s, VfxVertexBuffer.withAlpha(sky, 0.4f * (1 - t) * tail));
        buf.plane(RING, VfxBlend.ADD, floor.lift(-0.9f * s), (0.4f + 3.6f * e) * s, VfxVertexBuffer.withAlpha(sky, 0.8f * (1 - t) * (1 - t)));

        // concentric hexagon shockwaves, turning against each other
        for (int k = 0; k < 3; k++) {
            float dt = Mth.clamp((t - 0.05f * k) / (0.8f - 0.05f * k), 0, 1), ee = VfxAnim.easeOutCubic(dt);
            buf.billboard(ctx, CELL, VfxBlend.ADD, c, (0.9f + (2.0f + 1.5f * k) * ee) * s * 1.6f, (k % 2 == 0 ? 1 : -1) * (0.3f + dt * 1.6f),
                    VfxVertexBuffer.withAlpha(k == 1 ? silver : sky, 0.85f * (1 - dt) * (1 - dt)));
        }
        // the glass bubble blooms and pops
        float bt = Mth.clamp(t / 0.35f, 0, 1);
        if (bt < 1) buf.billboard(ctx, ORB, VfxBlend.ADD, c, (0.6f + 3.0f * VfxAnim.easeOutBack(bt)) * s, 0, VfxVertexBuffer.withAlpha(sky, 0.9f * (1 - bt * bt)));
        // crack web on a plane facing the hint (or the camera)
        float ct = Mth.clamp(t / 0.7f, 0, 1);
        if (has) plate(buf, CRACK, VfxBlend.ADD, c, dir, 0.5f, (0.8f + 1.2f * VfxAnim.easeOutCubic(ct)) * s, VfxVertexBuffer.withAlpha(silver, 0.9f * (1 - ct)));
        else buf.billboard(ctx, CRACK, VfxBlend.ADD, c, (1.6f + 2.4f * VfxAnim.easeOutCubic(ct)) * s, 0.5f, VfxVertexBuffer.withAlpha(silver, 0.9f * (1 - ct)));

        // splinters and tumbling hexagon fragments flying out, falling slowly
        int n = ctx.seg(14, 6);
        for (int i = 0; i < n; i++) {
            float az = hash(inst.seed, i, 1) * Mth.TWO_PI, el = (hash(inst.seed, i, 2) - 0.4f) * 1.7f;
            Vector3f d = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            if (has) d.add(new Vector3f(dir).mul(0.9f)).normalize();
            float dist = s * (0.4f + 3.4f * e) * (0.55f + hash(inst.seed, i, 3));
            Vector3f p = new Vector3f(c).add(new Vector3f(d).mul(dist)).add(0, -1.6f * s * t * t, 0);
            float sz = (0.28f + 0.36f * hash(inst.seed, i, 4)) * s;
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, sz, age * 0.35f * (hash(inst.seed, i, 5) - 0.5f) + i, VfxVertexBuffer.withAlpha(i % 3 == 0 ? sky : silver, Math.min(1f, (1 - t) * 1.5f)));
        }
        int h = ctx.seg(8, 4);
        for (int i = 0; i < h; i++) {
            float az = hash(inst.seed, i, 11) * Mth.TWO_PI, el = (hash(inst.seed, i, 12) - 0.3f) * 1.4f;
            Vector3f d = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
            if (has) d.add(new Vector3f(dir).mul(0.6f)).normalize();
            float dist = s * (0.3f + 2.6f * e) * (0.5f + hash(inst.seed, i, 13));
            Vector3f p = new Vector3f(c).add(new Vector3f(d).mul(dist)).add(0, -1.1f * s * t * t, 0);
            buf.billboard(ctx, CELL, VfxBlend.ADD, p, (0.22f + 0.22f * hash(inst.seed, i, 14)) * s, age * 0.25f * (hash(inst.seed, i, 15) - 0.5f) * 2 + i,
                    VfxVertexBuffer.withAlpha(i % 2 == 0 ? silver : sky, (1 - t) * 0.95f));
        }
        // sparks
        int sp = ctx.seg(8, 3);
        for (int i = 0; i < sp; i++) {
            float az = hash(inst.seed, i, 21) * Mth.TWO_PI, el = (hash(inst.seed, i, 22) - 0.5f) * 2f;
            float dist = s * (0.2f + 4.2f * e) * (0.5f + hash(inst.seed, i, 23));
            Vector3f p = new Vector3f(c).add(Mth.cos(az) * Mth.cos(el) * dist, Mth.sin(el) * dist, Mth.sin(az) * Mth.cos(el) * dist);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.22f * s, i, VfxVertexBuffer.withAlpha(WHITE, (1 - t) * (1 - t)));
        }
    }
}
