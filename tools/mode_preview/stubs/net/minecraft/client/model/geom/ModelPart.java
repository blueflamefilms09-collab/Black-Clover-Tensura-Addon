package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Preview stub: pivot + rotation, as ModelPart.translateAndRotate. */
public class ModelPart {
    public float x, y, z, xRot, yRot, zRot;
    public ModelPart(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public void translateAndRotate(PoseStack pose) {
        pose.translate(x / 16f, y / 16f, z / 16f);
        if (xRot != 0 || yRot != 0 || zRot != 0) pose.mulPose(new Quaternionf().rotationZYX(zRot, yRot, xRot));
    }
}
