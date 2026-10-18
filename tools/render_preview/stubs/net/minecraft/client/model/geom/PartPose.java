package net.minecraft.client.model.geom;

/**
 * Preview stub of PartPose: the pivot position and rotation of a model part. Like the game's class it has PUBLIC FIELDS (x, y, z, xRot, yRot, zRot);
 * the accessor methods x() ... are kept for the stubs that were written against the record form.
 */
public final class PartPose {
    public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public final float x, y, z, xRot, yRot, zRot;

    private PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
        this.x = x; this.y = y; this.z = z; this.xRot = xRot; this.yRot = yRot; this.zRot = zRot;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float z() { return z; }
    public float xRot() { return xRot; }
    public float yRot() { return yRot; }
    public float zRot() { return zRot; }

    public static PartPose offset(float x, float y, float z) { return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F); }

    public static PartPose rotation(float xRot, float yRot, float zRot) { return offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, yRot, zRot); }

    public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) { return new PartPose(x, y, z, xRot, yRot, zRot); }
}
