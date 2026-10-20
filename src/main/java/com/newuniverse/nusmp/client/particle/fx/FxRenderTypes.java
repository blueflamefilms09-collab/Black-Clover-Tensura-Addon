package com.newuniverse.nusmp.client.particle.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Why: the glow particles (embers, motes, rings, runes) look right only when their light ADDS to what is behind them, which the vanilla particle sheets (alpha-blended) cannot do. This is the
 * vanilla translucent particle sheet with the blend function changed to source-alpha-plus-one and the depth write off (so overlapping glows do not cut each other out). The engine renders
 * custom types after the built-in ones and resets the blend state itself at the end of the particle pass; every custom type sets what it needs in {@code begin}, so no state leaks between
 * types.
 */
public final class FxRenderTypes {
    private FxRenderTypes() {}

    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "nusmp:fx_additive";
        }
    };
}
