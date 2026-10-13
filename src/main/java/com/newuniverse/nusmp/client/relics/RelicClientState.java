package com.newuniverse.nusmp.client.relics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** What the client knows about relic cooldowns (told by the server), kept as game times; read by the HUD line and the tooltip. No Minecraft types. */
public final class RelicClientState {
    private RelicClientState() {}

    private static final Map<String, Long> READY = new ConcurrentHashMap<>();

    private static String key(String relicId, String actKey) { return relicId + "/" + actKey; }

    /** The server says this act is ready again at {@code readyAt} (a time not in the future clears it). */
    public static void cooldown(String relicId, String actKey, long readyAt) {
        if (READY.size() > 512) READY.clear();
        READY.put(key(relicId, actKey), readyAt);
    }

    /** Ticks left on the cooldown at game time {@code now} (0 if ready or unknown). */
    public static int remaining(String relicId, String actKey, long now) {
        Long at = READY.get(key(relicId, actKey));
        if (at == null || at <= now) return 0;
        return (int) Math.min(Integer.MAX_VALUE, at - now);
    }

    public static void clear() { READY.clear(); }
}
