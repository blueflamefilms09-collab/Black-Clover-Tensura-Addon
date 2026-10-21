package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;

/**
 * Why: a backpack is a prize, so which one drops, and how often, is a balance rule that belongs in a pure class a test can pin: the CHANCE rises with Luck and with depth
 * ({@link LuckGauss#backpackChance}); the TIER that drops is capped by the floor (a first-floor chest never holds a netherite pack) and nudged up by Luck; and when something
 * drops it is a plain backpack more often than an upgrade, an upgrade is never higher than what the floor allows. A drop is at most ONE item per roll (a backpack or an
 * upgrade), so a chest cannot become a pile of them. Pure.
 */
public final class BackpackDrops {
    private BackpackDrops() {}

    /** What dropped: nothing, the plain backpack, or the upgrade item for a tier. */
    public record Drop(boolean backpack, BackpackTier upgrade) {
        public static final Drop NONE = new Drop(false, null);

        public boolean any() { return backpack || upgrade != null; }
    }

    /** The first floor (absolute) at which each tier may drop: leather from the start, iron 10, gold 40, diamond 120, netherite 300. */
    private static final int[] FIRST_FLOOR = {0, 10, 40, 120, 300};

    /** The best tier the floor allows. */
    public static BackpackTier cap(int floor) {
        int f = Math.abs(floor);
        BackpackTier best = BackpackTier.LEATHER;
        for (BackpackTier t : BackpackTier.values()) if (f >= FIRST_FLOOR[t.ordinal()]) best = t;
        return best;
    }

    /** The first floor at which a tier may drop. */
    public static int firstFloor(BackpackTier t) { return FIRST_FLOOR[t.ordinal()]; }

    /**
     * One roll. {@code rate} multiplies the chance (the server's knob). On a hit, a plain backpack drops 60% of the time (45% for a boss, which leaves an upgrade more often);
     * otherwise an upgrade of a tier from iron up to the floor's cap, drawn uniformly, and with Luck the draw may step one tier higher (4% per point of Luck, at most 40%) as long
     * as the cap still allows it. On the first floors, where the cap is leather, only the plain backpack can drop.
     */
    public static Drop roll(Dice dice, double luck, int floor, LuckGauss.Kind kind, double rate) {
        double chance = Math.min(LuckGauss.MAX_BACKPACK, LuckGauss.backpackChance(luck, floor, kind) * Math.max(0.0, rate));
        if (chance <= 0.0 || dice.next() >= chance) return Drop.NONE;
        BackpackTier cap = cap(floor);
        double plain = kind == LuckGauss.Kind.BOSS ? 0.45 : 0.60;
        if (cap == BackpackTier.LEATHER || dice.next() < plain) return new Drop(true, null);
        BackpackTier pick = BackpackTier.of(1 + dice.nextInt(cap.ordinal()));          // iron .. cap
        if (pick.hasNext() && pick.next().ordinal() <= cap.ordinal() && dice.next() < Math.min(0.40, 0.04 * LuckGauss.luck(luck))) pick = pick.next();
        return new Drop(false, pick);
    }
}
