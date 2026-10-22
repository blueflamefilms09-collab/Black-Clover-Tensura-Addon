package com.newuniverse.nusmp.client.shishou;

import com.newuniverse.nusmp.shishou.ShishouNet;

/**
 * Why: what the client knows of the sword: the last state packet (level, mana, the selected skill, its cooldown, the window flags) and the last Appraisal answer, each with the time it arrived so the HUD
 * fades them out (the state after three seconds without a packet: he is no longer held; the readout after four seconds). Plain static fields written by the network thread's task on the client thread and
 * read by the HUD; nothing is allocated per frame.
 */
public final class ShishouClientState {
    private ShishouClientState() {}

    public static final long STATE_MS = 3000L, READOUT_MS = 4000L;

    private static ShishouNet.Sync sync;
    private static long syncAt;
    private static ShishouNet.AppraisalReply reply;
    private static long replyAt;

    public static void onSync(ShishouNet.Sync s) {
        sync = s;
        syncAt = System.currentTimeMillis();
    }

    public static void onReply(ShishouNet.AppraisalReply r) {
        reply = r;
        replyAt = System.currentTimeMillis();
    }

    /** The current state, or null when he is not held (no packet for three seconds). */
    public static ShishouNet.Sync state() { return sync != null && System.currentTimeMillis() - syncAt < STATE_MS ? sync : null; }

    /** The current Appraisal readout, or null when there is none (or it is not a real answer). */
    public static ShishouNet.AppraisalReply readout() { return reply != null && reply.kind() != 2 && System.currentTimeMillis() - replyAt < READOUT_MS ? reply : null; }

    public static void clear() {
        sync = null;
        reply = null;
    }
}
