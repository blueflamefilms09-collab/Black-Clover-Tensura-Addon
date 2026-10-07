package com.mojang.blaze3d.vertex;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Preview stub of Minecraft 1.21.1's PoseStack (Mojang mappings): the real matrix stack with the real maths (translate, scale with the
 * normal-matrix bookkeeping, mulPose, push / pop). popPose() on the root throws like the game's.
 */
public class PoseStack {
    private final Deque<Pose> poseStack = new ArrayDeque<>();

    public PoseStack() {
        poseStack.add(new Pose(new Matrix4f(), new Matrix3f()));
    }

    public void translate(double x, double y, double z) { translate((float) x, (float) y, (float) z); }

    public void translate(float x, float y, float z) { poseStack.getLast().pose.translate(x, y, z); }

    public void scale(float x, float y, float z) {
        Pose pose = poseStack.getLast();
        pose.pose.scale(x, y, z);
        if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
            if (x < 0.0F || y < 0.0F || z < 0.0F) pose.normal.scale(Math.signum(x), Math.signum(y), Math.signum(z));
        } else {
            pose.normal.scale(1.0F / x, 1.0F / y, 1.0F / z);
            pose.trustedNormals = false;
        }
    }

    public void mulPose(Quaternionf quaternion) {
        Pose pose = poseStack.getLast();
        pose.pose.rotate(quaternion);
        pose.normal.rotate(quaternion);
    }

    public void rotateAround(Quaternionf quaternion, float x, float y, float z) {
        Pose pose = poseStack.getLast();
        pose.pose.rotateAround(quaternion, x, y, z);
        pose.normal.rotate(quaternion);
    }

    public void pushPose() { poseStack.addLast(new Pose(poseStack.getLast())); }

    public void popPose() { poseStack.removeLast(); }

    public Pose last() { return poseStack.getLast(); }

    /** True when only the root pose is left (1.21.1 calls this {@code clear()}). */
    public boolean clear() { return poseStack.size() == 1; }

    public void setIdentity() {
        Pose pose = poseStack.getLast();
        pose.pose.identity();
        pose.normal.identity();
    }

    public void mulPose(Matrix4f matrix) { poseStack.getLast().pose.mul(matrix); }

    /** Preview helper (not in the game): how many poses are on the stack (1 = balanced). */
    public int previewDepth() { return poseStack.size(); }

    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;
        private boolean trustedNormals = true;

        Pose(Matrix4f pose, Matrix3f normal) {
            this.pose = pose;
            this.normal = normal;
        }

        Pose(Pose other) {
            this.pose = new Matrix4f(other.pose);
            this.normal = new Matrix3f(other.normal);
            this.trustedNormals = other.trustedNormals;
        }

        public Matrix4f pose() { return pose; }

        public Matrix3f normal() { return normal; }

        public Vector3f transformNormal(Vector3f vector, Vector3f destination) { return transformNormal(vector.x, vector.y, vector.z, destination); }

        public Vector3f transformNormal(float x, float y, float z, Vector3f destination) {
            Vector3f v = normal.transform(x, y, z, destination);
            return trustedNormals ? v : v.normalize();
        }

        public Pose copy() { return new Pose(this); }
    }
}
