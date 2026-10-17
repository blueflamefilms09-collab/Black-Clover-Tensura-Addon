package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Diane is a giant who is also, when she chooses or is forced to be, a girl of human size, and the owner asked for "a size-change mechanic" in her fight. A size
 * change that nobody can read or influence is just a phase, so it has rules a party can learn: she opens the fight at human size and grows after a few seconds (she
 * announces it, with a quake); as a giant she is slow and wide and tough, and she shrinks by herself after forty-five seconds; and a party that hurts her enough, fast
 * enough, CUTS HER DOWN TO SIZE at once: she is human for ten seconds (the window the party earned) and then grows again, which is also telegraphed. If there is no room
 * to grow (a low ceiling, a wall) she stays human and tries again later. This pure clock holds the stance, since when, and whether the last change was forced, and says
 * when she wants to change; the glue does the changing (the size attribute, the room check, the telegraph).
 */
public final class StanceClock {
    public enum Stance { HUMAN, GIANT }

    /** Ticks at human size before the first growth of a fight, before a growth after being cut down, and as a giant before she shrinks by herself. */
    public static final int OPENING_HUMAN = 100, CUT_DOWN_HUMAN = 200, GIANT_MAX = 900;
    /** After a growth that found no room, ticks before she tries again. */
    public static final int NO_ROOM_RETRY = 400;

    private Stance stance = Stance.HUMAN;
    private long since;
    private boolean byBurst;
    private long growBlockedUntil;

    /** A fresh fight: human size, since {@code now}. */
    public void start(long now) {
        stance = Stance.HUMAN;
        since = now;
        byBurst = false;
        growBlockedUntil = 0;
    }

    public Stance stance() { return stance; }

    public boolean giant() { return stance == Stance.GIANT; }

    /** Ticks in the current stance. */
    public long age(long now) { return Math.max(0, now - since); }

    public void becomeGiant(long now) {
        stance = Stance.GIANT;
        since = now;
        byBurst = false;
    }

    /** She is human now; {@code cutDown} says a burst of damage forced it (so she stays human for the longer, earned window). */
    public void becomeHuman(long now, boolean cutDown) {
        stance = Stance.HUMAN;
        since = now;
        byBurst = cutDown;
    }

    /** Was the present human size forced by the party's burst? */
    public boolean cutDown() { return stance == Stance.HUMAN && byBurst; }

    /** The Gates: a Diane who is still at human size wants to grow at once (and a blocked growth is tried again). */
    public void growNow(long now) {
        if (stance != Stance.HUMAN) return;
        since = now - CUT_DOWN_HUMAN;
        byBurst = false;
        growBlockedUntil = 0;
    }

    /** A growth found no room: do not try again for {@link #NO_ROOM_RETRY} ticks. */
    public void blockGrowth(long now) { growBlockedUntil = now + NO_ROOM_RETRY; }

    /** Does she want to grow now: human long enough (the opening, or the cut-down window) and not blocked. */
    public boolean wantsGrow(long now) {
        return stance == Stance.HUMAN && now >= growBlockedUntil && age(now) >= (byBurst ? CUT_DOWN_HUMAN : OPENING_HUMAN);
    }

    /** Does she want to shrink by herself now: a giant for the longest she stays one. */
    public boolean wantsShrink(long now) { return stance == Stance.GIANT && age(now) >= GIANT_MAX; }

    /** Ticks until she grows again after being cut down (0 when she is not cut down or the time is up). */
    public int cutDownLeft(long now) { return cutDown() ? (int) Math.max(0, CUT_DOWN_HUMAN - age(now)) : 0; }
}
