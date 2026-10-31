package com.newuniverse.nusmp.shishou;

import java.util.Arrays;

/**
 * Why (3.42.0 audit): the hunter used to remember ONE creature it had given up on (a charge that could not reach it), so with two creatures behind walls it ping-ponged: give up A, pick B, give up B (A forgotten),
 * pick A again, forever. This is the memory of a few at once: {@link #SLOTS} entity ids, each with the game time its pause ends. Fixed size (no growth, no allocation after the constructor); when it is full the
 * entry that would end first is replaced. No game classes, so it is tested without a game.
 */
public final class ShishouIgnore {
    public static final int SLOTS = 4;

    private final int[] ids = new int[SLOTS];
    private final long[] until = new long[SLOTS];

    public ShishouIgnore() { clear(); }

    public void clear() {
        Arrays.fill(ids, -1);
        Arrays.fill(until, 0L);
    }

    /** Gives {@code id} up until the game time {@code expires}: the same creature again just extends it; otherwise a free or finished slot, otherwise the entry that ends first. */
    public void add(int id, long expires, long now) {
        int slot = -1, soonest = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (ids[i] == id) {
                slot = i;
                break;
            }
            if (slot < 0 && (ids[i] < 0 || until[i] <= now)) slot = i;
            if (until[i] < until[soonest]) soonest = i;
        }
        if (slot < 0) slot = soonest;
        ids[slot] = id;
        until[slot] = expires;
    }

    /** Is this creature still given up on at game time {@code now}? */
    public boolean ignored(int id, long now) {
        for (int i = 0; i < SLOTS; i++) if (ids[i] == id && now < until[i]) return true;
        return false;
    }

    /** How many entries are still running at {@code now}. */
    public int active(long now) {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) if (ids[i] >= 0 && now < until[i]) n++;
        return n;
    }
}
