package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.DarkSlashStrip.*;

/**
 * 0.56 Yami's Dark Magic slashes (the manga stills in docs/attributes/art_reference/yami_*.webp, the palette of dark_vfx_collection.webp):
 * pitch-black cores, deep violet to magenta light, white-violet rims, starry specks. Textures from tools/gen_dark_slash_textures.py.
 * <ul>
 *   <li>DARK_SLASH_DIMENSION (Dark Cloaked Dimension Slash, Equinox): a spatial fracture. from = where the cut starts (the caster's eye), to = where
 *       the tear stands when it lands; power = size (height 3 + 1.6 * power blocks). A thin white-violet hairline flashes open into a tall vertical
 *       tear that flies out along from -> to in the first third and stays at 'to': a pitch-black core with a white-violet rim, tiny stars inside the
 *       void, a jagged violet / magenta aura that ripples and crawls, warped afterimage rings spreading from it, the cut line it left in the air
 *       and three long diagonal scratches (the manga). The tear is one quad drawn with the nusmp:dimension_slash shader ({@link DarkSlashShaderPass})
 *       whose jitter / ripple / stars are computed per pixel; without the shader the same texture is drawn as a ribbon that ripples in the vertices.</li>
 *   <li>DARK_SLASH_BLADE (Dark Cloaked Black Blade): darkness coats the sword and grows a long black blade. from = the hand, to = the tip the blade
 *       reaches; power = size (blade width). Black smoke wraps the grip first, then the blade grows from the hand to the tip with violet edge light
 *       that pulses, black flames drift up off it, ash and glints shed; it holds, then the light dies and the blade fades.</li>
 *   <li>DARK_SLASH_AVIDYA (Dark Cloaked Avidya Slash, and each wave of the wild slash): a wide dark crescent thrown forward. from = launch point,
 *       to = where it ends; power = size (crescent radius 1.8 * power). A tilted black crescent with a white-violet rim and ragged edges flies from
 *       -> to, an afterimage of it a little behind, a long black streak and a trail of black ash and smoke left in its path, glints at the tips.</li>
 *   <li>DARK_SLASH_WILD (Dark Cloaked Avidya Wild Slash, Yami slashing wildly): a flurry of crossed fast dark cuts round the caster. from = the eye,
 *       to = the direction (the flurry stands 1.6 + 0.6 * power blocks ahead); power = size (cut length 1.7..3 * power). Nine cuts at random angles
 *       strike one after the other, each drawing itself along its line with a flash and fading, speed lines streaking outward round them.</li>
 * </ul>
 * Everything stays under the 400 vertex cap of one activation (about 230 for the tear, 150 blade, 220 crescent, 310 flurry).
 */
public class DarkSlashLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation TEAR = t("dark_slash_tear"), AURA = t("dark_slash_aura"), RING = t("dark_slash_ring"), STARS = t("dark_slash_stars"),
            CUT = t("dark_slash_cut"), CUT_GLOW = t("dark_slash_cut_glow"), CRESCENT = t("dark_slash_crescent"), BLADE = t("dark_slash_blade"),
            BLADE_GLOW = t("dark_slash_blade_glow"), CRACKS = t("dark_slash_cracks"), FLARE = t("darkfx_flare"), SHOCK = t("darkfx_shock"),
            SHARD_A = t("darkfx_shard_a"), SHARD_B = t("darkfx_shard_b"), FLAME_A = t("dark_slash_flame_a"), FLAME_B = t("dark_slash_flame_b"), SMOKE = t("dark_slash_smoke"),
            ASH = t("dark_slash_ash"), SPEED = t("dark_slash_speed"), GLINT = t("dark_slash_glint");
    static final int WHITE = 0xFFFFFFFF, VIOLET = 0xFF9A3CFF, MAGENTA = 0xFFE83CC0, RIM = 0xFFEBDCFF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DARK_SLASH_DIMENSION, VfxShape.DARK_SLASH_BLADE, VfxShape.DARK_SLASH_AVIDYA, VfxShape.DARK_SLASH_WILD); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DARK_SLASH_DIMENSION -> 26;
            case DARK_SLASH_BLADE -> 30;
            case DARK_SLASH_AVIDYA -> 14;
            default -> 24;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFB040FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DARK_SLASH_DIMENSION -> dimension(inst, ctx, buf);
            case DARK_SLASH_BLADE -> blade(inst, ctx, buf);
            case DARK_SLASH_AVIDYA -> avidya(inst, ctx, buf);
            default -> wild(inst, ctx, buf);
        }
    }

    private static int col(int rgb, float a) { return VfxVertexBuffer.withAlpha(0xFF000000 | (rgb & 0xFFFFFF), saturate(a)); }

    /** The book colour nudges the violet a little (the palette stays violet / magenta whatever the caster's book colour is). */
    private static int tint(VfxInstance inst, int base) { return VfxVertexBuffer.lerpColor(base, inst.color | 0xFF000000, 0.18f); }

    // ================================================================ DIMENSION
    private void dimension(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.5f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float travel = Math.max(3f, dur * 0.36f);
        float fly = VfxAnim.easeOutCubic(Mth.clamp(age / travel, 0f, 1f));
        Vector3f head = new Vector3f(a).lerp(b, fly);
        float fade = saturate((dur - age) / (dur * 0.3f));
        float openX = Mth.lerp(VfxAnim.easeOutCubic(saturate(age / (dur * 0.22f))), 0.08f, 1f) * (0.1f + 0.9f * (float) Math.pow(fade, 0.8));
        float openY = VfxAnim.easeOutCubic(saturate(age / (dur * 0.13f))) * (0.75f + 0.25f * fade);
        float sz = (3.0f + 1.6f * P) * (1f + 0.02f * Math.max(0f, a.distance(b) - 14f));            // a far landing (Equinox) is drawn bigger so it still reads
        float H = sz * openY, Wd = sz * 0.6f * openX;
        if (H < 0.05f) return;
        Vector3f c = new Vector3f(head.x, head.y - 1.7f + H * 0.5f, head.z);
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f(up).cross(new Vector3f(c).negate());
        if (right.lengthSquared() < 1e-6f) right.set(ctx.camRight); else right.normalize();
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        float flick = 0.75f + 0.25f * Mth.sin(age * 2.3f);

        // the line it cut through the air, black with a hairline of light
        float scar = fade * saturate(age / 2f);
        if (scar > 0.01f && fly > 0.02f) {
            Vector3f s0 = new Vector3f(a.x, a.y - 0.25f, a.z), s1 = new Vector3f(head.x, head.y - 0.25f, head.z);
            DarkSlashStrip.line(buf, ctx, CUT, VfxBlend.ALPHA, s0, s1, 3, 0.5f * P * (0.3f + 0.7f * fade), col(0xFFFFFF, 0.9f * scar));
            DarkSlashStrip.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, s0, s1, 3, 1.0f * P, col(violet, 0.35f * scar));
        }

        // stars drifting round the void (additive specks, behind the aura)
        for (int k = -1; k <= 1; k++) {
            buf.billboard(ctx, STARS, VfxBlend.ADD, new Vector3f(c).add(0, k * H * 0.3f, 0), Math.max(1.0f, Wd * 1.8f), 0.9f * k + age * 0.004f * (k + 2), col(RIM, 0.5f * fade * saturate(age / 4f)));
        }

        // 0.58: the air cracks like glass round the fracture, a vertical flare as it opens, a column of violet light
        float crk = saturate(age / 3f) * fade * (0.6f + 0.4f * saturate(age / 8f));
        if (crk > 0.02) {
            float ch = H * 1.15f * (0.55f + 0.45f * openY), cw = ch * 1.1f;
            Vector3f q0 = new Vector3f(c).add(new Vector3f(right).mul(-cw)).add(0, -ch, 0), q1 = new Vector3f(c).add(new Vector3f(right).mul(cw)).add(0, -ch, 0),
                    q2 = new Vector3f(c).add(new Vector3f(right).mul(cw)).add(0, ch, 0), q3 = new Vector3f(c).add(new Vector3f(right).mul(-cw)).add(0, ch, 0);
            int cc = col(0xFFFFFF, 0.75f * crk);
            buf.quad(CRACKS, VfxBlend.ALPHA, q0, q1, q2, q3, 0f, 1f, 1f, 0f, cc, cc);
        }
        Vector3f baseP = new Vector3f(c.x, head.y - 1.7f, c.z);
        DarkSlashStrip.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, baseP, new Vector3f(baseP).add(0, H * 1.35f, 0), 2, Wd * 0.9f, col(violet, 0.22f * fade * saturate(age / 3f)));
        float open0 = saturate(1f - age / (dur * 0.22f));
        if (open0 > 0f) buf.billboard(ctx, FLARE, VfxBlend.ADD, c, H * 1.3f, Mth.PI * 0.5f, col(RIM, open0 * open0));

        // the tear: the aura crawls (two jagged copies pulsing against each other), the void is the shader quad or the ripple ribbon
        int n = ctx.seg(7, 4);
        Vector3f[] colm = new Vector3f[n];
        float[] w1 = new float[n], w2 = new float[n], w3 = new float[n];
        int[] c1 = new int[n], c2 = new int[n], c3 = new int[n];
        for (int i = 0; i < n; i++) {
            float s = (float) i / (n - 1) - 0.5f, env = Mth.sin((s + 0.5f) * Mth.PI);
            float wob = Wd * 0.07f * env * (Mth.sin(age * 0.8f + i * 1.9f) + 0.6f * Mth.sin(age * 1.5f + i * 3.1f));
            colm[i] = new Vector3f(c).add(0, s * H, 0).add(new Vector3f(right).mul(wob));
            w1[i] = Wd * 1.9f; w2[i] = Wd * 1.35f; w3[i] = Wd;
            c1[i] = col(magenta, 0.55f * fade * flick); c2[i] = col(violet, 0.8f * fade * (2f - flick)); c3[i] = WHITE;
        }
        DarkSlashStrip.ribbon(buf, ctx, AURA, VfxBlend.ADD, colm, w1, c1, 0f, 1f);
        DarkSlashStrip.ribbon(buf, ctx, AURA, VfxBlend.ADD, colm, w2, c2, 0f, 1f);
        float hw = Wd * 0.5f, hh = H * 0.5f;
        Vector3f p0 = new Vector3f(c).add(new Vector3f(right).mul(-hw)).add(0, -hh, 0), p1 = new Vector3f(c).add(new Vector3f(right).mul(hw)).add(0, -hh, 0),
                p2 = new Vector3f(c).add(new Vector3f(right).mul(hw)).add(0, hh, 0), p3 = new Vector3f(c).add(new Vector3f(right).mul(-hw)).add(0, hh, 0);
        boolean shaded = fade > 0.02f && DarkSlashShaderPass.quad(TEAR, ctx.modelView, p0, p1, p2, p3, tint(inst, WHITE) & 0x00FFFFFF | ((int) (255 * fade) << 24));
        if (!shaded) {
            for (int i = 0; i < n; i++) c3[i] = col(0xFFFFFF, fade);
            DarkSlashStrip.ribbon(buf, ctx, TEAR, VfxBlend.ALPHA, colm, w3, c3, 0f, 1f);
        }

        // warped afterimage rings spreading from the tear
        for (int k = 0; k < 3; k++) {
            float p = age / dur * 1.7f - 0.1f - k * 0.27f;
            if (p < 0f || p > 1f) continue;
            float size = H * (0.35f + 0.85f * VfxAnim.easeOutCubic(p));
            buf.billboard(ctx, RING, VfxBlend.ADD, c, size, k * 1.3f + age * 0.012f * (k % 2 == 0 ? 1 : -1), col(k == 1 ? magenta : violet, (1f - p) * saturate(p * 8f) * 0.7f * fade));
        }

        // pieces of space knocked out of the edge drift away, and the landing sends a ring over the ground
        for (int k = 0; k < 6; k++) {
            float ph = saturate((age - dur * 0.08f) / (dur * 0.8f)), sd = (k % 2 == 0 ? 1f : -1f);
            float y = (hash(inst.seed, 60 + k) - 0.5f) * H * 0.8f + ph * 0.5f * (k - 2.5f) * 0.3f;
            Vector3f pos = new Vector3f(c).add(new Vector3f(right).mul(sd * (Wd * 0.45f + ph * Wd * (0.8f + hash(inst.seed, 70 + k))))).add(0, y, 0);
            buf.billboard(ctx, k % 3 == 0 ? SHARD_A : SHARD_B, VfxBlend.ALPHA, pos, (0.35f + 0.35f * hash(inst.seed, 80 + k)) * P * (0.7f + 0.3f * (1f - ph)), age * 0.2f * sd + k, col(0xFFFFFF, saturate(age / 3f) * fade));
        }
        float lu = saturate((age - travel * 0.8f) / (dur * 0.6f));
        if (lu > 0f && lu < 1f) {
            VfxPose gp = VfxPose.ground(new Vector3f(baseP).add(0, 0.06f, 0));
            buf.plane(SHOCK, VfxBlend.ADD, gp.spin(0.7f), H * (0.25f + 0.85f * VfxAnim.easeOutCubic(lu)), col(violet, (1f - lu) * 0.8f));
            buf.plane(RING, VfxBlend.ADD, gp.lift(0.02f), H * (0.15f + 0.6f * VfxAnim.easeOutCubic(lu)), col(RIM, (1f - lu) * 0.5f));
        }

        // the three long diagonal scratches
        for (int k = 0; k < 3; k++) {
            float st = dur * (0.04f + 0.05f * k), prog = saturate((age - st) / (dur * 0.12f)), life = 1f - saturate((age - st - dur * 0.12f) / (dur * 0.38f));
            if (prog <= 0f || life <= 0f) continue;
            float ang = -1.05f + 0.08f * k + 0.1f * (hash(inst.seed, k) - 0.5f);
            Vector3f d2 = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang)));
            Vector3f perp = new Vector3f(ctx.camRight).mul(-Mth.sin(ang)).add(new Vector3f(ctx.camUp).mul(Mth.cos(ang)));
            float L = H * 1.1f;
            Vector3f s0 = new Vector3f(c).add(new Vector3f(perp).mul((k - 1) * H * 0.2f)).sub(new Vector3f(d2).mul(L * 0.5f));
            Vector3f s1 = new Vector3f(s0).add(new Vector3f(d2).mul(L * VfxAnim.easeOutCubic(prog)));
            DarkSlashStrip.line(buf, ctx, CUT, VfxBlend.ALPHA, s0, s1, 3, 0.2f * P * (0.6f + 0.4f * life), col(0xFFFFFF, life));
            DarkSlashStrip.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, s0, s1, 3, 0.55f * P, col(magenta, 0.55f * life));
        }

        // ash thrown off the edges, glints on the rim
        for (int k = 0; k < 8; k++) {
            float ph = frac(age / (dur * 0.9f) + hash(inst.seed, 10 + k)), life = Mth.sin(ph * Mth.PI);
            float side = (k % 2 == 0 ? 1f : -1f) * (Wd * 0.3f + ph * Wd * (0.9f + hash(inst.seed, 20 + k)));
            float y = (hash(inst.seed, 30 + k) - 0.5f) * H * 0.9f - ph * 0.5f;
            Vector3f pos = new Vector3f(c).add(new Vector3f(right).mul(side)).add(0, y, 0);
            buf.billboard(ctx, ASH, VfxBlend.ALPHA, pos, (0.28f + 0.3f * hash(inst.seed, 40 + k)) * P, age * 0.1f * (k - 3.5f) * 0.3f, col(0xFFFFFF, life * fade * saturate(age / 3f)));
        }
        for (int k = 0; k < 3; k++) {
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.9f + k * 2.1f);
            Vector3f pos = new Vector3f(c).add(new Vector3f(right).mul((hash(inst.seed, 50 + k) - 0.5f) * Wd * 0.5f)).add(0, (k - 1) * H * 0.42f, 0);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, pos, (0.5f + 0.5f * tw) * P * 0.9f, k, col(RIM, 0.9f * tw * fade));
        }
    }

    // ================================================================ BLADE
    private void blade(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.5f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        float grow = VfxAnim.easeOutCubic(saturate((age - dur * 0.1f) / (dur * 0.38f)));
        float fade = saturate((dur - age) / (dur * 0.3f));
        float coat = saturate(age / (dur * 0.12f)) * (1f - saturate((age - dur * 0.5f) / (dur * 0.3f)));
        float pulse = 0.78f + 0.22f * Mth.sin(age * 0.9f);
        Vector3f tip = new Vector3f(a).lerp(b, grow);
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        float W = 0.46f * P + 0.2f;

        // darkness coats the sword: black smoke round the grip and the first stretch of blade
        for (int k = 0; k < 4; k++) {
            float s = (0.1f + 0.28f * k) * Math.min(1f, len);
            Vector3f pos = new Vector3f(a).add(new Vector3f(dir).mul(s)).add(new Vector3f(ctx.camUp).mul(0.1f * Mth.sin(age * 0.4f + k)));
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, pos, (0.95f - 0.12f * k) * P, age * 0.04f * (k % 2 == 0 ? 1 : -1) + k, col(0xFFFFFF, coat * 0.9f * fade));
        }

        // the blade itself: black body, violet edge light (two widths, the wide one breathing)
        int n = ctx.seg(6, 3);
        Vector3f[] pts = new Vector3f[n];
        float[] w = new float[n], wg = new float[n];
        int[] cb = new int[n], cg = new int[n], cg2 = new int[n];
        for (int i = 0; i < n; i++) {
            float s = (float) i / (n - 1);
            pts[i] = new Vector3f(a).lerp(tip, s).add(new Vector3f(ctx.camUp).mul(0.02f * P * Mth.sin(age * 0.7f + i * 2f)));
            w[i] = W * (1.1f - 0.1f * s); wg[i] = W * 2.5f * (1f + 0.12f * Mth.sin(age * 0.8f + i));
            cb[i] = col(0xFFFFFF, fade * saturate(grow * 3f));
            cg[i] = col(violet, 0.85f * fade * pulse * saturate(grow * 3f));
            cg2[i] = col(magenta, 0.45f * fade * (1.8f - pulse) * saturate(grow * 3f));
        }
        if (grow > 0.01f) {
            DarkSlashStrip.ribbon(buf, ctx, BLADE, VfxBlend.ALPHA, pts, w, cb, 0f, 1f);
            DarkSlashStrip.ribbon(buf, ctx, BLADE_GLOW, VfxBlend.ADD, pts, wg, cg, 0f, 1f);
            DarkSlashStrip.ribbon(buf, ctx, BLADE_GLOW, VfxBlend.ADD, pts, w, cg2, 0f, 1f);
        }

        // black flames drifting up off the blade
        Vector3f perp = new Vector3f(dir).cross(new Vector3f(ctx.camUp).add(new Vector3f(ctx.camRight).mul(0.01f))).normalize();
        for (int k = 0; k < 8; k++) {
            float ph = frac(age / (9f + 7f * hash(inst.seed, k)) + hash(inst.seed, 8 + k)), life = Mth.sin(ph * Mth.PI);
            float s = (0.12f + 0.88f * hash(inst.seed, 16 + k)) * grow;
            Vector3f base = new Vector3f(a).lerp(tip, s).add(new Vector3f(perp).mul((hash(inst.seed, 24 + k) - 0.5f) * W * 0.9f)).add(0, ph * 0.5f * P, 0);
            Vector3f top = new Vector3f(base).add(new Vector3f(ctx.camRight).mul((k % 2 == 0 ? 0.12f : -0.12f) * P)).add(0, (0.35f + 0.5f * hash(inst.seed, 32 + k)) * P * (0.4f + life), 0);
            DarkSlashStrip.line(buf, ctx, k % 2 == 0 ? FLAME_A : FLAME_B, VfxBlend.ALPHA, base, top, 2, (0.22f + 0.2f * life) * P, col(0xFFFFFF, life * fade * saturate(grow * 4f)));
        }

        // sparks of light at the tip and the hand, ash falling away
        if (grow > 0.05f) {
            VfxBloom.glow(ctx, buf, tip, 0.45f * P, violet, 0.55f * fade * pulse);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, tip, (0.7f + 0.4f * Mth.sin(age * 1.1f)) * P, age * 0.05f, col(RIM, 0.95f * fade));
        }
        buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(a).add(new Vector3f(dir).mul(0.15f)), 0.5f * P * coat, -age * 0.06f, col(RIM, 0.8f * coat * fade));
        for (int k = 0; k < 6; k++) {
            float ph = frac(age / (dur * 0.8f) + hash(inst.seed, 40 + k)), life = Mth.sin(ph * Mth.PI);
            Vector3f pos = new Vector3f(a).lerp(tip, hash(inst.seed, 48 + k) * grow).add(new Vector3f(perp).mul((hash(inst.seed, 56 + k) - 0.5f) * 0.8f * P)).add(0, -ph * 0.7f * P, 0);
            buf.billboard(ctx, ASH, VfxBlend.ALPHA, pos, 0.25f * P, ph * 5f + k, col(0xFFFFFF, life * 0.9f * fade));
        }
    }

    // ================================================================ AVIDYA
    private void avidya(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.5f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        float u = saturate(age / dur), fade = saturate((1f - u) / 0.2f);
        float grow = VfxAnim.easeOutCubic(saturate(age / (dur * 0.16f)));
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        float R = 2.4f * P * grow;

        // the crescent's frame: lateral axis rolled by a random angle round the flight direction
        Vector3f lat0 = new Vector3f(dir).cross(0, 1, 0);
        if (lat0.lengthSquared() < 1e-6f) lat0.set(ctx.camRight);
        lat0.normalize();
        Vector3f up0 = new Vector3f(lat0).cross(dir).normalize();
        float roll = (hash(inst.seed, 1) - 0.5f) * 1.7f;
        Vector3f lat = new Vector3f(lat0).mul(Mth.cos(roll)).add(new Vector3f(up0).mul(Mth.sin(roll)));
        Vector3f upR = new Vector3f(up0).mul(Mth.cos(roll)).sub(new Vector3f(lat0).mul(Mth.sin(roll)));

        float pos = u;                                                                 // along from -> to (the wave keeps the projectile's pace)
        Vector3f head = new Vector3f(a).lerp(b, pos);
        int n = ctx.seg(9, 5);

        // the long black streak the wave leaves in the air, and the afterimage crescent a little behind it
        float tailLen = Math.min(len * pos, 6f * P);
        if (tailLen > 0.3f) {
            Vector3f t1 = new Vector3f(head).sub(new Vector3f(dir).mul(tailLen));
            DarkSlashStrip.line(buf, ctx, CUT, VfxBlend.ALPHA, t1, head, 3, 0.45f * P * grow, col(0xFFFFFF, 0.65f * fade));
            DarkSlashStrip.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, t1, head, 3, 1.2f * P * grow, col(violet, 0.4f * fade));
        }
        for (int pass = 0; pass < 2; pass++) {
            float back = pass == 0 ? 0.7f * P : 0f;
            float k = pass == 0 ? 0.5f : 1f;
            Vector3f[] pts = new Vector3f[n];
            float[] w = new float[n], wg = new float[n];
            int[] cb = new int[n], cg = new int[n];
            for (int i = 0; i < n; i++) {
                float s = (float) i / (n - 1), th = (s - 0.5f) * 2.3f;
                float prof = (float) Math.pow(Mth.sin(s * Mth.PI), 0.75);
                pts[i] = new Vector3f(head).sub(new Vector3f(dir).mul(back)).add(new Vector3f(lat).mul(R * Mth.sin(th)))
                        .add(new Vector3f(upR).mul(-R * 0.5f * (1f - Mth.cos(th)) + R * 0.2f))
                        .sub(new Vector3f(dir).mul(R * 0.35f * (1f - Mth.cos(th))));        // the tips sweep back
                w[i] = (0.3f + 1.35f * prof) * P * 0.8f * grow;
                wg[i] = w[i] * 1.6f;
                cb[i] = col(0xFFFFFF, k * fade);
                cg[i] = col(i % 2 == 0 ? violet : magenta, 0.5f * k * fade * prof);
            }
            DarkSlashStrip.ribbon(buf, ctx, CRESCENT, VfxBlend.ALPHA, pts, w, cb, 0f, 1f);
            DarkSlashStrip.ribbon(buf, ctx, CUT_GLOW, VfxBlend.ADD, pts, wg, cg, 0f, 1f);
            if (pass == 1) {                                                          // glints on the two tips
                buf.billboard(ctx, GLINT, VfxBlend.ADD, pts[0], 0.8f * P * grow, age * 0.08f, col(RIM, 0.9f * fade));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, pts[n - 1], 0.8f * P * grow, -age * 0.08f, col(RIM, 0.9f * fade));
            }
        }

        // black ash and smoke left behind
        for (int k = 0; k < 9; k++) {
            float s = hash(inst.seed, 10 + k), at = pos * s;                         // dropped at a point of the path
            float lifeAge = (pos - at) / Math.max(0.05f, 0.5f), life = saturate(1f - lifeAge) * saturate((pos - at) * 12f);
            if (life <= 0f) continue;
            float off = (hash(inst.seed, 20 + k) - 0.5f) * 2f * R * (0.5f + 0.5f * s);
            Vector3f p = new Vector3f(a).lerp(b, at).add(new Vector3f(lat).mul(off * 0.9f)).add(new Vector3f(upR).mul(-Math.abs(off) * 0.3f - lifeAge * 0.7f * P));
            buf.billboard(ctx, k % 3 == 0 ? SMOKE : ASH, VfxBlend.ALPHA, p, (k % 3 == 0 ? 0.8f : 0.34f) * P * (0.6f + 0.4f * life), k * 1.7f + lifeAge * 3f, col(0xFFFFFF, life * 0.85f));
        }
    }

    // ================================================================ WILD
    private void wild(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, P = Math.max(0.5f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) dir.set(0, 0, 1); else dir.normalize();
        Vector3f ctr = new Vector3f(a).add(new Vector3f(dir).mul(1.6f + 0.6f * P));
        int violet = tint(inst, VIOLET), magenta = tint(inst, MAGENTA);
        float env = Mth.sin(saturate(age / dur) * Mth.PI) ;
        int cuts = 11;

        // the first flash as the flurry starts
        if (age < dur * 0.18f) VfxBloom.glow(ctx, buf, ctr, 1.1f * P, violet, 0.5f * (1f - age / (dur * 0.18f)));

        // speed lines streaking outward round the flurry
        for (int k = 0; k < 9; k++) {
            float ph = frac(age / (dur * 0.5f) + hash(inst.seed, 100 + k));
            float ang = hash(inst.seed, 110 + k) * Mth.TWO_PI, r0 = (1.3f + 1.6f * hash(inst.seed, 120 + k)) * P + ph * 1.4f * P;
            Vector3f d2 = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang)));
            Vector3f s0 = new Vector3f(ctr).add(new Vector3f(d2).mul(r0)), s1 = new Vector3f(ctr).add(new Vector3f(d2).mul(r0 + (0.9f + 0.7f * hash(inst.seed, 130 + k)) * P));
            DarkSlashStrip.line(buf, ctx, SPEED, VfxBlend.ADD, s0, s1, 2, 0.08f * P, col(k % 2 == 0 ? RIM : violet, 0.8f * env * Mth.sin(ph * Mth.PI)));
        }

        // the cuts: each draws itself along its line, flashes, fades
        float stroke = Math.max(2f, dur * 0.16f);
        for (int k = 0; k < cuts; k++) {
            float t0 = k * dur * 0.055f, prog = saturate((age - t0) / stroke);
            if (age < t0) continue;
            float hold = 1f - saturate((age - t0 - stroke) / (dur * 0.42f));
            if (hold <= 0f) continue;
            float ang = (k % 2 == 0 ? 0.55f : -0.55f) + (hash(inst.seed, k) - 0.5f) * 1.1f + (k % 3) * 0.12f;
            Vector3f d2 = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang)));
            Vector3f centre = new Vector3f(ctr).add(new Vector3f(ctx.camRight).mul((hash(inst.seed, 30 + k) - 0.5f) * 2.4f * P)).add(new Vector3f(ctx.camUp).mul((hash(inst.seed, 40 + k) - 0.5f) * 1.7f * P));
            float L = (2.5f + 1.8f * hash(inst.seed, 50 + k)) * P;
            Vector3f s0 = new Vector3f(centre).sub(new Vector3f(d2).mul(L * 0.5f));
            Vector3f s1 = new Vector3f(s0).add(new Vector3f(d2).mul(L * VfxAnim.easeOutCubic(prog)));
            float flash = 1f - saturate((age - t0) / (stroke * 2.2f));
            DarkSlashStrip.line(buf, ctx, CUT, VfxBlend.ALPHA, s0, s1, 3, (0.38f + 0.2f * flash) * P, col(0xFFFFFF, hold));
            DarkSlashStrip.line(buf, ctx, CUT_GLOW, VfxBlend.ADD, s0, s1, 3, (0.95f + 0.7f * flash) * P, col(k % 2 == 0 ? violet : magenta, (0.2f + 0.45f * flash) * hold));
            if (prog < 1f && k % 3 == 0) buf.billboard(ctx, GLINT, VfxBlend.ADD, s1, 0.8f * P, k, col(RIM, hold));
        }

        // flakes of ash knocked loose
        for (int k = 0; k < 5; k++) {
            float ph = frac(age / (dur * 0.7f) + hash(inst.seed, 140 + k)), life = Mth.sin(ph * Mth.PI);
            Vector3f p = new Vector3f(ctr).add(new Vector3f(ctx.camRight).mul((hash(inst.seed, 150 + k) - 0.5f) * 3.4f * P)).add(new Vector3f(ctx.camUp).mul((hash(inst.seed, 160 + k) - 0.5f) * 2.2f * P - ph * 0.8f));
            buf.billboard(ctx, ASH, VfxBlend.ALPHA, p, 0.3f * P, ph * 6f + k, col(0xFFFFFF, life * env));
        }
    }
}
