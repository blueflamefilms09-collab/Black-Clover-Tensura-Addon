package com.newuniverse.nusmp.saiyan;

import java.util.Locale;

/**
 * Why: the numbers of Ki Flight (3.65.0, Stage 2) with no Minecraft type, so they are unit tested. Five mastery tiers (Novice hovers briefly, Apprentice flies slowly, Adept flies normally, Expert flies
 * fast, Master adds a burst), each with a flying speed (in the game's own units: creative flight is 0.05, about eleven blocks a second), a Ki cost per second and the flight seconds that earn it. The server
 * sets the speed on the player's abilities and also CHECKS what the player really did ({@link #speedOk}), so the speed can never be higher than the tier allows. No literal faster-than-light flight: the top
 * speed is about forty blocks a second, and the extreme speed of the lore is the burst dash and its afterimages.
 */
public final class SaiyanFlightRules {
    private SaiyanFlightRules() {}

    public enum Tier {
        NOVICE, APPRENTICE, ADEPT, EXPERT, MASTER;

        public String key() { return "nusmp.saiyan.flight." + name().toLowerCase(Locale.ROOT); }

        public static Tier of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
    }

    /** Flying speed per tier (the abilities' unit; 0.05 = creative). Novice drifts, Master is about 0.17 = 37 blocks a second. */
    public static final double[] SPEED = {0.010, 0.025, 0.050, 0.110, 0.170};
    /** Flight seconds that earn each tier. */
    public static final int[] SECONDS = {0, 120, 600, 1800, 5400};
    /** Ki cost per second in percent of the maximum Ki. */
    public static final double[] KI_PER_SECOND = {0.4, 0.6, 0.9, 1.4, 2.0};
    /** One abilities-speed unit is about this many blocks a second in flight (0.05 -> 10.9). */
    public static final double BLOCKS_PER_SPEED = 218.0;
    /** The highest flying speed any setting can ask for. */
    public static final double MAX_SPEED = 0.30;

    public static Tier tierOf(long flightSeconds, int[] thresholds) {
        Tier t = Tier.NOVICE;
        for (int i = 0; i < Tier.values().length && i < thresholds.length; i++) if (flightSeconds >= thresholds[i]) t = Tier.of(i);
        return t;
    }

    public static Tier tierOf(long flightSeconds) { return tierOf(flightSeconds, SECONDS); }

    /** The flying speed for a tier, scaled by the server's setting and cut at {@link #MAX_SPEED}. */
    public static double speed(Tier t, double scale) { return Math.max(0.0, Math.min(MAX_SPEED, SPEED[t.ordinal()] * Math.max(0.0, scale))); }

    public static double kiPerSecond(Tier t, double scale) { return KI_PER_SECOND[t.ordinal()] * Math.max(0.0, scale); }

    /** Only a Master has the burst dash. */
    public static boolean canBurst(Tier t) { return t == Tier.MASTER; }

    /** Only a Novice is limited to a short hover. */
    public static boolean hoverLimited(Tier t) { return t == Tier.NOVICE; }

    /** True while a hover of {@code seconds} is still allowed. */
    public static boolean hoverLeft(int seconds, int limitSeconds) { return seconds < Math.max(1, limitSeconds); }

    /** The most blocks a second a flying player at this speed can really cover: sprint doubles it, then a margin for lag; a burst dash is checked against {@link #BURST_BLOCKS_PER_SECOND}. */
    public static double maxBlocksPerSecond(double flySpeed) { return flySpeed * BLOCKS_PER_SPEED * 2.0 * 1.5 + 6.0; }

    /** A burst dash can cover this many blocks a second for the second it lasts. */
    public static final double BURST_BLOCKS_PER_SECOND = 110.0;

    /** Server-side movement check: did the player cover more ground in the last second than the tier (or a burst) allows? */
    public static boolean speedOk(double blocksInSecond, double flySpeed, boolean burstActive) {
        double cap = Math.max(maxBlocksPerSecond(flySpeed), burstActive ? BURST_BLOCKS_PER_SECOND : 0.0);
        return !(blocksInSecond > cap);
    }

    /** A second of flight counts toward the mastery only if the player really moved (a hover on the spot teaches nothing). */
    public static boolean countsAsFlight(double blocksInSecond) { return blocksInSecond >= 3.0; }
}
