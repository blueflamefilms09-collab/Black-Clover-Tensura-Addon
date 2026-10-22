package com.newuniverse.nusmp.spiritdark;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one packet of the Spirit of Darkness. The client only asks (a key went down); the server decides everything: the sender must be a real player, the action must be one of the three
 * ({@link SpiritDarkRules#validAction}), the player must hold Dark Magic with the switch on, and at most one ask per {@link SpiritDarkRules#PACKET_GAP} ticks is answered
 * ({@link SpiritDark#onKey}). Everything the spirit draws travels as an ordinary prop (its synced param word), the sounds as ordinary sound packets to players nearby, so this is the only new
 * traffic: one byte per key press, client to server. Register {@link #registerPayloads} on the mod bus.
 */
public final class SpiritDarkNet {
    private SpiritDarkNet() {}

    /** A key went down: 0 = link or unlink the spirit, 1 = Shadow Step, 2 = call or dismiss the powered form. */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "spirit_dark_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new Key(buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onKey(ServerPlayer p, int action) {
        if (!SpiritDarkRules.validAction(action)) return;                      // an unknown action is ignored, never trusted
        SpiritDark.onKey(p, action);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action()); }));
    }
}
