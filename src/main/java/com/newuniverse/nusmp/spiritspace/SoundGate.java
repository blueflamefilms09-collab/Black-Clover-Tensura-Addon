package com.newuniverse.nusmp.spiritspace;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the spirit makes sounds (a hum while it is out, pings, a swell during a portal, a whoosh for a cut) and the server plays them to the players nearby. Sounds are small, but a crowd of
 * casters must not be able to flood the network, so the server asks this gate first: the same sound of the same player is let through at most once per its own gap, and at most
 * {@link #PER_TICK} sounds of the whole spirit system go out in one game tick. Pure (no game classes), single threaded (the server thread), keyed by the player's UUID, dropped with the player.
 */
public final class SoundGate {
    /** The sound kinds, each with the least ticks between two of them from one player. */
    public enum Kind {
        HUM(120), PING(10), UNLINK(10), TEAR(20), SWELL(30), OPEN(20), GRAVITY(20), WISP(30), DENY(20),
        /** The creature voice cues (no sound event of their own kind: the cues are {@link SpaceMoodRules.Voice}s); the least ticks between two cues of one spirit. */
        VOICE(60);

        public final int gap;

        Kind(int gap) { this.gap = gap; }

        /** The sound event path of this kind: {@code spirit_space.hum}. */
        public String eventId() { return "spirit_space." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final int PER_TICK = 6;

    private final Map<UUID, long[]> last = new HashMap<>();
    private long tick = Long.MIN_VALUE;
    private int used;

    /** May this player's sound of this kind play at game time {@code now}? Counts it when yes. */
    public boolean allow(UUID id, Kind kind, long now) {
        if (id == null) return false;
        if (now != tick) { tick = now; used = 0; }
        if (used >= PER_TICK) return false;
        long[] l = last.computeIfAbsent(id, k -> filled());
        if (now - l[kind.ordinal()] < kind.gap) return false;
        l[kind.ordinal()] = now;
        used++;
        return true;
    }

    private static long[] filled() {
        long[] a = new long[Kind.values().length];
        java.util.Arrays.fill(a, Long.MIN_VALUE / 4);
        return a;
    }

    public void forget(UUID id) { last.remove(id); }

    public void clear() { last.clear(); tick = Long.MIN_VALUE; used = 0; }

    public int tracked() { return last.size(); }
}
