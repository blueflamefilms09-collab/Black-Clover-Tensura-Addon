package com.newuniverse.nusmp.core;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: a catch that must not break the caller (an event hook, a best-effort read) should still tell the owner once when it hides a real bug,
 * instead of staying empty. Each place passes its own key; the first failure of a key is logged with its stack, later ones are dropped. The set of keys is capped, so a
 * mistake in a caller can never grow it or flood the log.
 */
public final class CatchLog {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int MAX_KEYS = 64;
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private CatchLog() {}

    /** Logs {@code what} failed (with the stack) the first time this key is seen; later calls with the same key do nothing. */
    public static void once(String what, Throwable t) {
        if (SEEN.size() >= MAX_KEYS || !SEEN.add(what)) return;
        LOG.warn("[anime_multiverse] {} failed (only the first failure is logged): {}", what, t.toString(), t);
    }

    /** Logs an information line once per key (same cap as {@link #once}); for a rule that fired and the owner may need to know why. */
    public static void note(String key, String message) {
        if (SEEN.size() >= MAX_KEYS || !SEEN.add(key)) return;
        LOG.info("[anime_multiverse] {} (only the first is logged)", message);
    }
}
