package com.newuniverse.nusmp.dungeon;

/**
 * What cutting a Maw's roots is worth ({@link MawReward}): the sooner they are cut, the more Maw Fruit and experience there is. Pure; the
 * share is the part of the Maw's open time that was still left when it closed.
 */
public final class MawFruitMath {
    private MawFruitMath() {}

    /** The fraction of {@code total} still left, kept between 0 and 1 (0 if the total is unknown). */
    public static double share(long left, long total) {
        if (total <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, left / (double) total));
    }

    /** 3 fruit for a cut in the first third of the Maw's time, 2 up to two thirds, otherwise 1. */
    public static int count(long left, long total) {
        double s = share(left, total);
        return s >= 0.66 ? 3 : s >= 0.33 ? 2 : 1;
    }

    /** Experience points: 30 for a last-minute cut up to 100 for an instant one. */
    public static int xp(long left, long total) {
        return 30 + (int) Math.round(70 * share(left, total));
    }
}
