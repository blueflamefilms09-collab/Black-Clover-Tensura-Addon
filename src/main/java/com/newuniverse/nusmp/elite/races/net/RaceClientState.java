package com.newuniverse.nusmp.elite.races.net;

/**
 * Why: the client's copy of "which form am I in and for how long", a plain static holder in a COMMON package so the payload handler (which also exists on a dedicated server's
 * class path) never references a client class. The HUD layer reads it; the end time is stored as a client game-time tick so the HUD counts down smoothly between the server's
 * refreshes.
 */
public final class RaceClientState {
    private RaceClientState() {}

    private static volatile String state = "base";
    private static volatile int color = 0xFFFFFFFF;
    private static volatile long endsAt;
    private static volatile boolean timed;

    /** Called on the client thread with the payload and the client's current game time. */
    public static void set(String stateId, int remainingTicks, int argb, long clientNow) {
        state = stateId == null ? "base" : stateId;
        color = argb;
        timed = remainingTicks > 0;
        endsAt = clientNow + Math.max(0, remainingTicks);
    }

    public static String state() { return state; }

    public static int color() { return color; }

    public static boolean active() { return !"base".equals(state); }

    /** Ticks left for the HUD; -1 for an unlimited form. */
    public static long remaining(long clientNow) { return timed ? Math.max(0L, endsAt - clientNow) : -1L; }

    public static void clear() {
        state = "base";
        timed = false;
        endsAt = 0L;
    }
}
