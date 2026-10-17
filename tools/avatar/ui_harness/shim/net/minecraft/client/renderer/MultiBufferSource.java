package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Why: harness shim. */
public interface MultiBufferSource {
    class BufferSource implements MultiBufferSource {
        private final VertexConsumer consumer;
        public BufferSource(VertexConsumer c) { consumer = c; }
        public VertexConsumer getBuffer(RenderType t) { return consumer; }
    }
}
