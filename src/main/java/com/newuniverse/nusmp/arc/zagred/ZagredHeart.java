package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: in the story Zagred's body can be destroyed and his HEART rebuilds it, and his heart is the one place the heroes could finally hurt: it is the
 * weak spot he moves out of the way of a fatal blow, and only anti-magic reached it for certain. After the Gates open (act V, the devil form, the second
 * health bar) this is the whole defence: the heart is CLOSED and the body takes less than half of a blow, and it OPENS for a few seconds every time he
 * finishes speaking a Word (the mouth on the chest opens to speak), when a Word is cut, and when an Arch daemon falls. Anti-magic always reaches it.
 * The party's rhythm is therefore "he speaks, you strike": the more Words they cut and the more anchors they kill, the longer the heart stays open.
 * Pure (no game types): the numbers and the window arithmetic are unit tested.
 */
public final class ZagredHeart {
    /** What a closed heart lets through, what an open one takes (a window is also a little extra damage), and what anti-magic does to a closed one. */
    public static final float CLOSED = 0.45f, OPEN = 1.25f, CLOSED_ANTI_MAGIC = 1.0f;
    /** Ticks the heart stays open after a Word is spoken (3.5 s), with an Arch daemon anchoring him (2 s), after a cut Word, after an Arch falls. */
    public static final int AFTER_WORD = 70, AFTER_WORD_ANCHORED = 40, AFTER_CUT = 100, AFTER_ANCHOR = 100, AFTER_GATES = 100;

    private long openUntil;

    /** Opens the heart for {@code ticks}; a window never shortens one that is already open. */
    public void open(long now, int ticks) { openUntil = Math.max(openUntil, now + Math.max(0, ticks)); }

    /** Is the heart open now? */
    public boolean isOpen(long now) { return now < openUntil; }

    /** Ticks it stays open. */
    public long openLeft(long now) { return Math.max(0, openUntil - now); }

    /** What a blow is multiplied by: an open heart takes extra, a closed one almost nothing, except from anti-magic. */
    public float factor(long now, boolean antiMagic) {
        if (isOpen(now)) return OPEN;
        return antiMagic ? CLOSED_ANTI_MAGIC : CLOSED;
    }

    /** The window a finished Word gives. */
    public static int windowAfterWord(boolean anchored) { return anchored ? AFTER_WORD_ANCHORED : AFTER_WORD; }

    public void close() { openUntil = 0; }
}
