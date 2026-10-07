package com.newuniverse.nusmp.book;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 0.49: Rouge sits on her summoner's head as a real model (client/RougeCatLayer) while she watches over them (replacing the
 * 0.34 VFX). The server re-announces it every second to everyone tracking the player; each announcement lasts a little over a
 * second on the client, so she leaves on her own once Rouge is spent, and late arrivals see her too.
 */
public final class RougeCat {
    private RougeCat() {}

    /** Client side: entity id -> client game time she stays until. */
    public static final Map<Integer, Long> UNTIL = new HashMap<>();

    public record Payload(int entity, int ticks) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "rouge_cat"));
        public static final StreamCodec<FriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeVarInt(p.ticks()); }, buf -> new Payload(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() != null) UNTIL.put(p.entity(), ctx.player().level().getGameTime() + p.ticks());
        }));
    }

    /** Rouge is on this player's head for the next 45 ticks (sent once a second while armed). */
    public static void show(ServerPlayer p) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new Payload(p.getId(), 45));
    }

    public static boolean onHead(int entity, long now) {
        Long until = UNTIL.get(entity);
        if (until == null) return false;
        if (now >= until) { UNTIL.remove(entity); return false; }
        return true;
    }
}
