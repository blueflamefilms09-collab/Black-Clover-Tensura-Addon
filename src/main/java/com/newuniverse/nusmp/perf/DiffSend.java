package com.newuniverse.nusmp.perf;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: "send on change, plus a slow refresh" is the right shape for any state a client mirrors (a HUD value, a flag, a list): a packet per tick of a value that did not move is wasted
 * bandwidth and wasted server time, but a client that never hears again can drift (a lost packet, a late join), so an unchanged value is still repeated now and then. This class is only the
 * decision (pure, no game classes): the caller keeps one instance per kind of message, asks {@link #shouldSend} with a key (a player or entity id) and a signature of the state (a hash or
 * the value itself), and sends exactly when it answers true. The first look at a key always sends. Single-threaded (the server thread).
 */
public final class DiffSend {
    private static final class Last {
        long signature;
        long sentAt;
    }

    private final Map<Long, Last> last = new HashMap<>();
    private long sent, skipped;

    /**
     * Should the state of {@code key} be sent in tick {@code now}? True when it was never sent, when its {@code signature} differs from the last one sent, or when {@code refreshTicks}
     * (above 0) have passed since it was last sent. A true answer is recorded: the caller must then send.
     */
    public boolean shouldSend(long key, long signature, long now, int refreshTicks) {
        Last l = last.get(key);
        if (l == null) {
            l = new Last();
            last.put(key, l);
        } else if (l.signature == signature && !(refreshTicks > 0 && now - l.sentAt >= refreshTicks) && now >= l.sentAt) {
            skipped++;
            return false;
        }
        l.signature = signature;
        l.sentAt = now;
        sent++;
        return true;
    }

    /** Forget a key (the player left, the entity is gone): the next look sends again. */
    public void forget(long key) { last.remove(key); }

    public void clear() { last.clear(); }

    /** How many keys are remembered. */
    public int size() { return last.size(); }

    /** How many sends were allowed and how many were skipped since creation (for the report). */
    public long sent() { return sent; }

    public long skipped() { return skipped; }

    /** A signature for a few ints (order matters); a cheap way to fold several fields into the {@code signature} argument. */
    public static long sign(int a, int b, int c, int d) {
        long h = 1125899906842597L;
        h = 31 * h + a;
        h = 31 * h + b;
        h = 31 * h + c;
        h = 31 * h + d;
        return h;
    }

    /** A signature for a double, exact (the bits), so a change of any size is a change. */
    public static long sign(double v) { return Double.doubleToLongBits(v); }
}
