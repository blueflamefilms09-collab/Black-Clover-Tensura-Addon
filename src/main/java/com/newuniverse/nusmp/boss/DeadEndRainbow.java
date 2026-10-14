package com.newuniverse.nusmp.boss;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The hit counter of Dead End Rainbow (pure, unit tested): each landed blow of the boss stacks a mark on its victim; the seventh
 * mark inside the window closes the rainbow. A victim that has just been struck by it is safe for a cooldown, so the effect can never
 * chain. Marks are at least {@code gap} ticks apart, so a flurry from one move is a single mark. The glue turns the closing into a telegraphed, non-lethal drop of the victim's spiritual health (never of its hit points).
 */
public final class DeadEndRainbow<K> {
    /** The result of one blow: the stack after it (0 while the victim is safe) and whether the rainbow closes now. */
    public record Outcome(int count, boolean closes) {}

    private final int hits, window, cooldown, tracked, gap;
    private final Map<K, ArrayDeque<Long>> stacks = new HashMap<>();
    private final Map<K, Long> safeUntil = new HashMap<>();

    public DeadEndRainbow() { this(LuciusNumbers.RAINBOW_HITS, LuciusNumbers.RAINBOW_WINDOW_TICKS, LuciusNumbers.RAINBOW_COOLDOWN_TICKS, LuciusNumbers.RAINBOW_TRACKED, LuciusNumbers.RAINBOW_GAP_TICKS); }

    public DeadEndRainbow(int hits, int window, int cooldown, int tracked, int gap) {
        this.gap = Math.max(0, gap);
        this.hits = Math.max(2, hits);
        this.window = Math.max(1, window);
        this.cooldown = Math.max(0, cooldown);
        this.tracked = Math.max(1, tracked);
    }

    /** A blow landed on 'victim' at game time 'now'. */
    public Outcome hit(K victim, long now) {
        Long safe = safeUntil.get(victim);
        if (safe != null && now < safe) return new Outcome(0, false);
        if (stacks.size() >= tracked && !stacks.containsKey(victim)) prune(now);
        if (stacks.size() >= tracked && !stacks.containsKey(victim)) return new Outcome(0, false);      // the book is full of live stacks: this victim waits
        ArrayDeque<Long> s = stacks.computeIfAbsent(victim, k -> new ArrayDeque<>());
        while (!s.isEmpty() && now - s.peekFirst() > window) s.pollFirst();
        if (!s.isEmpty() && now - s.peekLast() < gap) return new Outcome(s.size(), false);          // the same blow, or a flurry of one move: one mark
        s.addLast(now);
        if (s.size() >= hits) {
            stacks.remove(victim);
            safeUntil.put(victim, now + cooldown);
            return new Outcome(hits, true);
        }
        return new Outcome(s.size(), false);
    }

    /** The stack on a victim now (marks inside the window). */
    public int count(K victim, long now) {
        ArrayDeque<Long> s = stacks.get(victim);
        if (s == null) return 0;
        int n = 0;
        for (long t : s) if (now - t <= window) n++;
        return n;
    }

    public boolean safe(K victim, long now) { return safeUntil.getOrDefault(victim, 0L) > now; }

    public void forget(K victim) {
        stacks.remove(victim);
        safeUntil.remove(victim);
    }

    public void clear() {
        stacks.clear();
        safeUntil.clear();
    }

    public int size() { return stacks.size(); }

    private void prune(long now) {
        for (Iterator<Map.Entry<K, ArrayDeque<Long>>> it = stacks.entrySet().iterator(); it.hasNext(); ) {
            ArrayDeque<Long> s = it.next().getValue();
            if (s.isEmpty() || now - s.peekLast() > window) it.remove();
        }
        safeUntil.values().removeIf(t -> t <= now);
    }
}
