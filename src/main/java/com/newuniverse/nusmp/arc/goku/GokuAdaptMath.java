package com.newuniverse.nusmp.arc.goku;

/**
 * Why: Goku Omega's adaptation, 5.1.1, the way the Tensura More Skills mod does it (Adaptability + Divine General Mahoraga, read from its jar): he counts the hits he took of each damage kind, a kind
 * gets a LEARNED STAGE from 1 to 5 (1 hit = stage 1, then 2, 3, 4, 5 hits) and takes a little LESS damage of that kind, never none: 1% less from the first hit, 1% more for every 3 further hits, at most 25%
 * (50% in Mastered Ultra Instinct, like Mahoraga's mastered cap). The old Great Sage numbers (6% per hit from the 4th, up to 75% off, with a decay) are {@link GokuOmegaMath#adaptResist}, kept behind
 * the setting {@code goku_boss.mahoragaAdapt}. Pure: no game type. Percent values are 0..99 as in the setting.
 */
public final class GokuAdaptMath {
    private GokuAdaptMath() {}

    /** Hits of one kind that give learned stages 1..5 (Mahoraga's defaults: stage1Hits 1, then 2, 3, 4, 5). */
    public static final int[] STAGE_HITS = {1, 2, 3, 4, 5};
    /** The most hits one kind keeps counting; a fight never gets near it. */
    public static final int MAX_HITS = 100000;
    /** No setting lifts the reduction above this share: he is never immune. */
    public static final double CEILING = 0.90;

    /** The learned stage (0 = none yet, 1..5) after {@code hits} hits of a kind. */
    public static int stage(int hits) {
        int s = 0;
        for (int i = 0; i < STAGE_HITS.length; i++) if (hits >= STAGE_HITS[i]) s = i + 1;
        return s;
    }

    /** The share of damage of a kind he takes less after {@code hits} hits: {@code basePct} from the first, +1% per {@code hitsPerPct} further hits, held to {@code capPct} (and to {@link #CEILING}). 0 before the first hit. */
    public static double reduction(int hits, double basePct, int hitsPerPct, double capPct) {
        if (hits < 1) return 0.0;
        double pct = Math.max(0.0, basePct) + (hits - 1) / Math.max(1, hitsPerPct);
        return Math.min(CEILING, Math.min(Math.max(0.0, capPct), pct) / 100.0);
    }

    /** The multiplier of a hit of that kind: 1 minus the reduction; never below {@code 1 - CEILING}. */
    public static double multiplier(int hits, double basePct, int hitsPerPct, double capPct) {
        return 1.0 - reduction(hits, basePct, hitsPerPct, capPct);
    }
}
