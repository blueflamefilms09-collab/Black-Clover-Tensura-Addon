package net.minecraft.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Preview stub of Camera: the position and orientation of the preview view being recorded (each view row has its own camera). */
public class Camera {
    /** Preview only: how often a painter reached for the camera (then the preview records every view with its own camera). */
    public static int previewUses;
    private Vec3 position = new Vec3(0, 2, 6);
    private final Vector3f forward = new Vector3f(0, 0, -1), up = new Vector3f(0, 1, 0), left = new Vector3f(-1, 0, 0);
    private final Quaternionf rotation = new Quaternionf();
    private float xRot, yRot;
    private Entity entity;

    public Vec3 getPosition() { return position; }
    public float getXRot() { return xRot; }
    public float getYRot() { return yRot; }
    /** The camera's orientation: maps camera space (x right, y up, z backwards) to the world. */
    public Quaternionf rotation() { return rotation; }
    public final Vector3f getLookVector() { return forward; }
    public final Vector3f getUpVector() { return up; }
    public final Vector3f getLeftVector() { return left; }
    public Entity getEntity() { return entity; }
    public boolean isDetached() { return true; }

    /** Preview helper (not in the game): look from eye at target. */
    public void previewSet(double ex, double ey, double ez, double tx, double ty, double tz) {
        position = new Vec3(ex, ey, ez);
        Vector3f f = new Vector3f((float) (tx - ex), (float) (ty - ey), (float) (tz - ez)).normalize();
        Vector3f r = new Vector3f(f).cross(0, 1, 0);
        if (r.lengthSquared() < 1e-8f) r.set(1, 0, 0);
        r.normalize();
        Vector3f u = new Vector3f(r).cross(f).normalize();
        forward.set(f);
        up.set(u);
        left.set(r).negate();
        Vector3f b = new Vector3f(f).negate();
        rotation.setFromNormalized(new Matrix3f(r.x, r.y, r.z, u.x, u.y, u.z, b.x, b.y, b.z));
        yRot = (float) Math.toDegrees(Math.atan2(-f.x, f.z));
        xRot = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, f.y))));
    }
}
