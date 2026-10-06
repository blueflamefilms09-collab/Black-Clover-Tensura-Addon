package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.SpiritLordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * The Spirit Lord companion drawn as a small floating orb in its spirit's colour (for now, instead of a full body): a soft halo,
 * the glowing orb with a bright heart, and three motes circling it, bobbing gently about a block above the ground.
 * The full-body GeckoLib renderer (SpiritLordRenderer) is kept and can be registered again later.
 */
public class SpiritOrbRenderer extends EntityRenderer<SpiritLordEntity> {
    private static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/spirit_orb.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");
    private static final float CORE = 0.45f, HALO = 1.15f, MOTE = 0.12f;

    public SpiritOrbRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.2f;
        this.shadowStrength = 0.4f;
    }

    @Override
    public void render(SpiritLordEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        int col = e.kind().color;
        float r = ((col >> 16) & 255) / 255f, g = ((col >> 8) & 255) / 255f, b = (col & 255) / 255f;
        boolean dark = r + g + b < 0.5f;                       // the black devil: a dark orb with a red rim
        float pulse = 1f + 0.06f * Mth.sin(age * 0.15f);

        pose.pushPose();
        pose.translate(0, 1.0 + 0.12 * Mth.sin(age * 0.08f), 0);
        // motes circling the orb (in world space, before facing the camera)
        for (int i = 0; i < 3; i++) {
            float a = age * 0.09f + i * Mth.TWO_PI / 3;
            pose.pushPose();
            pose.translate(Mth.cos(a) * 0.42, 0.12 * Mth.sin(age * 0.11f + i * 2), Mth.sin(a) * 0.42);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)), MOTE, r, g, b, 0.9f);
            pose.popPose();
        }
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)), HALO * pulse, dark ? 0.85f : r, dark ? 0.1f : g, dark ? 0.15f : b, 0.55f);
        quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(ORB)), CORE * pulse, dark ? 0.12f : r, dark ? 0.08f : g, dark ? 0.12f : b, 1f);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);       // name tag
    }

    /** A camera-facing square of half-size {@code s}, full bright. */
    private static void quad(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b, float a) {
        Matrix4f m = pose.last().pose();
        PoseStack.Pose last = pose.last();
        int light = 0xF000F0;
        vc.addVertex(m, -s, -s, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, -s, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, s, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, -s, s, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritLordEntity e) { return ORB; }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.SPIRIT_LORD.get(), SpiritOrbRenderer::new);
    }
}
