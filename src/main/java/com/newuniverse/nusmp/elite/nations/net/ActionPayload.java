package com.newuniverse.nusmp.elite.nations.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the one client-to-server request of the nations screen: an action word, two short strings and a number, exactly the arguments of {@code NationActions.run}. The server checks the permission and the
 * rules itself, rate-limits the sender and answers with a chat line and a fresh snapshot; the strings are bounded on the wire. Payload id {@code nusmp:nation_action}.
 */
public record ActionPayload(String action, String a, String b, long n) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "nation_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), ActionPayload::action,
            ByteBufCodecs.stringUtf8(96), ActionPayload::a,
            ByteBufCodecs.stringUtf8(96), ActionPayload::b,
            ByteBufCodecs.VAR_LONG, ActionPayload::n,
            ActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
