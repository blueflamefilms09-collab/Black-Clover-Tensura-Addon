package net.minecraft.client.renderer.entity;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;

/** Preview stub of EntityRenderDispatcher: the camera and its orientation (for camera-facing quads). */
public class EntityRenderDispatcher {
    public Camera camera;

    public EntityRenderDispatcher(Camera camera) { this.camera = camera; }

    public Quaternionf cameraOrientation() { return camera.rotation(); }
}
