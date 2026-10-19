package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.AirPropWord;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Air Magic: how its summons look (AIR_1, variant in the low byte of param, see prop.AirPropWord).
 * <ul>
 *   <li>0 and 2, the air blade: air_blade (a crescent of compressed air with a bright edge and a glowing hub), spinning about its vertical axis; a flying blade
 *       lies along its flight (the prop's pitch), an orbiting blade stands on edge. Two trailing streaks (additive) follow a flying blade.</li>
 *   <li>1, the pressure orb: air_orb (a glowing compression core in a glassy ball, two shell rings and six vanes). It plays the swell clip over its life (the
 *       core grows to 2.5 times), a ring ripples out of it on the ground every half second and a pale shock ring shows the burst radius in its last 6 ticks.</li>
 * </ul>
 * Render types: the models are TRANSLUCENT (the textures are glass), glow maps ADDITIVE; rings and streaks ADDITIVE full bright.
 */
public final class AirPropPainter {
    private AirPropPainter() {}

    private static final GeoSpec BLADE = GeoSpec.of("air", "blade"), ORB = GeoSpec.of("air", "orb");
    private static ResourceLocation ring, streak;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        ring = AuraRender.tex("particle/air_ring");
        streak = AuraRender.tex("particle/air_streak");
        PropPainters.register(PropKind.AIR_1, AirPropPainter::paint);
    }

    private static void flatRing(PoseStack pose, MultiBufferSource buffers, float y, float r, int argb) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, y, 0f);
        PropDraw.quad(pose, vc, -r, 0f, r, r, 0f, r, r, 0f, -r, -r, 0f, -r, 0f, 0f, 1f, 1f, argb, PropDraw.FULL_BRIGHT);
        pose.popPose();
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int v = AirPropWord.variant(e.param());
        float s = Math.max(0.3f, e.scale());
        float left = Math.max(1f, e.maxLife()) - age;
        float a = Mth.clamp(Math.min(age / 4f, left / 4f), 0f, 1f);
        if (v == 1) {
            float swell = Mth.clamp(age / Math.max(1f, e.maxLife()), 0f, 1f);
            float rp = ((e.life() + partial) % 10f) / 10f;
            int al = Mth.clamp((int) ((1f - rp) * 0.5f * 255f), 0, 255) << 24;
            flatRing(pose, buffers, 0.05f, (0.4f + 1.4f * rp) * s, al | 0xB8F2FF);
            if (left < 6f) flatRing(pose, buffers, 0.08f, 4.5f * (1f - left / 6f), (Mth.clamp((int) (left / 6f * 0.7f * 255f), 0, 255) << 24) | 0xE6FFFF);
            pose.pushPose();
            pose.translate(0f, -0.1f, 0f);
            float sc = s * 0.7f * (1f + 1.5f * swell * 0.5f);
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.4f, 0f);
            GeoDraw.paint(pose, buffers, ORB, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, AuraRender.alpha(0xFFFFFFFF, 0.9f * a),
                    AuraRender.alpha(0xFFD0FFFF, (0.6f + 0.4f * swell) * a));
            pose.popPose();
        } else {
            boolean flying = v == 0;
            pose.pushPose();
            pose.translate(0f, 0.2f * s, 0f);
            if (flying) pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
            else pose.mulPose(Axis.XP.rotationDegrees(80f));
            float sc = s * 0.55f;
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.2f, -0.4f);
            GeoDraw.paint(pose, buffers, BLADE, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, AuraRender.alpha(0xFFFFFFFF, 0.95f * a), AuraRender.alpha(0xFFD0FFFF, a));
            pose.popPose();
            if (flying) {
                VertexConsumer sv = buffers.getBuffer(AuraRender.additive(streak));
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
                for (int k = -1; k <= 1; k += 2)
                    PropDraw.quad(pose, sv, k * 0.5f * s - 0.12f * s, 0.2f * s, -2.2f * s, k * 0.5f * s + 0.12f * s, 0.2f * s, -2.2f * s, k * 0.4f * s + 0.12f * s, 0.2f * s, -0.2f * s, k * 0.4f * s - 0.12f * s, 0.2f * s, -0.2f * s,
                            0f, 0f, 1f, 1f, (Mth.clamp((int) (0.7f * a * 255f), 0, 255) << 24) | 0xE6FFFF, PropDraw.FULL_BRIGHT);
                pose.popPose();
            }
        }
    }
}
