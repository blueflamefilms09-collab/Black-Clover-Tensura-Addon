package com.newuniverse.nusmp.heroweapon.weapon.sword;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Gold Rebellion marks the foes it strikes as gilded, and the wielder's next blow on a gilded foe takes a little more from it. A mark is a promise with an end
 * time, so the marks need a table; a table must not grow without limit on a server that runs for weeks, so this one holds at most {@link SwordNumbers#MARK_CAP}
 * (the mark that ends soonest makes room), drops what has ended whenever it is read, and treats a mark stamped far in the future (a clock that went back) as gone.
 * No Minecraft types, tested alone.
 */
public final class MarkBook {
    private final Map<UUID, Long> marks = new HashMap<>();

    /** Marks a foe until {@code now + ticks} (a longer mark is never shortened). */
    public void mark(UUID foe, long now, int ticks) {
        if (foe == null || ticks <= 0) return;
        prune(now);
        Long old = marks.get(foe);
        long until = now + ticks;
        if (old != null && old > until && old - now <= SwordNumbers.GILDED_TICKS * 2L) until = old;
        if (old == null && marks.size() >= SwordNumbers.MARK_CAP) {
            UUID soonest = null;
            for (Map.Entry<UUID, Long> e : marks.entrySet()) if (soonest == null || e.getValue() < marks.get(soonest)) soonest = e.getKey();
            if (soonest != null) marks.remove(soonest);
        }
        marks.put(foe, until);
    }

    /** True if the foe is marked at {@code now}. */
    public boolean has(UUID foe, long now) {
        Long u = marks.get(foe);
        if (u == null) return false;
        if (u <= now || u - now > SwordNumbers.GILDED_TICKS * 4L) {
            marks.remove(foe);
            return false;
        }
        return true;
    }

    /** Takes the mark off a foe and says whether there was one. */
    public boolean consume(UUID foe, long now) {
        boolean had = has(foe, now);
        marks.remove(foe);
        return had;
    }

    public void prune(long now) {
        for (Iterator<Long> it = marks.values().iterator(); it.hasNext(); ) if (it.next() <= now) it.remove();
    }

    public int size() { return marks.size(); }

    public void clear() { marks.clear(); }
}
