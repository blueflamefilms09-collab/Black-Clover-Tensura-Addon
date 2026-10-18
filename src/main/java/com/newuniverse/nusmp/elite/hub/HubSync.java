package com.newuniverse.nusmp.elite.hub;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the quest overlay is drawn from what the server tells the client, and a busy fight changes a kill counter many times a second. So a change only marks the player dirty, and one server-tick
 * handler sends at most one {@link HubNet.HudSync} per dirty player every {@value #GAP_TICKS} ticks, carrying everything that changed meanwhile: a fight is a trickle, not a flood, and the first
 * change after a quiet spell goes out within half a second. A player without a connection (a fake player of a self-test) is skipped. The table is per server run and cleared when the server stops.
 */
public final class HubSync {
    private HubSync() {}

    /** Ticks between two syncs of the overlay. */
    public static final int GAP_TICKS = 10;
    private static final Set<UUID> DIRTY = new HashSet<>();

    /** A real player: a connection to send to, and not a fake player of a test or another mod. */
    public static boolean hasConnection(ServerPlayer p) { return p != null && !(p instanceof FakePlayer) && p.connection != null; }

    /** The player's overlay needs sending. */
    public static synchronized void request(ServerPlayer p) {
        if (hasConnection(p)) DIRTY.add(p.getUUID());
    }

    /** Server tick: sends the overlays that are due. Cheap when nobody is dirty. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % GAP_TICKS != 4) return;
        Set<UUID> due;
        synchronized (HubSync.class) {
            if (DIRTY.isEmpty()) return;
            due = new HashSet<>(DIRTY);
            DIRTY.clear();
        }
        for (UUID id : due) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && hasConnection(p)) PacketDistributor.sendToPlayer(p, new HubNet.HudSync(QuestService.hud(p)));
        }
    }

    public static synchronized void forget(UUID id) { DIRTY.remove(id); }

    public static synchronized void clear() { DIRTY.clear(); }
}
