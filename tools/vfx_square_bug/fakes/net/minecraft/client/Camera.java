package net.minecraft.client;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Camera {
    public Vec3 getPosition() { return new Vec3(10, 64, -3); }
    public Vector3f getLeftVector() { return new Vector3f(-0.8f, 0f, 0.6f); }
    public Vector3f getUpVector() { return new Vector3f(0.1f, 0.98f, 0.1f); }
}
