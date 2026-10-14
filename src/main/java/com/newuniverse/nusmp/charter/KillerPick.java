package com.newuniverse.nusmp.charter;

import java.util.UUID;

/**
 * Who gets the grimoire (pure). The player who dealt the killing blow (or whose pet, summon or projectile did); failing that, the last player who
 * hurt the boss within thirty seconds. A killer who is not eligible (creative, spectator) gets nothing, and nobody else is picked in their place:
 * the drop is for fights, not for admins finishing one off.
 */
public final class KillerPick {
    private KillerPick() {}

    /** 30 seconds. */
    public static final long FALLBACK_TICKS = 600L;

    /** A possible recipient: who, whether they may receive it, and how long ago they last hurt the boss (0 for the killing blow). */
    public record Candidate(UUID id, boolean eligible, long ageTicks) {}

    /** The recipient, or null. {@code direct} is the killing blow's player (null if it was not a player), {@code recent} the last attacker. */
    public static UUID choose(Candidate direct, Candidate recent) {
        if (direct != null) return direct.eligible() && direct.id() != null ? direct.id() : null;
        if (recent == null || recent.id() == null || !recent.eligible()) return null;
        return recent.ageTicks() >= 0L && recent.ageTicks() <= FALLBACK_TICKS ? recent.id() : null;
    }
}
