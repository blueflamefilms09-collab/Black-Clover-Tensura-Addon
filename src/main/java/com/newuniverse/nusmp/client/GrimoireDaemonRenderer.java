package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.entity.GrimoireDaemonEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.52: Zagred's Grimoire Daemons (lesser, greater, arch) and the Overwrite rule stones, drawn with {@link GrimoireDaemonModel}
 * and one skin per tier (tools/gen_daemon_textures.py), plus a full-bright glow pass for the eyes, the leaf emblems on the
 * grimoire cover, its glowing text and the rune trim.
 */
public class GrimoireDaemonRenderer extends MobRenderer<GrimoireDaemonEntity, GrimoireDaemonModel> {
    private static ResourceLocation tex(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + name + ".png"); }

    static final ResourceLocation[] SKIN = {tex("grimoire_daemon_lesser"), tex("grimoire_daemon_greater"), tex("grimoire_daemon_arch"), tex("grimoire_daemon_arch")};
    static final ResourceLocation[] GLOW = {tex("grimoire_daemon_lesser_glow"), tex("grimoire_daemon_greater_glow"), tex("grimoire_daemon_arch_glow"), tex("grimoire_daemon_arch_glow")};

    public GrimoireDaemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GrimoireDaemonModel(ctx.bakeLayer(GrimoireDaemonModel.LAYER)), 0.5f);
        addLayer(new Glow(this));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.GRIMOIRE_DAEMON.get(), GrimoireDaemonRenderer::new);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(GrimoireDaemonModel.LAYER, GrimoireDaemonModel::createLayer);
    }

    private static int tierOf(GrimoireDaemonEntity e) { return Math.max(0, Math.min(3, e.tier())); }

    @Override
    public ResourceLocation getTextureLocation(GrimoireDaemonEntity e) { return SKIN[tierOf(e)]; }

    /** The model is 1/16 scale; tiers differ in size. */
    @Override
    protected void scale(GrimoireDaemonEntity e, PoseStack pose, float partial) {
        float s = switch (e.tier()) { case GrimoireDaemonEntity.GREATER -> 1.15f; case GrimoireDaemonEntity.ARCH -> 1.7f; case GrimoireDaemonEntity.STONE -> 1.0f; default -> 0.85f; };
        pose.scale(s, s, s);
        pose.translate(0f, -0.3f, 0f);
    }

    @Override
    protected boolean shouldShowName(GrimoireDaemonEntity e) { return false; }

    /** Eyes, emblem, text and trim, full-bright and pulsing. */
    static final class Glow extends RenderLayer<GrimoireDaemonEntity, GrimoireDaemonModel> {
        Glow(GrimoireDaemonRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, GrimoireDaemonEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float pulse = (e.castLeft() > 0 ? 0.9f : 0.6f) + 0.3f * Mth.sin(age * 0.12f);
            int v = (int) (255 * Math.min(1f, pulse));
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW[tierOf(e)])), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }
}
