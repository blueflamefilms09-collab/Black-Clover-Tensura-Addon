package com.newuniverse.nusmp.fairytail.storm;

/**
 * Why: the small decisions of Storm Magic that are plain rules, kept out of the game classes so they are unit tested: whether the old Storm grimoire is hidden from the rolls, how
 * the server's caps shorten or narrow a spell, how much one spell can ever take from one creature. Pure (no game class).
 */
public final class StormRules {
    private StormRules() {}

    /** The one magic the staff-only setting is about (the name of the enum constant of the old grimoire magic). */
    public static final String STORM = "STORM";

    /** Is the magic of this enum name hidden from the rolls (a soul's pool, the starter pool, the wizard tower) under this setting of {@code storm.staffOnly}? */
    public static boolean hiddenName(String enumName, boolean staffOnly) { return staffOnly && STORM.equals(enumName); }

    /** The radius a spell really gets: what it asks for, never above the server's cap and never below 1. */
    public static double effectiveRadius(StormSpell s, double serverMax) { return Math.max(1.0, Math.min(s.radius, serverMax)); }

    /** The duration a spell really gets in ticks: what it asks for, never above the server's cap and never below one second. */
    public static int effectiveDuration(StormSpell s, int serverMax) { return Math.max(20, Math.min(s.durationTicks, serverMax)); }

    /**
     * The most raw damage one storm of this spell can ever deal to ONE creature, from the per-creature caps ({@link StormMath}): the bolts, the hail stones and the rain or funnel pulses
     * that may land on it, or the single blow of a front or surge. The balance law scales it afterwards (EP, the PvP cap).
     */
    public static double worstCaseRawPerTarget(StormSpell s) {
        if (s.isLine()) return s.tickRaw;
        double bolts = s.has(StormComp.BOLTS) ? Math.min(StormMath.MAX_BOLTS_PER_TARGET, s.maxBolts) * s.boltRaw : 0;
        double hail = s.has(StormComp.HAIL) ? StormMath.MAX_HAIL_PER_TARGET * s.hailRaw : 0;
        double pulses = (s.has(StormComp.RAIN) || s.has(StormComp.FUNNEL)) ? StormMath.MAX_PULSES_PER_TARGET * s.tickRaw : 0;
        return bolts + hail + pulses;
    }

    /** Worst-case raw damage per second to one creature over the spell's cooldown: what the spell can sustain if it is cast again the moment it is ready. */
    public static double worstCaseRawPerSecond(StormSpell s) { return worstCaseRawPerTarget(s) / (s.cooldownTicks / 20.0); }
}
