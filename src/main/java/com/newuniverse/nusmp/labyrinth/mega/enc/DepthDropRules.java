package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: "the dungeon mob drops get better the deeper or higher you go" is, like the mobs' strength, a property of a few curves of the same one input, the DEPTH of the floor
 * ({@link DepthRules#depthOf}, the distance from the Hub, the same up and down), so it is kept as plain arithmetic that a unit test can pin: more depth never pays less, every curve has a ceiling,
 * the first floor is plain. Four things grow, each on the saturating curve {@code max * x / (x + half)}, {@code x = depth - 1}: the number of EXTRA ROLLS of the mob's own loot table (so a deep
 * monster leaves its whole drop list again, with variety, not just bigger stacks), the BONUS ON EVERY STACK that drops (a fraction of the count, rounded by chance), the LUCK of those extra rolls
 * (which moves the quality-weighted entries of a loot table toward their better results), and the EXPERIENCE of the kill. The coins (CoinLoot, x3 per tier) and the equipment (GearRoller, the
 * grade mix moves with the depth) already scale with depth by their own rules and are NOT touched here. Pure.
 */
public final class DepthDropRules {
    private DepthDropRules() {}

    /** The tunable ceilings (and the scale that multiplies all of them). */
    public record Params(double rollsMax, double countMax, double luckMax, double xpMax, double scale) {
        public Params {
            rollsMax = clamp(rollsMax, 0.0, 20.0);
            countMax = clamp(countMax, 0.0, 10.0);
            luckMax = clamp(luckMax, 0.0, 20.0);
            xpMax = clamp(xpMax, 0.0, 20.0);
            scale = clamp(scale, 0.0, 10.0);
        }
    }

    /** The shipped ceilings: up to 3 extra rolls, +100 percent on every stack, +3 luck, +300 percent experience. */
    public static final Params DEFAULTS = new Params(3.0, 1.0, 3.0, 3.0, 1.0);

    /** Half-way depths of the four curves. */
    public static final double ROLLS_HALF = 200.0, COUNT_HALF = 400.0, LUCK_HALF = 300.0, XP_HALF = 300.0;

    /** The most extra stacks one death may add in total, whatever the rolls produce (a server must not be flooded by one kill). */
    public static final int MAX_EXTRA_STACKS = 24;

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    private static double curve(int depth, double max, double half, double scale) {
        double x = Math.max(0, depth - 1);
        return max * scale * x / (x + half);
    }

    /** The expected number of extra rolls of the mob's loot table at this depth (0 on the first floor). */
    public static double bonusRolls(int depth, Params p) { return curve(depth, p.rollsMax(), ROLLS_HALF, p.scale()); }

    /** The whole number of extra rolls for one kill: the integer part of {@link #bonusRolls}, plus one more with the fractional part as the chance ({@code r} is a uniform 0..1). */
    public static int extraRolls(int depth, Params p, double r) {
        double b = bonusRolls(depth, p);
        int n = (int) Math.floor(b);
        return n + (r < b - n ? 1 : 0);
    }

    /** The fraction added to every stack's count at this depth (0.5 = half as many again). */
    public static double countBonus(int depth, Params p) { return curve(depth, p.countMax(), COUNT_HALF, p.scale()); }

    /**
     * The count of a stack after the bonus: {@code count + floor(count * bonus)}, plus one with the fractional part as the chance, never above {@code maxStack}. A stack that cannot stack
     * (maxStack 1: a weapon, a book) is returned as it is.
     */
    public static int boostedCount(int count, int maxStack, int depth, Params p, double r) {
        if (count <= 0 || maxStack <= 1) return count;
        double add = count * countBonus(depth, p);
        int n = (int) Math.floor(add);
        n += r < add - n ? 1 : 0;
        return Math.min(maxStack, count + n);
    }

    /** The luck added to the extra rolls (the same luck that the Luck effect gives a player). */
    public static float luckBonus(int depth, Params p) { return (float) curve(depth, p.luckMax(), LUCK_HALF, p.scale()); }

    /** The multiplier of the experience of a kill: 1 on the first floor, up to {@code 1 + xpMax}. */
    public static double xpMultiplier(int depth, Params p) { return 1.0 + curve(depth, p.xpMax(), XP_HALF, p.scale()); }

    /** The experience of a kill after the multiplier, rounded to the nearest whole point. */
    public static int boostedXp(int xp, int depth, Params p) { return xp <= 0 ? xp : (int) Math.round(xp * xpMultiplier(depth, p)); }
}
