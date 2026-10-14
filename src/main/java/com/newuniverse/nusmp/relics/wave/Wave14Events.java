package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wave 14: the game-bus handlers, registered once by {@link Wave14#hooks}. Each leaves at once for anyone the wave does not concern: the relics'
 * own flag {@link Wave14Rules#F_ANY} is one cached lookup, and nothing here scans the world on a tick.
 */
final class Wave14Events {
    private Wave14Events() {}

    /** Per player: a once-a-second pass (staggered by entity id), plus a faster pass every few ticks for the boots that act on movement. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 3) {                                                                // clocks that must run even if the relic came off
            Wave14Head.siegePass(p);
            Wave14Feet.sneakPass(p);
            Wave14Feet.flightPass(p);
            Wave14Feet.highPass(p, false);
        }
        if (!RelicFlags.has(p, Wave14Rules.F_ANY)) return;
        if (phase == 5) {
            if (RelicFlags.has(p, Wave14Rules.F_DUCK)) Wave14Head.duckPass(p);
            if (RelicFlags.has(p, Wave14Rules.F_UNICORN)) Wave14Head.hairGlow(p);
        }
        int quick = (p.tickCount + p.getId()) % 4;
        if (quick == 0 && RelicFlags.has(p, Wave14Rules.F_INDOM)) Wave14Feet.sink(p);
        if (quick == 2 && RelicFlags.has(p, Wave14Rules.F_WATER)) Wave14Feet.walkOnWater(p);
        if (quick == 1 && RelicFlags.has(p, Wave14Rules.F_HIGH)) Wave14Feet.highPass(p, true);
        if (quick == 3 && RelicFlags.has(p, Wave14Rules.F_FIRE)) Wave14Feet.flightPass(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { Wave14Zones.tick(e.getServer()); }

    /** The wearer's fire keeps burning what it burnt (Dragon Fire). */
    static void onDamagePost(LivingDamageEvent.Post e) {
        Entity attacker = e.getSource().getEntity();
        if (attacker instanceof ServerPlayer p && RelicFlags.has(p, Wave14Rules.F_GOLD)) Wave14Head.dragonFire(p, e.getEntity(), e.getSource());
    }

    /** Winter's Fury and the high ground add to a blow; Warding Wind softens missiles; a hard hit breaks Siege Monster's concentration. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (src.getEntity() instanceof ServerPlayer p && src.getDirectEntity() == p && src.is(DamageTypes.PLAYER_ATTACK) && victim != p) {
            if (RelicFlags.has(p, Wave14Rules.F_SILVER)) e.setAmount(e.getAmount() + Wave14Head.wintersFury(p, victim));
            if (RelicFlags.has(p, Wave14Rules.F_HIGH) && Wave14Feet.highActive(p)) e.setAmount(e.getAmount() * Wave14Rules.HIGH_ADVANTAGE);
        }
        if (victim instanceof ServerPlayer v) {
            if (src.is(DamageTypeTags.IS_PROJECTILE) && RelicFlags.has(v, Wave14Rules.F_WIND) && Wave14Feet.warded(v)) e.setAmount(Wave14Rules.wardedProjectile(e.getAmount()));
            if (RelicFlags.has(v, Wave14Rules.F_STEEL)) Wave14Head.siegeHit(v, e.getAmount());
        }
    }

    /** Feather of the Phoenix (no fall hurts) and the Boots of the Indomitable (a fall lands on someone else). */
    static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, Wave14Rules.F_ANY)) return;
        if (RelicFlags.has(p, Wave14Rules.F_PHOENIX)) e.setDamageMultiplier(0f);
        else if (RelicFlags.has(p, Wave14Rules.F_INDOM)) Wave14Feet.indomitableFall(p, e);
    }

    /** Up in Flames raises the wearer; an evil deed ends the Unicorn Crown's attunement. */
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer v) {
            if (RelicFlags.has(v, Wave14Rules.F_PHOENIX) && Wave14Head.phoenixRise(v, e.getSource(), e)) return;
            Wave14Head.forget(v);
            Wave14Feet.flightForget(v);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer k && k != e.getEntity() && RelicFlags.has(k, Wave14Rules.F_UNICORN)) Wave14Head.evilDeed(k, e.getEntity());
    }

    /** Feeding a horse is aiding a unicorn, to the Dark Unicorn Crown. */
    static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, Wave14Rules.F_DARK)) return;
        if (e.getTarget() instanceof AbstractHorse h && h.isFood(e.getItemStack())) Wave14Head.aidedUnicorn(p);
    }

    /** The Sandals of Sneaking: their footfalls do not register with sculk sensors or the warden... */
    static void onGameEvent(VanillaGameEvent e) {
        if (Wave14Feet.SNEAKERS.isEmpty()) return;
        if (e.getCause() instanceof ServerPlayer p && Wave14Feet.SNEAKERS.contains(p.getUUID()) && e.getVanillaEvent().is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) e.setCanceled(true);
    }

    /** ... and make no sound for anyone else to hear: a step sound played for the wearer is cancelled. */
    static void onSound(PlayLevelSoundEvent.AtEntity e) {
        if (Wave14Feet.SNEAKERS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || !Wave14Feet.SNEAKERS.contains(p.getUUID())) return;
        Holder<SoundEvent> sound = e.getSound();
        if (sound != null && sound.unwrapKey().map(k -> k.location().getPath().endsWith(".step")).orElse(false)) e.setCanceled(true);
    }

    /** Footsteps are played at a position, not at the player: a step sound within a block and a half of a wearer is cancelled (server side). */
    static void onStepSound(PlayLevelSoundEvent.AtPosition e) {
        if (Wave14Feet.SNEAKERS.isEmpty() || e.getLevel().isClientSide()) return;
        Holder<SoundEvent> sound = e.getSound();
        if (sound == null || !sound.unwrapKey().map(k -> k.location().getPath().endsWith(".step")).orElse(false)) return;
        Vec3 at = e.getPosition();
        for (Player pl : e.getLevel().players()) {
            if (Wave14Feet.SNEAKERS.contains(pl.getUUID()) && pl.distanceToSqr(at) < 2.25) {
                e.setCanceled(true);
                return;
            }
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Wave14Head.forget(p);
            Wave14Feet.flightForget(p);
            Wave14Feet.sneakForget(p);
        }
    }

    /** A flight left behind by a crash or a crashed server is closed on the next login. */
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave14Feet.flightPass(p);
    }

    static void onStopping(ServerStoppingEvent e) {
        Wave14Zones.clear();
        Wave14Head.clearAll();
        Wave14Feet.SNEAKERS.clear();
    }
}
