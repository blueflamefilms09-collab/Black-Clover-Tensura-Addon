package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Goku awakening, the Kamehameha numbers and the Rock-Scissors-Paper timing are plain arithmetic, so they are pinned here with no game. */
class GokuRulesTest {
    // ------------------------------------------------------------------ the registry and the awakening
    @Test
    void theRegistryHoldsGokuRestrictedToSaiyans() {
        SaiyanUltimates.Ultimate u = SaiyanUltimates.byId("goku");
        assertNotNull(u);
        assertEquals(1, SaiyanUltimates.ALL.size());
        assertTrue(SaiyanUltimates.openTo(u, "saiyan"));
        assertFalse(SaiyanUltimates.openTo(u, "slime"));
        assertFalse(SaiyanUltimates.openTo(null, "saiyan"));
        assertNull(SaiyanUltimates.byId("frieza"), "Frieza is a racial evolution, not a Saiyan Ultimate");
        assertEquals("nusmp.saiyan.ultimate.goku", u.nameKey());
        assertEquals("nusmp.saiyan.ultimate.goku.epithet", u.epithetKey());
    }

    @Test
    void aSaiyanHoldsOnlyOnePrimaryUltimate() {
        assertTrue(SaiyanUltimates.mayTake(null));
        assertTrue(SaiyanUltimates.mayTake(""));
        assertFalse(SaiyanUltimates.mayTake("goku"));
    }

    @Test
    void theAwakeningAsksForEverythingTheSpecNames() {
        SaiyanUltimates.Need need = SaiyanUltimates.Need.GOKU_DEFAULT;
        assertEquals(100_000.0, need.bp(), 1e-9, "Legendary Warrior rank");
        assertEquals(5, need.techniques(), "five universal techniques");
        SaiyanUltimates.Progress none = new SaiyanUltimates.Progress(100, 0, 0, 0, 0, 0, 0);
        assertEquals(7, SaiyanUltimates.missing(need, none).size(), "every requirement is listed when nothing is done");
        assertFalse(SaiyanUltimates.ready(need, none));
        SaiyanUltimates.Progress done = new SaiyanUltimates.Progress(150_000, 5, 2, 1, 1, 1, 10);
        assertTrue(SaiyanUltimates.ready(need, done));
        assertFalse(SaiyanUltimates.ready(need, new SaiyanUltimates.Progress(150_000, 5, 2, 1, 1, 1, 9)), "the trial is not finished");
        assertFalse(SaiyanUltimates.ready(need, new SaiyanUltimates.Progress(99_999, 5, 2, 1, 1, 1, 10)), "not yet Legendary");
        assertTrue(SaiyanRules.rankOf(need.bp()) == SaiyanRules.Rank.LEGENDARY);
    }

    // ------------------------------------------------------------------ Kamehameha
    @Test
    void masteryShortensTheChargeButNeverBelowHalfASecond() {
        assertEquals(6.0, GokuRules.maxChargeSeconds(6.0, 0.0), 1e-9);
        assertEquals(3.6, GokuRules.maxChargeSeconds(6.0, 1.0), 1e-9);
        assertEquals(0.5, GokuRules.maxChargeSeconds(0.1, 1.0), 1e-9);
    }

    @Test
    void chargeGrowsFromZeroToOne() {
        assertEquals(0.0, GokuRules.charge(0, 6.0), 1e-9);
        assertEquals(0.5, GokuRules.charge(60, 6.0), 1e-9);
        assertEquals(1.0, GokuRules.charge(120, 6.0), 1e-9);
        assertEquals(1.0, GokuRules.charge(99_999, 6.0), 1e-9);
        assertEquals(0.0, GokuRules.charge(-5, 6.0), 1e-9);
        assertFalse(GokuRules.canFire(5, 10));
        assertTrue(GokuRules.canFire(10, 10));
    }

    @Test
    void aLongerChargeDoesMoreReachesFartCostsMoreAndPassesThroughMore() {
        assertTrue(GokuRules.damage(100, 1.0) > GokuRules.damage(100, 0.0));
        assertEquals(100.0, GokuRules.damage(100, 1.0), 1e-9);
        assertEquals(35.0, GokuRules.damage(100, 0.0), 1e-9);
        assertEquals(40.0, GokuRules.range(100, 0.0), 1e-9);
        assertEquals(100.0, GokuRules.range(100, 1.0), 1e-9);
        assertTrue(GokuRules.width(1.0) > GokuRules.width(0.0));
        assertEquals(1, GokuRules.pierce(0.0));
        assertEquals(8, GokuRules.pierce(1.0));
        assertTrue(GokuRules.knockback(1.0) > GokuRules.knockback(0.0));
        assertEquals(4.0, GokuRules.kiPercent(10.0, 0.0), 1e-9);
        assertEquals(10.0, GokuRules.kiPercent(10.0, 1.0), 1e-9);
        assertEquals(1.0, GokuRules.pierce(Double.NaN) / 1.0, 1e-9, "a broken charge is a quick shot");
    }

    // ------------------------------------------------------------------ Rock, Scissors 'N' Paper
    @Test
    void theComboChainsInOrderWithTimedInputs() {
        assertEquals(GokuRules.ROCK, GokuRules.nextStage(0, 100, -100, 6, 30), "nothing before: Rock");
        assertEquals(GokuRules.SCISSORS, GokuRules.nextStage(GokuRules.ROCK, 110, 100, 6, 30));
        assertEquals(GokuRules.PAPER, GokuRules.nextStage(GokuRules.SCISSORS, 125, 110, 6, 30));
        assertEquals(GokuRules.ROCK, GokuRules.nextStage(GokuRules.PAPER, 140, 125, 6, 30), "after Paper the combo starts again");
        assertEquals(GokuRules.ROCK, GokuRules.nextStage(GokuRules.ROCK, 200, 100, 6, 30), "too slow: back to Rock");
        assertEquals(0, GokuRules.nextStage(GokuRules.ROCK, 102, 100, 6, 30), "too fast: the press is ignored");
        assertEquals(GokuRules.ROCK, GokuRules.nextStage(GokuRules.SCISSORS, 50, 100, 6, 30), "a clock that went back starts over");
    }

    @Test
    void onlyAFullChainBoostsPaper() {
        assertTrue(GokuRules.chained(GokuRules.SCISSORS, GokuRules.ROCK));
        assertTrue(GokuRules.chained(GokuRules.PAPER, GokuRules.SCISSORS));
        assertFalse(GokuRules.chained(GokuRules.ROCK, 0));
        assertFalse(GokuRules.chained(GokuRules.PAPER, GokuRules.ROCK));
        assertEquals(1.0, GokuRules.stageDamage(GokuRules.ROCK, false), 1e-9);
        assertEquals(0.7, GokuRules.stageDamage(GokuRules.SCISSORS, true), 1e-9);
        assertTrue(GokuRules.stageDamage(GokuRules.PAPER, true) > GokuRules.stageDamage(GokuRules.PAPER, false));
        assertTrue(GokuRules.stageKnockback(GokuRules.PAPER) > GokuRules.stageKnockback(GokuRules.ROCK));
        assertEquals(1, GokuRules.stageTargets(GokuRules.ROCK, 8));
        assertEquals(8, GokuRules.stageTargets(GokuRules.PAPER, 8));
        assertEquals(1, GokuRules.stageTargets(GokuRules.PAPER, 0));
        assertEquals(List.of(1, 2, 3), List.of(GokuRules.ROCK, GokuRules.SCISSORS, GokuRules.PAPER));
    }
}
