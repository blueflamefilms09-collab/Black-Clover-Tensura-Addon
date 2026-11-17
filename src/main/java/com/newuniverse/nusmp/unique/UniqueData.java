package com.newuniverse.nusmp.unique;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Why: the state machine of the Unique Guardian, with no Minecraft type, so the cases a mistake would break are unit tests: a sighting moves a copy between HELD / DROPPED / CONTAINED and cancels any "missing" clock (so a
 * thief who now holds it is not a loss, and a copy that turns up during the grace is never released); a copy that is not accounted for is released into the pool only when the clock of {@link UniqueRules} says so, at most
 * a few times an hour; a release bumps the epoch so every older stamped copy is an old copy; nothing is released twice (a copy in the pool stays there until a new valid copy is seen). The saved-data wrapper only writes
 * this map to NBT. Server thread only.
 */
public final class UniqueData {
    public enum Release { RELEASED, FLAPPING, UNKNOWN }

    private final Map<String, UniqueRecord> recs = new LinkedHashMap<>();

    public UniqueRecord get(String id) { return id == null ? null : recs.get(id); }

    public Collection<UniqueRecord> all() { return Collections.unmodifiableCollection(recs.values()); }

    public int size() { return recs.size(); }

    /** Loading a saved record. */
    public void put(UniqueRecord r) { if (r != null) recs.put(r.id, r); }

    public int epoch(String id) { UniqueRecord r = recs.get(id); return r == null ? 0 : r.epoch; }

    /** Forgets a record (tests). */
    public boolean remove(String id) { return recs.remove(id) != null; }

    // ---------------------------------------------------------------- a valid copy is seen
    /** The one valid copy of {@code id} was seen at {@code spot}. Returns true when something worth saving changed (state, holder, dimension, a cleared missing clock, or a minute since the last write). */
    public boolean see(String id, UniqueSpot spot, long nowTick, long nowMs) {
        if (id == null || spot == null || spot.state() == null || spot.state() == UniqueState.POOL) throw new IllegalArgumentException("a sighting is HELD, DROPPED or CONTAINED");
        UniqueRecord r = recs.computeIfAbsent(id, UniqueRecord::new);
        String dim = spot.dim() == null ? "" : spot.dim();
        boolean changed = r.state != spot.state() || r.missingSince >= 0L || !Objects.equals(r.dim, dim) || nowMs - r.savedMs >= UniqueRules.SEEN_WRITE_MS;
        if (spot.state() == UniqueState.HELD) {
            changed |= !Objects.equals(r.holder, spot.holder());        // theft: the new holder simply replaces the old one, nothing is lost
            r.holder = spot.holder();
            r.seenMs = nowMs;
        } else if (spot.holder() != null) {
            r.holder = spot.holder();                                   // the last holder is kept for the log
        }
        r.state = spot.state();
        r.dim = dim;
        r.x = spot.x();
        r.y = spot.y();
        r.z = spot.z();
        r.entity = spot.state() == UniqueState.DROPPED ? spot.entity() : null;
        r.block = spot.state() == UniqueState.CONTAINED ? spot.block() : UniqueRecord.NO_BLOCK;
        r.seenTick = nowTick;
        r.missingSince = -1L;
        r.confirmed = false;
        if (changed) r.savedMs = nowMs;
        return changed;
    }

    // ---------------------------------------------------------------- a copy is not accounted for
    /**
     * The tracked copy is not accounted for anywhere (gone from its holder's scan, or its item entity was destroyed). {@code confirmed}: a destroying removal was seen. Returns true when the clock was started or
     * confirmed now. A copy already in the pool, or never seen, is not touched.
     */
    public boolean markMissing(String id, long nowTick, boolean confirmed) {
        UniqueRecord r = recs.get(id);
        if (r == null || r.state == UniqueState.POOL) return false;
        if (r.missingSince < 0L) {
            r.missingSince = nowTick;
            r.confirmed = confirmed;
            return true;
        }
        if (confirmed && !r.confirmed) {
            r.missingSince = nowTick;
            r.confirmed = true;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- back in the pool
    /**
     * The copy is gone for good: the unique returns to its normal sources. Bumps the epoch. {@link Release#FLAPPING} when the flap guard refuses (the record stays missing and is asked again later);
     * {@link Release#UNKNOWN} when nothing was tracked or it is already in the pool (so it is never released twice).
     */
    public Release release(String id, long nowMs, String reason) {
        UniqueRecord r = recs.get(id);
        if (r == null || r.state == UniqueState.POOL) return Release.UNKNOWN;
        if (!UniqueRules.mayRelease(r, nowMs)) return Release.FLAPPING;
        if (r.stampCount == UniqueRecord.STAMPS) {
            System.arraycopy(r.stamps, 1, r.stamps, 0, UniqueRecord.STAMPS - 1);
            r.stampCount--;
        }
        r.stamps[r.stampCount++] = nowMs;
        r.lastReleaseMs = nowMs;
        r.releases++;
        toPool(r, reason);
        return Release.RELEASED;
    }

    /** The owning system itself put the unique back (its own reset, its own rule): the guardian only notes it, with no flap guard and no count. Returns true if the record was not in the pool. */
    public boolean retire(String id, String reason) {
        UniqueRecord r = recs.get(id);
        if (r == null || r.state == UniqueState.POOL) return false;
        toPool(r, reason);
        return true;
    }

    private static void toPool(UniqueRecord r, String reason) {
        r.state = UniqueState.POOL;
        r.epoch++;
        r.missingSince = -1L;
        r.confirmed = false;
        r.entity = null;
        r.block = UniqueRecord.NO_BLOCK;
        r.lastReason = reason == null ? "" : reason;
    }

    public UUID holderOf(String id) { UniqueRecord r = recs.get(id); return r == null ? null : r.holder; }
}
