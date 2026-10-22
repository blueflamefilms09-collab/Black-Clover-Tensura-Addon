package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules;
import com.newuniverse.nusmp.spiritdark.SpiritDarkLod;
import com.newuniverse.nusmp.spiritdark.SpiritDarkRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the Spirit of Darkness prop (docs/magic/spirit_dark.md): the smoky shadow-wisp companion above the owner's left shoulder (variant 0) and the powered miniature demon behind the
 * owner (variant 1). What is drawn is {@link SpiritDarkDraw} (real models from tools/gen_spirit_dark_geo.py: cel-shaded hand-painted sheets, three levels of detail each, seven face sheets, a glow-only
 * model); which pose and face come from the prop word the server sets ({@link DarkMoodRules}).
 *
 * <p>Cost, all on the client: every form is placed from its OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level of
 * detail follows the distance and the effect quality ({@link SpiritDarkLod}: companion culled at 64 blocks, powered at 112), all spirits share one quad budget per frame and the owner's own close spirit
 * is never degraded; nothing is allocated per frame (primitive math only; the clip names are constants); the few smoke particles go through the client particle budget once per game tick.</p>
 */
public final class SpiritDarkPropPainter {
    private SpiritDarkPropPainter() {}

    private static final SpiritDarkLod.QuadBudget BUDGET = new SpiritDarkLod.QuadBudget();
    private static final int[] LAST_SMOKE = new int[16];
    private static final double[] TMP = new double[3];                                              // the anchor of the form being drawn (render thread only)
    private static final ThemeKey DARK = ThemeKey.of("magic.DARK");

    /** Called once by PropPainterRegistry. */
    public static void register() { PropPainters.register(PropKind.SPIRITDARK_1, SpiritDarkPropPainter::paint); }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = SpiritDarkRules.variant(param);
        boolean powered = variant == SpiritDarkRules.V_POWERED;
        if (!powered && SpiritDarkRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        boolean moving = false;
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (powered) SpiritDarkRules.poweredTo(TMP, ox, oy, oz, by); else SpiritDarkRules.companionTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
            Vec3 d = o.getDeltaMovement();
            moving = d.x * d.x + d.z * d.z > 0.0064;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = SpiritDarkLod.level(powered, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == SpiritDarkLod.CULL) return;
        boolean ownerClose = SpiritDarkLod.ownerClose(o != null && o == mc.player, dsq);
        int lod = SpiritDarkLod.fit(powered, wanted, ownerClose, BUDGET, SpiritDarkLod.frameKey(e.level().getGameTime(), partial));
        if (lod == SpiritDarkLod.CULL) return;
        float fade = Mth.clamp(Math.min((e.life() + partial) / 8f, (e.maxLife() - e.life() - partial) / 8f), 0f, 1f);
        if (fade <= 0.01f) return;

        long now = e.level().getGameTime();
        DarkMoodRules.Pose active = DarkMoodRules.activePose(param, now, powered);
        String clip = active.clip;
        if (powered && active == DarkMoodRules.rest(true) && moving) clip = "flap";
        float secs = DarkMoodRules.clipSeconds(param, now, partial, age / 20f, powered);
        int face = DarkMoodRules.activeFace(param, now, powered).ordinal();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        SpiritDarkDraw.spirit(pose, buffers, powered, lod, clip, secs, age, fade, light, face);
        pose.popPose();
        if (lod <= SpiritDarkLod.HALF && dsq < 24 * 24) smoke(e, ax, ay + (powered ? 0.9 : 0.0), az);
    }

    /** Dark smoke particles from the custom particle library, once per game tick per form, through the client particle budget. */
    private static void smoke(MagicPropEntity e, double x, double y, double z) {
        int slot = e.getId() & 15;
        if (LAST_SMOKE[slot] == e.tickCount || e.tickCount % 4 != 0) return;
        LAST_SMOKE[slot] = e.tickCount;
        Fx.send(e.level(), DARK, x, y, z, 1, 0.22);
    }
}
