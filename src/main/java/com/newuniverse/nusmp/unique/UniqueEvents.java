package com.newuniverse.nusmp.unique;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the game events of the Unique Guardian, each cheap for everybody else by construction: the player tick is one int compare for every player but one tick in twenty; an item entity joining or leaving a level is one
 * {@code instanceof} and (for an item entity only) a few item-class tests; a container close looks at that player only; the server tick is three modulo tests. Nothing here runs on the client.
 */
public final class UniqueEvents {
    private UniqueEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(UniqueEvents::onStarted);
        bus.addListener(UniqueEvents::onStopping);
        bus.addListener(UniqueEvents::onPlayerTick);
        bus.addListener(UniqueEvents::onServerTick);
        bus.addListener(EventPriority.LOWEST, UniqueEvents::onJoin);              // after anybody who may cancel it: only a join that happened is looked at
        bus.addListener(UniqueEvents::onLeave);
        bus.addListener(UniqueEvents::onContainerClose);
        bus.addListener(UniqueEvents::onLogin);
        bus.addListener(UniqueEvents::onLogout);
    }

    static void onStarted(ServerStartedEvent e) { UniqueGuardian.bind(e.getServer()); }

    static void onStopping(ServerStoppingEvent e) { UniqueGuardian.unbind(); }

    /** Once a second per player, spread by id: what they carry. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || !UniqueGuardian.active()) return;
        SafeCast.tick("unique.scan", () -> UniqueGuardian.scan(p, null));
    }

    static void onServerTick(ServerTickEvent.Post e) { SafeCast.tick("unique.tick", () -> UniqueGuardian.onServerTick(e.getServer())); }

    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel sl) || !(e.getEntity() instanceof ItemEntity ie) || !UniqueGuardian.active()) return;
        SafeCast.tick("unique.join", () -> {
            if (UniqueGuardian.joined(ie, sl)) e.setCanceled(true);
        });
    }

    static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel sl) || !(e.getEntity() instanceof ItemEntity ie) || !UniqueGuardian.active()) return;
        SafeCast.tick("unique.leave", () -> UniqueGuardian.left(ie, sl));
    }

    /** A container closed: what was left in it is where the copy is now. */
    static void onContainerClose(PlayerContainerEvent.Close e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || !UniqueGuardian.active()) return;
        SafeCast.tick("unique.close", () -> UniqueGuardian.scan(p, e.getContainer()));
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !UniqueGuardian.bound()) return;
        SafeCast.tick("unique.login", () -> HeroUnique.onLogin(p, UniqueGuardian.saved()));
    }

    /** A last look at what they carry (so the record says who held it), and the Holy Weapon holder's clock. */
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !UniqueGuardian.active()) return;
        SafeCast.tick("unique.logout", () -> {
            UniqueGuardian.scan(p, null);
            HeroUnique.onLogout(p, UniqueGuardian.saved());
        });
    }
}
