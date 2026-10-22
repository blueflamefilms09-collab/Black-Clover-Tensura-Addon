package com.newuniverse.nusmp.spiritlight;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one packet of the Spirit of Light. The client only asks (a key went down); the server decides everything: the sender must be a real player, the action must be one of the four it knows, at
 * most one ask per {@link SpiritLightRules#PACKET_GAP} ticks is answered, and every action then checks the link, the cooldowns and the caps itself ({@link SpiritLight#onKey}). Everything the spirit
 * draws travels as ordinary props, and its sounds as ordinary level sounds, so this is the only new traffic: one byte per key press, client to server, nothing is ever sent back. Register
 * {@link #registerPayloads} on the mod bus.
 */
public final class SpiritLightNet {
    private SpiritLightNet() {}

    /** A key of the spirit went down: 0 link toggle, 1 beam, 2 glow pulse, 3 powered form toggle (see {@link SpiritLightRules#ACT_LINK}). */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "spirit_light_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new Key(buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onKey(ServerPlayer p, int action) {
        if (!SpiritLightRules.validAction(action)) return;                 // an unknown action is ignored, never trusted
        SpiritLight.onKey(p, action);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action()); }));
    }
}
