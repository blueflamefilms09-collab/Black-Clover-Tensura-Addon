package com.newuniverse.nusmp.avatar.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the "Create" button. It carries only the creator's CHOICES (race id, personalities by name, the look, an optional name); the stats, luck and tax
 * are never in it, the server uses the roll it made itself (the token only tells it which roll the screen showed, so a stale screen is refused). The look is cleaned by {@code DynamicAppearanceEngine.sanitize} before use.
 */
public record C2S_FinalizeAvatarPayload(String raceId, String primary, String secondary, CompoundTag appearance, String name, long token) implements CustomPacketPayload {
    public static final Type<C2S_FinalizeAvatarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_finalize"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_FinalizeAvatarPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NetText.ID_MAX), C2S_FinalizeAvatarPayload::raceId,
            ByteBufCodecs.stringUtf8(NetText.ID_MAX), C2S_FinalizeAvatarPayload::primary,
            ByteBufCodecs.stringUtf8(NetText.ID_MAX), C2S_FinalizeAvatarPayload::secondary,
            ByteBufCodecs.COMPOUND_TAG, C2S_FinalizeAvatarPayload::appearance,
            ByteBufCodecs.stringUtf8(NetText.NAME_MAX), C2S_FinalizeAvatarPayload::name,
            ByteBufCodecs.VAR_LONG, C2S_FinalizeAvatarPayload::token,
            C2S_FinalizeAvatarPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
