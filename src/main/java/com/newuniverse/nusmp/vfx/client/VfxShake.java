package com.newuniverse.nusmp.vfx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Camera shake for powerful casts. Strength falls off with distance from the camera. */
public final class VfxShake {
    private static float strength;
    private static int ticksLeft, totalTicks;

    private VfxShake() {}

    /** strength ~1 = noticeable, 3 = huge. Shakes stack (strongest wins). */
    public static void add(Vec3 at, float power, int ticks) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        float falloff = (float) Mth.clamp(1 - player.position().distanceTo(at) / 32.0, 0, 1);
        float s = power * falloff;
        if (s > strength * ((float) ticksLeft / Math.max(1, totalTicks))) {
            strength = s;
            ticksLeft = totalTicks = ticks;
        }
    }

    public static void tick() { if (ticksLeft > 0) ticksLeft--; }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0 || strength <= 0) return;
        float time = (float) (ticksLeft - event.getPartialTick());
        float amp = strength * (time / totalTicks);
        event.setYaw(event.getYaw() + Mth.sin(time * 2.1f) * amp);
        event.setPitch(event.getPitch() + Mth.cos(time * 2.7f) * amp * 0.8f);
        event.setRoll(event.getRoll() + Mth.sin(time * 1.3f) * amp * 0.5f);
    }
}
