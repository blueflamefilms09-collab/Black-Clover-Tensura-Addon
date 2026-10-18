package com.newuniverse.nusmp.elite.hub;

/**
 * Why: a wallet that counts coins, takes a price out of mixed coins and hands change back is pure arithmetic, and the one thing that must never happen with money is a silent mistake (a coin
 * lost, minted or a total that wraps around). So the arithmetic lives here with no game type in sight and is tested at the edges; the game side ({@link CoinWallet}) only reads the inventory into a
 * count array, asks this class what to do, and does exactly that. Counts are indexed by {@link CoinKind#ordinal()}. All values are bronze coins and every sum saturates at {@link #CAP} (a
 * quarter of {@code Long.MAX_VALUE}, the same reason as the Black Market's {@code GoldLedger}: two capped amounts still add without wrapping). Three operations. {@link #value} adds a
 * holding up. {@link #mix} turns an amount into the BEST coin mix, fewest coins, biggest first (a gold coin before a hundred silver), which is how change and rewards are paid.
 * {@link #plan} turns a price and a holding into the coins to take: the SMALL coins go first (a player's gold coins are the ones they save), and when the small ones run out the next coin
 * that covers the rest is taken whole and the overshoot comes back as change, so the player always pays exactly the price and never a coin more.
 */
public final class CoinMath {
    private CoinMath() {}

    /** The most any amount of money can be (about 2.3 x 10^18 bronze). */
    public static final long CAP = Long.MAX_VALUE / 4;

    /** What a {@link #plan} says to do: take {@code take[k]} coins of each kind, then pay {@code change} bronze back as a {@link #mix}. */
    public record Charge(long[] take, long change) {
        /** True when the plan takes nothing and returns nothing (a price of zero). */
        public boolean isEmpty() {
            if (change != 0) return false;
            for (long t : take) if (t != 0) return false;
            return true;
        }
    }

    /** The total worth of a holding (index = kind ordinal); negative counts count as zero, the sum saturates. */
    public static long value(long[] counts) {
        long sum = 0;
        for (int k = 0; k < CoinKind.COUNT; k++) {
            long c = k < counts.length ? Math.max(0, counts[k]) : 0;
            sum = add(sum, mul(CoinKind.values()[k].value(), c));
        }
        return sum;
    }

    /** {@code a + b}, saturating at {@link #CAP}; negatives count as zero. */
    public static long add(long a, long b) {
        long x = Math.max(0, Math.min(a, CAP)), y = Math.max(0, Math.min(b, CAP));
        return y > CAP - x ? CAP : x + y;
    }

    /** {@code unit * count}, saturating at {@link #CAP}; zero when either is not positive. */
    public static long mul(long unit, long count) {
        if (unit <= 0 || count <= 0) return 0;
        return unit > CAP / count ? CAP : unit * count;
    }

    /** The best coin mix for an amount: the biggest coins first, so the fewest coins. A non-positive amount is the empty mix. */
    public static long[] mix(long amount) {
        long[] out = new long[CoinKind.COUNT];
        long rest = Math.min(Math.max(0, amount), CAP);
        for (int k = CoinKind.COUNT - 1; k >= 0; k--) {
            long v = CoinKind.values()[k].value();
            out[k] = rest / v;
            rest -= out[k] * v;
        }
        return out;
    }

    /**
     * How to pay {@code price} out of {@code have}: null when the holding is worth less than the price (or the price is negative); otherwise the coins to take and the change to give back.
     * A zero price is the empty plan. Small coins first; the coin that crosses the line is taken whole and the overshoot is the change, so {@code value(take) - change == price} always.
     */
    public static Charge plan(long price, long[] have) {
        if (price < 0 || price > CAP) return null;
        long[] take = new long[CoinKind.COUNT];
        if (price == 0) return new Charge(take, 0);
        if (value(have) < price) return null;
        long need = price;
        for (int k = 0; k < CoinKind.COUNT && need > 0; k++) {
            long v = CoinKind.values()[k].value();
            long owned = k < have.length ? Math.max(0, have[k]) : 0;
            long use = Math.min(owned, (need + v - 1) / v);          // enough of this kind to cover what is left, rounding up
            take[k] = use;
            need -= use * v;
        }
        return new Charge(take, need < 0 ? -need : 0);
    }

    /** How many coins an amount needs at the best mix. */
    public static long coinCount(long amount) {
        long n = 0;
        for (long c : mix(amount)) n += c;
        return n;
    }

    /** The amount written as coins, {@code "2 gold, 5 silver, 30 bronze"}, or {@code "0 bronze"}. Used by the shop, the quest panel and the logs. */
    public static String describe(long amount) {
        long[] m = mix(amount);
        StringBuilder sb = new StringBuilder();
        for (int k = CoinKind.COUNT - 1; k >= 0; k--) {
            if (m[k] == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(m[k]).append(' ').append(CoinKind.values()[k].id().replace('_', ' '));
        }
        return sb.length() == 0 ? "0 bronze" : sb.toString();
    }
}
