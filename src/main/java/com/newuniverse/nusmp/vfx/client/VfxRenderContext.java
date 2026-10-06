package com.newuniverse.nusmp.vfx.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Everything a layer needs for one frame. */
public final class VfxRenderContext {
    public final Camera camera;
    public final Vec3 camPos;
    public final Matrix4f modelView;
    public final float partialTick;
    public final ClientLevel level;
    /** 1 = All particles, 0.5 = Decreased. Layers multiply segment counts by this. */
    public final float detail;
    /** Camera right/up in world space, for billboards and ribbons. */
    public final Vector3f camRight, camUp;

    public VfxRenderContext(Camera camera, Matrix4f modelView, float partialTick, ClientLevel level, float detail) {
        this.camera = camera;
        this.camPos = camera.getPosition();
        this.modelView = modelView;
        this.partialTick = partialTick;
        this.level = level;
        this.detail = detail;
        this.camRight = new Vector3f(camera.getLeftVector()).negate();
        this.camUp = new Vector3f(camera.getUpVector());
    }

    /** World position -> camera-relative vector (what the vertex buffer wants). */
    public Vector3f rel(Vec3 world) {
        return new Vector3f((float) (world.x - camPos.x), (float) (world.y - camPos.y), (float) (world.z - camPos.z));
    }

    /** Scales a segment count by the particle setting, never below min. */
    public int seg(int full, int min) { return Math.max(min, Math.round(full * detail)); }
}
