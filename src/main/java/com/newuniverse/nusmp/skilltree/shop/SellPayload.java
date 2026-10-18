package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: selling takes items out of the player's inventory, so the packet names the inventory SLOT (as the design says) and also the item id the player believed was in it: if the
 * inventory shifted between the screen and the click (a pickup, a shift-click) the item in the slot is no longer the one on the screen, and the server refuses instead of selling
 * something the player did not choose. The count is a request that the server clamps to what the stack holds. Id {@code nusmp:shop_sell}, client to server. Only the main
 * inventory (slots 0 to 35) can be sold from: armour, the off hand and anything equipped are never on offer.
 *
 * @param slot  the player's inventory slot, 0 to 35
 * @param item  the registry id of the item the player saw in it
 * @param count how many to sell (at least 1)
 */
public record SellPayload(int slot, String item, int count) implements CustomPacketPayload {
    public static final Type<SellPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shop_sell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SellPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.slot());
                buf.writeUtf(p.item(), 96);
                buf.writeVarInt(p.count());
            },
            buf -> new SellPayload(buf.readVarInt(), buf.readUtf(96), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
