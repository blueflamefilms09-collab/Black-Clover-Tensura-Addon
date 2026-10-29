package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Why: the shop SELLS items that another system owns, and the shop must not learn how those stacks are built; this is the single seam between the two. The Skill Book and the SP Book, the only goods the
 * witch ever sold, were removed at the owner's request (see {@code docs/skill_books_removed.md}), so the book kinds still answer an EMPTY stack, which the shop treats as unsellable (nothing is charged).
 * The ITEM kind (see {@code docs/black_market_goods.md}) is what is on the shelf now: its offer names a registry item id and a count, and this class makes exactly that stack on the server, never from
 * anything a client sent. When a new kind of good is added later, this is the one file that builds its stack.
 */
public final class ShopGoods {
    private ShopGoods() {}

    /**
     * The items a purchase of this offer gives: for an ITEM offer the rolled item in the rolled stack size (never above the item's own stack size), re-derived here on the server from the shelf's
     * own offer, never from anything the client sent; {@link ItemStack#EMPTY} for the book kinds (the books are gone) and for an item this game does not have.
     */
    public static ItemStack stack(StockBoard.Offer offer) {
        ItemStack one = stack(offer.kind(), offer.ref());
        if (one.isEmpty()) return one;
        return new ItemStack(one.getItem(), Math.max(1, Math.min(offer.amount(), one.getMaxStackSize())));
    }

    /** One item for a kind and a ref: an item for ITEM (the ref is its registry id), {@link ItemStack#EMPTY} otherwise (the books are gone, an old stock line for one sells nothing). */
    public static ItemStack stack(StockEntry.Kind kind, String ref) {
        if (kind != StockEntry.Kind.ITEM) return ItemStack.EMPTY;
        ResourceLocation rl = ref == null ? null : ResourceLocation.tryParse(ref);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The ids of the routes the shop could stock a book for: none (no Skill Book exists any more). */
    public static List<String> routeIds() {
        return List.of();
    }
}
