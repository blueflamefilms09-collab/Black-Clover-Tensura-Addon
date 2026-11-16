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

    // =====================================================================================================================================================
    // Canon pass (setting shinraCanon, docs/shinra_rework.md): the Fire Force wiki's techniques. Everything above is the first rework and stays for shinraCanon=false.
    // =====================================================================================================================================================

    /** New move ids, appended after RAPID (= the Rapid-Man Kick, kept at its index). RAPID_JET is the technique Rapid itself, HELLFIRE the Corna technique, LINK the Adolla Link, GRACE the Adolla Grace. */
    public static final int RAPID_JET = 4, HELLFIRE = 5, LINK = 6, GRACE = 7;

    private static final int[] CANON_UNIQUE = {DASH, KICK, RAPID, RAPID_JET, HELLFIRE, LINK};
    private static final int[] CANON_FULL = {DASH, KICK, BURST, RAPID, RAPID_JET, HELLFIRE, LINK, GRACE};

    /** The wheel with the canon pass: the old wheel with the new moves appended (old positions never move). A fresh copy. */
    public static int[] wheel(boolean full, boolean canon) { return canon ? (full ? CANON_FULL : CANON_UNIQUE).clone() : wheel(full); }

    public static int move(boolean full, boolean canon, int mode) {
        int[] w = canon ? (full ? CANON_FULL : CANON_UNIQUE) : (full ? FULL_WHEEL : UNIQUE_WHEEL);
        return w[Math.floorMod(mode, w.length)];
    }

    /**
     * Unlock order, after the story (the mod has three tiers for what the story spreads over six steps, so this is a mapping, not a chronology): the ignition of the feet, the Adolla Link (it comes
     * early in the story, by itself) and Rapid with the Rapid-Man Kick from the start; Corna (Hellfire) and Hysterical Strength once the skill is mastered; Adolla Grace only as the Ultimate.
     */
    public static Tier minTier(int move) {
        return switch (move) { case HELLFIRE -> Tier.MASTERED; case GRACE -> Tier.ULTIMATE; default -> Tier.BASE; };
    }

    public static boolean unlocked(int move, Tier t) { return t.atLeast(minTier(move)); }

    // ---- Rapid (Tora Hishigi): the flame as one jet, far faster for less fire; the Rapid-Man Kick is a kick while it burns ----
    public static int rapidJetTicks(Tier t) { return t.pick(100, 140, 200); }
    public static double rapidJetCost(Tier t) { return t.ultimate() ? 400 : 150; }
    public static int rapidJetCooldown(Tier t) { return t.pick(300, 260, 200); }
    public static int rapidJetSpeedAmp(Tier t) { return t.pick(1, 2, 3); }
    /** While Rapid burns, the Ignition Dash and the Rapid-Man Kick cost half ("less of his fire"). */
    public static double rapidFireFactor() { return 0.5; }
    /** While Rapid burns: a longer dash, a longer and harder Rapid-Man Kick ("kinetic force is mass times speed"). */
    public static double rapidDashFactor() { return 1.35; }
    public static double rapidKickRangeFactor() { return 1.3; }
    public static double rapidKickDamageFactor() { return 1.5; }
    public static boolean rapidActive(long now, long until) { return until > 0 && now < until; }

    // ---- Hellfire (Corna): a demon's head of dark flame bites the line in front, then explodes ----
    public static double hellfireCost(Tier t) { return t.ultimate() ? 900 : 450; }
    public static int hellfireCooldown(Tier t) { return t.pick(180, 160, 100); }
    public static double hellfireRange(Tier t) { return t.pick(7.0, 8.0, 10.0); }
    public static double hellfireWidth() { return 2.2; }
    public static double hellfireDamage(Tier t) { return t.pick(18.0, 26.0, 36.0); }
    public static double hellfireBlastRadius(Tier t) { return t.ultimate() ? 4.5 : 3.5; }
    public static double hellfireBlastDamage(Tier t) { return hellfireDamage(t) * 0.75; }
    public static int hellfireBurnSeconds(Tier t) { return t.ultimate() ? 8 : 5; }

    // ---- Hysterical Strength (Pressure of Death): the ignition goes from 30 % to 100 % (a factor of 3.3): blue claw flames, more speed and strength. Learned later (mastered); the wiki says Shinra later uses it permanently (the Ultimate) ----
    /** A mastered Unique: under a third of health. The Ultimate: always (while Hell Mode is on). */
    public static boolean hysteria(double health, double max, Tier t) {
        if (!(max > 0) || !(health > 0)) return false;
        return t.ultimate() || t.atLeast(Tier.MASTERED) && health / max < grinBelow();
    }
    public static int hysteriaSpeedAmp(double health, double max, Tier t) { return hysteria(health, max, t) ? (t.ultimate() ? 1 : 0) : -1; }
    public static int hysteriaStrengthAmp(double health, double max, Tier t) { return hysteria(health, max, t) ? (t.ultimate() ? 1 : 0) : -1; }
    /** The Link strong emotion opens by itself: under a quarter of health, any tier. */
    public static boolean involuntaryLink(double health, double max) { return max > 0 && health > 0 && health / max < linkBelow(); }

    // ---- Adolla Link: the Link senses hostile minds (and is what Adolla Grace comes through) ----
    public static double linkCost(Tier t) { return t.ultimate() ? 400 : 250; }
    public static int linkCooldown() { return 400; }
    public static double linkRadius(Tier t) { return t.pick(16.0, 24.0, 32.0); }
    /** At most this many creatures are marked by one Link (a cap on the potion effects of a cast). */
    public static int linkMaxSensed() { return 16; }
    public static int linkGlowTicks() { return 160; }
    /** How long a cast Link stays open: Adolla Grace needs it. */
    public static int linkWindowTicks() { return 600; }
    /** The Link that strong emotion opens by itself (under a quarter of health, Hell Mode on): short. */
    public static int passiveLinkWindowTicks() { return 200; }
    public static int passiveLinkGlowTicks() { return 40; }
    public static boolean linkOpen(long now, long until) { return until > 0 && now < until; }

    // ---- Adolla Grace (Fourth Generation): a timed power boost through the Link, then the Ignition Limit (Overheat) ----
    public static double graceCost() { return 1200; }
    public static int graceCooldown() { return 3000; }
    /** The Grace is short: as little as a second in the story when the giver is weak. */
    public static int graceTicks(Tier t) { return t == Tier.ULTIMATE_MASTERED ? 240 : 160; }
    public static int graceSpeedAmp() { return 3; }
    public static int graceStrengthAmp() { return 2; }
    public static int graceResistanceAmp() { return 1; }
    /** Every move recharges at this fraction of its cooldown while the Grace burns. */
    public static double graceCooldownFactor() { return 0.5; }
    public static double graceLightSpeedDamageFactor() { return 1.5; }
    public static boolean graceActive(long now, long until) { return until > 0 && now < until; }
    /** The Grace has ended (the mark is cleared once). */
    public static boolean graceEnded(long now, long until) { return until > 0 && now >= until; }
    /** The Overheat is only paid if the Grace ended just now (not after a logout or a long absence). */
    public static boolean overheatDue(long now, long until) { return graceEnded(now, until) && now - until <= 100; }
    public static int overheatTicks() { return 120; }
}
