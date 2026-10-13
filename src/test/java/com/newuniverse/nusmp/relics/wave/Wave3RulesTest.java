package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.wave.Wave3Rules.CharmBook;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Element;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Gauntlet;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Godking;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.MarbleField;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Motes;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Slots;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Step;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Thermo;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 3: the pure rules of the head, hand, belt and consumable relics. */
class Wave3RulesTest {
    @Test
    void motesAreSpentOneByOneAndComeBackAMinuteAfterTheLast() {
        Motes m = new Motes(0, 0);                                    // a player who never fired
        assertEquals(3, m.at(100).left(), "reads as full");
        m = m.fire(100);
        assertEquals(2, m.left());
        m = m.fire(110).fire(120);
        assertEquals(0, m.left());
        assertEquals(120 + Wave3Rules.MOTE_REFILL, m.refillAt(), "the clock starts at the last mote");
        assertFalse(m.canFire(121));
        assertTrue(m.secondsToRefill(121) > 55);
        assertFalse(m.canFire(120 + Wave3Rules.MOTE_REFILL - 1));
        assertTrue(m.canFire(120 + Wave3Rules.MOTE_REFILL), "a minute later all three are back");
        assertEquals(3, m.at(120 + Wave3Rules.MOTE_REFILL).left());
        assertEquals(0, m.fire(121).left(), "firing an empty set changes nothing");
        assertEquals(0, m.secondsToRefill(120 + Wave3Rules.MOTE_REFILL));
    }

    @Test
    void theMotesTogetherDoNotKillAPlayerOutright() {
        assertTrue(Wave3Rules.MOTE_DAMAGE * Wave3Rules.MOTES < 20f * 0.5f, "9 of 20");
        assertTrue(Wave3Rules.MOTE_DAMAGE <= 20f * 0.3f, "one mote is under a third of a player");
    }

    @Test
    void dailyUsesResetWithTheDay() {
        assertTrue(Wave3Rules.dailyReady(0, 0), "never used");
        long used = Wave3Rules.dailyMark(5);
        assertFalse(Wave3Rules.dailyReady(used, 5));
        assertTrue(Wave3Rules.dailyReady(used, 6));
        assertFalse(Wave3Rules.dailyReady(Wave3Rules.dailyMark(0), 0), "day zero is a day too");
        assertEquals(0, Wave3Rules.dayOf(23999));
        assertEquals(1, Wave3Rules.dayOf(24000));
        assertEquals(-1, Wave3Rules.dayOf(-1));
    }

    @Test
    void chargePoolsStartFullAndRecoverAtDawn() {
        assertEquals(8, Wave3Rules.normalize(-1, 8));
        assertEquals(3, Wave3Rules.normalize(9, 3));
        assertEquals(2, Wave3Rules.normalize(2, 8));
        assertTrue(Wave3Rules.canSpend(3, 2));
        assertFalse(Wave3Rules.canSpend(1, 2));
        assertFalse(Wave3Rules.canSpend(5, 0), "spending nothing is not a spend");
        assertEquals(1, Wave3Rules.spend(3, 2));
        assertEquals(0, Wave3Rules.spend(1, 2));
        assertEquals(2, Wave3Rules.tricornDawn(false, 6));
        assertEquals(3, Wave3Rules.tricornDawn(true, 1));
        assertEquals(8, Wave3Rules.tricornDawn(true, 6), "1d6+2 tops out at 8, the whole hat");
        assertEquals(4, Wave3Rules.dawnRefill(2, 8, 3, 4, 2));
        assertEquals(8, Wave3Rules.dawnRefill(7, 8, 3, 4, 2), "never past the cap");
        assertEquals(2, Wave3Rules.dawnRefill(2, 8, 4, 4, 2), "the same day recovers nothing");
        assertEquals(2, Wave3Rules.dawnRefill(2, 8, 9, 4, 2), "a clock set back owes nothing");
        assertEquals(0, Wave3Rules.orderTargets(1, 4));
        assertEquals(2, Wave3Rules.orderTargets(5, 4));
        assertEquals(4, Wave3Rules.orderTargets(8, 4));
        assertEquals(3, Wave3Rules.orderTargets(8, 3), "capped by the number of takers");
    }

    @Test
    void elementsCycleThroughOffAndBack() {
        Element e = Element.OFF;
        int seen = 0;
        do {
            e = e.next();
            seen++;
        } while (e != Element.OFF && seen < 20);
        assertEquals(Element.values().length, seen);
        assertEquals(Element.FIRE, Element.OFF.next());
        assertEquals(Element.OFF, Element.ACID.next());
        assertFalse(Element.OFF.active());
        assertTrue(Element.THUNDER.active());
        assertEquals(Element.OFF, Element.byOrdinal(-3));
        assertEquals(Element.OFF, Element.byOrdinal(99));
        assertEquals(Element.COLD, Element.byOrdinal(2));
        assertEquals(Thermo.COLD, Thermo.FIRE.next());
        assertEquals(Thermo.FIRE, Thermo.COLD.next());
        assertEquals(Thermo.FIRE, Thermo.byOrdinal(7));
        assertEquals(Thermo.COLD, Thermo.byOrdinal(1));
    }

    @Test
    void theBeamsNeverRollUnderTwoAndNeverPassSixTogether() {
        assertEquals(2f, Gauntlet.beam(1, false), "a 1 counts as a 2");
        assertEquals(2f, Gauntlet.beam(2, false));
        assertEquals(4f, Gauntlet.beam(4, false));
        float worst = 0;
        for (int a = 1; a <= 4; a++) for (int b = 1; b <= 4; b++) worst = Math.max(worst, Gauntlet.beam(a, true) + Gauntlet.beam(b, true));
        assertEquals(6f, worst, "two beams at their best are 30% of a 20-point player");
        for (int d = 1; d <= 4; d++) assertTrue(Gauntlet.beam(d, false) <= 20f * 0.3f);
    }

    @Test
    void theBacklashIsRareAndNeverKills() {
        int hits = 0;
        for (int d20 = 1; d20 <= 20; d20++) if (Gauntlet.backlash(d20)) hits++;
        assertEquals(2, hits, "a 1 or a 2");
        assertEquals(6f, Gauntlet.backlashDamage(4, 4, 4, 20f));
        assertEquals(1.5f, Gauntlet.backlashDamage(1, 1, 1, 20f));
        assertEquals(2f, Gauntlet.backlashDamage(4, 4, 4, 3f), "leaves one point of health");
        assertEquals(0f, Gauntlet.backlashDamage(4, 4, 4, 1f), "at one point it does nothing at all");
        assertEquals(0f, Gauntlet.backlashDamage(4, 4, 4, 0f));
        assertEquals(6f, Gauntlet.maxHealthPenalty(6f, 20f));
        assertEquals(2f, Gauntlet.maxHealthPenalty(1.5f, 20f));
        assertEquals(1f, Gauntlet.maxHealthPenalty(6f, 5f), "the maximum never drops under four");
        assertEquals(0f, Gauntlet.maxHealthPenalty(6f, 4f));
    }

    @Test
    void theGodkingWarnsInStagesAndNeverKillsItsWearer() {
        assertEquals(0, Godking.warnLevel(Godking.ASCEND_TICKS));
        assertEquals(1, Godking.warnLevel(1200));
        assertEquals(2, Godking.warnLevel(600));
        assertEquals(3, Godking.warnLevel(200));
        assertEquals(4, Godking.warnLevel(100));
        assertEquals(4, Godking.warnLevel(0));
        assertEquals(1, Godking.newWarning(0, 1100));
        assertEquals(-1, Godking.newWarning(1, 1000), "already said");
        assertEquals(3, Godking.newWarning(1, 150), "skips over a stage if ticks were missed");
        assertEquals(8f, Godking.selfBurst(20f));
        assertEquals(2f, Godking.selfBurst(3f));
        assertEquals(0f, Godking.selfBurst(1f));
        assertEquals(0f, Godking.selfBurst(0.5f));
        assertEquals("five seconds", Godking.warnText(4));
        assertTrue(Godking.ASCEND_TICKS == 12000, "ten minutes");
    }

    @Test
    void theMarblesPopOnceThenOnlyMakeFoesSlip() {
        UUID owner = UUID.randomUUID();
        MarbleField f = new MarbleField(10.5, 64, 20.5, 1000, owner);
        assertTrue(f.contains(10.5, 64, 20.5));
        assertTrue(f.contains(12.0, 64.0, 19.0), "the 3x3 corner");
        assertFalse(f.contains(12.2, 64, 20.5));
        assertFalse(f.contains(10.5, 66, 20.5), "above it");
        assertFalse(f.contains(10.5, 62, 20.5), "far below it");
        assertEquals(Step.POP, f.step(7, 1005), "the first creature falls and the marbles go off");
        assertEquals(Step.NONE, f.step(7, 1010), "not again at once");
        assertEquals(Step.SLIP, f.step(8, 1010), "the next one only slips");
        assertEquals(Step.SLIP, f.step(7, 1005 + MarbleField.SLIP_GAP), "and once the gap is over, the first slips again");
        assertEquals(Step.NONE, f.step(9, 1000 + MarbleField.LIFE), "gone after half a minute");
        assertTrue(f.expired(1000 + MarbleField.LIFE));
        assertTrue(f.popped());
        assertEquals(40, Wave3Rules.blindTicks(1));
        assertEquals(160, Wave3Rules.blindTicks(4));
        assertEquals(160, Wave3Rules.blindTicks(9));
    }

    @Test
    void slotsDropTheOldestWhenFull() {
        Slots<String> s = new Slots<>(3);
        assertNull(s.add("a"));
        assertNull(s.add("b"));
        assertNull(s.add("c"));
        assertEquals("a", s.add("d"));
        assertEquals(3, s.size());
        assertEquals(2, s.count(x -> x.equals("c") || x.equals("d")));
        s.clear();
        assertEquals(0, s.size());
    }

    @Test
    void aCharmHoldsUntilItEndsOrItsMakerStrikes() {
        CharmBook b = new CharmBook(2);
        UUID mob = UUID.randomUUID(), owner = UUID.randomUUID(), other = UUID.randomUUID();
        b.add(mob, owner, 500, false);
        assertTrue(b.spares(mob, owner, 100));
        assertFalse(b.spares(mob, other, 100), "only the one who charmed it is spared");
        assertFalse(b.spares(mob, owner, 500), "over");
        assertFalse(b.breakBy(mob, other), "someone else's blow does not break it");
        assertTrue(b.breakBy(mob, owner));
        assertFalse(b.spares(mob, owner, 100));
        UUID calm = UUID.randomUUID();
        b.add(calm, owner, 500, true);
        assertTrue(b.spares(calm, other, 10), "a calmed creature spares everyone");
        UUID x = UUID.randomUUID(), y = UUID.randomUUID();
        b.add(x, owner, 900, false);
        b.add(y, owner, 900, false);
        assertEquals(2, b.size(), "bounded");
        assertNull(b.get(calm), "the oldest went out");
        b.purge(1000);
        assertEquals(0, b.size());
    }

    @Test
    void theSavingThrowIsExistenceAgainstExistence() {
        assertFalse(Wave3Rules.resists(100, 250), "not even three times");
        assertFalse(Wave3Rules.resists(100, 300), "exactly three times is not more");
        assertTrue(Wave3Rules.resists(100, 301), "far above the caster");
        assertFalse(Wave3Rules.resists(0, 1_000_000), "a caster with no existence data is believed: nothing resists");
        assertTrue(Wave3Rules.outranks(100, 100), "equal counts");
        assertTrue(Wave3Rules.outranks(100, 40));
        assertFalse(Wave3Rules.outranks(100, 101), "the aura does not bite what is stronger");
        assertTrue(Wave3Rules.outranks(0, 5000), "unknown existence: the aura works");
    }

    @Test
    void everyCardFlagIsDistinct() {
        java.util.Set<String> all = new java.util.HashSet<>(java.util.List.of(Wave3Rules.F_ANY, Wave3Rules.F_MOTES, Wave3Rules.F_PRIDE, Wave3Rules.F_DREAMER,
                Wave3Rules.F_TRICORN, Wave3Rules.F_GODKING, Wave3Rules.F_CLAWS, Wave3Rules.F_CLEAN, Wave3Rules.F_WRAPS, Wave3Rules.F_THERMO,
                Wave3Rules.F_GAUNTLETS, Wave3Rules.F_ALCHEMIST));
        assertEquals(12, all.size());
        for (String f : all) assertTrue(f.startsWith("w3_"), f);
    }

    @Test
    void charmBookForgetsAndTheCharmCanBeRemoved() {
        CharmBook b = new CharmBook(4);
        UUID mob = UUID.randomUUID(), owner = UUID.randomUUID();
        b.add(mob, owner, 100, false);
        assertEquals(1, b.size());
        b.remove(mob);
        assertEquals(0, b.size());
        assertFalse(b.spares(mob, owner, 10));
    }

    @Test
    void undeadFortitudeSavesOnceFromAKillingBlow() {
        assertEquals(5f, Wave3Rules.fortitudeCut(5f, 20f, false), "a blow that does not kill passes");
        assertEquals(9f, Wave3Rules.fortitudeCut(30f, 10f, false), "a killing blow leaves one point");
        assertEquals(30f, Wave3Rules.fortitudeCut(30f, 10f, true), "once");
        assertEquals(0f, Wave3Rules.fortitudeCut(30f, 1f, false));
    }
}
