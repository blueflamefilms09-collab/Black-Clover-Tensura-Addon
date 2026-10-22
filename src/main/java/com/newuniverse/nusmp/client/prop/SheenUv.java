package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Why: the iridescent sheen of the Sylph's wings (docs/magic/sylph.md) is ONE sheet that holds several phases of a pastel rainbow gradient stacked top to bottom (each phase is the layout of the
 * wing sheet, masked by the wing's cut-out, black under every transparent texel). The wing model's own uv run 0..1 over one phase; this wrapper hands every vertex to the real consumer with its V
 * moved into phase {@code k} of {@code n} ({@code (v + k) / n}), so showing the next phase is a different number, not a different model, and the sheen sweeps over the membrane as the phases follow
 * each other. One instance, reused: render thread only, no allocation.
 */
final class SheenUv implements VertexConsumer {
    static final SheenUv INSTANCE = new SheenUv();

    private VertexConsumer to;
    private float k, inv;

    private SheenUv() {}

    /** The wrapper over {@code real} showing phase {@code k} of {@code n} (valid until the next call). */
    static VertexConsumer over(VertexConsumer real, int k, int n) {
        INSTANCE.to = real;
        INSTANCE.k = k;
        INSTANCE.inv = 1f / n;
        return INSTANCE;
    }

    /** The V coordinate in the stacked sheet of a V of one phase (pure, for the tests). */
    static float v(float v, int k, int n) { return (v + k) / n; }

    @Override public VertexConsumer addVertex(float x, float y, float z) { to.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { to.setColor(red, green, blue, alpha); return this; }
    @Override public VertexConsumer setUv(float u, float v) { to.setUv(u, (v + k) * inv); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { to.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { to.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float normalX, float normalY, float normalZ) { to.setNormal(normalX, normalY, normalZ); return this; }
}
