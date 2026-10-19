package com.newuniverse.nusmp.labyrinth.mega.lock;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: every line the escape rules say is a TRANSLATABLE component with the English text as its fallback, so each player reads it in their own language and it still reads
 * properly before the lang fragment is merged or when a key is missing. {@code Component.translatableWithFallback} does both when it is given the English; this looks that text
 * up in {@link LockText}, the one map the lang fragment is written from, so no call site repeats an English string.
 */
final class LockMsg {
    private LockMsg() {}

    static MutableComponent text(String key, Object... args) {
        return Component.translatableWithFallback(key, LockText.english(key), args);
    }

    /** Sends a chat line to a real player only: a fake player of a self-test has no connection, and sending to it would throw. */
    static void tell(net.minecraft.server.level.ServerPlayer p, Component c) {
        if (p != null && com.newuniverse.nusmp.labyrinth.mega.system.SystemSync.hasConnection(p)) p.sendSystemMessage(c);
    }
}
