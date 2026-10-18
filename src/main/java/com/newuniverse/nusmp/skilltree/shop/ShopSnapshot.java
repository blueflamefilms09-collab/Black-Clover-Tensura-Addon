package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the shop screen must never decide anything, so it is drawn from ONE immutable picture of the shop that the server builds and sends whole: the player's gold, the shelf
 * (each offer with its real item stack, its price after the server's scale, and what is left), what the player carries that the witch would buy (with the gold per item), the
 * categories that have a "Sell all" button, the restock clock and the witch's current line. Sending whole stacks, not item ids, is what lets the screen draw the same icon, name and
 * tooltip the item itself would show (including the route colour and name of a Skill Book, which come from the item's own component), and sending the whole picture after every
 * action means the client never has to merge a delta and can never drift. It is small (a few hundred bytes to a few kilobytes) and bounded: the lists are capped on both ends of the
 * wire, and a packet that claims more is rejected as corrupt instead of being allocated. Used by {@link OpenShopPayload} and {@link ShopSyncPayload}.
 *
 * @param witch         the entity id of the witch the player trades with, or -1 when none is near (the screen closes)
 * @param gold          the player's gold
 * @param ticksToRestock game ticks until the next restock
 * @param dailyLeft     purchases the player may still make today, or -1 for no limit
 * @param offers        the shelf, in display order
 * @param sells         the player's goods the witch buys, in inventory order
 * @param bulk          the categories with a "Sell all" button
 * @param voice         the witch's line to show (empty for none)
 */
public record ShopSnapshot(int witch, long gold, long ticksToRestock, int dailyLeft, List<Offer> offers, List<Sell> sells, List<String> bulk, Component voice) {
    /** The most offers, goods and bulk categories one picture carries; the writer cuts there and the reader refuses more. */
    public static final int MAX_OFFERS = 128, MAX_SELLS = 64, MAX_BULK = 16;

    /**
     * One offer on the shelf.
     *
     * @param id       the slot id (the key of a purchase)
     * @param kind     0 for a route book, 1 for an SP book (the ordinal of {@link StockEntry.Kind})
     * @param ref      the route id or the SP kind
     * @param stack    the item as the player would receive it
     * @param price    gold, after the server's price scale
     * @param left     copies left
     * @param total    copies this restock put out
     * @param flagship true for the showpiece
     */
    public record Offer(String id, int kind, String ref, ItemStack stack, long price, int left, int total, boolean flagship) {}

    /**
     * One of the player's goods the witch buys.
     *
     * @param slot     the inventory slot (0 to 35)
     * @param stack    a copy of the stack, so the screen can draw it with its count
     * @param unit     gold per item, after the server's sell scale
     * @param category the price table's category
     */
    public record Sell(int slot, ItemStack stack, long unit, String category) {}

    public static void write(RegistryFriendlyByteBuf buf, ShopSnapshot s) {
        buf.writeVarInt(s.witch());
        buf.writeVarLong(s.gold());
        buf.writeVarLong(s.ticksToRestock());
        buf.writeVarInt(s.dailyLeft());
        int no = Math.min(s.offers().size(), MAX_OFFERS);
        buf.writeVarInt(no);
        for (int i = 0; i < no; i++) {
            Offer o = s.offers().get(i);
            buf.writeUtf(o.id(), StockBoard.MAX_ID);
            buf.writeByte(o.kind());
            buf.writeUtf(o.ref(), StockBoard.MAX_ID);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, o.stack());
            buf.writeVarLong(o.price());
            buf.writeVarInt(o.left());
            buf.writeVarInt(o.total());
            buf.writeBoolean(o.flagship());
        }
        int ns = Math.min(s.sells().size(), MAX_SELLS);
        buf.writeVarInt(ns);
        for (int i = 0; i < ns; i++) {
            Sell g = s.sells().get(i);
            buf.writeVarInt(g.slot());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, g.stack());
            buf.writeVarLong(g.unit());
            buf.writeUtf(g.category(), 32);
        }
        int nb = Math.min(s.bulk().size(), MAX_BULK);
        buf.writeVarInt(nb);
        for (int i = 0; i < nb; i++) buf.writeUtf(s.bulk().get(i), 32);
        ComponentSerialization.STREAM_CODEC.encode(buf, s.voice());
    }

    public static ShopSnapshot read(RegistryFriendlyByteBuf buf) {
        int witch = buf.readVarInt();
        long gold = buf.readVarLong();
        long ticks = buf.readVarLong();
        int daily = buf.readVarInt();
        int no = buf.readVarInt();
        if (no < 0 || no > MAX_OFFERS) throw new IllegalStateException("a shop picture with " + no + " offers");
        List<Offer> offers = new ArrayList<>(no);
        for (int i = 0; i < no; i++) {
            String id = buf.readUtf(StockBoard.MAX_ID);
            int kind = buf.readByte();
            String ref = buf.readUtf(StockBoard.MAX_ID);
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long price = buf.readVarLong();
            int left = buf.readVarInt();
            int total = buf.readVarInt();
            boolean flagship = buf.readBoolean();
            offers.add(new Offer(id, kind, ref, stack, price, left, total, flagship));
        }
        int ns = buf.readVarInt();
        if (ns < 0 || ns > MAX_SELLS) throw new IllegalStateException("a shop picture with " + ns + " goods");
        List<Sell> sells = new ArrayList<>(ns);
        for (int i = 0; i < ns; i++) {
            int slot = buf.readVarInt();
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long unit = buf.readVarLong();
            String category = buf.readUtf(32);
            sells.add(new Sell(slot, stack, unit, category));
        }
        int nb = buf.readVarInt();
        if (nb < 0 || nb > MAX_BULK) throw new IllegalStateException("a shop picture with " + nb + " bulk categories");
        List<String> bulk = new ArrayList<>(nb);
        for (int i = 0; i < nb; i++) bulk.add(buf.readUtf(32));
        Component voice = ComponentSerialization.STREAM_CODEC.decode(buf);
        return new ShopSnapshot(witch, gold, ticks, daily, List.copyOf(offers), List.copyOf(sells), List.copyOf(bulk), voice);
    }
}
