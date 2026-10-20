package com.newuniverse.nusmp.labyrinth.loot;

import java.util.List;

/**
 * Why: this is the whole of what the data component {@code nusmp:gear_roll} carries, and the component is what makes an item a PRE-ROLLED piece of dungeon gear: its rarity,
 * the seed and the floor it was rolled from (so the same drop can be re-rolled identically for a test or an admin command), and its two to four stat lines. It is a plain
 * record of values with value equality, so two stacks rolled alike stack alike. It sits in the pure part (no Minecraft import) because the roll maths are tested without a
 * server; the codecs that save and sync it are in {@link LootComponents}. The stat lines are ALSO written into the stack's vanilla attribute-modifier component when the
 * piece is made, so the numbers a player reads in the tooltip are the numbers the game applies, in any slot.
 *
 * @param tempered true once a Shadow Core Shard has been worked into the piece at an anvil (it can be done once)
 */
public record GearRoll(LootRarity rarity, long seed, int depth, List<Line> lines, boolean tempered) {
    public GearRoll {
        if (rarity == null) rarity = LootRarity.COMMON;
        depth = Math.max(0, depth);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** One stat line: what it is and how much. */
    public record Line(StatKind kind, double value) {}

    /** The sum of the lines of one kind. */
    public double sum(StatKind kind) {
        double s = 0.0;
        for (Line l : lines) if (l.kind() == kind) s += l.value();
        return s;
    }

    public boolean has(StatKind kind) {
        for (Line l : lines) if (l.kind() == kind) return true;
        return false;
    }

    public GearRoll withTemper(Line extra) {
        java.util.ArrayList<Line> all = new java.util.ArrayList<>(lines);
        all.add(extra);
        return new GearRoll(rarity, seed, depth, all, true);
    }
}
