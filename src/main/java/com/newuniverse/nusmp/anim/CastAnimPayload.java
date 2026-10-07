package com.newuniverse.nusmp.anim;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 0.54: "this player plays this casting body animation" (an empty clip name = stop), sent to everyone who sees them. */
public record CastAnimPayload(int entity, String clip) implements CustomPacketPayload {
    public static final Type<CastAnimPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "cast_anim"));
    public static final StreamCodec<FriendlyByteBuf, CastAnimPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeUtf(p.clip(), 48); },
            buf -> new CastAnimPayload(buf.readVarInt(), buf.readUtf(48)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.CastAnimClient.receive(p.entity(), p.clip())));
    }
}
