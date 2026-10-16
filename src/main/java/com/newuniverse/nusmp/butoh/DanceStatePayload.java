package com.newuniverse.nusmp.butoh;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: every client that sees a dancer has to play the same moves the server rolls evasion by, but sending a packet per move would be
 * wasteful and could arrive out of step. The server sends this once when the dance starts (the sequence seed and the game tick it began on)
 * and once when it ends; each client then walks the same {@link DanceChoreographer} sequence on its own clock.
 */
public record DanceStatePayload(int entity, boolean on, long seed, long startTick) implements CustomPacketPayload {
    public static final Type<DanceStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "butoh_dance_state"));
    public static final StreamCodec<FriendlyByteBuf, DanceStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeBoolean(p.on()); buf.writeLong(p.seed()); buf.writeLong(p.startTick()); },
            buf -> new DanceStatePayload(buf.readVarInt(), buf.readBoolean(), buf.readLong(), buf.readLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Mod-bus listener: the packet is client-bound only and optional (a client without the mod simply sees no dance). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.butoh.DanceClient.receive(p)));
    }
}
