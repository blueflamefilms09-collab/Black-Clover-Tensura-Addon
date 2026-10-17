package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: a pop-up line of the System is a lang key plus arguments, and the only other thing the overlay needs to know is what KIND of news it is, because the
 * kind decides its colour and its small marker (cyan for information, green for success, red for failure, gold for a reward, violet for a shadow). The kind
 * crosses the network as one byte, so each kind has a FIXED code that is never reused or renumbered (the same discipline as a saved format), and an unknown
 * code reads as plain information instead of failing the packet. Pure.
 */
public enum ToastKind {
    /** Plain news (a floor reached, a quest arrived). */
    INFO(0),
    /** A quest completed. */
    SUCCESS(1),
    /** A quest failed. */
    FAILURE(2),
    /** A reward: a stat point, a Shadow Core. */
    REWARD(3),
    /** News about a shadow: extraction ready, a shadow raised, a shadow lost. */
    SHADOW(4);

    private final int code;

    ToastKind(int code) { this.code = code; }

    /** The byte that goes over the wire. */
    public int code() { return code; }

    /** The kind with this code, or {@link #INFO} for an unknown one. */
    public static ToastKind fromCode(int code) {
        for (ToastKind k : values()) if (k.code == code) return k;
        return INFO;
    }
}
