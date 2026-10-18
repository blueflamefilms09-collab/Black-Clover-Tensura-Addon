package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Chances;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Kind;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Plan;
import com.newuniverse.nusmp.skilltree.loot.LootRules.Victim;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the books are meant to be a rare reward of the Abyssal descent and nothing else, so this test is the rule written as examples. NOTHING drops outside the
 * Mega-Dungeon, for a death no player caused, on a Holy floor, in a gap between sectors, from anything that is not an ordinary monster or a sector Guardian; an
 * Abyssal monster adds a route book at the book chance and one SP Book at the SP chance, as two independent draws (route book first); a Guardian on a boss floor (a
 * non-zero multiple of ten) of the Abyssal road adds a route book at the boss chance and always 1 to 3 SP Books, but nothing on a floor that is not a boss floor,
 * and nothing on the Holy road unless the owner's switch is on. The config values are clamped so no number in a file can break the rule. Long seeded runs check the
 * real rates: 0.4% and 4% for the monsters, 30% and a mean of 2 SP Books for the Guardian, each within five standard deviations (a fixed seed, so it either always
 * passes or always fails). With every chance at 1 the rule must add, with every chance at 0 it must not: that pair is the mutation guard against a chance wired to
 * the wrong field.
 */
class LootRulesTest {
    private static final Chances ALWAYS = new Chances(1, 1, 1, 1, 3, false);
    private static final Chances NEVER = new Chances(0, 0, 0, 0, 0, false);

    private static Dice fixed(double v) { return () -> v; }

    private static Dice seq(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    private static Victim monster(Polarity p) { return new Victim(true, true, Kind.MONSTER, p, p == null ? 0 : p.sign() * 4); }

    private static Victim guardian(Polarity p, int floor) { return new Victim(true, true, Kind.GUARDIAN, p, floor); }

    @Test
    void theDesignsDefaults() {
        Chances d = Chances.DEFAULT;
        assertEquals(0.004, d.abyssalBook(), 0.0);
        assertEquals(0.04, d.abyssalSp(), 0.0);
        assertEquals(0.30, d.bossBook(), 0.0);
        assertEquals(1, d.bossSpMin());
        assertEquals(3, d.bossSpMax());
        assertFalse(d.holyBosses());
    }

    @Test
    void nothingOutsideTheMegaDungeonOrWithoutAPlayerKill() {
        assertEquals(Plan.NOTHING, LootRules.decide(new Victim(false, true, Kind.MONSTER, Polarity.ABYSSAL, -4), ALWAYS, fixed(0.0)));
        assertEquals(Plan.NOTHING, LootRules.decide(new Victim(true, false, Kind.MONSTER, Polarity.ABYSSAL, -4), ALWAYS, fixed(0.0)));
        assertEquals(Plan.NOTHING, LootRules.decide(new Victim(true, false, Kind.GUARDIAN, Polarity.ABYSSAL, -10), ALWAYS, fixed(0.0)));
        assertEquals(Plan.NOTHING, LootRules.decide(null, ALWAYS, fixed(0.0)));
        assertEquals(Plan.NOTHING, LootRules.decide(monster(Polarity.ABYSSAL), null, fixed(0.0)));
        assertEquals(Plan.NOTHING, LootRules.decide(monster(Polarity.ABYSSAL), ALWAYS, null));
    }

    @Test
    void nothingFromAnythingThatIsNotAMonsterOrAGuardian() {
        assertEquals(Plan.NOTHING, LootRules.decide(new Victim(true, true, Kind.OTHER, Polarity.ABYSSAL, -10), ALWAYS, fixed(0.0)));
    }

    @Test
    void aMonsterPaysOnlyOnTheAbyssalRoad() {
        assertTrue(LootRules.decide(monster(Polarity.ABYSSAL), ALWAYS, fixed(0.0)).routeBook());
        assertEquals(Plan.NOTHING, LootRules.decide(monster(Polarity.HOLY), ALWAYS, fixed(0.0)), "Holy floors are for the other loot");
        assertEquals(Plan.NOTHING, LootRules.decide(monster(null), ALWAYS, fixed(0.0)), "the gaps between sectors pay nothing");
    }

    @Test
    void anAbyssalMonsterRollsTheBookFirstAndTheSpBookSecondAndIndependently() {
        Chances c = Chances.DEFAULT;
        Plan both = LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.001, 0.01));
        assertTrue(both.routeBook());
        assertEquals(1, both.spBooks());
        Plan bookOnly = LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.001, 0.9));
        assertTrue(bookOnly.routeBook());
        assertEquals(0, bookOnly.spBooks());
        Plan spOnly = LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.5, 0.01));
        assertFalse(spOnly.routeBook());
        assertEquals(1, spOnly.spBooks());
        assertTrue(LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.5, 0.9)).isEmpty());
        assertFalse(bookOnly.boss(), "a monster's SP Books use the monster weights");
    }

    @Test
    void theChanceBoundariesAreStrictlyBelow() {
        Chances c = Chances.DEFAULT;
        assertTrue(LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.00399, 0.9)).routeBook());
        assertFalse(LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.004, 0.9)).routeBook());
        assertEquals(1, LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.9, 0.0399)).spBooks());
        assertEquals(0, LootRules.decide(monster(Polarity.ABYSSAL), c, seq(0.9, 0.04)).spBooks());
    }

    @Test
    void everyChanceAtOneAddsAndEveryChanceAtZeroDoesNot() {
        Plan all = LootRules.decide(monster(Polarity.ABYSSAL), ALWAYS, fixed(0.9999999));
        assertTrue(all.routeBook());
        assertEquals(1, all.spBooks());
        assertTrue(LootRules.decide(monster(Polarity.ABYSSAL), NEVER, fixed(0.0)).isEmpty());
        assertTrue(LootRules.decide(guardian(Polarity.ABYSSAL, -10), NEVER, fixed(0.0)).isEmpty());
    }

    @Test
    void aGuardianPaysOnBossFloorsOfTheAbyssalRoad() {
        for (int floor : new int[] {-10, -20, -30, -100}) {
            Plan p = LootRules.decide(guardian(Polarity.ABYSSAL, floor), Chances.DEFAULT, seq(0.1, 0.0));
            assertTrue(p.routeBook(), "30% chance, 0.1 is below it: floor " + floor);
            assertEquals(1, p.spBooks());
            assertTrue(p.boss());
        }
        assertFalse(LootRules.decide(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, seq(0.3, 0.0)).routeBook(), "0.3 is not below 30%");
    }

    @Test
    void aGuardianAlwaysAddsOneToThreeSpBooks() {
        assertEquals(1, LootRules.decide(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, seq(0.9, 0.0)).spBooks());
        assertEquals(2, LootRules.decide(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, seq(0.9, 0.5)).spBooks());
        assertEquals(3, LootRules.decide(guardian(Polarity.ABYSSAL, -10), Chances.DEFAULT, seq(0.9, 0.99)).spBooks());
        Chances five = new Chances(0, 0, 0, 2, 2, false);
        assertEquals(2, LootRules.decide(guardian(Polarity.ABYSSAL, -10), five, fixed(0.99)).spBooks(), "min = max is a fixed count");
    }

    @Test
    void aGuardianOnAFloorThatIsNotAMultipleOfTenPaysNothing() {
        for (int floor : new int[] {0, -1, -5, -9, -11, -15, -19, -25}) {
            assertEquals(Plan.NOTHING, LootRules.decide(guardian(Polarity.ABYSSAL, floor), ALWAYS, fixed(0.0)), "floor " + floor);
        }
    }

    @Test
    void theHolyRoadPaysOnlyWhenTheSwitchIsOn() {
        assertEquals(Plan.NOTHING, LootRules.decide(guardian(Polarity.HOLY, 10), ALWAYS, fixed(0.0)));
        Chances holyOn = new Chances(1, 1, 1, 1, 3, true);
        assertTrue(LootRules.decide(guardian(Polarity.HOLY, 10), holyOn, fixed(0.0)).routeBook());
        assertEquals(Plan.NOTHING, LootRules.decide(guardian(Polarity.HOLY, 7), holyOn, fixed(0.0)), "the switch does not make an ordinary floor a boss floor");
        assertEquals(Plan.NOTHING, LootRules.decide(monster(Polarity.HOLY), holyOn, fixed(0.0)), "the switch is for bosses only: Holy monsters never pay");
        assertEquals(Plan.NOTHING, LootRules.decide(guardian(null, -10), ALWAYS, fixed(0.0)), "a Guardian that belongs to no sector pays nothing");
    }

    @Test
    void configValuesAreClamped() {
        Chances c = new Chances(-1, 5, Double.NaN, -4, -9, false);
        assertEquals(0.0, c.abyssalBook(), 0.0);
        assertEquals(1.0, c.abyssalSp(), 0.0);
        assertEquals(0.0, c.bossBook(), 0.0, "NaN is no chance");
        assertEquals(0, c.bossSpMin());
        assertEquals(0, c.bossSpMax());
        Chances d = new Chances(0.5, 0.5, 0.5, 6, 2, false);
        assertEquals(6, d.bossSpMin());
        assertEquals(6, d.bossSpMax(), "max is lifted to min");
        Chances e = new Chances(0, 0, 0, 3, 500, false);
        assertEquals(LootRules.MAX_BOSS_SP, e.bossSpMax());
        assertEquals(LootRules.MAX_BOSS_SP, new Chances(0, 0, 0, 500, 500, false).bossSpMin());
    }

    @Test
    void anAbyssalMonstersRealRates() {
        Random random = new Random(1_62_1L);
        Dice dice = random::nextDouble;
        int n = 1_000_000, books = 0, sps = 0;
        Victim v = monster(Polarity.ABYSSAL);
        for (int i = 0; i < n; i++) {
            Plan p = LootRules.decide(v, Chances.DEFAULT, dice);
            if (p.routeBook()) books++;
            sps += p.spBooks();
        }
        double sb = Math.sqrt(n * 0.004 * 0.996), ss = Math.sqrt(n * 0.04 * 0.96);
        assertTrue(Math.abs(books - n * 0.004) < 5 * sb, "route books " + books + " of " + n);
        assertTrue(Math.abs(sps - n * 0.04) < 5 * ss, "SP Books " + sps + " of " + n);
    }

    @Test
    void aGuardiansRealRates() {
        Random random = new Random(1_62_2L);
        Dice dice = random::nextDouble;
        int n = 200_000, books = 0;
        long sps = 0;
        int minSp = 99, maxSp = 0;
        Victim v = guardian(Polarity.ABYSSAL, -10);
        for (int i = 0; i < n; i++) {
            Plan p = LootRules.decide(v, Chances.DEFAULT, dice);
            if (p.routeBook()) books++;
            sps += p.spBooks();
            minSp = Math.min(minSp, p.spBooks());
            maxSp = Math.max(maxSp, p.spBooks());
        }
        double sb = Math.sqrt(n * 0.3 * 0.7);
        assertTrue(Math.abs(books - n * 0.3) < 5 * sb, "route books " + books + " of " + n);
        assertEquals(1, minSp, "a Guardian always adds at least one SP Book");
        assertEquals(3, maxSp);
        assertTrue(Math.abs((double) sps / n - 2.0) < 0.02, "mean SP Books " + (double) sps / n);
    }
}
