package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: the overlay must follow the quest as it changes (a kill, a room cleared, a stat gained), and a busy fight changes it many times a second; one packet per
 * change would be a flood for a few bytes of news. The brief caps it at one sync per ten ticks. The rule is the usual "dirty flag plus a minimum gap": a change
 * marks the player dirty, and the sync goes out at the first tick at least the gap after the previous one, carrying everything that changed meanwhile. The
 * first change after a quiet spell is sent at once (the previous send is long ago). One instance per player, pure.
 */
public final class SyncThrottle {
    private final int gap;
    private boolean dirty;
    private long lastSent = Long.MIN_VALUE / 2;

    public SyncThrottle(int gapTicks) { this.gap = Math.max(1, gapTicks); }

    /** Something changed: a sync is owed. */
    public void markDirty() { dirty = true; }

    public boolean isDirty() { return dirty; }

    /** Is a sync owed AND has the gap passed? */
    public boolean due(long now) { return dirty && now - lastSent >= gap; }

    /** A sync went out at {@code now}. */
    public void sent(long now) {
        dirty = false;
        lastSent = now;
    }
}
