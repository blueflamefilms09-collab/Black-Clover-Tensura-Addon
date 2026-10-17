package com.mojang.blaze3d.vertex;

import org.joml.Matrix4f;

/** Why: harness shim. */
public interface VertexConsumer {
    VertexConsumer addVertex(Matrix4f pose, float x, float y, float z);
    VertexConsumer setColor(int argb);
}
