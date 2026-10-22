package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.firecast.FireCastLod;
import com.newuniverse.nusmp.firecast.FireCastRules;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the four fire casting props (docs/magic/fire_casting.md). FIRECAST_1 is Magna's tossed ball (variant 0) and Leopold's charge spiral (variant 1); FIRECAST_2 is Fuegoleon's
 * Salamander: the small red form on the caster's shoulder (variant 2) and the big golden dragon above and behind the caster (variant 3). What is drawn is {@link FireCastDraw}'s (real models from
 * tools/gen_salamander_geo.py: hand-painted sheets, three levels of detail each, a glow-only model for the slit eyes and the furnace of the mouth).
 *
 * <p>Cost, all on the client: every form is placed from its OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level of
 * detail follows the distance and the effect quality ({@link FireCastLod}), all forms share one quad budget per frame, nothing is drawn past the last range, nothing is allocated per frame
 * (primitive math only), the wings and tail move by bone rotation only (the clips), and the few ember particles go through the client particle budget once per game tick.</p>
 */
public final class FireCastPropPainter {
    private FireCastPropPainter() {}

    private static final FireCastLod.QuadBudget BUDGET = new FireCastLod.QuadBudget();
    private static final int[] LAST_EMBER = new int[16];
    private static final double[] TMP = new double[3];                                              // the anchor of the form being drawn (render thread only)
    private static final ThemeKey FLAME = ThemeKey.of("magic.FLAME");

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.FIRECAST_1, FireCastPropPainter::ballOrSpiral);
        PropPainters.register(PropKind.FIRECAST_2, FireCastPropPainter::spirit);
    }

    // ================================================================================================ the Salamander
    private static void spirit(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = FireCastRules.variant(param);
        boolean dragon = variant == FireCastRules.V_DRAGON;
        if (!dragon && FireCastRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (dragon) FireCastRules.dragonPositionTo(TMP, ox, oy, oz, by); else FireCastRules.shoulderTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = FireCastLod.level(dragon, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == FireCastLod.CULL) return;
        int lod = FireCastLod.fit(dragon, wanted, BUDGET, FireCastLod.frameKey(e.level().getGameTime(), partial), o != null && o == mc.player);
        if (lod == FireCastLod.CULL) return;
        float fade = Mth.clamp(Math.min((e.life() + partial) / 8f, (e.maxLife() - e.life() - partial) / 8f), 0f, 1f);
        if (fade <= 0.01f) return;

        // the pose and the face come from the prop's param and the clock (SalamanderMoodRules.look: pure, allocation free); the server only wrote a pose id and a start tick
        int look = SalamanderMoodRules.look(dragon, param, e.level().getGameTime(), e.life(), e.maxLife());
        int poseId = SalamanderMoodRules.poseOfLook(look), face = SalamanderMoodRules.faceOfLook(look);
        String clip = SalamanderMoodRules.clipName(dragon, poseId);
        float secs = SalamanderMoodRules.looping(dragon, poseId) ? age / 20f : (SalamanderMoodRules.elapsedOfLook(look) + partial) / 20f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        FireCastDraw.salamander(pose, buffers, dragon, lod, clip, secs, age, fade, light, face);
        pose.popPose();
        if (lod == FireCastLod.FULL && !dragon && dsq < 24 * 24) embers(e, ax, ay + 0.35, az);
        else if (lod <= FireCastLod.HALF && dragon) embers(e, ax, ay, az);
    }

    /** Ember particles from the custom particle library, once per game tick per form, through the client particle budget. */
    private static void embers(MagicPropEntity e, double x, double y, double z) {
        int slot = e.getId() & 15;
        if (LAST_EMBER[slot] == e.tickCount || e.tickCount % 3 != 0) return;
        LAST_EMBER[slot] = e.tickCount;
        Fx.send(e.level(), FLAME, x, y, z, 1, 0.18);
    }

    // ================================================================================================ the ball and the spiral
    private static void ballOrSpiral(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = FireCastRules.variant(param);
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double dx = ex - cam.x, dy = ey - cam.y, dz = ez - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        float fade = Mth.clamp(Math.min((e.life() + partial) / 4f, (e.maxLife() - e.life() - partial) / 4f), 0f, 1f);
        if (variant == FireCastRules.V_BALL) {
            if (dsq > FireCastLod.BALL_RANGE * FireCastLod.BALL_RANGE) return;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));         // undo the renderer's turn
            pose.translate(0f, 0.35f, 0f);
            FireCastDraw.ball(pose, buffers, age, fade);
            pose.popPose();
            return;
        }
        if (dsq > FireCastLod.SPIRAL_RANGE * FireCastLod.SPIRAL_RANGE) return;
        int level = Mth.clamp(FireCastRules.extra(param), 1, FireCastRules.MAX_LEVEL);
        Entity o = e.target();
        double ax = ex, ay = ey + 0.5, az = ez;
        float lx = 0f, ly = 0f, lz = 1f;
        if (o instanceof LivingEntity l) {                                                          // in front of the owner's eyes, along their look, from their own interpolated body
            Vec3 eye = l.getEyePosition(partial), look = l.getViewVector(partial);
            ax = eye.x + look.x * 1.5; ay = eye.y + look.y * 1.5 - 0.1; az = eye.z + look.z * 1.5;
            lx = (float) look.x; ly = (float) look.y; lz = (float) look.z;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        pose.translate(ax - ex, ay - ey, az - ez);
        FireCastDraw.spiral(pose, buffers, level, age, fade, FireCastLod.spiralPoints(level, dsq), lx, ly, lz);
        pose.popPose();
    }
}
