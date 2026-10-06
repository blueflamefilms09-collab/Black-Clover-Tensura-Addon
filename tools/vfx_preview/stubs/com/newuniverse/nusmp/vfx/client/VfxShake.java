package com.newuniverse.nusmp.vfx.client;

import net.minecraft.world.phys.Vec3;

/** Preview stub: records the strongest shake requested. */
public final class VfxShake {
    public static float last;
    private VfxShake() {}
    public static void add(Vec3 at, float power, int ticks) { last = Math.max(last, power); }
    public static void tick() {}
}
