package com.newuniverse.nusmp.arc.early;

/**
 * Why: Heath Grice keeps a pocket watch and an appointment book (in the anime he checks the time constantly and loses his temper when events do not go
 * as he planned). The fight's version of that is a fixed rhythm: the Cage of Infinite Hail is not drawn at random, it comes at the next stroke of his watch,
 * so a party that learns the rhythm knows when it is coming, and when a party dodges the whole cage the plan has failed and he is flustered. This pure class
 * is the appointment book: when the next appointment falls due, and when the one after is set, shorter as his power grows (the framework's cooldown
 * multiplier), never shorter than {@value #FLOOR} ticks.
 */
public final class Timetable {
    /** The shortest gap between two appointments, whatever the power. */
    public static final int FLOOR = 120;

    private final int base;
    private long next;

    public Timetable(int baseInterval) { this.base = Math.max(FLOOR, baseInterval); }

    /** The gap after an appointment at a given cooldown multiplier (1 at the opening, 0.42 at the Gates). */
    public int interval(double cooldownMultiplier) { return (int) Math.max(FLOOR, Math.round(base * cooldownMultiplier)); }

    /** A new fight: the first appointment is open at once (the move itself still needs its power and range). */
    public void reset() { next = 0; }

    /** Has the next appointment fallen due? */
    public boolean due(long now) { return now >= next; }

    /** An appointment was kept at {@code now}: sets the next one. */
    public void kept(long now, double cooldownMultiplier) { next = now + interval(cooldownMultiplier); }

    /** The tick of the next appointment. */
    public long next() { return next; }
}
