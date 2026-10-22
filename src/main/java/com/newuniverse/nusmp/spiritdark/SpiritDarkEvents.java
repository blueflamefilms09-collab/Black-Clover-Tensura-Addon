package com.newuniverse.nusmp.spiritdark;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events the Spirit of Darkness listens to, all cheap and all server only: a melee strike of a linked owner (the damage event: life steal and the mark), the owner being hurt or
 * killing something (the moods), the once-every-four-ticks upkeep of the few players who have a spirit out, and the cleanup at logout, death, respawn, dimension change and server stop. Every
 * handler returns at once when nobody has a spirit ({@link SpiritDark#STATES} empty), so the cost for a server without linked spirits is one map check per damage event.
 */
public final class SpiritDarkEvents {
    private SpiritDarkEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SpiritDarkEvents::onPlayerTick);
        bus.addListener(SpiritDarkEvents::onDamaged);
        bus.addListener(SpiritDarkEvents::onDeath);
        bus.addListener(SpiritDarkEvents::onLogout);
        bus.addListener(SpiritDarkEvents::onRespawn);
        bus.addListener(SpiritDarkEvents::onDimension);
        bus.addListener(SpiritDarkEvents::onStopping);
    }

    /** The upkeep: one set lookup per player per tick, and only a player with a spirit out is visited, every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (SpiritDark.LIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 0 || !SpiritDark.LIVE.contains(p.getUUID())) return;
        SpiritDark.upkeep(p);
    }

    /** A landed melee strike of a player, or the owner being hurt. */
    static void onDamaged(LivingDamageEvent.Post e) {
        if (SpiritDark.STATES.isEmpty() || e.getNewDamage() <= 0f) return;
        if (e.getSource().getDirectEntity() instanceof ServerPlayer p && e.getSource().is(DamageTypes.PLAYER_ATTACK)) SpiritDark.onStrike(p, e.getEntity(), e.getNewDamage());
        if (e.getEntity() instanceof ServerPlayer owner) SpiritDark.onOwnerHurt(owner);
    }

    static void onDeath(LivingDeathEvent e) {
        if (SpiritDark.STATES.isEmpty()) return;
        LivingEntity dead = e.getEntity();
        if (dead instanceof ServerPlayer p) { SpiritDark.onOwnerDeath(p); return; }
        if (e.getSource().getEntity() instanceof ServerPlayer killer) SpiritDark.onOwnerKill(killer);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritDark.forget(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritDark.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritDark.forget(p); }

    static void onStopping(ServerStoppingEvent e) { SpiritDark.clearAll(); }
}
