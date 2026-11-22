package com.newuniverse.nusmp.arc.goku;

/**
 * Why (4.102.0): the owner asked that Goku carry 1 trillion Existence Points when the fight starts and 500 trillion at his strongest. EP is the Tensura number for how strong a being is, and Goku's own was
 * never set; his scaling reads the PLAYERS' EP only. This is the pure schedule: his EP in a form is {@code start} times {@code (end / start)} to the power of (step / top), so it is exactly {@code start}
 * in Base and exactly {@code end} in the last form, and every form multiplies it by the same factor (the same way the series grows his power, form on form). No game type; {@link GokuBoss} writes
 * the number into Tensura once per form change (never per tick) and the setting {@code goku_boss.epLadder} switches it off.
 */
public final class GokuEp {
    private GokuEp() {}

    /** The 4.102.0 numbers (1 trillion to 500 trillion): what a bad reading falls back to in the classic ladder, kept so the old behaviour stays reachable. */
    static final double CLASSIC_START = 1.0e12, CLASSIC_END = 5.0e14;
    /** 4.104.0: the gentle ladder's own fallbacks (1 billion to 10 billion) and its hard ceiling (100 billion). */
    static final double GENTLE_START = 1.0e9, GENTLE_END = 1.0e10, DEFAULT_CEILING = 1.0e11, DEFAULT_CLIMB = 10.0;

    /** The EP in step {@code idx} of a ladder whose top step is {@code top}. Never below 1, never above {@code max(start, end)}; a bad number reads as 1 trillion to 500 trillion. */
    public static double ep(int idx, int top, double start, double end) {
        double s = start >= 1.0 && Double.isFinite(start) ? start : CLASSIC_START;
        double e = end >= 1.0 && Double.isFinite(end) ? end : CLASSIC_END;
        double t = top <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, idx / (double) top));
        double v = s * Math.pow(e / s, t);
        return Math.max(1.0, Math.min(Math.max(s, e), v));
    }

    /** His EP in a form once the players are counted: the ladder number, raised to {@code share} of the strongest player's EP when that is more. A bad share or reading changes nothing. */
    public static double withPlayers(double ladder, double topPlayerEp, double share) {
        if (!(share > 0.0) || !Double.isFinite(share) || !(topPlayerEp > 0.0) || !Double.isFinite(topPlayerEp)) return ladder;
        return Math.max(ladder, topPlayerEp * share);
    }

    /** The same ladder lifted to a new Base: {@code base} in step 0 and {@code base x (end/start)} in the top step (the same climb, from where the players are). Never below the plain ladder. */
    public static double lifted(int idx, int top, double start, double end, double base) {
        double s = start >= 1.0 && Double.isFinite(start) ? start : CLASSIC_START;
        double e = end >= 1.0 && Double.isFinite(end) ? end : CLASSIC_END;
        double b = base >= s && Double.isFinite(base) ? Math.min(base, 1.0e18) : s;
        return Math.max(ep(idx, top, s, e), ep(idx, top, b, Math.min(1.0e18, b * (e / s))));
    }

    /**
     * 4.104.0 (the owner found 1 to 500 trillion far too high for servers that keep Tensura's EP scaling on): the same ladder, tamed. The climb from Base to the last form is held to
     * {@code maxClimb} (never below 1) whatever {@code start}/{@code end} say, and the whole ladder is slid down, never squashed, so its last form is never above {@code ceiling}. {@code base}
     * is where the players put Base (smart spawn); it only raises Base, and only up to {@code ceiling / climb}. Old configs that still say 1 trillion / 500 trillion land on a ceiling-high
     * last form with a 10x climb instead. Bad numbers read as the gentle defaults.
     */
    public static double tamed(int idx, int top, double start, double end, double base, double ceiling, double maxClimb) {
        double s = start >= 1.0 && Double.isFinite(start) ? start : GENTLE_START;
        double e = end >= 1.0 && Double.isFinite(end) ? end : GENTLE_END;
        double c = ceiling >= 1.0 && Double.isFinite(ceiling) ? ceiling : DEFAULT_CEILING;
        double climb = maxClimb >= 1.0 && Double.isFinite(maxClimb) ? maxClimb : DEFAULT_CLIMB;
        double ratio = Math.max(1.0, Math.min(e / s, climb));
        double b = base >= s && Double.isFinite(base) ? base : s;
        b = Math.max(1.0, Math.min(b, c / ratio));
        double t = top <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, idx / (double) top));
        return Math.min(c, b * Math.pow(ratio, t));
    }

    /** {@link #withPlayers} for the tamed ladder: the same one-hundredth floor, but the result never goes above {@code ceiling} (a bad ceiling reads as the default). */
    public static double withPlayersCapped(double ladder, double topPlayerEp, double share, double ceiling) {
        double c = ceiling >= 1.0 && Double.isFinite(ceiling) ? ceiling : DEFAULT_CEILING;
        return Math.min(c, withPlayers(ladder, topPlayerEp, share));
    }
}
