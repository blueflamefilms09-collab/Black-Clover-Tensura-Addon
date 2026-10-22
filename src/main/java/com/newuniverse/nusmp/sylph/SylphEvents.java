package com.newuniverse.nusmp.sylph;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Sylph listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a Sylph out, and the cleanup at logout, death,
 * respawn, dimension change and server stop (a Sylph never outlives the link, never follows a player to another world, never stays after the server).
 */
public final class SylphEvents {
    private SylphEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SylphEvents::onPlayerTick);
        bus.addListener(SylphEvents::onLogout);
        bus.addListener(SylphEvents::onDeath);
        bus.addListener(SylphEvents::onRespawn);
        bus.addListener(SylphEvents::onDimension);
        bus.addListener(SylphEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with something live is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (SylphCasting.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !SylphCasting.LIVE.contains(p.getUUID())) return;
        SylphCasting.upkeep(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) SylphCasting.forget(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) SylphCasting.forget(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SylphCasting.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SylphCasting.forget(p); }

    static void onStopping(ServerStoppingEvent e) { SylphCasting.clearAll(); }
}
