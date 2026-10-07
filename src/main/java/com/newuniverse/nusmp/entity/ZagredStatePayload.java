package com.newuniverse.nusmp.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 0.48: Zagred's state for the client: what it is doing (idle / stalking / in combat / casting), the word it is about to speak,
 * whom it is aiming at (the reticle) and its phase (the evolution of the fight). Sent to everyone tracking it when it changes.
 */
public record ZagredStatePayload(int entity, int state, int word, int target, int phase) implements CustomPacketPayload {
    public static final Type<ZagredStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "zagred_state"));
    public static final StreamCodec<FriendlyByteBuf, ZagredStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeByte(p.state()); buf.writeByte(p.word()); buf.writeVarInt(p.target()); buf.writeByte(p.phase()); },
            buf -> new ZagredStatePayload(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level().getEntity(p.entity()) instanceof ZagredBossEntity z) z.applyState(p.state(), p.word(), p.target());
        }));
    }
}
