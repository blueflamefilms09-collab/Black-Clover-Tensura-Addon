package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The drawn-weapon flags of a def (.bow() / .crossbow()), which the catalog turns into RelicBowItem / RelicCrossbowItem. */
class RelicRangedDefTest {
    private static RelicDef base() { return RelicDef.of("test_bow", "Test Bow", Grade.RARE, RelicSlot.MAIN_HAND); }

    @Test
    void aDefIsNotRangedByDefault() {
        assertEquals(RelicDef.RangedKind.NONE, base().rangedKind());
        assertFalse(base().isRanged());
    }

    @Test
    void bowAndCrossbowSetTheKindAndLeaveTheOriginalAlone() {
        RelicDef b = base();
        RelicDef bow = b.bow(), xbow = b.crossbow();
        assertEquals(RelicDef.RangedKind.BOW, bow.rangedKind());
        assertEquals(RelicDef.RangedKind.CROSSBOW, xbow.rangedKind());
        assertTrue(bow.isRanged() && xbow.isRanged());
        assertEquals(RelicDef.RangedKind.NONE, b.rangedKind(), "defs are immutable");
    }

    @Test
    void theKindSurvivesEveryOtherFluentCall() {
        RelicDef d = base().bow().attune().model3d().desc("x").engrave("severance").melee(5, 1.2).passive(Passive.flag("a")).palette(1, 2, 3).look(Look.BOW).vessel();
        assertEquals(RelicDef.RangedKind.BOW, d.rangedKind());
        assertEquals(RelicDef.RangedKind.CROSSBOW, d.crossbow().attune().rangedKind(), "the last kind wins");
        assertEquals(RelicDef.RangedKind.NONE, d.ranged(null).rangedKind(), "null means not ranged");
        assertEquals(RelicDef.RangedKind.NONE, d.ranged(RelicDef.RangedKind.NONE).rangedKind());
    }
}
