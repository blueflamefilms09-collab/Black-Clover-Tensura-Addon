package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Escanor's whole fight is the sun. His power is lowest at midnight and highest at noon (the Grace called Sunshine), and the owner asked that
 * the game's time of day matters and that it still works in the dark Mega-Dungeon, which has no sky. The sky is not what matters, the clock is: every
 * dimension shares the world's day time, so Escanor reads it, and this pure class turns a day time (0 to 23999, 0 = sunrise, 6000 = noon, 12000 =
 * sunset, 18000 = midnight, the game's own convention) into his Sunshine from {@link #NIGHT_FLOOR} (a frail man at midnight, but still a man with an
 * axe) up to 1.0 (noon, "The One"), and names the phase of the day.
 *
 * <p>It also holds the small clock of one fight. Until a fight starts it simply mirrors the world; when the fight starts it takes the world's time as
 * its start and runs on at {@code speed} times the pace of the game, so that a fight of a few minutes sees a good part of a day (at 3x a whole day
 * is under seven minutes) instead of standing still at whatever hour the party arrived. The party's choice of hour still matters a lot: it is where
 * the fight begins. A speed of 1 is plain world time, 0 holds the start hour for ever, and the start can be forced to dawn, noon or midnight for
 * servers that want one fixed fight.
 */
public final class SunClock {
    public static final int DAY = 24_000, NOON = 6_000, MIDNIGHT = 18_000;
    /** The Sunshine at the darkest hour: Escanor at night is weak, never helpless. */
    public static final double NIGHT_FLOOR = 0.18;
    /** From this Sunshine he is "The One" (about 14% of the day around noon). */
    public static final double THE_ONE = 0.90;
    /**
     * The glow of the sun reaches him a little before it clears the horizon and for a while after it sets: the height used is the true height plus
     * this, so the dark half of the day is about 47% and not 50%.
     */
    public static final double TWILIGHT = 0.25;

    /** The phases of the day, as the boss bar and the chat name them. */
    public enum Phase {
        NIGHT("Night"), DAWN("Dawn"), MORNING("Morning"), THE_ONE("Noon, The One"), AFTERNOON("Afternoon"), DUSK("Dusk");

        private final String label;

        Phase(String label) { this.label = label; }

        public String label() { return label; }
    }

    /** Where a fight's sun starts: the hour the world has, or a fixed hour. */
    public enum Start {
        WORLD, DAWN, NOON, MIDNIGHT;

        /** The start named by a config string (case-insensitive); anything unknown is {@link #WORLD}. */
        public static Start parse(String s) {
            if (s != null) for (Start v : values()) if (v.name().equalsIgnoreCase(s.trim())) return v;
            return WORLD;
        }

        /** The day time this start begins at, given the world's day time. */
        public long dayTime(long worldDayTime) {
            return switch (this) {
                case WORLD -> wrap(worldDayTime);
                case DAWN -> 0;
                case NOON -> SunClock.NOON;
                case MIDNIGHT -> SunClock.MIDNIGHT;
            };
        }
    }

    /** A day time folded into 0 .. {@link #DAY} - 1 (negative values too). */
    public static long wrap(long dayTime) { return Math.floorMod(dayTime, (long) DAY); }

    /** The height of the sun, -1 (midnight) to 1 (noon), the cosine of the sun's turn round the sky. */
    public static double height(long dayTime) { return Math.cos((wrap(dayTime) - NOON) * 2 * Math.PI / DAY); }

    /** Escanor's Sunshine at a day time: {@link #NIGHT_FLOOR} in the dark, rising through dawn to 1.0 at noon and falling again. */
    public static double sunshine(long dayTime) {
        double x = (height(dayTime) + TWILIGHT) / (1 + TWILIGHT);
        if (x <= 0) return NIGHT_FLOOR;
        return NIGHT_FLOOR + (1 - NIGHT_FLOOR) * Math.pow(Math.min(1.0, x), 1.5);
    }

    /** Is the sun on its way up (midnight to noon)? */
    public static boolean rising(long dayTime) {
        long t = wrap(dayTime);
        return t < NOON || t >= MIDNIGHT;
    }

    /** The phase for a Sunshine and a direction. */
    public static Phase phase(double sun, boolean rising) {
        if (sun >= THE_ONE) return Phase.THE_ONE;
        if (sun <= 0.22) return Phase.NIGHT;
        if (sun < 0.45) return rising ? Phase.DAWN : Phase.DUSK;
        return rising ? Phase.MORNING : Phase.AFTERNOON;
    }

    public static Phase phaseAt(long dayTime) { return phase(sunshine(dayTime), rising(dayTime)); }

    // ---------------------------------------------------------------- the clock of one fight
    private boolean running;
    private double anchorDay, speed = 1.0;
    private long anchorGame;

    /** Starts the fight's own sun: at {@code start}'s hour (the world's when {@link Start#WORLD}), at {@code speed} times the game's pace (0 to 12). */
    public void start(Start start, long worldDayTime, long gameTime, double speed) {
        this.running = true;
        this.anchorDay = (start == null ? Start.WORLD : start).dayTime(worldDayTime);
        this.anchorGame = gameTime;
        this.speed = Math.max(0.0, Math.min(12.0, speed));
    }

    /** The fight is over: the clock follows the world again. */
    public void stop() { running = false; }

    public boolean running() { return running; }

    public double speed() { return speed; }

    /** The day time now: the world's while no fight runs, otherwise the fight's own. */
    public long dayTime(long worldDayTime, long gameTime) {
        if (!running) return wrap(worldDayTime);
        return wrap((long) Math.floor(anchorDay + Math.max(0L, gameTime - anchorGame) * speed));
    }

    /** The Sunshine now. */
    public double sunshine(long worldDayTime, long gameTime) { return sunshine(dayTime(worldDayTime, gameTime)); }
}
