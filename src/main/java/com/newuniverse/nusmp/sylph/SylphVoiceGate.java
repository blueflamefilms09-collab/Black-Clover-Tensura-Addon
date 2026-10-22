package com.newuniverse.nusmp.sylph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a LISTENER must never be talked at. Whatever the Sylphs near a player decide, this pure gate (no game class; time is a tick number the caller passes) lets at most one spoken line through per
 * listener every {@link #GAP} ticks (4 seconds), and the same group at most once per {@link #GROUP_GAP} ticks, so a crowd of Yuno players with Sylphs cannot stack their voices on one listener. It sits
 * in front of the voice system's own limiter ({@code audio/voice/VoiceLimiter}: the server's {@code voice.maxPerSecond}, the client's volume and switch). State is keyed by the listener's UUID and
 * dropped on logout.
 */
public final class SylphVoiceGate {
    /** At least this many ticks between two Sylph lines to one listener (4 seconds); the same group needs this many (12 seconds). */
    public static final long GAP = 80, GROUP_GAP = 240;

    private static final class Entry {
        long lastAny = Long.MIN_VALUE / 4;
        final Map<String, Long> lastByGroup = new HashMap<>();
    }

    private final Map<UUID, Entry> listeners = new HashMap<>();

    /** Asks whether the listener may be sent a line of {@code group} at {@code now}; if yes, records it. */
    public boolean tryAcquire(UUID listener, String group, long now) {
        if (listener == null || group == null) return false;
        Entry e = listeners.computeIfAbsent(listener, k -> new Entry());
        if (now - e.lastAny < GAP) return false;
        if (now - e.lastByGroup.getOrDefault(group, Long.MIN_VALUE / 4) < GROUP_GAP) return false;
        e.lastAny = now;
        e.lastByGroup.put(group, now);
        return true;
    }

    public void forget(UUID listener) { listeners.remove(listener); }

    public void clear() { listeners.clear(); }

    public int tracked() { return listeners.size(); }

    /** Drops listeners that have been quiet for a while (call now and then so the map stays small). */
    public void prune(long now) {
        Iterator<Map.Entry<UUID, Entry>> it = listeners.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue().lastAny > GROUP_GAP * 4) it.remove();
    }
}
