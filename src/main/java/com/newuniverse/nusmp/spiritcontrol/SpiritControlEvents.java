package com.newuniverse.nusmp.spiritcontrol;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game events of the spirit control plane, all server only and cheap: the combat tick (one map lookup per player per tick, and nothing at all while nobody has combat mode on), and the cleanup at
 * logout, death, dimension change and server stop (combat mode is the spirits' own kind of state: it ends where theirs ends). It also adds the short {@code /spirit} alias of the command tree.
 */
public final class SpiritControlEvents {
    private SpiritControlEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(SpiritControlEvents::onPlayerTick);
        bus.addListener(SpiritControlEvents::onLogout);
        bus.addListener(SpiritControlEvents::onDeath);
        bus.addListener(SpiritControlEvents::onRespawn);
        bus.addListener(SpiritControlEvents::onDimension);
        bus.addListener(SpiritControlEvents::onStopping);
        bus.addListener(SpiritControlEvents::onCommands);
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (SpiritCombat.ACTIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || !SpiritCombat.ACTIVE.containsKey(p.getUUID())) return;
        SpiritCombat.tick(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritControl.forget(p.getUUID()); }

    /** The owner died: combat mode ends (the spirits' own links end with the death too). The pools stay: a respawn is not a refill. */
    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritCombat.stop(p.getUUID()); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritCombat.stop(p.getUUID()); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) SpiritCombat.stop(p.getUUID()); }

    static void onStopping(ServerStoppingEvent e) { SpiritControl.clearAll(); }

    static void onCommands(RegisterCommandsEvent e) { SpiritControlCommand.registerAlias(e.getDispatcher()); }
}
