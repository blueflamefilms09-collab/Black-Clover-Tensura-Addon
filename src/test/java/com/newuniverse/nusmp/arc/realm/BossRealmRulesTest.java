package com.newuniverse.nusmp.arc.realm;

import com.newuniverse.nusmp.arena.ArenaState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every decision of the boss realm is a pure rule ({@link BossRealmRules}), so each one is pinned here: who is a realm boss, which arena, who is carried along, whether the door opens,
 * how long a site may stand empty, when a fight times out and when the people of a resting site are carried home. The last test shows the bug the realm hardens against: the story-arena
 * referee gives up an empty fight after 40 ticks, and the realm's grace keeps it alive for the configured time.
 */
class BossRealmRulesTest {
    @Test
    void aRealmBossIsABossWithNoFloorNoOlderHallAndNoArenaOfItsOwn() {
        assertTrue(BossRealmRules.isRealmBoss(true, 0, false, false));                // goku_boss, sinbad_boss
        assertFalse(BossRealmRules.isRealmBoss(true, 12, false, false), "a dungeon floor boss");
        assertFalse(BossRealmRules.isRealmBoss(true, 0, true, false), "one of the seven older bosses");
        assertFalse(BossRealmRules.isRealmBoss(true, 0, false, true), "Lucius and the arenas named by a character");
        assertFalse(BossRealmRules.isRealmBoss(false, 0, false, false), "an NPC");
    }

    @Test
    void theTwoBossesHaveTheirOwnArenasAndEveryoneElseHasNone() {
        assertEquals("seven_seas_isle", BossRealmRules.shapeId("sinbad_boss"));
        assertEquals("tournament_grounds", BossRealmRules.shapeId("goku_boss"));
        assertNull(BossRealmRules.shapeId("lucius"));
        assertNull(BossRealmRules.shapeId(null));
    }

    @Test
    void companionsAreTheNearestEligibleOnesWithinTheRadiusAndNeverMoreThanThePartyCap() {
        double[] d2 = {400.0, 4.0, 9.0, 1.0, 100.0};
        boolean[] ok = {true, true, true, false, true};
        assertArrayEquals(new int[] {1, 2, 4}, BossRealmRules.companions(d2, ok, 16.0, 6), "nearest first; index 3 is not eligible, index 0 is 20 blocks away");
        assertArrayEquals(new int[] {1}, BossRealmRules.companions(d2, ok, 16.0, 2), "a cap of 2 is the user and one companion");
        assertEquals(0, BossRealmRules.companions(d2, ok, 16.0, 1).length, "a cap of 1 is the user alone");
        assertEquals(0, BossRealmRules.companions(d2, ok, 16.0, 0).length);
        double[] many = new double[40];
        boolean[] all = new boolean[40];
        for (int i = 0; i < 40; i++) {
            many[i] = i;
            all[i] = true;
        }
        assertEquals(15, BossRealmRules.companions(many, all, 100.0, 99).length, "a party never exceeds the arena's 16 arrival places");
    }

    @Test
    void theDoorOpensUnlessSwitchedOffCoolingOrEveryRealmSiteIsInUse() {
        assertEquals(BossRealmRules.Verdict.OPEN, BossRealmRules.verdict(true, 0, 4, false, false));
        assertEquals(BossRealmRules.Verdict.DISABLED, BossRealmRules.verdict(false, 0, 4, false, false));
        assertEquals(BossRealmRules.Verdict.SLOW_DOWN, BossRealmRules.verdict(true, 0, 4, false, true));
        assertEquals(BossRealmRules.Verdict.BUSY, BossRealmRules.verdict(true, 4, 4, false, false));
        assertEquals(BossRealmRules.Verdict.OPEN, BossRealmRules.verdict(true, 4, 4, true, false), "joining a site that is already in use needs no free site");
        assertEquals(BossRealmRules.Verdict.BUSY, BossRealmRules.verdict(true, 1, 0, false, false), "a cap below 1 still allows one site, never none");
    }

    @Test
    void anEmptySiteIsBelievedOccupiedForTheGraceThenGivenUp() {
        BossRealmRules.Clock c = new BossRealmRules.Clock();
        assertEquals(2, c.present(2, 100, 400));
        assertEquals(1, c.present(0, 120, 400), "the last one is gone: still 'someone' inside the grace");
        assertEquals(1, c.present(0, 519, 400));
        assertEquals(0, c.present(0, 520, 400), "400 ticks later it is empty");
        assertEquals(3, c.present(3, 540, 400), "somebody came back");
        assertEquals(1, c.present(0, 560, 400), "and the grace starts afresh");
        assertEquals(0, new BossRealmRules.Clock().present(0, 5, 0), "a grace of 0 gives the old behaviour");
    }

    @Test
    void aFightEndsAfterTheLimitAndALimitOfZeroMeansNone() {
        BossRealmRules.Clock c = new BossRealmRules.Clock();
        assertFalse(c.timedOut(1000, 36000), "no fight is running");
        c.fight(true, 1000);
        c.fight(true, 5000);
        assertFalse(c.timedOut(36999, 36000));
        assertTrue(c.timedOut(37000, 36000), "counted from the first look at the fight, not the last");
        assertFalse(c.timedOut(10_000_000, 0));
        c.fight(false, 40000);
        assertFalse(c.timedOut(90000, 36000));
    }

    @Test
    void thePeopleOfARestingSiteAreCarriedHomeAfterTheWaitAndNeverWhileItIsNotResting() {
        BossRealmRules.Clock c = new BossRealmRules.Clock();
        assertFalse(c.returnDue(false, 3, 100, 900));
        assertFalse(c.returnDue(true, 3, 200, 900));
        assertFalse(c.returnDue(true, 3, 1099, 900));
        assertTrue(c.returnDue(true, 3, 1100, 900));
        assertFalse(c.returnDue(true, 0, 1200, 900), "nobody is there to carry");
        assertFalse(c.returnDue(true, 2, 1300, 900), "the wait restarts when somebody arrives at an empty resting site");
        assertTrue(c.returnDue(true, 2, 2200, 900));
        assertTrue(new BossRealmRules.Clock().returnDue(true, 1, 5, 0), "0 seconds: at once");
        assertTrue(new BossRealmRules.Clock().idle(0));
        BossRealmRules.Clock f = new BossRealmRules.Clock();
        f.fight(true, 1);
        assertFalse(f.idle(0), "a clock with a fight is kept");
    }

    @Test
    void settingsBecomeBoundedTicks() {
        assertEquals(400, BossRealmRules.ticks(20, 20));
        assertEquals(36000, BossRealmRules.ticks(30, 1200));
        assertEquals(0, BossRealmRules.ticks(-5, 20));
        assertEquals(0, BossRealmRules.ticks(Double.NaN, 20));
        assertTrue(BossRealmRules.ticks(1.0e12, 1200) <= 1_200_000_000L, "never overflows");
    }

    @Test
    void theStoryRefereeAbandonsAnEmptyFightAfterTwoSecondsButARealmSiteWaitsForTheGrace() {
        // the referee looks every 20 ticks (ArcArenaDirector.STEP) and gives up at EMPTY_GRACE_TICKS = 40
        ArenaState story = new ArenaState();
        story.tickReady(20, 1, 1);
        ArenaState realm = new ArenaState();
        realm.tickReady(20, 1, 1);
        for (int t = 0; t < 200 && story.phase() == ArenaState.Phase.READY; t++) { story.tickReady(20, 1, 1); realm.tickReady(20, 1, 1); }
        assertEquals(ArenaState.Phase.FIGHTING, story.phase());
        BossRealmRules.Clock clock = new BossRealmRules.Clock();
        long now = 10_000;
        long grace = BossRealmRules.ticks(20, 20);
        ArenaState.Watch s = ArenaState.Watch.CONTINUE, r = ArenaState.Watch.CONTINUE;
        int storyAt = -1, realmAt = -1;
        for (int step = 1; step <= 40; step++) {
            now += 20;
            if (s == ArenaState.Watch.CONTINUE && (s = story.tickFighting(20, 0, true)) == ArenaState.Watch.ABANDONED) storyAt = step;
            if (r == ArenaState.Watch.CONTINUE && (r = realm.tickFighting(20, clock.present(0, now, grace), true)) == ArenaState.Watch.ABANDONED) realmAt = step;
        }
        assertEquals(2, storyAt, "the story arena gives up after two looks (2 seconds) with nobody in it");
        assertTrue(realmAt >= 20 + 2, "the realm waits the grace (20 s = 20 looks) and then the referee's own two: " + realmAt);
        assertTrue(realmAt <= 26, "and does give up in the end: " + realmAt);
    }
}
