package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: after every purchase or sale (and after every refusal) the screen must show what really happened, so the server answers with a fresh whole picture ({@link ShopSnapshot})
 * plus the verdict ({@link ShopRules.Reply}, as its wire number) and a random variant number that picks one of the witch's two lines for that verdict. Sending the whole picture
 * rather than "you bought X" means the client never merges state: a refusal because the price moved arrives together with the new price, a sale arrives together with the new gold
 * and the shrunken stack. The client ignores it when no shop screen is open (a late packet after the player closed it). Id {@code nusmp:shop_sync}, server to client.
 *
 * @param snapshot the new picture of the shop
 * @param reply    the ordinal of the {@link ShopRules.Reply}
 * @param variant  a number from 0 to 255 the client reduces to choose the witch's line
 */
public record ShopSyncPayload(ShopSnapshot snapshot, int reply, int variant) implements CustomPacketPayload {
    public static final Type<ShopSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shop_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShopSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ShopSnapshot.write(buf, p.snapshot());
                buf.writeByte(p.reply());
                buf.writeByte(p.variant());
            },
            buf -> {
                ShopSnapshot s = ShopSnapshot.read(buf);
                int reply = buf.readByte() & 0xFF;
                int variant = buf.readByte() & 0xFF;
                return new ShopSyncPayload(s, reply, variant);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** The verdict as the enum. */
    public ShopRules.Reply verdict() { return ShopRules.Reply.byOrdinal(reply); }
}
