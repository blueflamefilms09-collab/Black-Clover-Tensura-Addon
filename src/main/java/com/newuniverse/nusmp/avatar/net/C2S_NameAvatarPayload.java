package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Why: the naming dialog (opened by right-clicking your own Avatar with an empty hand) sends which Avatar and the name; the server checks it is yours and near. */
public record C2S_NameAvatarPayload(UUID avatarId, String name) implements CustomPacketPayload {
    public static final Type<C2S_NameAvatarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_NameAvatarPayload> CODEC = StreamCodec.composite(
            net.minecraft.core.UUIDUtil.STREAM_CODEC, C2S_NameAvatarPayload::avatarId,
            ByteBufCodecs.stringUtf8(NetText.NAME_MAX), C2S_NameAvatarPayload::name,
            C2S_NameAvatarPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
