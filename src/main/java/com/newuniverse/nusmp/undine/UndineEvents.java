package com.newuniverse.nusmp.undine;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Undine listens to, all cheap and all server only: the once-every-four-ticks upkeep of the few players that have a companion or a full form live, what happens to her master
 * (hurt, a kill, death: her pose, face and line), and the cleanup at logout, death, respawn, dimension change and server stop.
 */
public final class UndineEvents {
    private UndineEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(UndineEvents::onPlayerTick);
        bus.addListener(UndineEvents::onLogout);
        bus.addListener(UndineEvents::onDeath);
        bus.addListener(UndineEvents::onDamaged);
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

    /** Her master took damage: a flinch, a surprised face and a worried line (by the mood rules, on their cooldown). One map lookup per player damage; nothing for anything else. */
    static void onDamaged(LivingDamageEvent.Post e) {
        if (UndineCasting.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || e.getNewDamage() < 0.5f || !UndineCasting.LIVE.contains(p.getUUID())) return;
        UndineCasting.mood(p, UndineMoodRules.Event.OWNER_HURT);
    }

    /** Her master killed something (a smile and a wave), or died (her last words, then everything is forgotten). */
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            UndineCasting.ownerDied(p);
            UndineCasting.forget(p);
        }
        if (!UndineCasting.LIVE.isEmpty() && e.getSource().getEntity() instanceof ServerPlayer killer && killer != e.getEntity() && UndineCasting.LIVE.contains(killer.getUUID())) UndineCasting.mood(killer, UndineMoodRules.Event.OWNER_KILL);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) UndineCasting.forget(p); }

    static void onStopping(ServerStoppingEvent e) { UndineCasting.clearAll(); }
}
