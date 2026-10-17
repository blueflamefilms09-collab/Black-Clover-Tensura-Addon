package com.newuniverse.nusmp.avatar.aptitude;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the "Randomize Aptitude" button is the free gamble that balances the tax on a hand pick, and its result must be decided by the SERVER (a client that
 * rolls for itself could always "roll" a Legendary). This is the pure maths of that roll. The five rarity classes keep their base weights from
 * {@link Aptitude.Rarity#weight()} (60 / 25 / 10 / 4 / 1: they add up to 100, so they read as percentages), and the creator's stored luck shifts them
 * towards the rarer classes: a class of rank r (0 = common ... 4 = legendary) is multiplied by {@code exp(strength * luck * r)}, so luck 0 leaves the
 * base odds exactly as they are, positive luck lifts the rare classes more the rarer they are, and negative luck pushes the other way. A class is chosen
 * first, then one of its aptitudes with equal chance, so the number of aptitudes a pack ships per class never changes the class odds. No Minecraft types.
 */
public final class AptitudeRoll {
    private AptitudeRoll() {}

    /** Luck outside this range is clamped before it is used, so a modified attribute cannot make the odds absurd. */
    public static final double LUCK_MIN = -20.0, LUCK_MAX = 40.0;

    private static double clampLuck(double luck) { return Double.isNaN(luck) ? 0.0 : Math.max(LUCK_MIN, Math.min(LUCK_MAX, luck)); }

    private static double clampStrength(double s) { return Double.isNaN(s) ? 0.0 : Math.max(0.0, Math.min(1.0, s)); }

    /** The weight of one rarity class after the luck shift. */
    public static double classWeight(Aptitude.Rarity rarity, double luck, double strength) {
        return rarity.weight() * Math.exp(clampStrength(strength) * clampLuck(luck) * rarity.ordinal());
    }

    /**
     * The chance of each rarity class for this pool, in {@link Aptitude.Rarity#values()} order. A class with no aptitude in the pool has chance 0 and the rest
     * share its weight; an empty pool gives all zeros. The chances add up to 1 otherwise.
     */
    public static double[] odds(List<Aptitude> pool, double luck, double strength) {
        Aptitude.Rarity[] all = Aptitude.Rarity.values();
        double[] w = new double[all.length];
        double sum = 0.0;
        for (int i = 0; i < all.length; i++) {
            boolean present = false;
            if (pool != null) for (Aptitude a : pool) if (a != null && a.rarity() == all[i]) { present = true; break; }
            w[i] = present ? classWeight(all[i], luck, strength) : 0.0;
            sum += w[i];
        }
        if (sum <= 0.0) return new double[all.length];
        for (int i = 0; i < w.length; i++) w[i] /= sum;
        return w;
    }

    /** The aptitude the roll lands on, or null when the pool is empty. The caller removes aptitudes that must not come up (hand picks) from the pool first. */
    public static Aptitude pick(List<Aptitude> pool, double luck, double strength, Random rng) {
        double[] p = odds(pool, luck, strength);
        Aptitude.Rarity[] all = Aptitude.Rarity.values();
        double u = rng.nextDouble(), acc = 0.0;
        int cls = -1;
        for (int i = 0; i < p.length; i++) {
            if (p[i] <= 0.0) continue;
            cls = i;                                   // the last class with chance takes the rounding remainder
            acc += p[i];
            if (u < acc) break;
        }
        if (cls < 0) return null;
        List<Aptitude> inClass = new ArrayList<>();
        for (Aptitude a : pool) if (a != null && a.rarity() == all[cls]) inClass.add(a);
        return inClass.get(rng.nextInt(inClass.size()));
    }
}
