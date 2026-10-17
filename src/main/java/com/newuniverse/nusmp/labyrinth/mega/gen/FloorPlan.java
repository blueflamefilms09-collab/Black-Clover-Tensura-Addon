package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: in the floor model (1.58.0) every floor is a small level of its own, and where it stands, how big it is, which stairs it uses and how many rooms it
 * aims at are decisions that have to be the same for the planner, the generator's request, the simulator and the tests, and that a changed config must never
 * silently move. They are made here, once, as plain arithmetic. <b>Where:</b> floors are NOT joined by tunnels any more (a party changes floor by a stair, the
 * travel code teleports it), so a floor needs no neighbour planning at all: floor {@code k} of a road stands in a fixed slot of the road's lane, one
 * {@link #PITCH} further along x than floor {@code k - 1}, and two floors can never touch (the pitch is far more than a region plus the reach of a door cap).
 * That is also why a floor can be planned, regenerated or repaired without looking at any other floor. <b>How big:</b> a floor of 28 rooms needs about 17 x 17
 * modules; a region that is much larger than the rooms need is just empty void (the old 19 x 19 regions held ~100 rooms), so an ordinary floor gets the smallest odd
 * number of modules that holds its rooms (odd because the region must be centred on the arrival stairwell's door, which stands at a module centre) and a boss floor
 * always gets the full 19 x 19 (its boss chain alone is 12 modules long). <b>Which stairs:</b> the stairs that lead BACK are the opposite of the road (the Holy road
 * climbs, so the arrival stairwell of a Holy floor drops back down, and the other way round); the forward stairs follow the road, and the grand ones are for the
 * boss floors.
 */
public final class FloorPlan {
    private FloorPlan() {}

    /** The pools of the four kinds of stairwell (the folder {@code structure/mega/stairwell}, files {@code up_*}, {@code down_*}, {@code grand_up_*}, {@code grand_down_*}). */
    public static final String P_UP = "nusmp:mega/stairwell_up", P_DOWN = "nusmp:mega/stairwell_down",
            P_GRAND_UP = "nusmp:mega/stairwell_grand_up", P_GRAND_DOWN = "nusmp:mega/stairwell_grand_down";
    /** The pool the arrival stairwell's only door draws its first room from: rooms with several doors only, so a floor never starts in a dead end. */
    public static final String P_STAIR_DOOR = "nusmp:mega/stair_door";

    /** Smallest and largest region of a floor, in modules (both odd: the region is centred on a door that stands at a module centre). */
    public static final int MIN_REGION_MODULES = 13, MAX_REGION_MODULES = MegaGrid.REGION_MODULES;
    /** Extra rooms a boss floor has on top of the configured number: the boss chain (arena, antechamber, vault, Way Onward, grand stairwell) is five of them. */
    public static final int BOSS_EXTRA_ROOMS = 8;
    /** Distance between the slots of two floors along x, in blocks: the largest region plus three modules, so even two boss floors and the caps of their doors keep far apart. */
    public static final int PITCH = (MegaGrid.REGION_MODULES + 3) * MegaGrid.MODULE;
    /** The share of the aimed room count a floor must reach to pass the audit. */
    public static final double MIN_ROOM_SHARE = 0.6;

    /** The number of rooms a floor aims at: the configured number (kept within the config's range) and, on a boss floor, {@link #BOSS_EXTRA_ROOMS} more. */
    public static int rooms(int configured, boolean boss) {
        return Math.max(12, Math.min(60, configured)) + (boss ? BOSS_EXTRA_ROOMS : 0);
    }

    /** The least number of rooms the audit accepts for a floor that aims at {@code rooms}. */
    public static int minRooms(int rooms) { return Math.max(8, (int) Math.round(rooms * MIN_ROOM_SHARE)); }

    /**
     * The edge of the region of a floor that aims at {@code rooms}, in modules: the smallest odd number whose square holds the rooms at about 9 modules of area
     * each (an average room of the pools is 7 modules, the rest is room to wind), within {@link #MIN_REGION_MODULES}..{@link #MAX_REGION_MODULES}. A boss floor
     * always gets the maximum.
     */
    public static int regionModules(int rooms, boolean boss) {
        if (boss) return MAX_REGION_MODULES;
        int n = (int) Math.ceil(Math.sqrt(Math.max(1, rooms) * 9.0));
        if ((n & 1) == 0) n++;
        return Math.max(MIN_REGION_MODULES, Math.min(MAX_REGION_MODULES, n));
    }

    /**
     * The region anchor of floor {@code chainIndex} of a road: the jigsaw cell (door row) at the middle of the near edge of the floor's slot, the fixed cell the
     * region is built around ({@code Sector.entry} saves the floor cell below it). Floor 0 of the Holy road stands at x 0, z 0 where 1.54.0 put its sector 0.
     */
    public static Vec entry(Polarity road, int chainIndex) {
        return new Vec(Math.max(0, chainIndex) * PITCH, SectorPlanner.DOOR_Y, MegaGrid.laneZ(road));
    }

    /** The region of a floor that aims at {@code rooms}, anchored at {@code entry} and growing east (the lane runs along x). */
    public static Box region(Vec entry, int rooms, boolean boss) { return SectorPlanner.regionFor(entry, Facing.EAST, regionModules(rooms, boss)); }

    /** The pool of the ARRIVAL stairwell of a floor of this road: the stairs that lead back (the Holy road climbs, so they drop; the Abyssal road descends, so they climb). */
    public static String arrivalPool(Polarity road) { return road == Polarity.ABYSSAL ? P_UP : P_DOWN; }

    /** The pool of the FORWARD stairwell: up on the Holy road, down on the Abyssal road, the grand ones on a boss floor. */
    public static String forwardPool(Polarity road, boolean boss) {
        if (road == Polarity.ABYSSAL) return boss ? P_GRAND_DOWN : P_DOWN;
        return boss ? P_GRAND_UP : P_UP;
    }

    /** The {@code dir} argument of a stair-exit marker for stairs that lead to the next floor of this road: {@code up} on the Holy road, {@code down} on the Abyssal one. */
    public static String forwardDir(Polarity road) { return road == Polarity.ABYSSAL ? "down" : "up"; }

    /**
     * The request part for a floor of the floor model. {@code configuredRooms} is {@code mega.floorRooms}; {@code boss} is {@link FloorMap#isBossFloor} of the
     * floor's number.
     */
    public static Request.Floor spec(Polarity road, int chainIndex, int configuredRooms) {
        boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(road, chainIndex));
        int rooms = rooms(configuredRooms, boss);
        return new Request.Floor(boss, arrivalPool(road), forwardPool(road, boss), rooms, minRooms(rooms));
    }
}
