package com.newuniverse.nusmp.beastwill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: every game event the Beast Will system listens to, in one list (3.45.0). Each handler returns at once for a player with no will (one {@code hasData} lookup) and does nothing on the client.
 */
public final class BeastWillEvents {
    private BeastWillEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener((AddReloadListenerEvent e) -> e.addListener(new BeastWillLoader()));
        bus.addListener(BeastWillEvents::onTick);
        bus.addListener(BeastWillEvents::onLogin);
        bus.addListener(BeastWillEvents::onLogout);
        bus.addListener(BeastWillEvents::onRespawn);
        bus.addListener(BeastWillEvents::onDimension);
        bus.addListener(BeastWillEvents::onDeath);
        bus.addListener(BeastWillEvents::onDamage);
        bus.addListener(BeastWillDrops::onDrops);
        bus.addListener(BeastWillStealth::onChangeTarget);
        bus.addListener(BeastWillStealth::onGameEvent);
        bus.addListener(BeastWillApi::onInteract);
        bus.addListener((ServerStoppingEvent e) -> BeastWillManager.clearAll());
    }

    static void onTick(PlayerTickEvent.Post e) { if (e.getEntity() instanceof ServerPlayer p) BeastWillManager.tick(p); }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) BeastWillManager.onEnter(p); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BeastWillManager.onLogout(p);
        BeastWillApi.forget(p.getUUID());
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) BeastWillManager.onEnter(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) BeastWillManager.onEnter(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) BeastWillManager.onDeath(p); }

    /** After the damage was taken: a hurt player loses the veil, and a player who hit a creature loses it too (and, in form, earns a little mastery). */
    static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof ServerPlayer p) BeastWillManager.onHurt(p);
        if (e.getSource().getEntity() instanceof ServerPlayer a && a != victim) BeastWillManager.onHit(a, victim);
    }
}
