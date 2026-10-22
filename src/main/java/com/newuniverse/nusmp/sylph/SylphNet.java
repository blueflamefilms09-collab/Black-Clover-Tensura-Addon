package com.newuniverse.nusmp.sylph;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one packet of the Sylph. The client only asks (the Sylph key went down); the server decides everything: the sender must be a real player, Yuno's grimoire must be bound to them, the
 * switch must be on, and at most one ask per {@link SylphRules#PACKET_GAP} ticks is answered ({@link SylphCasting#toggle}). Everything the Sylph draws travels as an ordinary prop and as the cast
 * animation packet, so this is the only new traffic: one byte per key press, client to server. Register {@link #registerPayloads} on the mod bus.
 */
public final class SylphNet {
    private SylphNet() {}

    /** The Sylph key was pressed (the action is reserved: 0 = toggle the link). */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "sylph_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new Key(buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onKey(ServerPlayer p, int action) {
        if (action != 0) return;                                           // an unknown action is ignored, never trusted
        SylphCasting.toggle(p);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action()); }));
    }
}
