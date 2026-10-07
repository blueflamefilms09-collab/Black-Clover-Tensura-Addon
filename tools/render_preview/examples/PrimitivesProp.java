package examples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Self-test of the render preview: every PropDraw primitive in a row (box, boxUv, sphere, ellipsoid, open cylinder, capped cylinder, cone,
 * frustum, torus, crystal, tube, quad), each drawn three times: a cutout pass with a UV test texture, a translucent shell 12 % bigger and an
 * additive shell 25 % bigger. Registered under PropKind.BRONZE_1 (the real Bronze painter is a stub). Written the way a painter should be:
 * one buffer per render type, all primitives of a pass drawn before the next render type is requested.
 */
public final class PrimitivesProp {
    private PrimitivesProp() {}

    private interface Prim { void draw(PoseStack p, VertexConsumer vc, int argb, int light); }

    private static final Prim[] PRIMS = {
            (p, vc, c, l) -> PropDraw.box(p, vc, -0.32f, -0.32f, -0.32f, 0.32f, 0.32f, 0.32f, c, l),
            (p, vc, c, l) -> PropDraw.boxUv(p, vc, -0.3f, -0.4f, -0.2f, 0.3f, 0.4f, 0.2f, 0f, 0f, 0.5f, 0.5f, c, l),
            (p, vc, c, l) -> PropDraw.sphere(p, vc, 0f, 0f, 0f, 0.38f, 7, 10, c, l),
            (p, vc, c, l) -> PropDraw.ellipsoid(p, vc, 0f, 0f, 0f, 0.24f, 0.42f, 0.3f, 7, 10, c, l),
            (p, vc, c, l) -> PropDraw.cylinder(p, vc, 0f, -0.4f, 0f, 0f, 0.4f, 0f, 0.26f, 0.26f, 12, false, c, l),
            (p, vc, c, l) -> PropDraw.cylinder(p, vc, 0f, -0.4f, 0f, 0f, 0.4f, 0f, 0.26f, 0.26f, 12, true, c, l),
            (p, vc, c, l) -> PropDraw.cylinder(p, vc, 0f, -0.4f, 0f, 0f, 0.4f, 0f, 0.34f, 0f, 12, true, c, l),
            (p, vc, c, l) -> PropDraw.cylinder(p, vc, 0f, -0.4f, 0f, 0f, 0.4f, 0f, 0.38f, 0.14f, 10, true, c, l),
            (p, vc, c, l) -> PropDraw.torus(p, vc, 0f, 0f, 0f, 0f, 1f, 0f, 0.3f, 0.1f, 16, 8, c, l),
            (p, vc, c, l) -> PropDraw.crystal(p, vc, 0f, -0.42f, 0f, 0.17f, 0.45f, 0.3f, c, l),
            (p, vc, c, l) -> {
                float[][] pts = new float[9][];
                for (int i = 0; i < 9; i++) pts[i] = new float[]{0.26f * Mth.cos(i * 0.9f), -0.4f + i * 0.1f, 0.26f * Mth.sin(i * 0.9f)};
                PropDraw.tube(p, vc, pts, 0.09f, 0.03f, 6, c, l);
            },
            (p, vc, c, l) -> PropDraw.quad(p, vc, -0.4f, -0.4f, 0f, 0.4f, -0.4f, 0f, 0.4f, 0.4f, 0f, -0.4f, 0.4f, 0f, 0f, 0f, 1f, 1f, c, l),
    };

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            float s = e.scale();
            // pass 1: solid with holes; pass 2: glass; pass 3: additive glow
            pass(pose, buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker"))), s, age, 1.0f, 0xFFFFFFFF, light);
            pass(pose, buffers.getBuffer(AuraRender.translucent(AuraRender.tex("prop/preview_glass"))), s, age, 1.12f, AuraRender.alpha(0xFF90D0FF, 0.75f), light);
            pass(pose, buffers.getBuffer(AuraRender.additive(AuraRender.tex("prop/preview_glow"))), s, age, 1.25f, AuraRender.alpha(0xFFFFA030, 0.55f), PropDraw.FULL_BRIGHT);
        });
    }

    private static void pass(PoseStack pose, VertexConsumer vc, float scale, float age, float grow, int argb, int light) {
        for (int i = 0; i < PRIMS.length; i++) {
            pose.pushPose();
            pose.translate((i - (PRIMS.length - 1) / 2f) * 1.05f * scale, 0.55f * scale + 0.05f * Mth.sin(age * 0.12f + i), 0f);
            pose.mulPose(Axis.YP.rotationDegrees(age * 2.5f + i * 25f));
            pose.mulPose(Axis.XP.rotationDegrees(i == 8 ? 70f : 0f));       // the torus stands up so its ring is seen
            pose.scale(scale * grow, scale * grow, scale * grow);
            PRIMS[i].draw(pose, vc, argb, light);
            pose.popPose();
        }
    }
}
