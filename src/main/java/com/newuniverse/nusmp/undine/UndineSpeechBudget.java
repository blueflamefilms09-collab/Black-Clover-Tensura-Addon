package com.newuniverse.nusmp.undine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a spoken line must never be spam, and it must be the Undine's own pace, not the whole voice system's. This pure class (no Minecraft class; time is a tick number the caller passes in) holds
 * the two limits that are the Undine's: one cooldown per OWNER and GROUP of line ({@link UndineLines.Group#cooldown()}), and a gap of {@link #LISTENER_GAP} ticks (4 seconds) between two of her lines for
 * any one LISTENER, whoever's Undine says them. {@code audio.voice.Voice} applies its own limits on top (a gap between lines, {@code voice.maxPerSecond}, a cooldown per line). Everything is keyed by
 * player UUID, never by a static "current player"; {@link #forget} drops a player (logout), {@link #prune} the quiet ones.
 */
public final class UndineSpeechBudget {
    /** At most one spoken line per listener in this many ticks (4 seconds). */
    public static final long LISTENER_GAP = 80;
    /** Players farther from the speaker than this (blocks) are not sent the line. */
    public static final double RADIUS = 32.0;

    private final Map<UUID, long[]> byOwner = new HashMap<>();
    private final Map<UUID, Long> byListener = new HashMap<>();

    /** May this owner's Undine say a line of the group now? If yes, records it. */
    public boolean ownerMay(UUID owner, UndineLines.Group g, long now) {
        if (owner == null || g == null) return false;
        long[] last = byOwner.computeIfAbsent(owner, k -> filled());
        if (now - last[g.ordinal()] < g.cooldown()) return false;
        last[g.ordinal()] = now;
        return true;
    }

    /** May this listener hear a line now? If yes, records it. */
    public boolean listenerMay(UUID listener, long now) {
        if (listener == null) return false;
        Long last = byListener.get(listener);
        if (last != null && now - last < LISTENER_GAP) return false;
        byListener.put(listener, now);
        return true;
    }

    /** Would the listener be allowed (no recording)? */
    public boolean listenerCould(UUID listener, long now) {
        Long last = byListener.get(listener);
        return last == null || now - last >= LISTENER_GAP;
    }

    private static long[] filled() {
        long[] a = new long[UndineLines.Group.values().length];
        java.util.Arrays.fill(a, Long.MIN_VALUE / 4);
        return a;
    }

    public void forget(UUID player) { byOwner.remove(player); byListener.remove(player); }

    public void clear() { byOwner.clear(); byListener.clear(); }

    public int tracked() { return byOwner.size() + byListener.size(); }

    /** Drops listeners whose gap is long over and owners with nothing recent (call now and then so the maps stay small). */
    public void prune(long now) {
        Iterator<Map.Entry<UUID, Long>> it = byListener.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue() > 2400) it.remove();
        Iterator<Map.Entry<UUID, long[]>> ot = byOwner.entrySet().iterator();
        while (ot.hasNext()) {
            long newest = Long.MIN_VALUE;
            for (long v : ot.next().getValue()) newest = Math.max(newest, v);
            if (now - newest > 12000) ot.remove();
        }
    }
}
