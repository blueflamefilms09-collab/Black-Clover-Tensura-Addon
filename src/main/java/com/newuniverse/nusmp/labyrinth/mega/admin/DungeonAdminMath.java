package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: every decision of the {@code /dungeon} admin tree that is ARITHMETIC (which floor does a skip land on when the player stands on the Hub, which dungeon
 * level does floor -37 belong to, is floor 40 a boss floor, how far must the generator still grow before a teleport can happen) is kept here, with no Minecraft
 * import, for the reason the rest of the Mega-Dungeon keeps its rules pure: JUnit can pin them over every floor from -1000 to +1000, and the command executors
 * only carry the answer out. The numbers are the owner's: floors run from -1000 (the Abyssal road, 1000 floors down) to +1000 (the Holy road, 1000 floors up)
 * with Floor 0 the Sanctuary Hub in the middle that belongs to neither road; a dungeon LEVEL is a decade of ten floors (levels 1 to 100 on the Holy road,
 * -1 to -100 on the Abyssal one), and the Decennial boss of a level stands on the level's last floor (level 4 is floor 40, level -4 is floor -40). The sign
 * conventions are those of {@link FloorMap} and {@link Polarity}, which this class reuses instead of repeating, so a change to the floor model there is a
 * change here too.
 */
public final class DungeonAdminMath {
    private DungeonAdminMath() {}

    /** The farthest floor from the Hub on either road. */
    public static final int MAX_FLOOR = 1000;
    /** The deepest dungeon level (a decade of floors) on either road. */
    public static final int MAX_LEVEL = 100;
    /** Floors per dungeon level and the spacing of boss floors. */
    public static final int FLOORS_PER_LEVEL = FloorMap.BOSS_EVERY;

    // ---------------------------------------------------------------- floors
    /** Is this a floor the admin tree accepts: -1000 to +1000, Floor 0 (the Hub) included? */
    public static boolean validFloor(long floor) { return floor >= -MAX_FLOOR && floor <= MAX_FLOOR; }

    /** The floor brought into -1000 to +1000 (a value beyond is cut to the nearest end). */
    public static int clampFloor(long floor) { return (int) Math.max(-MAX_FLOOR, Math.min(MAX_FLOOR, floor)); }

    /** +1 on the Holy road, -1 on the Abyssal road, 0 on Floor 0. */
    public static int side(int floor) { return Integer.signum(floor); }

    /** The road a floor is on, or null for Floor 0 (the Hub belongs to neither). */
    public static Polarity roadOf(int floor) { return Polarity.ofFloor(floor); }

    /** The distance of a floor from the Hub, 0 to 1000 (the difficulty number the monsters scale with). */
    public static int depth(int floor) { return Math.abs(floor); }

    // ---------------------------------------------------------------- levels (decades) and bosses
    /**
     * The signed dungeon level of a floor: floors 1 to 10 are level 1, 11 to 20 level 2 ... 991 to 1000 level 100; the Abyssal floors give the negative levels;
     * Floor 0 is level 0 (the Hub has no level). A floor beyond the range reads as the last level of its side.
     */
    public static int levelOfFloor(int floor) {
        if (floor == 0) return 0;
        int d = Math.min(MAX_FLOOR, Math.abs(floor));
        return Integer.signum(floor) * ((d - 1) / FLOORS_PER_LEVEL + 1);
    }

    /** Is this a dungeon level the boss commands accept: -100 to -1 or 1 to 100 (0 is the Hub, which has no boss)? */
    public static boolean validLevel(int level) { return level != 0 && level >= -MAX_LEVEL && level <= MAX_LEVEL; }

    /** The floor a level's Decennial boss stands on (its last floor): level 4 is 40, level -4 is -40; 0 for a level that does not exist. */
    public static int bossFloorOfLevel(int level) { return validLevel(level) ? level * FLOORS_PER_LEVEL : 0; }

    /** The first floor of a level (level 4 starts on floor 31, level -4 on -31); 0 for a level that does not exist. */
    public static int firstFloorOfLevel(int level) { return validLevel(level) ? Integer.signum(level) * ((Math.abs(level) - 1) * FLOORS_PER_LEVEL + 1) : 0; }

    /** Is this floor the boss floor of its level (every tenth floor, never the Hub, never beyond the range)? */
    public static boolean isBossFloor(int floor) { return validFloor(floor) && FloorMap.isBossFloor(floor); }

    // ---------------------------------------------------------------- skipping
    /**
     * The result of a skip: the floor to go to, whether the number asked for had to be cut ({@code capped}: the end of the road at +-1000, or the Hub when going back),
     * and the side the skip was made on (+1 Holy, -1 Abyssal).
     */
    public record Skip(int target, boolean capped, int side) {}

    /**
     * Where a skip of {@code floors} floors from {@code current} lands. A positive number goes FORWARD, away from the Hub, along the road the player is on (the
     * road's sign is the sign of the current floor); a negative number goes BACK towards the Hub and stops there, it never crosses Floor 0 into the other road
     * (that is a change of road, which the admin makes with {@code tp}). On Floor 0 the player has no road, so {@code hubSide} says which way "forward" is (+1
     * Holy, -1 Abyssal; 0 reads as Holy), and going back from the Hub goes nowhere. 0 floors is no move. The target is always inside -1000 to +1000.
     */
    public static Skip skip(int current, int floors, int hubSide) {
        int cur = clampFloor(current);
        int own = side(cur);
        int hub = hubSide < 0 ? -1 : 1;
        if (floors == 0) return new Skip(cur, false, own == 0 ? hub : own);
        if (cur == 0) {
            if (floors < 0) return new Skip(0, true, hub);                                       // nothing behind the Hub
            long wanted = (long) hub * floors;
            int target = clampFloor(wanted);
            return new Skip(target, target != wanted, hub);
        }
        long wanted = (long) cur + (long) own * floors;
        if (floors < 0 && Long.signum(wanted) == -own) return new Skip(0, true, own);             // back past the Hub: stop on it
        int target = clampFloor(wanted);
        return new Skip(target, target != wanted, own);
    }

    // ---------------------------------------------------------------- growing the dungeon for a teleport
    /** The index, in its road's chain, of the sector that holds a floor (floor +1 and -1 are index 0 in the one-floor-per-sector model); -1 for Floor 0. */
    public static int chainIndexOf(int floor, int floorsPerSector) { return FloorMap.chainIndex(floor, Math.max(1, floorsPerSector)); }

    /** How many sectors still have to be built before the one at {@code targetIndex} exists, when the road's newest sector has index {@code tailIndex} (-1 for a road with none). */
    public static int sectorsToBuild(int tailIndex, int targetIndex) { return Math.max(0, targetIndex - tailIndex); }

    /** A rough time to wait, in seconds, for {@code sectors} sectors at {@code secondsPerSector} each (the generator builds one at a time), rounded up, never negative. */
    public static int etaSeconds(int sectors, double secondsPerSector) {
        if (sectors <= 0 || !(secondsPerSector > 0.0)) return 0;
        return (int) Math.ceil(sectors * secondsPerSector);
    }

    /** The share, 0 to 100, of a climb that is done: {@code built} of {@code total} sectors (100 when there was nothing to build). */
    public static int percent(int built, int total) {
        if (total <= 0) return 100;
        return Math.max(0, Math.min(100, (int) Math.floor(100.0 * built / total)));
    }
}
