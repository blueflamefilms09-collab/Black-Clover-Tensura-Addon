package com.newuniverse.nusmp.devil;

/**
 * Why (5.7.0, owner request): once in a while a devil speaks to a player on its own and offers a contract: a small chance, rolled rarely, never for a player who is busy. All the numbers and decisions
 * are here with no Minecraft type, so they are unit tested; {@link DevilCalling} only asks them. Cost: one roll per eligible player every {@link #DEFAULT_INTERVAL_MINUTES} minutes, nothing else.
 */
public final class DevilCallingRules {
    private DevilCallingRules() {}

    /** The owner asked for 2 percent. */
    public static final double DEFAULT_PERCENT = 2.0;
    public static final int DEFAULT_INTERVAL_MINUTES = 20;
    public static final int MAX_INTERVAL_MINUTES = 1440;
    /** The server looks at its players once a minute (1200 ticks): the interval is counted in these steps. */
    public static final int SWEEP_TICKS = 1200;

    public static double clampPercent(double p) { return Double.isNaN(p) ? DEFAULT_PERCENT : Math.max(0.0, Math.min(100.0, p)); }

    public static int clampMinutes(int m) { return Math.max(1, Math.min(MAX_INTERVAL_MINUTES, m)); }

    public static long intervalTicks(int minutes) { return clampMinutes(minutes) * 1200L; }

    /** True when it is time for this player's next roll: {@code last} is the game time of the last one (0 = never; then the clock starts now and nothing is rolled). */
    public static boolean due(long last, long now, long intervalTicks) { return last > 0 && now >= last && now - last >= intervalTicks; }

    /** The roll: {@code r} is a random number in [0, 1). A percent of 0 never wins and 100 always does. */
    public static boolean wins(double percent, double r) { return r * 100.0 < clampPercent(percent); }

    /** A devil only asks a player who is free: alive, playing (not spectating), with no contract, no ritual running and no offer open. */
    public static boolean eligible(boolean alive, boolean spectator, boolean hasContract, boolean ritual, boolean offerOpen) {
        return alive && !spectator && !hasContract && !ritual && !offerOpen;
    }

    /** Which of {@code n} candidates a random number {@code r} in [0, 1) picks (-1 when there is none). */
    public static int pick(int n, double r) { return n <= 0 ? -1 : Math.min(n - 1, (int) (Math.max(0.0, Math.min(0.999999, r)) * n)); }
}
