package com.newuniverse.nusmp.labyrinth.mega.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a stack saved by a future version, or edited by hand, must still LOAD in this one, because a stack that fails to decode is an item the player
 * loses. So the value type of the data component never fails: an unknown grade or slot becomes a safe default, a resistance is clamped, ids are matched
 * without regard to case or surrounding spaces, and two infos with the same values are equal (so two stacks stamped alike stack alike and a copy equals
 * its original). The ids themselves are what saved data, JSON, commands and quest arguments use, so they are pinned here too.
 */
class GearInfoTest {

    @Test
    void idsRoundTripAndAreForgiving() {
        for (GearGrade g : GearGrade.values()) {
            assertSame(g, GearGrade.fromId(g.id()));
            assertSame(g, GearGrade.fromId("  " + g.id().toUpperCase() + " "));
        }
        for (GearSlot s : GearSlot.values()) {
            assertSame(s, GearSlot.fromId(s.id()));
            assertSame(s, GearSlot.fromId(s.id().toUpperCase()));
        }
        assertNull(GearGrade.fromId(null));
        assertNull(GearGrade.fromId("unique"), "Unique and above are relics, not standard gear");
        assertNull(GearGrade.fromId("genesis"));
        assertNull(GearSlot.fromId("trident"));
        assertNull(GearSlot.fromId(null));
        assertEquals("normal", GearGrade.NORMAL.id());
        assertEquals("rare", GearGrade.RARE.id());
        assertEquals(3, GearGrade.values().length, "exactly the three grades of the brief");
    }

    @Test
    void theOrderOfTheGradesIsTheOrderOfTheScale() {
        assertEquals(0, GearGrade.NORMAL.ordinal());
        assertEquals(1, GearGrade.SPECIAL.ordinal());
        assertEquals(2, GearGrade.RARE.ordinal());
        assertTrue(GearGrade.RARE.atLeast(GearGrade.SPECIAL));
        assertFalse(GearGrade.NORMAL.atLeast(GearGrade.SPECIAL));
    }

    @Test
    void theSlotsKnowWhatTheyAre() {
        for (GearSlot s : GearSlot.values()) {
            assertEquals(s.isArmour(), !s.isHeld());
            assertEquals(s.isArmour(), s.armourIndex() >= 0);
            assertEquals(!s.isArmour(), s.engraves(), "engravings are for weapons and shields");
        }
        assertEquals(0, GearSlot.HELMET.armourIndex());
        assertEquals(1, GearSlot.CHESTPLATE.armourIndex());
        assertEquals(2, GearSlot.LEGGINGS.armourIndex());
        assertEquals(3, GearSlot.BOOTS.armourIndex());
        assertTrue(GearSlot.SWORD.hasPowerStat() && GearSlot.AXE.hasPowerStat() && GearSlot.BOOTS.hasPowerStat());
        assertFalse(GearSlot.BOW.hasPowerStat() || GearSlot.CROSSBOW.hasPowerStat() || GearSlot.SHIELD.hasPowerStat());
    }

    @Test
    void anInfoNeverFailsToBuild() {
        GearInfo bad = GearInfo.of("mythical", "wand", 400);
        assertEquals(GearGrade.NORMAL, bad.grade());
        assertEquals(GearSlot.SWORD, bad.slot());
        assertEquals(100, bad.magicResistPercent());
        assertEquals(0, GearInfo.of("rare", "boots", -9).magicResistPercent());
        assertEquals(GearGrade.NORMAL, new GearInfo(null, null, 0).grade());
        assertEquals(GearSlot.SWORD, new GearInfo(null, null, 0).slot());
    }

    @Test
    void equalValuesAreEqual() {
        GearInfo a = GearInfo.of("rare", "chestplate", 9), b = new GearInfo(GearGrade.RARE, GearSlot.CHESTPLATE, 9);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, GearInfo.of("rare", "chestplate", 10));
        assertNotEquals(a, GearInfo.of("special", "chestplate", 9));
        assertEquals("rare", a.gradeId());
        assertEquals("chestplate", a.slotId());
    }

    @Test
    void aSpecClonesItsListsAndDescribesItsInfo() {
        java.util.List<GearSpec.Ench> enchants = new java.util.ArrayList<>(java.util.List.of(new GearSpec.Ench("minecraft:sharpness", 2)));
        GearSpec s = new GearSpec(GearGrade.RARE, GearSlot.SWORD, "minecraft:iron_sword", 25, 30, enchants, null, 5);
        enchants.clear();
        assertEquals(1, s.enchants().size(), "the spec is immutable");
        assertTrue(s.engravings().isEmpty());
        assertEquals(new GearInfo(GearGrade.RARE, GearSlot.SWORD, 5), s.info());
        assertEquals(1, new GearSpec.Ench("x:y", -4).level(), "a level is at least 1");
        assertThrowsUnsupported(() -> s.enchants().add(new GearSpec.Ench("a:b", 1)));
    }

    private static void assertThrowsUnsupported(Runnable r) {
        try {
            r.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("expected an UnsupportedOperationException");
    }
}
