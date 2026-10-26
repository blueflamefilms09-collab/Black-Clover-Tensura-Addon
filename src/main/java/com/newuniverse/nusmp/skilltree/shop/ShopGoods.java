package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.rarity.BookFacts;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the shop SELLS items that another system owns (the Skill Book that opens a route and the SP Book that gives Skill Points are made by the items builder's classes, whose
 * components say which book a stack is), and the shop must not learn how those stacks are built. This is the single seam between the two: it asks the items classes for a stack
 * and the route registry for the routes that exist, and nothing else in the shop touches either. If the items classes change how a stack is made, this file is the only one to
 * edit; if a book cannot be made (an unknown route, an unknown SP kind) the answer is an EMPTY stack and the shop treats the offer as unsellable (nothing is charged) instead of
 * handing the player a blank item. It uses the items builder's two factories, {@code SkillBookItem.stack(String bookId)} and {@code SPBookItem.stack(String kindId)}, which are also
 * the ones the creative tab and the dungeon drops use, so a bought book is exactly the book a drop gives (and an unknown SP kind is an empty stack there too, by design).
 */
public final class ShopGoods {
    private ShopGoods() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The item a purchase of this offer gives (one copy), or {@link ItemStack#EMPTY} when it cannot be made. */
    public static ItemStack stack(StockBoard.Offer offer) {
        return stack(offer.kind(), offer.ref());
    }

    /** The item for a kind and a ref (a route id or an SP kind), or {@link ItemStack#EMPTY}. */
    public static ItemStack stack(StockEntry.Kind kind, String ref) {
        try {
            ItemStack made;
            if (kind == StockEntry.Kind.ROUTE_BOOK) {
                SkillRoute route = RouteRegistry.byId(ref);
                if (route == null || !LineIndex.known(route.id())) return ItemStack.EMPTY;
                made = SkillBookItem.stack(route.book());
            } else {
                made = ItemStack.EMPTY;                                         // the SP Books are gone with the Skill Points: an old stock line for one sells nothing
            }
            return made == null ? ItemStack.EMPTY : made;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: could not make the {} '{}': {}", kind, ref, e.toString());
            return ItemStack.EMPTY;
        }
    }

    /**
     * The ids of the routes that exist right now, in registry order, minus (3.26.0) the lines of a blocked franchise ({@code blockedFranchises} with {@code hideBlockedFranchises} on): the Black Market
     * neither stocks nor restocks them. A book already held still reads, and the commands and the creative tab are unchanged.
     */
    public static List<String> routeIds() {
        List<String> out = new ArrayList<>();
        for (SkillRoute r : RouteRegistry.all()) if (LineIndex.known(r.id()) && !BookFacts.isBlocked(r.id())) out.add(r.id());
        return out;
    }
}
