package com.newuniverse.nusmp.heroweapon.weapon.instrument;

/**
 * Why: the Vassal Musical Instrument is played, not just aimed, and the rhythm input the owner asked for ("a short rhythm input is welcome") has to work with the
 * one input every weapon has, the technique keys. So the beat is the gap between two techniques: cast the next one inside the pocket (not too soon, not too late)
 * and the harmony of the phrase grows by one, up to four; let the pocket pass and the phrase starts again. Harmony tightens the notes (the series says an untrained
 * wielder's notes fly wildly), lengthens the songs and, at three, opens the Crescendo. The counting is plain arithmetic on the game clock, kept apart from the game so a
 * test can play whole phrases. A cast that is refused must not count, so the caller asks {@link #preview} first and calls {@link #commit} only when the technique
 * really happened.
 */
public final class Tempo {
    /** The most harmony a phrase can build. */
    public static final int MAX = 4;
    /** A technique is in time when it comes at least this many ticks after the last one ... */
    public static final int POCKET_MIN = 20;
    /** ... and at most this many; the {@code steady_beat} passive widens the pocket by {@link #STEADY_EXTRA}. */
    public static final int POCKET_MAX = 100, STEADY_EXTRA = 20;

    private long lastAt = Long.MIN_VALUE / 4;
    private int stacks;

    /** The harmony the next technique would have if it were cast at {@code now}. */
    public int preview(long now, boolean steady) {
        long gap = now - lastAt;
        int max = POCKET_MAX + (steady ? STEADY_EXTRA : 0);
        if (gap > max) return 0;                                  // the phrase lapsed: this cast begins a new one
        if (gap < POCKET_MIN) return stacks;                      // rushed: no harmony gained, none lost
        return Math.min(MAX, stacks + 1);
    }

    /** Records a technique cast at {@code now}; returns the harmony it earned (the same number {@link #preview} gave). */
    public int commit(long now, boolean steady) {
        stacks = preview(now, steady);
        lastAt = now;
        return stacks;
    }

    /** The harmony standing at {@code now} (zero once the pocket has passed), for the display. */
    public int current(long now, boolean steady) {
        return now - lastAt > POCKET_MAX + (steady ? STEADY_EXTRA : 0) ? 0 : stacks;
    }

    /** Spends the whole phrase (the Crescendo) or forgets it (the weapon was put away). */
    public void reset() {
        stacks = 0;
        lastAt = Long.MIN_VALUE / 4;
    }
}
