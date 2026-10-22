package com.newuniverse.nusmp.shishou;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: several skills are windows of time per wielder (the Multilayer Barrier while it is on, Magic Sense's ten seconds, Stealth, the released Divine Sword, the Spatial Motion invulnerability frames). One
 * pure table keeps them: per wielder UUID and kind, when the window ends and when its next refresh is due. {@link #anyActive} is an O(1) answer for the event handlers that must cost nothing for everybody
 * else (a projectile hit, an incoming damage, a target change): when no window of that kind is open they return at once. {@link #sweep} closes the ended windows (called once a second, only while
 * something is open). Server thread only.
 */
public final class ShishouWindows {
    public enum Kind { BARRIER, SENSE, STEALTH, RELEASE, IFRAMES, VEIL }

    private static final int KINDS = Kind.values().length;

    private final Map<UUID, long[]> end = new HashMap<>(), next = new HashMap<>(), deflect = new HashMap<>();
    private final int[] open = new int[KINDS];

    private long[] row(Map<UUID, long[]> m, UUID who) { return m.computeIfAbsent(who, k -> new long[KINDS]); }

    /** Opens (or extends) a window of {@code ticks} from {@code now}; the first refresh is due at once. */
    public void start(UUID who, Kind k, long now, int ticks) {
        long[] e = row(end, who);
        if (e[k.ordinal()] == 0L) open[k.ordinal()]++;                       // a non-zero end is already counted (an ended window is zeroed by the sweep)
        e[k.ordinal()] = Math.max(e[k.ordinal()] > now ? e[k.ordinal()] : 0L, now + Math.max(1, ticks));
        row(next, who)[k.ordinal()] = now;
    }

    public void stop(UUID who, Kind k) {
        long[] e = end.get(who);
        if (e != null && e[k.ordinal()] > 0L) {
            open[k.ordinal()] = Math.max(0, open[k.ordinal()] - 1);
            e[k.ordinal()] = 0L;
        }
    }

    public boolean active(UUID who, Kind k, long now) {
        long[] e = end.get(who);
        return e != null && e[k.ordinal()] > now;
    }

    /** The ticks left of a window, 0 when it is closed. */
    public int left(UUID who, Kind k, long now) {
        long[] e = end.get(who);
        return e == null ? 0 : (int) Math.max(0L, e[k.ordinal()] - now);
    }

    /** Is any wielder's window of this kind open (a count kept by start, stop and sweep)? */
    public boolean anyActive(Kind k) { return open[k.ordinal()] > 0; }

    /** True once per {@code period} ticks while the window is open (the refresh scan of Magic Sense, the absorption refresh of the barrier). */
    public boolean dueRefresh(UUID who, Kind k, long now, int period) {
        if (!active(who, k, now)) return false;
        long[] n = row(next, who);
        if (now < n[k.ordinal()]) return false;
        n[k.ordinal()] = now + Math.max(1, period);
        return true;
    }

    /** May one more deflection happen in this second (the barrier turns at most {@link ShishouRules#BARRIER_DEFLECT_PER_SECOND} a second)? Counts it when yes. */
    public boolean deflectAllowed(UUID who, long now, int perSecond) {
        long[] d = row(deflect, who);
        if (now - d[0] >= 20L) {
            d[0] = now;
            d[1] = 0L;
        }
        if (d[1] >= perSecond) return false;
        d[1]++;
        return true;
    }

    /** Closes the windows that ended; returns how many were closed. */
    public int sweep(long now) {
        int closed = 0;
        for (long[] e : end.values())
            for (int k = 0; k < KINDS; k++)
                if (e[k] > 0L && e[k] <= now) {
                    e[k] = 0L;
                    open[k] = Math.max(0, open[k] - 1);
                    closed++;
                }
        return closed;
    }

    public boolean anyOpen() {
        for (int v : open) if (v > 0) return true;
        return false;
    }

    /** A wielder left, died or changed dimension: every window of theirs closes. */
    public void forget(UUID who) {
        long[] e = end.remove(who);
        if (e != null) for (int k = 0; k < KINDS; k++) if (e[k] > 0L) open[k] = Math.max(0, open[k] - 1);
        next.remove(who);
        deflect.remove(who);
    }

    public void clear() {
        end.clear();
        next.clear();
        deflect.clear();
        java.util.Arrays.fill(open, 0);
    }

    public int tracked() { return end.size(); }
}
