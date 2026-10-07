package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.DarkMagicFx.*;

/**
 * 0.58 the rest of Yami's Dark Magic (the palette of docs/attributes/art_reference/dark_vfx_collection.webp: pitch-black cores, deep violet to
 * magenta light, white-violet rims, tiny stars). Textures from tools/gen_dark_magic_textures.py (and the slash sprites of DarkSlashLayer).
 * None of these four shapes is auto-scaled: the spell passes the real size.
 * <ul>
 *   <li>DARK_BLACK_HOLE (Black Hole; also the collapsing rift where a Dimension Slash lands): from = the centre on the ground (the hole hangs
 *       0.35 * power above it), power = RADIUS in blocks (the accretion disk reaches that far, the black sphere is 0.27 of it). Anticipation: a
 *       bright ring collapses into the point and the sphere snaps open with a flash; then a tilted accretion disk (two counter-rotating skins and
 *       the lensed copy of its far side wrapped round the sphere), the thin photon ring, lensing ripples spreading outward, a dark shadow and a
 *       violet halo, tiny stars, and chunks of debris spiralling in with streaks behind them. The last 12 ticks it collapses into a white spark.</li>
 *   <li>DARK_BLACK_MOON (Dark Cloaked Black Moon): from = the centre of the zone on the ground, power = the moon's RADIUS (it hangs 1.5 * power + 4
 *       above, so it is huge and overhead). A flash of eclipse as it rises, a black cratered moon with a bright crescent rim, a violet corona
 *       with long rays, a thin white-violet ring, light strands pouring down to the ground and dark motes falling; on the ground a darkened disc
 *       with a rotating sigil (radius max(4, 2 * power)) that marks the zone. It fades over the last 22 ticks.</li>
 *   <li>DARK_THRUST (Death Thrust): from = the arm, to = where it ends; power = size (the spear is 1.2 + 1.1 * power blocks across at its
 *       belly, so the 0.8 the spell passes is a 2.1 block lance). A gathering ring and flare at the arm, then a black lance with a white-violet
 *       edge and two twisting streaks shoots out along from -> to, a shock cone of rings trails behind its head, a black cut is left in the
 *       air, the head ends in a burst (ring, flash, rays, shards) and the lance draws back into the point and fades.</li>
 *   <li>DARK_IAI (Dark Cloaked Iai Slash): from = where the draw starts, to = where the dash ends (through the target); power = size (cut
 *       half-width). A flash at the sheath (flare, ring), then ONE white-violet cut line snaps through the air along from -> to (a little past
 *       it, bowed a touch) and burns out fast; the black afterimage it leaves splits open and lingers longer, speed lines trail along it,
 *       the end gets a flash with glints and shards.</li>
 * </ul>
 * Each stays under the 400 vertex cap of one activation (about 190, 190, 230 and 200).
 */
public class DarkMagicLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation ORB = t("darkfx_orb"), DISK = t("darkfx_disk"), PHOTON = t("darkfx_photon"), RIPPLE = t("darkfx_ripple"),
            VIGNETTE = t("darkfx_vignette"), MOON = t("darkfx_moon"), CORONA = t("darkfx_corona"), MOONRING = t("darkfx_moonring"),
            SPEAR = t("darkfx_spear"), SPEAR_GLOW = t("darkfx_spear_glow"), SHOCK = t("darkfx_shock"), IAI = t("darkfx_iai"),
            AFTERIMAGE = t("darkfx_afterimage"), SHARD_A = t("darkfx_shard_a"), SHARD_B = t("darkfx_shard_b"), MOTE = t("darkfx_mote"),
            FLARE = t("darkfx_flare"),
            STARS = t("dark_slash_stars"), SPEED = t("dark_slash_speed"), GLINT = t("dark_slash_glint"), CUT_GLOW = t("dark_slash_cut_glow");
    static final int WHITE = 0xFFFFFF, VIOLET = 0x9A3CFF, MAGENTA = 0xE83CC0, RIM = 0xEBDCFF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DARK_BLACK_HOLE, VfxShape.DARK_BLACK_MOON, VfxShape.DARK_THRUST, VfxShape.DARK_IAI); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DARK_BLACK_HOLE -> 60;
            case DARK_BLACK_MOON -> 200;
            case DARK_THRUST -> 20;
            default -> 14;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFB040FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DARK_BLACK_HOLE -> blackHole(inst, ctx, buf);
            case DARK_BLACK_MOON -> blackMoon(inst, ctx, buf);
            case DARK_THRUST -> thrust(inst, ctx, buf);
            default -> iai(inst, ctx, buf);
        }
    }

    /** The book colour nudges the violet a little (the palette stays violet / magenta whatever the caster's book colour is). */
    private static int tint(VfxInstance inst, int base) { return VfxVertexBuffer.lerpColor(0xFF000000 | base, inst.color | 0xFF000000, 0.15f) & 0xFFFFFF; }

    // ================================================================ BLACK HOLE
    /** Where a piece of debris is on its way in: radius shrinks (slowly at first, then fast), the angle runs ahead. */
    private static Vector3f spiral(Vector3f c, com.newuniverse.nusmp.vfx.client.VfxPose pose, float P, float ph, float a0, float lift) {
        float r = P * Mth.lerp((float) Math.pow(Math.max(ph, 0f), 1.7), 1.15f, 0.30f);
        float th = a0 + 5.5f * (float) Math.pow(Math.max(ph, 0f), 1.3);
        return new Vector3f(c).add(new Vector3f(pose.right()).mul(r * Mth.cos(th))).add(new Vector3f(pose.up()).mul(r * Mth.sin(th)))
                .add(new Vector3f(pose.normal()).mul(lift * (1f - ph)));
    }

    private void blackHole(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.6f, inst.power);
        float openLen = Math.min(9f, dur * 0.3f), closeLen = Math.min(12f, dur * 0.4f), lead = Math.min(3f, dur * 0.1f);
        float open = Math.max(0f, VfxAnim.easeOutBack(saturate((age - lead) / openLen)));
        float endU = saturate((age - (dur - closeLen)) / closeLen);
        float sc = open * (1f - VfxAnim.easeInCubic(endU));
        float vis = saturate(age / 3f) * saturate((dur - age) / 3f);
        float pulse = 0.82f + 0.18f * Mth.sin(age * 0.55f);
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        Vector3f c = ctx.rel(inst.from(ctx)).add(0f, 0.35f * P, 0f);
        Vector3f toCam = new Vector3f(c).negate();
        if (toCam.lengthSquared() < 1e-6f) toCam.set(0, 0, 1); else toCam.normalize();
        Vector3f n = new Vector3f(toCam).mul(0.55f).add(0f, 0.83f, 0f).normalize();
        VfxPose pose = VfxPose.facing(c, n);

        // the shadow it casts on the air and the violet halo round it
        buf.billboard(ctx, VIGNETTE, VfxBlend.ALPHA, c, 3.0f * P * (0.4f + 0.6f * sc), 0f, col(WHITE, 0.62f * vis * saturate(open)));
        buf.billboard(ctx, STARS, VfxBlend.ADD, c, 3.4f * P * (0.5f + 0.5f * sc), age * 0.004f, col(RIM, 0.55f * vis * sc));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 3.2f * P * sc, 0f, col(violet, 0.32f * vis * pulse));

        // lensing ripples spreading from it
        for (int k = 0; k < 2; k++) {
            float p = frac(age / 26f + k * 0.5f);
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, c, P * (0.9f + 2.6f * VfxAnim.easeOutCubic(p)) * sc, k * 1.9f + age * 0.01f, col(k == 0 ? violet : magenta, (1f - p) * saturate(p * 6f) * 0.55f * vis));
        }

        // anticipation: a bright ring collapses into the point, then the sphere snaps open with a flash
        if (age < lead + 4f) {
            float aU = saturate(age / (lead + 4f));
            buf.billboard(ctx, PHOTON, VfxBlend.ADD, c, P * (4.6f - 4.0f * aU), age * 0.2f, col(RIM, (0.3f + 0.7f * aU) * saturate((lead + 4f - age) / 1.5f)));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, c, P * 1.6f * aU, 0f, col(WHITE, aU));
        }
        float burst = saturate((age - lead - 2f) / 9f);
        if (burst > 0f && burst < 1f) buf.billboard(ctx, FLARE, VfxBlend.ADD, c, P * 3.6f * (0.4f + 0.6f * burst), 0f, col(RIM, (1f - burst) * (1f - burst)));

        // the accretion disk: two counter-rotating skins, and the lensed copy of its far side wrapped round the sphere
        buf.plane(DISK, VfxBlend.ADD, pose.spin(age * 0.07f), P * sc, col(WHITE, 0.95f * vis));
        buf.plane(DISK, VfxBlend.ADD, pose.lift(0.02f * P).spin(1.1f - age * 0.045f), P * 0.82f * sc, col(magenta, 0.75f * vis * (1.35f - pulse)));
        buf.billboard(ctx, DISK, VfxBlend.ADD, c, 1.3f * P * sc, -age * 0.05f, col(violet, 0.4f * vis));

        // the sphere itself, and the photon ring hugging it
        buf.billboard(ctx, ORB, VfxBlend.ALPHA, c, 0.69f * P * sc * (1f + 0.02f * Mth.sin(age * 0.7f)), 0f, col(WHITE, vis));
        buf.billboard(ctx, PHOTON, VfxBlend.ADD, c, 0.97f * P * sc, age * 0.03f, col(RIM, 0.95f * vis * pulse));
        buf.billboard(ctx, PHOTON, VfxBlend.ADD, c, 1.35f * P * sc, -age * 0.02f + 2f, col(violet, 0.5f * vis));

        // debris spiralling in: chunks, with a streak behind each of the nearer ones
        for (int k = 0; k < 12; k++) {
            float spd = 0.8f + 0.5f * hash(inst.seed, k);
            float ph = frac(age / (30f / spd) + hash(inst.seed, 20 + k)), a0 = hash(inst.seed, 40 + k) * Mth.TWO_PI, lift = (hash(inst.seed, 60 + k) - 0.5f) * 0.5f * P;
            Vector3f pos = spiral(c, pose, P, ph, a0, lift);
            float size = (0.10f + 0.09f * hash(inst.seed, 80 + k)) * P * (1f - 0.6f * ph);
            float life = saturate(ph * 8f) * saturate((1f - ph) * 5f) * vis;
            buf.billboard(ctx, k % 2 == 0 ? SHARD_A : SHARD_B, VfxBlend.ALPHA, pos, size * 2.2f, ph * 9f + k, col(WHITE, life));
            if (k % 2 == 0) {
                Vector3f[] pts = new Vector3f[4];
                float[] w = new float[4];
                int[] cc = new int[4];
                for (int i = 0; i < 4; i++) {
                    pts[i] = spiral(c, pose, P, Math.max(0f, ph - 0.11f * (3 - i) / 3f), a0, lift);
                    w[i] = size * 0.55f * i / 3f + 0.01f * P;
                    cc[i] = col(i % 2 == 0 ? violet : RIM, life * 0.85f * i / 3f);
                }
                DarkMagicFx.ribbon(buf, ctx, SPEED, VfxBlend.ADD, pts, w, cc, 0f, 1f);
            }
        }
        for (int k = 0; k < 3; k++) {
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.8f + k * 2.3f);
            Vector3f pos = new Vector3f(c).add(new Vector3f(ctx.camRight).mul((hash(inst.seed, 100 + k) - 0.5f) * 2.2f * P)).add(new Vector3f(ctx.camUp).mul((hash(inst.seed, 110 + k) - 0.5f) * 1.6f * P));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, pos, (0.3f + 0.25f * tw) * P * sc, k, col(RIM, 0.9f * tw * vis));
        }

        // the collapse: a white spark and a last ring
        if (endU > 0f) {
            buf.billboard(ctx, FLARE, VfxBlend.ADD, c, P * 2.2f * endU, age * 0.1f, col(WHITE, endU * endU * (1f - 0.4f * endU)));
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, c, P * (1.2f + 2.4f * endU), 0f, col(RIM, endU * (1f - endU) * 2f * vis));
        }
    }

    // ================================================================ BLACK MOON
    private void blackMoon(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, R = Math.max(1f, inst.power);
        float rise = VfxAnim.easeOutCubic(saturate(age / 28f)), set = saturate((dur - age) / 22f), vis = rise * set;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.18f);
        float H = 1.5f * R + 4f, ringR = Math.max(4f, 2f * R);
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        Vector3f g = ctx.rel(inst.from(ctx));
        Vector3f m = new Vector3f(g).add(0f, H - (1f - rise) * H * 0.4f, 0f);
        float S = R * (0.55f + 0.45f * rise);

        // the zone on the ground: a darkened disc under a turning sigil
        float gr = VfxAnim.easeOutCubic(saturate(age / 20f)) * saturate((dur - age) / 14f);
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0f, 0.06f, 0f));
        buf.plane(VIGNETTE, VfxBlend.ALPHA, ground, ringR * 1.25f * gr, col(WHITE, 0.7f * gr));
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.01f), ringR * 1.1f * gr, col(violet, 0.22f * gr * pulse));
        buf.plane(MOONRING, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.012f), ringR * gr, col(violet, 0.85f * gr));
        buf.plane(MOONRING, VfxBlend.ADD, ground.lift(0.03f).spin(-age * 0.02f + 0.5f), ringR * 0.62f * gr, col(magenta, 0.55f * gr));

        // darkness all round it, the corona (two skins turning against each other) and the halo
        buf.billboard(ctx, VIGNETTE, VfxBlend.ALPHA, m, 7.5f * S, 0f, col(WHITE, 0.5f * vis));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, m, 5.2f * S, 0f, col(violet, 0.30f * vis * pulse));
        buf.billboard(ctx, CORONA, VfxBlend.ADD, m, 5.0f * S, age * 0.004f, col(WHITE, 0.95f * vis * pulse));
        buf.billboard(ctx, CORONA, VfxBlend.ADD, m, 6.4f * S, -age * 0.0025f + 1.3f, col(magenta, 0.5f * vis));

        // light strands pouring down from under it
        for (int k = 0; k < 7; k++) {
            float ang = hash(inst.seed, k) * Mth.TWO_PI, rad = (0.25f + 0.85f * hash(inst.seed, 10 + k)) * S;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(age * (0.18f + 0.1f * hash(inst.seed, 20 + k)) + k * 2.1f));
            Vector3f top = new Vector3f(m).add(Mth.cos(ang) * rad, -S * 0.55f, Mth.sin(ang) * rad);
            Vector3f bot = new Vector3f(g).add(top.x - m.x, 0.15f, top.z - m.z);
            DarkMagicFx.line(buf, ctx, SPEED, VfxBlend.ADD, top, bot, 2, (0.10f + 0.10f * hash(inst.seed, 30 + k)) * R, col(k % 2 == 0 ? violet : RIM, 0.55f * tw * vis * gr));
        }

        // the moon: a black cratered disc with a bright crescent and a thin ring of white-violet light
        buf.billboard(ctx, MOON, VfxBlend.ALPHA, m, 2.05f * S, 0f, col(WHITE, vis));
        buf.billboard(ctx, PHOTON, VfxBlend.ADD, m, 3.3f * S, age * 0.006f, col(RIM, 0.55f * vis * pulse));
        float flash = saturate(1f - age / 16f);
        if (flash > 0f) buf.billboard(ctx, FLARE, VfxBlend.ADD, m, 7f * S, 0f, col(RIM, flash * flash));

        // dark motes falling, a few glints
        for (int k = 0; k < 14; k++) {
            float ph = frac(age / (46f * (0.7f + 0.6f * hash(inst.seed, 40 + k))) + hash(inst.seed, 50 + k));
            float ang = hash(inst.seed, 60 + k) * Mth.TWO_PI, rad = Mth.lerp(ph, 0.8f * S, ringR * 0.85f) * (0.25f + 0.75f * hash(inst.seed, 70 + k));
            float y = Mth.lerp((float) Math.pow(ph, 1.15), m.y - 0.5f * S, g.y + 0.3f);
            Vector3f pos = new Vector3f(g.x + Mth.cos(ang) * rad, y, g.z + Mth.sin(ang) * rad);
            float life = Mth.sin(ph * Mth.PI) * vis;
            buf.billboard(ctx, MOTE, VfxBlend.ALPHA, pos, (0.5f + 0.7f * hash(inst.seed, 80 + k)) * Math.min(R, 3f) * (0.6f + 0.6f * ph), ph * 5f + k, col(WHITE, life * 0.9f));
            if (k < 6) buf.billboard(ctx, GLINT, VfxBlend.ADD, pos, 0.5f * Math.min(R, 3f) * life, k + ph * 3f, col(RIM, life));
        }
    }

    // ================================================================ DEATH THRUST
    private void thrust(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = 1.2f + 1.1f * inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)).add(0f, -0.3f, 0f), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        float aU = saturate(age / (dur * 0.10f));
        float thr = VfxAnim.easeOutCubic(saturate((age - dur * 0.10f) / (dur * 0.16f)));
        float retract = VfxAnim.easeInCubic(saturate((age - dur * 0.42f) / (dur * 0.40f)));
        float fade = saturate((dur - age) / (dur * 0.22f));
        float pulse = 0.8f + 0.2f * Mth.sin(age * 1.1f);
        Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(len * thr));
        float sLen = Math.min(len * thr, 5f + 2.2f * P) * (1f - retract);
        Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(sLen));
        float W = 0.5f * P;
        Vector3f p1 = DarkMagicFx.perp(dir, ctx), p2 = new Vector3f(dir).cross(p1).normalize();

        // the black cut left in the air
        float trail = saturate((dur - age) / (dur * 0.7f)) * saturate(thr * 4f);
        if (trail > 0.01f && len * thr > 0.5f) {
            DarkMagicFx.line(buf, ctx, AFTERIMAGE, VfxBlend.ALPHA, a, head, 3, 0.55f * P * (0.4f + 0.6f * trail), col(WHITE, 0.85f * trail));
            DarkMagicFx.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, a, head, 3, 1.5f * P, col(violet, 0.28f * trail));
        }

        // anticipation: a ring and a flare gather at the arm
        if (age < dur * 0.2f) {
            float gu = saturate(age / (dur * 0.2f));
            buf.billboard(ctx, PHOTON, VfxBlend.ADD, a, P * (3.4f - 2.6f * aU), age * 0.3f, col(RIM, aU * (1f - gu * gu)));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, a, P * 2.0f * aU, 0f, col(WHITE, aU * (1f - gu * gu)));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, P * 2.4f * aU, 0f, col(violet, 0.5f * aU * (1f - gu)));
        }

        // the lance: black body with a white-violet edge, a wide violet halo breathing round it, a hot one inside
        if (sLen > 0.3f) {
            int n = 5;
            Vector3f[] pts = new Vector3f[n];
            float[] w = new float[n], wg = new float[n], wh = new float[n];
            int[] cb = new int[n], cg = new int[n], ch = new int[n];
            for (int i = 0; i < n; i++) {
                float s = (float) i / (n - 1);
                pts[i] = new Vector3f(tail).lerp(head, s).add(new Vector3f(p1).mul(0.05f * P * Mth.sin(age * 1.3f + i * 2.4f) * (1f - s)));
                w[i] = W; wg[i] = W * 2.3f * (0.9f + 0.15f * Mth.sin(age * 0.9f + i)); wh[i] = W * 1.15f;
                cb[i] = col(WHITE, fade * saturate(thr * 4f));
                cg[i] = col(violet, 0.75f * fade * pulse * saturate(thr * 4f));
                ch[i] = col(magenta, 0.45f * fade * (1.6f - pulse) * saturate(thr * 4f));
            }
            DarkMagicFx.ribbon(buf, ctx, SPEAR_GLOW, VfxBlend.ADD, pts, wg, cg, 0f, 1f);
            DarkMagicFx.ribbon(buf, ctx, SPEAR, VfxBlend.ALPHA, pts, w, cb, 0f, 1f);
            DarkMagicFx.ribbon(buf, ctx, SPEAR_GLOW, VfxBlend.ADD, pts, wh, ch, 0f, 1f);

            // two streaks of darkness twisting round it
            for (int h = 0; h < 2; h++) {
                int m = 6;
                Vector3f[] hp = new Vector3f[m];
                float[] hw = new float[m];
                int[] hc = new int[m];
                for (int i = 0; i < m; i++) {
                    float s = (float) i / (m - 1), phi = s * 9f + age * 1.2f + h * Mth.PI;
                    float rr = W * (0.55f + 0.35f * Mth.sin(s * Mth.PI));
                    hp[i] = new Vector3f(tail).lerp(head, s).add(new Vector3f(p1).mul(rr * Mth.cos(phi))).add(new Vector3f(p2).mul(rr * Mth.sin(phi)));
                    hw[i] = 0.16f * P * Mth.sin(s * Mth.PI) + 0.02f;
                    hc[i] = col(h == 0 ? RIM : magenta, 0.8f * fade * Mth.sin(s * Mth.PI));
                }
                DarkMagicFx.ribbon(buf, ctx, SPEED, VfxBlend.ADD, hp, hw, hc, 0f, 1f);
            }
        }

        // the head: a bright point, and the shock cone of rings trailing behind it
        float headLife = saturate(1f - retract) * saturate(thr * 5f) * fade;
        if (headLife > 0.02f) {
            VfxBloom.glow(ctx, buf, head, 0.55f * P, violet, 0.8f * headLife);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 1.9f * P * pulse, age * 0.15f, col(RIM, 0.9f * headLife));
        }
        float cone = saturate(thr * 3f) * (1f - saturate((age - dur * 0.45f) / (dur * 0.25f)));
        for (int k = 0; k < 4; k++) {
            float ph = frac(age / 5f + k * 0.25f), d = ph * 3.2f * P;
            Vector3f ctr = new Vector3f(head).sub(new Vector3f(dir).mul(d));
            buf.plane(SHOCK, VfxBlend.ADD, VfxPose.facing(ctr, dir).spin(k * 1.7f + age * 0.2f), 0.5f * P + 0.55f * d, col(k % 2 == 0 ? violet : RIM, Mth.sin(ph * Mth.PI) * 0.75f * cone));
        }

        // the burst where it lands: rings, a flash, rays, shards
        float iu = saturate((age - dur * 0.26f) / (dur * 0.5f));
        if (iu > 0f && iu < 1f) {
            float e = VfxAnim.easeOutCubic(iu), inv = 1f - iu;
            buf.plane(SHOCK, VfxBlend.ADD, VfxPose.facing(head, dir).spin(0.4f), P * (0.6f + 3.4f * e), col(RIM, inv * inv * 0.95f));
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(head, dir).spin(1.2f), P * (0.4f + 4.6f * e), col(violet, inv * 0.7f));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 3.4f * P * inv * inv, 0f, col(WHITE, inv));
            for (int k = 0; k < 6; k++) {
                float ang = hash(inst.seed, k) * Mth.TWO_PI, r0 = P * (0.5f + 2.2f * e);
                Vector3f d2 = new Vector3f(p1).mul(Mth.cos(ang)).add(new Vector3f(p2).mul(Mth.sin(ang)));
                Vector3f s0 = new Vector3f(head).add(new Vector3f(d2).mul(r0)), s1 = new Vector3f(s0).add(new Vector3f(d2).mul(P * (0.7f + 0.8f * hash(inst.seed, 10 + k))));
                DarkMagicFx.line(buf, ctx, SPEED, VfxBlend.ADD, s0, s1, 2, 0.12f * P, col(k % 2 == 0 ? RIM : violet, inv));
            }
            for (int k = 0; k < 6; k++) {
                float ang = hash(inst.seed, 20 + k) * Mth.TWO_PI, sp = (1.5f + 2.5f * hash(inst.seed, 30 + k)) * P;
                Vector3f pos = new Vector3f(head).add(new Vector3f(p1).mul(Mth.cos(ang) * sp * e)).add(new Vector3f(p2).mul(Mth.sin(ang) * sp * e)).add(new Vector3f(dir).mul(P * 0.8f * e));
                buf.billboard(ctx, k % 2 == 0 ? SHARD_A : SHARD_B, VfxBlend.ALPHA, pos, 0.5f * P * (1f - 0.4f * iu), iu * 8f + k, col(WHITE, inv * 1.2f));
            }
        }
    }

    // ================================================================ IAI
    private void iai(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.5f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        Vector3f e = new Vector3f(b).add(new Vector3f(dir).mul(Math.min(1.8f, len * 0.3f)));         // the line runs a little past the target
        float prog = VfxAnim.easeOutCubic(saturate((age - 1f) / 2.5f));
        float flash = 1f - saturate((age - 1f) / (dur * 0.30f));
        float light = 1f - saturate((age - 3f) / (dur * 0.42f));
        float dark = 1f - saturate((age - 3f) / (dur * 0.85f));
        float split = VfxAnim.easeOutCubic(saturate((age - 2f) / 6f));
        Vector3f bow = new Vector3f(ctx.camUp).mul(Math.min(0.9f, len * 0.07f));
        Vector3f p1 = DarkMagicFx.perp(dir, ctx);

        // speed lines along the dash
        for (int k = 0; k < 9; k++) {
            float off = (hash(inst.seed, k) - 0.5f) * 3.4f * P, ph = frac(age / (dur * 0.7f) + hash(inst.seed, 10 + k));
            float s0 = hash(inst.seed, 20 + k) * 0.6f, s1 = Math.min(1f, s0 + 0.25f + 0.35f * hash(inst.seed, 30 + k));
            Vector3f o = new Vector3f(p1).mul(off).add(new Vector3f(ctx.camUp).mul((hash(inst.seed, 40 + k) - 0.5f) * 1.6f * P));
            Vector3f l0 = new Vector3f(a).lerp(e, s0).add(o), l1 = new Vector3f(a).lerp(e, s1).add(o);
            DarkMagicFx.line(buf, ctx, SPEED, VfxBlend.ADD, l0, l1, 2, 0.07f * P, col(k % 2 == 0 ? RIM : violet, 0.75f * Mth.sin(Math.min(1f, age / dur * 1.3f) * Mth.PI) * (1f - 0.5f * ph) * saturate(prog * 3f)));
        }

        // the cut: points along the line, bowed a touch; the black afterimage splits open and lingers, the light is white-hot and gone fast
        int n = ctx.seg(6, 4);
        Vector3f[] pts = new Vector3f[n];
        float[] wl = new float[n], wh = new float[n], wd = new float[n];
        int[] cl = new int[n], ch = new int[n], cd = new int[n];
        for (int i = 0; i < n; i++) {
            float s = (float) i / (n - 1);
            pts[i] = new Vector3f(a).lerp(new Vector3f(a).lerp(e, prog), s).add(new Vector3f(bow).mul(Mth.sin(s * prog * Mth.PI)));
            float env = Mth.sin(Math.max(0.05f, s) * Mth.PI) * 0.6f + 0.4f;
            wl[i] = 0.24f * P * (0.6f + 0.9f * flash) * env;
            wh[i] = 1.1f * P * (0.5f + 0.7f * flash) * env;
            wd[i] = 0.5f * P * (0.4f + 0.9f * split) * env;
            cl[i] = col(WHITE, light * saturate(prog * 3f));
            ch[i] = col(violet, 0.6f * light * (0.4f + 0.6f * flash));
            cd[i] = col(WHITE, 0.95f * dark * saturate(prog * 3f));
        }
        DarkMagicFx.ribbon(buf, ctx, AFTERIMAGE, VfxBlend.ALPHA, pts, wd, cd, 0f, 1f);
        DarkMagicFx.ribbon(buf, ctx, CUT_GLOW, VfxBlend.ADD, pts, wh, ch, 0f, 1f);
        DarkMagicFx.ribbon(buf, ctx, IAI, VfxBlend.ADD, pts, wl, cl, 0f, 1f);
        DarkMagicFx.ribbon(buf, ctx, IAI, VfxBlend.ADD, pts, widen(wl, 2.4f), tintAll(cl, magenta, 0.5f), 0f, 1f);

        // the sheath: a flash, a flare, a ring
        float su = saturate(age / (dur * 0.5f));
        if (su < 1f) {
            buf.billboard(ctx, FLARE, VfxBlend.ADD, a, 3.2f * P * (1f - 0.5f * su), 0f, col(RIM, (1f - su) * (1f - su)));
            buf.billboard(ctx, PHOTON, VfxBlend.ADD, a, P * (0.8f + 3.0f * VfxAnim.easeOutCubic(su)), age * 0.15f, col(violet, (1f - su) * 0.9f));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, 2.6f * P, 0f, col(violet, 0.45f * (1f - su)));
        }

        // where it ends: a flash, glints, shards
        float eu = saturate((age - 2f) / (dur * 0.7f));
        if (eu > 0f && eu < 1f) {
            float inv = 1f - eu;
            buf.billboard(ctx, FLARE, VfxBlend.ADD, b, 3.6f * P * (0.5f + 0.5f * VfxAnim.easeOutCubic(eu)), 0.4f, col(WHITE, inv * inv));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, b, 1.6f * P * inv, age * 0.1f, col(RIM, inv));
            for (int k = 0; k < 6; k++) {
                float ang = hash(inst.seed, 50 + k) * Mth.TWO_PI, sp = (1.0f + 1.8f * hash(inst.seed, 60 + k)) * P * VfxAnim.easeOutCubic(eu);
                Vector3f pos = new Vector3f(b).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * sp)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * sp));
                buf.billboard(ctx, k % 2 == 0 ? SHARD_A : SHARD_B, VfxBlend.ALPHA, pos, 0.4f * P * (1f - 0.5f * eu), eu * 9f + k, col(WHITE, inv * 1.2f));
            }
        }
    }

    private static float[] widen(float[] w, float f) {
        float[] o = new float[w.length];
        for (int i = 0; i < w.length; i++) o[i] = w[i] * f;
        return o;
    }

    private static int[] tintAll(int[] c, int rgb, float k) {
        int[] o = new int[c.length];
        for (int i = 0; i < c.length; i++) o[i] = VfxVertexBuffer.withAlpha((c[i] & 0xFF000000) | (rgb & 0xFFFFFF), k);
        return o;
    }
}
