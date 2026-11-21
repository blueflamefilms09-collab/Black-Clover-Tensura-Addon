package com.newuniverse.nusmp.arc.goku;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: everything the Goku boss knows about a player (who is fighting him, who counts toward his strength) is keyed by the player's UUID and must not outlive the player's session: when a
 * player logs out, dies or changes dimension he forgets them at once (they leave his fighter list and the next scan, within a tick, reads the players who are really left), and when the server
 * stops nothing carries over. Five tiny listeners (the fifth is the hourly training timer of Goku Omega); each walks at most {@code globalCap} (2) bosses. Server side only.
 */
public final class GokuBossEvents {
    private GokuBossEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(GokuBossEvents::onLogout);
        bus.addListener(GokuBossEvents::onDeath);
        bus.addListener(GokuBossEvents::onDimension);
        bus.addListener(GokuBossEvents::onStopping);
        bus.addListener(GokuBossEvents::onServerTick);
    }

    static void forget(ServerPlayer p) {
        for (GokuBoss b : GokuBoss.live()) {
            b.fighters().remove(p.getUUID());
            b.omega().forgetPlayer(p.getUUID());            // Goku Omega: his per-player counters for this player
            b.rescanSoon();
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (GokuBoss b : GokuBoss.live()) if (b.fighters().contains(p.getUUID())) b.omega().noteKill();     // Goku Omega: a player who dies in his fight counts toward his experience
        forget(p);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onStopping(ServerStoppingEvent e) { GokuBoss.clearLive(); }

    /** Goku Omega's autonomous training: once per in-game hour, one modulus per tick and then plain arithmetic on one saved record (no entity, no chunk, no player). */
    static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % GokuWorld.TIMER_TICKS == 0) GokuWorld.timer(e.getServer());
    }
}
