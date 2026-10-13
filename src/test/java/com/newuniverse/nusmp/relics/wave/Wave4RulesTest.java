package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 4: the small pure rules of the neck relics (timers, the Glass's d6, the Forest, the Beast Masters Amulet, Charm Person). */
class Wave4RulesTest {
    // ---------------------------------------------------------------- timers
    @Test
    void timersExpireAndStayBounded() {
        Wave4Timers t = new Wave4Timers(4);
        UUID a = UUID.randomUUID();
        t.put(a, 100, 0);
        assertTrue(t.active(a, 50));
        assertEquals(100, t.untilOf(a));
        assertFalse(t.active(a, 100), "ends on its tick");
        assertEquals(0, t.untilOf(a), "an expired row is dropped when met");
        for (int i = 0; i < 40; i++) t.put(UUID.randomUUID(), 1000 + i, 10);
        assertTrue(t.size() <= 4, "bounded, got " + t.size());
    }

    @Test
    void aLongerMarkIsKeptAndAShorterOneDoesNot() {
        Wave4Timers t = new Wave4Timers(8);
        UUID a = UUID.randomUUID();
        t.put(a, 500, 0);
        t.put(a, 300, 0);
        assertEquals(500, t.untilOf(a));
        t.put(a, 900, 0);
        assertEquals(900, t.untilOf(a));
        t.put(a, 10, 20);                                   // already over: ignored
        assertEquals(900, t.untilOf(a));
        t.remove(a);
        assertFalse(t.active(a, 0));
    }

    @Test
    void aFullTableGivesUpTheSoonestEnding() {
        Wave4Timers t = new Wave4Timers(2);
        UUID soon = UUID.randomUUID(), late = UUID.randomUUID(), newer = UUID.randomUUID();
        t.put(soon, 100, 0);
        t.put(late, 900, 0);
        t.put(newer, 500, 0);
        assertFalse(t.active(soon, 1));
        assertTrue(t.active(late, 1));
        assertTrue(t.active(newer, 1));
    }

    // ---------------------------------------------------------------- the Glass's d6
    @Test
    void theGlassRollsOneAndTwoClearAndThreeToSixSharp() {
        assertEquals(Wave4Glass.CLOUDED, Wave4Glass.kindOf(1));
        assertEquals(Wave4Glass.CLOUDED, Wave4Glass.kindOf(2));
        for (int d = 3; d <= 6; d++) assertEquals(Wave4Glass.KEEN, Wave4Glass.kindOf(d));
        assertEquals(1.5f, Wave4Glass.multiplier(Wave4Glass.KEEN), 1e-6);
        assertEquals(0.7f, Wave4Glass.multiplier(Wave4Glass.CLOUDED), 1e-6);
        assertEquals(1f, Wave4Glass.multiplier(0), 1e-6);
    }

    @Test
    void theGlassMarkLastsOneBlowOrTenSeconds() {
        Wave4Glass.clear();
        UUID p = UUID.randomUUID();
        Wave4Glass.arm(p, Wave4Glass.KEEN, 1000);
        assertTrue(Wave4Glass.armed(p, 1100));
        assertEquals(Wave4Glass.KEEN, Wave4Glass.take(p, 1100));
        assertEquals(0, Wave4Glass.take(p, 1100), "one blow only");
        Wave4Glass.arm(p, Wave4Glass.CLOUDED, 1000);
        assertEquals(0, Wave4Glass.take(p, 1000 + Wave4Glass.WINDOW), "ten seconds later it is gone");
        Wave4Glass.arm(p, Wave4Glass.KEEN, 0);
        Wave4Glass.forget(p);
        assertFalse(Wave4Glass.armed(p, 1));
        for (int i = 0; i < 1000; i++) Wave4Glass.arm(UUID.randomUUID(), 1, 0);
        Wave4Glass.clear();
        assertFalse(Wave4Glass.armed(p, 1));
    }

    // ---------------------------------------------------------------- the Forest
    @Test
    void everySpiritHasANameAnIdAndSomethingToGive() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Wave4Forest s : Wave4Forest.values()) {
            assertTrue(ids.add(s.id), "duplicate id " + s.id);
            assertSame(s, Wave4Forest.byId(s.id));
            assertSame(s, Wave4Forest.byId(s.id.toUpperCase()));
            assertTrue(s.mods.length + s.effects.length > 0 || s == Wave4Forest.RACCOON || s == Wave4Forest.OWL, s.id + " gives nothing");
            for (Wave4Forest.Mod m : s.mods) assertTrue(m.amount() != 0, s.id + " has a zero modifier");
        }
        assertEquals(6, Wave4Forest.values().length);
        assertNull(Wave4Forest.byId("dragon"));
        assertNull(Wave4Forest.byId(null));
        assertNull(Wave4Forest.byId(""));
    }

    @Test
    void lentSkillsAreRealTensuraIds() {
        // the five ids confirmed to exist in the repo; the forest lends only these
        java.util.Set<String> verified = java.util.Set.of("tensura:magic_sense", "tensura:sage", "tensura:danger_sense",
                "tensura:physical_attack_resistance", "tensura:poison_resistance");
        int lent = 0;
        for (Wave4Forest s : Wave4Forest.values()) for (String id : s.skills) { assertTrue(verified.contains(id), id); lent++; }
        assertEquals(5, lent, "cat 1, beetle 2, raccoon 1, owl 1");
        assertEquals(0, Wave4Forest.BEAR.skills.length);
        assertEquals(0, Wave4Forest.PEACOCK.skills.length);
    }

    @Test
    void blessingsLastADayAndTheBeetleHealsMore() {
        assertEquals(24000, Wave4Forest.DURATION);
        assertTrue(Wave4Forest.active(100, 99));
        assertFalse(Wave4Forest.active(100, 100));
        assertEquals(1.5f, Wave4Forest.healMultiplier(Wave4Forest.BEETLE), 1e-6);
        assertEquals(1f, Wave4Forest.healMultiplier(Wave4Forest.BEAR), 1e-6);
        assertEquals(20, Wave4Forest.minutesLeft(24000, 0));
        assertEquals(1, Wave4Forest.minutesLeft(1, 0));
        assertEquals(0, Wave4Forest.minutesLeft(0, 5));
    }

    @Test
    void theBearGrowsTheCatRunsAndNothingIsExcessive() {
        boolean scale = false, speed = false;
        for (Wave4Forest.Mod m : Wave4Forest.BEAR.mods) if (m.attr().equals("scale")) { scale = true; assertTrue(m.amount() > 0 && m.amount() <= 0.3, "one size larger, not a giant"); }
        for (Wave4Forest.Mod m : Wave4Forest.CAT.mods) if (m.attr().equals("movement_speed")) { speed = true; assertTrue(m.amount() > 0 && m.amount() <= 0.3); }
        assertTrue(scale && speed);
        for (Wave4Forest s : Wave4Forest.values()) for (Wave4Forest.Mod m : s.mods) {
            if (m.op() == Wave4Forest.Mod.ADD && m.attr().equals("max_health")) assertTrue(m.amount() <= 4.0);
            if (m.attr().equals("armor")) assertTrue(m.amount() <= 4.0);
        }
    }

    // ---------------------------------------------------------------- the Beast Masters Amulet
    @Test
    void creaturesWeighByMaximumHealth() {
        assertEquals(1, Wave4Beasts.weight(4));
        assertEquals(1, Wave4Beasts.weight(8));
        assertEquals(2, Wave4Beasts.weight(10));
        assertEquals(2, Wave4Beasts.weight(16));
        assertEquals(5, Wave4Beasts.weight(20));
        assertEquals(5, Wave4Beasts.weight(40));
        assertEquals(10, Wave4Beasts.weight(41));
        assertEquals(10, Wave4Beasts.weight(80));
        assertEquals(0, Wave4Beasts.weight(81), "a boss does not fit");
        assertEquals(0, Wave4Beasts.weight(0));
        assertEquals(0, Wave4Beasts.weight(Float.NaN));
        assertEquals(0, Wave4Beasts.weight(-3));
    }

    @Test
    void theAmuletHoldsTenPoints() {
        assertTrue(Wave4Beasts.fits(0, 10));
        assertFalse(Wave4Beasts.fits(1, 10));
        assertTrue(Wave4Beasts.fits(9, 1));
        assertFalse(Wave4Beasts.fits(10, 1));
        assertFalse(Wave4Beasts.fits(0, 0), "weight 0 never fits");
        int used = 0, n = 0;
        while (Wave4Beasts.fits(used, 1)) { used += 1; n++; }
        assertEquals(10, n, "ten of the weakest");
        used = 0; n = 0;
        while (Wave4Beasts.fits(used, 2)) { used += 2; n++; }
        assertEquals(5, n, "five of the next");
        used = 0; n = 0;
        while (Wave4Beasts.fits(used, 5)) { used += 5; n++; }
        assertEquals(2, n, "two of the next");
    }

    @Test
    void unwillingCreaturesMustBeWoundedFirst() {
        assertTrue(Wave4Beasts.willing(true, 20, 20), "your own pet is always willing");
        assertFalse(Wave4Beasts.willing(false, 20, 20));
        assertFalse(Wave4Beasts.willing(false, 11, 20));
        assertTrue(Wave4Beasts.willing(false, 10, 20));
        assertTrue(Wave4Beasts.willing(false, 1, 20));
    }

    // ---------------------------------------------------------------- Charm Person
    @Test
    void charmReachesHumanoidsOnly() {
        assertTrue(Wave4Charm.humanoid("minecraft", "pillager"));
        assertTrue(Wave4Charm.humanoid("minecraft", "zombie"));
        assertTrue(Wave4Charm.humanoid("tensura", "goblin"));
        assertFalse(Wave4Charm.humanoid("minecraft", "creeper"));
        assertFalse(Wave4Charm.humanoid("minecraft", "wolf"));
        assertFalse(Wave4Charm.humanoid("minecraft", "ender_dragon"));
        assertFalse(Wave4Charm.humanoid("othermod", "zombie"), "only the vanilla and Tensura namespaces are named");
        assertFalse(Wave4Charm.humanoid("minecraft", null));
        assertEquals(5.0, Wave4Charm.RADIUS, 1e-9, "fifteen feet");
    }

    @Test
    void theMightyShrugTheCharmOff() {
        assertFalse(Wave4Charm.resists(20));
        assertFalse(Wave4Charm.resists(39.9f));
        assertTrue(Wave4Charm.resists(40));
        assertTrue(Wave4Charm.resists(300));
    }
}
