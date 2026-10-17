package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemNet;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Why: the overlay is drawn many times a second but the server speaks only when something changes, so the client keeps the last picture it was given in one
 * place: plain static fields, written by the payload handlers on the client thread (they enqueue onto the main thread) and read by the overlay on the same
 * thread, so there is no locking and no copying. It holds the window's facts (floor, road, threat, the quest and its count, the clock, the stat points), the
 * queue of pop-up lines waiting to be shown, and two switches: {@link #broken} (a draw failed once: the window stays hidden until the server sends news, so a
 * bug in the drawing can never spam the log or crash a frame) and {@link #version} (bumped on every sync, so the overlay rebuilds its cached strings only
 * when something changed). It has no Minecraft import on purpose, so it is safe to load anywhere and testable.
 */
public final class ClientSystem {
    private ClientSystem() {}

    /** The most pop-up lines kept waiting. */
    public static final int MAX_TOASTS = 6;

    // ---- the window
    static boolean active;
    static int floor;
    static String polarity = "";
    static int threat;
    static boolean boss;
    static int questState;
    static String kind = "";
    static String titleKey = "";
    static String titleFallback = "";
    static int progress, target;
    static int timeLeft = -1;
    static boolean clockRunning;
    static long timeStamp;
    static final int[] STATS = new int[Stat.COUNT];

    // ---- switches
    /** A draw failed: hide the window until the server sends news. */
    static boolean broken;
    /** Bumped on every sync (the overlay's cached text is rebuilt when it changes). */
    static int version;

    // ---- the pop-up lines
    /** One pop-up line waiting or showing: when it started (game time plus partial tick; NaN until first drawn), what it says (key and arguments) and what kind it is. */
    static final class Toast {
        final ToastKind kind;
        final String key;
        final List<String> args;
        float start = Float.NaN;
        String text;
        List<String> lines;

        Toast(ToastKind kind, String key, List<String> args) {
            this.kind = kind;
            this.key = key;
            this.args = args;
        }
    }

    static final ArrayDeque<Toast> TOASTS = new ArrayDeque<>();

    // ================================================================ from the payloads
    /** The server's picture of the window arrived. {@code gameTime} is the client level's game time now (the clock counts down from it). */
    public static void apply(SystemNet.Sync p, long gameTime) {
        active = p.active();
        floor = p.floor();
        polarity = p.polarity();
        threat = p.threat();
        boss = p.boss();
        questState = p.questState();
        kind = p.kind();
        titleKey = p.titleKey();
        titleFallback = p.titleFallback();
        progress = p.progress();
        target = p.target();
        timeLeft = p.timeLeft();
        clockRunning = p.clockRunning();
        timeStamp = gameTime;
        for (int i = 0; i < Stat.COUNT; i++) STATS[i] = i < p.stats().size() ? p.stats().get(i) : 0;
        broken = false;
        version++;
    }

    /** A pop-up line arrived; the oldest waiting line goes if too many are queued. */
    public static void pushToast(SystemNet.Toast p) {
        while (TOASTS.size() >= MAX_TOASTS) TOASTS.pollFirst();
        TOASTS.addLast(new Toast(ToastKind.fromCode(p.kind()), p.key(), List.copyOf(p.args())));
    }

    /** Ticks left on the clock now, counted down from the last sync while the server says the clock runs; -1 when the quest has no clock. */
    public static int timeLeftNow(long gameTime) {
        if (timeLeft < 0) return -1;
        if (!clockRunning) return timeLeft;
        return (int) Math.max(0L, timeLeft - Math.max(0L, gameTime - timeStamp));
    }

    /** Forgets everything (the player left the world). */
    public static void clear() {
        active = false;
        floor = 0;
        polarity = "";
        threat = 0;
        boss = false;
        questState = 0;
        kind = "";
        titleKey = "";
        titleFallback = "";
        progress = 0;
        target = 0;
        timeLeft = -1;
        clockRunning = false;
        for (int i = 0; i < STATS.length; i++) STATS[i] = 0;
        TOASTS.clear();
        broken = false;
        version++;
    }

    // ================================================================ reading
    public static boolean active() { return active; }

    public static int floor() { return floor; }

    public static int stat(Stat s) { return STATS[s.ordinal()]; }
}
