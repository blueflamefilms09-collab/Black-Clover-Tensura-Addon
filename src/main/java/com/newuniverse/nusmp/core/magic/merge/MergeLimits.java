package com.newuniverse.nusmp.core.magic.merge;

/** The fixed limits of the merge generator. */
public final class MergeLimits {
    private MergeLimits() {}

    /** Merged magics per world. */
    public static final int MAX_MERGED = 64;
    public static final int MIN_PARENTS = 2, MAX_PARENTS = 4;
    public static final int MIN_NAME = 3, MAX_NAME = 32;
    /** Every cost, cooldown and mastery need of a merged magic is the largest of its parents' times this. */
    public static final double COST_FACTOR = 1.15;
    /** A merged page never costs more than this percent of max magicule, whatever the parents. */
    public static final double COST_CAP_PERCENT = 60;
}
