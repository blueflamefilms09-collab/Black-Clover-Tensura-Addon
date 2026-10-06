package com.newuniverse.nusmp.vfx.client;

import org.joml.Vector3f;

/**
 * A camera-relative position plus an orthonormal basis (right, up, normal).
 * Used to place flat geometry (circles, rings, slashes) in 3D. Immutable.
 */
public record VfxPose(Vector3f origin, Vector3f right, Vector3f up, Vector3f normal) {

    /** Plane centered at origin whose surface faces along 'facing'. */
    public static VfxPose facing(Vector3f origin, Vector3f facing) {
        Vector3f n = new Vector3f(facing);
        if (n.lengthSquared() < 1e-6f) n.set(0, 1, 0);
        n.normalize();
        Vector3f ref = Math.abs(n.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f r = new Vector3f(ref).cross(n).normalize();
        Vector3f u = new Vector3f(n).cross(r).normalize();
        return new VfxPose(new Vector3f(origin), r, u, n);
    }

    /** Horizontal plane (ground circles). */
    public static VfxPose ground(Vector3f origin) { return facing(origin, new Vector3f(0, 1, 0)); }

    /** Same plane, spun by angle radians around its normal. */
    public VfxPose spin(float angle) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
        Vector3f r = new Vector3f(right).mul(c).add(new Vector3f(up).mul(s));
        Vector3f u = new Vector3f(up).mul(c).sub(new Vector3f(right).mul(s));
        return new VfxPose(origin, r, u, normal);
    }

    /** Pushed along the normal. */
    public VfxPose lift(float dist) { return new VfxPose(new Vector3f(normal).mul(dist).add(origin), right, up, normal); }

    /** World point at local (x, y) on the plane. */
    public Vector3f point(float x, float y) {
        return new Vector3f(origin).add(right.x * x + up.x * y, right.y * x + up.y * y, right.z * x + up.z * y);
    }
}
