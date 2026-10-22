package com.newuniverse.nusmp.shishou;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Why: Doppelganger makes a few flying duplicate blades that strike the current target for a short life. They are NOT entities: they are rows of this table, simulated on the server (one small step every
 * {@link ShishouRules#CLONE_PERIOD} ticks, a particle trail through the perf budget), never saved to disk, never more than {@link ShishouRules#CLONES_GLOBAL} in the whole world and
 * {@link ShishouRules#CLONES_PER_CAST} per cast, dropped at logout, death, dimension change and server stop. The table is pure: the caps, the lifetime and the bookkeeping are unit tests.
 */
public final class ShishouClones {
    /** One live clone: its wielder, its target, the tick it dissolves and the tick of its next strike. */
    public static final class Clone {
        public final UUID wielder, target;
        public final long endTick;
        public long nextStrike;
        public final int slot;

        Clone(UUID wielder, UUID target, long endTick, long nextStrike, int slot) {
            this.wielder = wielder;
            this.target = target;
            this.endTick = endTick;
            this.nextStrike = nextStrike;
            this.slot = slot;
        }
    }

    private final List<Clone> live = new ArrayList<>();

    /** Spawns up to {@code wanted} clones (never more than the per-cast and the world cap allow); returns how many were made. */
    public int spawn(UUID wielder, UUID target, int wanted, long now, int life, int perCast, int worldCap) {
        if (wielder == null || target == null || wanted <= 0) return 0;
        int room = Math.max(0, worldCap - live.size());
        int n = Math.min(Math.min(wanted, perCast), room);
        for (int i = 0; i < n; i++) live.add(new Clone(wielder, target, now + Math.max(1, life), now + ShishouRules.CLONE_PERIOD, i));
        return n;
    }

    public List<Clone> live() { return live; }

    public int count() { return live.size(); }

    public int count(UUID wielder) {
        int c = 0;
        for (Clone k : live) if (k.wielder.equals(wielder)) c++;
        return c;
    }

    /** Removes the ended clones; returns how many were removed. */
    public int sweep(long now) {
        int before = live.size();
        Iterator<Clone> it = live.iterator();
        while (it.hasNext()) if (it.next().endTick <= now) it.remove();
        return before - live.size();
    }

    /** Removes every clone of a wielder (logout, death, dimension change). */
    public int forget(UUID wielder) {
        int before = live.size();
        live.removeIf(k -> k.wielder.equals(wielder));
        return before - live.size();
    }

    public void clear() { live.clear(); }
}
