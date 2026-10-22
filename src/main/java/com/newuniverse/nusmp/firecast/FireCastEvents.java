package com.newuniverse.nusmp.firecast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the fire casting styles listen to, all cheap and all server only: Mereoleona's landed punches (the damage event of the player's own unarmed melee attack), the
 * once-every-four-ticks upkeep of the few players that have a charge or a prop live, and the cleanup at logout, death, respawn, dimension change and server stop.
 */
public final class FireCastEvents {
    private FireCastEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(FireCastEvents::onPlayerTick);
        bus.addListener(FireCastEvents::onDamaged);
        bus.addListener(FireCastEvents::onHurt);
        bus.addListener(FireCastEvents::onLogout);
        bus.addListener(FireCastEvents::onDeath);
        bus.addListener(FireCastEvents::onRespawn);
        bus.addListener(FireCastEvents::onDimension);
        bus.addListener(FireCastEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with something live is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (FireCasting.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !FireCasting.LIVE.contains(p.getUUID())) return;
        FireCasting.upkeep(p);
    }

    /** A landed unarmed punch of a player: the melee attack damage type, delivered by the player themselves. */
    static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getSource().getDirectEntity() instanceof ServerPlayer p) || e.getNewDamage() <= 0f || !e.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        FireCasting.onPunch(p, e.getEntity());
        FireCasting.onOwnerMelee(p);
    }

    /** A player took damage: their Salamander (if they have one out) flinches. One map lookup for a player without one. */
    static void onHurt(LivingDamageEvent.Post e) {
        if (e.getNewDamage() > 0f && e.getEntity() instanceof ServerPlayer p && !p.isDeadOrDying()) FireCasting.onOwnerHurt(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        FireCasting.forget(p);
        SalamanderSounds.forget(p.getUUID());                                                  // the listener's sound gate record goes with them
    }

    /** A player died (their Salamander whimpers, then everything goes), or something died by a player's hand (the Salamander is proud). */
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            FireCasting.onOwnerDeath(p);
            FireCasting.forget(p);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer killer && killer != e.getEntity()) FireCasting.onOwnerKill(killer);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) FireCasting.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) FireCasting.forget(p); }

    static void onStopping(ServerStoppingEvent e) {
        FireCasting.clearAll();
        SalamanderSounds.clearAll();
    }
}
