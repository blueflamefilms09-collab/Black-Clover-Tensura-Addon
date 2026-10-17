package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the decennial Metal Vessel is promised ONCE per sector, and a boss can die twice from the server's point of view (a Guardian re-forged at its
 * altar, an admin command, a crashed save replayed), so the reward needs a memory that survives a restart: which sectors have already paid, and
 * with which relic. The rule is tiny and must never be wrong (a second vessel from one sector is a duplicated artifact, a missing one is a broken
 * promise), so it is a pure class with no Minecraft import: {@link #claim} is true exactly once per sector, and the saved-data wrapper
 * ({@code FlowData}) only turns it into NBT. The relic id is kept alongside so an admin can see who got what.
 */
public final class RewardLog {
    private final Map<Integer, String> given = new LinkedHashMap<>();

    /** Records that the sector paid this relic. True the first time for the sector, false (and nothing changes) if it had paid already. */
    public boolean claim(int sectorId, String relicId) {
        if (given.containsKey(sectorId)) return false;
        given.put(sectorId, relicId == null ? "" : relicId);
        return true;
    }

    /** Loads an entry from saved data without the once-only check being an event (a repeated sector keeps its first relic). */
    public void restore(int sectorId, String relicId) { given.putIfAbsent(sectorId, relicId == null ? "" : relicId); }

    public boolean contains(int sectorId) { return given.containsKey(sectorId); }

    /** The relic the sector paid, or null if it has not paid. */
    public String relicOf(int sectorId) { return given.get(sectorId); }

    public int size() { return given.size(); }

    /** Sector id to relic id, in the order the sectors paid (read-only). */
    public Map<Integer, String> entries() { return Collections.unmodifiableMap(given); }
}
