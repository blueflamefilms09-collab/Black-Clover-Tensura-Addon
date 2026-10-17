package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: the party has two ways to stop a Word mid-sentence (an anti-magic blow cuts it, an emptied ward unwrites him) and a boss that can be silenced
 * again the moment he recovers is a boss that is stun-locked, which the owner forbids ("does not stun-lock"). This tiny pure state machine is the rule:
 * an interruption silences him for a time, and the interruption is then locked out for a longer time, so two cuts can never chain and every cut buys the
 * party one clear window and no more. His hard immunity to paralysis, sleep and the like is the framework's own ({@code ZogratisBoss.canBeAffected});
 * this covers the one kind of control the fight itself hands the players.
 */
public final class ZagredComposure {
    /** Ticks he cannot speak after a cut (three seconds). */
    public static final int CUT_SILENCE = 60;
    /** Ticks after a silence ends during which he cannot be interrupted again (five seconds). */
    public static final int LOCKOUT = 100;

    private long silencedUntil, lockUntil;

    /**
     * An anti-magic blow cuts the Word being spoken. Returns false (and changes nothing) while a recent interruption still locks him out.
     */
    public boolean cut(long now) {
        if (now < lockUntil) return false;
        silence(now, CUT_SILENCE);
        return true;
    }

    /** An emptied ward unwrites him: he is silenced for {@code ticks} whether or not he was locked out (the party earned it), and locked out after. */
    public void unwrite(long now, int ticks) { silence(now, ticks); }

    private void silence(long now, int ticks) {
        silencedUntil = Math.max(silencedUntil, now + ticks);
        lockUntil = Math.max(lockUntil, silencedUntil + LOCKOUT);
    }

    /** Is he unable to start a Word? */
    public boolean silenced(long now) { return now < silencedUntil; }

    /** Is he currently safe from being interrupted again? */
    public boolean locked(long now) { return now < lockUntil; }

    /** Ticks of silence left. */
    public long silenceLeft(long now) { return Math.max(0, silencedUntil - now); }

    public void reset() {
        silencedUntil = 0;
        lockUntil = 0;
    }
}
