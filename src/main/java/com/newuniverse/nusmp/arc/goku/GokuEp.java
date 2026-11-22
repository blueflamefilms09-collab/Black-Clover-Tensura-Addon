package com.newuniverse.nusmp.arc.goku;

/**
 * Why (4.102.0): the owner asked that Goku carry 1 trillion Existence Points when the fight starts and 500 trillion at his strongest. EP is the Tensura number for how strong a being is, and Goku's own was
 * never set; his scaling reads the PLAYERS' EP only. This is the pure schedule: his EP in a form is {@code start} times {@code (end / start)} to the power of (step / top), so it is exactly {@code start}
 * in Base and exactly {@code end} in the last form, and every form multiplies it by the same factor (the same way the series grows his power, form on form). No game type; {@link GokuBoss} writes
 * the number into Tensura once per form change (never per tick) and the setting {@code goku_boss.epLadder} switches it off.
 */
public final class GokuEp {
    private GokuEp() {}

    /** The EP in step {@code idx} of a ladder whose top step is {@code top}. Never below 1, never above {@code max(start, end)}; a bad number reads as 1 trillion to 500 trillion. */
    public static double ep(int idx, int top, double start, double end) {
        double s = start >= 1.0 && Double.isFinite(start) ? start : GokuBossConfig.DEFAULT_EP_START;
        double e = end >= 1.0 && Double.isFinite(end) ? end : GokuBossConfig.DEFAULT_EP_END;
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
        double s = start >= 1.0 && Double.isFinite(start) ? start : GokuBossConfig.DEFAULT_EP_START;
        double e = end >= 1.0 && Double.isFinite(end) ? end : GokuBossConfig.DEFAULT_EP_END;
        double b = base >= s && Double.isFinite(base) ? Math.min(base, 1.0e18) : s;
        return Math.max(ep(idx, top, s, e), ep(idx, top, b, Math.min(1.0e18, b * (e / s))));
    }
}
