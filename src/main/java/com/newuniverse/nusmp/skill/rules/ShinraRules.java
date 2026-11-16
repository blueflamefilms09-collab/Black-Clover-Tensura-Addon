package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Hell Walker is now the Shinra Kusakabe skill (docs/shinra_rework.md): Devil's Footprints (the dash and kick, numbers in {@link FireForceRules}), the Rapid-Man Kick, the
 * low-health Devil's Grin and Adolla Link, and (as Adolla Burst) the Ultimate. This class holds what is new and pure: which moves are on the wheel, the health thresholds of the grin and the
 * link, the numbers of the Rapid-Man Kick and the cost of the passive effects, so the tests can pin them (the Ultimate must out-class the Unique in every number, and nothing is unbounded).
 * Minecraft-free.
 */
public final class ShinraRules {
    private ShinraRules() {}

    /** Move ids. DASH, KICK and BURST are the {@link FireForceRules} indexes; RAPID is new and appended. */
    public static final int DASH = FireForceRules.DASH, KICK = FireForceRules.KICK, BURST = FireForceRules.BURST, RAPID = 3;

    private static final int[] UNIQUE_WHEEL = {DASH, KICK, RAPID};
    private static final int[] FULL_WHEEL = {DASH, KICK, BURST, RAPID};

    /** The wheel: Ignition Dash, Devil's Kick, Rapid-Man Kick for the Unique; Light Speed, Devil's Kick, Adolla Burst, Rapid-Man Kick for Adolla Burst. A fresh copy, never null. */
    public static int[] wheel(boolean full) { return (full ? FULL_WHEEL : UNIQUE_WHEEL).clone(); }

    /** The move at a wheel position (any int is wrapped, so a stale mode index from an old save is harmless). */
    public static int move(boolean full, int mode) {
        int[] w = full ? FULL_WHEEL : UNIQUE_WHEEL;
        return w[Math.floorMod(mode, w.length)];
    }

    // ---- Devil's Grin / Adolla Link (the low-health passives under the toggle) ----
    public static double grinBelow() { return 0.35; }
    public static double linkBelow() { return 0.25; }

    /** The devil's grin: under a third of health (and alive, with a real max) Shinra smiles and his feet burn hotter. */
    public static boolean grin(double health, double max) { return max > 0 && health > 0 && health / max < grinBelow(); }

    /** Adolla Link: a mastered Unique or the Ultimate, under a quarter of health. */
    public static boolean link(double health, double max, Tier t) { return t.atLeast(Tier.MASTERED) && max > 0 && health > 0 && health / max < linkBelow(); }

    /** Speed amplifier while the grin or the link is on (-1 = none): the grin Speed I, the link Speed II. */
    public static int speedAmp(double health, double max, Tier t) { return link(health, max, t) ? 1 : grin(health, max) ? 0 : -1; }

    /** Strength amplifier (-1 = none): only the link, Strength I (the Ultimate: II). */
    public static int strengthAmp(double health, double max, Tier t) { return link(health, max, t) ? (t.ultimate() ? 1 : 0) : -1; }

    /** Cooldown factor of every move while the link is on: the flames answer faster. */
    public static double cooldownFactor(double health, double max, Tier t) { return link(health, max, t) ? 0.6 : 1.0; }

    /** How long the passive effects last, refreshed once a second (so they end within 3 seconds of the grin ending or the toggle going off). */
    public static final int EFFECT_TICKS = 60;

    // ---- Rapid-Man Kick: a short ignition dash that kicks everything on its line ----
    public static double rapidCost(Tier t) { return t.ultimate() ? 500 : 200; }
    public static int rapidCooldown(Tier t) { return t.ultimate() ? 50 : 80; }
    public static double rapidRange(Tier t) { return t.pick(7.0, 8.0, 10.0); }
    public static double rapidWidth() { return 1.6; }
    public static double rapidDamage(Tier t) { return t.pick(9.0, 13.0, 18.0); }
    public static int rapidBurnSeconds(Tier t) { return t.ultimate() ? 6 : 4; }
    public static double rapidPower(Tier t) { return t == Tier.BASE ? 1.5 : 1.9; }
    /** Most foes one move may hit: the cost per cast is bounded whatever the crowd. */
    public static int maxTargets() { return 12; }
}
