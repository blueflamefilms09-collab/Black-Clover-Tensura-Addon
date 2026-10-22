package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.undine.UndineLod;
import com.newuniverse.nusmp.undine.UndineRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the Undine prop (docs/magic/undine.md). UNDINE_1 is the companion (variant 0, floating beside the caster's right shoulder) and the full form (variant 1, standing on a ring of waves
 * behind the caster). What is drawn is {@link UndineDraw}'s (real models from tools/gen_undine_geo.py: hand-painted sheets, three levels of detail, a translucent water pass, a soft additive foam pass, the wave base).
 *
 * <p>Cost, all on the client: every form is placed from its OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level of detail follows
 * the distance and the effect quality ({@link UndineLod}), all forms share one quad budget per frame (the owner's own close one is never dropped, it only drops a level), nothing is drawn past 64 blocks (the
 * companion) or 112 blocks (the full form), nothing is allocated per frame (primitive math only; the arrays below are reused), the hair, gown and arms move by bone rotation only (the clips), and the few
 * droplet particles go through the client particle budget once every few game ticks.</p>
 */
public final class UndinePropPainter {
    private UndinePropPainter() {}

    private static final UndineLod.QuadBudget BUDGET = new UndineLod.QuadBudget();
    private static final int[] LAST_SPRAY = new int[16];
    private static final double[] TMP = new double[3];                                              // the anchor of the form being drawn (render thread only)
    private static final ThemeKey WATER = ThemeKey.of("magic.WATER");

    /** Called once by PropPainterRegistry. */
    public static void register() { PropPainters.register(PropKind.UNDINE_1, UndinePropPainter::paint); }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = UndineRules.variant(param);
        boolean full = variant == UndineRules.V_FULL;
        if (!full && UndineRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (full) UndineRules.fullTo(TMP, ox, oy, oz, by); else UndineRules.companionTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay + 1.0 - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = UndineLod.level(full, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == UndineLod.CULL) return;
        boolean mine = o != null && o == mc.player;
        int lod = UndineLod.fit(full, wanted, BUDGET, UndineLod.frameKey(e.level().getGameTime(), partial), UndineLod.ownerPriority(mine, dsq));
        if (lod == UndineLod.CULL) return;
        float life = e.life() + partial;
        float fade = full ? UndineRules.fade(life, e.maxLife(), 10f, 12f) : Math.min(1f, life / 10f);
        if (fade <= 0.01f) return;

        long now = e.level().getGameTime();
        int since = UndineRules.sinceStamp(UndineRules.extra(param), now);
        String clip = UndineRules.clip(full, since);
        float secs = UndineRules.clipSeconds(clip, age, since + partial);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        UndineDraw.undine(pose, buffers, full, lod, clip, secs, age, fade, light);
        pose.popPose();
        if (lod <= UndineLod.HALF && dsq < 28 * 28) spray(e, ax, ay + (full ? 0.4 : 1.0), az, full);
    }

    /** Droplet particles from the custom particle library, once per few game ticks per form, through the client particle budget. */
    private static void spray(MagicPropEntity e, double x, double y, double z, boolean full) {
        int slot = e.getId() & 15, every = full ? 3 : 12;
        if (LAST_SPRAY[slot] == e.tickCount || e.tickCount % every != 0) return;
        LAST_SPRAY[slot] = e.tickCount;
        Fx.send(e.level(), WATER, x, y, z, full ? 2 : 1, full ? 1.6 : 0.6);
    }
}
