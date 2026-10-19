package com.newuniverse.nusmp.labyrinth.mega.lock;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the list of ways the dungeon can lock a player in is the owner's checklist ("find every way"), kept as data so it cannot silently grow a lock that has no way out. A case
 * that can hold a person must have an automatic way out (a timer, a rune, an exit of its own, or a generator proof) AND the command {@code /dungeon unstuck}; a case that was
 * checked and cannot hold anyone must say what keeps it that way. Ids are unique and every text is a sentence.
 */
class LockCaseTest {
    @Test void everyCaseIsCovered() {
        for (LockCase c : LockCase.values()) assertTrue(c.covered(), c.id() + " can hold a person and has no way out: " + c.guarantees());
    }

    @Test void aCaseThatCanHoldAPersonHasAnAutomaticWayOutAndTheCommand() {
        for (LockCase c : LockCase.values()) {
            if (!c.canHold()) continue;
            assertTrue(c.guarantees().contains(LockCase.Guarantee.UNSTUCK), c.id() + " has no /dungeon unstuck");
            boolean auto = c.guarantees().contains(LockCase.Guarantee.TIMED_RELEASE) || c.guarantees().contains(LockCase.Guarantee.RUNE)
                    || c.guarantees().contains(LockCase.Guarantee.OWN_EXIT) || c.guarantees().contains(LockCase.Guarantee.GENERATOR_CHECK);
            assertTrue(auto, c.id() + " has no automatic way out");
        }
    }

    @Test void theLocksThatBarADoorHaveATimerAndARune() {
        for (LockCase c : new LockCase[] {LockCase.TRIAL_ARCHWAY, LockCase.MECHANIC_SEAL_DOORS, LockCase.MECHANIC_CAGE, LockCase.MECHANIC_BOSS_CAGE, LockCase.UNSOLVABLE_COUNTER}) {
            assertTrue(c.canHold(), c.id());
            assertTrue(c.guarantees().contains(LockCase.Guarantee.TIMED_RELEASE) && c.guarantees().contains(LockCase.Guarantee.RUNE), c.id());
        }
    }

    @Test void idsAreUniqueAndTextsAreSentences() {
        Set<String> ids = new HashSet<>();
        for (LockCase c : LockCase.values()) {
            assertTrue(ids.add(c.id()), "duplicate id " + c.id());
            assertFalse(c.what().isBlank());
            assertTrue(c.what().endsWith("."), c.id());
        }
        assertEquals(LockCase.values().length, ids.size());
    }

    @Test void theGeneratorAndTheUnbreakableWallsAreInTheList() {
        assertTrue(LockCase.GENERATOR_POCKET.guarantees().contains(LockCase.Guarantee.GENERATOR_CHECK));
        assertTrue(LockCase.UNREACHABLE_SPAWN.guarantees().contains(LockCase.Guarantee.GENERATOR_CHECK));
        assertTrue(LockCase.UNBREAKABLE_WALLS.guarantees().contains(LockCase.Guarantee.UNSTUCK), "digging out is impossible, so the way out must not need a wall to break");
    }
}
