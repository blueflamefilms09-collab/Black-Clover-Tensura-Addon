package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Why: the dice button. Carries nothing: the client asks, the server rolls (it never reads stats from a client). */
public record C2S_RequestStatRollPayload() implements CustomPacketPayload {
    public static final Type<C2S_RequestStatRollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_request_roll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_RequestStatRollPayload> CODEC = StreamCodec.unit(new C2S_RequestStatRollPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
