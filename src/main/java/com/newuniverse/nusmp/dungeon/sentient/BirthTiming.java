package com.newuniverse.nusmp.dungeon.sentient;

/**
 * The clock of one wall birth: the crack grows in ten stages over the delay (the vanilla crack stages 0 to 9), is redrawn every fourth tick,
 * and the wall gives way when the delay has run out. Pure.
 */
public final class BirthTiming {
    private BirthTiming() {}

    /** The shortest and the longest delay a birth may be given, in ticks; the ticks between two redraws of the crack; the crack stages. */
    public static final int MIN_DELAY = 20, MAX_DELAY = 120, CRACK_EVERY = 4, STAGES = 10;

    public static int clamp(int delay) { return Math.max(MIN_DELAY, Math.min(MAX_DELAY, delay)); }

    /** Is the crack redrawn at this age (ticks since the birth was scheduled)? */
    public static boolean crackDue(int age) { return age >= 0 && age % CRACK_EVERY == 0; }

    /** The crack stage (0 to 9) at this age: it only ever rises. */
    public static int stage(int age, int delay) {
        if (age <= 0) return 0;
        return (int) Math.min(STAGES - 1, (long) age * STAGES / clamp(delay));
    }

    /** Has the wall given way? */
    public static boolean done(int age, int delay) { return age >= clamp(delay); }
}
