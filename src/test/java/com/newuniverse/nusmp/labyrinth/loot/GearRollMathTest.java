package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.relics.GradeDamage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The roll maths of the loot gear: ranges, determinism, rarity weights, the damage grades. */
class GearRollMathTest {
    private static final String[] TYPES = {"dagger", "longsword", "spear", "battleaxe", "katana", "greatsword", "staff", "hammer"};

    @Test
    void theSameSeedMakesTheSameRoll() {
        for (GearKind kind : GearKind.values()) {
            for (LootRarity r : LootRarity.values()) {
                GearRoll a = GearRollMath.roll(12345L, 40, r, kind, StatKind.ARMOR, 8.0);
                GearRoll b = GearRollMath.roll(12345L, 40, r, kind, StatKind.ARMOR, 8.0);
                assertEquals(a, b, kind + "/" + r);
            }
        }
        assertFalse(GearRollMath.roll(1L, 40, LootRarity.EPIC, GearKind.ARMOUR, StatKind.ARMOR, 0).equals(GearRollMath.roll(2L, 40, LootRarity.EPIC, GearKind.ARMOUR, StatKind.ARMOR, 0)));
    }

    @Test
    void lineCountsFollowTheRarity() {
        for (LootRarity r : LootRarity.values()) {
            Set<Integer> seen = new HashSet<>();
            for (long s = 0; s < 400; s++) {
                GearRoll roll = GearRollMath.roll(s, 30, r, GearKind.WEAPON, StatKind.ATTACK_DAMAGE, 12.0);
                int n = roll.lines().size();
                assertTrue(n >= r.minLines() && n <= r.maxLines(), r + " has " + n + " lines");
                assertTrue(n >= 2 && n <= 4, "two to four lines: " + n);
                seen.add(n);
            }
            assertEquals(r.maxLines() - r.minLines() + 1, seen.size(), r + " uses its whole range of line counts");
        }
    }

    @Test
    void everyValueIsInsideItsRangeAndItsCapAtEveryDepthAndRarity() {
        for (GearKind kind : GearKind.values()) {
            for (LootRarity r : LootRarity.values()) {
                for (int depth : new int[]{0, 1, 9, 10, 29, 30, 99, 100, 299, 300, 1000, 2000}) {
                    for (long s = 0; s < 40; s++) {
                        double headroom = kind == GearKind.WEAPON ? 15.0 : 0.0;
                        GearRoll roll = GearRollMath.roll(s * 7919 + depth, depth, r, kind, kind == GearKind.WEAPON ? StatKind.ATTACK_DAMAGE : StatKind.ARMOR, headroom);
                        Set<StatKind> kinds = new HashSet<>();
                        for (GearRoll.Line l : roll.lines()) {
                            assertTrue(kinds.add(l.kind()), "no stat twice: " + roll);
                            assertTrue(l.kind().allowedOn(kind), l.kind() + " is not allowed on " + kind);
                            assertTrue(l.value() > 0.0, "a positive line");
                            assertTrue(l.value() <= l.kind().cap() * (l.kind() == StatKind.ATTACK_DAMAGE ? headroom : 1.0) + 1e-9, l.kind() + " over its cap: " + l.value());
                            if (l.kind() != StatKind.ATTACK_DAMAGE) {
                                double[] range = l.kind().range(r, depth);
                                assertTrue(l.value() >= Math.min(range[0], l.kind().cap()) - l.kind().step() - 1e-9 && l.value() <= range[1] + l.kind().step() + 1e-9,
                                        l.kind() + " " + l.value() + " outside " + range[0] + ".." + range[1] + " at " + r + " depth " + depth);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void theFirstLineIsThePrimaryStat() {
        for (long s = 0; s < 200; s++) {
            assertEquals(StatKind.ATTACK_DAMAGE, GearRollMath.roll(s, 12, LootRarity.RARE, GearKind.WEAPON, StatKind.ATTACK_DAMAGE, 9.0).lines().get(0).kind());
            assertEquals(StatKind.ARMOR, GearRollMath.roll(s, 12, LootRarity.RARE, GearKind.ARMOUR, StatKind.ARMOR, 0.0).lines().get(0).kind());
            assertEquals(StatKind.MANA, GearRollMath.roll(s, 12, LootRarity.RARE, GearKind.TRINKET, StatKind.MANA, 0.0).lines().get(0).kind());
        }
    }

    @Test
    void aWeaponAtTheTopOfItsGradeRollsNoAttackDamage() {
        assertEquals(StatKind.ATTACK_SPEED, GearRollMath.weaponPrimary(0.0));
        assertEquals(StatKind.ATTACK_DAMAGE, GearRollMath.weaponPrimary(5.0));
        for (long s = 0; s < 100; s++) {
            GearRoll roll = GearRollMath.roll(s, 50, LootRarity.EPIC, GearKind.WEAPON, StatKind.ATTACK_SPEED, 0.0);
            assertFalse(roll.has(StatKind.ATTACK_DAMAGE), "no headroom, no attack damage line");
        }
    }

    @Test
    void depthAndRarityRaiseTheRanges() {
        for (StatKind k : StatKind.values()) {
            double[] lo = k.range(LootRarity.COMMON, 0), hi = k.range(LootRarity.LEGENDARY, 2000);
            assertTrue(hi[1] >= lo[1], k + " grows");
            assertTrue(hi[1] <= k.cap() + 1e-9, k + " never passes its cap");
        }
        assertEquals(1.0, StatKind.depthFactor(0), 1e-12);
        assertTrue(StatKind.depthFactor(100) > 1.2 && StatKind.depthFactor(2000) < 1.6 + 1e-9);
        double last = 0;
        for (int d = 0; d <= 2000; d += 25) {
            double f = StatKind.depthFactor(d);
            assertTrue(f >= last, "monotonic");
            last = f;
        }
    }

    @Test
    void weaponDamageStaysInsideTheGradeRange() {
        for (String type : TYPES) {
            for (LootRarity r : LootRarity.values()) {
                int[] range = GradeDamage.range(r.grade());
                double base = GearRollMath.baseDamage(r, type);
                assertTrue(base >= range[0] && base <= range[1], type + " " + r + " base " + base);
                double head = GearRollMath.headroom(r, type);
                assertEquals(range[1] - base, head, 1e-9);
                for (long s = 0; s < 80; s++) {
                    GearRoll roll = GearRollMath.roll(s, 400, r, GearKind.WEAPON, GearRollMath.weaponPrimary(head), head);
                    double total = GearRollMath.totalDamage(r, type, roll);
                    assertTrue(total >= base && total <= range[1] + 1e-9, type + " " + r + " total " + total + " outside " + range[0] + ".." + range[1]);
                }
            }
        }
        assertEquals(0, LootRarity.UNCOMMON.grade());
        assertEquals(1, LootRarity.EPIC.grade());
        assertEquals(2, LootRarity.LEGENDARY.grade());
    }

    @Test
    void rarityWeightsMoveUpWithDepthAndLuck() {
        double[] shallow = LootRarity.weights(0, 0), deep = LootRarity.weights(4, 0);
        assertTrue(shallow[0] > deep[0], "common falls with depth");
        assertTrue(deep[4] > shallow[4] && deep[3] > shallow[3], "the top tiers rise with depth");
        double[] lucky = LootRarity.weights(2, 5), plain = LootRarity.weights(2, 0);
        assertTrue(lucky[0] < plain[0] && lucky[2] > plain[2], "luck moves weight up");
        double sumL = 0, sumP = 0;
        for (int i = 0; i < 5; i++) {
            sumL += lucky[i];
            sumP += plain[i];
            assertTrue(lucky[i] >= 0.0);
        }
        assertEquals(sumP, sumL, 1e-9, "luck moves weight, it does not add any");
        assertTrue(LootRarity.weights(0, 100)[0] >= 2.0 - 1e-9, "common never below 2");
    }

    @Test
    void thePickFollowsTheWeights() {
        int[] n = new int[5];
        int trials = 100000;
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < trials; i++) n[LootRarity.pick(rnd.nextDouble(), 2, 0).ordinal()]++;
        double[] w = LootRarity.weights(2, 0);
        double sum = 0;
        for (double x : w) sum += x;
        for (int i = 0; i < 5; i++) {
            double expect = w[i] / sum;
            assertEquals(expect, n[i] / (double) trials, 0.01, "tier " + i);
        }
        assertEquals(LootRarity.COMMON, LootRarity.pick(0.0, 4, 0));
        assertEquals(LootRarity.LEGENDARY, LootRarity.pick(0.999999, 4, 0));
        assertEquals(LootRarity.COMMON, LootRarity.pick(-1, 0, 0));
        assertEquals(LootRarity.COMMON, LootRarity.fromId("COMMON"));
        assertEquals(null, LootRarity.fromId("nonsense"));
    }

    @Test
    void temperingAddsOneFreshLineOnce() {
        for (long s = 0; s < 120; s++) {
            GearRoll base = GearRollMath.roll(s, 60, LootRarity.RARE, GearKind.ARMOUR, StatKind.ARMOR, 0.0);
            GearRoll.Line extra = GearRollMath.temperLine(base, GearKind.ARMOUR, 0.0);
            assertTrue(extra.kind().allowedOn(GearKind.ARMOUR));
            assertTrue(extra.value() > 0.0 && extra.value() <= extra.kind().cap());
            if (base.lines().size() < GearRollMath.eligible(GearKind.ARMOUR, 0.0).size()) assertFalse(base.has(extra.kind()), "a kind the piece does not have yet");
            GearRoll t = base.withTemper(extra);
            assertTrue(t.tempered());
            assertEquals(base.lines().size() + 1, t.lines().size());
            assertEquals(extra, GearRollMath.temperLine(base, GearKind.ARMOUR, 0.0), "deterministic");
        }
    }

    @Test
    void sellValuesGrowWithRarityAndDepth() {
        long last = 0;
        for (LootRarity r : LootRarity.values()) {
            long v = GearRollMath.sellValue(100, r, 10);
            assertTrue(v > last, r + " sells for more");
            last = v;
        }
        assertTrue(GearRollMath.sellValue(100, LootRarity.RARE, 400) > GearRollMath.sellValue(100, LootRarity.RARE, 1));
        assertTrue(GearRollMath.sellValue(0, LootRarity.COMMON, 0) >= 1);
        assertEquals("1.5", GearRollMath.format(StatKind.MAX_HEALTH, 1.5));
        assertEquals("3", GearRollMath.format(StatKind.CRIT_CHANCE, 0.03));
        assertEquals("1.5", GearRollMath.format(StatKind.MOVE_SPEED, 0.015));
        assertEquals("2", GearRollMath.format(StatKind.ARMOR, 2.0));
    }
}
