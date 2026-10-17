package com.newuniverse.nusmp.arc.spade;

/**
 * Why: Morris's Specimen Mark is a thread from him to one foe that drags the foe in over eight seconds and ends with a dissection. A thread that only
 * hurts is not a mechanic; this one has to be BREAKABLE by the party in several ways that each ask for something different: an ally stepping onto the
 * thread cuts it, a wall between them breaks it, a burst of damage snaps it, anti-magic dissolves it, running too far loses it, and if nobody does any of
 * that the specimen reaches him and is dissected. This class is the whole rule as a state machine fed once per tick with plain facts (is a third foe on
 * the thread, is the line blocked, is the specimen holding anti-magic, did a burst land, how far apart they are) and answering how, if at all, the thread
 * ended. The geometry of "on the thread" is in {@link TetherMath}. Pure, so every ending is tested without a game.
 */
public final class Tether {
    /** How a thread ended. {@link #HOLD} means it has not. */
    public enum End { HOLD, CUT, BURST, ANTI_MAGIC, LOST_SIGHT, TOO_FAR, REACHED, TIMEOUT }

    /** Consecutive ticks a third foe must stand on the thread to cut it, and consecutive ticks of a blocked line that lose it. */
    public static final int CUT_TICKS = 6, SIGHT_TICKS = 30;
    /** Farther apart than this and the thread is lost; this close and the specimen has been reached. */
    public static final double MAX_DISTANCE = 28, REACH = 2.4;

    private final int life;
    private int age, crossed, blocked;
    private End ended = End.HOLD;

    /** A thread that lives {@code life} ticks at most. */
    public Tether(int life) { this.life = Math.max(1, life); }

    public int age() { return age; }

    public int life() { return life; }

    public boolean over() { return ended != End.HOLD; }

    public End ending() { return ended; }

    /**
     * One tick of the thread. Endings are checked in a fixed order (anti-magic, burst, cut, sight, distance, reached, time), so the answer for a given tick
     * never depends on the order the glue happens to read its facts.
     *
     * @param crossedByThird a foe other than the specimen is on the thread this tick
     * @param lineBlocked    something solid is between the two
     * @param antiMagic      the specimen holds anti-magic, or an anti-magic blow landed on the owner this tick
     * @param burst          the burst watch has tripped
     * @param distance       blocks between the specimen and the owner
     */
    public End tick(boolean crossedByThird, boolean lineBlocked, boolean antiMagic, boolean burst, double distance) {
        if (ended != End.HOLD) return ended;
        age++;
        crossed = crossedByThird ? crossed + 1 : 0;
        blocked = lineBlocked ? blocked + 1 : 0;
        if (antiMagic) ended = End.ANTI_MAGIC;
        else if (burst) ended = End.BURST;
        else if (crossed >= CUT_TICKS) ended = End.CUT;
        else if (blocked >= SIGHT_TICKS) ended = End.LOST_SIGHT;
        else if (distance > MAX_DISTANCE) ended = End.TOO_FAR;
        else if (distance <= REACH) ended = End.REACHED;
        else if (age >= life) ended = End.TIMEOUT;
        return ended;
    }

    /** How the thread drags the specimen: slow at first, firmer as it goes (blocks per tick), never beyond {@link TetherMath#MAX_PULL}. */
    public double pull() { return TetherMath.pullSpeed(age, life); }
}
