package com.newuniverse.nusmp.avatar;

/**
 * Why: a creation is two steps (roll, then finalize) and the client must never be able to choose the scores, so the roll is remembered on the server, on the
 * player, under a random token; finalize only accepts a request that presents the token of the roll the server made. It is an unserialised attachment on
 * purpose: a pending roll is thrown away at logout or restart, which is the safe way to lose it.
 */
public final class AvatarSession {
    public long token;
    public int[] stats = new int[0];
    public boolean fiveDice;
    public double luck;
    public int rerolls;
    public boolean active;

    public void clear() {
        token = 0L;
        stats = new int[0];
        fiveDice = false;
        luck = 0.0;
        rerolls = 0;
        active = false;
    }
}
