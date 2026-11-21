package com.newuniverse.nusmp.bossdrops2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: "who fought the boss" has to be known when it dies, and the game keeps no per-player damage. This is the small ledger that remembers it (pure, unit tested): boss UUID -> player UUID -> damage dealt.
 * Bounds, so it can never grow on a public server: at most {@link #MAX_BOSSES} bosses at once (the least recently hit is forgotten first), at most {@link #MAX_PLAYERS} players per boss (a latecomer past
 * that is not counted), and at most {@link #MAX_PAID} remembered "already paid" boss ids (so a second death event of the same boss pays nothing). A boss is forgotten when it is paid ({@link #take}).
 * One add is two hash lookups and no allocation once the player is in. Server thread only.
 */
public final class DamageLedger {
    public static final int MAX_BOSSES = 64, MAX_PLAYERS = 16, MAX_PAID = 256;

    private final LinkedHashMap<UUID, Map<UUID, Double>> hits = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Map<UUID, Double>> eldest) { return size() > MAX_BOSSES; }
    };
    private final Set<UUID> paid = new LinkedHashSet<>();

    /** Adds a hit; a hit of zero, a negative one or a NaN is ignored. */
    public void add(UUID boss, UUID player, double amount) {
        if (boss == null || player == null || !(amount > 0.0) || Double.isInfinite(amount)) return;
        Map<UUID, Double> m = hits.computeIfAbsent(boss, k -> new HashMap<>());
        Double have = m.get(player);
        if (have == null && m.size() >= MAX_PLAYERS) return;
        m.put(player, have == null ? amount : have + amount);
    }

    /** Removes and returns what was dealt to this boss (never null). */
    public Map<UUID, Double> take(UUID boss) {
        Map<UUID, Double> m = boss == null ? null : hits.remove(boss);
        return m == null ? Map.of() : m;
    }

    /** True the first time a boss is claimed for payment, false for every later call with the same id. */
    public boolean claim(UUID boss) {
        if (boss == null || !paid.add(boss)) return false;
        if (paid.size() > MAX_PAID) {
            var it = paid.iterator();
            it.next();
            it.remove();
        }
        return true;
    }

    public int bosses() { return hits.size(); }

    public void clear() {
        hits.clear();
        paid.clear();
    }
}
