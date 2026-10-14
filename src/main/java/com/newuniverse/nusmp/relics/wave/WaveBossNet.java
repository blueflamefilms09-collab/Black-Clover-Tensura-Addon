package com.newuniverse.nusmp.relics.wave;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one tiny packet of the boss relics: a cue that tells every client that can see a player that one of the three relics just fired, so its worn
 * model can play the matching beat (the Mimic's bite, the crown's pull, the ring's flare). The state the renderers read is a start stamp per
 * (player, relic) kept on the client ({@code client.relics.worn.WornCues}); nothing about the cue is saved or needs the server afterwards.
 * Register {@link #register} on the mod bus.
 */
public final class WaveBossNet {
    private WaveBossNet() {}

    /** Which relic a cue is for. */
    public static final int WHICH_CROWN = 0, WHICH_RING = 1, WHICH_MIMIC = 2;
    /** Crown: the well. Ring: the step, the gates. Mimic: the bite, the yawn of the hoard. */
    public static final int KIND_WELL = 1, KIND_STEP = 1, KIND_GATES = 2, KIND_BITE = 1, KIND_YAWN = 2;

    /** {@code entity} is the id of the player who fires, {@code which} one of the WHICH_ values, {@code kind} one of the KIND_ values. */
    public record Cue(int entity, int which, int kind) implements CustomPacketPayload {
        public static final Type<Cue> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "worn_cue"));
        public static final StreamCodec<FriendlyByteBuf, Cue> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entity());
                    buf.writeByte(p.which());
                    buf.writeByte(p.kind());
                },
                buf -> new Cue(buf.readVarInt(), buf.readByte(), buf.readByte()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Tells the player and everyone who can see them. Safe to call for any player; a failure to send is never an error of the ability. */
    public static void cue(ServerPlayer p, int which, int kind) {
        if (p == null) return;
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new Cue(p.getId(), which, kind));
        } catch (RuntimeException | LinkageError ignored) {
            // no connection yet (a test world, a fake player): the relic still works, it just does not animate
        }
    }

    /** Mod-bus listener. The client handler only runs on a client, so the client class is never loaded on a server. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Cue.TYPE, Cue.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.relics.worn.WornCues.receive(payload.entity(), payload.which(), payload.kind())));
    }
}
