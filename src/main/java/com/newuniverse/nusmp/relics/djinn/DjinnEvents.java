package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Every game-bus listener of the Djinn system in one place. Call {@code DjinnEvents.register(NeoForge.EVENT_BUS)} once from the mod constructor.
 */
public final class DjinnEvents {
    private DjinnEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(DjinnEngine::onPlayerTick);
        bus.addListener(DjinnEngine::onStartTracking);
        bus.addListener(DjinnEvents::onIncoming);
        bus.addListener(EventPriority.LOWEST, DjinnEvents::onDeath);
        bus.addListener(DjinnEvents::onLogin);
        bus.addListener(DjinnEvents::onLogout);
        bus.addListener(DjinnEvents::onRespawn);
        bus.addListener(DjinnEvents::onDimension);
        bus.addListener(DjinnEvents::onServerStopping);
        bus.addListener(DjinnSources::onCollapse);
        bus.addListener(DjinnSources::onServerTick);
        bus.addListener(DjinnSources::onContainerOpen);
        com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponEvents.register(bus);                                  // the temporary Djinn weapons: enforcement, guards, the Brand mark
        com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn.register(bus);                  // the Dark Djinn: Holy Water cleanses it, Lucius may drop a shard
    }

    /** The bearer's blows (Half Equip) and the bearer's elemental calm. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DjinnMagic.onIncoming(e);
        DjinnMagic.onOutgoing(e);
    }

    /**
     * A player dies: whatever phase they had out ends, but the bond stays. An owner keeps the Djinn (the record is copied on death and the vessel keeps
     * its binding, so the vessel they drop is still theirs and the Djinn is there again once they hold it), a household member keeps what was lent.
     * Only {@code /multiverse djinn release}, a deliberate release or an admin ends a bond. Runs last, and not at all if another mod cancelled the death.
     */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DjinnState s = DjinnState.of(p);
        if (s.owns() || s.lent() || s.phase() != DjinnPhaseRules.OFF) DjinnEngine.shutdown(p, DjinnPhaseRules.EndReason.DEATH);
        if (s.owns())
            p.displayClientMessage(Component.literal(s.djinn().fullName() + " waits in its vessel. Take the vessel back to call it again.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DjinnEngine.restore(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DjinnEngine.onLogout(p);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DjinnEngine.restore(p);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DjinnEngine.onDimension(p);
    }

    static void onServerStopping(ServerStoppingEvent e) { DjinnEngine.onServerStopping(); }
}
