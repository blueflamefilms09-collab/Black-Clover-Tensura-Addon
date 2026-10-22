package com.newuniverse.nusmp.spiritlight;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Spirit of Light listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a spirit linked, and the cleanup at logout, death,
 * respawn, dimension change and server stop (the spirit, its props, its ledger slots and Swiftness III all go with the link).
 */
public final class SpiritLightEvents {
    private SpiritLightEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SpiritLightEvents::onPlayerTick);
        bus.addListener(SpiritLightEvents::onHurt);
        bus.addListener(SpiritLightEvents::onLogout);
        bus.addListener(SpiritLightEvents::onDeath);
        bus.addListener(SpiritLightEvents::onRespawn);
        bus.addListener(SpiritLightEvents::onDimension);
        bus.addListener(SpiritLightEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with a spirit is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (SpiritLight.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !SpiritLight.LIVE.contains(p.getUUID())) return;
        SpiritLight.upkeep(p);
    }

    /** The owner took damage (one set lookup first: only a player with a spirit is visited). */
    static void onHurt(LivingDamageEvent.Post e) {
        if (SpiritLight.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || e.getNewDamage() <= 0f || !SpiritLight.LIVE.contains(p.getUUID())) return;
        SpiritLight.onOwnerHurt(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SpiritLight.forget(p);
        SpiritLightVoice.forget(p.getUUID());
    }

    /** The owner died (a last word, then everything goes), or something died to a player with a spirit (a victory). */
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            if (SpiritLight.LIVE.contains(p.getUUID())) SpiritLight.onOwnerDied(p);
            SpiritLight.forget(p);
        } else if (!SpiritLight.LIVE.isEmpty() && e.getSource().getEntity() instanceof ServerPlayer killer && SpiritLight.LIVE.contains(killer.getUUID())) {
            SpiritLight.onOwnerKill(killer);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritLight.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritLight.forget(p); }

    static void onStopping(ServerStoppingEvent e) {
        SpiritLight.clearAll();
        SpiritLightVoice.clear();
    }
}
