package com.newuniverse.nusmp.arc.danmachi;

/**
 * Why: the Amphisbaena has two heads and the story's fight is decided by cutting the right one first: the red head breathes a mist that dissipates magic
 * (it even undid Haruhime's boost), and it was a physical blow, strengthened by Mikoto's gravity, that took it off, after which magic worked again and the
 * other head fell. The game cannot aim at a head, so the rule is built from what the players CAN do: when the red head rears up to breathe (the wind-up of
 * the Red Mist Breath, plus a little after), the blows that land on the boss wear the head down, and enough of them cut it off. Physical blows count in
 * full, anti-magic blows count double (they unravel the mist), and spells count for nothing (the mist eats them). A head that is not cut when the window
 * closes forgets the wear. A boss that reaches {@value #FORCE_CUT_POWER}% power loses the head anyway ({@link #forceCut}): the fight must never depend on a mechanic being found.
 * Pure, so every rule is tested; the boss class feeds it the blows.
 */
public final class HeadCut {
    /** How a blow is counted against the head. */
    public enum Kind { PHYSICAL, MAGIC, ANTI }

    /** The share of the boss's maximum health, in counted damage, that cuts the head. */
    public static final double CUT_FRACTION = 0.07;
    /** Ticks the head stays reared after the wind-up of the mist ends. */
    public static final int LINGER_TICKS = 40;
    /** The power (percent) at which the fight tears the head off by itself: the Twin Calamity (60) still breathes the mist once, the Gates never do. */
    public static final int FORCE_CUT_POWER = 80;

    /** Does the fight itself take the head at this power? */
    public static boolean mustCut(int power) { return power >= FORCE_CUT_POWER; }

    private boolean cut;
    private long windowUntil;
    private double worn;

    public boolean cut() { return cut; }

    public boolean windowOpen(long now) { return !cut && now < windowUntil; }

    /** The counted damage the open window has seen so far. */
    public double worn() { return worn; }

    /** The head rears up for {@code ticks} ticks (the wind-up and the linger after it): the wear starts from nothing. */
    public void open(long now, int ticks) {
        if (cut) return;
        windowUntil = now + Math.max(1, ticks);
        worn = 0;
    }

    /** How much a blow counts: physical 1, anti-magic 2, magic 0. */
    public static double weight(Kind kind) {
        return switch (kind == null ? Kind.PHYSICAL : kind) {
            case PHYSICAL -> 1.0;
            case ANTI -> 2.0;
            case MAGIC -> 0.0;
        };
    }

    /** A blow of {@code amount} landed. Returns true when it cut the head. */
    public boolean onHit(long now, Kind kind, float amount, float maxHealth) {
        if (!windowOpen(now) || amount <= 0f || maxHealth <= 0f) return false;
        worn += weight(kind) * amount;
        if (worn < CUT_FRACTION * maxHealth) return false;
        cut = true;
        worn = 0;
        return true;
    }

    /** The head is torn off by the fight itself (the boss's power rose to {@link #FORCE_CUT_POWER}). Returns true if it was still on. */
    public boolean forceCut() {
        if (cut) return false;
        cut = true;
        worn = 0;
        windowUntil = 0;
        return true;
    }

    /** Puts the head back (a new fight). */
    public void reset() {
        cut = false;
        windowUntil = 0;
        worn = 0;
    }
}
