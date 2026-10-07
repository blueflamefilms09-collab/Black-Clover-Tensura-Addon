package net.minecraft.client.renderer;

import net.minecraft.client.Camera;

/** Preview stub of GameRenderer: the main camera. */
public class GameRenderer {
    private final Camera mainCamera = new Camera();

    public Camera getMainCamera() { Camera.previewUses++; return mainCamera; }
}
