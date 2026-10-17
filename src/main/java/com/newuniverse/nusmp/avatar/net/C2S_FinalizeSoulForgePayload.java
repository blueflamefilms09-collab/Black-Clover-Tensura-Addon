package com.newuniverse.nusmp.avatar.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the "Finalize" button of the Soul Forge. Like the classic Create packet it carries only the creator's CHOICES: the race id, the personalities by
 * name, the look, an optional name, the token of the stat roll the screen showed, and the Soul Forge's own choices: the Unique and Ultimate skill ids, the
 * aptitudes picked by hand, the token of the free aptitude roll (0 = not used: the server holds the rolled id itself, so the packet cannot name a rare
 * aptitude and call it free), and whether the creator pays for a guaranteed Instant Skill Awakening. There is no price, no tax and no result in it. Cut to
 * safe lengths on both sides; the server cleans, resolves and validates everything again.
 */
public record C2S_FinalizeSoulForgePayload(String raceId, String primary, String secondary, CompoundTag appearance, String name, long statToken,
                                           String unique, String ultimate, List<String> aptitudes, long rollToken, boolean paidAwakening) implements CustomPacketPayload {
    public static final int MAX_APTITUDES = 8;
    public static final Type<C2S_FinalizeSoulForgePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_finalize"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_FinalizeSoulForgePayload> CODEC = StreamCodec.of(C2S_FinalizeSoulForgePayload::write, C2S_FinalizeSoulForgePayload::read);

    private static void write(RegistryFriendlyByteBuf b, C2S_FinalizeSoulForgePayload p) {
        b.writeUtf(NetText.clean(p.raceId, NetText.ID_MAX), NetText.ID_MAX);
        b.writeUtf(NetText.clean(p.primary, NetText.ID_MAX), NetText.ID_MAX);
        b.writeUtf(NetText.clean(p.secondary, NetText.ID_MAX), NetText.ID_MAX);
        ByteBufCodecs.COMPOUND_TAG.encode(b, p.appearance);
        b.writeUtf(NetText.clean(p.name, NetText.NAME_MAX), NetText.NAME_MAX);
        b.writeLong(p.statToken);
        b.writeUtf(NetText.clean(p.unique, NetText.ID_MAX), NetText.ID_MAX);
        b.writeUtf(NetText.clean(p.ultimate, NetText.ID_MAX), NetText.ID_MAX);
        int n = Math.min(p.aptitudes.size(), MAX_APTITUDES);
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) b.writeUtf(NetText.clean(p.aptitudes.get(i), NetText.ID_MAX), NetText.ID_MAX);
        b.writeLong(p.rollToken);
        b.writeBoolean(p.paidAwakening);
    }

    private static C2S_FinalizeSoulForgePayload read(RegistryFriendlyByteBuf b) {
        String race = b.readUtf(NetText.ID_MAX), primary = b.readUtf(NetText.ID_MAX), secondary = b.readUtf(NetText.ID_MAX);
        CompoundTag look = ByteBufCodecs.COMPOUND_TAG.decode(b);
        String name = b.readUtf(NetText.NAME_MAX);
        long statToken = b.readLong();
        String unique = b.readUtf(NetText.ID_MAX), ultimate = b.readUtf(NetText.ID_MAX);
        int n = Math.max(0, Math.min(b.readVarInt(), MAX_APTITUDES));
        List<String> apt = new ArrayList<>(n);
        for (int i = 0; i < n; i++) apt.add(b.readUtf(NetText.ID_MAX));
        long rollToken = b.readLong();
        boolean paid = b.readBoolean();
        return new C2S_FinalizeSoulForgePayload(race, primary, secondary, look, name, statToken, unique, ultimate, apt, rollToken, paid);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
