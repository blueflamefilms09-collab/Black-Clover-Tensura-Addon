package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every sight and sound of Israfil goes out from one place, so the cost is easy to read: one effect packet per ability use (Rift Walk: one a second for the few seconds it lasts), always through
 * {@link VfxSpawn}, which sends to nearby players only and goes through the effect-packet budget. The shapes are drawn by the client's {@code TrialLayer}. Server thread only.
 */
public final class TrialFx {
    private TrialFx() {}

    /** Cold cyan, the tint every Trial effect starts from (the layer adds the indigo and the brass). */
    public static final int CYAN = 0xFF6FE8F0;

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** The cage of Dimensional Labyrinth, fixed where it was raised. */
    public static void labyrinth(ServerLevel l, LivingEntity target, double radius, int ticks) {
        Vec3 at = target.position();
        VfxSpawn.send(l, VfxShape.TRIAL_LABYRINTH, at.add(0.0, 1.0, 0.0), at.add(0.0, 2.0, 0.0), CYAN, ticks, (float) radius);
        sound(l, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.5f);
    }

    /** The door of the pocket maze: the labyrinth closing round a spot (where a creature is taken in, and where it arrives). Sent to the players near that spot only. */
    public static void pocket(ServerLevel l, Vec3 at, float radius, int ticks) {
        VfxSpawn.send(l, VfxShape.TRIAL_LABYRINTH, at.add(0.0, 1.0, 0.0), at.add(0.0, 2.0, 0.0), CYAN, ticks, radius);
        sound(l, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.5f);
    }

    /** Weight of Sins: the crush on the target. */
    public static void gravity(ServerLevel l, LivingEntity target) {
        Vec3 at = target.position();
        VfxSpawn.sendFollowing(l, VfxShape.TRIAL_GRAVITY, target, at, CYAN, 40, (float) Math.max(1.0, target.getBbWidth() * 1.6));
        sound(l, at, SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
    }

    /** Spatial Collapse: the implosion and the lingering Nihility Collapse field. */
    public static void collapse(ServerLevel l, Vec3 center, float radius, int ticks) {
        VfxSpawn.send(l, VfxShape.TRIAL_COLLAPSE, center, center.add(0.0, 1.0, 0.0), CYAN, ticks, radius);
        sound(l, center, SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 0.6f);
    }

    /** Coordinate Shift (and the natural blink): from where the owner was to where they are now. */
    public static void shift(ServerLevel l, Vec3 from, Vec3 to) {
        VfxSpawn.send(l, VfxShape.TRIAL_SHIFT, from, to, CYAN, 14, 1.0f);
        sound(l, from, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);
    }

    /** The Grand Trumpet: a horn of light and rings out to {@code radius}. */
    public static void trumpet(ServerLevel l, Vec3 center, float radius) {
        VfxSpawn.send(l, VfxShape.TRIAL_TRUMPET, center, center.add(0.0, 1.0, 0.0), CYAN, 50, radius);
        sound(l, center, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), 4.0f, 0.7f);
    }

    /** Rift Walk: a shimmer around the walker, sent when the walk starts and again each second it lasts. */
    public static void rift(ServerLevel l, LivingEntity walker) {
        Vec3 at = walker.position();
        VfxSpawn.sendFollowing(l, VfxShape.TRIAL_RIFT, walker, at.add(0.0, 1.0, 0.0), CYAN, 26, 1.0f);
    }
}
