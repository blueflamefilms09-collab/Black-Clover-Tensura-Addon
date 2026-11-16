package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.BeastKind;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.Move;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dungeon-boss rework (docs/dungeon_bosses_rework.md): the pure numbers and rules of the Floor Warden's book and the creatures' moves. No Minecraft class is loaded.
 */
class DungeonReworkRulesTest {
    @Test
    void theWardenHasFiveMovesAndOneUltimate() {
        List<Move> book = DungeonReworkRules.WARDEN;
        assertEquals(5, book.size());
        long ultimates = book.stream().filter(Move::ultimate).count();
        assertEquals(1, ultimates);
        assertTrue(book.get(DungeonReworkRules.W_TOLL).ultimate());
        assertEquals(3, book.get(DungeonReworkRules.W_TOLL).minPhase(), "the ultimate comes at a quarter of its health");
        assertEquals(2, book.get(DungeonReworkRules.W_PULSE).minPhase());
        Set<String> ids = new HashSet<>();
        for (Move m : book) {
            assertTrue(ids.add(m.id()), "unique id " + m.id());
            assertTrue(m.windup() >= 12, m.id() + " has a telegraph of at least 0.6 s");
            assertTrue(m.cooldown() >= DungeonReworkRules.MIN_COOLDOWN, m.id() + " cooldown");
            assertTrue(m.maxRange() > m.minRange(), m.id() + " window");
        }
    }

    @Test
    void phasesChangeAtHalfAndAQuarter() {
        assertEquals(1, DungeonReworkRules.phase(1.0));
        assertEquals(1, DungeonReworkRules.phase(0.51));
        assertEquals(2, DungeonReworkRules.phase(0.5));
        assertEquals(2, DungeonReworkRules.phase(0.26));
        assertEquals(3, DungeonReworkRules.phase(0.25));
        assertEquals(3, DungeonReworkRules.phase(0.0));
    }

    @Test
    void cooldownsShrinkWithThePhaseAndTheEnrageButNeverUnderTheFloor() {
        assertEquals(100, DungeonReworkRules.cooldown(100, 1, false));
        assertEquals(85, DungeonReworkRules.cooldown(100, 2, false));
        assertEquals(70, DungeonReworkRules.cooldown(100, 3, false));
        assertEquals(56, DungeonReworkRules.cooldown(100, 3, true));
        assertEquals(DungeonReworkRules.MIN_COOLDOWN, DungeonReworkRules.cooldown(10, 3, true));
    }

    @Test
    void theEnrageComesAfterTwoAndAHalfMinutes() {
        assertFalse(DungeonReworkRules.enraged(DungeonReworkRules.ENRAGE_TICKS - 1));
        assertTrue(DungeonReworkRules.enraged(DungeonReworkRules.ENRAGE_TICKS));
        assertEquals(3000, DungeonReworkRules.ENRAGE_TICKS);
        assertEquals(1.0, DungeonReworkRules.damageFactor(false));
        assertEquals(1.2, DungeonReworkRules.damageFactor(true));
    }

    @Test
    void theChoiceFollowsDistancePhaseAndCooldown() {
        List<Move> book = DungeonReworkRules.WARDEN;
        long[] ready = new long[book.size()];
        // point blank, phase 1: only the Hammer Slam fits (the Seal Pulse needs phase 2)
        for (int roll = -3; roll < 12; roll++) assertEquals(DungeonReworkRules.W_SLAM, DungeonReworkRules.choose(book, 2.0, 1, 100, ready, roll));
        // far: the lash or the step, never the slam
        for (int roll = -3; roll < 12; roll++) {
            int c = DungeonReworkRules.choose(book, 15.0, 1, 100, ready, roll);
            assertTrue(c == DungeonReworkRules.W_LASH || c == DungeonReworkRules.W_STEP, "far gave " + c);
        }
        // nothing ready: -1
        long[] busy = {1000, 1000, 1000, 1000, 1000};
        assertEquals(-1, DungeonReworkRules.choose(book, 2.0, 2, 100, busy, 0));
        // the ultimate wins when it is ready and in phase 3, whatever the roll
        for (int roll = -3; roll < 12; roll++) assertEquals(DungeonReworkRules.W_TOLL, DungeonReworkRules.choose(book, 8.0, 3, 100, ready, roll));
        // ... and is never drawn in phase 2
        for (int roll = -3; roll < 40; roll++) assertTrue(DungeonReworkRules.choose(book, 8.0, 2, 100, ready, roll) != DungeonReworkRules.W_TOLL);
        // a move on cooldown is skipped
        ready[DungeonReworkRules.W_SLAM] = 500;
        assertEquals(-1, DungeonReworkRules.choose(book, 2.0, 1, 100, ready, 0));
    }

    @Test
    void everyWeightedMoveCanBeDrawn() {
        List<Move> book = DungeonReworkRules.WARDEN;
        long[] ready = new long[book.size()];
        Set<Integer> seen = new HashSet<>();
        for (int roll = 0; roll < 60; roll++) seen.add(DungeonReworkRules.choose(book, 4.0, 2, 100, ready, roll));
        assertTrue(seen.contains(DungeonReworkRules.W_SLAM) && seen.contains(DungeonReworkRules.W_PULSE), "close in phase 2: " + seen);
    }

    @Test
    void theWardensWorstCaseDamageStaysUnderTheCeiling() {
        for (int phase = 1; phase <= 3; phase++) {
            for (boolean enraged : new boolean[]{false, true}) {
                double s = DungeonReworkRules.worstCaseBasePerSecond(DungeonReworkRules.WARDEN, phase, enraged);
                assertTrue(s > 0 && s <= 12.0, "phase " + phase + " enraged " + enraged + ": " + s);
            }
        }
        assertTrue(DungeonReworkRules.worstCaseBasePerSecond(DungeonReworkRules.WARDEN, 3, false) > DungeonReworkRules.worstCaseBasePerSecond(DungeonReworkRules.WARDEN, 1, false),
                "the fight gets harder as it goes");
    }

    @Test
    void theKeyTollSigilsAreSafeAndTheRestOfTheArenaIsNot() {
        double[] s = DungeonReworkRules.sigils(100.0, 50.0, 0.0);
        assertEquals(4, s.length);
        assertEquals(107.0, s[0], 1e-9);
        assertEquals(93.0, s[2], 1e-9);
        assertTrue(DungeonReworkRules.insideSigil(s, 107.0, 50.0));
        assertTrue(DungeonReworkRules.insideSigil(s, 93.0, 52.9));
        assertFalse(DungeonReworkRules.insideSigil(s, 100.0, 50.0), "standing on the Warden is not safe");
        assertFalse(DungeonReworkRules.insideSigil(s, 107.0, 53.5));
        assertEquals(1.0, DungeonReworkRules.tollShare(false));
        assertEquals(0.2, DungeonReworkRules.tollShare(true));
    }

    @Test
    void theSealPulseCanBeJumped() {
        assertTrue(DungeonReworkRules.inWave(8.0, 8.0, 64.0, 64.0));
        assertTrue(DungeonReworkRules.inWave(9.5, 8.0, 64.0, 64.0));
        assertFalse(DungeonReworkRules.inWave(10.0, 8.0, 64.0, 64.0), "outside the band");
        assertFalse(DungeonReworkRules.inWave(8.0, 8.0, 65.2, 64.0), "a jump clears it");
    }

    @Test
    void theCreaturesUseOnlyTheirOwnMoves() {
        assertEquals(7, DungeonReworkRules.BEASTS.size());
        Set<Integer> all = new HashSet<>();
        for (BeastKind k : BeastKind.values()) {
            int[] ids = DungeonReworkRules.beastMoves(k);
            assertTrue(ids.length >= 2, k + " has at least two moves");
            for (int id : ids) {
                assertTrue(id >= 0 && id < DungeonReworkRules.BEASTS.size());
                assertTrue(all.add(id), "move " + id + " belongs to one creature only");
                Move m = DungeonReworkRules.BEASTS.get(id);
                assertTrue(m.windup() >= 12, m.id() + " telegraph");
                assertFalse(m.ultimate());
            }
        }
        assertEquals(7, all.size(), "every move is used");
    }

    @Test
    void thePackCallsStartOnlyWhenTheyAreReadyAndOnlyForTheRightCreature() {
        int[] wolf = DungeonReworkRules.beastMoves(BeastKind.DIREWOLF);
        List<Move> book = List.of(DungeonReworkRules.BEASTS.get(wolf[0]), DungeonReworkRules.BEASTS.get(wolf[1]));
        long[] ready = new long[2];
        assertEquals(1, DungeonReworkRules.choose(book, 1.0, 1, 10, ready, 0), "only the howl fits at point blank");
        ready[1] = 700;
        assertEquals(-1, DungeonReworkRules.choose(book, 1.0, 1, 10, ready, 0));
        int[] armor = DungeonReworkRules.beastMoves(BeastKind.ARMOR);
        List<Move> armorBook = List.of(DungeonReworkRules.BEASTS.get(armor[0]), DungeonReworkRules.BEASTS.get(armor[1]));
        long[] ar = new long[2];
        for (int roll = 0; roll < 20; roll++) assertEquals(0, DungeonReworkRules.choose(armorBook, 2.0, 1, 10, ar, roll), "the rattle needs half health gone");
    }
}
