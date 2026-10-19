package com.newuniverse.nusmp.labyrinth.mega.lock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: these are the owner's rules for being locked in a room, one test for each sentence of the request. "Add a way to get out": a visible rune that appears after a timeout, or
 * at once when the room is idle; "an automatic rescue if a player has not moved out of a sealed room for N minutes with no hostile alive"; "never lock a player in during or
 * after a fight where the boss is dead"; "a boss arena may stay closed while the boss lives and a player is inside, but must open when the boss dies, when every player inside
 * has left or died, or after the configured maximum fight time, and must always show the player an escape option". The timings come from the config and are clamped, so the
 * last tests try values that would switch the guarantee off.
 */
class EscapeRulesTest {
    private static final EscapeRules.Config C = EscapeRules.Config.DEFAULT;

    private static long s(int seconds) { return seconds * 20L; }

    private static EscapeRules.Facts seal(long sealedSeconds, int players, long emptySeconds, int hostiles, long quietSeconds, boolean solvable) {
        return new EscapeRules.Facts(EscapeRules.Kind.SEAL, s((int) sealedSeconds), players, s((int) emptySeconds), hostiles, s((int) quietSeconds), false, solvable);
    }

    private static EscapeRules.Facts boss(long sealedSeconds, int players, long emptySeconds, boolean bossAlive) {
        return new EscapeRules.Facts(EscapeRules.Kind.BOSS, s((int) sealedSeconds), players, s((int) emptySeconds), bossAlive ? 1 : 0, 0, bossAlive, true);
    }

    // ---------------------------------------------------------------- a seal that is a fight
    @Test void aFreshSealHoldsAndAFightOnlyShowsTheRuneAfterTheTimeout() {
        assertEquals(EscapeRules.Action.HOLD, EscapeRules.decide(seal(1, 2, 0, 4, 0, true), C).action());
        assertEquals(EscapeRules.Action.HOLD, EscapeRules.decide(seal(59, 2, 0, 4, 0, true), C).action());
        EscapeRules.Decision d = EscapeRules.decide(seal(60, 2, 0, 4, 0, true), C);
        assertEquals(EscapeRules.Action.SHOW_RUNE, d.action());
        assertEquals(EscapeRules.Why.TIMEOUT, d.why());
        assertTrue(d.rune() && !d.release());
    }

    @Test void aSealOpensWhateverIsGoingOnAfterTheLongestTime() {
        EscapeRules.Decision d = EscapeRules.decide(seal(600, 2, 0, 9, 0, true), C);
        assertTrue(d.release());
        assertEquals(EscapeRules.Why.MAX_TIME, d.why());
        assertEquals(EscapeRules.Action.SHOW_RUNE, EscapeRules.decide(seal(599, 2, 0, 9, 0, true), C).action());
    }

    // ---------------------------------------------------------------- an idle room
    @Test void anIdleRoomShowsTheRuneAtOnceAndRescuesAfterTheMinutes() {
        assertEquals(EscapeRules.Action.HOLD, EscapeRules.decide(seal(5, 1, 0, 0, 5, true), C).action());
        EscapeRules.Decision rune = EscapeRules.decide(seal(15, 1, 0, 0, 15, true), C);
        assertEquals(EscapeRules.Action.SHOW_RUNE, rune.action());
        assertEquals(EscapeRules.Why.IDLE, rune.why());
        EscapeRules.Decision rescue = EscapeRules.decide(seal(300, 1, 0, 0, 300, true), C);
        assertTrue(rescue.release());
        assertEquals(EscapeRules.Why.RESCUE, rescue.why());
        assertFalse(EscapeRules.decide(seal(299, 1, 0, 0, 299, true), C).release());
    }

    @Test void aFightThatWentQuietIsNotRescuedUntilItHasBeenQuietLongEnough() {
        // sealed for ten minutes minus a second, but a hostile was alive until ten seconds ago
        EscapeRules.Decision d = EscapeRules.decide(seal(500, 1, 0, 0, 10, true), C);
        assertFalse(d.release());
        assertEquals(EscapeRules.Action.SHOW_RUNE, d.action());
    }

    // ---------------------------------------------------------------- nobody inside
    @Test void aSealWithNobodyInsideOpensAfterTheShortWait() {
        assertEquals(EscapeRules.Action.HOLD, EscapeRules.decide(seal(5, 0, 5, 3, 0, true), C).action());
        EscapeRules.Decision d = EscapeRules.decide(seal(20, 0, 10, 3, 0, true), C);
        assertTrue(d.release());
        assertEquals(EscapeRules.Why.ABSENT, d.why());
    }

    @Test void theNobodyInsideRuleBeatsEverythingElse() {
        assertEquals(EscapeRules.Why.ABSENT, EscapeRules.decide(boss(100, 0, 10, true), C).why(), "a boss arena with nobody in it opens even with the boss alive");
    }

    // ---------------------------------------------------------------- a seal nobody can beat
    @Test void aSealThePartyCannotBeatShowsTheRuneEarlyAndOpensByItself() {
        EscapeRules.Decision rune = EscapeRules.decide(seal(15, 1, 0, 3, 0, false), C);
        assertEquals(EscapeRules.Action.SHOW_RUNE, rune.action());
        assertEquals(EscapeRules.Why.UNSOLVABLE, rune.why());
        EscapeRules.Decision open = EscapeRules.decide(seal(30, 1, 0, 3, 0, false), C);
        assertTrue(open.release());
        assertEquals(EscapeRules.Why.UNSOLVABLE, open.why());
    }

    @Test void solvabilityFollowsTheCounterAndThePartySize() {
        assertFalse(EscapeRules.solvable("none", 1, 4));
        assertFalse(EscapeRules.solvable("", 1, 4));
        assertFalse(EscapeRules.solvable(null, 1, 4));
        assertFalse(EscapeRules.solvable("split_plates", 2, 1), "a plate puzzle for two cannot be beaten by one");
        assertTrue(EscapeRules.solvable("split_plates", 2, 2));
        assertTrue(EscapeRules.solvable("sequence", 1, 1));
        assertTrue(EscapeRules.solvable("kill_all", 1, 1));
        assertTrue(EscapeRules.solvable("all_present", 1, 1));
    }

    // ---------------------------------------------------------------- the boss arena
    @Test void aBossArenaStaysClosedWhileTheBossLivesButTheExitRuneIsThereFromTheFirstMoment() {
        for (int sealed : new int[] {0, 1, 30, 300, 899}) {
            EscapeRules.Decision d = EscapeRules.decide(boss(sealed, 2, 0, true), C);
            assertEquals(EscapeRules.Action.SHOW_RUNE, d.action(), "at " + sealed + " s");
            assertEquals(EscapeRules.Why.BOSS_FIGHT, d.why());
        }
        assertEquals(EscapeRules.RuneUse.EXIT_AND_RESET, EscapeRules.runeFor(EscapeRules.Kind.BOSS));
        assertEquals(EscapeRules.RuneUse.RELEASE, EscapeRules.runeFor(EscapeRules.Kind.SEAL));
        assertEquals(EscapeRules.RuneUse.RELEASE, EscapeRules.runeFor(EscapeRules.Kind.TRIAL));
    }

    @Test void aBossArenaOpensWhenTheBossDiesAndAfterTheMaximumFightTime() {
        EscapeRules.Decision dead = EscapeRules.decide(boss(100, 2, 0, false), C);
        assertTrue(dead.release());
        assertEquals(EscapeRules.Why.BOSS_DEAD, dead.why());
        EscapeRules.Decision long_ = EscapeRules.decide(boss(900, 2, 0, true), C);
        assertTrue(long_.release());
        assertEquals(EscapeRules.Why.MAX_TIME, long_.why());
    }

    // ---------------------------------------------------------------- a trial chamber
    @Test void aTrialChamberUsesTheSameRules() {
        EscapeRules.Facts f = new EscapeRules.Facts(EscapeRules.Kind.TRIAL, s(61), 3, 0, 5, 0, false, true);
        assertEquals(EscapeRules.Action.SHOW_RUNE, EscapeRules.decide(f, C).action());
        EscapeRules.Facts stuck = new EscapeRules.Facts(EscapeRules.Kind.TRIAL, s(601), 3, 0, 1, 0, false, true);
        assertTrue(EscapeRules.decide(stuck, C).release(), "a trial with one mob that can never be killed still opens");
    }

    // ---------------------------------------------------------------- the config cannot switch the guarantee off
    @Test void aBadConfigIsClampedSoTheGuaranteeStays() {
        EscapeRules.Config zero = new EscapeRules.Config(0, 0, 0, 0, 0, 0, 0);
        EscapeRules.Config c = zero.clamped();
        assertTrue(c.absentSeconds() >= 3 && c.runeAfterSeconds() >= 5 && c.idleRuneSeconds() >= 1 && c.rescueMinutes() >= 1 && c.maxSealSeconds() >= 30 && c.bossMaxSeconds() >= 60 && c.unsolvableSeconds() >= 5);
        assertEquals(EscapeRules.Action.HOLD, EscapeRules.decide(seal(0, 1, 0, 3, 0, true), zero).action(), "a zero config does not open a seal the moment it closes");
        EscapeRules.Config huge = new EscapeRules.Config(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).clamped();
        assertTrue(huge.maxSealSeconds() <= 7200 && huge.bossMaxSeconds() <= 7200 && huge.rescueMinutes() <= 120, "a huge config still ends: " + huge);
    }

    @Test void everySealEventuallyOpensWhateverTheFacts() {
        // brute force: for every combination of facts that has a prisoner, some later time opens it
        EscapeRules.Kind[] kinds = EscapeRules.Kind.values();
        for (EscapeRules.Kind k : kinds) {
            for (int players : new int[] {0, 1, 4}) {
                for (int hostiles : new int[] {0, 1, 20}) {
                    for (boolean bossAlive : new boolean[] {false, true}) {
                        for (boolean solvable : new boolean[] {false, true}) {
                            EscapeRules.Facts f = new EscapeRules.Facts(k, s(100000), players, players == 0 ? s(100000) : 0, hostiles, 0, bossAlive, solvable);
                            assertTrue(EscapeRules.decide(f, C).release(), "never opens: " + f);
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the status numbers
    @Test void theStatusCommandCountsDownToTheRuneAndTheOpening() {
        EscapeRules.Facts f = seal(10, 1, 0, 3, 0, true);
        assertEquals(50, EscapeRules.secondsUntilRune(f, C));
        assertEquals(590, EscapeRules.secondsUntilRelease(f, C));
        EscapeRules.Facts idle = seal(10, 1, 0, 0, 10, true);
        assertEquals(5, EscapeRules.secondsUntilRune(idle, C));
        assertEquals(290, EscapeRules.secondsUntilRelease(idle, C));
        EscapeRules.Facts unbeatable = seal(10, 1, 0, 3, 0, false);
        assertEquals(5, EscapeRules.secondsUntilRune(unbeatable, C));
        assertEquals(20, EscapeRules.secondsUntilRelease(unbeatable, C));
        assertEquals(0, EscapeRules.secondsUntilRune(boss(0, 1, 0, true), C));
        assertEquals(0, EscapeRules.secondsUntilRelease(seal(10000, 1, 0, 3, 0, true), C), "never negative");
    }

    // ---------------------------------------------------------------- which mechanics lock
    @Test void sealingMechanicsAreFoundByTheirPrimitives() {
        assertTrue(EscapeRules.seals(List.of("sound", "seal_doors", "flood")));
        assertTrue(EscapeRules.seals(List.of("cage", "damage")));
        assertFalse(EscapeRules.seals(List.of("effect", "damage", "rukh")));
        assertFalse(EscapeRules.seals(List.of()));
        assertFalse(EscapeRules.seals(null));
    }
}
