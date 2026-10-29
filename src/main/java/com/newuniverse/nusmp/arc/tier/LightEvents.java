package com.newuniverse.nusmp.arc.tier;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: a Death Note scene is keyed by the player's UUID and must never outlive the player's session: when the target logs out, dies (of anything, including the Death Note itself) or changes
 * dimension, the scene is cancelled at once (the boss tells its viewers on its next tick), and when the server stops nothing carries over to the next world. Four tiny listeners, each one map lookup.
 */
public final class LightEvents {
    private LightEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(LightEvents::onLogout);
        bus.addListener(LightEvents::onDeath);
        bus.addListener(LightEvents::onDimension);
        bus.addListener(LightEvents::onStopping);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) LightScenes.playerGone(p.getUUID()); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) LightScenes.playerGone(p.getUUID()); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) LightScenes.playerGone(p.getUUID()); }

    static void onStopping(ServerStoppingEvent e) { LightScenes.reset(); }
}
