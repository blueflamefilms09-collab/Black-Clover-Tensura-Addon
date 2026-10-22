package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.sylph.SylphLod;
import com.newuniverse.nusmp.sylph.SylphRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the Sylph prop (docs/magic/sylph.md): the small fairy beside the caster's head (variant 0) and the powered form above and behind them (variant 1). What is drawn is
 * {@link SylphDraw}'s (real models from tools/gen_sylph_geo.py: hand-painted sheets, three levels of detail each, translucent wings, a glow-only model for the eyes and the hem).
 *
 * <p>Cost, all on the client: every Sylph is placed from her OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level
 * of detail follows the distance and the effect quality, all Sylphs share one quad budget per frame ({@link SylphView}), nothing is drawn past the last range (64 blocks small, 112 powered),
 * nothing is allocated per frame (primitive math only), and she moves by bone rotation only (the clips).</p>
 */
public final class SylphPropPainter {
    private SylphPropPainter() {}

    private static final double[] TMP = new double[3];                                              // the anchor of the Sylph being drawn (render thread only)

    /** Called once by PropPainterRegistry. */
    public static void register() { PropPainters.register(PropKind.SYLPH_1, SylphPropPainter::spirit); }

    private static void spirit(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = SylphRules.variant(param);
        boolean powered = variant == SylphRules.V_POWERED;
        if (!powered && SylphRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        boolean moving = false;
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so she never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (powered) SylphRules.poweredTo(TMP, ox, oy, oz, by); else SylphRules.smallTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
            moving = o.getDeltaMovement().horizontalDistanceSqr() > 0.004;
        }
        if (!powered) ay += 0.05 * Mth.sin(age * 0.11f);                                    // she bobs a little on the air
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        long now = e.level().getGameTime();
        int lod = SylphView.lod(powered ? SylphLod.POWERED : SylphLod.SMALL, dsq, now, partial, o != null && o == mc.player);
        if (lod == SylphLod.CULL) return;
        float fade = Mth.clamp(Math.min((e.life() + partial) / 8f, (e.maxLife() - e.life() - partial) / 8f), 0f, 1f);
        if (fade <= 0.01f) return;
        int since = SylphRules.sinceStamp(SylphRules.extra(param), now);
        int mood = SylphRules.pose(param);
        String clip = powered ? SylphRules.poweredClip(since, e.life(), mood) : SylphRules.smallClip(since, moving, now, e.seed(), mood);
        float secs = ("cast".equals(clip) || "powered_cast".equals(clip)) ? (since + partial) / 20f : age / 20f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        SylphDraw.sylph(pose, buffers, powered, lod, clip, secs, age, fade, light);
        pose.popPose();
    }
}
