package com.newuniverse.nusmp.client.ui.gs;

import java.util.ArrayDeque;

/**
 * The notices of the alert crest ("[Notice... Great Sage has ...]"), with hard bounds so a spamming server or a long fight can never build a
 * pile of work: at most {@link #VISIBLE} notices on screen, at most {@link #QUEUED} waiting (the oldest waiting one is dropped when the queue is
 * full), at most {@link #PER_SECOND} new notices accepted per second (the rest are refused), and the same key is not accepted again within
 * {@link #DEDUPE} ticks. Time is the client clock in ticks as a float. No Minecraft types; one instance lives on the client thread only.
 */
public final class GsNoticeQueue {
    public static final int VISIBLE = 3, QUEUED = 16, PER_SECOND = 4, MAX_TEXT = 140, RECENT = 16;
    /** Ticks: a second, how long a notice stays, the dedupe window, the slide-in and fade-out. */
    public static final float SECOND = 20f, LIFE = 110f, DEDUPE = 60f, SLIDE_IN = 8f, FADE_OUT = 12f;

    public enum Kind { OK, INFO, WARN, BAD }

    public static final class Notice {
        public final String key, text;
        public final Kind kind;
        /** The clock value when it appeared on screen (set by the queue). */
        public float shownAt;
        /** Renderer cache: the text wrapped to two lines and the translated header, set on the first frame it is drawn (never per frame). */
        public String[] lines;
        public String head;

        Notice(String key, String text, Kind kind) { this.key = key; this.text = text; this.kind = kind; }
    }

    private final Notice[] visible = new Notice[VISIBLE];
    private final ArrayDeque<Notice> queued = new ArrayDeque<>(QUEUED + 1);
    private final float[] accepted = new float[PER_SECOND];
    private int acceptedCount, acceptedHead;
    private final String[] recentKey = new String[RECENT];
    private final float[] recentAt = new float[RECENT];
    private int recentHead;
    private int dropped, refused;

    /**
     * Offers a notice. Returns false when it was refused (blank text, the same key seen within the dedupe window, or the per-second limit);
     * true when it was accepted (shown at once if a slot is free, else queued).
     */
    public boolean offer(String key, String text, Kind kind, float now) {
        if (text == null || text.isBlank()) return false;
        String k = key == null || key.isEmpty() ? text : key;
        for (int i = 0; i < RECENT; i++) {
            if (recentKey[i] != null && recentKey[i].equals(k) && now - recentAt[i] < DEDUPE && now >= recentAt[i]) { refused++; return false; }
        }
        // the per-second limit: the oldest of the last PER_SECOND accepted notices must be a full second old
        if (acceptedCount >= PER_SECOND) {
            float oldest = accepted[acceptedHead];
            if (now - oldest < SECOND && now >= oldest) { refused++; return false; }
        }
        if (acceptedCount < PER_SECOND) {
            accepted[(acceptedHead + acceptedCount) % PER_SECOND] = now;
            acceptedCount++;
        } else {
            accepted[acceptedHead] = now;
            acceptedHead = (acceptedHead + 1) % PER_SECOND;
        }
        recentKey[recentHead] = k;
        recentAt[recentHead] = now;
        recentHead = (recentHead + 1) % RECENT;
        String t = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT - 1) + "…" : text;
        Notice n = new Notice(k, t, kind == null ? Kind.INFO : kind);
        int slot = freeSlot();
        if (slot >= 0 && queued.isEmpty()) {
            n.shownAt = now;
            visible[slot] = n;
        } else {
            if (queued.size() >= QUEUED) { queued.pollFirst(); dropped++; }
            queued.addLast(n);
        }
        return true;
    }

    /** Once per frame (or tick): removes the notices that ran out and moves waiting ones into free slots. */
    public void update(float now) {
        for (int i = 0; i < VISIBLE; i++) {
            Notice n = visible[i];
            if (n != null && (now - n.shownAt >= LIFE || now < n.shownAt)) visible[i] = null;
        }
        int slot;
        while (!queued.isEmpty() && (slot = freeSlot()) >= 0) {
            Notice n = queued.pollFirst();
            n.shownAt = now;
            visible[slot] = n;
        }
    }

    private int freeSlot() {
        for (int i = 0; i < VISIBLE; i++) if (visible[i] == null) return i;
        return -1;
    }

    /** The notice in slot i (0 .. VISIBLE-1) or null. */
    public Notice slot(int i) { return visible[i]; }

    public int visibleCount() {
        int c = 0;
        for (Notice n : visible) if (n != null) c++;
        return c;
    }

    public int queuedCount() { return queued.size(); }

    /** How many waiting notices were dropped because the queue was full, and how many offers were refused (dedupe or rate limit). */
    public int dropped() { return dropped; }

    public int refused() { return refused; }

    public boolean empty() { return visibleCount() == 0 && queued.isEmpty(); }

    public void clear() {
        for (int i = 0; i < VISIBLE; i++) visible[i] = null;
        queued.clear();
        acceptedCount = 0;
        acceptedHead = 0;
        for (int i = 0; i < RECENT; i++) recentKey[i] = null;
        recentHead = 0;
    }

    // ---------------------------------------------------------------- the look of one notice over its life
    /** 0..1: slides out from the crest over the first ticks (eased out). */
    public static float slide(float age) {
        float t = age <= 0f ? 0f : Math.min(1f, age / SLIDE_IN);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** 0..1: fades in with the slide and out over the last ticks. */
    public static float alpha(float age) {
        if (age <= 0f || age >= LIFE) return 0f;
        float in = Math.min(1f, age / (SLIDE_IN * 0.6f));
        float out = Math.min(1f, (LIFE - age) / FADE_OUT);
        return Math.min(in, out);
    }
}
