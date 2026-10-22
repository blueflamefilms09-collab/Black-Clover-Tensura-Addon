package com.newuniverse.nusmp.shishou;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Shishou must never be spam, in text or in sound. This pure class (no Minecraft class; time is a tick number the caller passes in) holds the two limits of his speech: one cooldown per WIELDER and
 * GROUP of line ({@link ShishouVoiceLines.Group#cooldown()}), and a gap of {@link #LISTENER_GAP} ticks (4 seconds) between two of his lines for any one LISTENER (the wielder's action bar and a nearby
 * player's ears count the same). Everything is keyed by player UUID (never a static "current player"); {@link #forget} drops a player, {@link #prune} the quiet ones.
 */
public final class ShishouSpeechGate {
    /** At most one line per listener in this many ticks (4 seconds), and only players this near (blocks) hear him. */
    public static final long LISTENER_GAP = 80;
    public static final double RADIUS = 32.0;

    private final Map<UUID, long[]> byWielder = new HashMap<>();
    private final Map<UUID, Long> byListener = new HashMap<>();

    /** May this wielder's sword say a line of the group now? If yes, records it. */
    public boolean wielderMay(UUID wielder, ShishouVoiceLines.Group g, long now) {
        if (wielder == null || g == null) return false;
        long[] last = byWielder.computeIfAbsent(wielder, k -> filled());
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

    /** Would the listener be allowed (nothing recorded)? */
    public boolean listenerCould(UUID listener, long now) {
        Long last = byListener.get(listener);
        return last == null || now - last >= LISTENER_GAP;
    }

    private static long[] filled() {
        long[] a = new long[ShishouVoiceLines.Group.values().length];
        java.util.Arrays.fill(a, Long.MIN_VALUE / 4);
        return a;
    }

    public void forget(UUID player) {
        byWielder.remove(player);
        byListener.remove(player);
    }

    public void clear() {
        byWielder.clear();
        byListener.clear();
    }

    public int tracked() { return byWielder.size() + byListener.size(); }

    public void prune(long now) {
        Iterator<Map.Entry<UUID, Long>> it = byListener.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue() > 2400) it.remove();
        Iterator<Map.Entry<UUID, long[]>> ot = byWielder.entrySet().iterator();
        while (ot.hasNext()) {
            long newest = Long.MIN_VALUE;
            for (long v : ot.next().getValue()) newest = Math.max(newest, v);
            if (now - newest > 12000) ot.remove();
        }
    }
}
