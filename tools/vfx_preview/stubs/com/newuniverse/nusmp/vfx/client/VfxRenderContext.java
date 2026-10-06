package com.newuniverse.nusmp.vfx.client;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Preview stub of the per-frame context: a camera at camPos looking at a target. */
public final class VfxRenderContext {
    public final Vec3 camPos;
    public final Matrix4f modelView = new Matrix4f();
    public final float partialTick;
    public final Object level = null;
    public final float detail;
    public final Vector3f camRight, camUp, camForward;

    public VfxRenderContext(Vec3 camPos, Vec3 target, float partialTick, float detail) {
        this.camPos = camPos;
        this.partialTick = partialTick;
        this.detail = detail;
        camForward = new Vector3f((float) (target.x - camPos.x), (float) (target.y - camPos.y), (float) (target.z - camPos.z)).normalize();
        camRight = new Vector3f(camForward).cross(0, 1, 0).normalize();
        camUp = new Vector3f(camRight).cross(camForward).normalize();
    }

    public Vector3f rel(Vec3 world) {
        return new Vector3f((float) (world.x - camPos.x), (float) (world.y - camPos.y), (float) (world.z - camPos.z));
    }

    public int seg(int full, int min) { return Math.max(min, Math.round(full * detail)); }
}
