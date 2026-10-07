package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.45 Dice Magic: real 3D dice built from quads, at the Time Magic standard. Translucent resin bodies (faces drawn at partial
 * alpha so the far faces and the core show through), engraved glyphs and numbers glowing gold, a swirling galaxy core in the
 * d20. They tumble out of the caster's hand on a bouncing arc, slow, and settle with the rolled face turned up toward the
 * viewer; then the result flares (gold on a critical, red cracks of light on a fumble).
 * <ul>
 *   <li>DICE_D6: two d6, faces 1 fire, 2 earth, 3 water, 4 wind, 5 lightning, 6 light, each tinted in its element.
 *       seed & 7 = die A, (seed >> 3) & 7 = die B.</li>
 *   <li>DICE_D20: an icosahedron, opposite faces summing to 21 as on a real d20. seed & 31 = the face rolled.</li>
 * </ul>
 */
public class DiceLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation RESIN = t("dice_resin"), GLYPHS = t("dice_d6_glyphs"), NUMBERS = t("dice_d20_numbers"),
            TRI = t("dice_tri"), GALAXY = t("dice_galaxy"), STAR = t("light_star");
    static final int GOLD = 0xFFFFD86A;
    static final int[] D6_COLOR = {0, 0xFFFF5A2A, 0xFF5AC060, 0xFF3A8CFF, 0xFFE8F4FF, 0xFFFFE65A, 0xFFFFF0B0};

    // ---------------------------------------------------------------- geometry
    /** d6 face normals for faces 1..6 (opposite faces sum to 7). */
    static final Vector3f[] D6_N = {null, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1),
            new Vector3f(0, 0, -1), new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0)};

    /** Icosahedron: 12 vertices, 20 outward-wound faces, numbers with opposite faces summing to 21. */
    static final Vector3f[] ICO_V;
    static final int[][] ICO_F;
    static final int[] ICO_NUM;
    static {
        float p = (1 + (float) Math.sqrt(5)) / 2;
        float[][] raw = {{-1, p, 0}, {1, p, 0}, {-1, -p, 0}, {1, -p, 0}, {0, -1, p}, {0, 1, p}, {0, -1, -p}, {0, 1, -p},
                {p, 0, -1}, {p, 0, 1}, {-p, 0, -1}, {-p, 0, 1}};
        ICO_V = new Vector3f[12];
        for (int i = 0; i < 12; i++) ICO_V[i] = new Vector3f(raw[i][0], raw[i][1], raw[i][2]).normalize();
        List<int[]> faces = new ArrayList<>();
        float edge = ICO_V[0].distance(ICO_V[1]);
        for (int i = 0; i < 12; i++) for (int j = i + 1; j < 12; j++) for (int k = j + 1; k < 12; k++) {
            if (Math.abs(ICO_V[i].distance(ICO_V[j]) - edge) > 1e-3 || Math.abs(ICO_V[j].distance(ICO_V[k]) - edge) > 1e-3
                    || Math.abs(ICO_V[i].distance(ICO_V[k]) - edge) > 1e-3) continue;
            Vector3f n = new Vector3f(ICO_V[j]).sub(ICO_V[i]).cross(new Vector3f(ICO_V[k]).sub(ICO_V[i]));
            Vector3f c = new Vector3f(ICO_V[i]).add(ICO_V[j]).add(ICO_V[k]);
            faces.add(n.dot(c) > 0 ? new int[]{i, j, k} : new int[]{i, k, j});
        }
        ICO_F = faces.toArray(new int[0][]);
        ICO_NUM = new int[ICO_F.length];
        int next = 1;
        for (int f = 0; f < ICO_F.length; f++) {
            if (ICO_NUM[f] != 0) continue;
            ICO_NUM[f] = next;
            Vector3f c = centroid(f);
            for (int g = 0; g < ICO_F.length; g++) if (centroid(g).add(c).lengthSquared() < 1e-4f) ICO_NUM[g] = 21 - next;
            next++;
        }
    }

    static Vector3f centroid(int f) { return new Vector3f(ICO_V[ICO_F[f][0]]).add(ICO_V[ICO_F[f][1]]).add(ICO_V[ICO_F[f][2]]).div(3); }

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DICE_D6, VfxShape.DICE_D20); }

    @Override
    public int defaultDuration(VfxShape s) { return 36; }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFB070FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.DICE_D6) d6pair(inst, ctx, buf);
        else d20(inst, ctx, buf);
    }

    // ---------------------------------------------------------------- motion
    /** Position on the throw: an arc out of the hand, two shrinking bounces, then rest at 'to'. */
    static Vector3f throwPos(Vector3f a, Vector3f b, float t, float size) {
        float land = Mth.clamp(t / 0.6f, 0, 1);
        Vector3f p = new Vector3f(a).lerp(b, VfxAnim.easeOutCubic(land));
        float h;
        if (land < 0.55f) h = 4 * (land / 0.55f) * (1 - land / 0.55f) * 1.2f;
        else if (land < 0.85f) { float u = (land - 0.55f) / 0.3f; h = 4 * u * (1 - u) * 0.35f; }
        else { float u = (land - 0.85f) / 0.15f; h = 4 * u * (1 - u) * 0.1f; }
        return p.add(0, h * size, 0);
    }

    /** Orientation: tumbling fast, slowing, and slerping onto the pose that shows 'faceN' to 'want'. */
    static Quaternionf orient(long seed, float t, float age, Vector3f faceN, Vector3f want) {
        Vector3f axis = new Vector3f(hash(seed, 1, 1) - 0.5f, hash(seed, 1, 2) - 0.5f + 0.8f, hash(seed, 1, 3) - 0.5f).normalize();
        float spin = age * 0.9f * (1 - Mth.clamp(t / 0.6f, 0, 1));
        Quaternionf tumble = new Quaternionf().rotateAxis(spin + seed % 7, axis).rotateX(spin * 0.7f);
        Quaternionf rest = new Quaternionf().rotationTo(faceN, want);
        float settle = VfxAnim.easeInOutSine(Mth.clamp((t - 0.35f) / 0.3f, 0, 1));
        return tumble.slerp(rest, settle);
    }

    /** Where a landed die shows its result: up, leaning toward the camera so it can be read. */
    static Vector3f viewUp(Vector3f at) {
        Vector3f toCam = new Vector3f(at).negate().normalize();
        return new Vector3f(0, 1, 0).add(toCam).normalize();
    }

    // ---------------------------------------------------------------- d6
    private void d6pair(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f side = side(new Vector3f(b).sub(a).normalize());
        int va = Mth.clamp((int) (inst.seed & 7), 1, 6), vb = Mth.clamp((int) ((inst.seed >> 3) & 7), 1, 6);
        float fade = life(inst, age, 0, 8);
        for (int k = 0; k < 2; k++) {
            int face = k == 0 ? va : vb;
            Vector3f end = new Vector3f(b).add(new Vector3f(side).mul(k == 0 ? -0.45f : 0.45f));
            Vector3f c = throwPos(new Vector3f(a).add(new Vector3f(side).mul(k == 0 ? -0.15f : 0.15f)), end, t + k * 0.02f, 1f);
            Quaternionf q = orient(inst.seed + k * 977L, t, age + k * 3, D6_N[face], viewUp(end));
            cube(ctx, buf, c, 0.22f, q, fade, face);
            if (t > 0.6f) {                                                  // the result flares in its element
                float f = Mth.clamp((t - 0.6f) / 0.15f, 0, 1) * (1 - Mth.clamp((t - 0.8f) / 0.2f, 0, 1));
                VfxBloom.glow(ctx, buf, c, 0.9f, D6_COLOR[face], f * fade);
            }
        }
    }

    /** A d6 of half-size h at c, rotated by q; each face resin-tinted in its element, its glyph and number glowing gold. */
    static void cube(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float h, Quaternionf q, float a, int shown) {
        for (int f = 1; f <= 6; f++) {
            Vector3f n = q.transform(new Vector3f(D6_N[f]));
            Vector3f u = q.transform(new Vector3f(Math.abs(D6_N[f].y) > 0.5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)));
            Vector3f r = new Vector3f(u).cross(n).normalize();
            Vector3f fc = new Vector3f(c).add(new Vector3f(n).mul(h));
            Vector3f rr = new Vector3f(r).mul(h), uu = new Vector3f(u).mul(h);
            int body = VfxVertexBuffer.withAlpha(D6_COLOR[f], 0.62f * a);
            buf.quad(RESIN, VfxBlend.ALPHA, new Vector3f(fc).sub(rr).sub(uu), new Vector3f(fc).add(rr).sub(uu), new Vector3f(fc).add(rr).add(uu),
                    new Vector3f(fc).sub(rr).add(uu), 0, 0, 1, 1, body, body);
            float u0 = ((f - 1) % 3) / 3f, v0 = ((f - 1) / 3) / 2f;
            Vector3f lift = new Vector3f(n).mul(0.004f);
            int glow = VfxVertexBuffer.withAlpha(GOLD, (f == shown ? 1f : 0.6f) * a);
            buf.quad(GLYPHS, VfxBlend.ADD, new Vector3f(fc).add(lift).sub(rr).sub(uu), new Vector3f(fc).add(lift).add(rr).sub(uu),
                    new Vector3f(fc).add(lift).add(rr).add(uu), new Vector3f(fc).add(lift).sub(rr).add(uu), u0, v0, u0 + 1 / 3f, v0 + 0.5f, glow, glow);
        }
    }

    // ---------------------------------------------------------------- d20
    private void d20(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        int rolled = Mth.clamp((int) (inst.seed & 31), 1, 20);
        int faceIdx = 0;
        for (int f = 0; f < ICO_NUM.length; f++) if (ICO_NUM[f] == rolled) faceIdx = f;
        float fade = life(inst, age, 0, 8), R = 0.45f * s;
        Vector3f c = throwPos(a, b, t, s);
        Quaternionf q = orient(inst.seed, t, age, centroid(faceIdx).normalize(), viewUp(b));
        // the galaxy core, swirling, seen through the resin
        buf.billboard(ctx, GALAXY, VfxBlend.ADD, c, R * 1.3f, age * 0.12f, VfxVertexBuffer.withAlpha(inst.color, 0.9f * fade));
        buf.billboard(ctx, GALAXY, VfxBlend.ADD, c, R * 0.8f, -age * 0.2f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.4f * fade));
        for (int f = 0; f < ICO_F.length; f++) {
            Vector3f p0 = q.transform(new Vector3f(ICO_V[ICO_F[f][0]])).mul(R).add(c);
            Vector3f p1 = q.transform(new Vector3f(ICO_V[ICO_F[f][1]])).mul(R).add(c);
            Vector3f p2 = q.transform(new Vector3f(ICO_V[ICO_F[f][2]])).mul(R).add(c);
            int body = VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(inst.color, 0xFF1A1030, 0.35f), 0.5f * fade);
            buf.quad(TRI, VfxBlend.ALPHA, p0, p1, p2, p2, 0, 0, 1, 1, body, body);
            // the engraved number: a small square on the face, its 'up' toward the face's first vertex
            Vector3f fc = new Vector3f(p0).add(p1).add(p2).div(3);
            Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0)).normalize();
            Vector3f up = new Vector3f(p0).sub(fc).normalize(), right = new Vector3f(up).cross(n).normalize();
            float hs = R * 0.28f;
            Vector3f uu = new Vector3f(up).mul(hs), rr = new Vector3f(right).mul(hs), lift = new Vector3f(n).mul(0.003f);
            int num = ICO_NUM[f] - 1;
            float u0 = (num % 5) / 5f, v0 = (num / 5) / 4f;
            int glow = VfxVertexBuffer.withAlpha(GOLD, (f == faceIdx ? 1f : 0.55f) * fade);
            buf.quad(NUMBERS, VfxBlend.ADD, new Vector3f(fc).add(lift).sub(rr).sub(uu), new Vector3f(fc).add(lift).add(rr).sub(uu),
                    new Vector3f(fc).add(lift).add(rr).add(uu), new Vector3f(fc).add(lift).sub(rr).add(uu), u0, v0, u0 + 0.2f, v0 + 0.25f, glow, glow);
        }
        // the result: a gold burst on 20, red cracks of light on 1, a soft glow otherwise
        if (t > 0.62f) {
            float f = Mth.clamp((t - 0.62f) / 0.12f, 0, 1) * (1 - Mth.clamp((t - 0.82f) / 0.18f, 0, 1));
            if (rolled == 20) {
                buf.billboard(ctx, STAR, VfxBlend.ADD, c, 2.4f * s * f, age * 0.03f, VfxVertexBuffer.withAlpha(0xFFFFE080, f * fade));
                VfxBloom.glow(ctx, buf, c, 2f * s, 0xFFFFC040, f * fade);
            } else if (rolled == 1) {
                for (int k = 0; k < 5; k++) {
                    float ang = k * 1.3f + hash(inst.seed, k, 9);
                    Vector3f tip = new Vector3f(c).add(Mth.cos(ang) * 0.9f * s * f, (hash(inst.seed, k, 8) - 0.3f) * 0.6f * s, Mth.sin(ang) * 0.9f * s * f);
                    DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, c, tip, 0, 1, 0.08f * s, VfxVertexBuffer.withAlpha(0xFFFF2020, f * fade));
                }
                VfxBloom.glow(ctx, buf, c, 1.2f * s, 0xFFFF2020, 0.7f * f * fade);
            } else VfxBloom.glow(ctx, buf, c, 1.2f * s, inst.color, 0.6f * f * fade);
        }
    }
}
