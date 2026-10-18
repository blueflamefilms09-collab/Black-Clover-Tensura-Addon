package com.newuniverse.nusmp.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.data.RouteLoader;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.net.TreeNet;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Why: the engine has to keep three things true without anyone remembering to call it: the routes are loaded and reach every client (on server start, on {@code /reload}, when a player joins);
 * a player's tree reaches their client and their ManasCore skills match it (on login, respawn, a dimension change, the move to a new body); and nothing outlives the server (the static route set,
 * the rate limits, the queued syncs). These are the game-bus events that do it, in one small class so the list is easy to audit. Each handler is guarded: an event handler that throws would
 * break login or a reload for everybody, and a failed housekeeping step costs a player nothing the next login does not repair. Death keeps everything because the tree attachment is
 * {@code copyOnDeath}; this only has to tell the new body's client and re-teach its skills. Registered once by {@code TreeCore.init}.
 */
final class TreeEvents {
    private static final Logger LOG = LogUtils.getLogger();

    private TreeEvents() {}

    static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(TreeEvents::onAddReloadListeners);
        bus.addListener(TreeEvents::onDatapackSync);
        bus.addListener(TreeEvents::onLogin);
        bus.addListener(TreeEvents::onRespawn);
        bus.addListener(TreeEvents::onDimension);
        bus.addListener(TreeEvents::onClone);
        bus.addListener(TreeEvents::onLogout);
        bus.addListener(TreeEvents::onServerStopped);
        bus.addListener(TreeNet::onServerTick);
    }

    /** The datapack reload: the route loader joins it (server start and every /reload). */
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        try {
            event.addListener(new RouteLoader());
        } catch (RuntimeException e) {
            LOG.error("[nusmp] skill tree: could not add the route loader", e);
        }
    }

    /** A player joined (event player set) or the datapacks reloaded (no player: everyone): the routes go to the relevant players, and after a reload their skills are checked against the new trees. */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            TreeNet.sendRoutes(event.getRelevantPlayers());
            if (event.getPlayer() == null) event.getRelevantPlayers().forEach(SkillTreeApi::reconcileSkills);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: the datapack sync failed", e);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillTreeApi.onJoin(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillTreeApi.onJoin(player);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillTreeApi.resync(player);
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillTreeApi.resync(player);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TreeNet.forget(event.getEntity().getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        RouteRegistry.clear();
        TreeNet.clear();
        SkillGrants.clear();
    }
}
