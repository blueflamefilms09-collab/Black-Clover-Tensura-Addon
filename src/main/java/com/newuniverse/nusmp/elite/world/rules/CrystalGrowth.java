package com.newuniverse.nusmp.elite.world.rules;

/**
 * Why: the growth rules of the Magic Ore crystals as pure functions, so the "budding amethyst" pattern (a Mother Rock seeds buds, buds ripen on random ticks) can be tested and tuned
 * without a world. Everything is a chance per RANDOM TICK of the block (about once a minute per block at the default random tick speed), driven by the chunk's magicule density.
 *
 * <ul>
 * <li>A bud ripens to the next stage only above that stage's {@link CrystalStage#growNeed}; the chance rises from a quarter of the base at the threshold to the full base at density 1.</li>
 * <li>A cluster becomes a Mother Rock only in very dense chunks, only next to other crystals (a grove, not a lone cluster), only while the chunk has room for another Mother Rock, and rarely.</li>
 * <li>A Mother Rock has a charge 0..{@value #MAX_CHARGE}: it gains charge with the density, spends one charge to seed a bud nearby, and raises the chunk's density while it lives.</li>
 * </ul>
 */
public final class CrystalGrowth {
    private CrystalGrowth() {}

    public static final int MAX_CHARGE = 3;
    /** Crystals around a cluster (within the grove radius) needed for it to mature into a Mother Rock. */
    public static final int GROVE_NEIGHBOURS = 3;
    /** The density a cluster needs to mature into a Mother Rock. */
    public static final double MOTHER_DENSITY = 0.85;
    /** The chance of a qualifying cluster to turn into a Mother Rock, per random tick, at its best. */
    public static final double MOTHER_CHANCE = 0.04;

    /** Chance per random tick that a bud ripens to the next stage (0 when it cannot, and always 0 for the cluster, whose step has its own rule). */
    public static double growChance(CrystalStage from, double density, double baseChance) {
        if (from == null || from.next() == null || from == CrystalStage.CLUSTER) return 0.0;
        double need = from.growNeed;
        double d = MagiculeModel.clamp01(density);
        if (d < need || need >= 1.0 || baseChance <= 0.0) return 0.0;
        double k = (d - need) / (1.0 - need);
        double c = baseChance * (0.25 + 0.75 * k) * from.chanceFactor;
        return Math.max(0.0, Math.min(1.0, c));
    }

    /** May this cluster mature into a Mother Rock now? {@code roll} is a uniform 0..1 number. */
    public static boolean motherReady(double density, int neighbourCrystals, int rocksInChunk, int rockCap, double baseChance, double roll) {
        if (MagiculeModel.clamp01(density) < MOTHER_DENSITY) return false;
        if (neighbourCrystals < GROVE_NEIGHBOURS || rocksInChunk >= Math.max(0, rockCap)) return false;
        double c = Math.max(0.0, Math.min(1.0, baseChance * (0.5 + 0.5 * ((density - MOTHER_DENSITY) / (1.0 - MOTHER_DENSITY)))));
        return roll < c;
    }

    /** The chance per random tick that a Mother Rock gains one charge. Zero when it is full. */
    public static double chargeChance(int charge, double density) {
        if (charge >= MAX_CHARGE) return 0.0;
        return 0.10 + 0.40 * MagiculeModel.clamp01(density);
    }

    /** The chance per random tick that a Mother Rock with this charge seeds a bud (zero without charge). {@code base} is the config rate. */
    public static double seedChance(int charge, double density, double base) {
        if (charge <= 0 || base <= 0.0) return 0.0;
        return Math.min(1.0, base * (0.5 + 0.5 * MagiculeModel.clamp01(density)) * (0.5 + 0.25 * Math.min(charge, MAX_CHARGE)));
    }

    /** The density bonus of a chunk's Mother Rocks: each one gives 0.06, each point of charge 0.02, never more than the cap of the model. */
    public static double motherBonus(int rocks, int chargeSum) {
        if (rocks <= 0) return 0.0;
        return Math.min(MagiculeModel.MOTHER_CAP, 0.06 * rocks + 0.02 * Math.max(0, chargeSum));
    }

    /** A bud sprouts only on a free face: air or water next to a solid block. */
    public static boolean canSeed(boolean spotFree, boolean hasSolidNeighbour) { return spotFree && hasSolidNeighbour; }
}
