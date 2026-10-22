package com.newuniverse.nusmp.undine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a sound must never be spam. This pure class (no Minecraft class; time is a tick number the caller passes in, so tests drive it) decides on the SERVER whether an Undine may make a sound now:
 * the same cue has a cooldown per owner, a short gap between any two sounds of one owner, and the whole server gets at most {@code maxPerSecond} Undine sounds in any second. The state is keyed by
 * the owner's UUID (never a static "current player") and {@link #forget} drops it on logout. Same pattern as {@code audio.voice.VoiceLimiter}.
 */
public final class UndineVoiceBudget {
    private static final class Entry {
        final Map<String, Long> lastByCue = new HashMap<>();
        long lastAny = Long.MIN_VALUE / 2;
    }

    private final Map<UUID, Entry> owners = new HashMap<>();
    private long windowStart = Long.MIN_VALUE / 2;
    private int inWindow;

    /** Default most sounds per second across the whole server. */
    public static final int DEFAULT_PER_SECOND = 12;
    /** Ticks between two sounds of one owner, whatever the cue. */
    public static final long ANY_GAP = 3;

    /** Asks whether the owner's Undine may sound the cue at tick {@code now}; if yes, records it. {@code cueCooldown} is the least number of ticks between two of that cue (0 = none). */
    public boolean tryAcquire(UUID owner, String cue, long now, long cueCooldown, int maxPerSecond) {
        if (owner == null || cue == null || maxPerSecond < 1) return false;
        Entry e = owners.computeIfAbsent(owner, k -> new Entry());
        Long last = e.lastByCue.get(cue);
        if (last != null && now - last < cueCooldown) return false;
        if (now - e.lastAny < ANY_GAP) return false;
        if (now - windowStart >= 20) { windowStart = now; inWindow = 0; }
        if (inWindow >= maxPerSecond) return false;
        inWindow++;
        e.lastAny = now;
        e.lastByCue.put(cue, now);
        return true;
    }

    public void forget(UUID owner) { owners.remove(owner); }

    public void clear() { owners.clear(); windowStart = Long.MIN_VALUE / 2; inWindow = 0; }

    public int tracked() { return owners.size(); }

    /** Drops owners that have been quiet for a minute (call now and then so the map stays small). */
    public void prune(long now) {
        Iterator<Map.Entry<UUID, Entry>> it = owners.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue().lastAny > 1200) it.remove();
    }
}
