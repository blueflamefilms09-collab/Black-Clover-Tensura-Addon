package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Why: a {@link MultiBufferSource.BufferSource} gives out one live vertex builder at a time: asking for a second render type ends the
 * first builder, and a painter that still holds the first consumer then dies with "IllegalStateException: Not building!" on its next
 * vertex (the 1.45.0 crash report: WikiAura.circles takes a ring buffer and a glyph buffer up front and alternates between them).
 * The aura painters hold several consumers on purpose (it keeps them readable), so instead of rewriting fourteen painters this wrapper
 * hands out a consumer that asks the real source for its builder again at the start of EVERY vertex. Asking for the type that is
 * already live is a cheap lookup, and asking for another one ends the previous builder cleanly, so any interleaving is safe.
 * Only the aura layer uses it; everything else keeps the plain source.
 */
public final class LazyBuffers implements MultiBufferSource {
    private final MultiBufferSource real;

    public LazyBuffers(MultiBufferSource real) { this.real = real; }

    @Override
    public VertexConsumer getBuffer(RenderType type) { return new Lazy(real, type); }

    private static final class Lazy implements VertexConsumer {
        private final MultiBufferSource real;
        private final RenderType type;
        private VertexConsumer cur;

        Lazy(MultiBufferSource real, RenderType type) { this.real = real; this.type = type; }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            cur = real.getBuffer(type);
            return cur.addVertex(x, y, z);
        }

        private VertexConsumer live() {
            if (cur == null) cur = real.getBuffer(type);
            return cur;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return live().setColor(r, g, b, a); }
        @Override public VertexConsumer setUv(float u, float v) { return live().setUv(u, v); }
        @Override public VertexConsumer setUv1(int u, int v) { return live().setUv1(u, v); }
        @Override public VertexConsumer setUv2(int u, int v) { return live().setUv2(u, v); }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return live().setNormal(x, y, z); }
    }
}
