package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: this is the table of every refusal in the shop, and the ORDER of the refusals is part of the design (a player far from the witch is told they are far, not that they are
 * poor), so each rule is pinned on its own with everything else in order, then in pairs. The sale arithmetic is tested at the purse cap, where a careless clamp would eat gold.
 */
class ShopRulesTest {
    /** A purchase that would go through: near the witch, on the shelf, in stock, the price the player saw, no limit, rich. */
    private static ShopRules.BuyCase ok() {
        return new ShopRules.BuyCase(true, 2.0, true, 1, 2_500_000, 2_500_000, 5_000_000, 0, 0);
    }

    private static ShopRules.BuyCase with(boolean enabled, double distance, boolean known, int left, long price, long expected, long gold, int limit, int today) {
        return new ShopRules.BuyCase(enabled, distance, known, left, price, expected, gold, limit, today);
    }

    @Test
    void anOrdinaryPurchaseGoesThrough() {
        assertEquals(ShopRules.Reply.BOUGHT, ShopRules.judgeBuy(ok()));
        assertEquals(ShopRules.Reply.BOUGHT, ShopRules.judgeBuy(with(true, 2.0, true, 1, 2_500_000, 2_500_000, 2_500_000, 0, 0)), "exactly enough gold is enough");
        assertEquals(ShopRules.Reply.BOUGHT, ShopRules.judgeBuy(with(true, ShopRules.REACH, true, 1, 100, 100, 100, 0, 0)), "exactly at the reach is in reach");
    }

    @Test
    void eachRefusalFiresOnItsOwn() {
        assertEquals(ShopRules.Reply.CLOSED, ShopRules.judgeBuy(with(false, 2.0, true, 1, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeBuy(with(true, ShopRules.REACH + 0.01, true, 1, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeBuy(with(true, Double.NaN, true, 1, 5, 5, 99, 0, 0)), "no witch near at all");
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeBuy(with(true, Double.POSITIVE_INFINITY, true, 1, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.UNKNOWN, ShopRules.judgeBuy(with(true, 2.0, false, 1, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.SOLD_OUT, ShopRules.judgeBuy(with(true, 2.0, true, 0, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.SOLD_OUT, ShopRules.judgeBuy(with(true, 2.0, true, -3, 5, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.PRICE_MOVED, ShopRules.judgeBuy(with(true, 2.0, true, 1, 6, 5, 99, 0, 0)));
        assertEquals(ShopRules.Reply.PRICE_MOVED, ShopRules.judgeBuy(with(true, 2.0, true, 1, 5, 6, 99, 0, 0)), "a price that fell is also reported: the player decides again");
        assertEquals(ShopRules.Reply.LIMIT, ShopRules.judgeBuy(with(true, 2.0, true, 1, 5, 5, 99, 3, 3)));
        assertEquals(ShopRules.Reply.POOR, ShopRules.judgeBuy(with(true, 2.0, true, 1, 5, 5, 4, 0, 0)));
        assertEquals(ShopRules.Reply.POOR, ShopRules.judgeBuy(with(true, 2.0, true, 1, -5, -5, 99, 0, 0)), "a negative price is never a gift");
    }

    @Test
    void theOrderOfTheChecksIsClosedFarUnknownSoldOutPriceLimitPoor() {
        // everything wrong at once: the first check in the order wins, and removing it exposes the next
        ShopRules.BuyCase all = with(false, 50.0, false, 0, 6, 5, 0, 1, 1);
        assertEquals(ShopRules.Reply.CLOSED, ShopRules.judgeBuy(all));
        all = with(true, 50.0, false, 0, 6, 5, 0, 1, 1);
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeBuy(all));
        all = with(true, 2.0, false, 0, 6, 5, 0, 1, 1);
        assertEquals(ShopRules.Reply.UNKNOWN, ShopRules.judgeBuy(all));
        all = with(true, 2.0, true, 0, 6, 5, 0, 1, 1);
        assertEquals(ShopRules.Reply.SOLD_OUT, ShopRules.judgeBuy(all));
        all = with(true, 2.0, true, 1, 6, 5, 0, 1, 1);
        assertEquals(ShopRules.Reply.PRICE_MOVED, ShopRules.judgeBuy(all));
        all = with(true, 2.0, true, 1, 6, 6, 0, 1, 1);
        assertEquals(ShopRules.Reply.LIMIT, ShopRules.judgeBuy(all));
        all = with(true, 2.0, true, 1, 6, 6, 0, 1, 0);
        assertEquals(ShopRules.Reply.POOR, ShopRules.judgeBuy(all));
    }

    @Test
    void theDailyLimit() {
        assertFalse(ShopRules.limitReached(0, 99), "0 means no limit");
        assertFalse(ShopRules.limitReached(3, 2));
        assertTrue(ShopRules.limitReached(3, 3));
        assertTrue(ShopRules.limitReached(3, 9));
        assertEquals(0, ShopRules.boughtToday(7, 4, 8), "yesterday's counter does not count today");
        assertEquals(4, ShopRules.boughtToday(8, 4, 8));
        assertEquals(0, ShopRules.boughtToday(8, -4, 8), "a corrupted negative counter reads as 0");
        assertEquals(-1, ShopRules.dailyLeft(0, 5));
        assertEquals(2, ShopRules.dailyLeft(5, 3));
        assertEquals(0, ShopRules.dailyLeft(5, 9));
    }

    @Test
    void aSaleIsClampedToTheStackAndTheRoomInThePurse() {
        ShopRules.Sale s = ShopRules.judgeSell(true, 1.0, 50_000, 13, 5, 0);
        assertEquals(ShopRules.Reply.SOLD, s.reply());
        assertEquals(5, s.count());
        assertEquals(250_000, s.gold());

        s = ShopRules.judgeSell(true, 1.0, 50_000, 3, 99, 0);
        assertEquals(3, s.count(), "cannot sell more than the stack holds");
        assertEquals(150_000, s.gold());

        long room = 120_000;
        s = ShopRules.judgeSell(true, 1.0, 50_000, 10, 10, GoldLedger.CAP - room);
        assertEquals(2, s.count(), "only what the purse has room for is sold, never gold thrown away");
        assertEquals(100_000, s.gold());

        s = ShopRules.judgeSell(true, 1.0, 50_000, 10, 10, GoldLedger.CAP);
        assertEquals(ShopRules.Reply.FULL_PURSE, s.reply());
        assertEquals(0, s.count());
        assertEquals(0, s.gold());

        s = ShopRules.judgeSell(true, 1.0, Long.MAX_VALUE / 2, 3, 3, 0);
        assertEquals(ShopRules.Reply.FULL_PURSE, s.reply(), "a single item worth more than the cap cannot be sold");
    }

    @Test
    void aSaleRefusalsFireInOrder() {
        assertEquals(ShopRules.Reply.CLOSED, ShopRules.judgeSell(false, 1.0, 5, 1, 1, 0).reply());
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeSell(true, 99.0, 5, 1, 1, 0).reply());
        assertEquals(ShopRules.Reply.FAR, ShopRules.judgeSell(true, Double.NaN, 5, 1, 1, 0).reply());
        assertEquals(ShopRules.Reply.NOTHING, ShopRules.judgeSell(true, 1.0, 0, 1, 1, 0).reply(), "an item the witch has no price for");
        assertEquals(ShopRules.Reply.NOTHING, ShopRules.judgeSell(true, 1.0, 5, 0, 1, 0).reply());
        assertEquals(ShopRules.Reply.NOTHING, ShopRules.judgeSell(true, 1.0, 5, 1, 0, 0).reply());
        assertEquals(ShopRules.Reply.NOTHING, ShopRules.judgeSell(true, 1.0, 5, 1, -2, 0).reply());
        assertEquals(ShopRules.Reply.CLOSED, ShopRules.judgeSell(false, 99.0, 0, 0, 0, 0).reply(), "closed beats everything");
    }

    @Test
    void theBuyPriceFollowsTheServersScale() {
        assertEquals(2_347_000, ShopRules.buyPrice(2_347_000, 1.0));
        assertEquals(4_694_000, ShopRules.buyPrice(2_347_000, 2.0));
        assertEquals(1_174_000, ShopRules.buyPrice(2_347_000, 0.5));
        assertEquals(2_347_000, ShopRules.buyPrice(2_347_000, 0.0), "a nonsense scale is read as 1");
        assertEquals(2_347_000, ShopRules.buyPrice(2_347_000, -3.0));
        assertEquals(2_347_000, ShopRules.buyPrice(2_347_000, Double.NaN));
        assertEquals(1, ShopRules.buyPrice(1, 0.0001), "a price is never zero");
        assertTrue(ShopRules.buyPrice(StockSchema.MAX_PRICE, 1_000_000.0) <= GoldLedger.CAP, "a huge scale cannot push a price past the purse cap");
        for (long p = 1; p < 3_000_000; p += 12_347) assertEquals(StockRoll.nice(p), ShopRules.buyPrice(StockRoll.nice(p), 1.0), "scale 1 changes no rolled price: " + p);
    }

    @Test
    void theSellUnitFollowsTheServersScaleAndRoundsDown() {
        assertEquals(50_000, ShopRules.sellUnit(50_000, 1.0));
        assertEquals(25_000, ShopRules.sellUnit(50_000, 0.5));
        assertEquals(1, ShopRules.sellUnit(3, 0.5), "3 * 0.5 rounds down to 1");
        assertEquals(0, ShopRules.sellUnit(1, 0.5), "scaled to nothing: the witch will not buy it");
        assertEquals(0, ShopRules.sellUnit(50_000, 0.0), "scale 0 switches buying off");
        assertEquals(50_000, ShopRules.sellUnit(50_000, -1.0), "a negative scale is read as 1");
        assertEquals(50_000, ShopRules.sellUnit(50_000, Double.NaN));
        assertTrue(ShopRules.sellUnit(PriceTable.MAX_PRICE, 1_000_000_000.0) <= GoldLedger.CAP);
    }

    @Test
    void repliesTravelAsNumbersAndNeverBreakOnAnUnknownOne() {
        for (ShopRules.Reply r : ShopRules.Reply.values()) assertEquals(r, ShopRules.Reply.byOrdinal(r.ordinal()));
        assertEquals(ShopRules.Reply.NONE, ShopRules.Reply.byOrdinal(-1));
        assertEquals(ShopRules.Reply.NONE, ShopRules.Reply.byOrdinal(999));
        assertEquals("gui.nusmp.shop.reply.sold_out", ShopRules.Reply.SOLD_OUT.key());
        assertTrue(ShopRules.Reply.BOUGHT.ok() && ShopRules.Reply.SOLD.ok() && ShopRules.Reply.OPENED.ok() && ShopRules.Reply.NONE.ok());
        assertFalse(ShopRules.Reply.POOR.ok() || ShopRules.Reply.FAR.ok() || ShopRules.Reply.FAILED.ok());
        assertEquals(0, ShopRules.Reply.NONE.ordinal(), "NONE is the zero default of a packet");
    }
}
