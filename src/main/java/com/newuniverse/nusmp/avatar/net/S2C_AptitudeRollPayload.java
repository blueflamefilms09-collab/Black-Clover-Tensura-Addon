package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the answer to the "Randomize Aptitude" button. The result is decided by the SERVER (so a client cannot "roll" a Legendary for itself); this packet
 * carries it back: the aptitude id the roll landed on, the token that stands for it (the screen sends the token, never the id, when it finalizes), how many
 * free rolls remain, and a seed for the slot-machine reel so the decoys that spin past are the same every time this roll is replayed.
 */
public record S2C_AptitudeRollPayload(String aptitudeId, long token, int rollsLeft, int reelSeed) implements CustomPacketPayload {
    public static final Type<S2C_AptitudeRollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_roll_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_AptitudeRollPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(NetText.clean(p.aptitudeId, NetText.ID_MAX), NetText.ID_MAX); b.writeLong(p.token); b.writeVarInt(p.rollsLeft); b.writeInt(p.reelSeed); },
            b -> new S2C_AptitudeRollPayload(b.readUtf(NetText.ID_MAX), b.readLong(), b.readVarInt(), b.readInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
