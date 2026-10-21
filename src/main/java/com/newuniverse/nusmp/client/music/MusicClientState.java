package com.newuniverse.nusmp.client.music;

import com.newuniverse.nusmp.audio.music.MusicState;

/**
 * Why: the last music state the server sent, with the client tick it arrived on, so the engine can tell a fresh answer from a stale one. The server repeats its state every five
 * seconds (a keep-alive), so a state older than {@link #STALE_TICKS} means the server went quiet (a lost packet, a server without the mod after a world change): the engine then
 * treats it as "no themed music" and fades out. Written on the client thread by the packet handler and read on the client thread by the engine; nothing here is shared with the server.
 */
public final class MusicClientState {
    private MusicClientState() {}

    /** Ticks after which a state is no longer believed (20 seconds, four keep-alives). */
    public static final int STALE_TICKS = 400;

    private static MusicState state = MusicState.OFF;
    private static long receivedAt = Long.MIN_VALUE / 2;
    private static boolean pending;

    /** Called by the packet handler (client thread). The arrival tick is stamped by the engine's next tick, so the handler needs no clock. */
    public static void receive(MusicState s) {
        state = s == null ? MusicState.OFF : s;
        pending = true;
    }

    /** The engine calls this once per tick with its own tick counter; returns the state if it is fresh, else {@link MusicState#OFF}. */
    public static MusicState fresh(long now) {
        if (pending) {
            receivedAt = now;
            pending = false;
        }
        return now - receivedAt <= STALE_TICKS ? state : MusicState.OFF;
    }

    public static void clear() {
        state = MusicState.OFF;
        receivedAt = Long.MIN_VALUE / 2;
        pending = false;
    }
}
