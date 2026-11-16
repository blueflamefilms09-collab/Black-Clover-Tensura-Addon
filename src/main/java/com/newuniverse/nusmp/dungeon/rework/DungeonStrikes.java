package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.ZogratisDamage;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the small server-side toolbox the dungeon bosses' and creatures' moves share, so no move writes its own damage, control or effect code. SERVER SAFE: it never touches a client class. Damage goes
 * through {@link ZogratisDamage#hit} (the boss damage law: the victim's EP factor, then {@code BalanceLaw.damage}, which caps one hit at the pvp share of a player's max health, 40 % by default);
 * control (a slow) goes through {@code BalanceLaw.beginControl} / {@code controlTicks} (one shared cooldown per caster, diminishing returns, a boss's control is short); effects are the facade's
 * ({@code Fx}, theme {@code dungeon.seal}) which already counts against the particle budgets and reaches only the players near the point; sounds are vanilla events played at a point
 * (the game sends them to nearby players only). Everything is bounded: a query returns at most {@link DungeonReworkRules#MAX_VICTIMS} players.
 */
final class DungeonStrikes {
    private DungeonStrikes() {}

    /** The living, non-creative, non-spectator players within {@code radius} of {@code from}, nearest first, at most {@link DungeonReworkRules#MAX_VICTIMS}. */
    static List<Player> victims(ServerLevel sl, Entity from, double radius) {
        List<Player> list = new ArrayList<>(sl.getEntitiesOfClass(Player.class, from.getBoundingBox().inflate(radius, radius * 0.6, radius),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(from) <= radius * radius));
        list.sort(Comparator.comparingDouble(p -> p.distanceToSqr(from)));
        return list.size() > DungeonReworkRules.MAX_VICTIMS ? new ArrayList<>(list.subList(0, DungeonReworkRules.MAX_VICTIMS)) : list;
    }

    /** One hit of {@code base} damage (before the balance law) from {@code from} on {@code victim}; false if it did not land. */
    static boolean strike(LivingEntity from, Player victim, double base, boolean magic) {
        return ZogratisDamage.hit(from, victim, (float) base, 1.0, 1.0, 0.0, magic);
    }

    /** A shove away from {@code from} (horizontal power {@code power}), the way the old Ground Slam did. */
    static void knock(LivingEntity from, LivingEntity victim, double power) {
        victim.knockback(power, from.getX() - victim.getX(), from.getZ() - victim.getZ());
    }

    /** A slow of {@code ticks} for a player through the control law (one control per caster per 12 s; a short, diminishing duration). Returns whether it was applied. */
    static boolean slow(LivingEntity from, Player victim, int ticks, int amp) {
        if (!BalanceLaw.beginControl(from)) return false;
        victim.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(victim, ticks), amp, false, true));
        return true;
    }

    /** A timed effect on a living entity that is not control (poison, weakness): the duration is clamped to 6 seconds. */
    static void effect(LivingEntity victim, Holder<MobEffect> effect, int ticks, int amp) {
        victim.addEffect(new MobEffectInstance(effect, Math.min(ticks, 120), amp, false, true));
    }

    /** A flat ring of the seal theme around a point (the telegraph and the shock of a blow). {@code dark} picks the shadowed set. */
    static void ring(ServerLevel sl, double x, double y, double z, double radius, int points, boolean dark) {
        Fx.ring(sl, FxThemes.DUNGEON_SEAL, FxTone.of(dark), x, y, z, radius, points);
    }

    /** A burst of the seal theme at a point. */
    static void burst(ServerLevel sl, double x, double y, double z, int count, double spread, boolean dark) {
        Fx.send(sl, FxThemes.DUNGEON_SEAL, FxTone.of(dark), x, y, z, count, spread, 0.06);
    }

    /** One impact flash (the old explosion slot of the theme). */
    static void flash(ServerLevel sl, double x, double y, double z) {
        Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
    }

    /** A line of motes from one point to another (a chain, a gaze, a spit). */
    static void line(ServerLevel sl, Vec3 from, Vec3 to, boolean dark, int perTick) {
        Fx.stream(sl, FxThemes.DUNGEON_SEAL, from, to, FxTone.of(dark), perTick);
    }

    /** A vanilla sound at a point (the game sends it to the players near it only). */
    static void sound(ServerLevel sl, Entity at, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    static void sound(ServerLevel sl, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, x, y, z, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
