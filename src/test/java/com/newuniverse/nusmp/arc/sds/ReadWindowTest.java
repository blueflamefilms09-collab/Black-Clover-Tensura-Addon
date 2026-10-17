package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.DamageKind;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Light of Prying Eyes is the rule that answers a party that does one thing as hard as it can, so what it reads, what it cuts, for how long and how it ends are pinned: it can never make a kind of damage harmless. */
class ReadWindowTest {
    @Test
    void heReadsTheKindDealtTheMostInTheLastEightSeconds() {
        ReadWindow w = new ReadWindow(GowtherTable.READ_SPAN);
        w.record(100, DamageKind.MELEE, 30f);
        w.record(110, DamageKind.SPELL, 10f);
        w.record(120, DamageKind.MELEE, 15f);
        assertEquals(DamageKind.MELEE, w.dominant(130, GowtherTable.READ_MIN));
        Map<DamageKind, Float> t = w.tally(130);
        assertEquals(45f, t.get(DamageKind.MELEE), 1e-6f);
        assertEquals(10f, t.get(DamageKind.SPELL), 1e-6f);
    }

    @Test
    void aPartyThatHasBarelyHitHimIsNotRead() {
        ReadWindow w = new ReadWindow(160);
        assertNull(w.dominant(0, 20f), "nothing dealt");
        w.record(5, DamageKind.SPELL, 12f);
        assertNull(w.dominant(10, 20f), "12 is under the minimum");
        w.record(6, DamageKind.SPELL, 12f);
        assertEquals(DamageKind.SPELL, w.dominant(10, 20f));
    }

    @Test
    void oldBlowsFallOutOfTheWindow() {
        ReadWindow w = new ReadWindow(160);
        w.record(0, DamageKind.FIRE, 100f);
        assertEquals(DamageKind.FIRE, w.dominant(159, 20f));
        assertNull(w.dominant(160, 20f), "exactly eight seconds later he has forgotten");
    }

    @Test
    void aTieGoesToTheEarlierKindOfTheEnum() {
        ReadWindow w = new ReadWindow(160);
        w.record(0, DamageKind.SPELL, 20f);
        w.record(1, DamageKind.MELEE, 20f);
        assertEquals(DamageKind.MELEE, w.dominant(2, 10f), "MELEE comes before SPELL");
    }

    @Test
    void aReadingCutsOnlyTheKindItReadAndOnlyWhileItLasts() {
        ReadWindow w = new ReadWindow(160);
        assertEquals(1f, w.factor(0, DamageKind.MELEE, 0.55f), 1e-6f, "no reading, no cut");
        w.begin(100, DamageKind.MELEE, 160);
        assertTrue(w.active(100) && w.active(259));
        assertEquals(DamageKind.MELEE, w.reading(150));
        assertEquals(0.45f, w.factor(150, DamageKind.MELEE, 0.55f), 1e-6f);
        assertEquals(1f, w.factor(150, DamageKind.SPELL, 0.55f), 1e-6f, "every other kind lands in full");
        assertEquals(1f, w.factor(260, DamageKind.MELEE, 0.55f), 1e-6f, "the reading is over");
        assertFalse(w.active(260));
        assertNull(w.reading(260));
        assertEquals(60, w.ticksLeft(200));
        assertEquals(0, w.ticksLeft(300));
    }

    @Test
    void theCutIsBoundedSoNoKindIsEverHarmless() {
        ReadWindow w = new ReadWindow(160);
        w.begin(0, DamageKind.ICE, 100);
        assertEquals(0.1f, w.factor(1, DamageKind.ICE, 5f), 1e-6f, "at most 90% is turned aside");
        assertEquals(1f, w.factor(1, DamageKind.ICE, -3f), 1e-6f, "a negative cut is no cut");
    }

    @Test
    void endingAReadingAndResettingForgetEverything() {
        ReadWindow w = new ReadWindow(160);
        w.record(0, DamageKind.MELEE, 50f);
        w.begin(0, DamageKind.MELEE, 100);
        w.end();
        assertFalse(w.active(1));
        assertEquals(DamageKind.MELEE, w.dominant(1, 10f), "ending the reading keeps what he has seen");
        w.reset();
        assertNull(w.dominant(1, 10f));
    }

    @Test
    void badBlowsAreIgnored() {
        ReadWindow w = new ReadWindow(160);
        w.record(0, null, 10f);
        w.record(0, DamageKind.SPELL, Float.NaN);
        w.record(0, DamageKind.SPELL, -4f);
        assertTrue(w.tally(0).isEmpty());
    }
}
