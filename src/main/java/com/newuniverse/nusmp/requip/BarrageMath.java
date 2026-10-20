package com.newuniverse.nusmp.requip;

import com.newuniverse.nusmp.relics.GradeDamage;

/**
 * Why: Requip: Barrage is the Fairy Tail sword rain: a ring of spectral copies of the weapon in the hand that hang around the caster for a beat and then fly at the crosshair. Where the swords stand, when each
 * one leaves, how hard each one hits (from the weapon's own damage through the grade table), how many targets a sword may pierce and how much of a boss's health one volley may take is arithmetic that must
 * not be checked by feel; this class is that arithmetic (no Minecraft classes), {@code skill.RequipBarrage} runs it and {@code BarrageMathTest} pins it. Requip: Strike and the finishers use the same
 * damage rules.
 */
public final class BarrageMath {
    private BarrageMath() {}

    // ------------------------------------------------------------ how many
    public static final int MIN_SWORDS = 8, MAX_SWORDS = 24;

    /** 8 swords at the start, up to 10 more with mastery and up to 6 more with max EP (50k, 200k and 1M), 24 at most. */
    public static int swordCount(double mastery, double maxEp) {
        int n = MIN_SWORDS + (int) Math.round(10.0 * clamp01(mastery));
        if (maxEp >= 50_000) n += 2;
        if (maxEp >= 200_000) n += 2;
        if (maxEp >= 1_000_000) n += 2;
        return Math.max(MIN_SWORDS, Math.min(MAX_SWORDS, n));
    }

    // ------------------------------------------------------------ where they stand
    /** The tier a sword stands on: one ring for up to twelve swords, two rings (an outer, higher one and an inner, lower one) above that. */
    public static int tierOf(int index, int count) { return count <= 12 ? 0 : index % 2; }

    /**
     * Offsets {x, y, z} from the caster's feet of {@code count} swords: a ring of the given radius at shoulder height for up to twelve, two interleaved rings above that. {@code phase} (radians) turns the
     * whole ring (the volley slowly turns while it hovers).
     */
    public static double[][] ring(int count, double radius, double phase) {
        double[][] out = new double[Math.max(0, count)][];
        int inTier0 = count <= 12 ? count : (count + 1) / 2, inTier1 = count - inTier0;
        for (int i = 0; i < count; i++) {
            int tier = tierOf(i, count);
            int k = count <= 12 ? i : i / 2;
            int n = tier == 0 ? inTier0 : Math.max(1, inTier1);
            double a = phase + (tier == 1 ? Math.PI / n : 0.0) + 2.0 * Math.PI * k / n;
            double r = tier == 0 ? radius : radius * 0.62;
            double y = tier == 0 ? 1.55 + (count > 12 ? 0.45 : 0.0) : 1.15;
            out[i] = new double[]{Math.cos(a) * r, y, Math.sin(a) * r};
        }
        return out;
    }

    /** Where the {@code index}-th of {@code count} swords aims when there is no target: spread round the crosshair point (a golden-angle spiral of at most {@code radius}). */
    public static double[] aimOffset(int index, int count, double radius) {
        if (count <= 1) return new double[]{0, 0};
        double a = index * 2.399963229728653, r = radius * Math.sqrt((index + 0.5) / count);
        return new double[]{Math.cos(a) * r, Math.sin(a) * r};
    }

    // ------------------------------------------------------------ when they leave
    /** Ticks the swords hang before the first leaves. */
    public static final int HOVER_TICKS = 14;
    /** The fewest and most ticks a sword takes to fly. */
    public static final int MIN_FLIGHT = 3, MAX_FLIGHT = 24;
    /** Blocks a sword flies per tick. */
    public static final double SPEED = 2.2;

    /** Ticks between two swords leaving: two for a small volley, one for a big one, so the whole volley is over inside about 2 seconds. */
    public static int stagger(int count) { return count <= 12 ? 2 : 1; }

    /** The tick (counted from the cast) at which sword {@code index} leaves its place. */
    public static int launchTick(int index, int count) { return HOVER_TICKS + Math.max(0, index) * stagger(count); }

    public static int flightTicks(double distance) {
        return Math.max(MIN_FLIGHT, Math.min(MAX_FLIGHT, (int) Math.ceil(Math.max(0.0, distance) / SPEED)));
    }

    /** The tick at which the last sword has landed, for a flight of at most {@link #MAX_FLIGHT} ticks. */
    public static int lastLandingTick(int count) { return launchTick(count - 1, count) + MAX_FLIGHT; }

    // ------------------------------------------------------------ how hard
    /** Per-sword base damage by the weapon's grade, index 0 = a weapon below the Tensura grade table (a plain vanilla sword), then Normal to Genesis. */
    private static final double[] GRADE_RAW = {3.0, 4.0, 5.5, 7.0, 8.5, 10.0, 12.0, 14.0};

    /** The grade (0 Normal .. 6 Genesis) whose range holds the damage, or -1 for a weapon weaker than the Normal grade. A damage between two ranges belongs to the lower grade. */
    public static int gradeOf(double weaponDamage) {
        int g = -1;
        for (int i = 0; i < 7; i++) if (weaponDamage + 1e-9 >= GradeDamage.range(i)[0]) g = i;
        return g;
    }

    /** The Weapon Affinity passive: summoned weapons hit harder with mastery (from 20 percent mastery, up to +40 percent). */
    public static double affinity(double mastery) {
        double m = clamp01(mastery);
        return m < 0.20 ? 1.0 : 1.0 + 0.40 * m;
    }

    /** Big volleys share their power: above twelve swords every sword hits a little softer, so 24 swords are not twice 12. */
    public static double countFactor(int count) { return count <= 12 ? 1.0 : Math.sqrt(12.0 / count); }

    /**
     * The raw damage of one sword, before the balance law scales it with the caster's EP and mastery: the base of the weapon's grade, a little more for a weapon high in its grade, the Weapon Affinity
     * passive, and the share a big volley gives up.
     */
    public static double perSwordRaw(double weaponDamage, int count, double mastery) {
        int g = gradeOf(weaponDamage);
        double base = GRADE_RAW[g + 1];
        double within = 0.0;
        if (g >= 0) {
            int[] r = GradeDamage.range(g);
            within = r[1] > r[0] ? Math.max(0.0, Math.min(1.0, (weaponDamage - r[0]) / (r[1] - r[0]))) : 0.0;
        }
        return base * (1.0 + 0.15 * within) * affinity(mastery) * countFactor(count);
    }

    /** The raw damage of the thrown weapon of Requip: Strike on each of its two passes. */
    public static double strikeRaw(double weaponDamage, double mastery) { return perSwordRaw(weaponDamage, 8, mastery) * 2.2; }

    /** The raw damage of the Dimension-Cut: a heavy single stroke, 3.5 swords' worth. */
    public static double cutRaw(double weaponDamage, double mastery) { return perSwordRaw(weaponDamage, 8, mastery) * 3.5; }

    /** How many creatures one sword may pass through before it is spent: 1, 2 from 50 percent mastery, 3 from 90 percent. */
    public static int pierce(double mastery) {
        double m = clamp01(mastery);
        return 1 + (m >= 0.5 ? 1 : 0) + (m >= 0.9 ? 1 : 0);
    }

    /** One volley may take at most this much of a boss's health in total (a share of max health); anything else has no volley cap. */
    public static final double BOSS_VOLLEY_SHARE = 0.10;

    public static double volleyCap(double maxHealth, boolean boss) { return volleyCap(maxHealth, boss, BOSS_VOLLEY_SHARE); }

    /** The same with the share a server owner set in the config (held to 1 to 100 percent). */
    public static double volleyCap(double maxHealth, boolean boss, double share) {
        double s = Double.isNaN(share) ? BOSS_VOLLEY_SHARE : Math.max(0.01, Math.min(1.0, share));
        return boss ? Math.max(1.0, maxHealth * s) : Double.POSITIVE_INFINITY;
    }

    /** What of {@code damage} may still be dealt to a target that has already taken {@code dealt} of a volley with the cap {@code cap}. */
    public static double allowed(double damage, double dealt, double cap) { return Math.max(0.0, Math.min(damage, cap - dealt)); }

    // ------------------------------------------------------------ the Dimension-Cut
    /** The share of the target's armour the Dimension-Cut ignores: 30 percent, a little more with mastery, never more than {@link #CUT_BYPASS_CAP}. */
    public static final double CUT_BYPASS = 0.30, CUT_BYPASS_CAP = 0.40;

    public static double cutBypass(double mastery) { return Math.min(CUT_BYPASS_CAP, CUT_BYPASS + 0.10 * clamp01(mastery)); }

    /** The factor the armour bypass may raise damage by (the same cap the rest of the mod uses). */
    public static final double MAX_BYPASS_FACTOR = 3.0;

    // ------------------------------------------------------------ shapes for the finishers
    /** A point of a rose curve with {@code petals} petals (odd count k gives k petals; the curve r = R*|cos(k*theta/2)|), {@code t} in 0..1 round the flower. Returns {x, z}. */
    public static double[] rose(int petals, double t, double radius) {
        double theta = t * 2.0 * Math.PI * 2.0;
        double r = radius * Math.abs(Math.cos(petals * theta / 2.0));
        return new double[]{Math.cos(theta) * r, Math.sin(theta) * r};
    }

    /** {@code count} points on a circle of the radius, starting at the angle {@code phase}. Returns {x, z} pairs. */
    public static double[][] circle(int count, double radius, double phase) {
        double[][] out = new double[Math.max(0, count)][];
        for (int i = 0; i < count; i++) {
            double a = phase + 2.0 * Math.PI * i / count;
            out[i] = new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
        }
        return out;
    }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }
}
