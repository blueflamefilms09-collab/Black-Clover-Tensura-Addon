package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;

import java.util.Map;

/**
 * Why: the owner wants skill books to fall out of the secret rift chests and the pocket-realm vaults. A pocket's vault holds three chests that use the dungeon's own tables (the relic vault's
 * and the trial's, which the main dungeon also uses), so "a rift chest" cannot be told by its table alone: it must ALSO stand inside a pocket's cell, and the dungeon already has the exact
 * test for that ({@link RiftRules#slotAt}, the field of cells far outside the sectors). This class is the whole decision as pure functions so it can be unit tested without a world: the chance of
 * a table (from the modifier's JSON, times the server's scale, clamped to 0..1, 0 for a table the JSON does not list), whether a position is inside a pocket, and the roll. Which line is picked
 * is not decided here ({@link LootDrops#routeBook}: weighted by {@code loot_weight}, with the pity rule), so the odds of "a book" and the shape of "which book" stay apart as in the rest of the
 * skill-book loot.
 */
public final class RiftBookRules {
    private RiftBookRules() {}

    /** The chance that a roll of this table adds a book: the JSON's number times the config's scale, clamped to 0..1; 0 for an unlisted table. */
    public static double chance(Map<String, Double> tables, String tableId, double scale) {
        if (tables == null || tableId == null) return 0.0;
        Double base = tables.get(tableId);
        if (base == null || !(base > 0.0) || !(scale > 0.0)) return 0.0;
        double p = base * scale;
        return Double.isNaN(p) ? 0.0 : Math.min(1.0, p);
    }

    /** True when the position (a chest's origin) is inside the cell of one of the pocket realms. */
    public static boolean inPocket(double x, double z) { return RiftRules.slotAt(x, z) >= 0; }

    /** The decision: only in the Mega-Dungeon dimension, only inside a pocket, then the roll at the chance. */
    public static boolean decide(boolean inMega, boolean inPocket, double chance, Dice dice) {
        if (!inMega || !inPocket || dice == null || !(chance > 0.0)) return false;
        return chance >= 1.0 || dice.next() < chance;
    }
}
