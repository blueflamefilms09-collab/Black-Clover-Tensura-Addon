package com.newuniverse.nusmp.mana.zone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one client-to-server packet of the Mana Zone upgrade: where the caster is aiming inside the zone. 28 bytes at most, sent by a zone owner holding a skill key (the client caps it
 * at about 6 per second, the server drops anything faster than one per {@code aimGap} ticks: 10 per second at the very most per player, and nothing at all from a player without a zone).
 * Everything in it is untrusted: {@link ZoneCast#onAim} validates the sender, the switch, the dimension, the numbers and the target against the server's own zone before it keeps it.
 */
public final class ZoneCastNet {
    private ZoneCastNet() {}

    /** The aimed point (the crosshair's block hit, or the foe's centre) and the foe's entity id, -1 for none. */
    public record AimPayload(double x, double y, double z, int entityId) implements CustomPacketPayload {
        public static final Type<AimPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "zone_aim"));
        public static final StreamCodec<FriendlyByteBuf, AimPayload> CODEC = CustomPacketPayload.codec(AimPayload::write, AimPayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeDouble(x);
            b.writeDouble(y);
            b.writeDouble(z);
            b.writeVarInt(entityId + 1);
        }

        private static AimPayload read(FriendlyByteBuf b) { return new AimPayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt() - 1); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener: registers the packet (optional: a client without the mod simply never sends it). The handler runs on the server thread, with the real sender. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(AimPayload.TYPE, AimPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ZoneCast.onAim(sp, p.x(), p.y(), p.z(), p.entityId());
        }));
    }
}
