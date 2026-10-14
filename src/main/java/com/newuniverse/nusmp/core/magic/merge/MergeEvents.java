package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The game-bus listeners of the merge generator. One line in NUSMP registers them all: {@code MergeEvents.register(NeoForge.EVENT_BUS)}. */
public final class MergeEvents {
    private MergeEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MergeEvents::onServerStarted);
        gameBus.addListener(MergeEvents::onServerStopping);
        gameBus.addListener(MergeEvents::onLogin);
        gameBus.addListener(MergeEvents::onPlayerTick);
    }

    /** The world's merged magics become readable (profiles registered); the startup audit of the magic framework also runs here. */
    public static void onServerStarted(ServerStartedEvent e) {
        try {
            MergedMagicData.get(e.getServer()).publish();
            com.newuniverse.nusmp.core.magic.profile.MagicAuditRuntime.runAtStartup();
        } catch (RuntimeException ex) {
            SafeCast.fault("merge-load", ex);
        }
    }

    /** Leaving the world: nothing of its merged magics stays in the static registries, and the fault ledger starts clean. */
    public static void onServerStopping(ServerStoppingEvent e) {
        MergedRegistry.clear();
        SafeCast.reset();
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        try {
            MergedSync.sendTo(p, MergedMagicData.get(p.getServer()));
            MergeAccess.tick(p);
        } catch (RuntimeException ex) {
            SafeCast.fault("merge-login", ex);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || MergedRegistry.size() == 0) return;
        try {
            if (p.tickCount % 100 == 11) MergeAccess.tick(p);
            else if (p.tickCount % 40 == 23) MergeAccess.unlockPages(p);
        } catch (RuntimeException ex) {
            SafeCast.fault("merge-tick", ex);
        }
    }
}
