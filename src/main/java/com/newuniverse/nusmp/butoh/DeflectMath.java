package com.newuniverse.nusmp.butoh;

/**
 * Why: the deflect dash must turn missiles back on the shooter without becoming a perfect shield or a ping-pong that never ends. The numbers
 * (window length, spread, speed, how many times one missile may be turned) live in one pure class so they are tested and tuned in one place.
 * Outside the dance the deflection is plain; while dancing it is stronger (faster return, tighter aim) as the brief asks. No game classes.
 */
public final class DeflectMath {
    private DeflectMath() {}

    /** The deflect window after a dash, in ticks. */
    public static final int WINDOW_TICKS = 12;
    /** The cooldown of the page, in ticks (about 6 seconds). */
    public static final int COOLDOWN_TICKS = 120;
    /** One missile is turned back at most this many times (two wielders trading it must not keep it alive forever). */
    public static final int MAX_TURNS = 2;

    /** The random spread of a returned missile, in degrees either way. A dancer's return is tighter. */
    public static double spreadDegrees(boolean dancing) { return dancing ? 3.0 : 7.0; }

    /** The speed of a returned missile as a multiple of the speed it arrived with. */
    public static double speedFactor(boolean dancing) { return dancing ? 1.25 : 1.0; }

    /** The dash length in blocks (the dance dashes a little further). */
    public static double dashDistance(boolean dancing) { return dancing ? 6.5 : 5.5; }

    /** Whether a missile that was already turned {@code turns} times may be turned again. */
    public static boolean mayTurn(int turns) { return turns < MAX_TURNS; }

    /** The window is open at {@code now} when the dash set it to end at {@code until}. */
    public static boolean open(long now, long until) { return now <= until; }
}
