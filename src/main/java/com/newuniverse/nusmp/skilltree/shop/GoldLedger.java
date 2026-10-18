package com.newuniverse.nusmp.skilltree.shop;

/**
 * Why: gold is the one number of the Black Market and it lives inside a player's save, so every way of changing it has to be impossible to get wrong: it must never go
 * negative (a debt would be a bug, not a feature), never wrap around (a price times a count, or a command with a huge argument, must not turn a rich player poor), and never
 * be changed by a client. Those rules are plain arithmetic on a {@code long}, so they live here with no Minecraft type in sight and are unit tested at the extremes of the
 * number line; the game side ({@link MarketGold}) only reads and writes the number in the player's tree and calls this class for every change. The cap is a quarter of
 * {@code Long.MAX_VALUE} on purpose: two capped purses, or a capped price added to a capped purse, still fit in a {@code long}, so no intermediate sum in the rest of the
 * shop can overflow before it is clamped.
 */
public final class GoldLedger {
    private GoldLedger() {}

    /** The most gold a purse can hold (about 2.3 x 10^18): a quarter of the largest long, so that no sum of two capped amounts can wrap. */
    public static final long CAP = Long.MAX_VALUE / 4;

    /** A stored value as a legal purse: negatives (a corrupted or hand-edited save) become 0, anything above the cap becomes the cap. */
    public static long clamp(long value) {
        return value < 0 ? 0 : Math.min(value, CAP);
    }

    /** The purse after adding {@code amount} (negative removes): saturating at the cap and at zero, whatever the arguments. */
    public static long add(long purse, long amount) {
        long p = clamp(purse);
        if (amount >= 0) return amount > CAP - p ? CAP : p + amount;
        long removed = amount == Long.MIN_VALUE ? Long.MAX_VALUE : -amount;
        return removed >= p ? 0 : p - removed;
    }

    /** The purse after paying {@code amount} (a negative amount pays nothing); never below zero. Ask {@link #canAfford} first when a refusal matters. */
    public static long take(long purse, long amount) {
        long p = clamp(purse);
        if (amount <= 0) return p;
        return amount >= p ? 0 : p - amount;
    }

    /** True when the purse holds at least {@code price} (a negative price is never affordable: it is a bug upstream, not a gift). */
    public static boolean canAfford(long purse, long price) {
        return price >= 0 && clamp(purse) >= price;
    }

    /** How much more the purse can take before it is full. */
    public static long room(long purse) {
        return CAP - clamp(purse);
    }

    /** {@code unit * count}, saturating at the cap; zero when either factor is not positive. */
    public static long mul(long unit, long count) {
        if (unit <= 0 || count <= 0) return 0;
        return unit > CAP / count ? CAP : unit * count;
    }

    /** How many items worth {@code unit} each fit into {@code room} gold of space (zero for a worthless item). */
    public static long maxCount(long unit, long room) {
        return unit <= 0 || room <= 0 ? 0 : room / unit;
    }
}
