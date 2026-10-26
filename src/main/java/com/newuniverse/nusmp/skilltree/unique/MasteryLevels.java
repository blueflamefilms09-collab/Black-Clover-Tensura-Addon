package com.newuniverse.nusmp.skilltree.unique;

/**
 * Why: with the Skill Tree gone there are no Skill Points to buy node levels with, but the line data still has per-level tables (an art that hits harder at level 3, a stat bonus that grows). The unique skill
 * keeps them honest with Tensura's own progression: the skill's MASTERY (0 to 100 percent of its maximum, earned by using the art and by keeping the skill on) sets the level every node counts at, and sets which
 * of the skill's arts have awakened. This pure class holds the two formulas so they are one rule for the engine, the skill, the tooltip and the tests. Level: 1 at no mastery, rising by one for every
 * {@code 1/maxLevel} of mastery, never above the node's maximum level (a fully mastered skill counts every node at its top level). Arts: the first art (the signature art, first in the route file) is awake from the
 * start; art number {@code k} of {@code n} awakens at {@code k/n} of the mastery, so the last of ten arts needs 90 percent. All inputs are clamped, so a broken instance (NaN, negative, above the maximum)
 * can never produce a level of zero or an index out of range.
 */
public final class MasteryLevels {
    private MasteryLevels() {}

    /** A mastery fraction in 0..1 from the instance's current and maximum mastery; 0 when the maximum is not positive or either number is not finite. */
    public static double fraction(double mastery, double maxMastery) {
        if (!(maxMastery > 0.0) || Double.isNaN(mastery) || Double.isInfinite(mastery) || Double.isInfinite(maxMastery)) return 0.0;
        return Math.max(0.0, Math.min(1.0, mastery / maxMastery));
    }

    /** The level a node counts at for a mastery fraction: 1 up to {@code maxLevel} (a maximum below 1 counts as 1). */
    public static int level(double fraction, int maxLevel) {
        int max = Math.max(1, maxLevel);
        double f = Double.isNaN(fraction) ? 0.0 : Math.max(0.0, Math.min(1.0, fraction));
        return Math.max(1, Math.min(max, 1 + (int) Math.floor(f * max)));
    }

    /** The mastery fraction at which art number {@code index} (0 based) of {@code count} awakens; the signature art (index 0) needs none. */
    public static double threshold(int index, int count) {
        if (count <= 1 || index <= 0) return 0.0;
        return Math.min(1.0, (double) index / count);
    }

    /** True when art {@code index} of {@code count} is awake at this mastery. */
    public static boolean awake(int index, int count, double fraction) {
        if (index < 0 || index >= Math.max(1, count)) return false;
        double f = Double.isNaN(fraction) ? 0.0 : fraction;
        return f + 1e-9 >= threshold(index, count);
    }

    /** 3.26.0 (Evolution skills): true when the skill is MASTERED, i.e. at its full mastery (what the Ultimate Evolution art asks for on top of the evolution requirement). */
    public static boolean mastered(double fraction) { return !Double.isNaN(fraction) && fraction + 1e-9 >= 1.0; }

    /** The whole percent of mastery art {@code index} of {@code count} needs (0 to 100), rounded up. */
    public static int percentNeeded(int index, int count) { return (int) Math.ceil(threshold(index, count) * 100.0 - 1e-9); }

    /** The mode a press of the key uses: the mode number wrapped into 0..count-1 (a skill with no art has no mode: -1). */
    public static int wrapMode(int mode, int count) { return count <= 0 ? -1 : Math.floorMod(mode, count); }
}
