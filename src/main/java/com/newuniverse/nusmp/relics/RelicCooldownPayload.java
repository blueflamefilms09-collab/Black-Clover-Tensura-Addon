package com.newuniverse.nusmp.relics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: when an act of a relic is ready again (game time), for the HUD line and the tooltip. A time in the past clears it. */
public record RelicCooldownPayload(String relicId, String actKey, long readyAt) implements CustomPacketPayload {
    public static final Type<RelicCooldownPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RelicCooldownPayload::relicId,
            ByteBufCodecs.STRING_UTF8, RelicCooldownPayload::actKey,
            ByteBufCodecs.VAR_LONG, RelicCooldownPayload::readyAt,
            RelicCooldownPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
