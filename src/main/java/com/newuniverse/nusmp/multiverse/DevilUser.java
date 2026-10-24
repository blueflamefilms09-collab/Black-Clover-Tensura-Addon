package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.GrimoireCover;

/**
 * Who counts as a "devil user" for the Great Spirit menu skin (Slime Crest instead of Parchment and Ink). Decided on the server and sent to the
 * player inside the status summary the mod already syncs ({@code MultiverseSync}, key {@code Devil}); the client only reads it, so there is no
 * new packet and nothing the client could fake into a real effect (it is a look only).
 *
 * <p>A player is a devil user when any of these holds: their grimoire cover is forbidden (a devil lives in it, {@link GrimoireCover#isForbidden}),
 * the grimoire is bound to a devil, Devil Union is or was active (the grimoire remembers {@code UnionUntil}; the Anti-Magic Devil Union mode is
 * mode 3), the player is a Devil by profile, or their Tensura race family is Daemon / Devil. Pure (no player access), unit tested.
 */
public final class DevilUser {
    private DevilUser() {}

    /** The Anti-Magic mode index of Devil Union ({@code MultiverseProfile.ANTI_MODES}). */
    public static final int ANTI_DEVIL_UNION = 3;

    public static boolean is(GrimoireCover cover, boolean devilBound, long unionUntil, int antiMode, Race profileRace, Race tensuraFamily) {
        if (cover != null && cover.isForbidden()) return true;
        if (devilBound) return true;
        if (unionUntil > 0L) return true;                       // Devil Union is active now or was at some point
        if (antiMode == ANTI_DEVIL_UNION) return true;
        if (profileRace == Race.DEVIL || profileRace == Race.DAEMON) return true;
        return tensuraFamily == Race.DEVIL || tensuraFamily == Race.DAEMON;
    }
}
