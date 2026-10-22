package com.newuniverse.nusmp.gnome;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Gnome listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a Gnome out, and the cleanup at logout, death, respawn,
 * dimension change and server stop (so a Gnome never outlives its master and no ledger keeps a slot).
 */
public final class GnomeEvents {
    private GnomeEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(GnomeEvents::onPlayerTick);
        bus.addListener(GnomeEvents::onLogout);
        bus.addListener(GnomeEvents::onDeath);
        bus.addListener(GnomeEvents::onRespawn);
        bus.addListener(GnomeEvents::onDimension);
        bus.addListener(GnomeEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with a Gnome out is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (Gnome.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !Gnome.LIVE.contains(p.getUUID())) return;
        Gnome.upkeep(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) Gnome.forget(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) Gnome.forget(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) Gnome.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) Gnome.forget(p); }

    static void onStopping(ServerStoppingEvent e) { Gnome.clearAll(); }
}
