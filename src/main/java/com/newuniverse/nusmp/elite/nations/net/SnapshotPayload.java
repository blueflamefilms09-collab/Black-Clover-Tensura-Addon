package com.newuniverse.nusmp.elite.nations.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the compact server-to-client view of the nations for ONE player: their own nation in detail (members, roles, treasury, ledger, board, wars, alliances, perks) plus a short directory of the others,
 * the open bounties and their reputation. Built by {@code NationSnapshot}, sent only while the player's screen is open (and when something changed, at most every few ticks); the client only displays it
 * and never decides anything. {@code openTab} is the tab to show when the packet opens the screen (-1: just refresh). Payload id {@code nusmp:nation_snapshot}.
 */
public record SnapshotPayload(CompoundTag data, int openTab) implements CustomPacketPayload {
    public static final Type<SnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "nation_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SnapshotPayload::data,
            ByteBufCodecs.VAR_INT, SnapshotPayload::openTab,
            SnapshotPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
