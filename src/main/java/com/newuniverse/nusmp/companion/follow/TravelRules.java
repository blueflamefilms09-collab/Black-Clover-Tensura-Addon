package com.newuniverse.nusmp.companion.follow;

/**
 * Why: the limits of carrying followers through a dimension change, as pure numbers the game-side {@code CompanionTravel} and the tests share. When a player changes dimension, only the followers that
 * stand within {@link #COLLECT_RADIUS} blocks of them are carried, at most {@link #carried} of them (the server setting, never more than {@link #HARD_CAP}), nearest first; a record of who is to be
 * carried is dropped after {@link #STALE_TICKS} ticks (a crossing that never finished), and a follower of the same dimension that has fallen further behind than {@link #TELEPORT_BEHIND} blocks is
 * simply put beside the player instead of walking. Nothing here loads a chunk, scans a world or keeps anything for good.
 */
public final class TravelRules {
    private TravelRules() {}

    public static final double COLLECT_RADIUS = 32.0, TELEPORT_BEHIND = 24.0;
    public static final int HARD_CAP = 6, STALE_TICKS = 100;
    /** Followers keep this distance (blocks) from the player; they start walking to them beyond {@link #START_FOLLOW}. */
    public static final double KEEP_DISTANCE = 3.0, START_FOLLOW = 6.0;

    /** How many of {@code found} followers are carried when the server allows {@code configured}. */
    public static int carried(int found, int configured) {
        return Math.max(0, Math.min(found, Math.min(Math.max(configured, 0), HARD_CAP)));
    }

    public static boolean withinCollect(double distanceSqr) { return distanceSqr <= COLLECT_RADIUS * COLLECT_RADIUS; }

    public static boolean tooFarBehind(double distanceSqr) { return distanceSqr > TELEPORT_BEHIND * TELEPORT_BEHIND; }

    public static boolean stale(long recordedAt, long now) { return now < recordedAt || now - recordedAt > STALE_TICKS; }
}
