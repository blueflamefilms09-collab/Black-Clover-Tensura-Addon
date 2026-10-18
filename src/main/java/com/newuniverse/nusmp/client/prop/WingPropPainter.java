package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.AirPropWord;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Wing Magic: how its summons look (WING_1, variant in the low byte of param, see prop.AirPropWord).
 * <ul>
 *   <li>0, the feather hawk: wing_hawk (a bird of silver and cream feathers, gold beak, glowing eyes, fan tail). Idle: the wing beat; in a dive (synced state 2)
 *       the dive clip folds the wings back while the entity's pitch tilts the body down at its target.</li>
 *   <li>1, the feather blade: wing_blade (a long feather with a gold core and tip), spinning about its length and bobbing; its pitch points it at the target.</li>
 * </ul>
 * Render types: models CUTOUT first, TRANSLUCENT for the first and last 6 ticks so they fade, glow maps ADDITIVE (GeoDraw). The prop's feet are the middle of
 * its body (it flies), so the pose is lifted by half its height.
 */
public final class WingPropPainter {
    private WingPropPainter() {}

    private static final GeoSpec HAWK = GeoSpec.of("wing", "hawk"), BLADE = GeoSpec.of("wing", "blade");

    /** Called once on the client by PropPainterRegistry. */
    public static void register() { PropPainters.register(PropKind.WING_1, WingPropPainter::paint); }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param(), v = AirPropWord.variant(p);
        float s = Math.max(0.3f, e.scale());
        float left = Math.max(1f, e.maxLife()) - age;
        float a = Mth.clamp(Math.min(age / 6f, left / 6f), 0f, 1f);
        boolean solid = a >= 0.99f;
        pose.pushPose();
        pose.translate(0f, 0.35f * s, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
        if (v == 0) {
            boolean dive = AirPropWord.state(p) == AirPropWord.ACT2;
            float sc = s * 0.62f;
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.5f, 0f);
            GeoDraw.paint(pose, buffers, HAWK, dive ? "dive" : "idle", dive ? Math.min(0.6f, (AirPropWord.since(p, e.level().getGameTime()) + partial) / 20f) : age / 20f, GeoDraw.Space.PROP,
                    solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light, solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, a), AuraRender.alpha(0xFFFFEAB0, 0.8f * a));
        } else {
            float sc = s * 0.5f;
            pose.mulPose(Axis.XP.rotationDegrees(-90f));                     // lie along the way it flies (the model stands up along y)
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.7f, 0f);
            GeoDraw.paint(pose, buffers, BLADE, "idle", age / 20f, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                    solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, a), AuraRender.alpha(0xFFFFEAB0, a));
        }
        pose.popPose();
    }
}
