package com.newuniverse.nusmp.celestial;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules: collection and completion, the contract, the summon gate, bond, cost / cooldown / power, control caps, Star Dress unlocks and the drop table. */
class CelestialRulesTest {
    private static int maskOf(CelestialKey... keys) {
        int m = 0;
        for (CelestialKey k : keys) m = CelestialRules.collect(m, k);
        return m;
    }

    // ------------------------------------------------------------------ collection
    @Test
    void completionNeedsAllTwelveGoldKeysAndSilverDoesNotCount() {
        int m = 0;
        for (CelestialKey k : CelestialKey.gold()) {
            assertFalse(CelestialRules.completes(m), "before " + k.id);
            m = CelestialRules.collect(m, k);
        }
        assertTrue(CelestialRules.completes(m));
        assertEquals(12, CelestialRules.goldCount(m));
        int silverOnly = maskOf(CelestialKey.HOROLOGIUM, CelestialKey.CRUX, CelestialKey.LYRA, CelestialKey.NIKORA, CelestialKey.SPIRIT_KING_GATE);
        assertFalse(CelestialRules.completes(silverOnly));
        assertEquals(0, CelestialRules.goldCount(silverOnly));
    }

    @Test
    void elevenGoldKeysAreNotComplete() {
        for (CelestialKey missing : CelestialKey.gold()) {
            int m = CelestialKey.GOLD_MASK & ~missing.bit();
            assertFalse(CelestialRules.completes(m), missing.id);
            assertEquals(1, CelestialRules.missingGold(m).size());
            assertSame(missing, CelestialRules.missingGold(m).get(0));
        }
        assertTrue(CelestialRules.missingGold(CelestialKey.GOLD_MASK).isEmpty());
    }

    // ------------------------------------------------------------------ bond
    @Test
    void bondLevelsRiseWithPointsAndStopAtFive() {
        assertEquals(0, CelestialRules.bondLevel(0));
        assertEquals(0, CelestialRules.bondLevel(2));
        assertEquals(1, CelestialRules.bondLevel(3));
        assertEquals(2, CelestialRules.bondLevel(8));
        assertEquals(3, CelestialRules.bondLevel(16));
        assertEquals(4, CelestialRules.bondLevel(30));
        assertEquals(5, CelestialRules.bondLevel(50));
        assertEquals(5, CelestialRules.bondLevel(10_000));
        assertEquals(3, CelestialRules.bondNext(0));
        assertEquals(-1, CelestialRules.bondNext(50));
    }

    // ------------------------------------------------------------------ the contract
    @Test
    void theContractRules() {
        assertEquals(CelestialRules.Bind.NOT_A_SPIRIT_KEY, CelestialRules.canBind(CelestialKey.SPIRIT_KING_GATE, false, "", "me", 1e6, 1e6));
        assertEquals(CelestialRules.Bind.NOT_A_SPIRIT_KEY, CelestialRules.canBind(null, false, "", "me", 1e6, 1e6));
        assertEquals(CelestialRules.Bind.SOMEONE_ELSES, CelestialRules.canBind(CelestialKey.LEO, false, "other", "me", 1e6, 1e6));
        assertEquals(CelestialRules.Bind.ALREADY_BOUND, CelestialRules.canBind(CelestialKey.LEO, true, "me", "me", 1e6, 1e6));
        assertEquals(CelestialRules.Bind.NOT_ENOUGH_MAGICULES, CelestialRules.canBind(CelestialKey.LEO, false, "", "me", 10, 10_000));
        assertEquals(CelestialRules.Bind.OK, CelestialRules.canBind(CelestialKey.LEO, false, "", "me", 10_000, 10_000));
        assertEquals(CelestialRules.Bind.OK, CelestialRules.canBind(CelestialKey.LEO, false, null, "me", 10_000, 10_000));
        assertEquals(CelestialRules.COST_FLOOR, CelestialRules.bindCost(100.0), 1e-9, "a small pool still pays the floor");
        assertEquals(300.0, CelestialRules.bindCost(10_000.0), 1e-9);
    }

    // ------------------------------------------------------------------ the summon gate
    @Test
    void theSummonIsRefusedForTheFirstReasonInOrder() {
        CelestialKey k = CelestialKey.TAURUS;
        assertEquals(CelestialRules.Refusal.NOT_A_SPIRIT_KEY, CelestialRules.check(CelestialKey.SPIRIT_KING_GATE, true, true, "", "me", 0, 1e9, 1, 0, 1));
        assertEquals(CelestialRules.Refusal.NO_KEY, CelestialRules.check(k, false, true, "", "me", 0, 1e9, 1, 0, 1));
        assertEquals(CelestialRules.Refusal.SOMEONE_ELSES, CelestialRules.check(k, true, true, "other", "me", 0, 1e9, 1, 0, 1));
        assertEquals(CelestialRules.Refusal.NOT_BOUND, CelestialRules.check(k, true, false, "", "me", 0, 1e9, 1, 0, 1));
        assertEquals(CelestialRules.Refusal.COOLDOWN, CelestialRules.check(k, true, true, "me", "me", 5, 1e9, 1, 0, 1));
        assertEquals(CelestialRules.Refusal.TOO_MANY_SPIRITS, CelestialRules.check(k, true, true, "me", "me", 0, 1e9, 1, 1, 1));
        assertEquals(CelestialRules.Refusal.NOT_ENOUGH_MAGICULES, CelestialRules.check(k, true, true, "me", "me", 0, 10, 100, 0, 1));
        assertEquals(CelestialRules.Refusal.NONE, CelestialRules.check(k, true, true, "me", "me", 0, 1000, 100, 0, 1));
    }

    @Test
    void masteryOpensMoreSpiritsAtOnce() {
        assertEquals(1, CelestialRules.spiritCap(0.0));
        assertEquals(1, CelestialRules.spiritCap(0.49));
        assertEquals(2, CelestialRules.spiritCap(0.5));
        assertEquals(3, CelestialRules.spiritCap(0.9));
        assertEquals(3, CelestialRules.spiritCap(1.0));
    }

    // ------------------------------------------------------------------ numbers
    @Test
    void costCooldownAndPowerFollowTheBondAndTheEp() {
        CelestialKey k = CelestialKey.LEO;
        double max = 100_000;
        double c0 = CelestialRules.cost(k, max, 0), c5 = CelestialRules.cost(k, max, 5);
        assertEquals(max * k.costPermille / 1000.0, c0, 1e-6);
        assertTrue(c5 < c0 && c5 > c0 * 0.8, "bond makes it a little cheaper, never free");
        assertEquals(CelestialRules.COST_FLOOR, CelestialRules.cost(k, 10, 0), 1e-9, "the floor");
        assertTrue(CelestialRules.cooldown(k, 5) < CelestialRules.cooldown(k, 0));
        assertEquals(k.cooldown, CelestialRules.cooldown(k, 0));
        float p1 = CelestialRules.power(k, 1.0, 0, 0.0), p5 = CelestialRules.power(k, 1.0, 5, 0.0), pm = CelestialRules.power(k, 1.0, 0, 1.0), pe = CelestialRules.power(k, 6.0, 0, 0.0);
        assertTrue(p5 > p1 && pm > p1 && pe > p1 * 5.9f, "bond, mastery and EP all raise the blow");
        assertEquals(p1 * 6.0f, pe, 1e-3f, "EP scales it linearly through the balance law's scale");
        assertEquals(0f, CelestialRules.power(CelestialKey.ARIES, 5.0, 3, 1.0), 1e-9, "Aries does no damage");
        assertEquals(CelestialRules.power(k, 1.0, 5, 1.0), CelestialRules.power(k, 1.0, 99, 5.0), 1e-6f, "out-of-range bond and mastery are clamped");
    }

    @Test
    void bossesTakeLessAndHardControlIsShortened() {
        assertEquals(10f, CelestialRules.vsBoss(10f, false));
        assertEquals(6f, CelestialRules.vsBoss(10f, true), 1e-6f);
        assertEquals(100, CelestialRules.controlTicks(CelestialKey.VIRGO, 100, false));
        int onBoss = CelestialRules.controlTicks(CelestialKey.VIRGO, 100, true);
        assertTrue(onBoss <= 20, "never more than a second on a boss");
        assertTrue(CelestialRules.controlTicks(CelestialKey.VIRGO, 40, true) <= 40 * CelestialKey.VIRGO.bossFactor + 1);
    }

    // ------------------------------------------------------------------ Star Dress
    @Test
    void starDressesOpenOneAfterAnotherWithMasteryAndNeedABondedBoundGoldKey() {
        CelestialKey first = CelestialKey.AQUARIUS, last = CelestialKey.LIBRA;
        assertEquals(0.20, CelestialRules.dressMastery(first), 1e-9);
        assertEquals(0.64, CelestialRules.dressMastery(last), 1e-9);
        assertFalse(CelestialRules.starDressUnlocked(first, true, 2, 0.19));
        assertTrue(CelestialRules.starDressUnlocked(first, true, 2, 0.20));
        assertFalse(CelestialRules.starDressUnlocked(first, true, 1, 1.0), "the bond must be at least 2");
        assertFalse(CelestialRules.starDressUnlocked(first, false, 5, 1.0), "the contract must be sealed");
        assertFalse(CelestialRules.starDressUnlocked(CelestialKey.HOROLOGIUM, true, 5, 1.0), "silver keys have no Star Dress");
        assertFalse(CelestialRules.starDressUnlocked(null, true, 5, 1.0));
        assertFalse(CelestialRules.starDressUnlocked(last, true, 5, 0.63));
        assertTrue(CelestialRules.starDressUnlocked(last, true, 5, 0.64));
        int open = 0;
        for (CelestialKey k : CelestialKey.gold()) if (CelestialRules.starDressUnlocked(k, true, 3, 0.4)) open++;
        assertEquals(6, open, "at 40% mastery six of the twelve dresses are open");
    }

    // ------------------------------------------------------------------ drops
    @Test
    void dropChancesAreSmallOrderedAndCapped() {
        double vault = CelestialRules.dropChance(CelestialRules.Source.VAULT_CHEST, 10, 0), chest = CelestialRules.dropChance(CelestialRules.Source.CHEST, 10, 0),
                court = CelestialRules.dropChance(CelestialRules.Source.COURT_MOB, 10, 0), boss = CelestialRules.dropChance(CelestialRules.Source.BOSS_MOB, 10, 0),
                mob = CelestialRules.dropChance(CelestialRules.Source.MOB, 10, 0);
        assertTrue(vault > chest && boss > court && court > mob && chest > mob, "vault > chest, boss > court mob > any mob");
        assertTrue(vault < 0.15 && boss < 0.15, "rare");
        assertTrue(CelestialRules.dropChance(CelestialRules.Source.VAULT_CHEST, 10, 10) > vault, "luck helps");
        assertTrue(CelestialRules.dropChance(CelestialRules.Source.VAULT_CHEST, 50, 10) > vault, "depth helps");
        assertEquals(0.0, CelestialRules.dropChance(CelestialRules.Source.VAULT_CHEST, 0, 0), "nothing off the floors");
        assertTrue(CelestialRules.dropChance(CelestialRules.Source.VAULT_CHEST, 60, 10) <= 0.5);
        assertTrue(CelestialRules.isCourt("celestial_court"));
        assertFalse(CelestialRules.isCourt("abyss"));
    }

    @Test
    void theDropTableNeverGivesTheGateKeyAndKeepsGoldOffTheFirstFloors() {
        Random r = new Random(7);
        for (int i = 0; i < 4000; i++) {
            CelestialKey k = CelestialRules.pickKey(r.nextDouble(), r.nextInt(1 << 17), 1 + r.nextInt(2));
            assertNotNull(k);
            assertTrue(k.summons(), "the gate key never drops");
            assertFalse(k.isGold(), "no gold key above floor 3");
        }
        boolean sawGold = false;
        for (int i = 0; i < 4000; i++) {
            CelestialKey k = CelestialRules.pickKey(r.nextDouble(), 0, 20);
            assertTrue(k.summons());
            sawGold |= k.isGold();
        }
        assertTrue(sawGold);
    }

    @Test
    void theDropTableFavoursWhatIsMissingSoACollectionFillsUp() {
        Random r = new Random(11);
        int ownedAll = CelestialKey.GOLD_MASK & ~CelestialKey.LIBRA.bit();               // eleven gold keys held, Libra missing
        Map<CelestialKey, Integer> n = new EnumMap<>(CelestialKey.class);
        for (int i = 0; i < 20_000; i++) n.merge(CelestialRules.pickKey(r.nextDouble(), ownedAll, 30), 1, Integer::sum);
        int libra = n.getOrDefault(CelestialKey.LIBRA, 0), leo = n.getOrDefault(CelestialKey.LEO, 0);
        assertTrue(libra > leo * 3, "the missing key is about four times as likely as a duplicate: " + libra + " vs " + leo);
        assertTrue(leo > 0, "a duplicate is still possible");
        int silver = n.getOrDefault(CelestialKey.HOROLOGIUM, 0);
        assertTrue(silver > libra, "silver keys are more common than a single missing gold key");
    }

    @Test
    void pickKeyIsDeterministicAndHandlesTheEdges() {
        assertSame(CelestialRules.pickKey(0.37, 5, 12), CelestialRules.pickKey(0.37, 5, 12));
        assertNotNull(CelestialRules.pickKey(0.0, 0, 12));
        assertNotNull(CelestialRules.pickKey(1.0, 0, 12));
        assertNotNull(CelestialRules.pickKey(-3.0, 0, 12));
        assertNotNull(CelestialRules.pickKey(0.0, 0, 1), "even on the first floors the silver keys can drop");
    }
}
