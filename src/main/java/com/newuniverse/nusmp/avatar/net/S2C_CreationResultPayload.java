package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Why: the server's verdict on a request, with the reason when it refused ("create", "roll" or "name" says which request), so the screen can show it instead of failing silently. */
public record S2C_CreationResultPayload(boolean ok, String kind, String message) implements CustomPacketPayload {
    public static final Type<S2C_CreationResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_CreationResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, S2C_CreationResultPayload::ok,
            ByteBufCodecs.stringUtf8(16), S2C_CreationResultPayload::kind,
            ByteBufCodecs.stringUtf8(256), S2C_CreationResultPayload::message,
            S2C_CreationResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
