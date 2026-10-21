package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.gen.Clearance7;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the {@code seal_doors} primitive bars the doorways of a room with a block until its mechanic is beaten. It was written for the doorway of the first generator, 3 wide and
 * {@link MegaGrid#DOOR_H} (4) high, and it found the open columns of a wall by scanning one row and then stacked four blocks on each. A sector made with the 7 x 7 clearance
 * ({@code gen.clearance7}, docs/labyrinth/clearance7.md) has doorways 7 wide and 7 high, so four blocks would leave three rows open above the bar (not reachable on foot, reachable
 * by anything that flies or climbs, and by a Tensura giant who simply steps over it), and a long wall with five or more such doorways has more open columns than the "a whole side
 * that is open is a cavern, not a doorway" limit of 30. These are the two numbers, as plain integers so they can be tested: how many rows to seal, and how many open columns one
 * side of the room may show before it is judged a cavern. A room of a sector that was not cut keeps 4 rows and the limit of 30 exactly. Pure.
 */
public final class DoorSealing {
    private DoorSealing() {}

    /** The most open columns a side may show and still be a row of doorways, in a room that was not cut. */
    public static final int OLD_SIDE_LIMIT = 30;

    /** How many rows of a doorway the bar takes: the 7 of the clearance rule when the room was cut, the 4 of the old doorway otherwise. */
    public static int rows(boolean cut) { return cut ? Clearance7.REQUIRED : MegaGrid.DOOR_H; }

    /**
     * The most open columns one side of a room may show before the side is a cavern: 30 for a room that was not cut; for a cut room one 7-wide doorway per module of the side (at least
     * three), and never fewer than 30.
     */
    public static int sideLimit(boolean cut, int sideLength) {
        if (!cut) return OLD_SIDE_LIMIT;
        int doors = Math.max(3, sideLength / MegaGrid.MODULE);
        return Math.max(OLD_SIDE_LIMIT, doors * Clearance7.REQUIRED);
    }

    /** Judges one side: {@code open} columns were found; are they doorways (true) or a cavern (false)? */
    public static boolean isDoorways(boolean cut, int sideLength, int open) { return open <= sideLimit(cut, sideLength); }

    /** A check of the numbers for the self-test {@code dungeon.clearance7}: returns the problems (empty = they hold). */
    public static List<String> selfCheck() {
        List<String> bad = new ArrayList<>();
        if (rows(false) != 4) bad.add("an old doorway is barred " + rows(false) + " rows high, not 4");
        if (rows(true) != Clearance7.REQUIRED) bad.add("a 7 x 7 doorway is barred " + rows(true) + " rows high, not " + Clearance7.REQUIRED);
        if (sideLimit(false, 27) != OLD_SIDE_LIMIT || sideLimit(false, 90) != OLD_SIDE_LIMIT) bad.add("the limit of a room that was not cut changed");
        if (!isDoorways(true, 27, 3 * Clearance7.REQUIRED)) bad.add("three 7-wide doorways in a wall of 27 are judged a cavern");
        if (!isDoorways(true, 45, 5 * Clearance7.REQUIRED)) bad.add("five 7-wide doorways in a wall of 45 are judged a cavern");
        if (isDoorways(false, 45, 5 * Clearance7.REQUIRED)) bad.add("the old limit accepts 35 open columns (the check is not meaningful)");
        if (isDoorways(true, 45, 43)) bad.add("a wall open all along 45 blocks (a cavern) is judged doorways in a cut room");
        return bad;
    }
}
