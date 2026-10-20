package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

/**
 * Why: the rarity table of the rifts as pure arithmetic, so the owner's "mega-rare roughly one rift in a few hundred" is a number a test can pin and a server owner can retune. The spawner
 * (RiftService) still decides, as it always did, WHETHER a rift opens near a player and whether it is a pocket or a shortcut (the old chances stay); this table decides, for a rift that is
 * going to open, how rare it is. Three things move the chance: DEPTH (the deeper the floor, the likelier an ancient tomb: up to {@code depthBoost} times more at floor 1000), LUCK (the
 * player's Luck attribute, a few percent a point, never below half or above three times), and PITY (every rift that opened near this player without being mega-rare counts: past
 * {@code pityStart} the chance climbs on a square curve and at {@code pityHard} it is certain, so a player is never starved for ever and the average wait stays near the configured one).
 * No Minecraft type is used here.
 */
public final class RiftTiers {
    private RiftTiers() {}

    /** The least "one in N" a config may ask for (a server owner who wants every second rift to be mega-rare can still use the admin commands). */
    public static final double MIN_ONE_IN = 20.0;
    /** The most the chance may reach by depth and luck alone, before pity. */
    public static final double CAP = 0.5;

    /**
     * The knobs. {@code megaOneIn}: on average one rift in this many is mega-rare at depth 0 and luck 0; {@code pityStart} / {@code pityHard}: rifts without a mega-rare one at which the pity
     * curve starts and at which the next rift is certain; {@code depthBoost}: extra multiple at floor 1000 (1.0 = twice as likely); {@code luckStep}: change of the chance per point of Luck.
     */
    public record Config(double megaOneIn, int pityStart, int pityHard, double depthBoost, double luckStep) {
        public Config {
            megaOneIn = Double.isNaN(megaOneIn) ? 250.0 : Math.max(MIN_ONE_IN, megaOneIn);
            pityStart = Math.max(0, pityStart);
            pityHard = Math.max(pityStart + 1, pityHard);
            depthBoost = Double.isNaN(depthBoost) ? 1.0 : Math.max(0.0, Math.min(10.0, depthBoost));
            luckStep = Double.isNaN(luckStep) ? 0.08 : Math.max(0.0, Math.min(0.5, luckStep));
        }

        /** The defaults: one rift in 250, pity from the 150th to the certain 600th, twice as likely at floor 1000, 8 percent a point of Luck. */
        public static Config defaults() { return new Config(250.0, 150, 600, 1.0, 0.08); }
    }

    /** The multiple of the base chance that a floor (its absolute number) gives: 1 at the top, {@code 1 + depthBoost} at floor 1000 and deeper. */
    public static double depthFactor(Config c, int floor) {
        return 1.0 + c.depthBoost() * Math.min(1000, Math.max(0, Math.abs(floor))) / 1000.0;
    }

    /** The multiple a player's Luck gives, from 0.5 to 3. */
    public static double luckFactor(Config c, double luck) {
        return Math.max(0.5, Math.min(3.0, 1.0 + c.luckStep() * luck));
    }

    /** The chance (0..1) that a rift that is going to open is mega-rare, for a player with this many rifts behind them since their last mega-rare one. */
    public static double megaChance(Config c, int floor, double luck, int pity) {
        double base = 1.0 / c.megaOneIn();
        double p = Math.min(CAP, base * depthFactor(c, floor) * luckFactor(c, luck));
        int rifts = Math.max(0, pity);
        if (rifts >= c.pityHard()) return 1.0;
        if (rifts > c.pityStart()) {
            double k = (rifts - c.pityStart()) / (double) (c.pityHard() - c.pityStart());
            p = p + (1.0 - p) * k * k;
        }
        return Math.max(0.0, Math.min(1.0, p));
    }

    /**
     * The table of the three tiers for one rift that is going to open: {@code {common, rare, mega}}, summing to 1. The common/rare split follows the old chances (the shortcut and pocket chance
     * of a roll are in that proportion, so the plain game is unchanged); the mega share is {@link #megaChance} and comes out of both.
     */
    public static double[] table(Config c, double shortcutChance, double pocketChance, int floor, double luck, int pity) {
        double mega = megaChance(c, floor, luck, pity);
        double s = Math.max(0.0, shortcutChance), p = Math.max(0.0, pocketChance);
        double commonShare = s + p <= 0.0 ? 0.5 : s / (s + p);
        double rest = 1.0 - mega;
        return new double[]{rest * commonShare, rest * (1.0 - commonShare), mega};
    }

    /** The tier a roll {@code r} (0 inclusive to 1 exclusive) lands on in a table from {@link #table}. */
    public static RiftTier roll(double[] table, double r) {
        double acc = 0.0;
        for (int i = 0; i < table.length; i++) {
            acc += table[i];
            if (r < acc) return RiftTier.byOrdinal(i);
        }
        return RiftTier.COMMON;                                           // r at the very top of a table that sums to a hair under 1: the plain tear, never the rarest
    }

    /** The pity counter after a rift of this tier opened for the player: reset by a mega-rare one, else one more. Capped so it can never overflow. */
    public static int nextPity(int pity, RiftTier tier) {
        if (tier == RiftTier.MEGA_RARE) return 0;
        return Math.min(Integer.MAX_VALUE / 2, Math.max(0, pity) + 1);
    }

    /** Is a rift that is going to open mega-rare for a roll {@code r}? (The shortcut used by the spawner: one draw, not a whole table.) */
    public static boolean isMega(Config c, int floor, double luck, int pity, double r) { return r < megaChance(c, floor, luck, pity); }
}
