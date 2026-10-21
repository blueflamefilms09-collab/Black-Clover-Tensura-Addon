package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;

/**
 * Why: "coins must drop from dungeon mobs, chests and bosses", scaled by how deep the floor is, is a balance question, and a balance question belongs in a pure class that a unit test can pin:
 * DEEPER PAYS MORE in every source, the Hub (floor 0) pays nothing, a chest pays more inside a Secret Rift pocket, a boss always pays, and a mob pays on a chance that Looting raises. The roll
 * returns an AMOUNT in bronze coins (the unit of {@link CoinKind}); {@link CoinMath#mix} turns it into the coin stacks that drop (a payout of 40,000 bronze drops 4 gold coins, never 400 silver
 * ones), so nothing is lost to rounding. The game side is {@link CoinLootModifier} (global loot modifier for mobs and chests) and {@link CoinBossDrops} (the decennial
 * Guardian, which rolls no loot table). Depth uses the same five tiers of 200 floors as the Mob Foundry ({@code FoundryScaling.tierOf}, a test compares the two) so "Tier 3" means one thing
 * everywhere: the payout grows by {@code tierGrowth} per tier and by up to {@code floorGrowth} (a fraction) from the first to the last floor of a tier, which stays below the step to
 * the next tier so the last floor of a tier never pays more than the first floor of the next (the test proves it). Numbers are a first pass nobody has played; every one is a config value.
 */
public final class CoinLoot {
    private CoinLoot() {}

    /** Where the money comes from. */
    public enum Source { MOB, CHEST, RIFT_CHEST, BOSS }

    /** Floors per tier and the number of tiers (the Mob Foundry's rule). */
    public static final int FLOORS_PER_TIER = 200, TIERS = 5;

    /**
     * The tunable numbers. {@code mobBase}, {@code chestBase} and {@code bossBase} are the payout on the first floor, in bronze coins; {@code tierGrowth} multiplies it per tier;
     * {@code floorGrowth} adds up to that fraction over a tier; {@code riftMultiplier} multiplies a chest inside a rift pocket; {@code lootingBonus} adds that fraction per Looting level;
     * {@code scale} multiplies everything (the server owner's one knob).
     */
    public record Params(double mobChance, long mobBase, long chestBase, long bossBase, double riftMultiplier, double tierGrowth, double floorGrowth, double lootingBonus, double scale) {
        public Params {
            mobChance = clamp(mobChance, 0.0, 1.0);
            mobBase = Math.max(0, mobBase);
            chestBase = Math.max(0, chestBase);
            bossBase = Math.max(0, bossBase);
            riftMultiplier = clamp(riftMultiplier, 1.0, 100.0);
            tierGrowth = clamp(tierGrowth, 1.0, 10.0);
            floorGrowth = clamp(floorGrowth, 0.0, Math.min(1.0, (tierGrowth - 1.0) * 0.9));          // always less than the step to the next tier
            lootingBonus = clamp(lootingBonus, 0.0, 1.0);
            scale = clamp(scale, 0.0, 100.0);
        }
    }

    /** The shipped numbers: a tier-1 mob leaves about 6 bronze half the time, a tier-1 chest 80 bronze (rift pockets x3), a tier-1 decennial boss 15 silver; x3 per tier. */
    public static final Params DEFAULTS = new Params(0.55, 6, 80, 1500, 3.0, 3.0, 0.5, 0.10, 1.0);

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }

    /** The tier (1..5) of a signed floor; 0 for the Hub. */
    public static int tierOf(int floor) {
        return floor == 0 ? 0 : Math.min(TIERS, (Math.abs(floor) - 1) / FLOORS_PER_TIER + 1);
    }

    /** How far down its tier a floor is: 0 on the first floor of the tier, 1 on the last (a floor past 1000 counts as the last). */
    public static double depth(int floor) {
        if (floor == 0) return 0.0;
        int a = Math.abs(floor);
        if (a > TIERS * FLOORS_PER_TIER) return 1.0;
        return ((a - 1) % FLOORS_PER_TIER) / (double) (FLOORS_PER_TIER - 1);
    }

    /** The chance that a mob of this floor pays at all: the base chance and 5 points per Looting level, never above 1. Zero in the Hub. */
    public static double mobChance(int floor, int looting, Params p) {
        if (floor == 0 || p.scale() <= 0.0) return 0.0;
        return Math.min(1.0, p.mobChance() + 0.05 * Math.max(0, looting));
    }

    /** The middle of the payout (no jitter, no chance) for a source on a floor, in bronze: base x tierGrowth^(tier-1) x (1 + floorGrowth x depth) x scale. Zero in the Hub. */
    public static double expected(Source source, int floor, Params p) {
        if (floor == 0) return 0.0;
        double base = switch (source) {
            case MOB -> p.mobBase();
            case CHEST -> p.chestBase();
            case RIFT_CHEST -> p.chestBase() * p.riftMultiplier();
            case BOSS -> p.bossBase();
        };
        int tier = tierOf(floor);
        return base * Math.pow(p.tierGrowth(), tier - 1) * (1.0 + p.floorGrowth() * depth(floor)) * p.scale();
    }

    /**
     * One payout in bronze coins (0 = nothing). A mob pays on its chance, a chest and a boss always. The amount is the expected value times a jitter (0.6 to 1.4 for mobs and chests, 0.9 to 1.1
     * for a boss) and times {@code 1 + lootingBonus x looting} (mobs and chests; a boss ignores Looting), rounded, at least 1 when a payout happens and the base is not zero.
     * This is the roll with no Luck; {@link #roll(Source, int, int, double, LuckGauss.Params, Params, Dice)} is the same roll with Luck.
     */
    public static long roll(Source source, int floor, int looting, Params p, Dice dice) {
        return roll(source, floor, looting, 0.0, LuckGauss.COINS, p, dice);
    }

    /**
     * 1.107.0: the same roll with the payer's Luck. Luck adds to a mob's chance of paying (1 point per point, at most 15) and multiplies the amount by a Gaussian factor that is never
     * below 1 ({@link LuckGauss#factor}, the Luck-only curve: the floor already scales the payout). With no Luck, or {@code luckParams} that do nothing, no extra random number is drawn,
     * so the result is exactly the old roll's.
     */
    public static long roll(Source source, int floor, int looting, double luck, LuckGauss.Params luckParams, Params p, Dice dice) {
        if (floor == 0 || p.scale() <= 0.0) return 0;
        double l = LuckGauss.luck(luck);
        if (source == Source.MOB && !dice.chance(Math.min(1.0, mobChance(floor, looting, p) + LuckGauss.coinChanceBonus(l)))) return 0;
        double mid = expected(source, floor, p);
        if (mid <= 0.0) return 0;
        double spread = source == Source.BOSS ? 0.1 : 0.4;
        double jitter = 1.0 - spread + 2.0 * spread * dice.next();
        double lootBoost = source == Source.BOSS ? 1.0 : 1.0 + p.lootingBonus() * Math.max(0, looting);
        double luckBoost = l > 0.0 && luckParams != null ? LuckGauss.factor(l, dice, luckParams) : 1.0;
        double v = mid * jitter * lootBoost * luckBoost;
        if (v >= CoinMath.CAP) return CoinMath.CAP;
        return Math.max(1L, Math.round(v));
    }
}
