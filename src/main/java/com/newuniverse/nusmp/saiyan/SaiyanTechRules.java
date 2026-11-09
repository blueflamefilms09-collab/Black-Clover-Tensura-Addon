package com.newuniverse.nusmp.saiyan;

/**
 * Why: the numbers of the four new Saiyan techniques (3.66.0) with no Minecraft type, so they are unit tested. KI BARRIER: a shield with a pool of damage that soaks blows for a few seconds. INSTANT
 * TRANSMISSION: a teleport to the creature you look at (or a blink to where you look), with a range and a cost. SPIRIT BOMB: hold to gather, the more living friends are near the faster and bigger it
 * gets; throw it where you look and it lands after a travel time and blasts everything hostile in a radius. METEOR COMBINATION: a rush of timed strikes on one foe that ends in a heavy last blow.
 * Every number is a starting value and a server setting; all of them are bounded here (sources, hits, ticks), so a technique can never cost more than a fixed amount of work.
 */
public final class SaiyanTechRules {
    private SaiyanTechRules() {}

    /** The most living friends that add to a Spirit Bomb (one entity query a second, at most this many looked at). */
    public static final int MAX_SOURCES = 24;
    /** The most hits one Meteor Combination can have, whatever the setting says. */
    public static final int MAX_METEOR_HITS = 16;
    /** The most creatures a Spirit Bomb blast looks at. */
    public static final int MAX_BLAST_TARGETS = 64;

    // ------------------------------------------------------------------ Ki Barrier
    /** The damage the barrier can soak: the base pool, up to three times that as Battle Power growth (0..1) approaches the top. */
    public static double barrierPool(double base, double growth) { return Math.max(0.0, base) * (1.0 + 2.0 * clamp01(growth)); }

    /** What gets through a barrier of this pool: the blow minus what the pool soaks (never negative). */
    public static double throughBarrier(double pool, double damage) { return Math.max(0.0, damage - Math.max(0.0, pool)); }

    /** What is left of the pool after the blow. */
    public static double poolAfter(double pool, double damage) { return Math.max(0.0, pool - Math.max(0.0, damage)); }

    /** True while the barrier still stands: time left and pool left. */
    public static boolean barrierUp(double pool, long now, long until) { return pool > 0.0 && now < until; }

    // ------------------------------------------------------------------ Instant Transmission
    /** The most the blink (nothing to lock onto) goes: the range, capped by the blink setting. */
    public static double blinkRange(double range, double blinkMax) { return Math.max(2.0, Math.min(range, blinkMax)); }

    /** Where to land when a creature is the target: {@code behind} blocks short of it along the line from the Saiyan; the same point when the Saiyan is closer than that. */
    public static double landingDistance(double distanceToTarget, double behind) { return Math.max(0.0, distanceToTarget - Math.max(0.0, behind)); }

    // ------------------------------------------------------------------ Spirit Bomb
    /** Friends that count: bounded. */
    public static int sources(int found) { return Math.max(0, Math.min(found, MAX_SOURCES)); }

    /** How fast the bomb gathers compared with a lone Saiyan: +12% per friend, at most 2.5 times as fast. */
    public static double gatherRate(int sources) { return Math.min(2.5, 1.0 + 0.12 * Math.max(0, sources)); }

    /** 0..1: how gathered the bomb is after this many ticks. */
    public static double charge(long heldTicks, double maxSeconds, double avgRate) {
        return clamp01(Math.max(0, heldTicks) / 20.0 / Math.max(0.5, maxSeconds) * Math.max(1.0, avgRate));
    }

    /** The blast damage before the balance law: from 30% (a quick throw) to all of it, +2% per friend that fed it. */
    public static double bombDamage(double full, double charge, int sources) { return Math.max(0.0, full) * (0.3 + 0.7 * clamp01(charge)) * (1.0 + 0.02 * sources(sources)); }

    /** The blast radius in blocks: 40% of the full radius for a quick throw, all of it for a full charge. */
    public static double bombRadius(double full, double charge) { return Math.max(2.0, full * (0.4 + 0.6 * clamp01(charge))); }

    /** Ticks the thrown bomb travels: one tick per 1.2 blocks, between 10 and {@code max}. */
    public static int travelTicks(double distance, int max) { return (int) Math.max(10, Math.min(Math.max(10, max), Math.round(Math.max(0.0, distance) / 1.2))); }

    // ------------------------------------------------------------------ Meteor Combination
    /** The number of hits, bounded. */
    public static int meteorHits(int setting) { return Math.max(1, Math.min(setting, MAX_METEOR_HITS)); }

    /** The damage multiplier of hit i (0-based) of n: ordinary hits count 1, the last one 3. */
    public static double meteorHitFactor(int i, int n) { return i >= n - 1 ? 3.0 : 1.0; }

    /** True if the target is still close enough for the next hit (otherwise the Saiyan flashes to it first). */
    public static boolean inReach(double distance, double reach) { return distance <= reach; }

    static double clamp01(double x) { return x < 0.0 ? 0.0 : (x > 1.0 ? 1.0 : x); }
}
