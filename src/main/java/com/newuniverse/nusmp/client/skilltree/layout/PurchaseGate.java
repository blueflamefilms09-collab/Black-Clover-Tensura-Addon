package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: a purchase is a round trip (click, packet, the server's check, the sync packet back), and for those few frames the screen still shows the old level and the old SP. If it
 * accepted a second click in that window, a nervous double click would send two packets and, when the SP allows, buy two levels the player meant to buy one of. The gate lets one
 * request through, then refuses new ones until the level it asked for has appeared in the synced tree or a short timeout has passed; the timeout also tells the screen when the
 * server said no (nothing changed), so it can show a notice instead of waiting forever. Pure and driven by a millisecond clock handed in, so it is tested with a fake clock and has no
 * state that needs resetting between screens: a new screen makes a new gate.
 */
public final class PurchaseGate {
    /** How long to wait for the synced level before treating the request as refused. */
    public static final long TIMEOUT_MS = 800;

    private boolean pending;
    private long sentAt;
    private int expectAbove;

    /** True while a request is in flight (the Buy button shows a wait state). */
    public boolean isPending(long nowMs) { return pending && nowMs - sentAt < TIMEOUT_MS; }

    /**
     * Asks to send a request for a node whose synced level is {@code currentLevel}. Returns true (and starts waiting) when no request is in flight, false when the caller must not
     * send.
     */
    public boolean tryBegin(long nowMs, int currentLevel) {
        if (isPending(nowMs)) return false;
        pending = true;
        sentAt = nowMs;
        expectAbove = currentLevel;
        return true;
    }

    /**
     * Tells the gate the level the synced tree shows now for the node it is waiting on. Returns true exactly once when the request succeeded (the level rose), false while still
     * waiting and when nothing was pending.
     */
    public boolean observe(int level) {
        if (pending && level > expectAbove) {
            pending = false;
            return true;
        }
        return false;
    }

    /** Returns true exactly once when the timeout passed with no change: the server did not accept the request. */
    public boolean refused(long nowMs) {
        if (pending && nowMs - sentAt >= TIMEOUT_MS) {
            pending = false;
            return true;
        }
        return false;
    }

    /** Forgets any request in flight (the player picked another node or route). */
    public void reset() { pending = false; }
}
