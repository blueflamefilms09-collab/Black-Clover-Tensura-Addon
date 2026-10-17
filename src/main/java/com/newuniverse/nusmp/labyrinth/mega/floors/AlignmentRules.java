package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: the Directional Lock is a rule about THREE moments (stepping onto a floor, moving on from it, asking to take a road), and the same rule must hold
 * for the Hub's gates, the chain travel, the floor tracker, the hard guard and the admin command. Written once, here, as pure functions over
 * {@link PlayerDungeonAlignment} and plain values, it is pinned by unit tests move by move; the game-side classes ({@code FloorTracker},
 * {@code LockPolicy}, {@code HardLock}) only ask it and do the talking, sounds and saving. No Minecraft import.
 *
 * <p>The state machine (road = the player's {@code chosen}; "open" = not locked). Floor 0 is not on any road, and going back to it NEVER changes anything:
 * <pre>
 *   state                  event                                         result
 *   ---------------------  --------------------------------------------  --------------------------------------------------------
 *   any                    enter a floor of road R at distance d         deepest(R) = max(deepest(R), d)
 *   open (road none or X)  enter a floor of road R                       road = R   (not binding yet: see below)
 *   locked (road X)        enter a floor of road R (operator, teleport)  deepest(R) updated; road and lock unchanged
 *   open                   move on from floor f of R to a deeper one     f is cleared; if |f| &gt;= lockAtFloor and (road none or R):
 *                                                                          road = R, LOCKED
 *   locked (road X)        ask to take road X                            allowed
 *   locked (road X)        ask to take the other road                    refused (the way back to Floor 0 is not a road: always open)
 *   open                   ask to take either road                       allowed
 *   any                    reset                                         road none, open, both deepest 0
 * </pre>
 * The road follows the LAST floor entered while the lock is open, not the first: the brief says a player is committed once they clear Floor +1 or -1, not
 * when they first step onto it, so someone who dips into +1, backs out and then clears -1 is bound to the Abyssal road. If the first road stuck, that
 * dip would let them clear the whole second road without ever locking, which is the loophole the lock exists to close. A floor is "cleared" when the
 * player moves from it to a deeper floor of the same road, once per floor: {@link #clearsPrevious} compares with the deepest distance, which only grows.
 */
public final class AlignmentRules {
    private AlignmentRules() {}

    /**
     * May a player with this road and lock take the road {@code polarity}? Always true while the lock is switched off, while the player is not locked,
     * when the state has no road to bind them to (a damaged save must never trap someone), and for a null polarity (Floor 0 is not a road).
     */
    public static boolean mayEnter(Polarity chosen, boolean locked, Polarity polarity, boolean lockEnabled) {
        if (!lockEnabled || !locked || chosen == null || polarity == null) return true;
        return chosen == polarity;
    }

    /** Should a player standing on the road {@code here} (null: no road) be cast out of the dungeon? The hard guard's whole decision: the same rule as {@link #mayEnter}. */
    public static boolean shouldCastOut(Polarity chosen, boolean locked, Polarity here, boolean lockEnabled) {
        return here != null && !mayEnter(chosen, locked, here, lockEnabled);
    }

    /**
     * The player stepped onto {@code floor}. Raises the deepest distance of that road and, while the lock is open, makes it the player's road.
     * Floor 0 changes nothing. True when the state changed (the caller then saves and syncs).
     */
    public static boolean onFloorEntered(PlayerDungeonAlignment s, int floor) {
        Polarity road = Polarity.ofFloor(floor);
        if (road == null) return false;
        boolean changed = s.raiseDeepest(road, distance(floor));
        if (!s.locked() && s.chosen() != road) {
            s.choose(road);
            changed = true;
        }
        return changed;
    }

    /** Would stepping onto {@code floor} be the first time the player reaches that depth on its road? Ask BEFORE {@link #onFloorEntered}. */
    public static boolean isNewDeepest(PlayerDungeonAlignment s, int floor) {
        Polarity road = Polarity.ofFloor(floor);
        return road != null && distance(floor) > s.deepest(road);
    }

    /**
     * Does moving from {@code previous} to {@code current} clear {@code previous}? Yes when both are floors of the same road, the new one is deeper, and
     * {@code previous} is the deepest the player ever reached there (a floor they were already below has been cleared before, so it is never cleared
     * twice). Ask BEFORE {@link #onFloorEntered}, which raises the deepest.
     */
    public static boolean clearsPrevious(PlayerDungeonAlignment s, int previous, int current) {
        if (previous == 0 || current == 0) return false;
        Polarity road = Polarity.ofFloor(previous);
        if (road != Polarity.ofFloor(current)) return false;
        int from = distance(previous);
        return distance(current) > from && from >= s.deepest(road);
    }

    /** {@link #onFloorCleared(PlayerDungeonAlignment, int, int, long)} without a time (the lock time stays 0). */
    public static boolean onFloorCleared(PlayerDungeonAlignment s, int floor, int lockAtFloor) {
        return onFloorCleared(s, floor, lockAtFloor, 0L);
    }

    /**
     * {@code floor} was cleared. Closes the lock when it is still open, the floor is at least {@code lockAtFloor} from Floor 0, and the floor's road is the
     * player's road (or they have none yet): the road becomes theirs and {@code now} is stored. True when it NEWLY locks.
     */
    public static boolean onFloorCleared(PlayerDungeonAlignment s, int floor, int lockAtFloor, long now) {
        Polarity road = Polarity.ofFloor(floor);
        if (road == null || s.locked()) return false;
        if (distance(floor) < Math.max(1, lockAtFloor)) return false;
        if (s.chosen() != null && s.chosen() != road) return false;
        s.choose(road);
        s.lock(now);
        return true;
    }

    /** Operator: makes {@code road} the player's road without touching the lock. True when it changed. */
    public static boolean choose(PlayerDungeonAlignment s, Polarity road) {
        if (s.chosen() == road) return false;
        s.choose(road);
        return true;
    }

    /** Operator: closes the lock on the player's current road. False when they have no road or are locked already. */
    public static boolean lock(PlayerDungeonAlignment s, long now) {
        if (s.chosen() == null || s.locked()) return false;
        s.lock(now);
        return true;
    }

    /** Operator: opens the lock (the road stays). False when it was not locked. */
    public static boolean unlock(PlayerDungeonAlignment s) {
        if (!s.locked()) return false;
        s.unlock();
        return true;
    }

    /** Operator or new world: forgets everything (no road, open, nothing reached). */
    public static void reset(PlayerDungeonAlignment s) { s.clear(); }

    /** The distance of a floor from Floor 0 (always 0 or more). */
    private static int distance(int floor) { return floor < 0 ? -floor : floor; }
}
