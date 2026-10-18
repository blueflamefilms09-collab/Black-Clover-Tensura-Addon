package com.newuniverse.nusmp.elite.items.crate;

import java.util.List;

/**
 * Why: the parsed reward table of one crate tier: how many times the crate rolls (a range, so an Elite Crate can give 3 to 4 things) and the weighted entries it rolls on. Immutable; the loader keeps
 * one per tier and swaps them on a datapack reload.
 */
public record CrateTable(String tier, int rollsMin, int rollsMax, List<RewardEntry> entries) {
    public CrateTable {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (rollsMin < 1) rollsMin = 1;
        if (rollsMax < rollsMin) rollsMax = rollsMin;
    }

    /** Sum of all weights (the denominator of a roll). */
    public long totalWeight() {
        long t = 0;
        for (RewardEntry e : entries) t += e.weight();
        return t;
    }
}
