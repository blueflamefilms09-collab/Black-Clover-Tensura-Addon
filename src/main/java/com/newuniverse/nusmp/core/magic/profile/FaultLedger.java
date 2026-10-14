package com.newuniverse.nusmp.core.magic.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Pure bookkeeping behind {@link SafeCast}: which magic faulted, how often, and until when it is on its recovery cooldown. The first
 * fault of an id is reported to the log sink (once per id, with the stack trace); later faults only count. The cooldown starts at
 * {@code baseTicks} and doubles with every further fault up to {@code maxTicks}, so a magic that keeps failing backs off instead of
 * throwing every tick. Not thread safe on purpose: casting runs on the server thread only (the client guard keeps its own instance).
 */
public final class FaultLedger {
    private record State(int count, long until) {}

    private final Map<String, State> states = new HashMap<>();
    private final Consumer<String> log;
    private final int baseTicks, maxTicks;

    public FaultLedger(int baseTicks, int maxTicks, Consumer<String> log) {
        this.baseTicks = Math.max(1, baseTicks);
        this.maxTicks = Math.max(this.baseTicks, maxTicks);
        this.log = log == null ? s -> {} : log;
    }

    /** True while the id is on its recovery cooldown at game time {@code now}. */
    public boolean blocked(String id, long now) {
        State s = states.get(MagicKeys.canonical(id));
        return s != null && now < s.until();
    }

    /** Ticks left on the cooldown (0 = none). */
    public int ticksLeft(String id, long now) {
        State s = states.get(MagicKeys.canonical(id));
        return s == null ? 0 : (int) Math.max(0, s.until() - now);
    }

    /**
     * Records a fault. Logs on the id's first fault only. Returns the cooldown ticks now applied.
     * @param what a short description of where it happened (a page id, a layer name), part of the one log line
     */
    public int record(String id, long now, String what, Throwable t) {
        String key = MagicKeys.canonical(id);
        State old = states.get(key);
        int n = old == null ? 1 : old.count() + 1;
        int cd = (int) Math.min((long) maxTicks, (long) baseTicks << Math.min(n - 1, 10));
        states.put(key, new State(n, now + cd));
        if (n == 1) log.accept("magic '" + key + "' faulted in " + what + ": " + t + " (it is on a " + cd + "-tick cooldown and falls back to the generic effect; further faults of this magic are counted, not logged)" + trace(t));
        return cd;
    }

    private static String trace(Throwable t) {
        if (t == null) return "";
        StackTraceElement[] st = t.getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(6, st.length); i++) sb.append("\n    at ").append(st[i]);
        return sb.toString();
    }

    public int count(String id) {
        State s = states.get(MagicKeys.canonical(id));
        return s == null ? 0 : s.count();
    }

    /** Forgets one id (used when a magic is fixed by a reload / removed). */
    public void clear(String id) { states.remove(MagicKeys.canonical(id)); }

    public void clearAll() { states.clear(); }

    /** Every id that has faulted, sorted. */
    public Set<String> faulted() { return Collections.unmodifiableSet(new TreeSet<>(states.keySet())); }
}
