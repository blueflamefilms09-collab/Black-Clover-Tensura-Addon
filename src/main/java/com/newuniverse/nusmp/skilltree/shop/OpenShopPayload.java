package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: opening the shop is a SERVER decision (the player sneaked and right-clicked the witch, she is in reach, the shop is switched on), so the server tells the client to open
 * the screen and hands it the first full picture in the same packet; the client cannot open the shop by itself and the screen never shows data the server did not send. NeoForge
 * 21.1 has no {@code SimpleChannel}: this is a {@link CustomPacketPayload} record with a {@link StreamCodec}, registered in {@link ShopNet} on the mod bus, which is the supported,
 * typed and bounded way and fails loudly on a bad packet. Id {@code nusmp:shop_open}, server to client. A separate type from {@link ShopSyncPayload} (instead of one packet with an
 * "open" flag) because the two do different things on arrival: this one OPENS a screen, the other only updates the screen that is open and is ignored when none is.
 */
public record OpenShopPayload(ShopSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shop_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ShopSnapshot.write(buf, p.snapshot()),
            buf -> new OpenShopPayload(ShopSnapshot.read(buf)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
