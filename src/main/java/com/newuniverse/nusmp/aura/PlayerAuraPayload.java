package com.newuniverse.nusmp.aura;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 0.53: "this player has this aura for this long" (ticks 0 = it ends). Sent to everyone who can see the player. */
public record PlayerAuraPayload(int entity, int aura, int ticks, int style) implements CustomPacketPayload {
    public static final Type<PlayerAuraPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "player_aura"));
    public static final StreamCodec<FriendlyByteBuf, PlayerAuraPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeByte(p.aura()); buf.writeVarInt(p.ticks()); buf.writeByte(p.style()); },
            buf -> new PlayerAuraPayload(buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() ->
                com.newuniverse.nusmp.client.aura.PlayerAuraClient.receive(p.entity(), p.aura(), p.ticks(), p.style())));
    }
}
