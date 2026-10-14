package com.newuniverse.nusmp.boss.refill;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The look of a boss refilling its health bar (server side, cheap: one VFX packet at the start, a few dust rings while it fills, a flash at
 * the end). 'rgb' and 'accent' are 0xRRGGBB. Safe to call from any boss; nothing is placed or broken.
 */
public final class RefillFx {
    private RefillFx() {}

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** The first tick: a big circle, a white-hot burst and a rising note. */
    public static void start(ServerLevel sl, Vec3 at, double height, int rgb, int accent, int ticks) {
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | rgb, Math.max(20, ticks + 10), 3.2f);
        sl.sendParticles(dust(0xFFFFFF, 2.0f), at.x, at.y + height * 0.5, at.z, 40, 0.5, height * 0.4, 0.5, 0.2);
        sl.sendParticles(dust(accent, 1.6f), at.x, at.y + height * 0.5, at.z, 40, 0.7, height * 0.45, 0.7, 0.25);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3f, 0.5f);
    }

    /** Every tick of the refill: motes rise into the body, a ring climbs it every few ticks. progress is 0..1. */
    public static void fill(ServerLevel sl, Vec3 at, double height, double width, int rgb, int accent, float progress, int tick) {
        if (tick % 2 == 0) {
            double a = sl.random.nextDouble() * Math.PI * 2, r = width * 1.4;
            sl.sendParticles(dust(accent, 1.1f), at.x + Math.cos(a) * r, at.y + 0.2, at.z + Math.sin(a) * r, 1, 0, height * 0.03, 0, 0.02);
        }
        if (tick % 8 == 0) {
            double y = at.y + height * progress;
            DustParticleOptions d = dust(rgb, 1.3f);
            for (int i = 0; i < 18; i++) {
                double a = i * Math.PI * 2 / 18;
                sl.sendParticles(d, at.x + Math.cos(a) * width, y, at.z + Math.sin(a) * width, 1, 0, 0, 0, 0);
            }
        }
    }

    /** The last tick: the flash. */
    public static void end(ServerLevel sl, Vec3 at, double height, int rgb, int accent) {
        sl.sendParticles(dust(0xFFFFFF, 2.2f), at.x, at.y + height * 0.5, at.z, 60, 0.6, height * 0.5, 0.6, 0.35);
        sl.sendParticles(dust(accent, 1.8f), at.x, at.y + height * 0.5, at.z, 40, 0.8, height * 0.5, 0.8, 0.3);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 1.4f);
    }
}
