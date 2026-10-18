package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: a hunter comes back from the dungeon with thirty Magic Stones in five stacks and does not want to click thirty times, so one packet sells every stack of a CATEGORY of
 * goods ("Sell all stones"). It carries only the category name; the server decides which categories may be sold this way (the price table's {@code bulk} list, so a nether star is
 * never swept up by a misclick), which stacks belong to it, and how much the witch pays. Id {@code nusmp:shop_sell_all}, client to server.
 *
 * @param category the price table category ({@code stones}, {@code meat}, {@code materials} ...)
 */
public record SellAllPayload(String category) implements CustomPacketPayload {
    public static final Type<SellAllPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shop_sell_all"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SellAllPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.category(), 32),
            buf -> new SellAllPayload(buf.readUtf(32)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
