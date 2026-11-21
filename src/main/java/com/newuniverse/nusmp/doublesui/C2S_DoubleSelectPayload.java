package com.newuniverse.nusmp.doublesui;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Why: the one thing the HUD asks the server: move the selection. {@code op} 0 = next, 1 = previous (the server walks its own list; {@code id} is ignored), 2 = select the double
 * with {@code id}. The server checks the sender's own doubles, the level and the range, and rate limits it ({@link DoublesRoster#select}); the client never says what a double is.
 */
public record C2S_DoubleSelectPayload(byte op, UUID id) implements CustomPacketPayload {
    public static final byte NEXT = 0, PREV = 1, SET = 2;
    public static final Type<C2S_DoubleSelectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "doubles_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DoubleSelectPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, C2S_DoubleSelectPayload::op, UUIDUtil.STREAM_CODEC, C2S_DoubleSelectPayload::id, C2S_DoubleSelectPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
