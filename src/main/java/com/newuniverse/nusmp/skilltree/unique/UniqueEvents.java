package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.data.RouteLoader;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Why: the unique skills need three things to stay true without anyone remembering to call them: the line data (the arts and passives, in {@code data/nusmp/skill_trees}) is loaded on server start and on every
 * {@code /reload}; a player's old Skill Tree record is moved into unique skills when they log in or come back from the dead ({@link LegacyMigration}); and nothing outlives the server (the static route set). These are the
 * game-bus events that do it, in one small class so the list is easy to audit. Each handler is guarded: an event handler that throws would break login or a reload for everybody. The client needs none of this: it
 * reads only the shipped table of books. Registered once by {@code SkillBookSystem.init}.
 */
public final class UniqueEvents {
    private static final Logger LOG = LogUtils.getLogger();
    private static boolean done;

    private UniqueEvents() {}

    public static synchronized void register() {
        if (done) return;
        done = true;
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(UniqueEvents::onAddReloadListeners);
        bus.addListener(UniqueEvents::onLogin);
        bus.addListener(UniqueEvents::onRespawn);
        bus.addListener(UniqueEvents::onServerStopped);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        try {
            event.addListener(new RouteLoader());
        } catch (RuntimeException e) {
            LOG.error("[nusmp] skill books: could not add the line data loader", e);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LegacyMigration.onJoin(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LegacyMigration.onJoin(player);
    }

    private static void onServerStopped(ServerStoppedEvent event) { RouteRegistry.clear(); }
}
