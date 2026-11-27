package com.newuniverse.nusmp.labyrinth.mega.weekly;

import java.util.Arrays;

/**
 * Why: the first-clear reward of the dungeon (replay value: a Guardian pays each player once, the first time THEY see it fall, so a second run through the same floor is for the
 * tension and the loot, not for the same one-off prize). The rule is pure: a player's record is a short sorted list of sector ids, {@link #claim} is true exactly once per sector and
 * never grows past the cap (a full record pays nothing more, so the saved data of one player stays small), and the size of the prize grows with the floor. No Minecraft import.
 */
public final class FirstClearRules {
    private FirstClearRules() {}

    /** The most sectors one player's record holds (4 bytes each). */
    public static final int RECORD_CAP = 512;

    /** The new record when {@code sectorId} is claimed for the first time, or null when it was claimed before or the record is full. The input array is not changed. */
    public static int[] claim(int[] record, int sectorId) {
        int[] r = record == null ? new int[0] : record;
        if (Arrays.binarySearch(r, sectorId) >= 0 || r.length >= RECORD_CAP) return null;
        int[] out = Arrays.copyOf(r, r.length + 1);
        out[r.length] = sectorId;
        Arrays.sort(out);
        return out;
    }

    /** Experience levels of the prize: 1 at the first floors, +1 per 10 floors, at most 30. */
    public static int levels(int floor) { return Math.max(1, Math.min(30, 1 + Math.max(0, floor) / 10)); }

    /** How many of the prize item: 1 at the first floors, +1 per 20 floors, at most 16. */
    public static int items(int floor) { return Math.max(1, Math.min(16, 1 + Math.max(0, floor) / 20)); }
}
