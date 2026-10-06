package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.Set;

/**
 * A layer draws one family of magic (circles, flame, wind, ...). To add a new magic type:
 * subclass this, return the shapes you handle, implement render, and register it in VfxManager.
 */
public abstract class AbstractVfxLayer {
    /** Shapes this layer draws. */
    public abstract Set<VfxShape> shapes();

    /** Draw one instance. Budget is already set to the instance's vertex cap. */
    public abstract void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf);

    /** Default ticks if the payload/json don't specify one. */
    public int defaultDuration(VfxShape shape) { return 30; }

    /** Default ARGB tint. */
    public int defaultColor(VfxShape shape) { return 0xFFFFD86B; }

    /** Vertex cap for one activation (never above 400). */
    public int vertexBudget(VfxShape shape) { return VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION; }

    /** Called once when the effect starts (sounds/shake). */
    public void onSpawn(VfxInstance inst) {}

    /** Ticked every client tick while alive (shake timing, etc.). */
    public void onTick(VfxInstance inst) {}
}
