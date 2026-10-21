package com.newuniverse.nusmp.audio.voice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a spoken line must never be spam. This pure class (no Minecraft class; time is a tick number the caller passes in, so tests drive it) decides on the SERVER whether a player may
 * be sent a line now: the same line has a cooldown per player ({@code lineCooldownTicks}), any line has a short per-player gap, and a player gets at most {@code maxPerSecond} lines in
 * any second (the server option {@code voice.maxPerSecond}). The state is keyed by player UUID, never by a static "current player", and {@link #forget} drops it on logout.
 */
public final class VoiceLimiter {
    /** Default cooldown of one and the same line for one player: 8 seconds. */
    public static final long DEFAULT_LINE_COOLDOWN = 160;
    /** Ticks per second. */
    public static final long SECOND = 20;

    private static final class Entry {
        final Map<String, Long> lastByLine = new HashMap<>();
        long windowStart = Long.MIN_VALUE / 2;
        int inWindow;
        long lastAny = Long.MIN_VALUE / 2;
    }

    private final Map<UUID, Entry> players = new HashMap<>();
    private final long lineCooldownTicks;

    public VoiceLimiter() { this(DEFAULT_LINE_COOLDOWN); }

    public VoiceLimiter(long lineCooldownTicks) { this.lineCooldownTicks = Math.max(0, lineCooldownTicks); }

    /**
     * Asks whether the player may be sent the line at tick {@code now}; if yes, records it. {@code cooldownTicks} overrides the default cooldown of this line when it is not negative;
     * {@code maxPerSecond} below 1 means "no lines at all".
     */
    public boolean tryAcquire(UUID player, String line, long now, int maxPerSecond, long cooldownTicks) {
        if (player == null || line == null || maxPerSecond < 1) return false;
        Entry e = players.computeIfAbsent(player, k -> new Entry());
        long cd = cooldownTicks >= 0 ? cooldownTicks : lineCooldownTicks;
        Long last = e.lastByLine.get(line);
        if (last != null && now - last < cd) return false;
        if (now - e.lastAny < 4) return false;                              // never two lines on top of each other
        if (now - e.windowStart >= SECOND) {
            e.windowStart = now;
            e.inWindow = 0;
        }
        if (e.inWindow >= maxPerSecond) return false;
        e.inWindow++;
        e.lastAny = now;
        e.lastByLine.put(line, now);
        return true;
    }

    public boolean tryAcquire(UUID player, String line, long now, int maxPerSecond) { return tryAcquire(player, line, now, maxPerSecond, -1); }

    public void forget(UUID player) { players.remove(player); }

    public void clear() { players.clear(); }

    public int tracked() { return players.size(); }

    /** Drops lines whose cooldown is over and players with nothing left (call now and then so the map stays small). */
    public void prune(long now) {
        Iterator<Map.Entry<UUID, Entry>> it = players.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            e.lastByLine.values().removeIf(t -> now - t >= lineCooldownTicks * 4 + 1200);
            if (e.lastByLine.isEmpty() && now - e.lastAny > 1200) it.remove();
        }
    }
}
