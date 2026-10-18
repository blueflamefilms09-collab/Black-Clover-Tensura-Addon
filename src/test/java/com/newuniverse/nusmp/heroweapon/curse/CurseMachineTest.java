package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.Sin;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: the Curse Series asks a wielder for hearts and hands, so its rules are pinned with plain numbers: despair is counted once per pause and fades only when the
 * wielder is calm, the weapon whispers at the threshold and the whisper lapses, NOTHING unlocks without an Embrace of an open offer, the tax is 4 hearts and then 2
 * and never takes the wielder below 4 hearts, corruption rises with a curse form and falls without one, the Overrun starts at 1000, lasts the rolled seconds and
 * leaves 400 corruption and a minute of weakness, and the charm, the rest and the admin's wipe do exactly what the documentation says.
 */
class CurseMachineTest {
    private static final long T0 = 100_000L;

    private static CurseTuning tuning() { return CurseTuning.defaults(); }

    // ---------------------------------------------------------------- despair
    @Test
    void despairIsCountedOncePerPauseAndClampedAtTheTop() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertEquals(150, CurseMachine.addDespair(s, Cause.PET_DEATH, t.amount(Cause.PET_DEATH), T0, t));
        assertEquals(0, CurseMachine.addDespair(s, Cause.PET_DEATH, 150, T0 + 99, t), "inside the pause the same cause is not counted again");
        assertEquals(150, CurseMachine.addDespair(s, Cause.PET_DEATH, 150, T0 + 100, t), "at the end of the pause it counts again");
        assertEquals(250, CurseMachine.addDespair(s, Cause.PARTY_DEATH, 250, T0 + 100, t), "another cause has its own pause");
        assertEquals(550, s.despair());
        assertEquals(0, CurseMachine.addDespair(s, Cause.BETRAYAL, 0, T0 + 500, t), "zero adds nothing");
        assertEquals(0, CurseMachine.addDespair(s, Cause.BETRAYAL, -40, T0 + 500, t), "a negative amount adds nothing");
        assertEquals(200, CurseMachine.addDespair(s, Cause.DEFEAT, 200, T0 + 500, t));
        for (int i = 0; i < 10; i++) CurseMachine.addDespair(s, Cause.DEFEAT, 400, T0 + 600 + i, t);
        assertEquals(1000, s.despair(), "despair never passes the top");
        assertEquals(0, CurseMachine.addDespair(s, Cause.DEFEAT, 400, T0 + 700, t), "nothing is added at the top");
    }

    @Test
    void aClockThatRanBackwardsDoesNotFreezeACause() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        CurseMachine.addDespair(s, Cause.NEAR_DEATH, 120, T0, t);
        assertEquals(120, CurseMachine.addDespair(s, Cause.NEAR_DEATH, 120, T0 - 5000, t), "a time before the record is accepted, not blocked for ever");
    }

    @Test
    void nearDeathIsCountedOnceAMinute() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertEquals(120, CurseMachine.addDespair(s, Cause.NEAR_DEATH, 120, T0, t));
        assertEquals(0, CurseMachine.addDespair(s, Cause.NEAR_DEATH, 120, T0 + 1199, t));
        assertEquals(120, CurseMachine.addDespair(s, Cause.NEAR_DEATH, 120, T0 + 1200, t));
    }

    @Test
    void despairFadesOnlyWhenCalmAndWithoutAnOpenOffer() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.despair = 500;
        s.lastGain = T0;
        CurseMachine.decayDespair(s, T0 + 399, true, t);
        assertEquals(500, s.despair(), "not calm long enough yet");
        CurseMachine.decayDespair(s, T0 + 400, false, t);
        assertEquals(500, s.despair(), "the wielder is being hurt");
        CurseMachine.decayDespair(s, T0 + 400, true, t);
        assertEquals(498, s.despair());
        s.offered = Sin.WRATH.id();
        s.offeredAt = T0;
        CurseMachine.decayDespair(s, T0 + 800, true, t);
        assertEquals(498, s.despair(), "an open offer holds the despair");
        s.despair = 1;
        s.offered = "";
        CurseMachine.decayDespair(s, T0 + 800, true, t);
        assertEquals(0, s.despair(), "never below zero");
    }

    // ---------------------------------------------------------------- the offer
    @Test
    void theWeaponWhispersAtTheThresholdForTheFirstFreeSin() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        List<Sin> sword = List.of(Sin.GREED, Sin.GLUTTONY);
        s.despair = 599;
        assertNull(CurseMachine.tryOffer(s, sword, T0, t), "one point short");
        s.despair = 600;
        assertEquals(Sin.GREED, CurseMachine.tryOffer(s, sword, T0, t));
        assertTrue(CurseMachine.offerOpen(s, T0 + 10, t));
        assertEquals(Sin.GREED, s.offered());
        assertNull(CurseMachine.tryOffer(s, sword, T0 + 10, t), "one offer at a time");
        assertEquals(1800, CurseMachine.offerTicksLeft(s, T0, t));
        assertEquals(1790, CurseMachine.offerTicksLeft(s, T0 + 10, t));
        s.offered = "";
        s.series.add(Sin.GREED.id());
        assertEquals(Sin.GLUTTONY, CurseMachine.tryOffer(s, sword, T0 + 20, t), "the second canon sin is offered once the first is embraced");
        s.offered = "";
        s.series.add(Sin.GLUTTONY.id());
        assertNull(CurseMachine.tryOffer(s, sword, T0 + 30, t), "nothing is left to offer");
        assertNull(CurseMachine.tryOffer(new CurseState(), List.of(), T0, t), "a weapon with no curse forms never whispers");
    }

    @Test
    void noOfferDuringAnOverrunOrBeforeThePauseIsOver() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.despair = 900;
        s.overrunUntil = T0 + 100;
        assertNull(CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0, t), "the weapon holds the wielder");
        s.overrunUntil = 0;
        s.nextOfferAt = T0 + 50;
        assertNull(CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0, t), "the pause after the last offer is not over");
        assertEquals(Sin.WRATH, CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0 + 50, t));
    }

    @Test
    void anUnansweredOfferLapsesAndTheWeaponWaitsBeforeItSpeaksAgain() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.despair = 800;
        CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0, t);
        assertFalse(CurseMachine.lapseIfExpired(s, T0 + 1799, t), "still open");
        assertTrue(CurseMachine.lapseIfExpired(s, T0 + 1800, t), "ran out");
        assertFalse(CurseMachine.offerOpen(s, T0 + 1800, t));
        assertEquals(300, s.despair(), "the despair falls to the decline level");
        assertTrue(s.series().isEmpty(), "ignoring the offer never unlocks anything");
        s.despair = 900;
        assertNull(CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0 + 1800 + 3599, t), "the weapon waits three minutes");
        assertEquals(Sin.WRATH, CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0 + 1800 + 3600, t));
        assertFalse(CurseMachine.lapseIfExpired(new CurseState(), T0, t), "nothing to lapse");
    }

    @Test
    void decliningClosesTheOfferAtOnce() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertFalse(CurseMachine.decline(s, T0, t), "nothing to decline");
        s.despair = 700;
        CurseMachine.tryOffer(s, List.of(Sin.PRIDE), T0, t);
        assertTrue(CurseMachine.decline(s, T0 + 5, t));
        assertFalse(CurseMachine.offerOpen(s, T0 + 5, t));
        assertEquals(300, s.despair());
        assertTrue(s.series().isEmpty());
        assertEquals(CurseMachine.Refusal.NO_OFFER, CurseMachine.embrace(s, T0 + 6, t).refusal(), "a declined offer cannot be embraced");
    }

    @Test
    void nothingEverUnlocksByItself() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        for (Cause c : Cause.values()) CurseMachine.addDespair(s, c, 1000, T0, t);
        for (long now = T0; now < T0 + 200_000; now += 20) {
            CurseMachine.tryOffer(s, List.of(Sin.WRATH, Sin.PRIDE), now, t);
            CurseMachine.lapseIfExpired(s, now, t);
            CurseMachine.decayDespair(s, now, true, t);
            CurseMachine.tickCorruption(s, true, 6, now, t, 0.5);
            CurseMachine.endOverrunIfDue(s, now, t);
            CurseMachine.addDespair(s, Cause.DEFEAT, 400, now, t);
        }
        assertTrue(s.series().isEmpty(), "only an Embrace of an open offer, or an administrator, may add a series");
        assertEquals(0, s.tax());
    }

    // ---------------------------------------------------------------- the Embrace and the tax
    @Test
    void theEmbraceTakesFourHeartsThenTwo() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.despair = 700;
        CurseMachine.tryOffer(s, List.of(Sin.GREED, Sin.GLUTTONY), T0, t);
        CurseMachine.Embraced first = CurseMachine.embrace(s, T0 + 100, t);
        assertTrue(first.ok());
        assertEquals(Sin.GREED, first.sin());
        assertEquals(4, first.hearts());
        assertEquals(4, s.tax());
        assertTrue(s.hasSeries(Sin.GREED));
        assertEquals(150, s.despair(), "the despair falls to the after-Embrace level");
        assertFalse(CurseMachine.offerOpen(s, T0 + 100, t), "the offer is closed");
        s.despair = 900;
        assertNull(CurseMachine.tryOffer(s, List.of(Sin.GREED, Sin.GLUTTONY), T0 + 101, t), "the weapon waits before it offers again");
        assertEquals(Sin.GLUTTONY, CurseMachine.tryOffer(s, List.of(Sin.GREED, Sin.GLUTTONY), T0 + 100 + 3600, t));
        CurseMachine.Embraced second = CurseMachine.embrace(s, T0 + 100 + 3700, t);
        assertEquals(2, second.hearts());
        assertEquals(6, s.tax());
        assertEquals(List.of("greed", "gluttony"), List.copyOf(s.series()));
        assertEquals(2, CurseMachine.taxFor(2, t));
        assertEquals(2, CurseMachine.taxFor(5, t));
        assertEquals(4, CurseMachine.taxFor(0, t));
    }

    @Test
    void theEmbraceNeedsAnOpenOfferAndAFreeHand() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertEquals(CurseMachine.Refusal.NO_OFFER, CurseMachine.embrace(s, T0, t).refusal());
        s.despair = 700;
        CurseMachine.tryOffer(s, List.of(Sin.WRATH), T0, t);
        assertEquals(CurseMachine.Refusal.NO_OFFER, CurseMachine.embrace(s, T0 + 1800, t).refusal(), "an offer that ran out cannot be accepted");
        assertTrue(s.series().isEmpty());
        s.offered = Sin.WRATH.id();
        s.offeredAt = T0 + 2000;
        s.overrunUntil = T0 + 5000;
        assertEquals(CurseMachine.Refusal.LOCKED, CurseMachine.embrace(s, T0 + 2100, t).refusal(), "the weapon holds the wielder");
        assertTrue(s.series().isEmpty());
        s.overrunUntil = 0;
        s.series.add(Sin.WRATH.id());
        assertEquals(CurseMachine.Refusal.ALREADY, CurseMachine.embrace(s, T0 + 2100, t).refusal());
        assertFalse(CurseMachine.offerOpen(s, T0 + 2100, t), "the stale offer is dropped");
        s.offered = "not_a_sin";
        s.offeredAt = T0;
        assertEquals(CurseMachine.Refusal.NO_OFFER, CurseMachine.embrace(s, T0 + 1, t).refusal(), "a damaged save does not throw");
    }

    @Test
    void theTaxNeverTakesMoreThanLeavesFourHearts() {
        assertEquals(8.0, CurseMachine.appliedTaxHp(4, 20.0, 4), 1e-9, "4 hearts off 20 health leaves 12");
        assertEquals(6.0, CurseMachine.appliedTaxHp(4, 14.0, 4), 1e-9, "only what leaves 8 health");
        assertEquals(2.0, CurseMachine.appliedTaxHp(6, 10.0, 4), 1e-9);
        assertEquals(0.0, CurseMachine.appliedTaxHp(4, 8.0, 4), 1e-9, "already at four hearts");
        assertEquals(0.0, CurseMachine.appliedTaxHp(4, 6.0, 4), 1e-9, "below four hearts already: nothing is taken");
        assertEquals(0.0, CurseMachine.appliedTaxHp(0, 20.0, 4), 1e-9);
        assertEquals(0.0, CurseMachine.appliedTaxHp(-3, 20.0, 4), 1e-9);
        assertEquals(40.0, CurseMachine.appliedTaxHp(20, 40.0, 0), 1e-9, "with no floor the whole tax is taken: 20 hearts are 40 health points");
        // the walk of a 20 health wielder through three series: 20 -> 12 -> 8 -> 8
        double max = 20.0;
        int tax = 0;
        double[] seen = new double[3];
        int[] stage = {4, 2, 2};
        for (int i = 0; i < 3; i++) {
            tax += stage[i];
            seen[i] = max - CurseMachine.appliedTaxHp(tax, max, 4);
        }
        assertEquals(12.0, seen[0], 1e-9);
        assertEquals(8.0, seen[1], 1e-9);
        assertEquals(8.0, seen[2], 1e-9, "the third series cannot go below four hearts");
    }

    @Test
    void anAdministratorMayGrantAndWipe() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.offered = Sin.LUST.id();
        s.offeredAt = T0;
        assertEquals(0, CurseMachine.grant(s, Sin.LUST, false, t), "a gift has no tax");
        assertFalse(CurseMachine.offerOpen(s, T0, t), "the offer for the same sin is closed");
        assertEquals(-1, CurseMachine.grant(s, Sin.LUST, false, t), "already unlocked");
        assertEquals(2, CurseMachine.grant(s, Sin.ENVY, true, t), "the second series with its tax");
        assertEquals(2, s.tax());
        assertEquals(-1, CurseMachine.grant(s, null, false, t));
        assertTrue(CurseMachine.forceOffer(s, Sin.SLOTH, T0));
        assertFalse(CurseMachine.forceOffer(s, Sin.SLOTH, T0), "one offer at a time");
        assertFalse(CurseMachine.forceOffer(new CurseState(), null, T0));
        s.corruption = 500;
        s.refunds = 1;
        CurseMachine.clearAll(s);
        assertTrue(s.isDefault() || s.lastRest == 0, "a wipe leaves nothing of the record");
        assertEquals(0, s.tax());
        assertTrue(s.series().isEmpty());
    }

    // ---------------------------------------------------------------- corruption and the Overrun
    @Test
    void corruptionRisesWithACurseFormAndFallsWithout() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertEquals(CurseMachine.Step.NONE, CurseMachine.tickCorruption(s, true, 6, T0, t, 0.5));
        assertEquals(6, s.corruption());
        CurseMachine.tickCorruption(s, true, 0, T0 + 20, t, 0.5);
        assertEquals(8, s.corruption(), "a curse form never rises slower than the minimum");
        t.corruptionScale = 2.0;
        CurseMachine.tickCorruption(s, true, 6, T0 + 40, t, 0.5);
        assertEquals(20, s.corruption(), "the global scale doubles the rise");
        t.corruptionScale = 1.0;
        CurseMachine.tickCorruption(s, false, 6, T0 + 60, t, 0.5);
        assertEquals(18, s.corruption(), "without a curse form it falls by the decay");
        s.corruption = 1;
        CurseMachine.tickCorruption(s, false, 6, T0 + 80, t, 0.5);
        assertEquals(0, s.corruption(), "never below zero");
    }

    @Test
    void techniquesAddCorruptionAndAnUltimateAddsMore() {
        CurseTuning t = tuning();
        assertEquals(6, CurseMachine.techniqueCorruption(false, t));
        assertEquals(18, CurseMachine.techniqueCorruption(true, t));
        t.corruptionScale = 0.5;
        assertEquals(3, CurseMachine.techniqueCorruption(false, t));
        t.corruptionScale = 0.0;
        assertEquals(0, CurseMachine.techniqueCorruption(true, t));
        t.corruptionScale = 1.0;
        CurseState s = new CurseState();
        assertEquals(CurseMachine.Step.NONE, CurseMachine.addCorruption(s, 100, T0, t, 0.5));
        assertEquals(100, s.corruption());
        CurseMachine.addCorruption(s, -30, T0, t, 0.5);
        assertEquals(70, s.corruption());
        CurseMachine.addCorruption(s, -300, T0, t, 0.5);
        assertEquals(0, s.corruption());
    }

    @Test
    void theOverrunStartsAtTheTopAndLastsTheRolledSeconds() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.corruption = 990;
        s.offered = Sin.WRATH.id();
        s.offeredAt = T0;
        assertEquals(CurseMachine.Step.OVERRUN_STARTED, CurseMachine.tickCorruption(s, true, 12, T0, t, 0.0));
        assertEquals(1000, s.corruption());
        assertEquals(T0 + 200, s.overrunUntil(), "roll 0 is the shortest Overrun: ten seconds");
        assertTrue(CurseMachine.inOverrun(s, T0 + 199));
        assertFalse(CurseMachine.inOverrun(s, T0 + 200));
        assertFalse(CurseMachine.offerOpen(s, T0, t), "an open offer is withdrawn");
        CurseState longest = new CurseState();
        longest.corruption = 999;
        CurseMachine.tickCorruption(longest, true, 6, T0, t, 0.99999);
        assertEquals(T0 + 400, longest.overrunUntil(), "the roll just below 1 is the longest: twenty seconds");
        CurseState viaTechnique = new CurseState();
        viaTechnique.corruption = 990;
        assertEquals(CurseMachine.Step.OVERRUN_STARTED, CurseMachine.addCorruption(viaTechnique, 45, T0, t, 0.5));
        assertTrue(CurseMachine.inOverrun(viaTechnique, T0 + 1));
    }

    @Test
    void duringAnOverrunNothingMovesThenItEndsWithWeakness() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        CurseMachine.startOverrun(s, T0, t, 0.0);
        assertEquals(CurseMachine.Step.NONE, CurseMachine.tickCorruption(s, true, 50, T0 + 20, t, 0.5));
        assertEquals(CurseMachine.Step.NONE, CurseMachine.tickCorruption(s, false, 50, T0 + 40, t, 0.5));
        assertEquals(1000, s.corruption(), "the number is held at the top");
        assertEquals(CurseMachine.Step.NONE, CurseMachine.addCorruption(s, -500, T0 + 40, t, 0.5), "an Overrun cannot be cleansed away");
        assertEquals(1000, s.corruption());
        assertFalse(CurseMachine.overrunDue(s, T0 + 199));
        assertEquals(CurseMachine.Step.NONE, CurseMachine.endOverrunIfDue(s, T0 + 199, t));
        assertTrue(CurseMachine.overrunDue(s, T0 + 200));
        assertEquals(CurseMachine.Step.OVERRUN_ENDED, CurseMachine.endOverrunIfDue(s, T0 + 205, t));
        assertEquals(400, s.corruption(), "the corruption falls to 400");
        assertEquals(T0 + 205 + 1200, s.weakUntil(), "a minute of weakness");
        assertTrue(CurseMachine.weak(s, T0 + 1000));
        assertFalse(CurseMachine.weak(s, T0 + 205 + 1200));
        assertFalse(CurseMachine.inOverrun(s, T0 + 206));
        assertEquals(CurseMachine.Step.NONE, CurseMachine.endOverrunIfDue(s, T0 + 300, t), "it ends once");
    }

    @Test
    void aDeathEndsAnOverrunWithoutWeakness() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        CurseMachine.startOverrun(s, T0, t, 0.5);
        CurseMachine.abortOverrun(s, t);
        assertFalse(CurseMachine.inOverrun(s, T0 + 1));
        assertEquals(400, s.corruption());
        assertEquals(0L, s.weakUntil());
        s.corruption = 300;
        s.overrunUntil = 0;
        CurseMachine.abortOverrun(s, t);
        assertEquals(300, s.corruption(), "outside an Overrun a death changes nothing here");
    }

    @Test
    void aDeathAlsoEndsTheWeaknessSoTheNewBodyStartsRested() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.corruption = 700;
        s.weakUntil = T0 + 900;
        CurseMachine.onWielderDeath(s, t);
        assertEquals(0L, s.weakUntil());
        assertFalse(CurseMachine.weak(s, T0));
        assertEquals(700, s.corruption(), "outside an Overrun the corruption is left as it was");
        CurseMachine.startOverrun(s, T0, t, 0.5);
        CurseMachine.onWielderDeath(s, t);
        assertFalse(CurseMachine.inOverrun(s, T0 + 1));
        assertEquals(400, s.corruption());
    }

    @Test
    void overrunLengthStaysInsideItsBoundsWhateverTheConfigSays() {
        for (int i = 0; i <= 100; i++) {
            double roll = i / 100.0;
            long ticks = OverrunMath.ticks(10, 20, roll);
            assertTrue(ticks >= 200 && ticks <= 400 && ticks % 20 == 0, "roll " + roll + " -> " + ticks);
        }
        assertEquals(200L, OverrunMath.ticks(10, 20, 0.0));
        assertEquals(400L, OverrunMath.ticks(10, 20, 0.9999999));
        assertEquals(400L, OverrunMath.ticks(10, 20, 5.0), "a roll above 1 is clamped");
        assertEquals(200L, OverrunMath.ticks(10, 20, -1.0), "a negative roll is clamped");
        assertEquals(200L, OverrunMath.ticks(10, 20, Double.NaN));
        assertEquals(300L, OverrunMath.ticks(15, 15, 0.5));
        long swapped = OverrunMath.ticks(20, 10, 0.5);
        assertTrue(swapped >= 200 && swapped <= 400, "swapped bounds are put in order");
        assertEquals(20L, OverrunMath.ticks(0, 0, 0.5), "a zero-length Overrun is raised to one second");
    }

    // ---------------------------------------------------------------- cleansing
    @Test
    void theCharmClearsAndGivesHeartsBackOnlyOnce() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.corruption = 700;
        s.tax = 4;
        CurseMachine.Cleansed first = CurseMachine.charm(s, T0, t);
        assertEquals(700, first.corruption());
        assertEquals(2, first.hearts());
        assertEquals(2, s.tax());
        assertEquals(0, s.corruption());
        s.corruption = 300;
        CurseMachine.Cleansed second = CurseMachine.charm(s, T0 + 10, t);
        assertEquals(300, second.corruption());
        assertEquals(0, second.hearts(), "the hearts come back once");
        assertEquals(2, s.tax());
        CurseMachine.Cleansed nothing = CurseMachine.charm(s, T0 + 20, t);
        assertFalse(nothing.any(), "nothing to cleanse");
    }

    @Test
    void theCharmKeepsItsRefundForWhenThereIsTaxToGiveBack() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.corruption = 100;
        CurseMachine.Cleansed early = CurseMachine.charm(s, T0, t);
        assertEquals(0, early.hearts());
        assertEquals(0, s.refunds, "an early charm does not use up the refund");
        s.tax = 1;
        s.corruption = 50;
        CurseMachine.Cleansed late = CurseMachine.charm(s, T0 + 10, t);
        assertEquals(1, late.hearts(), "never more than the tax owed");
        assertEquals(0, s.tax());
        t.charmRefundUses = 2;
        s.tax = 6;
        assertEquals(2, CurseMachine.charm(s, T0 + 20, t).hearts(), "the config may allow more refunds");
    }

    @Test
    void theCharmCannotReachAWielderTheWeaponHolds() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        CurseMachine.startOverrun(s, T0, t, 0.5);
        s.tax = 4;
        CurseMachine.Cleansed c = CurseMachine.charm(s, T0 + 10, t);
        assertFalse(c.any());
        assertEquals(1000, s.corruption());
        assertEquals(4, s.tax());
    }

    @Test
    void aFreshRestClearsHalfTheCorruptionOnce() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        s.corruption = 800;
        assertEquals(400, CurseMachine.restClear(s, T0 - 20, T0, t));
        assertEquals(400, s.corruption());
        assertEquals(0, CurseMachine.restClear(s, T0 - 20, T0 + 20, t), "the same rest is not honoured twice");
        assertEquals(0, CurseMachine.restClear(s, T0 - 30, T0 + 20, t), "an older rest than the one seen is ignored");
        assertEquals(200, CurseMachine.restClear(s, T0 + 40, T0 + 60, t), "a newer rest clears half of what is left");
        CurseState stale = new CurseState();
        stale.corruption = 600;
        assertEquals(0, CurseMachine.restClear(stale, T0 - 5000, T0, t), "an old rest seen after a login is remembered but not honoured");
        assertEquals(600, stale.corruption());
        assertEquals(T0 - 5000, stale.lastRest);
        CurseState held = new CurseState();
        CurseMachine.startOverrun(held, T0, t, 0.5);
        assertEquals(0, CurseMachine.restClear(held, T0 + 10, T0 + 20, t), "a rest cannot reach a wielder the weapon holds");
        assertEquals(1000, held.corruption());
    }

    @Test
    void oldCausesArePrunedSoTheRecordStaysSmall() {
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        CurseMachine.addDespair(s, Cause.PET_DEATH, 100, T0, t);
        CurseMachine.addDespair(s, Cause.BETRAYAL, 100, T0 + 5000, t);
        CurseMachine.pruneCauses(s, T0 + 5000, t);
        assertFalse(s.causeAt.containsKey(Cause.PET_DEATH.key()), "older than the longest pause");
        assertTrue(s.causeAt.containsKey(Cause.BETRAYAL.key()));
        assertNotNull(s.causeAt.get(Cause.BETRAYAL.key()));
    }

    @Test
    void anUnknownCauseKeyIsOtherAndHasNoPause() {
        assertEquals(Cause.OTHER, Cause.byKey("a_new_system"));
        assertEquals(Cause.OTHER, Cause.byKey(null));
        assertEquals(Cause.BETRAYAL, Cause.byKey(" Betrayal "));
        CurseTuning t = tuning();
        CurseState s = new CurseState();
        assertEquals(40, CurseMachine.addDespair(s, Cause.OTHER, 40, T0, t));
        assertEquals(40, CurseMachine.addDespair(s, Cause.OTHER, 40, T0, t), "another system's reports are not paused");
    }
}
