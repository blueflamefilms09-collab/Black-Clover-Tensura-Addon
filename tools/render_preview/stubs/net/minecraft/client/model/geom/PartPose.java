package net.minecraft.client.model.geom;

/** Preview stub of PartPose: the pivot position and rotation of a model part. */
public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
    public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public static PartPose offset(float x, float y, float z) { return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F); }

    public static PartPose rotation(float xRot, float yRot, float zRot) { return offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, yRot, zRot); }

    public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) { return new PartPose(x, y, z, xRot, yRot, zRot); }
}
