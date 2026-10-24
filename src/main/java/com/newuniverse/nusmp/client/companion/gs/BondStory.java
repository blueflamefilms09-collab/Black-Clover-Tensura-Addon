package com.newuniverse.nusmp.client.companion.gs;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The small "bond story" of the Bond tab: the last few things that happened between this player and one companion (rapport rose, a gift she loved or hated, she joined or left, she used the pouch on you),
 * newest first. A ring buffer of {@link #MAX} events per companion and at most {@link #MAX_COMPANIONS} companions are kept in memory (the least recently used goes first), client only, dropped on logout.
 * An event is a kind, a short already-translated text and the tick (the screen's clock) it happened; no Minecraft type, unit tested.
 */
public final class BondStory {
    /** The kinds, each with its own colour and glyph on the screen. Constants are only ever appended. */
    public enum Kind { RAPPORT, LOVED, HATED, LIKED, JOINED, LEFT, HEAL, FIVE, POUCH, STANCE, NOTE }

    public record Event(Kind kind, String text, long at) {}

    public static final int MAX = 6, MAX_COMPANIONS = 8, MAX_TEXT = 90, MAX_KEY = 80;

    private static final class Ring {
        final Event[] items = new Event[MAX];
        int next, size;

        void add(Event e) {
            items[next] = e;
            next = (next + 1) % MAX;
            if (size < MAX) size++;
        }

        /** 0 = the newest. */
        Event get(int i) { return i < 0 || i >= size ? null : items[((next - 1 - i) % MAX + MAX) % MAX]; }
    }

    private final Map<String, Ring> byCompanion = new LinkedHashMap<>(16, 0.75f, true);

    private static String cut(String s, int n) { return s == null ? "" : s.length() <= n ? s : s.substring(0, n); }

    /** Adds an event to a companion's story. A blank text is ignored; the same text twice in a row is ignored (a double click). */
    public void add(String companion, Kind kind, String text, long at) {
        if (companion == null || companion.isEmpty() || text == null || text.isBlank()) return;
        String c = cut(companion, MAX_KEY);
        Ring r = byCompanion.get(c);
        if (r == null) {
            if (byCompanion.size() >= MAX_COMPANIONS) {
                Iterator<String> it = byCompanion.keySet().iterator();
                it.next();
                it.remove();
            }
            r = new Ring();
            byCompanion.put(c, r);
        }
        String t = cut(text.strip(), MAX_TEXT);
        Event last = r.get(0);
        if (last != null && last.kind() == kind && last.text().equals(t) && at - last.at() < 20) return;
        r.add(new Event(kind == null ? Kind.NOTE : kind, t, at));
    }

    /** How many events a companion has (0..{@link #MAX}). */
    public int size(String companion) {
        Ring r = byCompanion.get(cut(companion, MAX_KEY));
        return r == null ? 0 : r.size;
    }

    /** The i-th newest event of a companion (0 = newest), or null. */
    public Event get(String companion, int i) {
        Ring r = byCompanion.get(cut(companion, MAX_KEY));
        return r == null ? null : r.get(i);
    }

    public int companions() { return byCompanion.size(); }

    public void clear() { byCompanion.clear(); }
}
