package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: canon leaves (docs/grimoire_canon_leaves.md) change what every grimoire says, costs and hits for, and nobody can watch that in a game here, so the CI server smoke test checks the rules on the live SERVER
 * settings: {@code canon.meaning} (every cover has a line, the two legendary covers are marked mod-only), {@code canon.ceremony} (the five-leaf weight is 0 with canon on and moves to the four-leaf, the sum stays),
 * {@code canon.luck} (a luck cover with the injected random: never at chance 0, always at chance 1, only luck covers), {@code canon.price} (the devil's cost factor, never lethal, rate limit) and
 * {@code canon.mastery} (cost and damage factors finite and at least x1, monotone in the wielder's EP). Server safe, no client class, no player and nothing left in the world.
 */
public final class CanonLeavesSelfTests {
    private CanonLeavesSelfTests() {}

    public static void register(IEventBus bus) {
        SelfTests.register("canon.meaning", s -> meaning());
        SelfTests.register("canon.ceremony", s -> ceremony());
        SelfTests.register("canon.luck", s -> luck());
        SelfTests.register("canon.price", s -> price());
        SelfTests.register("canon.mastery", s -> mastery());
        CanonLeaves.register(bus);
    }

    private static Result meaning() {
        int modOnly = 0;
        for (GrimoireCover c : GrimoireCover.values()) {
            if (GrimoireMeaning.text(c).isBlank() || !GrimoireMeaning.key(c).startsWith("anime_multiverse.grimoire.meaning.")) return Result.fail("no meaning for " + c);
            if (GrimoireMeaning.modOnly(c)) { modOnly++; if (!c.isLegendary() || !GrimoireMeaning.text(c).startsWith("Mod-only")) return Result.fail(c + " is marked wrong"); }
        }
        return modOnly == 2 ? Result.ok(GrimoireCover.values().length + " covers have a meaning, 2 are mod-only") : Result.fail(modOnly + " mod-only covers");
    }

    private static Result ceremony() {
        boolean canon = CanonLeaves.on();
        double[] w = CanonLeaves.tierWeights(80, 16, 3.5, 0.5, 0.1, canon);
        double sum = w[0] + w[1] + w[2] + w[3] + w[4];
        if (Math.abs(sum - 100.1) > 1e-9) return Result.fail("the weights do not add up: " + sum);
        if (canon && (w[2] != 0.0 || w[1] != 19.5)) return Result.fail("the five-leaf weight did not move to the four-leaf");
        if (!canon && (w[2] != 3.5 || w[1] != 16)) return Result.fail("off changed the weights");
        return Result.ok("canonLeaves=" + canon + ": five-leaf weight " + w[2] + ", four-leaf " + w[1]);
    }

    private static Result luck() {
        double chance = CanonLeaves.luckChance(), mult = CanonLeaves.luckMult();
        if (CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 0.0, mult, () -> 0.0) != 1.0) return Result.fail("chance 0 gave luck");
        if (CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 1.0, 1.5, () -> 0.999999) != 1.5) return Result.fail("chance 1 did not give luck");
        if (CanonLeaves.luckFactor(GrimoireCover.THREE_LEAF, true, 1.0, 1.5, () -> 0.0) != 1.0) return Result.fail("a three-leaf got luck");
        if (CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, false, 1.0, 1.5, () -> 0.0) != 1.0) return Result.fail("canon off still gave luck");
        if (!(chance >= 0.0 && chance <= 1.0 && mult >= 1.0)) return Result.fail("the settings are out of range: " + chance + " / " + mult);
        return Result.ok("luck " + chance + " x" + mult + " on the four-leaf and the five-sided diamond only");
    }

    private static Result price() {
        double share = CanonLeaves.devilShare();
        double f = CanonLeaves.devilCostFactor(GrimoireCover.FIVE_LEAF, share);
        if (!(f >= 1.0 && Double.isFinite(f))) return Result.fail("devil cost factor " + f);
        if (CanonLeaves.devilCostFactor(GrimoireCover.GOD_TIER, share) != 1.0 || CanonLeaves.devilCostFactor(GrimoireCover.FOUR_LEAF, share) != 1.0) return Result.fail("a cover without a devil pays");
        if (!CanonLeaves.paysDevil(GrimoireCover.BLACK_MAGIC) || !CanonLeaves.paysDevil(GrimoireCover.TRIPLE_SPADE)) return Result.fail("black magic or the triple spade does not pay");
        double hit = CanonLeaves.healthPrice(20.0, 20.0, 1.0, 100.0, 1000L, 0L);
        if (hit <= 0.0 || hit > 0.4 + 1e-9) return Result.fail("price " + hit);
        if (CanonLeaves.healthPrice(2.0, 20.0, 0.0, 100.0, 1000L, 0L) != 0.0) return Result.fail("a lethal price");
        if (CanonLeaves.healthPrice(20.0, 20.0, 1.0, 100.0, 1005L, 1000L) != 0.0) return Result.fail("not rate limited");
        if (CanonLeaves.healthPrice(20.0, 20.0, 50.0, 100.0, 1000L, 0L) != 0.0) return Result.fail("paid with plenty of mana");
        return Result.ok("devilMaxManaShare=" + share + ": cost x" + f + ", price " + hit + " hp, never lethal, once a second, " + CanonLeaves.tracked() + " tracked");
    }

    private static Result mastery() {
        double last = 1.0;
        for (GrimoireCover c : GrimoireCover.values()) {
            for (double ep : new double[]{0, 1e3, 1e4, 1e5, 1e6, 1e7, 1e12, Double.NaN}) {
                double cost = GrimoireMastery.costFactor(c, ep, 1.5), dmg = GrimoireMastery.damageFactor(c, ep, 0.12);
                if (!(Double.isFinite(cost) && cost >= 1.0 && cost <= 1.5 && Double.isFinite(dmg) && dmg >= 1.0 && dmg <= 1.12)) return Result.fail(c + " at " + ep + " EP: cost " + cost + ", damage " + dmg);
            }
        }
        double a = GrimoireMastery.costFactor(GrimoireCover.FIVE_LEAF, 1e3, 1.5), b = GrimoireMastery.costFactor(GrimoireCover.FIVE_LEAF, 1e5, 1.5);
        if (a <= b - 1e-12 && a != b) return Result.fail("the cost does not fall with the level");
        if (!(GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e7, 0.12) > GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e4, 0.12))) return Result.fail("the bonus does not rise with the level");
        return Result.ok("five-leaf cost x" + String.format(java.util.Locale.ROOT, "%.3f", a) + " at 1k EP, x" + String.format(java.util.Locale.ROOT, "%.3f", b) + " at 100k; factors in 1.." + last);
    }
}
