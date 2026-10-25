package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.worn.lib.WornPlacer.Placement;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The anchor allocator: several relics of one slot never share a place, index 0 is the untouched look, head items and the shrink behave. */
class WornPlacerTest {
    static final String[] SLOTS = {"CHARM", "NECK", "HEAD", "BACK", "BELT", "HANDS", "FEET", "CHEST", "MAIN_HAND"};

    @Test
    void indexZeroIsUntouched() {
        for (String s : SLOTS) assertEquals(Placement.NONE, WornPlacer.place(s, 0), s);
        assertEquals(Placement.NONE, WornPlacer.place("CHARM", -3));
        assertEquals(Placement.NONE, WornPlacer.place(null, 5));
    }

    @Test
    void noTwoRelicsOfASlotShareAPlace() {
        for (String s : SLOTS) {
            Set<Placement> seen = new HashSet<>();
            for (int i = 0; i < 12; i++) assertTrue(seen.add(WornPlacer.place(s, i)), s + " index " + i + " repeats an earlier place");
        }
    }

    @Test
    void placesAreDeterministic() {
        for (String s : SLOTS) for (int i = 0; i < 8; i++) assertEquals(WornPlacer.place(s, i), WornPlacer.place(s, i));
        assertEquals(WornPlacer.place("neck", 2), WornPlacer.place("NECK", 2), "the slot name is case-blind");
    }

    @Test
    void necklacesHangLowerAndStaySane() {
        for (int i = 1; i < 6; i++) {
            assertTrue(WornPlacer.place("NECK", i).dy() > WornPlacer.place("NECK", i - 1).dy());
            assertTrue(WornPlacer.place("NECK", i).scale() >= 0.6f);
        }
    }

    @Test
    void charmsSpreadAroundTheBody() {
        assertTrue(WornPlacer.place("CHARM", 1).mirror(), "the second charm is on the other hip");
        assertEquals(180f, WornPlacer.place("CHARM", 2).rotY(), 0f, "the third is on the back");
        assertTrue(WornPlacer.place("CHARM", 5).dy() > 0f, "past four they hang lower");
    }

    @Test
    void headItemsStackThenGoToTheSides() {
        assertTrue(WornPlacer.place("HEAD", 1).dy() < 0f, "the second head item sits higher");
        assertTrue(WornPlacer.place("HEAD", 2).dx() * WornPlacer.place("HEAD", 3).dx() < 0f, "then left and right");
    }

    @Test
    void handsMoveAlongTheArm() {
        assertTrue(WornPlacer.place("HANDS", 1).dy() < 0f);
        assertTrue(WornPlacer.place("HANDS", 2).dy() > 0f);
    }

    @Test
    void indexesCountPerSlot() {
        assertArrayEquals(new int[]{0, 0, 1, 2, 1}, WornPlacer.indexes(List.of("CHARM", "NECK", "CHARM", "CHARM", "neck")));
        assertArrayEquals(new int[0], WornPlacer.indexes(List.of()));
    }

    @Test
    void bodyModelsAreSmallerHeadModelsAreNot() {
        assertEquals(Placement.NONE, WornPlacer.fit(WornAnchor.HEAD, "crown_spired"));
        assertEquals(WornPlacer.BODY_SCALE, WornPlacer.fit(WornAnchor.BODY, "chest_collar").scale(), 0f);
        assertTrue(WornPlacer.BODY_SCALE < 1f && WornPlacer.BODY_SCALE > 0.7f);
    }

    @Test
    void armPiecesMoveTowardTheBody() {
        Placement r = WornPlacer.fit(WornAnchor.RIGHT_ARM, "pauldron_plate"), l = WornPlacer.fit(WornAnchor.LEFT_ARM, "pauldron_plate");
        assertTrue(r.dx() > 0f && l.dx() < 0f, "inward for both arms");
        assertTrue(r.scale() < WornPlacer.BODY_SCALE);
        assertEquals(0f, WornPlacer.fit(WornAnchor.RIGHT_ARM, "ring_gem").dx(), 0f, "a ring is not a shoulder piece");
        assertTrue(WornPlacer.hugsBody("bracer_phasing") && WornPlacer.hugsBody("mantle_shoulder") && WornPlacer.hugsBody("gauntlet_fist") && !WornPlacer.hugsBody("crown_duck"));
    }

    @Test
    void combineAddsOffsetsAndMultipliesScales() {
        Placement p = new Placement(1, 2, 3, 10, 0.5f, true).then(new Placement(1, 1, 1, 5, 0.5f, true));
        assertEquals(new Placement(2, 3, 4, 15, 0.25f, false), p);
    }

    /** 3.9.0: the shrink is about the model's own middle, so what was at the sole, the hand or the hem stays there (within the shrink of half the model height). */
    @Test
    void contactFitKeepsTheMiddleOfAModelWhereItWas() {
        for (WornAnchor a : new WornAnchor[]{WornAnchor.RIGHT_LEG, WornAnchor.LEFT_LEG, WornAnchor.BODY, WornAnchor.RIGHT_ARM, WornAnchor.LEFT_ARM}) {
            Placement p = WornPlacer.fit(a, "boots_cuffed", 8.9f, true);
            assertEquals(8.9f, p.dy() + p.scale() * 8.9f, 0.001f, a.name() + ": the middle stays");
            float sole = p.dy() + p.scale() * 12.6f;
            assertTrue(Math.abs(sole - 12.6f) <= 0.6f, a + " keeps the lower end close to where the file put it: " + sole);
        }
        Placement old = WornPlacer.fit(WornAnchor.RIGHT_LEG, "boots_cuffed");
        assertTrue(old.scale() * 12.6f < 11.0f, "the 3.8 placement lifted the sole by more than a pixel");
        assertEquals(old, WornPlacer.fit(WornAnchor.RIGHT_LEG, "boots_cuffed", 8.9f, false), "the switch off gives the 3.8 placement");
        assertEquals(Placement.NONE, WornPlacer.fit(WornAnchor.HEAD, "crown_spired", -9f, true), "a head item is never touched");
    }

    @Test
    void hipCharmsHangBesideTheThighNotInsideTheArm() {
        assertTrue(WornPlacer.hipHung("charm_flask") && WornPlacer.hipHung("charm_tome") && WornPlacer.hipHung("charm_pouch") && WornPlacer.hipHung("charm_pouch_pair"));
        assertFalse(WornPlacer.hipHung("charm_orbit") || WornPlacer.hipHung("amulet_medallion") || WornPlacer.hipHung(null));
        float lowered = WornPlacer.fit(WornAnchor.BODY, "charm_flask", 12.6f, true).dy() - WornPlacer.fit(WornAnchor.BODY, "charm_flask", 12.6f, false).dy();
        assertEquals(WornPlacer.HIP_DROP + 12.6f * (1f - WornPlacer.BODY_SCALE), lowered, 0.001f);
        assertEquals(WornPlacer.fit(WornAnchor.BODY, "charm_flask"), WornPlacer.fit(WornAnchor.BODY, "charm_flask", 12.6f, false), "off = the 3.8 placement");
    }
}
