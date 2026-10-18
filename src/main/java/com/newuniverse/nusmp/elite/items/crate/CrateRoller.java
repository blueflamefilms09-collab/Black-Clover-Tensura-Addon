package com.newuniverse.nusmp.elite.items.crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * Why: the dice of a crate opening, with no Minecraft in it so the odds are testable: pick the number of rolls in the table's range, then for each roll a weighted entry and an amount in the entry's
 * range. {@code excluded} lists reward types that must not come up (the opening code excludes {@code coins} when the wallet is off and re-rolls that slot, so a crate never pays out less because of a
 * switched-off economy). Deterministic for a given dice, so a test can pin a roll by feeding it numbers.
 */
public final class CrateRoller {
    private CrateRoller() {}

    /** One rolled reward: the entry and how much of it. */
    public record Roll(RewardEntry entry, int amount) {}

    /** The number of rolls this opening makes: uniform over the table's range. */
    public static int rollCount(CrateTable table, DoubleSupplier dice) {
        int span = table.rollsMax() - table.rollsMin() + 1;
        return table.rollsMin() + clampIndex(dice.getAsDouble(), span);
    }

    /** One weighted pick among the entries whose type is not excluded; null when nothing can be rolled (an empty table, or everything excluded). */
    public static Roll rollOne(CrateTable table, DoubleSupplier dice, Set<String> excluded) {
        long total = 0;
        for (RewardEntry e : table.entries()) if (!excluded.contains(e.type())) total += e.weight();
        if (total <= 0) return null;
        long pick = clampIndex(dice.getAsDouble(), (int) Math.min(Integer.MAX_VALUE, total));
        RewardEntry chosen = null;
        for (RewardEntry e : table.entries()) {
            if (excluded.contains(e.type()) || e.weight() <= 0) continue;
            if (pick < e.weight()) { chosen = e; break; }
            pick -= e.weight();
        }
        if (chosen == null) return null;
        int span = chosen.max() - chosen.min() + 1;
        int amount = chosen.min() + clampIndex(dice.getAsDouble(), span);
        return new Roll(chosen, Math.max(1, amount));
    }

    /** A whole opening: {@link #rollCount} picks. */
    public static List<Roll> roll(CrateTable table, DoubleSupplier dice, Set<String> excluded) {
        List<Roll> out = new ArrayList<>();
        int n = rollCount(table, dice);
        for (int i = 0; i < n; i++) {
            Roll r = rollOne(table, dice, excluded);
            if (r != null) out.add(r);
        }
        return out;
    }

    /**
     * A whole opening with a granter: for each roll the granter tries to give the reward and returns what to show, or null when it could not (the economy is off, the skill tree is off). A refused
     * roll is re-rolled with that reward TYPE excluded, up to {@link #ATTEMPTS} tries, so a switched-off system never makes a crate pay less. Generic so the test can use strings.
     */
    public static <T> List<T> open(CrateTable table, DoubleSupplier dice, java.util.function.Function<Roll, T> grant) {
        Set<String> excluded = new java.util.HashSet<>();
        List<T> out = new ArrayList<>();
        int n = rollCount(table, dice);
        for (int i = 0; i < n; i++) {
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                Roll r = rollOne(table, dice, excluded);
                if (r == null) break;
                T given = grant.apply(r);
                if (given != null) { out.add(given); break; }
                excluded.add(r.entry().type());
            }
        }
        return out;
    }

    /** How many times one slot is re-rolled before it is given up. */
    public static final int ATTEMPTS = 4;

    /** floor(d * n) kept inside 0..n-1, so a dice that returns exactly 1.0 (or NaN) cannot overrun. */
    static int clampIndex(double d, int n) {
        if (n <= 1) return 0;
        if (Double.isNaN(d) || d < 0) return 0;
        int i = (int) Math.floor(d * n);
        return Math.min(n - 1, Math.max(0, i));
    }
}
