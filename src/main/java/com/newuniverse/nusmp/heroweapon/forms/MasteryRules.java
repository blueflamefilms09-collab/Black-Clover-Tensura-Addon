package com.newuniverse.nusmp.heroweapon.forms;

/**
 * Why: mastery is the "use it and it becomes yours" half of a form (design.md section 6): a point per blow, two per technique, one per successful block with the
 * form active, up to the form's own total, and when the total is reached the form is finished once and pays its permanent bonus. The counting and the
 * "finished exactly once" edge are the parts that break quietly (a form that finishes every blow, or never), so they are pure functions with a test, and
 * {@link FormMastery} only moves the numbers in and out of the bond.
 */
public final class MasteryRules {
    private MasteryRules() {}

    /** The result of adding points: the new total, and whether this very step reached the form's total. */
    public record Step(int value, boolean finished) {}

    /**
     * Adds points to a form's mastery.
     *
     * @param current the points so far (any value; below 0 counts as 0, above the cap as the cap)
     * @param points  the points to add (a negative number adds nothing)
     * @param cap     the form's {@code mastery}; a form with a cap of 0 or less has no mastery and always answers 0
     */
    public static Step add(int current, int points, int cap) {
        if (cap <= 0) return new Step(0, false);
        int now = Math.max(0, Math.min(cap, current));
        if (now >= cap) return new Step(cap, false);
        int next = (int) Math.min((long) cap, (long) now + Math.max(0, points));
        return new Step(next, next >= cap);
    }

    /** Share of the way to finishing, 0 to 1 (1 for a form without mastery). */
    public static double share(int value, int cap) {
        if (cap <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, value / (double) cap));
    }

    /** The weapon experience handed out for a one-off event of a form of this tier: {@code perTier} for tier 1 and below, then growing with the tier. */
    public static long reward(int perTier, int tier) { return (long) Math.max(0, perTier) * Math.max(1, tier); }
}
