package com.newuniverse.nusmp.heroweapon.weapon.sword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Why: the ledger holds every rule of the Sword's blow-by-blow speed (the Tempo, the Flow, the ghost of an afterimage, and the form passives), and its main promise is
 * that a single blow never earns more than ONE small bonus strike and that a passive spends only what it was chosen for. Each rule is checked with plain tick
 * numbers, including the ones that are easy to break: a bonus that loses keeps its charge, the Flow needs a full swing, a clock that goes back starts the count over.
 */
class HitLedgerTest {
    private static final Predicate<String> NONE = p -> false;

    private static Predicate<String> has(String... ids) {
        Set<String> s = Set.of(ids);
        return s::contains;
    }

    /** Lands {@code n} full blows two ticks apart from time {@code from}; returns the time of the last one. */
    private static long run(HitLedger l, long from, int n, Predicate<String> has) {
        long t = from;
        for (int i = 0; i < n; i++) {
            l.hit(t, true, has, false, 1.0, false);
            t += 2;
        }
        return t - 2;
    }

    @Test
    void theTempoBuildsWithBlowsInTheWindowAndLapsesAfterIt() {
        HitLedger l = new HitLedger();
        assertEquals(0, l.tempo(100));
        l.hit(100, true, NONE, false, 1, false);
        assertEquals(1, l.tempo(100));
        l.hit(110, true, NONE, false, 1, false);
        l.hit(120, true, NONE, false, 1, false);
        assertEquals(3, l.tempo(120));
        assertEquals(3, l.tempo(120 + SwordNumbers.TEMPO_WINDOW));
        assertEquals(0, l.tempo(120 + SwordNumbers.TEMPO_WINDOW + 1), "the run lapsed");
        l.hit(300, true, NONE, false, 1, false);
        assertEquals(1, l.tempo(300), "a blow after a pause starts a new run");
    }

    @Test
    void theTempoNeverPassesTheMaximumAndAFullOneBecomesAFlowStrike() {
        HitLedger l = new HitLedger();
        long last = run(l, 100, SwordNumbers.TEMPO_MAX - 1, NONE);
        assertEquals(SwordNumbers.TEMPO_MAX - 1, l.tempo(last));
        HitLedger.Plan flow = l.hit(last + 2, true, NONE, false, 1, false);
        assertNotNull(flow);
        assertEquals("flow", flow.source());
        assertTrue(flow.flow());
        assertEquals(SwordNumbers.FLOW_RAW, flow.raw(), 1e-9);
        assertEquals(0, l.tempo(last + 2), "the Flow spends the Tempo");
        assertNull(l.hit(last + 4, true, NONE, false, 1, false), "the next blow starts again");
        assertEquals(1, l.tempo(last + 4));
    }

    @Test
    void theFlowNeedsAFullSwingAndWaitsForOne() {
        HitLedger l = new HitLedger();
        long t = 100;
        for (int i = 0; i < 12; i++, t += 2) assertNull(l.hit(t, false, NONE, false, 1, false), "weak blows never flow");
        assertEquals(SwordNumbers.TEMPO_MAX, l.tempo(t - 2), "but the Tempo stays at its maximum");
        HitLedger.Plan p = l.hit(t, true, NONE, false, 1, false);
        assertNotNull(p);
        assertEquals("flow", p.source());
    }

    @Test
    void anAfterimageLeavesOneGhostBlowAndOnlyForAWhile() {
        HitLedger l = new HitLedger();
        l.armGhost(100, SwordNumbers.GHOST_TICKS);
        assertTrue(l.ghostArmed(100));
        HitLedger.Plan p = l.hit(110, true, NONE, false, 1, false);
        assertNotNull(p);
        assertEquals("ghost", p.source());
        assertEquals(SwordNumbers.GHOST_RAW, p.raw(), 1e-9);
        assertNull(l.hit(112, true, NONE, false, 1, false), "one ghost per step");
        l.armGhost(200, SwordNumbers.GHOST_TICKS);
        assertNull(l.hit(200 + SwordNumbers.GHOST_TICKS + 1, true, NONE, false, 1, false), "the ghost faded");
    }

    @Test
    void theOpeningStrikeNeedsAPauseAndTheStabNeedsTheBack() {
        HitLedger l = new HitLedger();
        Predicate<String> h = has("quick_draw", "shadow_blade");
        HitLedger.Plan open = l.hit(1000, true, h, false, 1, false);
        assertNotNull(open);
        assertEquals("quick_draw", open.source());
        assertNull(l.hit(1004, true, h, false, 1, false), "no second opening strike straight after");
        HitLedger.Plan stab = l.hit(1008, true, h, true, 1, false);
        assertNotNull(stab);
        assertEquals("shadow_blade", stab.source());
        assertNull(new HitLedger().hit(10, true, has("shadow_blade"), false, 1, false), "from the front: nothing");
    }

    @Test
    void theStarFallsOnEveryFifthBlowWithAPause() {
        HitLedger l = new HitLedger();
        Predicate<String> h = has("star_shower");
        long t = 100;
        for (int i = 1; i <= 4; i++, t += 10) assertNull(l.hit(t, false, h, false, 1, false));
        HitLedger.Plan star = l.hit(t, false, h, false, 1, false);
        assertNotNull(star);
        assertEquals("star_shower", star.source());
        t += 10;
        for (int i = 1; i <= 4; i++, t += 2) assertNull(l.hit(t, false, h, false, 1, false), "the count started again");
        assertNull(l.hit(t, false, h, false, 1, false), "the fifth, but inside the pause");
        HitLedger.Plan again = l.hit(t + SwordNumbers.STAR_PAUSE, false, h, false, 1, false);
        assertNotNull(again, "the star was due and waited for the pause");
    }

    @Test
    void theHundredEdgeFallsOnEveryTenthBlow() {
        HitLedger l = new HitLedger();
        Predicate<String> h = has("hundred_edge");
        long t = 100;
        int got = 0;
        for (int i = 1; i <= 20; i++, t += 50) {                                       // slow blows: no Tempo, no Flow
            HitLedger.Plan p = l.hit(t, false, h, false, 1, false);
            if (p != null) {
                got++;
                assertEquals("hundred_edge", p.source());
                assertTrue(i == 10 || i == 20, "on blow " + i);
            }
        }
        assertEquals(2, got);
    }

    @Test
    void theFangBitesOnlyAFoeUnderThirtyPercentOfItsHealth() {
        HitLedger l = new HitLedger();
        Predicate<String> h = has("fang_maw");
        assertNull(l.hit(100, false, h, false, 0.5, false));
        assertNull(l.hit(200, false, h, false, 0.30, false), "exactly thirty percent is not under it");
        HitLedger.Plan p = l.hit(300, false, h, false, 0.29, false);
        assertNotNull(p);
        assertEquals("fang_maw", p.source());
        assertNull(l.hit(400, false, h, false, 0.0, false), "a foe at 0 is already dead");
    }

    @Test
    void theThunderArcsOnceInThePause() {
        HitLedger l = new HitLedger();
        Predicate<String> h = has("thunder_edge");
        assertNotNull(l.hit(100, false, h, false, 1, false));
        assertNull(l.hit(105, false, h, false, 1, false));
        assertNotNull(l.hit(100 + SwordNumbers.THUNDER_PAUSE, false, h, false, 1, false));
    }

    @Test
    void aGildedFoeTakesAFlatBonus() {
        HitLedger l = new HitLedger();
        HitLedger.Plan p = l.hit(100, false, NONE, false, 1, true);
        assertNotNull(p);
        assertEquals("gilded", p.source());
        assertEquals(SwordNumbers.GILDED_RAW, p.raw(), 1e-9);
    }

    @Test
    void oneBlowEarnsOneBonusAndTheLoserKeepsItsCharge() {
        // the tenth blow completes the Tempo (the Flow, 6 raw) and is also the tenth blow of the Hundred Edge (3 raw): the larger wins,
        // and the Hundred Edge, which lost, keeps its charge and strikes on the next blow
        HitLedger l = new HitLedger();
        Predicate<String> h = has("hundred_edge");
        long t = 100;
        for (int i = 1; i <= 9; i++, t += 2) assertNull(l.hit(t, true, h, false, 1, false), "blow " + i);
        HitLedger.Plan tenth = l.hit(t, true, h, false, 1, false);
        assertNotNull(tenth);
        assertEquals("flow", tenth.source(), "the tenth blow: the Flow, the larger bonus");
        HitLedger.Plan eleventh = l.hit(t + 2, true, h, false, 1, false);
        assertNotNull(eleventh, "the Hundred Edge that lost keeps its charge");
        assertEquals("hundred_edge", eleventh.source());
        assertNull(l.hit(t + 4, true, h, false, 1, false), "and it spent it");
    }

    @Test
    void noBonusIsEverLargerThanTheCap() {
        HitLedger l = new HitLedger();
        Predicate<String> all = p -> true;
        long t = 100;
        for (int i = 0; i < 200; i++, t += 2) {
            HitLedger.Plan p = l.hit(t, true, all, i % 2 == 0, (i % 7) / 10.0, i % 3 == 0);
            if (p != null) assertTrue(p.raw() > 0 && p.raw() <= SwordNumbers.BONUS_MAX, "raw " + p.raw());
        }
    }

    @Test
    void speedComesWithTempoAndEarlierForTheWindWalker() {
        assertEquals(0, HitLedger.speedLevel(0, false));
        assertEquals(0, HitLedger.speedLevel(SwordNumbers.SPEED_ONE_AT - 1, false));
        assertEquals(1, HitLedger.speedLevel(SwordNumbers.SPEED_ONE_AT, false));
        assertEquals(1, HitLedger.speedLevel(SwordNumbers.SPEED_TWO_AT - 1, false));
        assertEquals(2, HitLedger.speedLevel(SwordNumbers.SPEED_TWO_AT, false));
        assertEquals(1, HitLedger.speedLevel(SwordNumbers.SPEED_ONE_AT - 1, true), "one blow earlier");
        assertEquals(2, HitLedger.speedLevel(SwordNumbers.SPEED_TWO_AT - 1, true));
        assertFalse(HitLedger.speedLevel(SwordNumbers.TEMPO_MAX, false) > 2);
    }

    @Test
    void aKillHelpsTheTempoButNeverFinishesTheFlow() {
        HitLedger l = new HitLedger();
        l.kill(100);
        assertEquals(2, l.tempo(100));
        for (int i = 0; i < 20; i++) l.kill(100 + i);
        assertEquals(SwordNumbers.TEMPO_MAX - 1, l.tempo(120), "kills alone stop one short of the Flow");
    }

    @Test
    void aClockThatWentBackStartsTheCountOver() {
        HitLedger l = new HitLedger();
        run(l, 100000, 6, NONE);
        assertEquals(0, l.tempo(50), "the clock is behind the last blow");
        l.hit(50, true, NONE, false, 1, false);
        assertEquals(1, l.tempo(50));
    }

    @Test
    void resetForgetsEverything() {
        HitLedger l = new HitLedger();
        run(l, 100, 6, NONE);
        l.armGhost(100, 60);
        l.reset();
        assertEquals(0, l.tempo(110));
        assertFalse(l.ghostArmed(110));
    }
}
