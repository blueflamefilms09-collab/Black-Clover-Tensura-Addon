package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.newuniverse.nusmp.vfx.WaterBc;
import com.newuniverse.nusmp.vfx.WaterBcMeshBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws the mesh quads of the water hybrid ({@link WaterBcMeshBuffer#FRAME}, filled by {@code WaterBcLayer} while the effects were drawn) once per frame, in three passes and at most three draw calls:
 * the solid quads with the depth buffer written (the cores occlude each other and the glass behind them), the translucent hulls blended without writing depth, the glow quads additive. It runs just BEFORE
 * the sprite buffer's flush, so the glass sprites (depth tested, never written) are hidden by the cores that are in front of them and cover the ones behind. The vertices are camera relative and are moved
 * to view space on the CPU with the same matrix the sprite buffer uses (the model-view uniform is identity then, see {@link VfxManager#render}). The state of every pass is set in full, culling is off (the
 * pieces are written two sided and shaded two sided), and the state the sprite buffer expects is restored. Render thread only; nothing is allocated (the scratch vector is static, the arrays are the
 * buffer's own).
 */
public final class WaterBcDraw {
    private WaterBcDraw() {}

    private static final ResourceLocation SOLID = VfxTextures.byName(WaterBc.fileName(WaterBc.SOLID, false));
    private static final Vector3f P = new Vector3f();

    /** Draws and empties the frame's mesh quads. */
    public static void flush(Matrix4f modelView) {
        WaterBcMeshBuffer buf = WaterBcMeshBuffer.FRAME;
        if (buf.total() == 0) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, SOLID);
        if (buf.count(WaterBcMeshBuffer.SOLID) > 0) {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            pass(buf, WaterBcMeshBuffer.SOLID, modelView);
        }
        if (buf.count(WaterBcMeshBuffer.TRANSLUCENT) > 0) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            pass(buf, WaterBcMeshBuffer.TRANSLUCENT, modelView);
        }
        if (buf.count(WaterBcMeshBuffer.GLOW) > 0) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            pass(buf, WaterBcMeshBuffer.GLOW, modelView);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        buf.reset();
    }

    private static void pass(WaterBcMeshBuffer buf, int pass, Matrix4f modelView) {
        float[] d = buf.data(pass);
        int[] colour = buf.colours(pass);
        int n = buf.count(pass);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int q = 0; q < n; q++) {
            int c = colour[q], r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255, a = c >>> 24;
            for (int k = 0; k < 4; k++) {
                int i = q * WaterBcMeshBuffer.STRIDE + k * 3;
                modelView.transformPosition(d[i], d[i + 1], d[i + 2], P);
                bb.addVertex(P.x, P.y, P.z).setUv(0.5f, 0.5f).setColor(r, g, b, a);
            }
        }
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
