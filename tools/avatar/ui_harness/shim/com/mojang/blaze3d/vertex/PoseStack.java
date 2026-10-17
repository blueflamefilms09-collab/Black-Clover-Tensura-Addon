package com.mojang.blaze3d.vertex;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;

/** Why: harness shim: a stack of matrices. */
public class PoseStack {
    public static final class Pose {
        private final Matrix4f m;
        Pose(Matrix4f m) { this.m = m; }
        public Matrix4f pose() { return m; }
    }
    private final Deque<Pose> stack = new ArrayDeque<>();
    public PoseStack() { stack.push(new Pose(new Matrix4f())); }
    public Pose last() { return stack.peek(); }
    public void pushPose() { stack.push(new Pose(new Matrix4f(stack.peek().m))); }
    public void popPose() { stack.pop(); }
    public void translate(float x, float y, float z) { stack.peek().m.translate(x, y, z); }
    public void translate(double x, double y, double z) { stack.peek().m.translate((float) x, (float) y, (float) z); }
    public void scale(float x, float y, float z) { stack.peek().m.scale(x, y, z); }
    public void mulPose(Quaternionf q) { stack.peek().m.rotate(q); }
    public boolean clear() { return stack.size() == 1; }
}
