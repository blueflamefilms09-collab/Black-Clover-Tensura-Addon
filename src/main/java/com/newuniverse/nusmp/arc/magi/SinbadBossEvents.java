package com.newuniverse.nusmp.arc.magi;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the one thing Sinbad keeps about a player is the framework's fighter list (UUIDs), and it must not outlive the player's session: when a player logs out, dies or changes dimension he
 * drops them from it at once, and when the server stops his live set is emptied. Four tiny listeners; each walks at most {@code globalCap} (4) bosses. His delayed strikes check that their
 * target is alive and that he is, and his locked spots are cleared when a fight ends. Server side only.
 */
public final class SinbadBossEvents {
    private SinbadBossEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SinbadBossEvents::onLogout);
        bus.addListener(SinbadBossEvents::onDeath);
        bus.addListener(SinbadBossEvents::onDimension);
        bus.addListener(SinbadBossEvents::onStopping);
    }

    static void forget(ServerPlayer p) {
        for (SinbadBoss b : SinbadBoss.live()) b.fighters().remove(p.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onStopping(ServerStoppingEvent e) { SinbadBoss.clearLive(); }
}
