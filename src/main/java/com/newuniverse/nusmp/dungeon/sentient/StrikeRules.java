package com.newuniverse.nusmp.dungeon.sentient;

/**
 * The fallback blow of a born mob. Tensura's mobs strike with brains of their own; the Dungeon only makes sure a mob that is in reach and
 * has not struck for two seconds does, once, and never twice in a row with its own brain's. A mob of the water only strikes when both
 * are in the water or they are truly touching. Pure.
 */
public final class StrikeRules {
    private StrikeRules() {}

    /** Ticks between two fallback blows of one mob. */
    public static final int GAP = 40;
    /** How far a born mob looks for a player to go for, and a Juggernaut. */
    public static final double SIGHT = 32.0, JUGGERNAUT_SIGHT = 64.0;
    /** A target is kept until it is this many times the sight away (so a mob does not flip between two players). */
    public static final double KEEP = 1.5;
    /** The reach box: the mob's box grown by this much sideways and upward. */
    public static final double REACH_XZ = 1.1, REACH_Y = 0.7, TOUCH = 0.4;

    public static double sight(boolean juggernaut) { return juggernaut ? JUGGERNAUT_SIGHT : SIGHT; }

    /** Is a current target still worth keeping: a valid player no farther than {@link #KEEP} times the sight? */
    public static boolean keep(boolean validPlayer, double distanceSq, double sight) {
        double far = sight * KEEP;
        return validPlayer && distanceSq <= far * far;
    }

    /** May the mob make its fallback blow now? */
    public static boolean mayStrike(long now, long nextAt, boolean inReach, boolean ownBrainStruck, boolean waterMob, boolean bothInWater, boolean touching) {
        if (now < nextAt || !inReach || ownBrainStruck) return false;
        return !waterMob || bothInWater || touching;
    }

    public static long nextStrike(long now) { return now + GAP; }
}
