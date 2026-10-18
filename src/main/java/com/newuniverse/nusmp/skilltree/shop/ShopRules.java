package com.newuniverse.nusmp.skilltree.shop;

/**
 * Why: the server is the only authority in the shop (the client only draws and asks), so every decision of a purchase or a sale is made here, as a pure function of plain
 * numbers, and the packet handlers merely gather those numbers from the game and carry out the verdict. That is what makes the rules testable without a running game, and
 * it fixes ONE order of refusals for the whole mod: closed, too far from the witch, no such offer, sold out, price changed under the player's finger (a restock or a config
 * reload between the screen and the click: the player is told instead of charged more than they saw), daily limit, too poor. The same verdict becomes the witch's line in the
 * screen, so a player always learns why a purchase did not happen. Sales are clamped so that the purse can never overflow and the amount of gold is exact.
 */
public final class ShopRules {
    private ShopRules() {}

    /** How far from the witch (blocks) a player may be and still trade. The screen is opened from arm's reach; the extra room lets a step back not cancel a deal. */
    public static final double REACH = 8.0;

    /** What happened, in the order the checks run. The numbers go on the wire (the screen picks the witch's line from them), so only ever APPEND entries. */
    public enum Reply {
        NONE(true), OPENED(true), BOUGHT(true), SOLD(true),
        CLOSED(false), FAR(false), UNKNOWN(false), SOLD_OUT(false), PRICE_MOVED(false), LIMIT(false), POOR(false), NOTHING(false), FULL_PURSE(false), FAILED(false);

        private final boolean ok;

        Reply(boolean ok) {
            this.ok = ok;
        }

        /** True when nothing went wrong. */
        public boolean ok() { return ok; }

        /** The language key stem of the witch's line for this reply ({@code gui.nusmp.shop.reply.sold_out}; the screen adds {@code .1} or {@code .2}). */
        public String key() { return "gui.nusmp.shop.reply." + name().toLowerCase(java.util.Locale.ROOT); }

        /** The reply with this wire number, or {@link #NONE} for a number from a newer or damaged packet. */
        public static Reply byOrdinal(int n) {
            Reply[] all = values();
            return n >= 0 && n < all.length ? all[n] : NONE;
        }
    }

    // ================================================================ buying
    /**
     * Everything a purchase depends on.
     *
     * @param enabled       the shop switch of the config
     * @param distance      blocks to the nearest witch, or NaN / infinity when none is near
     * @param known         the offer exists on the shelf
     * @param left          copies left of it
     * @param price         its price right now (scaled)
     * @param expectedPrice the price the player saw when they clicked
     * @param gold          the player's purse
     * @param dailyLimit    purchases a player may make per in-game day, 0 for no limit
     * @param boughtToday   purchases the player has made today
     */
    public record BuyCase(boolean enabled, double distance, boolean known, int left, long price, long expectedPrice, long gold, int dailyLimit, int boughtToday) {}

    /** The verdict on a purchase: {@link Reply#BOUGHT} when it may go ahead, otherwise the reason it may not. */
    public static Reply judgeBuy(BuyCase c) {
        if (!c.enabled()) return Reply.CLOSED;
        if (!(c.distance() <= REACH)) return Reply.FAR;
        if (!c.known()) return Reply.UNKNOWN;
        if (c.left() <= 0) return Reply.SOLD_OUT;
        if (c.expectedPrice() != c.price()) return Reply.PRICE_MOVED;
        if (limitReached(c.dailyLimit(), c.boughtToday())) return Reply.LIMIT;
        if (!GoldLedger.canAfford(c.gold(), c.price())) return Reply.POOR;
        return Reply.BOUGHT;
    }

    /** True when a limit is set and the player has used it up. */
    public static boolean limitReached(int dailyLimit, int boughtToday) {
        return dailyLimit > 0 && boughtToday >= dailyLimit;
    }

    /** Purchases counted for {@code today}: the stored counter only counts if it was written today. */
    public static int boughtToday(long storedDay, int storedCount, long today) {
        return storedDay == today ? Math.max(0, storedCount) : 0;
    }

    /** Purchases the player may still make today, or -1 for no limit (what the screen shows). */
    public static int dailyLeft(int dailyLimit, int boughtToday) {
        return dailyLimit <= 0 ? -1 : Math.max(0, dailyLimit - boughtToday);
    }

    // ================================================================ selling
    /** The verdict on a sale: the reply, how many items go, and the gold they fetch. */
    public record Sale(Reply reply, int count, long gold) {
        public static Sale refused(Reply why) { return new Sale(why, 0, 0); }
    }

    /**
     * Judges selling up to {@code want} of a stack of {@code have} items worth {@code unit} gold each to a witch {@code distance} blocks away, into a purse of {@code purse}.
     * The count is clamped to what the stack holds and to what the purse still has room for, so a sale is never partly lost to the cap.
     */
    public static Sale judgeSell(boolean enabled, double distance, long unit, int have, int want, long purse) {
        if (!enabled) return Sale.refused(Reply.CLOSED);
        if (!(distance <= REACH)) return Sale.refused(Reply.FAR);
        if (unit <= 0 || have <= 0 || want <= 0) return Sale.refused(Reply.NOTHING);
        long fit = GoldLedger.maxCount(unit, GoldLedger.room(purse));
        if (fit <= 0) return Sale.refused(Reply.FULL_PURSE);
        int n = (int) Math.min(Math.min(have, want), fit);
        return new Sale(Reply.SOLD, n, GoldLedger.mul(unit, n));
    }

    // ================================================================ prices
    /** The price the player pays: the rolled price times the server's {@code priceScale}, then rounded to a market number; never below 1. */
    public static long buyPrice(long rolled, double scale) {
        double s = scale > 0 && Double.isFinite(scale) ? scale : 1.0;
        return Math.min(GoldLedger.CAP, Math.max(1, StockRoll.nice(Math.max(1, toGold(rolled * s)))));
    }

    /** The gold the witch pays for ONE item: the table's price times the server's {@code sellScale}, rounded down; zero when the scale makes it worthless. */
    public static long sellUnit(long base, double scale) {
        double s = scale >= 0 && Double.isFinite(scale) ? scale : 1.0;
        return toGold(Math.floor(base * s));
    }

    /**
     * A double as a legal amount of gold, rounded to the nearest whole number and never above the cap. The cap is NOT exactly representable as a double (it rounds up to 2^61),
     * so the comparison is made before the cast, never after: {@code (long) (double) CAP} would be one more than the cap.
     */
    private static long toGold(double value) {
        if (!(value > 0.0)) return 0;
        if (value >= (double) GoldLedger.CAP) return GoldLedger.CAP;
        return GoldLedger.clamp(Math.round(value));
    }
}
