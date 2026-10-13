package com.newuniverse.nusmp.dungeon.sentient;

/**
 * What the dead of the Dungeon leave: ash and Magic Stones. The grade of a mob (its tier, or a boss, or a Juggernaut) sets how many stones
 * and crystals come; Looting adds one to the most per level; the server's stoneDrops setting scales the lot (stochastic rounding, so 0.5 is
 * half the stones on average). Mobs killed by anything but a player leave a stone one time in four. Pure.
 */
public final class StoneTable {
    private StoneTable() {}

    public enum Grade { TIER1, TIER2, TIER3, JUGGERNAUT, BOSS }

    /** What drops: Magic Stones and low, medium and high quality magic crystals. */
    public record Haul(int stones, int low, int medium, int high) {
        public static final Haul NONE = new Haul(0, 0, 0, 0);

        public boolean isEmpty() { return stones == 0 && low == 0 && medium == 0 && high == 0; }
    }

    /** The share of non-player deaths that leave one stone. */
    public static final double AMBIENT_CHANCE = 0.25;

    /** The grade of a mob: a floor boss and a Juggernaut by their marks, the rest by the roster tier, or by health when it has none (under 20 is tier 1, under 60 tier 2, else tier 3). */
    public static Grade gradeOf(int wildTier, double maxHealth, boolean boss, boolean juggernaut) {
        if (boss) return Grade.BOSS;
        if (juggernaut) return Grade.JUGGERNAUT;
        if (wildTier >= 3) return Grade.TIER3;
        if (wildTier == 2) return Grade.TIER2;
        if (wildTier == 1) return Grade.TIER1;
        if (maxHealth < 20.0) return Grade.TIER1;
        return maxHealth < 60.0 ? Grade.TIER2 : Grade.TIER3;
    }

    /** The fewest and the most stones of a grade, before Looting. */
    public static int least(Grade grade) {
        return switch (grade) {
            case TIER1 -> 1;
            case TIER2 -> 1;
            case TIER3 -> 2;
            case JUGGERNAUT -> 3;
            case BOSS -> 5;
        };
    }

    public static int most(Grade grade) {
        return switch (grade) {
            case TIER1 -> 1;
            case TIER2 -> 2;
            case TIER3 -> 3;
            case JUGGERNAUT -> 5;
            case BOSS -> 8;
        };
    }

    /** One player kill: stones from the grade's range (Looting adds to the most), crystals by the grade, all scaled. */
    public static Haul roll(Grade grade, int looting, double scale, Dice dice) {
        int lo = least(grade), hi = most(grade) + Math.max(0, looting);
        int stones = lo + dice.nextInt(hi - lo + 1);
        int low = 0, medium = 0, high = 0;
        switch (grade) {
            case TIER3 -> low = dice.chance(0.25) ? 1 : 0;
            case JUGGERNAUT -> medium = 1;
            case BOSS -> {
                medium = 1;
                high = dice.chance(0.15) ? 1 : 0;
            }
            default -> { }
        }
        return new Haul(scaled(stones, scale, dice), scaled(low, scale, dice), scaled(medium, scale, dice), scaled(high, scale, dice));
    }

    /** A death that no player caused: one stone one time in four (and no more often if the stoneDrops setting is below 1). */
    public static Haul ambient(double scale, Dice dice) {
        if (!dice.chance(AMBIENT_CHANCE)) return Haul.NONE;
        return new Haul(scaled(1, Math.min(1.0, scale), dice), 0, 0, 0);
    }

    /** n times the scale, the fraction rounded up with the chance of the fraction. */
    static int scaled(int n, double scale, Dice dice) {
        if (n <= 0 || scale <= 0.0) return 0;
        double x = n * scale;
        int whole = (int) Math.floor(x);
        return whole + (dice.next() < x - whole ? 1 : 0);
    }
}
