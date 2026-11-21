package com.newuniverse.nusmp.rebirth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a rite needs two clicks, and the first only warns. This is the one small map of the feature: per player UUID, the game tick of the warning and which rite it was for. At most one entry per
 * player online; {@link RebirthBoot} clears a player's entry on logout, death and dimension change and the whole map when the server stops. Server thread only; no other state, nothing is saved.
 */
public final class RebirthState {
    private RebirthState() {}

    private record Warning(RebirthRules.Rite rite, long at) {}

    private static final Map<UUID, Warning> WARNED = new HashMap<>();

    static void warn(UUID id, RebirthRules.Rite rite, long now) { WARNED.put(id, new Warning(rite, now)); }

    /** The tick of this player's warning for this rite, or -1. */
    static long warnedAt(UUID id, RebirthRules.Rite rite) {
        Warning w = WARNED.get(id);
        return w != null && w.rite == rite ? w.at : -1;
    }

    public static void forget(UUID id) { WARNED.remove(id); }

    public static void clearAll() { WARNED.clear(); }

    public static int size() { return WARNED.size(); }
}
