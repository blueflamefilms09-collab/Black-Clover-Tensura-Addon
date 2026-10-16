package com.newuniverse.nusmp.song;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the only meeting point between the voice thread and the game thread. The voice bridge (or the fallback key) writes numbers
 * into this map, the server ticker reads them, and nothing else is shared, so no game object is ever touched from the audio thread
 * and the mod does not load a single voice-mod class unless that mod is installed.
 */
public final class SingingState {
    private static final Map<UUID, SingingMeter> METERS = new ConcurrentHashMap<>();

    private SingingState() {}

    /** Voice thread: one microphone frame, as an RMS (0..1), from player id. */
    public static void feedRms(UUID id, float rms, long nowMs) {
        float loud = SongMath.loudness(rms, SongConfig.gate(), SongConfig.ceiling());
        METERS.computeIfAbsent(id, k -> new SingingMeter()).feed(loud, nowMs, SongConfig.holdMs());
    }

    /** Any thread: the key fallback holds a steady level while the key is down, 0 on release. */
    public static void setSteady(UUID id, float level, long nowMs) {
        if (level <= 0f) { METERS.remove(id); return; }
        METERS.computeIfAbsent(id, k -> new SingingMeter()).force(level, nowMs);
    }

    /** Game thread: the singing level of this player right now, 0..1. */
    public static float level(UUID id, long nowMs) {
        SingingMeter m = METERS.get(id);
        return m == null ? 0f : m.level(nowMs, SongConfig.holdMs());
    }

    public static void clear(UUID id) { METERS.remove(id); }

    public static void clearAll() { METERS.clear(); }
}
