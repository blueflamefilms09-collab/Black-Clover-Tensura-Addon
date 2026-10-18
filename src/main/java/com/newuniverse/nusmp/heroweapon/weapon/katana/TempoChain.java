package com.newuniverse.nusmp.heroweapon.weapon.katana;

/**
 * Why: a duel with a katana has a rhythm, and the Vassal Katana makes the rhythm a rule: using DIFFERENT katana techniques one after another, each within four
 * seconds of the last, builds TEMPO (0 to 3). Each level adds damage to the next cuts and thrusts and to the dance of sakura, a parry jumps the tempo to full,
 * and a pause longer than four seconds lets it lapse, so a wielder who mashes one technique gains nothing and one who flows from a cut to a draw to a thrust
 * is rewarded. This is the counter and nothing else, with no game imports, so the rules (growth only on a different technique, lapse, cap, the perfect jump) are
 * tested. The clock is the level's game time. One chain per wielder; it lives in memory only.
 */
public final class TempoChain {
    private int tempo;
    private String lastKey = "";
    private long lastAt = Long.MIN_VALUE / 2;

    /** The tempo the chain holds at {@code now}: its level, or 0 once the window has lapsed. */
    public int level(long now) {
        return now - lastAt > KatanaNumbers.TEMPO_WINDOW ? 0 : tempo;
    }

    /**
     * Records that technique {@code key} was cast at {@code now} and returns the tempo that cast is made with (the level AFTER this cast joined the chain):
     * the first cast of a chain has tempo 0, a different technique within the window raises it by one up to the maximum, the same technique again changes
     * nothing, and a cast after the window starts a fresh chain.
     */
    public int record(String key, long now) {
        if (now - lastAt > KatanaNumbers.TEMPO_WINDOW) tempo = 0;
        else if (!key.equals(lastKey)) tempo = Math.min(KatanaNumbers.TEMPO_MAX, tempo + 1);
        lastKey = key;
        lastAt = now;
        return tempo;
    }

    /** A perfect parry: the tempo jumps to the maximum and the window restarts, so the next technique is cast at full tempo. */
    public void perfect(long now) {
        tempo = KatanaNumbers.TEMPO_MAX;
        lastKey = "parry";
        lastAt = now;
    }

    /** Spends the tempo (the dance of sakura does): back to nothing. */
    public void reset() {
        tempo = 0;
        lastKey = "";
        lastAt = Long.MIN_VALUE / 2;
    }
}
