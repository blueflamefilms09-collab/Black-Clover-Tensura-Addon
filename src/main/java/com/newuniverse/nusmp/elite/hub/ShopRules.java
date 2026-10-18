package com.newuniverse.nusmp.elite.hub;

/**
 * Why: a purchase is validated on the SERVER from facts the client cannot lie about, and the order of the checks decides what a refused player is told, so the decision is one pure function with a
 * verdict, tested over every combination. The client sends only an offer id; the price is read from the table here (never from the packet), the daily stock is the player's own counter, the floor
 * lock is their saved deepest floor, and the money is the wallet's. The price is the table price times the server's {@code shop.priceScale}, rounded, never below 0.
 */
public final class ShopRules {
    private ShopRules() {}

    /** Why a purchase was or was not made. */
    public enum Verdict { OK, SHOP_OFF, UNKNOWN_OFFER, LOCKED, SOLD_OUT, CANNOT_AFFORD }

    /** The price of an offer after the server's scale (bronze). */
    public static long price(ShopTable.Offer o, double scale) {
        double v = o.price() * Math.max(0.0, scale);
        return v >= CoinMath.CAP ? CoinMath.CAP : Math.round(v);
    }

    /** Stacks the player may still buy today: {@code -1} = no limit. */
    public static int remaining(ShopTable.Offer o, int boughtToday) {
        return o.dailyLimit() <= 0 ? -1 : Math.max(0, o.dailyLimit() - Math.max(0, boughtToday));
    }

    /** The verdict for a purchase of one stack. */
    public static Verdict decide(boolean shopOn, ShopTable.Offer offer, int boughtToday, int deepestFloor, long balance, double priceScale) {
        if (!shopOn) return Verdict.SHOP_OFF;
        if (offer == null) return Verdict.UNKNOWN_OFFER;
        if (offer.minFloor() > deepestFloor) return Verdict.LOCKED;
        if (remaining(offer, boughtToday) == 0) return Verdict.SOLD_OUT;
        if (balance < price(offer, priceScale)) return Verdict.CANNOT_AFFORD;
        return Verdict.OK;
    }
}
