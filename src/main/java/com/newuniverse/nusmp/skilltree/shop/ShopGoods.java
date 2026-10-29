package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: the shop SELLS items that another system owns, and the shop must not learn how those stacks are built; this is the single seam between the two. The only goods the witch ever sold were the Skill
 * Book (a route book) and the SP Book, and both items were removed at the owner's request (see {@code docs/skill_books_removed.md}), so nothing can be made into a purchase any more: every offer answers an EMPTY
 * stack, which the shop treats as unsellable (nothing is charged), and no route exists to stock. The shelf is therefore empty and {@code stock.json} lists no line; the witch still BUYS goods for gold (the
 * sell side is untouched). When a new kind of good is added later, this is the one file that builds its stack.
 */
public final class ShopGoods {
    private ShopGoods() {}

    /** The item a purchase of this offer gives (one copy): always {@link ItemStack#EMPTY}, there is nothing for sale. */
    public static ItemStack stack(StockBoard.Offer offer) {
        return stack(offer.kind(), offer.ref());
    }

    /** The item for a kind and a ref: always {@link ItemStack#EMPTY} (the books are gone, an old stock line for one sells nothing). */
    public static ItemStack stack(StockEntry.Kind kind, String ref) {
        return ItemStack.EMPTY;
    }

    /** The ids of the routes the shop could stock a book for: none (no Skill Book exists any more). */
    public static List<String> routeIds() {
        return List.of();
    }
}
