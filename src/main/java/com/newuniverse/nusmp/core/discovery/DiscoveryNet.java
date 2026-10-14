package com.newuniverse.nusmp.core.discovery;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The one packet of the discovery system, {@code nusmp:discovery_sync} (server to client). The first packet of a login carries
 * {@code replace = true} and the first chunk of the player's keys; the next chunks (if any) and every later change are deltas
 * ({@code replace = false}). Every packet carries the viewer's show-all state (creative mode, the config). {@code announce} marks a delta of
 * things found just now. Register {@link #registerPayloads} on the mod bus.
 */
public final class DiscoveryNet {
    private DiscoveryNet() {}

    public record Sync(boolean replace, boolean showAll, boolean announce, List<String> keys) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "discovery_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.replace());
                    buf.writeBoolean(p.showAll());
                    buf.writeBoolean(p.announce());
                    int n = Math.min(p.keys().size(), DiscoveryRules.CHUNK);
                    buf.writeVarInt(n);
                    for (int i = 0; i < n; i++) buf.writeUtf(p.keys().get(i), DiscoveryRules.MAX_KEY_LENGTH);
                },
                buf -> {
                    boolean replace = buf.readBoolean(), all = buf.readBoolean(), announce = buf.readBoolean();
                    int n = Math.max(0, Math.min(buf.readVarInt(), DiscoveryRules.CHUNK));
                    List<String> keys = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) keys.add(buf.readUtf(DiscoveryRules.MAX_KEY_LENGTH));
                    return new Sync(replace, all, announce, keys);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only touches the side-neutral cache, so it is safe on every side. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> DiscoveryCache.receive(payload.replace(), payload.showAll(), payload.announce(), payload.keys())));
    }

    /** The whole set to one player (login, respawn): chunked, the first chunk replaces. */
    public static void sendFull(ServerPlayer p, DiscoveryData data, boolean showAll) {
        List<List<String>> chunks = DiscoveryRules.chunks(data.snapshot());
        for (int i = 0; i < chunks.size(); i++) PacketDistributor.sendToPlayer(p, new Sync(i == 0, showAll, false, chunks.get(i)));
    }

    /** New keys (found just now) and the current show-all state. */
    public static void sendDelta(ServerPlayer p, List<String> keys, boolean showAll) {
        for (List<String> chunk : DiscoveryRules.chunks(keys)) PacketDistributor.sendToPlayer(p, new Sync(false, showAll, true, chunk));
    }

    /** Only the show-all state changed (creative mode entered or left, the config flipped). */
    public static void sendState(ServerPlayer p, boolean showAll) {
        PacketDistributor.sendToPlayer(p, new Sync(false, showAll, false, List.of()));
    }
}
