package com.newuniverse.nusmp.manacore;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: every game event the rank Mana Cores listen to, in one list (3.51.0). Each handler returns at once when no player has any core state ({@code RUN.isEmpty()}) and none touches the client.
 */
public final class ManaCoreEvents {
    private ManaCoreEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(ManaCoreRuntime::onTick);
        bus.addListener(ManaCoreRuntime::onIncoming);
        bus.addListener(ManaCoreRuntime::onDeath);
        bus.addListener(ManaCoreDrops::onDrops);
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) ManaCoreRuntime.cleanup(p); });
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) ManaCoreRuntime.cleanup(p); });
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) ManaCoreRuntime.cleanup(p); });
        bus.addListener((ServerStoppingEvent e) -> ManaCoreRuntime.clearAll());
    }
}
