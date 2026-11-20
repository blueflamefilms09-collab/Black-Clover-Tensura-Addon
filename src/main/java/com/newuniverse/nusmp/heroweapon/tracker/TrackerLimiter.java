package com.newuniverse.nusmp.heroweapon.tracker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a client can send the tracker request as often as it likes, so the server answers each player at most once per {@link #GAP_TICKS} (2 seconds) and drops the rest without reply. Pure (a map from player uuid to the
 * tick of the last answer, no Minecraft type) so the rule is a unit test. The table holds one entry per player who asked, is emptied of old entries whenever it grows past {@link #SWEEP_AT}, and a player's entry is
 * dropped at logout ({@link #forget}); the server thread is the only caller.
 */
public final class TrackerLimiter {
    /** Ticks between two answers for one player (40 = 2 seconds). */
    public static final int GAP_TICKS = 40;
    /** The size at which old entries are swept out (more than any realistic number of players online at once). */
    static final int SWEEP_AT = 256;

    private final Map<UUID, Long> last = new HashMap<>();

    /** True when {@code id} may be answered now (and records it); false when the request is too soon and must be dropped. A clock that went backwards (a reloaded world) is allowed. */
    public boolean allow(UUID id, long nowTick) {
        if (id == null) return false;
        Long prev = last.get(id);
        if (prev != null && nowTick >= prev && nowTick - prev < GAP_TICKS) return false;
        if (last.size() >= SWEEP_AT) sweep(nowTick);
        last.put(id, nowTick);
        return true;
    }

    /** Drops the entry of a player who left. */
    public void forget(UUID id) { last.remove(id); }

    /** Drops everything (the server is stopping). */
    public void clear() { last.clear(); }

    public int size() { return last.size(); }

    private void sweep(long nowTick) {
        for (Iterator<Map.Entry<UUID, Long>> it = last.entrySet().iterator(); it.hasNext(); ) {
            long t = it.next().getValue();
            if (nowTick < t || nowTick - t >= GAP_TICKS) it.remove();
        }
    }
}
