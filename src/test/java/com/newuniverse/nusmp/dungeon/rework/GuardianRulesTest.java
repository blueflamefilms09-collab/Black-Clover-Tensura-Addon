package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.dungeon.rework.GuardianRules.Kit;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules.Verdict;
import com.newuniverse.nusmp.dungeon.rework2.DungeonBossSoundRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dungeon-boss rework 2 (docs/dungeon_bosses_rework2.md, part 1): the pure numbers of the Labyrinth Guardian's book and the nine kits. No Minecraft class is loaded.
 */
class GuardianRulesTest {
    @Test
    void theBookHasFourMovesAndOneUltimateWithTelegraphs() {
        assertEquals(5, GuardianRules.BOOK.size());
        assertEquals("slam", GuardianRules.BOOK.get(GuardianRules.G_SLAM).id());
        assertEquals("lance", GuardianRules.BOOK.get(GuardianRules.G_LANCE).id());
        assertEquals("rain", GuardianRules.BOOK.get(GuardianRules.G_RAIN).id());
        assertEquals("wave", GuardianRules.BOOK.get(GuardianRules.G_WAVE).id());
        assertEquals("verdict", GuardianRules.BOOK.get(GuardianRules.G_VERDICT).id());
        int ultimates = 0;
        for (DungeonReworkRules.Move m : GuardianRules.BOOK) {
            assertTrue(m.windup() >= 20, m.id() + " is telegraphed for at least a second");
            assertTrue(m.cooldown() >= DungeonReworkRules.MIN_COOLDOWN, m.id());
            assertTrue(m.damage() > 0 && m.hits() >= 1 && m.minRange() <= m.maxRange(), m.id());
            if (m.ultimate()) ultimates++;
        }
        assertEquals(1, ultimates, "one signature ultimate");
        assertEquals(1, GuardianRules.BOOK.get(GuardianRules.G_SLAM).minPhase(), "the first three moves are there from the start");
        assertEquals(2, GuardianRules.BOOK.get(GuardianRules.G_WAVE).minPhase(), "the Wave comes at 50 % health");
        assertEquals(3, GuardianRules.BOOK.get(GuardianRules.G_VERDICT).minPhase(), "the Verdict comes at 25 % health");
        assertTrue(GuardianRules.BOOK.get(GuardianRules.G_VERDICT).windup() >= 60, "the ultimate gives a long warning");
    }

    @Test
    void nineKitsOneForEachTraitInOrderEachWithItsOwnThemeAndRealCues() {
        String[] order = {"gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral"};
        assertEquals(order.length, GuardianRules.KITS.size());
        Set<String> themes = new HashSet<>();
        for (int i = 0; i < order.length; i++) {
            Kit k = GuardianRules.KITS.get(i);
            assertEquals(order[i], k.id());
            assertEquals("dungeon.boss.guardian." + order[i], k.theme());
            assertTrue(themes.add(k.theme()));
            for (String cue : new String[]{k.beginCue(), k.hitCue(), k.accentCue()}) assertNotNull(DungeonBossSoundRules.cue(cue), k.id() + " names the cue " + cue);
            assertSame(k, GuardianRules.kit(order[i].toUpperCase()), "the lookup ignores case");
        }
        assertEquals("gravity", GuardianRules.kit("nonsense").id(), "an unknown trait gets the gravity kit");
        assertEquals("gravity", GuardianRules.kit(null).id());
        Set<Verdict> verdicts = new HashSet<>();
        for (Kit k : GuardianRules.KITS) verdicts.add(k.verdict());
        assertEquals(2, verdicts.size(), "both kinds of Verdict are in use");
        // the kits are not copies of each other: no two share all four flavours and the Verdict
        Set<String> fingerprints = new HashSet<>();
        for (Kit k : GuardianRules.KITS) assertTrue(fingerprints.add(k.slam() + "" + k.lance() + k.rain() + k.wave() + k.verdict() + k.beginCue()), k.id() + " is a copy of another kit");
    }

    private static void assertSame(Object a, Object b, String msg) { assertTrue(a == b, msg); }

    @Test
    void damageIsAShareOfTheAttackAndTheTierOnlyRaisesTheVerdictUpToAQuarter() {
        assertEquals(10.0, GuardianRules.damage(GuardianRules.G_SLAM, 10.0, 0) / 0.9, 1e-9);
        assertEquals(0.0, GuardianRules.damage(GuardianRules.G_SLAM, -5.0, 0), 1e-9, "a negative attack never heals");
        assertEquals(1.0, GuardianRules.tierFactor(0), 1e-9);
        assertEquals(1.0, GuardianRules.tierFactor(1), 1e-9);
        assertEquals(1.25, GuardianRules.tierFactor(5), 1e-9);
        assertEquals(1.25, GuardianRules.tierFactor(99), 1e-9, "never over a quarter more");
        assertEquals(GuardianRules.damage(GuardianRules.G_SLAM, 10, 0), GuardianRules.damage(GuardianRules.G_SLAM, 10, 5), 1e-9, "the tier changes the ultimate only");
        assertTrue(GuardianRules.damage(GuardianRules.G_VERDICT, 10, 5) > GuardianRules.damage(GuardianRules.G_VERDICT, 10, 1));
        assertEquals(1.0, GuardianRules.sizeFactor(1.0), 1e-9);
        assertEquals(1.0, GuardianRules.sizeFactor(1.8), 1e-9);
        assertEquals(1.6, GuardianRules.sizeFactor(10.0), 1e-9);
    }

    @Test
    void theWorstCaseDamagePerSecondIsBoundedAndGrowsWithThePhases() {
        double p1 = GuardianRules.worstCaseSharePerSecond(1, 0);
        double p2 = GuardianRules.worstCaseSharePerSecond(2, 0);
        double p3 = GuardianRules.worstCaseSharePerSecond(3, 5);
        assertTrue(p1 < p2 && p2 < p3, "later phases hit more often");
        assertTrue(p1 < 0.5, "phase 1 is gentle: " + p1);
        assertTrue(p3 <= 1.0, "even with every move and the highest tier the sum stays under one attack a second: " + p3);
    }

    @Test
    void theGeometryIsRight() {
        // a Lance from the origin along +x, 22 long, 1.6 each side
        assertTrue(GuardianRules.onLine(0, 0, 1, 0, 10, 1.5, 22, 1.6));
        assertFalse(GuardianRules.onLine(0, 0, 1, 0, 10, 1.7, 22, 1.6), "just beside the line");
        assertFalse(GuardianRules.onLine(0, 0, 1, 0, -1, 0, 22, 1.6), "behind the boss");
        assertFalse(GuardianRules.onLine(0, 0, 1, 0, 23, 0, 22, 1.6), "past the end");
        assertTrue(GuardianRules.inCircle(0, 0, 5.5, 3, 4));
        assertFalse(GuardianRules.inCircle(0, 0, 5.5, 4, 4));
        double[] s = GuardianRules.sigils(0, 0, 0, 1.0);
        assertEquals(4, s.length);
        assertEquals(9.0, s[0], 1e-9);
        assertEquals(-9.0, s[2], 1e-9);
        assertTrue(GuardianRules.insideSigil(s, 9.5, 0.5));
        assertFalse(GuardianRules.insideSigil(s, 0, 0), "the boss's own feet are not safe");
        // the Verdict: safe zone means the sigil protects, keep-out means distance protects
        assertEquals(1.0, GuardianRules.verdictShare(Verdict.SAFE_ZONE, false, 3, 1.0), 1e-9);
        assertEquals(GuardianRules.SIGIL_GRAZE, GuardianRules.verdictShare(Verdict.SAFE_ZONE, true, 3, 1.0), 1e-9);
        assertEquals(1.0, GuardianRules.verdictShare(Verdict.KEEP_OUT, false, 5, 1.0), 1e-9);
        assertEquals(0.0, GuardianRules.verdictShare(Verdict.KEEP_OUT, false, 13, 1.0), 1e-9);
        assertTrue(GuardianRules.waveRadius(0, 1.0) >= 2.0 && GuardianRules.waveRadius(100, 1.0) == 16.0, "the Wave starts small and stops at 16 blocks");
        assertEquals(16.0 * 1.6, GuardianRules.waveRadius(100, 1.6), 1e-9);
    }
}
