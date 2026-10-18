package com.newuniverse.nusmp.skilltree.line;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.net.RoutesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is the server half of the VISIBILITY LOCK. A player is sent the full trees of the lines they OWN and nothing else: not the other lines' nodes, not their numbers. What every client does get is
 * the cards of all lines (see {@link LineCard}: names, colours, books, never nodes), because Skill Books, the Black Market and the creative tab must name a line the player has not unlocked. Which routes
 * a player holds is worked out by the pure {@link LineSyncPlan}; this class only sends them: the first packet REPLACES the client's copy (the old {@code RoutesPayload}, so an older client still
 * understands the first chunk), further packets ADD to it ({@link LinePayloads.More}). It re-sends the owned routes only when the owned SET changes (a book was read, a line was taken away): the
 * signature of the last set sent is kept per player in one small map that is emptied when the player leaves and when the server stops. An administrator (an operator, or a creative player) may ask for the full
 * tree of any single line to inspect it; the server checks the permission each time, so a normal client that sends the request is ignored. Packets are sent inside a catch because a player with no
 * connection yet (a test world, a login in progress) must not break the tree sync that called this.
 */
public final class LineSync {
    private LineSync() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    /** An operator or a creative player: the people who may inspect lines they do not own. */
    public static boolean isAdmin(ServerPlayer p) { return p.hasPermissions(2) || p.isCreative(); }

    /** Sends the cards of every line to these players (the login, a reload). */
    public static void sendCards(ServerPlayer p) {
        try {
            if (p instanceof FakePlayer) return;
            PacketDistributor.sendToPlayer(p, new LinePayloads.Cards(RouteRegistry.snapshot().cards()));
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] lines: could not send the cards to {}", p.getGameProfile().getName(), e);
        }
    }

    /** The routes this player owns, in id order (what a sync carries). */
    public static List<SkillRoute> ownedRoutes(ServerPlayer p) { return LineSyncPlan.owned(SkillTreeApi.of(p).unlockedRoutes(), RouteRegistry::byId); }

    /** Replaces the player's copy of the routes with the ones they own, in as many packets as the limits need. */
    public static void sendOwned(ServerPlayer p) {
        try {
            if (p instanceof FakePlayer) return;
            List<SkillRoute> owned = ownedRoutes(p);
            List<List<SkillRoute>> packets = LineSyncPlan.packets(owned);
            PacketDistributor.sendToPlayer(p, new RoutesPayload(packets.isEmpty() ? List.of() : packets.get(0)));
            for (int i = 1; i < packets.size(); i++) PacketDistributor.sendToPlayer(p, new LinePayloads.More(packets.get(i)));
            SENT.put(p.getUUID(), LineSyncPlan.signature(SkillTreeApi.of(p).unlockedRoutes()));
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] lines: could not send the owned routes to {}", p.getGameProfile().getName(), e);
        }
    }

    /** Called after every tree sync: sends the owned routes again only if the set of owned lines is not the one the player last received. */
    public static void afterTree(ServerPlayer p) {
        try {
            if (p instanceof FakePlayer) return;
            Integer sent = SENT.get(p.getUUID());
            if (sent == null || sent != LineSyncPlan.signature(SkillTreeApi.of(p).unlockedRoutes())) sendOwned(p);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] lines: the route sync check failed", e);
        }
    }

    /** An administrator asks for the full tree of one line. A request from anybody else is ignored. */
    public static boolean sendInspect(ServerPlayer p, String routeId) {
        if (!isAdmin(p)) return false;
        SkillRoute r = RouteRegistry.byId(routeId);
        if (r == null) return false;
        try {
            PacketDistributor.sendToPlayer(p, new LinePayloads.More(List.of(r)));
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] lines: could not send {} to {}", routeId, p.getGameProfile().getName(), e);
            return false;
        }
    }

    public static void forget(UUID id) { SENT.remove(id); }

    public static void clear() { SENT.clear(); }
}
