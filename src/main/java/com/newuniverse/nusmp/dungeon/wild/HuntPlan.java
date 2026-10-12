package com.newuniverse.nusmp.dungeon.wild;

/**
 * One hunt, as a small state machine the driver steps every couple of ticks: chase the prey, strike when it is in reach (with a cooldown),
 * and stop when the prey is dead, the hunter is full, the prey got away, the hunt has gone on too long or the hunter cannot find a way to it.
 * The caller does the walking and the hitting; this only decides what to do next. Pure.
 */
public final class HuntPlan {
    public enum Act { CHASE, STRIKE, HOLD, DONE, GIVE_UP }

    /** Game ticks a hunt may last, ticks between two strikes, ticks between two path requests, failed path requests before giving up. */
    public static final int MAX_AGE = 400, COOLDOWN = 20, REPATH = 10, MAX_NO_PATH = 3;
    /** Squared distance (blocks) at which the prey has got away. */
    public static final double LOST_DISTANCE_SQ = 24.0 * 24.0;

    private int age, cooldown, repath, noPath, strikes;

    /**
     * @param dt         game ticks since the last step
     * @param distSq     squared distance to the prey
     * @param inReach    whether the prey is within melee reach
     * @param preyAlive  whether the prey still lives (and is still there)
     * @param hunterFull whether the hunter has eaten its fill
     */
    public Act step(int dt, double distSq, boolean inReach, boolean preyAlive, boolean hunterFull) {
        age += dt;
        if (!preyAlive) return Act.DONE;
        if (hunterFull || age > MAX_AGE || distSq > LOST_DISTANCE_SQ || noPath >= MAX_NO_PATH) return Act.GIVE_UP;
        cooldown = Math.max(0, cooldown - dt);
        repath -= dt;
        if (inReach) {
            if (cooldown > 0) return Act.HOLD;
            cooldown = COOLDOWN;
            strikes++;
            return Act.STRIKE;
        }
        return Act.CHASE;
    }

    /** True when the hunter should ask for a fresh path to the prey now (the answer starts the countdown again). */
    public boolean wantsPath() {
        if (repath > 0) return false;
        repath = REPATH;
        return true;
    }

    /** What the path request found: a route, or nothing. */
    public void pathResult(boolean found) { noPath = found ? 0 : noPath + 1; }

    public int strikes() { return strikes; }

    public int age() { return age; }
}
