package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: one gate on one frame, as plain numbers: where it is, how it is turned, how open it is and where the viewer stands. The layer
 * fills it, the composer reads it; keeping it a mutable bag (reused every frame, no allocation) and free of Minecraft types is what
 * lets the same composer run in the preview.
 */
public final class PortalFrame {
    /** Centre of the opening, camera-relative. */
    public float cx, cy, cz;
    /** Unit axes of the gate: right, up and front normal, in camera-relative space. */
    public float rx, ry, rz, ux, uy, uz, nx, ny, nz;
    /** Half width and half height in blocks, already multiplied by the opening animation. */
    public float hw, hh;
    /** Radius scale of the cast (power): drives thickness and depth in blocks. */
    public float size = 1f;
    /** Age in ticks (smooth) and the effect's total duration. */
    public float age, duration;
    /** 0..1 overall brightness (opening and closing). */
    public float fade = 1f;
    /** Viewer position in the gate's own axes (blocks from the centre: x right, y up, z front). */
    public float vx, vy, vz;
    /** Base colour, ARGB (alpha ignored). */
    public int argb = 0xFFB088FF;
    public long seed;
    /** True when the horizon is drawn by the portal shader: no backdrop, the shader supplies its own dark tint. */
    public boolean shader;

    /** Sets the viewer's gate-local position from its camera-relative offset to the centre (the camera is at the origin). */
    public void viewerFromCamera() {
        float dx = -cx, dy = -cy, dz = -cz;
        vx = dx * rx + dy * ry + dz * rz;
        vy = dx * ux + dy * uy + dz * uz;
        vz = dx * nx + dy * ny + dz * nz;
    }
}
