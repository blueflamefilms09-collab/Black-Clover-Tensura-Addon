package com.newuniverse.nusmp.arc.kakashi;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: everything the Kakashi boss knows about a player (the fighter list, the Sharingan copy ledger, the wet mark, the Lightning Beast's prey) is keyed by the player's UUID and must not outlive
 * the player's session: on logout, death or a change of dimension he forgets them at once, and when the server stops nothing carries over. A death near him is also his cue to speak. Four
 * tiny listeners; each walks at most {@code globalCap} (2) bosses. Server side only.
 */
public final class KakashiEvents {
    private KakashiEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(KakashiEvents::onLogout);
        bus.addListener(KakashiEvents::onDeath);
        bus.addListener(KakashiEvents::onDimension);
        bus.addListener(KakashiEvents::onStopping);
    }

    static void forget(ServerPlayer p) {
        for (KakashiBoss b : KakashiBoss.live()) b.forget(p.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (KakashiBoss b : KakashiBoss.live()) if (b.fighters().contains(p.getUUID()) && b.level() == p.level()) b.noteKill();
        forget(p);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }

    static void onStopping(ServerStoppingEvent e) { KakashiBoss.clearLive(); }
}
