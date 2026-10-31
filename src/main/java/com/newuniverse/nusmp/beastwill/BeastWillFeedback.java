package com.newuniverse.nusmp.beastwill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the messages of the Beast Will system (3.45.0) without chat flooding. Every message is a lang key {@code nusmp.beastwill.msg.<key>}; most go to the action bar (one line that replaces itself), only
 * the big moments (absorbed, phase unlocked, rejection) go to chat. The same key is never repeated inside its window ({@link #SAME_TICKS}), and one player gets at most {@link #BUDGET} lines per
 * {@link #WINDOW} ticks, the rest is dropped: a bug or a spammed key can not fill a chat. Per-player memory is a few entries, dropped at logout ({@link #forget}). Server thread only.
 */
public final class BeastWillFeedback {
    private BeastWillFeedback() {}

    public static final int SAME_TICKS = 40, WINDOW = 100, BUDGET = 5;

    private static final Map<UUID, Map<String, Long>> LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, long[]> WINDOW_USE = new ConcurrentHashMap<>();

    /** Sends one message to the player. {@code chat} true = a line in the chat, false = the action bar. */
    public static void say(ServerPlayer p, String key, boolean chat, Object... args) {
        long now = p.level().getGameTime();
        Map<String, Long> mine = LAST.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
        Long last = mine.get(key);
        if (last != null && now - last < SAME_TICKS) return;
        long[] use = WINDOW_USE.computeIfAbsent(p.getUUID(), u -> new long[2]);          // [0] window start, [1] lines in the window
        if (now - use[0] >= WINDOW || now < use[0]) {
            use[0] = now;
            use[1] = 0;
        }
        if (use[1] >= BUDGET) return;
        use[1]++;
        if (mine.size() > 24) mine.clear();
        mine.put(key, now);
        p.displayClientMessage(Component.translatable("nusmp.beastwill.msg." + key, args), !chat);
    }

    public static void forget(UUID player) {
        LAST.remove(player);
        WINDOW_USE.remove(player);
    }
}
