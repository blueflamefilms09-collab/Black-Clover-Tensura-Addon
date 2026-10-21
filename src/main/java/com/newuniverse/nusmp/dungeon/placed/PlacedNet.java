package com.newuniverse.nusmp.dungeon.placed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one small server-to-client packet of the player-placed exemption (optional: a client without the mod simply never starts a crack on a placed
 * block, and the server still lets the break through). {@code op} REPLACE sets the marks of one chunk to exactly the list sent (sent when a chunk is sent to a
 * player, and empty when it is dropped), ADD and REMOVE change single marks (sent to everyone tracking the chunk). A chunk holds at most the per-chunk cap
 * (4096 by default) of 8-byte keys, 32 kB at the very most, far under the packet limit; an ordinary chunk sends nothing at all.
 */
public final class PlacedNet {
    private PlacedNet() {}

    public static final int REPLACE = 0, ADD = 1, REMOVE = 2;

    public record PlacedPayload(String dim, int op, int cx, int cz, long[] keys) implements CustomPacketPayload {
        public static final Type<PlacedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "dungeon_placed"));
        public static final StreamCodec<FriendlyByteBuf, PlacedPayload> CODEC = CustomPacketPayload.codec(PlacedPayload::write, PlacedPayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeUtf(dim, 128);
            b.writeByte(op);
            b.writeInt(cx);
            b.writeInt(cz);
            b.writeLongArray(keys);
        }

        private static PlacedPayload read(FriendlyByteBuf b) { return new PlacedPayload(b.readUtf(128), b.readByte(), b.readInt(), b.readInt(), b.readLongArray()); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What a client does with a packet (public so a self-test can drive it without a connection). */
    public static void apply(PlacedPayload p) {
        switch (p.op()) {
            case REPLACE -> PlacedMirror.replaceChunk(p.dim(), p.cx(), p.cz(), p.keys());
            case ADD -> PlacedMirror.add(p.dim(), p.keys());
            case REMOVE -> PlacedMirror.remove(p.dim(), p.keys());
            default -> { /* a newer server: ignored */ }
        }
    }

    /** Mod-bus listener: registers the packet (client-bound only, optional). */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(PlacedPayload.TYPE, PlacedPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> apply(p)));
    }

    // ---------------------------------------------------------------- server side senders (never throw: a missing connection means nobody to tell)
    private static String dim(ServerLevel level) { return level.dimension().location().toString(); }

    /** The marks of one chunk to one player (or, with {@code empty}, the order to forget them). */
    public static void sendChunk(ServerPlayer to, ServerLevel level, int cx, int cz, long[] keys) {
        try { PacketDistributor.sendToPlayer(to, new PlacedPayload(dim(level), REPLACE, cx, cz, keys)); } catch (RuntimeException noConnection) { /* nobody to tell */ }
    }

    /** The marks of one chunk to everyone who tracks it (after a bulk change such as a chunk or box clear). */
    public static void sendChunkToTrackers(ServerLevel level, int cx, int cz, long[] keys) {
        try { PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(cx, cz), new PlacedPayload(dim(level), REPLACE, cx, cz, keys)); } catch (RuntimeException noConnection) { /* nobody to tell */ }
    }

    /** One mark added or removed, to everyone who tracks its chunk. */
    public static void sendDelta(ServerLevel level, long key, boolean added) {
        try {
            int cx = PlacedSet.unpackX(key) >> 4, cz = PlacedSet.unpackZ(key) >> 4;
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(cx, cz), new PlacedPayload(dim(level), added ? ADD : REMOVE, cx, cz, new long[]{key}));
        } catch (RuntimeException noConnection) { /* nobody to tell */ }
    }
}
