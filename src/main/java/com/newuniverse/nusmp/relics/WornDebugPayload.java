package com.newuniverse.nusmp.relics;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server to client, one operator: {@code /multiverse relic debug} printed the server's half of the report and now asks the operator's own client for the other half (what the layer
 * on that client found and decided for {@code target}). {@code serverLines} is the server's list, one "id|SLOT|WHERE" entry per worn relic, so the client can say what the server
 * sees that it does not.
 */
public record WornDebugPayload(UUID target, List<String> serverLines) implements CustomPacketPayload {
    public static final Type<WornDebugPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_worn_debug"));
    public static final int MAX_LINES = 64;

    public static final StreamCodec<RegistryFriendlyByteBuf, WornDebugPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.target());
                int n = Math.min(MAX_LINES, p.serverLines().size());
                buf.writeVarInt(n);
                for (int i = 0; i < n; i++) ByteBufCodecs.STRING_UTF8.encode(buf, p.serverLines().get(i));
            },
            buf -> {
                UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                if (n < 0 || n > MAX_LINES) throw new IllegalStateException("worn debug list too long: " + n);
                List<String> lines = new ArrayList<>(n);
                for (int i = 0; i < n; i++) lines.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                return new WornDebugPayload(id, lines);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
