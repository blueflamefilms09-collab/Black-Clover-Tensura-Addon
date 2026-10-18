package com.newuniverse.nusmp.skilltree.net;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.TreeConfig;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.rules.RateLimiter;
import com.newuniverse.nusmp.skilltree.rules.SyncQueue;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.PlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * Why: this is the whole network surface of the Skill Tree, in one place so the trust boundary is easy to see. NeoForge 21.1 has no {@code SimpleChannel}; the supported way is typed
 * {@code CustomPacketPayload} records with a {@code StreamCodec}, registered once in {@code RegisterPayloadHandlersEvent} on the mod bus with a protocol version ("1": a peer with another
 * layout is refused by the handshake rather than corrupting a stream) and marked {@code optional()} like every other payload of this mod. Direction decides trust. SERVER to CLIENT: the whole
 * tree ({@link TreeSyncPayload}, on login, respawn, dimension change, clone and after every change, coalesced so a burst of changes is one packet per player per tick by {@link SyncQueue}) and
 * all the routes ({@link RoutesPayload}, on join and on every datapack reload); the client only stores them. CLIENT to SERVER: a purchase ({@link UnlockNodePayload}) and a class choice
 * ({@link SetClassPayload}), which carry ids and nothing else: the handler re-validates everything through {@link SkillTreeApi}, drops the request when the player is not alive or has
 * exceeded {@code purchaseRateLimit} per second ({@link RateLimiter}, counted in server ticks), and tells the player why in one action-bar line when a refusal could be an honest race (two
 * quick clicks). Handlers run inside {@code enqueueWork} (on the main thread) and never throw: NeoForge disconnects a player whose handler throws, which is far too harsh for a refused
 * purchase. Nothing in the handlers touches a client-only class (the client side only stores into common classes), so no client proxy is needed and a dedicated server loads this file safely.
 * The two static collections below hold per-player bookkeeping only; they are emptied when a player leaves and when the server stops, so they never outlive a server.
 */
public final class TreeNet {
    private TreeNet() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The payload protocol version. */
    public static final String PROTOCOL = "1";

    private static final RateLimiter<UUID> LIMITER = new RateLimiter<>();
    private static final SyncQueue<UUID> QUEUE = new SyncQueue<>();

    // ---------------------------------------------------------------- registration (mod bus)
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToClient(TreeSyncPayload.TYPE, TreeSyncPayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> receiveTree(payload, ctx));
        });
        r.playToClient(RoutesPayload.TYPE, RoutesPayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> RouteRegistry.setClient(payload.routes()));
        });
        r.playToServer(UnlockNodePayload.TYPE, UnlockNodePayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> handleUnlock(payload, ctx));
        });
        r.playToServer(SetClassPayload.TYPE, SetClassPayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> handleSetClass(payload, ctx));
        });
    }

    // ---------------------------------------------------------------- client side: store what the server sent
    private static void receiveTree(TreeSyncPayload payload, IPayloadContext ctx) {
        try {
            Player player = ctx.player();
            if (player == null) return;
            PlayerSkillTree fresh = PlayerSkillTree.fromTag(payload.tree());
            IPlayerSkillTree live = SkillTreeAttachments.get(player);
            if (live instanceof PlayerSkillTree own) own.copyFrom(fresh);        // in place, so a screen holding the old object sees the new state
            else player.setData(SkillTreeAttachments.TREE, fresh);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: could not apply the tree the server sent", e);
        }
    }

    // ---------------------------------------------------------------- server side: the requests, re-validated
    private static void handleUnlock(UnlockNodePayload payload, IPayloadContext ctx) {
        try {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.isAlive() || !allow(player)) return;
            SkillTreeApi.UpgradeResult result = SkillTreeApi.upgradeNode(player, payload.route(), payload.node());
            if (result != SkillTreeApi.UpgradeResult.OK) tell(player, "tree.msg.refused." + result.name().toLowerCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: a purchase request failed", e);
        }
    }

    private static void handleSetClass(SetClassPayload payload, IPayloadContext ctx) {
        try {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.isAlive() || !allow(player)) return;
            var verdict = SkillTreeApi.switchClass(player, payload.classId(), false);
            switch (verdict) {
                case OK -> player.displayClientMessage(Component.translatable("tree.msg.class.set", payload.classId().replace('_', ' ')).withStyle(ChatFormatting.GOLD), true);
                case COOLDOWN -> player.displayClientMessage(Component.translatable("tree.msg.class.cooldown", (SkillTreeApi.classCooldownTicks(player) + 19) / 20).withStyle(ChatFormatting.RED), true);
                case NOT_UNLOCKED -> tell(player, "tree.msg.class.not_unlocked");
                case SAME -> { /* already that class: nothing to say */ }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: a class request failed", e);
        }
    }

    private static boolean allow(ServerPlayer player) {
        return LIMITER.allow(player.getUUID(), player.getServer().getTickCount(), TreeConfig.purchaseRateLimit());
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    // ---------------------------------------------------------------- server side: sending
    /** Queues a full sync of this player's tree for the end of the tick (any number of calls in a tick make one packet). */
    public static void markDirty(ServerPlayer player) {
        if (player != null && !(player instanceof FakePlayer)) QUEUE.mark(player.getUUID());
    }

    /** The end-of-tick flush: one {@link TreeSyncPayload} for each player marked since the last tick. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) return;
        try {
            var players = event.getServer().getPlayerList();
            for (UUID id : QUEUE.drain()) {
                ServerPlayer player = players.getPlayer(id);
                if (player != null) sendTree(player);
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: the sync flush failed", e);
        }
    }

    /** Sends this player's tree now (used by the flush; prefer {@link #markDirty}). */
    public static void sendTree(ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayer(player, new TreeSyncPayload(SkillTreeAttachments.get(player).toTag()));
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] skill tree: could not send the tree to {}", player.getGameProfile().getName(), e);          // no connection yet (a test world): nothing to show anyway
        }
    }

    /** Sends the routes the registry holds to these players. */
    public static void sendRoutes(Stream<ServerPlayer> players) {
        try {
            RoutesPayload payload = new RoutesPayload(RouteRegistry.all());
            players.forEach(p -> {
                try {
                    PacketDistributor.sendToPlayer(p, payload);
                } catch (RuntimeException | LinkageError e) {
                    LOG.debug("[nusmp] skill tree: could not send the routes to {}", p.getGameProfile().getName(), e);
                }
            });
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: could not send the routes", e);
        }
    }

    // ---------------------------------------------------------------- bookkeeping
    /** A player left: forget their rate-limit history and any queued sync. */
    public static void forget(UUID id) {
        LIMITER.forget(id);
        QUEUE.forget(id);
    }

    /** The server stopped: nothing here may outlive it. */
    public static void clear() {
        LIMITER.clear();
        QUEUE.clear();
    }
}
