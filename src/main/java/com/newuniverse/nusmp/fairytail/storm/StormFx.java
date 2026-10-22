package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the server sends a storm to the clients as a few tiny effect packets (about 60 bytes each, through {@code VfxSpawn}: nearby players only, and counted against the shared
 * per-tick effect-packet budget), not as particles or blocks: one packet for the cloud, one for what falls from it, one per bolt, one when a storm is ended early. The clients build
 * every vertex themselves from the seed ({@link StormMath}), so the picture costs the server nothing per tick. A failure to send is never the spell's failure.
 */
final class StormFx {
    private StormFx() {}

    private static Vec3 v(double[] a) { return new Vec3(a[0], a[1], a[2]); }

    private static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int duration, float power, long seed) {
        try {
            VfxSpawn.send(level, new VfxPayload(shape.ordinal(), from, to, 0, duration, power, -1, seed));
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    /** The cloud and what falls from it, for a standing cell. */
    static void cell(ServerLevel level, StormCell c) {
        send(level, VfxShape.STORM_CLOUD, v(c.from), v(c.to), c.duration, (float) c.radius, c.seed);
        send(level, VfxShape.STORM_FALL, v(c.from), v(c.to), c.duration, (float) c.radius, c.seed);
    }

    /** A gust front or a storm surge, with its low roll of cloud. */
    static void front(ServerLevel level, StormCell c) {
        send(level, VfxShape.STORM_FRONT, v(c.from), v(c.to), c.duration, (float) c.radius, c.seed);
    }

    /** One forked bolt from the cloud to its mark. */
    static void bolt(ServerLevel level, Vec3 origin, Vec3 mark, long seed) {
        send(level, VfxShape.STORM_BOLT, origin, mark, 10, 1.0f, seed);
        sound(level, mark, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.1f, 1.25f);
        sound(level, mark, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f, 0.8f + (seed & 3) * 0.1f);
    }

    /** Tells the clients near a storm that was ended early (its owner left, died or cast a newer storm) to stop drawing it. */
    static void cancel(ServerLevel level, StormCell c) {
        send(level, VfxShape.STORM_CANCEL, v(c.from), v(c.to), 2, 1.0f, c.seed);
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        if (level == null || at == null || sound == null) return;
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.WEATHER, volume, pitch);
    }
}
