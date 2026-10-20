package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Requip Armoury is the pocket space of Fairy Tail's Equip Magic: a per-player store that the user fills and empties at will. What it may hold, how many slots a user has, and how a stack
 * moves in or out without ever being lost or doubled is arithmetic that must not be checked by hand in game. This class is that arithmetic, generic over the stack type and free of Minecraft classes
 * ({@link Ops} is the small adaptor: the game wraps {@code ItemStack}, the tests wrap a record), so {@code ArmouryRulesTest} can hold it to the two promises: nothing is duplicated and nothing is lost.
 * <p>
 * Capacity: slots are the first {@code capacity} cells of a fixed {@link #MAX_SLOTS} grid; a cell past the capacity (the user lost mastery, a config changed) still holds what it holds and can still
 * be taken out, it just takes nothing new. The capacity comes from mastery, the Pocket Capacity passive and max EP.
 */
public final class ArmouryRules {
    private ArmouryRules() {}

    /** The grid is nine columns by six rows. */
    public static final int COLUMNS = 9, ROWS = 6, MAX_SLOTS = COLUMNS * ROWS;
    /** The fewest slots anyone with the skill has. */
    public static final int MIN_CAPACITY = 18;

    /** What the rules need to know about a stack; {@code S} is {@code ItemStack} in the game. Stacks are treated as values: the rules never share one between two cells. */
    public interface Ops<S> {
        S empty();
        boolean isEmpty(S s);
        int count(S s);
        /** The most one cell holds of this stack's item (64 for most, 1 for tools). */
        int max(S s);
        /** True when the two may merge into one cell: the same item and the same components. */
        boolean same(S a, S b);
        /** A copy of the stack with another count; a count of 0 or less gives {@link #empty()}. */
        S withCount(S s, int count);
    }

    // ------------------------------------------------------------ capacity
    /** The share of the grid max EP allows: nothing below 25,000, then 27, 36, 45 and the whole 54 slots from 250,000. */
    public static int epCap(double maxEp) {
        if (maxEp >= 250_000) return MAX_SLOTS;
        if (maxEp >= 100_000) return 45;
        if (maxEp >= 50_000) return 36;
        if (maxEp >= 25_000) return 27;
        return MIN_CAPACITY;
    }

    /** Slots mastery alone gives: 18 at the start, one more per 5.6 percent, 36 at full mastery. */
    public static int masteryCap(double mastery) {
        return MIN_CAPACITY + (int) Math.floor(18.0 * clamp01(mastery));
    }

    /** The Pocket Capacity passive: +6 slots from 35 percent mastery, +12 from 60 percent, +18 from 85 percent. */
    public static int pocketBonus(double mastery) {
        double m = clamp01(mastery);
        return m >= 0.85 ? 18 : m >= 0.60 ? 12 : m >= 0.35 ? 6 : 0;
    }

    /** The slots a user has: mastery plus the passive, held to what max EP allows and to the grid. An operator or a creative player has the whole grid. */
    public static int capacity(double mastery, double maxEp, boolean privileged) {
        if (privileged) return MAX_SLOTS;
        int byMastery = masteryCap(mastery) + pocketBonus(mastery);
        return Math.max(MIN_CAPACITY, Math.min(MAX_SLOTS, Math.min(byMastery, epCap(maxEp))));
    }

    // ------------------------------------------------------------ storing
    /**
     * Puts a stack into the cells below {@code capacity}: it first tops up equal stacks, then fills empty cells. Returns what did not fit (empty when it all went in). The cells are changed in place;
     * the total count of the cells plus the returned remainder always equals the total before plus the stack.
     */
    public static <S> S insert(List<S> cells, int capacity, S stack, Ops<S> ops) {
        if (stack == null || ops.isEmpty(stack)) return ops.empty();
        int limit = Math.min(capacity, cells.size());
        int left = ops.count(stack);
        int max = Math.max(1, ops.max(stack));
        for (int i = 0; i < limit && left > 0; i++) {
            S c = cells.get(i);
            if (ops.isEmpty(c) || !ops.same(c, stack)) continue;
            int room = max - ops.count(c);
            if (room <= 0) continue;
            int add = Math.min(room, left);
            cells.set(i, ops.withCount(c, ops.count(c) + add));
            left -= add;
        }
        for (int i = 0; i < limit && left > 0; i++) {
            if (!ops.isEmpty(cells.get(i))) continue;
            int add = Math.min(max, left);
            cells.set(i, ops.withCount(stack, add));
            left -= add;
        }
        return left <= 0 ? ops.empty() : ops.withCount(stack, left);
    }

    /** All or nothing: stores every stack, or changes no cell and returns false (the armoury is too full). */
    public static <S> boolean insertAll(List<S> cells, int capacity, List<S> stacks, Ops<S> ops) {
        List<S> trial = new ArrayList<>(cells);
        for (S s : stacks) {
            if (s == null || ops.isEmpty(s)) continue;
            if (!ops.isEmpty(insert(trial, capacity, s, ops))) return false;
        }
        for (int i = 0; i < cells.size(); i++) cells.set(i, trial.get(i));
        return true;
    }

    /** Takes up to {@code amount} out of one cell. Returns the taken part (empty for a bad index or an empty cell). */
    public static <S> S take(List<S> cells, int index, int amount, Ops<S> ops) {
        if (index < 0 || index >= cells.size() || amount <= 0) return ops.empty();
        S c = cells.get(index);
        if (ops.isEmpty(c)) return ops.empty();
        int n = Math.min(amount, ops.count(c));
        S out = ops.withCount(c, n);
        cells.set(index, ops.withCount(c, ops.count(c) - n));
        return out;
    }

    public enum Result { OK, PARTIAL, FULL, EMPTY, BAD_INDEX }

    /**
     * Moves the whole stack in one cell of {@code from} (the user's inventory) into the armoury. OK when all went in, PARTIAL when some stayed behind, FULL when nothing fitted. Nothing is removed from
     * {@code from} before it is stored, and only what was stored is removed, so a stack is never lost or doubled.
     */
    public static <S> Result store(List<S> from, int index, List<S> armoury, int capacity, Ops<S> ops) {
        if (index < 0 || index >= from.size()) return Result.BAD_INDEX;
        S s = from.get(index);
        if (ops.isEmpty(s)) return Result.EMPTY;
        S rest = insert(armoury, capacity, s, ops);
        if (ops.isEmpty(rest)) { from.set(index, ops.empty()); return Result.OK; }
        if (ops.count(rest) == ops.count(s)) return Result.FULL;
        from.set(index, rest);
        return Result.PARTIAL;
    }

    /** Moves the whole stack in one armoury cell into the user's inventory cells ({@code to}, all of them usable). The same promises as {@link #store}. */
    public static <S> Result retrieve(List<S> armoury, int index, List<S> to, Ops<S> ops) {
        if (index < 0 || index >= armoury.size()) return Result.BAD_INDEX;
        S s = armoury.get(index);
        if (ops.isEmpty(s)) return Result.EMPTY;
        S rest = insert(to, to.size(), s, ops);
        if (ops.isEmpty(rest)) { armoury.set(index, ops.empty()); return Result.OK; }
        if (ops.count(rest) == ops.count(s)) return Result.FULL;
        armoury.set(index, rest);
        return Result.PARTIAL;
    }

    // ------------------------------------------------------------ reading
    /** The sum of the counts in the cells: the number the no-duplication tests compare before and after. */
    public static <S> long total(List<S> cells, Ops<S> ops) {
        long n = 0;
        for (S c : cells) if (c != null && !ops.isEmpty(c)) n += ops.count(c);
        return n;
    }

    /** Empty cells below the capacity. */
    public static <S> int freeSlots(List<S> cells, int capacity, Ops<S> ops) {
        int n = 0;
        for (int i = 0; i < Math.min(capacity, cells.size()); i++) if (ops.isEmpty(cells.get(i))) n++;
        return n;
    }

    /** True when the armoury has room for the stack right now (merging counts). */
    public static <S> boolean fits(List<S> cells, int capacity, S stack, Ops<S> ops) {
        List<S> trial = new ArrayList<>(cells);
        return ops.isEmpty(insert(trial, capacity, stack, ops));
    }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }
}
