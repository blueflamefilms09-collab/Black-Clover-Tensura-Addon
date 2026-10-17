package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: a sector's {@code threat} is read by the Guardian's scaling, the faction drops, the rest ambush and, when a floor is unknown, the encounter director, and
 * until 1.55.0 it was {@code 1 + id}: the number of the sector in the order they were built. With two roads that number means nothing (the Abyssal road's first
 * sector may be sector 5), and with ten floors per sector it is far too slow (the owner's brief makes Floor +11 harder than Floor +10, not sector 1 harder than
 * sector 0). The brief's own measure of difficulty is the distance from Floor 0 ({@link FloorMap#threat}), so a sector's threat is that distance for its FIRST floor,
 * the floor of its Landing. Why the first and not the last floor (the Guardian's): it keeps the number of 1.54.0's first sector (1), it is the floor the rest of the
 * mod already answers with for a position in a sector whose rooms were not numbered ({@code MegaDungeon.floorAt}), and everything that wants the difficulty of a
 * PLACE inside a sector asks the floor of the place, not the sector. A module that wants the Guardian's own number reads
 * {@code FloorMap.threat(sector.lastFloor())}; this class is the one place to change if the owner would rather have the sector-wide number follow the last floor.
 * The Holy and the Abyssal road are equally hard at the same distance, so the number is the same for both. Pure.
 */
public final class SectorThreat {
    private SectorThreat() {}

    /** The threat of the {@code chainIndex}-th sector of a road with {@code perRegion} floors per sector (at least 1): Holy sector 0 is 1, sector 1 is 11, the Abyssal road counts the same. */
    public static int of(Polarity road, int chainIndex, int perRegion) {
        return FloorMap.threat(FloorMap.firstFloor(road, Math.max(0, chainIndex), Math.max(1, perRegion)));
    }
}
