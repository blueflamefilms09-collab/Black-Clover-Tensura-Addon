package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.entity.zogratis.LuciusTable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The three cast spells appended to Lucius's move list, and their geometry. */
class LuciusSpellTableTest {
    @Test
    void theSpellsAreAppendedAndTheOriginalTableIsUntouched() {
        List<LuciusTable.Spec> all = LuciusSpellTable.allSpecs();
        assertEquals(LuciusTable.specs().size() + 5, all.size(), "three spells and the two 1.35.0 late-phase moves");
        assertEquals(LuciusNumbers.JUDGMENT, all.get(LuciusTable.specs().size() + 3).id(), "the new moves come last");
        assertEquals(LuciusNumbers.CHRONO_RINGS, all.get(LuciusTable.specs().size() + 4).id());
        for (int i = 0; i < LuciusTable.specs().size(); i++) assertSame(LuciusTable.specs().get(i), all.get(i), "the old moves keep their index");
        Set<String> ids = new HashSet<>();
        for (LuciusTable.Spec s : all) assertTrue(ids.add(s.id()), "unique id " + s.id());
        assertTrue(LuciusSpellTable.isSpell("megiddo") && LuciusSpellTable.isSpell("true_darkness") && LuciusSpellTable.isSpell("dimension_cutter"));
        assertFalse(LuciusSpellTable.isSpell("time_stop"));
    }

    @Test
    void everySpellIsTelegraphedAndBounded() {
        for (LuciusTable.Spec s : LuciusSpellTable.specs()) {
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.windUpTicks() >= 30);
            assertFalse(s.ultimate(), "the spells are ordinary moves");
            assertTrue(s.cooldownTicks() >= 60 && s.recovery() >= 6 && s.weight() >= 1);
            assertTrue(s.maxRange() > s.minRange() && s.minPower() >= 20 && s.minPower() <= 80);
            assertTrue(s.manaCost() > 0);
            assertNotNull(s.label());
        }
        assertTrue(LuciusNumbers.JUDGMENT_BLAST <= LuciusTable.STORM_BOLT && LuciusNumbers.RING_BLOW <= LuciusTable.STORM_BOLT && LuciusNumbers.JUDGMENT_MAX_MARKS <= 6);
        assertTrue(LuciusNumbers.MEGIDDO_MAX_MARKS <= 6 && LuciusNumbers.MEGIDDO_MAX_HITS_PER_FOE <= 2);
        assertTrue(LuciusNumbers.DARKNESS_BLIND_TICKS <= 60, "at most three seconds of blindness");
        assertTrue(LuciusNumbers.MEGIDDO_BLAST <= LuciusTable.STORM_BOLT && LuciusNumbers.CUTTER_BLADE <= LuciusTable.HALT_BLOW);
    }

    @Test
    void theLaneIsAThinStripInFrontOfTheCaster() {
        assertTrue(SpellMath.inLane(0, 0, 1, 0, 10, 0, 32, 1.3));
        assertTrue(SpellMath.inLane(0, 0, 1, 0, 10, 1.2, 32, 1.3));
        assertFalse(SpellMath.inLane(0, 0, 1, 0, 10, 1.4, 32, 1.3), "step out of the lane");
        assertFalse(SpellMath.inLane(0, 0, 1, 0, -1, 0, 32, 1.3), "nothing behind the caster");
        assertFalse(SpellMath.inLane(0, 0, 1, 0, 33, 0, 32, 1.3), "nothing past the end");
        assertFalse(SpellMath.inLane(0, 0, 0, 0, 1, 0, 32, 1.3), "no direction, no lane");
        assertTrue(SpellMath.inLane(0, 0, 5, 5, 7, 7, 32, 1.3), "the direction is normalised");
        assertTrue(SpellMath.inLane(3, 4, 0, -2, 3, -10, 32, 0.5));
    }

    @Test
    void noFoeTakesMoreBlastsThanTheBudget() {
        SpellMath.HitBudget<UUID> b = new SpellMath.HitBudget<>(2);
        UUID a = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(b.take(a));
        assertTrue(b.take(a));
        assertFalse(b.take(a));
        assertTrue(b.take(c));
        assertEquals(2, b.used(a));
        assertEquals(1, b.used(c));
    }

    @Test
    void theDarknessGrowsOverTheTelegraph() {
        assertEquals(4.0, SpellMath.cloudRadius(48, 48, 18), 1e-9);
        assertEquals(18.0, SpellMath.cloudRadius(0, 48, 18), 1e-9);
        assertTrue(SpellMath.cloudRadius(24, 48, 18) > 4 && SpellMath.cloudRadius(24, 48, 18) < 18);
        assertEquals(18.0, SpellMath.cloudRadius(5, 0, 18), 1e-9);
    }
}
