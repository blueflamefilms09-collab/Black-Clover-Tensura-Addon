package com.mojang.blaze3d.vertex;

/** Preview stub: records vertices. */
public interface VertexConsumer {
    VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z);
    VertexConsumer setColor(int r, int g, int b, int a);
    VertexConsumer setUv(float u, float v);
    VertexConsumer setOverlay(int o);
    VertexConsumer setLight(int l);
    VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z);
}
