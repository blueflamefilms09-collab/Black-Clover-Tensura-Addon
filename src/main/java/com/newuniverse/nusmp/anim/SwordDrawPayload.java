package com.newuniverse.nusmp.anim;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 0.52: "this player is drawing a sword from their grimoire" (style 0 Anti-Magic, 1 Sword Magic), sent to everyone who sees them. */
public record SwordDrawPayload(int entity, int style) implements CustomPacketPayload {
    public static final Type<SwordDrawPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "sword_draw"));
    public static final StreamCodec<FriendlyByteBuf, SwordDrawPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeByte(p.style()); },
            buf -> new SwordDrawPayload(buf.readVarInt(), buf.readByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.SwordDrawClient.start(p.entity(), p.style())));
    }
}
