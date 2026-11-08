package com.newuniverse.nusmp.saiyan;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: every game event the Saiyan system listens to, in one list (3.63.0). Each handler returns at once for a player who is not a Saiyan (one {@code hasData} lookup) and does nothing on the client.
 */
public final class SaiyanEvents {
    private SaiyanEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SaiyanEvents::onTick);
        bus.addListener(SaiyanEvents::onLogin);
        bus.addListener(SaiyanEvents::onLogout);
        bus.addListener(SaiyanEvents::onRespawn);
        bus.addListener(SaiyanEvents::onDimension);
        bus.addListener(SaiyanEvents::onDeath);
        bus.addListener(SaiyanManager::onIncomingDamage);
        bus.addListener((ServerStoppingEvent e) -> SaiyanManager.clearAll());
    }

    static void onTick(PlayerTickEvent.Post e) { if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.tick(p); }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.onEnter(p); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.onLogout(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.onEnter(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.onEnter(p); }

    /** A Saiyan died (the body goes) and/or a Saiyan killed something (Battle Power, under the anti-farm rules). */
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SaiyanManager.onDeath(p);
        if (e.getSource().getEntity() instanceof ServerPlayer killer) SaiyanManager.creditKill(killer, e.getEntity());
    }
}
