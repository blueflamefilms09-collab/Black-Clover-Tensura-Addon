package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dungeon armour already carries randomised stat lines (the pre-rolled {@code nusmp:gear_roll}); what was missing is a way for a CHEST, or a lucky kill, to hand one out
 * at all, and for Luck and depth to decide how good it is. Three decisions, all pure: WHICH piece (any piece of a set whose first depth band the floor has reached, uniformly, so a
 * first-floor chest never holds the deepest set); HOW GOOD (the rarity the existing weights give at this band and Luck, one band better for a boss, never above the last band);
 * and THE SEED of the roll (drawn from the dice, so two chests give two different pieces and a test with a seeded dice gets the same piece every time). The roll of the stat lines
 * itself is {@link GearRollMath}, untouched. Pure.
 */
public final class LuckArmour {
    private LuckArmour() {}

    /** The piece, its rarity and the seed its lines are rolled from. */
    public record Pick(String id, LootRarity rarity, long seed) {}

    /** The ids of the armour pieces a floor of this band can hold: the pieces of every set whose first band is reached; the first set's pieces when none is (never empty if there is any armour). */
    public static List<String> candidates(LootRoster roster, int band) {
        List<String> out = new ArrayList<>();
        int lowest = Integer.MAX_VALUE;
        for (LootRoster.ArmourDef a : roster.armour()) {
            LootRoster.ArmourSetDef set = roster.armourSets().get(a.set());
            int first = set == null ? 0 : set.minBand();
            lowest = Math.min(lowest, first);
            if (first <= band) out.add(a.id());
        }
        if (out.isEmpty()) {
            for (LootRoster.ArmourDef a : roster.armour()) {
                LootRoster.ArmourSetDef set = roster.armourSets().get(a.set());
                if ((set == null ? 0 : set.minBand()) == lowest) out.add(a.id());
            }
        }
        return out;
    }

    /** One pick, or null when there is no armour at all. A boss counts as one band deeper. */
    public static Pick pick(LootRoster roster, int depth, double luck, boolean boss, Dice dice) {
        int band = Math.min(LootBand.BANDS - 1, LootBand.of(depth) + (boss ? 1 : 0));
        List<String> ids = candidates(roster, band);
        if (ids.isEmpty()) return null;
        String id = ids.get(dice.nextInt(ids.size()));
        LootRarity rarity = LootRarity.pick(dice.next(), band, LuckGauss.luck(luck));
        long seed = ((long) (dice.next() * (1L << 31)) << 31) ^ (long) (dice.next() * (1L << 31));
        return new Pick(id, rarity, seed);
    }
}
