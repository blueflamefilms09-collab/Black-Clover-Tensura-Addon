package com.newuniverse.nusmp.entity.riven;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Riven's state for the client: phase, the animation clip being played, the skill name for the bar, and the story charge (0-100). Sent when it changes. */
public record RivenStatePayload(int entity, int phase, String clip, String skill, int charge) implements CustomPacketPayload {
    public static final Type<RivenStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "riven_state"));
    public static final StreamCodec<FriendlyByteBuf, RivenStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeByte(p.phase()); buf.writeUtf(p.clip(), 40); buf.writeUtf(p.skill(), 80); buf.writeByte(p.charge()); },
            buf -> new RivenStatePayload(buf.readVarInt(), buf.readByte(), buf.readUtf(40), buf.readUtf(80), buf.readByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level().getEntity(p.entity()) instanceof RivenBossEntity r) r.applyState(p.phase(), p.clip(), p.skill(), p.charge());
        }));
    }
}
