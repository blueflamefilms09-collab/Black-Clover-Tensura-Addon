package com.newuniverse.nusmp.undine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one packet of the Undine. The client only asks (the Undine key went down); the server decides everything: the sender must be a real player, a Noelle caster with the switch on, and at most one
 * ask per {@link UndineRules#PACKET_GAP} ticks is answered ({@link UndineCasting#toggle}). Everything the Undine draws travels as an ordinary prop, so this is the only new traffic: one byte per key press,
 * client to server. Register {@link #registerPayloads} on the mod bus.
 */
public final class UndineNet {
    private UndineNet() {}

    /** The Undine key was pressed (the action is reserved: 0 = toggle the link). */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "undine_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new Key(buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onKey(ServerPlayer p, int action) {
        if (action != 0) return;                                           // an unknown action is ignored, never trusted
        UndineCasting.toggle(p);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action()); }));
    }
}
