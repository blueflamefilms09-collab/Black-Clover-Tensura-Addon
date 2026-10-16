package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Why: the race list of page 2, filtered by the server (each race says whether this creator may take it and which connection conditions fail). */
public record S2C_RacesPayload(List<RaceEntry> races) implements CustomPacketPayload {
    public static final Type<S2C_RacesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_races"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_RacesPayload> CODEC = StreamCodec.composite(
            RaceEntry.CODEC.apply(ByteBufCodecs.list(RaceEntry.MAX_RACES)), S2C_RacesPayload::races, S2C_RacesPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
