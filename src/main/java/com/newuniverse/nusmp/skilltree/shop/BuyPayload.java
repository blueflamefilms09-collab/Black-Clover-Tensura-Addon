package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: a purchase is the one thing the client can ask for that costs real gold, so the packet carries the least that identifies the player's intent and nothing the server should
 * trust: the id of the offer, and the price the player SAW on the button. The server re-validates everything (the witch is in reach, the offer is on the shelf, copies are left,
 * the daily limit, the gold) and refuses with {@link ShopRules.Reply#PRICE_MOVED} when the price it would charge differs from the one on the player's screen, so a restock or a
 * config reload between the screen and the click can never charge more than the player agreed to. Id {@code nusmp:shop_buy}, client to server. The price is a long (millions of
 * gold), written as a var-long.
 *
 * @param offer the slot id of the offer
 * @param price the price on the player's screen
 */
public record BuyPayload(String offer, long price) implements CustomPacketPayload {
    public static final Type<BuyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shop_buy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.offer(), StockBoard.MAX_ID);
                buf.writeVarLong(p.price());
            },
            buf -> new BuyPayload(buf.readUtf(StockBoard.MAX_ID), buf.readVarLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
