package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.53: the render types an aura should use, and the one thing every body overlay does (draw the posed player model again).
 * <ul>
 *   <li>{@link #additive}: full-bright glow added to the screen, for spectral light (RenderType.eyes: no depth write, so it never
 *       z-fights with the skin).</li>
 *   <li>{@link #translucent}: lit, alpha-blended, depth-sorted over the skin: barriers, bubbles, gel, glass, ghostly engulfing shells.</li>
 *   <li>{@link #emissive}: translucent but full-bright.</li>
 *   <li>{@link #cutout}: opaque with alpha holes, both faces: swollen muscle, bone plates, crystal armour, metal plating.</li>
 * </ul>
 * Layers that grow the body (muscle, plating) should be drawn scaled a few percent up with {@link #model} so they sit just over the skin.
 */
public final class AuraRender {
    private AuraRender() {}

    public static ResourceLocation tex(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/" + path + ".png"); }

    public static RenderType additive(ResourceLocation t) { return RenderType.eyes(t); }
    public static RenderType translucent(ResourceLocation t) { return RenderType.entityTranslucent(t); }
    public static RenderType emissive(ResourceLocation t) { return RenderType.entityTranslucentEmissive(t); }
    public static RenderType cutout(ResourceLocation t) { return RenderType.entityCutoutNoCull(t); }

    /** ARGB with the alpha scaled by k (0..1). */
    public static int alpha(int argb, float k) { return (Math.max(0, Math.min(255, (int) ((argb >>> 24) * k))) << 24) | (argb & 0xFFFFFF); }

    /**
     * Draws the player's posed model again with {@code rt}, scaled about the middle of the body (sx, sy, sz of 1 = the same size;
     * 1.08 = 8% bigger). 'texture' is laid out like a 64x64 player skin.
     */
    public static void model(AuraContext c, RenderType rt, float sx, float sy, float sz, int argb) {
        c.pose().pushPose();
        c.pose().translate(0f, 0.75f, 0f);
        c.pose().scale(sx, sy, sz);
        c.pose().translate(0f, -0.75f, 0f);
        VertexConsumer vc = c.buffers().getBuffer(rt);
        c.model().renderToBuffer(c.pose(), vc, c.light(), OverlayTexture.NO_OVERLAY, argb);
        c.pose().popPose();
    }

    /** The same, with a full-bright light (for additive / emissive passes). */
    public static void modelBright(AuraContext c, RenderType rt, float sx, float sy, float sz, int argb) {
        c.pose().pushPose();
        c.pose().translate(0f, 0.75f, 0f);
        c.pose().scale(sx, sy, sz);
        c.pose().translate(0f, -0.75f, 0f);
        VertexConsumer vc = c.buffers().getBuffer(rt);
        c.model().renderToBuffer(c.pose(), vc, 0xF000F0, OverlayTexture.NO_OVERLAY, argb);
        c.pose().popPose();
    }
}
