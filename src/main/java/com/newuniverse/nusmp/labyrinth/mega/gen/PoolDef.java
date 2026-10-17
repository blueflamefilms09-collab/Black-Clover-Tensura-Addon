package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.List;

/**
 * Why: a template pool reduced to the three things the assembler uses: the weighted templates, the weight of the "empty element" (the vanilla
 * way of saying "or nothing here"), and the fallback pool that supplies the door caps. It is read from the pool JSON by the game-side adapter
 * and from {@code catalog.json} by the tests, so both run the very same search. The empty element is NOT a template: when the weighted draw reaches
 * it the primary candidates are over (vanilla stops the whole search there; the assembler goes on with the fallback pool instead, because a doorway
 * with nothing behind it would be a hole into the void).
 *
 * @param id          the pool id, e.g. {@code nusmp:mega/side}
 * @param entries     the weighted templates, in file order
 * @param emptyWeight total weight of the empty elements (0 when the pool has none)
 * @param fallback    the pool tried after this one's candidates, {@code minecraft:empty} for none
 */
public record PoolDef(String id, List<PoolEntry> entries, int emptyWeight, String fallback) {
    public PoolDef {
        entries = List.copyOf(entries);
        if (emptyWeight < 0) emptyWeight = 0;
        if (fallback == null || fallback.isEmpty()) fallback = MegaGrid.P_EMPTY;
    }

    /** The sum of the template weights (without the empty element). */
    public int totalWeight() {
        int t = 0;
        for (PoolEntry e : entries) t += e.weight();
        return t;
    }

    /** A pool with no templates at all. */
    public boolean isEmpty() { return entries.isEmpty(); }
}
