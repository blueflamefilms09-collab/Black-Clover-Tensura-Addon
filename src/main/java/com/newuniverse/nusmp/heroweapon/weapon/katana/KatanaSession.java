package com.newuniverse.nusmp.heroweapon.weapon.katana;

/**
 * Why: besides its tempo ({@link TempoChain}) a wielding of the Katana remembers a few things for a short while: the window of a parry or of an evade that is
 * open, the phantom second blade of the Double Sword and how many cuts it has left, the window of a riposte after a parry, and when the wielder last did
 * anything with the katana (a Keen Opening needs four quiet seconds). All of it is "until game tick T" or a small counter, so it is one object per wielder with
 * no game imports and plain tests: a window opens, answers true until its tick and not after, closes when used, and the phantom blade gives its four cuts and
 * no more, never twice within its gap. It lives in memory only.
 */
public final class KatanaSession {
    private static final long LONG_AGO = Long.MIN_VALUE / 2;

    private final TempoChain chain = new TempoChain();
    private long counterUntil, evadeUntil, doubleUntil, riposteUntil;
    private long lastAction = LONG_AGO, lastEcho = LONG_AGO;
    private int echoesLeft;

    public TempoChain chain() { return chain; }

    // ---- parry
    public void openCounter(long now, int ticks) { counterUntil = now + ticks; }

    public boolean counterOpen(long now) { return now < counterUntil; }

    public void closeCounter() { counterUntil = 0; }

    // ---- evade
    public void openEvade(long now, int ticks) { evadeUntil = now + ticks; }

    public boolean evadeOpen(long now) { return now < evadeUntil; }

    public void closeEvade() { evadeUntil = 0; }

    // ---- the phantom second blade
    /** Opens the phantom blade for {@code ticks} with {@code echoes} cuts to give. */
    public void openDouble(long now, int ticks, int echoes) {
        doubleUntil = now + ticks;
        echoesLeft = Math.max(0, echoes);
        lastEcho = LONG_AGO;
    }

    public boolean doubleOpen(long now) { return now < doubleUntil && echoesLeft > 0; }

    /** Takes one phantom cut if the blade is open, has a cut left and the gap since the last one has passed; true when it may strike now. */
    public boolean takeEcho(long now) {
        if (!doubleOpen(now) || now - lastEcho < KatanaNumbers.ECHO_GAP) return false;
        echoesLeft--;
        lastEcho = now;
        return true;
    }

    public int echoesLeft() { return echoesLeft; }

    // ---- riposte
    public void openRiposte(long now, int ticks) { riposteUntil = now + ticks; }

    /** True once during the window: the riposte is used up by asking. */
    public boolean takeRiposte(long now) {
        if (now >= riposteUntil) return false;
        riposteUntil = 0;
        return true;
    }

    // ---- quiet time (Keen Opening)
    /** The wielder did something with the katana at {@code now}. */
    public void acted(long now) { lastAction = now; }

    /** True when the wielder has done nothing with the katana for at least {@code idle} ticks. */
    public boolean quiet(long now, int idle) { return now - lastAction >= idle; }

    public void reset() {
        chain.reset();
        counterUntil = 0;
        evadeUntil = 0;
        doubleUntil = 0;
        riposteUntil = 0;
        echoesLeft = 0;
        lastAction = LONG_AGO;
        lastEcho = LONG_AGO;
    }
}
