package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Why: every reaction of the System to the game is a listener on the game bus, and keeping them in one class that only DELEGATES (to {@link SystemQuests},
 * {@link ShadowExtraction}, {@link ShadowMinions}, {@link SystemSync}) means each rule has one home and this file is the table of contents of what the
 * System listens to: the server tick (the sync, the shadows' sweep, the extraction window), the player tick (the clock and the room, and every two seconds the safety net that keeps the stat bonus on exactly while the player is in a dungeon dimension), a death (kill quests,
 * ambush quests, the extraction memory, a shadow falling), damage (twice: a guard that stops a shadow and its owner from hurting each other, early, on the
 * incoming blow; and the quest notice for the player on {@code LivingDamageEvent.Post}, which reports the health the player REALLY lost after armour, absorption
 * and shield, so a blow another mod cancelled, a shield turned aside or an absorption heart swallowed never spoils an "unscathed" quest), a target change (a shadow must
 * never pick its owner), loot and experience (a shadow gives none), a creature joining (a saved shadow comes back), a creature changing dimension (a shadow
 * never leaves the Mega-Dungeon alone), and the player's comings and goings (login, respawn, dimension change: the stat bonus goes on or off with the dimension; logout) and the server stopping. Registered with
 * {@code addListener} like the rest of the mod. Every handler is wrapped so an exception here can never break the game's own event, and the cheap checks
 * come first.
 */
public final class SystemEvents {
    private SystemEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SystemEvents::onServerTick);
        bus.addListener(SystemEvents::onPlayerTick);
        bus.addListener(SystemEvents::onDeath);
        bus.addListener(EventPriority.HIGH, SystemEvents::onFriendlyFire);
        bus.addListener(SystemEvents::onPlayerHurt);
        bus.addListener(EventPriority.LOW, SystemEvents::onChangeTarget);
        bus.addListener(SystemEvents::onDrops);
        bus.addListener(SystemEvents::onExperience);
        bus.addListener(SystemEvents::onJoin);
        bus.addListener(SystemEvents::onTravel);
        bus.addListener(SystemEvents::onLogin);
        bus.addListener(SystemEvents::onLogout);
        bus.addListener(SystemEvents::onRespawn);
        bus.addListener(SystemEvents::onChangedDimension);
        bus.addListener(SystemEvents::onStopped);
    }

    // ================================================================ ticks
    static void onServerTick(ServerTickEvent.Post e) {
        try {
            var server = e.getServer();
            SystemSync.tick(server);
            int t = server.getTickCount();
            if (t % 10 == 3) ShadowMinions.sweep(server);
            if (t % 20 == 7) ShadowExtraction.expire(server);
        } catch (RuntimeException ex) {
            // the System must never break the server tick
        }
    }

    /** How often (ticks) the safety net looks at each player's stat bonus: every two seconds, offset so it does not share a tick with the quest work. */
    static final int RECONCILE_EVERY = 40;

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MEGA_SYSTEM_PLAYER = com.newuniverse.nusmp.perf.PerfProbe.section("s.mega.system_player");   // 1.99.0: performance probe (off by default: one flag read)

    static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MEGA_SYSTEM_PLAYER, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) {
                if (p.tickCount % RECONCILE_EVERY == 11) SystemStats.reconcile(p);
                SystemQuests.tickPlayer(p);
            }
        } catch (RuntimeException ex) {
            // a quest must never break the player's tick
        }
    }

    // ================================================================ deaths
    static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide) return;
            if (ShadowMinions.isMinion(dead)) {
                if (dead instanceof Mob m) ShadowMinions.onDeath(m);
                return;
            }
            if (!(dead.level() instanceof net.minecraft.server.level.ServerLevel level) || !Labyrinth.inMega(level)) return;
            Entity killer = e.getSource().getEntity();
            ServerPlayer credit = killer instanceof ServerPlayer sp ? sp : ShadowMinions.ownerPlayer(killer);
            if (credit != null) SystemQuests.killed(credit, dead);
            SystemQuests.ambushKilled(level, dead);
            ShadowExtraction.record(dead, e.getSource());
        } catch (RuntimeException ex) {
            // a quest must never break a death
        }
    }

    // ================================================================ damage
    /** A shadow and its owner (and the owner's other shadows) are one side: nothing they do hurts each other, explosions and arrows included. */
    static void onFriendlyFire(LivingIncomingDamageEvent e) {
        try {
            if (e.getEntity().level().isClientSide) return;
            UUID hitter = ShadowMinions.principal(e.getSource().getEntity());
            if (hitter == null) return;
            UUID victim = ShadowMinions.principal(e.getEntity());
            if (victim != null && hitter.equals(victim) && (ShadowMinions.isMinion(e.getEntity()) || ShadowMinions.isMinion(e.getSource().getEntity()))) e.setCanceled(true);
        } catch (RuntimeException ex) {
            // never break a hit
        }
    }

    /** The player really lost health (after armour, absorption and shield: the number is what the health bar went down by): the quest notice. */
    static void onPlayerHurt(LivingDamageEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) SystemQuests.damaged(p, e.getSource(), e.getNewDamage());
        } catch (RuntimeException ex) {
            // never break a hit
        }
    }

    // ================================================================ shadows
    /** A shadow never picks its owner or a fellow shadow as a target. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            LivingEntity who = e.getEntity();
            LivingEntity next = e.getNewAboutToBeSetTarget();
            if (next == null || who.level().isClientSide || !ShadowMinions.isMinion(who)) return;
            UUID owner = ShadowMinions.ownerOf(who);
            if (owner != null && owner.equals(ShadowMinions.principal(next))) e.setCanceled(true);
        } catch (RuntimeException ex) {
            // never break a target change
        }
    }

    static void onDrops(LivingDropsEvent e) {
        if (ShadowMinions.isMinion(e.getEntity())) e.setCanceled(true);
    }

    static void onExperience(LivingExperienceDropEvent e) {
        if (ShadowMinions.isMinion(e.getEntity())) e.setCanceled(true);
    }

    /** A saved shadow comes back with its chunk: listed again and given its goals, or cancelled when its owner has been gone for good. */
    static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (e.getLevel().isClientSide() || !e.loadedFromDisk()) return;
            if (e.getEntity() instanceof Mob m && ShadowMinions.isMinion(m) && !ShadowMinions.adopt(m)) e.setCanceled(true);
        } catch (RuntimeException ex) {
            // a damaged shadow just stays as it is
        }
    }

    /** A shadow never leaves the Mega-Dungeon by itself. */
    static void onTravel(EntityTravelToDimensionEvent e) {
        try {
            if (ShadowMinions.isMinion(e.getEntity()) && e.getDimension() != Labyrinth.MEGA_DIMENSION) e.setCanceled(true);
        } catch (RuntimeException ex) {
            // never break a portal
        }
    }

    // ================================================================ players
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) SystemQuests.refreshFresh(p, true);
        } catch (RuntimeException ex) {
            // never break a login
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) SystemQuests.refreshFresh(p, false);
        } catch (RuntimeException ex) {
            // never break a respawn
        }
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) SystemQuests.refresh(p);
        } catch (RuntimeException ex) {
            // never break a dimension change
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
            SystemStats.stashHealth(p);                         // the event fires before the player is saved
            ShadowMinions.ownerLeft(p.getServer(), p.getUUID());
            SystemQuests.forget(p.getUUID());
        } catch (RuntimeException ex) {
            // never break a logout
        }
    }

    static void onStopped(ServerStoppedEvent e) {
        SystemQuests.clearAll();
        ShadowExtraction.clear();
        ShadowMinions.clear();
    }
}
