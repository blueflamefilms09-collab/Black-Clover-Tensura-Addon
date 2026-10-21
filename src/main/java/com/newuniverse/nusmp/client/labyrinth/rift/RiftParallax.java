package com.newuniverse.nusmp.client.labyrinth.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Why: the ordinary tear's window is the vanilla end-portal or end-gateway sky, which does not move when the viewer does. The spec asks for a true parallax view through the crack, "stencil-based
 * or multi-pass". A stencil test is NOT used (see {@link ParallaxMath}: the pipeline gives no stencil buffer a mod can count on, and shader packs replace it); this is the multi-layer mask instead.
 * Two layers of the mod's own nebula texture ({@link MegaRiftRenderer#CORE}, made by tools/gen_rift_textures.py) are drawn over the window as strips that follow the lens outline, the deeper
 * layer narrower than the one in front, so nothing they show can pass the opening; their texture coordinates slide with the viewing angle in proportion to their depth, so walking round the
 * tear makes the sky inside it move at two different rates against the rim. Drawn translucent and full bright with the entity-emissive render type, tinted by the rift's colour, darker at the
 * edge than in the middle. Called in the tear's camera-facing frame, after the window. THE BUDGET: {@link ParallaxMath#vertices}: 224 near, 112 in the middle, none far. No allocation.
 */
public final class RiftParallax {
    private RiftParallax() {}

    /**
     * Draws the layers. {@code w, h} are the lens's half width at the belly and its height, {@code y0} its bottom, {@code angle} the viewer's angle round the tear (radians), {@code camRel} the viewer's
     * height above the lens's middle, {@code t} the tick with its fraction, {@code (r, g, b)} the tint and {@code a} the overall alpha.
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, int lod, float w, float h, float y0, float angle, float camRel, float t, float r, float g, float b, float a) {
        int layers = ParallaxMath.layers(lod);
        if (layers == 0 || a <= 0.01f) return;
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(MegaRiftRenderer.CORE));
        int n = ParallaxMath.SEGMENTS;
        for (int l = 0; l < layers; l++) {
            float inset = ParallaxMath.inset(l), br = ParallaxMath.LIGHT[l], z = 0.004f * (l + 1);
            float cr = r * br, cg = g * br, cb = b * br, al = a * (1f - 0.25f * l);
            for (int i = 0; i < n; i++) {
                float v0 = i / (float) n, v1 = (i + 1) / (float) n;
                float w0 = width(w, v0) * inset, w1 = width(w, v1) * inset, ya = y0 + h * v0, yb = y0 + h * v1;
                float ua = ParallaxMath.v(l, ya, camRel, t), ub = ParallaxMath.v(l, yb, camRel, t);
                float uL0 = ParallaxMath.u(l, -w0, angle, t), uL1 = ParallaxMath.u(l, -w1, angle, t), uC = ParallaxMath.u(l, 0f, angle, t);
                float uR0 = ParallaxMath.u(l, w0, angle, t), uR1 = ParallaxMath.u(l, w1, angle, t);
                // left half: edge -> middle
                vertex(vc, last, m, -w0, ya, z, uL0, ua, cr * 0.5f, cg * 0.5f, cb * 0.5f, al * 0.6f);
                vertex(vc, last, m, 0f, ya, z, uC, ua, cr, cg, cb, al);
                vertex(vc, last, m, 0f, yb, z, uC, ub, cr, cg, cb, al);
                vertex(vc, last, m, -w1, yb, z, uL1, ub, cr * 0.5f, cg * 0.5f, cb * 0.5f, al * 0.6f);
                // right half: middle -> edge
                vertex(vc, last, m, 0f, ya, z, uC, ua, cr, cg, cb, al);
                vertex(vc, last, m, w0, ya, z, uR0, ua, cr * 0.5f, cg * 0.5f, cb * 0.5f, al * 0.6f);
                vertex(vc, last, m, w1, yb, z, uR1, ub, cr * 0.5f, cg * 0.5f, cb * 0.5f, al * 0.6f);
                vertex(vc, last, m, 0f, yb, z, uC, ub, cr, cg, cb, al);
            }
        }
    }

    /** Half width of the lens at height fraction v: the same pointed oval as the lens itself. */
    private static float width(float w, float v) { return w * (float) Math.pow(Math.sin(Math.PI * Math.min(1f, Math.max(0f, v))), 0.8); }

    private static void vertex(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
    }
}
