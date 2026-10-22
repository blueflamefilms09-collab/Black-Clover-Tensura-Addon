package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritlight.SpiritLightLod;
import com.newuniverse.nusmp.spiritlight.SpiritLightRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the four Spirit of Light props (docs/magic/spirit_light.md). SPIRIT_LIGHT_1 is the lion cub (variant 0, beside the owner's shoulder) and the seraph (variant 1, behind the owner);
 * SPIRIT_LIGHT_2 is the piercing beam (variant 2) and the Glowing pulse ring (variant 3). What is drawn is {@link SpiritLightDraw}'s (real models from tools/gen_spirit_light_geo.py: hand-painted
 * cel-shaded sheets, three levels of detail each, a glow-only model for the halo, the eyes, the rings and the core).
 *
 * <p>Cost, all on the client: every form is placed from its OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level of
 * detail follows the distance and the effect quality ({@link SpiritLightLod}), all spirits share one quad budget per frame (the local player's own close spirit is counted but never degraded), nothing is
 * drawn past the last range, nothing is allocated per frame (primitive math only), the wings, rings and tail move by bone rotation only (the clips), and the few motes of light go through the client
 * particle budget once per game tick.</p>
 */
public final class SpiritLightPropPainter {
    private SpiritLightPropPainter() {}

    private static final SpiritLightLod.QuadBudget BUDGET = new SpiritLightLod.QuadBudget();
    private static final int[] LAST_MOTE = new int[16];
    private static final double[] TMP = new double[3];                                              // the anchor of the form being drawn (render thread only)
    private static final ThemeKey LIGHT = ThemeKey.of("magic.LIGHT");

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SPIRIT_LIGHT_1, SpiritLightPropPainter::form);
        PropPainters.register(PropKind.SPIRIT_LIGHT_2, SpiritLightPropPainter::effect);
    }

    // ================================================================================================ the cub and the seraph
    private static void form(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = SpiritLightRules.variant(param);
        boolean seraph = variant == SpiritLightRules.V_SERAPH;
        if (!seraph && SpiritLightRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (seraph) SpiritLightRules.seraphTo(TMP, ox, oy, oz, by); else SpiritLightRules.companionTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
        }
        ay += seraph ? 0.07 * Mth.sin(age * 0.07f) : 0.045 * Mth.sin(age * 0.12f);                    // the hover
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = SpiritLightLod.level(seraph, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == SpiritLightLod.CULL) return;
        boolean own = SpiritLightLod.ownClose(o != null && o == mc.player, dsq);
        int lod = SpiritLightLod.fit(seraph, wanted, BUDGET, SpiritLightLod.frameKey(e.level().getGameTime(), partial), own);
        if (lod == SpiritLightLod.CULL) return;
        float fade = Mth.clamp(Math.min((e.life() + partial) / 8f, (e.maxLife() - e.life() - partial) / 8f), 0f, 1f);
        if (fade <= 0.01f) return;

        int dt = SpiritLightRules.sinceCast(param, e.level().getGameTime());
        boolean casting = dt < SpiritLightRules.CAST_CLIP_TICKS;
        String clip = casting ? "cast" : "idle";
        float secs = casting ? (dt + partial) / 20f : age / 20f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        SpiritLightDraw.form(pose, buffers, seraph, lod, clip, secs, age, fade, light);
        pose.popPose();
        if (lod <= SpiritLightLod.HALF && dsq < 24 * 24) motes(e, ax, ay + (seraph ? 1.3 : 0.3), az);
    }

    /** Motes of light from the custom particle library, once per game tick per form, through the client particle budget. */
    private static void motes(MagicPropEntity e, double x, double y, double z) {
        int slot = e.getId() & 15;
        if (LAST_MOTE[slot] == e.tickCount || e.tickCount % 4 != 0) return;
        LAST_MOTE[slot] = e.tickCount;
        Fx.send(e.level(), LIGHT, x, y, z, 1, 0.22);
    }

    // ================================================================================================ the beam and the pulse ring
    private static void effect(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = SpiritLightRules.variant(param);
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double dx = ex - cam.x, dy = ey - cam.y, dz = ez - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        boolean pw = SpiritLightRules.powered(param);
        long key = SpiritLightLod.frameKey(e.level().getGameTime(), partial);
        if (variant == SpiritLightRules.V_BEAM) {
            double range = SpiritLightLod.BEAM_RANGE;
            if (dsq > range * range || !SpiritLightLod.fitSmall(BUDGET, key, SpiritLightLod.BEAM_QUADS)) return;
            float len = (float) SpiritLightRules.beamLength(param);
            double yawR = Math.toRadians(e.getYRot()), pitchR = Math.toRadians(SpiritLightRules.beamPitch(param));
            float cp = Mth.cos((float) pitchR);
            float bx = -Mth.sin((float) yawR) * cp, by = -Mth.sin((float) pitchR), bz = Mth.cos((float) yawR) * cp;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the beam runs in world axes
            SpiritLightDraw.beam(pose, buffers, len, bx, by, bz, (e.life() + partial) / SpiritLightRules.BEAM_LIFE, pw, len < SpiritLightRules.beamRange(pw) - 0.5);
            pose.popPose();
            return;
        }
        if (dsq > SpiritLightLod.PULSE_RANGE * SpiritLightLod.PULSE_RANGE || !SpiritLightLod.fitSmall(BUDGET, key, SpiritLightLod.PULSE_QUADS)) return;
        double ax = ex, ay = ey, az = ez;
        Entity o = e.target();
        if (o != null) { ax = Mth.lerp(partial, o.xo, o.getX()); ay = Mth.lerp(partial, o.yo, o.getY()); az = Mth.lerp(partial, o.zo, o.getZ()); }       // the ring follows the owner
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        pose.translate(ax - ex, ay - ey, az - ez);
        SpiritLightDraw.pulse(pose, buffers, (float) SpiritLightRules.pulseRadius(param), (e.life() + partial) / SpiritLightRules.PULSE_LIFE, pw);
        pose.popPose();
    }
}
