package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;

/**
 * Why: since 1.55.0 a sector is ten floors, bands of its room graph counted outward from the Landing, and the owner's brief speaks in floors ("Floor +3 is
 * harder than Floor +2"). The Dungeon Master's difficulty therefore has to follow the FLOOR the party stands on, not the sector it stands in: a sector-wide
 * number would make the ten rooms next to the Landing as dangerous as the Guardian's antechamber. This class is the one rule that turns "where the party is"
 * into the single threat number the dice, the budget, the toughness window, the table windows and the health scaling all read: the distance of the floor
 * from the Hub ({@link FloorMap#threat}, at least one, the same for Holy and Abyssal), or, when the floor is unknown (0: outside every sector, or an old save)
 * or the server turned the rule off, the sector's own threat as before. A party is as deep as its deepest member, because the monsters are put round the whole
 * party and the most dangerous place it stands decides how dangerous the encounter is. Pure (the only import is the pure floor arithmetic), so the rule is
 * pinned by unit tests instead of by walking a dungeon.
 */
public final class ThreatInput {
    private ThreatInput() {}

    /**
     * The threat the dice read. {@code floor} is the signed floor of the party (0 = unknown). With {@code useFloor} and a known floor it is
     * {@code max(1, |floor|)}; otherwise the sector's threat, never below one.
     */
    public static int of(int sectorThreat, int floor, boolean useFloor) {
        if (useFloor && floor != 0) return FloorMap.threat(floor);
        return Math.max(1, sectorThreat);
    }

    /**
     * The floor that decides for a party: the one furthest from the Hub (largest absolute value; on a tie the first one given). Floors of 0 mean "not in a
     * sector" and are ignored; an empty list, or one that holds only zeros, answers 0.
     */
    public static int deepest(int... floors) {
        int best = 0;
        if (floors == null) return best;
        for (int f : floors) {
            if (f == 0) continue;
            if (best == 0 || Math.abs((long) f) > Math.abs((long) best)) best = f;
        }
        return best;
    }
}
