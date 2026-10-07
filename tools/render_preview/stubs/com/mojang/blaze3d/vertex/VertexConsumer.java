package com.mojang.blaze3d.vertex;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Preview stub of Minecraft 1.21.1's VertexConsumer: the same abstract elements and the same default-method chain (addVertex with a pose
 * or a matrix transforms the position, setNormal with a pose transforms the normal, setLight / setOverlay split the packed int into two
 * shorts, setColor(int) is ARGB). The preview records vertices by implementing the six abstract methods.
 */
public interface VertexConsumer {
    VertexConsumer addVertex(float x, float y, float z);

    VertexConsumer setColor(int red, int green, int blue, int alpha);

    VertexConsumer setUv(float u, float v);

    VertexConsumer setUv1(int u, int v);

    VertexConsumer setUv2(int u, int v);

    VertexConsumer setNormal(float normalX, float normalY, float normalZ);

    default void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ) {
        this.addVertex(x, y, z);
        this.setColor(color);
        this.setUv(u, v);
        this.setOverlay(packedOverlay);
        this.setLight(packedLight);
        this.setNormal(normalX, normalY, normalZ);
    }

    default VertexConsumer setColor(float red, float green, float blue, float alpha) {
        return this.setColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    default VertexConsumer setColor(int color) {
        return this.setColor(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24);
    }

    default VertexConsumer setWhiteAlpha(int alpha) {
        return this.setColor((alpha << 24) | 0xFFFFFF);
    }

    default VertexConsumer setLight(int packedLight) {
        return this.setUv2(packedLight & 65535, packedLight >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int packedOverlay) {
        return this.setUv1(packedOverlay & 65535, packedOverlay >> 16 & 65535);
    }

    default VertexConsumer addVertex(Vector3f vector) {
        return this.addVertex(vector.x(), vector.y(), vector.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, Vector3f vector) {
        return this.addVertex(pose, vector.x(), vector.y(), vector.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.addVertex(pose.pose(), x, y, z);
    }

    default VertexConsumer addVertex(Matrix4f pose, float x, float y, float z) {
        Vector3f v = pose.transformPosition(x, y, z, new Vector3f());
        return this.addVertex(v.x(), v.y(), v.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, float normalX, float normalY, float normalZ) {
        Vector3f v = pose.transformNormal(normalX, normalY, normalZ, new Vector3f());
        return this.setNormal(v.x(), v.y(), v.z());
    }
}
