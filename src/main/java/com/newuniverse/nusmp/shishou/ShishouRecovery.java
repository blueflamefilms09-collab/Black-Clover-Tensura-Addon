package com.newuniverse.nusmp.shishou;

/**
 * Why: Shishou can never be lost for good, and never doubled. When the item or the minion is lost (the void, lava, a despawn, a thief's inventory, an unloadable place) or a non-wielder holds it too long,
 * the sword is recovered by ONE rule, pure and tested: to its wielder's inventory when the wielder is online; otherwise as a dormant sword at the wielder's home (their respawn point, or where they last held
 * the sword); with no wielder or no home, as a dormant sword at the world spawn. A dormant sword is kept for its old wielder for {@link ShishouRules#RESERVE_DAYS} days, then anyone may claim it.
 */
public final class ShishouRecovery {
    private ShishouRecovery() {}

    public enum Plan { GIVE_TO_WIELDER, DORMANT_AT_HOME, DORMANT_AT_SPAWN }

    public enum Cause { VOID, LAVA, EXPIRED, THIEF, KILLED, OPERATOR, UNLOADED, GHOST_BODY }

    public static Plan plan(boolean hasWielder, boolean wielderOnline, boolean hasHome) {
        if (!hasWielder) return Plan.DORMANT_AT_SPAWN;
        if (wielderOnline) return Plan.GIVE_TO_WIELDER;
        return hasHome ? Plan.DORMANT_AT_HOME : Plan.DORMANT_AT_SPAWN;
    }

    /** The reserve of a recovered dormant sword in ticks. */
    public static long reserveTicks(int days) { return (long) Math.max(0, days) * ShishouRules.DAY_TICKS; }

    /** Is the item at {@code y} in the void (below the lowest block of the dimension, with a margin)? */
    public static boolean inVoid(double y, int minBuildHeight) { return y < minBuildHeight + 1.0; }

    /** A lost item entity is rescued only when it is a real loss; a sword sitting in a chest is not one. */
    public static boolean isLoss(Cause c) { return c != null; }
}
