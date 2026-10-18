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

    /** floor(d * n) kept inside 0..n-1, so a dice that returns exactly 1.0 (or NaN) cannot overrun. */
    static int clampIndex(double d, int n) {
        if (n <= 1) return 0;
        if (Double.isNaN(d) || d < 0) return 0;
        int i = (int) Math.floor(d * n);
        return Math.min(n - 1, Math.max(0, i));
    }
}
