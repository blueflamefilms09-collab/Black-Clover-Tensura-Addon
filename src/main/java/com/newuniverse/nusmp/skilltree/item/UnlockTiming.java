package com.newuniverse.nusmp.skilltree.item;

/**
 * Why: the "route unlocked" moment is a short piece of staging that is played by TWO programs that never talk to each other while it runs: the server (the circle
 * that charges and bursts, the pillar of light, the flare, the sounds) and the client of the player who read the book (the screen flash and the banner). They stay
 * in step only because both read the same few numbers, so the numbers live in this one pure class instead of as two copies of "22" in two files: the server schedules
 * the second stage {@link #BURST_TICK} ticks after the first, the client starts its flash and its banner the same number of ticks after the packet arrives, and the
 * circle's length is derived from the burst tick (the magic circle effect charges for the first 60% of its life and bursts at the end of that, so its duration is the
 * burst tick divided by 0.6, rounded up). Change the burst and everything follows. Pure, with a unit test that pins the relations.
 */
public final class UnlockTiming {
    private UnlockTiming() {}

    /** Game ticks between the book being read and the burst (the circle's charge: a little over a second). */
    public static final int BURST_TICK = 22;
    /** Fraction of the circle effect's life that is its charge; it bursts when the charge ends. */
    public static final double CIRCLE_CHARGE_SHARE = 0.6;
    /** How long the aura of spirit spirals stays round the reader after the burst, in ticks. */
    public static final int AURA_TICKS = 70;
    /** How long the banner lives on the reader's screen, in ticks: three seconds. */
    public static final int BANNER_TICKS = 60;

    /** The life of the ground circle effect, in ticks, such that its burst falls on {@link #BURST_TICK}. */
    public static int circleTicks() { return (int) Math.ceil(BURST_TICK / CIRCLE_CHARGE_SHARE); }

    /** The tick of the circle effect's burst, as the effect itself computes it from the duration (60% of it, rounded down). */
    public static int circleBurstTick() { return (int) (circleTicks() * CIRCLE_CHARGE_SHARE); }
}
