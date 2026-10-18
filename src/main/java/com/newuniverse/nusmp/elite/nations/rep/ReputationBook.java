package com.newuniverse.nusmp.elite.nations.rep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: who stands how with which nation: the per-player, per-nation (or per-faction id) reputation values, pure and persisted by {@code NationNbt}. Values are kept clamped to the tier range and zero values
 * are dropped so the book does not grow with players who never did anything. The caller (the reputation service) turns a change into events and effects.
 */
public final class ReputationBook {
    private final Map<UUID, Map<String, Integer>> values = new HashMap<>();

    public int get(UUID player, String nation) {
        Map<String, Integer> m = values.get(player);
        Integer v = m == null ? null : m.get(nation);
        return v == null ? 0 : v;
    }

    public ReputationTier tier(UUID player, String nation) { return ReputationTier.of(get(player, nation)); }

    /** Sets the value (clamped). Returns the new value. */
    public int set(UUID player, String nation, int value) {
        int v = ReputationTier.clamp(value);
        if (player == null || nation == null || nation.isEmpty()) return 0;
        if (v == 0) {
            Map<String, Integer> m = values.get(player);
            if (m != null) { m.remove(nation); if (m.isEmpty()) values.remove(player); }
        } else {
            values.computeIfAbsent(player, k -> new HashMap<>()).put(nation, v);
        }
        return v;
    }

    /** Adds a delta (clamped). Returns the new value. */
    public int add(UUID player, String nation, int delta) { return set(player, nation, get(player, nation) + delta); }

    /** Every nation a player has a non-zero value with. */
    public Map<String, Integer> of(UUID player) { return Map.copyOf(values.getOrDefault(player, Map.of())); }

    /** Forgets a nation everywhere (disbanded). */
    public void removeNation(String nation) {
        values.values().forEach(m -> m.remove(nation));
        values.values().removeIf(Map::isEmpty);
    }

    /** One decay step for everybody. Returns how many values changed. */
    public int decayAll(int amount) {
        int changed = 0;
        for (var it = values.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            for (var mi = e.getValue().entrySet().iterator(); mi.hasNext(); ) {
                var me = mi.next();
                int nv = ReputationTier.decay(me.getValue(), amount);
                if (nv != me.getValue()) { changed++; if (nv == 0) mi.remove(); else me.setValue(nv); }
            }
            if (e.getValue().isEmpty()) it.remove();
        }
        return changed;
    }

    public Map<UUID, Map<String, Integer>> raw() { return values; }
}
