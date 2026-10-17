package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the guard systems run inside the server's own loops (a block entity's ticker, an entity's {@code aiStep}, an item entity's update, a
 * right click), where one uncaught exception does not just break the feature, it crashes the tick or kicks the player. A boss fight is
 * the worst place for that, so every such entry point catches {@code RuntimeException} and reports it here instead: the first three
 * failures of each place are logged with their stack (enough to find the bug in a log), the rest are counted and stay quiet so a bug that
 * fires every tick cannot flood the log. It never hides a failure from the CI self-tests: while one runs, {@link #strict} is set and a
 * reported failure is thrown again, so the test fails with the exception's own message instead of passing over a swallowed error.
 */
final class GuardLog {
    private GuardLog() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Integer> SEEN = new ConcurrentHashMap<>();

    /** Set by {@link GuardSelfTests} for the length of a test: failures are rethrown instead of logged. */
    static volatile boolean strict;

    /** Reports a failure at {@code where} (a short name such as {@code altar tick}). */
    static void failed(String where, RuntimeException e) {
        if (strict) throw e;
        int n = SEEN.merge(where, 1, Integer::sum);
        if (n <= 3) LOG.warn("[nusmp] the guard systems failed in {} (occurrence {}): {}", where, n, e.toString(), e);
    }
}
