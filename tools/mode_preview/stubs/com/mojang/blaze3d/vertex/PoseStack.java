package com.mojang.blaze3d.vertex;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;

/** Preview stub of PoseStack: the same matrix maths. */
public class PoseStack {
    public static final class Pose {
        public final Matrix4f pose;
        public final Matrix3f normal;
        Pose(Matrix4f p, Matrix3f n) { pose = p; normal = n; }
        public Matrix4f pose() { return pose; }
    }
    private final ArrayDeque<Pose> stack = new ArrayDeque<>();
    public PoseStack() { stack.add(new Pose(new Matrix4f(), new Matrix3f())); }
    public void pushPose() { Pose p = stack.getLast(); stack.addLast(new Pose(new Matrix4f(p.pose), new Matrix3f(p.normal))); }
    public void popPose() { stack.removeLast(); }
    public Pose last() { return stack.getLast(); }
    public void translate(float x, float y, float z) { stack.getLast().pose.translate(x, y, z); }
    public void translate(double x, double y, double z) { translate((float) x, (float) y, (float) z); }
    public void scale(float x, float y, float z) { stack.getLast().pose.scale(x, y, z); }
    public void mulPose(Quaternionf q) { stack.getLast().pose.rotate(q); stack.getLast().normal.rotate(q); }
}
