package com.newuniverse.nusmp.fairytail.storm;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The spell table of the Stormcaller Staff: append-only ordinals, every spell is a real storm with real numbers, and no spell out-damages the Fairy Tail skills it stands next to. */
class StormSpellTest {
    @Test
    void theOrdinalsAreStoredAndSoOnlyEverAppended() {
        // the order the spells shipped in: a staff stores the ordinal, the clients get it in the seed
        String[] shipped = {"gathering_clouds", "downpour", "hailstorm", "thunderhead", "gale_front", "storm_surge", "twister", "eye_of_the_storm"};
        StormSpell[] now = StormSpell.values();
        assertTrue(now.length >= shipped.length);
        for (int i = 0; i < shipped.length; i++) assertEquals(shipped[i], now[i].id, "spell " + i + " moved");
        assertTrue(now.length <= 16, "the seed has four bits for the spell");
    }

    @Test
    void everySpellIsAStormWithRealNumbers() {
        Set<String> ids = new HashSet<>();
        for (StormSpell s : StormSpell.values()) {
            assertTrue(ids.add(s.id), "duplicate " + s.id);
            assertTrue(s.id.matches("[a-z_]+"), s.id);
            assertNotNull(s.description);
            assertFalse(s.description.isBlank());
            assertTrue(s.mask != 0 && s.mask <= 0xFF, s.id + " mask");
            assertTrue(s.costPercent >= 1 && s.costPercent <= 30, s.id + " cost");
            assertTrue(s.cooldownTicks >= 60, s.id + " cooldown");
            assertTrue(s.durationTicks >= 20 && s.durationTicks <= 400, s.id + " duration");
            assertTrue(s.radius > 0 && s.radius <= 16, s.id + " radius");
            assertTrue(s.driftPerSecond >= 0 && s.driftPerSecond <= 4, s.id + " drift");
            if (s.has(StormComp.BOLTS)) assertTrue(s.boltRaw > 0 && s.maxBolts > 0 && s.boltEvery >= 8, s.id + " bolts");
            if (s.has(StormComp.HAIL)) assertTrue(s.hailRaw > 0, s.id + " hail");
            if (s.isLine()) assertTrue(s.aimRange > 0 && s.aimRange <= StormMath.MAX_AIM && s.tickRaw > 0 && s.push > 0 && s.durationTicks <= 60, s.id + " line");
            else assertTrue(s.aimRange <= StormMath.MAX_AIM, s.id + " aim");
            assertTrue(s.aimRange >= 0);
        }
    }

    @Test
    void theSpellsMapTheStormMagicOfTheSeries() {
        // clouds, rain, hail, thunder and lightning out of the cloud, gales and a tornado-like wind, a surge of storm water, and a whole sky
        int all = 0;
        for (StormSpell s : StormSpell.values()) all |= s.mask;
        for (int part : new int[]{StormComp.RAIN, StormComp.HAIL, StormComp.BOLTS, StormComp.GUST, StormComp.FUNNEL, StormComp.EYE, StormComp.FRONT, StormComp.SURGE})
            assertTrue((all & part) != 0, "no spell has weather part " + part);
        assertTrue(StormSpell.EYE_OF_THE_STORM.has(StormComp.RAIN | StormComp.HAIL | StormComp.BOLTS | StormComp.GUST | StormComp.EYE), "the ultimate is every part of the cell");
        assertTrue(StormSpell.GALE_FRONT.isLine() && StormSpell.STORM_SURGE.isLine());
        assertFalse(StormSpell.TWISTER.isLine());
    }

    @Test
    void lookupAndCyclingNeverFail() {
        assertSame(StormSpell.GATHERING_CLOUDS, StormSpell.at(-5));
        assertSame(StormSpell.GATHERING_CLOUDS, StormSpell.at(999));
        assertSame(StormSpell.HAILSTORM, StormSpell.byId("hailstorm"));
        assertNull(StormSpell.byId("nope"));
        assertNull(StormSpell.byId(null));
        StormSpell s = StormSpell.at(0);
        for (int i = 0; i < StormSpell.count(); i++) s = s.next();
        assertSame(StormSpell.at(0), s, "turning through every spell comes back to the first");
    }

    @Test
    void noStormOutDamagesTheFairyTailSkillsItStandsNextTo() {
        // the Fairy Tail roars deal 9 to 11 raw every 4.5 to 6 s and its close blows 9 to 10 every 2 to 2.5 s (2 to 4 raw a second); a storm cast again the moment it is ready, with every cap hit on one creature, stays under 3 a second
        for (StormSpell s : StormSpell.values()) {
            double perSecond = StormRules.worstCaseRawPerSecond(s);
            assertTrue(perSecond <= 3.0, s.id + " worst case " + perSecond + " raw per second");
            assertTrue(StormRules.worstCaseRawPerTarget(s) <= 80.0, s.id + " takes " + StormRules.worstCaseRawPerTarget(s) + " from one creature");
        }
        assertTrue(StormRules.worstCaseRawPerTarget(StormSpell.GALE_FRONT) == StormSpell.GALE_FRONT.tickRaw, "a front is one blow");
    }

    @Test
    void theServersCapsNarrowAndShortenASpell() {
        assertEquals(12.0, StormRules.effectiveRadius(StormSpell.EYE_OF_THE_STORM, 16.0), 1e-9, "the spell asks for 12 and the server allows 16");
        assertEquals(5.0, StormRules.effectiveRadius(StormSpell.EYE_OF_THE_STORM, 5.0), 1e-9);
        assertEquals(1.0, StormRules.effectiveRadius(StormSpell.EYE_OF_THE_STORM, 0.0), 1e-9);
        assertEquals(240, StormRules.effectiveDuration(StormSpell.EYE_OF_THE_STORM, 400));
        assertEquals(100, StormRules.effectiveDuration(StormSpell.EYE_OF_THE_STORM, 100));
        assertEquals(20, StormRules.effectiveDuration(StormSpell.EYE_OF_THE_STORM, 1), "never under a second");
    }

    @Test
    void staffOnlyHidesTheOldGrimoireFromTheRollsAndNothingElse() {
        assertTrue(StormRules.hiddenName("STORM", true));
        assertFalse(StormRules.hiddenName("STORM", false), "staffOnly false restores the old grimoire behaviour");
        for (String other : new String[]{"WIND", "LIGHTNING", "AIR", "VORTEX", "WING", "SOUND", "FLAME"}) assertFalse(StormRules.hiddenName(other, true), other);
        assertFalse(StormRules.hiddenName(null, true));
    }
}
