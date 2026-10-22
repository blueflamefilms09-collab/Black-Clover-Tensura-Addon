package com.newuniverse.nusmp.spiritcontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the rate limit of the control plane, per player UUID (never a "current player"): a player may use a given control (summon, release, combat toggle, ability) at most once per {@code gap} game ticks,
 * whether the ask comes from a grimoire page or from the command. Pure. Entries are small and are dropped at logout, death and server stop; the map is also held under {@link #MAX_ENTRIES} so a flood of
 * names can never grow it without end.
 */
public final class PlayerGap {
    public static final int MAX_ENTRIES = 512;

    private final Map<UUID, Long> last = new HashMap<>();

    /** True (and the time is recorded) when at least {@code gap} ticks passed since this player's last allowed ask. */
    public boolean allow(UUID player, long now, int gap) {
        if (player == null) return false;
        Long prev = last.get(player);
        if (prev != null && now >= prev && now - prev < Math.max(0, gap)) return false;
        if (prev == null && last.size() >= MAX_ENTRIES) last.clear();
        last.put(player, now);
        return true;
    }

    public void forget(UUID player) { last.remove(player); }

    public void clear() { last.clear(); }

    public int size() { return last.size(); }
}
