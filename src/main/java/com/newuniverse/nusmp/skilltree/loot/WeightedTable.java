package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.function.IntPredicate;

/**
 * Why: with 283 lines the old pick ({@link RoutePick}: add the weights up on every call, walk the array) is fine for one drop but wasteful as a habit, and the loot code that called it
 * also built the weight array from the registry's list on EVERY drop. A drop is rare, but a global loot modifier runs on every kill and chest roll of the dungeon, and the owner's
 * rule is "pick uniformly weighted by loot_weight without building a list per drop". This is the weights summed ONCE into cumulative totals (built when the registry changes, see
 * {@link RouteTable}); a pick is then one dice value, one multiplication and a binary search, O(log n), no allocation. It answers exactly what {@link RoutePick} answers for the same dice
 * (the unit test cross-checks them on tens of thousands of random weight sets and dice values), so the proportions the owner already approved do not move: a weight of 0 or less is
 * never chosen, nothing positive answers -1 instead of guessing, and a dice that returns exactly 1.0 cannot walk off the end.
 *
 * <p>{@link #pickAvoiding} is what the pity rule needs ({@link BookPity}): the same weighted pick, redone up to {@code tries} times while the caller says "not this one" (a line the player
 * already owns), then a guaranteed scan from a random start for any acceptable entry, so "never" really means never unless every line is unacceptable (then the plain first pick stands:
 * a player who owns every line can still be handed a duplicate, which is worth Skill Points). Pure: no Minecraft type, immutable after construction, safe to share between threads.
 */
public final class WeightedTable {
    public static final WeightedTable EMPTY = new WeightedTable(new long[0], new int[0]);

    private final long[] cumulative;     // cumulative[i] = sum of the positive weights up to and including entry i (only positive entries are stored)
    private final int[] original;        // original[i] = the index in the caller's array of entry i

    private WeightedTable(long[] cumulative, int[] original) {
        this.cumulative = cumulative;
        this.original = original;
    }

    /** Builds the table; entries with a weight of 0 or less are left out (never picked). */
    public static WeightedTable of(int[] weights) {
        if (weights == null) return EMPTY;
        int positive = 0;
        for (int w : weights) if (w > 0) positive++;
        if (positive == 0) return EMPTY;
        long[] cum = new long[positive];
        int[] orig = new int[positive];
        long sum = 0;
        int k = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] <= 0) continue;
            sum += weights[i];
            cum[k] = sum;
            orig[k++] = i;
        }
        return new WeightedTable(cum, orig);
    }

    public boolean isEmpty() { return cumulative.length == 0; }

    /** How many entries can be picked (positive weight). */
    public int size() { return cumulative.length; }

    /** The sum of the positive weights. */
    public long total() { return cumulative.length == 0 ? 0 : cumulative[cumulative.length - 1]; }

    /** The weight of an entry by its index in the original array (0 when it cannot be picked). */
    public int weightOf(int originalIndex) {
        for (int i = 0; i < original.length; i++) {
            if (original[i] == originalIndex) return (int) (cumulative[i] - (i == 0 ? 0 : cumulative[i - 1]));
        }
        return 0;
    }

    /** The original index picked with probability proportional to its weight, or -1 when the table is empty or the dice is null. O(log n), allocation free. */
    public int pick(Dice dice) {
        if (dice == null || cumulative.length == 0) return -1;
        long total = cumulative[cumulative.length - 1];
        long r = Math.min(total - 1, (long) (dice.next() * total));
        int lo = 0, hi = cumulative.length - 1;
        while (lo < hi) {                                   // the first entry whose running total exceeds r
            int mid = (lo + hi) >>> 1;
            if (cumulative[mid] > r) hi = mid;
            else lo = mid + 1;
        }
        return original[lo];
    }

    /**
     * A weighted pick that avoids the entries {@code avoid} accepts: up to {@code tries} more weighted draws, then a scan of every entry from a random start. Returns the plain first pick when
     * every entry is to be avoided (nothing better exists), and -1 only when the table is empty.
     */
    public int pickAvoiding(Dice dice, IntPredicate avoid, int tries) {
        int first = pick(dice);
        if (first < 0 || avoid == null || !avoid.test(first)) return first;
        for (int i = 0; i < tries; i++) {
            int p = pick(dice);
            if (!avoid.test(p)) return p;
        }
        int start = dice.nextInt(original.length);
        for (int k = 0; k < original.length; k++) {
            int candidate = original[(start + k) % original.length];
            if (!avoid.test(candidate)) return candidate;
        }
        return first;
    }
}
