package net.minecraft.client.renderer.entity;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;

/** Preview stub of EntityRenderDispatcher: the camera and its orientation (for camera-facing quads). */
public class EntityRenderDispatcher {
    public Camera camera;

    public EntityRenderDispatcher(Camera camera) { this.camera = camera; }

    public Quaternionf cameraOrientation() { return camera.rotation(); }

    /** The preview's viewer is always near: every aura is drawn (PlayerAuraLayer asks for the squared distance). */
    public double distanceToSqr(net.minecraft.world.entity.Entity e) { return 4.0; }
}
