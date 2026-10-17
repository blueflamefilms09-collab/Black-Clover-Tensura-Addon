package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: page 3 of the Soul Forge shows a sample "bark" (a line the Avatar would say) for the chosen personalities. The chatter lines are datapack data that
 * only the server has, so the screen asks for one: the primary and the optional secondary personality by name, nothing else. The answer comes back as a
 * result packet of kind "bark".
 */
public record C2S_BarkRequestPayload(String primary, String secondary) implements CustomPacketPayload {
    public static final Type<C2S_BarkRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_bark"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_BarkRequestPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(NetText.clean(p.primary, NetText.ID_MAX), NetText.ID_MAX); b.writeUtf(NetText.clean(p.secondary, NetText.ID_MAX), NetText.ID_MAX); },
            b -> new C2S_BarkRequestPayload(b.readUtf(NetText.ID_MAX), b.readUtf(NetText.ID_MAX)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
