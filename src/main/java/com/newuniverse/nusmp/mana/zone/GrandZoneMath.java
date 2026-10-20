package com.newuniverse.nusmp.mana.zone;

/**
 * The numbers of the Grand Magic Zones, with no Minecraft types (unit tested). A zone's pressure is read against the player's EP: a player
 * far under the zone's recommendation is {@code harm}ed (slowed, burned, thrown about); a player far over it is fed ({@code boon}: mana
 * regeneration and a little spell power); a Mana Zone user resists half the harm. Everything is capped so no pulse can kill or fling
 * someone out of the world, and everything is a function of the time and a seed, so the server keeps no per-effect state.
 */
public final class GrandZoneMath {
    private GrandZoneMath() {}

    /** Largest speed (blocks per tick) a gravity pulse may give, and the largest push a current gives per application. */
    public static final double MAX_GRAVITY_SPEED = 0.7, MAX_CURRENT = 0.18;
    /** How many blocks inside the border an effect reaches full strength (it fades in from the border). */
    public static final double EDGE_FADE_BLOCKS = 24.0;
    /** Largest share of the player's maximum health one pressure pulse takes (never lethal either: the caller leaves 1 HP). */
    public static final double PRESSURE_MAX_SHARE = 0.06;
    /** The server looks at each player this often (ticks), staggered by entity id. */
    public static final int SCAN_INTERVAL = 10;
    /** The longest gap between two refreshes of the zone state the client holds (ticks). */
    public static final int SYNC_REFRESH = 100;

    public record Exposure(double harm, double boon) {
        public static final Exposure NONE = new Exposure(0, 0);

        public boolean calm() { return harm <= 0.0 && boon <= 0.0; }
    }

    /**
     * Harm and boon for a player of {@code ep} in a zone recommending {@code recommendedEp} with the given density (1..3).
     * Under the recommendation: harm 0..1 (rising with the shortfall, a little more in denser mana). Over it: boon 0..1 (a doubling is a
     * third, eight times the recommendation is full). A safe haven never harms and always feeds a little.
     */
    public static Exposure exposure(double ep, double recommendedEp, double density, boolean calm, boolean manaZoneUser) {
        double ratio = recommendedEp <= 0 ? 8.0 : Math.max(0.0, ep) / recommendedEp;
        double dens = Math.max(0.6, Math.min(1.5, density / 2.0));
        double harm = 0, boon = 0;
        if (ratio < 1.0) {
            harm = Math.min(1.0, Math.pow(1.0 - ratio, 0.75) * dens);
            if (harm < 0.05) harm = 0;
            if (manaZoneUser) harm *= 0.5;
        } else {
            boon = Math.min(1.0, (Math.log(ratio) / Math.log(2.0)) / 3.0 * Math.max(0.75, Math.min(1.25, dens)));
        }
        if (calm) return new Exposure(0, Math.max(0.3, boon));
        return new Exposure(harm, boon);
    }

    /** 0..1: how far into the zone the player is, over {@link #EDGE_FADE_BLOCKS}; the effects scale with it so the border is not a cliff. */
    public static double edgeFade(double depth) { return depth <= 0 ? 0 : Math.min(1.0, depth / EDGE_FADE_BLOCKS); }

    /** Sync bucket of an exposure value (twentieths): a new packet is sent only when the bucket changes. */
    public static int bucket(double v) { return (int) Math.round(Math.max(0, Math.min(1, v)) * 20.0); }

    // ---------------------------------------------------------------- pressure
    /** Health taken by one pressure pulse (every 2 s): nothing under 0.35 harm, at most {@link #PRESSURE_MAX_SHARE} of the maximum health. */
    public static double pressureDamage(double harm, double maxHealth) {
        if (harm < 0.35) return 0;
        return Math.min(maxHealth * PRESSURE_MAX_SHARE, (harm - 0.3) * 0.08 * maxHealth);
    }

    /** Slowness amplifier for a weak player: -1 none, 0 or 1. */
    public static int slownessAmplifier(double harm) { return harm >= 0.7 ? 1 : harm >= 0.3 ? 0 : -1; }

    // ---------------------------------------------------------------- gravity (Gravito Rock Zone)
    public enum PulseKind { LIFT, PUSH, SLAM }

    /** A small, fast, deterministic hash of a seed and a counter (no allocation). */
    public static long mix(long seed, long n) {
        long z = seed + n * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Ticks until the next gravity pulse for a player: 60 to 160, from the seed. */
    public static int pulseGap(long seed, long n) { return 60 + (int) Math.floorMod(mix(seed, n), 100L); }

    /**
     * The velocity change of one gravity pulse, written into {@code out} (x,y,z); returns its kind. The magnitude grows with the harm, shrinks
     * with the boon (strong players ride the zone) and with a Mana Zone, and never passes {@link #MAX_GRAVITY_SPEED}. A SLAM only happens to
     * players that are being harmed, and is shorter than the lift that precedes it.
     */
    public static PulseKind gravityPulse(long seed, long n, double harm, double boon, boolean manaZoneUser, double[] out) {
        long h = mix(seed, n * 3 + 1);
        int k = (int) Math.floorMod(h, 6L);
        PulseKind kind = k <= 2 ? PulseKind.LIFT : k <= 4 ? PulseKind.PUSH : (harm >= 0.4 ? PulseKind.SLAM : PulseKind.LIFT);
        double strength = (0.35 + 0.5 * Math.max(0, Math.min(1, harm))) * (1.0 - 0.6 * Math.max(0, Math.min(1, boon))) * (manaZoneUser ? 0.5 : 1.0);
        double ang = (Math.floorMod(mix(seed, n * 3 + 2), 3600L) / 3600.0) * Math.PI * 2;
        double hx = Math.cos(ang), hz = Math.sin(ang);
        switch (kind) {
            case LIFT -> { out[0] = hx * 0.1 * strength; out[1] = 0.9 * strength; out[2] = hz * 0.1 * strength; }
            case PUSH -> { out[0] = hx * 0.9 * strength; out[1] = 0.15 * strength; out[2] = hz * 0.9 * strength; }
            default -> { out[0] = 0; out[1] = -0.8 * strength; out[2] = 0; }
        }
        double len = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
        if (len > MAX_GRAVITY_SPEED) { double s = MAX_GRAVITY_SPEED / len; out[0] *= s; out[1] *= s; out[2] *= s; }
        return kind;
    }

    /** Slow-falling ticks to give after a LIFT (chaotic low gravity lasts a moment). */
    public static int liftSlowFallTicks(double strengthHint) { return 40 + (int) Math.round(40 * Math.max(0, Math.min(1, strengthHint))); }

    // ---------------------------------------------------------------- currents (Seabed Temple)
    /**
     * The push of a violent current on something at (x, z), written into {@code out} (x,y,z): a swirl round the zone's centre axis with a
     * wandering sideways surge, stronger with the harm, never over {@link #MAX_CURRENT}. {@code timeTicks} is the game time.
     */
    public static void currentPush(long timeTicks, double x, double z, double cx, double cz, double harm, double boon, boolean manaZoneUser, double[] out) {
        double dx = x - cx, dz = z - cz;
        double len = Math.sqrt(dx * dx + dz * dz);
        double tx = len < 1e-3 ? 1 : -dz / len, tz = len < 1e-3 ? 0 : dx / len;          // tangent: round the centre
        double phase = timeTicks / 37.0 + x * 0.071 + z * 0.053;
        double surge = Math.sin(phase), lift = Math.sin(phase * 0.6 + 1.7);
        double strength = (0.55 + 0.45 * Math.max(0, Math.min(1, harm))) * (1.0 - 0.5 * Math.max(0, Math.min(1, boon))) * (manaZoneUser ? 0.6 : 1.0);
        double m = MAX_CURRENT * strength;
        out[0] = (tx * 0.8 + (len < 1e-3 ? 0 : dx / len) * 0.5 * surge) * m;
        out[1] = lift * 0.35 * m;
        out[2] = (tz * 0.8 + (len < 1e-3 ? 0 : dz / len) * 0.5 * surge) * m;
        double l = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
        if (l > MAX_CURRENT) { double s = MAX_CURRENT / l; out[0] *= s; out[1] *= s; out[2] *= s; }
    }

    // ---------------------------------------------------------------- heat (Ultime-Volcano Mountain Trail)
    /** Seconds of fire a heat pulse sets on a weak player (0 = none). */
    public static int heatFireSeconds(double harm) { return harm >= 0.15 ? 1 + (int) Math.round(harm * 3) : 0; }

    /** Damage of a heat pulse (every 2 s); at most 8% of the maximum health. */
    public static double heatDamage(double harm, double maxHealth) { return harm < 0.15 ? 0 : Math.min(maxHealth * 0.08, 1.0 + 3.0 * harm); }

    // ---------------------------------------------------------------- weather
    /** True on the seconds a mana storm throws a lightning bolt near a player (about one in seven seconds, never two in a row). */
    public static boolean stormBolt(long seconds, long seed) { return Math.floorMod(mix(seed, seconds), 7L) == 0; }

    /** Chance (0..1) that a bolt aimed at a weak player hits them (the rest only light the sky). */
    public static double boltHitChance(double harm) { return harm < 0.4 ? 0 : harm * 0.12; }

    /** Damage of a bolt that hits: a share of the maximum health, never lethal at the caller. */
    public static double boltDamage(double maxHealth) { return Math.min(maxHealth * 0.1, 6.0); }

    // ---------------------------------------------------------------- the boon
    /** Percent of the maximum magicule a fed player gains each second. */
    public static double manaRegenPercent(double boon, double density) { return 0.25 * Math.max(0, Math.min(1, boon)) * Math.max(0.6, Math.min(1.5, density / 2.0)); }

    /** Spell power multiplier of a fed player inside the zone (at most +10%). */
    public static double powerMult(double boon) { return 1.0 + 0.10 * Math.max(0, Math.min(1, boon)); }

    /** Health a safe haven restores every two seconds to anyone below full health. */
    public static double havenHeal() { return 1.0; }

    // ---------------------------------------------------------------- client look
    /** Share of the sky and fog that is tinted with the zone's colour: from the border (0) to the heart (full), stronger in a storm. */
    public static double skyTint(double edgeFade, boolean storm, double density) {
        double base = storm ? 0.45 : 0.2;
        return Math.max(0, Math.min(0.75, edgeFade * (base + 0.12 * Math.max(0, density - 1.0))));
    }

    /** Fog distance multiplier (1 = none): the storm and the witches' glow close the view in. */
    public static double fogScale(double edgeFade, boolean storm, boolean forest) {
        double f = 1.0;
        if (storm) f -= 0.35 * edgeFade;
        if (forest) f -= 0.25 * edgeFade;
        return Math.max(0.35, f);
    }
}
