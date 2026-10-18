package com.newuniverse.nusmp.skilltree.ft.dragon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: a Dragon Slayer technique is more than one instant (a roar is an inhale and then a breath, Thunder Palace is a warning and then a bolt every half second, the Secret
 * Art is a barrage and then a blast), and the server has no per-spell thread to sleep on: the only honest way to "wait" on a NeoForge server is to remember what to do and
 * the tick it is due, and to look again on the next server tick. This is that memory, kept as a PURE class (no Minecraft or NeoForge type) so the ordering and the bounds
 * are unit-tested without a server: items come out oldest-due first, ties in the order they were added, nothing comes out early, and the queue refuses (returns false)
 * instead of growing without limit or throwing, because a combat ability must never take a server down. {@link DragonTimers} wraps one instance for the game.
 *
 * @param <T> what is waiting (the game wraps a Runnable)
 */
public final class DelayQueue<T> {
    private record Entry<E>(long due, long seq, E item) {}

    private final List<Entry<T>> entries = new ArrayList<>();
    private final int capacity;
    private long nextSeq;

    /** @param capacity the most items that may wait at once (at least 1) */
    public DelayQueue(int capacity) { this.capacity = Math.max(1, capacity); }

    /** Queues {@code item} for the tick {@code dueTick}; false (and nothing queued) when the queue is full or the item is null. */
    public boolean add(long dueTick, T item) {
        if (item == null || entries.size() >= capacity) return false;
        entries.add(new Entry<>(dueTick, nextSeq++, item));
        return true;
    }

    /** Removes and returns every item due at or before {@code now}, the earliest due first (equal due ticks in the order they were added). */
    public List<T> drainDue(long now) {
        List<Entry<T>> due = new ArrayList<>();
        for (Entry<T> e : entries) if (e.due() <= now) due.add(e);
        if (due.isEmpty()) return List.of();
        entries.removeIf(e -> e.due() <= now);
        due.sort(Comparator.comparingLong((Entry<T> e) -> e.due()).thenComparingLong(e -> e.seq()));
        List<T> out = new ArrayList<>(due.size());
        for (Entry<T> e : due) out.add(e.item());
        return out;
    }

    public int size() { return entries.size(); }

    public void clear() { entries.clear(); }
}
