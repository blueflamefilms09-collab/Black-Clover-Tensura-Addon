package com.newuniverse.nusmp.dungeon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A cooldown per key, in game ticks, kept in memory (the meal attunement lets a spell's rider act once a second on each target, however
 * many times the spell hits it). Pure. Old entries are swept when the table grows, and a clock that went backwards (another world was
 * opened in the same session) counts as ready, so nothing here can leak or lock a key out.
 */
public final class Cooldowns<K> {
    private static final int SWEEP_AT = 512;

    private final int cooldown;
    private final Map<K, Long> last = new HashMap<>();

    public Cooldowns(int cooldown) { this.cooldown = cooldown; }

    /** True, and the key starts cooling, if it is ready at {@code now}. */
    public boolean tryUse(K key, long now) {
        if (last.size() > SWEEP_AT) sweep(now);
        Long at = last.get(key);
        if (at != null && now >= at && now - at < cooldown) return false;
        last.put(key, now);
        return true;
    }

    private void sweep(long now) {
        for (Iterator<Long> it = last.values().iterator(); it.hasNext(); ) {
            long at = it.next();
            if (now < at || now - at >= cooldown) it.remove();
        }
    }

    public int size() { return last.size(); }

    public void clear() { last.clear(); }
}
