package com.newuniverse.nusmp.book;

/**
 * Why: the numbers of the rebuilt Song Magic that a test can pin without Minecraft: the three choir spirits stand on a ring of radius 2.4 round the singer, 120 degrees apart, the ring
 * turns by 0.02 radians per tick, and a spirit opens its mouth for 12 ticks at the start of every 30 tick verse (the three are staggered by 10 ticks so the choir never falls silent
 * all at once); the metronome keeps a field of radius 6 and beats once per 20 ticks; Staccato fires three notes 4 ticks apart.
 */
public final class SongRules {
    private SongRules() {}

    public static final int VERSE_TICKS = 30, SING_TICKS = 12, VERSE_STAGGER = 10, BEAT_TICKS = 20, STACCATO_NOTES = 3, STACCATO_GAP = 4, METRONOME_RADIUS = 6;
    public static final double CHOIR_RING = 2.4, RING_SPEED = 0.02;

    /** The angle of choir slot {@code role} (0..2) on the ring at tick {@code t}. */
    public static double ringAngle(int role, int t) { return role * (Math.PI * 2 / 3.0) + t * RING_SPEED; }

    /** True while the spirit in slot {@code role} sings at tick {@code t}: its verse opens at phase 0 and the mouth shuts at phase {@link #SING_TICKS}. */
    public static boolean singingAt(int role, int t) { return Math.floorMod(t + role * VERSE_STAGGER, VERSE_TICKS) < SING_TICKS; }

    /** True when the verse of slot {@code role} begins at tick {@code t} (the moment it heals and strikes). */
    public static boolean verseStarts(int role, int t) { return Math.floorMod(t + role * VERSE_STAGGER, VERSE_TICKS) == 0; }

    /** The metronome beats at the start of every second (the tick after the one that is a multiple of {@link #BEAT_TICKS}). */
    public static boolean beats(int life) { return life % BEAT_TICKS == 1; }
}
