package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.panel;
import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.stand;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.42: Mirror Magic (Gauche Adlai), after the owner's reference frames: real antique mirrors summoned out of violet mana.
 * Textures from tools/gen_mirror_vfx_textures.py. Built as layered cards so it reads like a 3D object at any angle:
 * corona (behind) -> glass (tinted, with its own sheen) -> a light-distortion ripple across the glass -> the silver frame in
 * front, with an ethereal violet edge-glow over it.
 * <ul>
 *   <li>MIRROR_FRAME: one ornate mirror at 'from' facing 'to' (shape chosen by the effect's seed: oval, arched, round, gothic
 *       crest, diamond). Pops in with a flash, breathes, its corona flickering and a ripple running over the glass.</li>
 *   <li>MIRROR_ARRAY: power mirrors of mixed shapes turning round the caster (follows), facing outward, bobbing.</li>
 *   <li>MIRROR_SHATTER: a mirror bursts: a flash, a violet shock ring and glass shards that tumble outward and fall.</li>
 *   <li>MIRROR_STEP: a Real Double steps out of the glass: prismatic light streaks (red to violet) rising round a silhouette
 *       that fades in from the light.</li>
 * </ul>
 */
public class MirrorLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final String[] SHAPES = {"oval", "arch", "round", "crest", "diamond"};
    static final ResourceLocation[] FRAME = new ResourceLocation[5], GLASS = new ResourceLocation[5];
    static {
        for (int k = 0; k < 5; k++) { FRAME[k] = t("mirror_frame_" + SHAPES[k]); GLASS[k] = t("mirror_glass_" + SHAPES[k]); }
    }
    static final ResourceLocation CORONA = t("mirror_corona"), RIPPLE = t("mirror_ripple"), PRISM = t("mirror_prism"), SILHOUETTE = t("arc_silhouette");

    static final int SILVER = 0xFFB8B4CC, GLASS_TINT = 0xFFDCD2FF, DEEP = 0xFF7A4CFF;
    /** Prism colours, red -> violet. */
    static final int[] PRISM_COLS = {0xFFFF6A7A, 0xFFFFB45A, 0xFFFFF07A, 0xFF7AFFA8, 0xFF6AC8FF, 0xFFA87AFF};

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.MIRROR_FRAME, VfxShape.MIRROR_ARRAY, VfxShape.MIRROR_SHATTER, VfxShape.MIRROR_STEP);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case MIRROR_FRAME -> 40;
            case MIRROR_ARRAY -> 800;
            case MIRROR_SHATTER -> 24;
            case MIRROR_STEP -> 20;
            default -> 30;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFB89CFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.MIRROR_SHATTER) VfxShake.add(inst.payload.from(), 0.35f, 6);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MIRROR_FRAME -> single(inst, ctx, buf);
            case MIRROR_ARRAY -> array(inst, ctx, buf);
            case MIRROR_SHATTER -> shatter(inst, ctx, buf);
            case MIRROR_STEP -> step(inst, ctx, buf);
            default -> { }
        }
    }

    /**
     * One mirror: centre c, facing n (unit), half-height h, shape index k, age for animation, alpha a. The textures are square
     * with the mirror's shape inside, so a square card of half-size h carries any shape.
     */
    static void mirror(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, Vector3f n, float h, int k, float age, float a, int col, long seed) {
        Vector3f right = side(n), up = new Vector3f(right).cross(n).normalize();
        if (up.y < 0) { up.negate(); right.negate(); }
        Vector3f back = new Vector3f(n).mul(-0.06f * h), front = new Vector3f(n).mul(0.03f * h);
        // the mana corona behind: two layers turning against each other, flickering
        float flick = 0.75f + 0.25f * Mth.sin(age * 0.7f + seed % 13);
        Vector3f cb = new Vector3f(c).add(back);
        float cs = h * 1.55f;
        for (int l = 0; l < 2; l++) {
            float rot = (l == 0 ? 1 : -1) * age * 0.03f + l;
            Vector3f r = new Vector3f(right).mul(Mth.cos(rot)).add(new Vector3f(up).mul(Mth.sin(rot)));
            Vector3f u = new Vector3f(up).mul(Mth.cos(rot)).sub(new Vector3f(right).mul(Mth.sin(rot)));
            panel(buf, CORONA, VfxBlend.ADD, cb, r, u, cs, cs, VfxVertexBuffer.withAlpha(l == 0 ? col : DEEP, 0.55f * a * flick));
        }
        // the glass, and a distortion ripple running across it
        panel(buf, GLASS[k], VfxBlend.ALPHA, c, right, up, h, h, VfxVertexBuffer.withAlpha(GLASS_TINT, 0.9f * a));
        float rp = (age * 0.03f + (seed & 7) * 0.13f) % 1f;
        panel(buf, RIPPLE, VfxBlend.ADD, new Vector3f(c).add(front), right, up, h * (0.3f + 0.6f * rp), h * (0.3f + 0.6f * rp),
                VfxVertexBuffer.withAlpha(WHITE, 0.35f * a * (1 - rp)));
        // the silver frame, and its violet edge-glow
        Vector3f cf = new Vector3f(c).add(front).add(front);
        panel(buf, FRAME[k], VfxBlend.ALPHA, cf, right, up, h, h, VfxVertexBuffer.withAlpha(SILVER, a));
        panel(buf, FRAME[k], VfxBlend.ADD, cf, right, up, h * 1.03f, h * 1.03f, VfxVertexBuffer.withAlpha(DEEP, 0.3f * a * flick));
    }

    private void single(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power;
        Vector3f c = ctx.rel(inst.from(ctx)), n = ctx.rel(inst.to(ctx)).sub(c);
        if (n.lengthSquared() < 1e-4f) n.set(0, 0, 1);
        n.normalize();
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1)), fade = life(inst, age, 0, 6);
        float breathe = 1 + 0.02f * Mth.sin(age * 0.2f);
        int k = Math.floorMod((int) inst.seed, 5);
        mirror(buf, ctx, new Vector3f(c).add(0, 0.05f * Mth.sin(age * 0.12f), 0), n, s * pop * breathe, k, age, fade, inst.color, inst.seed);
        // the summoning flash and a few motes of mana
        float flash = Mth.clamp(1 - age / 8f, 0, 1);
        if (flash > 0) VfxBloom.glow(ctx, buf, c, 2.2f * s, inst.color, flash * fade);
        int m = ctx.seg(6, 3);
        for (int i = 0; i < m; i++) {
            float lt = ((age * 0.03f) + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * s * 1.2f, (lt - 0.5f) * 2.2f * s, Mth.sin(ang) * s * 1.2f);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.18f * s, age * 0.1f, VfxVertexBuffer.withAlpha(inst.color, fade * Mth.sin(lt * Mth.PI)));
        }
    }

    private void array(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int count = Math.max(1, Math.min(8, Math.round(inst.power)));
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 0, 12), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float rad = 1.9f + 0.1f * count;
        for (int k = 0; k < count; k++) {
            float ang = Mth.TWO_PI * k / count + age * 0.03f;
            Vector3f n = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
            Vector3f c = new Vector3f(g).add(new Vector3f(n).mul(rad * open)).add(0, 0.5f + 0.12f * Mth.sin(age * 0.1f + k * 1.7f), 0);
            mirror(buf, ctx, c, n, 0.55f * open, (k + (int) Math.floorMod(inst.seed, 5)) % 5, age + k * 7, fade, inst.color, inst.seed + k);
        }
        VfxBloom.glow(ctx, buf, g, 1.4f, inst.color, 0.25f * fade);
    }

    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 0, 8);
        VfxBloom.glow(ctx, buf, c, 2f * s * (1 - t * 0.5f), inst.color, (1 - t) * 0.9f);
        buf.billboard(ctx, CORONA, VfxBlend.ADD, c, 3.2f * s * VfxAnim.easeOutCubic(t), age * 0.05f, VfxVertexBuffer.withAlpha(inst.color, 0.7f * (1 - t)));
        int n = ctx.seg(18, 8);
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, el = (hash(inst.seed, i, 2) - 0.35f) * 1.6f;
            float d = s * (0.3f + 2.4f * VfxAnim.easeOutCubic(t) * (0.5f + hash(inst.seed, i, 3)));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * d - 2.5f * t * t, Mth.sin(ang) * Mth.cos(el) * d);
            int col = i % 3 == 0 ? inst.color : SILVER;
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, q, (0.25f + 0.35f * hash(inst.seed, i, 4)) * s, age * 0.4f * (hash(inst.seed, i, 5) - 0.5f) + i,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.3f), fade));
        }
    }

    private void step(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), h = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 0, 6);
        // prismatic streaks rising round the figure, red through violet
        int n = PRISM_COLS.length;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.08f, r = 0.7f + 0.2f * Mth.sin(age * 0.3f + i);
            Vector3f foot = new Vector3f(g).add(Mth.cos(ang) * r, 0.05f + 0.6f * t, Mth.sin(ang) * r);
            stand(ctx, buf, PRISM, VfxBlend.ADD, foot, 0.35f, h * (0.6f + 0.6f * t), VfxVertexBuffer.withAlpha(PRISM_COLS[i], 0.8f * fade));
        }
        // the double, fading in out of the light
        float show = Mth.clamp((t - 0.2f) / 0.5f, 0, 1);
        stand(ctx, buf, SILHOUETTE, VfxBlend.ALPHA, new Vector3f(g).add(0, 0.02f, 0), 0.95f, h, VfxVertexBuffer.withAlpha(GLASS_TINT, 0.6f * show * fade));
        stand(ctx, buf, SILHOUETTE, VfxBlend.ADD, new Vector3f(g).add(0, 0.02f, 0), 1.0f, h * 1.02f, VfxVertexBuffer.withAlpha(inst.color, 0.5f * fade));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(g).add(0, 0.04f, 0)), 0.4f, 1.1f, ctx.seg(14, 8), 1, 0, VfxVertexBuffer.withAlpha(inst.color, 0.6f * fade));
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, h * 0.5f, 0), 1.6f, inst.color, 0.6f * fade * (1 - t));
    }
}
