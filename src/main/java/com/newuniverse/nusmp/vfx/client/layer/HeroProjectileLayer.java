package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.HeroSolid;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why (docs/heroweapon/flat_parts.md): the arrow of light of the Legendary Bow and the tip of a spear thrust were a flat starlight streak ({@code STAR_MAGIC}): no arrow, no spear head, only a line of sparkles. These two
 * shapes draw them as what they are: a small SOLID shaded 3D model ({@code SolidModels.arrow}, {@code SolidModels.spearTip}) that flies from {@code from} to {@code to}, point first, on a short glow trail.
 *
 * <ul>
 *   <li>{@code HERO_ARROW}: an arrow (shaft, nock, broadhead with a ridge, crossed vanes). power = size (0.35 to 2.4: the draw, the wide Pride arrows, the giant meteor arrow).</li>
 *   <li>{@code HERO_SPEAR}: a spear tip (socket, collar and lugs, leaf blade with a ridge). power = size.</li>
 * </ul>
 * Both: color = the weapon's light colour, duration = ticks; the model lands after 70 % of the time and stays (fading) after that.
 *
 * <p>{@code visual.heroSolidParts = false} ({@link HeroSolid#solid()}, CLIENT config) draws the old look exactly: the {@code STAR_MAGIC} starlight beam, nothing solid. The shapes are sent by the server instead of
 * {@code STAR_MAGIC} (same single packet, no extra packet), so a client that turns the switch off sees what it always saw.
 *
 * <p>Worst case per effect: one model of at most {@link HeroSolid#MAX_QUADS_EACH} quads + a trail (2 quads) + at most 4 glints (8 quads) + a head glow (1 quad): under {@link HeroSolid#VERTEX_BUDGET} vertices. Per frame at
 * most {@link HeroSolid#MAX_PER_FRAME} models on a client (the rest keep the trail), none beyond 40 blocks ({@code SolidParts.MAX_DISTANCE}), and the manager's own caps on running effects and on the frame's vertices still
 * apply. Nothing is allocated per frame except the few short-lived vectors every layer here makes; the server draws nothing.
 */
public final class HeroProjectileLayer extends AbstractVfxLayer {
    private static final Vector3f HEAD = new Vector3f(), AIM = new Vector3f(), CENTRE = new Vector3f(), TAIL = new Vector3f();       // render thread only

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.HERO_ARROW, VfxShape.HERO_SPEAR); }

    @Override
    public int defaultDuration(VfxShape shape) { return 8; }

    @Override
    public int defaultColor(VfxShape shape) { return 0xFFFFE8A0; }

    @Override
    public int vertexBudget(VfxShape shape) { return HeroSolid.VERTEX_BUDGET; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        float size = Math.max(0.35f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx));
        Vector3f to = ctx.rel(inst.to(ctx));
        AIM.set(to).sub(from);
        float len = AIM.length();
        if (!HeroSolid.solid() || len < 1.0E-3f) {
            legacy(inst, ctx, buf, age, fade, size, from, to);
            return;
        }
        AIM.div(len);
        float p = HeroSolid.flight(age, inst.duration);
        HEAD.set(from).fma(len * p, AIM);                                               // the point of the arrow / tip
        int tint = VfxVertexBuffer.withAlpha(inst.color, fade);
        float trail = Math.min(len * p, 3.5f + 1.5f * size);                            // a short glow behind the point, not the whole line
        TAIL.set(HEAD).fma(-trail, AIM);
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, HEAD, TAIL, 0.16f * size, 0.01f, 2, age * 0.08f, tint, tint);
        int count = ctx.seg(4, 2);
        for (int n = 0; n < count; n++) {                                               // a few glints along the trail
            float t = (n + 0.5f) / count;
            float glint = 0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(age * 0.35f + n));
            CENTRE.set(HEAD).fma(-trail * t, AIM);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, CENTRE, 0.35f * size, age * 0.17f + n * 2.4f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade * glint));
        }
        boolean arrow = inst.shape == VfxShape.HERO_ARROW;
        float scale = arrow ? Mth.clamp(0.7f + 0.35f * size, 0.8f, 1.5f) : Mth.clamp(0.8f + 0.4f * size, 0.9f, 1.8f);
        float alpha = Math.min(1f, fade * 3f);                                          // it stays solid, then fades in the last third
        if (alpha > 0.02f && HeroSolid.takeSlot(ctx)) {
            if (arrow) {
                CENTRE.set(HEAD).fma(-0.5f * scale, AIM);                               // the model is 1.0 long, centred, the point at +0.5
                if (SolidModels.arrow(buf, CENTRE, AIM, scale, alpha, inst.color)) VfxBloom.glow(ctx, buf, HEAD, 0.25f * size, inst.color, 0.5f * alpha);
            } else {
                CENTRE.set(HEAD).fma(-0.56f * scale, AIM);                              // the collar is the origin, the point 0.56 ahead of it
                if (SolidModels.spearTip(buf, CENTRE, AIM, scale, alpha, inst.color, false)) VfxBloom.glow(ctx, buf, HEAD, 0.3f * size, inst.color, 0.5f * alpha);
            }
        }
    }

    /** The old look, exactly what {@code STAR_MAGIC} drew: a starlight beam and glints along the whole line. */
    private static void legacy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, float fade, float size, Vector3f from, Vector3f to) {
        int tint = VfxVertexBuffer.withAlpha(inst.color, fade);
        Vector3f tip = new Vector3f(from).lerp(to, Mth.clamp(age / Math.max(1f, inst.duration), 0f, 1f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, from, 0.16f * size, 0.01f, 2, age * 0.08f, tint, tint);
        int count = ctx.seg(12, 5);
        Vector3f side = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
        for (int n = 0; n < count; n++) {
            float t = (n + 0.5f) / count;
            Vector3f centre = new Vector3f(from).lerp(to, t);
            float angle = age * 0.17f + n * 2.4f;
            centre.add(new Vector3f(side).mul(Mth.cos(angle) * 0.55f * size));
            centre.add(new Vector3f(up).mul(Mth.sin(angle) * 0.35f * size));
            float glint = 0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(age * 0.35f + n));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, centre, 0.45f * size, angle, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade * glint));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 0.7f * size, -angle, VfxVertexBuffer.withAlpha(inst.color, fade * glint));
        }
    }
}
