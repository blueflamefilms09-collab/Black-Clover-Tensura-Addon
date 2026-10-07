package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Preview stub of MultiBufferSource: getBuffer(renderType) hands out a vertex consumer for that render type. As in the game, asking for
 * a different render type ends the previous one (it is drawn right then), and writing to an ended consumer throws.
 */
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType renderType);
}
