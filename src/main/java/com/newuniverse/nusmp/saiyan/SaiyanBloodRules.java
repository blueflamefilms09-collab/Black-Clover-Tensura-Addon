package com.newuniverse.nusmp.saiyan;

/**
 * The numbers of the Saiyan-blooded evolution (Human to Saiyan, docs/saiyan_prodigy.md). Pure: no game type, unit-tested ({@code SaiyanBloodRulesTest}). A Human becomes a Saiyan by three deeds: existence points
 * (the same currency Tensura's other evolutions ask for), worthy fights won, and surviving at the edge of death. The weights add up to 100 (Tensura's own convention for an evolution: each requirement gives a share, the
 * evolution opens at 100). A deed counter of 0 switches that deed off and its weight goes to the others.
 */
public final class SaiyanBloodRules {
    private SaiyanBloodRules() {}

    public static final double W_EP = 40.0, W_KILLS = 40.0, W_NEAR = 20.0;
    /** Tag keys on the Human race instance (saved and synced with the race). */
    public static final String TAG_KILLS = "nusmp_blood_kills", TAG_NEAR = "nusmp_blood_near", TAG_NEAR_AT = "nusmp_blood_near_at";
    /** A deed counter never grows past this (a bound on the saved number, not a goal). */
    public static final int COUNT_CAP = 1000;
    /** A near-death survival is counted at most once per this many ticks (no farming of it): ten minutes. */
    public static final long NEAR_GAP_TICKS = 12_000L;
    /** "At the edge of death": at or below this share of maximum health after the blow, still alive. */
    public static final double NEAR_SHARE = 0.20;

    /** A kill counts when the victim was hostile and could hurt: at least this much maximum health (no chickens, no slimes of size one). */
    public static final double WORTHY_HEALTH = 10.0;

    public static boolean worthyKill(boolean victimHostile, double victimMaxHealth, boolean victimIsPlayerOrPet) { return victimHostile && !victimIsPlayerOrPet && victimMaxHealth >= WORTHY_HEALTH; }

    public static boolean nearDeath(double healthAfter, double maxHealth) { return maxHealth > 0.0 && healthAfter > 0.0 && healthAfter / maxHealth <= NEAR_SHARE; }

    public static boolean nearGapOver(long now, long last) { return last < 0 || now < last || now - last >= NEAR_GAP_TICKS; }

    public static int bump(int count) { return Math.min(COUNT_CAP, Math.max(0, count) + 1); }

    /** Weights for a server's switches: the EP share, the kill share and the near-death share, normalised to 100 (a deed with a need of 0 is off and gives its share away). */
    public static double[] weights(double epNeed, int killNeed, int nearNeed) {
        double e = epNeed > 0 ? W_EP : 0, k = killNeed > 0 ? W_KILLS : 0, n = nearNeed > 0 ? W_NEAR : 0, sum = e + k + n;
        if (sum <= 0) return new double[] {100.0, 0.0, 0.0};          // everything off: the EP deed alone (a need of 0 is then met at once)
        double f = 100.0 / sum;
        return new double[] {e * f, k * f, n * f};
    }

    /** One deed's progress 0..1. A need of 0 or less is met. */
    public static double share(double have, double need) {
        if (!(need > 0.0)) return 1.0;
        if (Double.isNaN(have)) return 0.0;
        return Math.max(0.0, Math.min(1.0, have / need));
    }

    /** The whole evolution progress 0..100 (Tensura asks for 100). */
    public static double progress(double ep, double epNeed, int kills, int killNeed, int near, int nearNeed) {
        double[] w = weights(epNeed, killNeed, nearNeed);
        return Math.max(0.0, Math.min(100.0, w[0] * share(ep, epNeed) + w[1] * share(kills, killNeed) + w[2] * share(near, nearNeed)));
    }

    public static boolean ready(double progress) { return progress >= 100.0 - 1.0e-9; }
}
