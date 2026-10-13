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
    }

    /** The bearer's blows (Half Equip) and the bearer's elemental calm. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DjinnMagic.onIncoming(e);
        DjinnMagic.onOutgoing(e);
    }

    /**
     * An owner dies: the Djinn leaves, its Seal falls where they fell (never lost, never burned), the household is called back. A household member
     * who dies only loses the phase they had out. Runs last, and not at all if another mod cancelled the death.
     */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DjinnState s = DjinnState.of(p);
        if (s.owns()) {
            Djinn d = s.djinn();
            Vec3 at = p.position().add(0, 0.5, 0);
            ItemStack seal = DjinnBinding.releaseOwned(p);
            if (!seal.isEmpty()) {
                DjinnSources.dropSeal(p.serverLevel(), at, seal);
                if (p.getServer() != null)
                    p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " has fallen, and " + d.fullName()
                            + " leaves them. Its Seal lies where they died.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        } else if (s.lent() || s.phase() != DjinnPhaseRules.OFF) {
            DjinnEngine.shutdown(p, DjinnPhaseRules.EndReason.DEATH);
        }
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
