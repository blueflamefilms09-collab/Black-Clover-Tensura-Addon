package examples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Self-test of the preview's checks: a painter that does one wrong thing per e.param(), registered under PropKind.BRONZE_2:
 * 0 bad geometry (NaN, zero-area quad, missing texture, normal of length 3, a quad wound against its normal, 3 stray vertices),
 * 1 unbalanced pushPose, 2 a vertex without its normal, 3 a consumer used after the next getBuffer, 4 too many vertices,
 * 5 e.owner() on the client, 6 everything transparent.
 */
public final class ErrorProps {
    private ErrorProps() {}

    public static void register() {
        PropPainters.register(PropKind.BRONZE_2, (e, partial, age, pose, buffers, light) -> {
            ResourceLocation glow = AuraRender.tex("prop/preview_glow");
            switch (e.param()) {
                case 0 -> {
                    VertexConsumer vc = buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/does_not_exist")));
                    PropDraw.box(pose, vc, -0.5f, 0f, -0.5f, 0.5f, 1f, 0.5f, 0xFFFFFFFF, light);                    // fine, but the texture is missing
                    vertex(pose, vc, Float.NaN, 0.2f, 0f, 0f, 1f, 0f, light);                                       // a NaN corner (and 3 more to make a quad)
                    vertex(pose, vc, 0.1f, 0.2f, 0f, 0f, 1f, 0f, light);
                    vertex(pose, vc, 0.1f, 0.3f, 0f, 0f, 1f, 0f, light);
                    vertex(pose, vc, 0.0f, 0.3f, 0f, 0f, 1f, 0f, light);
                    for (int i = 0; i < 4; i++) vertex(pose, vc, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, light);             // zero area: four equal corners
                    for (int i = 0; i < 4; i++) vertex(pose, vc, i < 2 ? 0.3f : 0.6f, 1.2f + (i == 1 || i == 2 ? 0.3f : 0f), i == 1 || i == 2 ? 0.3f : 0f, 0f, 3f, 0f, light);   // normal of length 3
                    // clockwise seen from above but its normal says up: wound against its normal
                    vertex(pose, vc, -0.4f, 1.6f, -0.4f, 0f, 1f, 0f, light);
                    vertex(pose, vc, 0.4f, 1.6f, -0.4f, 0f, 1f, 0f, light);
                    vertex(pose, vc, 0.4f, 1.6f, 0.4f, 0f, 1f, 0f, light);
                    vertex(pose, vc, -0.4f, 1.6f, 0.4f, 0f, 1f, 0f, light);
                    for (int i = 0; i < 3; i++) vertex(pose, vc, i * 0.1f, 2f, 0f, 0f, 1f, 0f, light);             // 3 stray vertices: not a quad
                }
                case 1 -> {
                    pose.pushPose();                                                                                // never popped
                    PropDraw.box(pose, buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker"))), -0.4f, 0f, -0.4f, 0.4f, 0.8f, 0.4f, 0xFFFFFFFF, light);
                }
                case 2 -> {
                    VertexConsumer vc = buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker")));
                    for (int i = 0; i < 4; i++) vc.addVertex(pose.last(), i < 2 ? -0.3f : 0.3f, i == 1 || i == 2 ? 0.6f : 0f, 0f).setColor(255, 255, 255, 255).setUv(i < 2 ? 0f : 1f, i == 1 || i == 2 ? 0f : 1f).setLight(light);
                    vc.addVertex(pose.last(), 0f, 0f, 0f);                                                          // the vertex above never got overlay and normal
                }
                case 3 -> {
                    VertexConsumer a = buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker")));
                    VertexConsumer b = buffers.getBuffer(AuraRender.additive(glow));
                    PropDraw.box(pose, b, -0.3f, 0f, -0.3f, 0.3f, 0.6f, 0.3f, 0xFFFFFFFF, light);
                    PropDraw.box(pose, a, -0.2f, 0f, -0.2f, 0.2f, 0.4f, 0.2f, 0xFFFFFFFF, light);                  // a was ended by the getBuffer above: throws
                }
                case 4 -> {
                    VertexConsumer vc = buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker")));
                    for (int i = 0; i < 20; i++) PropDraw.sphere(pose, vc, (i % 5) * 0.3f, 0.3f + (i / 5) * 0.3f, 0f, 0.14f, 8, 12, 0xFFFFFFFF, light);
                }
                case 5 -> {
                    e.owner();                                                                                      // always null on the client
                    PropDraw.box(pose, buffers.getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker"))), -0.4f, 0f, -0.4f, 0.4f, 0.8f, 0.4f, 0xFFFFFFFF, light);
                }
                default -> PropDraw.box(pose, buffers.getBuffer(AuraRender.translucent(glow)), -0.4f, 0f, -0.4f, 0.4f, 0.8f, 0.4f, 0x00FFFFFF, light);
            }
        });
    }

    private static void vertex(PoseStack pose, VertexConsumer vc, float x, float y, float z, float nx, float ny, float nz, int light) {
        vc.addVertex(pose.last(), x, y, z).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(0).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}
