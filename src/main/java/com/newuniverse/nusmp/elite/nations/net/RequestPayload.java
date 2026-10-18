package com.newuniverse.nusmp.elite.nations.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: client-to-server "I opened / closed / want a refresh of the nations screen". {@code watch} true = send me snapshots while I look; false = stop. It carries no data to trust. Payload id
 * {@code nusmp:nation_request}.
 */
public record RequestPayload(boolean watch, int tab) implements CustomPacketPayload {
    public static final Type<RequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "nation_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RequestPayload::watch,
            ByteBufCodecs.VAR_INT, RequestPayload::tab,
            RequestPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
