package com.newuniverse.nusmp.vfx;

import net.minecraft.world.phys.Vec3;

/** Preview stub: the payload fields layers read. */
public record VfxPayload(int shape, Vec3 from, Vec3 to, int color, int duration, float power, int followEntity, long seed) {
    public VfxShape shapeType() { return VfxShape.byId(shape); }
}
