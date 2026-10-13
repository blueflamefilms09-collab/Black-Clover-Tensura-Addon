package com.newuniverse.nusmp.relics.wave;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wave 5's game-bus listeners. One player tick listener does everything periodic (staggered: a pass once a second per player, the dragon five
 * times a second only while a form is on, the Safeguarding move only when one is pending); the rest react to damage, targets, effects and
 * deaths. Nothing here keeps a static reference to a level, an entity or a player, so there is nothing to clear when a server stops.
 */
final class Wave5Hooks {
    private Wave5Hooks() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static boolean registered;

    static synchronized void register() {
        if (registered) return;
        registered = true;
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(Wave5Hooks::onPlayerTick);
        bus.addListener(Wave5Hooks::onIncoming);
        bus.addListener(EventPriority.LOW, Wave5Hooks::onDamagePre);            // after the other mods have had their say: a rescue is for what would still kill
        bus.addListener(Wave5Hooks::onChangeTarget);
        bus.addListener(Wave5Hooks::onEffect);
        bus.addListener(Wave5Hooks::onDeath);
        bus.addListener(Wave5Hooks::onLogin);
        bus.addListener(Wave5Hooks::onRespawn);
        bus.addListener(Wave5Hooks::onLogout);
    }

    // ---------------------------------------------------------------- the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        try {
            if (Wave5Data.hasPlayerData(p)) {
                Wave5Safeguard.applyMove(p);
                if (p.tickCount % 5 == 0 && Wave5Dragon.pending(p)) Wave5Dragon.tick(p);
            }
            if ((p.tickCount + p.getId()) % 20 == 7) slow(p);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 tick failed", failed);
        }
    }

    /** Once a second per player. */
    private static void slow(ServerPlayer p) {
        Wave5Warmage.tick(p);
        Wave5Hemlock.tick(p);
        Wave5Huntress.tick(p);
        Wave5Souls.tick(p);
        Wave5Safeguard.tick(p);
    }

    // ---------------------------------------------------------------- damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        try {
            Wave5Dragon.onIncoming(e);
            if (e.isCanceled()) return;
            Wave5Huntress.onOutgoing(e);
            Wave5Souls.onOutgoing(e);
            Wave5Hemlock.onIncoming(e);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 damage hook failed", failed);
        }
    }

    /** A blow that would kill the wearer: the dragon's own rule first, then the Reliquary, then the Safeguarding record. */
    private static void onDamagePre(LivingDamageEvent.Pre e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || !p.isAlive()) return;
        try {
            float dmg = e.getNewDamage();
            if (dmg <= 0f) return;
            if (Wave5Dragon.onHit(e, p)) return;
            if (dmg - p.getAbsorptionAmount() < p.getHealth()) return;            // not a lethal blow
            if (Wave5Hemlock.reliquary(e, p)) return;
            Wave5Safeguard.restore(e, p);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 lethal-blow hook failed", failed);
        }
    }

    // ---------------------------------------------------------------- targets and effects
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            Wave5Hemlock.onChangeTarget(e);
            if (!e.isCanceled()) Wave5Huntress.onChangeTarget(e);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 target hook failed", failed);
        }
    }

    private static void onEffect(MobEffectEvent.Applicable e) {
        try {
            Wave5Hemlock.onEffect(e);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 effect hook failed", failed);
        }
    }

    // ---------------------------------------------------------------- lives
    private static void onDeath(LivingDeathEvent e) {
        try {
            Wave5Hemlock.onDeath(e);
            if (e.getEntity() instanceof ServerPlayer p) end(p);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 death hook failed", failed);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) end(p); }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) end(p); }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) end(p); }

    /** Every temporary thing wave 5 gave a player ends with a death, a relog or a respawn: the dragon form (and its wings), the ward and the shroud. */
    private static void end(ServerPlayer p) {
        try {
            Wave5Dragon.cleanup(p);
            Wave5Souls.clear(p);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 5 cleanup failed", failed);
        }
    }
}
