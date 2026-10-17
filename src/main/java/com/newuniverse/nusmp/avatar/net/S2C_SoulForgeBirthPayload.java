package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the camera shake of the birth spectacle. The beam, the circle and the sounds are ordinary effect packets, but the shake of the camera is a client
 * thing, so the server sends this one packet at the strike: where it happened, how hard and for how many ticks. The client hands it to the mod's own camera
 * shake (strength falls off with distance, so only people near the forge feel it).
 */
public record S2C_SoulForgeBirthPayload(double x, double y, double z, float power, int ticks) implements CustomPacketPayload {
    public static final Type<S2C_SoulForgeBirthPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_birth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SoulForgeBirthPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeFloat(p.power); b.writeVarInt(p.ticks); },
            b -> new S2C_SoulForgeBirthPayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
