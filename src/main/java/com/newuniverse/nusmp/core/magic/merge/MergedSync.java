package com.newuniverse.nusmp.core.magic.merge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * nusmp:merged_magic_sync: the world's merged magics (the encoded lines of the {@link MergeStore}) to a client, so everyone renders them:
 * on login and after every change. The client simply publishes them to {@link MergedRegistry}.
 */
public record MergedSync(List<String> lines) implements CustomPacketPayload {
    public static final Type<MergedSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "merged_magic_sync"));
    public static final StreamCodec<FriendlyByteBuf, MergedSync> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.lines().size()); for (String s : p.lines()) buf.writeUtf(s, 1024); },
            buf -> {
                int n = Math.min(buf.readVarInt(), MergeLimits.MAX_MERGED + 1);
                List<String> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(buf.readUtf(1024));
                return new MergedSync(l);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Mod-bus listener: registers the packet (client-bound, optional: a client without it just sees the parents' effects). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> MergedRegistry.replaceFrom(p.lines())));
    }

    public static void sendTo(ServerPlayer player, MergedMagicData data) { PacketDistributor.sendToPlayer(player, new MergedSync(data.lines())); }

    public static void sendToAll(MinecraftServer server, MergedMagicData data) { PacketDistributor.sendToAllPlayers(new MergedSync(data.lines())); }
}
