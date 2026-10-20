package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Why: vanilla's living-entity renderer draws the body with a constant white colour (and a fixed 15 percent alpha when it is "translucent"), so a character that fades out, is tinted by its
 * paint, or flashes gold would need its own copy of the whole render path. This wraps the buffer source the renderer is handed: every vertex it receives has its colour multiplied by an ARGB
 * (alpha included), which makes the body, the armour, the held items and the head item fade and tint together, and nothing else about the draw changes (the hurt overlay, the light and the
 * normals pass through). The render types are untouched: a fade needs a translucent render type, which the renderer picks ({@code getRenderType}) when the opacity is below 1.
 *
 * <p>1.99.0: one instance per renderer, re-aimed every frame with {@link #set} (a frame of a tinted character allocated one of these and one wrapper per buffer; now it allocates none of the
 * first: the renderer draws one entity at a time and never re-enters itself).
 */
public final class TintedBuffers implements MultiBufferSource {
    private MultiBufferSource delegate;
    private int a, r, g, b;

    /** @param argb the multiplier, 0xAARRGGBB (0xFFFFFFFF = none) */
    public TintedBuffers(MultiBufferSource delegate, int argb) { set(delegate, argb); }

    /** Aims this wrapper at a buffer source with a multiplier. Returns this. */
    public TintedBuffers set(MultiBufferSource delegate, int argb) {
        this.delegate = delegate;
        this.a = argb >>> 24;
        this.r = (argb >> 16) & 255;
        this.g = (argb >> 8) & 255;
        this.b = argb & 255;
        return this;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) { return new Tinted(delegate.getBuffer(type), r, g, b, a); }

    /** {@code (v * k) / 255} for 0..255 values. */
    static int mul(int v, int k) { return (v * k + 127) / 255; }

    /** The wrapper of one buffer; it keeps its own copy of the multiplier, so a buffer a layer still holds is not affected by the next frame's {@link #set}. */
    private static final class Tinted implements VertexConsumer {
        private final VertexConsumer vc;
        private final int r, g, b, a;

        Tinted(VertexConsumer vc, int r, int g, int b, int a) {
            this.vc = vc;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { vc.addVertex(x, y, z); return this; }

        @Override public VertexConsumer setColor(int rr, int gg, int bb, int aa) { vc.setColor(mul(rr, r), mul(gg, g), mul(bb, b), mul(aa, a)); return this; }

        @Override public VertexConsumer setUv(float u, float v) { vc.setUv(u, v); return this; }

        @Override public VertexConsumer setUv1(int u, int v) { vc.setUv1(u, v); return this; }

        @Override public VertexConsumer setUv2(int u, int v) { vc.setUv2(u, v); return this; }

        @Override public VertexConsumer setNormal(float x, float y, float z) { vc.setNormal(x, y, z); return this; }
    }
}
