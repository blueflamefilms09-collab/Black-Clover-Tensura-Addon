package com.newuniverse.nusmp.spiritspace;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Spirit of Space listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a spirit, a channel or rings live; a hit on a
 * channelling player (the portal closes); the waypoint book following the player to a new body; and the cleanup at logout, death, respawn, dimension change and server stop.
 */
public final class SpiritSpaceEvents {
    private SpiritSpaceEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SpiritSpaceEvents::onPlayerTick);
        bus.addListener(SpiritSpaceEvents::onHurt);
        bus.addListener(SpiritSpaceEvents::onClone);
        bus.addListener(SpiritSpaceEvents::onLogout);
        bus.addListener(SpiritSpaceEvents::onDeath);
        bus.addListener(SpiritSpaceEvents::onRespawn);
        bus.addListener(SpiritSpaceEvents::onDimension);
        bus.addListener(SpiritSpaceEvents::onStopping);
        bus.addListener((RegisterCommandsEvent e) -> SpiritSpaceCommands.register(e.getDispatcher()));
    }

    /** The upkeep: one set lookup per player per tick, and only a player with something live is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (SpiritSpace.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !SpiritSpace.LIVE.contains(p.getUUID())) return;
        SpiritSpace.upkeep(p);
    }

    static void onHurt(LivingDamageEvent.Post e) {
        if (SpiritSpace.LIVE.isEmpty() || e.getNewDamage() <= 0f || !(e.getEntity() instanceof ServerPlayer p) || !SpiritSpace.LIVE.contains(p.getUUID())) return;
        SpiritSpace.onHurt(p);
    }

    static void onClone(PlayerEvent.Clone e) { WaypointStore.copy(e.getOriginal(), e.getEntity()); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritSpace.forget(p); }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SpiritSpace.died(p);
        else if (!SpiritSpace.LIVE.isEmpty() && e.getSource().getEntity() instanceof ServerPlayer killer && SpiritSpace.LIVE.contains(killer.getUUID())) SpiritSpace.onKill(killer, e.getEntity());
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritSpace.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritSpace.forget(p); }

    static void onStopping(ServerStoppingEvent e) { SpiritSpace.clearAll(); }
}
