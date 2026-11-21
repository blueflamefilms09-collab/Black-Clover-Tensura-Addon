package com.newuniverse.nusmp.dragonballs;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The spoken half of the ritual. Every chat line is looked at once: a line that is far too long is ignored; the summoner of a running ritual has the line read as a WISH; anyone else has the line added to a
 * small per-player window (at most 4 lines, {@code phraseWindowSeconds} old) and the window is compared with the words of each set. When the words match, the real work (the ritual check, which may scan
 * the ground once) is queued onto the main thread, and that player cannot try again for 3 seconds. State is keyed by player UUID and dropped on logout. Cost per chat line: a few string passes over at
 * most 256 characters and about ten phrases.
 */
public final class DbChat {
    private DbChat() {}

    private static final Map<UUID, DbRules.Window> WINDOWS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED = 512;

    public static void onChat(ServerChatEvent e) {
        if (!DbConfig.enabled()) return;
        ServerPlayer p = e.getPlayer();
        String raw = e.getRawText();
        if (raw == null || raw.length() > 256 || p.getServer() == null) return;
        p.getServer().execute(() -> handle(p, raw));
    }

    /**
     * A line that was not typed in chat but spoken (dragonballs/voice/SpeechBridge): it goes through exactly the same {@link #handle} as a chat line, with the same length cap, and is not shown in chat.
     * Server thread only; the caller has checked that the player is online and alive.
     */
    public static void acceptLine(ServerPlayer p, String raw) {
        if (!DbConfig.enabled() || raw == null || raw.length() > 256) return;
        handle(p, raw);
    }

    static void handle(ServerPlayer p, String raw) {
        DbSession mine = DbSession.byPlayer(p.getUUID());
        if (mine != null) {
            mine.wish(p, raw);
            return;
        }
        long now = System.currentTimeMillis();
        if (WINDOWS.size() >= MAX_TRACKED) clear();
        DbRules.Window w = WINDOWS.computeIfAbsent(p.getUUID(), k -> new DbRules.Window());
        w.add(now, raw);
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < 3000L) return;
        String joined = w.joined(now, DbConfig.windowSeconds() * 1000L);
        for (BallSet set : BallSet.values()) {
            if (!DbRules.matchesAny(joined, DbConfig.phrases(set))) continue;
            LAST.put(p.getUUID(), now);
            w.clear();
            var err = DbManager.trySummon(p, set);
            if (err != null) p.sendSystemMessage(err);
            return;
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        WINDOWS.remove(id);
        LAST.remove(id);
    }

    static void clear() {
        WINDOWS.clear();
        LAST.clear();
    }
}
