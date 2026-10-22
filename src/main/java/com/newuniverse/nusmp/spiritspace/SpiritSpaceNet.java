package com.newuniverse.nusmp.spiritspace;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the one packet of the Spirit of Space. The client only asks (a key went down); the server decides everything: the sender must be a real player, the action must be one of the two known ones,
 * the ability and slot numbers must be in range, the spirit must be switched on and allowed for that player, and at most one ask per {@link SpiritSpaceRules#PACKET_GAP} ticks is answered
 * ({@link SpiritSpace#onKey}). Everything the spirit draws travels as ordinary props, so this is the only new traffic: three bytes per key press, client to server, nothing is sent back. Register
 * {@link #registerPayloads} on the mod bus.
 */
public final class SpiritSpaceNet {
    private SpiritSpaceNet() {}

    /** A key of the spirit: {@code action} 0 toggles the link, 1 uses an ability ({@code ability} 0 portal, 1 gravity, 2 cut, 3 set the waypoint of {@code slot}); {@code slot} is 0-based. */
    public record Key(int action, int ability, int slot) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "spirit_space_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeByte(p.action()); buf.writeByte(p.ability()); buf.writeByte(p.slot()); },
                buf -> new Key(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onKey(ServerPlayer p, int action, int ability, int slot) {
        if (!SpiritSpaceRules.validAction(action)) return;                                   // an unknown action is ignored, never trusted
        if (action == SpiritSpaceRules.ACT_USE && (!SpiritSpaceRules.validAbility(ability) || !SpiritSpaceRules.validSlot(slot, SpiritSpaceRules.WAYPOINTS_HARD))) return;
        SpiritSpace.onKey(p, action, ability, slot);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action(), payload.ability(), payload.slot()); }));
    }
}
