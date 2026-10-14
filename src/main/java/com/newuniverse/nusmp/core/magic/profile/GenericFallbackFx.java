package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The generic effect a magic falls back to when its own cast or effect code failed: a bolt of the magic's colour to the aim point and a
 * burst there, in the shared wiki-style layers, plus a few of the magic's particles. It does no damage. It is itself guarded: it can
 * never throw, so a broken magic can not break the fallback too.
 */
public final class GenericFallbackFx {
    private GenericFallbackFx() {}

    /** Plays the effect; always returns true (the cast counts as spent). */
    public static boolean play(ServerPlayer player, MagicProfile profile) {
        try {
            ServerLevel level = player.serverLevel();
            Vec3 eye = player.getEyePosition();
            Vec3 at = player.pick(14.0, 1.0f, false).getLocation();
            int color = profile.color();
            VfxSpawn.send(level, profile.castShape(), eye.add(0, -0.3, 0), at, color, 18, 0.9f);
            VfxSpawn.send(level, profile.burstShape(), at, at.add(0, 1, 0), color, 20, 1.0f);
            level.sendParticles(profile.particle(), at.x, at.y + 0.4, at.z, 14, 0.4, 0.4, 0.4, 0.02);
        } catch (Throwable ignored) {
            // the fallback must never fault: nothing more to fall back to
        }
        return true;
    }
}
