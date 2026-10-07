package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.life;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.pointed;

/**
 * Cherry Blossom Magic (Sakura), after the owner's anime stills: a sea of soft cel-shaded pink cumulus made of petals, solid pale
 * petals with a notched tip and fine veins, thin clean linework, pink / white / mauve only. Textures from
 * tools/gen_cherry_blossom_textures.py (petal, flower, cloud, sigil, burst, ribbon, blade, motes).
 * <ul>
 *   <li>CHERRY_BLOSSOM_FX1 (cast / projectile): at 'from' a blossom opens and spins while petals are drawn in (first quarter), then
 *       a petal stream flies to 'to': a pink ribbon with two counter-wound helices of tumbling petals, soft petal clouds, razor
 *       petal blades at the head and fading blossom afterimages; a petal burst blooms at 'to'. power scales the size (0.6 to 3),
 *       duration is the flight time, colour tints the pinks.</li>
 *   <li>CHERRY_BLOSSOM_FX2 (zone): centred on 'from', drawn out to radius = power: a ground seal (ten-petal ring and inner blossom,
 *       two layers turning against each other) with a pulse running out, a bank of petal cloud along the rim and a few inside,
 *       a swirl of petals rising in a spiral, glints, a pale light column. Fades in over 8 ticks, out over the last 12.</li>
 *   <li>CHERRY_BLOSSOM_FX3 (burst / signature): at 'from' a flash and a giant blossom that opens and turns, a seal shock ring on the
 *       ground, puffs of petal cloud, a spray of tumbling petals (biased along 'to' - 'from' when given), razor blades flying
 *       outward and drifting glints that linger as the afterglow. power scales it, colour tints the pinks.</li>
 * </ul>
 */
public class CherryBlossomLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation PETAL = t("cherry_blossom_petal"), FLOWER = t("cherry_blossom_flower"), CLOUD = t("cherry_blossom_cloud"),
            SIGIL = t("cherry_blossom_sigil"), BURST = t("cherry_blossom_burst"), RIBBON = t("cherry_blossom_ribbon"),
            BLADE = t("cherry_blossom_blade"), MOTES = t("cherry_blossom_motes");

    static final int PINK = 0xFFFFB0D0, DEEP = 0xFFE07AA8, PALE = 0xFFFFE6F0, MAUVE = 0xFFC87AA8;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CHERRY_BLOSSOM_FX1, VfxShape.CHERRY_BLOSSOM_FX2, VfxShape.CHERRY_BLOSSOM_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CHERRY_BLOSSOM_FX1 -> 16;
            case CHERRY_BLOSSOM_FX2 -> 80;
            default -> 28;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFFFB0D0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CHERRY_BLOSSOM_FX1 -> cast(inst, ctx, buf);
            case CHERRY_BLOSSOM_FX2 -> zone(inst, ctx, buf);
            case CHERRY_BLOSSOM_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    private static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(PINK, inst.color | 0xFF000000, 0.4f); }

    private static int petalCol(int tint, long seed, int i, float a) {
        float k = hash(seed, i, 9);
        int base = VfxVertexBuffer.lerpColor(PALE, tint, 0.35f + 0.4f * k);
        return VfxVertexBuffer.withAlpha(k > 0.7f ? VfxVertexBuffer.lerpColor(base, DEEP, 0.5f) : base, a);
    }

    /** A tumbling petal facing the camera: rotation 'rot' in the screen plane, 'flutter' phase narrows it as it turns edge-on. */
    private static void petal(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float size, float rot, float flutter, int col) {
        float f = 0.3f + 0.7f * Math.abs(Mth.cos(flutter)), cr = Mth.cos(rot), sr = Mth.sin(rot);
        Vector3f r = new Vector3f(ctx.camRight).mul(cr).add(new Vector3f(ctx.camUp).mul(sr));
        Vector3f u = new Vector3f(ctx.camUp).mul(cr).sub(new Vector3f(ctx.camRight).mul(sr));
        panel(buf, PETAL, VfxBlend.ALPHA, c, r, u, size * 0.5f * f, size * 0.5f, col);
    }

    private static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float hw, float hh, int argb) {
        Vector3f r = new Vector3f(right).mul(hw), u = new Vector3f(up).mul(hh);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, argb, argb);
    }

    /** An upright camera-facing card standing on {@code foot}. */
    private static void stand(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f foot, float w, float h, int argb) {
        Vector3f right = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
        panel(buf, tex, blend, new Vector3f(foot).add(0, h / 2, 0), right.normalize(), new Vector3f(0, 1, 0), w / 2, h / 2, argb);
    }

    private static float screenAngle(VfxRenderContext ctx, Vector3f d) { return Mth.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)); }

    // ------------------------------------------------------------------ FX1

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), t = Mth.clamp(age / dur, 0, 1);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        int col = tint(inst);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 0.25f) { dir.set(ctx.camRight).mul(3f); b = new Vector3f(a).add(dir); }
        float len = dir.length();
        float fade = life(inst, age, 0, 2);

        // charge: a blossom opens at the hand, petals are drawn in
        float cq = Mth.clamp(age / (dur * 0.28f), 0, 1);
        if (t < 0.34f) {
            float out = 1 - Mth.clamp((t - 0.26f) / 0.08f, 0, 1);
            VfxBloom.glow(ctx, buf, a, 0.7f * s * cq, col, 0.8f * out);
            buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, a, 0.85f * s * VfxAnim.easeOutBack(cq), age * 0.22f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.3f), out));
            int n = ctx.seg(8, 5);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + age * 0.35f, r = (1.25f - cq) * 1.0f * s + 0.12f;
                Vector3f p = new Vector3f(a).add(Mth.cos(ang) * r, Mth.sin(ang) * r * 0.8f, 0).add(new Vector3f(ctx.camRight).mul(0)).add(0, 0, 0);
                petal(buf, ctx, p, 0.3f * s, ang + 1.2f, age * 0.5f + i, petalCol(col, inst.seed, i, out * cq));
            }
        }

        // the stream: head position h along a slightly arched path
        float h = VfxAnim.easeInOutSine(Mth.clamp((t - 0.22f) / 0.78f, 0, 1));
        float tail = Math.max(0, h - 0.42f);
        float headFade = 1 - Mth.clamp((t - 0.86f) / 0.14f, 0, 1);
        if (t > 0.2f && h > 0.001f) {
            Vector3f ph = path(a, dir, len, h), pt = path(a, dir, len, tail);
            int cH = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), 0.9f * headFade);
            int cT = VfxVertexBuffer.withAlpha(DEEP, 0.5f * headFade);
            buf.beam(ctx, RIBBON, VfxBlend.ADD, ph, pt, 0.9f * s, 0.2f * s, ctx.seg(6, 3), -age * 0.12f, cH, cT);
            // soft petal clouds along the trail
            for (int i = 0; i < 4; i++) {
                float lt = (i + 0.5f) / 4f;
                Vector3f p = path(a, dir, len, Mth.lerp(lt, h, tail));
                buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p, (0.55f + 0.5f * lt) * s, age * 0.03f * (i % 2 == 0 ? 1 : -1) + i,
                        VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.45f), 0.55f * (1 - lt * 0.8f) * headFade));
            }
            // two counter-wound helices of tumbling petals
            Vector3f d = new Vector3f(dir).normalize(), s1 = ElementFx.side(d), s2 = new Vector3f(d).cross(s1).normalize();
            int n = ctx.seg(14, 8);
            for (int i = 0; i < n; i++) {
                float lt = (i + hash(inst.seed, i, 1)) / n, u = Mth.lerp(lt, h, tail);
                float ph0 = (i % 2 == 0 ? 0 : Mth.PI) + u * 14f - age * 0.55f, r = (0.2f + 0.4f * lt) * s;
                Vector3f p = path(a, dir, len, u).add(new Vector3f(s1).mul(Mth.cos(ph0) * r)).add(new Vector3f(s2).mul(Mth.sin(ph0) * r));
                petal(buf, ctx, p, (0.4f - 0.12f * lt) * s, age * 0.4f + i * 1.7f, age * 0.6f + i, petalCol(col, inst.seed, i, (1 - lt * 0.7f) * headFade));
            }
            // head: flower on a burst, razor blades and afterimages
            buf.billboard(ctx, BURST, VfxBlend.ADD, ph, 1.3f * s, age * 0.3f, VfxVertexBuffer.withAlpha(col, 0.75f * headFade));
            buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, ph, 0.5f * s, -age * 0.4f, VfxVertexBuffer.withAlpha(PALE, headFade));
            VfxBloom.glow(ctx, buf, ph, 0.5f * s, col, headFade);
            for (int g = 1; g <= 2; g++) {
                Vector3f pg = path(a, dir, len, Math.max(0, h - 0.07f * g));
                buf.billboard(ctx, FLOWER, VfxBlend.ADD, pg, 0.42f * s, -age * 0.4f + g, VfxVertexBuffer.withAlpha(col, 0.4f / g * headFade));
            }
            float ang = screenAngle(ctx, d);
            for (int i = 0; i < 4; i++) {
                float lt = 0.12f + 0.2f * i;
                Vector3f p = path(a, dir, len, Mth.lerp(lt, h, tail)).add(new Vector3f(s1).mul((hash(inst.seed, i, 3) - 0.5f) * 0.5f * s));
                pointed(buf, ctx, BLADE, VfxBlend.ADD, p, 0.8f * s, ang, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.85f * (1 - lt) * headFade));
            }
        }

        // impact at 'to'
        if (t > 0.78f) {
            float k = Mth.clamp((t - 0.78f) / 0.22f, 0, 1), e = VfxAnim.easeOutCubic(k);
            VfxBloom.glow(ctx, buf, b, 0.9f * s * (0.4f + e), col, (1 - k) * 0.9f);
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, 1.9f * s * e + 0.2f, age * 0.2f, VfxVertexBuffer.withAlpha(col, 0.8f * (1 - k)));
            buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, b, 0.9f * s * e, -age * 0.3f, VfxVertexBuffer.withAlpha(PALE, (1 - k) * 0.9f));
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * i / 6 + hash(inst.seed, i, 5), r = (0.2f + 0.9f * e) * s;
                Vector3f p = new Vector3f(b).add(Mth.cos(ang) * r, Mth.sin(ang) * r - 0.3f * k * k, 0).add(new Vector3f(ctx.camRight).mul(0));
                petal(buf, ctx, p, 0.26f * s, ang + age, age * 0.5f + i, petalCol(col, inst.seed, i + 20, 1 - k));
            }
        }
    }

    private static Vector3f path(Vector3f a, Vector3f dir, float len, float u) {
        return new Vector3f(a).add(new Vector3f(dir).mul(u)).add(0, Mth.sin(u * Mth.PI) * 0.05f * len, 0);
    }

    // ------------------------------------------------------------------ FX2

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(0.8f, inst.power), lf = life(inst, age, 8, 12), open = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        int col = tint(inst);
        Vector3f g = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.04f, 0));
        float sz = Mth.clamp((float) Math.sqrt(R) * 0.8f, 0.8f, 2.2f);

        VfxBloom.planeGlow(buf, ground, R * open, col, 0.7f * lf);
        // the seal: two layers turning against each other
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(age * 0.012f), R * open, VfxVertexBuffer.withAlpha(col, 0.85f * lf));
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.lift(0.02f).spin(-age * 0.02f), R * 0.82f * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.4f), 0.45f * lf));
        // a pulse running out over the seal
        float pu = (age * 0.025f) % 1f;
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.05f).spin(age * 0.03f), R * (0.2f + 0.8f * pu) * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.5f * (1 - pu) * lf));
        // the pale light column
        stand(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, g, 0.9f * Math.min(R, 3f), 2.2f + R * 0.5f, VfxVertexBuffer.withAlpha(col, 0.28f * lf));

        // the bank of petal cloud along the rim, and a few inside
        int nc = ctx.seg(10, 6);
        for (int i = 0; i < nc; i++) {
            float ang = Mth.TWO_PI * i / nc + hash(inst.seed, i, 1) * 0.5f + age * 0.004f;
            float rr = R * (0.88f + 0.1f * hash(inst.seed, i, 2)) * open;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * rr, 0.25f * sz + 0.1f * Mth.sin(age * 0.07f + i), Mth.sin(ang) * rr);
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p, sz * (1.1f + 0.5f * hash(inst.seed, i, 3)), (hash(inst.seed, i, 4) - 0.5f) * 1.2f + age * 0.005f * (i % 2 == 0 ? 1 : -1),
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.5f), 0.78f * lf));
        }
        for (int i = 0; i < 3; i++) {
            float ang = hash(inst.seed, i, 6) * Mth.TWO_PI + age * 0.01f, rr = R * (0.35f + 0.2f * i) * open;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * rr, 0.15f, Mth.sin(ang) * rr);
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p, sz * 0.8f, age * 0.01f + i, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.4f), 0.55f * lf));
        }

        // a swirl of petals rising in a spiral
        int n = ctx.seg(34, 16);
        float ps = 0.2f + 0.08f * sz;
        for (int i = 0; i < n; i++) {
            float l = (age * 0.014f * (0.6f + hash(inst.seed, i, 7)) + hash(inst.seed, i, 8)) % 1f;
            float rr = R * (0.12f + 0.84f * (float) Math.sqrt(hash(inst.seed, i, 3)));
            float ang = hash(inst.seed, i, 4) * Mth.TWO_PI + age * 0.05f * (1.3f - rr / R) + l * 2.5f;
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * rr * open, 0.12f + l * (1.2f + R * 0.3f), Mth.sin(ang) * rr * open);
            petal(buf, ctx, p, ps * (0.8f + 0.6f * hash(inst.seed, i, 5)), age * 0.2f + i * 2.1f, age * 0.35f + i, petalCol(col, inst.seed, i, Mth.sin(l * Mth.PI) * lf));
        }
        // glints
        for (int i = 0; i < 8; i++) {
            float l = (age * 0.02f + hash(inst.seed, i, 11)) % 1f, ang = hash(inst.seed, i, 12) * Mth.TWO_PI, rr = R * 0.9f * hash(inst.seed, i, 13);
            Vector3f p = new Vector3f(g).add(Mth.cos(ang) * rr, 0.3f + l * 1.6f, Mth.sin(ang) * rr);
            buf.billboard(ctx, MOTES, VfxBlend.ADD, p, 0.4f * sz, age * 0.1f + i, VfxVertexBuffer.withAlpha(col, Mth.sin(l * Mth.PI) * lf));
        }
    }

    // ------------------------------------------------------------------ FX3

    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), t = Mth.clamp(age / dur, 0, 1);
        float s = Math.max(0.5f, inst.power);
        int col = tint(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean dirOn = hint.lengthSquared() > 0.01f;
        if (dirOn) hint.normalize();
        float e = VfxAnim.easeOutCubic(t), e2 = VfxAnim.easeOutCubic(Mth.clamp(t * 1.8f, 0, 1)), fade = 1 - t;

        VfxBloom.glow(ctx, buf, c, 2.4f * s * (0.4f + 0.6f * e2), col, fade * fade * 1.2f);
        buf.plane(SIGIL, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0)).spin(age * 0.04f), 2.4f * s * e, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        // cloud puffs billowing outward
        for (int i = 0; i < 6; i++) {
            float ang = Mth.TWO_PI * i / 6 + hash(inst.seed, i, 1);
            float r = (0.3f + 1.1f * e) * s;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * r, (hash(inst.seed, i, 2) - 0.3f) * 0.6f * s * e, Mth.sin(ang) * r);
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p, (0.7f + 0.5f * hash(inst.seed, i, 3)) * s * (0.5f + 0.5f * e2), ang + age * 0.02f,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, col, 0.5f), 0.6f * fade));
        }
        // the great blossom opening and turning
        buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, c, 2.4f * s * VfxAnim.easeOutBack(Mth.clamp(t * 2f, 0, 1)), age * 0.12f, VfxVertexBuffer.withAlpha(PALE, 0.9f * fade * fade));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, 4.2f * s * e2, -age * 0.08f, VfxVertexBuffer.withAlpha(col, 0.85f * fade));

        // the spray of petals
        int n = ctx.seg(40, 20);
        for (int i = 0; i < n; i++) {
            Vector3f v = new Vector3f(hash(inst.seed, i, 1) * 2 - 1, (hash(inst.seed, i, 2) * 2 - 1) * 0.8f, hash(inst.seed, i, 3) * 2 - 1);
            if (dirOn) v.add(new Vector3f(hint).mul(0.9f));
            if (v.lengthSquared() < 1e-4f) v.set(0, 1, 0);
            v.normalize();
            float sp = s * (1.0f + 2.2f * hash(inst.seed, i, 4)), turn = t * 1.4f * (hash(inst.seed, i, 5) - 0.3f);
            float cs = Mth.cos(turn), sn = Mth.sin(turn);
            Vector3f p = new Vector3f(c).add((v.x * cs - v.z * sn) * sp * e, v.y * sp * e - 0.9f * s * t * t, (v.x * sn + v.z * cs) * sp * e);
            float al = 1 - Mth.clamp((t - 0.55f) / 0.45f, 0, 1);
            petal(buf, ctx, p, (0.22f + 0.16f * hash(inst.seed, i, 6)) * Math.min(s, 2f), age * 0.3f + i * 1.9f, age * 0.5f + i, petalCol(col, inst.seed, i, al));
        }
        // razor petals flying out
        for (int i = 0; i < 8; i++) {
            Vector3f v = new Vector3f(hash(inst.seed, i, 21) * 2 - 1, (hash(inst.seed, i, 22) * 2 - 1) * 0.6f, hash(inst.seed, i, 23) * 2 - 1);
            if (dirOn) v.add(new Vector3f(hint).mul(0.7f));
            if (v.lengthSquared() < 1e-4f) v.set(1, 0, 0);
            v.normalize();
            Vector3f p = new Vector3f(c).add(new Vector3f(v).mul((0.4f + 2.6f * e) * s));
            pointed(buf, ctx, BLADE, VfxBlend.ADD, p, 0.9f * s * (1 - 0.4f * t), screenAngle(ctx, v), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.9f * fade));
        }
        // the afterglow: glints drifting down
        float ag = Mth.clamp((t - 0.25f) / 0.75f, 0, 1);
        for (int i = 0; i < 6; i++) {
            float ang = hash(inst.seed, i, 31) * Mth.TWO_PI, r = (0.4f + 1.6f * hash(inst.seed, i, 32)) * s;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * r, 0.5f * s - 0.9f * ag * s, Mth.sin(ang) * r);
            buf.billboard(ctx, MOTES, VfxBlend.ADD, p, 0.5f * s, age * 0.08f + i, VfxVertexBuffer.withAlpha(col, Mth.sin(ag * Mth.PI) * 0.9f));
        }
    }
}
