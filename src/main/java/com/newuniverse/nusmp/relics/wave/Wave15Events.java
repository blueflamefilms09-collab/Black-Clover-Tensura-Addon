package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 15: the game-bus handlers, registered once by {@link Wave15#hooks}. Each leaves at once for anyone the wave does not concern: the relics'
 * own flag {@link Wave15Rules#F_ANY} is one cached lookup, and nothing here scans the world on a tick.
 */
final class Wave15Events {
    private Wave15Events() {}

    /** Once a second per player (staggered by entity id): the clocks that must run even if the relic came off, then the wearer's upkeep. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 6) Wave15Wear.clocks(p);
        if (phase != 11 || !RelicFlags.has(p, Wave15Rules.F_ANY)) return;
        Wave15Wear.pass(p);
        if (RelicFlags.has(p, Wave15Rules.F_FRIENDS)) Wave15Hands.friendsPass(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { Wave15Field.tick(e.getServer()); }

    /** Before armour: frost bounces off a wrapped towel, a flurry or a blur turns blows aside; the wearer's own blows get their bonuses. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (src.getEntity() instanceof ServerPlayer a && RelicFlags.has(a, Wave15Rules.F_ANY)) {
            if (src.getDirectEntity() == a && RelicFlags.has(a, Wave15Rules.F_INITIATIVE)) Wave15Hands.ambush(a, victim, e);
            Wave15Hands.bareBonus(a, victim, src, e);
        }
        if (!(victim instanceof ServerPlayer v) || !RelicFlags.has(v, Wave15Rules.F_ANY)) return;
        if (RelicFlags.has(v, Wave15Rules.F_TOWEL) && src.is(DamageTypeTags.IS_FREEZING) && Wave15Wear.wrapped(v)) {
            e.setCanceled(true);
            return;
        }
        CompoundTag d = Wave15Util.peek(v);
        if (d == null) return;
        long now = v.level().getGameTime();
        long until = d.getLong("evade_until");
        if (now >= until) return;
        Entity direct = src.getDirectEntity();
        if (direct == null) return;
        boolean flurry = "flurry".equals(d.getString("evade_kind"));
        if (flurry && RelicFlags.has(v, Wave15Rules.F_SNOW) && direct instanceof LivingEntity && Wave15Rules.flurryEvades(now, until, d.getLong("evade_last"))) {
            d.putLong("evade_last", now);
            e.setCanceled(true);
            Fx.spray(v.serverLevel(), FxThemes.RELIC_WAVE_15, FxTone.LIGHT, v.getX(), v.getY() + 1.0, v.getZ(), 20, 0.5, 0.6, 0.5, 0.05);
        } else if (!flurry && RelicFlags.has(v, Wave15Rules.F_AUTUMN) && (direct instanceof LivingEntity || direct instanceof Projectile)
                && Wave15Rules.blurEvades(now, until, v.getRandom().nextInt(100))) {
            e.setCanceled(true);
            Fx.spray(v.serverLevel(), FxThemes.RELIC_WAVE_15, Wave15Util.align(0xC8601E), v.getX(), v.getY() + 1.0, v.getZ(), 14, 0.5, 0.6, 0.5, 0.03);
        }
    }

    /** After armour: the friend's pull-back from a killing blow (works for a bound friend who wears nothing). */
    static void onDamagePre(LivingDamageEvent.Pre e) {
        if (e.getEntity() instanceof ServerPlayer v && e.getNewDamage() >= v.getHealth()) Wave15Hands.rescue(v, e);
    }

    /** After the blow: the Cape of Projection puts a share of it on whoever struck; the sentient cloak may smother the attacker. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || !RelicFlags.has(v, Wave15Rules.F_ANY)) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker) || attacker == v) return;
        if (RelicFlags.has(v, Wave15Rules.F_LEVITATE)) Wave15Wear.levitationReaction(v, attacker, v.getHealth());
        if (RelicFlags.has(v, Wave15Rules.F_PROJECTION) && !src.is(DamageTypes.THORNS) && Wave15Util.target(v, attacker)) {
            CompoundTag d = Wave15Util.data(v);
            long now = v.level().getGameTime();
            if (now < d.getLong("proj_at")) return;
            float back = Wave15Rules.reflected(e.getNewDamage());
            if (back < 0.5f) return;
            d.putLong("proj_at", now + 20);
            attacker.hurt(v.damageSources().thorns(v), back);
            Wave15Util.tell(v, "\"That was clearly " + attacker.getName().getString() + "'s fault!\"", net.minecraft.ChatFormatting.GOLD);
        }
    }

    /** The cloak of levitation: no fall damage. */
    static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && RelicFlags.has(p, Wave15Rules.F_LEVITATE)) {
            e.setDistance(0f);
            e.setDamageMultiplier(0f);
        }
    }

    /** A creature does not pick a target it was stared out of, or one hidden under an Elvenkind hood. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewAboutToBeSetTarget();
        if (t == null || !(e.getEntity() instanceof Mob m)) return;
        if (Wave15Wear.calmed(m, t) || (t instanceof ServerPlayer p && RelicFlags.has(p, Wave15Rules.F_ELVEN) && Wave15Wear.elvenHides(m, p))) e.setCanceled(true);
    }

    static void onWake(PlayerWakeUpEvent e) {
        if (e.wakeImmediately() || !(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, Wave15Rules.F_FRIENDS)) return;
        Wave15Hands.rested(p);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave15Field.restoreStale(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave15Field.onLogout(p);
    }

    /** A player who died while phased comes back in the game mode they had (the new body inherits spectator mode from the old one). */
    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave15Field.restoreStale(p);
    }

    static void onStopping(ServerStoppingEvent e) { Wave15Field.clear(e.getServer()); }
}
