package com.newuniverse.nusmp.balance;

/**
 * Why: the owner asked to scale the mod's damage down (docs/damage_scale_down.md). One pure class holds every number and rule, so the game side (DamageScaleEvents, BalanceLaw.cap, the Djinn
 * Extreme Magic) only asks it and it is unit-tested without Minecraft.
 * <ul>
 *   <li>{@link #ABSOLUTE_MAX} 15,000: the hard maximum of one hit anywhere in the mod, a constant no setting can lift (the apex weapon's own ceiling is clamped to it).</li>
 *   <li>global ceiling 10,000 for every hit that comes from the mod (default of {@code damage.globalHitCap}).</li>
 *   <li>the Djinn band: the weakest Djinn's weapon hit is 4,000, the strongest's 8,000, the others on a straight line by their rank ({@link #djinnHit}); the same number is the per-hit
 *       ceiling of that Djinn's Extreme Magic.</li>
 * </ul>
 * Everything is a multiply or a compare per damage event: no allocation, no state.
 */
public final class DamageScale {
    private DamageScale() {}

    /** The hard maximum of one hit anywhere in the mod, whatever the settings say. */
    public static final double ABSOLUTE_MAX = 15_000.0;
    public static final double DEFAULT_GLOBAL = 10_000.0, DEFAULT_WEAPON = 10_000.0, DEFAULT_APEX = 15_000.0, DEFAULT_DJINN_MIN = 4_000.0, DEFAULT_DJINN_MAX = 8_000.0;
    /** The apex weapon: the one weapon that may hit above the global ceiling (the owner's "Escadosa", read as Elsdocia). */
    public static final String DEFAULT_APEX_ID = "anime_multiverse:elsdocia";

    /** What kind of hit it is, for the ceiling. Entries are only appended. */
    public enum Kind { GENERAL, WEAPON, DJINN_WEAPON, EXTREME, APEX, DJINN_BEARER }

    /** The settings, read once per event by the game side. {@code enabled = false} is the old damage: nothing is limited. */
    public record Limits(boolean enabled, double global, double weapon, double djinnMin, double djinnMax, double apex, boolean djinnFloor) {
        public static Limits defaults() { return new Limits(true, DEFAULT_GLOBAL, DEFAULT_WEAPON, DEFAULT_DJINN_MIN, DEFAULT_DJINN_MAX, DEFAULT_APEX, true); }
    }

    private static double sane(double v, double fallback) { return Double.isFinite(v) && v > 0 ? Math.min(v, ABSOLUTE_MAX) : fallback; }

    /**
     * The per-hit number of a Djinn of rank {@code rank} (1 = the strongest of {@code count}): {@code djinnMax} for rank 1 down to {@code djinnMin} for the last, on a straight line.
     * A bad rank or count is clamped; a band whose max is under its min is treated as a flat band at the min.
     */
    public static double djinnHit(int rank, int count, double djinnMin, double djinnMax) {
        double lo = sane(djinnMin, DEFAULT_DJINN_MIN), hi = Math.max(lo, sane(djinnMax, DEFAULT_DJINN_MAX));
        if (count <= 1) return hi;
        int r = Math.max(1, Math.min(count, rank));
        return lo + (hi - lo) * (count - r) / (double) (count - 1);
    }

    /** The vanilla swing factor: a hit with a partly charged weapon deals {@code 0.2 + 0.8 f^2} of the full one (f clamped to 0..1). */
    public static double charge(double f) {
        double c = Double.isNaN(f) ? 1.0 : Math.max(0.0, Math.min(1.0, f));
        return 0.2 + 0.8 * c * c;
    }

    /**
     * The most one hit of this kind may deal. {@code djinnRank} (1..count) matters for DJINN_WEAPON and EXTREME; for the others it is ignored.
     * A hit with the apex weapon may reach the apex ceiling, every other hit stays at or under the global ceiling, and nothing ever passes {@link #ABSOLUTE_MAX}.
     */
    public static double ceiling(Limits l, Kind kind, int djinnRank, int djinnCount) {
        if (l == null || !l.enabled()) return Double.MAX_VALUE;
        double global = sane(l.global(), DEFAULT_GLOBAL);
        double c = switch (kind) {
            case APEX -> sane(l.apex(), DEFAULT_APEX);
            case WEAPON -> Math.min(global, sane(l.weapon(), DEFAULT_WEAPON));
            case DJINN_WEAPON, DJINN_BEARER -> Math.min(global, Math.min(sane(l.weapon(), DEFAULT_WEAPON), djinnHit(djinnRank, djinnCount, l.djinnMin(), l.djinnMax())));
            case EXTREME -> Math.min(global, djinnHit(djinnRank, djinnCount, l.djinnMin(), l.djinnMax()));
            default -> global;
        };
        return Math.min(c, ABSOLUTE_MAX);
    }

    /** The widest ceiling any hit may have under these settings: the larger of the global and the apex ceiling, never above {@link #ABSOLUTE_MAX}. BalanceLaw.cap uses it; the event decides the exact one. */
    public static double softCeiling(Limits l) {
        if (l == null || !l.enabled()) return Double.MAX_VALUE;
        return Math.min(ABSOLUTE_MAX, Math.max(sane(l.global(), DEFAULT_GLOBAL), sane(l.apex(), DEFAULT_APEX)));
    }

    /** The lowest a Djinn weapon's full swing may land: the band's floor, scaled by the swing's charge. */
    public static double djinnFloor(Limits l, double chargeFactor) {
        return l == null || !l.enabled() || !l.djinnFloor() ? 0.0 : sane(l.djinnMin(), DEFAULT_DJINN_MIN) * Math.max(0.0, Math.min(1.0, chargeFactor));
    }

    /**
     * The final damage of one hit. A Djinn weapon swing (DJINN_WEAPON) is raised to the band floor (times the swing's charge factor, not against players: the PvP cap decides there) and
     * lowered to its rank's number (times the charge factor); every other kind is only lowered to its ceiling. NaN and negative amounts come back as 0.
     */
    public static double apply(Limits l, Kind kind, int djinnRank, int djinnCount, double amount, double chargeFactor, boolean targetIsPlayer) {
        if (Double.isNaN(amount) || amount <= 0) return 0.0;
        if (l == null || !l.enabled()) return amount;
        double cf = kind == Kind.DJINN_WEAPON ? Math.max(0.0, Math.min(1.0, chargeFactor)) : 1.0;
        double hi = ceiling(l, kind, djinnRank, djinnCount) * cf;
        double a = amount;
        if (kind == Kind.DJINN_WEAPON && !targetIsPlayer) a = Math.max(a, Math.min(djinnFloor(l, cf), hi));
        return Math.min(a, hi);
    }

    // ---------------------------------------------------------------- the Extreme Magic scope (server thread only)
    private static int extremeRank, extremeCount;
    private static boolean extremeOn;

    /** Marks that the hit about to be dealt is a Djinn's Extreme Magic of this rank (the server thread, around one {@code hurt} call). */
    public static void beginExtreme(int rank, int count) { extremeRank = rank; extremeCount = count; extremeOn = true; }

    public static void endExtreme() { extremeOn = false; }

    public static boolean extremeActive() { return extremeOn; }

    public static int extremeRank() { return extremeRank; }

    public static int extremeCount() { return extremeCount; }
}
