package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Why: the server's answer to the dice button: the six rolled stats (STR DEX CON INT WIS CHA), whether the lucky 5d6 was used, the creator's total luck and the next reroll's price. */
public record S2C_RollPayload(int[] stats, boolean fiveDice, double luck, long token, int rerollCost) implements CustomPacketPayload {
    public static final Type<S2C_RollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_roll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_RollPayload> CODEC = StreamCodec.of(S2C_RollPayload::write, S2C_RollPayload::read);

    private static void write(RegistryFriendlyByteBuf b, S2C_RollPayload p) {
        for (int i = 0; i < 6; i++) b.writeVarInt(i < p.stats.length ? p.stats[i] : 0);
        b.writeBoolean(p.fiveDice); b.writeDouble(p.luck); b.writeLong(p.token); b.writeVarInt(p.rerollCost);
    }

    private static S2C_RollPayload read(RegistryFriendlyByteBuf b) {
        int[] s = new int[6];
        for (int i = 0; i < 6; i++) s[i] = b.readVarInt();
        return new S2C_RollPayload(s, b.readBoolean(), b.readDouble(), b.readLong(), b.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
