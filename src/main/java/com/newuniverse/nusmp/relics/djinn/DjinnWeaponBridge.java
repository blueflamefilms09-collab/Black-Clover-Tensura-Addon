package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What the Djinn weapons (relics.djinn.weapon) may use of this package's helpers, which are package-private: the Magoi payment with the same drain
 * factor the phases pay, the foes and allies of the Djinn spells, the shove, and the bounded particle shapes. Nothing here has logic of its own; it
 * only makes the existing economy and target rules reachable, so a weapon act is paid and aimed exactly like the Djinn's other magic.
 */
public final class DjinnWeaponBridge {
    private DjinnWeaponBridge() {}

    /** Pays {@code share} of the maximum Magoi times the phase drain factor (and the dark Djinn's); false, and nothing spent, if the pool cannot (it never goes below the floor). */
    public static boolean pay(ServerPlayer p, double share) {
        DjinnState s = DjinnState.of(p);
        double factor = DjinnPhaseRules.drainFactor(Bonds.mask(p), Bonds.multiBond(p), s.assimilation());
        return Magoi.pay(p, share * factor, DjinnPhaseRules.FLOOR);
    }

    /** The bearer's Magoi bar as a share (1.0 for creative players). */
    public static double magoi(ServerPlayer p) { return Magoi.fraction(p); }

    /** Foes within {@code radius} of {@code centre} (the Djinn spells' own targets: never creative or spectator players, allies, pets or summons; at most 24). */
    public static List<LivingEntity> foesNear(ServerPlayer p, Vec3 centre, double radius) { return DjinnTargets.foesNear(p, centre, radius); }

    /** The caster, their allies by team and the household of their Djinn within {@code radius}. */
    public static List<ServerPlayer> alliesNear(ServerPlayer p, double radius) { return DjinnTargets.alliesNear(p, radius); }

    /** Throws a living thing away from {@code from}. */
    public static void push(LivingEntity t, Vec3 from, double strength, double up) { DjinnMagic.push(t, from, strength, up); }

    public static String damageType(Djinn d) { return DjinnFx.damageType(d.affinity()); }

    public static ParticleOptions particle(Djinn d) { return DjinnFx.particle(d.affinity()); }

    public static DustParticleOptions dust(int rgb, float size) { return DjinnFx.dust(rgb, size); }

    public static void ring(ServerLevel l, Vec3 at, double radius, int rgb, int points, double lift) { DjinnFx.ring(l, at, radius, rgb, points, lift); }

    public static void line(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions o, int count) { DjinnFx.line(l, a, b, o, count); }

    public static void column(ServerLevel l, double x, double y0, double y1, double z, ParticleOptions o, int count) { DjinnFx.column(l, x, y0, y1, z, o, count); }

    /** A mob effect by id (or the pseudo id "nusmp:burn"); an id the game does not know is skipped. */
    public static void effect(LivingEntity t, String id, int ticks, int amp) { DjinnFx.effect(t, id, ticks, amp); }

    public static boolean dark(ServerPlayer p) { return DarkDjinn.active(DjinnState.of(p)); }

    /** True while an Extreme Magic is being charged (the weapon is quiet until it has struck). */
    public static boolean charging(ServerPlayer p) { return DjinnEngine.charging(p); }
}
