package com.newuniverse.nusmp.entity.riven;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Riven's state for the client's boss bar: phase, health fraction, story charge, and the skill he is casting (name, cast length in
 * ticks). Sent to everyone tracking him when something changes, and every 10 ticks otherwise; the bar interpolates between them.
 */
public record RivenStatePayload(int entity, int phase, float hp, int story, String cast, int castTicks, int maxConstructs) implements CustomPacketPayload {
    public static final Type<RivenStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "riven_state"));
    public static final StreamCodec<FriendlyByteBuf, RivenStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeByte(p.phase()); buf.writeFloat(p.hp()); buf.writeByte(p.story());
                          buf.writeUtf(p.cast(), 64); buf.writeVarInt(p.castTicks()); buf.writeByte(p.maxConstructs()); },
            buf -> new RivenStatePayload(buf.readVarInt(), buf.readByte(), buf.readFloat(), buf.readByte(), buf.readUtf(64), buf.readVarInt(), buf.readByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.riven.RivenClientState.accept(p)));
    }
}
