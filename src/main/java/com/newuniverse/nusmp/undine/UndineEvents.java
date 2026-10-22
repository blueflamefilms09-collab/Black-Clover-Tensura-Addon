package com.newuniverse.nusmp.undine;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Undine listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a companion or a full form live, and the cleanup at logout,
 * death, respawn, dimension change and server stop.
 */
public final class UndineEvents {
    private UndineEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(UndineEvents::onPlayerTick);
        bus.addListener(UndineEvents::onLogout);
        bus.addListener(UndineEvents::onDeath);
        bus.addListener(UndineEvents::onRespawn);
        bus.addListener(UndineEvents::onDimension);
        bus.addListener(UndineEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with something live is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (UndineCasting.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !UndineCasting.LIVE.contains(p.getUUID())) return;
        UndineCasting.upkeep(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onStopping(ServerStoppingEvent e) { UndineCasting.clearAll(); }
}
