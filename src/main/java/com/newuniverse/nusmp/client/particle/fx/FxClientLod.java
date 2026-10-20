package com.newuniverse.nusmp.client.particle.fx;

import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Why: a custom particle spawned 150 blocks from the camera is a few pixels and a wasted draw, so the facade asks how much detail a spawn at a point deserves: 1 near the camera, less
 * further out (the quality level's distance bands, the same ones the VFX layers use), 0 beyond the cull distance. It is the only place the common facade touches the client's camera, so it
 * lives in the client package and is only called when the level is a client level; it never throws (no camera yet = full detail).
 */
public final class FxClientLod {
    private FxClientLod() {}

    public static float distanceDetail(double x, double y, double z, VfxQuality q) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameRenderer == null) return 1f;
            Camera cam = mc.gameRenderer.getMainCamera();
            if (cam == null || !cam.isInitialized()) return 1f;
            Vec3 p = cam.getPosition();
            double dx = p.x - x, dy = p.y - y, dz = p.z - z;
            return q.detailAtSq(dx * dx + dy * dy + dz * dz);
        } catch (RuntimeException e) {
            return 1f;
        }
    }
}
