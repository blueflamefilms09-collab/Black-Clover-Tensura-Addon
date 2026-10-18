package com.newuniverse.nusmp.elite.world.rules;

/**
 * Why: the five stages of Magic Ore growth, in the owner's order (entries are only ever appended): Small Bud, Medium Bud, Large Bud, Cluster, Mother Rock. The numbers are the mod's own:
 * {@code weight} is what a block of that stage adds to the chunk's crystal score (the density bonus), {@code growNeed} is the density a block of that stage needs before it can ripen to the
 * next, {@code chanceFactor} slows the later stages down. The Mother Rock is the last stage and is handled by {@link CrystalGrowth} (charge and seeding), it has no "next".
 */
public enum CrystalStage {
    SMALL_BUD("magic_bud_small", 1, 0.15, 1.0),
    MEDIUM_BUD("magic_bud_medium", 2, 0.30, 0.9),
    LARGE_BUD("magic_bud_large", 3, 0.45, 0.75),
    CLUSTER("magic_crystal_cluster", 5, 0.80, 0.30),
    MOTHER_ROCK("mother_rock", 0, 1.00, 0.0);

    public final String blockId;
    public final int weight;
    public final double growNeed, chanceFactor;

    CrystalStage(String blockId, int weight, double growNeed, double chanceFactor) {
        this.blockId = blockId;
        this.weight = weight;
        this.growNeed = growNeed;
        this.chanceFactor = chanceFactor;
    }

    /** The stage after this one, or null for the last. */
    public CrystalStage next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }

    public boolean isBud() { return this == SMALL_BUD || this == MEDIUM_BUD || this == LARGE_BUD; }
}
