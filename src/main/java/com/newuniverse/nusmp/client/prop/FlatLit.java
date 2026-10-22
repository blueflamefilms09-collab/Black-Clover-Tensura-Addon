package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Why: the Sylph is a spirit of light drawn in an anime cel look, and the cel shading is painted into her sheets. A lit entity render type multiplies every face by the game's two-light
 * diffuse term (a face that looks to the front gets about 0.74, a side face about 0.5), which turns a near white skin grey and flattens the painted shading twice. This wrapper hands every
 * vertex to the real consumer with an upward world normal, so the diffuse term is 1 for every face and the painted colours show as painted; the lightmap (the block and sky light) still
 * applies. One instance, reused: render thread only, no allocation.
 */
final class FlatLit implements VertexConsumer {
    static final FlatLit INSTANCE = new FlatLit();

    private VertexConsumer to;

    private FlatLit() {}

    /** The wrapper over {@code real} (valid until the next call). */
    static VertexConsumer over(VertexConsumer real) {
        INSTANCE.to = real;
        return INSTANCE;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { to.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { to.setColor(red, green, blue, alpha); return this; }
    @Override public VertexConsumer setUv(float u, float v) { to.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { to.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { to.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float normalX, float normalY, float normalZ) { to.setNormal(0f, 1f, 0f); return this; }
}
