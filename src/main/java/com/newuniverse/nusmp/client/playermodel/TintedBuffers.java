package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Why: vanilla's living-entity renderer draws the body with a constant white colour (and a fixed 15 percent alpha when it is "translucent"), so a character that fades out, is tinted by its
 * paint, or flashes gold would need its own copy of the whole render path. This wraps the buffer source the renderer is handed: every vertex it receives has its colour multiplied by an ARGB
 * (alpha included), which makes the body, the armour, the held items and the head item fade and tint together, and nothing else about the draw changes (the hurt overlay, the light and the
 * normals pass through). The render types are untouched: a fade needs a translucent render type, which the renderer picks ({@code getRenderType}) when the opacity is below 1.
 */
public final class TintedBuffers implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final int a, r, g, b;

    /** @param argb the multiplier, 0xAARRGGBB (0xFFFFFFFF = none) */
    public TintedBuffers(MultiBufferSource delegate, int argb) {
        this.delegate = delegate;
        this.a = argb >>> 24;
        this.r = (argb >> 16) & 255;
        this.g = (argb >> 8) & 255;
        this.b = argb & 255;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) { return new Tinted(delegate.getBuffer(type)); }

    /** {@code (v * k) / 255} for 0..255 values. */
    static int mul(int v, int k) { return (v * k + 127) / 255; }

    private final class Tinted implements VertexConsumer {
        private final VertexConsumer vc;

        Tinted(VertexConsumer vc) { this.vc = vc; }

        @Override public VertexConsumer addVertex(float x, float y, float z) { vc.addVertex(x, y, z); return this; }

        @Override public VertexConsumer setColor(int rr, int gg, int bb, int aa) { vc.setColor(mul(rr, r), mul(gg, g), mul(bb, b), mul(aa, a)); return this; }

        @Override public VertexConsumer setUv(float u, float v) { vc.setUv(u, v); return this; }

        @Override public VertexConsumer setUv1(int u, int v) { vc.setUv1(u, v); return this; }

        @Override public VertexConsumer setUv2(int u, int v) { vc.setUv2(u, v); return this; }

        @Override public VertexConsumer setNormal(float x, float y, float z) { vc.setNormal(x, y, z); return this; }
    }
}
