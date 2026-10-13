package com.newuniverse.nusmp.relics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the "Use Worn Relic" key. It names a relic and one of its acts and says how many ticks the key was held; the server checks that
 * the player really wears that relic, that it works for them, and every cost and cooldown. Nothing in it is trusted.
 */
public record RelicActPayload(String relicId, String actKey, int chargeTicks) implements CustomPacketPayload {
    public static final Type<RelicActPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_act"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicActPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RelicActPayload::relicId,
            ByteBufCodecs.STRING_UTF8, RelicActPayload::actKey,
            ByteBufCodecs.VAR_INT, RelicActPayload::chargeTicks,
            RelicActPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
