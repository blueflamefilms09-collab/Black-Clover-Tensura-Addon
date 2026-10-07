package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.45: Light Magic and World Tree Magic, at the Time Magic standard. Textures from tools/gen_attr_vfx_textures.py.
 * <ul>
 *   <li>LIGHT_BLADE: a geometric light sword crossing 'from' -> 'to' in a few ticks (light speed): a blinding white-gold core,
 *       a long streak behind it, a lens flare riding the head with real lens ghosts (mirrored through the screen centre), and a
 *       flash where it lands.</li>
 *   <li>LIGHT_FLARE: a lens-flare burst: six-ray star, hexagonal ghosts across the screen, a bloom core and sparks.</li>
 *   <li>TREE_ROOTS: ancient roots arch up out of the ground round 'from' and plunge back in, bark-textured and tapering, with
 *       emerald leaves budding along them and mana motes rising.</li>
 *   <li>TREE_CANOPY: Magic Tree Descent. A ring opens in the sky, the tree comes down through it and roots: a twin-strip trunk,
 *       an emerald canopy of clumps breathing in the wind, mana spiralling up the trunk.</li>
 *   <li>TREE_DRAIN: Energy Drain. Emerald mana flows in a sinuous stream from 'from' to 'to', motes riding it.</li>
 * </ul>
 */
public class LightTreeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation BLADE = t("light_blade"), STREAK = t("light_streak"), STAR = t("light_star"), GHOST = t("light_ghost"),
            ROOT = t("tree_root"), LEAF = t("tree_leaf"), CANOPY = t("tree_canopy");
    static final int GOLD = 0xFFFFE8A0, EMERALD = 0xFF3CE08A, BARK = 0xFF8A6238, LEAFY = 0xFF2EB86A;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.LIGHT_BLADE, VfxShape.LIGHT_FLARE, VfxShape.TREE_ROOTS, VfxShape.TREE_CANOPY, VfxShape.TREE_DRAIN);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case LIGHT_BLADE -> 10;
            case LIGHT_FLARE -> 12;
            case TREE_ROOTS -> 60;
            case TREE_CANOPY -> 200;
            default -> 16;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case LIGHT_BLADE, LIGHT_FLARE -> GOLD;
            default -> EMERALD;
        };
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case LIGHT_BLADE -> blade(inst, ctx, buf);
            case LIGHT_FLARE -> flare(inst, ctx, buf);
            case TREE_ROOTS -> roots(inst, ctx, buf);
            case TREE_CANOPY -> canopy(inst, ctx, buf);
            case TREE_DRAIN -> drain(inst, ctx, buf);
            default -> { }
        }
    }

    // ================================================================ Light
    /** Lens ghosts: points mirrored through the view axis from 'src', so they line up across the screen as a real flare does. */
    static void ghosts(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f src, float size, int col, float a) {
        Vector3f fwd = new Vector3f(ctx.camRight).cross(ctx.camUp).normalize();
        if (fwd.dot(src) < 0) fwd.negate();
        float depth = fwd.dot(src);
        if (depth < 0.5f) return;
        Vector3f lateral = new Vector3f(src).sub(new Vector3f(fwd).mul(depth));
        float[] ks = {0.45f, -0.35f, -0.8f};
        float[] ss = {0.35f, 0.6f, 0.9f};
        for (int i = 0; i < ks.length; i++) {
            Vector3f q = new Vector3f(fwd).mul(depth).add(new Vector3f(lateral).mul(ks[i]));
            buf.billboard(ctx, GHOST, VfxBlend.ADD, q, size * ss[i] * depth * 0.08f, 0.3f * i, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(col, 0xFF9AD8FF, i / 2f), a * 0.35f));
        }
    }

    private void blade(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        dir.div(len);
        float travel = VfxAnim.easeOutCubic(Mth.clamp(t / 0.35f, 0, 1)), fade = life(inst, age, 0, 5);
        Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(len * travel));
        // the streak behind: long and thin, brightest just behind the head
        float tail = Mth.clamp((t - 0.2f) / 0.6f, 0, 1);
        Vector3f tailAt = new Vector3f(a).lerp(head, tail);
        DreamPaintLayer.strokePart(buf, ctx, STREAK, VfxBlend.ADD, tailAt, head, 0, 1, 0.5f * s, VfxVertexBuffer.withAlpha(GOLD, 0.8f * fade));
        DreamPaintLayer.strokePart(buf, ctx, STREAK, VfxBlend.ADD, tailAt, head, 0, 1, 0.16f * s, VfxVertexBuffer.withAlpha(WHITE, fade));
        // the sword itself: a geometric blade 1.8 long at the head while it flies
        if (travel < 1) {
            Vector3f back = new Vector3f(head).sub(new Vector3f(dir).mul(1.8f * s));
            DreamPaintLayer.strokePart(buf, ctx, BLADE, VfxBlend.ADD, back, head, 0, 1, 0.5f * s, VfxVertexBuffer.withAlpha(GOLD, fade));
            DreamPaintLayer.strokePart(buf, ctx, BLADE, VfxBlend.ADD, back, head, 0, 1, 0.3f * s, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        // the lens flare riding the head, then blazing where it lands
        float land = travel >= 1 ? 1 - Mth.clamp((t - 0.35f) / 0.65f, 0, 1) : 0.6f;
        buf.billboard(ctx, STAR, VfxBlend.ADD, head, (1.4f + 1.6f * land) * s, age * 0.05f, VfxVertexBuffer.withAlpha(WHITE, (0.5f + 0.5f * land) * fade));
        VfxBloom.glow(ctx, buf, head, (1.2f + land) * s, GOLD, 0.8f * fade);
        ghosts(ctx, buf, head, s, GOLD, land * fade);
    }

    private void flare(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx));
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1)), fade = 1 - VfxAnim.easeInCubic(Mth.clamp(t, 0, 1));
        buf.billboard(ctx, STAR, VfxBlend.ADD, c, 3.2f * s * pop, 0.2f + t * 0.4f, VfxVertexBuffer.withAlpha(WHITE, fade));
        buf.billboard(ctx, STAR, VfxBlend.ADD, c, 2.2f * s * pop, 0.6f - t * 0.3f, VfxVertexBuffer.withAlpha(inst.color, 0.7f * fade));
        VfxBloom.glow(ctx, buf, c, 2.2f * s * pop, inst.color, fade);
        ghosts(ctx, buf, c, 1.4f * s, inst.color, fade);
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 1), r = s * 1.6f * VfxAnim.easeOutCubic(t);
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * r, (hash(inst.seed, i, 2) - 0.5f) * r, Mth.sin(a) * r);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.15f * s, a, VfxVertexBuffer.withAlpha(GOLD, fade));
        }
    }

    // ================================================================ World Tree
    /** One root: an arch out of the ground from 'p0' to 'p1', 'segs' bark strips, tapering, grown to 'grow'. */
    static void root(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p0, Vector3f p1, float height, float thick, float grow, int segs, int col, float a) {
        Vector3f prev = new Vector3f(p0);
        for (int k = 1; k <= segs; k++) {
            float f = (float) k / segs;
            if (f - 1f / segs > grow) break;
            float ff = Math.min(f, grow);
            Vector3f cur = new Vector3f(p0).lerp(p1, ff).add(0, height * 4 * ff * (1 - ff), 0);
            float w = thick * (1 - 0.7f * ff);
            DreamPaintLayer.strokePart(buf, ctx, ROOT, VfxBlend.ALPHA, prev, cur, f - 1f / segs, ff, w, VfxVertexBuffer.withAlpha(col, a));
            prev = cur;
        }
    }

    private void roots(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx));
        int n = Math.max(3, (int) (inst.seed & 15));
        float grow = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1)), fade = life(inst, age, 0, 10);
        int segs = ctx.seg(4, 3);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + hash(inst.seed, i, 1) * 0.6f;
            float r0 = R * (0.15f + 0.2f * hash(inst.seed, i, 2)), r1 = R * (0.75f + 0.25f * hash(inst.seed, i, 3));
            Vector3f p0 = new Vector3f(g).add(Mth.cos(ang) * r0, -0.2f, Mth.sin(ang) * r0);
            Vector3f p1 = new Vector3f(g).add(Mth.cos(ang + 0.3f) * r1, -0.2f, Mth.sin(ang + 0.3f) * r1);
            float h = 0.4f + R * (0.25f + 0.2f * hash(inst.seed, i, 4)), thick = 0.25f + 0.12f * R;
            root(ctx, buf, p0, p1, h, thick, grow, segs, BARK, fade);
            // emerald leaves budding along it, glowing
            for (int k = 1; k <= 1; k++) {
                float f = 0.5f;
                if (f > grow) continue;
                Vector3f q = new Vector3f(p0).lerp(p1, f).add(0, h * 4 * f * (1 - f) + 0.15f, 0);
                float bud = Mth.clamp((age - 6 - k * 2) / 6f, 0, 1);
                buf.billboard(ctx, LEAF, VfxBlend.ALPHA, q, 0.35f * bud, ang + k, VfxVertexBuffer.withAlpha(LEAFY, fade));
                buf.billboard(ctx, LEAF, VfxBlend.ADD, q, 0.4f * bud, ang + k, VfxVertexBuffer.withAlpha(EMERALD, 0.4f * fade));
            }
        }
        // the ground cracking under them and mana motes rising
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(g).add(0, 0.03f, 0)), R * 0.8f * grow, VfxVertexBuffer.withAlpha(EMERALD, 0.25f * fade));
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float lt = ((age * 0.025f) + hash(inst.seed, i, 5)) % 1f, a = hash(inst.seed, i, 6) * Mth.TWO_PI, rr = R * hash(inst.seed, i, 7);
            Vector3f q = new Vector3f(g).add(Mth.cos(a) * rr, lt * 2.5f, Mth.sin(a) * rr);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.12f, a, VfxVertexBuffer.withAlpha(EMERALD, fade * Mth.sin(lt * Mth.PI)));
        }
    }

    private void canopy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), H = inst.power;
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 0, 20);
        float descend = VfxAnim.easeOutCubic(Mth.clamp((age - 6) / 18f, 0, 1));
        float drop = (1 - descend) * H * 1.5f;
        // the hole in the sky it comes through
        float hole = Mth.clamp(1 - (age - 20) / 12f, 0, 1) * Mth.clamp(age / 6f, 0, 1);
        if (hole > 0) {
            VfxPose sky = VfxPose.ground(new Vector3f(g).add(0, H * 2.4f, 0)).spin(age * 0.05f);
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, sky, H * 0.3f, H * 0.42f, ctx.seg(16, 8), 1, 0, VfxVertexBuffer.withAlpha(EMERALD, hole));
            buf.plane(VfxTextures.GLOW, VfxBlend.ALPHA, sky, H * 0.3f, VfxVertexBuffer.withAlpha(0xFF101820, 0.8f * hole));
        }
        if (age < 6) return;
        Vector3f base = new Vector3f(g).add(0, drop, 0), top = new Vector3f(base).add(0, H * 0.75f, 0);
        // the trunk: two crossed bark strips so it reads from every side
        float thick = 0.5f + 0.12f * H;
        DreamPaintLayer.strokePart(buf, ctx, ROOT, VfxBlend.ALPHA, base, top, 0, 1, thick, VfxVertexBuffer.withAlpha(BARK, fade));
        DreamPaintLayer.strokePart(buf, ctx, ROOT, VfxBlend.ALPHA, new Vector3f(base).add(0.05f, 0, 0.05f), new Vector3f(top).add(-0.05f, 0, 0.05f), 0, 1, thick * 0.8f,
                VfxVertexBuffer.withAlpha(ElementFx.mulRgb(BARK, 0.85f), fade));
        // the canopy: clumps round the crown, breathing in the wind
        int n = ctx.seg(9, 5);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 1), rr = H * (0.18f + 0.22f * hash(inst.seed, i, 2));
            float sway = 0.06f * H * Mth.sin(age * 0.05f + i);
            Vector3f q = new Vector3f(top).add(Mth.cos(a) * rr + sway, H * (0.05f + 0.18f * hash(inst.seed, i, 3)), Mth.sin(a) * rr);
            float sz = H * (0.45f + 0.2f * hash(inst.seed, i, 4)) * descend;
            buf.billboard(ctx, CANOPY, VfxBlend.ALPHA, q, sz, a, VfxVertexBuffer.withAlpha(LEAFY, 0.95f * fade));
            buf.billboard(ctx, CANOPY, VfxBlend.ADD, q, sz * 1.05f, a, VfxVertexBuffer.withAlpha(EMERALD, 0.25f * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(top).add(0, H * 0.15f, 0), H * 0.9f, EMERALD, 0.35f * fade);
        // mana spiralling up the trunk
        int m = ctx.seg(10, 5);
        for (int i = 0; i < m; i++) {
            float lt = ((age * 0.02f) + hash(inst.seed, i, 5)) % 1f, a = lt * 9 + i;
            Vector3f q = new Vector3f(base).add(Mth.cos(a) * thick, lt * H * 0.8f, Mth.sin(a) * thick);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.18f, a, VfxVertexBuffer.withAlpha(EMERALD, fade * Mth.sin(lt * Mth.PI)));
        }
    }

    private void drain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), w = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        float fade = life(inst, age, 2, 5);
        Vector3f sideV = side(new Vector3f(dir).div(len));
        int segs = ctx.seg(8, 4);
        Vector3f prev = new Vector3f(a);
        for (int k = 1; k <= segs; k++) {
            float f = (float) k / segs;
            Vector3f cur = new Vector3f(a).lerp(b, f).add(new Vector3f(sideV).mul(0.4f * Mth.sin(f * 7 + age * 0.4f) * Mth.sin(f * Mth.PI)));
            DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, cur, 0, 1, w, VfxVertexBuffer.withAlpha(EMERALD, 0.5f * fade));
            prev = cur;
        }
        int m = ctx.seg(10, 5);
        for (int i = 0; i < m; i++) {
            float f = ((t * 2.2f) + hash(inst.seed, i, 1)) % 1f;
            Vector3f q = new Vector3f(a).lerp(b, f).add(new Vector3f(sideV).mul(0.4f * Mth.sin(f * 7 + age * 0.4f) * Mth.sin(f * Mth.PI)));
            buf.billboard(ctx, LEAF, VfxBlend.ADD, q, 0.25f, f * 10 + i, VfxVertexBuffer.withAlpha(EMERALD, fade));
        }
        VfxBloom.glow(ctx, buf, b, 0.8f, EMERALD, 0.5f * fade);
    }
}
