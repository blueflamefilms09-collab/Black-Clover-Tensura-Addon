package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 3.12.0: the pure rules of "head and feet relics are worn in the Curios slots relic_head / relic_feet". */
class HeadFeetMoveRulesTest {
    @Test
    void onlyHeadAndFeetHaveASlotOfTheirOwn() {
        assertEquals("relic_head", HeadFeetMoveRules.curiosSlot("HEAD"));
        assertEquals("relic_feet", HeadFeetMoveRules.curiosSlot("feet"));
        assertEquals("relic_head", HeadFeetMoveRules.curiosSlot(" Head "));
        for (RelicSlot s : RelicSlot.values()) {
            if (s == RelicSlot.HEAD || s == RelicSlot.FEET) continue;
            assertNull(HeadFeetMoveRules.curiosSlot(s.name()), s.name());
        }
        assertNull(HeadFeetMoveRules.curiosSlot(null));
        assertNull(HeadFeetMoveRules.curiosSlot("nonsense"));
    }

    @Test
    void theSlotsAreTheOnesTheDataDefines() {
        assertTrue(RelicWear.OTHER_SLOTS.contains(HeadFeetMoveRules.curiosSlot("HEAD")));
        assertTrue(RelicWear.OTHER_SLOTS.contains(HeadFeetMoveRules.curiosSlot("FEET")));
    }

    @Test
    void aRightClickGoesToCuriosOnlyWhenOnNotSneakingAndHolding() {
        assertTrue(HeadFeetMoveRules.redirectOnUse(true, "HEAD", false, false));
        assertTrue(HeadFeetMoveRules.redirectOnUse(true, "FEET", false, false));
        assertFalse(HeadFeetMoveRules.redirectOnUse(false, "HEAD", false, false), "the setting off restores the armour slot");
        assertFalse(HeadFeetMoveRules.redirectOnUse(true, "HEAD", true, false), "sneak-use is the attunement ritual");
        assertFalse(HeadFeetMoveRules.redirectOnUse(true, "HEAD", false, true));
        assertFalse(HeadFeetMoveRules.redirectOnUse(true, "CHEST", false, false), "a chest relic stays armour");
        assertFalse(HeadFeetMoveRules.redirectOnUse(true, "NECK", false, false), "a neck relic has the old Curios path");
    }

    @Test
    void aRelicIsOnlyMovedOutOfItsOwnArmourSlot() {
        assertTrue(HeadFeetMoveRules.belongsTo("HEAD", "HEAD"));
        assertTrue(HeadFeetMoveRules.belongsTo("FEET", "feet"));
        assertFalse(HeadFeetMoveRules.belongsTo("HEAD", "FEET"));
        assertFalse(HeadFeetMoveRules.belongsTo("FEET", "HEAD"));
        assertFalse(HeadFeetMoveRules.belongsTo("CHEST", "CHEST"));
        assertFalse(HeadFeetMoveRules.belongsTo("HEAD", "CHEST"));
        assertFalse(HeadFeetMoveRules.belongsTo(null, "HEAD"));
    }

    @Test
    void theLookIsDueOnlyWhenOnAwakeAndPastTheBackOff() {
        assertTrue(HeadFeetMoveRules.due(true, false, 100, 0));
        assertTrue(HeadFeetMoveRules.due(true, false, 100, 100));
        assertFalse(HeadFeetMoveRules.due(true, false, 99, 100));
        assertFalse(HeadFeetMoveRules.due(false, false, 100, 0));
        assertFalse(HeadFeetMoveRules.due(true, true, 100, 0), "a spectator is left alone");
        assertEquals(1000 + HeadFeetMoveRules.RETRY_TICKS, HeadFeetMoveRules.retryAt(1000));
        assertTrue(HeadFeetMoveRules.RETRY_TICKS >= 100, "the back-off is at least five seconds");
    }

    @Test
    void aCuriosWornRelicKeepsTheArmourOfItsArmourForm() {
        List<HeadFeetMoveRules.Share> s = HeadFeetMoveRules.shares(List.of("mask", "robe", "boots"), List.of("HEAD", "CHEST", "FEET"), List.of(3, 8, 1), List.of(0.0, 4.0, 0.75));
        assertEquals(3, s.size(), "the chest relic is not a Curios head/feet relic");
        assertEquals(new HeadFeetMoveRules.Share("mask/armor", false, 3), s.get(0));
        assertEquals(new HeadFeetMoveRules.Share("boots/armor", false, 1), s.get(1));
        assertEquals(new HeadFeetMoveRules.Share("boots/toughness", true, 0.75), s.get(2));
        assertTrue(HeadFeetMoveRules.shares(List.of(), List.of(), List.of(), List.of()).isEmpty());
        assertTrue(HeadFeetMoveRules.shares(List.of("x"), List.of("HEAD"), List.of(0), List.of(0.0)).isEmpty(), "zero gives nothing");
    }

    @Test
    void theNumberOfChangesIsCapped() {
        int n = 50;
        List<String> ids = new java.util.ArrayList<>(), slots = new java.util.ArrayList<>();
        List<Integer> def = new java.util.ArrayList<>();
        List<Double> tough = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) { ids.add("r" + i); slots.add("HEAD"); def.add(2); tough.add(1.0); }
        assertEquals(HeadFeetMoveRules.MAX_SHARES, HeadFeetMoveRules.shares(ids, slots, def, tough).size());
    }
}
