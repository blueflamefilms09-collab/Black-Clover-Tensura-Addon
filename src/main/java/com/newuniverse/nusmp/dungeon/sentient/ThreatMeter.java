package com.newuniverse.nusmp.dungeon.sentient;

/**
 * How near one player is to being taken for a lethal threat by the Dungeon's immune system. Threat is a number of points that every
 * act against the Dungeon adds to and that bleeds away with time (1.5 a second); when it reaches the threshold of the server's config a
 * Juggernaut is born. The meter keeps no clock of its own: every call says what the game time is. Pure.
 */
public final class ThreatMeter {
    /** Points that fade every tick: 1.5 a second. */
    public static final double DECAY_PER_TICK = 1.5 / 20.0;
    /** A refused blow at a wall; one point per block of a refused blast (at most BLAST_CAP, and one blast is one addition); spells below SPELL_FLOOR percent are ignored. */
    public static final double WALL_HIT = 4.0, BLOCK = 1.0, BLAST_CAP = 40.0, SPELL_FLOOR = 15.0;
    /** Ticks between two wall hits that count (holding the button down fires the event again and again). */
    public static final int WALL_HIT_GAP = 4;
    /** The most threat a player can pile up. */
    public static final double CEILING = 1000.0;

    private double value;
    private long at;
    private long lastWallHit = Long.MIN_VALUE / 2;

    /** The points now (after what has faded since the last look). */
    public double value(long now) {
        settle(now);
        return value;
    }

    public void add(double points, long now) {
        settle(now);
        if (points > 0) value = Math.min(CEILING, value + points);
    }

    /** Counts a refused blow at a wall unless one counted less than {@link #WALL_HIT_GAP} ticks ago; true if it counted. */
    public boolean wallHit(long now) {
        if (now - lastWallHit < WALL_HIT_GAP) return false;
        lastWallHit = now;
        add(WALL_HIT, now);
        return true;
    }

    /** Would a wall hit at this time count? (Nothing is counted.) */
    public boolean wallHitDue(long now) { return now - lastWallHit >= WALL_HIT_GAP; }

    public boolean triggered(double threshold, long now) { return value(now) >= threshold; }

    /** Holds the points at no more than {@code max}: a Juggernaut that could not be sent must not leave the meter over its threshold for good. */
    public void clampTo(double max, long now) {
        settle(now);
        if (value > max) value = Math.max(0.0, max);
    }

    /** After a Juggernaut was sent: the meter starts from nothing again. */
    public void reset(long now) {
        value = 0;
        at = now;
    }

    private void settle(long now) {
        if (now < at) at = now;                                              // the clock went back (a world edit): nothing fades
        else if (now > at) {
            value = Math.max(0.0, value - (now - at) * DECAY_PER_TICK);
            at = now;
        }
    }

    // ---------------------------------------------------------------- what each act is worth
    /** A refused explosion: a point per block, at most {@link #BLAST_CAP} for one blast. */
    public static double blastPoints(int blocks) { return Math.min(BLAST_CAP, Math.max(0, blocks) * BLOCK); }

    /** A spell that cost this share of the caster's magicules: nothing below 15 percent, else cost squared over ten (25% = 62.5, 40% = 160). */
    public static double spellPoints(double costPercent) { return costPercent >= SPELL_FLOOR ? costPercent * costPercent / 10.0 : 0.0; }

    /** 0 below half the threshold, 1 from half, 2 from four fifths: the Dungeon's warnings before it acts. */
    public static int warning(double value, double threshold) {
        if (threshold <= 0 || value < threshold * 0.5) return 0;
        return value < threshold * 0.8 ? 1 : 2;
    }
}
