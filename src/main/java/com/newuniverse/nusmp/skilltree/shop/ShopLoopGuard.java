package com.newuniverse.nusmp.skilltree.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Why: the witch BUYS goods for gold ({@code prices.json}) and now SELLS goods for gold too. If she sold an item for less than she pays for it, a player could buy it, sell it back and print gold
 * without end, and the whole economy (a route book used to be millions of gold) would be worth nothing. This is the one rule that makes that impossible, written as pure functions of numbers so it
 * is unit tested and used in two places: at pool building (an item whose buy price is not BELOW the line's cheapest sell price never enters the pool, whatever tag the line names) and at the
 * shelf (an offer whose price, after the server's price scale, does not beat what the witch pays for the same stack after the server's sell scale, is left off). The data files and the config can
 * both be changed by the server owner, so the guard looks at the numbers that are really in force, not at the defaults.
 */
public final class ShopLoopGuard {
    private ShopLoopGuard() {}

    /**
     * True when a player could make gold by buying {@code amount} items for {@code paid} gold and selling them straight back at {@code sellUnit} gold each: the shop pays back at least what it took.
     * An offer for which this is true is never put on the shelf.
     */
    public static boolean loops(long paid, int amount, long sellUnit) {
        if (sellUnit <= 0 || amount <= 0) return false;                       // the witch pays nothing for it: nothing to loop through
        long back = sellUnit > Long.MAX_VALUE / amount ? Long.MAX_VALUE : sellUnit * amount;
        return back >= paid;
    }

    /** True when the item may be offered at {@code priceMin} or more per item: the witch does not buy it, or buys it for strictly less. */
    public static boolean safe(long priceMin, long buyPrice) {
        return buyPrice <= 0 || priceMin > buyPrice;
    }

    /**
     * The pool without the items the line would lose money on: every item whose buy price (the function answers 0 for an item the witch does not buy) is not strictly below {@code priceMin}.
     * Order is kept.
     */
    public static List<StockBoard.PoolItem> safePool(List<StockBoard.PoolItem> pool, long priceMin, ToLongFunction<String> buyPrice) {
        List<StockBoard.PoolItem> out = new ArrayList<>(pool.size());
        for (StockBoard.PoolItem p : pool) if (safe(priceMin, buyPrice.applyAsLong(p.id()))) out.add(p);
        return out;
    }
}
