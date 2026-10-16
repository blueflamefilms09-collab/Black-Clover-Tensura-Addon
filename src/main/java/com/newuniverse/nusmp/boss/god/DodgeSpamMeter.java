package com.newuniverse.nusmp.boss.god;

/**
 * Why: a player who chains dodges, saves and invulnerability frames can make a boss's whole kit miss. The owner's answer is the Time
 * Stasis Trap: it is laid under whoever spams them. "Spam" has to be one precise, tested definition: {@value #THRESHOLD} or more dodge
 * events within {@value #WINDOW} ticks (6 seconds). One meter per player; after it fires it is emptied, so the next trap needs three
 * fresh dodges. Pure, no Minecraft types: the mod's dodge systems report through the tiny public {@code DodgeEvents.record(player)}.
 */
public final class DodgeSpamMeter {
    public static final int THRESHOLD = 3, WINDOW = 120, KEEP = 8;

    private final long[] at = new long[KEEP];
    private int n;

    /** Notes a dodge at tick 'now'. Returns true when this one makes three within the window (the meter is then emptied). */
    public boolean record(long now) {
        int w = 0;
        for (int i = 0; i < n; i++) if (now - at[i] <= WINDOW && now >= at[i]) at[w++] = at[i];
        n = w;
        if (n == KEEP) {
            System.arraycopy(at, 1, at, 0, KEEP - 1);
            n--;
        }
        at[n++] = now;
        if (n >= THRESHOLD) {
            n = 0;
            return true;
        }
        return false;
    }

    /** Dodges currently counted inside the window ending at 'now'. */
    public int count(long now) {
        int c = 0;
        for (int i = 0; i < n; i++) if (now - at[i] <= WINDOW && now >= at[i]) c++;
        return c;
    }

    public void clear() { n = 0; }
}
