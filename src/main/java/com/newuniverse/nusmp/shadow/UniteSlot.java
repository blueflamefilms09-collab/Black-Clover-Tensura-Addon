package com.newuniverse.nusmp.shadow;

/**
 * Why: which Unite mode a player is in and until when is the one fact the armour overlay, the stats and the abilities all follow. Keeping the
 * bookkeeping (start replaces, stop ends, expiry is detected exactly once) in a pure value makes the on/off rules testable; the server wraps it
 * around the player's persistent data.
 *
 * @param kind  the {@link UniteKind} ordinal, -1 = none
 * @param until game time at which the mode ends
 */
public record UniteSlot(int kind, long until) {
    public static final UniteSlot NONE = new UniteSlot(-1, 0);

    /** The mode started now for {@code ticks} (replaces whatever was running). */
    public static UniteSlot start(UniteKind kind, long now, int ticks) {
        return ticks <= 0 ? NONE : new UniteSlot(kind.ordinal(), now + ticks);
    }

    public boolean active(long now) {
        return kind >= 0 && now < until && UniteKind.of(kind) != null;
    }

    /** The running mode, or null. */
    public UniteKind running(long now) {
        return active(now) ? UniteKind.of(kind) : null;
    }

    /** True once a mode that was set has run out (so the cleanup of attributes and the overlay happens exactly then). */
    public boolean expired(long now) {
        return kind >= 0 && now >= until;
    }

    public int ticksLeft(long now) {
        return active(now) ? (int) (until - now) : 0;
    }

    /** Ends the mode early. */
    public UniteSlot stopped() {
        return NONE;
    }

    /** Does a new start of {@code kind} replace a different running mode (so the old overlay and stats must go first)? */
    public boolean replaces(UniteKind kind, long now) {
        return active(now) && this.kind != kind.ordinal();
    }
}
