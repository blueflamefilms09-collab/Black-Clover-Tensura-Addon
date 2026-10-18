package com.newuniverse.nusmp.heroweapon.weapon.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Vassal Staff must stay inside the balance budget of the design whatever anybody edits later, so the whole table is judged here: every technique
 * passes the budget, the ids are the staff's and unique, the strongest value written in the table really is the strongest the mechanics can produce (a mark
 * and the Sharp Focus passive both counted), and hasting a cooldown can never take it below the floor of its kind.
 */
class StaffNumbersTest {
    @Test
    void everyTechniqueIsWithinTheBudget() {
        for (Budget.Spec s : StaffNumbers.ALL) {
            List<String> bad = Budget.violations(s);
            assertTrue(bad.isEmpty(), bad.toString());
        }
        assertEquals(10, StaffNumbers.ALL.size(), "the staff has ten techniques (at least six are required)");
    }

    @Test
    void idsAreTheStaffsAndUnique() {
        Set<String> seen = new HashSet<>();
        for (Budget.Spec s : StaffNumbers.ALL) {
            assertTrue(s.id().startsWith("staff."), s.id());
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
        }
        long ultimates = StaffNumbers.ALL.stream().filter(Budget.Spec::ultimate).count();
        assertEquals(1L, ultimates, "exactly one ultimate");
    }

    @Test
    void theStrongestValueInTheTableCoversTheMarkAndThePassive() {
        assertEquals((StaffNumbers.ARCANE_BOLT.rawBase() + StaffNumbers.SHARP_BONUS) * (1.0 + StaffNumbers.MARK_BONUS), StaffNumbers.ARCANE_BOLT.rawMax(), 1e-9, "bolt: sharp focus on a marked foe");
        assertTrue(StaffNumbers.ARCANE_BOLT.rawMax() <= 8.0, "the instant bolt never exceeds the 8 raw above which a technique needs a visible wind-up");
        assertEquals(StaffNumbers.marked(StaffNumbers.CHAIN_SPARK.rawBase(), true), StaffNumbers.CHAIN_SPARK.rawMax(), 1e-9);
        assertEquals(StaffNumbers.marked(StaffNumbers.RUNE_FIELD.rawBase(), true), StaffNumbers.RUNE_FIELD.rawMax(), 1e-9);
        assertEquals(StaffNumbers.marked(StaffNumbers.ARCANE_BURST.rawBase(), true), StaffNumbers.ARCANE_BURST.rawMax(), 1e-9);
        assertEquals(StaffNumbers.marked(StaffNumbers.BOMBARDMENT.rawBase(), true), StaffNumbers.BOMBARDMENT.rawMax(), 1e-9);
        assertEquals(StaffNumbers.RUNE_PULSES, StaffNumbers.RUNE_FIELD.hitsPerTarget(), "the rune field pulses as often as the table says");
        assertEquals(StaffNumbers.BOMB_SALVOS, StaffNumbers.BOMBARDMENT.hitsPerTarget(), "the bombardment falls as often as the table says");
        assertEquals(StaffNumbers.MIRROR_RETURNS, StaffNumbers.FLOAT_MIRROR.hitsPerTarget(), "one shooter can take every returned bolt");
        assertTrue(StaffNumbers.PRISM_COPIES_WIDE + StaffNumbers.BOLT_PIERCE_SHARP <= StaffNumbers.ARCANE_BOLT.maxTargets(), "a split bolt never touches more foes than the table says");
        assertTrue(StaffNumbers.CHAIN_JUMPS + 1 <= StaffNumbers.CHAIN_SPARK.maxTargets());
    }

    @Test
    void hastingNeverBreaksTheFloorOfAKind() {
        for (Budget.Spec s : StaffNumbers.ALL) {
            int hasted = Budget.scaled(s.cooldown(), StaffNumbers.MIN_FACTOR, Budget.floor(s.kind()));
            assertTrue(hasted >= Budget.floor(s.kind()), s.id());
            assertTrue(hasted <= s.cooldown(), s.id() + " is never slower than its nominal cooldown");
        }
    }

    @Test
    void theCooldownFactorCombinesHasteAndRhythmWithAFloor() {
        assertEquals(1.0, StaffNumbers.cooldownFactor(false, 0, true), 1e-9);
        assertEquals(1.0, StaffNumbers.cooldownFactor(false, 3, false), 1e-9, "without Quick Chant the rhythm does nothing");
        assertEquals(0.95, StaffNumbers.cooldownFactor(false, 1, true), 1e-9);
        assertEquals(0.85, StaffNumbers.cooldownFactor(false, 3, true), 1e-9);
        assertEquals(0.85, StaffNumbers.cooldownFactor(false, 9, true), 1e-9, "the chain bonus saturates");
        assertEquals(0.65, StaffNumbers.cooldownFactor(true, 0, false), 1e-9);
        assertEquals(0.65 * 0.85, StaffNumbers.cooldownFactor(true, 3, true), 1e-9);
        assertTrue(StaffNumbers.cooldownFactor(true, 3, true) >= StaffNumbers.MIN_FACTOR);
        assertEquals(1.15, StaffNumbers.marked(1.0, true), 1e-9);
        assertEquals(1.0, StaffNumbers.marked(1.0, false), 1e-9);
    }
}
