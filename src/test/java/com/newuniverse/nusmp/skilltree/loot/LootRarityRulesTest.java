package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Chances;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Decision;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Kind;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Plan;
import com.newuniverse.nusmp.skilltree.loot.LootRules.TieredPlan;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Victim;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.rarity.RarityConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner's rule for the rarity tiers is that with {@code rarityTiers} OFF the drops are EXACTLY those of 3.25, so nothing a server owner relies on moves. The strongest proof is a replay: the
 * same scripted dice (a seeded generator, and a list of fixed values) go through the old {@link LootRules#decide} and through the new entry point {@link LootRules#plan} with the tiers off, for every kind
 * of victim and many different chances, and the plan AND the number of dice values used must be the same each time. With the tiers ON the gates stay the same (the Mega-Dungeon, a player kill, the
 * Abyssal road, a boss floor, the Holy switch), a monster adds a book at {@code rarityBaseChance} times the Luck and Looting multiplier and then draws a rarity (never mythic), a Guardian is guaranteed at
 * {@code guardianBookChance} 1.0 without spending a dice value, and nothing here ever adds an SP Book.
 */
class LootRarityRulesTest {
    private static Victim monster(Polarity p, int floor) { return new Victim(true, true, Kind.MONSTER, p, floor); }

    private static Victim guardian(Polarity p, int floor) { return new Victim(true, true, Kind.GUARDIAN, p, floor); }

    /** a dice that counts the values it handed out */
    private static final class Counting implements Dice {
        final Dice base;
        int used;

        Counting(Dice base) { this.base = base; }

        @Override
        public double next() {
            used++;
            return base.next();
        }
    }

    private static List<Victim> victims() {
        List<Victim> out = new ArrayList<>();
        for (boolean mega : new boolean[] {true, false}) {
            for (boolean player : new boolean[] {true, false}) {
                for (Kind k : Kind.values()) {
                    for (Polarity p : new Polarity[] {Polarity.ABYSSAL, Polarity.HOLY, null}) {
                        for (int floor : new int[] {0, 4, -4, 10, -10, -20, 30, -30, -50, 7}) out.add(new Victim(mega, player, k, p, floor));
                    }
                }
            }
        }
        return out;
    }

    @Test
    void withTheTiersOffTheOldRuleIsReplayedExactlyIncludingTheDiceUsed() {
        Chances[] odds = {Chances.DEFAULT, new Chances(0.5, 0.5, 0.5, 1, 3, false), new Chances(1, 1, 1, 0, 0, true), new Chances(0, 0, 0, 2, 2, false), new Chances(0.004, 0.04, 0.3, 1, 3, true)};
        RarityConfig off = RarityConfig.DEFAULT.withTiers(false);
        int cases = 0;
        for (Victim v : victims()) {
            for (Chances c : odds) {
                for (long seed = 0; seed < 25; seed++) {
                    Counting a = new Counting(new Random(seed)::nextDouble), b = new Counting(new Random(seed)::nextDouble);
                    Plan old = LootRules.decide(v, c, a);
                    Decision now = LootRules.plan(v, c, off, 12.0, 5, b);                  // Luck and Looting must not matter with the tiers off
                    assertNull(now.tiered(), "no tiered plan with the tiers off");
                    assertEquals(old, now.old(), v + " " + c + " seed " + seed);
                    assertEquals(a.used, b.used, "the same number of dice values, " + v + " seed " + seed);
                    assertEquals(old.isEmpty(), now.isEmpty());
                    cases++;
                }
            }
        }
        assertTrue(cases > 10_000, "the replay ran: " + cases);
    }

    @Test
    void withTheTiersOffAFixedDiceListGivesTheOldPlanToo() {
        double[][] scripts = {{0.0}, {0.9999}, {0.001, 0.5}, {0.5, 0.001}, {0.0039, 0.0399, 0.99}, {0.299, 0.7}};
        RarityConfig off = RarityConfig.DEFAULT.withTiers(false);
        for (double[] s : scripts) {
            for (Victim v : new Victim[] {monster(Polarity.ABYSSAL, -4), guardian(Polarity.ABYSSAL, -10), guardian(Polarity.HOLY, 10)}) {
                int[] i = {0}, j = {0};
                Dice a = () -> s[Math.min(i[0]++, s.length - 1)], b = () -> s[Math.min(j[0]++, s.length - 1)];
                assertEquals(LootRules.decide(v, Chances.DEFAULT, a), LootRules.plan(v, Chances.DEFAULT, off, 0, 0, b).old());
                assertEquals(i[0], j[0]);
            }
        }
        assertNull(LootRules.plan(monster(Polarity.ABYSSAL, -4), Chances.DEFAULT, null, 0, 0, () -> 0.0).tiered(), "no config at all is the old rule");
    }

    @Test
    void theGatesOfTheTiersAreTheOldGates() {
        RarityConfig on = new RarityConfig(true, 1.0, 1.0, true, "");
        Dice zero = () -> 0.0;
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(new Victim(false, true, Kind.MONSTER, Polarity.ABYSSAL, -4), Chances.DEFAULT, on, 0, 0, zero), "outside the Mega-Dungeon");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(new Victim(true, false, Kind.MONSTER, Polarity.ABYSSAL, -4), Chances.DEFAULT, on, 0, 0, zero), "no player kill");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(monster(Polarity.HOLY, 4), Chances.DEFAULT, on, 0, 0, zero), "the Holy road");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(monster(null, 0), Chances.DEFAULT, on, 0, 0, zero), "a gap between sectors");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(new Victim(true, true, Kind.OTHER, Polarity.ABYSSAL, -4), Chances.DEFAULT, on, 0, 0, zero), "not a monster");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(guardian(Polarity.ABYSSAL, -7), Chances.DEFAULT, on, 0, 0, zero), "not a boss floor");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(guardian(Polarity.HOLY, 10), Chances.DEFAULT, on, 0, 0, zero), "Holy bosses are off");
        assertNotEquals(TieredPlan.NOTHING, LootRules.decideTiered(guardian(Polarity.HOLY, 10), new Chances(0, 0, 0, 0, 0, true), on, 0, 0, zero), "unless the owner's switch is on");
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(null, Chances.DEFAULT, on, 0, 0, zero));
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(monster(Polarity.ABYSSAL, -4), Chances.DEFAULT, null, 0, 0, zero));
        assertEquals(TieredPlan.NOTHING, LootRules.decideTiered(monster(Polarity.ABYSSAL, -4), Chances.DEFAULT, on, 0, 0, null));
        Decision tiered = LootRules.plan(monster(Polarity.ABYSSAL, -4), Chances.DEFAULT, on, 0, 0, zero);
        assertNotNull(tiered.tiered());
        assertEquals(Plan.NOTHING, tiered.old(), "the tiers never add SP Books");
    }

    @Test
    void aMonsterBooksChanceIsTheBaseTimesTheLuckAndLootingFactor() {
        RarityConfig on = new RarityConfig(true, 1.0, 0.001, true, "");
        Victim m = monster(Polarity.ABYSSAL, -4);
        // the first dice value is the chance roll: just under and just over the chance
        assertNotNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 0, 0, () -> 0.00099).rarity());
        assertNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 0, 0, () -> 0.00101).rarity());
        assertNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 0, 0, () -> 0.00176).rarity(), "looting 3 makes it 0.175 percent: 0.176 is still a miss");
        assertNotNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 0, 3, () -> 0.00174).rarity(), "looting 3: 0.175 percent");
        assertNotNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 20, 3, () -> 0.00274).rarity(), "looting 3 and Luck 20: 0.275 percent");
        assertNull(LootRules.decideTiered(m, Chances.DEFAULT, on, 20, 3, () -> 0.00276).rarity());
        RarityConfig zero = new RarityConfig(true, 1.0, 0.0, true, "");
        assertNull(LootRules.decideTiered(m, Chances.DEFAULT, zero, 99, 99, () -> 0.0).rarity(), "a base chance of 0 never drops");
    }

    @Test
    void theMonsterRarityIsDrawnAfterTheChanceAndIsNeverMythic() {
        RarityConfig on = new RarityConfig(true, 1.0, 1.0, true, "");
        Victim m = monster(Polarity.ABYSSAL, -4);
        Random random = new Random(26);
        int[] hits = new int[BookRarity.count()];
        for (int i = 0; i < 100_000; i++) {
            Counting d = new Counting(random::nextDouble);
            TieredPlan p = LootRules.decideTiered(m, Chances.DEFAULT, on, 0, 0, d);
            assertNotNull(p.rarity(), "base chance 1: always a book");
            assertFalse(p.boss());
            assertEquals(2, d.used, "one value for the chance, one for the rarity");
            hits[p.rarity().rank()]++;
        }
        assertEquals(0, hits[BookRarity.MYTHIC.rank()]);
        assertEquals(100_000 * 600.0 / 990.0, hits[0], 5 * Math.sqrt(100_000 * 0.606 * 0.394));
        assertTrue(hits[BookRarity.EPIC.rank()] > 0, "epic books do come (1 in 99)");
    }

    @Test
    void aGuardianIsGuaranteedByDefaultAndSpendsNoDiceUnlessTheRarityNeedsOne() {
        RarityConfig on = new RarityConfig(true, 1.0, 0.001, true, "");
        for (int floor : new int[] {-10, -20, -50, -90}) {
            Counting d = new Counting(() -> 0.9999);
            TieredPlan p = LootRules.decideTiered(guardian(Polarity.ABYSSAL, floor), Chances.DEFAULT, on, 0, 0, d);
            assertNotNull(p.rarity(), "floor " + floor);
            assertTrue(p.boss());
            assertEquals(0, d.used, "a guaranteed Guardian book at floor " + floor + " needs no dice value");
        }
        TieredPlan mixed = LootRules.decideTiered(guardian(Polarity.ABYSSAL, -30), Chances.DEFAULT, on, 0, 0, () -> 0.05);
        assertEquals(BookRarity.EPIC, mixed.rarity());
        assertEquals(BookRarity.RARE, LootRules.decideTiered(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, on, 0, 0, () -> 0.05).rarity());
        assertEquals(BookRarity.EPIC, LootRules.decideTiered(guardian(Polarity.ABYSSAL, -50), Chances.DEFAULT, on, 0, 0, () -> 0.99).rarity());
    }

    @Test
    void aGuardianChanceBelowOneRollsAndZeroNeverDrops() {
        RarityConfig half = new RarityConfig(true, 0.5, 0.001, true, "");
        assertNotNull(LootRules.decideTiered(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, half, 0, 0, () -> 0.49).rarity());
        assertNull(LootRules.decideTiered(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, half, 0, 0, () -> 0.51).rarity());
        RarityConfig none = new RarityConfig(true, 0.0, 0.001, true, "");
        assertNull(LootRules.decideTiered(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, none, 0, 0, () -> 0.0).rarity());
    }

    @Test
    void luckAndLootingNeverChangeAGuardian() {
        RarityConfig on = new RarityConfig(true, 1.0, 0.001, true, "");
        for (int floor : new int[] {-10, -30, -50}) {
            TieredPlan a = LootRules.decideTiered(guardian(Polarity.ABYSSAL, floor), Chances.DEFAULT, on, 0, 0, () -> 0.1);
            TieredPlan b = LootRules.decideTiered(guardian(Polarity.ABYSSAL, floor), Chances.DEFAULT, on, 50, 9, () -> 0.1);
            assertEquals(a, b);
        }
    }
}
